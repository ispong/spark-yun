import { http } from '@/app/utils/http'

export function PageRole(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/roles/page',
        data: params
    })
}

export function ListRole(params: { tenantId?: string } = {}): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/roles/list',
        data: params
    })
}

export function SaveRole(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/roles/save',
        data: params
    })
}

export function DeleteRole(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/roles/delete',
        data: params
    })
}

export function GetPermissionCatalog(): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/roles/permissions'
    })
}

export function ListOrg(): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/orgs/list'
    })
}

export function SaveOrg(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/orgs/save',
        params
    })
}

export function DeleteOrg(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/orgs/delete',
        params
    })
}
