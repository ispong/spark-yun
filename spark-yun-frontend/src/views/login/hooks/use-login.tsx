import type { ElForm, FormRules } from "element-plus";
import { reactive, readonly, ref, computed } from "vue";
import { useI18n } from 'vue-i18n'
import { useLocaleStore, type LocaleType } from '@/store/useLocale'

import logo from '@/assets/imgs/logo1.svg';
import { OauthUrlList } from "@/services/login.service";

export interface LoginModel {
  account: string,
  passwd: string
}

export function useLogin (callback: ((callback: LoginModel) => Promise<void>)) {
  const { t } = useI18n()
  const localeStore = useLocaleStore()
  const elFormRef = ref<InstanceType<typeof ElForm> | null>()

  const loginRule = computed<FormRules>(() => ({
    account: [
      {
        required: true,
        message: t('login.accountRequired'),
        trigger: [ 'blur', 'change' ]
      }
    ],
    passwd: [
      {
        required: true,
        message: t('login.passwordRequired'),
        trigger: [ 'blur', 'change' ]
      }
    ]
  }))
  let btnLoading = ref(false)
  let oauthUrlList = ref([])
  let loginModel = reactive<LoginModel>({
    account: '',
    passwd: ''
  })

  const handleLogin = function() {
    elFormRef.value?.validate((isValid) => {
      if (isValid) {
        btnLoading.value = true
        callback({...loginModel}).finally(() => {
          btnLoading.value = false
        })
      }
    })
  }
  const handleKeyup = function(e: any) {
    if (e.type === 'keyup' && e.key === 'Enter') {
      elFormRef.value?.validate((isValid) => {
        if (isValid) {
          btnLoading.value = true
          callback({...loginModel}).finally(() => {
            btnLoading.value = false
          })
        }
      })
    }
  }

  const handleShow = function() {
    OauthUrlList().then((res: any) => {
      oauthUrlList.value = res.data
    }).catch(() => {
      oauthUrlList.value = []
    })
  }
  const handleRedirect = function(e: any) {
    location.href = e.invokeUrl
  }

  const handleLanguageChange = function(lang: LocaleType) {
    localeStore.setLocale(lang)
  }

  return {
    renderLoginForm: () => (
      <div class="zqy-login__form-wrap">
        <div class="zqy-login__language-switch">
          <el-dropdown onCommand={handleLanguageChange}>
            {{
              default: () => (
                <span class="language-trigger">
                  <el-icon><operation /></el-icon>
                  <span>{ t(`language.${localeStore.locale === 'zh-CN' ? 'zhCN' : 'enUS'}`) }</span>
                </span>
              ),
              dropdown: () => (
                <el-dropdown-menu>
                  <el-dropdown-item command="zh-CN">{ t('language.zhCN') }</el-dropdown-item>
                  <el-dropdown-item command="en-US">{ t('language.enUS') }</el-dropdown-item>
                </el-dropdown-menu>
              )
            }}
          </el-dropdown>
        </div>
        <img src={logo}/>
        <div class="zqy-login__main__title">
          { t('login.title') }
        </div>
        <el-form
          ref={elFormRef}
          class="zqy-login__form"
          model={loginModel}
          rules={loginRule.value}
          onKeyup={handleKeyup}
          label-position="top"
        >
          <el-form-item prop="account">
            <el-input
              prefix-icon="User"
              class="zqy-login__input"
              v-model={loginModel.account}
              placeholder={t('login.accountPlaceholder')}
            ></el-input>
          </el-form-item>
          <el-form-item prop="passwd">
            <el-input
              prefix-icon="Lock"
              class="zqy-login__input"
              v-model={loginModel.passwd}
              type="password"
              show-password
              autocomplete="new-password"
              placeholder={t('login.passwordPlaceholder')}
            ></el-input>
          </el-form-item>
        </el-form>
        <el-button
          class="zqy-login__btn"
          type="primary"
          loading={btnLoading.value}
          onClick={handleLogin}
        >{ t('login.title') }</el-button>
        <div class="oauth-login">
          <el-popover
            trigger="click"
            placement="bottom"
            width={180}
            onShow={handleShow}
            v-slots={{
                reference: () => <span class="oauth-login-text">免密登录</span>,
                default: () =>
                <div class="oauth-redirect-url">
                  {
                    !oauthUrlList.value.length ?
                    <div style="margin: auto;">暂无数据</div> :
                    oauthUrlList.value.map(item => <el-button type="primary" onClick={ () => handleRedirect(item)}>{item.name}</el-button>)
                  }
                </div>
            }}>
          </el-popover>
        </div>
      </div>
    ),

    loginModel: readonly(loginModel)
  }
}