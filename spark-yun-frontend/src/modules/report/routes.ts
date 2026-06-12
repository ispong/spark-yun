import type { RouteRecordRaw } from 'vue-router'

const ReportComponents = () => import('./views/report-components/index.vue')
const ReportItem = () => import('./views/report-components/report-item/index.vue')
const ReportViews = () => import('./views/report-views/index.vue')
const ReportViewsDetail = () => import('./views/report-views/report-views-detail/index.vue')
const ShareReport = () => import('./views/report-views/share-report/index.vue')

const reportRoutes: RouteRecordRaw[] = [
    {
        path: 'report-components',
        name: 'report-components',
        component: ReportComponents
    },
    {
        path: 'report-item',
        name: 'report-item',
        component: ReportItem
    },
    {
        path: 'report-views',
        name: 'report-views',
        component: ReportViews
    },
    {
        path: 'report-views-detail',
        name: 'report-views-detail',
        component: ReportViewsDetail
    }
]

const shareReportRoute: RouteRecordRaw = {
    path: '/dashboard/:shareParam',
    name: 'share-report',
    component: ShareReport
}

export { reportRoutes, shareReportRoute }
export default reportRoutes
