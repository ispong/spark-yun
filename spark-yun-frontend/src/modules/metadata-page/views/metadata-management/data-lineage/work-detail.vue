<template>
    <BlockDrawer :drawer-config="drawerConfig">
        <!-- <LoadingPage :visible="loading"> -->
        <div class="workflow-page">
            <div class="flow-container flow-container__lineage">
                <spark-jar
                    v-if="workConfig.workType === 'SPARK_JAR' || workConfig.workType === 'FLINK_JAR'"
                    :work-item-config="workConfig"
                    :work-flow-data="workFlowData"
                    :disabled="true"
                />
                <WorkApi
                    v-if="workConfig.workType === 'API'"
                    :work-item-config="workConfig"
                    :work-flow-data="workFlowData"
                    :disabled="true"
                />
                <ApiSync
                    v-if="workConfig.workType === 'API_SYNC_JDBC'"
                    :work-item-config="workConfig"
                    :disabled="true"
                />
                <WorkItem
                    v-if="
                        !['SPARK_JAR', 'API_SYNC_JDBC', 'DATA_SYNC_JDBC', 'EXCEL_SYNC_JDBC', 'DB_MIGRATE'].includes(
                            workConfig.workType
                        )
                    "
                    :work-item-config="workConfig"
                    :work-flow-data="workFlowData"
                    :disabled="true"
                />
                <data-sync
                    v-if="workConfig.workType === 'DATA_SYNC_JDBC'"
                    :work-item-config="workConfig"
                    :disabled="true"
                />
                <ExcelImport
                    v-if="workConfig.workType === 'EXCEL_SYNC_JDBC'"
                    :work-item-config="workConfig"
                    :disabled="true"
                />
                <DatabaseMigrate
                    v-if="workConfig.workType === 'DB_MIGRATE'"
                    :work-item-config="workConfig"
                    :disabled="true"
                />
            </div>
        </div>
        <!-- </LoadingPage> -->
    </BlockDrawer>
</template>

<script lang="ts" setup>
import { computed, nextTick, reactive, ref } from 'vue'
import BlockDrawer from '@/components/block-drawer/index.vue'

import sparkJar from '@/modules/workflow/views/spark-jar/index.vue'
import WorkApi from '@/modules/workflow/views/work-api/index.vue'
import ApiSync from '@/modules/workflow/views/api-sync/index.vue'
import DataSync from '@/modules/workflow/views/data-sync/index.vue'
import WorkItem from '@/modules/workflow/views/work-item/index.vue'
import DatabaseMigrate from '@/modules/workflow/views/database-migrate/index.vue'
import ExcelImport from '@/modules/workflow/views/excel-import/index.vue'

import { GetWorkItemConfig } from '@/modules/workflow/api'

interface WorkConfig {
    workType: string
    id: string
}

interface WorkFlowData {
    name: string
    id: string
}

const workConfig = ref<WorkConfig>()
const workFlowData = ref<WorkFlowData>()
const loading = ref(false)
const networkError = ref(false)

const drawerConfig = reactive({
    title: '作业详情',
    visible: false,
    width: '685px',
    cancelConfig: {
        title: '取消',
        cancel: closeEvent,
        disabled: false
    },
    customClass: 'work-detail-modal',
    zIndex: 1100,
    closeOnClickModal: false
})

function showModal(data: any) {
    workConfig.value = {
        workType: data.workType,
        id: data.workId
    }
    workFlowData.value = {
        name: data.name,
        id: data.workId
    }
    drawerConfig.title = `作业详情-${workFlowData.value.name}`
    drawerConfig.visible = true
}

function okEvent() {}

function closeEvent() {
    drawerConfig.visible = false
}

defineExpose({
    showModal
})
</script>

<style lang="scss">
.work-detail-modal {
    .workflow-page {
        height: calc(100% - 0px);
        overflow: auto;
        .flow-container__lineage {
            width: 100%;
        }
    }
}
</style>
