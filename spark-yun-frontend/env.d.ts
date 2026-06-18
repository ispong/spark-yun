declare const __APP_VERSION__: string

// 官方没有ts声明
declare module 'vue-grid-layout'

declare module '*.svg?raw' {
    const content: string
    export default content
}
