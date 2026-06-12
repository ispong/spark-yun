import { createRouter, createWebHistory, type RouteLocationRaw, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import Home from '../views/home/home'
import { getVipLicenseEnabled, isVipMenuCode } from '@/utils/vip-license'
import { useAuthStore } from '@/store/useAuth'
import HomeChildren from './home-children'

const Login = () => import('../views/login/login')
const Ssoauth = () => import('../views/login/ssoauth')
const ShareForm = () => import('../views/share-form/index.vue')
const ShareReport = () => import('../views/report-views/share-report/index.vue')
const UserCenter = () => import('@/views/user-center/index.vue')
const TenantList = () => import('@/views/tenant-list/index.vue')
const License = () => import('@/views/license/index.vue')
const OauthManagement = () => import('@/views/oauth-management/index.vue')
const TenantUser = () => import('@/views/tenant-user/index.vue')
const RoleManagement = () => import('@/views/role-management/index.vue')
const OrgManagement = () => import('@/views/org-management/index.vue')
const PersonalInfo = () => import('@/views/personal-info/index.vue')
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
const workspaceChildren = HomeChildren.filter((route) => !managementRoutes.has(String(route.name || '')))

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
        redirect: defaultRoute
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
        children: [
            {
                path: 'users',
                name: 'user-center',
                component: UserCenter
            },
            {
                path: 'tenants',
                name: 'tenant-list',
                component: TenantList
            },
            {
                path: 'license',
                name: 'license',
                component: License
            },
            {
                path: 'auth',
                name: 'oauth-management',
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
        redirect: {
            name: 'tenant-user'
        },
        children: [
            {
                path: 'members',
                name: 'tenant-user',
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
        children: workspaceChildren
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
        path: '/no-tenant',
        name: 'no-tenant',
        component: NoTenant
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

const router = createRouter({
    history: createWebHistory(import.meta.env.VITE_VUE_APP_PUBLIC_PATH),
    routes
})

router.beforeEach(async (to) => {
    const authStore = useAuthStore()
    const routeName = typeof to.name === 'string' ? to.name : ''
    const openRouteName = new Set(['login', 'ssoauth', 'share', 'share-report'])
    if (openRouteName.has(routeName)) {
        return true
    }
    if (!authStore.token) {
        return {
            name: 'login'
        }
    }

    const area = to.meta.area
    if (area === 'platform' && !authStore.userInfo?.platformAdmin) {
        return {
            name: 'forbidden'
        }
    }
    if (area === 'admin' && !authStore.userInfo?.tenantAdmin && !authStore.userInfo?.normalAdmin) {
        return {
            name: 'forbidden'
        }
    }
    if (area === 'workspace') {
        if (authStore.userInfo?.systemAdmin) {
            return {
                name: 'forbidden'
            }
        }
        if (!authStore.tenantId) {
            return {
                name: 'no-tenant'
            }
        }
    }

    if (!routeName || !isVipMenuCode(routeName)) {
        return true
    }
    const vipEnabled = await getVipLicenseEnabled()
    if (vipEnabled) {
        return true
    }
    ElMessage.error('许可证未启用，无法访问商业版菜单')
    return workspaceDefaultRoute()
})

export default router
