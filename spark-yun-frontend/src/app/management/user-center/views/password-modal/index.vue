<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="formRef" label-position="top" :model="formData" :rules="rules" class="user-password-form">
            <el-form-item label="新密码" prop="newPassword">
                <el-input
                    v-model="formData.newPassword"
                    type="password"
                    show-password
                    maxlength="100"
                    placeholder="请输入新密码"
                />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                    v-model="formData.confirmPassword"
                    type="password"
                    show-password
                    maxlength="100"
                    placeholder="请再次输入新密码"
                />
            </el-form-item>
        </el-form>
    </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, ref, defineExpose, nextTick } from 'vue'
import BlockModal from '@/app/components/block-modal/index.vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'

const formRef = ref<FormInstance>()
const callback = ref<any>()

const modelConfig = reactive({
    title: '修改密码',
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
    customClass: 'user-center-password-modal',
    closeOnClickModal: false
})

const formData = reactive({
    userId: '',
    newPassword: '',
    confirmPassword: ''
})

const validateConfirmPassword = (_: any, value: string, callback: (error?: Error) => void) => {
    if (!value) {
        callback(new Error('请再次输入新密码'))
        return
    }

    if (value !== formData.newPassword) {
        callback(new Error('两次输入的新密码不一致'))
        return
    }

    callback()
}

const rules = reactive<FormRules>({
    newPassword: [
        {
            required: true,
            message: '请输入新密码',
            trigger: ['blur', 'change']
        }
    ],
    confirmPassword: [
        {
            validator: validateConfirmPassword,
            trigger: ['blur', 'change']
        }
    ]
})

function showModal(cb: () => void, row: any) {
    callback.value = cb
    modelConfig.visible = true
    formData.userId = row.id
    formData.newPassword = ''
    formData.confirmPassword = ''
    nextTick(() => {
        formRef.value?.clearValidate()
    })
}

function okEvent() {
    formRef.value?.validate((valid) => {
        if (valid) {
            modelConfig.okConfig.loading = true
            callback
                .value({
                    userId: formData.userId,
                    newPassword: formData.newPassword,
                    confirmPassword: formData.confirmPassword
                })
                .then((res: any) => {
                    modelConfig.okConfig.loading = false
                    if (res === undefined) {
                        modelConfig.visible = false
                    } else {
                        modelConfig.visible = true
                    }
                })
                .catch(() => {
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
.user-center-password-modal.zqy-block-modal {
    --user-modal-x-padding: 20px;
    --user-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        margin-right: 0;
        min-height: 46px;
        padding: 9px var(--user-modal-x-padding) 8px !important;
        border-bottom: none;
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
            background-color: var(--user-modal-border-color);
        }
    }
    .el-dialog__title {
        display: block;
        line-height: 28px;
    }
    .el-dialog__body {
        padding: 0 !important;
    }
    .el-dialog__footer {
        position: relative;
        min-height: 56px;
        padding: 12px var(--user-modal-x-padding);
        border-top: none;
        align-items: center;
        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--user-modal-border-color);
        }
    }
    .user-password-form {
        padding: 14px var(--user-modal-x-padding) 4px;
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
        .el-input {
            width: 100%;
        }
        .el-input__wrapper {
            border-radius: 2px;
        }
    }
}
</style>
