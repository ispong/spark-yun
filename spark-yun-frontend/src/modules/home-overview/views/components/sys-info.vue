<template>
    <div class="sys-info">
        <div class="sys-info__header">
            <span class="sys-info__title">资源总览</span>
            <div class="sys-info__ops">
                <el-icon class="sys-info__icon" @click="querySysInfoData">
                    <RefreshRight />
                </el-icon>
                <!-- <el-icon class="sys-info__icon"><Setting /></el-icon> -->
            </div>
        </div>
        <div class="sys-info__body">
            <template v-for="sysInfo in sysInfoData" :key="sysInfo.type">
                <sys-chart :chart-data="sysInfo" />
            </template>
        </div>
    </div>
</template>

<script setup lang="ts">
import SysChart from './sys-chart.vue'
import { ChartInfo } from './component'
import { onMounted, ref } from 'vue'
import { querySystemBaseInfo } from '../services/computer-group'

const sysInfoData = ref<Array<ChartInfo>>([
    {
        type: 'clusterMonitor',
        title: '计算集群',
        mix: undefined,
        total: 100,
        color: '#ED7B09'
    },
    {
        type: 'datasourceMonitor',
        title: '数据源',
        mix: undefined,
        total: 100,
        color: '#1967BF'
    },
    {
        type: 'successWorkflowInstanceMonitor',
        title: '作业流',
        mix: undefined,
        total: 100,
        color: '#4B19BF'
    },
    {
        type: 'successWorkInstanceMonitor',
        title: '作业',
        mix: undefined,
        total: 100,
        color: '#03A89D'
    }
])

function querySysInfoData() {
    querySystemBaseInfo().then((systemBaseInfo) => {
        sysInfoData.value = sysInfoData.value.map((info) => {
            const baseInfo = systemBaseInfo.data[info.type]

            if (baseInfo) {
                info.mix = baseInfo.activeNum
                info.total = baseInfo.total
            }

            return info
        })
    })
}

onMounted(() => {
    querySysInfoData()
})
</script>

<style lang="scss">
.sys-info {
    margin-bottom: 24px;

    .sys-info__header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        height: 32px;
    }

    .sys-info__body {
        display: grid;
        grid-template-columns: repeat(4, minmax(160px, 1fr));
        gap: 16px;
        margin-top: 12px;

        .sys-chart {
            min-width: 0;
        }
    }

    .sys-info__title {
        font-size: getCssVar('font-size', 'medium');
        font-weight: bold;
    }

    .sys-info__icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 28px;
        height: 28px;
        border-radius: 4px;
        cursor: pointer;
        color: getCssVar('text-color', 'regular');

        &:hover {
            color: getCssVar('color', 'primary');
            background-color: getCssVar('color', 'primary', 'light-9');
        }
    }
}

@media (max-width: 1440px) {
    .sys-info {
        .sys-info__body {
            grid-template-columns: repeat(2, minmax(180px, 1fr));
        }
    }
}
</style>
