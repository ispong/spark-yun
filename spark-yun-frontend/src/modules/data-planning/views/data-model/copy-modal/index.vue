<template>
    <BlockModal :model-config="modelConfig">
        <el-form
            ref="form"
            class="data-model-copy-form"
            label-position="top"
            :model="formData"
            :rules="rules"
        >
            <el-form-item label="名称" prop="name">
                <el-input v-model="formData.name" maxlength="200" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="数据分层" prop="layerId">
                <el-select v-model="formData.layerId" filterable clearable placeholder="请选择">
                    <el-option
                        v-for="item in parentLayerIdList"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="表名" prop="tableName">
                <el-input v-model="formData.tableName" maxlength="200" placeholder="请输入" />
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
import { GetDataLayerList } from '../../../api/data-layer'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { GetDatasourceList } from '@/app/shared/api/resources'
import { GetDataSourceTables } from '@/app/shared/api'
import { useRoute } from 'vue-router'

interface Option {
    label: string
    value: string
}

const route = useRoute()

const form = ref<FormInstance>()
const callback = ref<any>()
const parentLayerIdList = ref<Option[]>([])
const modelTypeList = ref<Option[]>([
    {
        label: '原始模型',
        value: 'ORIGIN_MODEL'
    },
    {
        label: '关联模型',
        value: 'LINK_MODEL'
    }
])
const dbTypeList = ref<Option[]>([
    {
        label: 'Clickhouse',
        value: 'CLICKHOUSE'
    },
    {
        label: 'Db2',
        value: 'DB2'
    },
    {
        label: 'Doris',
        value: 'DORIS'
    },
    {
        label: 'DuckDB',
        value: 'DUCK_DB'
    },
    {
        label: '达梦',
        value: 'DM'
    },
    {
        label: 'Gauss',
        value: 'GAUSS'
    },
    {
        label: 'Gbase',
        value: 'GBASE'
    },
    {
        label: 'Greenplum',
        value: 'GREENPLUM'
    },
    {
        label: 'H2',
        value: 'H2'
    },
    {
        label: 'HanaSap',
        value: 'HANA_SAP'
    },
    {
        label: 'Hive',
        value: 'HIVE'
    },
    {
        label: 'Impala',
        value: 'IMPALA'
    },
    {
        label: 'Mysql',
        value: 'MYSQL'
    },
    {
        label: 'OceanBase',
        value: 'OCEANBASE'
    },
    {
        label: 'OpenGauss',
        value: 'OPEN_GAUSS'
    },
    {
        label: 'Oracle',
        value: 'ORACLE'
    },
    {
        label: 'PostgreSql',
        value: 'POSTGRE_SQL'
    },
    {
        label: 'Presto',
        value: 'PRESTO'
    },
    {
        label: 'SelectDB',
        value: 'SELECT_DB'
    },
    {
        label: 'SqlServer',
        value: 'SQL_SERVER'
    },
    {
        label: 'StarRocks',
        value: 'STAR_ROCKS'
    },
    {
        label: 'Sybase',
        value: 'SYBASE'
    },
    {
        label: 'TDengine',
        value: 'T_DENGINE'
    },
    {
        label: 'TiDB',
        value: 'TIDB'
    },
    {
        label: 'Trino',
        value: 'TRINO'
    }
])
const dataSourceList = ref<Option[]>([])
const tableNameList = ref<Option[]>([])

const modelConfig = reactive({
    title: '添加',
    visible: false,
    width: '520px',
    customClass: 'data-model-copy-modal',
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
const formData = reactive<any>({
    name: '',
    layerId: '',
    modelType: '', // 模型类型 ORIGIN_MODEL | LINK_MODEL
    dbType: '', // 数据源类型
    datasourceId: '',
    tableName: '',
    tableConfig: {}, // 高级配置
    remark: '',
    id: ''
})
const rules = reactive<FormRules>({
    name: [
        {
            required: true,
            message: '请输入采集任务名称',
            trigger: ['blur', 'change']
        }
    ],
    layerId: [
        {
            required: true,
            message: '请选择数据分层',
            trigger: ['blur', 'change']
        }
    ],
    // modelType: [{ required: true, message: '请选择模型类型', trigger: ['blur', 'change'] }],
    // dbType: [{ required: true, message: '请选择数据源类型', trigger: ['blur', 'change'] }],
    // datasourceId: [{ required: true, message: '请选择数据源', trigger: ['blur', 'change'] }],
    tableName: [
        {
            required: true,
            message: '请选择表名',
            trigger: ['blur', 'change']
        }
    ]
})

function showModal(cb: () => void, data: any): void {
    Object.keys(formData).forEach((key: string) => {
        formData[key] = data[key]
    })
    formData.name = formData.name + '_copy'
    modelConfig.title = '复制'

    getParentLayerIList()
    getDataSourceList(true)
    getDataSourceTable(true)

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

function getParentLayerIList() {
    GetDataLayerList({
        page: 0,
        pageSize: 10000,
        searchKeyWord: ''
    })
        .then((res: any) => {
            parentLayerIdList.value = [
                ...res.data.content.map((item: any) => {
                    return {
                        label: item.fullPathName,
                        value: item.id
                    }
                })
            ]
        })
        .catch(() => {
            parentLayerIdList.value = []
        })
}

function closeEvent() {
    modelConfig.visible = false
    modelConfig.okConfig.loading = false
}

function dbTypeChangeEvent() {
    formData.datasourceId = ''
    datasouceChangeEvent()
}

function datasouceChangeEvent() {
    formData.tableName = ''
}

function getDataSourceList(e: boolean, searchType?: string) {
    if (e && formData.dbType) {
        GetDatasourceList({
            page: 0,
            pageSize: 10000,
            searchKeyWord: searchType || '',
            datasourceType: formData.dbType
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
    } else {
        dataSourceList.value = []
    }
}

function getDataSourceTable(e: boolean) {
    if (e && formData.datasourceId) {
        GetDataSourceTables({
            dataSourceId: formData.datasourceId,
            tablePattern: ''
        })
            .then((res: any) => {
                tableNameList.value = res.data.tables.map((item: any) => {
                    return {
                        label: item,
                        value: item
                    }
                })
            })
            .catch(() => {
                tableNameList.value = []
            })
    } else {
        tableNameList.value = []
    }
}

defineExpose({
    showModal
})
</script>

<style lang="scss">
.data-model-copy-modal.zqy-block-modal {
    --data-planning-modal-x-padding: 20px;
    --data-planning-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--data-planning-modal-x-padding) 8px !important;
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
            background-color: var(--data-planning-modal-border-color);
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
        padding: 12px var(--data-planning-modal-x-padding);
        align-items: center;
        border-top: none;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--data-planning-modal-border-color);
        }
    }

    .data-model-copy-form {
        padding: 14px var(--data-planning-modal-x-padding) 4px;
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
