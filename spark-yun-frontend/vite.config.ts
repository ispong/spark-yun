import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { viteStaticCopy } from 'vite-plugin-static-copy'

import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { readFileSync } from 'node:fs'

// 读取VERSION文件
const version = readFileSync('../VERSION', 'utf-8').trim()

function getAntvChunk(id: string) {
  if (!id.includes('@antv/')) {
    return
  }

  if (id.includes('@antv/x6-vue-shape')) {
    return 'antv-x6-vue'
  }
  if (id.includes('@antv/x6')) {
    return 'antv-x6'
  }
  if (id.includes('@antv/g6')) {
    return 'antv-g6'
  }
  if (id.includes('@antv/layout') || id.includes('@antv/hierarchy') || id.includes('@antv/graphlib')) {
    return 'antv-layout'
  }
  if (
    id.includes('@antv/g') ||
    id.includes('@antv/g-') ||
    id.includes('@antv/component') ||
    id.includes('@antv/scale')
  ) {
    return 'antv-renderer'
  }
  if (id.includes('@antv/util') || id.includes('@antv/vendor') || id.includes('@antv/algorithm')) {
    return 'antv-utils'
  }

  return 'antv-common'
}

// https://vitejs.dev/config/
export default defineConfig({
  define: {
    __APP_VERSION__: JSON.stringify(version)
  },
  server: {
    host: '0.0.0.0'
  },
  plugins: [
    vue(),
    vueJsx(),
    viteStaticCopy({
      targets: [
        {
          src: 'public/*',
          dest: 'static'
        }
      ]
    }),
    Components({
      extensions: [ 'vue' ],
      include: [ /\.vue$/, /\.vue\?vue/, /\.md$/, /\.tsx$/, /\.jsx$/ ],
      resolvers: [ ElementPlusResolver({
        importStyle: 'sass'
      }) ]
    })
  ],
  build: {
    outDir: 'dist',
    assetsDir: 'static',
    copyPublicDir: false,
    manifest: false,
    // Large graph and chart libraries are lazy-loaded; keep the limit above the largest vendor chunk.
    chunkSizeWarningLimit: 1300,
    rolldownOptions: {
      checks: {
        invalidAnnotation: false,
        pluginTimings: false
      },
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }
          if (id.includes('@element-plus/icons-vue')) {
            return 'element-icons'
          }
          if (id.includes('element-plus')) {
            return 'element-plus'
          }
          if (id.includes('vxe-table')) {
            return 'vxe-table'
          }
          if (id.includes('vxe-pc-ui')) {
            return 'vxe-ui'
          }
          if (id.includes('xe-utils')) {
            return 'xe-utils'
          }
          const antvChunk = getAntvChunk(id)
          if (antvChunk) {
            return antvChunk
          }
          if (id.includes('zrender')) {
            return 'zrender'
          }
          if (id.includes('echarts')) {
            return 'echarts'
          }
          if (id.includes('@codemirror') || id.includes('codemirror')) {
            return 'codemirror'
          }
          if (id.includes('/ai/') || id.includes('@ai-sdk')) {
            return 'ai'
          }
          if (id.includes('/vue/') || id.includes('vue-router') || id.includes('pinia')) {
            return 'vue'
          }
          if (id.includes('lodash')) {
            return 'lodash'
          }
          return 'vendor'
        }
      }
    }
  },
  // optimizeDeps: {
  //   exclude: ['@antv/x6-vue-shape']
  // },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@core': fileURLToPath(new URL('./src', import.meta.url)),
      '@edition': fileURLToPath(new URL('./src/edition', import.meta.url)),
      '@shared': fileURLToPath(new URL('./src/shared', import.meta.url)),
      '@antv/x6': '@antv/x6/lib',
      '@antv/x6-vue-shape': '@antv/x6-vue-shape/lib'
    },
    dedupe: ['vue', 'vue-router', 'pinia']
    // alias: [
    //   {
    //     find: '@',
    //     replacement: fileURLToPath(new URL('./src', import.meta.url))
    //   },
    //   {
    //     find: '@antv/x6',
    //     replacement: '@antv/x6/dist/x6.js',
    //   },
    //   {
    //     find: '@antv/x6-vue-shape',
    //     replacement: '@antv/x6-vue-shape/lib',
    //   },
    // ]
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: '@use "@/assets/styles/variable.scss" as *;'
      }
    }
  }
})
