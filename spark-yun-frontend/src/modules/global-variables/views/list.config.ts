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
    formatter?: any
    fixed?: string
}

export interface Pagination {
    currentPage: number
    pageSize: number
    pageSizes?: number[]
    total: number
}

export interface TableConfig {
    tableData: Array<any>
    colConfigs: Array<colConfig>
    seqType: string
    checkbox?: boolean
    pagination?: Pagination // 分页数据
    loading?: boolean // 表格loading
}

export const BreadCrumbList: Array<BreadCrumb> = [
    {
        name: '全局变量',
        code: 'global-variables'
    }
]

export const colConfigs: colConfig[] = [
    {
        prop: 'keyName',
        title: '名称',
        minWidth: 125,
        showOverflowTooltip: true,
        customSlot: 'varName'
    },
    {
        prop: 'createUsername',
        title: '创建人',
        minWidth: 140
    },
    {
        prop: 'createDateTime',
        title: '创建时间',
        minWidth: 140
    },
    {
        prop: 'remark',
        title: '备注',
        minWidth: 100,
        showOverflowTooltip: true
    },
    {
        title: '操作',
        align: 'center',
        customSlot: 'options',
        width: 120,
        fixed: 'right'
    }
]

export const TableConfig: TableConfig = {
    tableData: [],
    colConfigs: colConfigs,
    checkbox: true,
    pagination: {
        currentPage: 1,
        pageSize: 10,
        pageSizes: [10, 20, 50, 100],
        total: 0
    },
    seqType: 'seq',
    loading: false
}
