<template>
  <div class="ai-result-container">
    <LoadingPage :visible="loading">
      <!-- AI结果展示头部 -->
      <div class="ai-result-header">
        <div class="ai-result-title">
          <el-icon class="ai-icon"><ChatDotRound /></el-icon>
          <span>AI结果展示</span>
        </div>
        <div class="ai-result-actions">
          <el-button size="small" type="primary" @click="copyContent" :icon="DocumentCopy">
            复制内容
          </el-button>
        </div>
      </div>

      <!-- AI结果内容 - 只显示Markdown格式 -->
      <div class="ai-result-content">
        <div class="ai-markdown-content" v-html="formattedContent"></div>
      </div>
    </LoadingPage>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, defineExpose, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, DocumentCopy } from '@element-plus/icons-vue'
import { GetResultData } from '@/services/schedule.service'
import LoadingPage from '@/components/loading/index.vue'
import hljs from 'highlight.js/lib/core'
// 只导入需要的语言
import javascript from 'highlight.js/lib/languages/javascript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import sql from 'highlight.js/lib/languages/sql'
import xml from 'highlight.js/lib/languages/xml'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'

// 注册语言
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('python', python)
hljs.registerLanguage('java', java)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('json', json)
hljs.registerLanguage('bash', bash)

const loading = ref<boolean>(false)
const aiContent = ref<string>('')

// 格式化AI内容为Markdown
const formattedContent = computed(() => {
  if (!aiContent.value) return ''
  
  let content = aiContent.value.trim()
  
  // 转换Markdown格式
  let formatted = content
  
  // 标题
  formatted = formatted.replace(/^# (.*$)/gm, '<h1 class="ai-h1">$1</h1>')
  formatted = formatted.replace(/^## (.*$)/gm, '<h2 class="ai-h2">$1</h2>')
  formatted = formatted.replace(/^### (.*$)/gm, '<h3 class="ai-h3">$1</h3>')
  
  // 粗体和斜体
  formatted = formatted.replace(/\*\*(.*?)\*\*/g, '<strong class="ai-bold">$1</strong>')
  formatted = formatted.replace(/\*(.*?)\*/g, '<em class="ai-italic">$1</em>')
  
  // 代码块（三个反引号）
  formatted = formatted.replace(/```(\w+)?\n([\s\S]*?)```/g, (match, lang, code) => {
    const codeId = 'code-' + Math.random().toString(36).substr(2, 9)
    const trimmedCode = code.trim()
    const language = lang || 'plaintext'

    // 使用 highlight.js 进行语法高亮
    let highlightedCode
    try {
      if (lang && hljs.getLanguage(lang)) {
        highlightedCode = hljs.highlight(trimmedCode, { language: lang }).value
      } else {
        highlightedCode = hljs.highlightAuto(trimmedCode).value
      }
    } catch (e) {
      highlightedCode = trimmedCode
    }

    return `<div class="ai-code-block">
      <div class="ai-code-header">
        <div class="ai-code-lang">${language}</div>
        <button class="ai-code-copy" onclick="copyCodeBlock('${codeId}')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
          </svg>
          复制
        </button>
      </div>
      <pre class="ai-code hljs"><code id="${codeId}">${highlightedCode}</code></pre>
    </div>`
  })

  // 代码块（单个反引号包围的多行代码，常见于AI回复）
  formatted = formatted.replace(/`(\w+)?\n([\s\S]*?)\n`/g, (match, lang, code) => {
    const codeId = 'code-' + Math.random().toString(36).substr(2, 9)
    const trimmedCode = code.trim()
    const language = lang || 'plaintext'

    // 使用 highlight.js 进行语法高亮
    let highlightedCode
    try {
      if (lang && hljs.getLanguage(lang)) {
        highlightedCode = hljs.highlight(trimmedCode, { language: lang }).value
      } else {
        highlightedCode = hljs.highlightAuto(trimmedCode).value
      }
    } catch (e) {
      highlightedCode = trimmedCode
    }

    return `<div class="ai-code-block">
      <div class="ai-code-header">
        <div class="ai-code-lang">${language}</div>
        <button class="ai-code-copy" onclick="copyCodeBlock('${codeId}')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
          </svg>
          复制
        </button>
      </div>
      <pre class="ai-code hljs"><code id="${codeId}">${highlightedCode}</code></pre>
    </div>`
  })

  // 行内代码（单个反引号，单行）
  formatted = formatted.replace(/`([^`\n]+)`/g, '<code class="ai-inline-code">$1</code>')
  
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

// 从JSON中提取content字段
function extractContentFromJson(jsonStr: string): string {
  try {
    const jsonData = JSON.parse(jsonStr)

    // 递归查找content字段
    function findContent(obj: any): string | null {
      if (typeof obj !== 'object' || obj === null) {
        return null
      }

      // 直接查找content字段
      if (obj.content && typeof obj.content === 'string') {
        return obj.content
      }

      // 递归查找所有嵌套对象中的content字段
      for (const key in obj) {
        if (typeof obj[key] === 'object') {
          const nestedContent = findContent(obj[key])
          if (nestedContent) {
            return nestedContent
          }
        }
      }

      return null
    }

    const content = findContent(jsonData)
    return content || '未找到content字段内容'

  } catch (e) {
    // JSON解析失败，返回错误信息
    return 'JSON解析失败，无法提取content内容'
  }
}

// 获取AI结果数据
function initData(id: string): void {
  if (!id) {
    aiContent.value = ''
    loading.value = false
    return
  }

  loading.value = true
  GetResultData({
    instanceId: id
  })
    .then((res: any) => {
      // 只从运行结果的JSON中提取content字段
      if (res.data.jsonData) {
        // 运行结果tab显示的是格式化的JSON，从中提取content
        aiContent.value = extractContentFromJson(res.data.jsonData)
      } else {
        aiContent.value = '暂无AI结果内容'
      }

      loading.value = false
    })
    .catch(() => {
      aiContent.value = '获取AI结果失败'
      loading.value = false
    })
}

function copyContent() {
  const textToCopy = aiContent.value || ''
  navigator.clipboard.writeText(textToCopy).then(() => {
    ElMessage.success('AI结果已复制到剪贴板')
  }).catch(() => {
    // 降级方案
    const textArea = document.createElement('textarea')
    textArea.value = textToCopy
    document.body.appendChild(textArea)
    textArea.select()
    document.execCommand('copy')
    document.body.removeChild(textArea)
    ElMessage.success('AI结果已复制到剪贴板')
  })
}

// 代码块复制函数
function copyCodeBlock(codeId: string) {
  const codeElement = document.getElementById(codeId)
  if (codeElement) {
    const codeText = codeElement.textContent || ''

    if (navigator.clipboard) {
      navigator.clipboard.writeText(codeText).then(() => {
        ElMessage.success('代码已复制到剪贴板')
        // 临时改变按钮文本显示复制成功
        const button = codeElement.closest('.ai-code-block')?.querySelector('.ai-code-copy')
        if (button) {
          const originalText = button.innerHTML
          button.innerHTML = `
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20,6 9,17 4,12"></polyline>
            </svg>
            已复制
          `
          setTimeout(() => {
            button.innerHTML = originalText
          }, 2000)
        }
      }).catch(() => {
        // 降级方案
        fallbackCopyTextToClipboard(codeText)
      })
    } else {
      // 降级方案
      fallbackCopyTextToClipboard(codeText)
    }
  }
}

// 降级复制方案
function fallbackCopyTextToClipboard(text: string) {
  const textArea = document.createElement('textarea')
  textArea.value = text
  textArea.style.position = 'fixed'
  textArea.style.left = '-999999px'
  textArea.style.top = '-999999px'
  document.body.appendChild(textArea)
  textArea.focus()
  textArea.select()

  try {
    document.execCommand('copy')
    ElMessage.success('代码已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败，请手动复制')
  }

  document.body.removeChild(textArea)
}



// 组件挂载时注册全局函数
onMounted(() => {
  // 将复制函数注册到全局，供HTML中的onclick使用
  ;(window as any).copyCodeBlock = copyCodeBlock
})

// 组件卸载时清理全局函数
onUnmounted(() => {
  // 清理全局函数
  if ((window as any).copyCodeBlock) {
    delete (window as any).copyCodeBlock
  }
})

defineExpose({
  initData
})
</script>

<style lang="scss" scoped>
.ai-result-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  border-radius: 8px;
  overflow: hidden;

  .ai-result-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-bottom: 1px solid #e1e8ed;

    .ai-result-title {
      display: flex;
      align-items: center;
      font-size: 16px;
      font-weight: 600;

      .ai-icon {
        font-size: 20px;
        margin-right: 8px;
      }
    }

    .ai-result-actions {
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

  .ai-result-content {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    width: 100%;
    max-width: 100%;

    .ai-markdown-content {
      background: white;
      padding: 20px;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      line-height: 1.6;
      font-size: 14px;
      color: #333;
      width: 100%;
      max-width: 100%;
      box-sizing: border-box;

      :deep(.ai-h1) {
        font-size: 24px;
        font-weight: 700;
        color: #2c3e50;
        margin: 20px 0 16px 0;
        padding-bottom: 8px;
        border-bottom: 2px solid #667eea;
      }

      :deep(.ai-h2) {
        font-size: 20px;
        font-weight: 600;
        color: #34495e;
        margin: 16px 0 12px 0;
      }

      :deep(.ai-h3) {
        font-size: 16px;
        font-weight: 600;
        color: #34495e;
        margin: 12px 0 8px 0;
      }

      :deep(.ai-bold) {
        font-weight: 600;
        color: #2c3e50;
      }

      :deep(.ai-italic) {
        font-style: italic;
        color: #7f8c8d;
      }

      :deep(.ai-inline-code) {
        background: #f1f2f6;
        padding: 2px 6px;
        border-radius: 4px;
        font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
        font-size: 13px;
        color: #e74c3c;
      }

      :deep(.ai-code-block) {
        margin: 16px 0;
        border-radius: 6px;
        overflow: hidden;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
        width: 100%;
        max-width: 100%;
        position: relative;
        background: #2d3748;
        border: 1px solid #4a5568;
      }

      :deep(.ai-code-header) {
        background: #2d3748;
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 8px 16px;
        margin: 0;
        border: none;
        border-bottom: none;
        min-height: 36px;
        position: relative;
      }

      :deep(.ai-code-lang) {
        color: #e2e8f0;
        font-size: 12px;
        font-weight: 600;
        margin: 0;
        padding: 0;
        line-height: 1.2;
        text-align: left;
        flex: 1;
      }

      :deep(.ai-code-copy) {
        background: rgba(255, 255, 255, 0.1);
        border: 1px solid rgba(255, 255, 255, 0.2);
        color: #e2e8f0;
        padding: 4px 8px;
        border-radius: 4px;
        font-size: 11px;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 4px;
        transition: all 0.2s ease;
        height: 24px;
        line-height: 1;
        white-space: nowrap;
        flex-shrink: 0;
        margin-left: auto;

        &:hover {
          background: rgba(255, 255, 255, 0.2);
          border-color: rgba(255, 255, 255, 0.3);
        }

        &:active {
          transform: translateY(1px);
        }

        svg {
          width: 12px;
          height: 12px;
          flex-shrink: 0;
        }
      }

      :deep(.ai-code) {
        background: #2d3748;
        color: #e2e8f0;
        padding: 12px 16px;
        margin: 0;
        overflow-x: auto;
        overflow-y: hidden;
        font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
        font-size: 13px;
        line-height: 1.3;
        width: 100%;
        max-width: 100%;
        white-space: pre;
        word-wrap: normal;
        border: none;

        /* 自定义滚动条样式 */
        &::-webkit-scrollbar {
          height: 8px;
        }

        &::-webkit-scrollbar-track {
          background: #1a202c;
          border-radius: 4px;
        }

        &::-webkit-scrollbar-thumb {
          background: #4a5568;
          border-radius: 4px;

          &:hover {
            background: #718096;
          }
        }

        /* Firefox滚动条样式 */
        scrollbar-width: thin;
        scrollbar-color: #4a5568 #1a202c;

        /* highlight.js 语法高亮样式 */
        .hljs-keyword { color: #c792ea; }
        .hljs-string { color: #c3e88d; }
        .hljs-number { color: #f78c6c; }
        .hljs-comment { color: #546e7a; font-style: italic; }
        .hljs-function { color: #82aaff; }
        .hljs-variable { color: #eeffff; }
        .hljs-built_in { color: #ffcb6b; }
        .hljs-type { color: #c792ea; }
        .hljs-class { color: #ffcb6b; }
        .hljs-title { color: #82aaff; }
        .hljs-attr { color: #c792ea; }
        .hljs-attribute { color: #c792ea; }
        .hljs-literal { color: #ff5370; }
        .hljs-meta { color: #ffcb6b; }
        .hljs-tag { color: #ff5370; }
        .hljs-name { color: #ff5370; }
        .hljs-selector-tag { color: #ff5370; }
        .hljs-selector-id { color: #f78c6c; }
        .hljs-selector-class { color: #ffcb6b; }
        .hljs-regexp { color: #c3e88d; }
        .hljs-link { color: #82aaff; }
        .hljs-symbol { color: #c792ea; }
        .hljs-bullet { color: #82aaff; }
        .hljs-addition { color: #c3e88d; background: rgba(195, 232, 141, 0.1); }
        .hljs-deletion { color: #ff5370; background: rgba(255, 83, 112, 0.1); }
      }

      :deep(.ai-code code) {
        background: transparent;
        color: inherit;
        padding: 0;
        font-family: inherit;
        font-size: inherit;
      }

      :deep(.ai-list), :deep(.ai-ordered-list) {
        margin: 12px 0;
        padding-left: 20px;
      }

      :deep(.ai-list-item), :deep(.ai-ordered-item) {
        margin: 4px 0;
        line-height: 1.6;
      }

      :deep(.ai-link) {
        color: #667eea;
        text-decoration: none;

        &:hover {
          text-decoration: underline;
        }
      }
    }


  }
}
</style>
