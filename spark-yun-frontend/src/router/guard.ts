import type { Router, RouteLocationRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCommercialEditionEnabled, isCommercialMenuCode } from '@edition'
import { useAuthStore } from '@/store/useAuth'

const openRouteName = new Set(['login', 'ssoauth', 'share', 'share-report'])

export function setupRouterGuard(router: Router, workspaceDefaultRoute: () => RouteLocationRaw): void {
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
        if (area === 'platform' && !authStore.userInfo?.platformAdmin) {
            return {
                name: 'forbidden'
            }
        }
        if (area === 'admin' && !authStore.userInfo?.tenantAdmin && !authStore.userInfo?.normalAdmin) {
            return {
                name: 'forbidden'
            }
        }
        if (area === 'workspace') {
            if (authStore.userInfo?.systemAdmin) {
                return {
                    name: 'forbidden'
                }
            }
            if (!authStore.tenantId) {
                return {
                    name: 'no-tenant'
                }
            }
        }

        if (!routeName || !isCommercialMenuCode(routeName)) {
            return true
        }
        const commercialEnabled = await getCommercialEditionEnabled()
        if (commercialEnabled) {
            return true
        }
        ElMessage.error('许可证未启用，无法访问商业版菜单')
        return workspaceDefaultRoute()
    })
}
