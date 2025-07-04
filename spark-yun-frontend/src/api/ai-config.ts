import { http } from '@/utils/http'

interface SearchParams {
  page: number;
  size: number;
  searchKeyWord: string;
}

interface AiConfigForm {
  id?: string;
  name: string;
  modelType: string;
  apiUrl: string;
  apiKey: string;
  remark?: string;
}

interface IdParam {
  id: string;
}

// 分页查询AI配置
export function GetAiConfigList(params: SearchParams): Promise<any> {
  return http.request({
    method: 'post',
    url: '/ai-config/pageAiConfig',
    params: params
  })
}

// 添加AI配置
export function AddAiConfig(params: AiConfigForm): Promise<any> {
  return http.request({
    method: 'post',
    url: '/ai-config/addAiConfig',
    params: params
  })
}

// 更新AI配置
export function UpdateAiConfig(params: AiConfigForm): Promise<any> {
  return http.request({
    method: 'post',
    url: '/ai-config/updateAiConfig',
    params: params
  })
}

// 删除AI配置
export function DeleteAiConfig(params: IdParam): Promise<any> {
  return http.request({
    method: 'post',
    url: '/ai-config/deleteAiConfig',
    params: params
  })
}

// 启用AI配置
export function EnableAiConfig(params: IdParam): Promise<any> {
  return http.request({
    method: 'post',
    url: '/ai-config/enableAiConfig',
    params: params
  })
}

// 禁用AI配置
export function DisableAiConfig(params: IdParam): Promise<any> {
  return http.request({
    method: 'post',
    url: '/ai-config/disableAiConfig',
    params: params
  })
}

// 测试AI配置连接
export function TestAiConfig(params: AiConfigForm): Promise<any> {
  return http.request({
    method: 'post',
    url: '/ai-config/testAiConfig',
    params: params
  })
}
