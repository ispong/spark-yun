<template>
    <BlockModal :model-config="modelConfig">
        <div class="share-report-form zqy-block-modal-form">
            <div class="share-report-setting">
                <div class="share-report-valid-day">
                    <span class="share-report-label">有效期（天）</span>
                    <el-input-number v-model="validDay" :min="1" controls-position="right" />
                </div>
                <el-button :loading="loading" type="primary" @click="getShareFormUrl">生成分享链接</el-button>
            </div>

            <div class="share-report-link-box">
                <span class="share-report-link-label">链接</span>
                <span class="share-report-link-value" :class="{ 'is-empty': !url }">
                    <EllipsisTooltip class="url-show" :label="url || '生成后显示分享链接'" />
                </span>
                <el-button
                    v-if="url"
                    id="share-report-url"
                    class="share-report-copy"
                    link
                    type="primary"
                    :data-clipboard-text="url"
                    @click="copyUrlEvent('share-report-url')"
                >
                    复制
                </el-button>
            </div>
        </div>
    </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, defineExpose, ref, nextTick } from 'vue'
import EllipsisTooltip from '@/app/components/ellipsis-tooltip/ellipsis-tooltip.vue'
import Clipboard from 'clipboard'
import { ElMessage } from 'element-plus'
import { GetChartsLinkConfig } from '../../../api'

const url = ref('')
const token = ref('')
const cardInfo = ref()
const loading = ref(false)
const validDay = ref(1)

const modelConfig = reactive({
    title: '分享大屏',
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
    GetChartsLinkConfig({
        viewId: cardInfo.value.id,
        validDay: validDay.value
    })
        .then((res: any) => {
            loading.value = false
            url.value = `${location.origin}/dashboard/${res.data.viewLinkId}`
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
    .share-report-form {
        padding-bottom: 18px;
    }

    .share-report-setting {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 18px;

        .share-report-valid-day {
            display: flex;
            align-items: center;
            gap: 10px;

            .share-report-label {
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

    .share-report-link-box {
        display: flex;
        align-items: center;
        min-height: 40px;
        padding: 0 12px;
        box-sizing: border-box;
        border: 1px solid getCssVar('border-color');
        border-radius: 2px;
        background-color: getCssVar('fill-color', 'lighter');

        .share-report-link-label {
            flex: 0 0 auto;
            margin-right: 10px;
            font-size: 12px;
            color: getCssVar('text-color', 'regular');
        }

        .share-report-link-value {
            min-width: 0;
            flex: 1;
            font-size: 12px;
            color: getCssVar('color', 'primary');

            &.is-empty {
                color: getCssVar('text-color', 'placeholder');
            }

            .url-show {
                max-width: 100%;
            }
        }

        .share-report-copy {
            flex: 0 0 auto;
            margin-left: 12px;
            font-size: getCssVar('font-size', 'extra-small');
        }
    }
}
</style>
