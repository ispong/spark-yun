import { defineStore } from 'pinia'
import { ref } from 'vue'
import i18n from '@/i18n'

export type LocaleType = 'zh-CN' | 'en-US'

export const useLocaleStore = defineStore('localeStore', () => {
  const locale = ref<LocaleType>((localStorage.getItem('locale') as LocaleType) || 'zh-CN')

  const setLocale = (newLocale: LocaleType) => {
    locale.value = newLocale
    i18n.global.locale.value = newLocale
    localStorage.setItem('locale', newLocale)
  }

  return {
    locale,
    setLocale
  }
}, {
  persist: true
})
