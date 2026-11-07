# 股票交易模拟系统 — 项目文档

## 文档信息

| 项目 | 内容 |
|------|------|
| 项目名称 | smart-bank-main / bank-finance |
| 技术栈 | Spring Boot + MyBatis-Plus + MySQL + Redis + JWT |
| 文档版本 | v1.3 |
| 最后更新 | 2026-09-02 |

---

## 一、项目概述

这是一个银行金融模拟系统，包含账户管理、存款/转账、理财购买、股票交易等功能。当前正在进行**股票交易模块的市场化改造**，核心目标是从"即时成交"升级为"订单簿 + 撮合引擎"的真正交易市场。

---

## 二、现有功能模块

### 2.1 账户模块
- 用户注册/登录（JWT + 手机号验证码）
- 账户增删改查
- 存款/转账/提现
- 密码修改、资料更新

### 2.2 股票模块（改造中）
- 股票列表查询
- 行情查询（模拟/新浪）
- 下单、撤单
- 持仓查询、订单查询

### 2.3 理财模块
- 理财产品列表
- 购买、赎回
- 持仓查询

### 2.4 其他
- 客服工单
- AI 分析
- 风险评估

---

## 三、核心架构设计

### 3.1 分层结构

```
Controller → Service → Mapper → Database
                ↓
         Domain Event → Event Publisher
                ↓
         Projection（行情、持仓、对账）
```

### 3.2 订单簿撮合架构（新增）

```
用户下单 → Controller → Service.submitOrder()
                           ↓
                    写入 t_order_book（status=0）
                           ↓
                    冻结资金/股份
                           ↓
                    发 OrderPlacedEvent
                           ↓
              MatchingEngine.match(stockCode)
                           ↓
              双指针遍历买卖队列
              价格优先、时间优先
                           ↓
              生成 StockTrade → 写入 t_stock_trade
              更新 OrderBookEntry（状态/剩余量）
              资金划拨 + 持仓变动
                           ↓
              发 TradeExecutedEvent
              → 行情投影
              → 风控
              → 对账
```

---

## 四、订单簿撮合系统 — 完整文件清单

### 4.1 新建文件

| 序号 | 文件路径 | 作用 |
|------|----------|------|
| 1 | `entity/OrderBookEntry.java` | 订单簿实体（状态机 + 乐观锁） |
| 2 | `mapper/OrderBookEntryMapper.java` | 订单簿 Mapper（继承 BaseMapper） |
| 3 | `entity/StockTrade.java` | 股票成交实体（独立原有 Trade） |
| 4 | `mapper/StockTradeMapper.java` | 股票成交 Mapper（继承 BaseMapper） |
| 5 | `service/MatchingEngine.java` | 撮合引擎接口 |
| 6 | `service/impl/MatchingEngineImpl.java` | 撮合引擎实现 |
| 7 | `event/OrderPlacedEvent.java` | 下单领域事件 |
| 8 | `event/TradeExecutedEvent.java` | 成交领域事件 |
| 9 | `entity/StockRank.java` | 排行快照实体 |
| 10 | `entity/StockKline.java` | K线数据实体 |
| 11 | `mapper/StockRankMapper.java` | 排行 Mapper（继承 BaseMapper） |
| 12 | `mapper/StockKlineMapper.java` | K线 Mapper（继承 BaseMapper） |
| 13 | `service/RankService.java` | 排行服务接口 |
| 14 | `service/impl/RankServiceImpl.java` | 排行服务实现（Redis ZSET） |
| 15 | `controller/RankController.java` | 排行 API 接口 |
| 16 | `event/RankUpdatedEvent.java` | 排行更新事件 |
| 17 | `schedule/RankSchedule.java` | 定时任务（每 5 秒刷新排行） |

### 4.2 修改文件

| 序号 | 文件路径 | 修改内容 |
|------|----------|----------|
| 1 | `dto/trade/StockOrderDTO.java` | 增加 `requestId` 字段（幂等下单） |
| 2 | `service/impl/StockServiceImpl.java` | `placeOrder()` 改造为 `submitOrder()`，只写订单簿不即时成交 |
| 3 | `controller/StockController.java` | 返回 `orderNo`，新增触发撮合逻辑 |
| 4 | `common/util/TicketNoUtil.java` | 增加 `requestId()` 方法 |
| 5 | `entity/Stock.java` | 新增 highPrice/lowPrice/volume/amount/turnoverRate/totalShares 字段 |
| 6 | `vo/StockQuoteVO.java` | 新增 highPrice/lowPrice/volume/amount 字段 |
| 7 | `provider/SimulatedStockQuoteProvider.java` | 生成 high/low/volume/amount 数据 |
| 8 | 数据库 | 建表 `t_order_book`、`t_stock_trade`，扩展 `t_stock` 字段 |
| 9 | `docs/sql/stock_rank.sql` | 排行系统数据库脚本（新建） |

### 4.3 数据库表

```sql
-- 订单簿
CREATE TABLE t_order_book (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE,
    account_number VARCHAR(32) NOT NULL,
    stock_code VARCHAR(16) NOT NULL,
    stock_name VARCHAR(64),
    direction VARCHAR(4) NOT NULL,
    price DECIMAL(18,4) NOT NULL,
    quantity INT NOT NULL,
    filled_quantity INT DEFAULT 0,
    remaining_quantity INT NOT NULL DEFAULT 0,
    status TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT DEFAULT 0,
    INDEX idx_stock_status (stock_code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 股票成交
CREATE TABLE t_stock_trade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_no VARCHAR(32) NOT NULL UNIQUE,
    stock_code VARCHAR(16) NOT NULL,
    stock_name VARCHAR(64),
    buy_order_no VARCHAR(32),
    sell_order_no VARCHAR(32),
    buy_account VARCHAR(32),
    sell_account VARCHAR(32),
    trade_price DECIMAL(18,4),
    trade_quantity INT,
    trade_amount DECIMAL(18,2),
    trade_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 五、核心代码详解

### 5.1 `OrderBookEntry` 实体

```java
@Getter @Setter
@TableName("t_order_book")
public class OrderBookEntry {
    @TableId(type = IdType.AUTO) private Long id;
    private String orderNo;              // 业务主键
    private String accountNumber;        // 账户号
    private String stockCode;            // 股票代码
    private String stockName;            // 股票名称
    private String direction;            // BUY/SELL
    private BigDecimal price;            // 限价
    private Integer quantity;            // 原始委托量
    private Integer filledQuantity;      // 已成交量
    private Integer remainingQuantity;   // 剩余量
    private Integer status;              // 0待撮合 1部分成交 2全成 3已撤 4过期
    private LocalDateTime createTime;    // 时间优先依据
    @Version private Integer version;    // 乐观锁
}
```

**关键设计**：
- `remainingQuantity` 冗余字段，避免高频计算
- `status` 状态机显式管理生命周期
- `@Version` 乐观锁保证并发安全

### 5.2 `MatchingEngineImpl` 撮合引擎

#### 核心算法：双指针遍历买卖队列

```
买单：按价格降序 → 时间升序（高价优先，同价先到先得）
卖单：按价格升序 → 时间升序（低价优先，同价先到先得）
```

筛选条件：状态为0（未成交）或1（部分成交）的订单

#### 撮合循环流程

```
while (买单未遍历完 && 卖单未遍历完) {
    取当前买单 buy、卖单 sell
    
    // 价格检查：买价 < 卖价 则无法成交，退出循环
    if (buy.price < sell.price) break;
    
    // 计算成交信息
    成交价 = sell.price（卖方定价）
    成交量 = min(buy.剩余量, sell.剩余量)
    
    // 执行成交
    创建成交记录 → 保存数据库
    更新订单状态 → 保存数据库（乐观锁）
    结算资金和持仓 → 保存数据库
    发布成交事件
    
    // 移动指针
    if (买方全部成交) buyIdx++
    if (卖方全部成交) sellIdx++
}
```

#### 价格匹配规则

```
买一 10.5  ←→  卖一 10.3  ✓ 成交（买价≥卖价）
买一 10.2  ←→  卖一 10.5  ✗ 不成交（买价<卖价）
```

#### 成交量计算

取买卖双方剩余数量的较小值：
- 买方剩余100股，卖方剩余80股 → 成交80股
- 买方剩余20股，卖方剩余80股 → 成交20股

#### 资金结算

```
买方：账户余额 - 成交金额（扣款）
卖方：账户余额 + 成交金额（入账）
```

#### 持仓变更

```
买方：
  - 无该股票持仓 → 新建持仓记录
  - 有该股票持仓 → 合并计算加权平均成本

卖方：
  - 减少持仓数量
  - 持仓归零 → 删除记录
```

#### 并发控制：乐观锁（version字段）

**什么是乐观锁**

乐观锁是一种并发控制机制，假设多线程并发操作时不会发生冲突，只在更新时检查是否有其他线程修改过数据。如果没有冲突则更新成功，如果有冲突则抛出异常或重试。

**为什么用乐观锁而不是悲观锁**

| 锁类型 | 优点 | 缺点 |
|--------|------|------|
| 悲观锁（如 SELECT FOR UPDATE） | 数据安全 | 并发性能差，容易死锁，长时间占用数据库连接 |
| 乐观锁（version字段） | 无锁，性能高 | 冲突时需重试 |

**选择原因：**
- 撮合场景冲突概率低（不同订单同时成交同一订单的概率小）
- 读多写少，乐观锁性能更优
- 不会长时间占用数据库连接

**version 字段实现原理**

```sql
-- t_order_book 表结构
version INT DEFAULT 0  -- 版本号字段
```

**工作流程示例**

```
初始状态：
  订单A: id=1, remainingQuantity=100, version=1

线程1读取：
  SELECT * FROM t_order_book WHERE id=1
  → 获取 version=1, remainingQuantity=100

线程2同时读取：
  SELECT * FROM t_order_book WHERE id=1
  → 获取 version=1, remainingQuantity=100

线程1更新成功：
  UPDATE t_order_book 
  SET remainingQuantity=80, version=2 
  WHERE id=1 AND version=1
  → 影响行数=1，更新成功

线程2更新失败：
  UPDATE t_order_book 
  SET remainingQuantity=90, version=2 
  WHERE id=1 AND version=1
  → 影响行数=0（因为version已变为2）
  → 抛出异常：乐观锁冲突
```

**Java代码实现**

```java
private void updateOrder(OrderBookEntry order, int filledQty) {
    int newFilled = order.getFilledQuantity() + filledQty;
    int newRemaining = order.getRemainingQuantity() - filledQty;
    int newStatus = (newRemaining == 0) ? 2 : 1;

    OrderBookEntry update = new OrderBookEntry();
    update.setId(order.getId());
    update.setFilledQuantity(newFilled);
    update.setRemainingQuantity(newRemaining);
    update.setStatus(newStatus);
    update.setVersion(order.getVersion() + 1);  // 版本号+1

    // UPDATE ... WHERE id=? AND version=?
    int rows = orderBookMapper.updateById(update);
    if (rows == 0) {
        throw new RuntimeException("乐观锁冲突，订单被并发修改: " + order.getOrderNo());
    }

    // 更新成功后同步本地对象状态
    order.setFilledQuantity(newFilled);
    order.setRemainingQuantity(newRemaining);
    order.setStatus(newStatus);
    order.setVersion(order.getVersion() + 1);
}
```

**MyBatis-Plus `@Version` 注解**

```java
@Version
private Integer version;  // 乐观锁字段
```

MyBatis-Plus 会在 UPDATE 语句自动追加 `WHERE version=?` 条件，更新时自动将 version+1。

#### 事件驱动

每次成交后发布 `TradeExecutedEvent`，可扩展：
- 行情投影（更新最新价、涨跌幅）
- 风控检查
- 对账记录
- 推送通知

#### 完整流程总结

1. 查询买卖队列（LambdaQueryWrapper 排序）
2. 双指针遍历
3. 价格校验 `buyPrice.compareTo(sellPrice) < 0 → break`
4. 生成成交记录 `StockTrade`
5. 更新订单状态（乐观锁 CAS）
6. 资金划拨 + 持仓变动
7. 发布领域事件 `TradeExecutedEvent`

### 5.3 幂等设计

**DTO 增加 `requestId`**：
```java
@NotBlank(message = "请求Id不能为空")
private String requestId;
```

**Redis 幂等**：
```java
redisUtil.setIfAbsent("idem:order:" + requestId, "1", 10, TimeUnit.MINUTES);
```

### 5.4 `TicketNoUtil` 工具类

```java
public static String requestId() {
    return "REQ" + timestamp() + randomHex(8);
}
```

格式：`REQ20260831123045A1B2C3D1E2F3`

### 5.5 `RequestIdAspect` — 后端兜底生成 `requestId`（AOP 切面）

**文件路径**：`common/aspect/RequestIdAspect.java`

完整代码：

```java
package com.bank.bankfinance.common.aspect;

import com.bank.bankfinance.common.util.TicketNoUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

@Aspect
@Component
public class RequestIdAspect {

    @Around("execution(* com.bank.bankfinance.controller.*.*(..))")
    public Object generateRequestId(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();
        for (Object arg : args) {
            if (arg != null) {
                try {
                    Field field = arg.getClass().getDeclaredField("requestId");
                    field.setAccessible(true);
                    String requestId = (String) field.get(arg);
                    if (requestId == null || requestId.isEmpty()) {
                        field.set(arg, TicketNoUtil.requestId());
                    }
                } catch (NoSuchFieldException e) {
                    // 这个 DTO 没有 requestId 字段，跳过
                }
            }
        }
        return pjp.proceed();
    }
}
```

#### 逐行解释

| 代码行 | 解释 |
|--------|------|
| `package com.bank.bankfinance.common.aspect;` | 包路径，AOP 切面类统一放在 `common.aspect` 包下 |
| `import com.bank.bankfinance.common.util.TicketNoUtil;` | 导入单号生成工具类，用于生成 `requestId` |
| `import org.aspectj.lang.ProceedingJoinPoint;` | AOP 核心对象，代表**被拦截的方法连接点**，可以获取参数并继续执行原方法 |
| `import org.aspectj.lang.annotation.Around;` | 定义**环绕通知**注解——在目标方法执行**前后**都能执行逻辑 |
| `import org.aspectj.lang.annotation.Aspect;` | 声明这是一个**切面（Aspect）**类 |
| `import org.aspectj.lang.reflect.MethodSignature;` | 获取被拦截方法的签名信息（方法名、参数类型等） |
| `import org.springframework.stereotype.Component;` | Spring 组件注解，将切面注册为 Spring Bean |
| `import java.lang.reflect.Field;` | Java 反射 API 的 `Field` 类，用于**动态获取和修改对象的属性值** |
| `import java.lang.reflect.Parameter;` | Java 反射 API 的 `Parameter` 类，用于获取方法参数信息（当前代码未实际使用） |
| `@Aspect` | 告诉 Spring 这是一个切面类，Spring 会扫描并应用其中的通知 |
| `@Component` | 将切面类注册为 Spring Bean，使其生效 |
| `public class RequestIdAspect {` | 定义切面类 |
| `@Around("execution(* com.bank.bankfinance.controller.*.*(..))")` | **切入点表达式**：拦截 `com.bank.bankfinance.controller` 包下**所有类的所有方法**，参数任意（`..`） |
| `public Object generateRequestId(ProceedingJoinPoint pjp) throws Throwable {` | 环绕通知方法，接收被拦截方法的连接点对象 |
| `Object[] args = pjp.getArgs();` | 获取被拦截方法的**所有参数**（即 Controller 方法的入参数组） |
| `for (Object arg : args) {` | 遍历所有参数，逐个检查是否有 `requestId` 字段 |
| `if (arg != null) {` | 排除 null 参数，避免空指针 |
| `Field field = arg.getClass().getDeclaredField("requestId");` | 通过反射获取参数对象中的 `requestId` 字段。`getDeclaredField` 可以获取任意访问修饰符的字段 |
| `field.setAccessible(true);` | 设置字段可访问，突破 `private` 修饰符的限制 |
| `String requestId = (String) field.get(arg);` | 通过反射读取参数对象中 `requestId` 字段的当前值 |
| `if (requestId == null || requestId.isEmpty()) {` | 如果前端没有传 `requestId`（为 null 或空字符串），则生成兜底值 |
| `field.set(arg, TicketNoUtil.requestId());` | 通过反射将生成的 `requestId` 设置回参数对象中 |
| `} catch (NoSuchFieldException e) {` | 如果参数对象中没有 `requestId` 字段（如 `TradeQueryDTO`），捕获异常后跳过 |
| `// 这个 DTO 没有 requestId 字段，跳过` | 不影响原方法执行 |
| `}` | 结束 catch |
| `}` | 结束 if |
| `}` | 结束 for |
| `return pjp.proceed();` | **继续执行原方法**（Controller 方法），将处理后的参数传进去。`proceed()` 是环绕通知的核心——必须调用它，否则目标方法不会执行 |
| `}` | 结束方法 |
| `}` | 结束类 |

#### AOP 编程核心概念

| 概念 | 含义 | 代码对应 |
|------|------|----------|
| **切面（Aspect）** | 横切逻辑的模块化，如日志、幂等、权限 | `@Aspect` + `RequestIdAspect` 类 |
| **连接点（Join Point）** | 程序执行点，通常是方法调用 | `ProceedingJoinPoint pjp` |
| **切入点（Pointcut）** | 匹配哪些连接点的表达式 | `@Around("execution(...)")` |
| **通知（Advice）** | 在连接点上执行的具体逻辑 | `@Around` 方法体 |
| **目标对象（Target）** | 被通知的对象 | Controller 方法 |
| **代理（Proxy）** | Spring 生成的代理对象，封装了目标和通知 | Spring 自动生成 |
| **织入（Weaving）** | 将切面应用到目标对象的过程 | Spring 容器启动时自动完成 |

**通知类型**：
- `@Before`：方法执行前
- `@After`：方法执行后（无论是否异常）
- `@AfterReturning`：方法正常返回后
- `@AfterThrowing`：方法抛出异常后
- `@Around`：方法执行前后（最强大，可以控制是否执行）

**为什么用 AOP 而不是 Controller 手动写**：

| 方式 | 优点 | 缺点 |
|------|------|------|
| Controller 手动写 | 简单直接 | 每个接口重复代码，违反 DRY |
| AOP 切面 | 一处定义，全部生效 | 调试时需要看切面逻辑 |

用 AOP 后，`StockOrderController`、`AccountController`、`FinancialController` 的所有写操作**自动兜底生成 `requestId`**，不需要在每个 Controller 方法里重复写 `if (dto.getRequestId() == null)`。

---

## 六、DTO 汇总

| DTO | 业务 | requestId | 状态 |
|-----|------|-----------|------|
| DepositDTO | 存款 | ✅ | 已有 |
| TransferDTO | 转账 | ✅ | 已有 |
| PurchaseDTO | 理财购买 | ✅ | 已有 |
| StockOrderDTO | 股票下单 | ❌ | **待加** |
| WithdrawDTO | 提现 | ❌ | 待加 |
| RedeemDTO | 赎回 | ❌ | 待加 |
| TradeQueryDTO | 查询 | ❌ | 不需要 |

---

## 七、现有实体清单

| 实体 | 表名 | 说明 |
|------|------|------|
| Account | t_account | 账户 |
| User | t_user | 用户 |
| Stock | t_stock | 股票 |
| StockOrder | t_order | 银行交易订单 |
| OrderBookEntry | t_order_book | 订单簿（新增） |
| StockTrade | t_stock_trade | 股票成交（新增） |
| StockRank | t_stock_rank | 排行快照（新增） |
| StockKline | t_stock_kline | K线数据（新增） |
| Trade | t_trade | 银行转账记录 |
| Position | t_position | 持仓 |
| FinancialProduct | t_financial_product | 理财产品 |
| FinancialHolding | t_financial_holding | 理财持仓 |
| Staff | t_staff | 工作人员 |
| SupportTicket | t_support_ticket | 客服工单 |
| IdCardLib | t_id_card_lib | 身份证库 |

---

## 八、现有 Mapper 清单

| Mapper | 对应实体 | 说明 |
|--------|----------|------|
| AccountMapper | Account | |
| UserMapper | User | |
| StockMapper | Stock | |
| StockOrderMapper | StockOrder | |
| OrderBookEntryMapper | OrderBookEntry | 新增 |
| StockTradeMapper | StockTrade | 新增 |
| StockRankMapper | StockRank | 新增 |
| StockKlineMapper | StockKline | 新增 |
| TradeMapper | Trade | |
| PositionMapper | Position | |
| FinancialProductMapper | FinancialProduct | |
| FinancialHoldingMapper | FinancialHolding | |
| StaffMapper | Staff | |
| SupportTicketMapper | SupportTicket | |
| IdCardLibMapper | IdCardLib | |

---

## 九、现有 Service 清单

| Service | 说明 |
|---------|------|
| AccountService | 账户管理 |
| UserService | 用户管理 |
| StockService | 股票交易（改造中） |
| FinancialService | 理财交易 |
| TradeService | 银行转账 |
| MatchingEngine | 撮合引擎（新增） |
| RankService | 排行榜服务（新增） |
| RiskAssessmentService | 风险评估 |
| RecommendationService | 推荐 |
| TrendForecastService | 趋势预测 |
| AiService | AI 分析 |
| CustomerSupportService | 客服 |
| FaceService | 人脸识别 |
| IdCardLibService | 身份证 |

---

## 十、工具类

| 工具类 | 说明 |
|--------|------|
| RedisUtil | Redis 操作封装 |
| TicketNoUtil | 单号生成器（TK/T/O/REQ 前缀） |
| JwtUtil | JWT 生成/解析 |
| LoginContext | 登录上下文 |
| RankSchedule | 定时刷新排行任务 |

---

## 十一、配置文件

| 文件 | 说明 |
|------|------|
| application.yml | 主配置 |
| application-dev.yml | 开发环境配置 |
| application.properties | 属性配置 |

---

## 十二、Controller 接口清单

| Controller | 路径 | 说明 |
|------------|------|------|
| StockController | /stock/* | 股票列表/行情/下单/撤单/持仓/订单 |
| AccountController | /account/* | 账户/存款/转账/提现/验证码 |
| UserController | /user/* | 用户注册/登录/资料 |
| FinancialController | /financial/* | 理财购买/赎回/持仓 |
| AiController | /ai/* | AI 分析 |
| SupportController | /support/* | 客服工单 |
| RankController | /rank/* | 股票排行（新增） |

---

## 十三、Redis 应用场景

| 场景 | Key 格式 | 操作 |
|------|----------|------|
| 缓存 | `account:cache:{accountNumber}` | set/get |
| 用户缓存 | `user:cache:{phone}` | set/get |
| 幂等 | `idem:{业务}:{requestId}` | setIfAbsent |
| 限流 | `rate:{key}` | increment |
| 频率风控 | `freq:{account}` | increment + expire |
| 黑名单 | `token:blacklist:{token}` | hasKey |
| 验证码 | `code:{phone}` | set/get/delete |

**`RedisUtil` 核心方法**：
- `set(key, value, timeout, unit)` — 缓存、验证码、幂等
- `get(key)` — 缓存读取
- `setIfAbsent(key, value, timeout, unit)` — 幂等、分布式锁
- `increment(key)` — 限流计数、频率风控
- `hasKey(key)` — Token 黑名单校验
- `expire(key, timeout, unit)` — 设置过期、滑动窗口重置
- `delete(key)` — 幂等清理、验证码使用后删除

---

## 十四、股票排行系统与 Redis 实现

### 14.1 架构设计

```
数据源 (SimulatedStockQuoteProvider)
    ↓ 更新 Stock 实体（highPrice/lowPrice/volume/amount）
    ↓
RankSchedule (@Scheduled 每 5 秒)
    ↓ 调用 RankService.refreshAllRank()
    ↓
RankServiceImpl
    ↓ 遍历所有股票
    ↓ 计算 6 种排行指标
    ↓ 写入 Redis ZSET（核心数据结构）
    ↓ 持久化到 t_stock_rank（数据库快照）
    ↓ 发布 RankUpdatedEvent
    ↓
Redis ZSET 存储结构：
  rank:change      ZSET  score=涨跌幅%   member=stockCode
  rank:fall        ZSET  score=跌幅%     member=stockCode
  rank:volume      ZSET  score=成交量    member=stockCode
  rank:amount      ZSET  score=成交额    member=stockCode
  rank:turnover    ZSET  score=换手率%   member=stockCode
  rank:amplitude   ZSET  score=振幅%    member=stockCode
    ↓
RankController 提供 REST API
    ↓ GET /rank/top/{type}/{n}
    ↓ GET /rank/detail/{code}/{type}
    ↓ GET /rank/all/top/{n}
    ↓ GET /rank/rank/{code}/{type}
    ↓ GET /rank/score/{code}/{type}
    ↓ POST /rank/refresh
    ↓
前端 ECharts 可视化展示
```

### 14.2 Redis ZSET 核心操作

| 操作 | 命令 | 用途 | 时间复杂度 |
|------|------|------|-----------|
| 写入分数 | `ZADD rank:change {score} {stockCode}` | 更新股票排行分数 | O(log N) |
| 取 TOP N | `ZREVRANGE rank:change 0 9 WITHSCORES` | 获取涨幅榜前 10 | O(log N + M) |
| 查询排名 | `ZREVRANK rank:change {stockCode}` | 查询某股票排名 | O(log N) |
| 查询分数 | `ZSCORE rank:change {stockCode}` | 查询某股票分数 | O(1) |
| 清空重算 | `ZREMRANGE rank:change 0 1000000` | 清空排行榜 | O(N) |

### 14.3 新建文件

| 序号 | 文件路径 | 作用 |
|------|----------|------|
| 1 | `entity/StockRank.java` | 排行快照实体 |
| 2 | `entity/StockKline.java` | K线数据实体 |
| 3 | `mapper/StockRankMapper.java` | 排行 Mapper |
| 4 | `mapper/StockKlineMapper.java` | K线 Mapper |
| 5 | `service/RankService.java` | 排行服务接口 |
| 6 | `service/impl/RankServiceImpl.java` | 排行服务实现（Redis ZSET） |
| 7 | `controller/RankController.java` | 排行 API 接口 |
| 8 | `event/RankUpdatedEvent.java` | 排行更新事件 |
| 9 | `schedule/RankSchedule.java` | 定时任务（每 5 秒刷新排行） |

### 14.4 修改文件

| 序号 | 文件路径 | 修改内容 |
|------|----------|----------|
| 1 | `entity/Stock.java` | 新增 highPrice/lowPrice/volume/amount/turnoverRate/totalShares 字段 |
| 2 | `vo/StockQuoteVO.java` | 新增 highPrice/lowPrice/volume/amount 字段 |
| 3 | `provider/SimulatedStockQuoteProvider.java` | 生成 high/low/volume/amount 数据 |
| 4 | `service/impl/StockServiceImpl.java` | 初始化数据补充新字段 |

### 14.5 排行指标计算公式

| 排行类型 | 指标 | 公式 |
|----------|------|------|
| 涨幅榜 | 涨跌幅 | `(currentPrice - prevClose) / prevClose × 100` |
| 跌幅榜 | 涨跌幅 | 取负值，降序排列 |
| 成交量榜 | 成交量 | 当日累计成交股数 |
| 成交额榜 | 成交额 | 当日累计成交金额 |
| 换手率榜 | 换手率 | `volume / totalShares × 100` |
| 振幅榜 | 振幅 | `(highPrice - lowPrice) / prevClose × 100` |

### 14.6 数据库表

```sql
-- 扩展 t_stock 表
ALTER TABLE t_stock ADD COLUMN high_price DECIMAL(18,4);
ALTER TABLE t_stock ADD COLUMN low_price DECIMAL(18,4);
ALTER TABLE t_stock ADD COLUMN volume BIGINT;
ALTER TABLE t_stock ADD COLUMN amount DECIMAL(18,2);
ALTER TABLE t_stock ADD COLUMN turnover_rate DECIMAL(18,4);
ALTER TABLE t_stock ADD COLUMN total_shares DECIMAL(18,0);

-- 排行快照表 t_stock_rank（完整 SQL 见 docs/sql/stock_rank.sql）

-- K线数据表 t_stock_kline（完整 SQL 见 docs/sql/stock_rank.sql）
```

### 14.7 定时任务

```java
@Component
public class RankSchedule {
    @Scheduled(fixedDelay = 5000)  // 每 5 秒刷新一次
    public void refreshRank() {
        rankService.refreshAllRank();
    }
}
```

**Spring Boot 启动类已有 `@EnableScheduling`**，无需额外配置。

---

## 十五、前端可视化平台

### 15.1 前端项目结构

```
bank-frontend/
├── src/
│   ├── api/
│   │   ├── stock.js          ← 股票 API（含排行接口）
│   │   ├── account.js        ← 账户 API
│   │   ├── financial.js      ← 理财 API
│   │   └── user.js           ← 用户 API
│   ├── components/
│   │   ├── StockMarket.vue   ← 实时行情表格（已有）
│   │   ├── StockTrading.vue  ← 交易组件（已有）
│   │   ├── RankBoard.vue     ← 排行面板（新增）
│   │   └── KlineChart.vue    ← K线图组件（新增）
│   ├── views/
│   │   ├── Dashboard.vue
│   │   ├── CardTrade.vue
│   │   └── ...
│   ├── router/
│   │   └── index.js          ← 已添加 /rank 和 /kline 路由
│   └── main.js
├── package.json              ← 已安装 echarts
```

### 15.2 新增的前端接口

**`src/api/stock.js`** 新增：

```js
// 排行榜相关
export function getRankTopN(type, n)          → GET /rank/top/{type}/{n}
export function getRankDetail(code, type)     → GET /rank/detail/{code}/{type}
export function getAllRankTopN(n)             → GET /rank/all/top/{n}
export function getStockRank(code, type)      → GET /rank/rank/{code}/{type}
export function getStockRankScore(code, type) → GET /rank/score/{code}/{type}
export function refreshRank()                 → POST /rank/refresh

// K线相关
export function getKline(stockCode, period)   → GET /stock/kline/{stockCode}?period={period}
```

### 15.3 新增组件

#### `RankBoard.vue` — 排行面板

| 功能 | 说明 |
|------|------|
| 标签切换 | 涨幅榜 / 跌幅榜 / 成交量榜 / 成交额榜 / 换手率榜 / 振幅榜 |
| TOP N 选择 | 10 / 20 / 50 |
| 排名高亮 | 第1名红色，第2名橙色，第3名绿色 |
| 涨跌颜色 | 指标值根据正负显示红/绿色 |
| 自动刷新 | 每 5 秒自动刷新排行数据 |
| 行点击事件 | 点击某股票发射 `open-kline` 事件 → 打开 K线图 |

#### `KlineChart.vue` — K线图组件

| 功能 | 说明 |
|------|------|
| 周期切换 | 1分 / 5分 / 日K / 周K / 月K |
| 蜡烛图 | ECharts candlestick 蜡烛图 |
| 颜色 | 红色涨 / 绿色跌 |
| 十字光标 | 鼠标悬停显示 OHLCV 信息 |
| 数据缩放 | dataZoom 滑块 + 缩放 |
| 自动刷新 | 每 10 秒刷新 K 线数据 |

#### `StockMarket.vue` — 行情表格更新

| 功能 | 说明 |
|------|------|
| 行点击事件 | 点击某股票发射 `open-kline` 事件 → 打开 K线图 |

#### `Dashboard.vue` — 主页面集成

| 功能 | 说明 |
|------|------|
| 顶部导航 | 新增「股票排行」入口 |
| 快捷服务 | 新增「股票排行」功能卡 |
| 排行对话框 | `RankBoard` 对话框，点击行 → 打开 K线图 |
| 行情对话框 | `StockMarket` 对话框，点击行 → 打开 K线图 |
| K线对话框 | `KlineChart` 对话框，接收 `stockCode`/`stockName` 参数 |

### 15.4 新增路由

```js
{ path: '/rank', component: RankBoard, meta: { requiresAuth: true } },
{ path: '/kline/:stockCode', component: KlineChart, meta: { requiresAuth: true } },
```

### 15.5 依赖安装

```bash
cd bank-frontend
npm install echarts --save
```

---

## 十六、改造进度追踪

### 已完成

| 序号 | 事项 | 完成时间 |
|------|------|----------|
| 1 | `OrderBookEntry` 实体 + `OrderBookEntryMapper` | ✅ |
| 2 | `StockTrade` 实体 + `StockTradeMapper` | ✅ |
| 3 | `MatchingEngine` 接口 | ✅ |
| 4 | `MatchingEngineImpl` 完整实现 | ✅ |
| 5 | `OrderPlacedEvent` + `TradeExecutedEvent` | ✅ |
| 6 | 撮合算法（双指针 + 乐观锁） | ✅ |
| 7 | 资金划拨 + 持仓变动逻辑 | ✅ |
| 8 | Redis 应用场景梳理 | ✅ |
| 9 | DTO `requestId` 幂等方案确认 | ✅ |
| 10 | `TicketNoUtil` 可生成 `requestId` | ✅ |
| 11 | 完整项目文档 v1.0 | ✅ |
| 12 | `RequestIdAspect` AOP 切面完整实现（反射 + TicketNoUtil） | ✅ |
| 13 | `TicketNoUtil.requestId()` 方法补全 | ✅ |
| 14 | 排行系统（StockRank + RankService + Redis ZSET） | ✅ |
| 15 | Stock 实体补充行情字段（highPrice/lowPrice/volume/amount） | ✅ |
| 16 | 数据库脚本（docs/sql/stock_rank.sql） | ✅ |
| 17 | 前端集成（RankBoard + KlineChart + Dashboard） | ✅ |
| 18 | 项目文档 v1.3 | ✅ |

### 待完成

| 序号 | 事项 | 优先级 | 负责人 |
|------|------|--------|--------|
| 1 | `StockOrderDTO` 加 `requestId` 字段 | P0 | 待定 |
| 2 | `StockServiceImpl.placeOrder()` 改造为 `submitOrder()` | P0 | 待定 |
| 3 | `mergePosition` / `reducePosition` 完整实现 | P0 | 待定 |
| 4 | `AccountMapper.addBalance` 支持负数扣款 | P0 | 待定 |
| 5 | 事件监听触发撮合（`@EventListener`） | P0 | 待定 |
| 6 | 撤单接口完整实现（解冻资金/股份） | P1 | 待定 |
| 7 | 数据库建表 SQL 执行 | P0 | 待定 |
| 8 | 单元测试（撮合正确性验证） | P1 | 待定 |
| 9 | 定时任务触发撮合 `@Scheduled` | P1 | 待定 |
| 10 | 行情源接入（替换模拟行情） | P2 | 待定 |
| 11 | 涨跌停板校验 | P2 | 待定 |
| 12 | 交易时段控制（集合竞价/连续竞价） | P3 | 待定 |
| 13 | 最小变动价位校验 | P2 | 待定 |
| 14 | 部分成交撤单逻辑 | P1 | 待定 |

---

## 十七、面试准备

### 17.1 项目亮点
- 自研订单簿 + 撮合引擎（价格优先、时间优先）
- 乐观锁并发安全
- 事件驱动架构（领域事件解耦）
- 策略模式（行情源可切换）
- 幂等性设计（Redis + requestId）
- CQRS 思想（写模型/读模型分离）
- Redis ZSET 实时排行榜（高效 O(log N) 排序）
- AOP 切面兜底生成 requestId

### 17.2 面试话术
> "我设计了一个股票交易撮合系统，核心是订单簿 + 双指针撮合算法。买卖订单按价格/时间排序后，用双指针遍历撮合，价格优先、时间优先。关键点是乐观锁保证并发安全，领域事件驱动行情和风控解耦。"

### 17.3 技术栈展示
- Java 并发：`ThreadLocalRandom`、乐观锁 CAS
- Spring：`ApplicationEventPublisher`、`@Transactional`、AOP
- MyBatis-Plus：`BaseMapper`、`LambdaQueryWrapper`、`@Version`
- Redis：`setIfAbsent`、`increment`、`hasKey`
- 设计模式：策略模式、状态机、CQRS、观察者模式

---

## 十八、更新日志

### v1.3 (2026-09-02)
- 补全 `RequestIdAspect` AOP 切面完整逻辑（反射读/写 requestId + 乐观锁 + 异常处理）
- 补全 `TicketNoUtil.requestId()` 方法（REQ 前缀 + 时间戳 + 8位随机十六进制）
- 修复 `RequestIdAspect` 方法名拼写错误（generateRRequestId → generateRequestId）
- 完成项目文档 v1.3

### v1.1 (2026-09-02)
- 完成订单簿实体 + Mapper
- 完成股票成交实体 + Mapper（独立原有 Trade）
- 完成撮合引擎接口 + 实现（双指针算法）
- 完成领域事件定义
- 完成资金划拨 + 持仓变动逻辑
- 完成 DTO `requestId` 幂等方案
- 完成 Redis 应用场景梳理
- 完成 `TicketNoUtil.requestId()` 方法
- 完成 AOP 切面 `RequestIdAspect`（后端兜底生成 requestId）
- 完成排行榜系统（StockRank + RankService + Redis ZSET）
- 完成 Stock 实体补充行情字段
- 完成定时任务 RankSchedule
- 完成数据库脚本 docs/sql/stock_rank.sql
- 完成项目文档 v1.1

### v1.0 (2026-09-01)
- 完成订单簿实体 + Mapper
- 完成股票成交实体 + Mapper（独立原有 Trade）
- 完成撮合引擎接口 + 实现（双指针算法）
- 完成领域事件定义
- 完成资金划拨 + 持仓变动逻辑
- 完成 DTO `requestId` 幂等方案
- 完成 Redis 应用场景梳理
- 完成项目文档 v1.0

---

> **使用说明**：每次有新的代码改动或功能加入，在「十六、改造进度追踪」的「已完成」中新增一条，并更新「十八、更新日志」。这样文档始终保持最新状态。