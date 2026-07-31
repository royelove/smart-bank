# 智慧银行（Smart Bank）开发日志

> 记录项目关键更新，按时间倒序排列。
> 模块路径：`bank-finance`（后端）/ `bank-frontend`（前端）

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

- IDEA 运行配置（`BankFinanceApplication`）中已配置 `API_KEY=sk-54905aaff9704094913b2ebae0c5d9a9`
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

## 待办 / 后续事项

- [ ] 后端编译验证（本机仅 JDK 8，工程需 JDK 17，只能在 IDEA 运行）
- [ ] AI 真实 API 联调确认（IDEA 中已配置 `API_KEY`）
- [ ] （可选）删除 `/login`、`/register`、`/dashboard` 隐藏路由及页面
