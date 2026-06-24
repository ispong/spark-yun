import { defineStore } from 'pinia'
import { setI18nLocale } from '@/app/i18n'
import {
    getStoredLocale,
    normalizeLocale,
    resolveInitialLocale,
    saveStoredLocale,
    type AppLocale
} from '@/app/i18n/locales'

interface LocaleState {
    locale: AppLocale
}

export const useLocaleStore = defineStore('localeStore', {
    state: (): LocaleState => ({
        locale: resolveInitialLocale()
    }),
    actions: {
        setLocale(locale: AppLocale): void {
            this.locale = locale
            saveStoredLocale(locale)
            setI18nLocale(locale)
        },
        applyUserLocale(locale?: string | null): AppLocale {
            const userLocale = normalizeLocale(locale)
            const nextLocale = userLocale || getStoredLocale() || this.locale
            this.setLocale(nextLocale)
            return nextLocale
        }
    },
    persist: true
})
