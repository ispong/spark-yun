<template>
    <BlockModal :model-config="modelConfig">
        <el-upload
            class="license-upload"
            action=""
            :limit="1"
            :multiple="false"
            :drag="true"
            :auto-upload="false"
            :on-change="handleChange"
        >
            <el-icon class="el-icon--upload">
                <upload-filled />
            </el-icon>
            <div class="el-upload__text">
                {{ t('license.uploadEnterpriseLicense') }}
                <em>{{ t('license.clickUpload') }}</em>
            </div>
        </el-upload>
    </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, defineExpose, ref, watch } from 'vue'
import BlockModal from '@/app/components/block-modal/index.vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'

const { t, locale } = useI18n()
const callback = ref<any>()
const fileData = ref(null)
const modelConfig = reactive({
    title: t('license.uploadCertificate'),
    visible: false,
    width: '520px',
    okConfig: {
        title: t('common.confirm'),
        ok: okEvent,
        disabled: false,
        loading: false
    },
    cancelConfig: {
        title: t('common.cancel'),
        cancel: closeEvent,
        disabled: false
    },
    needScale: false,
    zIndex: 1100,
    closeOnClickModal: false
})

function syncModalLocale() {
    modelConfig.title = t('license.uploadCertificate')
    modelConfig.okConfig.title = t('common.confirm')
    modelConfig.cancelConfig.title = t('common.cancel')
}

watch(locale, syncModalLocale)

function showModal(cb: () => void): void {
    callback.value = cb
    syncModalLocale()
    modelConfig.visible = true
}

function okEvent() {
    modelConfig.okConfig.loading = true
    callback
        .value(fileData.value)
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
            ElMessage.error(err)
        })
}

function closeEvent() {
    modelConfig.visible = false
}

function handleChange(e: any) {
    fileData.value = e.raw
}

defineExpose({
    showModal
})
</script>

<style lang="scss">
.license-upload {
    margin: 20px;
    .el-upload {
        .el-upload-dragger {
            border-radius: getCssVar('border-radius', 'small');
            .el-upload__text {
                font-size: getCssVar('font-size', 'extra-small');
            }
        }
    }
}
</style>
