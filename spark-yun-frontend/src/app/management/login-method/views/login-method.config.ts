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
        name: '登录方式',
        code: 'login-method'
    }
]

export const TableConfig = {
    tableData: [],
    colConfigs: [
        {
            prop: 'channel',
            title: '方式',
            minWidth: 100,
            customSlot: 'channel'
        },
        {
            prop: 'receiver',
            title: '接收账号',
            minWidth: 160,
            showOverflowTooltip: true
        },
        {
            prop: 'sendStatus',
            title: '发送状态',
            minWidth: 100,
            customSlot: 'sendStatus'
        },
        {
            prop: 'verifyStatus',
            title: '验证状态',
            minWidth: 100,
            customSlot: 'verifyStatus'
        },
        {
            prop: 'registered',
            title: '自动注册',
            minWidth: 100,
            customSlot: 'booleanTag'
        },
        {
            prop: 'autoTenantCreated',
            title: '初始化租户',
            minWidth: 110,
            customSlot: 'tenantTag'
        },
        {
            prop: 'createDateTime',
            title: '发送时间',
            minWidth: 160,
            showOverflowTooltip: true
        },
        {
            prop: 'verifyDateTime',
            title: '验证时间',
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
