import { expect, test } from '@playwright/test'

test.beforeEach(async ({ page }) => {
    await page.route('**/platform-setting/open/getSetting', async (route) => {
        await route.fulfill({
            json: {
                data: {}
            }
        })
    })

    await page.route('**/login-method/open/getConfig', async (route) => {
        await route.fulfill({
            json: {
                data: {
                    defaultLoginMethod: 'ACCOUNT',
                    accountEnabled: true,
                    accountPasswordEnabled: true,
                    accountPhonePasswordEnabled: true,
                    accountEmailPasswordEnabled: true,
                    emailEnabled: false,
                    emailRegisterEnabled: false,
                    phoneEnabled: false,
                    phoneRegisterEnabled: false
                }
            }
        })
    })

    await page.route('**/vip/auth/open/querySsoAuth', async (route) => {
        await route.fulfill({
            json: {
                data: []
            }
        })
    })
})

test('renders account login page', async ({ page }) => {
    await page.goto('/auth')

    await expect(page.getByRole('heading', { name: '用户登录' })).toBeVisible()
    await expect(page.getByPlaceholder('请输入账号/手机号/邮箱')).toBeVisible()
    await expect(page.getByPlaceholder('请输入密码')).toBeVisible()
    await expect(page.getByRole('button', { name: '确认登录' })).toBeVisible()
})
