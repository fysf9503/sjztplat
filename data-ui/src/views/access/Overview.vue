<template>
  <div class="dp-page">
    <PageHeader title="接入总览" desc="统一查看数据接入模块的运行状态、任务执行情况和数据同步趋势">
      <el-radio-group v-model="timeRange" size="small">
        <el-radio-button value="24h">近24小时</el-radio-button>
        <el-radio-button value="7d">近7天</el-radio-button>
        <el-radio-button value="30d">近30天</el-radio-button>
      </el-radio-group>
      <el-button :icon="Refresh" @click="handleRefresh" style="margin-left: 10px">刷新</el-button>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid dp-fade-up">
      <StatCard
        label="数据源总数"
        :value="stats.datasourceCount"
        unit="个"
        icon="Coin"
        color="#1664ff"
        bg="#e8f0ff"
        :trend="8"
      />
      <StatCard
        label="接入任务数"
        :value="stats.taskCount"
        unit="个"
        icon="Connection"
        color="#00b42a"
        bg="#e8ffea"
        :trend="12"
      />
      <StatCard
        label="今日同步数据量"
        :value="stats.todayDataVolume"
        unit="万条"
        icon="DataLine"
        color="#722ed1"
        bg="#f5e8ff"
        :trend="15"
      />
      <StatCard
        label="同步成功率"
        :value="stats.successRate"
        unit="%"
        icon="CircleCheck"
        color="#0fc6c2"
        bg="#e0fffa"
        :trend="2"
      />
    </div>

    <!-- 图表区域 -->
    <div class="chart-row dp-grid-2">
      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <h3 class="dp-card-title">接入数据量趋势</h3>
          <span class="dp-card-extra">近30天</span>
        </div>
        <EChart :option="trendChartOption" height="280px" />
      </div>

      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <h3 class="dp-card-title">数据源类型分布</h3>
          <span class="dp-card-extra">按接入数量统计</span>
        </div>
        <EChart :option="pieChartOption" height="280px" />
      </div>
    </div>

    <div class="chart-row">
      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <h3 class="dp-card-title">任务执行状态统计</h3>
          <span class="dp-card-extra">近7天</span>
        </div>
        <EChart :option="barChartOption" height="260px" />
      </div>
    </div>

    <!-- 最近任务列表 -->
    <div class="dp-card dp-fade-up">
      <div class="dp-card-header">
        <h3 class="dp-card-title">最近任务执行记录</h3>
        <span class="dp-card-extra">
          <el-link type="primary" :underline="false">查看全部</el-link>
        </span>
      </div>
      <el-table :data="recentTasks" border stripe style="width: 100%">
        <el-table-column prop="name" label="任务名称" min-width="200" />
        <el-table-column prop="mode" label="类型" width="80">
          <template #default="{ row }">
            <span class="ov-mode-tag" :class="row.mode.toLowerCase()">{{ row.mode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="源表" min-width="180" class-name="dp-mono" />
        <el-table-column prop="target" label="目标表" min-width="180" class-name="dp-mono" />
        <el-table-column prop="rows" label="同步行数" width="120" align="right">
          <template #default="{ row }">
            <span class="ov-rows">{{ formatNumber(row.rows) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="耗时" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="ov-status">
              <span class="ov-status-dot" :class="statusDotClass(row.status)"></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="lastRun" label="执行时间" width="160" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import EChart from '@/components/EChart.vue'
import { accessTasks, trend, dataSources } from '@/mock'

const timeRange = ref('30d')

const stats = ref({
  datasourceCount: 0,
  taskCount: 0,
  todayDataVolume: 0,
  successRate: 0
})

const recentTasks = ref([])

const trendChartOption = computed(() => {
  const data = trend.access
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e6eb',
      borderWidth: 1,
      textStyle: { color: '#1d2129' },
      formatter: params => {
        const p = params[0]
        return `${p.name}<br/>同步数据量：<b>${p.value.toLocaleString()}</b> 条`
      }
    },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date),
      axisLine: { lineStyle: { color: '#e5e6eb' } },
      axisLabel: { color: '#86909c', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: {
        color: '#86909c',
        fontSize: 11,
        formatter: v => v >= 10000 ? (v / 10000).toFixed(0) + 'w' : v
      }
    },
    series: [{
      type: 'line',
      data: data.map(d => d.value),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#1664ff', width: 2 },
      itemStyle: { color: '#1664ff', borderColor: '#fff', borderWidth: 2 },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(22, 100, 255, 0.2)' },
            { offset: 1, color: 'rgba(22, 100, 255, 0.02)' }
          ]
        }
      }
    }]
  }
})

const pieChartOption = computed(() => {
  const typeMap = {}
  dataSources.forEach(ds => {
    typeMap[ds.type] = (typeMap[ds.type] || 0) + 1
  })
  const data = Object.entries(typeMap).map(([name, value]) => ({ name, value }))
  const colors = ['#1664ff', '#00b42a', '#ff7d00', '#722ed1', '#0fc6c2', '#f759ab', '#86909c', '#14c9c9']
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e6eb',
      borderWidth: 1,
      textStyle: { color: '#1d2129' },
      formatter: '{b}: {c} 个 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { color: '#4e5969', fontSize: 12 }
    },
    color: colors,
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 14, fontWeight: 'bold' },
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.15)' }
      },
      data
    }]
  }
})

const barChartOption = computed(() => {
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const successData = [42, 38, 45, 40, 48, 25, 20]
  const failedData = [2, 3, 1, 4, 2, 1, 0]
  const runningData = [5, 4, 6, 3, 5, 2, 1]
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e6eb',
      borderWidth: 1,
      textStyle: { color: '#1d2129' },
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['成功', '失败', '运行中'],
      right: 10,
      top: 0,
      itemWidth: 12,
      itemHeight: 12,
      textStyle: { color: '#4e5969', fontSize: 12 }
    },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: days,
      axisLine: { lineStyle: { color: '#e5e6eb' } },
      axisLabel: { color: '#86909c', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#86909c', fontSize: 11 }
    },
    series: [
      {
        name: '成功',
        type: 'bar',
        data: successData,
        barWidth: 18,
        itemStyle: { color: '#00b42a', borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '失败',
        type: 'bar',
        data: failedData,
        barWidth: 18,
        itemStyle: { color: '#f53f3f', borderRadius: [4, 4, 0, 0] }
      },
      {
        name: '运行中',
        type: 'bar',
        data: runningData,
        barWidth: 18,
        itemStyle: { color: '#1664ff', borderRadius: [4, 4, 0, 0] }
      }
    ]
  }
})

function statusDotClass(status) {
  const map = {
    '成功': 'success',
    '失败': 'error',
    '运行中': 'running',
    '等待调度': 'waiting'
  }
  return map[status] || 'waiting'
}

function formatNumber(num) {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}

function handleRefresh() {
  ElMessage.success('数据已刷新')
}

onMounted(() => {
  stats.value = {
    datasourceCount: dataSources.length,
    taskCount: accessTasks.length,
    todayDataVolume: 1286,
    successRate: 98.5
  }
  recentTasks.value = accessTasks.slice(0, 10)
})
</script>

<style scoped>
.chart-row {
  margin-bottom: 16px;
}

.ov-mode-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 4px;
  font-weight: 500;
}
.ov-mode-tag.全量 { background: #e8f0ff; color: #1664ff; }
.ov-mode-tag.增量 { background: #e8ffea; color: #00b42a; }
.ov-mode-tag.实时 { background: #fff3e8; color: #ff7d00; }

.ov-rows {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--dp-text-2);
}

.ov-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.ov-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.ov-status-dot.success {
  background: var(--dp-success);
  box-shadow: 0 0 0 3px rgba(0, 180, 42, 0.15);
}
.ov-status-dot.error {
  background: var(--dp-danger);
  box-shadow: 0 0 0 3px rgba(245, 63, 63, 0.15);
}
.ov-status-dot.running {
  background: var(--dp-primary);
  box-shadow: 0 0 0 3px rgba(22, 100, 255, 0.15);
  animation: pulse 2s infinite;
}
.ov-status-dot.waiting {
  background: var(--dp-warning);
  box-shadow: 0 0 0 3px rgba(255, 125, 0, 0.15);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
</style>
