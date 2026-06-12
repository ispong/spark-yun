import type { RouteRecordRaw } from 'vue-router'

const DataSource = () => import('./views/index.vue')

const datasourceRoutes: RouteRecordRaw[] = [
    {
        path: 'datasource',
        name: 'datasource',
        component: DataSource
    }
]

export default datasourceRoutes
