import type { LoginRes } from '@/app/type/models'

export function resolveLoginRoutePath(data: LoginRes): string {
    if (data.platformSuperAdmin || (data.platformAdmin && !data.tenantId)) {
        return '/platform'
    }

    if (data.defaultArea === 'workspace') {
        return '/workspace/index'
    }

    return data.defaultArea ? `/${data.defaultArea}` : '/workspace/index'
}
