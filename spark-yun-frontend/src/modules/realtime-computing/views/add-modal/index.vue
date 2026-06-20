<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="form" class="add-computer-group" label-position="top" :model="formData" :rules="rules">
            <el-form-item label="名称" prop="name">
                <el-input v-model="formData.name" maxlength="200" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="计算集群" prop="clusterId">
                <el-select v-model="formData.clusterId" placeholder="请选择" @visible-change="getClusterList">
                    <el-option v-for="item in clusterList" :key="item.value" :label="item.label" :value="item.value" />
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
    </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, defineExpose, ref, nextTick } from 'vue'
import BlockModal from '@/app/components/block-modal/index.vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { GetComputerGroupList, GetComputerPointData, GetDatasourceList } from '@/app/shared/api/resources'

const form = ref<FormInstance>()
const callback = ref<any>()
const clusterList = ref([]) // 计算集群
const clusterNodeList = ref([]) // 集群节点
const dataSourceList = ref([]) // 数据源
const showForm = ref(true)
const renderSense = ref('')

const modelConfig = reactive({
    title: '新建实时',
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
    customClass: 'realtime-computing-add-modal',
    closeOnClickModal: false
})
const formData = reactive({
    name: '',
    clusterId: '', // 计算集群
    remark: '',
    id: ''
})
const rules = reactive<FormRules>({
    name: [
        {
            required: true,
            message: '请输入作业名称',
            trigger: ['blur', 'change']
        }
    ],
    clusterId: [
        {
            required: true,
            message: '请选择计算集群',
            trigger: ['blur', 'change']
        }
    ]
})

function showModal(cb: () => void, data: any): void {
    callback.value = cb
    modelConfig.visible = true
    if (data && data.id) {
        Object.keys(data).forEach((key: string) => {
            formData[key] = data[key]
        })
        formData.clusterId && getClusterList(true)
        modelConfig.title = '编辑实时'
        renderSense.value = 'edit'
    } else {
        formData.name = ''
        formData.remark = ''
        formData.clusterId = ''
        formData.id = ''
        modelConfig.title = '新建实时'
        renderSense.value = 'new'
    }
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
                .catch((err: any) => {
                    modelConfig.okConfig.loading = false
                })
        } else {
            ElMessage.warning('请将表单输入完整')
        }
    })
}

function getClusterList(e: boolean) {
    if (e) {
        GetComputerGroupList({
            page: 0,
            pageSize: 10000,
            searchKeyWord: ''
        })
            .then((res: any) => {
                clusterList.value = res.data.content.map((item: any) => {
                    return {
                        label: item.name,
                        value: item.id
                    }
                })
            })
            .catch(() => {
                clusterList.value = []
            })
    }
}

function closeEvent() {
    modelConfig.visible = false
}

defineExpose({
    showModal
})
</script>

<style lang="scss">
.realtime-computing-add-modal.zqy-block-modal {
    --realtime-modal-x-padding: 20px;
    --realtime-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--realtime-modal-x-padding) 8px !important;
        margin-right: 0;
        border-bottom: none;

        &::after {
            position: absolute;
            right: 0;
            bottom: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--realtime-modal-border-color);
        }

        .el-dialog__headerbtn {
            top: 0;
            width: 42px;
            height: 46px;
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
        padding: 12px var(--realtime-modal-x-padding);
        border-top: none;
        align-items: center;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--realtime-modal-border-color);
        }
    }

    .add-computer-group {
        padding: 14px var(--realtime-modal-x-padding) 4px;
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
        .el-select__wrapper,
        .el-textarea__inner {
            border-radius: 2px;
        }

        .el-input__inner,
        .el-select__selected-item,
        .el-textarea__inner {
            font-size: getCssVar('font-size', 'base');
        }
    }
}
</style>
