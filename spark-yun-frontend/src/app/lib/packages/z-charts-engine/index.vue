<template>
    <div class="z-charts-engine">
        <ChartsChoose
            v-if="renderSence !== 'readonly'"
            :charts-list="chartsList"
            :show-report-components-btn="showReportComponentsBtn"
            @start-move-event="startMoveEvent"
            @end-move-event="endMoveEvent"
            @get-chart-list-event="getChartListEvent"
            @preview-chat-event="previewChatEvent"
            @go-report-components-event="goReportComponentsEvent"
        />
        <ChartsComponents
            ref="chartComponentsRef"
            class="charts-edit-container"
            :render-sence="renderSence"
            :chart-list="componentList"
            :get-preview-option="getPreviewOption"
            :get-real-data-option="getRealDataOption"
        />
    </div>
</template>

<script lang="ts" setup>
import { ref, defineProps, defineEmits } from 'vue'
import ChartsChoose from './charts-choose/index.vue'
import ChartsComponents from './charts-components/charts-grid-layout.vue'

const props = defineProps<{
    chartsList?: any
    renderSence?: any
    componentList?: any
    getPreviewOption?: any
    getRealDataOption?: any
    showReportComponentsBtn?: any
}>()
const emit = defineEmits(['getChartListEvent', 'previewChatEvent', 'goReportComponentsEvent'])

const chartComponentsRef = ref()

function startMoveEvent(e: any) {
    chartComponentsRef.value.startMoveEvent(e)
}

function endMoveEvent(e: any) {
    chartComponentsRef.value.endMoveEvent(e)
}

function resizeAllCharts() {
    chartComponentsRef.value.resizeAllCharts()
}

function getChartListEvent(e: string) {
    emit('getChartListEvent', e)
}
function previewChatEvent(e: any) {
    emit('previewChatEvent', e)
}

function goReportComponentsEvent() {
    emit('goReportComponentsEvent')
}

function downloadLog() {
    const logStr = ''
    const blob = new Blob([logStr], {
        type: 'text/plain;charset=utf-8'
    })
    const objectURL = URL.createObjectURL(blob)
    const aTag = document.createElement('a')
    aTag.href = objectURL
    aTag.download = '日志.log'
    aTag.click()
    URL.revokeObjectURL(objectURL)
}

// 获取到组件
function getComponentList() {
    return chartComponentsRef.value.getComponentList()
}

defineExpose({
    resizeAllCharts,
    getComponentList
})
</script>

<style lang="scss">
.z-charts-engine {
    display: flex;
    width: 100%;
    height: 100%;
    .charts-edit-container {
        width: 100%;
    }
    .charts-config-container {
        width: 200px;
        border-left: 1px solid #b2b2b2;
        height: 100%;
    }
}
</style>
