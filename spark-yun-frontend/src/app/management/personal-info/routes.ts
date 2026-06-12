import type { RouteRecordRaw } from 'vue-router'

const PersonalInfo = () => import('./views/index.vue')

const personalInfoWorkspaceRoutes: RouteRecordRaw[] = [
    {
        path: 'personal-info',
        name: 'personalInfo',
        component: PersonalInfo
    }
]

const personalInfoRootRoute: RouteRecordRaw = {
    path: '/personal-info',
    name: 'personalInfo',
    component: PersonalInfo
}

export { personalInfoWorkspaceRoutes, personalInfoRootRoute }
export default personalInfoWorkspaceRoutes
