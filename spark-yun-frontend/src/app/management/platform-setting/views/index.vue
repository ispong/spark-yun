<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-platform-setting platform-setting-page">
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData">
            <div class="zqy-platform-setting__wrap">
                <el-form class="zqy-platform-setting__form" label-position="top">
                    <div class="zqy-platform-setting__section">
                        <div class="zqy-platform-setting__section-title">基础设置</div>
                        <div class="zqy-platform-setting__switch-row">
                            <span class="zqy-platform-setting__label">注册后自动创建租户</span>
                            <el-switch
                                v-model="form.autoCreateTenant"
                                :loading="autoCreateTenantSaving"
                                @change="saveAutoCreateTenant"
                            />
                        </div>
                        <el-form-item label="平台描述">
                            <el-input
                                v-model="form.description"
                                type="textarea"
                                :rows="8"
                                :maxlength="2000"
                                show-word-limit
                            />
                        </el-form-item>
                        <div class="zqy-platform-setting__actions">
                            <el-button type="primary" :loading="saving" @click="saveSetting">保存</el-button>
                        </div>
                    </div>
                    <div class="zqy-platform-setting__section zqy-platform-setting__section--brand">
                        <div class="zqy-platform-setting__section-title">品牌设置</div>
                        <el-form-item label="浏览器标题">
                            <div class="zqy-platform-setting__title-row">
                                <el-input
                                    v-model="form.browserTitle"
                                    clearable
                                    :maxlength="100"
                                    placeholder="请输入浏览器标题文字"
                                />
                                <el-button type="primary" :loading="saving" @click="saveSetting">保存</el-button>
                                <el-button @click="form.browserTitle = defaultBrandSetting.browserTitle">
                                    恢复默认
                                </el-button>
                            </div>
                        </el-form-item>
                        <el-form-item label="主题色">
                            <div class="zqy-platform-setting__color-row">
                                <el-color-picker
                                    v-model="form.themeColor"
                                    :clearable="false"
                                    :predefine="themePredefineColors"
                                    @change="saveThemeColor"
                                />
                                <el-button @click="resetThemeColor">恢复默认</el-button>
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
                                                上传
                                            </el-button>
                                        </el-upload>
                                        <el-button @click="resetBrandImage(item.key)">恢复默认</el-button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </el-form>
            </div>
        </LoadingPage>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type UploadRawFile } from 'element-plus'

import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import { GetPlatformSetting, UpdatePlatformSetting, UploadBrandImage, type PlatformSetting } from '../api'
import { BreadCrumbList } from './platform-setting.config'
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
const MAX_BRAND_FILE_SIZE = 2 * 1024 * 1024

const breadCrumbList = reactive(BreadCrumbList)
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
    browserTitle: '',
    themeColor: defaultBrandSetting.themeColor,
    faviconUrl: '',
    topLogoUrl: '',
    topLogoSmallUrl: '',
    loginMainImageUrl: ''
})

const themePredefineColors = ['#f34c00', '#409eff', '#1677ff', '#21ba45', '#722ed1', '#db2828']

const brandImageItems: BrandImageItem[] = [
    {
        key: 'faviconUrl',
        label: '浏览器标签图标',
        description: '显示在浏览器标签页，建议使用 ico、svg 或 32x32 png。',
        accept: ICON_ACCEPT
    },
    {
        key: 'topLogoUrl',
        label: '品牌 Logo',
        description: '用于系统内页菜单展开态和登录页左上角 Logo，建议尺寸 498x126。',
        accept: IMAGE_ACCEPT
    },
    {
        key: 'topLogoSmallUrl',
        label: '顶部小 Logo',
        description: '用于系统内页菜单收起态的顶部品牌 Logo，建议尺寸 475x346。',
        accept: IMAGE_ACCEPT
    },
    {
        key: 'loginMainImageUrl',
        label: '登录页主视觉图',
        description: '用于登录页左侧的大幅视觉图，建议尺寸 1707x1470。',
        accept: IMAGE_ACCEPT
    }
]

function initData() {
    loading.value = true
    networkError.value = false
    GetPlatformSetting()
        .then((res: any) => {
            form.description = res.data?.description || ''
            form.autoCreateTenant = res.data?.autoCreateTenant ?? false
            form.browserTitle = res.data?.browserTitle || defaultBrandSetting.browserTitle
            form.themeColor = res.data?.themeColor || defaultBrandSetting.themeColor
            form.faviconUrl = res.data?.faviconUrl || ''
            form.topLogoUrl = res.data?.topLogoUrl || ''
            form.topLogoSmallUrl = res.data?.topLogoSmallUrl || ''
            form.loginMainImageUrl = res.data?.loginMainImageUrl || ''
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
        browserTitle: form.browserTitle?.trim() || '',
        themeColor: normalizeThemeColor(form.themeColor),
        faviconUrl: form.faviconUrl || '',
        topLogoUrl: form.topLogoUrl || '',
        topLogoSmallUrl: form.topLogoSmallUrl || '',
        loginMainImageUrl: form.loginMainImageUrl || ''
    }
}

function previewBrandImage(key: BrandImageKey): string {
    return form[key] || defaultBrandSetting[key]
}

function resetBrandImage(key: BrandImageKey) {
    form[key] = ''
    saveBrandImageSetting()
}

function resetThemeColor() {
    form.themeColor = defaultBrandSetting.themeColor
    saveThemeColor()
}

function saveThemeColor() {
    if (!/^#[0-9a-fA-F]{6}$/.test(form.themeColor?.trim() || '')) {
        ElMessage.warning('请输入正确的主题色')
        return
    }
    saveSetting()
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
        ElMessage.warning('请上传图片文件')
        return
    }
    if (file.size > MAX_BRAND_FILE_SIZE) {
        ElMessage.warning('图片大小不能超过2MB')
        return
    }

    brandUploadLoading[key] = true
    const formData = new FormData()
    formData.append('file', file)
    UploadBrandImage(formData)
        .then((res: any) => {
            const imageUrl = getUploadImageUrl(res)
            if (!imageUrl) {
                ElMessage.error('上传成功但未返回图片地址')
                return
            }
            form[key] = imageUrl
            return saveBrandImageSetting()
        })
        .finally(() => {
            brandUploadLoading[key] = false
        })
}

function getUploadImageUrl(res: any): string {
    return res?.data?.data?.url || res?.data?.url || res?.data?.data || res?.url || ''
}

function saveBrandImageSetting() {
    saving.value = true
    return UpdatePlatformSetting(createSubmitParams())
        .then(() => {
            applyBrandSetting(createSubmitParams())
            ElMessage.success('配置已保存')
        })
        .finally(() => {
            saving.value = false
        })
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

    .zqy-platform-setting__section--brand {
        .el-form-item {
            max-width: 420px;
            margin-bottom: 20px;
        }
    }

    .zqy-platform-setting__title-row,
    .zqy-platform-setting__color-row {
        display: flex;
        align-items: center;
        gap: 8px;
        width: 100%;
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
        transform: translateY(-2px);

        .el-upload {
            display: block;
        }

        .el-button + .el-button {
            margin-left: 0;
        }
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
    }
}
</style>
