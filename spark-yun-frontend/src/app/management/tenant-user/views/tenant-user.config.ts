/*
 * @Author: fanciNate
 * @Date: 2023-06-04 22:27:08
 * @LastEditTime: 2023-06-16 22:22:24
 * @LastEditors: fanciNate
 * @Description: In User Settings Edit
 * @FilePath: /spark-yun/spark-yun-website/src/views/tenant-user/tenant-user.config.ts
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

export const createBreadCrumbList = (t: (key: string) => string): Array<BreadCrumb> => [
    {
        name: t('tenantUser.title'),
        code: 'tenant-user'
    }
]

export const createColConfigs = (t: (key: string) => string): colConfig[] => [
    {
        prop: 'account',
        title: t('tenantUser.account'),
        minWidth: 100,
        showOverflowTooltip: true
    },
    {
        prop: 'username',
        title: t('tenantUser.name'),
        minWidth: 100,
        showOverflowTooltip: true
    },
    {
        prop: 'phone',
        title: t('tenantUser.phone'),
        minWidth: 100,
        showOverflowTooltip: true
    },
    {
        prop: 'email',
        title: t('tenantUser.email'),
        minWidth: 160,
        showOverflowTooltip: true
    },
    {
        prop: 'roleCode',
        title: t('tenantUser.role'),
        minWidth: 100,
        customSlot: 'roleCode'
    },
    {
        prop: 'status',
        title: t('tenantUser.status'),
        minWidth: 90,
        customSlot: 'status'
    },
    {
        title: t('tenantUser.options'),
        align: 'center',
        customSlot: 'options',
        width: 120,
        fixed: 'right'
    }
]

export const createTableConfig = (t: (key: string) => string): TableConfig => ({
    tableData: [],
    colConfigs: createColConfigs(t),
    pagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
    },
    seqType: 'seq',
    checkbox: true,
    loading: false
})

export const BreadCrumbList: Array<BreadCrumb> = createBreadCrumbList((key) => key)
export const colConfigs: colConfig[] = createColConfigs((key) => key)
export const TableConfig: TableConfig = createTableConfig((key) => key)
