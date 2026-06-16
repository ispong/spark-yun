<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="form" class="add-computer-group" label-position="top" :model="formData" :rules="rules">
            <el-form-item label="成员" prop="userId">
                <el-select v-model="formData.userId" placeholder="请选择">
                    <el-option v-for="item in userList" :key="item.id" :label="item.username" :value="item.id" />
                </el-select>
            </el-form-item>
            <el-form-item label="租户管理员">
                <el-switch v-model="formData.isTenantAdmin" />
            </el-form-item>
        </el-form>
    </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, defineExpose, ref, nextTick } from 'vue'
import BlockModal from '@/app/components/block-modal/index.vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { GetUserInfoList } from '@/app/management/tenant-user/api'

const form = ref<FormInstance>()
const callback = ref<any>()
const userList = ref([])
const modelConfig = reactive({
    title: '添加成员',
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
    customClass: 'tenant-user-add-modal',
    closeOnClickModal: false
})
const formData = reactive({
    isTenantAdmin: false,
    userId: '',
    id: ''
})
const rules = reactive<FormRules>({
    userId: [
        {
            required: true,
            message: '请选择成员',
            trigger: ['change']
        }
    ]
})

function showModal(cb: () => void, data: any): void {
    callback.value = cb
    modelConfig.visible = true
    getUserOfSystem()
    if (data) {
        formData.userId = data.userId
        formData.isTenantAdmin = data.normalAdmin || false
        formData.id = data.id
        modelConfig.title = '编辑成员'
    } else {
        formData.userId = ''
        formData.isTenantAdmin = false
        formData.id = ''
        modelConfig.title = '添加成员'
    }
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
            userList.value = res.data.content
        })
        .catch(() => {
            userList.value = []
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
.tenant-user-add-modal.zqy-block-modal {
    --tenant-user-modal-x-padding: 20px;
    --tenant-user-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--tenant-user-modal-x-padding) 8px !important;
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
            background-color: var(--tenant-user-modal-border-color);
        }
    }
    .el-dialog__body {
        padding: 0 !important;
    }
    .el-dialog__footer {
        position: relative;
        min-height: 56px;
        padding: 12px var(--tenant-user-modal-x-padding);
        align-items: center;
        border-top: none;
        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--tenant-user-modal-border-color);
        }
    }
    .add-computer-group {
        padding: 14px var(--tenant-user-modal-x-padding) 4px;
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
        .el-select {
            width: 100%;
        }
        .el-input__wrapper {
            border-radius: 2px;
        }
    }
}
</style>
