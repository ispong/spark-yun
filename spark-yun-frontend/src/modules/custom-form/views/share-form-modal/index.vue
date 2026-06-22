<template>
    <BlockModal :model-config="modelConfig">
        <div class="share-form-content zqy-block-modal-form">
            <div class="share-form-setting">
                <div class="share-form-valid-day">
                    <span class="share-form-label">有效期（天）</span>
                    <el-input-number v-model="validDay" :min="1" controls-position="right" />
                </div>
                <el-button :loading="loading" type="primary" @click="getShareFormUrl">生成分享链接</el-button>
            </div>

            <div class="share-form-link-box">
                <span class="share-form-link-label">链接</span>
                <span class="share-form-link-value" :class="{ 'is-empty': !url }">
                    <a v-if="url" class="share-form-link" :href="url" target="_blank" rel="noopener noreferrer">
                        <EllipsisTooltip class="url-show" :label="url" />
                    </a>
                    <EllipsisTooltip v-else class="url-show" label="生成后显示分享链接" />
                </span>
                <el-button
                    v-if="url"
                    id="share-form-url"
                    class="share-form-copy"
                    link
                    type="primary"
                    :data-clipboard-text="url"
                    @click="copyUrlEvent('share-form-url')"
                >
                    复制
                </el-button>
            </div>
        </div>
    </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, defineExpose, ref } from 'vue'
import EllipsisTooltip from '@/app/components/ellipsis-tooltip/ellipsis-tooltip.vue'
import Clipboard from 'clipboard'
import { ElMessage } from 'element-plus'
import { GetFormLinkConfig } from '../../api'

const url = ref('')
const cardInfo = ref()
const loading = ref(false)
const validDay = ref(1)

const modelConfig = reactive({
    title: '分享表单',
    visible: false,
    width: '520px',
    cancelConfig: {
        title: '关闭',
        cancel: closeEvent,
        disabled: false
    },
    needScale: false,
    customClass: 'share-form-setting__modal',
    zIndex: 1100,
    closeOnClickModal: false
})

function showModal(card: any): void {
    cardInfo.value = card
    url.value = ''
    validDay.value = 1
    modelConfig.visible = true
}

function getShareFormUrl() {
    loading.value = true
    GetFormLinkConfig({
        formId: cardInfo.value.id,
        validDay: validDay.value
    })
        .then((res: any) => {
            loading.value = false
            url.value = `${location.origin}/share/${res.data.formLinkId}`
        })
        .catch(() => {
            loading.value = false
        })
}

function copyUrlEvent(id: string) {
    const clipboard = new Clipboard('#' + id)
    clipboard.on('success', () => {
        ElMessage.success('复制成功')
        clipboard.destroy()
    })
}

function closeEvent() {
    modelConfig.visible = false
    url.value = ''
    validDay.value = 1
}

defineExpose({
    showModal
})
</script>

<style lang="scss">
.share-form-setting__modal.zqy-block-modal {
    .share-form-content {
        padding-bottom: 18px;
    }

    .share-form-setting {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 18px;

        .share-form-valid-day {
            display: flex;
            align-items: center;
            gap: 10px;

            .share-form-label {
                font-size: 12px;
                color: getCssVar('text-color', 'primary');
            }

            .el-input-number {
                width: 128px;

                .el-input-number__decrease,
                .el-input-number__increase {
                    border: 0;
                    background-color: transparent;
                }

                .el-input__wrapper {
                    border-radius: 2px;
                }
            }
        }
    }

    .share-form-link-box {
        display: flex;
        align-items: center;
        min-height: 40px;
        padding: 0 12px;
        box-sizing: border-box;
        border: 1px solid getCssVar('border-color');
        border-radius: 2px;
        background-color: getCssVar('fill-color', 'lighter');

        .share-form-link-label {
            flex: 0 0 auto;
            margin-right: 10px;
            font-size: 12px;
            color: getCssVar('text-color', 'regular');
        }

        .share-form-link-value {
            min-width: 0;
            flex: 1;
            font-size: 12px;
            color: getCssVar('color', 'primary');

            &.is-empty {
                color: getCssVar('text-color', 'placeholder');
            }

            .share-form-link {
                color: inherit;
                text-decoration: none;

                &:hover {
                    text-decoration: underline;
                }
            }

            .url-show {
                max-width: 100%;
            }
        }

        .share-form-copy {
            flex: 0 0 auto;
            margin-left: 12px;
            font-size: getCssVar('font-size', 'extra-small');
        }
    }
}
</style>
