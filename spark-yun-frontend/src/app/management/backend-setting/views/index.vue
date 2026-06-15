<template>
    <Breadcrumb :bread-crumb-list="[{ name: '后台设置', code: 'backend-setting' }]" />
    <div class="backend-setting-page" v-loading="loading">
        <el-form ref="formRef" label-position="top" :model="form" :rules="rules">
            <el-form-item label="租户名称" prop="name">
                <el-input v-model="form.name" maxlength="100" placeholder="请输入租户名称" show-word-limit />
            </el-form-item>
            <div class="backend-setting-page__actions">
                <el-button type="primary" :loading="saving" @click="saveSetting">保存</el-button>
            </div>
        </el-form>
    </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import { useAuthStore } from '@/app/store/useAuth'
import { GetTenant, UpdateTenantForTenantAdmin } from '@/app/management/backend-setting/api'

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
    id: '',
    name: ''
})
const rules = reactive<FormRules>({
    name: [
        {
            required: true,
            message: '请输入租户名称',
            trigger: ['blur', 'change']
        }
    ]
})

function loadSetting() {
    if (!authStore.tenantId) {
        return
    }
    loading.value = true
    GetTenant({
        tenantId: authStore.tenantId
    })
        .then((res: any) => {
            form.id = res.data?.id || authStore.tenantId
            form.name = res.data?.name || ''
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
            ElMessage.warning('请输入租户名称')
            return
        }
        saving.value = true
        UpdateTenantForTenantAdmin({
            id: form.id || authStore.tenantId,
            name: form.name
        })
            .then(() => {
                ElMessage.success('保存成功')
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
.backend-setting-page {
    width: 520px;
    padding: 20px;
    box-sizing: border-box;

    .el-form-item {
        margin-bottom: 20px;
    }
}

.backend-setting-page__actions {
    display: flex;
    justify-content: flex-end;
}
</style>
