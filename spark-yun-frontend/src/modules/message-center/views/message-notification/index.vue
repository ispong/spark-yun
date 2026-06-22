<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table message-notification-page">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addData">新增消息体</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入搜索条件 回车进行搜索"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(false)"
                />
            </div>
            <Transition name="message-batch-slide">
                <div v-if="selectedRows.length" class="message-batch-mask">
                    <div class="message-batch-actions">
                        <el-button class="message-batch-action" :loading="batchLoading" @click="batchEnableMessages">
                            启用
                        </el-button>
                        <el-button class="message-batch-action" :loading="batchLoading" @click="batchDisableMessages">
                            禁用
                        </el-button>
                        <el-button class="message-batch-action" :loading="batchLoading" @click="batchDeleteMessages">
                            删除
                        </el-button>
                        <el-button class="message-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
                            取消选择
                        </el-button>
                    </div>
                </div>
            </Transition>
        </div>
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData(false)">
            <div class="zqy-table">
                <BlockTable
                    :table-config="tableConfig"
                    @size-change="handleSizeChange"
                    @current-change="handleCurrentChange"
                    @checkbox-change="handleSelectionChange"
                >
                    <template #name="scopeSlot">
                        <span class="name-click" @click="editData(scopeSlot.row)">{{ scopeSlot.row.name }}</span>
                    </template>
                    <template #msgTypeTag="scopeSlot">
                        <el-tag
                            type="primary"
                            effect="light"
                            class="message-type-tag"
                            :class="`message-type-tag--${scopeSlot.row.msgType || 'default'}`"
                        >
                            {{ getMsgTypeName(scopeSlot.row.msgType) }}
                        </el-tag>
                    </template>
                    <template #statusTag="scopeSlot">
                        <div class="btn-group message-status-group">
                            <ZStatusTag
                                :status="
                                    scopeSlot.row.status == 'CHECK_FAIL'
                                        ? 'CHECK_ERROR'
                                        : scopeSlot.row.status == 'ACTIVE'
                                          ? 'ENABLE'
                                          : scopeSlot.row.status
                                "
                            />
                            <el-popover
                                placement="right"
                                title="响应信息"
                                :width="400"
                                trigger="hover"
                                popper-class="message-error-tooltip"
                                :content="scopeSlot.row.response"
                            >
                                <template #reference>
                                    <el-icon v-if="scopeSlot.row.response" class="hover-tooltip">
                                        <Warning />
                                    </el-icon>
                                </template>
                            </el-popover>
                        </div>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group message-action-group">
                            <span class="message-action-button" @click="editData(scopeSlot.row)">编辑</span>
                            <el-dropdown trigger="click" popper-class="message-action-dropdown">
                                <span class="click-show-more message-action-button">更多</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item @click="checkData(scopeSlot.row)">检测</el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="['CHECK_SUCCESS', 'DISABLE'].includes(scopeSlot.row.status)"
                                            :disabled="scopeSlot.row.statusLoading"
                                            @click="!scopeSlot.row.statusLoading && changeStatus(scopeSlot.row, true)"
                                        >
                                            <span v-if="!scopeSlot.row.statusLoading">启动</span>
                                            <el-icon v-else class="is-loading">
                                                <Loading />
                                            </el-icon>
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="['ACTIVE'].includes(scopeSlot.row.status)"
                                            :disabled="scopeSlot.row.statusLoading"
                                            @click="!scopeSlot.row.statusLoading && changeStatus(scopeSlot.row, false)"
                                        >
                                            <span v-if="!scopeSlot.row.statusLoading">禁用</span>
                                            <el-icon v-else class="is-loading">
                                                <Loading />
                                            </el-icon>
                                        </el-dropdown-item>
                                        <el-dropdown-item @click="deleteData(scopeSlot.row)">删除</el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                    </template>
                </BlockTable>
            </div>
        </LoadingPage>
        <AddModal ref="addModalRef" />
        <CheckModal ref="checkModalRef" />
    </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'
import CheckModal from './check-modal/index.vue'

import { BreadCrumbList, TableConfig } from './message-notification.config'
import {
    GetMessagePagesList,
    AddMessageData,
    UpdateMessageData,
    DeleteMessageData,
    EnableMessageData,
    DisabledMessageData,
    CheckMessageData
} from '../../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)
const addModalRef = ref(null)
const checkModalRef = ref(null)

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetMessagePagesList({
        page: tableConfig.pagination.currentPage - 1,
        pageSize: tableConfig.pagination.pageSize,
        searchKeyWord: keyword.value
    })
        .then((res: any) => {
            tableConfig.tableData = res.data.content
            tableConfig.pagination.total = res.data.totalElements
            selectedRows.value = []
            loading.value = false
            tableConfig.loading = false
            networkError.value = false
        })
        .catch(() => {
            tableConfig.tableData = []
            tableConfig.pagination.total = 0
            loading.value = false
            tableConfig.loading = false
            networkError.value = true
        })
}

function addData() {
    addModalRef.value.showModal((data: any) => {
        return new Promise((resolve: any, reject: any) => {
            AddMessageData(data)
                .then((res: any) => {
                    ElMessage.success(res.msg)
                    initData()
                    resolve()
                })
                .catch((error: any) => {
                    reject(error)
                })
        })
    })
}
function editData(data: any) {
    addModalRef.value.showModal((data: any) => {
        return new Promise((resolve: any, reject: any) => {
            UpdateMessageData(data)
                .then((res: any) => {
                    ElMessage.success(res.msg)
                    initData()
                    resolve()
                })
                .catch((error: any) => {
                    reject(error)
                })
        })
    }, data)
}

// 检测
function checkData(data: any) {
    checkModalRef.value.showModal((data: any) => {
        return new Promise((resolve: any, reject: any) => {
            CheckMessageData(data)
                .then((res: any) => {
                    ElMessage.success(res.msg)
                    initData()
                    resolve()
                })
                .catch((error: any) => {
                    reject(error)
                })
        })
    }, data)
}

function getMsgTypeName(msgType: string) {
    const msgTypeMap: Record<string, string> = {
        ALI_SMS: '阿里短信',
        EMAIL: '邮箱'
    }

    return msgTypeMap[msgType] || msgType || '-'
}

function handleSelectionChange(records: any[]) {
    selectedRows.value = records || []
}

function cancelSelection() {
    selectedRows.value = []
    tableConfig.tableData = [...tableConfig.tableData]
}

function batchEnableMessages() {
    const enableRows = selectedRows.value.filter((row: any) => ['CHECK_SUCCESS', 'DISABLE'].includes(row.status))
    if (!enableRows.length) {
        ElMessage.warning('请选择可启用的消息体')
        return
    }

    batchLoading.value = true
    Promise.all(enableRows.map((row: any) => EnableMessageData({ id: row.id })))
        .then(() => {
            ElMessage.success('批量启用成功')
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDisableMessages() {
    const disableRows = selectedRows.value.filter((row: any) => row.status === 'ACTIVE')
    if (!disableRows.length) {
        ElMessage.warning('请选择启用状态的消息体')
        return
    }

    batchLoading.value = true
    Promise.all(disableRows.map((row: any) => DisabledMessageData({ id: row.id })))
        .then(() => {
            ElMessage.success('批量禁用成功')
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDeleteMessages() {
    if (!selectedRows.value.length) {
        return
    }

    ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个消息体吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(selectedRows.value.map((row: any) => DeleteMessageData({ id: row.id })))
            .then((res: any) => {
                ElMessage.success(res?.msg || '批量删除成功')
                initData()
            })
            .catch(() => {})
            .finally(() => {
                batchLoading.value = false
            })
    })
}

// 启用 or 禁用
function changeStatus(data: any, status: boolean) {
    data.statusLoading = true
    const request = status ? EnableMessageData : DisabledMessageData

    request({
        id: data.id
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
            initData(true)
        })
        .catch(() => {})
        .finally(() => {
            data.statusLoading = false
        })
}

// 删除
function deleteData(data: any) {
    ElMessageBox.confirm('确定删除该消息通知吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteMessageData({
            id: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                initData()
            })
            .catch(() => {})
    })
}

function inputEvent(e: string) {
    if (e === '') {
        initData()
    }
}

function handleSizeChange(e: number) {
    tableConfig.pagination.pageSize = e
    tableConfig.pagination.currentPage = 1
    initData()
}

function handleCurrentChange(e: number) {
    tableConfig.pagination.currentPage = e
    initData()
}

onMounted(() => {
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initData()
})
</script>

<style lang="scss">
.zqy-seach-table.message-notification-page {
    .zqy-table-top {
        position: relative;
        overflow: hidden;
        .message-batch-mask {
            position: absolute;
            z-index: 2;
            inset: 0;
            display: flex;
            align-items: center;
            justify-content: flex-start;
            padding: 0 20px;
            box-sizing: border-box;
            background-color: #fff;
        }
        .message-batch-slide-enter-active,
        .message-batch-slide-leave-active {
            transition:
                transform 0.18s ease,
                opacity 0.18s ease;
            will-change: transform, opacity;
        }
        .message-batch-slide-enter-from,
        .message-batch-slide-leave-to {
            opacity: 0;
            transform: translateY(-100%);
        }
        .message-batch-slide-enter-to,
        .message-batch-slide-leave-from {
            opacity: 1;
            transform: translateY(0);
        }
    }
    .message-batch-actions {
        display: flex;
        align-items: center;
        gap: 10px;
        .message-batch-action {
            min-width: 66px;
            height: 32px;
            line-height: 30px;
            border-color: getCssVar('color', 'primary');
            color: getCssVar('color', 'primary');
            background-color: #fff;
            &:hover,
            &:focus {
                border-color: getCssVar('color', 'primary');
                color: #fff;
                background-color: getCssVar('color', 'primary');
            }
        }
        .message-batch-cancel {
            min-width: 74px;
            height: 32px;
            line-height: 30px;
            border-color: getCssVar('border-color');
            color: getCssVar('text-color', 'regular');
            background-color: #fff;
            &:hover,
            &:focus {
                border-color: getCssVar('border-color');
                color: getCssVar('text-color', 'regular');
                background-color: #fff;
            }
        }
    }
    .zqy-table {
        .name-click:hover {
            text-decoration: none;
        }
        .message-type-tag {
            --el-tag-text-color: #{getCssVar('color', 'primary')};
            --el-tag-border-color: #{getCssVar('color', 'primary', 'light-5')};
            --el-tag-bg-color: #{getCssVar('color', 'primary', 'light-9')};
            color: getCssVar('color', 'primary');
            border-color: getCssVar('color', 'primary', 'light-5');
            background-color: getCssVar('color', 'primary', 'light-9');
            white-space: nowrap;
        }
        .message-action-group {
            justify-content: center;
            gap: 16px;
            .message-action-button {
                display: inline-flex;
                align-items: center;
                line-height: 1;
                font-size: getCssVar('font-size', 'extra-small');
            }
        }
        .message-status-group {
            justify-content: flex-start;
            gap: 6px;
            width: 100%;
            white-space: nowrap;
            .hover-tooltip {
                flex: 0 0 auto;
                margin-left: 2px;
            }
        }
        .hover-tooltip {
            font-size: 16px;
            color: getCssVar('color', 'danger', 'light-5');
        }
    }
}

.message-action-dropdown {
    .el-dropdown-menu {
        padding: 4px 0;
    }
    .el-dropdown-menu__item {
        height: 26px;
        line-height: 26px;
        padding: 0 16px;
        box-sizing: border-box;
        font-family: Avenir, Helvetica, Arial, sans-serif;
        font-size: getCssVar('font-size', 'extra-small');
    }
}
.message-error-tooltip {
    .el-popover__title {
        font-size: 14px;
    }
    font-size: 12px;
}
</style>
