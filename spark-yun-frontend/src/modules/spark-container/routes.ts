import type { RouteRecordRaw } from 'vue-router'

const SparkContainer = () => import('./views/index.vue')

const sparkContainerRoutes: RouteRecordRaw[] = [
    {
        path: 'spark-container',
        name: 'spark-container',
        component: SparkContainer
    }
]

export default sparkContainerRoutes
