import type { RouteRecordRaw } from 'vue-router'

const DataLayer = () => import('./views/data-layer/index.vue')
const LayerArea = () => import('./views/data-layer/layer-area/index.vue')
const FieldFormat = () => import('./views/field-format/index.vue')
const DataModel = () => import('./views/data-model/index.vue')
const ModelField = () => import('./views/data-model/model-field/index.vue')

const dataPlanningRoutes: RouteRecordRaw[] = [
    {
        path: 'data-layer',
        name: 'data-layer',
        component: DataLayer
    },
    {
        path: 'layer-area',
        name: 'layer-area',
        component: LayerArea
    },
    {
        path: 'field-format',
        name: 'field-format',
        component: FieldFormat
    },
    {
        path: 'data-model',
        name: 'data-model',
        component: DataModel
    },
    {
        path: 'model-field',
        name: 'model-field',
        component: ModelField
    }
]

export default dataPlanningRoutes
