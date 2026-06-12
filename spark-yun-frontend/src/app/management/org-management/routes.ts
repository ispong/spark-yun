import type { RouteRecordRaw } from 'vue-router'

const OrgManagement = () => import('./views/index.vue')

const orgManagementAdminRoutes: RouteRecordRaw[] = [
    {
        path: 'orgs',
        name: 'org-management',
        component: OrgManagement
    }
]

export default orgManagementAdminRoutes
