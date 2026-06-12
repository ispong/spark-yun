import { createApp } from 'vue'
import VXETable from 'vxe-table'
import VxeUIAll from 'vxe-pc-ui'
import VueGridLayout from 'vue-grid-layout'
import '@antv/x6-vue-shape'

import App from '@/App.vue'
import router from '@/router'
import pinia from '@/store'
import { registerGlobalIcons } from '@/app/icons'

import 'normalize.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import 'vxe-table/lib/style.css'
import 'vxe-pc-ui/lib/style.css'
import '@/assets/styles/global.scss'

export function bootstrapApp(): void {
    console.log(
        `%c 至轻云 %c ${__APP_VERSION__} `,
        'background:#35495e; padding: 1px; border-radius: 3px 0 0 3px; color: #fff',
        'background:#41b883; padding: 1px; border-radius: 0 3px 3px 0; color: #fff'
    )

    const app = createApp(App)

    registerGlobalIcons(app)

    app.use(VxeUIAll).use(VXETable).use(pinia).use(router).use(VueGridLayout).mount('#app')
}
