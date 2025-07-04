<template>
  <BlockModal :model-config="modelConfig">
    <el-form
      ref="form"
      class="add-ai-config"
      label-position="top"
      :model="formData"
      :rules="rules"
    >
      <el-form-item
        label="配置名称"
        prop="name"
      >
        <el-input
          v-model="formData.name"
          maxlength="50"
          placeholder="请输入配置名称"
        />
      </el-form-item>
      <el-form-item
        label="模型类型"
        prop="modelType"
      >
        <el-select
          v-model="formData.modelType"
          placeholder="请选择模型类型"
          style="width: 100%"
          @change="modelTypeChange"
        >
          <el-option
            v-for="item in ModelTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        label="API地址"
        prop="apiUrl"
      >
        <el-tooltip placement="top">
          <template #content>
            <pre>
通义千问: https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation
ChatGPT-4o: https://api.openai.com/v1/chat/completions
Gemini: https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
Claude: https://api.anthropic.com/v1/messages
文心一言: https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions
            </pre>
          </template>
          <el-icon style="left: 50px" class="tooltip-msg"><QuestionFilled /></el-icon>
        </el-tooltip>
        <el-input
          v-model="formData.apiUrl"
          placeholder="请输入API地址"
          maxlength="500"
        />
      </el-form-item>
      <el-form-item
        label="API密钥"
        prop="apiKey"
      >
        <el-input
          v-model="formData.apiKey"
          type="password"
          show-password
          placeholder="请输入API密钥"
          maxlength="500"
        />
      </el-form-item>

      <el-form-item label="备注">
        <el-input
          v-model="formData.remark"
          type="textarea"
          maxlength="500"
          :autosize="{ minRows: 4, maxRows: 4 }"
          placeholder="请输入备注"
        />
      </el-form-item>
    </el-form>
    <template #customLeft>
      <div class="test-button">
        <el-button :loading="testLoading" type="primary" @click="testConnection">连接测试</el-button>
        <el-popover
          placement="right"
          title="测试结果"
          :width="400"
          trigger="hover"
          popper-class="message-error-tooltip"
          :content="testResult?.message"
          v-if="testResult?.message"
        >
          <template #reference>
            <el-icon class="hover-tooltip" v-if="!testResult?.success"><WarningFilled /></el-icon>
            <el-icon class="hover-tooltip success" v-else><SuccessFilled /></el-icon>
          </template>
        </el-popover>
      </div>
    </template>
  </BlockModal>
</template>

<script lang="ts" setup>
import { reactive, defineExpose, ref, nextTick } from 'vue'
import BlockModal from '@/components/block-modal/index.vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { QuestionFilled, WarningFilled, SuccessFilled } from '@element-plus/icons-vue'
import { ModelTypeOptions } from '../ai-config.config'
import { TestAiConfig } from '@/api/ai-config'

const form = ref<FormInstance>()
const callback = ref<any>()
const testLoading = ref(false)
const testResult = ref<any>()

const modelConfig = reactive({
  title: '添加AI配置',
  visible: false,
  width: '520px',
  customClass: 'ai-config-add-modal',
  okConfig: {
    title: '确定',
    ok: okEvent,
    disabled: false,
    loading: false
  },
  cancelConfig: {
    title: '取消',
    cancel: closeEvent,
    disabled: false
  },
  needScale: false,
  zIndex: 1100,
  closeOnClickModal: false
})

const formData = reactive({
  name: '',
  modelType: '',
  apiUrl: '',
  apiKey: '',
  remark: '',
  id: ''
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入配置名称', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
  ],
  modelType: [
    { required: true, message: '请选择模型类型', trigger: 'change' }
  ],
  apiUrl: [
    { required: true, message: '请输入API地址', trigger: 'blur' },
    { type: 'url', message: '请输入正确的URL格式', trigger: 'blur' }
  ],
  apiKey: [
    { required: true, message: '请输入API密钥', trigger: 'blur' }
  ]
}

// 默认API地址映射
const defaultApiUrls: Record<string, string> = {
  'QWEN': 'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation',
  'CHATGPT_4O': 'https://api.openai.com/v1/chat/completions',
  'GEMINI': 'https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent',
  'CLAUDE': 'https://api.anthropic.com/v1/messages',
  'ERNIE': 'https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions'
}

function showModal(cb: any, data?: any) {
  callback.value = cb
  testResult.value = null

  if (data) {
    formData.name = data.name
    formData.modelType = data.modelType
    formData.apiUrl = data.apiUrl
    formData.apiKey = data.apiKey
    formData.remark = data.remark || ''
    formData.id = data.id
    modelConfig.title = '编辑AI配置'
  } else {
    formData.name = ''
    formData.modelType = ''
    formData.apiUrl = ''
    formData.apiKey = ''
    formData.remark = ''
    formData.id = ''
    modelConfig.title = '添加AI配置'
  }

  modelConfig.visible = true
  nextTick(() => {
    form.value?.resetFields()
  })
}

function modelTypeChange(modelType: string) {
  // 当选择模型类型时，自动填充默认API地址
  if (modelType && defaultApiUrls[modelType]) {
    formData.apiUrl = defaultApiUrls[modelType]
  }
}

function testConnection() {
  form.value?.validate((valid: boolean) => {
    if (valid) {
      testLoading.value = true
      TestAiConfig({
        ...formData
      }).then((res: any) => {
        testLoading.value = false
        testResult.value = {
          success: true,
          message: res.data || 'AI配置连接测试成功！'
        }
        ElMessage.success(res.msg || '连接测试成功')
      }).catch((error: any) => {
        testLoading.value = false
        testResult.value = {
          success: false,
          message: error.message || 'AI配置连接测试失败'
        }
        ElMessage.error('连接测试失败')
      })
    } else {
      ElMessage.warning('请将表单输入完整')
    }
  })
}

function okEvent() {
  form.value?.validate((valid: boolean) => {
    if (valid) {
      modelConfig.okConfig.loading = true
      callback
        .value({
          ...formData,
          id: formData.id ? formData.id : undefined
        })
        .then((res: any) => {
          modelConfig.okConfig.loading = false
          if (res === undefined) {
            modelConfig.visible = false
          } else {
            modelConfig.visible = true
          }
        })
        .catch(() => {
          modelConfig.okConfig.loading = false
        })
    } else {
      ElMessage.warning('请将表单输入完整')
    }
  })
}

function closeEvent() {
  modelConfig.visible = false
}

defineExpose({
  showModal
})
</script>

<style lang="scss" scoped>
.add-ai-config {
  padding: 20px;

  .tooltip-msg {
    position: absolute;
    top: 8px;
    color: #909399;
    cursor: pointer;

    &:hover {
      color: #409eff;
    }
  }
}

.test-button {
  display: flex;
  align-items: center;
  gap: 8px;

  .hover-tooltip {
    font-size: 18px;
    cursor: pointer;
    color: #f56c6c;

    &.success {
      color: #67c23a;
    }

    &:hover {
      opacity: 0.8;
    }
  }
}
</style>

<style lang="scss">
.ai-config-add-modal {
  .el-form-item__label {
    font-weight: 500;
    color: #303133;
  }

  .el-input__wrapper {
    box-shadow: 0 0 0 1px #dcdfe6 inset;

    &:hover {
      box-shadow: 0 0 0 1px #c0c4cc inset;
    }

    &.is-focus {
      box-shadow: 0 0 0 1px #409eff inset;
    }
  }

  .el-select {
    .el-input__wrapper {
      cursor: pointer;
    }
  }

  .el-textarea__inner {
    box-shadow: 0 0 0 1px #dcdfe6 inset;

    &:hover {
      box-shadow: 0 0 0 1px #c0c4cc inset;
    }

    &:focus {
      box-shadow: 0 0 0 1px #409eff inset;
    }
  }
}

.message-error-tooltip {
  .el-popover__title {
    color: #f56c6c;
  }
}
</style>
