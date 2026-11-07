# 后端修复记录（CHANGELOG）

> 配套文档：[`BACKEND_AUDIT.md`](./BACKEND_AUDIT.md)（28 项问题清单）。
> 本文记录**本轮实际改动的每一项**：原来是什么、为什么有问题、现在改成什么、涉及哪个文件。
> 仓库内没有 `.git`，因此以本文代替 `git diff`；每处改动在源码里也保留了 `修复记录` 注释。

## 0. 本轮结论概览

- 改动文件：**24 个**（23 个 Java + 1 个 `application.yml`），**无数据库结构变更**，`db/*.sql`、`docs/sql/*.sql` 未动。
- 编译验证：`javac` 全量编译 **144 个源文件**通过（JDK 25 + Lombok，见第 8 节）。
- 行为验证：`.audit/FixVerify.java` **52 项断言全部通过**，覆盖参数校验、HTTP 状态码映射、
  SQL 守卫、缓存失效时序、JWT 过滤器职责边界、限流脚本原子性、鉴权失败的响应出口。
- 修复主线：**认证授权**（默认拒绝）→ **限流原子性** → **资金/持仓记账的原子性与失败可见性** →
  **理财业务** → **幂等与缓存** → **配置外置**。

---

## 1. 认证与授权（最严重的一类）

### 1.1 完全不携带 token 就能访问任何接口

- **原来**：`JwtAuthenticationFilter` 把整段鉴权逻辑包在 `if (authHeader != null && authHeader.startsWith("Bearer "))` 之内，
  没有 `Authorization` 头时**直接放行且不写入 `SecurityContextHolder`**；同时 `SecurityConfig` 是 `anyRequest().permitAll()`。
  两者叠加，认证事实上退化成「客户端自愿参加」。
  更荒谬的是：**携带非法 token 反而被 401**，即只有"老实人"会被拦。
  实测（`.audit/AuthBypassCheck.java`）：不带任何 token 请求 `/account/getAccountEntity`、`/account/transfer`、
  `/support/admin/idcard/list`、`/stock/orders` 全部 **200 并进入 Controller**；带无效 token 才返回 401。
- **现在**：
  - 授权规则**单点收敛**到 `SecurityConfig`：`PUBLIC_PATHS`（登录/注册/发码/重置密码/行情/接口文档）`permitAll`，
    `/support/admin/**` 要求 `hasRole("STAFF")`，**其余一律 `anyRequest().authenticated()`**。
  - `JwtAuthenticationFilter` 只做一件事：把合法 token 解析成 `Authentication` 写入 `SecurityContextHolder`，
    同时继续填充业务侧仍在使用的 `LoginContext`。它**不再维护第二份会漂移的公开路径白名单**。
  - 角色映射：卡号身份 → `ROLE_USER`；`U_<id>` → `ROLE_USER` + `LoginContext.userId`；
    `S_<id>` → `ROLE_STAFF` **和** `ROLE_USER`（客服复用前台只读接口），并填充 `LoginContext.staffId`。
- **文件**：`common/filter/JwtAuthenticationFilter.java`、`config/SecurityConfig.java`

### 1.2 身份残留（Tomcat 线程复用导致的越权）

- **原来**：过滤器从不清理 `LoginContext`（ThreadLocal）。Tomcat 线程是复用的，
  上一个请求的身份会**泄漏到下一个请求**；一旦某个请求的 token 解析失败或缺失，业务层读到的可能是别人的卡号。
- **现在**：`doFilterInternal` 用 `try/finally` 包裹，**两个上下文都清理**（`LoginContext.clear()` + `SecurityContextHolder.clearContext()`），
  包括提前 `return` 的分支。
- **文件**：`common/filter/JwtAuthenticationFilter.java`（验证点见第 8 节：过滤后上下文为空）

### 1.3 过滤器被注册两次

- **原来**：`JwtAuthenticationFilter` 带 `@Component` 且继承 `OncePerRequestFilter`，Spring Boot 会自动把它注册成
  **全局 Servlet Filter**；它同时又通过 `addFilterBefore` 进入 Spring Security 过滤链 ⇒ 同一请求被调用两次。
- **现在**：新增 `FilterRegistrationBean<JwtAuthenticationFilter>` 并 `setEnabled(false)`，只保留 Security 链里的那一份。
- **文件**：`config/SecurityConfig.java`

### 1.4 客服注册无门槛

- **原来**：`/support/register` 任何人可调用，可直接给自己开一个客服账号（进而访问 `S_` 权限的身份证资料接口）。
- **现在**：路径仍需公开（演示/初始化），但新增可选邀请码：配置 `app.support.staff-register-key` 后，
  必须携带请求头 `X-Staff-Register-Key` 且值一致（**常量时间比较**，避免时序侧信道）；
  未配置时放行并打 WARN 日志，明确提示线上必须配置；校验失败返回新增码 `700003` → HTTP 403。
- **文件**：`controller/SupportController.java`、`common/constant/ResponseCode.java`、`application.yml`

### 1.5 跨域放开到任意站点

- **原来**：`allowedOriginPatterns("*")` **并且** `allowCredentials(true)`：任意网站都能带着用户凭据调用接口，
  等于没有同源保护；且未限制方法与缓存时间。
- **现在**：来源白名单从配置读取（`app.cors.allowed-origins`，默认仅本机常用开发端口），显式指定方法，
  `maxAge(3600)`。
- **文件**：`config/CorsConfig.java`、`application.yml`

### 1.6 授权收紧后没有"失败出口"：未登录返回 403 + 空响应体

- **原来**（1.1 改完之后才暴露出来的问题）：`SecurityConfig` 里只写了 `authorizeHttpRequests`，
  **没有配置 `exceptionHandling`**。此时 Spring Security 使用默认的
  `Http403ForbiddenEntryPoint`：未登录访问受保护接口返回的是 **HTTP 403 + 空响应体**。
  前端既拿不到 `code`，也无法区分「未登录」和「无权限」，
  **"401 → 跳登录页"的既有逻辑直接失效**——授权是收紧了，错误语义却丢了。
  （注意：只有配了 `formLogin`/`httpBasic` 时默认入口点才是 401，本项目两者都没有。）
- **现在**：显式配置两个出口，且响应体结构与 `GlobalExceptionHandler` 完全一致：

  ```java
  .exceptionHandling(ex -> ex
          .authenticationEntryPoint(this::onUnauthenticated)   // 未登录 → 401 + {code:700002}
          .accessDeniedHandler(this::onAccessDenied))          // 角色不足 → 403 + {code:700001}
  ```

  | 场景 | HTTP | body.code |
  | --- | --- | --- |
  | 无 token / token 已过期或非法 / 匿名访问受保护接口 | **401** | `700002`（未登录）或 `401000`（token 失效） |
  | 已登录但角色不足（如非客服访问 `/support/admin/**`） | **403** | `700001` |
- **文件**：`config/SecurityConfig.java`

### 1.7 认证失败响应体结构不统一

- **原来**：`JwtAuthenticationFilter` 用**手写字符串拼接**输出
  `{"code":"401000","msg":"登录已失效，请重新登录"}`，没有 `data` 字段，
  与 `GlobalExceptionHandler` 的 `Result` 结构不一致；而 `"401000"` / `"429000"` 这两个码
  只以字符串字面量散落在过滤器与异常处理器里，没有枚举常量。
- **现在**：新增 `common/util/AuthResponseWriter`，过滤器与 Security 出口共用同一份
  「状态码 + `Result` JSON」写法（注入 Spring 的 `ObjectMapper`，保证与业务响应同一套序列化配置）；
  `ResponseCode` 补上 `TOKEN_INVALID("401000")` 与 `RATE_LIMITED("429000")` 两个常量，
  消除隐式协议。
- **文件**：`common/util/AuthResponseWriter.java`（新增）、`common/filter/JwtAuthenticationFilter.java`、
  `common/constant/ResponseCode.java`

---

## 2. 限流（用户最初点名的方向）

### 2.1 限流 key 会永久锁死

- **原来**：限流是 `INCR` 之后再单独 `EXPIRE`。两步之间只要发生异常（或进程被杀），
  key 就**永久没有 TTL**；此后该维度的计数只增不减 ⇒ **这个 IP/账号被永久限流**，且不会自愈。
  另外 `TTL` 为 -1 的 key 也没有补偿逻辑。
- **现在**：改为 **Lua 脚本一次原子完成**「自增 + 首次设置 TTL + TTL 丢失时补设」：

  ```lua
  local n = redis.call('INCR', KEYS[1])
  if n == 1 or redis.call('TTL', KEYS[1]) < 0 then
      redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
  end
  return n
  ```

  为此在 `RedisUtil` 上新增 `execute(RedisScript, keys, args...)`。
- **文件**：`common/aspect/RateLimitAspect.java`、`common/util/RedisUtil.java`

### 2.2 限流维度不完整 / 可被伪造头绕过

- **原来**：只按来源 IP 限流；且**无条件信任** `X-Forwarded-For` / `X-Real-IP`，
  客户端随便换一个假 IP 就能绕过限流。
- **现在**：
  - **双维度**同时生效：`rate_limit:ip:<ip>:<Type.method>` 与 `rate_limit:subject:<主体>:<Type.method>`；
  - `X-Forwarded-For` / `X-Real-IP` 仅在 `app.security.trust-forwarded-headers=true`（默认 **false**）时才采信；
  - 主体解析字段顺序为 `accountNumber, accountNo, outNumber, inNumber, userPhoneNumber, userPhone, phone, username`——
    **付款方 `outNumber` 必须排在收款方 `inNumber` 之前**，否则转账限流会打到收款人身上；
  - 注解新增 `perAccount` 开关；`RequestContextHolder` 无请求上下文时直接放行（不做无意义拦截）；
  - **Redis 异常时降级放行**并打 WARN（与既有黑名单校验的哲学一致：不把一次 Redis 抖动升级为全站不可用）。
- **文件**：`common/annotation/RateLimit.java`、`common/aspect/RateLimitAspect.java`、`application.yml`

### 2.3 限流覆盖面（新增 17 处注解）

| 接口 | 限额 | 接口 | 限额 |
| --- | --- | --- | --- |
| `/account/deposit` 存款 | 10/分钟 | `/stock/order` 下单 | 20/分钟 |
| `/account/transfer` 转账 | 10/分钟 | `/stock/cancel/{id}` 撤单 | 20/分钟 |
| `/account/withdraw` 提现 | 10/分钟 | `/user/register` | 5/小时 |
| `/account/register` 开户 | 5/小时 | `/user/login` | 5/分钟 |
| `/account/login` | 5/分钟 | `/user/face/verify` | 10/分钟 |
| `/account/updatePsw` | 5/10 分钟 | `/user/face/realname/verify` | 10/分钟 |
| `/account/sendCode` 发验证码 | **1/60 秒** | `/support/login` | 5/分钟 |
| `/account/resetPassword` | 5/10 分钟 | `/support/ticket` 工单 | 10/小时 |
| | | `/support/register` | 5/小时 |

- **文件**：`controller/AccountController.java`、`controller/StockController.java`、
  `controller/SupportController.java`、`controller/UserController.java`

### 2.4 并发上限没有显式配置

- **原来**：连接池、Tomcat 线程全用默认值（Hikari 默认 `maximum-pool-size=10`、无 `max-lifetime`），
  Tomcat 200 个工作线程配 10 个连接 ⇒ 高并发下请求堆在获取连接上，表现为"卡住直到超时"。
- **现在**：显式配置 Hikari（`maximum-pool-size: 20`、`minimum-idle: 5`、`connection-timeout: 30s`、
  `idle-timeout: 10m`、`max-lifetime: 30m`、`validation-timeout: 5s`）与 Tomcat
  （`threads.max: 200`、`threads.min-spare: 20`、`accept-count: 200`、`max-connections: 8192`）。
- **文件**：`application.yml`

---

## 3. 资金与持仓记账：原子性 + 失败可见性

> 这一类的共同病根是两点：**用内存快照算钱**（读-算-写，丢失更新）、
> **用 `GREATEST(x-n, 0)` 把记账错误静默压成 0**（错账不报错、不留痕）。

### 3.1 出账可以花掉"已冻结"的资金

- **原来**：`deductBalance` 只守卫 `account_balance >= amount`，**完全无视 `frozen_balance`**。
  于是「挂股票买单冻结资金 → 再提现/转出同一笔钱」可以成功，账户余额被扣到低于冻结额，
  之后成交结算要么把余额扣成负数、要么结算直接失败。
- **现在**：重命名为 `deductAvailableBalance`，守卫改为
  `account_balance - frozen_balance >= #{amount}`，并**检查返回行数**；
  提现、转账、理财购买三处全部改用它。
- **文件**：`mapper/AccountMapper.java`、`service/impl/AccountServiceImpl.java`、`service/impl/FinancialServiceImpl.java`

### 3.2 超额释放冻结资金/股数被静默吞掉

- **原来**：`unfreezeBalance` / `unfreezeShares` / `settleBuy` / `settleSell` 都用
  `GREATEST(frozen_x - n, 0)`，且**返回值从没被检查过**。
  后果：重复撤单、并发撤单不会报错，会把"别人的冻结额"当成自己的释放掉；账面永远看起来正常。
- **现在**：去掉 `GREATEST`，改为**强守卫**（`frozen_balance >= #{amount}`、`frozen_quantity >= #{qty}`，
  `settleBuy` 同时守卫余额、`settleSell` 同时守卫持仓与冻结股数）。**不足时返回 0 行，由调用方回滚事务**。
  买入成交失败这种最危险的不一致（持仓加上了、钱没扣成）现在不可能静默发生。
- **文件**：`mapper/AccountMapper.java`、`mapper/PositionMapper.java`

### 3.3 撤单/结算失败无法被上层发现

- **原来**：`releaseBuyFunds` / `settleBuyFunds` 返回 `void`，调用方想检查也检查不了。
- **现在**：两个方法都返回 `boolean`（`true` 表示记账真的生效），接口 javadoc 明确"失败必须回滚"的契约；
  `MatchingEngineImpl` 在结算失败时抛 `IllegalStateException` 回滚整个撮合事务；
  `StockServiceImpl.cancelOrder` 释放失败同样抛异常回滚。
- **文件**：`service/AccountService.java`、`service/impl/AccountServiceImpl.java`、
  `service/impl/MatchingEngineImpl.java`、`service/impl/StockServiceImpl.java`

### 3.4 转账用内存余额做读-算-写

- **原来**：`transfer` 虽然用 `SELECT ... FOR UPDATE` 按卡号顺序加锁，但写入是
  `outAccount.setAccountBalance(...)` + `updateById(整行)`：任何绕过该锁路径的并发写都会被覆盖，
  而且守卫用的是余额而不是可用资金。
- **现在**：改为原子条件更新——付款方 `deductAvailableBalance`（0 行 ⇒ 可用资金不足），
  收款方 `addBalance` 增量入账。`selectForUpdate` 的加锁顺序保留（用于校验与防 A→B/B→A 死锁）。
- **文件**：`service/impl/AccountServiceImpl.java`

### 3.5 自己和自己成交

- **原来**：撮合引擎不校验买卖双方是否为同一账户。同一账户同时挂买单和卖单时会自己和自己成交：
  持仓、资金净额几乎不变，却产生成交记录、改变最新价，并可能被用来制造虚假交易量。
- **现在**：`buy.accountNumber.equals(sell.accountNumber)` 时打 WARN、推进剩余量较大的一侧索引、`continue`，
  不再产生成交。
- **文件**：`service/impl/MatchingEngineImpl.java`

### 3.6 撤单与撮合并发（锁与事务的边界）

- **原来**：`cancelOrder` 用 `@Transactional` 包住整段，然后在方法内部拿 Redisson 锁。
  Spring 的提交发生在方法返回**之后**，即"**解锁早于提交**"：锁释放的瞬间数据还没提交，
  另一个请求拿到锁后读到的是旧状态；撮合同理。另外 `tryLock(5, 10, SECONDS)` 显式指定了 `leaseTime`，
  这会**关闭 Redisson 的看门狗自动续期**——锁最多持 10 秒就被强制释放，长事务会被并发进入。
- **现在**：
  - `cancelOrder` 去掉 `@Transactional`，改为「先拿锁 → `transactionTemplate.executeWithoutResult(...)` 在锁内开启并提交事务 → finally 解锁」，
    保证**提交先于解锁**；`placeOrder` 同样用 `transactionTemplate`。
  - `tryLock` 去掉显式 `leaseTime`（`tryLock(5, TimeUnit.SECONDS)`），恢复看门狗自动续期。
  - **新增：支持撤销"部分成交"的委托**（状态 1）。原实现只允许 `status=0`，
    部分成交的委托（`status=1`）**永远撤不掉**，剩余冻结资金/股数被永久锁死。
    现在用条件更新 `... AND status IN (0,1) SET status=3`，0 行则抛 `STOCK_ORDER_NOT_CANCELLABLE`，
    只释放**剩余未成交数量**的冻结；释放失败抛异常回滚。
- **文件**：`service/impl/StockServiceImpl.java`

### 3.7 成交价不落库 / 最新价被无条件覆盖

- **原来**：下单成功后**无条件**把 `stock.current_price` 改写成"本次委托价"，
  即使这笔委托没有成交（挂在盘口没撮合）也会篡改行情最新价；限价委托的价格也没有统一精度。
- **现在**：只有真正产生了成交（`trades` 非空）才更新 `current_price / change_percent / update_date`，
  且取**最后一笔成交价**；限价单价格统一 `setScale(3, HALF_UP)` 对齐 `t_order_book.price DECIMAL(12,3)`。
- **文件**：`service/impl/StockServiceImpl.java`

### 3.8 时间/流水号

- 成交流水号由自拼字符串改为 `TicketNoUtil.tradeNo()`（与其它业务统一，避免并发重号）。
- **文件**：`service/impl/MatchingEngineImpl.java`

---

## 4. 理财业务

### 4.1 产品额度可被超卖

- **原来**：`purchase` 先读 `product.remainingAmount` 比较，再 `setRemainingAmount(remaining - amount)` +
  `updateById(product)`。并发购买时两个请求读到同一个 `remaining_amount`，各自减完再写回，
  **后写的覆盖先写的** ⇒ 额度永远减不到 0，卖出总额可以超过 `total_amount`。
- **现在**：`FinancialProductMapper` 新增数据库层原子扣减：
  `UPDATE t_financial_product SET remaining_amount = remaining_amount - #{amount}
  WHERE id = #{id} AND status = 1 AND remaining_amount >= #{amount}`，返回 0 行即额度不足。
- **文件**：`mapper/FinancialProductMapper.java`、`service/impl/FinancialServiceImpl.java`

### 4.2 购买扣款用内存快照

- **原来**：`account.setAccountBalance(余额 - amount)` + `accountMapper.updateById(account)`，
  守卫是余额而非可用资金，且 `getAccountEntity` 可能直接命中 30 分钟的 Redis 缓存，快照更旧。
- **现在**：改用 `deductAvailableBalance` 原子扣减，0 行 ⇒ 资金不足。
- **文件**：`service/impl/FinancialServiceImpl.java`

### 4.3 到期赎回会"静默没收"用户资金

- **原来**：调用方先 `claimHolding` 把持仓状态置为**已赎回**，接着 `settleHolding` 里
  「查不到账户 / 账户已冻结就直接 `return`」——**钱没入账，持仓却已经是已赎回**，
  连一条日志都没有。用户的钱就这样消失了。
- **现在**：`settleHolding` 入账时检查 `addBalance` 返回行数，**0 行直接抛异常**，
  让调用方事务把状态位一起回滚；"是否允许结算"（账户是否存在/是否冻结）由调用方先判断：
  用户主动赎回 → 明确报错；定时任务 → 跳过并告警，留到下一轮。
- **文件**：`service/impl/FinancialServiceImpl.java`

### 4.4 定时结算一个失败拖垮全部、且失败无痕

- **原来**：`autoSettleMaturedHoldings` 在**整个 for 循环外**套了 `@Transactional`：
  任何一笔持仓结算抛异常（例如插入流水失败），当天**所有**已到期持仓一起回滚，而且只有异常堆栈，
  看不出"哪些持仓没结"。
- **现在**：改为**每笔持仓一个独立事务**（`transactionTemplate.executeWithoutResult`），
  失败只影响自己并打 `error` 日志（含 holdingId / 账号），状态位回滚为未结算、下一轮自动重试；
  账户冻结/不存在则明确跳过并打 WARN，等待人工处理。
- **文件**：`service/impl/FinancialServiceImpl.java`

### 4.5 幂等 key 没有账户维度

- **原来**：`idem:purchase:<requestId>`。`requestId` 由客户端生成，于是 A 用户撞上（或猜到）
  B 用户用过的 `requestId` 就会被判成重复提交而买不进去；反过来，恶意方只要复用别人的 `requestId`
  就能让对方 10 分钟内无法购买。
- **现在**：改为 `idem:purchase:<accountNumber>:<requestId>`，异常时清理的也是同一个 key。
- **文件**：`service/impl/FinancialServiceImpl.java`

---

## 5. 幂等与缓存

### 5.1 存款/转账/提现的幂等 key 同样缺少账户维度

- **原来**：`idem:deposit:<requestId>`、`idem:transfer:<requestId>`、`idem:withdraw:<requestId>`，
  问题与 4.5 相同（跨用户互相干扰、可被他人"占位"）。
- **现在**：
  - `idem:deposit:<accountNumber>:<requestId>`
  - `idem:withdraw:<accountNumber>:<requestId>`
  - `idem:transfer:<outNumber>:<inNumber>:<requestId>`
  - 异常清理使用同一个 key 变量（原实现两处硬编码字符串，容易改漏）。
- **文件**：`service/impl/AccountServiceImpl.java`

> 说明：理财**赎回**没有加幂等 key，因为它的幂等性由 `claimHolding` 的条件更新
> （`WHERE id=? AND status=0`）在数据库层保证，重复提交只有一个能成功。

### 5.2 缓存删除发生在事务提交之前 ⇒ 等于白删

- **原来**：`evictAccountCache` 在事务内直接 `DELETE` Redis key。若此刻有并发请求读到
  **尚未提交**的旧数据并回填缓存，删除动作就白做了，**30 分钟内所有人都看到旧余额**。
- **现在**：`evictAccountCache` 若检测到事务同步已激活，就注册 `TransactionSynchronization`，
  把删除推迟到**事务完成之后**执行；非事务上下文行为不变（立即删除）。
- **文件**：`service/impl/AccountServiceImpl.java`（验证点见第 8 节）

### 5.3 买理财之后余额不刷新

- **原来**：理财购买/赎回直接改 `t_account` 的余额，但**没有任何地方清账户缓存**，
  用户买完理财看到的还是买之前的余额（最长 30 分钟才自愈）。
- **现在**：`AccountService` 对外暴露 `evictAccountCache(String)`，理财购买/赎回在改动余额后调用它
  （同样是事务后生效）。
- **文件**：`service/AccountService.java`、`service/impl/AccountServiceImpl.java`、
  `service/impl/FinancialServiceImpl.java`

### 5.4 把口令散列和人脸特征缓存进 Redis，并且改了密码不生效

- **原来**：`getUserEntity` 把整个 `User` 实体缓存到 `user:cache:<phone>`（30 分钟），
  而该实体携带 **bcrypt 口令散列**和**人脸特征模板**：
  1. 凭据与生物特征被复制到第二个存储系统，Redis 通常没有数据库同等的访问控制/审计/静态加密，
     一次 Redis 可读就等于拿到全量凭据，且**改密码也挽不回生物特征**；
  2. 缓存**没有任何失效路径**：`updatePsw` 只更新数据库，登录仍从缓存取旧散列 ⇒
     **旧密码在最长 30 分钟内继续可用、新密码反而登录失败**（用户视角：密码改不掉）。
- **现在**：`getUserEntity` 改为直接查库（该方法只服务登录，`/user/login` 已限流 5 次/分钟），
  同时移除不再使用的 `RedisUtil` 依赖与相关常量。改密码**立即生效**。
- **文件**：`service/impl/UserServiceImpl.java`

---

## 6. 参数校验与错误语义

### 6.1 转账功能整体不可用（`@NotBlank` 用在 `BigDecimal` 上）

- **原来**：`TransferDTO.amount` 标了 `@NotBlank`。`@NotBlank` 只对 `CharSequence` 生效，
  作用在 `BigDecimal` 上会让 Hibernate Validator 抛
  `UnexpectedTypeException: HV000030: No validator could be found for constraint 'jakarta.validation.constraints.NotBlank'
  validating type 'java.math.BigDecimal'`——**任何转账请求都在校验阶段 500**。
- **现在**：与 `DepositDTO` 对齐，改为 `@NotNull(message="转账金额不能为空")` + `@DecimalMin("0.01", ...)`。
- **文件**：`dto/trade/TransferDTO.java`

### 6.2 响应码撞码

- **原来**：`TRANSACTION_INVALID_FREEZE_TRANSFER` 的码也是 `500001`，与 `TRANSACTION_INVALID_FREEZE` 完全相同，
  前端无法区分"本方账户冻结"和"对方账户冻结"。
- **现在**：改为 `500004`；并新增五个码：
  - `TOKEN_INVALID("401000")` token 缺失/非法/已登出（原为散落的字符串字面量，见 1.7）
  - `RATE_LIMITED("429000")` 限流命中（原为临时拼接的字符串，见 1.7）
  - `IDEMPOTENT_REJECTED("600019")` 重复提交
  - `DUPLICATE_RECORD("600020", "记录已存在，请勿重复提交")`
  - `STAFF_REGISTER_FORBIDDEN("700003")` 客服注册校验失败
- **文件**：`common/constant/ResponseCode.java`

### 6.3 所有错误都返回 HTTP 200

- **原来**：`GlobalExceptionHandler` 的每个分支都返回 `Result`，即**恒定 HTTP 200**：
  限流、未登录、越权、服务端异常在网关/SDK/熔断器/监控眼里**全都是成功**，
  这个缺陷会放大系统内每一个其它问题。
- **现在**：改为返回 `ResponseEntity<Result<?>>`，**刻意保持向后兼容**：
  - 普通业务错误（余额不足、密码错误……）**仍是 HTTP 200**，只靠响应体 `code` 区分（既有前端契约不变）；
  - 需要中间件/客户端采取行动的错误给真实状态码：`429000` → **429**、
    `401000`/`NO_LOGIN` → **401**、`NO_ADMIN_PERMISSION`/`STAFF_REGISTER_FORBIDDEN` → **403**、
    参数校验/约束违反/缺参/类型不匹配/请求体不可解析 → **400**、未捕获异常 → **500**；
  - 唯一索引冲突单独处理：`DuplicateKeyException` → 业务码 `600020` + HTTP 200 + WARN 日志
    （原来落到 `RuntimeException` 分支返回"系统繁忙"，把并发注册这类正常冲突说成服务端故障）；
  - 提供回退开关：`app.error.expose-http-status=false` ⇒ 恢复"全部 200"的旧行为。
- **文件**：`common/exception/GlobalExceptionHandler.java`、`application.yml`

---

## 7. 配置外置（`application.yml`）

新增 `app.*` 配置块（默认值都保持原行为或更安全的取值）：

| 配置项 | 默认值 | 作用 |
| --- | --- | --- |
| `app.error.expose-http-status` | `true` | 是否把业务错误映射为真实 HTTP 状态码；设 `false` 回退旧行为 |
| `app.cors.allowed-origins` | 本机 5173/3000/8080 | 跨域来源白名单（逗号分隔） |
| `app.security.trust-forwarded-headers` | `false` | 是否信任 `X-Forwarded-For`/`X-Real-IP`（仅限可信反代后面才开） |
| `app.support.staff-register-key` | `${STAFF_REGISTER_KEY:}`（空） | 客服注册邀请码；空 = 不校验并告警 |

同时新增 Hikari 连接池与 Tomcat 线程/连接配置（见 2.4）。YAML 已用解析器验证结构正确。

---

## 8. 验证方式与证据

由于本机没有可用的 MySQL / Redis / RabbitMQ，无法启动完整应用或跑 `@SpringBootTest`，
因此采用**两层可复现验证**：

1. **全量编译**：`javac` 编译 `src/main/java` 下全部 **143 个源文件**（产物 160 个 class）通过。
   注意 JDK 25 下必须加 `-proc:full`，否则 Lombok 注解处理器不运行、会报大量"找不到符号"。
2. **行为断言**：`.audit/FixVerify.java`（无需任何中间件），**46 项全部 PASS**：

   | 组 | 覆盖内容 |
   | --- | --- |
   | 1（4 项） | `TransferDTO` 校验：合法金额通过、`null`/`0.00` 被拒且是业务文案（HV000030 已消失） |
   | 2（7 项） | 异常映射：429000→429、未登录→401、越权→403、客服注册→403、普通业务码仍 200、运行时异常→500、开关关闭时回退 200 |
   | 3（12 项） | SQL 守卫：`settleBuy`/`unfreezeBalance`/`deductAvailableBalance`/`reserveAmount`/`unfreezeShares`/`settleSell` 的守卫条件齐备，且**不再出现 `GREATEST`** |
   | 4（3 项） | 缓存失效时序：非事务立即删、事务中不删、事务完成后删 |
   | 5（12 项） | JWT 过滤器：无 token 不写身份、合法卡号/U_/S_ 三种主体与角色映射正确、非法与黑名单 token 401 且不进业务链、过滤后上下文清空 |
   | 6（4 项） | 限流：Lua 含 `INCR`/`EXPIRE`/`TTL` 补偿，主体字段 `outNumber` 优先于 `inNumber` |
   | 7（4 项） | 接口契约：`releaseBuyFunds`/`settleBuyFunds` 返回 `boolean`、`evictAccountCache` 已暴露 |
   | 8（6 项） | 鉴权失败出口：未认证 → 401 + `{code:700002}`、角色不足 → 403 + `{code:700001}`、非法 token → 401 + `{code:401000}`，三者响应体都含 `data` 字段（结构一致） |

   复现命令（工作目录 = 项目根）：

   ```powershell
   # 1) 取依赖 classpath（只写 .audit，避免动 target/）
   .\mvnw.cmd -o -q dependency:build-classpath "-Dmdep.outputFile=.audit/cp.txt" "-DincludeScope=test"
   # 2) 编译全部源码
   Get-ChildItem -Recurse -Filter *.java .\src\main\java | ForEach-Object { $_.FullName.Replace('\','/') } |
       Set-Content .\.audit\sources.txt -Encoding ascii
   javac -nowarn -proc:full -encoding UTF-8 -cp (Get-Content .\.audit\cp.txt -Raw) -d .\.audit\out "@.audit/sources.txt"
   # 3) 运行行为断言
   $cp = (Get-Content .\.audit\cp.txt -Raw) + ";" + (Resolve-Path .\.audit\out).Path
   javac -nowarn -proc:full -encoding UTF-8 -cp $cp -d .\.audit\out .\.audit\FixVerify.java
   java -cp $cp FixVerify
   ```

   `.audit/AuthBypassCheck.java` 是最初复现"无 token 直达 Controller"的证据脚本，
   修复后过滤器不再承担授权职责，该脚本已**过时**（保留仅作历史证据）。

---

## 9. 本轮**没有**改的部分（明确取舍，避免误以为已修）

| 项 | 现状 | 为什么先不动 |
| --- | --- | --- |
| 账户枚举 | `BANK_ID_INVALID`（卡号不存在）与 `USER_EXISTS`/密码错误可区分，可用于撞库探测 | 属于接口契约变更，会影响前端提示文案，建议单独一轮统一处理 |
| 无界查询分页 | `listHoldings`、交易流水等仍全量返回 | 需要同步改前端分页组件，超出"后端修复"范围 |
| `@Scheduled` 分布式锁 | 定时结算在多实例下会重复扫描 | **正确性已有保障**：`claimHolding` 的条件更新（`WHERE id=? AND status=0`）保证同一持仓只会被结算一次；加分布式锁只是省重复扫描 |
| Redis 排行榜更新原子性 | 排序榜的 `delete + zAdd` 非原子，极端并发下可能瞬时为空 | 只影响榜单展示，不影响资金；改用 `ZADD`/`RENAME` 需先确认榜单 key 结构与前端容错 |
| `TradeExecutedEvent` | 事件在事务内发布，但**全项目没有任何监听者**，等于空操作 | 要么删事件、要么补监听者，属功能取舍，需产品确认 |
| RabbitMQ 回调 / 死信队列 | 未处理 `ConfirmCallback`/`ReturnsCallback`，无 DLQ | 需要与运维确认交换机/队列策略，且当前业务链路不依赖 MQ 才正确 |
| 外部调用超时 | 人脸/大模型调用的超时与重试策略未统一收敛 | 人脸服务仅有 `face.timeout-ms`；统一需要引入超时/熔断配置，建议独立一轮 |
| `IsolationForest` NaN | 特征含 NaN 时的分支未处理 | 属风控模型内部逻辑，需先确认特征缺失的预期语义 |
| `user:cache` 之外的用户缓存 | 已按要求移除 `User` 实体缓存 | —— |
| `verifyRealName` 与 `bindFace` 的差异 | `bindFace` 不做身份证库比对（自助绑脸） | 修改会改变人脸绑定的产品流程，需产品确认 |

---

## 10. 上线前请确认

1. **数据库无需变更**：本轮没有 DDL，唯一索引原本就存在（`db/support.sql`、`docs/sql/order_book.sql`）。
2. **必须配置的环境变量**：`STAFF_REGISTER_KEY`（否则客服注册无门槛，仅会打 WARN）、
   `CORS` 白名单（`app.cors.allowed-origins` 默认只允许本机开发端口，线上务必改成真实域名）。
3. **前端契约**：如果前端只认「HTTP 200 + body.code」，请把 `app.error.expose-http-status` 设为 `false`；
   建议逐步迁移到按真实状态码处理（401 跳登录、429 做退避）。
   注意**未登录/无权限**现在由 Spring Security 直接返回 **401/403 + 项目统一 JSON**
   （该路径不经过 `GlobalExceptionHandler`，所以 `expose-http-status=false` 不改变它）——
   前端需要同时处理「HTTP 状态码」和「body.code」，否则会出现「接口 401 但页面不跳登录」。
4. **行为变化（可能影响既有测试/脚本）**：
   - 撤单现在允许撤销"部分成交"的委托（原来会永久锁死剩余冻结）；
   - 买入结算失败会让撮合事务回滚（这是**刻意**的 fail-closed）；
   - 提现/转账/理财购买不再能花掉已冻结资金；
   - 限流在 Redis 不可用时降级放行（只告警）。
5. **部署在多实例时**：定时结算会重复扫描（但不会重复结算），如需减少无谓扫描可后续加分布式锁。
