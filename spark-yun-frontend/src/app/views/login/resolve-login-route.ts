import type { LoginRes } from '@/app/type/models'

export function resolveLoginRoutePath(data: LoginRes): string {
    const hasPlatformOrTenantAccess =
        !!data.platformSuperAdmin ||
        !!data.platformAdmin ||
        !!data.tenantSuperAdmin ||
        !!data.tenantAdmin ||
        !!data.tenantMember

    if (!hasPlatformOrTenantAccess) {
        return '/personal-info'
    }

    if (data.platformSuperAdmin || (data.platformAdmin && !data.tenantId)) {
        return '/platform'
    }

    if (data.defaultArea === 'workspace') {
        return '/workspace/index'
    }

    return data.defaultArea ? `/${data.defaultArea}` : '/workspace/index'
}
