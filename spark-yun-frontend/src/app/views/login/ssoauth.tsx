import { defineComponent, nextTick } from 'vue'
import { OauthLogin } from '@/app/api'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/app/store/useAuth'
import { useLocaleStore } from '@/app/store/useLocale'
import { getVipLicenseEnabled } from '@/app/utils/vip-license'
import { resolveLoginRoutePath } from './resolve-login-route'
import { UpdateMyLocale } from '@/app/management/personal-info/api'

export default defineComponent({
    setup() {
        const router = useRouter()
        const authStore = useAuthStore()
        const localeStore = useLocaleStore()
        const handleLogin = () => {
            const urlParams = new URLSearchParams(window.location.search)
            // 获取单个参数
            const code = urlParams.get('code') // "John"
            const clientId = urlParams.get('clientId')
            OauthLogin({
                code: code,
                clientId: clientId
            })
                .then((res: any) => {
                    authStore.applyAuthResponse(res.data)
                    const nextLocale = localeStore.applyUserLocale(res.data?.locale)
                    if (!res.data?.locale) {
                        UpdateMyLocale({ locale: nextLocale })
                            .then(() => {
                                authStore.setUserInfo({
                                    ...authStore.userInfo,
                                    locale: nextLocale
                                })
                            })
                            .catch(() => undefined)
                    }
                    const routePath = resolveLoginRoutePath(res.data)
                    if (!res.data.tenantId && (routePath === '/platform' || routePath.startsWith('/personal-info'))) {
                        ElMessage.success(res.msg)
                        nextTick(() => {
                            router.push(routePath)
                        })
                        return
                    }
                    getVipLicenseEnabled(true).finally(() => {
                        ElMessage.success(res.msg)
                        nextTick(() => {
                            router.push(routePath)
                        })
                    })
                })
                .catch((error: any) => {
                    console.error('请求失败，查看原因', error)
                    ElMessage.error('登录失败，参数缺失')
                })
        }
        handleLogin()
        return () => <div></div>
    }
})
