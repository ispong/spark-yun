import type { RouteRecordRaw } from 'vue-router'

const LibPackage = () => import('./views/index.vue')

const libPackageRoutes: RouteRecordRaw[] = [
    {
        path: 'lib-package',
        name: 'lib-package',
        component: LibPackage
    }
]

export default libPackageRoutes
