<template>
  <div class="dp-page">
    <PageHeader title="开发总览" desc="围绕SQL开发、工作流编排和模型管理，提供数据开发全流程概览与监控">
      <el-radio-group v-model="timeRange" size="default">
        <el-radio-button value="7">近7天</el-radio-button>
        <el-radio-button value="14">近14天</el-radio-button>
        <el-radio-button value="30">近30天</el-radio-button>
      </el-radio-group>
      <el-button :icon="Refresh" @click="refreshData" :loading="loading">刷新</el-button>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="ov-stat-grid">
      <StatCard
        label="开发任务数"
        :value="statData.taskCount"
        unit="个"
        icon="List"
        color="var(--dp-primary)"
        bg="var(--dp-primary-light)"
        :trend="statData.taskTrend"
      />
      <StatCard
        label="编排流程数"
        :value="statData.flowCount"
        unit="个"
        icon="Share"
        color="var(--dp-purple)"
        bg="var(--dp-purple-light)"
        :trend="statData.flowTrend"
      />
      <StatCard
        label="SQL脚本数"
        :value="statData.scriptCount"
        unit="个"
        icon="Document"
        color="var(--dp-success)"
        bg="var(--dp-success-light)"
        :trend="statData.scriptTrend"
      />
      <StatCard
        label="执行成功率"
        :value="statData.successRate"
        unit="%"
        icon="CircleCheck"
        color="var(--dp-cyan)"
        bg="var(--dp-cyan-light)"
        :trend="statData.successTrend"
      />
    </div>

    <!-- 图表区域 -->
    <div class="ov-grid-2">
      <!-- 任务执行趋势 -->
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">任务执行趋势</div>
          <div class="dp-card-extra">近{{ timeRange }}天</div>
        </div>
        <EChart :option="trendOption" height="300px" />
      </div>

      <!-- 资源使用情况 -->
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">资源使用情况</div>
          <div class="dp-card-extra">实时</div>
        </div>
        <div class="ov-resource">
          <div v-for="r in resources" :key="r.name" class="ov-res-item">
            <div class="ov-ring" :style="{ '--ring-color': r.color }">
              <svg viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="42" fill="none" stroke="#f0f1f5" stroke-width="8" />
                <circle
                  cx="50" cy="50" r="42" fill="none"
                  :stroke="r.color" stroke-width="8" stroke-linecap="round"
                  :stroke-dasharray="263.89"
                  :stroke-dashoffset="263.89 * (1 - r.value / 100)"
                  transform="rotate(-90 50 50)"
                />
              </svg>
              <div class="ov-ring-value">
                <span class="ov-ring-num">{{ r.value }}</span>
                <span class="ov-ring-unit">%</span>
              </div>
            </div>
            <div class="ov-res-info">
              <div class="ov-res-name">{{ r.name }}</div>
              <div class="ov-res-desc">{{ r.used }} / {{ r.total }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 下方区域 -->
    <div class="ov-grid-2">
      <!-- 最近开发任务 -->
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">最近开发任务</div>
          <div class="dp-card-extra">
            <el-button link type="primary" @click="router.push('/develop/processes')">查看全部</el-button>
          </div>
        </div>
        <el-table :data="recentTasks" border stripe size="small">
          <el-table-column prop="name" label="任务名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="90">
            <template #default="{ row }">
              <span class="dp-tag" :class="typeTagClass(row.type)">{{ row.type }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="owner" label="负责人" width="80" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <span class="ov-status">
                <span class="ov-status-dot" :class="statusDotClass(row.status)"></span>
                {{ row.status }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="150" />
        </el-table>
      </div>

      <!-- 热门脚本排行 -->
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">热门脚本排行</div>
          <div class="dp-card-extra">按执行次数</div>
        </div>
        <EChart :option="hotScriptOption" height="300px" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import EChart from '@/components/EChart.vue'
import { dataProcesses, sqlScripts } from '@/mock'
import { genTrend } from '@/mock/helpers'
import { Refresh } from '@element-plus/icons-vue'

const router = useRouter()
const timeRange = ref('30')
const loading = ref(false)

const statData = ref({
  taskCount: 128,
  taskTrend: 8.5,
  flowCount: 36,
  flowTrend: 12.3,
  scriptCount: 256,
  scriptTrend: 5.7,
  successRate: 96.8,
  successTrend: 2.1
})

const resources = ref([
  { name: 'CPU使用率', value: 68, used: '27.2核', total: '40核', color: '#1664ff' },
  { name: '内存使用率', value: 75, used: '240GB', total: '320GB', color: '#00b42a' },
  { name: '存储使用率', value: 62, used: '12.4TB', total: '20TB', color: '#ff7d00' }
])

const recentTasks = computed(() => dataProcesses.slice(0, 6))

const typeTagClass = type => ({
  'SQL任务': 'primary',
  '脚本任务': 'warning',
  '数据同步': 'success',
  '混合流程': 'purple'
}[type] || 'info')

const statusDotClass = status => ({
  '运行中': 'running',
  '已发布': 'success',
  '开发中': 'warning',
  '已暂停': 'paused'
}[status] || '')

const trendOption = computed(() => {
  const days = parseInt(timeRange.value)
  const trendData = genTrend(days, 80, 30, v => Math.round(v))
  return {
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['成功', '失败', '运行中'],
      right: 0,
      top: 0
    },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: trendData.map(d => d.date),
      axisLine: { lineStyle: { color: '#e5e6eb' } },
      axisLabel: { color: '#86909c', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#86909c', fontSize: 11 }
    },
    series: [
      {
        name: '成功',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: trendData.map(d => Math.round(d.value * 0.85)),
        lineStyle: { color: '#00b42a', width: 2 },
        itemStyle: { color: '#00b42a' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(0,180,42,0.2)' },
              { offset: 1, color: 'rgba(0,180,42,0)' }
            ]
          }
        }
      },
      {
        name: '失败',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: trendData.map(d => Math.round(d.value * 0.08)),
        lineStyle: { color: '#f53f3f', width: 2 },
        itemStyle: { color: '#f53f3f' }
      },
      {
        name: '运行中',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        data: trendData.map(d => Math.round(d.value * 0.12)),
        lineStyle: { color: '#1664ff', width: 2 },
        itemStyle: { color: '#1664ff' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(22,100,255,0.15)' },
              { offset: 1, color: 'rgba(22,100,255,0)' }
            ]
          }
        }
      }
    ]
  }
})

const hotScriptOption = computed(() => {
  const scripts = sqlScripts.slice(0, 8).map(s => ({
    name: s.name.replace('.sql', ''),
    value: Math.floor(Math.random() * 500) + 100
  })).sort((a, b) => a.value - b.value)

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: '{b}: {c} 次'
    },
    grid: { left: 120, right: 30, top: 10, bottom: 20 },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#86909c', fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: scripts.map(s => s.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#4e5969', fontSize: 12 }
    },
    series: [{
      type: 'bar',
      data: scripts.map((s, i) => ({
        value: s.value,
        itemStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
            colorStops: [
              { offset: 0, color: '#1664ff' },
              { offset: 1, color: '#0fc6c2' }
            ]
          },
          borderRadius: [0, 4, 4, 0]
        }
      })),
      barWidth: 16,
      label: {
        show: true,
        position: 'right',
        color: '#4e5969',
        fontSize: 11
      }
    }]
  }
})

function refreshData() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    // 模拟数据更新
    statData.value = {
      taskCount: Math.floor(128 + Math.random() * 10),
      taskTrend: +(5 + Math.random() * 8).toFixed(1),
      flowCount: Math.floor(36 + Math.random() * 5),
      flowTrend: +(8 + Math.random() * 8).toFixed(1),
      scriptCount: Math.floor(256 + Math.random() * 20),
      scriptTrend: +(3 + Math.random() * 6).toFixed(1),
      successRate: +(95 + Math.random() * 4).toFixed(1),
      successTrend: +(1 + Math.random() * 3).toFixed(1)
    }
    resources.value = [
      { name: 'CPU使用率', value: Math.floor(60 + Math.random() * 20), used: '27.2核', total: '40核', color: '#1664ff' },
      { name: '内存使用率', value: Math.floor(65 + Math.random() * 20), used: '240GB', total: '320GB', color: '#00b42a' },
      { name: '存储使用率', value: Math.floor(55 + Math.random() * 15), used: '12.4TB', total: '20TB', color: '#ff7d00' }
    ]
    ElMessage.success('数据已刷新')
  }, 800)
}

onMounted(() => {
  // 初始化
})
</script>

<style scoped>
.ov-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.ov-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.ov-grid-2 > .dp-card {
  margin-bottom: 0;
}

/* 资源使用 */
.ov-resource {
  display: flex;
  justify-content: space-around;
  align-items: center;
  padding: 10px 0;
}

.ov-res-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.ov-ring {
  position: relative;
  width: 90px;
  height: 90px;
}

.ov-ring svg {
  width: 100%;
  height: 100%;
  transform: rotate(0deg);
}

.ov-ring-value {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: baseline;
  justify-content: center;
}

.ov-ring-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}

.ov-ring-unit {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-left: 2px;
}

.ov-res-name {
  font-size: 13px;
  color: var(--dp-text-2);
  text-align: center;
  font-weight: 500;
}

.ov-res-desc {
  font-size: 11px;
  color: var(--dp-text-3);
  text-align: center;
  margin-top: 2px;
}

/* 状态圆点 */
.ov-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--dp-text-2);
}

.ov-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--dp-text-4);
}

.ov-status-dot.running {
  background: var(--dp-primary);
  box-shadow: 0 0 0 3px rgba(22, 100, 255, 0.15);
  animation: ov-pulse 2s infinite;
}

.ov-status-dot.success {
  background: var(--dp-success);
}

.ov-status-dot.warning {
  background: var(--dp-warning);
}

.ov-status-dot.paused {
  background: var(--dp-text-4);
}

@keyframes ov-pulse {
  0%, 100% { box-shadow: 0 0 0 3px rgba(22, 100, 255, 0.15); }
  50% { box-shadow: 0 0 0 6px rgba(22, 100, 255, 0.08); }
}

@media (max-width: 1200px) {
  .ov-stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .ov-grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
