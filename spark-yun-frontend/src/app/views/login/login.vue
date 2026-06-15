<template>
    <main class="zqy-login">

        <header class="zqy-login__header">
            <img class="zqy-login__brand" :src="logoIcon" alt="至轻云" />
        </header>

        <div class="zqy-login__body">

            <section class="zqy-login__visual" aria-hidden="true">
                <img class="zqy-login__preview" :src="logoURL" alt="" />
            </section>

            <section class="zqy-login__panel" aria-label="用户登录">
                <div class="zqy-login__card">
                    <img class="zqy-login__card-logo" :src="logo" alt="至轻云" />
                    <h1 class="zqy-login__title">{{ loginTitle }}</h1>
                    <el-form
                        v-if="activeLoginMethod === 'ACCOUNT'"
                        ref="elFormRef"
                        class="zqy-login__form"
                        :model="loginModel"
                        :rules="loginRule"
                        @keyup.enter="handleLogin"
                    >

                        <el-form-item prop="account">
                            <el-input
                                v-model="loginModel.account"
                                prefix-icon="User"
                                class="zqy-login__input"
                                :disabled="!accountLoginEnabled"
                                autocomplete="username"
                                :placeholder="accountLoginPlaceholder"
                            />
                        </el-form-item>

                        <el-form-item prop="passwd">
                            <el-input
                                v-model="loginModel.passwd"
                                prefix-icon="Lock"
                                class="zqy-login__input"
                                :disabled="!accountLoginEnabled"
                                type="password"
                                show-password
                                autocomplete="current-password"
                                placeholder="请输入密码"
                            />
                        </el-form-item>

                    </el-form>

                    <el-form
                        v-else
                        ref="codeFormRef"
                        class="zqy-login__form zqy-login__code-form"
                        :model="codeLoginModel"
                        :rules="codeLoginRules"
                        @keyup.enter="handleCodeLogin"
                    >
                        <el-form-item prop="receiver">
                            <el-input
                                v-model="codeLoginModel.receiver"
                                class="zqy-login__input"
                                :placeholder="codeReceiverPlaceholder"
                            />
                        </el-form-item>
                        <el-form-item prop="code">
                            <el-input
                                v-model="codeLoginModel.code"
                                class="zqy-login__input"
                                maxlength="6"
                                placeholder="请输入6位验证码"
                            >
                                <template #append>
                                    <el-button
                                        :disabled="!!sendCodeCountdown"
                                        :loading="sendCodeLoading"
                                        @click="handleSendCode"
                                    >
                                        {{ sendCodeCountdown ? `${sendCodeCountdown}s` : '获取验证码' }}
                                    </el-button>
                                </template>
                            </el-input>
                        </el-form-item>
                    </el-form>

                    <el-button
                        class="zqy-login__button"
                        type="primary"
                        :loading="activeLoginLoading"
                        :disabled="!activeLoginEnabled"
                        @click="handleActiveLogin"
                    >
                        确认登录
                    </el-button>

                    <div v-if="showLoginActions" class="zqy-login__actions">
                        <el-popover v-if="showCodeLogin" trigger="click" placement="bottom" :width="180">
                            <template #reference>
                                <span class="zqy-login__action-text">登录方式</span>
                            </template>
                            <div class="zqy-login__oauth-list">
                                <el-button
                                    v-if="openLoginConfig.accountEnabled && activeLoginMethod !== 'ACCOUNT'"
                                    class="zqy-login__oauth-button"
                                    type="primary"
                                    @click="switchLoginMethod('ACCOUNT')"
                                >
                                    账号登录
                                </el-button>
                                <el-button
                                    v-if="openLoginConfig.phoneEnabled && activeLoginMethod !== 'PHONE'"
                                    class="zqy-login__oauth-button"
                                    type="primary"
                                    @click="switchLoginMethod('PHONE')"
                                >
                                    手机登录
                                </el-button>
                                <el-button
                                    v-if="openLoginConfig.emailEnabled && activeLoginMethod !== 'EMAIL'"
                                    class="zqy-login__oauth-button"
                                    type="primary"
                                    @click="switchLoginMethod('EMAIL')"
                                >
                                    邮箱登录
                                </el-button>
                            </div>
                        </el-popover>

                        <el-popover
                            v-if="oauthLoaded && oauthUrlList.length"
                            trigger="click"
                            placement="bottom"
                            :width="180"
                        >
                            <template #reference>
                                <span class="zqy-login__action-text">免密登录</span>
                            </template>
                            <div class="zqy-login__oauth-list">
                                <el-button
                                    v-for="item in oauthUrlList"
                                    :key="item.invokeUrl"
                                    class="zqy-login__oauth-button"
                                    type="primary"
                                    @click="handleRedirect(item)"
                                >
                                    {{ item.name }}
                                </el-button>
                            </div>
                        </el-popover>
                    </div>

                </div>
            </section>
        </div>
    </main>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { OauthUrlList } from '@/app/api'
import logoIcon from '@/app/assets/imgs/logo-a.png'
import logoURL from '@/app/assets/imgs/logo-view.png'
import logo from '@/app/assets/imgs/logo1.svg'
import {
    GetOpenLoginMethodConfig,
    SendLoginCode,
    VerifyLoginCode,
    type LoginChannel,
    type LoginMethodType
} from '@/app/management/login-method/api'
import { useAuthStore } from '@/app/store/useAuth'
import { getVipLicenseEnabled } from '@/app/utils/vip-license'
import { getUser } from '@/app/type/user/user'
import type { LoginReq } from '@/app/type/models'
import { resolveLoginRoutePath } from './resolve-login-route'

interface OauthUrl {
    name: string
    invokeUrl: string
}

interface OpenLoginMethodConfig {
    defaultLoginMethod: LoginMethodType
    accountEnabled: boolean
    accountPhonePasswordEnabled: boolean
    accountEmailPasswordEnabled: boolean
    emailEnabled: boolean
    emailRegisterEnabled: boolean
    phoneEnabled: boolean
    phoneRegisterEnabled: boolean
}

const router = useRouter()
const authStore = useAuthStore()
const elFormRef = ref<FormInstance>()
const codeFormRef = ref<FormInstance>()
const btnLoading = ref(false)
const codeLoginLoading = ref(false)
const sendCodeLoading = ref(false)
const sendCodeCountdown = ref(0)
const oauthLoaded = ref(false)
const loginMethodLoaded = ref(false)
const oauthUrlList = ref<OauthUrl[]>([])
let sendCodeTimer: number | undefined
const activeLoginMethod = ref<LoginMethodType>('ACCOUNT')

const loginModel = reactive<LoginReq>({
    account: '',
    passwd: ''
})

const codeLoginModel = reactive({
    receiver: '',
    code: ''
})

const codeLoginChannel = ref<LoginChannel>('PHONE')
const openLoginConfig = reactive<OpenLoginMethodConfig>({
    defaultLoginMethod: 'ACCOUNT',
    accountEnabled: true,
    accountPhonePasswordEnabled: false,
    accountEmailPasswordEnabled: false,
    emailEnabled: false,
    emailRegisterEnabled: false,
    phoneEnabled: false,
    phoneRegisterEnabled: false
})

const loginRule: FormRules<LoginReq> = {
    account: [
        {
            required: true,
            message: '请输入账号/邮箱/手机号',
            trigger: ['blur', 'change']
        }
    ],
    passwd: [
        {
            required: true,
            message: '请输入密码',
            trigger: ['blur', 'change']
        }
    ]
}

const codeLoginRules: FormRules = {
    receiver: [
        {
            required: true,
            message: '请输入接收账号',
            trigger: ['blur', 'change']
        }
    ],
    code: [
        {
            required: true,
            message: '请输入验证码',
            trigger: ['blur', 'change']
        },
        {
            pattern: /^\d{6}$/,
            message: '请输入6位数字验证码',
            trigger: ['blur', 'change']
        }
    ]
}

const accountLoginEnabled = computed(() => openLoginConfig.accountEnabled !== false)
const activeLoginEnabled = computed(() => isLoginMethodEnabled(activeLoginMethod.value))
const activeLoginLoading = computed(() => {
    return activeLoginMethod.value === 'ACCOUNT' ? btnLoading.value : codeLoginLoading.value
})
const loginTitle = computed(() => {
    if (activeLoginMethod.value === 'PHONE') return '手机登录'
    if (activeLoginMethod.value === 'EMAIL') return '邮箱登录'
    return '用户登录'
})
const accountLoginPlaceholder = computed(() => {
    if (openLoginConfig.accountPhonePasswordEnabled && openLoginConfig.accountEmailPasswordEnabled) {
        return '请输入账号/邮箱/手机号'
    }
    if (openLoginConfig.accountPhonePasswordEnabled) {
        return '请输入账号/手机号'
    }
    if (openLoginConfig.accountEmailPasswordEnabled) {
        return '请输入账号/邮箱'
    }
    return '请输入账号'
})
const showCodeLogin = computed(() => {
    return loginMethodLoaded.value && enabledLoginMethodCount.value > 1
})
const showLoginActions = computed(() => {
    return showCodeLogin.value || (oauthLoaded.value && !!oauthUrlList.value.length)
})
const enabledLoginMethodCount = computed(() => {
    return (
        Number(openLoginConfig.accountEnabled) +
        Number(openLoginConfig.phoneEnabled) +
        Number(openLoginConfig.emailEnabled)
    )
})
const codeReceiverPlaceholder = computed(() => {
    return codeLoginChannel.value === 'PHONE' ? '请输入手机号' : '请输入邮箱'
})

function handleActiveLogin() {
    if (activeLoginMethod.value === 'ACCOUNT') {
        handleLogin()
        return
    }
    handleCodeLogin()
}

async function handleLogin() {

    if (!accountLoginEnabled.value) {
        ElMessage.warning('账号登录已关闭')
        return
    }

    // 判断loading
    if (btnLoading.value) return

    // 校验表单参数
    const isValid = await elFormRef.value?.validate().catch(() => false)

    // 校验不通过，直接返回
    if (!isValid) return

    // 通过后开始登录，卡住loading
    btnLoading.value = true

    try {

        // 调用登录接口
        const res = await getUser().login({ ...loginModel })
        await completeLogin(res)

    } finally {

        // loading解锁
        btnLoading.value = false
    }
}

async function completeLogin(res: any) {
    authStore.applyAuthResponse(res.data)
    const routePath = resolveLoginRoutePath(res.data)

    if (!res.data.tenantId && ['/platform', '/personal-info'].includes(routePath)) {
        ElMessage.success(res.msg)
        await nextTick()
        await router.push(routePath)
        return
    }

    await getVipLicenseEnabled(true)
    ElMessage.success(res.msg)
    await nextTick()
    await router.push(routePath)
}

function queryOauthList() {
    OauthUrlList()
        .then((res: any) => {
            oauthUrlList.value = res.data
        })
        .catch(() => {
            oauthUrlList.value = []
        })
        .finally(() => {
            oauthLoaded.value = true
        })
}

function queryOpenLoginMethodConfig() {
    GetOpenLoginMethodConfig()
        .then((res: any) => {
            Object.assign(openLoginConfig, {
                ...openLoginConfig,
                ...(res.data || {})
            })
            switchLoginMethod(resolveDefaultLoginMethod(openLoginConfig.defaultLoginMethod), false)
        })
        .catch(() => {})
        .finally(() => {
            loginMethodLoaded.value = true
        })
}

function handleRedirect(item: OauthUrl) {
    location.href = item.invokeUrl
}

function switchLoginMethod(loginMethod: LoginMethodType, resetForm = true) {
    if (!isLoginMethodEnabled(loginMethod)) {
        ElMessage.warning('当前登录方式未开启')
        return
    }
    activeLoginMethod.value = loginMethod
    if (loginMethod !== 'ACCOUNT') {
        codeLoginChannel.value = loginMethod
    }
    if (!resetForm) {
        return
    }
    loginModel.account = ''
    loginModel.passwd = ''
    codeLoginModel.receiver = ''
    codeLoginModel.code = ''
    clearSendCodeCountdown()
    nextTick(() => {
        elFormRef.value?.clearValidate()
        codeFormRef.value?.clearValidate()
    })
}

function resolveDefaultLoginMethod(defaultLoginMethod: LoginMethodType | undefined): LoginMethodType {
    if (isLoginMethodEnabled(defaultLoginMethod)) return defaultLoginMethod
    if (isLoginMethodEnabled('ACCOUNT')) return 'ACCOUNT'
    if (isLoginMethodEnabled('PHONE')) return 'PHONE'
    return 'EMAIL'
}

function isLoginMethodEnabled(loginMethod: LoginMethodType | undefined) {
    if (!loginMethod) return false
    if (loginMethod === 'ACCOUNT') return openLoginConfig.accountEnabled
    if (loginMethod === 'PHONE') return openLoginConfig.phoneEnabled
    return openLoginConfig.emailEnabled
}

async function handleSendCode() {
    if (sendCodeLoading.value || sendCodeCountdown.value) return
    const valid = await codeFormRef.value?.validateField('receiver').then(() => true).catch(() => false)
    if (!valid) return

    sendCodeLoading.value = true
    try {
        const res = await SendLoginCode({
            channel: codeLoginChannel.value,
            receiver: codeLoginModel.receiver
        })
        ElMessage.success(res.msg)
        startSendCodeCountdown()
    } finally {
        sendCodeLoading.value = false
    }
}

async function handleCodeLogin() {
    if (codeLoginLoading.value) return
    const valid = await codeFormRef.value?.validate().catch(() => false)
    if (!valid) return

    codeLoginLoading.value = true
    try {
        const res = await VerifyLoginCode({
            channel: codeLoginChannel.value,
            receiver: codeLoginModel.receiver,
            code: codeLoginModel.code
        })
        await completeLogin(res)
    } finally {
        codeLoginLoading.value = false
    }
}

function startSendCodeCountdown() {
    clearSendCodeCountdown()
    sendCodeCountdown.value = 60
    sendCodeTimer = window.setInterval(() => {
        sendCodeCountdown.value -= 1
        if (sendCodeCountdown.value <= 0) {
            clearSendCodeCountdown()
        }
    }, 1000)
}

function clearSendCodeCountdown() {
    if (sendCodeTimer) {
        window.clearInterval(sendCodeTimer)
        sendCodeTimer = undefined
    }
    sendCodeCountdown.value = 0
}

onMounted(() => {
    queryOauthList()
    queryOpenLoginMethodConfig()
})

onBeforeUnmount(() => {
    clearSendCodeCountdown()
})
</script>

<style lang="scss">
.zqy-login {
    width: 100vw;
    min-height: 100vh;
    position: relative;
    overflow: hidden;
    background-color: #ffffff;

    .zqy-login__header {
        position: absolute;
        left: 44px;
        top: 36px;
        z-index: 10;
    }

    .zqy-login__brand {
        width: 170px;
        height: auto;
        display: block;
    }

    .zqy-login__body {
        min-height: 100vh;
        display: grid;
        grid-template-columns: minmax(0, 1fr) 380px;
        gap: 96px;
        padding: 0 72px 0 96px;
        align-items: center;
        max-width: 1480px;
        margin: 0 auto;
        width: 100%;
    }

    .zqy-login__visual {
        display: flex;
        align-items: center;
        justify-content: center;
        min-width: 0;
    }

    .zqy-login__preview {
        width: min(78%, 800px);
        max-width: 800px;
        height: auto;
    }

    .zqy-login__panel {
        display: flex;
        width: 100%;
        justify-content: flex-end;
    }

    .zqy-login__card {
        width: 100%;
        padding: 46px 30px;
        border-radius: 8px;
        box-shadow: 0 0 10px var(--el-border-color);
        display: flex;
        flex-direction: column;
        align-items: center;
        background-color: #ffffff;
        transition: box-shadow 0.3s ease;

        &:hover {
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
        }
    }

    .zqy-login__card-logo {
        width: 90px;
        height: auto;
    }

    .zqy-login__actions {
        height: 50px;
        display: flex;
        justify-content: flex-end;
        gap: 16px;
        align-items: center;
        font-size: getCssVar('font-size', 'extra-small');
        width: 100%;
    }

    .zqy-login__action-text {
        color: getCssVar('color', 'primary');
        cursor: pointer;

        &:hover {
            text-decoration: underline;
        }
    }

    .zqy-login__oauth-list {
        display: flex;
        flex-direction: column;
        gap: 8px;
        padding: 4px 0;
    }

    .zqy-login__oauth-button {
        width: 100%;
        margin: 0;
        font-size: 12px;
        height: 32px;
    }

    .zqy-login__code-form {
        .el-input-group__append {
            padding: 0;

            .el-button {
                min-width: 92px;
                border-radius: 0;
            }
        }
    }

    .zqy-login__title {
        margin: 0;
        line-height: 80px;
        font-weight: 600;
        color: var(--el-color-black);
        font-size: 24px;
        text-align: center;
    }

    .zqy-login__form {
        width: 100%;

        .el-form-item {
            margin-bottom: 36px;

            &.is-error {
                .zqy-login__input {
                    .el-input__wrapper {
                        border-color: getCssVar('color', 'danger');
                        box-shadow: none;
                    }
                }
            }

            .zqy-login__input {
                height: 40px;

                .el-input__inner {
                    font-size: 12px;
                }
            }

            .el-form-item__error {
                padding: 6px 0;
                font-size: 12px;
            }
        }
    }

    .zqy-login__input {
        transition: all 0.4s linear;

        &:hover {
            .el-input__wrapper {
                border-color: getCssVar('color', 'primary');
            }
        }

        &:focus-within {
            .el-input__wrapper {
                border-color: getCssVar('color', 'primary');
                box-shadow: 0 0 0 2px rgba(var(--el-color-primary-rgb), 0.1);
            }
        }

        .el-input__wrapper {
            padding: 0 12px;
            box-shadow: none;
            border: 1px solid var(--el-border-color);
            border-radius: 6px;
            background-color: #ffffff;
            transition: all 0.4s linear;
            overflow: hidden;
        }

        .el-input__inner {
            border: none;
            box-shadow: none;
            background: transparent;
        }

        &.el-input {
            border: none;
            box-shadow: none;
            background: transparent;
        }
    }

    .zqy-login__button {
        width: 100%;
        background: linear-gradient(90deg, #ff8a3d, #ff4d12);
        border: none;
        font-size: 14px;
        border-radius: 6px;
        height: 44px;
        font-weight: 500;
        transition: all 0.3s ease;

        &:hover {
            background: linear-gradient(90deg, #ff7a28, #f04000);
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(255, 91, 32, 0.28);
        }
    }

}
</style>
