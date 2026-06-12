import type { RouteRecordRaw } from 'vue-router'
import { licenseRoutes } from '@/app/management/license'
import { oauthManagementPlatformRoutes } from '@/app/management/oauth-management'
import { tenantListPlatformRoutes } from '@/app/management/tenant-list'
import { userCenterPlatformRoutes } from '@/app/management/user-center'

const platformRoutes: RouteRecordRaw[] = [
    ...userCenterPlatformRoutes,
    ...tenantListPlatformRoutes,
    ...licenseRoutes,
    ...oauthManagementPlatformRoutes
]

export default platformRoutes
