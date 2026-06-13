import type { RouteRecordRaw } from 'vue-router'

const ShareReport = () => import('./views/report-views/share-report/index.vue')

const shareReportRoute: RouteRecordRaw = {
    path: '/dashboard/:shareParam',
    name: 'share-report',
    component: ShareReport
}

export default shareReportRoute
