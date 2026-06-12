import type { RouteRecordRaw } from 'vue-router'

const UserCenter = () => import('./views/index.vue')

const userCenterPlatformRoutes: RouteRecordRaw[] = [
    {
        path: 'users',
        name: 'user-center',
        component: UserCenter
    }
]

const userCenterWorkspaceRoutes: RouteRecordRaw[] = [
    {
        path: 'user-center',
        name: 'user-center',
        component: UserCenter
    }
]

export default userCenterPlatformRoutes
export { userCenterPlatformRoutes, userCenterWorkspaceRoutes }
