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

export const BreadCrumbList: BreadCrumb[] = [
    {
        name: '行为日志',
        code: 'user-log'
    }
]

export const TableConfig = {
    tableData: [],
    colConfigs: [
        {
            prop: 'account',
            title: '账号',
            minWidth: 140,
            showOverflowTooltip: true
        },
        {
            prop: 'logType',
            title: '类型',
            minWidth: 90,
            customSlot: 'logType'
        },
        {
            prop: 'tenantName',
            title: '租户',
            minWidth: 140,
            showOverflowTooltip: true
        },
        {
            prop: 'moduleName',
            title: '模块',
            minWidth: 120,
            showOverflowTooltip: true
        },
        {
            prop: 'apiName',
            title: '接口名称',
            minWidth: 150,
            showOverflowTooltip: true
        },
        {
            prop: 'reqPath',
            title: '请求路径',
            minWidth: 220,
            showOverflowTooltip: true
        },
        {
            prop: 'reqMethod',
            title: '请求方法',
            minWidth: 100
        },
        {
            prop: 'status',
            title: '状态',
            minWidth: 90,
            customSlot: 'status'
        },
        {
            prop: 'duration',
            title: '耗时',
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
            title: '调用时间',
            minWidth: 160,
            showOverflowTooltip: true
        },
        {
            title: '操作',
            width: 120,
            align: 'center',
            fixed: 'right',
            customSlot: 'options'
        }
    ] as ColConfig[],
    pagination: {
        currentPage: 1,
        pageSize: 10,
        pageSizes: [10, 20, 50, 100],
        total: 0
    },
    seqType: 'seq',
    loading: false,
    columnResizable: false
}
