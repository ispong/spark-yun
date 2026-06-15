import type { RouteRecordRaw } from 'vue-router'

const ZhiqingAi = () => import('./views/index.vue')

const routes: RouteRecordRaw[] = [
    {
        path: 'ai',
        name: 'zhiqing-ai',
        component: ZhiqingAi
    }
]

export default routes
