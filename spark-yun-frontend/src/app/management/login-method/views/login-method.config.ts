export const createBreadCrumbList = (t: (key: string) => string) => [
    {
        name: t('loginMethod.title'),
        code: 'login-method'
    }
]

export const BreadCrumbList = createBreadCrumbList((key) => key)
