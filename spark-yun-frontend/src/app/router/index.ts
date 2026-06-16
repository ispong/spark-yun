import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import Layout from '@/app/views/layout/layout.vue'
import { workspaceModuleRoutes } from './module-routes'
import { setupRouterGuard } from './guard'

// 动态加载，读到路由才会加载
const Login = () => import('@/app/views/login/login.vue')
const SsoAuth = () => import('@/app/views/login/ssoauth')
const Forbidden = () => import('@/app/views/system/forbidden.vue')
const UserCenter = () => import('@/app/management/user-center/views/index.vue')
const TenantList = () => import('@/app/management/tenant-list/views/index.vue')
const LoginMethod = () => import('@/app/management/login-method/views/index.vue')
const License = () => import('@/app/management/license/views/index.vue')
const PlatformSetting = () => import('@/app/management/platform-setting/views/index.vue')
const OauthManagement = () => import('@/app/management/oauth-management/views/index.vue')
const TenantUser = () => import('@/app/management/tenant-user/views/index.vue')
const RoleManagement = () => import('@/app/management/role-management/views/index.vue')
const AiConfig = () => import('@/app/management/ai-config/views/index.vue')
const BackendSetting = () => import('@/app/management/backend-setting/views/index.vue')
const OrgManagement = () => import('@/app/management/org-management/views/index.vue')
const PersonalInfo = () => import('@/app/management/personal-info/views/index.vue')
const ShareForm = () => import('@/modules/custom-form/views/share-form-page/index.vue')
const ShareReport = () => import('@/modules/report/views/report-views/share-report/index.vue')

export const routeArea = {
    platform: 'platform',
    admin: 'admin',
    workspace: 'workspace'
} as const

const personalInfoMeta = {
    personalInfo: true
}

function createPersonalInfoRoute(name: string): RouteRecordRaw {
    return {
        path: 'personal-info',
        name,
        component: PersonalInfo,
        meta: personalInfoMeta
    }
}

// 路由配置
const routes: Array<RouteRecordRaw> = [
    {
        path: '/ssoauth',
        name: 'ssoauth',
        component: SsoAuth
    },
    {
        path: '/',
        name: 'root',
        redirect: {
            name: 'login'
        }
    },
    {
        path: '/auth',
        name: 'login',
        component: Login
    },
    {
        path: '/platform',
        name: 'platform',
        component: Layout,
        meta: {
            area: routeArea.platform
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
                path: 'tenant-members',
                name: 'platform-tenant-user',
                component: TenantUser
            },
            {
                path: 'login-method',
                name: 'platform-login-method',
                component: LoginMethod
            },
            {
                path: 'license',
                name: 'license',
                component: License
            },
            {
                path: 'settings',
                name: 'platform-setting',
                component: PlatformSetting
            },
            {
                path: 'auth',
                name: 'platform-oauth-management',
                component: OauthManagement
            },
            createPersonalInfoRoute('platform-personalInfo')
        ]
    },
    {
        path: '/admin',
        name: 'admin',
        component: Layout,
        meta: {
            area: routeArea.admin
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
                path: 'ai-configs',
                name: 'ai-config',
                component: AiConfig
            },
            {
                path: 'settings',
                name: 'backend-setting',
                component: BackendSetting
            },
            {
                path: 'orgs',
                name: 'org-management',
                component: OrgManagement
            },
            createPersonalInfoRoute('admin-personalInfo')
        ]
    },
    {
        path: '/workspace',
        name: 'workspace',
        component: Layout,
        meta: {
            area: routeArea.workspace
        },
        redirect: '/workspace/ai',
        children: [
            createPersonalInfoRoute('workspace-personalInfo'),
            ...workspaceModuleRoutes
        ]
    },
    {
        path: '/personal-info',
        name: 'personalInfoRoot',
        component: Layout,
        meta: personalInfoMeta,
        children: [
            {
                path: '',
                name: 'personalInfo',
                component: PersonalInfo,
                meta: personalInfoMeta
            }
        ]
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
        name: 'not-found',
        redirect: {
            name: 'forbidden'
        }
    }
]

// 创建路由实例
const router = createRouter({
    history: createWebHistory(import.meta.env.VITE_VUE_APP_PUBLIC_PATH),
    routes
})

// 路由守卫
setupRouterGuard(router)

export default router
