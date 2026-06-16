<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-platform-setting">
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData">
            <el-form class="zqy-platform-setting__form" label-position="top">
                <el-form-item label="注册创建租户">
                    <el-switch v-model="form.autoCreateTenant" />
                </el-form-item>
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
            </el-form>
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
.zqy-platform-setting {
    height: calc(100vh - 114px);
    padding: 20px;
    box-sizing: border-box;
    overflow: auto;

    .zqy-platform-setting__form {
        max-width: 720px;
    }

    .zqy-platform-setting__actions {
        display: flex;
        justify-content: flex-end;
    }
}
</style>
