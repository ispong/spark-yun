import { createRouter, createWebHistory, type RouteLocationRaw, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import Home from '../views/home/home'
import Login from '../views/login/login'
import Ssoauth from '../views/login/ssoauth'
import ShareForm from '../views/share-form/index.vue'
import ShareReport from '../views/report-views/share-report/index.vue'
import UserCenter from '@/views/user-center/index.vue'
import TenantList from '@/views/tenant-list/index.vue'
import License from '@/views/license/index.vue'
import OauthManagement from '@/views/oauth-management/index.vue'
import TenantUser from '@/views/tenant-user/index.vue'
import RoleManagement from '@/views/role-management/index.vue'
import OrgManagement from '@/views/org-management/index.vue'
import PersonalInfo from '@/views/personal-info/index.vue'
import Forbidden from '@/views/system/forbidden.vue'
import NoTenant from '@/views/system/no-tenant.vue'
import { getVipLicenseEnabled, isVipMenuCode } from '@/utils/vip-license'
import { useAuthStore } from '@/store/useAuth'
import HomeChildren from './home-children'

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
    return target ? {
 name: target.name 
} : {
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
    return authStore.userInfo?.systemAdmin ? {
 name: 'platform' 
} : {
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
 path: 'users', name: 'user-center', component: UserCenter 
},
            {
 path: 'tenants', name: 'tenant-list', component: TenantList 
},
            {
 path: 'license', name: 'license', component: License 
},
            {
 path: 'auth', name: 'oauth-management', component: OauthManagement 
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
 path: 'members', name: 'tenant-user', component: TenantUser 
},
            {
 path: 'roles', name: 'role-management', component: RoleManagement 
},
            {
 path: 'orgs', name: 'org-management', component: OrgManagement 
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

router.beforeEach(async(to) => {
    const authStore = useAuthStore()
    const routeName = typeof to.name === 'string' ? to.name : ''
    const openRouteName = new Set([ 'login', 'ssoauth', 'share', 'share-report' ])
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
