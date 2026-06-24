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
    fixed?: string
    showOverflowTooltip?: boolean
}

export const createBreadCrumbList = (t: (key: string) => string): BreadCrumb[] => [
    {
        name: t('userLog.title'),
        code: 'user-log'
    }
]

export const createColConfigs = (t: (key: string) => string): ColConfig[] => [
    {
        prop: 'account',
        title: t('table.account'),
        minWidth: 140,
        showOverflowTooltip: true
    },
    {
        prop: 'logType',
        title: t('table.type'),
        minWidth: 90,
        customSlot: 'logType'
    },
    {
        prop: 'tenantName',
        title: t('table.tenant'),
        minWidth: 140,
        customSlot: 'tenantName',
        showOverflowTooltip: true
    },
    {
        prop: 'moduleName',
        title: t('table.module'),
        minWidth: 120,
        showOverflowTooltip: true
    },
    {
        prop: 'apiName',
        title: t('table.apiName'),
        minWidth: 150,
        showOverflowTooltip: true
    },
    {
        prop: 'reqPath',
        title: t('table.requestPath'),
        minWidth: 220,
        showOverflowTooltip: true
    },
    {
        prop: 'reqMethod',
        title: t('table.requestMethod'),
        minWidth: 100
    },
    {
        prop: 'status',
        title: t('table.status'),
        minWidth: 90,
        customSlot: 'status'
    },
    {
        prop: 'duration',
        title: t('table.duration'),
        minWidth: 100,
        customSlot: 'duration'
    },
    {
        prop: 'ipAddress',
        title: 'IP',
        minWidth: 130,
        showOverflowTooltip: true
    },
    {
        prop: 'userAgent',
        title: 'User-Agent',
        minWidth: 220,
        showOverflowTooltip: true
    },
    {
        prop: 'createDateTime',
        title: t('table.callTime'),
        minWidth: 160,
        showOverflowTooltip: true
    },
    {
        title: t('table.actions'),
        width: 120,
        align: 'center',
        fixed: 'right',
        customSlot: 'options'
    }
]

export const createTableConfig = (t: (key: string) => string) => ({
    tableData: [],
    colConfigs: createColConfigs(t),
    pagination: {
        currentPage: 1,
        pageSize: 10,
        pageSizes: [10, 20, 50, 100],
        total: 0
    },
    seqType: 'seq',
    loading: false,
    columnResizable: false
})

export const BreadCrumbList: BreadCrumb[] = createBreadCrumbList((key) => key)
export const TableConfig = createTableConfig((key) => key)
