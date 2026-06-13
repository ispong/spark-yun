import type { RouteRecordRaw } from 'vue-router'

const ShareForm = () => import('./views/share-form-page/index.vue')

const shareFormRoute: RouteRecordRaw = {
    path: '/share/:shareParam',
    name: 'share',
    component: ShareForm
}

export default shareFormRoute
