import type { RouteRecordRaw } from 'vue-router'

const AccessRule = () => import('./views/index.vue')

const accessRuleRoutes: RouteRecordRaw[] = [
    {
        path: 'access-rule',
        name: 'access-rule',
        component: AccessRule
    }
]

export default accessRuleRoutes
