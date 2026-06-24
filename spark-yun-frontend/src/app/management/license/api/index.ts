/*
 * @Author: fanciNate
 * @Date: 2023-04-26 17:01:16
 * @LastEditTime: 2023-05-03 21:36:23
 * @LastEditors: fanciNate
 * @Description: In User Settings Edit
 * @FilePath: /zqy-web/src/services/computer-group.service.ts
 */
import { http } from '@/app/utils/http'
interface SerchParams {
    page: number
    pageSize: number
    searchKeyWord: string
}

interface LicenseIdParam {
    licenseId: string
}

export function GetLicenseList(params: SerchParams): Promise<any> {
    return http.request({
        method: 'post',
        url: '/vip/license/queryLicense',
        params: params
    })
}

// Upload license certificate.
export function UploadLicenseFile(params: any): Promise<any> {
    return http.uploadFile({
        method: 'post',
        url: '/vip/license/uploadLicense',
        params
    })
}

// Disable license.
export function DisableLicense(params: LicenseIdParam): Promise<any> {
    return http.request({
        method: 'get',
        url: '/vip/license/disableLicense',
        params: params
    })
}

// Enable license.
export function EnableLicense(params: LicenseIdParam): Promise<any> {
    return http.request({
        method: 'get',
        url: '/vip/license/enableLicense',
        params: params
    })
}

// Delete license.
export function DeleteLicense(params: LicenseIdParam): Promise<any> {
    return http.request({
        method: 'get',
        url: '/vip/license/deleteLicense',
        params: params
    })
}

// Check license status.
export function CheckLicenseStatus(): Promise<any> {
    return http.request(
        {
            method: 'get',
            url: '/vip/license/open/checkLicense'
        },
        {
            // Let the caller handle a 404 as an inactive license without a global toast.
            isShowErrorMessage: false
        }
    )
}
