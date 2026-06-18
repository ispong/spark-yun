<template>
    <BlockModal :model-config="modelConfig">
        <el-form ref="form" class="file-center-form" label-position="top" :model="formData" :rules="rules">
            <el-form-item label="类型" prop="type">
                <el-select v-model="formData.type" placeholder="请选择" :disabled="renderSence === 'edit'">
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
        </el-form>
        <el-upload
            v-if="renderSence === 'new'"
            class="file-center-upload"
            action=""
            :limit="100"
            :multiple="true"
            :drag="true"
            :auto-upload="false"
            :file-list="fileList"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
        >
            <el-icon class="el-icon--upload">
                <upload-filled />
            </el-icon>
            <div class="el-upload__text">
                上传附件（支持多文件上传）
                <em>点击上传</em>
            </div>
        </el-upload>
    </BlockModal>
</template>

<script lang="ts" setup>
import { computed, defineExpose, reactive, ref, watch } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'

const form = ref<FormInstance>()
const callback = ref<((data: any) => Promise<any>) | null>(null)
const props = withDefaults(
    defineProps<{
        showExcelType?: boolean
    }>(),
    {
        showExcelType: true
    }
)
const allTypeList = [
    {
        label: '作业',
        value: 'JOB'
    },
    {
        label: '函数',
        value: 'FUNC'
    },
    {
        label: '依赖',
        value: 'LIB'
    },
    {
        label: 'Excel',
        value: 'EXCEL'
    }
]
const typeList = computed(() =>
    props.showExcelType ? allTypeList : allTypeList.filter((item) => item.value !== 'EXCEL')
)
const renderSence = ref<'new' | 'edit'>('new')
const fileList = ref<any[]>([]) // el-upload 使用的文件列表
const modelConfig = reactive({
    title: '上传资源',
    visible: false,
    width: '520px',
    customClass: 'file-center-add-modal',
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
    id: null as number | null,
    type: '',
    remark: '',
    fileData: [] as File[] // 始终是数组，空表示无新文件
})
const rules = reactive<FormRules>({
    type: [
        {
            required: true,
            message: '请选择类型',
            trigger: ['blur', 'change']
        }
    ]
})

// 实时同步 fileList → formData.fileData
watch(
    fileList,
    (newList) => {
        formData.fileData = newList.filter((item) => item.raw).map((item) => item.raw as File)
    },
    {
        deep: true
    }
)

function showModal(cb: (data: any) => Promise<any>, data?: any): void {
    // 重置状态
    formData.id = null
    formData.type = ''
    formData.remark = ''
    formData.fileData = []
    fileList.value = []
    renderSence.value = 'new'
    modelConfig.title = '上传资源'

    if (data) {
        modelConfig.title = '编辑资源'
        formData.type = data.fileType
        formData.remark = data.remark
        formData.id = data.id
        renderSence.value = 'edit'
    }

    callback.value = cb
    modelConfig.visible = true
}

function okEvent() {
    if (renderSence.value === 'new' && formData.fileData.length === 0) {
        ElMessage.warning('请上传至少一个文件')
        return
    }

    form.value?.validate((valid: boolean) => {
        if (!valid) {
            ElMessage.warning('请将表单输入完整')
            return
        }

        modelConfig.okConfig.loading = true

        callback.value!(formData)
            .then((res: any) => {
                modelConfig.okConfig.loading = false
                if (res === undefined) {
                    closeEvent()
                }
            })
            .catch(() => {
                modelConfig.okConfig.loading = false
            })
    })
}

function closeEvent() {
    modelConfig.visible = false
    modelConfig.okConfig.loading = false
    fileList.value = []
    formData.fileData = []
}

function handleFileChange(file: any, newFileList: any[]) {
    fileList.value = newFileList
}

function handleFileRemove(file: any, newFileList: any[]) {
    fileList.value = newFileList
}

defineExpose({
    showModal
})
</script>

<style lang="scss">
.file-center-add-modal.zqy-block-modal {
    --file-modal-x-padding: 20px;
    --file-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--file-modal-x-padding) 8px !important;
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
            background-color: var(--file-modal-border-color);
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
        padding: 12px var(--file-modal-x-padding);
        align-items: center;
        border-top: none;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--file-modal-border-color);
        }
    }

    .file-center-form {
        padding: 14px var(--file-modal-x-padding) 4px;
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

    .file-center-upload {
        width: calc(100% - var(--file-modal-x-padding) * 2);
        margin: 0 var(--file-modal-x-padding) 20px;

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
