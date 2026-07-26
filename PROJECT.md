# 智慧银行 (Smart Bank) — 项目文档

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端 | Vue 3 + Vite | Vue 3 |
| UI 框架 | Element Plus | — |
| 路由 | Vue Router 4 | — |
| HTTP | Axios | — |
| 后端 | Spring Boot | 3.5.16 |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | SQL Server | — |
| 缓存 | Redis | — |
| 安全 | Spring Security + JWT | — |
| 文档 | Knife4j (Swagger) | — |

---

## 项目结构

```
bank-frontend/                # Vue 3 前端
├── src/
│   ├── api/                  # API 接口封装
│   │   ├── account.js        #   账户相关（登录/注册/改密/冻结等）
│   │   └── financial.js      #   理财产品相关
│   ├── components/           # 可复用组件
│   │   ├── PasswordDialog.vue  交易密码输入弹窗
│   │   ├── Deposit.vue         存款
│   │   ├── Withdraw.vue        取款
│   │   ├── Transfer.vue        转账
│   │   ├── TradeRecords.vue    交易记录
│   │   ├── ProductList.vue     理财产品列表 + 购买
│   │   ├── MyHoldings.vue      我的持仓 + 赎回
│   │   ├── FreezeAccount.vue   账户冻结/解冻（管理员）
│   │   ├── ChangePassword.vue  修改密码
│   │   ├── ProfileSettings.vue 个人信息设置
│   │   └── ChatBot.vue         智能客服（浮动聊天）
│   ├── views/
│   │   ├── Login.vue           登录
│   │   ├── Register.vue        注册
│   │   ├── ForgotPassword.vue  忘记密码
│   │   └── Dashboard.vue       主面板
│   ├── router/index.js
│   └── utils/request.js        Axios 实例 + 拦截器

bank-finance/                 # Spring Boot 后端
├── src/main/java/com/bank/bankfinance/
│   ├── common/
│   │   ├── constant/ResponseCode.java   统一错误码
│   │   ├── exception/BusinessException.java  业务异常
│   │   ├── filter/JwtAuthenticationFilter.java  JWT 鉴权过滤器
│   │   ├── result/Result.java           统一返回体 {code, msg, data}
│   │   └── util/LoginContext.java       线程级用户上下文
│   ├── config/
│   │   └── SecurityConfig.java          Spring Security 配置
│   ├── controller/          # 控制器层
│   │   ├── AccountController.java       账户相关接口
│   │   ├── FinancialController.java     理财产品接口
│   │   └── AiController.java            智能客服接口
│   ├── dto/                 # 请求体 DTO
│   │   ├── LoginDTO.java
│   │   ├── RegisterDTO.java
│   │   ├── LoginResponse.java
│   │   ├── SendCodeDTO.java / ResetPasswordDTO.java  忘记密码
│   │   ├── UpdatePsdDTO.java            修改密码
│   │   ├── UpdateProfileDTO.java        个人信息
│   │   ├── DepositDTO.java / WithdrawDTO.java / TransferDTO.java
│   │   ├── PurchaseDTO.java / RedeemDTO.java        理财购买/赎回
│   │   └── TradeQueryDTO.java
│   ├── entity/             # 实体类
│   │   ├── Account.java                t_account
│   │   ├── Trade.java                  t_trade
│   │   ├── FinancialProduct.java       t_financial_product
│   │   └── FinancialHolding.java       t_financial_holding
│   ├── mapper/             # MyBatis-Plus Mapper
│   ├── service/            # Service 接口 + 实现
│   │   ├── AccountService.java / impl/AccountServiceImpl.java
│   │   ├── TradeService.java / impl/TradeServiceImpl.java
│   │   ├── FinancialService.java / impl/FinancialServiceImpl.java
│   │   └── AiService.java / impl/AiServiceImpl.java
│   └── vo/                 # 返回体 VO
│       └── AccountVO.java
```

---

## 统一约定

### 后端返回格式 `Result<T>`
```json
{ "code": "000000", "msg": "成功", "data": ... }
```
错误时 `code` 为非零字符串，前端 Axios 拦截器自动 `ElMessage.error(msg)`。

### 鉴权方式
- 前端在 `localStorage` 存 `token` 和 `userInfo`（JSON 序列化的 AccountVO）
- 每次请求 Axios 拦截器自动在 Header 加 `Authorization: Bearer <token>`
- 后端 `JwtAuthenticationFilter` 解析 token 后将账号设入 `LoginContext`
- 业务层通过 `LoginContext.getAccount()` 获取当前登录用户

### 角色（role）
| role 值 | 身份 |
|---------|------|
| `"1"`   | 管理员（可见管理菜单、可冻结/解冻） |
| 其他/未设置 | 普通用户 |

---

## 新增功能详解

### 1️⃣ 忘记密码（Forgot Password）

**流程：** 登录页 → 「忘记密码」→ 输入账号和新密码 → 发送验证码 → 校验通过后重置。

| 组件 | 说明 |
|------|------|
| `ForgotPassword.vue` | 两步表单：①账号+新密码 ②验证码 |
| `SendCodeDTO` | 字段：`accountNumber`, `userPhoneNumber` |
| `ResetPasswordDTO` | 字段：`accountNumber`, `code`, `newPassword`, `confirmPassword` |

**后端调用链：**
```
POST /account/sendCode
  → AccountServiceImpl.sendVerificationCode()
  → 校验手机号匹配 → 生成6位验证码 → 存入 Redis（key: verify:code:{account}，5分钟过期）

POST /account/resetPassword
  → AccountServiceImpl.resetPassword()
  → 校验验证码 → 更新密码 → 删除 Redis 中的验证码
```

> 验证码实际未对接短信网关，存 Redis 后默认值为 `"123456"`（见 `sendVerificationCode` 方法）。

---

### 2️⃣ 理财产品（Financial Products）

| 组件 | 说明 |
|------|------|
| `ProductList.vue` | 产品卡片网格 → 点击「购买」→ 购买弹窗 → 点击「确认购买」→ PasswordDialog |
| `MyHoldings.vue` | 持仓汇总（总投资/预期收益/总价值）+ 卡片列表 + 进度条 → 点击「提前赎回」→ PasswordDialog |
| `PasswordDialog.vue` | 6 位数字密码输入弹窗，teleport 到 body，emit `confirm(pin)` |

**数据库表（新增）：**

| 表 | 说明 |
|----|------|
| `t_financial_product` | 理财产品定义（名称/年化收益率/期限/起购金额/剩余份额/状态） |
| `t_financial_holding` | 用户持仓记录（关联 product_id/投资金额/状态/购买日期/赎回日期） |

**状态说明：**
- `t_financial_product.status`：`1`=在售（可购买），`0`=下架
- `t_financial_holding.status`：`0`=持有中，`1`=已赎回

**后端接口：**

| 端点 | 说明 |
|------|------|
| `GET /financial/products` | 查询所有在售产品 |
| `GET /financial/product/{id}` | 产品详情 |
| `POST /financial/purchase` | **购买** — 校验：产品存在/在售/起购金额/库存/账号冻结/余额/密码 |
| `GET /financial/holdings` | 查询当前用户持仓 |
| `POST /financial/redeem` | **赎回** — 校验：持仓归属/持有状态/账号冻结/密码 |

> 购买和赎回都会创建交易记录到 `t_trade` 表。

**核心业务逻辑 (FinancialServiceImpl.purchase)：**
```
1. 获取当前用户（LoginContext.getAccount()）
2. 校验产品存在、status=1、金额≥起购金额、剩余份额
3. 校验账户未冻结（accountStatus != 0）
4. 校验余额充足
5. 校验密码（passwordEncoder.matches）
6. 扣减余额、更新产品剩余份额、创建持仓记录
7. 记录交易流水（t_trade）
```

---

### 3️⃣ 密码安全统一管理

所有涉及资金的业务都需要输入 6 位交易密码：

| 操作 | 密码校验位置 |
|------|------------|
| 存款 | `AccountServiceImpl.deposit()` |
| 取款 | `AccountServiceImpl.withdraw()` |
| 转账 | `AccountServiceImpl.transfer()` |
| 购买理财 | `FinancialServiceImpl.purchase()` |
| 赎回理财 | `FinancialServiceImpl.redeem()` |
| 修改密码 | `AccountServiceImpl.updatePsd()`（需验证旧密码） |

**PasswordDialog 使用模式：**
```html
<!-- 父组件中 -->
<PasswordDialog
  :visible="passwordVisible"
  title="请输入交易密码"
  @update:visible="passwordVisible = $event"
  @confirm="handleConfirm"
  @cancel="passwordVisible = false"
/>
```

```js
const handleConfirm = async (pwd) => {
  passwordVisible.value = false
  // 调用 API，带上 pwd
}
```

---

### 4️⃣ 管理员功能

| 组件 | 说明 |
|------|------|
| `FreezeAccount.vue` | 输入目标账号 → 冻结/解冻 |
| 侧边栏 | 管理员可见「管理服务」→「账户管理」 |

**后端：**
- `checkAdmin(Account)` 校验当前用户 role 为 `"1"`
- `POST /account/admin/freezeAccount` — 将目标账号 status 置为 0
- `POST /account/admin/unfreezeAccount` — 将目标账号 status 置为 1

> 冻结的账号（status=0）**可以登录**，但**不能进行任何业务操作**（存款/取款/转账/购买/赎回）。各业务方法入口处有 `if (account.getAccountStatus() == 0) throw` 校验。

---

### 5️⃣ 个人信息 + 修改密码

| 组件 | 说明 |
|------|------|
| `ProfileSettings.vue` | 展示账号、类型（只读），可编辑户名、手机号 |
| `ChangePassword.vue` | 输入原密码、新密码、确认新密码 |

两者都从右上角用户下拉菜单进入。

**后端新增：**
- `POST /account/updateProfile` — 更新 `accountHolderName` / `userPhoneNumber`
- `UpdatePsdDTO` 新增 `oldPassword` 字段，`updatePsd()` 先用 `passwordEncoder.matches()` 校验旧密码

---

### 6️⃣ 智能客服

| 组件 | 说明 |
|------|------|
| `ChatBot.vue` | 固定在 Dashboard 右下角，浮动按钮展开聊天面板 |
| `AiController` | `POST /ai/chat` |
| `AiServiceImpl` | 内置关键词匹配回答（无需外部 API） |

**可回答的问题：** 开户、登录、存款、取款、转账、余额、交易记录、理财、修改密码、冻结、利率、赎回等。

---

## 关键注意事项（Agent 使用场景）

### 当你需要新增功能时的查询路线

1. **后端添加接口：** DTO → Service 接口 → ServiceImpl → Controller
2. **前端添加页面/组件：** 创建 `.vue` → `api/` 加接口函数 → `Dashboard.vue` 引入 + 加 dialog + menu handler（如果是弹窗）或 router（如果是独立页面）
3. **添加数据库表：** 建 Entity → Mapper → 在 Service 中使用

### 记住这些约定

```
- 所有请求统一走 Result<T> 返回
- 业务异常抛 BusinessException(响应码枚举)
- 当前用户用 LoginContext.getAccount() 获取
- 密码统一用 BCryptPasswordEncoder
- 前端 API 在 src/api/ 下，通过 request.js 的 Axios 实例调用
- 组件通信：父 → 子 props，子 → 父 emit
- 全局 Element Plus 图标无需 import（已在 main.js 注册）
- 所有交易类操作必须用 PasswordDialog 输入密码后调用接口
- Dashboard 的菜单用 @select + handleMenuSelect 控制弹窗
- 用户下拉菜单扩展在 Dashboard.vue 的 <el-dropdown-menu> 加 item
- 新功能如果是弹窗 → 加 showXxx ref + el-dialog + handler
- 新功能如果是独立页面 → router/index.js 加路由
```

### 当前已知的限制

| 问题 | 说明 |
|------|------|
| 验证码固定为 `"123456"` | 未对接短信网关，Redis 中默认 hardcode |
| `JwtAuthenticationFilter` 不返回 401 | 无效/过期 token 仍放行，仅打日志 |
| `SecurityConfig` 全部 `permitAll()` | 所有接口公开，鉴权全在业务层手写 |
| 无法在此环境编译 | 需 Java 17，当前环境仅 Java 8 |
| 后端 `application.yml` 含数据库密码 | 仅用于开发，生产需抽到环境变量 |
| ElMessage 弹窗层叠问题 | 多弹窗时 z-index 需留意 |
| 组件 emit('close') 约定 | Deposit/Withdraw/Transfer 等子组件在完成操作后需 emit('close') 通知父 dialog 关闭 |
