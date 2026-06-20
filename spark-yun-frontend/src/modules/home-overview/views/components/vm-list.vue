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
                <el-table
                    ref="vmTableRef"
                    class="vm-list__table"
                    :class="{ 'vm-list__table-empty': !tableData.length }"
                    :data="tableData"
                    height="420"
                >
                    <el-table-column prop="workflowName" label="作业流" width="180" show-overflow-tooltip />
                    <el-table-column prop="status" label="状态">
                        <template #default="{ row }">
                            <ZStatusTag :status="row.status" />
                        </template>
                    </el-table-column>
                    <el-table-column prop="lastModifiedBy" label="发布人">
                        <template #default="{ row }">
                            <person-tag :person-name="row.lastModifiedBy" />
                        </template>
                    </el-table-column>
                    <el-table-column prop="startDateTime" label="开始时间" width="170" show-overflow-tooltip />
                    <el-table-column prop="endDateTime" label="结束时间" width="170" show-overflow-tooltip />
                    <el-table-column label="操作" align="center">
                        <template #default="{ row }">
                            <el-dropdown trigger="click">
                                <el-icon class="vm-list__more">
                                    <MoreFilled />
                                </el-icon>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item @click="showDagDetail(row)">DAG</el-dropdown-item>
                                        <el-dropdown-item @click="reRunWorkFlowDataEvent(row)">重跑</el-dropdown-item>
                                        <el-dropdown-item @click="deleteWorkflowSchedule(row)">删除</el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </template>
                    </el-table-column>
                    <template #empty>
                        <empty-page />
                    </template>
                </el-table>
                <div v-if="loadMoreLoading" class="vm-list__load-state">加载中...</div>
            </div>
        </div>
        <dag-detail ref="dagDetailRef" />
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import PersonTag from './person-tag.vue'
import { ComputeInstance, queryComputeInstances } from '../services/computer-group'
import { DagDetail } from '@/modules/schedule/components'
import { ReRunWorkflow } from '@/modules/workflow/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DeleteWorkFlowScheduleLog } from '@/modules/schedule/api'

const keyWord = ref('')

const tableData = ref<Array<ComputeInstance>>([])
const total = ref<number>(0)
const paginationInfo = ref<{
    pageSize: number
}>({
    pageSize: 10
})
const dagDetailRef = ref()
const timer = ref()
const tableWrapRef = ref<HTMLElement>()
const vmTableRef = ref()
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
            total.value = data.totalElements
        })
        .catch(() => {
            if (reset) {
                tableData.value = []
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
            total.value = data.totalElements
        })
        .catch(() => {})
}

function bindTableScroll() {
    nextTick(() => {
        const scrollEl = tableWrapRef.value?.querySelector('.el-scrollbar__wrap') as HTMLElement | null
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
        padding: 4px 20px 8px;
        background-color: getCssVar('color', 'white');
        box-shadow: 0 1px 4px rgb(15 23 42 / 4%);
    }

    .vm-list__table-wrap {
        overflow: hidden;
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

    .vm-list__table {
        --el-table-header-text-color: #{getCssVar('text-color', 'regular')};
        --el-table-text-color: #{getCssVar('text-color', 'regular')};

        &.vm-list__table-empty {
            .el-table__body-wrapper {
                min-height: 132px;
                .el-scrollbar__wrap {
                    min-height: 132px;
                }
            }
        }

        .el-table__inner-wrapper::before {
            display: none;
        }

        &.el-table th.el-table__cell.is-leaf,
        .el-table td.el-table__cell {
            border: 0;
        }

        &.el-table th.el-table__cell {
            font-weight: 600;
            background-color: getCssVar('fill-color', 'blank');
        }

        .el-table__row {
            height: 52px;
        }
    }

    .vm-list__more {
        transform: rotate(90deg);
        cursor: pointer;
        margin-top: 4px;

        &:hover {
            color: getCssVar('color', 'primary');
        }
    }

    .vm-list__empty {
        padding: 20px 0 0;
        --el-empty-image-width: 60px;

        .el-empty__description {
            margin-top: 0;
        }
    }

    .vm-list__load-state {
        display: flex;
        height: 36px;
        align-items: center;
        justify-content: center;
        color: getCssVar('text-color', 'secondary');
        font-size: getCssVar('font-size', 'extra-small');
    }
}
</style>
