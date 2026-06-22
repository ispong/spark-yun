<template>
    <div class="zqy-seach-table costom-form">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addData">新建表单</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    placeholder="请输入表单名称 回车进行搜索"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(false)"
                />
            </div>
        </div>
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData(false)">
            <div class="form-card-container">
                <template v-if="formList?.length">
                    <el-scrollbar max-height="calc(100vh - 122px)" class="form-card-list">
                        <template v-for="card in formList" :key="card.id">
                            <el-tooltip
                                :disabled="!card.remark"
                                :content="card.remark"
                                placement="top"
                                :show-after="600"
                            >
                                <div class="form-card-item" @click="redirectQuery(card)">
                                    <div class="card-header">
                                        <div class="card-title">
                                            <EllipsisTooltip class="card-title-name" :label="card.name" />
                                        </div>
                                        <el-tag
                                            class="card-status"
                                            size="small"
                                            :type="card.status === 'UNPUBLISHED' ? 'warning' : 'success'"
                                        >
                                            {{ card.status === 'UNPUBLISHED' ? '未发布' : '已发布' }}
                                        </el-tag>
                                    </div>
                                    <div class="card-content">
                                        <div class="card-item">
                                            <span class="name">数据源</span>
                                            <EllipsisTooltip class="card-item-name" :label="card.datasourceName" />
                                        </div>
                                        <div class="card-item">
                                            <span class="name">表名</span>
                                            <EllipsisTooltip class="card-item-name" :label="card.mainTable" />
                                        </div>
                                        <div class="card-item">
                                            <span class="name">创建时间</span>
                                            <EllipsisTooltip class="card-item-name" :label="card.createDateTime" />
                                        </div>
                                    </div>
                                    <div class="card-actions">
                                        <span
                                            v-if="card.status === 'UNPUBLISHED'"
                                            class="card-action"
                                            @click.stop="editData(card)"
                                        >
                                            配置
                                        </span>
                                        <span class="card-action" @click.stop="updateData(card)">编辑</span>
                                        <span
                                            v-if="card.status === 'UNPUBLISHED'"
                                            class="card-action card-action__danger"
                                            @click.stop="deleteData(card)"
                                        >
                                            删除
                                        </span>
                                        <span
                                            v-if="card.status !== 'UNPUBLISHED'"
                                            class="card-action"
                                            @click.stop="shareForm(card)"
                                        >
                                            分享
                                        </span>
                                        <span
                                            v-if="card.status !== 'UNPUBLISHED'"
                                            class="card-action"
                                            @click.stop="underlineForm(card)"
                                        >
                                            下线
                                        </span>
                                        <span v-else class="card-action" @click.stop="publishForm(card)">发布</span>
                                    </div>
                                </div>
                            </el-tooltip>
                        </template>
                    </el-scrollbar>
                </template>
                <template v-else>
                    <empty-page />
                </template>
            </div>
        </LoadingPage>
        <add-form ref="addFormRef" />
        <ShareForm ref="shareFormRef" />
    </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import LoadingPage from '@/app/components/loading/index.vue'
import { useRouter } from 'vue-router'
import AddForm from './add-form/index.vue'
import EllipsisTooltip from '@/app/components/ellipsis-tooltip/ellipsis-tooltip.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ShareForm from './share-form-modal/index.vue'
import {
    CreateCustomFormData,
    DeleteCustomFormData,
    DeployCustomFormData,
    OfflineCustomFormData,
    QueryCustomFormList,
    UpdateCustomFormData
} from '../api'

interface formDataParam {
    name: string
    // clusterId: string
    datasourceId: string
    createMode: string
    mainTable: string
    remark: string
    id?: string
}

const router = useRouter()
const networkError = ref(false)
const loading = ref(false)
const addFormRef = ref()
const keyword = ref('')
const formList = ref<any[]>([])
const shareFormRef = ref()
const listPageSize = 1000

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    QueryCustomFormList({
        page: 0,
        pageSize: listPageSize,
        searchKeyWord: keyword.value || ''
    })
        .then((res: any) => {
            formList.value = res.data.content
            loading.value = false
            networkError.value = false
        })
        .catch(() => {
            formList.value = []
            loading.value = false
            networkError.value = true
        })
}

function inputEvent(e: string) {
    if (e === '') {
        initData()
    }
}

function addData() {
    addFormRef.value.showModal((data: formDataParam) => {
        return new Promise((resolve, reject) => {
            CreateCustomFormData(data)
                .then((res: any) => {
                    resolve()
                    router.push({
                        name: 'form-setting',
                        query: {
                            id: res.data.id,
                            formVersion: res.data.formVersion
                        }
                    })
                    ElMessage.success(res.msg)
                })
                .catch((err) => {
                    reject(err)
                })
        })
    })
}
function updateData(card: any) {
    addFormRef.value.showModal((data: formDataParam) => {
        return new Promise((resolve, reject) => {
            UpdateCustomFormData({
                id: data.id,
                name: data.name,
                remark: data.remark
            })
                .then((res: any) => {
                    ElMessage.success(res.msg)
                    resolve()
                    initData()
                })
                .catch((err) => {
                    reject(err)
                })
        })
    }, card)
}
// 编辑表单配置
function editData(card: any) {
    router.push({
        name: 'form-setting',
        query: {
            id: card.id,
            formVersion: card.formVersion
        }
    })
}
function deleteData(card: any) {
    ElMessageBox.confirm('确定删除该表单吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeleteCustomFormData({
            formId: card.id
        })
            .then((res: any) => {
                initData()
                ElMessage.success(res.msg)
            })
            .catch((err) => {})
    })
}
// 分享
function shareForm(card: any) {
    shareFormRef.value.showModal(card)
}
// 下线
function underlineForm(card: any) {
    ElMessageBox.confirm('确定下线该表单吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        OfflineCustomFormData({
            formId: card.id
        })
            .then((res: any) => {
                initData()
                ElMessage.success('下线成功')
            })
            .catch((err) => {})
    })
}
// 发布
function publishForm(card: any) {
    ElMessageBox.confirm('确定发布该表单吗？', '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        DeployCustomFormData({
            formId: card.id
        })
            .then((res: any) => {
                initData()
                ElMessage.success('发布成功')
            })
            .catch((err) => {})
    })
}

function redirectQuery(data: any) {
    router.push({
        name: 'form-query',
        query: {
            id: data.id,
            formVersion: data.formVersion
        }
    })
}

onMounted(() => {
    initData()
})
</script>

<style lang="scss">
.costom-form {
    .form-card-container {
        min-height: calc(100vh - 122px);

        .form-card-list {
            width: 100%;

            .el-scrollbar__wrap {
                .el-scrollbar__view {
                    display: grid;
                    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
                    gap: 14px;
                    padding: 16px 20px 20px;
                    box-sizing: border-box;
                }
            }

            .form-card-item {
                min-height: 168px;
                border: 1px solid getCssVar('border-color');
                border-radius: 4px;
                background-color: getCssVar('color', 'white');
                transition:
                    border-color 0.15s linear,
                    box-shadow 0.15s linear;
                display: flex;
                flex-direction: column;
                justify-content: space-between;
                padding: 14px 14px 12px;
                box-sizing: border-box;
                font-size: getCssVar('font-size', 'extra-small');
                cursor: pointer;
                color: getCssVar('text-color', 'regular');
                position: relative;

                &:hover {
                    border-color: getCssVar('color', 'primary', 'light-5');
                    box-shadow: 0 6px 18px rgb(31 45 61 / 8%);

                    .card-title {
                        color: getCssVar('color', 'primary');
                    }
                }

                .card-header {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 10px;
                    margin-bottom: 12px;
                }

                .card-status {
                    flex: none;
                }

                .card-content {
                    flex: 1;
                    display: flex;
                    flex-direction: column;
                    gap: 9px;
                }

                .card-item {
                    display: flex;
                    align-items: center;
                    min-width: 0;
                    line-height: 18px;

                    .name {
                        flex: none;
                        width: 54px;
                        color: getCssVar('text-color', 'secondary');
                    }

                    .card-item-name {
                        flex: 1;
                        min-width: 0;
                        color: getCssVar('text-color', 'regular');
                    }
                }

                .card-title {
                    min-width: 0;
                    flex: 1;
                    display: flex;
                    align-items: center;
                    font-size: 15px;
                    font-weight: 600;
                    color: getCssVar('text-color', 'regular');
                    transition: color 0.15s linear;

                    .card-title-name {
                        display: inline-block;
                        max-width: 100%;
                    }
                }

                .card-actions {
                    display: flex;
                    align-items: center;
                    flex-wrap: wrap;
                    gap: 0;
                    padding-top: 12px;
                    margin-top: 12px;
                    border-top: 1px solid getCssVar('border-color', 'lighter');
                    line-height: 16px;
                }

                .card-action {
                    cursor: pointer;
                    font-size: 12px;
                    color: getCssVar('color', 'primary');
                    padding: 0 8px;
                    border-right: 1px solid getCssVar('border-color');
                    &:first-child {
                        padding-left: 0;
                    }
                    &:last-child {
                        padding-right: 0;
                        border-right: none;
                    }
                    &:hover {
                        text-decoration: underline;
                    }
                }
            }
        }
    }
}
.custom-form-popover {
    .el-scrollbar {
        .el-scrollbar__wrap {
            .el-dropdown__list {
                .el-dropdown-menu {
                    .el-dropdown-menu__item {
                        font-size: 12px;
                    }
                }
            }
        }
    }
}
</style>
