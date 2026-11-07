# bank-finance 后端审计报告

审计对象：`src/main/java` 全部 137 个文件、`src/main/resources` 配置、`docs/sql` 与 `db/support.sql` 表结构。

**证据等级说明**

- ✅ **已执行验证** —— 用仓库内已有的审计 harness（`.audit/ValCheck.java`、`.audit/AuthBypassCheck.java`）建 classpath 实际运行，输出见下文引用。
- ⚪ **静态阅读** —— 逐行读代码得出结论，未运行验证。

> 本次审计**未修改任何业务代码**。临时生成的 `cp.txt`（classpath 文件）已删除。

---

## 结论摘要

这个后端的核心风险不在「缺限流」，而在三个更底层的问题：

1. **认证可以被客户端选择性地不参与** —— 不发送 `Authorization` 头即可直达所有受保护接口（✅ 已实测）。
2. **核心转账功能 100% 不可用** —— 参数校验注解用错类型，请求在进入控制器前就抛异常（✅ 已实测）。
3. **资金类操作普遍是「读-改-写」，存在丢失更新** —— 可导致重复扣款、超卖、冻结资金二次释放。

限流（`@RateLimit` 全仓库仅 1 处，且实现可被绕过）只是最表层的症状。

| 严重度 | 数量 | 主要影响 |
|---|---|---|
| P0 已验证 | 2 | 认证绕过、转账不可用 |
| P0 | 5 | 越权、双花/超卖、PII 泄露、LLM 滥用 |
| P1 | 6 | 并发容量、缓存一致性、多实例重复执行 |
| P2 | 12 | 可观测性、超时、安全基线 |

---

# P0 — 已执行验证

## ✅ P0-1 认证是「可选」的：不带 `Authorization` 头即可直达受保护接口

**证据**：运行 `.audit/AuthBypassCheck.java`

```
=== JwtAuthenticationFilter behaviour ===
/account/getAccountEntity    hdr=null  -> status=200  REACHED CONTROLLER unauthenticated
/account/transfer            hdr=null  -> status=200  REACHED CONTROLLER unauthenticated
/support/admin/idcard/list   hdr=null  -> status=200  REACHED CONTROLLER unauthenticated
/stock/orders                hdr=null  -> status=200  REACHED CONTROLLER unauthenticated
--- control: a Bearer token IS present but invalid ---
/account/getAccountEntity  hdr=Bearer not.a.real.token -> status=401  blocked
/stock/orders              hdr=Bearer not.a.real.token -> status=401  blocked
--- a PUBLIC_PATH with no token ---
/stock/list                hdr=null  -> status=200  REACHED CONTROLLER unauthenticated
```

**行为与预期完全相反**：携带**非法** token 会被 401 拦截；**完全不携带**反而畅通无阻，且 `LoginContext.userId=null`、`account=null`。

**根因**（两处叠加）

1. `common/filter/JwtAuthenticationFilter.java:44` —— 全部鉴权逻辑包在 `if (authHeader != null && authHeader.startsWith("Bearer "))` 之内。没有该请求头时，代码直接落到 `filterChain.doFilter(...)`，不做任何拦截。
2. `config/SecurityConfig.java:29` —— `anyRequest().permitAll()`，Spring Security 不提供兜底。

结果：认证退化为「客户端自愿声明」，唯一防线是各 Controller/Service 中手写的 `LoginContext.getXxx() == null` 判断。这种逐方法各自为政的写法，漏一个就是一个缺口——`/support/admin/idcard/list`（身份证库，PII）正在漏的那一侧。

**附带问题**：`JwtAuthenticationFilter` 的 `PUBLIC_PATHS` 用 `Set.contains(request.getRequestURI())` 精确匹配，`/stock/list/`（多一个斜杠）会被判为受保护路径；该集合与 `SecurityConfig` 的规则重复，会随时间漂移。

**修复**

```java
// config/SecurityConfig.java —— 默认拒绝，白名单显式开放
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/account/login", "/account/register", "/account/sendCode",
                     "/account/resetPassword", "/user/login", "/user/register",
                     "/support/login", "/support/register",
                     "/stock/list", "/stock/quote", "/stock/provider",
                     "/v3/api-docs/**", "/swagger-ui/**", "/doc.html").permitAll()
    .requestMatchers("/support/admin/**").hasRole("STAFF")
    .anyRequest().authenticated())
```

同时把 `JwtAuthenticationFilter` 改为「无法确定身份 → 直接 401」，并移除手写的 `PUBLIC_PATHS`（改由 `SecurityConfig` 单点维护）。

---

## ✅ P0-2 `/account/transfer` 在参数校验阶段抛异常，控制器一行都执行不到

**证据**：运行 `.audit/ValCheck.java`

```
TRANSFER: THREW jakarta.validation.UnexpectedTypeException
          msg=HV000030: No validator could be found for constraint
          'jakarta.validation.constraints.NotBlank' validating type 'java.math.BigDecimal'.
          Check configuration for 'amount'
DEPOSIT : OK, violations=0
STOCK   : violations=0
```

**问题代码**：`dto/trade/TransferDTO.java:21`

```java
@NotBlank
private BigDecimal amount;   // @NotBlank 仅适用于 CharSequence
```

同文件的 `DepositDTO` 使用 `@NotNull + @DecimalMin`，写法正确——说明这是笔误。

**后果**：任何一笔转账在 `@Valid` 校验时即抛 `UnexpectedTypeException`，方法体零执行。该异常是 `RuntimeException`，被 `common/exception/GlobalExceptionHandler.java:56` 的 `handleRuntimeException` 兜成 `999999 系统繁忙，请稍后重试` + **HTTP 200**。即：**转账功能完全不可用，且对外表现为"偶发系统繁忙"，日志中只有一条 ERROR 堆栈，没有任何监控信号。**

**修复**

```java
@NotNull(message = "转账金额不能为空")
@DecimalMin(value = "0.01", message = "转账金额必须大于0")
private BigDecimal amount;
```

**并建议全局排查**：确认所有 `@NotBlank` 都作用在 `String` 字段上。

> 附注：`STOCK : violations=0` 表示 `StockOrderDTO` 对该测试输入无违规项，这不是缺陷——`quantity` 的「>0 且为 100 整数倍」与 `price` 的限价必填均在 `StockServiceImpl` 服务层校验。

---

# P0 — 静态审计

## P0-3 资金类操作普遍是「读-改-写」，存在丢失更新

**业务后果最严重的一类问题。**

| 位置 | 问题 |
|---|---|
| `service/impl/FinancialServiceImpl.java:119-122`（赎回） | `account.setAccountBalance(account.getAccountBalance().subtract(amount)); accountMapper.updateById(account);` —— 全字段覆盖。两个并发请求读到同一余额，后者覆盖前者 → **一笔钱花两次** |
| `service/impl/FinancialServiceImpl.java:263-264` | `product.setRemainingAmount(...)` + `updateById` 同形状 → **产品超卖** |
| `service/impl/CustomerSupportServiceImpl.java:166-172` | 受理工单是 check-then-act（读 status → 判断 → `updateById`），两个客服同时受理均成功；`reply`(:179-188)、`close`(:195-200) 同形状 |
| `service/impl/MatchingEngineImpl.java:142-164` | `mergePosition` 为 selectOne + updateById，仅在调用方持有股票锁时安全 |

**正确写法已存在于代码库中**，可照抄 —— `mapper/AccountMapper.java:21-29`：

```java
@Update("UPDATE t_account SET account_balance = account_balance - #{amount} " +
        "WHERE account_number = #{accountNumber} AND account_balance >= #{amount}")
int deductBalance(...);   // 受影响行数 == 0 即余额不足
```

**修复**：所有余额／持仓／库存／工单状态的更新统一改为「原子条件 UPDATE + 校验受影响行数」，并将业务唯一键的并发冲突映射为正确的业务错误码。

> **对早期判断的更正**：`db/support.sql` 中**已存在**唯一索引（`uk_user_phone`、`uk_account_number`、`uk_trade_no`、`uk_stock_code`、`uk_position`、`uk_staff_username`、`uk_ticket_no`、`uk_id_card`）。因此 `userRegister`/`register`/`staffRegister` 的 check-then-insert 竞态**不会**产生重复数据——数据库会拒绝。但抛出的是 `DuplicateKeyException`，被全局处理器映射为 `999999 系统繁忙` 而非预期的 `USER_EXISTS`/`ACCOUNT_EXISTS`。**即：并发下预期的业务错误码不可达，用户看到的是误导性的系统错误。** 修复方式为捕获 `DuplicateKeyException` 并转换。

---

## P0-4 客服／管理员接口缺少权限校验

| 位置 | 问题 |
|---|---|
| `controller/SupportController.java:31,37` | `/support/admin/idcard/enroll`、`/idcard/list` 无任何 staff 校验；service 层（`IdCardLibServiceImpl.enroll/listAll`）从不调用 `currentStaff()` → **匿名可读写身份证／人脸特征库**（PII + 生物特征） |
| `controller/SupportController.java:63` | `/support/admin/tickets` 的 `listAllTickets` 同样不校验，且**返回全部工单（含姓名、手机号）无分页** |
| `controller/SupportController.java:47` | `staffRegister` **公开可访问** → 任何人可自助注册为客服，随后通过所有 `currentStaff()` 检查。**完整的匿名自升权链路** |

叠加 P0-1 后，这三个接口**连 token 都不需要**。

**修复**：在 `/support/admin/**` 上统一加 `hasRole("STAFF")`；`currentStaff()` 在 service 层入口强制调用；`staffRegister` 移入管理员专用路径或加注册密钥。

---

## P0-5 越权读取他人数据（IDOR）与凭据缓存错误

| 位置 | 问题 |
|---|---|
| `service/impl/CustomerSupportServiceImpl.java:142-145` | `listMyTickets` 直接信任客户端传入的 `accountNumber`，**无归属校验** → 任意登录用户可读他人工单（户名、手机号、投诉内容） |
| `service/impl/UserServiceImpl.java:63`（另见 :222、:192） | 整个 `User` 实体（含 bcrypt hash、手机号、身份证）写入 Redis `user:cache:<phone>`；`updatePsw` 与 `bindFace` **从不失效该缓存** → **改完密码后旧密码仍可登录最长 30 分钟** |
| `service/impl/UserServiceImpl.java:183-193` | `bindFace` 用任意客户端图片即可置 `faceBound=1`，而 `verifyFace`(:199) 只检查 `faceBound==1` → **实名／KYC 门禁被整体绕过** |

**修复**：查询前统一走 `validateAccountAccess` 风格的归属校验（比对 `account.getUserId()` 与 `LoginContext`）；缓存仅存非凭据字段，并在每次用户变更时淘汰；`bindFace` 前置 `realnameBound==1` 校验。

---

## P0-6 幂等键设计错误，导致错误地「双花」或「全站阻塞」

**位置**：`service/impl/FinancialServiceImpl.java:145-148`（另见 `:88`）；`AccountServiceImpl` 的 deposit/withdraw/transfer 为同一写法（`:117`、`:184`、`:239`）

```java
} catch (Exception e) {
    redisUtil.delete("idem:purchase:" + requestId);   // 任何异常都删
    throw e;
}
```

**两个问题**

1. `catch` 中对**任何**异常都删除幂等键，包括「数据库已提交、但响应超时／连接断开」——客户端重试会**真正再扣一次**。
2. 幂等键使用**未校验**的客户端 `requestId`，且不含用户维度：

```java
"idem:purchase:" + purchaseDTO.getRequestId()   // :88
```

`requestId` 为空时所有客户端共用 `idem:purchase:null` → **全站购买被锁 10 分钟**，也可被用于跨用户抢占 key 实施 DoS。

**修复**

- 键改为 `业务:userId:requestId`，`requestId` 加 `@NotBlank` 校验。
- **不要删键**。改为 `PENDING / SUCCESS` 状态机（`SETNX PENDING` → 业务成功置 `SUCCESS` → 失败置 `FAILED` 并允许重试）。这才是真正防重放的设计。

---

## P0-7 SSE 是假异步 + LLM 外呼无任何保护

**位置**：`controller/AiController.java:43`

```java
aiService.chatStream(message, emitter);   // 同步调用
return emitter;
```

`SseEmitter` 完全没有起到异步作用：整个 LLM 流式响应在 handler 线程内同步执行，**每个流占住一个 Tomcat 线程最长 60 秒**（`AiServiceImpl.java:180` 的 read timeout）。约 200 个并发流即可打满线程池。

叠加问题：

- `AiServiceImpl.java:102-135` —— 外呼 LLM **无限流、无并发上限、无舱壁、无缓存**；`AiController.java:30-42` 的 `conn.disconnect()` 从未调用，`SseEmitter` 也未注册 `onTimeout/onCompletion/onError` → 客户端断开或 120 秒超时即泄漏 socket／文件描述符。
- `AiServiceImpl.java:132` —— `root.path("choices").get(0)` 在 `choices` 缺失／为空时 NPE。
- `AiServiceImpl.java:88`（`142-145`）—— **守卫是死代码**：`JsonNode.path(...).asInt()/asText()` 永不返回 `null`，因此「LLM 回包异常 → 降级规则引擎」的 `fallback()` 永远不会执行；被截断的响应会静默持久化 `valid=0`。AI 评分也未做区间钳制（对比 `common/ai/TicketAiAnalyzer.java:117` 有钳制）。
- `AiServiceImpl.java:121-126`、`193-198` —— 无重试退避、无熔断；任何非 200 立即抛异常，服务不可用时每张工单付出 10 秒连接 + 30 秒读取的代价。
- `AiServiceImpl.java:85` —— **提示词注入**：工单标题／正文未转义直接拼进 prompt，而模型返回的 `valid` 被持久化为自动拦截裁决（`CustomerSupportServiceImpl.java:127-130`）。

**修复**：改用有界执行器并立即返回 emitter，注册生命周期回调，`finally` 中断开连接；对 `callChatCompletion` 加 `Semaphore`/Resilience4j bulkhead + 限流 + 缓存；补 `hasNonNull` 判断与评分钳制；对用户文本做分隔与转义，且**不允许模型输出单独决定拦截**。

---

# P1 — 并发与容量

## P1-1 现有唯一的限流可被绕过，且会导致永久封禁

**现状**：`@RateLimit` 全仓库**仅 1 处**——`controller/AccountController.java:78`（登录，5 次/分钟）。

而**所有校验卡密码的接口都不限流**：`/account/deposit`、`/account/transfer`、`/account/withdraw`、股票下单（`StockServiceImpl.java:169`）→ **卡密码可被无限暴力枚举**。

**缺陷 1 —— INCR 与 EXPIRE 非原子，产生永久封禁**（`common/aspect/RateLimitAspect.java:36-39`）

```java
Long count = redisUtil.increment(key);
if (count == 1) {
    redisUtil.expire(key, rateLimit.timeWindow(), rateLimit.timeUnit());
}
```

仅在 `count == 1` 时设置 TTL。若 INCR 成功之后、EXPIRE 执行之前发生进程重启／Redis 抖动／异常，该 key 将**永远没有 TTL**；后续请求 `count` 恒 > 1，再也进不了 `expire` 分支 → **该 IP 永久无法登录**，且无自愈路径。攻击者可主动制造此状态。

**缺陷 2 —— 客户端 IP 可伪造，限流形同虚设**（`common/aspect/RateLimitAspect.java:47-57`）

```java
String xff = request.getHeader("X-Forwarded-For");
if (xff != null && !xff.isEmpty()) {
    return xff.split(",")[0].trim();
}
```

无条件下信任请求头。攻击者每次请求携带随机 `X-Forwarded-For: 1.2.3.4`，即可获得**全新计数器**，5 次限制归零。

**缺陷 3 —— 只有 IP 单一维度**：同一 NAT 出口后的正常用户会互相挤掉，而攻击者换 IP 即可绕过。

**修复**

(a) 用 Lua 将「自增 + 首次补 TTL」原子化，并保证 TTL 永不缺失（防止历史脏 key 永久卡死）：

```lua
-- KEYS[1]=key  ARGV[1]=max  ARGV[2]=windowSeconds
local n = redis.call('INCR', KEYS[1])
if n == 1 or redis.call('TTL', KEYS[1]) < 0 then
  redis.call('EXPIRE', KEYS[1], ARGV[2])
end
return n
```

(b) 真实 IP 由入口统一决定：Nginx 用 `proxy_set_header X-Forwarded-For $remote_addr;`（覆盖而非追加），Spring Boot 侧配 `server.forward-headers-strategy: native`，代码内直接使用 `request.getRemoteAddr()`。

(c) 改为 **IP + 账号双维度**：

```java
String key = "rate_limit:" + methodName + ":ip:" + ip;
String acctKey = "rate_limit:" + methodName + ":acct:" + accountNo;
```

(d) 为所有校验密码／短信码的接口补上注解，并给 `resetPassword` 增加**失败计数**（验证码错误 5 次即作废）——否则 6 位验证码在 5 分钟有效期内可被穷举。

---

## P1-2 真正的「并发限制」缺失：没有任何舱壁

| 层 | 上限 | 配置位置 |
|---|---|---|
| Tomcat 工作线程 | 200（默认） | **未配置** |
| **HikariCP 连接池** | **10（默认）** | **未配置** |
| `asyncExecutor` | 8 + 队列 100 + `CallerRunsPolicy` | `config/ThreadPoolConfig.java:19-25` |
| `scheduleExecutor` | 4 | `config/ThreadPoolConfig.java:33-34` |
| `rankPool` | 6 | `config/ThreadPoolConfig.java:42-43` |
| RabbitMQ 监听 | 2~4 | `application.yml:30-31` |
| Redis Lettuce | 8 | `application.yml:14` |

**200 个 Tomcat 线程争抢 10 个数据库连接**，四层之间无任何联动。Hikari 默认 `connectionTimeout=30s` → 池打满后请求阻塞在 `getConnection()` 上 30 秒再失败，**表现为雪崩而非过载保护**。全项目无 `Semaphore`／bulkhead，没有任何机制限制同时在跑的业务操作数。

**修复（按投入产出排序）**

1. **对齐连接池与最大并发**：显式设置 `spring.datasource.hikari.maximum-pool-size`，并把 `server.tomcat.threads.max` 降到同量级（如 50），使排队发生在连接池之外而非数据库之前。
2. **加舱壁**：对最贵／最脆弱的操作（LLM 调用、行情外呼、下单撮合）各加 `Semaphore` 或 Resilience4j `Bulkhead`，超限快速失败而非拖死线程。
3. **幂等／去重优先于限流**：下单已有的 Redisson 锁模式（`StockServiceImpl.java:149`）应推广到 deposit／withdraw。

---

## P1-3 缓存一致性缺陷（资金正确性）

**位置**：`service/impl/AccountServiceImpl.java:105`

`evictAccountCache(...)` 在**事务提交之前**执行。并发读会把**旧余额**回填进 `account:cache:*`，脏数据可存活最长 30 分钟（`CACHE_EXPIRE_MINUTES = 30`）。在资金场景下这是真实的少扣／超卖风险。

**修复**：将淘汰推迟到事务提交后注册执行：

```java
TransactionSynchronizationManager.registerSynchronization(
    new TransactionSynchronization() {
        @Override public void afterCommit() { evictAccountCache(accountNumber); }
    });
```

---

## P1-4 多实例环境下重复执行与数据损坏

| 位置 | 问题 |
|---|---|
| `schedule/RankSchedule.java:18-27` | `@Scheduled` **无分布式锁**，5 秒一次 tick，6 个并发全量 `refreshRank` → 每个实例都执行一遍；且未定义 `TaskScheduler`，与 `FinancialServiceImpl.java:212` 的凌晨 1 点日终任务**共用默认单线程调度器**，一次超过 5 秒的 tick 就会推迟日终任务 |
| `service/impl/RankServiceImpl.java:113,200` | `removeRange(0, MAX)` 后逐条回填，**非原子** → 读者会看到空榜／半榜；多实例交错写入 |
| `service/impl/RankServiceImpl.java:178-188` | `saveRankSnapshot` 用 `getRankList(type, Integer.MAX_VALUE)` 将整个 ZSET 拉入内存，再逐行 insert（6×N 次往返），`loadStockMap()` 重复读股票表 6 次 |
| `config/*MigrationRunner.java`（3 个） | 在 `CommandLineRunner` 中**修改表结构 + 全表重写密码** → 多实例并发启动互相踩 |
| `service/impl/MatchingEngineImpl.java:84` | `TradeExecutedEvent` 在**事务提交前** publish；且**没有任何监听者**（已确认全仓库仅 `RankUpdateListener.java:13` 一个 `@EventListener`）→ **成交通知被静默丢弃**。`event/OrderPlacedEvent.java` 从未被 publish（死代码） |

**修复**：`@Scheduled` 任务用 Redisson 锁包裹（依赖已存在）；榜单改为写入临时 key 后原子 `RENAME`；快照改 `saveBatch` + 只加载一次股票映射；迁移逻辑改 Flyway/Liquibase 或加分布式锁；事件改用 `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` 并补齐消费者，删除死代码。

---

## P1-5 订单簿：锁租约与撤单路径（股票核心）

**正确的部分**（值得保留）：锁在 `finally` 中释放且检查 `isHeldByCurrentThread()`（`StockServiceImpl.java:160-164`）；`transactionTemplate` 的提交（`:156`）发生在解锁（`:162`）之前——类注释声称修复的「先解锁后提交」竞态确实被正确修复了。

**缺陷 1 —— 锁租约过短，watchdog 失效**（`StockServiceImpl.java:152`）

```java
locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
```

显式指定 `leaseTime` 会**关闭 Redisson 的看门狗自动续期**。若临界区（`:156` 的事务，包含整个撮合循环）超过 10 秒，锁会**静默自动过期**，第二个线程／实例在前者仍在写入时进入 → 正是类注释声称要防止的**重复撮合**。

**修复**：改用 `tryLock(5, TimeUnit.SECONDS)`（启用 watchdog 续期），或将租约设为远高于最坏耗时的值，并配合 fencing token／状态守卫写入。

**缺陷 2 —— `cancelOrder` 不加锁且丢弃乐观锁结果**（`StockServiceImpl.java:268-283`）

`orderBookMapper.updateById(entry)`（`:273`）在 `match()` 已递增 `OrderBookEntry.version`（`@Version`，`OrderBookEntry.java:31`）时返回 0 行，代码**未检查返回值**，仍然在 `:280`/`:282` 释放冻结资金。**一次并发成交 + 该撤单会释放同一笔冻结两次 → `account_balance` 变负。**

**修复**：改用与下单相同的 `"match:" + stockCode` 锁；仅在状态转换返回 1 行时才释放资金（`UPDATE ... SET status=3 WHERE id=? AND status=0 AND version=?`）。

**缺陷 3 —— `settleBuy` 无守卫且吞掉超额释放**（`mapper/AccountMapper.java:37-42`，经 `MatchingEngineImpl.java:136` 调用）

```java
@Update("UPDATE t_account SET frozen_balance = GREATEST(frozen_balance - #{freezeAmount}, 0), " +
        "account_balance = account_balance - #{actualAmount} " +
        "WHERE account_number = #{accountNumber}")
```

- `account_balance - actualAmount` 无 `>=` 守卫 → **余额可变为负数**。
- `GREATEST(..., 0)` **静默吞掉**超额释放，恰好掩盖了缺陷 2 的资金泄漏。
- 返回的受影响行数在任何调用点都**未被检查**。

**修复**：加 `AND account_balance >= #{actualAmount}`；用带检查的减法替代 `GREATEST`；断言受影响行数 == 1。

---

## P1-6 交易事件与价格回写

| 位置 | 问题 |
|---|---|
| `StockServiceImpl.java:241-243` | 最新价被写成**来单价格**而非成交价：一笔不可成交的限价买单（如 1480）会改写贵州茅台的行情与涨跌幅。`Stock` 无 `@Version`，多实例下丢失更新静默发生。修复：用 `match()` 返回的**已成交**记录回写 `currentPrice`，并用条件 SQL 更新 |
| `MatchingEngineImpl.java:37` | `match()` 上的 `@Transactional` 具误导性而非保护性：它总是加入调用方的 template 事务，因此该类**隐式依赖外部锁**；任何其他调用方直接调用 `MatchingEngine` 接口将完全无锁地执行撮合 |
| `MatchingEngineImpl.java:94` | `tradeNo = "TRD" + System.currentTimeMillis()` 对同一毫秒内的每笔成交**必然冲突**，而 `t_trade` 有 `uk_trade_no` 唯一索引（`db/support.sql:52`）→ 抛 `DuplicateKeyException`。修复：使用 `TicketNoUtil.orderNo()` 或序列 |
| `MatchingEngineImpl.java:132-139` | **无自成交防护**：同一账户可同时为买卖双方，`settle()` 对同一账户又扣又加，`mergePosition`/`reducePosition` 命中同一持仓 → **可对倒交易**。修复：跳过 `buy.getAccountNumber().equals(sell.getAccountNumber())` 的配对 |
| `MatchingEngineImpl.java:86-87` | 若某 status 0/1 行 `remainingQuantity == 0`（如 `order_book.sql` 的历史数据），`tradeQty = 0` 且两个下标都不推进 → **死循环**，持有锁与事务。修复：守卫 `tradeQty > 0`，为 0 时推进下标 |
| `StockServiceImpl.java:269` | `entry.getStatus() != 0` 拒绝了 status 1（部分成交，`MatchingEngineImpl.java:111` 设置）→ **部分成交的订单永远无法撤单，剩余冻结资金／股份永久锁死**。修复：接受 0 与 1，按 `price × remainingQuantity` 释放 |
| `StockServiceImpl.java:197` vs `MatchingEngineImpl.java:135` | 冻结金额在 Java 侧按 DTO／现价计算，而 `settle` 从 `DECIMAL` 列重读 `buy.getPrice()` → 精度／标度差异使**每次成交的释放额 ≠ 冻结额**，`GREATEST` 掩盖了残差。修复：以同一持久化值、同一显式标度计算 |
| `StockServiceImpl.java:355` | `synchronized (this)` 仅在单 JVM 内串行化种子数据；跨实例两者都看到 `selectCount == 0` 并双双插入 → 撞 `uk_stock_code` 唯一索引抛异常，且因 `seeded` 恒为 false，**之后每次 `listStocks()`/`listQuotes()` 都会重试同一失败**。该监视器还会阻塞所有无关调用方 |
| `StockServiceImpl.java:382-410` | `initTestPositions` 在**每个未初始化环境**创建 3 个做市账户，密码 `"123456"`、余额 10 万~20 万；种子过程非事务（自动提交），中途失败留下半成品池，而 `:359`/`:382` 的 `selectCount` 守卫会**永久跳过**剩余初始化 |
| `StockServiceImpl.java:233,446,472` | `StockOrder.frozenAmount` 与 `toOrderVO` 为死代码：`:237` 创建的 `t_order` 行在撮合后**从不更新**（`filledQuantity`/`status` 恒为 0）；市价单的 `priceType` 被硬编码为 `"LIMIT"`。另外 `t_order` **没有 `order_no` 唯一约束** |

> 已确认：这两个文件中**没有** `BigDecimal` 用 `==`／`.equals()` 比较的误用（`MatchingEngineImpl.java:69,181` 用 `compareTo`，`StockServiceImpl.java:333,490` 用 `signum()`）。

---

# P2 — 可观测性、超时与安全基线

1. **异常处理不设 HTTP 状态码**（`common/exception/GlobalExceptionHandler.java:22-28`）—— 限流 `429000`、未登录 `401000` 一律返回 **HTTP 200**。网关、客户端 SDK、熔断器、监控**全都感知不到错误**。这会放大上述每一个问题，成本极低，建议尽早修。

2. **外部调用全部无超时** —— 人脸服务 `http://localhost:8001`（`application.yml:70-71`）、腾讯行情、DeepSeek 均为阻塞式。更严重的是 `FinancialServiceImpl.java:118` 的风控评估与客服的 `analyzeTicket`（`CustomerSupportServiceImpl.java:126`）**在 `@Transactional` 内做远程调用** → 一个卡住的 `localhost:8001` 就钉住数据库事务与连接。修复：移出事务 + 显式连接／读取超时 + 降级。

3. **CORS 全开且允许凭据**（`config/CorsConfig.java:16-19`）—— `allowedOriginPatterns("*")` + `allowCredentials(true)` 使任意来源均可发起带凭据请求。

4. **卡密码哈希存入 Redis** —— `Account` 实体（含 BCrypt hash）被整体序列化进 `account:cache:*`；且 `config/RedisConfig.java:55-58` 启用 Jackson `activateDefaultTyping` 并采用宽松的类型校验 → **Redis 一旦可写即构成反序列化 RCE 面**。

5. **账户枚举与无锁定**（`service/impl/UserServiceImpl.java:60,98-100`）—— 未知手机号返回 `BANK_ID_INVALID`，密码错返回 `USER_PASSWORD_INVALID`，可据此批量探测手机号；登录无失败计数。

6. **赎回资金被静默没收**（`service/impl/FinancialServiceImpl.java:245-262`）—— `claimHolding` 先将持仓置为已赎回（status=1）并写 `redeemDate`，随后 `settleHolding` 在 `account == null || accountStatus == 0` 时**直接 return 不入账**（`:259-262`），且**无任何重试路径**。批量任务 `:224-230` 有相同的提前返回。修复：先入账，或失败时回滚 claim 状态。

7. **无界全表读取** —— `listProducts()`（`FinancialServiceImpl.java:69-73`，无分页）、凌晨任务把全部 status=0 持仓载入内存且置于**单个 `@Transactional` 批次**内（任一条失败回滚整批）、`listAllTickets`（`CustomerSupportServiceImpl.java:159`）无 `LIMIT`。

8. **风控算法失效** —— `common/algo/IsolationForest.java:25,32`：空输入触发 `samples[0].length` AIOOBE；`m /= n` 产生 NaN 均值／标准差 → `:87-91` 的所有比较恒为 false，`score()` 返回 NaN，导致 `RiskAssessmentServiceImpl.java:106` 的拦截**永不触发**。修复：校验维度并拒绝非有限值。

9. **风控特征向量与训练不一致** —— `RiskAssessmentServiceImpl.java:136,151`：评分时 `feature[2]` 硬编码为 `0.0`，而训练时使用真实的交易间隔天数。修复：计算同一特征或从模型中移除该维度。

10. **`forestCache` 无界且非原子**（`RiskAssessmentServiceImpl.java:44,142-149`）—— 一个永不淘汰的 `ConcurrentHashMap`（TTL 分支仅在访问时替换），每账户 60 棵树；`get`→`build`→`put` 非原子，并发请求各自重建。修复：改用 Caffeine `maximumSize + expireAfterWrite` + `computeIfAbsent`。

11. **预测服务可抛 `NumberFormatException`**（`service/impl/TrendForecastServiceImpl.java:64`，另见 `:81-86`、`:99`）—— `BigDecimal.valueOf(NaN/Infinity)` 直接抛异常；历史查询**无 `LIMIT`**。修复：`Double.isFinite` 守卫 + 有界查询。

12. **RabbitMQ 配置与实际使用不匹配**（`application.yml:34-37`）—— 启用了 `publisher-confirm-type: correlated`、`publisher-returns`、`mandatory`，但全项目**没有 `RabbitTemplate`、`ConfirmCallback`、`ReturnsCallback`、`@RabbitListener`，也没有 DLX/DLQ**。当前配置只带来开销而不提供可靠性保证：消息静默丢失，且无消费端幂等。修复：补齐回调与死信队列，或移除该配置。

13. **排名接口无参数约束**（`controller/RankController.java:18-35`）—— `n` 未加 `@Min`/`@Max`：`n=0` 会向 Redis 发送非法范围 `end = -1`（`RankServiceImpl.java:18-35`），`n=Integer.MAX_VALUE` 会返回整个 ZSET 并触发全表读取（`getAllRankTopN` ×6）。修复：`@Validated` + `@Min(1) @Max(100)`。

14. **AI 提示词无长度限制**（`dto/support/TicketSubmitDTO.java:14-16`）—— 仅有 `@NotBlank`，无 `@Size` 也无截断 → prompt 体积／token 成本无上限。修复：`@Size` + 硬截断。

15. **`ResponseCode` 存在重复码**（`common/constant/ResponseCode.java:15-16`）—— `TRANSACTION_INVALID_FREEZE` 与 `TRANSACTION_INVALID_FREEZE_TRANSFER` 同为 `"500001"`，客户端无法区分。

---

# 建议修复顺序

| 顺序 | 事项 | 成本 | 收益 |
|---|---|---|---|
| **1** | 修 `TransferDTO.amount` 注解 | 1 行 | 核心功能从全坏变可用 |
| **2** | `SecurityConfig` 默认拒绝 + 白名单；`/support/admin/**` 加角色校验 | 1~2 文件 | 关闭整个未授权面（含 PII 与生物特征） |
| **3** | `GlobalExceptionHandler` 补 HTTP 状态码 | 极小 | 让后续所有问题**可见** |
| **4** | 余额／库存／工单改原子条件 UPDATE；`DuplicateKeyException` 映射到正确业务码 | 中 | 止血双花与超卖 |
| **5** | 幂等键改 `业务:userId:requestId` + `PENDING/SUCCESS` 状态机 | 中 | 止血重放双扣 |
| **6** | 修 `cancelOrder` 加锁与受影响行数检查；`settleBuy` 加守卫 | 中 | 止血负余额与资金泄漏 |
| **7** | `placeOrder` 锁租约修正（启用 watchdog） | 小 | 止血重复撮合 |
| **8** | 限流补全 + Lua 原子化 + 真实 IP + 双维度 | 中 | 卡密码暴破防护 |
| **9** | 显式配 HikariCP／Tomcat／外呼超时 + 舱壁 | 中 | 真正的容量保护 |
| **10** | 缓存淘汰移到 `afterCommit`；Redis 不存密码哈希 | 中 | 资金一致性 + 安全基线 |

---

## 附：复现方式

两个已验证结论的 harness 位于 `.audit/`（非生产代码，不参与构建）：

```powershell
.\mvnw.cmd -o -q dependency:build-classpath "-Dmdep.outputFile=cp.txt" "-DincludeScope=test"
$cp = ".audit\out;target\classes;" + (Get-Content cp.txt -Raw).Trim()
java -cp $cp com.bank.bankfinance.dto.trade.ValCheck    # 验证 P0-2
java -cp $cp AuthBypassCheck                            # 验证 P0-1
Remove-Item cp.txt
```

`ValCheck.java` 声明为 `package com.bank.bankfinance.dto.trade`，因此可访问 DTO 的包级私有构造器。
