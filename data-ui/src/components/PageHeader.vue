<template>
  <div class="dp-page-header dp-fade-up">
    <div>
      <h2 class="dp-page-header-title">{{ title }}</h2>
      <p v-if="desc" class="dp-page-header-desc">{{ desc }}</p>
    </div>
    <div class="dp-page-header-actions"><slot /></div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  desc: { type: String, default: '' }
})
</script>

<style scoped>
.dp-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.dp-page-header-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--dp-text-1);
}
.dp-page-header-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--dp-text-3);
}
</style>