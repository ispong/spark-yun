import type { RouteRecordRaw } from 'vue-router'

const CustomFunc = () => import('./views/index.vue')

const customFuncRoutes: RouteRecordRaw[] = [
    {
        path: 'custom-func',
        name: 'custom-func',
        component: CustomFunc
    }
]

export default customFuncRoutes
