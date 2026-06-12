import type { RouteRecordRaw } from 'vue-router'

const License = () => import('./views/index.vue')

const licenseRoutes: RouteRecordRaw[] = [
    {
        path: 'license',
        name: 'license',
        component: License
    }
]

export default licenseRoutes
