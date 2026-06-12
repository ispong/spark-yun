import type { RouteRecordRaw } from 'vue-router'

const AcquisitionTask = () => import('./views/acquisition-task/index.vue')
const AcquisitionInstance = () => import('./views/acquisition-instance/index.vue')
const MetadataManagement = () => import('./views/metadata-management/index.vue')

const metadataPageRoutes: RouteRecordRaw[] = [
    {
        path: 'acquisition-task',
        name: 'acquisition-task',
        component: AcquisitionTask
    },
    {
        path: 'acquisition-instance',
        name: 'acquisition-instance',
        component: AcquisitionInstance
    },
    {
        path: 'metadata-management',
        name: 'metadata-management',
        component: MetadataManagement
    }
]

export default metadataPageRoutes
