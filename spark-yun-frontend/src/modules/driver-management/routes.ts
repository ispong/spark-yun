import type { RouteRecordRaw } from 'vue-router'

const DriverManagement = () => import('./views/index.vue')

const driverManagementRoutes: RouteRecordRaw[] = [
    {
        path: 'driver-management',
        name: 'driver-management',
        component: DriverManagement
    }
]

export default driverManagementRoutes
