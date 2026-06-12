import type { RouteRecordRaw } from 'vue-router'

const FileCenter = () => import('./views/index.vue')

const fileCenterRoutes: RouteRecordRaw[] = [
    {
        path: 'file-center',
        name: 'file-center',
        component: FileCenter
    }
]

export default fileCenterRoutes
