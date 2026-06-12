import { createRouter, createWebHistory, type RouteLocationRaw, type RouteRecordRaw } from 'vue-router'
import Home from '../views/home/home'
import { editionWorkspaceRoutes } from '@edition'
import { useAuthStore } from '@/store/useAuth'
import adminRoutes from '@/modules/admin/routes'
import { personalInfoRootRoute } from '@/modules/personal-info'
import platformRoutes from '@/modules/platform/routes'
import { shareReportRoute } from '@/modules/report'
import workspaceRoutes from '@/modules/workspace/routes'
import { setupRouterGuard } from './guard'

const Login = () => import('../views/login/login')
const Ssoauth = () => import('../views/login/ssoauth')
const ShareForm = () => import('../views/share-form/index.vue')
const Forbidden = () => import('@/views/system/forbidden.vue')
const NoTenant = () => import('@/views/system/no-tenant.vue')

const managementRoutes = new Set([
    'tenant-user',
    'user-center',
    'tenant-list',
    'oauth-management',
    'license',
    'personalInfo'
])
const workspaceChildren = workspaceRoutes.filter((route) => !managementRoutes.has(String(route.name || '')))
    .concat(editionWorkspaceRoutes)

function workspaceDefaultRoute(): RouteLocationRaw {
    const authStore = useAuthStore()
    if (authStore.userInfo?.workspaceAllPermissions) {
        return {
            name: 'index'
        }
    }
    const permissions: string[] = authStore.userInfo?.permissions || []
    const menuModules = permissions.filter((code) => code.endsWith(':menu')).map((code) => code.split(':')[1])
    const target = workspaceChildren.find((route) => menuModules.includes(String(route.name || '')))
    return target
        ? {
              name: target.name
          }
        : {
              name: 'forbidden'
          }
}

function defaultRoute(): RouteLocationRaw {
    const authStore = useAuthStore()
    if (!authStore.token) {
        return {
            name: 'login'
        }
    }
    return authStore.userInfo?.systemAdmin
        ? {
              name: 'platform'
          }
        : {
              name: 'workspace'
          }
}

const routes: Array<RouteRecordRaw> = [
    {
        path: '/',
        redirect: {
            name: 'login'
        }
    },
    {
        path: '/ssoauth',
        name: 'ssoauth',
        component: Ssoauth
    },
    {
        path: '/auth',
        name: 'login',
        component: Login
    },
    {
        path: '/home',
        name: 'home',
        redirect: defaultRoute
    },
    {
        path: '/platform',
        name: 'platform',
        component: Home,
        meta: {
            area: 'platform'
        },
        redirect: {
            name: 'user-center'
        },
        children: platformRoutes
    },
    {
        path: '/admin',
        name: 'admin',
        component: Home,
        meta: {
            area: 'admin'
        },
        redirect: {
            name: 'tenant-user'
        },
        children: adminRoutes
    },
    {
        path: '/workspace',
        name: 'workspace',
        component: Home,
        meta: {
            area: 'workspace'
        },
        redirect: workspaceDefaultRoute,
        children: workspaceChildren
    },
    personalInfoRootRoute,
    {
        path: '/403',
        name: 'forbidden',
        component: Forbidden
    },
    {
        path: '/no-tenant',
        name: 'no-tenant',
        component: NoTenant
    },
    {
        path: '/share/:shareParam',
        name: 'share',
        component: ShareForm
    },
    shareReportRoute,
    {
        path: '/:pathMatch(.*)*',
        redirect: defaultRoute
    }
]

const router = createRouter({
    history: createWebHistory(import.meta.env.VITE_VUE_APP_PUBLIC_PATH),
    routes
})

setupRouterGuard(router, workspaceDefaultRoute)

export default router
