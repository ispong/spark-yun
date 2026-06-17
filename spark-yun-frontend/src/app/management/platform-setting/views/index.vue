<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-platform-setting platform-setting-page">
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData">
            <div class="zqy-platform-setting__wrap">
                <el-form class="zqy-platform-setting__form" label-position="top">
                    <div class="zqy-platform-setting__section">
                        <div class="zqy-platform-setting__switch-row">
                            <span class="zqy-platform-setting__label">注册后自动创建租户</span>
                            <el-switch v-model="form.autoCreateTenant" />
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
                    </div>
                    <div class="zqy-platform-setting__actions">
                        <el-button type="primary" :loading="saving" @click="saveSetting">保存</el-button>
                    </div>
                </el-form>
            </div>
        </LoadingPage>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import { GetPlatformSetting, UpdatePlatformSetting, type PlatformSetting } from '../api'
import { BreadCrumbList } from './platform-setting.config'

const breadCrumbList = reactive(BreadCrumbList)
const loading = ref(false)
const saving = ref(false)
const networkError = ref(false)

const form = reactive<PlatformSetting>({
    description: '',
    autoCreateTenant: true
})

function initData() {
    loading.value = true
    networkError.value = false
    GetPlatformSetting()
        .then((res: any) => {
            form.description = res.data?.description || ''
            form.autoCreateTenant = res.data?.autoCreateTenant ?? true
            loading.value = false
        })
        .catch(() => {
            loading.value = false
            networkError.value = true
        })
}

function saveSetting() {
    saving.value = true
    UpdatePlatformSetting({
        description: form.description,
        autoCreateTenant: form.autoCreateTenant
    })
        .then((res: any) => {
            ElMessage.success(res.msg)
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
    min-height: calc(100vh - 114px);
    padding: 24px 20px;
    box-sizing: border-box;
    overflow: auto;
    background-color: #fff;

    .zqy-loading {
        min-height: calc(100vh - 162px);
    }

    .zqy-platform-setting__wrap {
        width: 100%;
        max-width: 760px;
    }

    .zqy-platform-setting__form {
        width: 100%;
        background-color: #fff;
    }

    .zqy-platform-setting__section {
        padding: 22px 24px 20px;
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

    .zqy-platform-setting__actions {
        display: flex;
        min-height: 56px;
        padding: 12px 24px 14px;
        box-sizing: border-box;
        align-items: center;
        justify-content: flex-end;
    }
}
</style>
