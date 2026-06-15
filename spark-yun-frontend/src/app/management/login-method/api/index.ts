import { http } from '@/app/utils/http'

export type LoginChannel = 'EMAIL' | 'PHONE'

export type LoginMethodType = 'ACCOUNT' | LoginChannel

export interface EmailLoginConfig {
    host?: string
    port?: number
    username?: string
    password?: string
    fromAddress?: string
    fromName?: string
    ssl?: boolean
    startTls?: boolean
    subject?: string
}

export interface PhoneLoginConfig {
    provider?: string
    regionId?: string
    accessKeyId?: string
    accessKeySecret?: string
    signName?: string
    templateCode?: string
    templateParamName?: string
}

export interface LoginMethodConfig {
    defaultLoginMethod: LoginMethodType
    accountEnabled: boolean
    accountPhonePasswordEnabled: boolean
    accountEmailPasswordEnabled: boolean
    emailEnabled: boolean
    emailRegisterEnabled: boolean
    phoneEnabled: boolean
    phoneRegisterEnabled: boolean
    autoCreateTenant: boolean
    config: {
        emailConfig: EmailLoginConfig
        phoneConfig: PhoneLoginConfig
    }
}

export function GetLoginMethodConfig(): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/platform/login-method/getConfig'
    })
}

export function UpdateLoginMethodConfig(params: LoginMethodConfig): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/platform/login-method/updateConfig',
        params
    })
}

export function PageLoginCodeRecord(params: {
    page: number
    pageSize: number
    searchKeyWord: string
    channel?: string
}): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/platform/login-method/pageRecord',
        params
    })
}

export function GetOpenLoginMethodConfig(): Promise<any> {
    return http.request(
        {
            method: 'post',
            url: '/login-method/open/getConfig'
        },
        {
            isShowErrorMessage: false
        }
    )
}

export function SendLoginCode(params: { channel: LoginChannel; receiver: string }): Promise<any> {
    return http.request({
        method: 'post',
        url: '/login-method/open/sendCode',
        params
    })
}

export function VerifyLoginCode(params: { channel: LoginChannel; receiver: string; code: string }): Promise<any> {
    return http.request({
        method: 'post',
        url: '/login-method/open/verifyLogin',
        params
    })
}
