<template>
    <div class="vm-list">
        <div class="vm-list__header">
            <span class="vm-list__title">实例列表</span>
            <div class="vm-list__ops">
                <el-icon class="vm-list__icon" @click="initVmListData">
                    <RefreshRight />
                </el-icon>
                <el-input
                    v-model="keyWord"
                    class="vm-list__search"
                    placeholder="作业流"
                    clearable
                    @keydown.enter="initVmListData"
                    @clear="initVmListData"
                />
            </div>
        </div>
        <div class="vm-list__body">
            <div ref="tableWrapRef" v-loading="tableLoading" class="vm-list__table-wrap">
                <BlockTable :table-config="tableConfig">
                    <template #workflowName="scopeSlot">
                        <span class="vm-list__workflow-name">
                            {{ scopeSlot.row.workflowName }}
                        </span>
                    </template>
                    <template #status="scopeSlot">
                        <ZStatusTag :status="scopeSlot.row.status" />
                    </template>
                    <template #lastModifiedBy="scopeSlot">
                        <person-tag :person-name="scopeSlot.row.lastModifiedBy" />
                    </template>
                    <template #duration="scopeSlot">
                        {{ formatDuration(scopeSlot.row.duration) }}
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group vm-list__action-group">
                            <el-dropdown trigger="click" popper-class="vm-list__action-dropdown">
                                <el-icon class="vm-list__more">
                                    <MoreFilled />
                                </el-icon>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item @click="showDagDetail(scopeSlot.row)">DAG</el-dropdown-item>
                                        <el-dropdown-item @click="reRunWorkFlowDataEvent(scopeSlot.row)">
                                            重跑
                                        </el-dropdown-item>
                                        <el-dropdown-item @click="deleteWorkflowSchedule(scopeSlot.row)">
                                            删除
                                        </el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                    </template>
                </BlockTable>
                <div v-if="loadMoreLoading" class="vm-list__load-state">加载中...</div>
            </div>
        </div>
        <dag-detail ref="dagDetailRef" />
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import BlockTable from '@/app/components/block-table/index.vue'
import PersonTag from './person-tag.vue'
import { ComputeInstance, queryComputeInstances } from '../services/computer-group'
import { DagDetail } from '@/modules/schedule/components'
import { ReRunWorkflow } from '@/modules/workflow/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DeleteWorkFlowScheduleLog } from '@/modules/schedule/api'

const keyWord = ref('')

const tableData = ref<Array<ComputeInstance>>([])
const tableConfig = reactive({
    tableData: tableData.value,
    colConfigs: [
        {
            prop: 'workflowName',
            title: '作业流',
            minWidth: 180,
            customSlot: 'workflowName',
            showOverflowTooltip: true
        },
        {
            prop: 'workflowInstanceId',
            title: '实例编码',
            minWidth: 220,
            showOverflowTooltip: true
        },
        {
            prop: 'status',
            title: '状态',
            minWidth: 100,
            customSlot: 'status'
        },
        {
            prop: 'lastModifiedBy',
            title: '发布人',
            minWidth: 180,
            customSlot: 'lastModifiedBy',
            showOverflowTooltip: true
        },
        {
            prop: 'startDateTime',
            title: '开始时间',
            minWidth: 170,
            showOverflowTooltip: true
        },
        {
            prop: 'endDateTime',
            title: '结束时间',
            minWidth: 170,
            showOverflowTooltip: true
        },
        {
            prop: 'duration',
            title: '耗时',
            minWidth: 100,
            customSlot: 'duration',
            showOverflowTooltip: true
        },
        {
            title: '操作',
            align: 'center',
            width: 120,
            customSlot: 'options'
        }
    ],
    columnResizable: false,
    showFixedLeftDivider: true,
    loading: false
})
const total = ref<number>(0)
const paginationInfo = ref<{
    pageSize: number
}>({
    pageSize: 10
})
const dagDetailRef = ref()
const timer = ref()
const tableWrapRef = ref<HTMLElement>()
const tableLoading = ref<boolean>(false)
const loadMoreLoading = ref(false)
const hasMore = computed(() => tableData.value.length < total.value)
let tableScrollEl: HTMLElement | null = null

function initVmListData() {
    queryVmlistData(true)
}

function queryVmlistData(reset = false, silent = false) {
    if (tableLoading.value || loadMoreLoading.value) {
        return
    }

    const pageSize = paginationInfo.value.pageSize
    const page = reset ? 0 : Math.floor(tableData.value.length / pageSize)

    if (!silent) {
        if (reset) {
            tableLoading.value = true
        } else {
            loadMoreLoading.value = true
        }
    }

    queryComputeInstances({
        page,
        pageSize,
        searchKeyWord: keyWord.value || null
    })
        .then(({ data }) => {
            tableData.value = reset ? data.content : [...tableData.value, ...data.content]
            tableConfig.tableData = tableData.value
            total.value = data.totalElements
        })
        .catch(() => {
            if (reset) {
                tableData.value = []
                tableConfig.tableData = []
                total.value = 0
            }
        })
        .finally(() => {
            tableLoading.value = false
            loadMoreLoading.value = false
            bindTableScroll()
        })
}

function refreshLoadedData() {
    if (tableLoading.value || loadMoreLoading.value || !tableData.value.length) {
        return
    }

    queryComputeInstances({
        page: 0,
        pageSize: Math.max(tableData.value.length, paginationInfo.value.pageSize),
        searchKeyWord: keyWord.value || null
    })
        .then(({ data }) => {
            tableData.value = data.content
            tableConfig.tableData = tableData.value
            total.value = data.totalElements
        })
        .catch(() => {})
}

function bindTableScroll() {
    nextTick(() => {
        const scrollEl = tableWrapRef.value?.querySelector('.vxe-table--body-wrapper') as HTMLElement | null
        if (!scrollEl || scrollEl === tableScrollEl) {
            return
        }

        tableScrollEl?.removeEventListener('scroll', handleTableScroll)
        tableScrollEl = scrollEl
        tableScrollEl.addEventListener('scroll', handleTableScroll, { passive: true })
    })
}

function handleTableScroll(event: Event) {
    const target = event.target as HTMLElement
    const distanceToBottom = target.scrollHeight - target.scrollTop - target.clientHeight
    if (distanceToBottom <= 24 && hasMore.value) {
        queryVmlistData()
    }
}

function formatDuration(duration?: number) {
    if (duration === null || duration === undefined) {
        return '-'
    }

    return `${duration}s`
}

// 展示工作流对应流程图
function showDagDetail(data: any) {
    dagDetailRef.value.showModal(data)
}

// 重跑工作流
function reRunWorkFlowDataEvent(data: any) {
    ReRunWorkflow({
        workflowInstanceId: data.workflowInstanceId
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            initVmListData()
        })
        .catch(() => {})
}

// 删除作业流调度
function deleteWorkflowSchedule(data: any) {
    ElMessageBox.confirm('确定删除该实例吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteWorkFlowScheduleLog({
            workflowInstanceId: data.workflowInstanceId
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                initVmListData()
            })
            .catch(() => {})
    })
}

onMounted(() => {
    initVmListData()
    bindTableScroll()
    timer.value = setInterval(() => {
        refreshLoadedData()
    }, 3000)
})

onUnmounted(() => {
    if (timer.value) {
        clearInterval(timer.value)
    }
    timer.value = null
    tableScrollEl?.removeEventListener('scroll', handleTableScroll)
    tableScrollEl = null
})
</script>

<style lang="scss">
.vm-list {
    margin-bottom: 24px;

    .vm-list__header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        height: 32px;
    }

    .vm-list__title {
        font-size: getCssVar('font-size', 'medium');
        font-weight: 600;
    }

    .vm-list__body {
        margin-top: 12px;
        border: 1px solid getCssVar('border-color', 'lighter');
        border-radius: 6px;
        background-color: getCssVar('color', 'white');
        box-shadow: 0 1px 4px rgb(15 23 42 / 4%);
        overflow: hidden;
    }

    .vm-list__table-wrap {
        position: relative;
        height: 420px;
        overflow: hidden;

        .block-table {
            height: 100%;
        }

        .vxe-table--render-wrapper,
        .vxe-table--main-wrapper,
        .vxe-table--body-wrapper {
            border-radius: 0;
        }

        .block-table.block-table__empty {
            .vxe-table--render-wrapper {
                min-height: 100%;
            }

            .vxe-table--empty-content {
                height: 376px;
            }

            .empty-page {
                width: 100%;
                right: auto;
                left: 0;
            }
        }
    }

    .vm-list__ops {
        display: flex;
        align-items: center;
        gap: 10px;
    }

    .vm-list__icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 28px;
        height: 28px;
        border-radius: 4px;
        cursor: pointer;
        color: getCssVar('text-color', 'regular');

        &:hover {
            color: getCssVar('color', 'primary');
            background-color: getCssVar('color', 'primary', 'light-9');
        }
    }

    .vm-list__search {
        width: 180px;
    }

    .vm-list__search.el-input .el-input__wrapper {
        border-radius: 2px;
    }

    .vm-list__workflow-name {
        color: getCssVar('text-color', 'regular');
    }

    .vm-list__action-group {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 16px;
        width: 100%;
    }

    .vm-list__more {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 24px;
        height: 24px;
        color: getCssVar('color', 'primary');
        cursor: pointer;
        transform: rotate(90deg);

        &:hover {
            color: getCssVar('color', 'primary', 'light-3');
        }
    }

    .vm-list__load-state {
        position: absolute;
        right: 0;
        bottom: 0;
        left: 0;
        display: flex;
        height: 36px;
        align-items: center;
        justify-content: center;
        background-color: rgb(255 255 255 / 88%);
        color: getCssVar('text-color', 'secondary');
        font-size: getCssVar('font-size', 'extra-small');
    }
}

.vm-list__action-dropdown {
    .el-dropdown-menu {
        padding: 4px 0;
    }

    .el-dropdown-menu__item {
        height: 26px;
        line-height: 26px;
        font-size: getCssVar('font-size', 'extra-small');
    }
}
</style>
