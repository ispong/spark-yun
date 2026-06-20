<template>
    <div class="zqy-flow">
        <section class="section-cot">
            <div ref="containerRef" class="flow-graph-container">
                <div id="draw-cot" />
            </div>
            <!-- 后续动态注入 -->
            <div class="status-container">
                <span class="status-tag status-SUCCESS">成功</span>
                <span class="status-tag status-PENDING">等待中</span>
                <span class="status-tag status-RUNNING">运行中</span>
                <span class="status-tag status-BREAK">中断</span>
                <span class="status-tag status-FAIL">失败</span>
                <span class="status-tag status-ABORT">已中止</span>
                <span class="status-tag status-ABORTING">中止中</span>
            </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, createVNode, ref, defineEmits } from 'vue'
import { Graph, Path, Addon } from '@antv/x6'
import CustomNode from './custom-node.vue'

let _Graph: any
let dnd: any
let pendingCellData: any = null

const runningStatus = ref(false)
const hideGridStatus = ref(false)
const containerRef = ref<HTMLElement>()

const emit = defineEmits(['refresh'])

function initGraph() {
    Graph.registerNode(
        'dag-node',
        {
            inherit: 'vue-shape',
            width: 180,
            height: 36,
            component: {
                render: () => {
                    return createVNode(CustomNode)
                }
            },
            ports: {
                groups: {
                    top: {
                        position: 'top',
                        attrs: {
                            circle: {
                                r: 4,
                                magnet: true,
                                stroke: '#C2C8D5',
                                strokeWidth: 1,
                                fill: '#fff'
                            }
                        }
                    },
                    bottom: {
                        position: 'bottom',
                        attrs: {
                            circle: {
                                r: 4,
                                magnet: true,
                                stroke: '#C2C8D5',
                                strokeWidth: 1,
                                fill: '#fff'
                            }
                        }
                    }
                }
            }
        },
        true
    )
    Graph.registerEdge(
        'dag-edge',
        {
            inherit: 'edge',
            attrs: {
                line: {
                    stroke: '#c2c8d5',
                    strokeWidth: 1,
                    targetMarker: {
                        name: 'block',
                        width: 6,
                        height: 4
                    }
                }
            }
        },
        true
    )
    Graph.registerConnector(
        'algo-connector',
        (s, e) => {
            const offset = 4
            const deltaY = Math.abs(e.y - s.y)
            const control = Math.floor((deltaY / 3) * 2)

            const v1 = {
                x: s.x,
                y: s.y + offset + control
            }
            const v2 = {
                x: e.x,
                y: e.y - offset - control
            }

            return Path.normalize(
                `M ${s.x} ${s.y}
             L ${s.x} ${s.y + offset}
             C ${v1.x} ${v1.y} ${v2.x} ${v2.y} ${e.x} ${e.y - offset}
             L ${e.x} ${e.y}
            `
            )
        },
        true
    )
    _Graph = new Graph({
        grid: {
            size: 10,
            visible: true,
            type: 'dot', // 'dot' | 'fixedDot' | 'mesh'
            args: {
                color: '#e1e6f0', // 网格线/点颜色
                thickness: 1 // 网格线宽度/网格点大小
            }
        },
        background: {
            color: '#fff' // 设置画布背景颜色
        },
        container: containerRef.value,
        panning: {
            enabled: true,
            eventTypes: ['leftMouseDown', 'mouseWheel']
        },
        mousewheel: {
            enabled: true,
            modifiers: 'ctrl',
            factor: 1.1,
            maxScale: 1.5,
            minScale: 0.5
        },
        highlighting: {
            magnetAdsorbed: {
                name: 'stroke',
                args: {
                    attrs: {
                        fill: '#fff',
                        stroke: '#31d0c6',
                        strokeWidth: 4
                    }
                }
            }
        },
        connecting: {
            snap: true,
            allowBlank: false,
            allowLoop: false,
            highlight: true,
            allowMulti: true,
            connector: 'algo-connector',
            connectionPoint: 'anchor',
            sourceAnchor: 'bottom',
            targetAnchor: 'top',
            allowNode: false,
            allowEdge: false,
            validateMagnet({ magnet }) {
                // if (!hideGridStatus.value) {
                //     return false
                // } else {
                //     return magnet.getAttribute('port-group') !== 'top'
                // }
                return magnet.getAttribute('port-group') !== 'top'
            },
            createEdge() {
                return _Graph.createEdge({
                    shape: 'dag-edge',
                    attrs: {
                        line: {
                            // strokeDasharray: '5 5'
                        }
                    },
                    zIndex: -1
                })
            }
        },
        selecting: {
            enabled: true,
            multiple: true,
            rubberEdge: true,
            rubberNode: true,
            modifiers: 'shift',
            rubberband: true
        },
        keyboard: true,
        clipboard: true,
        history: true,
        autoResize: true
    })
    dnd = new Addon.Dnd({
        target: _Graph,
        getDragNode: (node: any) =>
            node.clone({
                keepId: true
            }),
        getDropNode: (node: any) =>
            node.clone({
                keepId: true
            }),
        validateNode() {
            return true
        }
    })
    _Graph.on('node:mouseenter', ({ node }) => {
        if (!runningStatus.value && !hideGridStatus.value) {
            node.addTools({
                name: 'button-remove',
                args: {
                    x: 144, // 160最右边
                    y: 13,
                    // x: 170,
                    // y: 0,
                    // offset: { x: 15, y: 5 },
                    offset: {
                        x: 10,
                        y: 10
                    }
                }
            })
        }
    })
    _Graph.on('node:mouseleave', ({ node }) => {
        node.removeTools()
    })
    _Graph.on('edge:mouseenter', ({ edge }) => {
        if (!runningStatus.value && !hideGridStatus.value) {
            edge.addTools({
                name: 'button-remove',
                args: {
                    distance: '50%'
                }
            })
        }
    })
    _Graph.on('edge:mouseleave', ({ edge }) => {
        edge.removeTools()
    })
    _Graph.bindKey('backspace', () => {
        if (!runningStatus.value && !hideGridStatus.value) {
            const cells = _Graph.getSelectedCells()
            if (cells.length) {
                _Graph.removeCells(cells)
            }
        }
    })
}

// 添加节点
function addNodeFn(item: any, e: any) {
    if (!_Graph || !dnd) {
        return
    }
    const node = _Graph.createNode({
        id: item.id,
        shape: 'dag-node',
        data: {
            name: item.name,
            nodeConfigData: item
        },
        ports: [
            {
                id: '1-2',
                group: 'top'
            },
            {
                id: '1-1',
                group: 'bottom'
            }
        ]
    })
    dnd.start(node, e)
}

// 获取所有节点以及连线的数据
function getAllCellData() {
    if (!_Graph) {
        return []
    }
    return _Graph.getCells()
}

// 选中某一个边
function selectNodeEvent(nodeId: string) {
    if (!_Graph) {
        return
    }
    _Graph.resetSelection(nodeId)
}

// 根据给定的数据结构渲染流程图
function initCellList(data: any) {
    if (!_Graph) {
        pendingCellData = data
        return
    }
    if (data) {
        _Graph.fromJSON(data)
        _Graph.centerContent()
    }
}

// 运行刚开始时，后端节点实例可能还没生成，先让画布立即进入等待态。
function markFlowPending() {
    if (!_Graph) {
        return
    }
    runningStatus.value = true
    _Graph.getNodes().forEach((node: any) => {
        const data = node.getData()
        node.setData({
            ...data,
            workInstanceId: '',
            status: 'PENDING',
            isRunning: true
        })
    })
}

// 更新节点状态
function updateFlowStatus(statusList: Array<any>, isRunning: boolean) {
    if (!_Graph) {
        return
    }
    runningStatus.value = isRunning
    const statusMap = new Map((statusList || []).map((item: any) => [item.workId, item]))
    statusMap.forEach((item: any) => {
        const node = _Graph.getCellById(item.workId)
        if (!node) {
            return
        }
        const data = node.getData()
        node.setData({
            ...data,
            workInstanceId: item.workInstanceId,
            status: item.runStatus,
            isRunning: isRunning
        })
    })
    if (!isRunning) {
        _Graph.getNodes().forEach((node: any) => {
            if (statusMap.has(node.id)) {
                return
            }
            const data = node.getData()
            if (!['PENDING', 'RUNNING', 'ABORTING'].includes(data?.status)) {
                return
            }
            node.setData({
                ...data,
                workInstanceId: '',
                status: '',
                isRunning: false
            })
        })
    }
}

// 设置是否隐藏网格以及工具---运行中
function hideGrid(status: boolean) {
    if (!_Graph) {
        return
    }
    hideGridStatus.value = status
    if (status) {
        _Graph.hideGrid()
        _Graph.hideTools()
    } else {
        _Graph.showGrid()
        _Graph.showTools()
    }
}

function zoomIn() {
    if (!_Graph) {
        return
    }
    _Graph.zoom(0.2)
}
function zoomOut() {
    if (!_Graph) {
        return
    }
    _Graph.zoom(-0.2)
}
function locationCenter() {
    if (!_Graph) {
        return
    }
    _Graph.centerContent()
}
function refresh() {
    emit('refresh')
}
function locationContentCenter() {
    if (!_Graph) {
        return
    }
    _Graph.center()
}

onMounted(async () => {
    await import('@antv/x6-vue-shape')
    if (containerRef.value) {
        initGraph()
    }
    if (pendingCellData !== null) {
        initCellList(pendingCellData)
        pendingCellData = null
    }
})

onUnmounted(() => {
    _Graph?.dispose?.()
    _Graph = null
    dnd = null
    pendingCellData = null
})

defineExpose({
    addNodeFn,
    getAllCellData,
    initCellList,
    markFlowPending,
    updateFlowStatus,
    hideGrid,
    selectNodeEvent,
    zoomIn,
    zoomOut,
    refresh,
    locationCenter,
    locationContentCenter
})
</script>

<style lang="scss" scoped>
$--status-SUCCESS: #52c41a;
$--status-PENDING: #f5b041;
$--status-BREAK: #3f3a24;
$--status-FAIL: #ff4d4f;
$--status-ABORT: #9f26e1;
$--status-ABORTING: #b2b2b2;
$--status-RUNNING: #1890ff;

.zqy-flow {
    height: calc(100vh - 106px);

    .my-selecting {
        border: 1px solid red;
        display: block;
        z-index: 0;
    }

    .x6-node-selected .node {
        border-color: #1890ff;
        border-radius: 2px;
        box-shadow: 0 0 0 4px #d4e8fe;
    }

    .x6-node-selected .node.success {
        border-color: #52c41a;
        border-radius: 2px;
        box-shadow: 0 0 0 4px #ccecc0;
    }

    .x6-node-selected .node.failed {
        border-color: #ff4d4f;
        border-radius: 2px;
        box-shadow: 0 0 0 4px #fedcdc;
    }

    .x6-edge:hover path:nth-child(2) {
        stroke: #1890ff;
        stroke-width: 1px;
    }

    .x6-edge-selected path:nth-child(2) {
        stroke: #1890ff;
        stroke-width: 1.5px !important;
    }

    .section-cot {
        width: 100%;
        height: 100%;
        display: flex;
        position: relative;

        .status-container {
            position: absolute;
            bottom: 8px;
            left: 20px;
            display: flex;
            flex-direction: column;
            padding: 6px 10px 6px 16px;
            border: 1px solid rgba(220, 223, 230, 0.72);
            border-radius: 4px;
            background-color: rgba(255, 255, 255, 0.72);
            box-shadow: 0 2px 8px rgba(31, 35, 41, 0.04);
            opacity: 0.72;
            pointer-events: auto;
            transition:
                opacity 0.15s ease,
                background-color 0.15s ease,
                box-shadow 0.15s ease;

            &:hover {
                background-color: rgba(255, 255, 255, 0.94);
                box-shadow: 0 4px 12px rgba(31, 35, 41, 0.08);
                opacity: 1;
            }

            .status-tag {
                font-size: 12px;
                position: relative;
                margin: 2px 0;
                line-height: 16px;
                &::before {
                    content: '';
                    width: 6px;
                    height: 6px;
                    border-radius: 4px;
                    position: absolute;
                    left: -10px;
                    top: 5px;
                }
            }
            .status {
                &-SUCCESS {
                    color: $--status-SUCCESS;
                    &::before {
                        background-color: $--status-SUCCESS;
                    }
                }
                &-PENDING {
                    color: $--status-PENDING;
                    &::before {
                        background-color: $--status-PENDING;
                    }
                }
                &-BREAK {
                    color: $--status-BREAK;
                    &::before {
                        background-color: $--status-BREAK;
                    }
                }
                &-FAIL {
                    color: $--status-FAIL;
                    &::before {
                        background-color: $--status-FAIL;
                    }
                }
                &-ABORT {
                    color: $--status-ABORT;
                    &::before {
                        background-color: $--status-ABORT;
                    }
                }
                &-ABORTING {
                    color: $--status-ABORTING;
                    &::before {
                        background-color: $--status-ABORTING;
                    }
                }
                &-RUNNING {
                    color: $--status-RUNNING;
                    &::before {
                        background-color: $--status-RUNNING;
                    }
                }
            }
        }
    }

    .section-cot .flow-graph-container {
        height: 100% !important;
        position: relative;
        flex: 1;
    }

    .section-cot .flow-graph-container #draw-cot {
        width: 100%;
        height: 100%;
    }

    ::-webkit-scrollbar {
        width: 0;
    }
}

@keyframes running-line {
    to {
        stroke-dashoffset: -1000;
    }
}

@keyframes spin {
    from {
        transform: rotate(0deg);
    }

    to {
        transform: rotate(360deg);
    }
}
</style>
