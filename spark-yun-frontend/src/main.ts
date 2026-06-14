import { createApp } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import VXETable from 'vxe-table'
import VueGridLayout from 'vue-grid-layout'

import App from '@/App.vue'
import router from '@/app/router'
import pinia from '@/app/store'

import 'normalize.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import 'vxe-table/lib/style.css'
import '@/app/assets/styles/global.scss'

// 打印控制台版本信息
console.info(`至轻云 ${__APP_VERSION__}`)

// 绑定index中app组件
const app = createApp(App)

// 全局注册elementPlus的Icon
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

app.use(VXETable)
    .use(pinia)
    .use(router).use(VueGridLayout)
    .mount('#app')
