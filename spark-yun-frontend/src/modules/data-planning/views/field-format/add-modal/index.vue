<template>
    <BlockModal :model-config="modelConfig">
        <el-form
            ref="form"
            class="field-format-add-form"
            label-position="top"
            :model="formData"
            :rules="rules"
            :disabled="readonly"
        >
            <el-form-item label="名称" prop="name">
                <el-input v-model="formData.name" maxlength="500" placeholder="请输入" />
            </el-form-item>
            <el-form-item prop="columnRuleInput" class="label-tooltip-form-item">
                <template #label>
                    <el-tooltip
                        content="支持前缀/后缀/包含/精确匹配，系统会自动转为正则保存"
                        placement="top"
                        :enterable="false"
                    >
                        <span class="label-tooltip-text">字段名规范</span>
                    </el-tooltip>
                </template>
                <div class="table-rule-config">
                    <el-select v-model="columnRuleMode">
                        <el-option
                            v-for="item in columnRuleModeList"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                    <el-input v-model="formData.columnRuleInput" maxlength="500" :placeholder="columnRulePlaceholder" />
                </div>
            </el-form-item>
            <el-form-item label="字段类型" prop="columnTypeCode">
                <el-select v-model="formData.columnTypeCode" filterable placeholder="请选择">
                    <el-option
                        v-for="item in fieldTypeList"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                    />
                </el-select>
            </el-form-item>
            <el-form-item label="字段精度">
                <el-input v-model="formData.columnType" maxlength="200" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="属性" class="field-attr-row">
                <el-checkbox v-model="formData.isPrimary" true-label="ENABLE" false-label="DISABLE">主键</el-checkbox>
                <el-checkbox v-model="formData.isNull" true-label="ENABLE" false-label="DISABLE">非空</el-checkbox>
                <el-checkbox v-model="formData.isDuplicate" true-label="ENABLE" false-label="DISABLE">唯一</el-checkbox>
                <el-checkbox v-model="formData.isPartition" true-label="ENABLE" false-label="DISABLE">
                    分区键
                </el-checkbox>
            </el-form-item>
            <el-form-item label="默认值" prop="defaultValue" :show-message="false">
                <el-input
                    v-model="formData.defaultValue"
                    maxlength="500"
                    :placeholder="formData.isNull === 'ENABLE' ? '勾选非空时，请输入默认值' : '请输入'"
                />
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
import { reactive, defineExpose, ref, watch, computed } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'

interface Option {
    label: string
    value: string
}

type ColumnRuleMode = 'prefix' | 'suffix' | 'contains' | 'exact' | 'regex'

const form = ref<FormInstance>()
const callback = ref<any>()
const readonly = ref<boolean>(false)
const columnRuleMode = ref<ColumnRuleMode>('prefix')
const columnRuleModeList: Option[] = [
    {
        label: '前缀匹配',
        value: 'prefix'
    },
    {
        label: '后缀匹配',
        value: 'suffix'
    },
    {
        label: '包含匹配',
        value: 'contains'
    },
    {
        label: '精确匹配',
        value: 'exact'
    },
    {
        label: '自定义正则',
        value: 'regex'
    }
]
const fieldTypeList = ref<Option[]>([
    {
        label: '大文本',
        value: 'TEXT'
    },
    {
        label: '字符串',
        value: 'STRING'
    },
    {
        label: '日期',
        value: 'DATE'
    },
    {
        label: '日期时间',
        value: 'DATETIME'
    },
    {
        label: '整数',
        value: 'INT'
    },
    {
        label: '小数',
        value: 'DOUBLE'
    },
    {
        label: '自定义',
        value: 'CUSTOM'
    }
])

const modelConfig = reactive({
    title: '新建标准',
    visible: false,
    width: '520px',
    customClass: 'field-format-add-modal',
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
    columnTypeCode: '',
    columnType: '',
    isNull: 'DISABLE',
    isPrimary: 'DISABLE',
    isDuplicate: 'DISABLE',
    isPartition: 'DISABLE',
    defaultValue: '',
    columnRuleInput: '',
    columnRule: '',
    remark: '',
    id: ''
})
const rules = reactive<FormRules>({
    name: [
        {
            required: true,
            message: '请输入名称',
            trigger: ['blur', 'change']
        }
    ],
    columnTypeCode: [
        {
            required: true,
            message: '请选择字段类型',
            trigger: ['blur', 'change']
        }
    ],
    columnType: [
        {
            required: true,
            message: '请输入字段精度',
            trigger: ['blur', 'change']
        }
    ],
    columnRuleInput: [
        {
            validator: (_rule, value, callback) => {
                if (value?.trim() && columnRuleMode.value === 'regex') {
                    try {
                        new RegExp(value)
                    } catch (error) {
                        callback(new Error('请输入合法的正则表达式'))
                        return
                    }
                }
                callback()
            },
            trigger: ['blur', 'change']
        }
    ],
    defaultValue: [
        {
            trigger: ['blur', 'change'],
            validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
                if (formData.isNull === 'ENABLE' && !value?.trim()) {
                    callback(new Error('勾选非空时，请输入默认值'))
                    return
                }
                callback()
            }
        }
    ]
})

const columnRulePlaceholder = computed(() => {
    if (columnRuleMode.value === 'regex') {
        return '请输入正则表达式，例如：^user_.*'
    }
    return '请输入关键字，例如：user_'
})

watch(
    () => formData.isNull,
    () => {
        form.value?.validateField('defaultValue')
    }
)

function showModal(cb: () => void, data: any, type?: string): void {
    modelConfig.okConfig = {
        title: '确定',
        ok: okEvent,
        disabled: false,
        loading: false
    }
    if (data) {
        readonly.value = false
        Object.keys(formData).forEach((key: string) => {
            formData[key] = data[key]
        })
        const columnRuleInfo = parseColumnRule(formData.columnRule)
        columnRuleMode.value = columnRuleInfo.mode
        formData.columnRuleInput = columnRuleInfo.input
        modelConfig.title = '编辑标准'
    } else {
        readonly.value = false
        const keys = ['isNull', 'isPrimary', 'isDuplicate', 'isPartition']
        Object.keys(formData).forEach((key: string) => {
            formData[key] = ''
            if (keys.includes(key)) {
                formData[key] = 'DISABLE'
            }
        })
        columnRuleMode.value = 'prefix'
        modelConfig.title = '新建标准'
    }

    if (type === 'readonly') {
        readonly.value = true
        modelConfig.title = '查看'
        modelConfig.okConfig = null
    }

    callback.value = cb
    modelConfig.visible = true
}

function okEvent() {
    form.value?.validate((valid: boolean) => {
        if (valid) {
            modelConfig.okConfig.loading = true
            const submitData = {
                ...formData,
                columnRule: buildColumnRule(columnRuleMode.value, formData.columnRuleInput)
            }
            callback
                .value(submitData)
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
    modelConfig.okConfig && (modelConfig.okConfig.loading = false)
}

function escapeRegexChar(value: string) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function unEscapeRegexChar(value: string) {
    return value.replace(/\\([.*+?^${}()|[\]\\])/g, '$1')
}

function buildColumnRule(mode: ColumnRuleMode, inputValue: string) {
    const trimmedValue = (inputValue || '').trim()
    if (!trimmedValue) {
        return ''
    }
    if (mode === 'regex') {
        return trimmedValue
    }

    const safeValue = escapeRegexChar(trimmedValue)
    if (mode === 'prefix') {
        return `^${safeValue}.*`
    }
    if (mode === 'suffix') {
        return `.*${safeValue}$`
    }
    if (mode === 'contains') {
        return `.*${safeValue}.*`
    }
    return `^${safeValue}$`
}

function parseColumnRule(value: string): { mode: ColumnRuleMode; input: string } {
    if (!value) {
        return {
            mode: 'prefix',
            input: ''
        }
    }

    const prefixMatch = value.match(/^\^(.+)\.\*$/)
    if (prefixMatch?.[1]) {
        return {
            mode: 'prefix',
            input: unEscapeRegexChar(prefixMatch[1])
        }
    }

    const suffixMatch = value.match(/^\.\*(.+)\$$/)
    if (suffixMatch?.[1]) {
        return {
            mode: 'suffix',
            input: unEscapeRegexChar(suffixMatch[1])
        }
    }

    const containsMatch = value.match(/^\.\*(.+)\.\*$/)
    if (containsMatch?.[1]) {
        return {
            mode: 'contains',
            input: unEscapeRegexChar(containsMatch[1])
        }
    }

    const exactMatch = value.match(/^\^(.+)\$$/)
    if (exactMatch?.[1]) {
        return {
            mode: 'exact',
            input: unEscapeRegexChar(exactMatch[1])
        }
    }

    return {
        mode: 'regex',
        input: value
    }
}

defineExpose({
    showModal
})
</script>

<style lang="scss">
.field-format-add-modal.zqy-block-modal {
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

    .field-format-add-form {
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

        .el-form-item__content {
            position: relative;
            flex-wrap: nowrap;
        }

        .el-input__wrapper,
        .el-textarea__inner {
            border-radius: 2px;
        }

        .label-tooltip-form-item {
            .label-tooltip-text {
                display: inline-block;
                cursor: help;

                &:hover {
                    color: getCssVar('color', 'primary');
                }
            }
        }

        .field-attr-row {
            .el-form-item__content {
                justify-content: flex-start;
                gap: 18px;
            }
            .el-checkbox {
                display: inline-flex;
                flex-direction: row-reverse;
                align-items: center;
                gap: 6px;
                margin-right: 0;
            }
            .el-checkbox__label {
                padding-right: 6px;
                padding-left: 0;
                color: getCssVar('text-color', 'regular');
                font-size: 12px;
                font-weight: 400;
            }
            .el-checkbox.is-checked .el-checkbox__label {
                color: getCssVar('color', 'primary');
            }
        }
    }

    .table-rule-config {
        display: grid;
        grid-template-columns: 132px minmax(0, 1fr);
        gap: 8px;
        width: 100%;
    }
}
</style>
