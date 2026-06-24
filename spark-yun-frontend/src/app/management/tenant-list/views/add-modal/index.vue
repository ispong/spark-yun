<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="form" class="add-computer-group" label-position="top" :model="formData" :rules="rules">
            <el-form-item :label="t('tenantList.tenantName')" prop="name">
                <el-input v-model="formData.name" maxlength="100" :placeholder="t('common.pleaseInput')" show-word-limit />
            </el-form-item>
            <el-form-item :label="t('tenantList.memberCount')">
                <el-input-number
                    v-model="formData.maxMemberNum"
                    :placeholder="t('common.pleaseInput')"
                    :min="0"
                    :max="100000"
                    :step="1"
                    controls-position="right"
                />
            </el-form-item>
            <el-form-item :label="t('tenantList.workflowCount')">
                <el-input-number
                    v-model="formData.maxWorkflowNum"
                    :placeholder="t('common.pleaseInput')"
                    :min="0"
                    :max="100000"
                    :step="1"
                    controls-position="right"
                />
            </el-form-item>
            <el-form-item :label="t('tenantList.tenantSuperAdmin')" prop="adminUserId">
                <el-select
                    v-model="formData.adminUserId"
                    :placeholder="t('common.pleaseSelect')"
                    popper-class="tenant-admin-select-popper"
                >
                    <el-option v-for="item in userList" :key="item.id" :label="item.username" :value="item.id" />
                </el-select>
            </el-form-item>
            <el-form-item :label="t('tenantList.remark')">
                <el-input
                    v-model="formData.remark"
                    show-word-limit
                    type="textarea"
                    maxlength="200"
                    :autosize="{ minRows: 4, maxRows: 4 }"
                    :placeholder="t('common.pleaseInput')"
                />
            </el-form-item>
        </el-form>
        <template #customLeft>
            <div class="valid-time">
                <el-date-picker
                    v-model="formData.validDateTime"
                    type="datetimerange"
                    format="YYYY-MM-DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    :unlink-panels="true"
                    range-separator="~"
                    :start-placeholder="t('tenantList.validStartTime')"
                    :end-placeholder="t('tenantList.validEndTime')"
                    :editable="false"
                />
            </div>
        </template>
    </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, defineExpose, ref, nextTick, computed, watch } from 'vue'
import BlockModal from '@/app/components/block-modal/index.vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { GetUserInfoList } from '@/app/management/tenant-user/api'
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()
const form = ref<FormInstance>()
const callback = ref<any>()
const userList = ref<any[]>([])
const renderSence = ref('new')
const currentAdminOption = ref<any>()
const modelConfig = reactive({
    title: t('tenantList.addTenant'),
    visible: false,
    width: '520px',
    okConfig: {
        title: t('common.confirm'),
        ok: okEvent,
        disabled: false,
        loading: false
    },
    cancelConfig: {
        title: t('common.cancel'),
        cancel: closeEvent,
        disabled: false
    },
    needScale: false,
    zIndex: 1100,
    customClass: 'tenant-add-modal',
    closeOnClickModal: false
})
const formData = reactive({
    name: '',
    maxMemberNum: 5,
    maxWorkflowNum: 20,
    adminUserId: '',
    createAdminUser: false,
    validDateTime: [],
    remark: '',
    id: ''
})
const rules = computed<FormRules>(() => ({
    name: [
        {
            required: true,
            message: t('tenantList.inputTenantName'),
            trigger: ['change', 'blur']
        }
    ],
    adminUserId: [
        {
            required: true,
            message: t('tenantList.selectTenantSuperAdmin'),
            trigger: ['change', 'blur']
        }
    ]
}))

function syncModalText() {
    modelConfig.title = renderSence.value === 'edit' ? t('tenantList.editTenant') : t('tenantList.addTenant')
    modelConfig.okConfig.title = t('common.confirm')
    modelConfig.cancelConfig.title = t('common.cancel')
}

watch(locale, () => {
    syncModalText()
    form.value?.clearValidate()
})

function showModal(cb: () => void, data: any): void {
    callback.value = cb
    modelConfig.visible = true
    if (data) {
        formData.name = data.name
        formData.maxMemberNum = data.maxMemberNum
        formData.maxWorkflowNum = data.maxWorkflowNum
        formData.adminUserId = data.adminUserId || ''
        currentAdminOption.value =
            data.adminUserId && data.adminUserName
                ? {
                      id: data.adminUserId,
                      username: data.adminUserName
                  }
                : undefined
        formData.remark = data.remark
        if (data.validStartDateTime && data.validEndDateTime) {
            formData.validDateTime = [data.validStartDateTime, data.validEndDateTime]
        } else {
            formData.validDateTime = []
        }
        formData.id = data.id
        renderSence.value = 'edit'
    } else {
        formData.name = ''
        formData.maxMemberNum = 2
        formData.maxWorkflowNum = 5
        formData.adminUserId = ''
        currentAdminOption.value = undefined
        formData.remark = ''
        formData.validDateTime = []
        formData.id = ''
        renderSence.value = 'new'
    }
    syncModalText()
    getUserOfSystem()
    nextTick(() => {
        form.value?.resetFields()
    })
}

function getUserOfSystem() {
    GetUserInfoList({
        page: 0,
        pageSize: 999,
        searchKeyWord: ''
    })
        .then((res: any) => {
            const users = res.data.content || []
            if (currentAdminOption.value && !users.some((item: any) => item.id === currentAdminOption.value.id)) {
                userList.value = [currentAdminOption.value, ...users]
            } else {
                userList.value = users
            }
        })
        .catch(() => {
            userList.value = []
        })
}

function okEvent() {
    form.value?.validate((valid) => {
        if (valid) {
            if (renderSence.value === 'new' && !formData.adminUserId) {
                ElMessage.warning(t('tenantList.selectTenantSuperAdmin'))
                return
            }
            modelConfig.okConfig.loading = true
            callback
                .value({
                    ...formData,
                    createAdminUser: false,
                    adminUserId: formData.adminUserId,
                    id: formData.id ? formData.id : undefined
                })
                .then((res: any) => {
                    modelConfig.okConfig.loading = false
                    if (res === undefined) {
                        modelConfig.visible = false
                    } else {
                        modelConfig.visible = true
                    }
                })
                .catch((err: any) => {
                    modelConfig.okConfig.loading = false
                })
        } else {
            ElMessage.warning(t('validation.completeForm'))
        }
    })
}

function closeEvent() {
    modelConfig.visible = false
}

defineExpose({
    showModal
})
</script>

<style lang="scss">
.tenant-add-modal.zqy-block-modal {
    --tenant-modal-x-padding: 20px;
    --tenant-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--tenant-modal-x-padding) 8px !important;
        margin-right: 0;
        border-bottom: none;
        .el-dialog__title {
            display: block;
            line-height: 28px;
        }
        .el-dialog__headerbtn {
            top: 0;
            width: 42px;
            height: 46px;
        }
        &::after {
            position: absolute;
            right: 0;
            bottom: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--tenant-modal-border-color);
        }
    }
    .el-dialog__body {
        padding: 0 !important;
    }
    .el-dialog__footer {
        position: relative;
        min-height: 56px;
        padding: 12px var(--tenant-modal-x-padding);
        align-items: center;
        border-top: none;
        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--tenant-modal-border-color);
        }
    }
    .add-computer-group {
        padding: 14px var(--tenant-modal-x-padding) 4px;
        box-sizing: border-box;
        .el-form-item {
            margin-bottom: 20px;
        }
        .el-form-item__label {
            width: 100%;
            padding: 0;
            margin-bottom: 4px;
            line-height: 16px;
            color: getCssVar('text-color', 'regular');
        }
        .el-form-item__content,
        .el-input,
        .el-input-number,
        .el-select,
        .el-textarea {
            width: 100%;
        }
        .el-input__wrapper,
        .el-textarea__inner {
            border-radius: 2px;
        }
    }
    .valid-time {
        position: absolute;
        left: var(--tenant-modal-x-padding);
        display: flex;
        align-items: center;
        height: 28px;
        .el-date-editor--datetimerange {
            width: 300px;
            height: 28px;
            padding: 0 6px;
            .el-range-input {
                font-size: 12px;
            }
            .el-range-separator {
                max-width: 8px;
                padding: 0;
            }
        }
    }
}
.el-date-range-picker {
    .el-picker-panel__footer {
        display: flex;
        justify-content: space-between;
    }
}
.tenant-admin-select-popper.el-select__popper {
    z-index: 6000 !important;
}
@media (max-width: 560px) {
    .tenant-add-modal.zqy-block-modal {
        .el-dialog__footer {
            padding-top: 48px;
        }
        .valid-time {
            top: 12px;
            right: var(--tenant-modal-x-padding);
            .el-date-editor--datetimerange {
                width: 100%;
            }
        }
    }
}
</style>
