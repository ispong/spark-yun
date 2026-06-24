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
        name: t('license.title'),
        code: 'license'
    }
]

export const createColConfigs = (t: Translate = fallbackTranslate): colConfig[] => [
    {
        prop: 'code',
        title: t('license.certificateCode'),
        minWidth: 260,
        showOverflowTooltip: true
    },
    {
        prop: 'startDateTime',
        title: t('license.createTime'),
        minWidth: 110
    },
    {
        prop: 'endDateTime',
        title: t('license.expireTime'),
        minWidth: 110
    },
    {
        prop: 'maxMemberNum',
        title: t('license.maxMemberNum'),
        minWidth: 96
    },
    {
        prop: 'maxTenantNum',
        title: t('license.maxTenantNum'),
        minWidth: 96
    },
    {
        prop: 'maxWorkflowNum',
        title: t('license.maxWorkflowNum'),
        minWidth: 108
    },
    {
        prop: 'status',
        title: t('license.status'),
        minWidth: 100,
        customSlot: 'statusTag'
    },
    {
        prop: 'remark',
        title: t('license.remark'),
        minWidth: 100,
        showOverflowTooltip: true
    },
    {
        title: t('license.options'),
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
