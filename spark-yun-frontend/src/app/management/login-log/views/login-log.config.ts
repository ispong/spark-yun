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

export const BreadCrumbList: BreadCrumb[] = [
    {
        name: '登录日志',
        code: 'login-log'
    }
]

export const TableConfig = {
    tableData: [],
    colConfigs: [
        {
            prop: 'loginMethod',
            title: '登录方式',
            minWidth: 130,
            customSlot: 'loginMethod'
        },
        {
            prop: 'accountIdentifier',
            title: '账号标识',
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
            title: '设备',
            minWidth: 220,
            showOverflowTooltip: true
        },
        {
            prop: 'loginStatus',
            title: '结果',
            minWidth: 90,
            customSlot: 'loginStatus'
        },
        {
            prop: 'registered',
            title: '自动注册',
            minWidth: 100,
            customSlot: 'registered'
        },
        {
            prop: 'createDateTime',
            title: '登录时间',
            minWidth: 160,
            showOverflowTooltip: true
        },
        {
            prop: 'errorMessage',
            title: '失败原因',
            minWidth: 180,
            showOverflowTooltip: true
        }
    ] as ColConfig[],
    pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
    },
    seqType: 'seq',
    loading: false
}
