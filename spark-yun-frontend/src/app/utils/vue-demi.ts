import * as Vue from 'vue'

export * from 'vue'

export const isVue2 = false
export const isVue3 = true
export const Vue2 = undefined

export function install() {}

export function set(target: any, key: any, value: any) {
    if (Array.isArray(target)) {
        target.length = Math.max(target.length, key)
        target.splice(key, 1, value)
        return value
    }
    target[key] = value
    return value
}

export function del(target: any, key: any) {
    if (Array.isArray(target)) {
        target.splice(key, 1)
        return
    }
    delete target[key]
}

export { Vue }
