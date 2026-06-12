import type { RouteRecordRaw } from 'vue-router'
import { licenseRoutes } from '@/app/management/license'
import { oauthManagementWorkspaceRoutes } from '@/app/management/oauth-management'
import { personalInfoWorkspaceRoutes } from '@/app/management/personal-info'
import { tenantListWorkspaceRoutes } from '@/app/management/tenant-list'
import { tenantUserWorkspaceRoutes } from '@/app/management/tenant-user'
import { userCenterWorkspaceRoutes } from '@/app/management/user-center'

const workspaceManagementRoutes: RouteRecordRaw[] = [
    ...tenantUserWorkspaceRoutes,
    ...userCenterWorkspaceRoutes,
    ...tenantListWorkspaceRoutes,
    ...oauthManagementWorkspaceRoutes,
    ...licenseRoutes,
    ...personalInfoWorkspaceRoutes
]

export default workspaceManagementRoutes
