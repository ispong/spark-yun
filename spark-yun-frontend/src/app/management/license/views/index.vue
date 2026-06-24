<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-seach-table license-page">
        <div class="zqy-table-top">
            <el-button type="primary" @click="addData">{{ t('license.uploadCertificate') }}</el-button>
            <div class="zqy-seach">
                <el-input
                    v-model="keyword"
                    :placeholder="t('license.searchPlaceholder')"
                    :maxlength="200"
                    clearable
                    @input="inputEvent"
                    @keyup.enter="initData(false)"
                />
            </div>
            <Transition name="license-batch-slide">
                <div v-if="selectedRows.length" class="license-batch-mask">
                    <div class="license-batch-actions">
                        <el-button class="license-batch-action" :loading="batchLoading" @click="batchEnableLicense">
                            {{ t('license.enable') }}
                        </el-button>
                        <el-button class="license-batch-action" :loading="batchLoading" @click="batchDisableLicense">
                            {{ t('license.disable') }}
                        </el-button>
                        <el-button class="license-batch-action" :loading="batchLoading" @click="batchDeleteLicense">
                            {{ t('license.delete') }}
                        </el-button>
                        <el-button class="license-batch-cancel" :disabled="batchLoading" @click="cancelSelection">
                            {{ t('license.cancelSelection') }}
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
                    <template #statusTag="scopeSlot">
                        <el-tag v-if="scopeSlot.row.status === 'ENABLE'" type="success">{{ t('license.enable') }}</el-tag>
                        <el-tag v-if="scopeSlot.row.status === 'DISABLE'" type="danger">{{ t('license.disable') }}</el-tag>
                    </template>
                    <template #options="scopeSlot">
                        <div class="btn-group license-action-group">
                            <el-dropdown trigger="click" popper-class="license-action-dropdown">
                                <span class="click-show-more license-action-button">{{ t('common.more') }}</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item
                                            :disabled="scopeSlot.row.statusLoading"
                                            @click="
                                                !scopeSlot.row.statusLoading &&
                                                    changeStatus(scopeSlot.row, scopeSlot.row.status !== 'ENABLE')
                                            "
                                        >
                                            <span v-if="!scopeSlot.row.statusLoading">
                                                {{
                                                    scopeSlot.row.status === 'ENABLE'
                                                        ? t('license.disable')
                                                        : t('license.enable')
                                                }}
                                            </span>
                                            <el-icon v-else class="is-loading">
                                                <Loading />
                                            </el-icon>
                                        </el-dropdown-item>
                                        <el-dropdown-item @click="deleteData(scopeSlot.row)">
                                            {{ t('license.delete') }}
                                        </el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
                    </template>
                </BlockTable>
            </div>
        </LoadingPage>
        <AddModal ref="addModalRef" />
    </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted, watch } from 'vue'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import AddModal from './add-modal/index.vue'

import { createBreadCrumbList, createColConfigs, createTableConfig } from './license.config'
import {
    GetLicenseList,
    UploadLicenseFile,
    DisableLicense,
    EnableLicense,
    DeleteLicense,
    CheckLicenseStatus
} from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()
const breadCrumbList = reactive(createBreadCrumbList(t))
const tableConfig: any = reactive(createTableConfig(t))
const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const addModalRef = ref(null)
const selectedRows = ref<any[]>([])
const batchLoading = ref(false)

watch(locale, () => {
    breadCrumbList.splice(0, breadCrumbList.length, ...createBreadCrumbList(t))
    tableConfig.colConfigs = createColConfigs(t)
})

function refreshLicenseAndReload() {
    CheckLicenseStatus()
        .catch(() => {
            // Refresh the page even if the license status refresh fails.
        })
        .finally(() => {
            window.location.reload()
        })
}

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = networkError.value || false
    GetLicenseList({
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
            selectedRows.value = []
            loading.value = false
            tableConfig.loading = false
            networkError.value = true
        })
}

function addData() {
    addModalRef.value.showModal((data: any) => {
        return new Promise((resolve: any, reject: any) => {
            const formData = new FormData()
            formData.append('license', data)
            UploadLicenseFile(formData)
                .then((res: any) => {
                    ElMessage({
                        type: 'success',
                        message: res?.data?.msg || t('license.uploadSuccess'),
                        onClose: () => {
                            refreshLicenseAndReload()
                        }
                    })
                    initData()
                    resolve()
                })
                .catch((error: any) => {
                    reject(error)
                })
        })
    })
}

function handleSelectionChange(records: any[]) {
    selectedRows.value = records || []
}

function cancelSelection() {
    selectedRows.value = []
    tableConfig.tableData = [...tableConfig.tableData]
}

function batchEnableLicense() {
    const disableRows = selectedRows.value.filter((row: any) => row.status === 'DISABLE')
    if (!disableRows.length) {
        ElMessage.warning(t('license.selectDisabledCertificates'))
        return
    }

    batchLoading.value = true
    Promise.all(disableRows.map((row: any) => EnableLicense({ licenseId: row.id })))
        .then(() => {
            ElMessage.success(t('license.batchEnableSuccess'))
            refreshLicenseAndReload()
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDisableLicense() {
    const enableRows = selectedRows.value.filter((row: any) => row.status === 'ENABLE')
    if (!enableRows.length) {
        ElMessage.warning(t('license.selectEnabledCertificates'))
        return
    }

    batchLoading.value = true
    Promise.all(enableRows.map((row: any) => DisableLicense({ licenseId: row.id })))
        .then(() => {
            ElMessage.success(t('license.batchDisableSuccess'))
            refreshLicenseAndReload()
        })
        .catch(() => {})
        .finally(() => {
            batchLoading.value = false
        })
}

function batchDeleteLicense() {
    if (!selectedRows.value.length) {
        return
    }

    ElMessageBox.confirm(t('license.deleteSelectedConfirm', { count: selectedRows.value.length }), t('common.warning'), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
    }).then(() => {
        batchLoading.value = true
        Promise.all(selectedRows.value.map((row: any) => DeleteLicense({ licenseId: row.id })))
            .then(() => {
                ElMessage.success(t('license.batchDeleteSuccess'))
                initData()
            })
            .catch(() => {})
            .finally(() => {
                batchLoading.value = false
            })
    })
}

function changeStatus(data: any, status: boolean) {
    data.statusLoading = true
    if (status) {
        EnableLicense({
            licenseId: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                data.statusLoading = false
                initData(true)
                refreshLicenseAndReload()
            })
            .catch(() => {
                data.statusLoading = false
            })
    } else {
        DisableLicense({
            licenseId: data.id
        })
            .then((res: any) => {
                ElMessage.success(res.msg)
                data.statusLoading = false
                initData(true)
                refreshLicenseAndReload()
            })
            .catch(() => {
                data.statusLoading = false
            })
    }
}

function deleteData(data: any) {
    ElMessageBox.confirm(t('license.deleteCertificateConfirm'), t('common.warning'), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
    }).then(() => {
        DeleteLicense({
            licenseId: data.id
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
.zqy-seach-table.license-page {
    .zqy-table-top {
        position: relative;
        overflow: hidden;
    }

    .license-batch-mask {
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

    .license-batch-slide-enter-active,
    .license-batch-slide-leave-active {
        transition:
            transform 0.18s ease,
            opacity 0.18s ease;
        will-change: transform, opacity;
    }

    .license-batch-slide-enter-from,
    .license-batch-slide-leave-to {
        opacity: 0;
        transform: translateY(-100%);
    }

    .license-batch-slide-enter-to,
    .license-batch-slide-leave-from {
        opacity: 1;
        transform: translateY(0);
    }

    .license-batch-actions {
        display: flex;
        align-items: center;
        gap: 12px;
    }

    .license-batch-action {
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

    .license-batch-cancel {
        height: 32px;
        line-height: 30px;
    }

    .license-action-group {
        width: 100%;
        justify-content: center;

        .el-dropdown {
            display: inline-flex;
            justify-content: center;
        }

        .license-action-button {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            line-height: 1;
            font-size: getCssVar('font-size', 'extra-small');
            color: getCssVar('color', 'primary');
            cursor: pointer;
            white-space: nowrap;
        }
    }
}

.license-action-dropdown {
    .el-dropdown-menu {
        padding: 4px 0;
    }

    .el-dropdown-menu__item {
        height: 26px;
        line-height: 26px;
        font-family: Avenir, Helvetica, Arial, sans-serif;
        font-size: getCssVar('font-size', 'extra-small');

        .el-icon {
            font-size: getCssVar('font-size', 'extra-small');
        }
    }
}
</style>
