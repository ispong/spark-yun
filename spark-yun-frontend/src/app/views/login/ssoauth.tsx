import { defineComponent, nextTick } from 'vue'
import { OauthLogin } from '@/app/api'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/app/store/useAuth'
import { getVipLicenseEnabled } from '@/app/utils/vip-license'

export default defineComponent({
    setup() {
        const router = useRouter()
        const authStore = useAuthStore()
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
                    getVipLicenseEnabled(true).finally(() => {
                        ElMessage.success(res.msg)
                        nextTick(() => {
                            router.push(res.data.defaultArea === 'platform' ? '/platform' : '/workspace/index')
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
