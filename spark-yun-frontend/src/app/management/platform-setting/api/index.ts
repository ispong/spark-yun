import { http } from '@/app/utils/http'

export interface PlatformSetting {
    description: string
    autoCreateTenant: boolean
}

export function GetPlatformSetting(): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/platform/settings/getSetting'
    })
}

export function UpdatePlatformSetting(params: PlatformSetting): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/platform/settings/updateSetting',
        params
    })
}
