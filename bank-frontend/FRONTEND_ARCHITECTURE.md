# 钱钱银行前端 · 建筑导览

> 把 `bank-frontend` 想象成一家银行网点，每个文件夹都是在其中上班的"部门"。
> 看完这篇，你会清楚谁负责什么、活儿是怎么流转的。

---

## 🗺️ 一图看懂（谁在哪上班）

```
bank-frontend/                      ← 整个银行网点
│
├── index.html                      ← 网点门口的"招牌"（入口门面）
├── vite.config.js                  ← 施工图纸（怎么盖楼）
├── package.json                    ← 采购清单（要用哪些供应商）
├── public/                         ← 公告栏（原样张贴的静态海报）
├── dist/                           ← 建好的"分行"（部署上线用的成品）
├── node_modules/                   ← 供应商大仓库（快递全堆这）
│
└── src/                            ← 网点内部（真正的营业区）
    ├── main.js                     ← 开门钥匙：一按，全网点营业
    ├── App.vue                     ← 大堂总框架：所有业务都在这里展开
    ├── router/                     ← 楼层导航图：客人该去哪个厅
    ├── views/                      ← 营业大厅：整面整面的业务厅
    ├── components/                 ← 柜台/自助机：可复用的办事窗口
    ├── api/                        ← 专属话务员：和后端打电话的号码簿
    └── utils/                      ← 万能工具间：送信员、盖章机都在此
```

---

## 🔑 main.js —— 开门钥匙

整个网点的启动开关：

```js
createApp(App)      // 通电
  .use(router)      // 打开楼层导航
  .use(ElementPlus) // 装好标准柜台设备
  .mount('#app')    // 正式开门营业
```

没有它，其他人都进不了门。

## 🏛️ App.vue —— 大堂总框架

所有页面的"母体"。不管你是去登录厅还是客服工作台，都在这栋大堂里进出。

## 🧭 router/ —— 楼层导航图

一张地图，写明了 **URL 地址 → 去哪个厅**：

```js
{ path: '/user/login', component: UserLogin }      // 去"用户登录厅"
{ path: '/support',    component: SupportDashboard } // 去"客服工作台"
```

还配了个保安（导航守卫）：

```js
router.beforeEach((to, from, next) => { ... })
```

没带工牌（token）想硬闯需要登录的厅？保安直接把你请去登录厅。

## 🏢 views/ —— 营业大厅（整页）

**一个文件 = 一整个业务大厅**，路由直接挂载它们。客人来了就进这些厅办事：

| 文件 | 大厅 |
|---|---|
| `UserLogin.vue` | 用户登录厅 |
| `UserDashboard.vue` | 用户服务中心 |
| `SupportDashboard.vue` | 客服工作台（后台管理） |
| `CardTrade.vue` | 交易厅 |

## 💼 components/ —— 柜台与自助机（零件）

**能被多个厅复用的"办事窗口"**，像 ATM、转账机、咨询台：

| 组件 | 干啥的 |
|---|---|
| `Deposit.vue` | 存款机 |
| `Transfer.vue` | 转账机 |
| `Withdraw.vue` | 取款机 |
| `SupportCenter.vue` | 客服咨询台（用户提交工单/投诉） |
| `ChatBot.vue` | AI 客服机器人 |

大厅（views）就是把这些零件摆起来。零件写一次，处处能用——这就是"复用"。

## 📞 api/ —— 专属话务员（后端号码簿）

**前端自己绝不直接拨后端电话**，全通过这里。一个文件 = 跟后端某一类业务的通话清单：

```js
// api/user.js
export function userLogin(data) {
  return request({ url: '/user/login', method: 'post', data })
}
```

好处：后端路径改了只改一处；业务代码里只用一句话就能"拨号"。

## 📮 utils/request.js —— 万能送信员

所有请求真正出发的地方，干三件大事：

1. **统一带工牌**：每次发请求自动从 localStorage 取 token 塞进 `Authorization` 头
2. **认家门**：客服后台接口（`/support/admin/`）送客服工牌（staff_token），其余送用户工牌
3. **统一报错**：后端说"你没权限/过期了"，它统一弹提示并踢回登录页

你平时写的 `api/*` 都是通过它来真正"寄信"。

---

## 🧩 一张图看懂"活儿是怎么流转的"

```
用户点击"转账"按钮
        │
        ▼
views/CardTrade.vue（交易厅）
        │  调用
        ▼
api/account.js（话务员拿起号码簿）
        │  拨号
        ▼
utils/request.js（送信员带上工牌去寄信）
        │  HTTP 请求
        ▼
后端 Java 服务（处理完回信）
        │  响应 JSON
        ▼
回到页面 → 弹"转账成功"
```

**一句话总结**：
- `views` = 大厅（整页）
- `components` = 柜台零件（复用）
- `api` = 和后端打电话的号码簿
- `utils/request.js` = 真正寄信的送信员
- `router` = 楼层导航 + 门卫
- `main.js` = 开门钥匙

这就是一个 Vue 前端项目的"组织架构"，面试讲项目时按这套逻辑讲，评委一听就懂。
