import { http } from '@/app/utils/http'

export interface PageUserLogParams {
    page: number
    pageSize: number
    searchKeyWord: string
    moduleCode?: string
    logType?: string
    account?: string
    tenantId?: string
    startDateTime?: string
    endDateTime?: string
}

export function PageUserLog(params: PageUserLogParams): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/platform/user-log/page',
        params
    })
}

export function GetUserLogDefinitions(): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/platform/user-log/definitions'
    })
}
