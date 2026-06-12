import { http } from '@/utils/http'

export function PageRole(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/roles/page',
        params
    })
}

export function ListRole(): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/roles/list'
    })
}

export function SaveRole(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/roles/save',
        params
    })
}

export function DeleteRole(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/roles/delete',
        params
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
