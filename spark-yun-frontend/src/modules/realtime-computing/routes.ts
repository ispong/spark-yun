import type { RouteRecordRaw } from 'vue-router'

const RealtimeComputing = () => import('./views/index.vue')
const ComputingDetail = () => import('./views/computing-detail/index.vue')

const realtimeComputingRoutes: RouteRecordRaw[] = [
    {
        path: 'realtime-computing',
        name: 'realtime-computing',
        component: RealtimeComputing
    },
    {
        path: 'computing-detail',
        name: 'computing-detail',
        component: ComputingDetail
    }
]

export default realtimeComputingRoutes
