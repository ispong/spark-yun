type Translate = (key: string) => string

const fallbackTranslate: Translate = (key: string) => key

export const createBreadCrumbList = (t: Translate = fallbackTranslate) => [
    {
        name: t('platformSetting.title'),
        code: 'platform-setting'
    }
]

export const BreadCrumbList = createBreadCrumbList()
