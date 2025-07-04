<template>
  <Breadcrumb :bread-crumb-list="breadCrumbList" />
  <div class="zqy-seach-table">
    <div class="zqy-table-top">
      <el-button
        type="primary"
        @click="addData"
      >
        添加AI配置
      </el-button>
      <div class="zqy-seach">
        <el-input
          v-model="keyword"
          placeholder="请输入配置名称/备注 回车进行搜索"
          :maxlength="200"
          clearable
          @input="inputEvent"
          @keyup.enter="initData(false)"
        />
      </div>
    </div>
    <LoadingPage
      :visible="loading"
      :network-error="networkError"
      @loading-refresh="initData(false)"
    >
      <div class="zqy-table">
        <BlockTable
          :table-config="tableConfig"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        >
          <template #modelType="scopeSlot">
            <el-tag :type="getModelTypeColor(scopeSlot.row.modelType)">
              {{ getModelTypeName(scopeSlot.row.modelType) }}
            </el-tag>
          </template>
          <template #status="scopeSlot">
            <el-tag :type="scopeSlot.row.status === 'ENABLE' ? 'success' : 'danger'">
              {{ scopeSlot.row.status === 'ENABLE' ? '启用' : '禁用' }}
            </el-tag>
          </template>
          <template #options="scopeSlot">
            <div class="btn-group">
              <span @click="editData(scopeSlot.row)">编辑</span>
              <span
                v-if="!scopeSlot.row.statusLoading"
                @click="toggleStatus(scopeSlot.row)"
              >
                {{ scopeSlot.row.status === 'ENABLE' ? '禁用' : '启用' }}
              </span>
              <el-icon
                v-else
                class="is-loading"
              >
                <Loading />
              </el-icon>
              <span @click="deleteData(scopeSlot.row)">删除</span>
            </div>
          </template>
        </BlockTable>
      </div>
    </LoadingPage>
    <AddModal ref="addModalRef" />
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import Breadcrumb from '@/layout/bread-crumb/index.vue'
import BlockTable from '@/components/block-table/index.vue'
import LoadingPage from '@/components/loading/index.vue'
import AddModal from './add-modal/index.vue'

import { BreadCrumbList, TableConfig, ModelTypeMap, ModelTypeColorMap } from './ai-config.config'
import { GetAiConfigList, AddAiConfig, UpdateAiConfig, DeleteAiConfig, EnableAiConfig, DisableAiConfig } from '@/api/ai-config'
import { ElMessage, ElMessageBox } from 'element-plus'

interface AiConfigForm {
  id?: string;
  name: string;
  modelType: string;
  apiUrl: string;
  apiKey: string;
  modelName?: string;
  remark?: string;
}

const breadCrumbList = reactive(BreadCrumbList)
const tableConfig: any = reactive(TableConfig)
const keyword = ref('')
const loading = ref(false)
const networkError = ref(false)
const addModalRef = ref(null)

function inputEvent() {
  if (keyword.value === '') {
    initData(false)
  }
}

function initData(tableLoading?: boolean) {
  loading.value = tableLoading ? false : true
  networkError.value = networkError.value || false
  GetAiConfigList({
    page: tableConfig.pagination.currentPage - 1,
    size: tableConfig.pagination.pageSize,
    searchKeyWord: keyword.value
  })
    .then((res: any) => {
      tableConfig.tableData = res.data.content
      tableConfig.pagination.total = res.data.totalElements
      loading.value = false
      tableConfig.loading = false
      networkError.value = false
    })
    .catch(() => {
      tableConfig.tableData = []
      tableConfig.pagination.total = 0
      loading.value = false
      tableConfig.loading = false
      networkError.value = true
    })
}

function addData() {
  addModalRef.value.showModal((formData: AiConfigForm) => {
    return new Promise((resolve: any, reject: any) => {
      AddAiConfig(formData)
        .then((res: any) => {
          ElMessage.success(res.msg)
          initData()
          resolve()
        })
        .catch((error: any) => {
          reject(error)
        })
    })
  })
}

function editData(data: any) {
  addModalRef.value.showModal((formData: AiConfigForm) => {
    return new Promise((resolve: any, reject: any) => {
      UpdateAiConfig(formData)
        .then((res: any) => {
          ElMessage.success(res.msg)
          initData()
          resolve()
        })
        .catch((error: any) => {
          reject(error)
        })
    })
  }, data)
}

function toggleStatus(data: any) {
  data.statusLoading = true
  const action = data.status === 'ENABLE' ? DisableAiConfig : EnableAiConfig
  action({
    id: data.id
  })
    .then((res: any) => {
      data.statusLoading = false
      ElMessage.success(res.msg)
      initData(true)
    })
    .catch(() => {
      data.statusLoading = false
    })
}

function deleteData(data: any) {
  ElMessageBox.confirm('确定要删除这个AI配置吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      DeleteAiConfig({
        id: data.id
      })
        .then((res: any) => {
          ElMessage.success(res.msg)
          initData()
        })
        .catch(() => {
          ElMessage.error('删除失败')
        })
    })
    .catch(() => {
      // 用户取消删除
    })
}

function getModelTypeName(type: string) {
  return ModelTypeMap[type] || type
}

function getModelTypeColor(type: string) {
  return ModelTypeColorMap[type] || 'default'
}

function handleSizeChange(val: number) {
  tableConfig.pagination.pageSize = val
  tableConfig.pagination.currentPage = 1
  initData()
}

function handleCurrentChange(val: number) {
  tableConfig.pagination.currentPage = val
  initData()
}

onMounted(() => {
  initData()
})
</script>


