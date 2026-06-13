import type { RouteRecordRaw } from 'vue-router'

const TenantList = () => import('./views/index.vue')

const tenantListPlatformRoutes: RouteRecordRaw[] = [
    {
        path: 'tenants',
        name: 'platform-tenant-list',
        component: TenantList
    }
]

const tenantListWorkspaceRoutes: RouteRecordRaw[] = [
    {
        path: 'tenant-list',
        name: 'tenant-list',
        component: TenantList
    }
]

export default tenantListPlatformRoutes
export { tenantListPlatformRoutes, tenantListWorkspaceRoutes }
