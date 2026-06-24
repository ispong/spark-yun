<template>
    <Breadcrumb :bread-crumb-list="breadCrumbList" />
    <div class="personal-info">
        <div class="personal-info__content">
            <template v-if="activeMenu === 'basic-info'">
                <div class="personal-info__panel">
                    <el-form
                        ref="elFormRef"
                        class="personal-info__form"
                        :model="personalModel"
                        label-position="top"
                        :rules="personalRule"
                    >
                        <el-form-item :label="t('personal.account')">
                            <div class="personal-info__account-row">
                                <el-input v-model="personalModel.account" placeholder="--" readonly>
                                    <template #suffix>
                                        <el-tooltip :content="t('personal.copyAccount')" placement="top">
                                            <el-button
                                                class="personal-info__copy-button"
                                                :icon="CopyDocument"
                                                :disabled="!personalModel.account"
                                                link
                                                @click="copyAccount"
                                            />
                                        </el-tooltip>
                                    </template>
                                </el-input>
                            </div>
                        </el-form-item>
                        <el-form-item :label="t('personal.username')" prop="username">
                            <el-input
                                v-model="personalModel.username"
                                maxlength="100"
                                :placeholder="t('common.pleaseInput')"
                                show-word-limit
                            />
                        </el-form-item>
                        <el-form-item :label="t('personal.phone')" prop="phone">
                            <el-input
                                v-model="personalModel.phone"
                                maxlength="100"
                                placeholder=""
                                show-word-limit
                                disabled
                            />
                        </el-form-item>
                        <el-form-item :label="t('personal.email')" prop="email">
                            <el-input
                                v-model="personalModel.email"
                                maxlength="100"
                                placeholder=""
                                show-word-limit
                                disabled
                            />
                        </el-form-item>
                        <el-form-item :label="t('personal.remark')">
                            <el-input
                                v-model="personalModel.remark"
                                show-word-limit
                                type="textarea"
                                maxlength="200"
                                :autosize="{ minRows: 4, maxRows: 4 }"
                                :placeholder="t('common.pleaseInput')"
                            />
                        </el-form-item>
                    </el-form>

                    <div class="personal-info__actions">
                        <el-button type="primary" @click="handleSave">{{ t('common.save') }}</el-button>
                    </div>
                </div>
            </template>

            <template v-else-if="activeMenu === 'change-password'">
                <div class="personal-info__panel">
                    <el-form
                        ref="passwordFormRef"
                        class="personal-info__form"
                        :model="passwordModel"
                        label-position="top"
                        :rules="passwordRule"
                    >
                        <el-form-item :label="t('personal.newPassword')" prop="newPassword">
                            <el-input
                                v-model="passwordModel.newPassword"
                                type="password"
                                show-password
                                :placeholder="t('personal.inputNewPassword')"
                            />
                        </el-form-item>
                        <el-form-item :label="t('personal.confirmNewPassword')" prop="confirmPassword">
                            <el-input
                                v-model="passwordModel.confirmPassword"
                                type="password"
                                show-password
                                :placeholder="t('personal.inputConfirmNewPassword')"
                            />
                        </el-form-item>
                        <template v-if="shouldInputOldPassword">
                            <div class="personal-info__password-section">
                                <div class="personal-info__section-title">
                                    {{ t('personal.oldPasswordVerification') }}
                                </div>
                                <el-form-item :label="t('personal.oldPassword')" prop="oldPassword">
                                    <el-input
                                        v-model="passwordModel.oldPassword"
                                        type="password"
                                        show-password
                                        :placeholder="t('personal.inputOldPassword')"
                                    />
                                </el-form-item>
                                <div class="personal-info__section-actions">
                                    <el-button
                                        type="primary"
                                        :loading="updatePasswordLoading === 'OLD_PASSWORD'"
                                        @click="handleChangePassword('OLD_PASSWORD')"
                                    >
                                        {{ t('personal.confirmChange') }}
                                    </el-button>
                                </div>
                            </div>
                            <div class="personal-info__password-section">
                                <div class="personal-info__section-title">
                                    {{ t('personal.smsVerification') }}
                                </div>
                                <el-form-item :label="t('personal.currentPhone')">
                                    <el-input v-model="personalModel.phone" placeholder="" disabled />
                                </el-form-item>
                                <el-form-item :label="t('personal.verificationCode')" prop="phoneCode">
                                    <div class="personal-info__code-row">
                                        <el-input
                                            v-model="passwordModel.phoneCode"
                                            maxlength="6"
                                            :placeholder="t('personal.inputCode')"
                                            clearable
                                            @input="handlePasswordCodeInput('PHONE')"
                                        />
                                        <el-button
                                            class="personal-info__code-button"
                                            :loading="sendPasswordCodeLoading === 'PHONE'"
                                            :disabled="
                                                sendPasswordCodeLoading === 'PHONE' || !!sendPasswordPhoneCountdown
                                            "
                                            @click="handleSendPasswordCode('PHONE')"
                                        >
                                            {{
                                                sendPasswordPhoneCountdown
                                                    ? `${sendPasswordPhoneCountdown}s`
                                                    : t('login.getCode')
                                            }}
                                        </el-button>
                                    </div>
                                </el-form-item>
                                <div class="personal-info__section-actions">
                                    <el-button
                                        type="primary"
                                        :loading="updatePasswordLoading === 'PHONE'"
                                        @click="handleChangePassword('PHONE')"
                                    >
                                        {{ t('personal.confirmChange') }}
                                    </el-button>
                                </div>
                            </div>
                            <div class="personal-info__password-section">
                                <div class="personal-info__section-title">
                                    {{ t('personal.emailVerification') }}
                                </div>
                                <el-form-item :label="t('personal.currentEmail')">
                                    <el-input v-model="personalModel.email" placeholder="" disabled />
                                </el-form-item>
                                <el-form-item :label="t('personal.verificationCode')" prop="emailCode">
                                    <div class="personal-info__code-row">
                                        <el-input
                                            v-model="passwordModel.emailCode"
                                            maxlength="6"
                                            :placeholder="t('personal.inputCode')"
                                            clearable
                                            @input="handlePasswordCodeInput('EMAIL')"
                                        />
                                        <el-button
                                            class="personal-info__code-button"
                                            :loading="sendPasswordCodeLoading === 'EMAIL'"
                                            :disabled="
                                                sendPasswordCodeLoading === 'EMAIL' || !!sendPasswordEmailCountdown
                                            "
                                            @click="handleSendPasswordCode('EMAIL')"
                                        >
                                            {{
                                                sendPasswordEmailCountdown
                                                    ? `${sendPasswordEmailCountdown}s`
                                                    : t('login.getCode')
                                            }}
                                        </el-button>
                                    </div>
                                </el-form-item>
                                <div class="personal-info__section-actions">
                                    <el-button
                                        type="primary"
                                        :loading="updatePasswordLoading === 'EMAIL'"
                                        @click="handleChangePassword('EMAIL')"
                                    >
                                        {{ t('personal.confirmChange') }}
                                    </el-button>
                                </div>
                            </div>
                        </template>
                    </el-form>

                    <div v-if="!shouldInputOldPassword" class="personal-info__actions">
                        <el-button
                            type="primary"
                            :loading="updatePasswordLoading === 'OLD_PASSWORD'"
                            @click="handleChangePassword('OLD_PASSWORD')"
                        >
                            {{ passwordSubmitText }}
                        </el-button>
                    </div>
                </div>
            </template>

            <template v-else-if="activeMenu === 'change-phone'">
                <div class="personal-info__panel">
                    <el-form
                        ref="phoneFormRef"
                        class="personal-info__form"
                        :model="phoneModel"
                        label-position="top"
                        :rules="phoneRule"
                    >
                        <el-form-item :label="t('personal.currentPhone')">
                            <el-input v-model="personalModel.phone" placeholder="" disabled />
                        </el-form-item>
                        <el-form-item :label="t('personal.newPhone')" prop="phone">
                            <el-input
                                v-model="phoneModel.phone"
                                maxlength="11"
                                :placeholder="t('personal.inputNewPhone')"
                                clearable
                            />
                        </el-form-item>
                        <el-form-item :label="t('personal.verificationCode')" prop="code">
                            <div class="personal-info__code-row">
                                <el-input
                                    v-model="phoneModel.code"
                                    maxlength="6"
                                    :placeholder="t('personal.inputCode')"
                                    clearable
                                    @input="handlePhoneCodeInput"
                                />
                                <el-button
                                    class="personal-info__code-button"
                                    :loading="sendCodeLoading"
                                    :disabled="sendCodeLoading || !!sendCodeCountdown"
                                    @click="handleSendPhoneCode"
                                >
                                    {{ sendCodeCountdown ? `${sendCodeCountdown}s` : t('login.getCode') }}
                                </el-button>
                            </div>
                        </el-form-item>
                    </el-form>

                    <div class="personal-info__actions">
                        <el-button type="primary" :loading="updatePhoneLoading" @click="handleUpdatePhone">
                            {{ t('personal.confirmChange') }}
                        </el-button>
                    </div>
                </div>
            </template>

            <template v-else-if="activeMenu === 'change-email'">
                <div class="personal-info__panel">
                    <el-form
                        ref="emailFormRef"
                        class="personal-info__form"
                        :model="emailModel"
                        label-position="top"
                        :rules="emailRule"
                    >
                        <el-form-item :label="t('personal.currentEmail')">
                            <el-input v-model="personalModel.email" placeholder="" disabled />
                        </el-form-item>
                        <el-form-item :label="t('personal.newEmail')" prop="email">
                            <el-input
                                v-model="emailModel.email"
                                maxlength="100"
                                :placeholder="t('personal.inputNewEmail')"
                                clearable
                            />
                        </el-form-item>
                        <el-form-item :label="t('personal.verificationCode')" prop="code">
                            <div class="personal-info__code-row">
                                <el-input
                                    v-model="emailModel.code"
                                    maxlength="6"
                                    :placeholder="t('personal.inputCode')"
                                    clearable
                                    @input="handleEmailCodeInput"
                                />
                                <el-button
                                    class="personal-info__code-button"
                                    :loading="sendEmailCodeLoading"
                                    :disabled="sendEmailCodeLoading || !!sendEmailCodeCountdown"
                                    @click="handleSendEmailCode"
                                >
                                    {{ sendEmailCodeCountdown ? `${sendEmailCodeCountdown}s` : t('login.getCode') }}
                                </el-button>
                            </div>
                        </el-form-item>
                    </el-form>

                    <div class="personal-info__actions">
                        <el-button type="primary" :loading="updateEmailLoading" @click="handleUpdateEmail">
                            {{ t('personal.confirmChange') }}
                        </el-button>
                    </div>
                </div>
            </template>

            <template v-else>
                <div class="personal-info__panel">
                    <el-form
                        ref="languageFormRef"
                        class="personal-info__form"
                        :model="languageModel"
                        label-position="top"
                        :rules="languageRule"
                    >
                        <el-form-item :label="t('personal.currentLanguage')" prop="locale">
                            <el-select v-model="languageModel.locale" class="personal-info__language-select">
                                <el-option
                                    v-for="localeOption in localeOptions"
                                    :key="localeOption.value"
                                    :label="localeOption.label"
                                    :value="localeOption.value"
                                />
                            </el-select>
                        </el-form-item>
                    </el-form>

                    <div class="personal-info__actions">
                        <el-button type="primary" :loading="updateLanguageLoading" @click="handleUpdateLanguage">
                            {{ t('common.save') }}
                        </el-button>
                    </div>
                </div>
            </template>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { EmailModel, LanguageModel, PasswordModel, PersonalModel, PhoneModel } from './personal-info'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import { useAuthStore } from '@/app/store/useAuth'
import { useLocaleStore } from '@/app/store/useLocale'
import {
    SendUpdateEmailCode,
    SendUpdatePasswordCode,
    SendUpdatePhoneCode,
    UpdateMyEmail,
    UpdateMyLocale,
    UpdateMyPassword,
    UpdateMyPhone,
    UpdateUserInfo
} from '../api'
import { ElForm, ElMessage, FormRules } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import { SUPPORTED_LOCALES, type AppLocale } from '@/app/i18n/locales'

const authStore = useAuthStore()
const localeStore = useLocaleStore()
const route = useRoute()
const { t } = useI18n()

const { userInfo } = authStore

const elFormRef = ref<InstanceType<typeof ElForm> | null>()
const passwordFormRef = ref<InstanceType<typeof ElForm> | null>()
const phoneFormRef = ref<InstanceType<typeof ElForm> | null>()
const emailFormRef = ref<InstanceType<typeof ElForm> | null>()
const languageFormRef = ref<InstanceType<typeof ElForm> | null>()
const sendCodeLoading = ref(false)
const sendCodeCountdown = ref(0)
const updatePhoneLoading = ref(false)
const sendEmailCodeLoading = ref(false)
const sendEmailCodeCountdown = ref(0)
const updateEmailLoading = ref(false)
const updateLanguageLoading = ref(false)
const sendPasswordCodeLoading = ref<'PHONE' | 'EMAIL' | ''>('')
const sendPasswordPhoneCountdown = ref(0)
const sendPasswordEmailCountdown = ref(0)
const updatePasswordLoading = ref<'OLD_PASSWORD' | 'PHONE' | 'EMAIL' | ''>('')
let sendCodeTimer: number | undefined
let sendEmailCodeTimer: number | undefined
let sendPasswordPhoneTimer: number | undefined
let sendPasswordEmailTimer: number | undefined

type PersonalInfoMenu = 'basic-info' | 'change-password' | 'change-phone' | 'change-email' | 'change-language'

function resolveMenu(menu: unknown): PersonalInfoMenu {
    return menu === 'change-password' || menu === 'change-phone' || menu === 'change-email' || menu === 'change-language'
        ? menu
        : 'basic-info'
}

const activeMenu = ref<PersonalInfoMenu>(resolveMenu(route.query.tab))

const personalRule = computed<FormRules>(() => ({
    username: [
        {
            required: true,
            message: t('validation.inputUsername'),
            trigger: ['blur', 'change']
        }
    ]
}))

const personalModel = reactive<PersonalModel>({
    username: userInfo.username || '',
    account: userInfo.account || '',
    phone: userInfo.phone || '',
    email: userInfo.email || '',
    remark: userInfo.remark || ''
})

async function copyAccount() {
    if (!personalModel.account) {
        return
    }

    try {
        await navigator.clipboard.writeText(personalModel.account)
        ElMessage.success(t('common.copySuccess'))
    } catch {
        ElMessage.error(t('common.copyFail'))
    }
}

const passwordModel = reactive<PasswordModel>({
    oldPassword: '',
    verifyType: 'OLD_PASSWORD',
    code: '',
    phoneCode: '',
    emailCode: '',
    newPassword: '',
    confirmPassword: ''
})

function resetPasswordForm() {
    Object.assign(passwordModel, {
        oldPassword: '',
        verifyType: 'OLD_PASSWORD',
        code: '',
        phoneCode: '',
        emailCode: '',
        newPassword: '',
        confirmPassword: ''
    })
    passwordFormRef.value?.clearValidate()
}

const phoneModel = reactive<PhoneModel>({
    phone: '',
    code: ''
})

const emailModel = reactive<EmailModel>({
    email: '',
    code: ''
})

const languageModel = reactive<LanguageModel>({
    locale: localeStore.locale
})

const localeOptions = computed(() =>
    SUPPORTED_LOCALES.map((locale) => ({
        value: locale,
        label: t(`app.locale.${locale}`)
    }))
)

const shouldInputOldPassword = computed(() => authStore.userInfo.hasPassword !== false)

const passwordSubmitText = computed(() =>
    shouldInputOldPassword.value ? t('personal.confirmChange') : t('personal.confirmSet')
)

const validateOldPassword = (_: any, value: string, callback: (error?: Error) => void) => {
    if (shouldInputOldPassword.value && passwordModel.verifyType === 'OLD_PASSWORD' && !value) {
        callback(new Error(t('personal.inputOldPassword')))
        return
    }

    callback()
}

const validateConfirmPassword = (_: any, value: string, callback: (error?: Error) => void) => {
    if (!value) {
        callback(new Error(t('personal.inputConfirmNewPassword')))
        return
    }

    if (value !== passwordModel.newPassword) {
        callback(new Error(t('personal.passwordMismatch')))
        return
    }

    callback()
}

const validatePasswordPhoneCode = (_: any, value: string, callback: (error?: Error) => void) => {
    if (!shouldInputOldPassword.value || passwordModel.verifyType !== 'PHONE') {
        callback()
        return
    }
    if (!value) {
        callback(new Error(t('personal.inputCode')))
        return
    }
    if (!/^\d{6}$/.test(value)) {
        callback(new Error(t('personal.inputSixDigitCode')))
        return
    }
    callback()
}

const validatePasswordEmailCode = (_: any, value: string, callback: (error?: Error) => void) => {
    if (!shouldInputOldPassword.value || passwordModel.verifyType !== 'EMAIL') {
        callback()
        return
    }
    if (!value) {
        callback(new Error(t('personal.inputCode')))
        return
    }
    if (!/^\d{6}$/.test(value)) {
        callback(new Error(t('personal.inputSixDigitCode')))
        return
    }
    callback()
}

const passwordRule = computed<FormRules>(() => ({
    oldPassword: [
        {
            required: true,
            message: t('personal.inputOldPassword'),
            trigger: ['blur', 'change']
        },
        {
            validator: validateOldPassword,
            trigger: ['blur', 'change']
        }
    ],
    phoneCode: [
        {
            required: true,
            message: t('personal.inputCode'),
            trigger: ['blur', 'change']
        },
        {
            validator: validatePasswordPhoneCode,
            trigger: ['blur', 'change']
        }
    ],
    emailCode: [
        {
            required: true,
            message: t('personal.inputCode'),
            trigger: ['blur', 'change']
        },
        {
            validator: validatePasswordEmailCode,
            trigger: ['blur', 'change']
        }
    ],
    newPassword: [
        {
            required: true,
            message: t('personal.inputNewPassword'),
            trigger: ['blur', 'change']
        }
    ],
    confirmPassword: [
        {
            required: true,
            message: t('personal.inputConfirmNewPassword'),
            trigger: ['blur', 'change']
        },
        {
            validator: validateConfirmPassword,
            trigger: ['blur', 'change']
        }
    ]
}))

const validatePhone = (_: any, value: string, callback: (error?: Error) => void) => {
    if (!value) {
        callback(new Error(t('personal.inputNewPhone')))
        return
    }

    if (!/^1[3-9]\d{9}$/.test(value)) {
        callback(new Error(t('personal.invalidPhone')))
        return
    }

    if (value === personalModel.phone) {
        callback(new Error(t('personal.samePhone')))
        return
    }

    callback()
}

const phoneRule = computed<FormRules>(() => ({
    phone: [
        {
            validator: validatePhone,
            trigger: ['blur', 'change']
        }
    ],
    code: [
        {
            required: true,
            message: t('personal.inputCode'),
            trigger: ['blur', 'change']
        },
        {
            pattern: /^\d{6}$/,
            message: t('personal.inputSixDigitCode'),
            trigger: ['blur', 'change']
        }
    ]
}))

const validateEmail = (_: any, value: string, callback: (error?: Error) => void) => {
    if (!value) {
        callback(new Error(t('personal.inputNewEmail')))
        return
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        callback(new Error(t('personal.invalidEmail')))
        return
    }

    if (value === personalModel.email) {
        callback(new Error(t('personal.sameEmail')))
        return
    }

    callback()
}

const emailRule = computed<FormRules>(() => ({
    email: [
        {
            validator: validateEmail,
            trigger: ['blur', 'change']
        }
    ],
    code: [
        {
            required: true,
            message: t('personal.inputCode'),
            trigger: ['blur', 'change']
        },
        {
            pattern: /^\d{6}$/,
            message: t('personal.inputSixDigitCode'),
            trigger: ['blur', 'change']
        }
    ]
}))

const languageRule: FormRules = {
    locale: [
        {
            required: true,
            message: t('validation.selectLanguage'),
            trigger: ['blur', 'change']
        }
    ]
}

function normalizeCodeInput(value: string | number | undefined): string {
    return String(value ?? '').replace(/\D/g, '').slice(0, 6)
}

function handlePhoneCodeInput(value: string | number) {
    phoneModel.code = normalizeCodeInput(value)
    if (phoneModel.code.length === 6) {
        handleUpdatePhone()
    }
}

function handleEmailCodeInput(value: string | number) {
    emailModel.code = normalizeCodeInput(value)
    if (emailModel.code.length === 6) {
        handleUpdateEmail()
    }
}

function handlePasswordCodeInput(channel: 'PHONE' | 'EMAIL') {
    if (channel === 'PHONE') {
        passwordModel.phoneCode = normalizeCodeInput(passwordModel.phoneCode)
        if (passwordModel.phoneCode.length === 6) {
            handleChangePassword('PHONE')
        }
        return
    }
    passwordModel.emailCode = normalizeCodeInput(passwordModel.emailCode)
    if (passwordModel.emailCode.length === 6) {
        handleChangePassword('EMAIL')
    }
}

const pageTitle = computed(() => {
    if (activeMenu.value === 'basic-info') {
        return t('menu.basic-info')
    }
    if (activeMenu.value === 'change-phone') {
        return t('menu.change-phone')
    }
    if (activeMenu.value === 'change-email') {
        return t('menu.change-email')
    }
    if (activeMenu.value === 'change-language') {
        return t('personal.changeLanguage')
    }

    return shouldInputOldPassword.value ? t('menu.change-password') : t('personal.setPassword')
})

const breadCrumbList = computed(() => [
    {
        name: pageTitle.value,
        code: 'personal-info'
    }
])

watch(
    () => route.query.tab,
    (tab) => {
        activeMenu.value = resolveMenu(tab)
    }
)

const handleSave = function () {
    elFormRef.value?.validate((valid) => {
        if (valid) {
            const updateParams = {
                username: personalModel.username,
                remark: personalModel.remark
            }

            UpdateUserInfo(updateParams).then((res: any) => {
                authStore.setUserInfo(Object.assign(userInfo, updateParams))
                ElMessage.success(res.msg)
            })
        }
    })
}

const handleChangePassword = function (verifyType: 'OLD_PASSWORD' | 'PHONE' | 'EMAIL') {
    if (updatePasswordLoading.value) return
    passwordModel.verifyType = verifyType
    passwordModel.code = verifyType === 'PHONE' ? passwordModel.phoneCode : verifyType === 'EMAIL' ? passwordModel.emailCode : ''
    passwordFormRef.value?.validate((valid) => {
        if (valid) {
            const updateParams = {
                oldPassword:
                    shouldInputOldPassword.value && verifyType === 'OLD_PASSWORD'
                        ? passwordModel.oldPassword
                        : undefined,
                verifyType: shouldInputOldPassword.value ? verifyType : undefined,
                code:
                    shouldInputOldPassword.value && verifyType !== 'OLD_PASSWORD'
                        ? passwordModel.code
                        : undefined,
                newPassword: passwordModel.newPassword,
                confirmPassword: passwordModel.confirmPassword
            }
            updatePasswordLoading.value = verifyType
            UpdateMyPassword(updateParams)
                .then(async (res: any) => {
                    ElMessage.success(res.msg)
                    resetPasswordForm()
                    authStore.setUserInfo({
                        ...authStore.userInfo,
                        hasPassword: true
                    })
                    await nextTick()
                    resetPasswordForm()
                    clearSendPasswordCountdown('PHONE')
                    clearSendPasswordCountdown('EMAIL')
                })
                .catch(() => {
                    if (verifyType === 'PHONE') {
                        passwordModel.phoneCode = ''
                        passwordModel.code = ''
                        passwordFormRef.value?.clearValidate('phoneCode')
                    }
                    if (verifyType === 'EMAIL') {
                        passwordModel.emailCode = ''
                        passwordModel.code = ''
                        passwordFormRef.value?.clearValidate('emailCode')
                    }
                })
                .finally(() => {
                    updatePasswordLoading.value = ''
                })
        }
    })
}

async function handleSendPasswordCode(channel: 'PHONE' | 'EMAIL') {
    const countdown = channel === 'PHONE' ? sendPasswordPhoneCountdown.value : sendPasswordEmailCountdown.value
    if (sendPasswordCodeLoading.value || countdown) return
    if (channel === 'PHONE' && !personalModel.phone) {
        ElMessage.warning(t('personal.accountPhoneNotBound'))
        return
    }
    if (channel === 'EMAIL' && !personalModel.email) {
        ElMessage.warning(t('personal.accountEmailNotBound'))
        return
    }

    sendPasswordCodeLoading.value = channel
    try {
        const res = await SendUpdatePasswordCode({
            channel
        })
        ElMessage.success(res.msg)
        startSendPasswordCountdown(channel)
    } finally {
        sendPasswordCodeLoading.value = ''
    }
}

async function handleSendPhoneCode() {
    if (sendCodeLoading.value || sendCodeCountdown.value) return
    const valid = await phoneFormRef.value?.validateField('phone').then(() => true).catch(() => false)
    if (!valid) return

    sendCodeLoading.value = true
    try {
        const res = await SendUpdatePhoneCode({
            phone: phoneModel.phone
        })
        ElMessage.success(res.msg)
        startSendCodeCountdown()
    } finally {
        sendCodeLoading.value = false
    }
}

async function handleUpdatePhone() {
    if (updatePhoneLoading.value) return
    const valid = await phoneFormRef.value?.validate().catch(() => false)
    if (!valid) return

    updatePhoneLoading.value = true
    try {
        const res = await UpdateMyPhone({
            phone: phoneModel.phone,
            code: phoneModel.code
        })
        personalModel.phone = phoneModel.phone
        authStore.setUserInfo({
            ...authStore.userInfo,
            phone: phoneModel.phone
        })
        phoneFormRef.value?.resetFields()
        clearSendCodeCountdown()
        ElMessage.success(res.msg)
    } catch {
        phoneModel.code = ''
        phoneFormRef.value?.clearValidate('code')
    } finally {
        updatePhoneLoading.value = false
    }
}

async function handleSendEmailCode() {
    if (sendEmailCodeLoading.value || sendEmailCodeCountdown.value) return
    const valid = await emailFormRef.value?.validateField('email').then(() => true).catch(() => false)
    if (!valid) return

    sendEmailCodeLoading.value = true
    try {
        const res = await SendUpdateEmailCode({
            email: emailModel.email
        })
        ElMessage.success(res.msg)
        startSendEmailCodeCountdown()
    } finally {
        sendEmailCodeLoading.value = false
    }
}

async function handleUpdateEmail() {
    if (updateEmailLoading.value) return
    const valid = await emailFormRef.value?.validate().catch(() => false)
    if (!valid) return

    updateEmailLoading.value = true
    try {
        const res = await UpdateMyEmail({
            email: emailModel.email,
            code: emailModel.code
        })
        personalModel.email = emailModel.email
        authStore.setUserInfo({
            ...authStore.userInfo,
            email: emailModel.email
        })
        emailFormRef.value?.resetFields()
        clearSendEmailCodeCountdown()
        ElMessage.success(res.msg)
    } catch {
        emailModel.code = ''
        emailFormRef.value?.clearValidate('code')
    } finally {
        updateEmailLoading.value = false
    }
}

async function handleUpdateLanguage() {
    if (updateLanguageLoading.value) return
    const valid = await languageFormRef.value?.validate().catch(() => false)
    if (!valid) return

    updateLanguageLoading.value = true
    const locale = languageModel.locale as AppLocale
    try {
        const res = await UpdateMyLocale({
            locale
        })
        localeStore.setLocale(locale)
        authStore.setUserInfo({
            ...authStore.userInfo,
            locale
        })
        ElMessage.success(res.msg || t('personal.languageSaved'))
    } finally {
        updateLanguageLoading.value = false
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

function startSendEmailCodeCountdown() {
    clearSendEmailCodeCountdown()
    sendEmailCodeCountdown.value = 60
    sendEmailCodeTimer = window.setInterval(() => {
        sendEmailCodeCountdown.value -= 1
        if (sendEmailCodeCountdown.value <= 0) {
            clearSendEmailCodeCountdown()
        }
    }, 1000)
}

function clearSendEmailCodeCountdown() {
    if (sendEmailCodeTimer) {
        window.clearInterval(sendEmailCodeTimer)
        sendEmailCodeTimer = undefined
    }
    sendEmailCodeCountdown.value = 0
}

function startSendPasswordCountdown(channel: 'PHONE' | 'EMAIL') {
    clearSendPasswordCountdown(channel)
    const countdownRef = channel === 'PHONE' ? sendPasswordPhoneCountdown : sendPasswordEmailCountdown
    countdownRef.value = 60
    const timer = window.setInterval(() => {
        countdownRef.value -= 1
        if (countdownRef.value <= 0) {
            clearSendPasswordCountdown(channel)
        }
    }, 1000)
    if (channel === 'PHONE') {
        sendPasswordPhoneTimer = timer
    } else {
        sendPasswordEmailTimer = timer
    }
}

function clearSendPasswordCountdown(channel: 'PHONE' | 'EMAIL') {
    if (channel === 'PHONE') {
        if (sendPasswordPhoneTimer) {
            window.clearInterval(sendPasswordPhoneTimer)
            sendPasswordPhoneTimer = undefined
        }
        sendPasswordPhoneCountdown.value = 0
        return
    }
    if (sendPasswordEmailTimer) {
        window.clearInterval(sendPasswordEmailTimer)
        sendPasswordEmailTimer = undefined
    }
    sendPasswordEmailCountdown.value = 0
}

onUnmounted(() => {
    clearSendCodeCountdown()
    clearSendEmailCodeCountdown()
    clearSendPasswordCountdown('PHONE')
    clearSendPasswordCountdown('EMAIL')
})
</script>

<style lang="scss" src="./personal-info.scss"></style>
