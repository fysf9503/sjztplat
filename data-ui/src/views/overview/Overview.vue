<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">{{ eyebrow }}</div>
      <h1 class="dp-page-title">{{ title }}</h1>
      <p class="dp-page-desc">{{ desc }}</p>
    </div>

    <div class="dp-stat-grid">
      <StatCard
        v-for="stat in statCards"
        :key="stat.label"
        :label="stat.label"
        :value="stat.value"
        :unit="stat.unit"
        :icon="stat.icon"
        :color="stat.color"
        :bg="stat.bg"
        :trend="stat.trend"
      />
    </div>

    <div class="dp-grid-2">
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">数据接入量趋势（近 30 天）</div>
        </div>
        <EChart :option="accessTrendOption" height="280px" />
      </div>
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">质量合格率趋势（近 30 天）</div>
        </div>
        <EChart :option="qualityTrendOption" height="280px" />
      </div>
    </div>

    <div class="dp-card">
      <div class="dp-card-header">
        <div class="dp-card-title">核心模块能力</div>
        <span class="dp-card-extra">覆盖数据全生命周期管理</span>
      </div>
      <div class="ov-module-grid">
        <div v-for="mod in modules" :key="mod.title" class="ov-module-card dp-fade-up">
          <div class="ov-module-head">
            <div class="ov-module-icon" :style="{ background: mod.bg, color: mod.color }">
              <el-icon :size="22"><component :is="mod.icon" /></el-icon>
            </div>
            <div>
              <div class="ov-module-title">{{ mod.title }}</div>
              <div class="ov-module-desc">{{ mod.desc }}</div>
            </div>
          </div>
          <div class="ov-module-features">
            <div v-for="feat in mod.features" :key="feat" class="ov-feature-item">
              <el-icon class="ov-feature-check" color="#00b42a"><CircleCheck /></el-icon>
              <span>{{ feat }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { CircleCheck, Upload, CircleCheck as CheckIcon, Monitor, Location, Grid, Connection } from '@element-plus/icons-vue'
import StatCard from '@/components/StatCard.vue'
import EChart from '@/components/EChart.vue'
import { trend } from '@/mock'

const eyebrow = 'PLATFORM OVERVIEW'
const title = '平台总览'
const desc = '围绕数据接入、质量管控、数据开发、数据地图、数据建模、数据服务、报表平台、智能体平台和系统管理，汇总当前能力结构与核心资产规模。'

const statCards = [
  { label: '数据源池', value: 11, unit: '个', icon: 'Coin', color: '#165dff', bg: '#e8f3ff', trend: 8 },
  { label: '接入任务', value: 72, unit: '个', icon: 'Upload', color: '#00b42a', bg: '#e8ffea', trend: 12 },
  { label: '质量规则', value: 3, unit: '条', icon: 'CircleCheck', color: '#ff7d00', bg: '#fff3e8', trend: -2 },
  { label: '开发编排', value: 2, unit: '个', icon: 'Share', color: '#722ed1', bg: '#f5e8ff', trend: 5 },
  { label: '建模资产', value: 5, unit: '个', icon: 'Grid', color: '#14c9c9', bg: '#e0fffa', trend: 3 },
  { label: '服务发布', value: 6, unit: '个', icon: 'Promotion', color: '#f53f3f', bg: '#ffece8', trend: 10 },
  { label: '智能体', value: 2, unit: '个', icon: 'MagicStick', color: '#f759ab', bg: '#ffe8f4', trend: 1 }
]

const modules = [
  {
    title: '数据接入',
    desc: '多源异构数据统一接入与同步',
    icon: 'Upload',
    color: '#165dff',
    bg: '#e8f3ff',
    features: ['数据源管理与探测', '批量与实时同步任务', '文件上传与解析']
  },
  {
    title: '质量管控',
    desc: '全链路数据质量监控与治理',
    icon: 'CircleCheck',
    color: '#00b42a',
    bg: '#e8ffea',
    features: ['质量规则配置', '质量策略与调度', '质量结果分析']
  },
  {
    title: '数据开发',
    desc: '可视化数据加工与任务编排',
    icon: 'Monitor',
    color: '#722ed1',
    bg: '#f5e8ff',
    features: ['SQL 工作台', '数据处理流程', '工作流编排']
  },
  {
    title: '数据地图',
    desc: '资产目录与血缘关系可视化',
    icon: 'Location',
    color: '#14c9c9',
    bg: '#e0fffa',
    features: ['资产目录检索', '数据血缘分析', '影响分析评估']
  },
  {
    title: '数据建模',
    desc: '从业务概念到物理模型的全流程',
    icon: 'Grid',
    color: '#ff7d00',
    bg: '#fff3e8',
    features: ['逻辑模型设计', '物理模型生成', '模型部署与同步']
  },
  {
    title: '数据服务',
    desc: '数据能力 API 化对外供给',
    icon: 'Connection',
    color: '#f53f3f',
    bg: '#ffece8',
    features: ['服务目录管理', '服务开发与测试', '授权与审计']
  }
]

const accessTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, top: 20, bottom: 30 },
  xAxis: { type: 'category', data: trend.access.map(d => d.date), axisLine: { lineStyle: { color: '#e5e6eb' } }, axisLabel: { color: '#86909c' } },
  yAxis: { type: 'value', name: '万行', nameTextStyle: { color: '#86909c' }, axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#f2f3f5' } }, axisLabel: { color: '#86909c' } },
  series: [{
    name: '接入数据量', type: 'line', smooth: true, symbol: 'none',
    data: trend.access.map(d => d.value),
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(22,93,255,0.25)' }, { offset: 1, color: 'rgba(22,93,255,0)' }] } },
    lineStyle: { color: '#165dff', width: 2 }
  }]
}))

const qualityTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, top: 20, bottom: 30 },
  xAxis: { type: 'category', data: trend.quality.map(d => d.date), axisLine: { lineStyle: { color: '#e5e6eb' } }, axisLabel: { color: '#86909c' } },
  yAxis: { type: 'value', name: '%', nameTextStyle: { color: '#86909c' }, min: 90, axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#f2f3f5' } }, axisLabel: { color: '#86909c' } },
  series: [{
    name: '质量合格率', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
    data: trend.quality.map(d => d.value),
    itemStyle: { color: '#00b42a' },
    areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(0,180,42,0.2)' }, { offset: 1, color: 'rgba(0,180,42,0)' }] } },
    lineStyle: { color: '#00b42a', width: 2 }
  }]
}))
</script>

<style scoped>
.ov-module-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.ov-module-card {
  border: 1px solid var(--dp-border);
  border-radius: 10px;
  padding: 18px;
  background: #fafbfc;
  transition: all 0.25s;
}

.ov-module-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.1);
  transform: translateY(-2px);
  background: #fff;
}

.ov-module-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
}

.ov-module-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ov-module-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 2px;
}

.ov-module-desc {
  font-size: 12px;
  color: var(--dp-text-3);
}

.ov-module-features {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px dashed var(--dp-border);
}

.ov-feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--dp-text-2);
}

.ov-feature-check {
  flex-shrink: 0;
}

@media (max-width: 1200px) {
  .ov-module-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .ov-module-grid {
    grid-template-columns: 1fr;
  }
}
</style>
