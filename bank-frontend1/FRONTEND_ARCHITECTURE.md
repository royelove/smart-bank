# bank-frontend · 前端架构与布局规范

> Vue 3 + Vite + TypeScript + Tailwind 的单页应用。
> 这篇文档说明三件事：**目录怎么分**、**布局怎么套**、**数据怎么流**。
> 新增页面时按第 2、3 节照抄即可，不需要再自己设计外壳。

---

## 1. 目录结构

```
bank-frontend/
├── index.html                  唯一的 HTML 入口（挂载点 #app）
├── vite.config.js              别名 @ -> src、开发代理（/user /support /api /account /stock /financial /ai -> :8081）
├── tailwind.config.js          设计令牌：primary/navy/success/danger/warning/info + 字号/圆角/阴影
├── scripts/check-sfc.mjs       SFC 编译自检（见第 6 节）
└── src/
    ├── main.ts                 createApp + Pinia + Router + main.css
    ├── App.vue                 只做 RouterView，不含任何布局
    ├── assets/main.css         全局设计系统：CSS 变量 + .app-* / .card / .btn-* / .input 等原子类
    ├── layout/                 两个外壳
    │   ├── MainLayout.vue      登录后的应用外壳（侧栏 + 顶栏 + 内容区）
    │   └── AuthLayout.vue      登录/注册页外壳（左品牌图 + 右表单）
    ├── components/
    │   ├── ui/                 通用 UI 原子组件（Button/Input/Card/Table/Modal/Badge/…）
    │   └── layout/             外壳零件（AppSidebar / AppTopbar / PageHeader / types.ts）
    ├── views/                  页面，一个文件 = 一个路由页面
    │   ├── auth/               登录、注册、找回密码、客服登录
    │   ├── user/               账户总览、我的银行卡、交易中心、投资理财、设置
    │   ├── support/            工单管理、用户管理（客服端）
    │   └── NotFound.vue        404（独立页面，不套外壳）
    ├── router/index.ts         路由表 + 导航守卫（页面标题的唯一来源）
    ├── stores/auth.ts          Pinia：用户/客服双身份、token 与资料持久化
    ├── composables/            useTheme（暗色模式）、usePageMeta（读取路由 meta）
    ├── api/                    后端接口清单，一个文件 = 一类业务
    ├── utils/                  request.js（axios 封装）+ format/validation/trade/account/charts/market/support
    └── types/index.ts          后端 VO 的 TS 镜像
```

**没有 Artifact 目录、没有 `layouts/`（已合并进 `layout/`）、没有 Element Plus。**
所有组件都是自己写的，第三方 UI 库只有 `lucide-vue-next`（图标）、`chart.js` + `vue-chartjs`（图表）。

---

## 2. 布局规范（重要）

### 2.1 外壳只有一个

登录后的所有页面共用 `layout/MainLayout.vue`：

```
.app-shell   ← flex 横向容器
├── .app-sidebar   左侧导航栏
│                  lg 以上：sticky、可折叠（折叠状态存 localStorage）
│                  lg 以下：Teleport 到 body 的抽屉，点遮罩/跳转自动关闭
└── .app-main      flex-1 min-w-0 的主列（min-w-0 让宽表格内部滚动，而不是把侧栏顶出屏幕）
    ├── .app-topbar      sticky 顶栏：汉堡键（移动端）、面包屑、暗色模式开关、用户菜单
    └── <main>           页面内容滚动区，带 out-in 路由过渡
```

**面包屑完全由路由 meta 生成**，页面不需要传任何东西。

### 2.2 每个页面的固定开头

页面只管自己的内容，外壳的事一律不碰。所有页面统一长这样：

```vue
<script setup lang="ts">
import { PageHeader } from '@/components/layout'
import { usePageMeta } from '@/composables/usePageMeta'

const { title, subtitle } = usePageMeta()   // 直接读 route.meta.title / .subtitle
</script>

<template>
  <div class="app-container">
    <PageHeader :title="title" :subtitle="subtitle">
      <template #actions>
        <!-- 页面级操作按钮，如「刷新」「新建」 -->
      </template>
    </PageHeader>

    <!-- 从这里开始写业务内容 -->
  </div>
</template>
```

几条硬性约定：

1. **必须只有一个根元素**（`RouterView` 用了 `mode="out-in"` 的 `Transition`，多根会报错）。
2. 根元素用 `.app-container`（`max-w-[1440px]` 居中 + 统一左右留白），窄表单页可用 `.app-container-narrow`。外壳不包容器，容器由页面自己声明。
3. **标题只写在 `router/index.ts` 的 `meta` 里**，不要在页面里再写一遍字面量，否则面包屑和标题会各说各话。
4. `<Card>` 内部如果要用 `.card-header` + `.card-body`，就**不要**再加 `card-padded`（会双重内边距）；无标题的卡片才用 `card-padded`。
5. Tailwind 类名必须是字面量，不能拼接字符串（拼接的类名不会进产物）。需要按条件变色时用映射表，例如 `utils/trade.ts` 的 `directionTone`。

### 2.3 断点约定

| 断点 | 侧栏 | 内容 |
|---|---|---|
| `< lg` (1024px) | 抽屉式，顶栏出现汉堡键 | 单列；表格允许横向滚动 |
| `lg ~ xl` | 固定窄栏（可折叠） | 双列为主 |
| `≥ xl` (1280px) | 固定宽栏 | 三列 / 主从布局 |

`sm`(640) 用来切换按钮组和表单的横竖排列，`xl` 用来切换"统计卡 2 列 → 4 列"这类栅格密度。

### 2.4 页面骨架的三种模式

| 模式 | 适用 | 结构 |
|---|---|---|
| **总览型** | 账户总览、投资理财 | 4 张 `StatCard` → 图表卡片（2 列）→ 明细表格 |
| **列表型** | 工单管理、我的银行卡、交易明细 | `StatCard` 行 → 筛选栏卡片 → 表格卡片 → 详情用 `Modal` |
| **表单型** | 设置、交易中心 | 左侧表单卡片（`max-w-2xl`）+ 右侧信息/确认卡片 |

列表型里，**筛选、分页、行内操作永远放在表格上方那条卡片里**，不要把筛选条件塞进表头或散落在页面各处。

### 2.5 视觉语言

- 主色 `primary` = **银行蓝**（`#2563eb` 一系），中性色 `navy` 是带蓝调的灰，语义色 success/danger/warning/info 只表达状态，不做装饰。
- 暗色模式是**真实生效**的：`html.dark` + `useTheme()` 单例，三档回退（localStorage `smartbank-theme` → 系统 `prefers-color-scheme` → 浅色）。所有颜色都要带 `dark:` 变体。
- 圆角 `rounded-lg/xl`、卡片 `shadow-sm` + 1px 描边、字号走 `text-heading-*` / `text-body-*` 令牌，不要在页面里写 `text-[13px]` 这种魔法值。

---

## 3. 数据流

```
页面 (views/*.vue)
   │  调用
   ▼
api/*.js            每个函数 = 一个后端接口，只负责 URL + method + 参数
   │
   ▼
utils/request.js    axios 实例
   │  ① 按路径自动带工牌：/support/admin/** 用 staff_token，其余用 token
   │  ② 解包统一响应体 Result<T> = { code, msg, data }
   │     成功(code === '000000') → resolve 出 data 本身
   │     失败 → reject(new Error(msg))
   │  ③ 401/未登录 → 清 token 并跳回对应登录页
   ▼
后端 Spring Boot (:8081，开发期由 Vite 代理)
```

因此 **api 函数 resolve 出来的就是业务数据本身**，页面里写的是：

```ts
const accounts = await getUserAccounts()      // 已经是 AccountVO[]
```

而不是 `res.data.data`。

### 状态管理

`stores/auth.ts` 是唯一的全局状态：用户与客服是两套 token（`token` / `staff_token`），并额外把 `user_info` / `staff_info` 也持久化到 localStorage——否则刷新页面顶栏就没有名字了。页面级数据一律用组件内 `ref` + `onMounted` 拉取，不进 store。

### 路由 meta 约定

```ts
meta: { title: '工单管理', subtitle: '受理、回复与关闭客户工单', parent: '客服工作台' }
```

- `title` → 面包屑末级 + `PageHeader` 标题
- `subtitle` → `PageHeader` 副标题
- `parent` → 面包屑中间段（对应侧栏分组名）
- `public: true` + `layout: 'auth'` → 免登录页
- 未登录访问受保护路由时，守卫会带上 `?redirect=<原路径>`，登录页用 `safeRedirect()` 校验后跳回（只接受同源绝对路径）。

---

## 4. UI 组件库（`components/ui`）

| 组件 | 关键点 |
|---|---|
| `Button` | `variant: primary/secondary/ghost/danger/link`，`size: sm/md/lg/icon`，`loading` 会禁用点击 |
| `Input` | 传 `error` 会自动渲染错误文案并加 `aria-invalid`，**页面不要再手写一遍错误提示** |
| `Card` | `variant: default/hover/padded` |
| `Table` | `columns[].render` 返回 VNode；渲染列走内部 `RenderCell` 函数式组件，**绝不能用 `{{ }}` 插值 VNode**（会变成 `[object Object]`） |
| `Modal` | `v-model` 控制，`footer` 具名插槽，Teleport 到 body |
| `Tabs` | **多根组件**（tabs 条 + 每个 tab 一个 `role=tabpanel`），不要给它传 `class` |
| `Badge` | `variant: success/warning/danger/info/neutral` |
| `StatCard` | `label/value/hint/trend/icon/loading` |
| `EmptyState` | `title/description/icon` + `action` 插槽 |
| `Alert` | `variant/title/description/dismissible`，注意 `warning` 用 amber、`info` 用 sky |

---

## 5. 工具函数（`utils/`）

| 文件 | 内容 |
|---|---|
| `request.js` | axios 实例、token 注入、`Result` 解包、401 处理 |
| `format.ts` | `formatMoney/formatAmount/formatPercent/formatDateTime/formatDate/maskAccountNumber`（全部 NaN 安全） |
| `validation.ts` | 手机号/身份证/6 位密码/短信码正则与校验函数、`maskPhone`、`safeRedirect`、`errorMessage`、`readImageAsBase64` |
| `trade.ts` | 买卖方向、订单状态、价格类型的文案与配色 |
| `account.ts` | 账户类型文案、`sumBalance` |
| `charts.ts` | Chart.js 注册 + `CHART_COLORS`（`[0]` 必须与 `primary[600]` 同步）+ 具名导出 `BarChart/DoughnutChart/LineChart` |
| `market.ts` | 行情/理财状态映射（注释里标注了对应的 Java 行号） |
| `support.ts` | 工单状态/情感/AI 有效性映射 + `canAccept/canReply/canClose` 三个按钮可用性判断 |

`utils/*.ts` 里的业务映射都写了**对应 Java 源码位置**的注释，后端改了状态码只需搜这个常量名。

---

## 6. 开发与自检

```bash
npm install
npm run dev          # 开发服务器（需要后端 :8081 一起跑）
npm run build        # vite build，产物在 dist/
node scripts/check-sfc.mjs   # 不启动浏览器，检查所有 SFC 能否编译
```

`check-sfc.mjs` 用 `@vue/compiler-sfc` 逐个 parse + compileScript + compileTemplate，
能在不跑构建的情况下抓出模板语法错误、`<script setup>` 里的非法语句、标签未闭合等问题，
输出形如 `OK - 33 SFC(s) parsed and compiled cleanly`。

注意：`npm run build` **不跑 `vue-tsc`**，所以类型不严谨不会让构建失败；真要卡类型得单独跑 `vue-tsc --noEmit`。
