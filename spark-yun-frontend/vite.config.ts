import {fileURLToPath, URL} from 'node:url'

import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'

import Components from 'unplugin-vue-components/vite'
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers'
import {existsSync, readFileSync} from 'node:fs'

const openSourceEditionRoot = fileURLToPath(new URL('./src/edition', import.meta.url))
const vipEditionRoot = fileURLToPath(new URL('../spark-yun-vip/spark-yun-frontend/src/edition', import.meta.url))
const vipFrontendRoot = fileURLToPath(new URL('../spark-yun-vip/spark-yun-frontend', import.meta.url))

function dependencyPath(pkg: string) {
    return fileURLToPath(new URL(`./node_modules/${pkg}`, import.meta.url))
}

function getEditionRoot() {
    return existsSync(vipFrontendRoot) ? vipEditionRoot : openSourceEditionRoot
}


// 负责启动部署，不需要外部传值
export default defineConfig({

    // 定义变量
    define: {
        __APP_VERSION__: JSON.stringify(readFileSync('../VERSION', 'utf-8').trim()),
    },

    // 本地启动
    server: {
        host: '0.0.0.0',
        fs: {
            allow: ['.', vipFrontendRoot] // 为了可能读取外部文件夹代码
        }
    },

    // 插件管理
    plugins: [
        vue(), // 解析vue文件
        vueJsx(), // 解析tsx文件
        Components({
            dirs: ['src/app/components'], // 自动扫描组件
            extensions: ['vue'],
            include: [/\.vue$/, /\.vue\?vue/, /\.tsx$/], // 解析组件文件
            resolvers: [ElementPlusResolver({
                importStyle: 'sass'  // 引入sass版本的element组件库，需要自定义样式
            })]
        })
    ],

    // 构建配置
    build: {
        outDir: 'dist', // 打包路径
        manifest: false, // 纯前端渲染，不需要文件结构
        rolldownOptions: {
            checks: { // rolldown打包检查
                invalidAnnotation: false, // 关闭无效注释打印
                pluginTimings: false // 关闭插件耗时打印
            }
        }
    },

    // 解析说明
    resolve: {
        alias: { // 起别名，方便引入
            '@': fileURLToPath(new URL('./src', import.meta.url)),
            '@edition': getEditionRoot(),
            'element-plus': dependencyPath('element-plus'),
            '@element-plus/icons-vue': dependencyPath('@element-plus/icons-vue'),
            '@codemirror/lang-json': dependencyPath('@codemirror/lang-json'),
            '@codemirror/lang-sql': dependencyPath('@codemirror/lang-sql'),
            clipboard: dependencyPath('clipboard'),
            'g6-extension-vue': fileURLToPath(new URL('./src/app/lib/g6-extension-vue.ts', import.meta.url)),
            '@antv/x6-vue-shape': fileURLToPath(new URL('./src/app/lib/x6-vue-shape.ts', import.meta.url)),
            '@antv/x6': '@antv/x6/lib',
        },
        dedupe: ['vue', 'vue-router', 'pinia']
    },

    // 样式说明
    css: {
        preprocessorOptions: {
            scss: {
                additionalData: '@use "@/app/assets/styles/variable.scss" as *;'
            }
        }
    }
})
