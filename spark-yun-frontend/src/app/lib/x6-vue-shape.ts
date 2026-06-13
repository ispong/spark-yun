import { createApp, h, type App as VueApp } from 'vue'
import { FunctionExt, Graph, Markup, Node, NodeView, ObjectExt, Registry, Scheduler } from '@antv/x6'

type VueComponent = any
type VueDefinition = VueComponent | ((this: Graph, node: Node) => VueComponent)

export const registry = Registry.create<VueDefinition>({
    type: 'vue component'
})

;(Graph as any).registerVueComponent = registry.register
;(Graph as any).unregisterVueComponent = registry.unregister

;(Graph.Hook.prototype as any).getVueComponent = function (node: VueShape): VueDefinition {
    const getVueComponent = this.options.getVueComponent
    if (typeof getVueComponent === 'function') {
        const ret = FunctionExt.call(getVueComponent, this.graph, node)
        if (ret != null) {
            return ret
        }
    }

    let ret = node.getComponent()
    if (typeof ret === 'string') {
        const component = registry.get(ret)
        if (component == null) {
            return registry.onNotFound(ret)
        }
        ret = component
    }

    return ret
}

export class VueShape extends Node {
    get component(): VueDefinition {
        return this.getComponent()
    }

    set component(val: VueDefinition) {
        this.setComponent(val)
    }

    getComponent(): VueDefinition {
        return this.store.get('component')
    }

    setComponent(component: VueDefinition, options = {}): this {
        if (component == null) {
            this.removeComponent(options)
        } else {
            this.store.set('component', component, options)
        }
        return this
    }

    removeComponent(options = {}): this {
        this.store.remove('component', options)
        return this
    }
}

function getMarkup(useForeignObject: boolean, primer = 'rect') {
    const markup: any[] = [
        {
            tagName: primer,
            selector: 'body'
        }
    ]

    if (useForeignObject) {
        markup.push(Markup.getForeignObjectMarkup())
    } else {
        markup.push({
            tagName: 'g',
            selector: 'content'
        })
    }

    markup.push({
        tagName: 'text',
        selector: 'label'
    })

    return markup
}

VueShape.config({
    view: 'vue-shape-view',
    markup: getMarkup(true),
    attrs: {
        body: {
            fill: 'none',
            stroke: 'none',
            refWidth: '100%',
            refHeight: '100%'
        },
        fo: {
            refWidth: '100%',
            refHeight: '100%'
        },
        label: {
            fontSize: 14,
            fill: '#333',
            refX: '50%',
            refY: '50%',
            textAnchor: 'middle',
            textVerticalAnchor: 'middle'
        }
    },
    propHooks(metadata: Record<string, any>) {
        if (metadata.markup == null) {
            const primer = metadata.primer
            const useForeignObject = metadata.useForeignObject

            if (primer != null || useForeignObject != null) {
                metadata.markup = getMarkup(useForeignObject !== false, primer)

                if (primer) {
                    metadata.attrs = metadata.attrs || {}
                    let attrs = {}

                    if (primer === 'circle') {
                        attrs = {
                            refCx: '50%',
                            refCy: '50%',
                            refR: '50%'
                        }
                    } else if (primer === 'ellipse') {
                        attrs = {
                            refCx: '50%',
                            refCy: '50%',
                            refRx: '50%',
                            refRy: '50%'
                        }
                    }

                    if (primer !== 'rect') {
                        metadata.attrs = ObjectExt.merge({}, metadata.attrs, {
                            body: Object.assign({ refWidth: null, refHeight: null }, attrs)
                        })
                    }
                }
            }
        }

        return metadata
    }
})

Node.registry.register('vue-shape', VueShape, true)

export class VueShapeView extends NodeView<VueShape> {
    private vm: VueApp<Element> | null = null

    protected init(): void {
        super.init()
    }

    getComponentContainer(): HTMLDivElement {
        return this.selectors.foContent as HTMLDivElement
    }

    confirmUpdate(flag: number): number {
        const ret = super.confirmUpdate(flag)
        return this.handleAction(ret, VueShapeView.action, () => {
            Scheduler.scheduleTask(() => {
                this.renderVueComponent()
            })
        })
    }

    protected renderVueComponent(): void {
        this.unmountVueComponent()

        const root = this.getComponentContainer()
        const node = this.cell
        const graph = this.graph

        if (!root) {
            return
        }

        const component = this.graph.hook.getVueComponent(node)
        this.vm = createApp({
            render() {
                return h(component, { graph, node })
            },
            provide() {
                return {
                    getGraph: () => graph,
                    getNode: () => node
                }
            }
        })
        this.vm.mount(root)
    }

    protected unmountVueComponent(): HTMLDivElement {
        const root = this.getComponentContainer()
        if (this.vm) {
            this.vm.unmount()
            this.vm = null
        }
        root.innerHTML = ''
        return root
    }

    unmount(): this {
        this.unmountVueComponent()
        super.unmount()
        return this
    }
}

export namespace VueShapeView {
    export const action = 'vue'
}

VueShapeView.config({
    bootstrap: [VueShapeView.action],
    actions: {
        component: VueShapeView.action
    }
})

NodeView.registry.register('vue-shape-view', VueShapeView, true)
