export interface PersonalModel {
    username: string
    account: string
    phone: string
    email: string
    remark: string
}

export interface PasswordModel {
    oldPassword: string
    verifyType: 'OLD_PASSWORD' | 'PHONE' | 'EMAIL'
    code: string
    phoneCode: string
    emailCode: string
    newPassword: string
    confirmPassword: string
}

export interface PhoneModel {
    phone: string
    code: string
}

export interface EmailModel {
    email: string
    code: string
}
