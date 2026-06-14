import type { RouteRecordRaw } from 'vue-router'

// 声明路由类型
type RouteList = RouteRecordRaw[]

// vue自动加载路由
const workspaceRouteModules = import.meta.glob<RouteList>('../../modules/*/routes.ts', {
    eager: true,
    import: 'default'
})

// 返回工作台路由
export const workspaceModuleRoutes: RouteRecordRaw[] = Object.keys(workspaceRouteModules).sort().flatMap(
    (path) => workspaceRouteModules[path] || []
)
