import { reactive } from 'vue'

import defaultTopLogo from '@/app/assets/imgs/logo-a.png'
import defaultTopLogoSmall from '@/app/assets/imgs/logo.png'
import defaultLoginMainImage from '@/app/assets/imgs/logo-view.png'
import { GetOpenPlatformSetting, type PlatformSetting } from '@/app/management/platform-setting/api'

export interface BrandSetting {
    browserTitle: string
    themeColor: string
    faviconUrl: string
    topLogoUrl: string
    topLogoSmallUrl: string
    loginMainImageUrl: string
}

export const defaultBrandSetting: BrandSetting = {
    browserTitle: '至轻云',
    themeColor: '#f34c00',
    faviconUrl: '/favicon.ico',
    topLogoUrl: defaultTopLogo,
    topLogoSmallUrl: defaultTopLogoSmall,
    loginMainImageUrl: defaultLoginMainImage
}

export const brandSetting = reactive<BrandSetting>({ ...defaultBrandSetting })

let pendingBrandingPromise: Promise<void> | null = null

function valueOrDefault(value: string | undefined, defaultValue: string): string {
    return value && value.trim() ? value : defaultValue
}

export function normalizeBrandSetting(setting?: Partial<PlatformSetting>): BrandSetting {
    return {
        browserTitle: valueOrDefault(setting?.browserTitle, defaultBrandSetting.browserTitle),
        themeColor: normalizeThemeColor(setting?.themeColor),
        faviconUrl: valueOrDefault(setting?.faviconUrl, defaultBrandSetting.faviconUrl),
        topLogoUrl: valueOrDefault(setting?.topLogoUrl, defaultBrandSetting.topLogoUrl),
        topLogoSmallUrl: valueOrDefault(setting?.topLogoSmallUrl, defaultBrandSetting.topLogoSmallUrl),
        loginMainImageUrl: valueOrDefault(setting?.loginMainImageUrl, defaultBrandSetting.loginMainImageUrl)
    }
}

export function applyBrandSetting(setting: Partial<PlatformSetting> = {}): BrandSetting {
    const normalized = normalizeBrandSetting(setting)
    Object.assign(brandSetting, normalized)
    applyDocumentBranding(normalized)
    return normalized
}

export function loadBrandSetting(forceRefresh = false): Promise<void> {
    if (pendingBrandingPromise && !forceRefresh) {
        return pendingBrandingPromise
    }

    pendingBrandingPromise = GetOpenPlatformSetting()
        .then((res: any) => {
            applyBrandSetting(res.data || {})
        })
        .catch(() => {
            applyBrandSetting()
        })
        .finally(() => {
            pendingBrandingPromise = null
        })

    return pendingBrandingPromise
}

function applyDocumentBranding(setting: BrandSetting) {
    if (typeof document === 'undefined') {
        return
    }
    document.title = setting.browserTitle
    setFavicon(setting.faviconUrl)
    setThemeColor(setting.themeColor)
}

function setFavicon(iconUrl: string) {
    let link = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
    if (!link) {
        link = document.createElement('link')
        link.rel = 'icon'
        document.head.appendChild(link)
    }
    link.href = iconUrl
}

function normalizeThemeColor(color: string | undefined): string {
    const value = color?.trim()
    return value && /^#[0-9a-fA-F]{6}$/.test(value) ? value : defaultBrandSetting.themeColor
}

function setThemeColor(color: string) {
    const rootStyle = document.documentElement.style
    const rgb = hexToRgb(color)
    rootStyle.setProperty('--el-color-primary', color)
    rootStyle.setProperty('--el-color-primary-rgb', `${rgb.r}, ${rgb.g}, ${rgb.b}`)
    rootStyle.setProperty('--el-color-primary-dark-2', mixColor(color, '#000000', 0.2))
    Array.from({ length: 9 }, (_, index) => index + 1).forEach((level) => {
        rootStyle.setProperty(`--el-color-primary-light-${level}`, mixColor(color, '#ffffff', level / 10))
    })
}

function hexToRgb(hex: string): { r: number; g: number; b: number } {
    const normalized = normalizeThemeColor(hex).slice(1)
    return {
        r: parseInt(normalized.slice(0, 2), 16),
        g: parseInt(normalized.slice(2, 4), 16),
        b: parseInt(normalized.slice(4, 6), 16)
    }
}

function mixColor(color: string, target: string, weight: number): string {
    const sourceRgb = hexToRgb(color)
    const targetRgb = hexToRgb(target)
    const mixed = {
        r: Math.round(sourceRgb.r * (1 - weight) + targetRgb.r * weight),
        g: Math.round(sourceRgb.g * (1 - weight) + targetRgb.g * weight),
        b: Math.round(sourceRgb.b * (1 - weight) + targetRgb.b * weight)
    }
    return `#${toHex(mixed.r)}${toHex(mixed.g)}${toHex(mixed.b)}`
}

function toHex(value: number): string {
    return value.toString(16).padStart(2, '0')
}
