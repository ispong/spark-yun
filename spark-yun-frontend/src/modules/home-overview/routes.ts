import type { RouteRecordRaw } from 'vue-router'

const HomeOverview = () => import('./views/index.vue')

const homeOverviewRoutes: RouteRecordRaw[] = [
    {
        path: 'index',
        name: 'index',
        component: HomeOverview
    }
]

export default homeOverviewRoutes
