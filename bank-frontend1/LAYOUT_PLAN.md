# bank-frontend · 布局改造方案

> 状态：**方案，未实施**。本文只描述"改成什么样、按什么顺序改"，不含代码改动。
> 配套文档：[`FRONTEND_ARCHITECTURE.md`](FRONTEND_ARCHITECTURE.md)（现行规范，布局契约仍以它为准）。
>
> 核查基线：工作区未提交状态，`node scripts/check-sfc.mjs` → `OK - 34 SFC(s) parsed and compiled cleanly`（34 = 14 个 `components/ui` + 3 个 `components/layout` + 2 个 `layout` + 14 个页面 + `App.vue`）。
> 所有行号对应当前工作区文件，改动前请重新确认。
> 文中计数均由当前工作区文件实测得出（全文正则统计，非抽样），可复算；凡与直觉不符的数字，以本表为准。

---

## 0. 结论摘要

**外壳不用动。** `MainLayout`（sticky 侧栏 + 顶栏 + off-canvas 抽屉）、设计令牌、`components/ui` 原子组件、`PageHeader` + 路由 `meta` 面包屑这条主线是成立的，问题全部在**页面层**，集中在三件事：

| # | 问题 | 一句话 |
|---|------|--------|
| 1 | **导航层断裂** | 做好的客服页面没有路由也没有入口；交易中心页侧栏无高亮 |
| 2 | **设计系统与页面脱节** | 8 个布局令牌零使用；31 处手写 `mb-6`；手写 tab / 14 处手写提示块重复实现 |
| 3 | **页面形态单调** | 一律 `.app-container`（1440）+ 单列卡片堆叠；市场页 1208 行单体；设置页右侧 2/3 空白 |

推荐的路线是**三个层次依次推进**，而不是一次性重写：先修导航与节奏（低风险、立刻可见），再回收设计系统，最后才动页面结构。

---

## 1. 现状盘点：哪些不要动

| 资产 | 位置 | 判断 |
|------|------|------|
| 应用外壳 | `src/layout/MainLayout.vue` | ✅ 保留。布局契约（rail + main column + `min-w-0`）正确 |
| 侧栏 / 顶栏零件 | `src/components/layout/` | ✅ 保留。`AppSidebar` 折叠 + 抽屉双模、`AppTopbar` 用 `Dropdown` 都是对的 |
| 设计令牌 | `tailwind.config.js` | ✅ 保留。primary/navy/语义色 + 字号/圆角/阴影档位齐全 |
| 全局样式层 | `src/assets/main.css` | ⚠️ 保留但需**回收使用**（见 P1-1）与去重（P2-2） |
| UI 原子组件 | `src/components/ui/`（14 个） | ✅ 保留。`Table`/`Tabs`/`Alert` 能力够，问题是没人用 |
| 页面头部 | `components/layout/PageHeader.vue` + `usePageMeta()` | ✅ 保留。标题单一来源这条约定做得很好 |
| 数据层 | `api/` → `utils/request.js` → 后端 | ✅ 不动。与布局无关 |
| 登录外壳 | `src/layout/AuthLayout.vue` | ✅ 保留。左右分屏 + 移动端折叠，无问题 |

**一句话**：不要重写外壳，不要引入 Element Plus / 任何 UI 库，不要推翻 `main.css`。这轮改造的性质是**"接线 + 收口 + 分级"**，不是"换框架"。

---

## 2. 问题清单

### P0 — 硬伤（功能可达性 / 方位感）

#### P0-1 用户端客服页面是死页面

`src/views/user/Support.vue`（663 行）是一个完整实现：`Tabs` 两页签（智能客服 / 我的工单）、流式对话、`QUICK_QUESTIONS` 快捷问题、`transcript` 滚动容器、工单列表。但：

- `src/router/index.ts` 的路由表（第 4–110 行）**完全没有引用它**；
- `MainLayout.vue` 的 `userSections`（第 42–58 行）**没有入口**；
- 旧的浮动客服组件 `src/components/ChatBot.vue` 已从工作区删除（`git status` 显示 `MD`）。

⇒ **用户当前无法触达任何客服 / AI 对话功能。**

> ⚠️ 关键约束（容易踩）：**新路由不能放在 `/support` 前缀下**。两处代码把 `/support` 前缀硬编码为"客服端身份"：
> - `MainLayout.vue:40` — `isSupport = route.path.startsWith('/support')` → 命中就换成客服侧栏
> - `router/index.ts:149` — `if (to.path.startsWith('/support'))` → 命中就要求 `isStaffAuthenticated`
>
> 建议用户端路径用 **`/help`**（客服中心），与 `/support/*`（客服工作台）彻底分开。

#### P0-2 `/trade/:accountNumber` 上侧栏无高亮

`AppSidebar.vue:41-45`：

```js
function isActive(item) {
  const path = item.to
  if (item.exact) return route.path === path
  return route.path === path || route.path.startsWith(`${path}/`)
}
```

`/trade/{n}` 既不等于 `/user/dashboard` 也不以它开头 ⇒ 在交易中心页，侧栏**没有任何一项高亮**。同样是"钻取页"的还有未来的详情页。

⇒ 建议支持 **`meta.activePath`**：`CardTrade` 路由声明 `activePath: '/user/dashboard'`，`isActive` 优先读它。

#### P0-3 纵向节奏靠手写 `mb-6`

全项目 `.vue` 中 `mb-6` 出现 **31 处**（`support/Dashboard.vue` 6、`support/Users.vue` 4、`user/Dashboard.vue` 3、其余 1–2 处）。而设计系统里 `main.css:330-332` 已经定义了：

```css
.page-content { @apply space-y-6; }
```

**使用次数：0。** 意味着每个页面各自维护兄弟节点间距，任何一次插入/删除区块都要手动补 margin。

### P1 — 设计系统脱节（维护性 / 一致性）

#### P1-1 八个布局令牌零使用

在 `src/**/*.vue` 中全量搜索，以下 `main.css` 令牌**使用次数为 0**：

| 令牌 | 定义位置 | 本该用于 |
|------|----------|----------|
| `.page-content` | `main.css:330-332` | 所有页面正文 wrapper（替代手写 `mb-6`，其中 22 处属 1.6 范围） |
| `.app-container-narrow` | `main.css:296-298` | 表单型页面 |
| `.grid-dashboard` | `main.css:392-394` | 4 列 StatCard 行 |
| `.grid-dashboard-3` | `main.css:396-398` | 3 列 StatCard 行 |
| `.grid-cards` | `main.css:400-402` | 卡片网格 |
| `.grid-2` | `main.css:404-406` | 双列区块 |
| `.grid-3` | `main.css:408-410` | 三列区块 |
| `.skeleton-card` | `main.css:429-431` | 卡片骨架屏 |

反过来，页面里到处在写等价的内联版本：

| 手写位置 | 实际类名 | 与令牌的差异 |
|----------|----------|--------------|
| `support/Dashboard.vue:299` | `mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4` | 仅 `gap-4` vs `gap-6` |
| `support/Users.vue:317` | `mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3` | 仅 `gap-4` vs `gap-6` |
| `user/Dashboard.vue:299` | `grid gap-4 sm:grid-cols-2 xl:grid-cols-4 mb-6` | `gap-4` **且**断点是 `xl`(1280) 而非 `lg`(1024) |
| `user/Market.vue:737` | 同上（`<section>` 版本） | 同上 |
| `user/UserDashboard.vue:172` | 同上 | 同上 |

⚠️ **不等价**：用户端三处用 `xl:grid-cols-4`，`lg`(1024–1279) 区间仍是 2 列；而 `.grid-dashboard` 在 `lg` 就变 4 列。直接替换会改变 1024–1279 宽度下的排布 —— 这是替换时必须确认的第一个点。

⇒ 栅格密度无法全局调整，改一处漏一处。

#### P1-2 `Market.vue` 单体页面 + 表格嵌卡片造成双层边框

`user/Market.vue` 共 **1208 行**（`<template>` 670–1208）。结构：

```
.app-container (671)
├── PageHeader + 账户选择器 <select class="input w-64"> (672-714)
├── EmptyState 未绑卡 (716-726)
└── v-else (728-949)
    ├── 手写提示条 <p> (729-735)
    ├── 4× StatCard 手写栅格 (737-766)
    ├── 【一个巨型 Card】(768-948)
    │   ├── card-header：手写 tab 按钮 (769-787)
    │   └── card-body (p-6) (789-947)
    │       ├── tab=products：推荐网格 + Table (791-855)
    │       ├── tab=holdings：Table (858-877)
    │       └── tab=stocks：3 个 <section> 各含 Table (880-946)
    └── 3 个 Modal (952-1206)
```

两个具体缺陷：

1. **双层边框 + 双层内边距**：`Table.vue:46,63` 自带 `.table-container`（`rounded-lg border`），被放进 `card-body`（`p-6`）里，于是表格边框内又套一层卡片边框、外加 24px 内边距。
2. **tab 切换导致页面高度剧烈跳动**：三个面板各自堆 1–3 张表，且顶部 4 张 StatCard 是**全局**的 —— 切到股票 tab 时，上面仍挂着"理财持仓金额 / 理财预期收益"两张理财指标。

#### P1-3 手写 tab 与手写提示块重复实现

- **手写 tab**：`Market.vue:770-783`（圆角药丸样式）、`CardTrade.vue:576-587`。而 `Tabs.vue` 已有 `variant="pills"` 分支（`Tabs.vue:44-50`）。
- **手写提示块**：全站共 **14 处**，散落在 **4 个文件**里，这 4 个文件**都没有** `import { Alert }`：

| 形态 | 位置 | 数量 |
|------|------|------|
| 带边框彩色提示条 | `user/Market.vue` `729-735`(warning) / `839-845` / `862-868` / `884-890` / `927-933`(danger) | 5 |
| 带边框彩色提示条 | `user/CardTrade.vue` `542-549`(danger) / `550-557`(success) / `618-625`(danger) | 3 |
| 带边框彩色提示条 | `user/Dashboard.vue` `247-253`(danger，`<div>` 而非 `<p>`) | 1 |
| 带边框彩色提示条 | `user/UserDashboard.vue` `164-170`(danger，`<div>`) | 1 |
| 无边框内联错误 | `user/Market.vue` `1002` / `1059` / `1182`（`role="alert"` + `text-sm text-danger-600`） | 3 |
| 无边框内联错误 | `user/UserDashboard.vue` `380`（同上） | 1 |

其中带边框的 10 处有两种圆角（`rounded-xl` 用于页面级、`rounded-lg` 用于卡内级）和两种内边距（`px-4 py-3` / `px-4 py-2`），已开始漂移。

> 📌 精确说法是"**这 4 个文件没 import `Alert`**"，不是"全项目没人用 `Alert`"。`Alert` 实际已在 **9 个文件**里正常使用：`auth/UserLogin.vue`、`auth/UserRegister.vue`、`auth/Register.vue`、`auth/ForgotPassword.vue`、`auth/SupportLogin.vue`、`user/Settings.vue`、`user/Support.vue`、`support/Dashboard.vue`、`support/Users.vue`。所以这是**覆盖率问题**（业务页面漏用），不是组件能力问题。

⇒ 告警样式有 3–4 份实现、tab 样式有 2–3 份实现，必然会漂移。

#### P1-4 渐变 hero 两套实现 + 总资产信息重复

| 位置 | 形态 |
|------|------|
| `user/Dashboard.vue:256-296` | `card-padded` + `from-primary-600 to-primary-700`，显示"总资产（人民币）" |
| `user/CardTrade.vue:366-425` | 自建渐变 hero，显示账户余额 |
| `user/UserDashboard.vue:46-56` | `CARD_GRADIENTS` 数组（5 套渐变），银行卡卡面 |

三种渐变表面、三种圆角、三种内边距 —— 这是设计系统之外的"第四种视觉重量级"。

更要紧的是 **`Dashboard.vue` 信息重复**：hero（256–296）显示"总资产"，紧接着的 `StatCard`（300–306）**又显示一遍同一个 `totalBalance`**，两屏之内同一数字出现两次，稀释了其余三张卡的信息量。

#### P1-5 表格列宽内联 px，窄屏无降级

`Table.vue:101` 用 `:style="{ width: column.width }"` 写死列宽。全项目 `width: '...'` 共 **56 处**：

| 文件 | 处数 | 列定义行范围 |
|------|------|--------------|
| `user/Market.vue` | 33 | 118–168、188–234、270–305、317–354、358–415（5 组） |
| `support/Dashboard.vue` | 8 | — |
| `user/Support.vue` | 6 | — |
| `support/Users.vue` | 5 | — |
| `user/CardTrade.vue` | 4 | 199–240 |

没有共享的列宽档位。后果：窄视口下只有 `.table-container` 的 `overflow-x-auto` 横向滚动，**没有任何"表格 → 卡片列表"的降级**，手机上只能左右拖着看。

#### P1-6 内容宽度只有一档，设置页右侧大片空白

`.app-container`（`main.css:292-294`）= `max-w-[1440px]`，**8 个页面**在用（`user/` 6 个 + `support/` 2 个）；`.app-container-narrow`（max-w-3xl）**零使用**。

具体表现：`user/Settings.vue`（997 行）用页内 `Tabs`（64–69 定义，405–408 使用）分三面板，但表单被 `max-w-xl` 卡住（`529`、`694`），在 1440 宽度下**右侧约 2/3 是纯空白**。设置页天然适合"左侧分区导航 + 右侧面板"的两栏形态，而不是顶部 tab + 单列。

### P2 — 结构债

#### P2-1 页面骨架类型未组件化（文档有约定，代码无载体）

`FRONTEND_ARCHITECTURE.md` §2.4 定义了三种骨架（总览型 / 列表型 / 表单型），但目前只是**文档约定**，没有对应组件。每个新页面都要手抄一遍"StatCard 行 → 筛选卡 → 表格卡 → 详情 Modal"。

实际达成度其实不低 —— `support/Dashboard.vue`（StatCard 行 299–328 → 筛选卡 330–401 → 表格卡 421–431 → 详情 Modal 434+）和 `support/Users.vue`（317–339 → 341–355 → 372–382 → 385+）都严格符合列表型。**只是这份正确性无法复用、无法校验**。

#### P2-2 `main.css` 内部有重复定义

`.page-container` 被定义了两次，内容完全相同：

- `main.css:321-324`
- `main.css:386-389`

后者是重构时残留的重复块（其上方注释"Page layout"也是重复的）。

---

## 3. 三档方案

三档是**叠加**关系，不是三选一。可以只做第 1 档就停下。

---

### 第 1 档 · 修导航 + 统一节奏

**性质**：低风险接线与清理。不动任何页面的视觉结构，页面看起来几乎一样，但**功能可达性和代码一致性明显改善**。

| # | 文件 | 改动 |
|---|------|------|
| 1.1 | `src/router/index.ts` | 在受保护的用户路由 children（51–82 行）内新增 `{ path: 'help', name: 'UserSupport', component: () => import('@/views/user/Support.vue'), meta: { title: '客服中心', subtitle: '智能客服与我的工单', parent: '服务支持', activePath: '/help' } }`。**注意路径不能以 `/support` 开头**（见 P0-1 约束） |
| 1.2 | `src/layout/MainLayout.vue` | `userSections`（42–58）重排为四组，新增「服务支持 → 客服中心 `/help`（`LifeBuoy` 图标）」 |
| 1.3 | `src/layout/MainLayout.vue` | `breadcrumb`（83–87）已读 `route.meta.parent`，新增路由的 `parent` 填 `'服务支持'` 即可自动生效 |
| 1.4 | `src/router/index.ts` | `CardTrade` 路由（64–69）meta 增加 `activePath: '/user/dashboard'` |
| 1.5 | `src/components/layout/AppSidebar.vue` | `isActive`（41–45）优先读 `route.meta.activePath`：命中则直接返回 `true`，否则回落现有前缀逻辑 |
| 1.6 | 全部 **8 个**使用 `.app-container` 的页面 | 在 `.app-container` 内、`PageHeader` 之后加一层 `<div class="page-content">` 包住正文，删除直接子节点上的 `mb-6`（这 8 个页面内共 22 处：`support/Dashboard` 6、`support/Users` 4、`user/Dashboard` 3、`CardTrade` 2、`Market` 2、`Support` 2、`UserDashboard` 2、`Settings` 1。全项目 31 处中的其余 9 处在 6 个 `AuthLayout` 页面里，不属本项） |
| 1.7 | `src/assets/main.css` | 删除重复的 `.page-container`（386–389 块） |

**目标 IA**：

```
我的账户   账户总览        /dashboard
          我的银行卡      /user/dashboard     ← /trade/:n 也高亮此项
          交易中心        /trade/:accountNumber（钻取页，不进侧栏）
财富管理   投资理财        /market
服务支持   客服中心        /help               ← 新增，接入 Support.vue
账户设置   设置            /settings
```

**预期效果**：客服功能从"不可达"变"可用"；交易中心页有方位感；新增区块不再需要手算 margin。

**风险**：低。1.1–1.5 是纯增量；1.6 是机械替换，需逐个页面目视确认间距未翻倍（`.page-content` 的 `space-y-6` 与原有 `mb-6` 等价，但要注意**最后一个子节点的 `mb-6` 要删掉**，否则底部多 24px）。

**验证**：`node scripts/check-sfc.mjs`；手动走 `/help`、`/trade/{n}`、`/dashboard`，确认侧栏高亮与抽屉自动关闭；浅色/深色各看一遍。

---

### 第 2 档 · 设计系统回收

**性质**：把第 1 档暴露出的重复实现收回设计系统。**页面外观会变**（告警块、tab、表格边框会变好看且统一）。

| # | 文件 | 改动 |
|---|------|------|
| 2.1 | `Market.vue`、`CardTrade.vue`、`Dashboard.vue`、`UserDashboard.vue` | 10 处带边框提示条 → `<Alert variant="danger\|success\|warning" description="..." />`：`Market` `729-735`/`839-845`/`862-868`/`884-890`/`927-933`，`CardTrade` `542-549`/`550-557`/`618-625`，`Dashboard` `247-253`，`UserDashboard` `164-170`。这 4 个文件需**新增 `Alert` import**（目前都没 import）。另 4 处无边框内联错误（`Market` `1002`/`1059`/`1182`、`UserDashboard` `380`）可保留，仅统一为同一套文字色 |
| 2.2 | `Market.vue:770-783`、`CardTrade.vue:576-587` | 手写 tab 按钮 → `<Tabs variant="pills">`；`Market.vue` 需新增 `Tabs` import |
| 2.3 | `support/Dashboard.vue:299`、`support/Users.vue:317`（`lg` 断点，可直接换） / `user/Dashboard.vue:299`、`user/Market.vue:737`、`user/UserDashboard.vue:172`（`xl` 断点，**需确认是否接受在 1024–1279 变 4 列**） | 手写 StatCard 栅格 → `.grid-dashboard` / `.grid-dashboard-3`。注意令牌是 `gap-6`、手写是 `gap-4`，替换会把间距从 16px 拉到 24px |
| 2.4 | 新增 `components/ui/BalanceHero.vue` | 抽出渐变 hero：props `label / amount / meta / actions` 插槽。`Dashboard.vue:256-296` 与 `CardTrade.vue:366-425` 都改用它 |
| 2.5 | `Dashboard.vue:298-330` | **去重**：hero 已显示总资产 → 把第 1 张 `StatCard`（300–306，"总资产"）换成有增量的指标（如"本月净流入"），保留其余三张 |
| 2.6 | `UserDashboard.vue:46-56` | `CARD_GRADIENTS` 的 5 套渐变收敛到 `BalanceHero` 同一套色阶，或保留但由 `BalanceHero` 统一导出 |
| 2.7 | `Table.vue` + 各页 | 新增 `bleed` prop（或 `Card` 新增 `bodyTight`）：表格嵌卡片时**不要**双层边框，`Table` 去掉自身的 `border`/`rounded`，由卡片提供外框 |
| 2.8 | `utils/*` 或 `Table.vue` | 定义列宽档位常量（如 `COL_W.narrow/short/number/action`），替换全站 **56 处**内联 px（`Market` 33、`support/Dashboard` 8、`Support` 6、`support/Users` 5、`CardTrade` 4） |
| 2.9 | `Table.vue` | 增加窄视口降级：`< lg` 时按 `columns` 渲染为卡片列表（复用 `column.header` 作字段名）。这是本档唯一有实质实现量的项 |

**风险**：中。2.1–2.3 是替换，风险低；2.7–2.9 触及 `Table.vue` 公共组件，**所有列表页都会受影响**，必须逐页回归（`support/Dashboard`、`support/Users`、`Market` 3 张表、`CardTrade` 1 张表）。

**验证**：`check-sfc.mjs` + `npm run build`；重点回归 5 张表格在 375 / 768 / 1024 / 1440 四个宽度下的表现；深色模式检查 `Alert` 各 variant 配色。

---

### 第 3 档 · 页面结构重构

**性质**：改变页面的信息架构与路由形态。改动最大，收益也最大。

| # | 文件 | 改动 |
|---|------|------|
| 3.1 | 新增 `components/layout/SectionNav.vue` | 页内二级导航组件：`items: {label, to, icon?}[]` + `<RouterLink>`，`lg` 以上渲染为左侧 `w-56` sticky 竖排，`lg` 以下渲染为横向可滚动 pills |
| 3.2 | 新增 `layout/SectionLayout.vue`（或骨架类 `.app-container-settings`） | 两栏骨架：左 `SectionNav` + 右内容面板；供设置型页面复用 |
| 3.3 | `views/user/Market.vue` → `views/user/market/` | 拆成：`index.vue`（外壳：账户选择 + StatCard 行 + `<SectionNav>` + `<RouterView>`）+ `ProductsPanel.vue` + `HoldingsPanel.vue` + `StocksPanel.vue`。单文件回落到 300 行内 |
| 3.4 | `router/index.ts` | `/market` 改为带 children 的路由：`''` 重定向到 `products`，`products` / `holdings` / `stocks` 三个子路由，各自 `meta.title` |
| 3.5 | `views/user/market/index.vue` | StatCard 行改为**按子路由作用域**显示：`products`/`holdings` 显示理财指标，`stocks` 显示股票市值/浮盈 —— 修掉 P1-2 的"指标与内容不匹配" |
| 3.6 | `views/user/Settings.vue` | 页内 `Tabs`（405–408）→ `SectionNav` + 子路由体系（`/settings/profile`、`/settings/security`、`/settings/appearance`），右侧面板宽度从 `max-w-xl` 放宽到 `max-w-2xl`，两栏填满 1440 |
| 3.7 | `views/user/CardTrade.vue` | 筛选区（574–616；类型 pills + 日期区间）从 `card-body` 里提出来，独立成筛选卡片置于表格卡之上，符合列表型骨架 |

**为什么值得做**：`Market.vue` 1208 行 / `Settings.vue` 997 行已经越过"单文件可维护"的边界；拆成子路由换来**可深链、后退可用、按路由分包（首屏更小）、单文件可控**，并且让"统计卡作用域"这个 P1-2 的问题自然消失。

**风险**：高。3.3–3.6 是重写级改动，会破坏现有 tab 的页面状态（如 `Market` 的 `tab` ref、`Settings` 的 `activeTab`），需要重新设计状态归属（提到外壳 or 各面板自治）。

**验证**：除构建与 SFC 自检外，需要逐条走通：`/market` 重定向、三个子路由直接刷新可进、浏览器后退/前进、`Market` 的账户切换在各子路由下不丢、`Settings` 三个子路由的保存提示仍能显示。

---

## 4. 关键约束与坑

改之前务必记住这几条，都是当前代码的真实硬约束：

1. **`/support` 前缀被硬编码为"客服身份"** —— `MainLayout.vue:40` 与 `router/index.ts:149`。用户端客服页**必须**用别的前缀（建议 `/help`），否则会切到客服侧栏并要求 staff token。
2. **每个页面只能有一个根元素** —— `MainLayout.vue:183-193` 用了 `mode="out-in"` 的 `<Transition>`，多根会报错。加 `.page-content` wrapper 时正好符合这条。
3. **Tailwind 类名必须是字面量**，不能字符串拼接（拼接的类名不进产物）。按条件变色用映射表 —— 参考 `utils/trade.ts` 的 `directionTone`、`Month`/`gradientFor` 的写法。
4. **`Tabs.vue` 是多根组件**（tabs 条 + 每个 tab 一个 `role=tabpanel`，`Tabs.vue:78-87`），**不要给它传 `class`**。
5. **`Table` 的 `columns[].render` 返回 VNode**，渲染列必须走内部 `RenderCell`（`Table.vue:54-59`），**绝不能用 `{{ }}` 插值**（会变成 `[object Object]`）。
6. **`.card-header` + `.card-body` 与 `card-padded` 不要同时用**（双重内边距）。无标题的卡片才用 `card-padded`。
7. **改 `Table.vue` 是全站影响面** —— 5 张表 + 未来所有列表页。
8. **标题只写在 `router/index.ts` 的 `meta`**，页面里不要再写字面量，否则面包屑与标题不一致。

---

## 5. 落地顺序与验证

```
第 1 档 ──► 验证 ──► 第 2 档 ──► 验证 ──► 第 3 档 ──► 验证
接线+节奏    跑通     回收设计系统   回归5张表   结构重构     走一遍路由
(低风险)             (中风险)                 (高风险)
```

**每档结束都要跑**：

```bash
node scripts/check-sfc.mjs    # SFC 编译自检，期望 "OK - 34 SFC(s) ..."（第 3 档会变多）
npm run build                 # 确认产物构建通过
```

注意 `npm run build` **不跑 `vue-tsc`**，类型不严谨不会让构建失败；要卡类型得单独跑 `vue-tsc --noEmit`。

**每档建议的真机检查清单**：

- [ ] 375 / 768 / 1024 / 1440 四个宽度下无横向滚动（表格除外，且表格应有降级）
- [ ] 浅色 / 深色两套主题下无对比度问题（所有新类名都要带 `dark:` 变体）
- [ ] 侧栏：`lg` 以上折叠/展开、`lg` 以下抽屉点遮罩关闭、路由跳转后自动关闭
- [ ] 面包屑与 `PageHeader` 标题一致
- [ ] 骨架屏 → 数据 → 空态 三种状态都不跳版（图表必须有固定高度，如 `Dashboard.vue:356,387` 的 `h-56`）

---

## 6. 附：不建议做的事

| 诱惑 | 为什么不要 |
|------|-----------|
| 引入 Element Plus / Naive UI | 项目已有一套自研 `components/ui`，混用会出现两套视觉语言 + 两套 z-index 体系（历史 `DEV_LOG` 已记录过 ElMessage 层叠问题） |
| 重写 `MainLayout` 换"顶部导航 / 双栏可拖拽" | 现有 rail + 抽屉方案在 5–8 个一级入口的规模下是正确解，改造成本远大于收益 |
| 把 `main.css` 换成 tailwind 插件 / 删掉 `@layer components` | 现有原子类（`.card` / `.btn-*` / `.app-*`）被 30+ 处引用，是这轮改造的**基础**而不是负担。要做的是提高使用率，不是替换 |
| 给每个页面都套第 3 档的两栏骨架 | 总览型 / 列表型页面用满 1440 是对的。两栏骨架只适合**设置型 / 表单型**（当前就是 `Settings`，未来可能有"通知偏好""设备管理"） |
| 一次性做完三档再提交 | 第 3 档会实质重写 `Market.vue` / `Settings.vue`。分档提交才能在出问题时明确回滚边界 |
