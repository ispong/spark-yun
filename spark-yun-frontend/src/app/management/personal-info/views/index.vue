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
                        <el-form-item label="账号">
                            <div class="personal-info__account-row">
                                <el-input v-model="personalModel.account" placeholder="--" readonly>
                                    <template #suffix>
                                        <el-tooltip content="复制账号" placement="top">
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
                        <el-form-item label="用户名" prop="username">
                            <el-input
                                v-model="personalModel.username"
                                maxlength="100"
                                placeholder="请输入"
                                show-word-limit
                            />
                        </el-form-item>
                        <el-form-item label="手机号" prop="phone">
                            <el-input
                                v-model="personalModel.phone"
                                maxlength="100"
                                placeholder=""
                                show-word-limit
                                disabled
                            />
                        </el-form-item>
                        <el-form-item label="邮箱" prop="email">
                            <el-input
                                v-model="personalModel.email"
                                maxlength="100"
                                placeholder=""
                                show-word-limit
                                disabled
                            />
                        </el-form-item>
                        <el-form-item label="备注">
                            <el-input
                                v-model="personalModel.remark"
                                show-word-limit
                                type="textarea"
                                maxlength="200"
                                :autosize="{ minRows: 4, maxRows: 4 }"
                                placeholder="请输入"
                            />
                        </el-form-item>
                    </el-form>

                    <div class="personal-info__actions">
                        <el-button type="primary" @click="handleSave">保存</el-button>
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
                        <el-form-item label="新密码" prop="newPassword">
                            <el-input
                                v-model="passwordModel.newPassword"
                                type="password"
                                show-password
                                placeholder="请输入新密码"
                            />
                        </el-form-item>
                        <el-form-item label="确认新密码" prop="confirmPassword">
                            <el-input
                                v-model="passwordModel.confirmPassword"
                                type="password"
                                show-password
                                placeholder="请再次输入新密码"
                            />
                        </el-form-item>
                        <template v-if="shouldInputOldPassword">
                            <div class="personal-info__password-section">
                                <div class="personal-info__section-title">原密码验证</div>
                                <el-form-item label="原密码" prop="oldPassword">
                                    <el-input
                                        v-model="passwordModel.oldPassword"
                                        type="password"
                                        show-password
                                        placeholder="请输入原密码"
                                    />
                                </el-form-item>
                                <div class="personal-info__section-actions">
                                    <el-button
                                        type="primary"
                                        :loading="updatePasswordLoading === 'OLD_PASSWORD'"
                                        @click="handleChangePassword('OLD_PASSWORD')"
                                    >
                                        确认修改
                                    </el-button>
                                </div>
                            </div>
                            <div class="personal-info__password-section">
              <div class="personal-info__section-title">短信验证</div>
                                <el-form-item label="当前手机号">
                                    <el-input v-model="personalModel.phone" placeholder="" disabled />
                                </el-form-item>
                                <el-form-item label="验证码" prop="phoneCode">
                                    <div class="personal-info__code-row">
                                        <el-input
                                            v-model="passwordModel.phoneCode"
                                            maxlength="6"
                                            placeholder="请输入验证码"
                                            clearable
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
                                                    : '获取验证码'
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
                                        确认修改
                                    </el-button>
                                </div>
                            </div>
                            <div class="personal-info__password-section">
                                <div class="personal-info__section-title">邮箱验证</div>
                                <el-form-item label="当前邮箱">
                                    <el-input v-model="personalModel.email" placeholder="" disabled />
                                </el-form-item>
                                <el-form-item label="验证码" prop="emailCode">
                                    <div class="personal-info__code-row">
                                        <el-input
                                            v-model="passwordModel.emailCode"
                                            maxlength="6"
                                            placeholder="请输入验证码"
                                            clearable
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
                                                    : '获取验证码'
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
                                        确认修改
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
                        <el-form-item label="当前手机号">
                            <el-input v-model="personalModel.phone" placeholder="" disabled />
                        </el-form-item>
                        <el-form-item label="新手机号" prop="phone">
                            <el-input
                                v-model="phoneModel.phone"
                                maxlength="11"
                                placeholder="请输入新手机号"
                                clearable
                            />
                        </el-form-item>
                        <el-form-item label="验证码" prop="code">
                            <div class="personal-info__code-row">
                                <el-input
                                    v-model="phoneModel.code"
                                    maxlength="6"
                                    placeholder="请输入验证码"
                                    clearable
                                />
                                <el-button
                                    class="personal-info__code-button"
                                    :loading="sendCodeLoading"
                                    :disabled="sendCodeLoading || !!sendCodeCountdown"
                                    @click="handleSendPhoneCode"
                                >
                                    {{ sendCodeCountdown ? `${sendCodeCountdown}s` : '获取验证码' }}
                                </el-button>
                            </div>
                        </el-form-item>
                    </el-form>

                    <div class="personal-info__actions">
                        <el-button type="primary" :loading="updatePhoneLoading" @click="handleUpdatePhone">
                            确认修改
                        </el-button>
                    </div>
                </div>
            </template>

            <template v-else>
                <div class="personal-info__panel">
                    <el-form
                        ref="emailFormRef"
                        class="personal-info__form"
                        :model="emailModel"
                        label-position="top"
                        :rules="emailRule"
                    >
                        <el-form-item label="当前邮箱">
                            <el-input v-model="personalModel.email" placeholder="" disabled />
                        </el-form-item>
                        <el-form-item label="新邮箱" prop="email">
                            <el-input
                                v-model="emailModel.email"
                                maxlength="100"
                                placeholder="请输入新邮箱"
                                clearable
                            />
                        </el-form-item>
                        <el-form-item label="验证码" prop="code">
                            <div class="personal-info__code-row">
                                <el-input
                                    v-model="emailModel.code"
                                    maxlength="6"
                                    placeholder="请输入验证码"
                                    clearable
                                />
                                <el-button
                                    class="personal-info__code-button"
                                    :loading="sendEmailCodeLoading"
                                    :disabled="sendEmailCodeLoading || !!sendEmailCodeCountdown"
                                    @click="handleSendEmailCode"
                                >
                                    {{ sendEmailCodeCountdown ? `${sendEmailCodeCountdown}s` : '获取验证码' }}
                                </el-button>
                            </div>
                        </el-form-item>
                    </el-form>

                    <div class="personal-info__actions">
                        <el-button type="primary" :loading="updateEmailLoading" @click="handleUpdateEmail">
                            确认修改
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
import { EmailModel, PasswordModel, PersonalModel, PhoneModel } from './personal-info'
import Breadcrumb from '@/app/layout/bread-crumb/index.vue'
import { useAuthStore } from '@/app/store/useAuth'
import {
    SendUpdateEmailCode,
    SendUpdatePasswordCode,
    SendUpdatePhoneCode,
    UpdateMyEmail,
    UpdateMyPassword,
    UpdateMyPhone,
    UpdateUserInfo
} from '../api'
import { ElForm, ElMessage, FormRules } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'

const authStore = useAuthStore()
const route = useRoute()

const { userInfo } = authStore

const elFormRef = ref<InstanceType<typeof ElForm> | null>()
const passwordFormRef = ref<InstanceType<typeof ElForm> | null>()
const phoneFormRef = ref<InstanceType<typeof ElForm> | null>()
const emailFormRef = ref<InstanceType<typeof ElForm> | null>()
const sendCodeLoading = ref(false)
const sendCodeCountdown = ref(0)
const updatePhoneLoading = ref(false)
const sendEmailCodeLoading = ref(false)
const sendEmailCodeCountdown = ref(0)
const updateEmailLoading = ref(false)
const sendPasswordCodeLoading = ref<'PHONE' | 'EMAIL' | ''>('')
const sendPasswordPhoneCountdown = ref(0)
const sendPasswordEmailCountdown = ref(0)
const updatePasswordLoading = ref<'OLD_PASSWORD' | 'PHONE' | 'EMAIL' | ''>('')
let sendCodeTimer: number | undefined
let sendEmailCodeTimer: number | undefined
let sendPasswordPhoneTimer: number | undefined
let sendPasswordEmailTimer: number | undefined

type PersonalInfoMenu = 'basic-info' | 'change-password' | 'change-phone' | 'change-email'

function resolveMenu(menu: unknown): PersonalInfoMenu {
    return menu === 'change-password' || menu === 'change-phone' || menu === 'change-email' ? menu : 'basic-info'
}

const activeMenu = ref<PersonalInfoMenu>(resolveMenu(route.query.tab))

const personalRule: FormRules = {
    username: [
        {
            required: true,
            message: '请输入用户名',
            trigger: ['blur', 'change']
        }
    ]
}

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
        ElMessage.success('复制成功')
    } catch {
        ElMessage.error('复制失败')
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

const shouldInputOldPassword = computed(() => authStore.userInfo.hasPassword !== false)

const passwordSubmitText = computed(() => (shouldInputOldPassword.value ? '确认修改' : '确认设置'))

const validateOldPassword = (_: any, value: string, callback: (error?: Error) => void) => {
    if (shouldInputOldPassword.value && passwordModel.verifyType === 'OLD_PASSWORD' && !value) {
        callback(new Error('请输入原密码'))
        return
    }

    callback()
}

const validateConfirmPassword = (_: any, value: string, callback: (error?: Error) => void) => {
    if (!value) {
        callback(new Error('请再次输入新密码'))
        return
    }

    if (value !== passwordModel.newPassword) {
        callback(new Error('两次输入的新密码不一致'))
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
        callback(new Error('请输入验证码'))
        return
    }
    if (!/^\d{6}$/.test(value)) {
        callback(new Error('请输入6位数字验证码'))
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
        callback(new Error('请输入验证码'))
        return
    }
    if (!/^\d{6}$/.test(value)) {
        callback(new Error('请输入6位数字验证码'))
        return
    }
    callback()
}

const passwordRule: FormRules = {
    oldPassword: [
        {
            required: true,
            message: '请输入原密码',
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
            message: '请输入验证码',
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
            message: '请输入验证码',
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
            message: '请输入新密码',
            trigger: ['blur', 'change']
        }
    ],
    confirmPassword: [
        {
            required: true,
            message: '请再次输入新密码',
            trigger: ['blur', 'change']
        },
        {
            validator: validateConfirmPassword,
            trigger: ['blur', 'change']
        }
    ]
}

const validatePhone = (_: any, value: string, callback: (error?: Error) => void) => {
    if (!value) {
        callback(new Error('请输入新手机号'))
        return
    }

    if (!/^1[3-9]\d{9}$/.test(value)) {
        callback(new Error('请输入正确的手机号'))
        return
    }

    if (value === personalModel.phone) {
        callback(new Error('新手机号不能与当前手机号相同'))
        return
    }

    callback()
}

const phoneRule: FormRules = {
    phone: [
        {
            validator: validatePhone,
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

const validateEmail = (_: any, value: string, callback: (error?: Error) => void) => {
    if (!value) {
        callback(new Error('请输入新邮箱'))
        return
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        callback(new Error('请输入正确的邮箱格式'))
        return
    }

    if (value === personalModel.email) {
        callback(new Error('新邮箱不能与当前邮箱相同'))
        return
    }

    callback()
}

const emailRule: FormRules = {
    email: [
        {
            validator: validateEmail,
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

const pageTitle = computed(() => {
    if (activeMenu.value === 'basic-info') {
        return '基础信息'
    }
    if (activeMenu.value === 'change-phone') {
        return '修改手机'
    }
    if (activeMenu.value === 'change-email') {
        return '修改邮箱'
    }

    return shouldInputOldPassword.value ? '修改密码' : '设置密码'
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
            UpdateMyPassword(updateParams).then(async (res: any) => {
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
            }).finally(() => {
                updatePasswordLoading.value = ''
            })
        }
    })
}

async function handleSendPasswordCode(channel: 'PHONE' | 'EMAIL') {
    const countdown = channel === 'PHONE' ? sendPasswordPhoneCountdown.value : sendPasswordEmailCountdown.value
    if (sendPasswordCodeLoading.value || countdown) return
    if (channel === 'PHONE' && !personalModel.phone) {
        ElMessage.warning('当前账号未绑定手机号')
        return
    }
    if (channel === 'EMAIL' && !personalModel.email) {
        ElMessage.warning('当前账号未绑定邮箱')
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
    } finally {
        updateEmailLoading.value = false
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
