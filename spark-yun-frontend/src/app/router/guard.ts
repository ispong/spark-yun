import type {Router} from 'vue-router'
import {ElMessage} from 'element-plus'
import {getVipLicenseEnabled, isVipMenuCode} from '@/app/utils/vip-license'
import {useAuthStore} from '@/app/store/useAuth'
import {routeArea} from "@/app/router/index.ts";

// 开放路由
const openRouteName = new Set(['login', 'ssoauth', 'share', 'share-report'])

// 路由守卫，判断权限
export function setupRouterGuard(router: Router): void {

    router.beforeEach(async (to) => {

        const authStore = useAuthStore()
        const routeName = to.name as string

        // 开放路由直接放行
        if (openRouteName.has(routeName)) {
            return true
        }

        // 没有token，直接返回登录页面
        if (!authStore.token) {
            return {
                name: 'login'
            }
        }

        // 根据路由判断角色权限
        const area = to.meta.area as string
        if (authStore.userInfo?.platformSuperAdmin && to.meta.personalInfo) {
            return {
                name: 'forbidden'
            }
        }
        if (authStore.userInfo?.platformSuperAdmin && area !== routeArea.platform && routeName !== 'forbidden') {
            return {
                name: 'forbidden'
            }
        }

        switch (area) {
            case routeArea.platform:
                if (
                    !(
                        authStore.userInfo?.platformSuperAdmin ||
                        authStore.userInfo?.platformAdmin
                    )
                ) {
                    return {
                        name: 'forbidden'
                    }
                }
                break
            case routeArea.admin:

                // 没有租户id，直接退出
                if (!authStore.tenantId) {
                    return {
                        name: 'forbidden'
                    }
                }

                if (
                    !(
                        authStore.userInfo?.platformAdmin ||
                        authStore.userInfo?.tenantSuperAdmin ||
                        authStore.userInfo?.tenantAdmin
                    )
                ) {
                    return {
                        name: 'forbidden'
                    }
                }
                break
            case routeArea.workspace:

                // 没有租户id，直接退出
                if (!authStore.tenantId) {
                    return {
                        name: 'forbidden'
                    }
                }

                if (
                    !(
                        authStore.userInfo?.tenantSuperAdmin ||
                        authStore.userInfo?.tenantAdmin ||
                        authStore.userInfo?.tenantMember
                    )
                ) {
                    return {
                        name: 'forbidden'
                    }
                }
                break
        }

        // 开源菜单，直接放行
        if (!isVipMenuCode(routeName)) {
            return true
        }

        // 许可证菜单，需要检查许可证
        const commercialEnabled = await getVipLicenseEnabled()
        if (commercialEnabled) {
            return true
        }

        // 没有许可证报错
        ElMessage.error('请上传许可证')
    })
}
