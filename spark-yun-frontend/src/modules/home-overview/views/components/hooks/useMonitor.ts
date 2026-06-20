import { computed, onUnmounted, ref, type Ref } from 'vue'
import { queryClusterMonitorInfo } from '../../services/computer-group'
import type { ColonyInfo } from '../component'
import type { Frequency } from './useFrequency'

export interface MonitorInfo {
    type: 'cpuPercent' | 'usedMemorySize' | 'diskIoReadWriteSpeed' | 'networkIoReadWriteSpeed' | 'usedStorageSize'
    name: string
    value: number
    unit: '%' | 'GB' | 'KB/s' | 'MB/s'
    color: string
    data: Array<number>
}

export function useMonitor(currentColony: Ref<ColonyInfo | undefined>, currentFrequency: Ref<Frequency | undefined>) {
    const dateTimeList = ref<Array<string>>([])
    const cpuMonitorDataList = ref<Array<number>>([])
    const memoryMonitorDataList = ref<Array<number>>([])
    const diskIoMonitorList = ref<Array<number>>([])
    const networkIoMonitorList = ref<Array<number>>([])
    const storageMonitorDataList = ref<Array<number>>([])
    const currentInfo = ref<Record<MonitorInfo['type'], number>>({
        cpuPercent: 0,
        usedMemorySize: 0,
        diskIoReadWriteSpeed: 0,
        networkIoReadWriteSpeed: 0,
        usedStorageSize: 0
    })

    const monitorDataList = computed<Array<MonitorInfo>>(() => {
        return [
            {
                type: 'cpuPercent',
                name: 'CPU',
                value: currentInfo.value.cpuPercent || 0,
                unit: '%',
                color: '#2a82e485',
                data: cpuMonitorDataList.value
            },
            {
                type: 'usedMemorySize',
                name: '内存',
                value: currentInfo.value.usedMemorySize || 0,
                unit: 'GB',
                color: '#FF8D1A85',
                data: memoryMonitorDataList.value
            },
            {
                type: 'usedStorageSize',
                name: '存储',
                value: currentInfo.value.usedStorageSize || 0,
                unit: 'GB',
                color: '#D4303085',
                data: storageMonitorDataList.value
            },
            {
                type: 'diskIoReadWriteSpeed',
                name: 'IO读写',
                value: currentInfo.value.diskIoReadWriteSpeed || 0,
                unit: 'MB/s',
                color: '#D580FF85',
                data: diskIoMonitorList.value
            },
            {
                type: 'networkIoReadWriteSpeed',
                name: '网络IO',
                value: currentInfo.value.networkIoReadWriteSpeed || 0,
                unit: 'MB/s',
                color: '#14B8A685',
                data: networkIoMonitorList.value
            }
        ]
    })

    function parseMonitorData(data?: string) {
        return data ? parseFloat(data) : 0
    }

    function queryMonitorData() {
        if (!currentColony.value || !currentFrequency.value) {
            return
        }

        queryClusterMonitorInfo({
            clusterId: currentColony.value.id,
            timeType: currentFrequency.value.value
        }).then(({ data }) => {
            const timeList: string[] = []
            const cpuPercentList: number[] = []
            const usedMemorySizeList: number[] = []
            const diskIoWriteSpeedList: number[] = []
            const networkIoReadWriteSpeedList: number[] = []
            const usedStorageSizeList: number[] = []

            data.line.forEach((clusterMonitorInfo) => {
                timeList.push(clusterMonitorInfo.dateTime)

                cpuPercentList.push(parseMonitorData(clusterMonitorInfo.cpuPercent))
                usedMemorySizeList.push(parseMonitorData(clusterMonitorInfo.usedMemorySize))
                diskIoWriteSpeedList.push(
                    parseMonitorData(clusterMonitorInfo.diskIoReadSpeed) +
                        parseMonitorData(clusterMonitorInfo.diskIoWriteSpeed)
                )
                networkIoReadWriteSpeedList.push(
                    parseMonitorData(clusterMonitorInfo.networkIoReadSpeed) +
                        parseMonitorData(clusterMonitorInfo.networkIoWriteSpeed)
                )
                usedStorageSizeList.push(parseMonitorData(clusterMonitorInfo.usedStorageSize))
            })

            dateTimeList.value = timeList
            cpuMonitorDataList.value = cpuPercentList
            memoryMonitorDataList.value = usedMemorySizeList
            diskIoMonitorList.value = diskIoWriteSpeedList
            networkIoMonitorList.value = networkIoReadWriteSpeedList
            storageMonitorDataList.value = usedStorageSizeList

            currentInfo.value = {
                cpuPercent: cpuMonitorDataList.value[cpuMonitorDataList.value.length - 1],
                usedMemorySize: memoryMonitorDataList.value[memoryMonitorDataList.value.length - 1],
                diskIoReadWriteSpeed: diskIoMonitorList.value[diskIoMonitorList.value.length - 1],
                networkIoReadWriteSpeed: networkIoMonitorList.value[networkIoMonitorList.value.length - 1],
                usedStorageSize: storageMonitorDataList.value[storageMonitorDataList.value.length - 1]
            }
        })
    }

    const timer = setInterval(queryMonitorData, 60 * 1000)

    onUnmounted(() => {
        clearInterval(timer)
    })

    return {
        monitorDataList,
        dateTimeList,

        queryMonitorData
    }
}
