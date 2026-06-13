import { computed, defineComponent } from 'vue'
import { adminMenuListData, platformMenuListData, workspaceMenuListData } from './menu.config'
import { useRouterMenu } from './hooks/use-router-menu'

import logoURLSmall from '@/app/assets/imgs/logo.png'
import logoURL from '@/app/assets/imgs/logo-a.png'
import './home.scss'
import { useAuthStore } from '@/app/store/useAuth'
import { useRoute } from 'vue-router'

export default defineComponent({
    setup() {
        const authStore = useAuthStore()
        const route = useRoute()
        const menuListData = computed(() => {
            if (route.path.startsWith('/platform')) {
                return platformMenuListData
            }
            if (route.path.startsWith('/admin')) {
                return adminMenuListData
            }
            return workspaceMenuListData
        })
        const { renderHomeMenu, isCollapse } = useRouterMenu(menuListData)

        const homeClass = computed<Record<string, boolean>>(() => ({
            'zqy-home': true,
            'is-collapse': isCollapse.value
        }))

        return () => (
            <div class={homeClass.value}>
                <div class="zqy-home__sidebar">
                    <div class="zqy-home__nav">
                        <img class="zqy-home__logo" src={isCollapse.value ? logoURLSmall : logoURL} alt="logo" />
                        {/* <span class="zqy-home__title">至轻云</span> */}
                    </div>
                    {renderHomeMenu()}
                </div>
                <div class="zqy-home__main">
                    <router-view key={authStore.tenantId}></router-view>
                </div>
            </div>
        )
    }
})
