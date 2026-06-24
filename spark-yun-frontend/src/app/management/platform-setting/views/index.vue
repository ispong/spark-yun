<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-platform-setting platform-setting-page">
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData">
            <div class="zqy-platform-setting__wrap">
                <el-form class="zqy-platform-setting__form" label-position="top">
                    <div class="zqy-platform-setting__section zqy-platform-setting__section--basic">
                        <div class="zqy-platform-setting__section-title">{{ t('platformSetting.basicSetting') }}</div>
                        <div class="zqy-platform-setting__switch-row zqy-platform-setting__switch-row--plain">
                            <span class="zqy-platform-setting__label">{{ t('platformSetting.autoCreateTenant') }}</span>
                            <el-switch
                                v-model="form.autoCreateTenant"
                                :loading="autoCreateTenantSaving"
                                @change="saveAutoCreateTenant"
                            />
                        </div>
                        <div class="zqy-platform-setting__auto-tenant-grid">
                            <el-form-item :label="t('platformSetting.defaultMemberNum')">
                                <div class="zqy-platform-setting__number-row">
                                    <el-input-number
                                        v-model="form.defaultTenantMemberNum"
                                        :min="1"
                                        :max="1000000"
                                        :step="1"
                                        step-strictly
                                        controls-position="right"
                                    />
                                    <span>{{ t('platformSetting.unitCount') }}</span>
                                </div>
                            </el-form-item>
                            <el-form-item :label="t('platformSetting.defaultWorkflowNum')">
                                <div class="zqy-platform-setting__number-row">
                                    <el-input-number
                                        v-model="form.defaultTenantWorkflowNum"
                                        :min="1"
                                        :max="1000000"
                                        :step="1"
                                        step-strictly
                                        controls-position="right"
                                    />
                                    <span>{{ t('platformSetting.unitCount') }}</span>
                                </div>
                            </el-form-item>
                            <el-form-item :label="t('platformSetting.defaultValidDays')">
                                <div class="zqy-platform-setting__number-row">
                                    <el-input-number
                                        v-model="form.defaultTenantValidDays"
                                        :min="1"
                                        :max="3650"
                                        :step="1"
                                        step-strictly
                                        controls-position="right"
                                    />
                                    <span>{{ t('platformSetting.unitDays') }}</span>
                                </div>
                            </el-form-item>
                        </div>
                        <div class="zqy-platform-setting__switch-row zqy-platform-setting__switch-row--plain">
                            <span class="zqy-platform-setting__label">{{ t('platformSetting.enableUserLog') }}</span>
                            <el-switch v-model="form.userLogEnabled" :loading="saving" @change="saveSetting" />
                        </div>
                        <el-form-item
                            class="zqy-platform-setting__retention-item"
                            :label="t('platformSetting.userLogRetentionDays')"
                        >
                            <div class="zqy-platform-setting__retention-row">
                                <el-input-number
                                    v-model="form.userLogRetentionDays"
                                    :min="1"
                                    :max="3650"
                                    :step="1"
                                    step-strictly
                                    controls-position="right"
                                />
                                <span>{{ t('platformSetting.unitDays') }}</span>
                            </div>
                        </el-form-item>
                        <el-form-item :label="t('platformSetting.description')">
                            <el-input
                                v-model="form.description"
                                type="textarea"
                                :rows="8"
                                :maxlength="2000"
                                show-word-limit
                            />
                        </el-form-item>
                        <div class="zqy-platform-setting__actions">
                            <el-button type="primary" :loading="saving" @click="saveSetting">
                                {{ t('common.save') }}
                            </el-button>
                        </div>
                    </div>
                    <div class="zqy-platform-setting__section zqy-platform-setting__section--brand">
                        <div class="zqy-platform-setting__section-title">{{ t('platformSetting.brandSetting') }}</div>
                        <el-form-item :label="t('platformSetting.browserTitle')">
                            <div class="zqy-platform-setting__title-row">
                                <el-input
                                    v-model="form.browserTitle"
                                    clearable
                                    :maxlength="100"
                                    :placeholder="t('platformSetting.inputBrowserTitle')"
                                />
                                <el-button @click="form.browserTitle = defaultBrandSetting.browserTitle">
                                    {{ t('platformSetting.resetDefault') }}
                                </el-button>
                            </div>
                        </el-form-item>
                        <el-form-item :label="t('platformSetting.themeColor')">
                            <div class="zqy-platform-setting__color-row">
                                <el-color-picker
                                    v-model="form.themeColor"
                                    :clearable="false"
                                    :predefine="themePredefineColors"
                                />
                                <el-button @click="resetThemeColor">{{ t('platformSetting.resetDefault') }}</el-button>
                            </div>
                        </el-form-item>
                        <div class="zqy-platform-setting__brand-grid">
                            <div
                                v-for="item in brandImageItems"
                                :key="item.key"
                                class="zqy-platform-setting__brand-item"
                            >
                                <div class="zqy-platform-setting__brand-preview">
                                    <img :src="previewBrandImage(item.key)" :alt="item.label" />
                                </div>
                                <div class="zqy-platform-setting__brand-info">
                                    <div class="zqy-platform-setting__brand-name">{{ item.label }}</div>
                                    <div class="zqy-platform-setting__brand-desc">{{ item.description }}</div>
                                    <div class="zqy-platform-setting__brand-actions">
                                        <el-upload
                                            :show-file-list="false"
                                            :auto-upload="false"
                                            :accept="item.accept"
                                            :disabled="brandUploadLoading[item.key]"
                                            :on-change="(file) => handleBrandFile(file.raw, item.key)"
                                        >
                                            <el-button
                                                :loading="brandUploadLoading[item.key]"
                                                :disabled="brandUploadLoading[item.key]"
                                            >
                                                {{ t('platformSetting.upload') }}
                                            </el-button>
                                        </el-upload>
                                        <el-button @click="resetBrandImage(item.key)">
                                            {{ t('platformSetting.resetDefault') }}
                                        </el-button>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="zqy-platform-setting__brand-save">
                            <el-button type="primary" :loading="saving" @click="saveSetting">
                                {{ t('common.save') }}
                            </el-button>
                        </div>
                    </div>
                </el-form>
            </div>
        </LoadingPage>
    </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type UploadRawFile } from 'element-plus'
import { useI18n } from 'vue-i18n'

import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import { GetPlatformSetting, UpdatePlatformSetting, UploadBrandImage, type PlatformSetting } from '../api'
import { createBreadCrumbList } from './platform-setting.config'
import { applyBrandSetting, defaultBrandSetting } from '@/app/shared/branding'

type BrandImageKey =
    | 'faviconUrl'
    | 'topLogoUrl'
    | 'topLogoSmallUrl'
    | 'loginMainImageUrl'

interface BrandImageItem {
    key: BrandImageKey
    label: string
    description: string
    accept: string
}

const IMAGE_ACCEPT = 'image/png,image/jpeg,image/webp,image/svg+xml'
const ICON_ACCEPT = `${IMAGE_ACCEPT},image/x-icon,.ico`
const MAX_BRAND_FILE_SIZE = 5 * 1024 * 1024

const { t, locale } = useI18n()
const breadCrumbList = reactive(createBreadCrumbList(t))
const loading = ref(false)
const saving = ref(false)
const autoCreateTenantSaving = ref(false)
const networkError = ref(false)
const brandUploadLoading = reactive<Record<BrandImageKey, boolean>>({
    faviconUrl: false,
    topLogoUrl: false,
    topLogoSmallUrl: false,
    loginMainImageUrl: false
})

const form = reactive<PlatformSetting>({
    description: '',
    autoCreateTenant: false,
    defaultTenantMemberNum: 5,
    defaultTenantWorkflowNum: 10,
    defaultTenantValidDays: 7,
    browserTitle: '',
    themeColor: defaultBrandSetting.themeColor,
    faviconUrl: '',
    topLogoUrl: '',
    topLogoSmallUrl: '',
    loginMainImageUrl: '',
    userLogEnabled: false,
    userLogRetentionDays: 180
})

const themePredefineColors = ['#f34c00', '#409eff', '#1677ff', '#21ba45', '#722ed1', '#db2828']

const brandImageItems = computed<BrandImageItem[]>(() => [
    {
        key: 'faviconUrl',
        label: t('platformSetting.favicon'),
        description: t('platformSetting.faviconDesc'),
        accept: ICON_ACCEPT
    },
    {
        key: 'topLogoUrl',
        label: t('platformSetting.topLogo'),
        description: t('platformSetting.topLogoDesc'),
        accept: IMAGE_ACCEPT
    },
    {
        key: 'topLogoSmallUrl',
        label: t('platformSetting.topLogoSmall'),
        description: t('platformSetting.topLogoSmallDesc'),
        accept: IMAGE_ACCEPT
    },
    {
        key: 'loginMainImageUrl',
        label: t('platformSetting.loginMainImage'),
        description: t('platformSetting.loginMainImageDesc'),
        accept: IMAGE_ACCEPT
    }
])

watch(locale, () => {
    breadCrumbList.splice(0, breadCrumbList.length, ...createBreadCrumbList(t))
})

function initData() {
    loading.value = true
    networkError.value = false
    GetPlatformSetting()
        .then((res: any) => {
            form.description = res.data?.description || ''
            form.autoCreateTenant = res.data?.autoCreateTenant ?? false
            form.defaultTenantMemberNum = res.data?.defaultTenantMemberNum || 5
            form.defaultTenantWorkflowNum = res.data?.defaultTenantWorkflowNum || 10
            form.defaultTenantValidDays = res.data?.defaultTenantValidDays || 7
            form.browserTitle = res.data?.browserTitle || defaultBrandSetting.browserTitle
            form.themeColor = res.data?.themeColor || defaultBrandSetting.themeColor
            form.faviconUrl = res.data?.faviconUrl || ''
            form.topLogoUrl = res.data?.topLogoUrl || ''
            form.topLogoSmallUrl = res.data?.topLogoSmallUrl || ''
            form.loginMainImageUrl = res.data?.loginMainImageUrl || ''
            form.userLogEnabled = res.data?.userLogEnabled ?? false
            form.userLogRetentionDays = res.data?.userLogRetentionDays || 180
            loading.value = false
        })
        .catch(() => {
            loading.value = false
            networkError.value = true
        })
}

function saveSetting() {
    saving.value = true
    UpdatePlatformSetting(createSubmitParams())
        .then((res: any) => {
            ElMessage.success(res.msg)
            applyBrandSetting(createSubmitParams())
        })
        .finally(() => {
            saving.value = false
        })
}

function saveAutoCreateTenant() {
    autoCreateTenantSaving.value = true
    UpdatePlatformSetting(createSubmitParams())
        .then((res: any) => {
            ElMessage.success(res.msg)
            applyBrandSetting(createSubmitParams())
        })
        .catch(() => {
            initData()
        })
        .finally(() => {
            autoCreateTenantSaving.value = false
        })
}

function createSubmitParams(): PlatformSetting {
    return {
        description: form.description,
        autoCreateTenant: form.autoCreateTenant,
        defaultTenantMemberNum: form.defaultTenantMemberNum || 5,
        defaultTenantWorkflowNum: form.defaultTenantWorkflowNum || 10,
        defaultTenantValidDays: form.defaultTenantValidDays || 7,
        browserTitle: form.browserTitle?.trim() || '',
        themeColor: normalizeThemeColor(form.themeColor),
        faviconUrl: form.faviconUrl || '',
        topLogoUrl: form.topLogoUrl || '',
        topLogoSmallUrl: form.topLogoSmallUrl || '',
        loginMainImageUrl: form.loginMainImageUrl || '',
        userLogEnabled: form.userLogEnabled ?? false,
        userLogRetentionDays: form.userLogRetentionDays || 180
    }
}

function previewBrandImage(key: BrandImageKey): string {
    return form[key] || defaultBrandSetting[key]
}

function resetBrandImage(key: BrandImageKey) {
    form[key] = ''
}

function resetThemeColor() {
    form.themeColor = defaultBrandSetting.themeColor
}

function normalizeThemeColor(color: string | undefined): string {
    const value = color?.trim() || ''
    return /^#[0-9a-fA-F]{6}$/.test(value) ? value : defaultBrandSetting.themeColor
}

function handleBrandFile(file: UploadRawFile | undefined, key: BrandImageKey) {
    if (!file) {
        return
    }
    if (!file.type.startsWith('image/') && !file.name.toLowerCase().endsWith('.ico')) {
        ElMessage.warning(t('platformSetting.uploadImageOnly'))
        return
    }
    if (file.size > MAX_BRAND_FILE_SIZE) {
        ElMessage.warning(t('platformSetting.imageMaxSize'))
        return
    }

    brandUploadLoading[key] = true
    const formData = new FormData()
    formData.append('file', file)
    UploadBrandImage(formData)
        .then((res: any) => {
            const imageUrl = getUploadImageUrl(res)
            if (!imageUrl) {
                ElMessage.error(t('platformSetting.uploadNoUrl'))
                return
            }
            form[key] = imageUrl
        })
        .finally(() => {
            brandUploadLoading[key] = false
        })
}

function getUploadImageUrl(res: any): string {
    if (!res) {
        return ''
    }
    if (typeof res === 'string') {
        return res
    }
    if (typeof res !== 'object') {
        return ''
    }
    if (typeof res.url === 'string') {
        return res.url
    }
    if (typeof res.data === 'string') {
        return res.data
    }
    return getUploadImageUrl(res.data)
}

onMounted(() => {
    initData()
})

</script>

<style lang="scss">
.zqy-platform-setting.platform-setting-page {
    height: calc(100vh - 56px);
    padding: 24px 20px;
    box-sizing: border-box;
    overflow: auto;
    background-color: #fff;

    .zqy-loading {
        min-height: calc(100vh - 104px);
    }

    .zqy-platform-setting__wrap {
        width: 100%;
        max-width: 1040px;
    }

    .zqy-platform-setting__form {
        width: 100%;
        background-color: #fff;
    }

    .zqy-platform-setting__section {
        padding: 22px 24px 20px;
    }

    .zqy-platform-setting__section + .zqy-platform-setting__section {
        border-top: 1px solid getCssVar('border-color', 'lighter');
    }

    .zqy-platform-setting__section-title {
        margin-bottom: 18px;
        font-size: getCssVar('font-size', 'base');
        font-weight: 600;
        line-height: 20px;
        color: getCssVar('text-color', 'primary');
    }

    .zqy-platform-setting__section--basic {
        .zqy-platform-setting__section-title {
            margin-bottom: 22px;
        }

        .zqy-platform-setting__switch-row--plain {
            min-height: 24px;
            padding-bottom: 0;
            margin-bottom: 14px;
        }

        .zqy-platform-setting__retention-item {
            margin-bottom: 16px;
        }

        .zqy-platform-setting__auto-tenant-grid {
            margin-bottom: 16px;
        }
    }

    .zqy-platform-setting__section--brand {
        .el-form-item {
            max-width: 420px;
            margin-bottom: 20px;
        }
    }

    .zqy-platform-setting__title-row,
    .zqy-platform-setting__color-row,
    .zqy-platform-setting__number-row,
    .zqy-platform-setting__retention-row {
        display: flex;
        align-items: center;
        gap: 8px;
        width: 100%;
    }

    .zqy-platform-setting__retention-row {
        .el-input-number {
            width: 160px;
        }
    }

    .zqy-platform-setting__auto-tenant-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 12px;
        max-width: 720px;
    }

    .zqy-platform-setting__number-row {
        .el-input-number {
            width: 100%;
            min-width: 0;
        }
    }

    .zqy-platform-setting__title-row {
        .el-input {
            flex: 1;
            min-width: 0;
        }

        .el-button + .el-button {
            margin-left: 0;
        }
    }

    .zqy-platform-setting__switch-row {
        display: flex;
        min-height: 32px;
        align-items: center;
        justify-content: space-between;
        padding-bottom: 18px;
        margin-bottom: 18px;
        border-bottom: 1px solid getCssVar('border-color', 'lighter');
    }

    .zqy-platform-setting__switch-row--plain {
        padding-bottom: 8px;
        margin-bottom: 0;
        border-bottom: 0;
    }

    .zqy-platform-setting__retention-item {
        max-width: 420px;
    }

    .zqy-platform-setting__label {
        font-size: getCssVar('font-size', 'extra-small');
        line-height: 20px;
        color: getCssVar('text-color', 'regular');
    }

    .el-form-item {
        margin-bottom: 0;

        .el-form-item__label {
            height: auto;
            padding: 0 0 8px;
            margin-bottom: 0;
            line-height: 20px;
            color: getCssVar('text-color', 'regular');
        }
    }

    .el-textarea {
        width: 100%;

        .el-textarea__inner {
            min-height: 160px !important;
            padding: 10px 12px;
            line-height: 20px;
            resize: vertical;
        }

        .el-input__count {
            right: 10px;
            bottom: 8px;
            line-height: 18px;
            color: getCssVar('text-color', 'placeholder');
            background-color: transparent;
        }
    }

    .zqy-platform-setting__brand-grid {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 12px;
    }

    .zqy-platform-setting__brand-item {
        display: flex;
        min-width: 0;
        min-height: 128px;
        padding: 14px;
        box-sizing: border-box;
        border: 1px solid getCssVar('border-color', 'lighter');
        border-radius: 6px;
        background-color: getCssVar('fill-color', 'blank');
    }

    .zqy-platform-setting__brand-preview {
        width: 112px;
        height: 100px;
        flex: 0 0 112px;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 8px;
        box-sizing: border-box;
        border: 1px dashed getCssVar('border-color');
        border-radius: 4px;
        background:
            linear-gradient(45deg, #f5f7fa 25%, transparent 25%),
            linear-gradient(-45deg, #f5f7fa 25%, transparent 25%),
            linear-gradient(45deg, transparent 75%, #f5f7fa 75%),
            linear-gradient(-45deg, transparent 75%, #f5f7fa 75%);
        background-color: #fff;
        background-position: 0 0, 0 6px, 6px -6px, -6px 0;
        background-size: 12px 12px;

        img {
            max-width: 100%;
            max-height: 100%;
            object-fit: contain;
        }
    }

    .zqy-platform-setting__brand-info {
        min-width: 0;
        height: 100px;
        flex: 1;
        display: flex;
        flex-direction: column;
        padding-left: 12px;
    }

    .zqy-platform-setting__brand-name {
        font-size: getCssVar('font-size', 'extra-small');
        font-weight: 600;
        line-height: 18px;
        color: getCssVar('text-color', 'primary');
    }

    .zqy-platform-setting__brand-desc {
        min-height: 36px;
        margin-top: 4px;
        font-size: 12px;
        line-height: 18px;
        color: getCssVar('text-color', 'secondary');
    }

    .zqy-platform-setting__brand-actions {
        display: flex;
        gap: 8px;
        margin-top: auto;

        .el-upload {
            display: block;
        }

        .el-button + .el-button {
            margin-left: 0;
        }
    }

    .zqy-platform-setting__brand-save {
        display: flex;
        justify-content: flex-end;
        padding-top: 16px;
    }

    .zqy-platform-setting__actions {
        display: flex;
        min-height: 44px;
        padding-top: 12px;
        box-sizing: border-box;
        align-items: center;
        justify-content: flex-end;
    }

    @media (max-width: 980px) {
        .zqy-platform-setting__brand-grid {
            grid-template-columns: 1fr;
        }

        .zqy-platform-setting__auto-tenant-grid {
            grid-template-columns: 1fr;
            max-width: 420px;
        }
    }
}
</style>
