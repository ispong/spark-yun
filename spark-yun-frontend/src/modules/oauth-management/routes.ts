import type { RouteRecordRaw } from 'vue-router'

const OauthManagement = () => import('./views/index.vue')

const oauthManagementWorkspaceRoutes: RouteRecordRaw[] = [
    {
        path: 'oauth-management',
        name: 'oauth-management',
        component: OauthManagement
    }
]

const oauthManagementPlatformRoutes: RouteRecordRaw[] = [
    {
        path: 'auth',
        name: 'oauth-management',
        component: OauthManagement
    }
]

export { oauthManagementWorkspaceRoutes, oauthManagementPlatformRoutes }
export default oauthManagementWorkspaceRoutes
