import type { RouteRecordRaw } from 'vue-router'

const CustomForm = () => import('./views/index.vue')
const CustomFormList = () => import('./views/custom-form-list.vue')
const CustomFormQuery = () => import('./views/custom-form-query/index.vue')
const CustomFormSetting = () => import('./views/form-setting/index.vue')

const customFormRoutes: RouteRecordRaw[] = [
    {
        path: 'custom-form',
        name: 'custom-form',
        component: CustomForm,
        redirect: {
            name: 'form-list'
        },
        children: [
            {
                path: 'form-list',
                name: 'form-list',
                component: CustomFormList
            },
            {
                path: 'form-query',
                name: 'form-query',
                component: CustomFormQuery
            },
            {
                path: 'form-setting',
                name: 'form-setting',
                component: CustomFormSetting
            }
        ]
    }
]

export default customFormRoutes
