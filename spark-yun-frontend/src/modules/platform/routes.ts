import type { RouteRecordRaw } from 'vue-router'
import { licenseRoutes } from '@/modules/license'
import { oauthManagementPlatformRoutes } from '@/modules/oauth-management'
import { tenantListPlatformRoutes } from '@/modules/tenant-list'
import { userCenterPlatformRoutes } from '@/modules/user-center'

const platformRoutes: RouteRecordRaw[] = [
    ...userCenterPlatformRoutes,
    ...tenantListPlatformRoutes,
    ...licenseRoutes,
    ...oauthManagementPlatformRoutes
]

export default platformRoutes
