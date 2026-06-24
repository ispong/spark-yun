import { createI18n } from 'vue-i18n'
import elementZhCn from 'element-plus/es/locale/lang/zh-cn'
import elementEn from 'element-plus/es/locale/lang/en'
import type { Language } from 'element-plus/es/locale'
import { DEFAULT_LOCALE, resolveInitialLocale, type AppLocale } from './locales'
import { messages } from './messages'

export const i18n = createI18n({
    legacy: false,
    locale: resolveInitialLocale(),
    fallbackLocale: DEFAULT_LOCALE,
    messages,
    missingWarn: import.meta.env.DEV,
    fallbackWarn: import.meta.env.DEV
})

export function setI18nLocale(locale: AppLocale) {
    i18n.global.locale.value = locale
    document.documentElement.lang = locale
}

export function getElementLocale(locale: AppLocale): Language {
    return locale === 'en-US' ? elementEn : elementZhCn
}

setI18nLocale(i18n.global.locale.value as AppLocale)
