<template>
    <div class="computer-group">
        <div class="computer-group__left">
            <sys-info />
            <vm-chart />
            <vm-list />
        </div>
        <div class="computer-group__right">
            <monitor-info />
        </div>
    </div>
</template>

<script lang="ts" setup>
import VmChart from './components/vm-chart.vue'
import VmList from './components/vm-list.vue'
import MonitorInfo from './components/monitor-info.vue'
import SysInfo from './components/sys-info.vue'
</script>

<style lang="scss">
.computer-group {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 360px;
    gap: 24px;
    padding: 20px 24px 24px;
    overflow-x: hidden;
    overflow-y: auto;
    box-sizing: border-box;
    height: 100%;
    background-color: getCssVar('fill-color', 'lighter');

    &__left {
        min-width: 0;
    }

    &__right {
        min-width: 0;
    }
}

@media (max-width: 1280px) {
    .computer-group {
        grid-template-columns: 1fr;

        &__right {
            width: auto;
        }
    }
}
</style>
