<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="form" class="add-computer-group" label-position="top" :model="formData" :rules="rules">
            <el-form-item :label="t('userCenter.account')" prop="account">
                <el-input v-model="formData.account" maxlength="100" :placeholder="t('common.pleaseInput')" show-word-limit />
            </el-form-item>
            <el-form-item :label="t('userCenter.name')" prop="username">
                <el-input v-model="formData.username" maxlength="100" :placeholder="t('common.pleaseInput')" show-word-limit />
            </el-form-item>
            <el-form-item v-if="renderSence === 'new'" :label="t('userCenter.password')" prop="passwd">
                <el-input
                    v-model="formData.passwd"
                    maxlength="100"
                    type="password"
                    show-password
                    :placeholder="t('common.pleaseInput')"
                />
            </el-form-item>
            <el-form-item :label="t('userCenter.phone')" prop="phone">
                <el-input
                    v-model="formData.phone"
                    maxlength="11"
                    :placeholder="t('userCenter.inputPhone')"
                    show-word-limit
                />
            </el-form-item>
            <el-form-item :label="t('userCenter.email')" prop="email">
                <el-input
                    v-model="formData.email"
                    maxlength="100"
                    :placeholder="t('userCenter.inputEmail')"
                    show-word-limit
                />
            </el-form-item>
            <el-form-item :label="t('userCenter.remark')">
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
                    :start-placeholder="t('userCenter.validStartTime')"
                    :end-placeholder="t('userCenter.validEndTime')"
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
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()
const form = ref<FormInstance>()
const callback = ref<any>()
const renderSence = ref('new')
const modelConfig = reactive({
    title: t('userCenter.addUser'),
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
    customClass: 'user-center-add-modal',
    closeOnClickModal: false
})
const formData = reactive({
    username: '',
    account: '',
    passwd: '',
    validDateTime: '',
    phone: '',
    email: '',
    remark: '',
    id: ''
})
// China mainland phone number validator.
const validatePhone = (rule: any, value: any, callback: any) => {
    if (!value) {
        callback()
        return
    }
    // Starts with 1, second digit 3-9, total 11 digits.
    const phoneReg = /^1[3-9]\d{9}$/
    if (!phoneReg.test(value)) {
        callback(new Error(t('userCenter.invalidPhone')))
    } else {
        callback()
    }
}

// Email validator.
const validateEmail = (rule: any, value: any, callback: any) => {
    if (!value) {
        callback()
        return
    }
    const emailReg = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
    if (!emailReg.test(value)) {
        callback(new Error(t('userCenter.invalidEmail')))
    } else {
        callback()
    }
}

const rules = computed<FormRules>(() => ({
    username: [
        {
            required: true,
            message: t('userCenter.inputName'),
            trigger: ['change']
        }
    ],
    account: [
        {
            required: true,
            message: t('userCenter.inputAccount'),
            trigger: ['change']
        }
    ],
    passwd: [
        {
            required: true,
            message: t('userCenter.inputPassword'),
            trigger: ['change']
        }
    ],
    phone: [
        {
            validator: validatePhone,
            trigger: ['blur', 'change']
        }
    ],
    email: [
        {
            validator: validateEmail,
            trigger: ['blur', 'change']
        }
    ]
}))

function syncModalText() {
    modelConfig.title = renderSence.value === 'edit' ? t('userCenter.editUser') : t('userCenter.addUser')
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
        formData.username = data.username
        formData.account = data.account
        formData.phone = data.phone
        formData.email = data.email
        formData.remark = data.remark
        if (data.validStartDateTime && data.validEndDateTime) {
            formData.validDateTime = [data.validStartDateTime, data.validEndDateTime]
        } else {
            formData.validDateTime = []
        }
        formData.id = data.id
        renderSence.value = 'edit'
    } else {
        formData.username = ''
        formData.account = ''
        formData.passwd = ''
        formData.validDateTime = []
        formData.phone = ''
        formData.email = ''
        formData.remark = ''
        formData.id = ''
        renderSence.value = 'new'
    }
    syncModalText()
    nextTick(() => {
        form.value?.resetFields()
    })
}

function okEvent() {
    form.value?.validate((valid) => {
        if (valid) {
            modelConfig.okConfig.loading = true
            callback
                .value({
                    ...formData,
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
                .catch(() => {
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
.user-center-add-modal.zqy-block-modal {
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
    .add-computer-group {
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
        .el-input,
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
        left: var(--user-modal-x-padding);
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
    .user-center-add-modal.zqy-block-modal {
        .el-dialog__footer {
            padding-top: 48px;
        }
        .valid-time {
            top: 12px;
            right: var(--user-modal-x-padding);
            .el-date-editor--datetimerange {
                width: 100%;
            }
        }
    }
}
</style>
