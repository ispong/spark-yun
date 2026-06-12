import type { RouteRecordRaw } from 'vue-router'

const Workflow = () => import('@/modules/workflow/views/index.vue')
const WorkflowDetail = () => import('@/modules/workflow/views/workflow-detail/index.vue')
const WorkflowPage = () => import('@/modules/workflow/views/workflow-page/index.vue')
const WorkItem = () => import('@/modules/workflow/views/work-item/index.vue')

const workflowRoutes: RouteRecordRaw[] = [
    {
        path: 'workflow',
        name: 'workflow',
        component: Workflow
    },
    {
        path: 'workflow-detail',
        name: 'workflow-detail',
        component: WorkflowDetail
    },
    {
        path: 'workflow-page',
        name: 'workflow-page',
        component: WorkflowPage
    },
    {
        path: 'work-item',
        name: 'work-item',
        component: WorkItem
    }
]

export default workflowRoutes
