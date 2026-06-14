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
                    <h1 class="zqy-login__title">用户登录</h1>
                    <el-form
                        ref="elFormRef"
                        class="zqy-login__form"
                        :model="loginModel"
                        :rules="loginRule"
                        label-position="top"
                        @keyup.enter="handleLogin"
                    >

                        <el-form-item prop="account">
                            <el-input
                                v-model="loginModel.account"
                                prefix-icon="User"
                                class="zqy-login__input"
                                autocomplete="username"
                                placeholder="请输入账号/邮箱/手机号"
                            />
                        </el-form-item>

                        <el-form-item prop="passwd">
                            <el-input
                                v-model="loginModel.passwd"
                                prefix-icon="Lock"
                                class="zqy-login__input"
                                type="password"
                                show-password
                                autocomplete="current-password"
                                placeholder="请输入密码"
                            />
                        </el-form-item>

                    </el-form>

                    <el-button
                        class="zqy-login__button"
                        type="primary"
                        :loading="btnLoading"
                        :disabled="btnLoading"
                        @click="handleLogin"
                    >
                        确认登录
                    </el-button>

                    <div v-if="oauthLoaded && oauthUrlList.length" class="zqy-login__oauth">
                        <el-popover trigger="click" placement="bottom" :width="180">
                            <template #reference>
                                <span class="zqy-login__oauth-text">免密登录</span>
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
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import { LoginUserInfo, OauthUrlList } from '@/app/api'
import logoIcon from '@/app/assets/imgs/logo-a.png'
import logoURL from '@/app/assets/imgs/logo-view.png'
import logo from '@/app/assets/imgs/logo1.svg'
import { useAuthStore } from '@/app/store/useAuth'
import { getVipLicenseEnabled } from '@/app/utils/vip-license'

interface LoginModel {
    account: string
    passwd: string
}

interface OauthUrl {
    name: string
    invokeUrl: string
}

const router = useRouter()
const authStore = useAuthStore()
const elFormRef = ref<FormInstance>()
const btnLoading = ref(false)
const oauthLoaded = ref(false)
const oauthUrlList = ref<OauthUrl[]>([])

const loginModel = reactive<LoginModel>({
    account: '',
    passwd: ''
})

const loginRule: FormRules<LoginModel> = {
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

async function submitLogin() {
    if (btnLoading.value) {
        return
    }

    btnLoading.value = true
    try {
        const res: any = await LoginUserInfo({ ...loginModel })
        authStore.applyAuthResponse(res.data)
        await getVipLicenseEnabled(true)
        ElMessage.success(res.msg)
        await nextTick()
        router.push(res.data.defaultArea === 'platform' ? '/platform' : '/workspace')
    } finally {
        btnLoading.value = false
    }
}

async function handleLogin() {
    if (btnLoading.value) {
        return
    }

    const isValid = await elFormRef.value?.validate().catch(() => false)
    if (isValid) {
        await submitLogin()
    }
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

function handleRedirect(item: OauthUrl) {
    location.href = item.invokeUrl
}

onMounted(() => {
    queryOauthList()
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

    .zqy-login__oauth {
        height: 50px;
        display: flex;
        justify-content: flex-end;
        align-items: center;
        font-size: getCssVar('font-size', 'extra-small');
        width: 100%;
    }

    .zqy-login__oauth-text {
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
                box-shadow: 0 0 0 2px rgba(80, 107, 254, 0.1);
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
