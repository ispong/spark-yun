import type { RouteRecordRaw } from 'vue-router'

type RouteList = RouteRecordRaw[]
type ShareRouteModule = RouteRecordRaw | RouteRecordRaw[]

const workspaceRouteModules = import.meta.glob<RouteList>('../../modules/*/routes.ts', {
    eager: true,
    import: 'default'
})

const shareRouteModules = import.meta.glob<ShareRouteModule>('../../modules/*/share-routes.ts', {
    eager: true,
    import: 'default'
})

const workspaceModuleOrder = [
    'home-overview',
    'computer-group',
    'datasource',
    'workflow',
    'driver-management',
    'file-center',
    'schedule',
    'custom-form',
    'custom-api',
    'access-rule',
    'spark-container',
    'custom-func',
    'lib-package',
    'realtime-computing',
    'report',
    'message-center',
    'metadata-page',
    'data-planning',
    'global-variables'
]

function getModuleName(path: string): string {
    return path.match(/\/modules\/([^/]+)\//)?.[1] || path
}

function sortByWorkspaceOrder(paths: string[]): string[] {
    return paths.sort((left, right) => {
        const leftIndex = workspaceModuleOrder.indexOf(getModuleName(left))
        const rightIndex = workspaceModuleOrder.indexOf(getModuleName(right))
        const normalizedLeftIndex = leftIndex === -1 ? Number.MAX_SAFE_INTEGER : leftIndex
        const normalizedRightIndex = rightIndex === -1 ? Number.MAX_SAFE_INTEGER : rightIndex

        if (normalizedLeftIndex !== normalizedRightIndex) {
            return normalizedLeftIndex - normalizedRightIndex
        }

        return left.localeCompare(right)
    })
}

function toRouteList(moduleRoutes: ShareRouteModule): RouteRecordRaw[] {
    return Array.isArray(moduleRoutes) ? moduleRoutes : [moduleRoutes]
}

export const workspaceModuleRoutes: RouteRecordRaw[] = sortByWorkspaceOrder(Object.keys(workspaceRouteModules)).flatMap(
    (path) => workspaceRouteModules[path] || []
)

export const shareModuleRoutes: RouteRecordRaw[] = Object.keys(shareRouteModules)
    .sort()
    .flatMap((path) => toRouteList(shareRouteModules[path]))
