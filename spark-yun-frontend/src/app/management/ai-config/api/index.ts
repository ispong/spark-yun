import { http } from '@/app/utils/http'

export function PageAiConfig(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/ai-configs/page',
        params
    })
}

export function SaveAiConfig(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/ai-configs/save',
        params
    })
}

export function DeleteAiConfig(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/ai-configs/delete',
        params
    })
}

export function TestAiConfig(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/ai-configs/test',
        params
    })
}

export function ListEnabledAiConfig(): Promise<any> {
    return http.request({
        method: 'post',
        url: '/api/admin/ai-configs/listEnabled'
    })
}
