import type { RouteRecordRaw } from 'vue-router'

const ComputerGroup = () => import('./views/index.vue')
const ComputerPointer = () => import('./views/computer-pointer/index.vue')

const computerGroupRoutes: RouteRecordRaw[] = [
    {
        path: 'computer-group',
        name: 'computer-group',
        component: ComputerGroup
    },
    {
        path: 'computer-pointer',
        name: 'computer-pointer',
        component: ComputerPointer
    }
]

export default computerGroupRoutes
