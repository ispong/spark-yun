import { http } from '@/app/utils/http'

export interface PlatformSetting {
    description: string
    autoCreateTenant: boolean
    browserTitle?: string
    themeColor?: string
    faviconUrl?: string
    topLogoUrl?: string
    topLogoSmallUrl?: string
    loginMainImageUrl?: string
    userLogEnabled?: boolean
    userLogRetentionDays?: number
}

export function GetPlatformSetting(): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/platform/settings/getSetting'
    })
}

export function GetOpenPlatformSetting(): Promise<any> {
    return http.request(
        {
            method: 'post',
            url: '/platform-setting/open/getSetting'
        },
        {
            isShowErrorMessage: false
        }
    )
}

export function UpdatePlatformSetting(params: PlatformSetting): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/platform/settings/updateSetting',
        params
    })
}

export function UploadBrandImage(params: FormData): Promise<any> {
    return http.uploadFile({
        method: 'post',
        url: '/api/platform/settings/uploadBrandImage',
        data: params
    })
}
