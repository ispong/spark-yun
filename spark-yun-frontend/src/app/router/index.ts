import { createRouter, createWebHistory, type RouteLocationRaw, type RouteRecordRaw } from 'vue-router'
import Home from '@/app/views/home/home'
import { useAuthStore } from '@/app/store/useAuth'
import { licenseRoutes } from '@/app/management/license'
import { oauthManagementPlatformRoutes, oauthManagementWorkspaceRoutes } from '@/app/management/oauth-management'
import { orgManagementAdminRoutes } from '@/app/management/org-management'
import { personalInfoRootRoute, personalInfoWorkspaceRoutes } from '@/app/management/personal-info'
import { roleManagementAdminRoutes } from '@/app/management/role-management'
import { tenantListPlatformRoutes, tenantListWorkspaceRoutes } from '@/app/management/tenant-list'
import { tenantUserAdminRoutes, tenantUserWorkspaceRoutes } from '@/app/management/tenant-user'
import { userCenterPlatformRoutes, userCenterWorkspaceRoutes } from '@/app/management/user-center'
import { shareModuleRoutes, workspaceModuleRoutes } from './module-routes'
import { setupRouterGuard } from './guard'

// 动态加载，读到路由才会加载
const Login = () => import('@/app/views/login/login')
const SsoAuth = () => import('@/app/views/login/ssoauth')
const Forbidden = () => import('@/app/views/system/forbidden.vue')
const NoTenant = () => import('@/app/views/system/no-tenant.vue')

function workspaceDefaultRoute(): RouteLocationRaw {
    const authStore = useAuthStore()
    if (authStore.userInfo?.workspaceAllPermissions) {
        return {
            name: 'index'
        }
    }
    const permissions: string[] = authStore.userInfo?.permissions || []
    const menuModules = permissions.filter((code) => code.endsWith(':menu')).map((code) => code.split(':')[1])
    const workspaceRoute = routes.find((route) => route.name === 'workspace')
    const workspaceChildren = workspaceRoute?.children || []
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

// 路由配置
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
        component: SsoAuth
    },
    {
        path: '/auth',
        name: 'login',
        component: Login
    },
    {
        path: '/platform',
        name: 'platform',
        component: Home,
        meta: {
            area: 'platform'
        },
        redirect: '/platform/users',
        children: [
            ...userCenterPlatformRoutes,
            ...tenantListPlatformRoutes,
            ...licenseRoutes,
            ...oauthManagementPlatformRoutes
        ]
    },
    {
        path: '/admin',
        name: 'admin',
        component: Home,
        meta: {
            area: 'admin'
        },
        redirect: '/admin/members',
        children: [
            ...tenantUserAdminRoutes,
            ...roleManagementAdminRoutes,
            ...orgManagementAdminRoutes
        ]
    },
    {
        path: '/workspace',
        name: 'workspace',
        component: Home,
        meta: {
            area: 'workspace'
        },
        redirect: workspaceDefaultRoute,
        children: [
            ...workspaceModuleRoutes,
            ...tenantUserWorkspaceRoutes,
            ...userCenterWorkspaceRoutes,
            ...tenantListWorkspaceRoutes,
            ...oauthManagementWorkspaceRoutes,
            ...licenseRoutes,
            ...personalInfoWorkspaceRoutes
        ]
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

// 创建路由实例
const router = createRouter({
    history: createWebHistory(import.meta.env.VITE_VUE_APP_PUBLIC_PATH),
    routes
})

setupRouterGuard(router, workspaceDefaultRoute)

export default router
