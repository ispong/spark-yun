import {fileURLToPath, URL} from 'node:url'

import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'

import ElementPlus from 'unplugin-element-plus/vite'
import Components from 'unplugin-vue-components/vite'
import {ElementPlusResolver} from 'unplugin-vue-components/resolvers'
import {readFileSync} from 'node:fs'

function patchRolldownVueInitPlugin() {
    const initFns = [
        'init_reactivity_esm_bundler',
        'init_runtime_core_esm_bundler',
        'init_runtime_dom_esm_bundler',
        'init_shared_esm_bundler'
    ]

    return {
        name: 'patch-rolldown-vue-init',
        augmentChunkHash(chunkInfo: { name: string }) {
            if (chunkInfo.name === 'vue-router' || chunkInfo.name === 'vue-vendor') {
                return 'patch-rolldown-vue-init-v1'
            }

            return null
        },
        generateBundle(_options: unknown, bundle: Record<string, any>) {
            Object.values(bundle).forEach((chunk) => {
                if (chunk.type !== 'chunk') {
                    return
                }

                const missingInitFns = initFns.filter(
                    (fn) => chunk.code.includes(`${fn}()`) && !chunk.code.includes(`function ${fn}`)
                )

                if (missingInitFns.length) {
                    chunk.code = `${missingInitFns.map((fn) => `var ${fn}=()=>{};`).join('')}${chunk.code}`
                }
            })
        }
    }
}

// 负责启动部署
export default defineConfig({

    // 定义变量
    define: {
        __APP_VERSION__: JSON.stringify(readFileSync('../VERSION', 'utf-8').trim()),
    },

    // 本地启动
    server: {
        host: '0.0.0.0'
    },

    optimizeDeps: {
        exclude: ['g6-extension-vue', '@antv/x6-vue-shape']
    },

    // 插件管理
    plugins: [
        vue(), // 解析vue文件
        vueJsx(), // 解析tsx文件
        ElementPlus({
            useSource: true // 使用sass源码，避免默认css覆盖自定义主题变量
        }), // 按需引入element-plus样式
        Components({
            dirs: ['src/app/components'], // 自动扫描组件
            extensions: ['vue'],
            include: [/\.vue$/, /\.vue\?vue/, /\.tsx$/], // 解析组件文件
            resolvers: [ElementPlusResolver({
                importStyle: 'sass'  // 引入sass版本的element组件库，需要自定义样式
            })]
        }),
        patchRolldownVueInitPlugin()
    ],

    // 构建配置
    build: {
        outDir: 'dist', // 打包路径
        manifest: false, // 纯前端渲染，不需要文件结构
        rolldownOptions: {
            checks: { // rolldown打包检查
                invalidAnnotation: false, // 关闭无效注释打印
                pluginTimings: false // 关闭插件耗时打印
            },
            output: {
                manualChunks(id) {
                    if (id.includes('/node_modules/vue/') || id.includes('/node_modules/@vue/') || id.includes('/vue-demi')) {
                        return 'vue-vendor'
                    }
                }
            }
        }
    },

    // 解析说明
    resolve: {
        alias: { // 起别名，方便引入
            '@': fileURLToPath(new URL('./src', import.meta.url)),
            'vue-demi': fileURLToPath(new URL('./src/app/utils/vue-demi.ts', import.meta.url))
        },
        dedupe: ['vue', 'vue-router', 'pinia'] // 防止重复加载
    },

    // 样式说明
    css: {
        preprocessorOptions: {
            scss: {
                additionalData: '@use "@/app/assets/styles/variable.scss" as *;' // 全局样式导入
            }
        }
    }
})
