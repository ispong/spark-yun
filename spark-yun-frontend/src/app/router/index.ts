import { createRouter, createWebHistory, type RouteLocationRaw, type RouteRecordRaw } from 'vue-router'
import Home from '@/app/views/home/home'
import { editionWorkspaceRoutes } from '@edition'
import { useAuthStore } from '@/app/store/useAuth'
import adminRoutes from '@/app/routes/admin'
import { personalInfoRootRoute } from '@/app/management/personal-info'
import platformRoutes from '@/app/routes/platform'
import workspaceManagementRoutes from '@/app/routes/workspace-management'
import { shareModuleRoutes, workspaceModuleRoutes } from './module-routes'
import { setupRouterGuard } from './guard'

const Login = () => import('@/app/views/login/login')
const Ssoauth = () => import('@/app/views/login/ssoauth')
const Forbidden = () => import('@/app/views/system/forbidden.vue')
const NoTenant = () => import('@/app/views/system/no-tenant.vue')

const workspaceChildren = workspaceModuleRoutes.concat(workspaceManagementRoutes, editionWorkspaceRoutes)

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
        redirect: '/platform/users',
        children: platformRoutes
    },
    {
        path: '/admin',
        name: 'admin',
        component: Home,
        meta: {
            area: 'admin'
        },
        redirect: '/admin/members',
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
    ...shareModuleRoutes,
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
