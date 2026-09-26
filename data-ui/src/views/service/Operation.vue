<template>
  <div class="dp-page operation-page">
    <PageHeader title="服务运营总览" desc="全方位监控数据服务运行状态，实时掌握调用趋势、成功率与异常告警">
      <el-radio-group v-model="timeRange" size="default" @change="handleTimeRangeChange">
        <el-radio-button value="today">今日</el-radio-button>
        <el-radio-button value="7d">近7天</el-radio-button>
        <el-radio-button value="30d">近30天</el-radio-button>
        <el-radio-button value="90d">近90天</el-radio-button>
      </el-radio-group>
      <el-button :icon="Refresh" :loading="loading" @click="refreshData">刷新</el-button>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="stat-cards-row">
      <div class="stat-card dp-fade-up" style="--card-color: var(--dp-primary); --card-bg: var(--dp-primary-light)">
        <div class="stat-card-icon">
          <el-icon :size="22"><Grid /></el-icon>
        </div>
        <div class="stat-card-info">
          <div class="stat-card-label">API总数</div>
          <div class="stat-card-value">{{ statData.apiTotal }}<span class="stat-card-unit">个</span></div>
          <div class="stat-card-trend up">
            <el-icon><Top /></el-icon>
            {{ statData.apiTrend }}%<span class="trend-label">较上周</span>
          </div>
        </div>
      </div>
      <div class="stat-card dp-fade-up" style="--card-color: var(--dp-purple); --card-bg: var(--dp-purple-light); animation-delay: 0.05s">
        <div class="stat-card-icon">
          <el-icon :size="22"><DataLine /></el-icon>
        </div>
        <div class="stat-card-info">
          <div class="stat-card-label">调用总量</div>
          <div class="stat-card-value">{{ formatNumber(statData.callTotal) }}<span class="stat-card-unit">次</span></div>
          <div class="stat-card-trend up">
            <el-icon><Top /></el-icon>
            {{ statData.callTrend }}%<span class="trend-label">较上周</span>
          </div>
        </div>
      </div>
      <div class="stat-card dp-fade-up" style="--card-color: var(--dp-success); --card-bg: var(--dp-success-light); animation-delay: 0.1s">
        <div class="stat-card-icon">
          <el-icon :size="22"><CircleCheck /></el-icon>
        </div>
        <div class="stat-card-info">
          <div class="stat-card-label">成功率</div>
          <div class="stat-card-value">{{ statData.successRate }}<span class="stat-card-unit">%</span></div>
          <div class="stat-card-trend up">
            <el-icon><Top /></el-icon>
            {{ statData.successTrend }}%<span class="trend-label">较上周</span>
          </div>
        </div>
      </div>
      <div class="stat-card dp-fade-up" style="--card-color: var(--dp-warning); --card-bg: var(--dp-warning-light); animation-delay: 0.15s">
        <div class="stat-card-icon">
          <el-icon :size="22"><Clock /></el-icon>
        </div>
        <div class="stat-card-info">
          <div class="stat-card-label">平均响应时间</div>
          <div class="stat-card-value">{{ statData.avgResponse }}<span class="stat-card-unit">ms</span></div>
          <div class="stat-card-trend down">
            <el-icon><Bottom /></el-icon>
            {{ statData.responseTrend }}%<span class="trend-label">较上周</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="chart-grid">
      <div class="dp-card chart-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">调用趋势</div>
          <div class="dp-card-extra">
            <el-radio-group v-model="trendType" size="small" @change="updateTrendChart">
              <el-radio-button value="call">调用量</el-radio-button>
              <el-radio-button value="success">成功率</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <EChart :option="trendChartOption" height="280px" />
      </div>
      <div class="dp-card chart-card dp-fade-up" style="animation-delay: 0.05s">
        <div class="dp-card-header">
          <div class="dp-card-title">API 调用排行 TOP10</div>
        </div>
        <EChart :option="rankChartOption" height="280px" />
      </div>
    </div>

    <div class="chart-grid">
      <div class="dp-card chart-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">调用方应用分布</div>
        </div>
        <EChart :option="appDistChartOption" height="300px" />
      </div>
      <div class="dp-card chart-card dp-fade-up" style="animation-delay: 0.05s">
        <div class="dp-card-header">
          <div class="dp-card-title">异常告警</div>
          <div class="dp-card-extra">
            <el-badge :value="alertList.length" class="alert-badge" :max="99">
              <el-button link type="primary" size="small">查看全部</el-button>
            </el-badge>
          </div>
        </div>
        <div class="alert-list">
          <div v-for="(item, index) in alertList" :key="index" class="alert-item" :class="item.level">
            <div class="alert-dot"></div>
            <div class="alert-content">
              <div class="alert-title">{{ item.title }}</div>
              <div class="alert-desc">{{ item.desc }}</div>
            </div>
            <div class="alert-time">{{ item.time }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 最近调用记录 -->
    <div class="dp-card dp-fade-up">
      <div class="dp-card-header">
        <div class="dp-card-title">最近调用记录</div>
        <div class="dp-card-extra">
          <el-button link type="primary" size="small" @click="goToAudit">查看全部</el-button>
        </div>
      </div>
      <el-table :data="recentCalls" border stripe size="default">
        <el-table-column prop="time" label="调用时间" width="170" />
        <el-table-column prop="app" label="应用名称" width="120" />
        <el-table-column prop="apiName" label="API名称" min-width="160" />
        <el-table-column prop="path" label="请求路径" min-width="220" class-name="dp-mono">
          <template #default="{ row }">
            <span class="method-tag" :class="row.method.toLowerCase()">{{ row.method }}</span>
            <span class="path-text">{{ row.path }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cost" label="响应时间(ms)" width="120" sortable>
          <template #default="{ row }">
            <span :class="{ 'slow-response': row.cost > 300 }">{{ row.cost }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态码" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="请求来源IP" width="140" class-name="dp-mono" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EChart from '@/components/EChart.vue'
import { services, serviceApps, auditLogs, trend } from '@/mock'
import { Refresh } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const timeRange = ref('30d')
const trendType = ref('call')

const statData = reactive({
  apiTotal: 86,
  apiTrend: 12.5,
  callTotal: 2586420,
  callTrend: 18.3,
  successRate: 99.6,
  successTrend: 0.8,
  avgResponse: 86,
  responseTrend: 5.2
})

const alertList = ref([
  { level: 'danger', title: 'api_order_detail_3 调用异常', desc: '5分钟内错误率达到 15.2%，已触发熔断', time: '2分钟前' },
  { level: 'warning', title: '营销中台应用 QPS 超限', desc: '当前 QPS 480，已达限流阈值 96%', time: '8分钟前' },
  { level: 'warning', title: 'api_customer_query_1 响应变慢', desc: '平均响应时间 520ms，超过告警阈值', time: '15分钟前' },
  { level: 'danger', title: 'ods_customer_db 数据源连接异常', desc: '连接池耗尽，影响 5 个 API 服务', time: '23分钟前' },
  { level: 'info', title: 'api_stat_report_7 发布成功', desc: '新版本 v2.1.0 已上线，自动灰度中', time: '1小时前' }
])

const recentCalls = ref([])

function formatNumber(num) {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toLocaleString()
}

function statusTagType(status) {
  if (status === 200) return 'success'
  if (status === 429) return 'warning'
  if (status === 500) return 'danger'
  return 'info'
}

function goToAudit() {
  router.push('/service/audit')
}

function handleTimeRangeChange() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    updateStats()
    updateTrendChart()
    ElMessage.success('数据已更新')
  }, 500)
}

function refreshData() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    updateStats()
    updateTrendChart()
    ElMessage.success('刷新成功')
  }, 800)
}

function updateStats() {
  const multiplier = { today: 0.03, '7d': 0.25, '30d': 1, '90d': 3.2 }[timeRange.value] || 1
  statData.callTotal = Math.round(2586420 * multiplier)
  statData.apiTotal = Math.round(86 + Math.random() * 5)
  statData.successRate = +(99.3 + Math.random() * 0.6).toFixed(1)
  statData.avgResponse = Math.round(80 + Math.random() * 20)
}

const trendChartOption = computed(() => {
  const days = { today: 24, '7d': 7, '30d': 30, '90d': 90 }[timeRange.value] || 30
  const labels = trend.calls.slice(0, days).map(d => d.date)
  const callData = trend.calls.slice(0, days).map(d => d.value)
  const successData = trend.calls.slice(0, days).map(() => +(99 + Math.random() * 0.8).toFixed(2))

  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e6eb',
      borderWidth: 1,
      textStyle: { color: '#1d2129' }
    },
    legend: {
      data: trendType.value === 'call' ? ['调用量'] : ['成功率(%)'],
      bottom: 0,
      textStyle: { color: '#4e5969', fontSize: 12 }
    },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#86909c', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f2f3f5', type: 'dashed' } },
      axisLabel: { color: '#86909c', fontSize: 11 }
    },
    series: trendType.value === 'call'
      ? [{
          name: '调用量',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          showSymbol: false,
          data: callData,
          lineStyle: { color: '#1664ff', width: 2 },
          itemStyle: { color: '#1664ff' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(22, 100, 255, 0.25)' },
                { offset: 1, color: 'rgba(22, 100, 255, 0.02)' }
              ]
            }
          }
        }]
      : [{
          name: '成功率(%)',
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 6,
          showSymbol: false,
          data: successData,
          lineStyle: { color: '#00b42a', width: 2 },
          itemStyle: { color: '#00b42a' },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0, y: 0, x2: 0, y2: 1,
              colorStops: [
                { offset: 0, color: 'rgba(0, 180, 42, 0.2)' },
                { offset: 1, color: 'rgba(0, 180, 42, 0.02)' }
              ]
            }
          }
        }]
  }
})

const rankChartOption = computed(() => {
  const topApis = [...services]
    .sort((a, b) => b.callsToday - a.callsToday)
    .slice(0, 10)
    .reverse()

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e6eb',
      borderWidth: 1,
      textStyle: { color: '#1d2129' }
    },
    grid: { left: 140, right: 40, top: 10, bottom: 20 },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f2f3f5', type: 'dashed' } },
      axisLabel: { color: '#86909c', fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: topApis.map(s => s.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#4e5969', fontSize: 11, width: 130, overflow: 'truncate' }
    },
    series: [{
      type: 'bar',
      barMaxWidth: 14,
      data: topApis.map((s, i) => ({
        value: s.callsToday,
        itemStyle: {
          color: i >= 7
            ? { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#f53f3f' }, { offset: 1, color: '#ff7875' }] }
            : i >= 4
            ? { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#ff7d00' }, { offset: 1, color: '#ffb366' }] }
            : { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [{ offset: 0, color: '#1664ff' }, { offset: 1, color: '#5b8def' }] },
          borderRadius: [0, 3, 3, 0]
        }
      })),
      label: {
        show: true,
        position: 'right',
        fontSize: 11,
        color: '#4e5969',
        formatter: p => p.value.toLocaleString()
      }
    }]
  }
})

const appDistChartOption = computed(() => {
  const appData = serviceApps.slice(0, 6).map((app, i) => ({
    name: app.name,
    value: app.callsMonth
  }))
  const colors = ['#1664ff', '#00b42a', '#ff7d00', '#722ed1', '#0fc6c2', '#f759ab']

  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e6eb',
      borderWidth: 1,
      textStyle: { color: '#1d2129' },
      formatter: '{b}<br/>调用量: {c} 次 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { color: '#4e5969', fontSize: 12 },
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 12
    },
    color: colors,
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 4,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: { show: false },
      emphasis: {
        label: {
          show: true,
          fontSize: 13,
          fontWeight: 'bold',
          color: '#1d2129'
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      },
      labelLine: { show: false },
      data: appData
    }]
  }
})

function updateTrendChart() {
  // computed will handle it
}

onMounted(() => {
  recentCalls.value = auditLogs.slice(0, 10).map(log => ({
    ...log,
    apiName: log.service.split('/').pop() + '_' + Math.floor(Math.random() * 20)
  }))
})
</script>

<style scoped>
.operation-page {
  min-height: 100%;
}

.stat-cards-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: var(--dp-radius);
  padding: 18px 20px;
  border: 1px solid var(--dp-border-light);
  box-shadow: var(--dp-shadow-sm);
  display: flex;
  align-items: flex-start;
  gap: 14px;
  transition: all 0.25s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.stat-card-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: var(--card-bg);
  color: var(--card-color);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card-info {
  flex: 1;
  min-width: 0;
}

.stat-card-label {
  font-size: 13px;
  color: var(--dp-text-3);
  margin-bottom: 6px;
}

.stat-card-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--dp-text-1);
  line-height: 1.2;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
  letter-spacing: -0.5px;
}

.stat-card-unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--dp-text-3);
  margin-left: 4px;
}

.stat-card-trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  margin-top: 6px;
  font-weight: 500;
}

.stat-card-trend.up {
  color: var(--dp-success);
}

.stat-card-trend.down {
  color: var(--dp-danger);
}

.trend-label {
  color: var(--dp-text-3);
  margin-left: 4px;
  font-weight: 400;
}

.chart-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.chart-card {
  margin-bottom: 0;
}

.method-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  font-family: Consolas, 'JetBrains Mono', monospace;
  margin-right: 8px;
  vertical-align: middle;
}

.method-tag.get {
  background: var(--dp-success-light);
  color: var(--dp-success);
}

.method-tag.post {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
}

.method-tag.put {
  background: var(--dp-warning-light);
  color: var(--dp-warning);
}

.method-tag.delete {
  background: var(--dp-danger-light);
  color: var(--dp-danger);
}

.path-text {
  vertical-align: middle;
  font-size: 12.5px;
}

.slow-response {
  color: var(--dp-warning);
  font-weight: 600;
}

/* 告警列表 */
.alert-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--dp-bg-page);
  transition: all 0.2s;
}

.alert-item:hover {
  background: var(--dp-primary-bg);
}

.alert-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}

.alert-item.danger .alert-dot {
  background: var(--dp-danger);
  box-shadow: 0 0 0 3px var(--dp-danger-light);
}

.alert-item.warning .alert-dot {
  background: var(--dp-warning);
  box-shadow: 0 0 0 3px var(--dp-warning-light);
}

.alert-item.info .alert-dot {
  background: var(--dp-primary);
  box-shadow: 0 0 0 3px var(--dp-primary-light);
}

.alert-content {
  flex: 1;
  min-width: 0;
}

.alert-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-1);
  margin-bottom: 3px;
}

.alert-desc {
  font-size: 12px;
  color: var(--dp-text-3);
  line-height: 1.5;
}

.alert-time {
  font-size: 11px;
  color: var(--dp-text-4);
  flex-shrink: 0;
  padding-top: 2px;
}

.alert-badge :deep(.el-badge__content) {
  border: none;
  background: var(--dp-danger);
}

@media (max-width: 1400px) {
  .stat-cards-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1100px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
