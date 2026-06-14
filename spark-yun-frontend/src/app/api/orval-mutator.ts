import type { AxiosRequestConfig } from 'axios'

import type { Result } from '@/app/plugins/http-request/utils/types'
import { http } from '@/app/utils/http'

export type AppResult<T> = Omit<Result<T>, 'data'> & {
    data: T
}

export function appRequest<T>(config: AxiosRequestConfig, options?: AxiosRequestConfig): Promise<AppResult<T>> {
    return http.request<AppResult<T>>({
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
