export interface BreadCrumb {
    name: string
    code: string
    hidden?: boolean
}

export interface colConfig {
    prop?: string
    title: string
    align?: string
    showOverflowTooltip?: boolean
    customSlot?: string
    width?: number
    minWidth?: number
    fixed?: string
}

export interface Pagination {
    currentPage: number
    pageSize: number
    total: number
}

export interface TableConfig {
    tableData: Array<any>
    colConfigs: Array<colConfig>
    seqType: string
    checkbox?: boolean
    pagination?: Pagination
    loading?: boolean
}

type Translate = (key: string) => string

const fallbackTranslate: Translate = (key: string) => key

export const createBreadCrumbList = (t: Translate = fallbackTranslate): Array<BreadCrumb> => [
    {
        name: t('oauthManagement.title'),
        code: 'oauth-management'
    }
]

export const createColConfigs = (t: Translate = fallbackTranslate): colConfig[] => [
    {
        prop: 'name',
        title: t('oauthManagement.name'),
        minWidth: 125,
        customSlot: 'name',
        showOverflowTooltip: true
    },
    {
        prop: 'ssoType',
        title: t('oauthManagement.type'),
        minWidth: 100,
        customSlot: 'ssoType'
    },
    {
        prop: 'clientId',
        title: 'clientId',
        minWidth: 110
    },
    {
        prop: 'status',
        title: t('oauthManagement.status'),
        minWidth: 100,
        customSlot: 'statusTag'
    },
    {
        prop: 'createDateTime',
        title: t('oauthManagement.createTime'),
        minWidth: 110
    },
    {
        prop: 'remark',
        title: t('oauthManagement.remark'),
        minWidth: 100,
        showOverflowTooltip: true
    },
    {
        title: t('oauthManagement.options'),
        align: 'center',
        customSlot: 'options',
        width: 120,
        fixed: 'right'
    }
]

export const createTableConfig = (t: Translate = fallbackTranslate): TableConfig => ({
    tableData: [],
    colConfigs: createColConfigs(t),
    checkbox: true,
    pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
    },
    seqType: 'seq',
    loading: false
})

export const BreadCrumbList: Array<BreadCrumb> = createBreadCrumbList()
export const colConfigs: colConfig[] = createColConfigs()
export const TableConfig: TableConfig = createTableConfig()
