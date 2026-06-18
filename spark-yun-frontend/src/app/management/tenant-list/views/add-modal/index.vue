<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="form" class="add-computer-group" label-position="top" :model="formData" :rules="rules">
            <el-form-item label="租户名称" prop="name">
                <el-input v-model="formData.name" maxlength="100" placeholder="请输入" show-word-limit />
            </el-form-item>
            <el-form-item label="成员数">
                <el-input-number
                    v-model="formData.maxMemberNum"
                    placeholder="请输入"
                    :min="0"
                    :max="100000"
                    :step="1"
                    controls-position="right"
                />
            </el-form-item>
            <el-form-item label="作业流数">
                <el-input-number
                    v-model="formData.maxWorkflowNum"
                    placeholder="请输入"
                    :min="0"
                    :max="100000"
                    :step="1"
                    controls-position="right"
                />
            </el-form-item>
            <el-form-item label="租户超级管理员" prop="adminUserId">
                <el-select v-model="formData.adminUserId" placeholder="请选择">
                    <el-option v-for="item in userList" :key="item.id" :label="item.username" :value="item.id" />
                </el-select>
            </el-form-item>
            <el-form-item label="备注">
                <el-input
                    v-model="formData.remark"
                    show-word-limit
                    type="textarea"
                    maxlength="200"
                    :autosize="{ minRows: 4, maxRows: 4 }"
                    placeholder="请输入"
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
                    start-placeholder="有效开始时间"
                    end-placeholder="有效结束时间"
                    :editable="false"
                />
            </div>
        </template>
    </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, defineExpose, ref, nextTick } from 'vue'
import BlockModal from '@/app/components/block-modal/index.vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { GetUserInfoList } from '@/app/management/tenant-user/api'

const form = ref<FormInstance>()
const callback = ref<any>()
const userList = ref<any[]>([])
const renderSence = ref('new')
const currentAdminOption = ref<any>()
const modelConfig = reactive({
    title: '新建租户',
    visible: false,
    width: '520px',
    okConfig: {
        title: '确定',
        ok: okEvent,
        disabled: false,
        loading: false
    },
    cancelConfig: {
        title: '取消',
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
const rules = reactive<FormRules>({
    name: [
        {
            required: true,
            message: '请输入租户名称',
            trigger: ['change', 'blur']
        }
    ],
    adminUserId: [
        {
            required: true,
            message: '请选择租户超级管理员',
            trigger: ['change', 'blur']
        }
    ]
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
        modelConfig.title = '编辑租户'
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
        modelConfig.title = '新建租户'
        renderSence.value = 'new'
    }
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
                ElMessage.warning('请选择租户超级管理员')
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
            ElMessage.warning('请将表单输入完整')
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
