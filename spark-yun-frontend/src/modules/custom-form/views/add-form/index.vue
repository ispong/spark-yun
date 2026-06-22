<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="form" class="add-computer-group" label-position="top" :model="formData" :rules="rules">
            <el-form-item label="名称" prop="name">
                <el-input v-model="formData.name" maxlength="200" placeholder="请输入" />
            </el-form-item>
            <!-- <el-form-item label="计算集群" prop="clusterId">
                <el-select
                    v-model="formData.clusterId"
                    placeholder="请选择"
                    @visible-change="getClusterList"
                >
                    <el-option
                    v-for="item in clusterList"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                    />
                </el-select>
            </el-form-item> -->
            <el-form-item label="数据源" prop="datasourceId">
                <el-select
                    v-model="formData.datasourceId"
                    placeholder="请选择"
                    :disabled="isEdit"
                    @visible-change="getDataSourceList"
                    @change="dataSourceChange"
                >
                    <el-option
                        v-for="item in dataSourceList"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                    />
                </el-select>
            </el-form-item>
            <el-form-item v-if="!isEdit" label="模式" prop="createMode">
                <el-radio-group v-model="formData.createMode" :disabled="isEdit" @change="dataSourceChange">
                    <el-radio label="AUTO_TABLE">自动创建表</el-radio>
                    <el-radio label="EXIST_TABLE">选择已有表</el-radio>
                    <el-radio label="CREATE_TABLE">创建新表</el-radio>
                </el-radio-group>
            </el-form-item>
            <el-form-item v-if="isEdit" label="表名">
                <el-input v-model="formData.mainTable" disabled />
            </el-form-item>
            <el-form-item v-else-if="formData.createMode === 'EXIST_TABLE'" prop="mainTable" label="表（选择已有表名）">
                <el-select
                    v-model="formData.mainTable"
                    clearable
                    filterable
                    placeholder="请选择"
                    @visible-change="getDataSourceTable($event, formData.datasourceId)"
                >
                    <el-option
                        v-for="item in sourceTablesList"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                    />
                </el-select>
            </el-form-item>
            <el-form-item
                v-else-if="formData.createMode === 'CREATE_TABLE'"
                prop="mainTable"
                label="表（手动输入，生成新的表名）"
            >
                <el-input v-model="formData.mainTable" maxlength="20" placeholder="请输入" />
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
import { reactive, defineExpose, ref, nextTick } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { GetComputerGroupList, GetDatasourceList } from '@/app/shared/api/resources'
import { GetDataSourceTables } from '@/app/shared/api'

interface Option {
    label: string
    value: string
}

interface formDataParam {
    name: string
    // clusterId: string
    datasourceId: string
    createMode: string
    mainTable: string
    remark: string
    id?: string
}

const form = ref<FormInstance>()
const callback = ref<any>()
const clusterList = ref([]) // 计算集群
const dataSourceList = ref([]) // 数据源
const sourceTablesList = ref<Option[]>([])
const isEdit = ref(false)

const modelConfig = reactive({
    title: '新建表单',
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
    closeOnClickModal: false,
    customClass: 'custom-form-create-modal'
})
const formData = reactive<formDataParam>({
    name: '',
    // clusterId: '',
    datasourceId: '',
    createMode: 'AUTO_TABLE',
    mainTable: '',
    remark: '',
    id: ''
})
const rules = reactive<FormRules>({
    name: [
        {
            required: true,
            message: '请输入表单名称',
            trigger: ['change', 'blur']
        }
    ],
    clusterId: [
        {
            required: true,
            message: '请选择计算集群',
            trigger: ['change', 'blur']
        }
    ],
    datasourceId: [
        {
            required: true,
            message: '请选择数据源',
            trigger: ['change', 'blur']
        }
    ],
    mainTable: [
        {
            required: true,
            message: '请选择表名',
            trigger: ['change', 'blur']
        }
    ]
    // mainTable_name: [
    //     {
    //         required: true,
    //         message: '请输入表名',
    //         trigger: ['change', 'blur']
    //     }
    // ]
})

function showModal(cb: () => void, data?: formDataParam): void {
    callback.value = cb
    modelConfig.visible = true
    if (data) {
        Object.keys(data).forEach((key: string) => {
            formData[key] = data[key]
        })
        isEdit.value = true
        getDataSourceList(true)
        getDataSourceTable(true, formData.datasourceId)
        modelConfig.title = '编辑表单'
    } else {
        formData.name = ''
        // formData.clusterId = ''
        formData.datasourceId = ''
        formData.createMode = 'AUTO_TABLE'
        formData.mainTable = ''
        formData.remark = ''
        formData.id = ''
        isEdit.value = false
        modelConfig.title = '新建表单'
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
                .catch(() => {
                    modelConfig.okConfig.loading = false
                })
        } else {
            ElMessage.warning('请将表单输入完整')
        }
    })
}

function dataSourceChange(e: string) {
    formData.mainTable = ''
    sourceTablesList.value = []
}

// 查询计算集群
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

// 查询数据源
function getDataSourceList(e: boolean, searchType?: string) {
    if (e) {
        GetDatasourceList({
            page: 0,
            pageSize: 10000,
            searchKeyWord: searchType || ''
        })
            .then((res: any) => {
                dataSourceList.value = res.data.content.map((item: any) => {
                    return {
                        label: item.name,
                        value: item.id
                    }
                })
            })
            .catch(() => {
                dataSourceList.value = []
            })
    }
}
// 获取数据源表
function getDataSourceTable(e: boolean, dataSourceId: string) {
    if (e && dataSourceId) {
        let options = []
        GetDataSourceTables({
            dataSourceId: dataSourceId,
            tablePattern: ''
        })
            .then((res: any) => {
                options = res.data.tables.map((item: any) => {
                    return {
                        label: item,
                        value: item
                    }
                })
                sourceTablesList.value = options
            })
            .catch((err) => {
                console.error(err)
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
.custom-form-create-modal.zqy-block-modal {
    --custom-form-modal-x-padding: 20px;
    --custom-form-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--custom-form-modal-x-padding) 8px !important;
        margin-right: 0;
        border-bottom: none;

        &::after {
            position: absolute;
            right: 0;
            bottom: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--custom-form-modal-border-color);
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
        padding: 12px var(--custom-form-modal-x-padding);
        border-top: none;
        align-items: center;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--custom-form-modal-border-color);
        }
    }

    .add-computer-group {
        padding: 14px var(--custom-form-modal-x-padding) 4px;
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

        .el-radio-group {
            display: flex;
            flex-wrap: wrap;
            gap: 8px 16px;
            line-height: 20px;
        }

        .el-radio {
            height: 24px;
            margin-right: 0;
        }
    }
}
</style>
