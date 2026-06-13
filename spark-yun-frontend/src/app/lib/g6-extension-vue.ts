import { HTML } from '@antv/g6'
import { render as renderVue, type VNode } from 'vue'

export const vue_core_mark = '__vue_app__'

export type AppContainer = Element & {
    [vue_core_mark]?: boolean | null
}

export function render(component: VNode | (() => VNode), container: AppContainer): AppContainer {
    const vnode = typeof component === 'function' ? component() : component
    renderVue(vnode, container)
    container[vue_core_mark] = true
    return container
}

export function unmount(container: AppContainer): AppContainer {
    renderVue(null, container)
    container[vue_core_mark] = null
    return container
}

export class VueNode extends HTML {
    getKeyStyle(attributes: any): any {
        return {
            ...super.getKeyStyle(attributes)
        }
    }

    update(attr?: any): void {
        super.update(attr)
    }

    connectedCallback(): void {
        super.connectedCallback()
        render(this.attributes.component, this.getDomElement() as AppContainer)
    }

    attributeChangedCallback(name: string, oldValue: any, newValue: any): void {
        super.attributeChangedCallback(name, oldValue, newValue)
        if (name === 'component' && oldValue !== newValue) {
            render(this.attributes.component, this.getDomElement() as AppContainer)
        }
    }

    destroy(): void {
        unmount(this.getDomElement() as AppContainer)
        super.destroy()
    }
}
