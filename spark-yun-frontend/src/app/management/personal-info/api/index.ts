import { http } from '@/app/utils/http'

export interface UpdateUserInfoParams {
    username: string
    remark?: string
}

export function UpdateUserInfo(params: UpdateUserInfoParams): Promise<any> {
    return http.request({
        method: 'post',
        url: '/user/updateUserInfo',
        params: params
    })
}

export function SendUpdatePhoneCode(params: { phone: string }): Promise<any> {
    return http.request({
        method: 'post',
        url: '/user/sendUpdatePhoneCode',
        params
    })
}

export function UpdateMyPhone(params: { phone: string; code: string }): Promise<any> {
    return http.request({
        method: 'post',
        url: '/user/updateMyPhone',
        params
    })
}

export function SendUpdateEmailCode(params: { email: string }): Promise<any> {
    return http.request({
        method: 'post',
        url: '/user/sendUpdateEmailCode',
        params
    })
}

export function UpdateMyEmail(params: { email: string; code: string }): Promise<any> {
    return http.request({
        method: 'post',
        url: '/user/updateMyEmail',
        params
    })
}

export interface UpdateMyPasswordParams {
    oldPassword?: string
    verifyType?: 'OLD_PASSWORD' | 'PHONE' | 'EMAIL'
    code?: string
    newPassword: string
    confirmPassword: string
}

export function SendUpdatePasswordCode(params: { channel: 'PHONE' | 'EMAIL' }): Promise<any> {
    return http.request({
        method: 'post',
        url: '/user/sendUpdatePasswordCode',
        params
    })
}

export function UpdateMyPassword(params: UpdateMyPasswordParams): Promise<any> {
    return http.request({
        method: 'post',
        url: '/user/updateMyPassword',
        params: params
    })
}
