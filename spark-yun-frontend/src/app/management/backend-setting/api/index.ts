import { http } from '@/app/utils/http'

export function GetTenant(params: { tenantId: string }): Promise<any> {
    return http.request({
        method: 'post',
        url: '/tenant/getTenant',
        params
    })
}

export function UpdateTenantForTenantAdmin(params: { id: string; name: string; introduce?: string }): Promise<any> {
    return http.request({
        method: 'post',
        url: '/tenant/updateTenantForTenantAdmin',
        data: params
    })
}
