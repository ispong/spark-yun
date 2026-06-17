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
                            <div class="zqy-login__code-row">
                                <div class="zqy-login__code-digits">
                                    <input
                                        v-for="(_, index) in codeDigits"
                                        :key="index"
                                        :ref="(element) => setCodeDigitRef(element, index)"
                                        v-model="codeDigits[index]"
                                        class="zqy-login__code-digit"
                                        type="text"
                                        inputmode="numeric"
                                        autocomplete="one-time-code"
                                        maxlength="1"
                                        @input="handleCodeDigitInput($event, index)"
                                        @keydown="handleCodeDigitKeydown($event, index)"
                                        @paste="handleCodeDigitPaste($event, index)"
                                        @focus="handleCodeDigitFocus($event)"
                                    />
                                </div>
                                <el-button
                                    class="zqy-login__send-code"
                                    :disabled="!!sendCodeCountdown"
                                    :loading="sendCodeLoading"
                                    @click="handleSendCode"
                                >
                                    {{ sendCodeCountdown ? `${sendCodeCountdown}s` : '获取验证码' }}
                                </el-button>
                            </div>
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
                        <div class="zqy-login__action-left">
                            <el-dropdown v-if="showCodeLogin" trigger="click" popper-class="login-method-dropdown">
                                <span class="zqy-login__action-text">登录方式</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item
                                            v-if="openLoginConfig.accountEnabled && activeLoginMethod !== 'ACCOUNT'"
                                            @click="switchLoginMethod('ACCOUNT')"
                                        >
                                            账号登录
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="openLoginConfig.phoneEnabled && activeLoginMethod !== 'PHONE'"
                                            @click="switchLoginMethod('PHONE')"
                                        >
                                            手机登录
                                        </el-dropdown-item>
                                        <el-dropdown-item
                                            v-if="openLoginConfig.emailEnabled && activeLoginMethod !== 'EMAIL'"
                                            @click="switchLoginMethod('EMAIL')"
                                        >
                                            邮箱登录
                                        </el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>

                        <div class="zqy-login__action-right">
                            <el-dropdown
                                v-if="oauthLoaded && oauthUrlList.length"
                                trigger="click"
                                popper-class="login-method-dropdown"
                            >
                                <span class="zqy-login__action-text">免密登录</span>
                                <template #dropdown>
                                    <el-dropdown-menu>
                                        <el-dropdown-item
                                            v-for="item in oauthUrlList"
                                            :key="item.invokeUrl"
                                            @click="handleRedirect(item)"
                                        >
                                            {{ item.name }}
                                        </el-dropdown-item>
                                    </el-dropdown-menu>
                                </template>
                            </el-dropdown>
                        </div>
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
    accountPasswordEnabled: boolean
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
const codeDigits = reactive(['', '', '', '', '', ''])
const codeDigitRefs = ref<Array<HTMLInputElement | null>>([])
const phonePattern = /^1[3-9]\d{9}$/
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const codeLoginChannel = ref<LoginChannel>('PHONE')
const openLoginConfig = reactive<OpenLoginMethodConfig>({
    defaultLoginMethod: 'ACCOUNT',
    accountEnabled: true,
    accountPasswordEnabled: true,
    accountPhonePasswordEnabled: true,
    accountEmailPasswordEnabled: true,
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
        },
        {
            validator: validateCodeReceiver,
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

const accountLoginEnabled = computed(() => {
    return (
        openLoginConfig.accountEnabled !== false &&
        (
            openLoginConfig.accountPasswordEnabled ||
            openLoginConfig.accountPhonePasswordEnabled ||
            openLoginConfig.accountEmailPasswordEnabled
        )
    )
})

function validateCodeReceiver(_: unknown, value: string, callback: (error?: Error) => void) {
    const receiver = (value || '').trim()
    if (!receiver) {
        callback()
        return
    }
    if (codeLoginChannel.value === 'PHONE' && !phonePattern.test(receiver)) {
        callback(new Error('请输入正确的手机号'))
        return
    }
    if (codeLoginChannel.value === 'EMAIL' && !emailPattern.test(receiver)) {
        callback(new Error('请输入正确的邮箱'))
        return
    }
    callback()
}
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
    const options: string[] = []
    if (openLoginConfig.accountPasswordEnabled) {
        options.push('账号')
    }
    if (openLoginConfig.accountPhonePasswordEnabled) {
        options.push('手机号')
    }
    if (openLoginConfig.accountEmailPasswordEnabled) {
        options.push('邮箱')
    }
    if (options.length === 3) {
        return '请输入账号/手机号/邮箱'
    }
    if (options.length) {
        return `请输入${options.join('/')}`
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
        Number(accountLoginEnabled.value) +
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
    resetCodeDigits()
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
    if (loginMethod === 'ACCOUNT') return accountLoginEnabled.value
    if (loginMethod === 'PHONE') return openLoginConfig.phoneEnabled
    return openLoginConfig.emailEnabled
}

function setCodeDigitRef(element: Element | null, index: number) {
    codeDigitRefs.value[index] = element as HTMLInputElement | null
}

function handleCodeDigitInput(event: Event, index: number) {
    const input = event.target as HTMLInputElement
    const value = input.value.replace(/\D/g, '')
    if (!value) {
        codeDigits[index] = ''
        input.value = ''
        syncCodeDigits()
        return
    }

    if (value.length > 1) {
        applyCodeDigits(value, index)
        return
    }

    codeDigits[index] = value
    input.value = value
    syncCodeDigits()
    focusCodeDigit(index + 1)
}

function handleCodeDigitKeydown(event: KeyboardEvent, index: number) {
    if (event.key === 'Backspace' && !codeDigits[index] && index > 0) {
        codeDigits[index - 1] = ''
        syncCodeDigits()
        focusCodeDigit(index - 1)
        event.preventDefault()
        return
    }
    if (event.key === 'ArrowLeft') {
        focusCodeDigit(index - 1)
        event.preventDefault()
        return
    }
    if (event.key === 'ArrowRight') {
        focusCodeDigit(index + 1)
        event.preventDefault()
    }
}

function handleCodeDigitPaste(event: ClipboardEvent, index: number) {
    event.preventDefault()
    applyCodeDigits(event.clipboardData?.getData('text') || '', index)
}

function handleCodeDigitFocus(event: FocusEvent) {
    const input = event.target as HTMLInputElement
    input.select()
}

function applyCodeDigits(value: string, startIndex = 0) {
    const digits = value.replace(/\D/g, '').slice(0, codeDigits.length - startIndex)
    if (!digits) return
    digits.split('').forEach((digit, offset) => {
        codeDigits[startIndex + offset] = digit
    })
    syncCodeDigits()
    focusCodeDigit(startIndex + digits.length)
}

function resetCodeDigits() {
    codeDigits.forEach((_, index) => {
        codeDigits[index] = ''
    })
    syncCodeDigits()
}

function syncCodeDigits() {
    codeLoginModel.code = codeDigits.join('')
    if (codeLoginModel.code.length === codeDigits.length) {
        codeFormRef.value?.validateField('code').catch(() => {})
    }
}

function focusCodeDigit(index: number) {
    if (index < 0 || index >= codeDigits.length) return
    nextTick(() => {
        codeDigitRefs.value[index]?.focus()
    })
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
        grid-template-columns: minmax(420px, 1fr) minmax(340px, 380px);
        gap: 64px;
        padding: 104px 72px 56px 96px;
        box-sizing: border-box;
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
        width: min(100%, 800px);
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
        min-width: 0;
        padding: 46px 30px;
        box-sizing: border-box;
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
        justify-content: space-between;
        align-items: center;
        font-size: getCssVar('font-size', 'extra-small');
        width: 100%;
    }

    .zqy-login__action-left,
    .zqy-login__action-right {
        min-width: 72px;
        display: flex;
        align-items: center;
    }

    .zqy-login__action-left {
        justify-content: flex-start;
    }

    .zqy-login__action-right {
        justify-content: flex-end;
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
        justify-content: flex-start;
    }

    .zqy-login__code-form {
        .zqy-login__code-row {
            width: 100%;
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .zqy-login__code-digits {
            flex: 1;
            min-width: 0;
            display: grid;
            grid-template-columns: repeat(6, minmax(0, 1fr));
            gap: 6px;
        }

        .zqy-login__code-digit {
            width: 100%;
            height: 40px;
            padding: 0;
            border: 1px solid var(--el-border-color);
            border-radius: 6px;
            box-sizing: border-box;
            text-align: center;
            color: getCssVar('text-color', 'primary');
            font-size: 16px;
            line-height: 40px;
            outline: none;
            background-color: #ffffff;
            transition: all 0.3s ease;

            &:hover,
            &:focus {
                border-color: getCssVar('color', 'primary');
                box-shadow: 0 0 0 2px rgba(var(--el-color-primary-rgb), 0.1);
            }
        }

        .zqy-login__send-code {
            width: 88px;
            height: 40px;
            padding: 0;
            flex: 0 0 88px;
            font-size: 12px;
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

    @media (max-width: 1180px) {
        .zqy-login__body {
            grid-template-columns: minmax(320px, 1fr) minmax(320px, 360px);
            gap: 40px;
            padding: 104px 40px 48px;
        }

        .zqy-login__preview {
            max-width: 640px;
        }
    }

    @media (max-width: 900px) {
        overflow: auto;

        .zqy-login__header {
            position: static;
            padding: 28px 24px 0;
        }

        .zqy-login__brand {
            width: 148px;
        }

        .zqy-login__body {
            min-height: auto;
            display: flex;
            justify-content: center;
            padding: 36px 24px 40px;
        }

        .zqy-login__visual {
            display: none;
        }

        .zqy-login__panel {
            width: 100%;
            max-width: 380px;
            justify-content: center;
        }
    }

    @media (max-width: 430px) {
        .zqy-login__body {
            padding: 28px 16px 32px;
        }

        .zqy-login__card {
            padding: 34px 20px;
        }

        .zqy-login__title {
            line-height: 64px;
        }

        .zqy-login__code-form {
            .zqy-login__code-row {
                gap: 8px;
            }

            .zqy-login__code-digits {
                gap: 4px;
            }

            .zqy-login__code-digit {
                height: 38px;
                line-height: 38px;
            }

            .zqy-login__send-code {
                width: 78px;
                flex-basis: 78px;
            }
        }
    }
}

.login-method-dropdown {
    .el-dropdown-menu {
        padding: 4px 0;
    }

    .el-dropdown-menu__item {
        height: 26px;
        line-height: 26px;
        font-family: Avenir, Helvetica, Arial, sans-serif;
        font-size: getCssVar('font-size', 'extra-small');
    }
}
</style>
