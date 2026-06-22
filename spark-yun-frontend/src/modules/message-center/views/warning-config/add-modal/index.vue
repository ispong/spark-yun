<template>
    <BlockModal :model-config="modelConfig">
        <el-form
            ref="form"
            class="add-computer-group message-check-form"
            label-position="top"
            :model="formData"
            :rules="rules"
        >
            <el-form-item label="名称" prop="name">
                <el-input v-model="formData.name" maxlength="200" placeholder="请输入" />
            </el-form-item>
            <el-form-item label="类型" prop="alarmType">
                <el-select v-model="formData.alarmType" :disabled="!!formData.id" placeholder="请选择">
                    <el-option v-for="item in typeList" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
            </el-form-item>
            <el-form-item label="告警事件" prop="alarmEvent">
                <el-select v-model="formData.alarmEvent" placeholder="请选择">
                    <el-option v-for="item in eventList" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
            </el-form-item>
            <el-form-item label="消息通知" prop="msgId">
                <el-select v-model="formData.msgId" placeholder="请选择">
                    <el-option v-for="item in messageNotiList" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
            </el-form-item>
            <el-form-item label="通知人" prop="receiverList">
                <el-select
                    v-model="formData.receiverList"
                    multiple
                    collapse-tags
                    collapse-tags-tooltip
                    placeholder="请选择"
                >
                    <el-option
                        v-for="item in userList"
                        :key="item.userId"
                        :label="item.username"
                        :value="item.userId"
                    />
                </el-select>
            </el-form-item>
            <el-form-item
                class="alarm-template-item"
                :class="{ 'show-screen__full': fullStatus }"
                label="通知内容"
                prop="alarmTemplate"
            >
                <span class="format-json" @click="formatterJsonEvent(formData, 'alarmTemplate')">格式化JSON</span>
                <el-icon class="modal-full-screen" @click="fullScreenEvent()">
                    <FullScreen v-if="!fullStatus" />
                    <Close v-else />
                </el-icon>
                <code-mirror ref="responseBodyRef" v-model="formData.alarmTemplate" basic :lang="jsonLang" />
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
import { reactive, defineExpose, nextTick, ref } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
// import CodeMirror from 'vue-codemirror6'
import { json } from '@codemirror/lang-json'
import { jsonFormatter } from '@/app/utils/formatter'
import { GetUserList } from '@/app/shared/api/user'
import { GetMessagePagesList } from '../../../api'

const form = ref<FormInstance>()
const jsonLang = ref<any>(json())
const callback = ref<any>()
const fullStatus = ref(false)
const userList = ref([])
const typeList = ref([
    {
        label: '作业',
        value: 'WORK'
    },
    {
        label: '作业流',
        value: 'WORKFLOW'
    }
])
const eventList = ref([
    {
        label: '开始运行',
        value: 'START_RUN'
    },
    {
        label: '运行结束',
        value: 'RUN_END'
    },
    {
        label: '运行成功',
        value: 'RUN_SUCCESS'
    },
    {
        label: '运行失败',
        value: 'RUN_FAIL'
    }
])
const messageNotiList = ref([])
const modelConfig = reactive({
    title: '新建基线',
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
    customClass: 'warning-config-add-modal',
    closeOnClickModal: false
})
const formData = reactive({
    name: '',
    alarmType: '', // 类型
    alarmEvent: '', // 告警事件
    msgId: '', // 消息通知
    receiverList: [], // 通知人
    alarmTemplate: '', // 通知内容
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
    alarmType: [
        {
            required: true,
            message: '请选择类型',
            trigger: ['blur', 'change']
        }
    ],
    alarmEvent: [
        {
            required: true,
            message: '请选择告警事件',
            trigger: ['blur', 'change']
        }
    ],
    msgId: [
        {
            required: true,
            message: '请选择消息通知',
            trigger: ['blur', 'change']
        }
    ],
    receiverList: [
        {
            required: true,
            message: '请选择通知人',
            trigger: ['blur', 'change']
        }
    ],
    alarmTemplate: [
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
    getMessageList()
    if (data) {
        Object.keys(formData).forEach((key: string) => {
            if (key == 'receiverList') {
                formData[key] = JSON.parse(data[key])
            } else {
                formData[key] = data[key]
            }
        })
        modelConfig.title = '编辑基线'
    } else {
        formData.name = ''
        formData.alarmType = ''
        formData.alarmEvent = ''
        formData.msgId = ''
        formData.receiverList = []
        formData.alarmTemplate = ''
        formData.remark = ''
        formData.id = ''
        modelConfig.title = '新建基线'
    }

    callback.value = cb
    modelConfig.visible = true
    nextTick(() => {
        form.value?.resetFields()
    })
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

function getMessageList() {
    GetMessagePagesList({
        page: 0,
        pageSize: 10000,
        searchKeyWord: ''
    })
        .then((res: any) => {
            messageNotiList.value = res.data.content
        })
        .catch(() => {
            messageNotiList.value = []
        })
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
.warning-config-add-modal.zqy-block-modal {
    --warning-modal-x-padding: 20px;
    --warning-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--warning-modal-x-padding) 8px !important;
        margin-right: 0;
        border-bottom: none;

        &::after {
            position: absolute;
            right: 0;
            bottom: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--warning-modal-border-color);
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
        padding: 12px var(--warning-modal-x-padding);
        align-items: center;
        border-top: none;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--warning-modal-border-color);
        }
    }

    .add-computer-group {
        box-sizing: border-box;
        padding: 14px var(--warning-modal-x-padding) 4px;

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
        .alarm-template-item {
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
