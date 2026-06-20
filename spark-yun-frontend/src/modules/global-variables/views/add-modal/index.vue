<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="form" class="global-variables-add-form" label-position="top" :model="formData" :rules="rules">
            <el-form-item label="变量名" prop="keyName">
                <el-input v-model="formData.keyName" maxlength="200" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="变量值" prop="secretValue">
                <el-input v-model="formData.secretValue" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="备注">
                <el-input
                    v-model="formData.remark"
                    type="textarea"
                    maxlength="200"
                    :autosize="{ minRows: 4, maxRows: 4 }"
                    placeholder="请输入"
                />
            </el-form-item>
        </el-form>
    </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, defineExpose, ref } from 'vue'
import BlockModal from '@/app/components/block-modal/index.vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
const form = ref<FormInstance>()
const callback = ref<any>()
const modelConfig = reactive({
    title: '新建变量',
    visible: false,
    width: '520px',
    customClass: 'global-variables-add-modal',
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
    closeOnClickModal: false
})
const formData = reactive({
    keyName: '',
    secretValue: '',
    remark: '',
    id: ''
})
const rules = reactive<FormRules>({
    keyName: [
        {
            required: true,
            message: '请输入变量名',
            trigger: ['blur', 'change']
        }
    ],
    secretValue: [
        {
            required: true,
            message: '请输入变量值',
            trigger: ['blur', 'change']
        }
    ]
})
function showModal(cb: () => void, data: any): void {
    if (data) {
        formData.keyName = data.keyName
        formData.secretValue = ''
        formData.remark = data.remark
        formData.id = data.id
        modelConfig.title = '编辑变量'
    } else {
        formData.keyName = ''
        formData.secretValue = ''
        formData.remark = ''
        modelConfig.title = '新建变量'
    }
    callback.value = cb
    modelConfig.visible = true
}
function okEvent() {
    form.value?.validate((valid: boolean) => {
        if (valid) {
            modelConfig.okConfig.loading = true
            callback
                .value(formData)
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
.global-variables-add-modal.zqy-block-modal {
    --global-variables-modal-x-padding: 20px;
    --global-variables-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--global-variables-modal-x-padding) 8px !important;
        margin-right: 0;
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
            background-color: var(--global-variables-modal-border-color);
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
        padding: 12px var(--global-variables-modal-x-padding);
        border-top: none;
        align-items: center;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--global-variables-modal-border-color);
        }
    }

    .global-variables-add-form {
        box-sizing: border-box;
        padding: 14px var(--global-variables-modal-x-padding) 4px;

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

        .el-input__inner,
        .el-textarea__inner {
            font-size: getCssVar('font-size', 'base');
        }
    }
}
</style>
