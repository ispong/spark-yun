<template>
    <vxe-table
        ref="vxeTableRef"
        class="block-table"
        :class="{ 'block-table__empty': !tableConfig.tableData?.length }"
        :row-config="{ isHover: true, drag: true }"
        :data="tableConfig.tableData"
        :seq-config="{ seqMethod }"
        :header-cell-config="{ height: 44 }"
        :cell-config="{ height: 40 }"
        :loading="tableConfig.loading"
        :max-height="'100%'"
        :row-drag-config="rowDragConfig"
        @row-dragend="rowDragendEvent"
    >
        <vxe-column v-if="tableConfig.seqType" :type="tableConfig.seqType" align="center" width="44" fixed="left">
            <template #header>#</template>
        </vxe-column>
        <template v-for="(colConfig, colIndex) in normalizedColConfigs">
            <vxe-column
                v-if="colConfig.customSlot"
                :key="colConfig.prop || colConfig.title || colIndex"
                :width="colConfig.width"
                :field="colConfig.prop"
                :fixed="colConfig.fixed"
                :resizable="colIndex < normalizedColConfigs.length - 1"
                :header-class-name="colConfig.headerClassName"
                :show-header-overflow="colConfig.showHeaderOverflow || false"
                :show-overflow="colConfig.showOverflowTooltip || true"
                :drag-sort="colConfig.dragSort"
                v-bind="colConfig"
            >
                <template v-if="colConfig.customHeaderSlot" #header>
                    <slot :name="colConfig.customHeaderSlot" />
                </template>
                <template #default="{ row, rowIndex, column }">
                    <slot
                        :name="colConfig.customSlot"
                        :row="row"
                        :index="rowIndex"
                        :column="columnSlotAdapter(column, colConfig)"
                        :col-index="colIndex"
                    />
                </template>
            </vxe-column>
            <vxe-column
                v-else
                :key="`${colConfig.prop || colConfig.title || colIndex}s`"
                :show-header-overflow="colConfig.showHeaderOverflow || false"
                :width="colConfig.width"
                :field="colConfig.prop"
                :resizable="colIndex < normalizedColConfigs.length - 1"
                :header-class-name="colConfig.headerClassName"
                :show-overflow="colConfig.showOverflowTooltip || true"
                :drag-sort="colConfig.dragSort"
                v-bind="colConfig"
            />
        </template>
        <template #empty>
            <!-- 空页面 -->
            <EmptyPage />
        </template>
    </vxe-table>
    <el-pagination
        v-if="tableConfig.pagination"
        class="pagination"
        popper-class="pagination-popper"
        background
        layout="prev, pager, next, total, jumper"
        :default-page-size="tableConfig.pagination.pageSize"
        :default-current-page="tableConfig.pagination.currentPage"
        :hide-on-single-page="false"
        :total="tableConfig.pagination.total || 0"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
    />
</template>

<script lang="ts" setup>
import { computed, defineProps, defineEmits, reactive, ref } from 'vue'
import EmptyPage from '@/app/components/empty-page/index.vue'
import type { VxeTablePropTypes } from 'vxe-table'

interface Pagination {
    currentPage: number
    pageSize: number
    total: number
}

interface colConfig {
    prop?: string
    title: string
    align?: string
    showOverflowTooltip?: boolean
    customSlot?: string
    minWidth?: number
    width?: number
    fixed?: 'left' | 'right' | boolean | string
    headerClassName?: string
    formatter?: () => string
}

interface TableConfig {
    tableData: Array<any>
    colConfigs: Array<colConfig>
    seqType?: string
    pagination?: Pagination // 分页数据
    loading?: boolean // 表格loading
}

const props = defineProps<{
    tableConfig: TableConfig
}>()

const emit = defineEmits(['size-change', 'current-change', 'rowDragendEvent'])

const vxeTableRef = ref<any>(null)

const normalizedColConfigs = computed(() => {
    const columns = props.tableConfig.colConfigs || []

    return columns.map((colConfig, colIndex) => {
        const headerClassNameList = [colConfig.headerClassName]

        if (colIndex === 0) {
            headerClassNameList.push('block-table__fixed-left-end')
        }

        if (colIndex === 1) {
            headerClassNameList.push('block-table__fixed-left-next')
        }

        const nextColConfig = {
            ...colConfig,
            headerClassName: headerClassNameList.filter(Boolean).join(' ')
        }

        if ('fixed' in colConfig) {
            return nextColConfig
        }

        if (colIndex === 0) {
            return {
                ...nextColConfig,
                fixed: 'left'
            }
        }

        if (colIndex === columns.length - 1) {
            return {
                ...nextColConfig,
                fixed: 'right'
            }
        }

        return nextColConfig
    })
})

const rowDragConfig = reactive<VxeTablePropTypes.RowDragConfig<RowVO>>({
    // icon: 'vxe-icon-sort',
    trigger: 'cell'
})

const handleSizeChange = (e: number) => {
    emit('size-change', e)
}
const handleCurrentChange = (e: number) => {
    emit('current-change', e)
}

function seqMethod({ rowIndex }): number {
    if (props.tableConfig && props.tableConfig.pagination) {
        return (props.tableConfig?.pagination.currentPage - 1) * props.tableConfig.pagination.pageSize + rowIndex + 1
    } else {
        return rowIndex + 1
    }
}

function columnSlotAdapter(column: any, colConfig: any) {
    return {
        property: column.property,
        title: colConfig.title,
        realWidth: column.renderWidth
    }
}

function rowDragendEvent(e: any) {
    emit('rowDragendEvent', vxeTableRef.value.getTableData())
}
</script>

<style lang="scss">
.block-table {
    max-height: 100%;
    &.block-table__empty {
        .vxe-table--render-wrapper {
            min-height: 176px;
        }
    }
    .vxe-table--render-wrapper {
        .vxe-table--layout-wrapper {
            .vxe-table--scroll-y-virtual {
                height: auto !important;
                max-width: 10px;
                .vxe-table--scroll-y-wrapper {
                    // max-height: calc(100% - 44px);
                    // .vxe-table--scroll-y-handle {
                    //   max-width: 10px;
                    // }
                }
            }
        }
    }
    .vxe-table--header tr.vxe-header--row > th {
        // height: getCssVar('menu', 'item-height');
        padding: 0;
        background-color: #fff;
    }
    .vxe-table--header tr.vxe-header--row > th.block-table__fixed-left-end {
        .vxe-cell--col-resizable {
            display: none;
        }
    }
    .vxe-table--header tr.vxe-header--row > th.block-table__fixed-left-next {
        position: relative;
        &::before {
            content: '';
            position: absolute;
            top: 25%;
            bottom: 25%;
            left: 0;
            width: 1px;
            pointer-events: none;
            background-color: var(--vxe-ui-table-resizable-line-color);
        }
    }
    .vxe-table--body-wrapper {
        .vxe-table--body-inner-wrapper {
            min-height: unset !important;
        }
    }
    .vxe-table--body tr > td.vxe-body--column {
        padding: 0;
        color: getCssVar('text-color', 'primary');
        border-bottom-color: getCssVar('border-color', 'lighter');
        .vxe-cell {
            font-size: getCssVar('font-size', 'extra-small');

            .name-click {
                cursor: pointer;
                font-weight: bold;
                color: getCssVar('color', 'primary', 'light-3');

                &:hover {
                    color: getCssVar('color', 'primary');
                    text-decoration: underline;
                }
            }
        }
    }
    .vxe-table--empty-content {
        height: 132px;
    }
    .vxe-loading {
        .vxe-loading--chunk {
            color: getCssVar('color', 'primary');
        }
    }
    .vxe-icon-sort {
        color: getCssVar('color', 'primary');
    }
    .vxe-body--row {
        transition: background-color 0.16s ease;
        &.row--hover {
            .vxe-body--column {
                background-color: getCssVar('fill-color', 'light');
                &.is--drag-cell {
                    color: getCssVar('color', 'primary') !important;
                }
            }
        }
    }
    .vxe-table--scroll-x-virtual {
        max-height: 10px;
        .vxe-table--scroll-x-wrapper {
            .vxe-table--scroll-x-handle {
                max-height: 10px;
            }
        }
    }
    .vxe-table--scroll-x-right-corner {
        max-width: 10px;
    }
    .vxe-table--fixed-wrapper {
        .vxe-table--fixed-left-wrapper {
            height: 100% !important;
            // .vxe-table--body-wrapper {
            //   &.fixed-left--wrapper {
            //     bottom: 0;
            //   }
            // }
        }
        .vxe-table--fixed-right-wrapper {
            height: 100% !important;
            // .vxe-table--body-wrapper {
            //   &.fixed-right--wrapper {
            //     bottom: 0;
            //   }
            // }
        }
    }
}
.vxe-table--tooltip-wrapper {
    &.is--active {
        &.is--visible {
            z-index: 3000 !important;
        }
    }
}
.pagination {
    display: flex;
    padding: 20px 0;
    margin-right: 0;
    justify-content: flex-end;

    &.el-pagination.is-background .btn-prev,
    &.el-pagination.is-background .btn-next,
    &.el-pagination.is-background .el-pager li {
        // background: #fff;
        border: getCssVar('border-color') solid 1px;
        &.active {
            background-color: getCssVar('color', 'primary');
        }
    }

    &.el-pagination.is-background .el-pager li.active:not(.disabled):hover {
        // color: #fff !important;
    }
}
.pagination-popper.el-select-dropdown {
    z-index: 6000 !important;
    min-width: 100px !important;

    .el-select-dropdown__item {
        text-align: center;
    }
}
</style>
