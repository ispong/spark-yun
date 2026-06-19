import { defineStore } from 'pinia'

interface AuthState {
    userInfo: Record<string, any>
    token: string
    tenantId: string
    role: string
    currentMenu: string
    isCollapse: boolean
}

interface AuthResponse {
    token?: string
    tenantId?: string
    role?: string
    [key: string]: any
}

const USER_PROFILE_FIELDS = ['id', 'username', 'account', 'phone', 'email', 'remark']

function getPreservedUserProfile(userInfo: Record<string, any>, data: AuthResponse): Record<string, any> {
    return USER_PROFILE_FIELDS.reduce((profile, field) => {
        if (data[field] === undefined && userInfo[field] !== undefined) {
            profile[field] = userInfo[field]
        }
        return profile
    }, {} as Record<string, any>)
}

export const useAuthStore = defineStore('authStore', {
    state: (): AuthState => ({
        userInfo: {},
        token: '',
        tenantId: '',
        role: '',
        currentMenu: '',
        isCollapse: false
    }),
    actions: {
        setUserInfo(this: AuthState, userInfo: Record<string, any>): void {
            this.userInfo = userInfo
        },
        setToken(this: AuthState, data: string): void {
            this.token = data
        },
        setTenantId(this: AuthState, tenantId: string): void {
            this.tenantId = tenantId
        },
        setRole(this: AuthState, role: string): void {
            this.role = role
        },
        setCurrentMenu(this: AuthState, menu: string): void {
            this.currentMenu = menu
        },
        setCollapse(this: AuthState, isCollapse: boolean): void {
            this.isCollapse = isCollapse
        },
        applyAuthResponse(this: AuthState, data: AuthResponse): void {
            this.userInfo = {
                ...getPreservedUserProfile(this.userInfo, data),
                ...data
            }
            this.token = data.token || ''
            this.tenantId = data.tenantId || ''
            this.role = data.role || ''
        }
    },
    persist: true
})
