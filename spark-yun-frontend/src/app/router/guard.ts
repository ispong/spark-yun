import type { Router } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getVipLicenseEnabled, isVipMenuCode } from '@/app/utils/vip-license'
import { useAuthStore } from '@/app/store/useAuth'

const openRouteName = new Set(['login', 'ssoauth', 'share', 'share-report'])
const systemAdminRole = 'ROLE_SYS_ADMIN'
const platformAdminRole = 'ROLE_PLATFORM_ADMIN'
const tenantAdminRoles = new Set(['ROLE_TENANT_ADMIN', 'ROLE_TENANT_NORMAL_ADMIN'])

// 路由守卫，判断权限
export function setupRouterGuard(router: Router): void {

    router.beforeEach(async (to) => {

        const authStore = useAuthStore()
        const routeName = typeof to.name === 'string' ? to.name : ''

        if (openRouteName.has(routeName)) {
            return true
        }
        if (!authStore.token) {
            return {
                name: 'login'
            }
        }

        const area = to.meta.area
        if (
            area === 'platform' &&
            !(
                authStore.userInfo?.systemAdmin ||
                authStore.userInfo?.platformAdmin ||
                authStore.role === systemAdminRole ||
                authStore.role === platformAdminRole
            )
        ) {
            return {
                name: 'forbidden'
            }
        }
        if (
            area === 'admin' &&
            !(authStore.userInfo?.tenantAdmin || authStore.userInfo?.normalAdmin || tenantAdminRoles.has(authStore.role))
        ) {
            return {
                name: 'forbidden'
            }
        }
        if (area === 'workspace') {
            if (authStore.userInfo?.systemAdmin || authStore.role === systemAdminRole) {
                return {
                    name: 'forbidden'
                }
            }
            if (!authStore.tenantId) {
                return {
                    name: 'forbidden'
                }
            }
        }

        if (!routeName || !isVipMenuCode(routeName)) {
            return true
        }
        const commercialEnabled = await getVipLicenseEnabled()
        if (commercialEnabled) {
            return true
        }
        ElMessage.error('许可证未启用，无法访问商业版菜单');
    })
}
