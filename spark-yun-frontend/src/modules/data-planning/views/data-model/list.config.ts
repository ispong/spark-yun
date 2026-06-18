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
    columnResizable?: boolean
    pagination?: Pagination // 分页数据
    loading?: boolean // 表格loading
}

export const BreadCrumbList: Array<BreadCrumb> = [
    {
        name: '数据模型',
        code: 'data-model'
    }
]

export const colConfigs: colConfig[] = [
    {
        prop: 'name',
        title: '名称',
        minWidth: 150,
        customSlot: 'nameSlot',
        showOverflowTooltip: true
    },
    {
        prop: 'modelType',
        title: '模型类型',
        minWidth: 120,
        formatter: (data: any) => {
            const obj: any = {
                ORIGIN_MODEL: '原始模型',
                LINK_MODEL: '关联模型'
            }
            return data.cellValue && obj[data.cellValue] ? obj[data.cellValue] : '-'
        },
        showOverflowTooltip: true
    },
    {
        prop: 'layerName',
        title: '分层路径',
        customSlot: 'layerNameSlot',
        minWidth: 180,
        showOverflowTooltip: true
    },
    {
        prop: 'datasourceName',
        title: '数据源',
        minWidth: 140,
        showOverflowTooltip: true
    },
    {
        prop: 'tableName',
        title: '表名',
        minWidth: 160,
        showOverflowTooltip: true
    },
    {
        prop: 'status',
        title: '状态',
        customSlot: 'statusTag',
        minWidth: 100,
        showOverflowTooltip: true
    },
    {
        prop: 'remark',
        title: '备注',
        minWidth: 140,
        showOverflowTooltip: true
    },
    {
        prop: 'createUsername',
        title: '创建人',
        minWidth: 120,
        showOverflowTooltip: true
    },
    {
        prop: 'createDateTime',
        title: '创建时间',
        minWidth: 150,
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
    loading: false
}
