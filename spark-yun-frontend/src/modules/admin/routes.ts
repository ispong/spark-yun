import type { RouteRecordRaw } from 'vue-router'
import { tenantUserAdminRoutes } from '@/modules/tenant-user'
import { roleManagementAdminRoutes } from '@/modules/role-management'
import { orgManagementAdminRoutes } from '@/modules/org-management'

const adminRoutes: RouteRecordRaw[] = [
    ...tenantUserAdminRoutes,
    ...roleManagementAdminRoutes,
    ...orgManagementAdminRoutes
]

export default adminRoutes
