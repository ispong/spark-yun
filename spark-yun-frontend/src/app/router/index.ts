import { createRouter, createWebHistory, type RouteLocationRaw, type RouteRecordRaw } from 'vue-router'
import Home from '@/app/views/home/home'
import { useAuthStore } from '@/app/store/useAuth'
import { workspaceModuleRoutes } from './module-routes'
import { setupRouterGuard } from './guard'

// 动态加载，读到路由才会加载
const Login = () => import('@/app/views/login/login')
const SsoAuth = () => import('@/app/views/login/ssoauth')
const Forbidden = () => import('@/app/views/system/forbidden.vue')
const UserCenter = () => import('@/app/management/user-center/views/index.vue')
const TenantList = () => import('@/app/management/tenant-list/views/index.vue')
const License = () => import('@/app/management/license/views/index.vue')
const OauthManagement = () => import('@/app/management/oauth-management/views/index.vue')
const TenantUser = () => import('@/app/management/tenant-user/views/index.vue')
const RoleManagement = () => import('@/app/management/role-management/views/index.vue')
const OrgManagement = () => import('@/app/management/org-management/views/index.vue')
const PersonalInfo = () => import('@/app/management/personal-info/views/index.vue')
const ShareForm = () => import('@/modules/custom-form/views/share-form-page/index.vue')
const ShareReport = () => import('@/modules/report/views/report-views/share-report/index.vue')

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
            {
                path: 'users',
                name: 'platform-user-center',
                component: UserCenter
            },
            {
                path: 'tenants',
                name: 'platform-tenant-list',
                component: TenantList
            },
            {
                path: 'license',
                name: 'license',
                component: License
            },
            {
                path: 'auth',
                name: 'platform-oauth-management',
                component: OauthManagement
            }
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
            {
                path: 'members',
                name: 'admin-tenant-user',
                component: TenantUser
            },
            {
                path: 'roles',
                name: 'role-management',
                component: RoleManagement
            },
            {
                path: 'orgs',
                name: 'org-management',
                component: OrgManagement
            }
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
            {
                path: 'tenant-user',
                name: 'tenant-user',
                component: TenantUser
            },
            {
                path: 'user-center',
                name: 'user-center',
                component: UserCenter
            },
            {
                path: 'tenant-list',
                name: 'tenant-list',
                component: TenantList
            },
            {
                path: 'oauth-management',
                name: 'oauth-management',
                component: OauthManagement
            },
            {
                path: 'license',
                name: 'license',
                component: License
            },
            {
                path: 'personal-info',
                name: 'personalInfo',
                component: PersonalInfo
            },
            ...workspaceModuleRoutes
        ]
    },
    {
        path: '/personal-info',
        name: 'personalInfo',
        component: PersonalInfo
    },
    {
        path: '/403',
        name: 'forbidden',
        component: Forbidden
    },
    {
        path: '/share/:shareParam',
        name: 'share',
        component: ShareForm
    },
    {
        path: '/dashboard/:shareParam',
        name: 'share-report',
        component: ShareReport
    },
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
