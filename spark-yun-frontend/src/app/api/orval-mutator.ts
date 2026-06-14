import type { AxiosRequestConfig } from 'axios'

import type { Result } from '@/app/plugins/http-request/utils/types'
import { http } from '@/app/utils/http'

export function appRequest<T>(config: AxiosRequestConfig, options?: AxiosRequestConfig): Promise<Result<T>> {
    return http.request<Result<T>>({
        ...config,
        ...options,
        headers: {
            ...config.headers,
            ...options?.headers
        }
    })
}

export type ErrorType<Error> = Error
export type BodyType<BodyData> = BodyData
