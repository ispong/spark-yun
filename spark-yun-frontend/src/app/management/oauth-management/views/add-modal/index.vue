<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="form" class="add-computer-group" label-position="top" :model="formData" :rules="rules">
            <el-form-item :label="t('oauthManagement.name')" prop="name">
                <el-input v-model="formData.name" maxlength="200" :placeholder="t('common.pleaseInput')" />
            </el-form-item>
            <el-form-item :label="t('oauthManagement.type')" prop="ssoType">
                <el-select v-model="formData.ssoType" :placeholder="t('common.pleaseSelect')">
                    <el-option v-for="item in typeList" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
            </el-form-item>
            <el-form-item label="clientId" prop="clientId">
                <el-input v-model="formData.clientId" maxlength="2000" :placeholder="t('common.pleaseInput')" />
            </el-form-item>
            <el-form-item label="clientSecret" prop="clientSecret">
                <el-input v-model="formData.clientSecret" maxlength="2000" :placeholder="t('common.pleaseInput')" />
            </el-form-item>
            <el-form-item label="scope">
                <el-input v-model="formData.scope" maxlength="2000" :placeholder="t('common.pleaseInput')" />
            </el-form-item>
            <el-form-item label="authUrl" prop="authUrl">
                <el-input v-model="formData.authUrl" maxlength="2000" :placeholder="t('common.pleaseInput')" />
            </el-form-item>
            <el-form-item label="accessTokenUrl" prop="accessTokenUrl">
                <el-input v-model="formData.accessTokenUrl" maxlength="2000" :placeholder="t('common.pleaseInput')" />
            </el-form-item>
            <el-form-item :label="t('oauthManagement.redirectUrl')" prop="redirectUrl">
                <el-input v-model="formData.redirectUrl" maxlength="2000" :placeholder="t('common.pleaseInput')" />
            </el-form-item>
            <el-form-item label="userUrl" prop="userUrl">
                <el-input v-model="formData.userUrl" maxlength="2000" :placeholder="t('common.pleaseInput')" />
            </el-form-item>
            <el-form-item label="authJsonPath" prop="authJsonPath">
                <el-input v-model="formData.authJsonPath" maxlength="2000" :placeholder="t('common.pleaseInput')" />
            </el-form-item>
            <el-form-item :label="t('oauthManagement.remark')">
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
    </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, defineExpose, ref, nextTick, computed, watch } from 'vue'
import BlockModal from '@/app/components/block-modal/index.vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'

interface ConfigParam {
    name: string
    ssoType: string
    remark: string
    authJsonPath: string
    scope: string
    clientId: string
    clientSecret: string
    accessTokenUrl: string
    authUrl: string
    userUrl: string
    redirectUrl: string
    id?: string
}

interface Option {
    label: string
    value: string
}

const form = ref<FormInstance>()
const { t, locale } = useI18n()
const callback = ref<any>()
const renderSence = ref('new')
const typeList = ref<Option[]>([
    {
        label: 'Github',
        value: 'GITHUB'
    },
    {
        label: 'Keycloak',
        value: 'KEYCLOAK'
    }
])
const modelConfig = reactive({
    title: t('oauthManagement.addOauth'),
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
    customClass: 'oauth-add-modal',
    closeOnClickModal: false
})
const formData = reactive<ConfigParam>({
    name: '',
    ssoType: '',
    remark: '',
    authJsonPath: '',
    scope: '',
    clientId: '',
    clientSecret: '',
    accessTokenUrl: '',
    authUrl: '',
    userUrl: '',
    redirectUrl: '',
    id: ''
})
const rules = computed<FormRules>(() => ({
    name: [
        {
            required: true,
            message: t('oauthManagement.inputName'),
            trigger: ['change']
        }
    ],
    ssoType: [
        {
            required: true,
            message: t('oauthManagement.selectType'),
            trigger: ['change']
        }
    ],
    authJsonPath: [
        {
            required: true,
            message: t('oauthManagement.inputAuthJsonPath'),
            trigger: ['change']
        }
    ],
    scope: [
        {
            required: true,
            message: t('oauthManagement.inputScope'),
            trigger: ['change']
        }
    ],
    clientId: [
        {
            required: true,
            message: t('oauthManagement.inputClientId'),
            trigger: ['change']
        }
    ],
    clientSecret: [
        {
            required: true,
            message: t('oauthManagement.inputClientSecret'),
            trigger: ['change']
        }
    ],
    accessTokenUrl: [
        {
            required: true,
            message: t('oauthManagement.inputAccessTokenUrl'),
            trigger: ['change']
        }
    ],
    authUrl: [
        {
            required: true,
            message: t('oauthManagement.inputAuthUrl'),
            trigger: ['change']
        }
    ],
    userUrl: [
        {
            required: true,
            message: t('oauthManagement.inputUserUrl'),
            trigger: ['change']
        }
    ],
    redirectUrl: [
        {
            required: true,
            message: t('oauthManagement.inputRedirectUrl'),
            trigger: ['change']
        }
    ]
}))

function syncModalLocale() {
    modelConfig.title = renderSence.value === 'edit' ? t('oauthManagement.editOauth') : t('oauthManagement.addOauth')
    modelConfig.okConfig.title = t('common.confirm')
    modelConfig.cancelConfig.title = t('common.cancel')
}

watch(locale, syncModalLocale)

function showModal(cb: () => void, data: any): void {
    callback.value = cb
    modelConfig.visible = true
    if (data) {
        Object.keys(formData).forEach((key: string) => {
            formData[key] = data[key]
        })
        renderSence.value = 'edit'
    } else {
        Object.keys(formData).forEach((key: string) => {
            formData[key] = ''
        })
        renderSence.value = 'new'
    }
    syncModalLocale()
    nextTick(() => {
        form.value?.resetFields()
    })
}

function okEvent() {
    form.value?.validate((valid: boolean) => {
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
.oauth-add-modal.zqy-block-modal {
    --oauth-modal-x-padding: 20px;
    --oauth-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--oauth-modal-x-padding) 8px !important;
        margin-right: 0;
        border-bottom: none;

        &::after {
            position: absolute;
            right: 0;
            bottom: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--oauth-modal-border-color);
        }
    }

    .el-dialog__title {
        display: block;
        line-height: 28px;
    }

    .el-dialog__headerbtn {
        top: 0;
        width: 42px;
        height: 46px;
    }

    .el-dialog__body {
        padding: 0 !important;
    }

    .el-dialog__footer {
        position: relative;
        min-height: 56px;
        padding: 12px var(--oauth-modal-x-padding);
        border-top: none;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--oauth-modal-border-color);
        }
    }

    .add-computer-group {
        padding: 14px var(--oauth-modal-x-padding) 4px;
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
        .el-select,
        .el-textarea {
            width: 100%;
        }

        .el-input__wrapper,
        .el-textarea__inner {
            border-radius: 2px;
        }
    }
}
</style>
