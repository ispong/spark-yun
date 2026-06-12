import type { RouteRecordRaw } from 'vue-router'
import { tenantUserAdminRoutes } from '@/app/management/tenant-user'
import { roleManagementAdminRoutes } from '@/app/management/role-management'
import { orgManagementAdminRoutes } from '@/app/management/org-management'

const adminRoutes: RouteRecordRaw[] = [
    ...tenantUserAdminRoutes,
    ...roleManagementAdminRoutes,
    ...orgManagementAdminRoutes
]

export default adminRoutes
