# 智慧银行（Smart Bank）开发日志

> 记录项目关键更新，按时间倒序排列。
> 模块路径：`bank-finance`（后端）/ `bank-frontend`（前端）

---

## 2026-08-06 · 接入人脸实名认证（本地 ArcFace 微服务）

### 方案
新增 `face_server/`（Python + InsightFace/ArcFace），与后端解耦的独立微服务，监听 `8001`；人脸特征以 JSON+Base64 传递，特征统一为 512 维归一化向量；判定阈值 `FACE_THRESHOLD`（默认 0.45）。模型经 ModelScope 下载 `fireicewolf/buffalo_l`（GitHub 直连被墙）。

### 后端
| 文件 | 改动 |
|------|------|
| `config/FaceProperties.java` + `application.yml` | `face.base-url` / `face.timeout-ms` 配置，默认 `http://localhost:8001` |
| `config/FaceColumnMigrationRunner.java` | `@Order(1)` 启动时幂等建列：`t_user.face_feature` NVARCHAR(MAX) NULL、`face_bound` INT DEFAULT 0（须先于 `PasswordMigrationRunner` 执行，否则其 selectList 报"列名无效"） |
| `service/FaceService` + `impl` | 用 `RestClient` 调 `/api/face/feature`；服务不可达→`910004`，图片无脸→`910001` |
| `service/impl/UserServiceImpl` | `bindFace(userId, image)`（未登录/重复绑定/无脸校验，特征逗号拼接 `%.6f` 入库，`face_bound=1`）；`verifyFace(userId, image)`（未绑定→`910002`，相似度低于阈值→`910003`） |
| `controller/UserController` | `POST /user/face/bind`、`POST /user/face/verify` |
| `common/constant/ResponseCode.java` | 新增 `910001` 无脸 / `910002` 未绑定 / `910003` 不匹配 / `910004` 服务异常 |
| `dto/user/` | `FaceBindDTO`、`FaceVerifyDTO`（`@NotBlank image`） |
| `vo/UserVO.java` | 新增 `faceBound` 字段 |

### 前端
- `api/user.js`：`bindFace` / `verifyFace`
- `views/UserDashboard.vue`：新增"人脸认证"入口卡片 + 弹窗（图片上传/预览、绑定/核身、核身结果相似度展示）

### 验证
- `face_server/test_client.py` 功能用例 6/6 通过；同图相似度 1.0，水平翻转 0.964（>0.45 判定通过）
- 后端 JDK17 `mvn compile` ✅；前端 `npm run build` ✅
- 真实照片回归模式：`python test_client.py --realdir imgs/me imgs/others`（需自备照片）

### 其他本次同步
- DTO 按 `user` / `trade` / `support` 分包（21 个，含 4 个通配 import 文件更新）
- 清理死代码：`LoginInterceptor`、`PageResult`、`TradeConstant`、`AccountConstant`、`TradeVO`（空接口 `SupportTicketService`）
- 版本：后端 `0.1.0-SNAPSHOT`、前端 `0.1.0`
- 安全：`DEV_LOG.md` 历史明文 API Key 已脱敏

---

## 2026-08-03 · 修复用户提交工单被误判为"只能使用本人名下的银行卡"

**问题**：用户登录（`U_` 身份）后提交工单，后端 `submitTicket` 归属校验走到 else 分支（`userId == null`），抛 `400004` "只能使用本人名下的银行卡222"。

**根因**：`bank-frontend/src/utils/request.js` 的 token 分流逻辑过宽——拦截器按 `url.startsWith('/support')` 判断，导致**用户提交工单**（`/support/ticket`、`/support/tickets`）也套用了客服专用的 `staff_token`（`S_` 身份）。后端 filter 对 `S_` 前缀只设 `staffId` 不设 `userId`，于是工单提交时 `LoginContext.getUserId()` 为 null，归属校验误走"未登录/非本人"分支。

**方案**：token 分流收窄为仅客服后台接口 `/support/admin/*` 使用 `staff_token`，其余（含用户工单提交、用户登录注册）统一用用户 `token`；401 跳转逻辑同步收窄。

### 前端
| 文件 | 改动 |
|------|------|
| `utils/request.js` | 请求拦截器：`isSupportAdmin = url.startsWith('/support/admin/')`，仅此处取 `staff_token`，其余取 `token`；响应拦截器 401 跳转同规则 |

### 验证
- 前端 `npm run build` ✅
- 用户 `U_` 身份提交工单 → 后端收到用户 token → `userId` 有值 → 走"只能使用本人名下的银行卡111"正常校验分支

### 附：诊断日志
`common/filter/JwtAuthenticationFilter.java` 已加 `[JWT]` 诊断日志（打印 URI / 解析出的 subject / 身份类型 / id，解析失败打印原因），便于定位此类身份问题。

---

**问题**：用户-卡分离后登录 token 为 `U_<userId>`，`LoginInterceptor` 只设 `userId` 不设 `account`；理财 `purchase`/`listHoldings`/`redeem` 读 `LoginContext.getAccount()` 恒为 null → 抛 `NO_LOGIN`，前端购买/持仓/赎回全部失败。

**方案**：理财操作显式传 `accountNumber`（前端当前选中卡），后端归属校验（与 `getAccountBalance`/`getTrades` 同款）。

### 后端
| 文件 | 改动 |
|------|------|
| `dto/PurchaseDTO.java` | 新增 `@NotBlank accountNumber` |
| `dto/RedeemDTO.java` | 新增 `@NotBlank accountNumber` |
| `service/FinancialService.java` | `listHoldings(String accountNumber)` |
| `service/impl/FinancialServiceImpl.java` | 新增 `validateAccountAccess(accountNumber)`：有 account token 必须相等；有 userId 校验 `account.userId` 归属；否则 `NO_LOGIN`；`purchase`/`redeem` 改用 DTO 卡号，`listHoldings` 显式传卡号 |
| `controller/FinancialController.java` | `/financial/holdings` 改 `@RequestParam String accountNumber` |

### 前端
| 文件 | 改动 |
|------|------|
| `api/financial.js` | `listHoldings(accountNumber)`；purchase/redeem 请求体带 `accountNumber` |
| `components/ProductList.vue` | 新增 `accountNumber` prop，购买时传入 |
| `components/MyHoldings.vue` | 新增 `accountNumber` prop，列表/赎回时传入 |
| `views/CardTrade.vue` | `:account-number="accountNumber"`（路由当前卡） |
| `views/Dashboard.vue` | `:account-number="userInfo?.accountNumber"` |

### 验证
- 前端 `npm run build` ✅
- 后端需在 IDEA（JDK 17）编译运行验证

---

## 2026-07-31 · 理财到期自动结算 + 提前赎回无收益

**目标**：理财到期后系统自动把本息打回账户；提前赎回只退本金、不计收益。

### 后端
| 文件 | 改动 |
|------|------|
| `BankFinanceApplication.java` | 新增 `@EnableScheduling` 开启定时任务 |
| `service/impl/FinancialServiceImpl.java` | `redeem` 改为：到期 → 本金+收益；未到期（提前赎回）→ 仅退本金；新增 `@Scheduled(cron="0 0 1 * * ?")` 每天凌晨 1 点扫描到期持仓自动结算；新增 `isMatured`/`claimHolding`/`settleHolding` 辅助方法（`claimHolding` 用 `status=0` 原子更新防止重复结算） |

### 前端
| 文件 | 改动 |
|------|------|
| `components/MyHoldings.vue` | 提示文案：`到期自动赎回本息 · 提前赎回仅退本金`；赎回成功提示改为 `赎回成功，资金已到账` |

### 结算规则
- 到期判断：`purchaseDate + termDays <= 当前时间`
- 到期：到账 = 本金 + 单利收益（本息）
- 提前赎回：到账 = 本金（无收益）
- 定时任务跳过已冻结账户（`accountStatus==0`），解冻后次日继续结算

### 验证
- 后端需在 IDEA（JDK 17）编译运行验证

---

## 2026-07-31 · 移除账户管理功能 & 去除 Account.role 属性

**目标**：系统不再区分管理员/普通用户，取消管理员账户管理能力。

### 后端
| 文件 | 改动 |
|------|------|
| `entity/Account.java` | 删除 `role` 字段 |
| `vo/AccountVO.java` | 删除 `role` 字段 |
| `service/AccountService.java` | 移除 `freezeAccount` / `unfreezeAccount` 声明 |
| `service/impl/AccountServiceImpl.java` | 删除 `checkAdmin`、`freezeAccount`、`unfreezeAccount`、`setRole`；`getAccountBalance` 非本人查询改为抛 `NO_LOGIN` |
| `service/impl/UserServiceImpl.java` | 删除 `setRole` |
| `controller/AccountController.java` | 删除 `/admin/freezeAccount`、`/admin/unfreezeAccount`；`getTrades` 移除 admin 分支，登录用户只能查自己的流水 |

### 前端
| 文件 | 改动 |
|------|------|
| `components/FreezeAccount.vue` | **整个文件删除**（账户管理组件） |
| `views/Dashboard.vue` | 移除"账户管理"菜单、`showAdmin` 弹窗、`isAdmin` 逻辑 |
| `views/CardTrade.vue` | 同上 |
| `components/BalanceQuery.vue` | 移除 admin 搜索框与逻辑，只查询本人余额 |
| `components/TradeRecords.vue` | 移除 admin 搜索框，只查当前卡流水 |
| `api/account.js` | 删除 `freezeAccount` / `unfreezeAccount` |

### 验证
- 前端 `npm run build` ✅ 通过
- 后端需在 IDEA（JDK 17）编译运行验证

---

## 2026-07-31 · AI 智能客服：加入系统语境 & 修复 SSE 解析 Bug

### 加入系统语境（后端）
`service/impl/AiServiceImpl.java`：
- 新增 `SYSTEM_PROMPT` 常量：角色设定（智慧银行客服）+ 系统功能清单（开户/存款/取款/转账/理财/冻结/改密等）+ 回答要求（简洁中文、200 字内）
- 流式请求 `messages` 改为 `[system, user]`

### 修复 SSE 前端解析 Bug（前端）
`components/ChatBot.vue`：
- **问题**：Spring `SseEmitter` 实际输出 `data:内容`（冒号后**无空格**），前端却匹配 `'data: '`（带空格），导致 AI 回复永不显示
- **验证**：反编译 Spring `SseEventBuilderImpl` 字节码确认格式
- **修复**：改为 `startsWith('data:')` + `substring(5)`，并清理无效的 `event:` 行判断

---

## 2026-07-31 · AI 智能客服：SSE 流式回答（简化版）

`service/impl/AiServiceImpl.java` 重构，主方法从 ~80 行缩到 ~25 行：
- 抽出 `openChatStream`：连接创建 + 请求体 + 状态校验
- 抽出 `extractContent`：单行 SSE 解析
- 抽出 `finish`：统一发送 `message` + `done` + `complete`
- 抽出 `isBlank`：统一 key 判空

行为不变：无 key → mock 兜底；有 key → DeepSeek 真实流式。

---

## 2026-07-31 · 环境变量配置确认

- IDEA 运行配置（`BankFinanceApplication`）中已配置 `API_KEY=sk-xxxx`（敏感信息脱敏，请勿提交真实 key）
- `application.yml` 的 `ai.api-key: ${API_KEY:}` 启动时读取环境变量，无 key 则空串 → 走 mock
- `AiProperties.apiKey` 默认值 `""` 仅为兜底，实际由 yml 覆盖

---

## 2026-07-30 · 前端 AI 对话（ChatBot）与后端 SSE 对接

### 后端
| 文件 | 改动 |
|------|------|
| `controller/AiController.java` | 新增 `POST /ai/chat/stream`（SSE，SseEmitter，120s 超时） |
| `service/AiService.java` | 新增 `chatStream(String, SseEmitter)` |
| `service/impl/AiServiceImpl.java` | HttpURLConnection 调 DeepSeek `/chat/completions`（stream=true），逐 chunk 发 SSE（`event: message` / `event: done`），无 key 时 mock 兜底 |
| `config/AiProperties.java` | 新增，绑定 `ai.*` 配置 |
| `resources/application.yml` | 新增 `ai.api-key / model / base-url` |

### 前端
- `components/ChatBot.vue`：fetch `ReadableStream` 流式渲染回复

---

## 2026-07-29 · 用户与银行卡分离体系（一人多卡）

### 核心改动
| 模块 | 说明 |
|------|------|
| 后端 | token 前缀 `U_` → `LoginContext.setUserId`；普通 token → `getAccount` |
| 后端 `GET /user/info` | 新增 `UserVO(id, userPhone, userIdCard)` |
| 后端 `getAccountBalance` / `/account/trades` | 支持 U_ token + 显式 `accountNumber` 参数，校验归属（`getUserId` 匹配） |
| 前端 `UserDashboard.vue` | `Promise.all(getUserInfo(), getUserAccounts())` 并行加载；新增「注册新卡」dialog；row-click 跳转交易界面 |
| 前端 `CardTrade.vue` | 重写为完整卡片交易界面（侧边栏、余额卡、快捷操作、全部弹窗、「切换银行卡」） |
| 前端 `Deposit/Withdraw/Transfer` | 新增 `accountNumber` prop 预填表单 |
| 前端 `TradeRecords` | 新增 `accountNumber` prop，非 admin 也能按卡查 |
| 路由 | 新增 `/trade/:accountNumber` |

### 隐藏卡号登录入口
- `Login.vue`：删除 `goUserLogin`
- `Register.vue`：删除 footer 链接
- `router/index.js`：根路径 `/` 重定向 `/user/login`

### 密码修复
- `t_user` 19912349999 明文密码 → BCrypt 哈希
- `PasswordMigrationRunner` 扩展为同时迁移 Account / User 明文密码

### 身份证脱敏
- 数据库 13 行 `t_account` 补齐 `user_id_card`
- 前端 `maskIdCard`（前6位 + `********` + 后4位）用于 UserDashboard / Dashboard / BalanceQuery

### 学习文档
- 生成 `D:\javaproject\vueLearn\VueRouter入门与实践.md`（8 章节）

---

## 2026-08-03 · 客服系统（工单/投诉处理）

### 数据库
- `db/support.sql`：`t_staff`（客服账号，初始 admin/password）+ `t_support_ticket`（工单），需在 SQL Server 执行

### 后端
| 文件 | 说明 |
|------|------|
| `entity/Staff.java` / `SupportTicket.java` + Mapper | 客服账号 / 工单实体 |
| `service/CustomerSupportService(Impl)` | 客服登录、用户提交工单、我的工单、客服受理/回复/关闭（状态机 0待处理→1处理中→2已回复→3已关闭） |
| `controller/SupportController.java` | `/support/login`、`/support/ticket`、`/support/tickets`、`/support/admin/**` |
| `LoginContext` + `JwtAuthenticationFilter` | 新增 `S_` 前缀 token → `LoginContext.setStaffId`，区分客服身份 |

### 前端
- `api/support.js`：全部客服接口
- `views/SupportLogin.vue`（`/support/login`）+ `views/SupportDashboard.vue`（`/support` 客服工作台：状态 tab、受理/回复/关闭）
- `components/SupportCenter.vue`：账户中心「客服/投诉」快捷卡片弹窗（提交工单 + 我的工单状态）
- `Dashboard.vue` 快捷操作新增「客服/投诉」入口

### 验证
- 后端 `mvn compile` ✅、前端 `npm run build` ✅
- 使用前需执行 `db/support.sql` 建表 + 初始化客服账号（admin / password）

---

## 2026-07-31 · 三大创新算法

### 1. 孤立森林异常交易检测（无第三方 ML 依赖）
- 手写 `common/algo/IsolationForest.java`：iTree 递归构建、路径长度、异常分 s=2^(-E(h)/c(n))
- 集成风控第 6 条规则：历史 ≥20 笔训练（特征：金额/交易时点/距上笔间隔天数，z-score 标准化），新交易异常分 >0.62 → +30
- 模型本地 `ConcurrentHashMap` 缓存，TTL 10 分钟

### 2. 智能理财推荐引擎（基于风险偏好画像）
- `service/RecommendationService(Impl)` + `FinancialRecommendVO`
- 用户风险偏好：持仓加权收益率 → 无持仓用历史平均单笔金额映射收益率锚点 → 风险等级（保守/稳健/进取/激进）
- 产品打分：收益率匹配(60%) + 期限偏好(30%) + 剩余额度(10%)，输出 Top-N + 推荐理由
- 接口 `GET /financial/recommend?accountNumber&topN`

### 3. 余额趋势预测（线性回归）
- `service/TrendForecastService(Impl)` + `BalanceForecastVO/BalancePointVO`
- 近 30 天每日净流入 → 最小二乘线性回归 → 预测未来 7 天余额，min<0 → 资金不足预警
- 接口 `GET /account/forecast?accountNumber`

### 前端
- `ProductList.vue`：顶部"为你推荐"区（匹配度进度条 + 理由 + 风险等级，可点击购买）
- `BalanceQuery.vue`：下方"未来 7 天余额趋势预测"柱状图 + 资金预警标签

### 验证
- 后端 `mvn compile` ✅
- 前端 `npm run build` ✅

---

## 2026-07-31 · 用户中心改密码接口 + UI 升级

### 后端
| 文件 | 说明 |
|------|------|
| `dto/UserUpdatePsdDTO.java` | 新增：oldPassword / newPassword / confirmPassword（NotBlank 校验） |
| `service/UserService.java` | 新增 `updatePsw(Long userId, UserUpdatePsdDTO)` |
| `service/impl/UserServiceImpl.java` | 实现：校验原密码、两次密码一致、encode 后更新 |
| `controller/UserController.java` | 新增 `POST /user/updatePsw`（LoginContext 取 userId） |

### 前端
- `api/user.js` 新增 `updateUserPassword`
- `UserDashboard.vue`：下拉菜单新增「修改密码」，弹窗表单提交

### UI 升级
- `UserDashboard.vue` 整体重写为 Dashboard.vue 风格：深蓝渐变用户资料卡、头部时间 + 用户下拉、快捷操作卡（注册新卡/添加银行卡）、统一圆角卡片样式

### 验证
- 后端 `mvn compile` ✅（JDK 17 + IDEA Maven 3.9.9）
- 前端 `npm run build` ✅

---

## 2026-07-31 · 交易风控系统（规则引擎 + 统计偏离检测）

**目标**：给转账/取款/理财购买加风险评分，异常交易拦截并提示具体原因。

### 新增
| 文件 | 说明 |
|------|------|
| `service/RiskAssessmentService.java` | 风控接口 `assessAndBlock(accountNumber, amount, counterparty)` |
| `service/impl/RiskAssessmentServiceImpl.java` | 规则引擎实现 |

### 规则与分值
| 规则 | 分值 | 说明 |
|------|------|------|
| 单笔 ≥ 10000 元 | +30 | 超大额 |
| 金额 > 历史均值 × 5 | +30 | 统计偏离（历史不足 3 笔跳过） |
| 23:00~05:00 交易 | +20 | 深夜交易 |
| 5 分钟内交易 ≥ 5 笔 | +25 | Redis 计数窗口 |
| 首次向该对手方转账 | +15 | 新对手方 |

总分 ≥ 60 拦截，异常 msg 列出命中规则。

### 接入点
- `AccountServiceImpl.transfer`：`assessAndBlock(outNumber, amount, inNumber)`
- `AccountServiceImpl.withdraw`：`assessAndBlock(accountNumber, amount, null)`
- `FinancialServiceImpl.purchase`：`assessAndBlock(accountNumber, amount, null)`

### 其他
- `ResponseCode` 新增 `TRANSACTION_RISK_REJECTED("500003")`

### 验证
- ✅ 后端编译通过：`C:\Users\26656\.jdks\ms-17.0.19`（Microsoft OpenJDK 17.0.19）+ IDEA 自带 Maven 3.9.9（`D:\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3`）
  - 本机系统 Maven 3.6.1 太旧（compiler-plugin 3.14.1 需 ≥3.6.3），需用 IDEA 内置 Maven
- 功能测试：可临时调低阈值（`BLOCK_THRESHOLD`）或用"深夜+首次转账+超限金额"验证拦截效果

---

## 2026-07-31 · User 新增 userName 字段

**目标**：用户表增加"用户名"，注册时填写，用户中心展示。

### 数据库
- `t_user` 新增列 `user_name NVARCHAR(50) NULL`
- 存量 2 个用户 `user_name` 用手机号填充

### 后端
| 文件 | 改动 |
|------|------|
| `entity/User.java` | 新增 `@TableField("user_name") private String userName` |
| `vo/UserVO.java` | 新增 `userName` |
| `dto/UserRegisterDTO.java` | 新增 `@NotBlank userName` |
| `service/impl/UserServiceImpl.java` | `userRegister` 设置 userName；`getUserInfo` 返回 userName |

### 前端
| 文件 | 改动 |
|------|------|
| `views/UserRegister.vue` | 注册表单新增"用户名"输入框（第一位）+ 必填校验 |
| `views/UserDashboard.vue` | 个人信息卡展示"用户名" |

### 验证
- 前端 `npm run build` ✅
- 后端需在 IDEA（JDK 17）编译验证

---

## 待办 / 后续事项

### 🔴 需 IDEA（JDK 17）编译运行验证
- [ ] 理财接口改造（显式 accountNumber + 归属校验）编译验证
- [ ] 理财到期自动结算 `@Scheduled` + 提前赎回无收益 编译验证（可在 IDEA 手动调 `autoSettleMaturedHoldings()` 测试）
- [ ] 后端编译验证（本机仅 JDK 8，工程需 JDK 17，只能在 IDEA 运行）
- [ ] AI 真实 API 联调确认（IDEA 中已配置 `API_KEY`）

### 🟡 前端未确认
- [ ] 理财"卡号不能为空"：代码链路已确认正确，需用户硬刷新（Ctrl+F5）后重测，若仍失败看 Network 的 Payload 是否带 `accountNumber`

### 🟢 清理项（本次审查发现）
- [ ] **死代码**：`LoginInterceptor.java` 未注册（`WebConfig` 是空的，没有 `addInterceptors`），不生效；与 `JwtAuthenticationFilter` 逻辑重复 → 二选一：删除 LoginInterceptor，或补上注册并停用 filter
- [ ] **半成品**：前端 `utils/request.js` 的 `status === 401` 分支几乎不触发——后端不返回 HTTP 401（拦截用的是业务码 `NO_LOGIN`，HTTP 200）→ 决定：改为后端真返回 401，或删除前端 401 分支
- [ ] （可选）删除 `/login`、`/register`、`/dashboard` 隐藏路由及页面
