import type { RouteRecordRaw } from 'vue-router'

const Schedule = () => import('./views/index.vue')

const scheduleRoutes: RouteRecordRaw[] = [
    {
        path: 'schedule',
        name: 'schedule',
        component: Schedule
    }
]

export default scheduleRoutes
