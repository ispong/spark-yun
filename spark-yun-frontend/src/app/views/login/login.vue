<template>
    <main class="zqy-login">
        <img class="zqy-logo-icon" :src="logoIcon" alt="至轻云" />
        <div class="zqy-login__body">

            <div class="zqy-login__playground">
                <img class="zqy-login__logo" :src="logoURL" alt="" />
            </div>

            <div class="zqy-login__main">
                <div class="zqy-login__form-wrap">
                    <img :src="logo" alt="至轻云" />
                    <div class="zqy-login__main__title">用户登录</div>
                    <el-form
                        ref="elFormRef"
                        class="zqy-login__form"
                        :model="loginModel"
                        :rules="loginRule"
                        label-position="top"
                        @keyup="handleKeyup"
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

                    <el-button class="zqy-login__btn" type="primary" :loading="btnLoading" @click="handleLogin">
                        确认登录
                    </el-button>

                    <div v-if="oauthLoaded && oauthUrlList.length" class="oauth-login">
                        <el-popover trigger="click" placement="bottom" :width="180">
                            <template #reference>
                                <span class="oauth-login-text">免密登录</span>
                            </template>
                            <div class="oauth-redirect-url">
                                <el-button
                                    v-for="item in oauthUrlList"
                                    :key="item.invokeUrl"
                                    type="primary"
                                    @click="handleRedirect(item)"
                                >
                                    {{ item.name }}
                                </el-button>
                            </div>
                        </el-popover>
                    </div>

                </div>
            </div>
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

function submitLogin() {
    btnLoading.value = true
    LoginUserInfo({ ...loginModel })
        .then((res: any) => {
            authStore.applyAuthResponse(res.data)
            return getVipLicenseEnabled(true).finally(() => {
                ElMessage.success(res.msg)
                nextTick(() => {
                    router.push(res.data.defaultArea === 'platform' ? '/platform' : '/workspace')
                })
            })
        })
        .finally(() => {
            btnLoading.value = false
        })
}

function handleLogin() {
    elFormRef.value?.validate((isValid) => {
        if (isValid) {
            submitLogin()
        }
    })
}

function handleKeyup(event: KeyboardEvent) {
    if (event.key === 'Enter') {
        handleLogin()
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

    .zqy-logo-icon {
        width: 170px;
        height: auto;
        position: absolute;
        left: 44px;
        top: 36px;
        z-index: 10;
    }

    .zqy-login__body {
        min-height: 100vh;
        display: flex;
        padding: 0 20px;
        align-items: center;
        max-width: 1360px;
        margin: 0 auto;
        width: 100%;
    }

    .zqy-login__playground {
        display: flex;
        align-items: center;
        justify-content: flex-end;
        flex: 6;
        height: 100%;
        margin-right: 56px;

        img {
            width: 76%;
            max-width: 800px;
            height: auto;
        }
    }

    .zqy-login__main {
        display: flex;
        flex: 4;
        flex-direction: column;
        justify-content: center;
        height: 100%;
        min-width: 400px;

        .zqy-login__form-wrap {
            width: 340px;
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

            img {
                width: 90px;
                height: auto;
            }

            .oauth-login {
                height: 50px;
                display: flex;
                justify-content: flex-end;
                align-items: center;
                font-size: getCssVar('font-size', 'extra-small');
                width: 100%;

                .oauth-login-text {
                    color: getCssVar('color', 'primary');
                    cursor: pointer;

                    &:hover {
                        text-decoration: underline;
                    }
                }
            }

            .oauth-redirect-url {
                display: flex;
                flex-direction: column;
                gap: 8px;
                padding: 4px 0;

                .el-button {
                    width: 100%;
                    margin: 0;
                    font-size: 12px;
                    height: 32px;
                }
            }
        }
    }

    .zqy-login__main__title {
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

    .zqy-login__btn {
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
