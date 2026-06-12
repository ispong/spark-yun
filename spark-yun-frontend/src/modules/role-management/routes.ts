import type { RouteRecordRaw } from 'vue-router'

const RoleManagement = () => import('./views/index.vue')

const roleManagementAdminRoutes: RouteRecordRaw[] = [
    {
        path: 'roles',
        name: 'role-management',
        component: RoleManagement
    }
]

export default roleManagementAdminRoutes
