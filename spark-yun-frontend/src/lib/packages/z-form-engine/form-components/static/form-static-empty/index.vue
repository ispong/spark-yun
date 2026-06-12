<template>
    <form-render-item :form-config="formConfig" :is-dragger="isDragger" class="form-static-empty">
        <el-input v-model="formData" class="empty-container" :readonly="true" />
    </form-render-item>
</template>
<script lang="ts" setup>
import { defineProps, defineEmits, computed, ref, watch } from 'vue'
import FormRenderItem from '../../form-render-item/index.vue'

const props = defineProps(['renderSence', 'modelValue', 'formConfig', 'isDragger'])
const formData = ref('')
</script>

<style lang="scss">
.form-static-empty {
    // pointer-events: none;
    .el-form-item__label {
        opacity: 0;
    }
    .el-form-item__content {
        .empty-container {
            pointer-events: none;

            opacity: 0;
        }
    }
}
</style>
