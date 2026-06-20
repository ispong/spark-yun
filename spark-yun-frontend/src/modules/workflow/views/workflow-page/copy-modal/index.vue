<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="form" class="workflow-job-copy-form" label-position="top" :model="formData" :rules="rules">
            <el-form-item label="名称" prop="name">
                <el-input v-model="formData.name" maxlength="200" placeholder="请输入" />
            </el-form-item>
        </el-form>
    </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, defineExpose, ref, nextTick } from 'vue'
import BlockModal from '@/app/components/block-modal/index.vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'

const form = ref<FormInstance>()
const callback = ref<any>()
const renderSense = ref('')

const modelConfig = reactive({
    title: '复制作业',
    visible: false,
    width: '520px',
    customClass: 'workflow-job-copy-modal',
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
    name: '',
    id: ''
})
const rules = reactive<FormRules>({
    name: [
        {
            required: true,
            message: '请输入作业名称',
            trigger: ['blur', 'change']
        }
    ]
})

function showModal(cb: () => void, data: any): void {
    callback.value = cb
    modelConfig.visible = true
    formData.name = data.name + '_copy'
    formData.id = data.id
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
                    workName: formData.name,
                    workId: formData.id
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
.workflow-job-copy-modal.zqy-block-modal {
    --workflow-job-copy-modal-x-padding: 20px;
    --workflow-job-copy-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--workflow-job-copy-modal-x-padding) 8px !important;
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
            background-color: var(--workflow-job-copy-modal-border-color);
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
        padding: 12px var(--workflow-job-copy-modal-x-padding);
        border-top: none;
        align-items: center;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--workflow-job-copy-modal-border-color);
        }
    }

    .workflow-job-copy-form {
        padding: 14px var(--workflow-job-copy-modal-x-padding) 4px;
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
