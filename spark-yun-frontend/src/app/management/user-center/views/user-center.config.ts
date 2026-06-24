/*
 * @Author: fanciNate
 * @Date: 2023-06-04 22:27:08
 * @LastEditTime: 2026-01-14 22:32:55
 * @LastEditors: fancinate 1585546519@qq.com
 * @Description: In User Settings Edit
 * @FilePath: /spark-yun/spark-yun-website/src/views/user-center/user-center.config.ts
 */
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
    pagination?: Pagination // 分页数据
    loading?: boolean // 表格loading
}

export const BreadCrumbList: Array<BreadCrumb> = [
    {
        name: '用户中心',
        code: 'user-center'
    }
]

export function createBreadCrumbList(t: (key: string) => string): Array<BreadCrumb> {
    return [
        {
            name: t('userCenter.title'),
            code: 'user-center'
        }
    ]
}

export function createColConfigs(t: (key: string) => string): colConfig[] {
    return [
    {
        prop: 'account',
        title: t('userCenter.account'),
        minWidth: 100,
        customSlot: 'account',
        showOverflowTooltip: true
    },
    {
        prop: 'username',
        title: t('userCenter.name'),
        minWidth: 100,
        showOverflowTooltip: true
    },
    {
        prop: 'phone',
        title: t('userCenter.phone'),
        minWidth: 100,
        showOverflowTooltip: true
    },
    {
        prop: 'email',
        title: t('userCenter.email'),
        minWidth: 160,
        showOverflowTooltip: true
    },
    {
        prop: 'validStartDateTime',
        title: t('userCenter.validStartTime'),
        minWidth: 140,
        showOverflowTooltip: true
    },
    {
        prop: 'validEndDateTime',
        title: t('userCenter.validEndTime'),
        minWidth: 140,
        showOverflowTooltip: true
    },
    {
        prop: 'platformAdmin',
        title: t('userCenter.platformRole'),
        minWidth: 110,
        customSlot: 'platformAdmin'
    },
    {
        prop: 'status',
        title: t('userCenter.status'),
        minWidth: 100,
        customSlot: 'statusTag'
    },
    {
        prop: 'remark',
        title: t('userCenter.remark'),
        minWidth: 120,
        showOverflowTooltip: true
    },
    {
        title: t('userCenter.options'),
        align: 'center',
        customSlot: 'options',
        width: 120,
        fixed: 'right'
    }
    ]
}

export const TableConfig: TableConfig = {
    tableData: [],
    colConfigs: createColConfigs((key) => key),
    pagination: {
        currentPage: 1,
        pageSize: 10,
        pageSizes: [10, 20, 50, 100],
        total: 0
    },
    seqType: 'seq',
    checkbox: true,
    loading: false
}

export function createTableConfig(t: (key: string) => string): TableConfig {
    return {
        tableData: [],
        colConfigs: createColConfigs(t),
        pagination: {
            currentPage: 1,
            pageSize: 10,
            pageSizes: [10, 20, 50, 100],
            total: 0
        },
        seqType: 'seq',
        checkbox: true,
        loading: false
    }
}
