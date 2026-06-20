<template>
    <div class="monitor-chart">
        <div class="monitor-chart__header">
            <span class="monitor-chart__title">{{ monitorData.name }}</span>
            <span v-if="!isEmpty" class="monitor-chart__active">{{ monitorData.value + monitorData.unit }}</span>
        </div>
        <div class="monitor-chart__body">
            <div v-if="!isEmpty" ref="chartContainerRef" class="monitor-chart__container" />
            <empty-page v-else />
        </div>
    </div>
</template>

<script setup lang="ts">
import { MonitorInfo } from './hooks/useMonitor'
import { computed, nextTick, onMounted, onUnmounted, ref, watch, defineEmits } from 'vue'
import * as echarts from 'echarts/core'
import { TooltipComponent, TooltipComponentOption, GridComponent, GridComponentOption } from 'echarts/components'
import { LineChart, LineSeriesOption } from 'echarts/charts'
import { UniversalTransition } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'

import allScreen from '@/app/assets/imgs/fullScreen.svg'

echarts.use([TooltipComponent, GridComponent, LineChart, CanvasRenderer, UniversalTransition])

type EChartsOption = echarts.ComposeOption<TooltipComponentOption | GridComponentOption | LineSeriesOption>

const props = withDefaults(
    defineProps<{
        monitorData: MonitorInfo
        dateTimeList: Array<string>
        hideFull: boolean
    }>(),
    {}
)

const chartVm = ref<echarts.ECharts>()
const chartContainerRef = ref<HTMLDivElement>()

const emit = defineEmits(['showDetailEvent'])

const isEmpty = computed(() => {
    return props.monitorData.data.length === 0
})

const options = computed<EChartsOption>(() => {
    const status = !props?.hideFull
    const gridBottom = props?.hideFull ? '16%' : '0'
    return {
        tooltip: {
            trigger: 'item',
            axisPointer: {
                type: 'cross',
                label: {
                    backgroundColor: '#6a7985'
                }
            }
        },
        toolbox: {
            show: status,
            top: 0,
            right: 18,
            feature: {
                myFullScreen: {
                    title: '',
                    icon: 'image://'.concat(allScreen),
                    onclick: () => {
                        emit('showDetailEvent')
                    }
                }
            }
        },
        grid: {
            top: '18%',
            left: '4%',
            right: '8%',
            bottom: gridBottom,
            containLabel: true
        },
        xAxis: {
            type: 'category',
            boundaryGap: false,
            offset: 16,
            data: props.dateTimeList,
            axisLine: {
                show: false
            },
            axisTick: {
                show: false
            }
        },
        yAxis: {
            type: 'value',
            axisLabel: {
                formatter(value) {
                    return value + props.monitorData?.unit
                }
            }
        },
        series: [
            {
                name: props.monitorData?.type,
                type: 'line',
                areaStyle: {},
                smooth: true,
                color: props.monitorData?.color,
                data: props.monitorData?.data
            }
        ]
    }
})

watch(
    () => options.value,
    (val) => {
        if (chartVm.value) {
            chartVm.value.setOption(options.value)
        }
    }
)

watch(
    () => isEmpty.value,
    (newVal) => {
        if (!newVal && chartContainerRef.value) {
            chartVm.value = echarts.init(chartContainerRef.value)
            chartVm.value.setOption(options.value)
        }
    },
    {
        flush: 'post'
    }
)

function resizeChart() {
    nextTick(() => {
        chartVm.value?.resize()
    })
}

onMounted(() => {
    if (chartContainerRef.value) {
        chartVm.value = echarts.init(chartContainerRef.value)
        chartVm.value.setOption(options.value)

        window.addEventListener('resize', resizeChart)
    }
})

onUnmounted(() => {
    window.removeEventListener('resize', resizeChart)
})
</script>

<style scoped lang="scss">
.monitor-chart {
    margin-top: 12px;
    height: 204px;
    border: 1px solid getCssVar('border-color', 'lighter');
    border-radius: 6px;
    background-color: getCssVar('color', 'white');
    box-shadow: 0 1px 4px rgb(15 23 42 / 4%);
    box-sizing: border-box;
    padding: 10px 12px 12px;
    transition:
        border-color 0.18s ease,
        box-shadow 0.18s ease;

    &:hover {
        border-color: getCssVar('border-color');
        box-shadow: 0 4px 12px rgb(15 23 42 / 8%);
    }

    .monitor-chart__body {
        display: flex;
        align-items: center;
        justify-content: center;
        height: calc(100% - 36px);
        position: relative;
    }

    .monitor-chart__title {
        color: getCssVar('text-color', 'regular');
        font-size: getCssVar('font-size', 'base');
        font-weight: 600;
    }

    .monitor-chart__active {
        color: getCssVar('color', 'primary');
        font-size: getCssVar('font-size', 'base');
        font-weight: 600;
    }

    .monitor-chart__header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 36px;
    }

    .monitor-chart__container {
        width: 100%;
        height: 100%;
    }

    .monitor-chart__empty {
        padding: 0;

        --el-empty-image-width: 60px;
    }
}
</style>
