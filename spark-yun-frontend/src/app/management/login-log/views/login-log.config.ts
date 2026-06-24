export interface BreadCrumb {
    name: string
    code: string
}

export interface ColConfig {
    prop?: string
    title: string
    align?: string
    customSlot?: string
    minWidth?: number
    width?: number
    showOverflowTooltip?: boolean
}

export const createBreadCrumbList = (t: (key: string) => string): BreadCrumb[] => [
    {
        name: t('loginLog.title'),
        code: 'login-log'
    }
]

export const createColConfigs = (t: (key: string) => string): ColConfig[] => [
    {
        prop: 'loginMethod',
        title: t('loginLog.loginMethod'),
        minWidth: 130,
        customSlot: 'loginMethod'
    },
    {
        prop: 'accountIdentifier',
        title: t('loginLog.accountIdentifier'),
        minWidth: 140,
        showOverflowTooltip: true
    },
    {
        prop: 'ipAddress',
        title: 'IP',
        minWidth: 130,
        showOverflowTooltip: true
    },
    {
        prop: 'userAgent',
        title: t('loginLog.device'),
        minWidth: 220,
        showOverflowTooltip: true
    },
    {
        prop: 'loginStatus',
        title: t('loginLog.result'),
        minWidth: 90,
        customSlot: 'loginStatus'
    },
    {
        prop: 'registered',
        title: t('loginLog.autoRegister'),
        minWidth: 100,
        customSlot: 'registered'
    },
    {
        prop: 'createDateTime',
        title: t('loginLog.loginTime'),
        minWidth: 160,
        showOverflowTooltip: true
    },
    {
        prop: 'errorMessage',
        title: t('loginLog.failureReason'),
        minWidth: 180,
        customSlot: 'failureReason',
        showOverflowTooltip: true
    }
]

export const createTableConfig = (t: (key: string) => string) => ({
    tableData: [],
    colConfigs: createColConfigs(t),
    pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
    },
    seqType: 'seq',
    loading: false
})

export const BreadCrumbList: BreadCrumb[] = createBreadCrumbList((key) => key)
export const TableConfig = createTableConfig((key) => key)
