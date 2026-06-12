import type { RouteRecordRaw } from 'vue-router'

const GlobalVariables = () => import('./views/index.vue')

const globalVariablesRoutes: RouteRecordRaw[] = [
    {
        path: 'global-variables',
        name: 'global-variables',
        component: GlobalVariables
    }
]

export default globalVariablesRoutes
