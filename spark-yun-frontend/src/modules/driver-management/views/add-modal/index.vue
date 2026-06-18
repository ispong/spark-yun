<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="form" class="add-driver-form" label-position="top" :model="formData" :rules="rules">
            <el-form-item label="名称" prop="name">
                <el-input
                    v-model="formData.name"
                    maxlength="200"
                    placeholder="请输入"
                    :disabled="renderSence === 'edit'"
                />
            </el-form-item>
            <el-form-item v-if="renderSence === 'new'" label="类型" prop="dbType">
                <el-select v-model="formData.dbType" placeholder="请选择">
                    <el-option v-for="item in typeList" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
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
            <el-form-item v-if="renderSence === 'new'" label="驱动" prop="driver">
                <el-upload
                    ref="uploadRef"
                    class="driver-upload"
                    action=""
                    :limit="1"
                    :multiple="false"
                    :drag="true"
                    :auto-upload="false"
                    :on-change="handleChange"
                    :on-remove="removeChange"
                >
                    <el-icon class="el-icon--upload">
                        <upload-filled />
                    </el-icon>
                    <div class="el-upload__text">
                        上传驱动
                        <em>点击上传</em>
                    </div>
                </el-upload>
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
const uploadRef = ref()
const renderSence = ref<string>('new')
const modelConfig = reactive({
    title: '新建驱动',
    visible: false,
    width: '520px',
    customClass: 'driver-add-modal',
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
    dbType: '',
    remark: '',
    driver: null,
    id: ''
})
const typeList = reactive([
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
const rules = reactive<FormRules>({
    name: [
        {
            required: true,
            message: '请输入驱动名称',
            trigger: ['blur', 'change']
        }
    ],
    dbType: [
        {
            required: true,
            message: '请选择类型',
            trigger: ['blur', 'change']
        }
    ],
    driver: [
        {
            required: true,
            message: '请上传驱动',
            trigger: 'change'
        }
    ]
})

function showModal(cb: () => void, data: any): void {
    callback.value = cb
    modelConfig.visible = true
    renderSence.value = 'new'
    formData.driver = null
    uploadRef.value?.clearFiles()
    if (data) {
        formData.name = data.name
        formData.dbType = data.dbType
        formData.remark = data.remark
        formData.id = data.id
        modelConfig.title = '编辑备注'
        renderSence.value = 'edit'
    } else {
        formData.name = ''
        formData.dbType = ''
        formData.remark = ''
        formData.id = ''
        modelConfig.title = '新建驱动'
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

function handleChange(e: any) {
    formData.driver = e.raw
    form.value?.validateField('driver')
}

function removeChange() {
    formData.driver = null
    uploadRef.value.clearFiles()
    form.value?.validateField('driver')
}

function closeEvent() {
    modelConfig.visible = false
    modelConfig.okConfig.loading = false
    formData.driver = null
    uploadRef.value?.clearFiles()
}

defineExpose({
    showModal
})
</script>

<style lang="scss">
.driver-add-modal.zqy-block-modal {
    --driver-modal-x-padding: 20px;
    --driver-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--driver-modal-x-padding) 8px !important;
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
            background-color: var(--driver-modal-border-color);
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
        padding: 12px var(--driver-modal-x-padding);
        align-items: center;
        border-top: none;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--driver-modal-border-color);
        }
    }

    .add-driver-form {
        padding: 14px var(--driver-modal-x-padding) 4px;
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
        .el-textarea,
        .driver-upload {
            width: 100%;
        }

        .el-input__wrapper,
        .el-textarea__inner {
            border-radius: 2px;
        }
    }

    .driver-upload {
        margin: 0;

        .el-upload {
            width: 100%;
        }

        .el-upload-dragger {
            width: 100%;
            padding: 18px 0;
            border-radius: 2px;
        }

        .el-icon--upload {
            margin-bottom: 8px;
            font-size: 40px;
            line-height: 1;
        }

        .el-upload__text {
            font-size: getCssVar('font-size', 'extra-small');
        }
    }
}
</style>
