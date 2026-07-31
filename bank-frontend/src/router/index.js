import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import ForgotPassword from '../views/ForgotPassword.vue'
import Dashboard from '../views/Dashboard.vue'
import UserLogin from '../views/UserLogin.vue'
import UserRegister from '../views/UserRegister.vue'
import UserDashboard from '../views/UserDashboard.vue'
import CardTrade from '../views/CardTrade.vue'

const routes = [
    { path: '/', redirect: '/user/login' },
    { path: '/login', component: Login },
    { path: '/register', component: Register },
    { path: '/forgot-password', component: ForgotPassword },
    { path: '/dashboard', component: Dashboard, meta: { requiresAuth: true } },
    { path: '/user/login', component: UserLogin },
    { path: '/user/register', component: UserRegister },
    { path: '/user/dashboard', component: UserDashboard, meta: { requiresAuth: true } },
    { path: '/trade/:accountNumber', component: CardTrade, meta: { requiresAuth: true } }
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes
})

router.beforeEach((to, from, next) => {
    const token = localStorage.getItem('token')
    if (to.meta.requiresAuth && !token) {
        const userPath = to.path.startsWith('/user/') ? '/user/login' : '/login'
        next(userPath)
    } else {
        next()
    }
})

export default router
