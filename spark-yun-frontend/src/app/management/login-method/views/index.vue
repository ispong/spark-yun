<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-login-method zqy-seach-table">
        <div class="zqy-table-top">
            <div class="zqy-login-method__settings">
                <div class="zqy-login-method__setting-item">
                    <span>默认登录方式</span>
                    <el-select
                        v-model="form.defaultLoginMethod"
                        class="zqy-login-method__default-select"
                        :disabled="saving"
                        @change="saveConfig(true)"
                    >
                        <el-option
                            v-for="item in defaultLoginMethodOptions"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                </div>
                <div class="zqy-login-method__setting-item">
                    <span>自动注册初始化租户</span>
                    <el-switch
                        v-model="form.autoCreateTenant"
                        :loading="saving"
                        @change="saveConfig(true)"
                    />
                </div>
            </div>
            <el-button :loading="loading" @click="initData(false)">刷新</el-button>
        </div>

        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData(false)">
            <div class="zqy-login-method__content">
                <div class="zqy-login-method__cards">
                    <el-card class="zqy-login-method__card">
                        <template #header>
                            <div class="zqy-login-method__card-title">
                                <span>
                                    <el-icon><User /></el-icon>
                                    账号登录
                                </span>
                                <el-switch
                                    v-model="form.accountEnabled"
                                    :loading="saving"
                                    @change="saveConfig(true)"
                                />
                            </div>
                        </template>
                        <div class="zqy-login-method__card-body">
                            <el-checkbox
                                v-model="form.accountPhonePasswordEnabled"
                                :disabled="!form.accountEnabled || saving"
                                @change="saveConfig(true)"
                            >
                                支持手机+密码登录
                            </el-checkbox>
                            <el-checkbox
                                v-model="form.accountEmailPasswordEnabled"
                                :disabled="!form.accountEnabled || saving"
                                @change="saveConfig(true)"
                            >
                                支持邮箱+密码登录
                            </el-checkbox>
                        </div>
                    </el-card>

                    <el-card class="zqy-login-method__card">
                        <template #header>
                            <div class="zqy-login-method__card-title">
                                <span>
                                    <el-icon><Message /></el-icon>
                                    邮箱登录
                                </span>
                                <el-switch
                                    v-model="form.emailEnabled"
                                    :loading="saving"
                                    @change="saveConfig(true)"
                                />
                            </div>
                        </template>
                        <div class="zqy-login-method__card-body">
                            <div class="zqy-login-method__switch-row">
                                <span>支持注册</span>
                                <el-switch
                                    v-model="form.emailRegisterEnabled"
                                    :disabled="saving"
                                    @change="saveConfig(true)"
                                />
                            </div>
                            <el-form label-position="top" class="zqy-login-method__form">
                                <el-form-item label="SMTP服务器">
                                    <el-input v-model="form.config.emailConfig.host" />
                                </el-form-item>
                                <el-form-item label="SMTP端口">
                                    <el-input-number v-model="form.config.emailConfig.port" :min="1" :max="65535" />
                                </el-form-item>
                                <el-form-item label="用户名">
                                    <el-input v-model="form.config.emailConfig.username" />
                                </el-form-item>
                                <el-form-item label="密码">
                                    <el-input
                                        v-model="form.config.emailConfig.password"
                                        type="password"
                                        show-password
                                    />
                                </el-form-item>
                                <el-form-item label="发件邮箱">
                                    <el-input v-model="form.config.emailConfig.fromAddress" />
                                </el-form-item>
                                <el-form-item label="邮件标题">
                                    <el-input v-model="form.config.emailConfig.subject" />
                                </el-form-item>
                                <el-form-item label="SSL">
                                    <el-switch v-model="form.config.emailConfig.ssl" />
                                </el-form-item>
                                <el-form-item label="STARTTLS">
                                    <el-switch v-model="form.config.emailConfig.startTls" />
                                </el-form-item>
                            </el-form>
                            <div class="zqy-login-method__actions">
                                <el-button :loading="saving" type="primary" @click="saveConfig(false)">
                                    保存配置
                                </el-button>
                                <el-button @click="showRecord('EMAIL')">发送记录</el-button>
                            </div>
                        </div>
                    </el-card>

                    <el-card class="zqy-login-method__card">
                        <template #header>
                            <div class="zqy-login-method__card-title">
                                <span>
                                    <el-icon><Iphone /></el-icon>
                                    手机登录
                                </span>
                                <el-switch
                                    v-model="form.phoneEnabled"
                                    :loading="saving"
                                    @change="saveConfig(true)"
                                />
                            </div>
                        </template>
                        <div class="zqy-login-method__card-body">
                            <div class="zqy-login-method__switch-row">
                                <span>支持注册</span>
                                <el-switch
                                    v-model="form.phoneRegisterEnabled"
                                    :disabled="saving"
                                    @change="saveConfig(true)"
                                />
                            </div>
                            <el-form label-position="top" class="zqy-login-method__form">
                                <el-form-item label="服务商">
                                    <el-select v-model="form.config.phoneConfig.provider">
                                        <el-option label="阿里云" value="ALIYUN" />
                                    </el-select>
                                </el-form-item>
                                <el-form-item label="RegionId">
                                    <el-input v-model="form.config.phoneConfig.regionId" />
                                </el-form-item>
                                <el-form-item label="AccessKeyId">
                                    <el-input v-model="form.config.phoneConfig.accessKeyId" />
                                </el-form-item>
                                <el-form-item label="AccessKeySecret">
                                    <el-input
                                        v-model="form.config.phoneConfig.accessKeySecret"
                                        type="password"
                                        show-password
                                    />
                                </el-form-item>
                                <el-form-item label="短信签名">
                                    <el-input v-model="form.config.phoneConfig.signName" />
                                </el-form-item>
                                <el-form-item label="模板Code">
                                    <el-input v-model="form.config.phoneConfig.templateCode" />
                                </el-form-item>
                                <el-form-item label="验证码变量名">
                                    <el-input v-model="form.config.phoneConfig.templateParamName" />
                                </el-form-item>
                            </el-form>
                            <div class="zqy-login-method__actions">
                                <el-button :loading="saving" type="primary" @click="saveConfig(false)">
                                    保存配置
                                </el-button>
                                <el-button @click="showRecord('PHONE')">发送记录</el-button>
                            </div>
                        </div>
                    </el-card>
                </div>

                <div class="zqy-login-method__records">
                    <div class="zqy-login-method__records-top">
                        <div class="zqy-login-method__records-title">发送记录</div>
                        <div class="zqy-login-method__records-tools">
                            <el-select v-model="recordChannel" clearable placeholder="全部方式" @change="initRecord">
                                <el-option label="邮箱登录" value="EMAIL" />
                                <el-option label="手机登录" value="PHONE" />
                            </el-select>
                            <el-input
                                v-model="recordKeyword"
                                placeholder="请输入接收账号/状态 回车搜索"
                                clearable
                                @input="inputEvent"
                                @keyup.enter="initRecord"
                            />
                        </div>
                    </div>
                    <div class="zqy-table">
                        <BlockTable
                            :table-config="tableConfig"
                            @size-change="handleSizeChange"
                            @current-change="handleCurrentChange"
                        >
                            <template #channel="scopeSlot">
                                <el-tag v-if="scopeSlot.row.channel === 'EMAIL'">邮箱登录</el-tag>
                                <el-tag v-else type="success">手机登录</el-tag>
                            </template>
                            <template #sendStatus="scopeSlot">
                                <el-tag v-if="scopeSlot.row.sendStatus === 'SUCCESS'" type="success">成功</el-tag>
                                <el-tag v-else type="danger">失败</el-tag>
                            </template>
                            <template #verifyStatus="scopeSlot">
                                <el-tag :type="getVerifyStatusType(scopeSlot.row.verifyStatus)">
                                    {{ getVerifyStatusText(scopeSlot.row.verifyStatus) }}
                                </el-tag>
                            </template>
                            <template #booleanTag="scopeSlot">
                                <el-tag v-if="scopeSlot.row.registered" type="success">是</el-tag>
                                <el-tag v-else>否</el-tag>
                            </template>
                            <template #tenantTag="scopeSlot">
                                <el-tag v-if="scopeSlot.row.autoTenantCreated" type="success">是</el-tag>
                                <el-tag v-else>否</el-tag>
                            </template>
                        </BlockTable>
                    </div>
                </div>
            </div>
        </LoadingPage>
    </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Iphone, Message, User } from '@element-plus/icons-vue'

import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import BlockTable from '@/app/components/block-table/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import {
    GetLoginMethodConfig,
    PageLoginCodeRecord,
    UpdateLoginMethodConfig,
    type LoginChannel,
    type LoginMethodConfig,
    type LoginMethodType
} from '@/app/management/login-method/api'
import { BreadCrumbList, TableConfig } from './login-method.config'

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const loading = ref(false)
const saving = ref(false)
const networkError = ref(false)
const recordKeyword = ref('')
const recordChannel = ref('')

const form = reactive<LoginMethodConfig>(createDefaultForm())
const defaultLoginMethodOptions = computed(() => {
    const options: Array<{ label: string; value: LoginMethodType }> = []
    if (form.accountEnabled) {
        options.push({ label: '账号登录', value: 'ACCOUNT' })
    }
    if (form.phoneEnabled) {
        options.push({ label: '手机登录', value: 'PHONE' })
    }
    if (form.emailEnabled) {
        options.push({ label: '邮箱登录', value: 'EMAIL' })
    }
    return options
})

function createDefaultForm(): LoginMethodConfig {
    return {
        defaultLoginMethod: 'ACCOUNT',
        accountEnabled: true,
        accountPhonePasswordEnabled: false,
        accountEmailPasswordEnabled: false,
        emailEnabled: false,
        emailRegisterEnabled: false,
        phoneEnabled: false,
        phoneRegisterEnabled: false,
        autoCreateTenant: true,
        config: {
            emailConfig: {
                port: 587,
                ssl: false,
                startTls: true,
                subject: '至轻云登录验证码'
            },
            phoneConfig: {
                provider: 'ALIYUN',
                regionId: 'cn-hangzhou',
                templateParamName: 'code'
            }
        }
    }
}

function normalizeConfig(data: Partial<LoginMethodConfig>): LoginMethodConfig {
    const base = createDefaultForm()
    const config = {
        ...base,
        ...data,
        config: {
            emailConfig: {
                ...base.config.emailConfig,
                ...(data.config?.emailConfig || {})
            },
            phoneConfig: {
                ...base.config.phoneConfig,
                ...(data.config?.phoneConfig || {})
            }
        }
    }
    config.defaultLoginMethod = resolveDefaultLoginMethod(config.defaultLoginMethod, config)
    return config
}

function applyConfig(data: Partial<LoginMethodConfig>) {
    Object.assign(form, normalizeConfig(data))
}

function cloneForm(): LoginMethodConfig {
    form.defaultLoginMethod = resolveDefaultLoginMethod(form.defaultLoginMethod, form)
    return JSON.parse(JSON.stringify(form))
}

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = false
    GetLoginMethodConfig()
        .then((res: any) => {
            applyConfig(res.data || {})
            loading.value = false
            networkError.value = false
            initRecord()
        })
        .catch(() => {
            loading.value = false
            networkError.value = true
        })
}

function saveConfig(silent: boolean) {
    if (!form.accountEnabled && !form.emailEnabled && !form.phoneEnabled) {
        ElMessage.error('至少需要开启一种登录方式')
        initData(true)
        return
    }
    form.defaultLoginMethod = resolveDefaultLoginMethod(form.defaultLoginMethod, form)
    saving.value = true
    UpdateLoginMethodConfig(cloneForm())
        .then((res: any) => {
            if (!silent) {
                ElMessage.success(res.msg)
            }
            applyConfig(res.data || form)
        })
        .catch(() => {
            initData(true)
        })
        .finally(() => {
            saving.value = false
        })
}

function initRecord() {
    PageLoginCodeRecord({
        page: tableConfig.pagination.currentPage - 1,
        pageSize: tableConfig.pagination.pageSize,
        searchKeyWord: recordKeyword.value,
        channel: recordChannel.value
    })
        .then((res: any) => {
            tableConfig.tableData = res.data.content
            tableConfig.pagination.total = res.data.totalElements
            tableConfig.loading = false
        })
        .catch(() => {
            tableConfig.tableData = []
            tableConfig.pagination.total = 0
            tableConfig.loading = false
        })
}

function showRecord(channel: LoginChannel) {
    recordChannel.value = channel
    tableConfig.pagination.currentPage = 1
    initRecord()
}

function inputEvent(value: string) {
    if (value === '') {
        initRecord()
    }
}

function handleSizeChange(size: number) {
    tableConfig.pagination.pageSize = size
    initRecord()
}

function handleCurrentChange(page: number) {
    tableConfig.pagination.currentPage = page
    initRecord()
}

function getVerifyStatusType(status: string) {
    if (status === 'VERIFIED') return 'success'
    if (status === 'WAIT') return 'warning'
    return 'danger'
}

function getVerifyStatusText(status: string) {
    const statusText: Record<string, string> = {
        WAIT: '待验证',
        VERIFIED: '已验证',
        EXPIRED: '已过期',
        FAIL: '失败'
    }
    return statusText[status] || status
}

function resolveDefaultLoginMethod(defaultLoginMethod: LoginMethodType | undefined, config: LoginMethodConfig): LoginMethodType {
    if (defaultLoginMethod === 'ACCOUNT' && config.accountEnabled) return 'ACCOUNT'
    if (defaultLoginMethod === 'PHONE' && config.phoneEnabled) return 'PHONE'
    if (defaultLoginMethod === 'EMAIL' && config.emailEnabled) return 'EMAIL'
    if (config.accountEnabled) return 'ACCOUNT'
    if (config.phoneEnabled) return 'PHONE'
    return 'EMAIL'
}

onMounted(() => {
    tableConfig.pagination.currentPage = 1
    tableConfig.pagination.pageSize = 10
    initData()
})
</script>

<style lang="scss">
.zqy-login-method {
    .zqy-login-method__settings {
        display: flex;
        align-items: center;
        gap: 24px;
    }

    .zqy-login-method__setting-item {
        display: flex;
        align-items: center;
        gap: 12px;
        color: getCssVar('text-color', 'primary');
        font-size: getCssVar('font-size', 'small');
    }

    .zqy-login-method__default-select {
        width: 132px;
    }

    .zqy-login-method__content {
        height: calc(100vh - 114px);
        overflow: auto;
        padding: 0 20px 20px;
        box-sizing: border-box;
    }

    .zqy-login-method__cards {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 16px;
        align-items: stretch;
    }

    .zqy-login-method__card {
        min-width: 0;

        .el-card__header {
            padding: 14px 16px;
        }

        .el-card__body {
            padding: 16px;
        }
    }

    .zqy-login-method__card-title {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        color: getCssVar('text-color', 'primary');
        font-size: 14px;
        font-weight: 600;

        span {
            display: inline-flex;
            align-items: center;
            gap: 6px;
            min-width: 0;
        }
    }

    .zqy-login-method__card-body {
        display: flex;
        flex-direction: column;
        gap: 12px;
    }

    .zqy-login-method__switch-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        min-height: 32px;
        color: getCssVar('text-color', 'regular');
        font-size: getCssVar('font-size', 'extra-small');
    }

    .zqy-login-method__form {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 0 12px;

        .el-form-item {
            margin-bottom: 12px;
        }
    }

    .zqy-login-method__actions {
        display: flex;
        justify-content: flex-end;
        gap: 8px;
        padding-top: 4px;
    }

    .zqy-login-method__records {
        margin-top: 20px;
        border-top: 1px solid var(--el-border-color-light);
        padding-top: 16px;
    }

    .zqy-login-method__records-top {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 16px;
        padding-bottom: 12px;
    }

    .zqy-login-method__records-title {
        font-size: 14px;
        font-weight: 600;
        color: getCssVar('text-color', 'primary');
    }

    .zqy-login-method__records-tools {
        display: flex;
        justify-content: flex-end;
        gap: 8px;

        .el-select {
            width: 120px;
        }

        .el-input {
            width: 300px;
        }
    }
}

@media (max-width: 1200px) {
    .zqy-login-method {
        .zqy-login-method__cards {
            grid-template-columns: 1fr;
        }
    }
}
</style>
