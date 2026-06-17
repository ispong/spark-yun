<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="zqy-login-method zqy-seach-table">
        <LoadingPage :visible="loading" :network-error="networkError" @loading-refresh="initData(false)">
            <div class="zqy-login-method__content">
                <el-card class="zqy-login-method__card zqy-login-method__card--account" shadow="never">
                    <template #header>
                        <div class="zqy-login-method__card-header">
                            <div class="zqy-login-method__card-name">
                                <el-icon><User /></el-icon>
                                账号登录
                            </div>
                            <div class="zqy-login-method__header-switches zqy-login-method__header-placeholder" aria-hidden="true">
                                <span>启用</span>
                                <el-switch disabled />
                            </div>
                        </div>
                    </template>
                    <div class="zqy-login-method__card-body">
                        <div class="zqy-login-method__option-row">
                            <span>首页登录方式</span>
                            <el-switch
                                :model-value="form.defaultLoginMethod === 'ACCOUNT'"
                                :disabled="saving"
                                @change="changeDefaultLoginMethod('ACCOUNT', $event)"
                            />
                        </div>
                        <div class="zqy-login-method__option-row">
                            <span>手机+密码登录</span>
                            <el-switch
                                v-model="form.accountPhonePasswordEnabled"
                                :disabled="saving"
                                @change="persistConfig"
                            />
                        </div>
                        <div class="zqy-login-method__option-row">
                            <span>邮箱+密码登录</span>
                            <el-switch
                                v-model="form.accountEmailPasswordEnabled"
                                :disabled="saving"
                                @change="persistConfig"
                            />
                        </div>
                    </div>
                </el-card>

                <el-card class="zqy-login-method__card zqy-login-method__card--phone" shadow="never">
                    <template #header>
                        <div class="zqy-login-method__card-header">
                            <div class="zqy-login-method__card-name">
                                <el-icon><Iphone /></el-icon>
                                手机登录
                            </div>
                            <div class="zqy-login-method__header-switches">
                                <span>启用</span>
                                <el-switch v-model="form.phoneEnabled" :loading="saving" @change="persistConfig" />
                            </div>
                        </div>
                    </template>
                    <div class="zqy-login-method__card-body">
                        <div class="zqy-login-method__option-row" :class="{ 'is-disabled': !form.phoneEnabled }">
                            <span>首页登录方式</span>
                            <el-switch
                                :model-value="form.defaultLoginMethod === 'PHONE'"
                                :disabled="!form.phoneEnabled || saving"
                                @change="changeDefaultLoginMethod('PHONE', $event)"
                            />
                        </div>
                        <div class="zqy-login-method__option-row" :class="{ 'is-disabled': !form.phoneEnabled }">
                            <span>自动注册</span>
                            <el-switch
                                v-model="form.phoneRegisterEnabled"
                                :disabled="!form.phoneEnabled || saving"
                                @change="persistConfig"
                            />
                        </div>
                        <button
                            class="zqy-login-method__config-row"
                            :class="{ 'is-disabled': !form.phoneEnabled }"
                            type="button"
                            :disabled="!form.phoneEnabled || saving"
                            @click="openPhoneConfig"
                        >
                            <span>短信配置</span>
                            <el-icon><Setting /></el-icon>
                        </button>
                    </div>
                </el-card>

                <el-card class="zqy-login-method__card zqy-login-method__card--email" shadow="never">
                    <template #header>
                        <div class="zqy-login-method__card-header">
                            <div class="zqy-login-method__card-name">
                                <el-icon><Message /></el-icon>
                                邮箱登录
                            </div>
                            <div class="zqy-login-method__header-switches">
                                <span>启用</span>
                                <el-switch v-model="form.emailEnabled" :loading="saving" @change="persistConfig" />
                            </div>
                        </div>
                    </template>
                    <div class="zqy-login-method__card-body">
                        <div class="zqy-login-method__option-row" :class="{ 'is-disabled': !form.emailEnabled }">
                            <span>首页登录方式</span>
                            <el-switch
                                :model-value="form.defaultLoginMethod === 'EMAIL'"
                                :disabled="!form.emailEnabled || saving"
                                @change="changeDefaultLoginMethod('EMAIL', $event)"
                            />
                        </div>
                        <div class="zqy-login-method__option-row" :class="{ 'is-disabled': !form.emailEnabled }">
                            <span>自动注册</span>
                            <el-switch
                                v-model="form.emailRegisterEnabled"
                                :disabled="!form.emailEnabled || saving"
                                @change="persistConfig"
                            />
                        </div>
                        <button
                            class="zqy-login-method__config-row"
                            :class="{ 'is-disabled': !form.emailEnabled }"
                            type="button"
                            :disabled="!form.emailEnabled || saving"
                            @click="openEmailConfig"
                        >
                            <span>邮箱配置</span>
                            <el-icon><Setting /></el-icon>
                        </button>
                    </div>
                </el-card>
            </div>
        </LoadingPage>

        <el-dialog
            v-model="phoneConfigVisible"
            class="login-method-config-dialog"
            title="短信配置"
            width="520px"
        >
            <el-form class="zqy-login-method__dialog-form zqy-login-method__dialog-form--single" label-position="top">
                <el-form-item label="类型">
                    <el-select v-model="form.config.phoneConfig.provider">
                        <el-option label="阿里云短信" value="ALIYUN" />
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
                        placeholder="留空表示不修改"
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
                <el-form-item label="测试手机号">
                    <el-input v-model="phoneTestReceiver" placeholder="请输入接收手机号" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="login-method-config-dialog__footer">
                    <div class="login-method-config-dialog__test-actions">
                        <el-button
                            class="login-method-config-dialog__test-button"
                            type="primary"
                            :loading="testingChannel === 'PHONE'"
                            @click="testConfig('PHONE')"
                        >
                            测试发送
                        </el-button>
                        <el-button
                            v-if="phoneTestErrorMessage"
                            class="login-method-config-dialog__test-error"
                            link
                            type="danger"
                            @click="showPhoneTestError"
                        >
                            连接失败
                        </el-button>
                    </div>
                    <div class="login-method-config-dialog__footer-actions">
                        <el-button @click="phoneConfigVisible = false">关闭</el-button>
                        <el-button type="primary" :loading="saving" @click="saveConfigWithMessage">保存配置</el-button>
                    </div>
                </div>
            </template>
        </el-dialog>

        <el-dialog
            v-model="emailConfigVisible"
            class="login-method-config-dialog"
            title="邮箱配置"
            width="520px"
        >
            <el-form class="zqy-login-method__dialog-form zqy-login-method__dialog-form--single" label-position="top">
                <el-form-item label="类型">
                    <el-select v-model="form.config.emailConfig.provider" @change="applyEmailProvider">
                        <el-option label="QQ邮箱" value="QQ" />
                    </el-select>
                </el-form-item>
                <el-form-item label="SMTP服务器">
                    <el-input v-model="form.config.emailConfig.host" />
                </el-form-item>
                <el-form-item label="SMTP端口">
                    <el-input-number v-model="form.config.emailConfig.port" :min="1" :max="65535" :controls="false" />
                </el-form-item>
                <el-form-item label="用户名">
                    <el-input v-model="form.config.emailConfig.username" />
                </el-form-item>
                <el-form-item label="授权码">
                    <el-input
                        v-model="form.config.emailConfig.password"
                        type="password"
                        show-password
                        placeholder="留空表示不修改"
                    />
                </el-form-item>
                <div class="zqy-login-method__switch-row">
                    <el-form-item label="SSL">
                        <el-switch v-model="form.config.emailConfig.ssl" />
                    </el-form-item>
                    <el-form-item label="STARTTLS">
                        <el-switch v-model="form.config.emailConfig.startTls" />
                    </el-form-item>
                </div>
                <el-form-item label="测试邮箱">
                    <el-input v-model="emailTestReceiver" placeholder="请输入接收邮箱" />
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="login-method-config-dialog__footer">
                    <div class="login-method-config-dialog__test-actions">
                        <el-button
                            class="login-method-config-dialog__test-button"
                            type="primary"
                            :loading="testingChannel === 'EMAIL'"
                            @click="testConfig('EMAIL')"
                        >
                            测试发送
                        </el-button>
                        <el-button
                            v-if="emailTestErrorMessage"
                            class="login-method-config-dialog__test-error"
                            link
                            type="danger"
                            @click="showEmailTestError"
                        >
                            连接失败
                        </el-button>
                    </div>
                    <div class="login-method-config-dialog__footer-actions">
                        <el-button @click="emailConfigVisible = false">关闭</el-button>
                        <el-button type="primary" :loading="saving" @click="saveConfigWithMessage">保存配置</el-button>
                    </div>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Iphone, Message, Setting, User } from '@element-plus/icons-vue'

import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import LoadingPage from '@/app/components/loading/index.vue'
import {
    GetLoginMethodConfig,
    TestLoginConfig,
    UpdateLoginMethodConfig,
    type LoginChannel,
    type LoginMethodConfig,
    type LoginMethodType
} from '@/app/management/login-method/api'
import { BreadCrumbList } from './login-method.config'

const breadCrumbList = reactive(BreadCrumbList)
const loading = ref(false)
const saving = ref(false)
const networkError = ref(false)
const phoneConfigVisible = ref(false)
const emailConfigVisible = ref(false)
const phoneTestReceiver = ref('')
const emailTestReceiver = ref('')
const testingChannel = ref<LoginChannel | ''>('')
const phoneTestErrorMessage = ref('')
const emailTestErrorMessage = ref('')
const phonePattern = /^1[3-9]\d{9}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const form = reactive<LoginMethodConfig>(createDefaultForm())

function createDefaultForm(): LoginMethodConfig {
    return {
        defaultLoginMethod: 'ACCOUNT',
        accountEnabled: true,
        accountPasswordEnabled: true,
        accountPhonePasswordEnabled: false,
        accountEmailPasswordEnabled: false,
        emailEnabled: false,
        emailRegisterEnabled: false,
        phoneEnabled: false,
        phoneRegisterEnabled: false,
        config: {
            emailConfig: {
                provider: 'QQ',
                host: 'smtp.qq.com',
                port: 465,
                fromName: '至轻云',
                ssl: true,
                startTls: false,
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
    config.accountEnabled = true
    config.accountPasswordEnabled = true
    config.defaultLoginMethod = resolveDefaultLoginMethod(config.defaultLoginMethod, config)
    return config
}

function applyConfig(data: Partial<LoginMethodConfig>) {
    Object.assign(form, normalizeConfig(data))
}

function cloneForm(): LoginMethodConfig {
    form.accountEnabled = true
    form.accountPasswordEnabled = true
    form.defaultLoginMethod = resolveDefaultLoginMethod(form.defaultLoginMethod, form)
    const data = JSON.parse(JSON.stringify(form))
    data.config.emailConfig.fromAddress =
        data.config.emailConfig.provider === 'QQ'
            ? data.config.emailConfig.username || ''
            : data.config.emailConfig.fromAddress || data.config.emailConfig.username || ''
    data.config.emailConfig.fromName = data.config.emailConfig.fromName || '至轻云'
    data.config.emailConfig.subject = data.config.emailConfig.subject || '至轻云登录验证码'
    return data
}

function initData(tableLoading?: boolean) {
    loading.value = tableLoading ? false : true
    networkError.value = false
    GetLoginMethodConfig()
        .then((res: any) => {
            applyConfig(res.data || {})
            loading.value = false
            networkError.value = false
        })
        .catch(() => {
            loading.value = false
            networkError.value = true
        })
}

function persistConfig() {
    saveConfig(true).catch(() => undefined)
}

function saveConfigWithMessage() {
    saveConfig(false).catch(() => undefined)
}

function saveConfig(silent: boolean): Promise<void> {
    if (!validateConfig()) {
        initData(true)
        return Promise.reject(new Error('invalid login method config'))
    }
    form.defaultLoginMethod = resolveDefaultLoginMethod(form.defaultLoginMethod, form)
    saving.value = true
    return UpdateLoginMethodConfig(cloneForm())
        .then((res: any) => {
            if (!silent) {
                ElMessage.success(res.msg)
            }
            applyConfig(res.data || form)
        })
        .catch((error) => {
            initData(true)
            return Promise.reject(error)
        })
        .finally(() => {
            saving.value = false
        })
}

function validateConfig() {
    form.accountEnabled = true
    form.accountPasswordEnabled = true
    return true
}

function changeDefaultLoginMethod(loginMethod: LoginMethodType, enabled: boolean | string | number) {
    if (!enabled) {
        ElMessage.warning('首页登录方式必须保留一个')
        return
    }
    form.defaultLoginMethod = loginMethod
    persistConfig()
}

function resolveDefaultLoginMethod(defaultLoginMethod: LoginMethodType | undefined, config: LoginMethodConfig): LoginMethodType {
    if (defaultLoginMethod === 'ACCOUNT' && config.accountEnabled) return 'ACCOUNT'
    if (defaultLoginMethod === 'PHONE' && config.phoneEnabled) return 'PHONE'
    if (defaultLoginMethod === 'EMAIL' && config.emailEnabled) return 'EMAIL'
    if (config.accountEnabled) return 'ACCOUNT'
    if (config.phoneEnabled) return 'PHONE'
    return 'EMAIL'
}

function applyEmailProvider() {
    if (form.config.emailConfig.provider !== 'QQ') {
        return
    }
    form.config.emailConfig.host = 'smtp.qq.com'
    form.config.emailConfig.port = 465
    form.config.emailConfig.ssl = true
    form.config.emailConfig.startTls = false
    form.config.emailConfig.fromAddress = form.config.emailConfig.username || ''
}

function openPhoneConfig() {
    phoneTestErrorMessage.value = ''
    phoneConfigVisible.value = true
}

function openEmailConfig() {
    emailTestErrorMessage.value = ''
    emailConfigVisible.value = true
}

async function testConfig(channel: LoginChannel) {
    const receiver = (channel === 'PHONE' ? phoneTestReceiver.value : emailTestReceiver.value).trim()
    if (!receiver) {
        ElMessage.warning(channel === 'PHONE' ? '请输入测试手机号' : '请输入测试邮箱')
        return
    }
    if (channel === 'PHONE' && !phonePattern.test(receiver)) {
        ElMessage.warning('请输入正确的测试手机号')
        return
    }
    if (channel === 'EMAIL' && !emailPattern.test(receiver)) {
        ElMessage.warning('请输入正确的测试邮箱')
        return
    }
    testingChannel.value = channel
    if (channel === 'PHONE') {
        phoneTestErrorMessage.value = ''
    }
    if (channel === 'EMAIL') {
        emailTestErrorMessage.value = ''
    }
    try {
        await saveConfig(true)
        await TestLoginConfig({
            channel,
            receiver
        })
        ElMessage.success('测试发送成功')
    } catch (error) {
        if (channel === 'PHONE') {
            phoneTestErrorMessage.value = getErrorMessage(error)
        }
        if (channel === 'EMAIL') {
            emailTestErrorMessage.value = getErrorMessage(error)
        }
        // The shared HTTP handler has already shown the specific error message.
    } finally {
        testingChannel.value = ''
    }
}

function getErrorMessage(error: unknown) {
    if (error instanceof Error && error.message) {
        return error.message
    }
    if (typeof error === 'string' && error) {
        return error
    }
    if (error && typeof error === 'object') {
        const data = error as { msg?: string; message?: string }
        return data.msg || data.message || JSON.stringify(error)
    }
    return '测试发送失败'
}

function showPhoneTestError() {
    ElMessageBox.alert(phoneTestErrorMessage.value, '连接失败', {
        confirmButtonText: '确定',
        customClass: 'login-method-test-error-dialog'
    })
}

function showEmailTestError() {
    ElMessageBox.alert(emailTestErrorMessage.value, '连接失败', {
        confirmButtonText: '确定',
        customClass: 'login-method-test-error-dialog'
    })
}

onMounted(() => {
    initData()
})
</script>

<style lang="scss">
.zqy-login-method {
    .zqy-login-method__content {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 16px;
        padding: 16px 20px 20px;
        box-sizing: border-box;
    }

    .zqy-login-method__card {
        --login-method-accent: #{getCssVar('color-primary')};

        position: relative;
        min-width: 0;
        overflow: hidden;
        border: 1px solid getCssVar('border-color');
        border-radius: 8px;
        box-shadow: 0 8px 20px rgb(31 35 41 / 8%);

        &::before {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 3px;
            background: var(--login-method-accent);
            content: '';
        }

        .el-card__header {
            display: flex;
            align-items: center;
            min-height: 59px;
            padding: 16px 16px 14px;
            border-bottom-color: getCssVar('border-color', 'lighter');
            background: getCssVar('fill-color', 'blank');
            box-sizing: border-box;
        }

        .el-card__body {
            padding: 12px 16px 16px;
        }
    }

    .zqy-login-method__fixed-row {
        color: getCssVar('text-color', 'primary');
        font-weight: 500;
    }

    .zqy-login-method__card-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        width: 100%;
    }

    .zqy-login-method__card-name {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
        color: getCssVar('text-color', 'primary');
        font-size: 14px;
        font-weight: 600;
    }

    .zqy-login-method__header-switches {
        display: grid;
        grid-template-columns: auto auto;
        align-items: center;
        gap: 8px;
        color: getCssVar('text-color', 'regular');
        font-size: 12px;
        white-space: nowrap;
    }

    .zqy-login-method__header-placeholder {
        visibility: hidden;
        pointer-events: none;
    }

    .zqy-login-method__card-body {
        display: flex;
        flex-direction: column;
        gap: 10px;
    }

    .zqy-login-method__option-row,
    .zqy-login-method__config-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        min-height: 36px;
        padding: 0;
        color: inherit;
        font-size: 13px;

        &.is-disabled {
            color: getCssVar('text-color', 'disabled');
        }
    }

    .zqy-login-method__config-row {
        width: 100%;
        border: 0;
        background: transparent;
        cursor: pointer;

        .el-icon {
            color: getCssVar('color-primary');
            font-size: 16px;
        }

        &:disabled {
            cursor: not-allowed;

            .el-icon {
                color: getCssVar('text-color', 'disabled');
            }
        }
    }

    .zqy-login-method__dialog-form {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 0 16px;

        .el-form-item {
            margin-bottom: 14px;
        }

        .el-select,
        .el-input-number {
            width: 100%;
        }
    }

    .zqy-login-method__dialog-form--single {
        display: block;
    }
}

.login-method-config-dialog {
    --login-method-modal-x-padding: 20px;
    --login-method-modal-border-color: #ebeef5;

    .el-dialog__header {
        position: relative;
        min-height: 46px;
        padding: 9px var(--login-method-modal-x-padding) 8px !important;
        margin-right: 0;
        border-bottom: none;

        &::after {
            position: absolute;
            right: 0;
            bottom: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--login-method-modal-border-color);
        }
    }

    .el-dialog__title {
        display: block;
        line-height: 28px;
        text-align: center;
    }

    .el-dialog__headerbtn {
        top: 0;
        width: 42px;
        height: 46px;
    }

    .el-dialog__body {
        padding: 0 !important;
    }

    .el-dialog__footer {
        position: relative;
        min-height: 56px;
        padding: 12px var(--login-method-modal-x-padding);
        border-top: none;

        &::before {
            position: absolute;
            top: 0;
            right: 0;
            left: 0;
            height: 1px;
            content: '';
            background-color: var(--login-method-modal-border-color);
        }
    }

    .login-method-config-dialog__footer {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 16px;
        width: 100%;
    }

    .login-method-config-dialog__footer-actions {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 12px;
    }

    .login-method-config-dialog__test-actions {
        display: flex;
        align-items: center;
        gap: 8px;
    }

    .login-method-config-dialog__test-button {
        margin-left: 0;
    }

    .login-method-config-dialog__test-error {
        margin-left: 0;
        padding: 0;
    }

    .zqy-login-method__dialog-form {
        padding: 14px var(--login-method-modal-x-padding) 4px;
        box-sizing: border-box;

        .el-form-item {
            margin-bottom: 20px;
        }

        .el-form-item__label {
            width: 100%;
            padding: 0;
            margin-bottom: 4px;
            line-height: 16px;
            color: getCssVar('text-color', 'regular');
        }

        .el-form-item__content,
        .el-input,
        .el-input-number,
        .el-select {
            width: 100%;
        }

        .el-input__wrapper {
            border-radius: 2px;
        }
    }

    .zqy-login-method__switch-row {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 16px;

        .el-form-item {
            margin-bottom: 20px;
        }
    }
}

@media (max-width: 1200px) {
    .zqy-login-method {
        .zqy-login-method__content {
            grid-template-columns: 1fr;
        }
    }
}
</style>
