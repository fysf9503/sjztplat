<template>
  <div class="dp-stat-card dp-fade-up">
    <div class="dp-stat-icon" :style="{ background: bg, color: color }">
      <el-icon :size="20"><component :is="icon" /></el-icon>
    </div>
    <div class="dp-stat-body">
      <div class="dp-stat-label">{{ label }}</div>
      <div class="dp-stat-value">
        {{ value }}<span v-if="unit" class="dp-stat-unit">{{ unit }}</span>
      </div>
      <div v-if="trend !== undefined" class="dp-stat-trend" :class="trend >= 0 ? 'up' : 'down'">
        <el-icon><component :is="trend >= 0 ? 'Top' : 'Bottom'" /></el-icon>
        {{ Math.abs(trend) }}%<span class="dp-stat-trend-label">较上周</span>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  unit: { type: String, default: '' },
  icon: { type: String, default: 'DataLine' },
  color: { type: String, default: '#1664ff' },
  bg: { type: String, default: '#e8f0ff' },
  trend: { type: Number, default: undefined }
})
</script>

<style scoped>
.dp-stat-card {
  background: #fff;
  border: 1px solid var(--dp-border);
  border-radius: 6px;
  padding: 16px;
  display: flex;
  gap: 14px;
  align-items: flex-start;
}
.dp-stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.dp-stat-label { font-size: 13px; color: var(--dp-text-3); }
.dp-stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--dp-text-1);
  line-height: 1.3;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}
.dp-stat-unit { font-size: 12px; font-weight: 400; color: var(--dp-text-3); margin-left: 4px; }
.dp-stat-trend { font-size: 12px; display: flex; align-items: center; gap: 2px; margin-top: 2px; }
.dp-stat-trend.up { color: var(--dp-success); }
.dp-stat-trend.down { color: var(--dp-danger); }
.dp-stat-trend-label { color: var(--dp-text-3); margin-left: 4px; }
</style>