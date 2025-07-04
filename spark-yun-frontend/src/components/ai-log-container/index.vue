<template>
  <div class="ai-log-container" ref="containerRef">
    <!-- AI对话头部 -->
    <div class="ai-log-header">
      <div class="ai-log-title">
        <el-icon class="ai-icon"><ChatDotRound /></el-icon>
        <span>AI对话日志</span>
      </div>
      <div class="ai-log-actions">
        <el-button size="small" type="primary" @click="copyContent" :icon="DocumentCopy">
          复制内容
        </el-button>
        <el-button size="small" @click="toggleFormat" :icon="View">
          {{ showMarkdown ? '原始格式' : 'Markdown格式' }}
        </el-button>
      </div>
    </div>

    <!-- AI对话内容 -->
    <div class="ai-log-content" @mousewheel="mousewheelEvent">
      <!-- Markdown渲染模式 -->
      <div v-if="showMarkdown" class="ai-markdown-content" v-html="formattedContent"></div>
      
      <!-- 原始文本模式 -->
      <pre v-else class="ai-raw-content">{{ logMsg + loadingMsg }}</pre>
    </div>

    <!-- 下载按钮 -->
    <span v-if="!showResult" class="zqy-download-log" @click="downloadLog">下载日志</span>
  </div>
</template>

<script lang="ts" setup>
import { ref, defineProps, onMounted, computed, nextTick, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, DocumentCopy, View } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const props = defineProps<{
  logMsg: string,
  status: boolean,
  showResult: boolean
}>()

const containerRef = ref(null)
const position = ref(true)
const loadingTimer = ref()
const loadingPoint = ref('.')
const showMarkdown = ref(true)

watch(() => props.logMsg, () => {
  if (position.value) {
    nextTick(() => {
      scrollToBottom()
    })
  }
}, {
  immediate: true
})

const loadingMsg = computed(() => {
  const str = !props.status ? `加载中${loadingPoint.value}` : ''
  return str
})

// 格式化AI内容为Markdown
const formattedContent = computed(() => {
  if (!props.logMsg) return ''
  
  let content = props.logMsg.trim()
  
  // 尝试解析JSON格式的AI响应
  if (content.startsWith('{') && content.endsWith('}')) {
    try {
      const jsonData = JSON.parse(content)
      // 提取AI回复内容
      if (jsonData.content) {
        content = jsonData.content
      } else if (jsonData.text) {
        content = jsonData.text
      } else if (jsonData.message) {
        content = typeof jsonData.message === 'string' ? jsonData.message : JSON.stringify(jsonData.message, null, 2)
      } else if (jsonData.data && jsonData.data.content) {
        content = jsonData.data.content
      } else {
        content = JSON.stringify(jsonData, null, 2)
      }
    } catch (e) {
      // JSON解析失败，使用原始内容
    }
  }
  
  // 转换Markdown格式
  let formatted = content
  
  // 标题
  formatted = formatted.replace(/^# (.*$)/gm, '<h1 class="ai-h1">$1</h1>')
  formatted = formatted.replace(/^## (.*$)/gm, '<h2 class="ai-h2">$1</h2>')
  formatted = formatted.replace(/^### (.*$)/gm, '<h3 class="ai-h3">$1</h3>')
  
  // 粗体和斜体
  formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong class="ai-bold">$1</strong>')
  formatted = formatted.replace(/\*(.*?)\*/g, '<em class="ai-italic">$1</em>')
  
  // 行内代码
  formatted = formatted.replace(/`([^`]+)`/g, '<code class="ai-inline-code">$1</code>')
  
  // 代码块
  formatted = formatted.replace(/```(\w+)?\n([\s\S]*?)```/g, (match, lang, code) => {
    return `<div class="ai-code-block">
      ${lang ? `<div class="ai-code-lang">${lang}</div>` : ''}
      <pre class="ai-code"><code>${code.trim()}</code></pre>
    </div>`
  })
  
  // 列表
  formatted = formatted.replace(/^\* (.*$)/gm, '<li class="ai-list-item">$1</li>')
  formatted = formatted.replace(/^(\d+)\. (.*$)/gm, '<li class="ai-ordered-item">$2</li>')
  
  // 包装列表
  formatted = formatted.replace(/(<li class="ai-list-item">.*<\/li>)/gs, '<ul class="ai-list">$1</ul>')
  formatted = formatted.replace(/(<li class="ai-ordered-item">.*<\/li>)/gs, '<ol class="ai-ordered-list">$1</ol>')
  
  // 换行
  formatted = formatted.replace(/\n/g, '<br>')
  
  // 链接
  formatted = formatted.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" class="ai-link" target="_blank">$1</a>')
  
  return formatted
})

function copyContent() {
  const textToCopy = props.logMsg || ''
  navigator.clipboard.writeText(textToCopy).then(() => {
    ElMessage.success('AI日志已复制到剪贴板')
  }).catch(() => {
    // 降级方案
    const textArea = document.createElement('textarea')
    textArea.value = textToCopy
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success('AI日志已复制到剪贴板')
  })
}

function toggleFormat() {
  showMarkdown.value = !showMarkdown.value
  nextTick(() => {
    scrollToBottom()
  })
}

function downloadLog() {
  const logStr = props.logMsg
  const nowDate = dayjs(new Date()).format('YYYY-MM-DD HH:mm:ss')
  const blob = new Blob([logStr], {
    type: "text/plain;charset=utf-8"
  })
  const objectURL = URL.createObjectURL(blob)
  const aTag = document.createElement('a')
  aTag.href = objectURL
  aTag.download = `AI对话日志-${nowDate}.log`
  aTag.click()
  URL.revokeObjectURL(objectURL)
}

function scrollToBottom() {
  if (containerRef.value) {
    const contentElement = containerRef.value.querySelector('.ai-log-content')
    if (contentElement) {
      contentElement.scrollTop = contentElement.scrollHeight
    }
  }
}

function mousewheelEvent(e: any) {
  if (!(e.deltaY > 0)) {
    position.value = false
  }
}

// 外部调用
function resetPosition() {
  if (position.value) {
    nextTick(() => {
      scrollToBottom()
    })
  }
}

onMounted(() => {
  position.value = true
  if (loadingTimer.value) {
    clearInterval(loadingTimer.value)
  }
  loadingTimer.value = null
  loadingTimer.value = setInterval(() => {
    if (loadingPoint.value.length < 5) {
      loadingPoint.value = loadingPoint.value + '.'
    } else {
      loadingPoint.value = '.'
    }
  }, 1000)
})

onUnmounted(() => {
  if (loadingTimer.value) {
    clearInterval(loadingTimer.value)
  }
  loadingTimer.value = null
})

defineExpose({
  resetPosition
})
</script>

<style lang="scss">
.ai-log-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  border-radius: 8px;
  overflow: hidden;
  position: relative;

  .ai-log-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-bottom: 1px solid #e1e8ed;

    .ai-log-title {
      display: flex;
      align-items: center;
      font-size: 16px;
      font-weight: 600;

      .ai-icon {
        font-size: 20px;
        margin-right: 8px;
      }
    }

    .ai-log-actions {
      display: flex;
      gap: 8px;

      .el-button {
        background: rgba(255, 255, 255, 0.2);
        border: 1px solid rgba(255, 255, 255, 0.3);
        color: white;

        &:hover {
          background: rgba(255, 255, 255, 0.3);
          border-color: rgba(255, 255, 255, 0.5);
        }
      }
    }
  }

  .ai-log-content {
    flex: 1;
    overflow-y: auto;
    padding: 20px;

    .ai-markdown-content {
      background: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      line-height: 1.6;
      font-size: 14px;
      color: #333;

      .ai-h1 {
        font-size: 24px;
        font-weight: 700;
        color: #2c3e50;
        margin: 20px 0 16px 0;
        padding-bottom: 8px;
        border-bottom: 2px solid #667eea;
      }

      .ai-h2 {
        font-size: 20px;
        font-weight: 600;
        color: #34495e;
        margin: 16px 0 12px 0;
      }

      .ai-h3 {
        font-size: 16px;
        font-weight: 600;
        color: #34495e;
        margin: 12px 0 8px 0;
      }

      .ai-bold {
        font-weight: 600;
        color: #2c3e50;
      }

      .ai-italic {
        font-style: italic;
        color: #7f8c8d;
      }

      .ai-inline-code {
        background: #f1f2f6;
        padding: 2px 6px;
        border-radius: 4px;
        font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
        font-size: 13px;
        color: #e74c3c;
      }

      .ai-code-block {
        margin: 16px 0;
        border-radius: 8px;
        overflow: hidden;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

        .ai-code-lang {
          background: #2d3748;
          color: #e2e8f0;
          padding: 8px 16px;
          font-size: 12px;
          font-weight: 600;
        }

        .ai-code {
          background: #2d3748;
          color: #e2e8f0;
          padding: 16px;
          margin: 0;
          overflow-x: auto;
          font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
          font-size: 14px;
          line-height: 1.5;

          code {
            background: transparent;
            color: inherit;
            padding: 0;
          }
        }
      }

      .ai-list, .ai-ordered-list {
        margin: 12px 0;
        padding-left: 20px;

        .ai-list-item, .ai-ordered-item {
          margin: 4px 0;
          line-height: 1.6;
        }
      }

      .ai-link {
        color: #667eea;
        text-decoration: none;

        &:hover {
          text-decoration: underline;
        }
      }
    }

    .ai-raw-content {
      background: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      color: #333;
      font-size: 14px;
      line-height: 1.6;
      margin: 0;
      white-space: pre-wrap;
      word-wrap: break-word;
    }
  }

  .zqy-download-log {
    font-size: 12px;
    color: white;
    cursor: pointer;
    position: absolute;
    top: 16px;
    right: 20px;
    background: rgba(255, 255, 255, 0.2);
    padding: 4px 8px;
    border-radius: 4px;
    
    &:hover {
      background: rgba(255, 255, 255, 0.3);
      text-decoration: underline;
    }
  }
}
</style>
