const HomeOverview = () => import('@/views/home-overview/index.vue')
const ComputerGroup = () => import('@/views/computer-group/index.vue')
const ComputerPointer = () => import('@/views/computer-group/computer-pointer/index.vue')
const DataSource = () => import('@/views/datasource/index.vue')
const Workflow = () => import('@/views/workflow/index.vue')
const WorkflowDetail = () => import('@/views/workflow/workflow-detail/index.vue')
const GlobalVariables = () => import('@/views/global-variables/index.vue')
const WorkflowPage = () => import('@/views/workflow/workflow-page/index.vue')
const WorkItem = () => import('@/views/workflow/work-item/index.vue')
const Schedule = () => import('@/views/schedule/index.vue')
const UserCenter = () => import('@/views/user-center/index.vue')
const TenantList = () => import('@/views/tenant-list/index.vue')
const License = () => import('@/views/license/index.vue')
const TenantUser = () => import('@/views/tenant-user/index.vue')
const PersonalInfo = () => import('@/views/personal-info/index.vue')
const OauthManagement = () => import('@/views/oauth-management/index.vue')
const DriverManagement = () => import('@/views/driver-management/index.vue')
const CustomForm = () => import('@/views/custom-form/index.vue')
const CustomFormList = () => import('@/views/custom-form/custom-form-list.vue')
const CustomFormQuery = () => import('@/views/custom-form/custom-form-query/index.vue')
const CustomFormSetting = () => import('@/views/custom-form/form-setting/index.vue')
const AccessRule = () => import('@/views/access-rule/index.vue')
const CustomApi = () => import('@/views/custom-api/index.vue')
const SparkContainer = () => import('@/views/spark-container/index.vue')
const RealtimeComputing = () => import('@/views/realtime-computing/index.vue')
const ComputingDetail = () => import('@/views/realtime-computing/computing-detail/index.vue')
const FileCenter = () => import('@/views/file-center/index.vue')
const CustomFunc = () => import('@/views/custom-func/index.vue')
const LibPackage = () => import('@/views/lib-package/index.vue')
const ReportComponents = () => import('@/views/report-components/index.vue')
const ReportItem = () => import('@/views/report-components/report-item/index.vue')
const ReportViews = () => import('@/views/report-views/index.vue')
const ReportViewsDetail = () => import('@/views/report-views/report-views-detail/index.vue')
const MessageNotifications = () => import('@/views/message-center/message-notification/index.vue')
const WarningConfig = () => import('@/views/message-center/warning-config/index.vue')
const WarningSchedule = () => import('@/views/message-center/warning-schedule/index.vue')
const AcquisitionTask = () => import('@/views/metadata-page/acquisition-task/index.vue')
const AcquisitionInstance = () => import('@/views/metadata-page/acquisition-instance/index.vue')
const MetadataManagement = () => import('@/views/metadata-page/metadata-management/index.vue')
const DataLayer = () => import('@/views/data-planning/data-layer/index.vue')
const LayerArea = () => import('@/views/data-planning/data-layer/layer-area/index.vue')
const FieldFormat = () => import('@/views/data-planning/field-format/index.vue')
const DataModel = () => import('@/views/data-planning/data-model/index.vue')
const ModelField = () => import('@/views/data-planning/data-model/model-field/index.vue')

export default [
    {
        path: 'index',
        name: 'index',
        component: HomeOverview
    },
    {
        path: 'computer-group',
        name: 'computer-group',
        component: ComputerGroup
    },
    {
        path: 'computer-pointer',
        name: 'computer-pointer',
        component: ComputerPointer
    },
    {
        path: 'datasource',
        name: 'datasource',
        component: DataSource
    },
    {
        path: 'workflow',
        name: 'workflow',
        component: Workflow
    },
    {
        path: 'workflow-detail',
        name: 'workflow-detail',
        component: WorkflowDetail
    },
    {
        path: 'workflow-page',
        name: 'workflow-page',
        component: WorkflowPage
    },
    {
        path: 'driver-management',
        name: 'driver-management',
        component: DriverManagement
    },
    {
        path: 'work-item',
        name: 'work-item',
        component: WorkItem
    },
    {
        path: 'tenant-user',
        name: 'tenant-user',
        component: TenantUser
    },
    {
        path: 'user-center',
        name: 'user-center',
        component: UserCenter
    },
    {
        path: 'tenant-list',
        name: 'tenant-list',
        component: TenantList
    },
    {
        path: 'oauth-management',
        name: 'oauth-management',
        component: OauthManagement
    },
    {
        path: 'license',
        name: 'license',
        component: License
    },
    {
        path: 'file-center',
        name: 'file-center',
        component: FileCenter
    },
    {
        path: 'schedule',
        name: 'schedule',
        component: Schedule
    },
    {
        path: 'personal-info',
        name: 'personalInfo',
        component: PersonalInfo
    },
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
    },
    {
        path: 'access-rule',
        name: 'access-rule',
        component: AccessRule
    },
    {
        path: 'custom-api',
        name: 'custom-api',
        component: CustomApi
    },
    {
        path: 'spark-container',
        name: 'spark-container',
        component: SparkContainer
    },
    {
        path: 'custom-func',
        name: 'custom-func',
        component: CustomFunc
    },
    {
        path: 'lib-package',
        name: 'lib-package',
        component: LibPackage
    },
    {
        path: 'realtime-computing',
        name: 'realtime-computing',
        component: RealtimeComputing
    },
    {
        path: 'computing-detail',
        name: 'computing-detail',
        component: ComputingDetail
    },
    {
        path: 'report-components',
        name: 'report-components',
        component: ReportComponents
    },
    {
        path: 'report-item',
        name: 'report-item',
        component: ReportItem
    },
    {
        path: 'report-views',
        name: 'report-views',
        component: ReportViews
    },
    {
        path: 'report-views-detail',
        name: 'report-views-detail',
        component: ReportViewsDetail
    },
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
    },
    {
        path: 'acquisition-task',
        name: 'acquisition-task',
        component: AcquisitionTask
    },
    {
        path: 'acquisition-instance',
        name: 'acquisition-instance',
        component: AcquisitionInstance
    },
    {
        path: 'metadata-management',
        name: 'metadata-management',
        component: MetadataManagement
    },

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
    },
    {
        path: 'global-variables',
        name: 'global-variables',
        component: GlobalVariables
    }
]
