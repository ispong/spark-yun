<template>
    <BlockModal :model-config="modelConfig">
        <el-form
            ref="form"
            class="add-computer-group message-check-form"
            label-position="top"
            :model="formData"
            :rules="rules"
        >
            <el-form-item label="名称">
                <el-input v-model="formData.name" :disabled="true" maxlength="200" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="类型">
                <el-select v-model="formData.msgType" :disabled="true" placeholder="请选择">
                    <el-option v-for="item in typeList" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
            </el-form-item>
            <el-form-item label="通知内容模板">
                <el-input v-model="formData.contentTemplate" :disabled="true" maxlength="200" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="通知对象" prop="receiver">
                <el-select v-model="formData.receiver" placeholder="请选择">
                    <el-option
                        v-for="item in userList"
                        :key="item.userId"
                        :label="item.username"
                        :value="item.userId"
                    />
                </el-select>
            </el-form-item>
            <el-form-item
                class="message-test-content-item"
                :class="{ 'show-screen__full': fullStatus }"
                label="通知内容"
                prop="content"
            >
                <span
                    v-if="formData.msgType === 'ALI_SMS'"
                    class="format-json"
                    @click="formatterJsonEvent(formData, 'content')"
                >
                    格式化JSON
                </span>
                <el-icon class="modal-full-screen" @click="fullScreenEvent()">
                    <FullScreen v-if="!fullStatus" />
                    <Close v-else />
                </el-icon>
                <code-mirror ref="responseBodyRef" v-model="formData.content" basic :lang="jsonLang" />
            </el-form-item>
        </el-form>
    </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, defineExpose, nextTick, ref } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { GetUserList } from '@/app/shared/api/user'
// import CodeMirror from 'vue-codemirror6'
import { json } from '@codemirror/lang-json'
import { jsonFormatter } from '@/app/utils/formatter'

const form = ref<FormInstance>()
const jsonLang = ref<any>(json())
const callback = ref<any>()
const fullStatus = ref(false)
const typeList = ref([
    {
        label: '阿里短信',
        value: 'ALI_SMS'
    },
    {
        label: '邮箱',
        value: 'EMAIL'
    }
])
const userList = ref([])
const modelConfig = reactive({
    title: '测试',
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
    customClass: 'message-notification-check-modal',
    closeOnClickModal: false
})
const formData = reactive({
    name: '',
    msgType: '',
    receiver: '',
    content: '',
    contentTemplate: '',
    id: ''
})
const rules = reactive<FormRules>({
    receiver: [
        {
            required: true,
            message: '请选择通知对象',
            trigger: ['blur', 'change']
        }
    ],
    content: [
        {
            required: true,
            message: '请输入通知内容',
            trigger: ['blur', 'change']
        }
    ]
})

function showModal(cb: () => void, data: any): void {
    fullStatus.value = false
    getUserList()
    formData.name = data.name
    formData.msgType = data.msgType
    if (data.messageConfig) {
        formData.contentTemplate = data.messageConfig.contentTemplate
    }
    formData.id = data.id
    formData.content = ''
    formData.receiver = ''
    modelConfig.title = '测试'

    callback.value = cb
    modelConfig.visible = true
    nextTick(() => {
        form.value?.resetFields()
    })
}

function getUserList() {
    GetUserList({
        page: 0,
        pageSize: 10000,
        searchKeyWord: '',
        tenantId: ''
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
            const formDataParams = {
                receiver: formData.receiver,
                content: formData.content,
                id: formData.id
            }
            callback
                .value(formDataParams)
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
function fullScreenEvent() {
    fullStatus.value = !fullStatus.value
}
function formatterJsonEvent(formData: any, key: string) {
    try {
        formData[key] = jsonFormatter(formData[key])
    } catch (error) {
        console.error('请检查输入的JSON格式是否正确', error)
        ElMessage.error('请检查输入的JSON格式是否正确')
    }
}

function closeEvent() {
    fullStatus.value = false
    modelConfig.visible = false
}

defineExpose({
    showModal
})
</script>

<style lang="scss">
.message-notification-check-modal.zqy-block-modal {
    --message-check-modal-x-padding: 20px;
    --message-check-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--message-check-modal-x-padding) 8px !important;
        margin-right: 0;
        border-bottom: none;

        &::after {
            position: absolute;
            right: 0;
            bottom: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--message-check-modal-border-color);
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
        padding: 12px var(--message-check-modal-x-padding);
        align-items: center;
        border-top: none;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--message-check-modal-border-color);
        }
    }

    .add-computer-group {
        box-sizing: border-box;
        padding: 14px var(--message-check-modal-x-padding) 4px;

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

    .message-check-form {
        .message-test-content-item {
            .el-form-item__content {
                position: relative;
                display: block;
            }

            .format-json {
                position: absolute;
                top: -22px;
                right: 24px;
                font-size: 12px;
                line-height: 16px;
                color: getCssVar('color', 'primary');
                cursor: pointer;

                &:hover {
                    text-decoration: underline;
                }
            }

            .modal-full-screen {
                position: absolute;
                top: -22px;
                right: 0;
                width: 16px;
                height: 16px;
                cursor: pointer;

                &:hover {
                    color: getCssVar('color', 'primary');
                }
            }

            .vue-codemirror {
                width: 100%;
                height: 132px;

                .cm-editor {
                    height: 100%;
                    outline: none;
                    border: 1px solid #dcdfe6;
                    border-radius: 2px;
                }

                .cm-gutters,
                .cm-content {
                    font-size: 12px;
                    font-family:
                        v-sans,
                        system-ui,
                        -apple-system,
                        BlinkMacSystemFont,
                        'Segoe UI',
                        sans-serif,
                        'Apple Color Emoji',
                        'Segoe UI Emoji',
                        'Segoe UI Symbol';
                }

                .cm-tooltip-autocomplete {
                    ul {
                        li {
                            display: flex;
                            align-items: center;
                            height: 40px;
                            font-size: 12px;
                            background-color: #ffffff;
                            font-family:
                                v-sans,
                                system-ui,
                                -apple-system,
                                BlinkMacSystemFont,
                                'Segoe UI',
                                sans-serif,
                                'Apple Color Emoji',
                                'Segoe UI Emoji',
                                'Segoe UI Symbol';
                        }

                        li[aria-selected] {
                            background: #409eff;
                        }

                        .cm-completionIcon {
                            margin-right: -4px;
                            opacity: 0;
                        }
                    }
                }
            }

            &.show-screen__full {
                position: fixed;
                z-index: 3202;
                inset: 0;
                padding: 16px 20px;
                margin-bottom: 0;
                box-sizing: border-box;
                background-color: #ffffff;
                transition: all 0.15s linear;

                .el-form-item__content {
                    height: calc(100% - 20px);
                }

                .vue-codemirror {
                    height: 100%;
                }
            }
        }
    }
}
</style>
