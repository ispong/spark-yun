import { accessRuleRoutes } from '@/modules/access-rule'
import { computerGroupRoutes } from '@/modules/computer-group'
import { customApiRoutes } from '@/modules/custom-api'
import { customFormRoutes } from '@/modules/custom-form'
import { customFuncRoutes } from '@/modules/custom-func'
import { dataPlanningRoutes } from '@/modules/data-planning'
import { datasourceRoutes } from '@/modules/datasource'
import { driverManagementRoutes } from '@/modules/driver-management'
import { fileCenterRoutes } from '@/modules/file-center'
import { globalVariablesRoutes } from '@/modules/global-variables'
import { homeOverviewRoutes } from '@/modules/home-overview'
import { libPackageRoutes } from '@/modules/lib-package'
import { licenseRoutes } from '@/modules/license'
import { messageCenterRoutes } from '@/modules/message-center'
import { metadataPageRoutes } from '@/modules/metadata-page'
import { oauthManagementWorkspaceRoutes } from '@/modules/oauth-management'
import { personalInfoWorkspaceRoutes } from '@/modules/personal-info'
import { realtimeComputingRoutes } from '@/modules/realtime-computing'
import { reportRoutes } from '@/modules/report'
import { scheduleRoutes } from '@/modules/schedule'
import { sparkContainerRoutes } from '@/modules/spark-container'
import { tenantListWorkspaceRoutes } from '@/modules/tenant-list'
import { tenantUserWorkspaceRoutes } from '@/modules/tenant-user'
import { userCenterWorkspaceRoutes } from '@/modules/user-center'
import { workflowRoutes } from '@/modules/workflow'

export default [
    ...homeOverviewRoutes,
    ...computerGroupRoutes,
    ...datasourceRoutes,
    ...workflowRoutes,
    ...driverManagementRoutes,
    ...tenantUserWorkspaceRoutes,
    ...userCenterWorkspaceRoutes,
    ...tenantListWorkspaceRoutes,
    ...oauthManagementWorkspaceRoutes,
    ...licenseRoutes,
    ...fileCenterRoutes,
    ...scheduleRoutes,
    ...personalInfoWorkspaceRoutes,
    ...customFormRoutes,
    ...accessRuleRoutes,
    ...customApiRoutes,
    ...sparkContainerRoutes,
    ...customFuncRoutes,
    ...libPackageRoutes,
    ...realtimeComputingRoutes,
    ...reportRoutes,
    ...messageCenterRoutes,
    ...metadataPageRoutes,

    ...dataPlanningRoutes,
    ...globalVariablesRoutes
]
