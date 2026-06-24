<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-backend-setting backend-setting-page">
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="loadSetting">
            <div class="zqy-backend-setting__wrap">
                <el-form ref="formRef" class="zqy-backend-setting__form" label-position="top" :model="form" :rules="rules">
                    <div class="zqy-backend-setting__section">
                        <el-form-item :label="t('backendSetting.tenantName')" prop="name">
                            <el-input
                                v-model="form.name"
                                maxlength="100"
                                :placeholder="t('backendSetting.inputTenantName')"
                                show-word-limit
                            />
                        </el-form-item>
                    </div>
                    <div class="zqy-backend-setting__actions">
                        <el-button type="primary" :loading="saving" @click="saveSetting">
                            {{ t('common.save') }}
                        </el-button>
                    </div>
                </el-form>
            </div>
        </LoadingPage>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useI18n } from 'vue-i18n'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import { useAuthStore } from '@/app/store/useAuth'
import eventBus from '@/app/utils/eventBus'
import { GetTenant, UpdateTenantForTenantAdmin } from '@/app/management/backend-setting/api'

const { t } = useI18n({ useScope: 'global' })
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const networkError = ref(false)
const form = reactive({
    id: '',
    name: ''
})
const breadCrumbList = computed(() => [
    {
        name: t('menu.backend-setting'),
        code: 'backend-setting'
    }
])
const rules = computed<FormRules>(() => ({
    name: [
        {
            required: true,
            message: t('backendSetting.inputTenantName'),
            trigger: ['blur', 'change']
        }
    ]
}))

function loadSetting() {
    if (!authStore.tenantId) {
        return
    }
    loading.value = true
    networkError.value = false
    GetTenant({
        tenantId: authStore.tenantId
    })
        .then((res: any) => {
            form.id = res.data?.id || authStore.tenantId
            form.name = res.data?.name || ''
        })
        .catch(() => {
            networkError.value = true
        })
        .finally(() => {
            loading.value = false
        })
}

function saveSetting() {
    formRef.value?.validate((valid) => {
        if (!valid || saving.value) {
            return
        }
        form.name = form.name.trim()
        if (!form.name) {
            ElMessage.warning(t('backendSetting.inputTenantName'))
            return
        }
        saving.value = true
        UpdateTenantForTenantAdmin({
            id: form.id || authStore.tenantId,
            name: form.name
        })
            .then(() => {
                ElMessage.success(t('backendSetting.saveSuccess'))
                eventBus.emit('tenantListUpdate')
                loadSetting()
            })
            .finally(() => {
                saving.value = false
            })
    })
}

onMounted(() => {
    loadSetting()
})
</script>

<style lang="scss">
.zqy-backend-setting.backend-setting-page {
    min-height: calc(100vh - 114px);
    padding: 24px 20px;
    box-sizing: border-box;
    overflow: auto;
    background-color: #fff;

    .zqy-loading {
        min-height: calc(100vh - 162px);
    }

    .zqy-backend-setting__wrap {
        width: 100%;
        max-width: 760px;
    }

    .zqy-backend-setting__form {
        width: 100%;
        background-color: #fff;
    }

    .zqy-backend-setting__section {
        padding: 22px 24px 20px;
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

    .el-input {
        width: 100%;

        .el-input__wrapper {
            border-radius: 2px;
        }

        .el-input__count {
            color: getCssVar('text-color', 'placeholder');
            background-color: transparent;
        }
    }

    .zqy-backend-setting__actions {
        display: flex;
        min-height: 56px;
        padding: 12px 24px 14px;
        box-sizing: border-box;
        align-items: center;
        justify-content: flex-end;
    }
}
</style>
