import { http } from '@/app/utils/http'

interface SearchParams {
    page: number
    pageSize: number
    searchKeyWord: string
    datasourceType?: string
    type?: string
}

interface OptionalSearchParams {
    page: number
    pageSize: number
    searchKeyWord?: string
}

export function GetDatasourceList(params: SearchParams): Promise<any> {
    return http.request({
        method: 'post',
        url: '/datasource/pageDatasource',
        params
    })
}

export function GetComputerGroupList(params: SearchParams): Promise<any> {
    return http.request({
        method: 'post',
        url: '/cluster/pageCluster',
        params
    })
}

export function GetComputerPointData(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/cluster-node/pageClusterNode',
        params
    })
}

export function GetComputerPointDetailData(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/cluster-node/getClusterNode',
        params
    })
}

export function GetFileCenterList(params: SearchParams): Promise<any> {
    return http.request({
        method: 'post',
        url: '/file/pageFile',
        params
    })
}

export function GetCustomFuncList(params: SearchParams): Promise<any> {
    return http.request({
        method: 'post',
        url: '/func/pageFunc',
        params
    })
}

export function GetSparkContainerList(params: SearchParams): Promise<any> {
    return http.request({
        method: 'post',
        url: '/vip/container/pageContainer',
        params
    })
}

export function GetAlarmPagesList(params: any): Promise<any> {
    return http.request({
        method: 'post',
        url: '/alarm/pageAlarm',
        params
    })
}

export function PageLibPackage(params: OptionalSearchParams): Promise<any> {
    return http.request({
        method: 'post',
        url: '/file/pageLibPackage',
        params
    })
}
