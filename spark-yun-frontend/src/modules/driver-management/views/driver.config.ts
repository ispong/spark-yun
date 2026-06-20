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
    pageSizes?: number[]
    total: number
}

export interface TableConfig {
    tableData: Array<any>
    colConfigs: Array<colConfig>
    seqType: string
    checkbox?: boolean
    columnResizable?: boolean
    showFixedLeftDivider?: boolean
    pagination?: Pagination // 分页数据
    loading?: boolean // 表格loading
}

export interface SerchParams {
    page: number
    pageSize: number
    searchKeyWord: string
}

export const BreadCrumbList: Array<BreadCrumb> = [
    {
        name: '数据源',
        code: 'datasource'
    },
    {
        name: '驱动管理',
        code: 'driver-management'
    }
]

export const colConfigs: colConfig[] = [
    {
        prop: 'name',
        title: '名称',
        minWidth: 120,
        customSlot: 'nameSlot',
        showOverflowTooltip: true
    },
    {
        prop: 'dbType',
        title: '类型',
        minWidth: 110,
        customSlot: 'dbTypeSlot',
        showOverflowTooltip: true
    },
    {
        prop: 'fileName',
        title: '驱动文件',
        minWidth: 180,
        showOverflowTooltip: true
    },
    {
        prop: 'defaultDriver',
        title: '默认',
        minWidth: 90,
        customSlot: 'defaultTag'
    },
    {
        prop: 'createUsername',
        title: '创建人',
        minWidth: 100,
        showOverflowTooltip: true
    },
    {
        prop: 'createDateTime',
        title: '创建时间',
        minWidth: 150,
        showOverflowTooltip: true
    },
    {
        prop: 'remark',
        title: '备注',
        minWidth: 140,
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
    pagination: {
        currentPage: 1,
        pageSize: 10,
        pageSizes: [10, 20, 50, 100],
        total: 0
    },
    seqType: 'seq',
    checkbox: true,
    columnResizable: false,
    showFixedLeftDivider: true,
    loading: false
}
