<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="form" class="metadata-remark-form zqy-block-modal-form" label-position="top" :model="formData">
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
import { ElMessage, FormInstance } from 'element-plus'

const form = ref<FormInstance>()
const callback = ref<any>()

const modelConfig = reactive({
    title: '备注',
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
    zIndex: 3100,
    customClass: 'metadata-remark-modal',
    closeOnClickModal: false
})
const formData = reactive({
    remark: '',
    id: ''
})

function showModal(cb: () => void, data: any): void {
    formData.remark = data.remark

    callback.value = cb
    modelConfig.visible = true
}

function okEvent() {
    form.value?.validate((valid) => {
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
.metadata-remark-modal.zqy-block-modal {
    .metadata-remark-form {
        .el-form-item__content {
            position: relative;
            flex-wrap: nowrap;
            justify-content: space-between;
        }
    }
}
</style>
