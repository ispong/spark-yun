<template>
    <div class="form-components-config">
        <el-scrollbar>
            <el-form
                v-if="formData"
                ref="formConfigRef"
                class="form-config"
                :model="formData"
                :label-position="'top'"
                @validate="validateChange"
            >
                <template v-for="(config, index) in configListComp" :key="index">
                    <component
                        :is="getConfigComponentName(config)"
                        v-model="formData[getConfigComponentKey(config)]"
                        :form-config="formConfig"
                        :is-auto-create-table="isAutoCreateTable"
                        :get-table-codes-method="getTableCodesMethod"
                        @form-config-change="formConfigChange"
                    />
                </template>
            </el-form>
            <template v-else>
                <empty-page />
            </template>
        </el-scrollbar>
    </div>
</template>

<script lang="ts" setup>
import { defineProps, defineEmits, computed, ref, shallowRef, markRaw } from 'vue'
import FormSetConfig from './form-set-config'
import FormConfigConmponents from './form-config-components'

const props = defineProps<{
    modelValue?: any
    configList?: any
    formConfig?: any
    getTableCodesMethod?: any
    isAutoCreateTable?: any
}>()
const emit = defineEmits(['update:modelValue', 'componentListChange', 'formConfigChange'])
const formData = computed({
    get() {
        return props.modelValue
    },
    set(value) {
        emit('update:modelValue', value)
    }
})
const formSetConfig = ref(FormSetConfig)
const formConfigConmponents = shallowRef(FormConfigConmponents)

const getConfigComponentName = computed(() => {
    return (code: string) => {
        let configInstance = null
        try {
            configInstance = formSetConfig.value.find((item) => item.formTypeCode === code)
        } catch (error) {
            console.error('请检查配置组件是否注册')
        }
        return markRaw(formConfigConmponents.value[configInstance.formInstanceName])
    }
})
const configListComp = computed(() => {
    if (props.isAutoCreateTable) {
        return props.configList.filter((config: string) => config !== 'CODE_SELECT')
    } else {
        return props.configList
    }
})

const getConfigComponentKey = computed(() => {
    return (code: string) => {
        let configInstance
        try {
            configInstance = formSetConfig.value.find((item) => item.formTypeCode === code)
        } catch (error) {
            console.error('请检查配置组件是否注册')
        }
        return configInstance.formConfigValueCode
    }
})

function formConfigChange(e: any) {
    emit('formConfigChange', e)
}

function validateChange(prop: string, isValid: boolean, message: string): void {
    props.formConfig.valid = isValid
}
</script>

<style lang="scss">
.form-components-config {
    min-width: 320px;
    width: 320px;
    height: 100%;
    overflow: auto;
    box-sizing: border-box;
    background-color: #fff;

    .el-form {
        padding: 16px 12px;
        box-sizing: border-box;

        .el-form-item {
            display: block;
            margin-bottom: 18px;

            .el-form-item__label {
                display: block;
                width: 100%;
                min-height: 16px !important;
                padding: 0;
                margin-bottom: 6px;
                font-size: 12px;
                line-height: 16px !important;
                color: getCssVar('text-color', 'regular');
                position: relative;

                &::before {
                    position: absolute;
                    left: -8px;
                }
            }

            .el-form-item__content {
                display: flex;
                align-items: center;
                width: 100%;
                min-height: 32px;
                line-height: 32px;

                .el-input,
                .el-select,
                .el-input-number,
                .el-date-editor,
                .el-textarea {
                    width: 100%;
                }

                .el-input {
                    font-size: 12px;
                    height: 32px;

                    .el-input__inner {
                        height: 100%;
                        border-radius: 2px;
                        padding: 0 8px;
                    }

                    .el-input__wrapper {
                        width: 100%;
                        padding: 0 8px;
                        border-radius: 2px;
                    }
                }
            }
        }

        .form-config-switch {
            display: flex;
            align-items: center;
            justify-content: space-between;
            min-height: 24px;
            margin-bottom: 12px;

            .el-form-item__label {
                flex: 1;
                width: auto;
                margin-bottom: 0;
                line-height: 24px !important;
            }

            .el-form-item__content {
                position: static !important;
                top: auto !important;
                right: auto !important;
                flex: none;
                justify-content: flex-end;
                width: auto;
                min-height: 24px;
                line-height: 24px;
            }
        }

        .form-code-select {
            .form-code-select__type {
                top: -22px;
                right: 0;

                .el-radio-group {
                    display: flex;
                    align-items: center;
                    flex-wrap: nowrap;
                }

                .el-radio {
                    height: 20px;
                    margin-right: 12px;
                    line-height: 20px;

                    &:last-child {
                        margin-right: 0;
                    }
                }
            }
        }
    }
}
</style>
