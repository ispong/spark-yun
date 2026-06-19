import type { LoginRes } from '@/app/type/models'

export function resolveLoginRoutePath(data: LoginRes): string {
    if (data.hasPassword === false) {
        return '/personal-info?tab=change-password'
    }

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
        return '/workspace/ai'
    }

    return data.defaultArea ? `/${data.defaultArea}` : '/workspace/ai'
}
