import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import { viteStaticCopy } from 'vite-plugin-static-copy'

import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { existsSync, readFileSync } from 'node:fs'

// 读取VERSION文件
const version = readFileSync('../VERSION', 'utf-8').trim()

const openSourceEditionRoot = fileURLToPath(new URL('./src/edition', import.meta.url))
const vipFrontendRoot = fileURLToPath(new URL('../spark-yun-vip/spark-yun-frontend', import.meta.url))
const vipEditionRoot = fileURLToPath(new URL('../spark-yun-vip/spark-yun-frontend/src/edition', import.meta.url))

function dependencyPath(pkg: string) {
  return fileURLToPath(new URL(`./node_modules/${pkg}`, import.meta.url))
}

function getEditionRoot(mode: string) {
  const isVipEdition = mode === 'vip' || process.env.VITE_EDITION === 'vip'
  return isVipEdition && existsSync(vipEditionRoot) ? vipEditionRoot : openSourceEditionRoot
}

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  define: {
    __APP_VERSION__: JSON.stringify(version)
  },
  server: {
    host: '0.0.0.0',
    fs: {
      allow: ['.', vipFrontendRoot]
    }
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
      dirs: ['src/app/components'],
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
      '@edition': getEditionRoot(mode),
      '@shared': fileURLToPath(new URL('./src/app/shared', import.meta.url)),
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
        additionalData: '@use "@/app/assets/styles/variable.scss" as *;'
      }
    }
  }
}))
