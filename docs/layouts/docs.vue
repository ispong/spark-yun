<template>
  <div class="main">
    <LayoutHomeHeader />
    <slot />
  </div>
</template>

<script lang="ts" setup>
defineComponent("docs");
definePageMeta({});
</script>

<style>
.main {
  background-image: linear-gradient(
      to right,
      rgba(255, 255, 255, 0) 0%,
      rgba(255, 255, 255, 1) 20%,
      rgba(255, 255, 255, 0) 300%
    ),
    url(~/assets/img/tg-bg.jpeg);
  background-repeat: no-repeat;
  background-size: cover;
}
</style>
