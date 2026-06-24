export const SUPPORTED_LOCALES = ['zh-CN', 'en-US'] as const

export type AppLocale = (typeof SUPPORTED_LOCALES)[number]

export const DEFAULT_LOCALE: AppLocale = 'zh-CN'

export const LOCALE_STORAGE_KEY = 'spark-yun-locale'

export function normalizeLocale(locale?: string | null): AppLocale | null {
    if (!locale) {
        return null
    }

    const normalized = locale.replace('_', '-').toLowerCase()
    if (normalized === 'zh-cn' || normalized === 'zh') {
        return 'zh-CN'
    }
    if (normalized === 'en-us' || normalized === 'en') {
        return 'en-US'
    }
    return null
}

export function getBrowserLocale(): AppLocale {
    if (typeof navigator === 'undefined') {
        return DEFAULT_LOCALE
    }

    const locale = [navigator.language, ...(navigator.languages || [])]
        .map((language) => normalizeLocale(language))
        .find((language): language is AppLocale => !!language)

    return locale || DEFAULT_LOCALE
}

export function getStoredLocale(): AppLocale | null {
    if (typeof localStorage === 'undefined') {
        return null
    }
    return normalizeLocale(localStorage.getItem(LOCALE_STORAGE_KEY))
}

export function saveStoredLocale(locale: AppLocale) {
    if (typeof localStorage !== 'undefined') {
        localStorage.setItem(LOCALE_STORAGE_KEY, locale)
    }
}

export function resolveInitialLocale(): AppLocale {
    return getStoredLocale() || getBrowserLocale()
}
