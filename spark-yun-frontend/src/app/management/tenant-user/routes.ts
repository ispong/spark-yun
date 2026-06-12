import type { RouteRecordRaw } from 'vue-router'

const TenantUser = () => import('./views/index.vue')

const tenantUserAdminRoutes: RouteRecordRaw[] = [
    {
        path: 'members',
        name: 'tenant-user',
        component: TenantUser
    }
]

const tenantUserWorkspaceRoutes: RouteRecordRaw[] = [
    {
        path: 'tenant-user',
        name: 'tenant-user',
        component: TenantUser
    }
]

export default tenantUserAdminRoutes
export { tenantUserAdminRoutes, tenantUserWorkspaceRoutes }
