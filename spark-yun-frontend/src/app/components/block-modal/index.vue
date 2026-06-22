<template>
    <el-config-provider :z-index="modalPopperZIndex">
        <el-dialog
            v-model="visible"
            :title="modelConfig.title"
            :width="modelConfig.width"
            :z-index="modalZIndex"
            :class="[modelConfig.customClass, 'zqy-block-modal']"
            :close-on-press-escape="false"
            :close-on-click-modal="false"
            :append-to-body="true"
            :destroy-on-close="true"
            :center="true"
            @close="close"
        >
            <div class="modal-content">
                <slot />
            </div>
            <template v-if="!modelConfig.footerHidden" #footer>
                <slot name="customLeft" />
                <el-button
                    v-if="modelConfig.cancelConfig && !modelConfig.cancelConfig.hide"
                    :disabled="modelConfig.cancelConfig.disabled || false"
                    @click="clickToCancel"
                >
                    {{ modelConfig.cancelConfig.title }}
                </el-button>
                <el-button
                    v-if="modelConfig.okConfig"
                    type="primary"
                    :loading="modelConfig.okConfig.loading"
                    :disabled="modelConfig.okConfig.disabled || false"
                    @click="clickToSave"
                >
                    {{ modelConfig.okConfig.title }}
                </el-button>
            </template>
        </el-dialog>
    </el-config-provider>
</template>

<script lang="ts" setup>
import { computed, defineProps, ref, watch } from 'vue'

interface BtnConfig {
    title: string
    disabled: boolean
    loading: boolean
    hide?: boolean
}

interface OkBtnConfig extends BtnConfig {
    ok: () => void
}

interface CancelBtnConfig extends BtnConfig {
    cancel: () => void
}

interface ModalConfig {
    title: string
    visible: boolean
    width: string | number
    okConfig: OkBtnConfig
    cancelConfig: CancelBtnConfig
    zIndex?: number
    customClass?: string
    footerHidden?: boolean
}

const visible = ref(false)

const props = defineProps<{
    modelConfig: ModalConfig
}>()

const MIN_MODAL_Z_INDEX = 3000

const modalZIndex = computed(() => Math.max(props.modelConfig.zIndex || MIN_MODAL_Z_INDEX, MIN_MODAL_Z_INDEX))
const modalPopperZIndex = computed(() => modalZIndex.value + 1)

watch(
    () => props.modelConfig.visible,
    (newVal) => {
        visible.value = newVal
    }
)

function clickToSave() {
    props.modelConfig.okConfig.ok()
}

function clickToCancel() {
    props.modelConfig.cancelConfig.cancel()
}

function close() {
    props.modelConfig?.cancelConfig?.cancel()
}
</script>

<style lang="scss">
.zqy-block-modal {
    --zqy-modal-x-padding: 20px;
    --zqy-modal-border-color: #ebeef5;

    overflow: unset !important;
    border-radius: 2px;
    .el-dialog__header {
        position: relative;
        min-height: 46px;
        margin-right: 0;
        padding: 9px var(--zqy-modal-x-padding) 8px !important;
        width: 100%;
        box-sizing: border-box;
        cursor: default;
        border-bottom: none;
        .el-dialog__headerbtn {
            width: 42px;
            height: 46px;
            top: 0;
        }
        .el-dialog__title {
            display: block;
            line-height: 28px;
            font-size: 16px;
            color: getCssVar('text-color', 'primary');
        }
        &::after {
            position: absolute;
            right: 0;
            bottom: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--zqy-modal-border-color);
        }
    }
    .el-dialog__body {
        overflow: auto;
        // max-height: calc(100vh - 96px);
        max-height: calc(74vh - 102px);
        padding: 0 !important;
    }
    .custom-header-btn {
        position: absolute;
        top: 13px;
        right: 44px;
        .scale-all-screen {
            cursor: pointer;
            &:hover {
                color: #005bac;
            }
        }
        .scale-exist-screen {
            height: 24px;
            width: 24px;
            cursor: pointer;
            position: absolute;
            right: -6px;
            top: -3px;
            &:hover {
                color: #005bac;
            }
        }
    }
    .el-dialog__footer {
        position: relative;
        min-height: 56px;
        border-top: none;
        box-shadow: unset !important;
        border-radius: 0 0 4px 4px;
        padding: 12px var(--zqy-modal-x-padding);
        display: flex;
        align-items: center;
        justify-content: flex-end;
        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--zqy-modal-border-color);
        }
        > span {
            display: flex;
            align-items: center;
        }
    }
    .zqy-block-modal-form {
        padding: 14px var(--zqy-modal-x-padding) 4px;
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
        .el-input-number,
        .el-date-editor {
            width: 100%;
        }
        .el-input__wrapper,
        .el-textarea__inner {
            border-radius: 2px;
        }
        .el-textarea__inner {
            min-height: 88px !important;
        }
    }
}
</style>
