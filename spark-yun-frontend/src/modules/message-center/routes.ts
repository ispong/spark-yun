import type { RouteRecordRaw } from 'vue-router'

const MessageNotifications = () => import('./views/message-notification/index.vue')
const WarningConfig = () => import('./views/warning-config/index.vue')
const WarningSchedule = () => import('./views/warning-schedule/index.vue')

const messageCenterRoutes: RouteRecordRaw[] = [
    {
        path: 'message-notifications',
        name: 'message-notifications',
        component: MessageNotifications
    },
    {
        path: 'warning-config',
        name: 'warning-config',
        component: WarningConfig
    },
    {
        path: 'warning-schedule',
        name: 'warning-schedule',
        component: WarningSchedule
    }
]

export default messageCenterRoutes
