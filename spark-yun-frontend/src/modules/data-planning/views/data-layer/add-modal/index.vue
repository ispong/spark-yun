<template>
    <BlockModal :model-config="modelConfig">
        <el-form
            ref="form"
            class="data-layer-add-form"
            label-position="top"
            :model="formData"
            :rules="rules"
        >
            <el-form-item label="分层名称" prop="name">
                <el-input v-model="formData.name" maxlength="200" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="父级分层" prop="parentLayerId">
                <el-select v-model="formData.parentLayerId" filterable clearable placeholder="请选择">
                    <el-option
                        v-for="item in parentLayerIdList"
                        :key="item.value"
                        :label="item.label"
                        :value="item.value"
                    />
                </el-select>
            </el-form-item>
            <el-form-item prop="tableRuleInput" class="label-tooltip-form-item">
                <template #label>
                    <el-tooltip
                        content="支持前缀/后缀/包含/精确匹配，系统会自动转为正则保存"
                        placement="top"
                        :enterable="false"
                    >
                        <span class="label-tooltip-text">表名规范</span>
                    </el-tooltip>
                </template>
                <div class="table-rule-config">
                    <el-select v-model="tableRuleMode">
                        <el-option
                            v-for="item in tableRuleModeList"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                    <el-input v-model="formData.tableRuleInput" maxlength="200" :placeholder="tableRulePlaceholder" />
                </div>
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
import { reactive, defineExpose, ref, computed } from 'vue'
import { GetDataLayerList } from '../../../api/data-layer'
import { ElMessage, FormInstance, FormRules } from 'element-plus'

interface Option {
    label: string
    value: string
}

type TableRuleMode = 'prefix' | 'suffix' | 'contains' | 'exact' | 'regex'

const form = ref<FormInstance>()
const callback = ref<any>()
const parentLayerIdList = ref<Option[]>([])
const currentLayerId = ref<string>('')
const tableRuleMode = ref<TableRuleMode>('prefix')
const tableRuleModeList: Option[] = [
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

const modelConfig = reactive({
    title: '新建分层',
    visible: false,
    width: '520px',
    customClass: 'data-layer-add-modal',
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
    parentLayerId: '',
    tableRuleInput: '',
    tableRule: '',
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
    // parentLayerId: [{ required: true, message: '请选择父级分层', trigger: ['blur', 'change'] }],
    tableRuleInput: [
        {
            validator: (_rule, value, callback) => {
                if (value?.trim() && tableRuleMode.value === 'regex') {
                    try {
                        // 仅校验正则合法性，不在这里实际使用

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
    ]
})

const tableRulePlaceholder = computed(() => {
    if (tableRuleMode.value === 'regex') {
        return '请输入正则表达式，例如：^user_.*'
    }
    return '请输入关键字，例如：user_'
})

function showModal(cb: () => void, data: any, parentLayerId: string): void {
    currentLayerId.value = data?.id ?? ''
    getParentLayerIList(currentLayerId.value)
    if (data) {
        Object.keys(formData).forEach((key: string) => {
            formData[key] = data[key]
        })
        const tableRuleInfo = parseTableRule(formData.tableRule)
        tableRuleMode.value = tableRuleInfo.mode
        formData.tableRuleInput = tableRuleInfo.input
        modelConfig.title = '编辑分层'
    } else {
        Object.keys(formData).forEach((key: string) => {
            formData[key] = ''
            if (parentLayerId && key === 'parentLayerId') {
                formData[key] = parentLayerId
            }
        })
        tableRuleMode.value = 'prefix'
        modelConfig.title = '新建分层'
    }

    callback.value = cb
    modelConfig.visible = true
}

function okEvent() {
    form.value?.validate((valid: boolean) => {
        if (valid) {
            modelConfig.okConfig.loading = true
            const submitData = {
                id: formData.id,
                name: formData.name,
                parentLayerId: formData.parentLayerId,
                tableRule: buildTableRule(tableRuleMode.value, formData.tableRuleInput),
                remark: formData.remark
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

function getParentLayerIList(excludeLayerId?: string) {
    GetDataLayerList({
        page: 0,
        pageSize: 10000,
        searchKeyWord: ''
    })
        .then((res: any) => {
            parentLayerIdList.value = [
                ...res.data.content
                    .filter((item: any) => item.id !== excludeLayerId)
                    .map((item: any) => {
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

function escapeRegexChar(value: string) {
    return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function unEscapeRegexChar(value: string) {
    return value.replace(/\\([.*+?^${}()|[\]\\])/g, '$1')
}

function buildTableRule(mode: TableRuleMode, inputValue: string) {
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

function parseTableRule(value: string): { mode: TableRuleMode; input: string } {
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
.data-layer-add-modal.zqy-block-modal {
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

    .data-layer-add-form {
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
    }

    .table-rule-config {
        display: grid;
        grid-template-columns: 132px minmax(0, 1fr);
        gap: 8px;
        width: 100%;
    }
}
</style>
