<template>
  <div class="dp-page">
    <PageHeader title="质量总览" desc="围绕质量规则、策略任务和问题分析形成质量检测与闭环治理链路。">
      <el-radio-group v-model="timeRange" size="default" @change="handleRefresh">
        <el-radio-button value="today">今日</el-radio-button>
        <el-radio-button value="7d">近7天</el-radio-button>
        <el-radio-button value="30d">近30天</el-radio-button>
      </el-radio-group>
      <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid overview-stat-grid">
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>质量规则总数</span>
          <div class="dp-stat-icon" style="background: var(--dp-primary-light); color: var(--dp-primary)">
            <el-icon :size="18"><List /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">
          {{ statData.ruleCount }}<span class="dp-stat-unit">条</span>
        </div>
        <div class="dp-stat-sub">
          <span class="dp-stat-trend" :class="statData.ruleTrend >= 0 ? 'up' : 'down'">
            <el-icon><component :is="statData.ruleTrend >= 0 ? 'Top' : 'Bottom'" /></el-icon>
            {{ Math.abs(statData.ruleTrend) }}%
          </span>
          <span>较上周</span>
        </div>
      </div>

      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>规则执行次数</span>
          <div class="dp-stat-icon" style="background: var(--dp-success-light); color: var(--dp-success)">
            <el-icon :size="18"><VideoPlay /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">
          {{ statData.execCount.toLocaleString() }}<span class="dp-stat-unit">次</span>
        </div>
        <div class="dp-stat-sub">
          <span class="dp-stat-trend" :class="statData.execTrend >= 0 ? 'up' : 'down'">
            <el-icon><component :is="statData.execTrend >= 0 ? 'Top' : 'Bottom'" /></el-icon>
            {{ Math.abs(statData.execTrend) }}%
          </span>
          <span>较上周</span>
        </div>
      </div>

      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>平均合格率</span>
          <div class="dp-stat-icon" style="background: var(--dp-cyan-light); color: var(--dp-cyan)">
            <el-icon :size="18"><CircleCheck /></el-icon>
          </div>
        </div>
        <div class="dp-flex dp-gap-12" style="align-items: center">
          <div class="dp-stat-value">
            {{ statData.avgPassRate }}<span class="dp-stat-unit">%</span>
          </div>
          <div class="ring-progress">
            <svg viewBox="0 0 36 36" class="circular-chart">
              <path class="circle-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
              <path class="circle" :stroke-dasharray="`${statData.avgPassRate}, 100`" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
            </svg>
          </div>
        </div>
        <div class="dp-stat-sub">
          <span class="dp-stat-trend" :class="statData.passTrend >= 0 ? 'up' : 'down'">
            <el-icon><component :is="statData.passTrend >= 0 ? 'Top' : 'Bottom'" /></el-icon>
            {{ Math.abs(statData.passTrend) }}%
          </span>
          <span>较上周</span>
        </div>
      </div>

      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>异常数据量</span>
          <div class="dp-stat-icon" style="background: var(--dp-danger-light); color: var(--dp-danger)">
            <el-icon :size="18"><Warning /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">
          {{ statData.anomalyCount.toLocaleString() }}<span class="dp-stat-unit">条</span>
        </div>
        <div class="dp-stat-sub">
          <span class="dp-stat-trend" :class="statData.anomalyTrend >= 0 ? 'up' : 'down'">
            <el-icon><component :is="statData.anomalyTrend >= 0 ? 'Top' : 'Bottom'" /></el-icon>
            {{ Math.abs(statData.anomalyTrend) }}%
          </span>
          <span>较上周</span>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="dp-grid-2">
      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">质量合格率趋势</div>
          <div class="dp-card-extra">近30天</div>
        </div>
        <EChart :option="trendOption" height="300px" />
      </div>

      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">规则类型分布</div>
        </div>
        <EChart :option="pieOption" height="300px" />
      </div>
    </div>

    <div class="dp-grid-2">
      <!-- 异常数据表 -->
      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">最近异常数据</div>
          <el-button link type="primary" size="small" @click="goToResults">查看全部</el-button>
        </div>
        <el-table :data="recentAnomalies" border stripe size="small">
          <el-table-column prop="dataId" label="数据ID" width="100" class-name="dp-mono" />
          <el-table-column prop="tableName" label="所属表" min-width="150" class-name="dp-mono" show-overflow-tooltip />
          <el-table-column prop="ruleName" label="异常规则" min-width="120" show-overflow-tooltip />
          <el-table-column prop="level" label="级别" width="70">
            <template #default="{ row }">
              <span class="level-dot" :class="row.level">
                <i></i>{{ row.level }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="foundTime" label="发现时间" width="150" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === '未处理' ? 'danger' : row.status === '处理中' ? 'warning' : 'success'" size="small" effect="plain">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 质量维度雷达图 -->
      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">质量维度得分</div>
        </div>
        <EChart :option="radarOption" height="300px" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EChart from '@/components/EChart.vue'
import { Refresh } from '@element-plus/icons-vue'

const router = useRouter()
const timeRange = ref('30d')
const loading = ref(false)

const statData = reactive({
  ruleCount: 128,
  ruleTrend: 12,
  execCount: 28456,
  execTrend: 8.5,
  avgPassRate: 96.8,
  passTrend: 2.1,
  anomalyCount: 1256,
  anomalyTrend: -5.3
})

// 生成近30天日期
function generateDates(days) {
  const dates = []
  const today = new Date()
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    dates.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  return dates
}

const dates30 = generateDates(30)

// 质量合格率趋势图
const trendOption = computed(() => {
  const baseValue = 94
  const generateLine = (offset, volatility) => {
    return dates30.map((_, i) => {
      const wave = Math.sin(i / 5 + offset) * volatility
      const noise = (Math.random() - 0.5) * volatility * 0.5
      return +(baseValue + offset + wave + noise).toFixed(1)
    })
  }

  return {
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['完整性', '准确性', '一致性', '唯一性'],
      top: 0,
      right: 0
    },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: dates30,
      axisLabel: { fontSize: 11, interval: 4 }
    },
    yAxis: {
      type: 'value',
      min: 85,
      max: 100,
      axisLabel: { formatter: '{value}%', fontSize: 11 }
    },
    series: [
      {
        name: '完整性', type: 'line', smooth: true, symbol: 'circle', symbolSize: 4,
        data: generateLine(3, 2),
        lineStyle: { color: '#1664ff', width: 2 },
        itemStyle: { color: '#1664ff' }
      },
      {
        name: '准确性', type: 'line', smooth: true, symbol: 'circle', symbolSize: 4,
        data: generateLine(1.5, 2.5),
        lineStyle: { color: '#00b42a', width: 2 },
        itemStyle: { color: '#00b42a' }
      },
      {
        name: '一致性', type: 'line', smooth: true, symbol: 'circle', symbolSize: 4,
        data: generateLine(0.5, 3),
        lineStyle: { color: '#ff7d00', width: 2 },
        itemStyle: { color: '#ff7d00' }
      },
      {
        name: '唯一性', type: 'line', smooth: true, symbol: 'circle', symbolSize: 4,
        data: generateLine(2, 1.5),
        lineStyle: { color: '#722ed1', width: 2 },
        itemStyle: { color: '#722ed1' }
      }
    ]
  }
})

// 规则类型分布饼图
const pieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
  legend: { bottom: 0, type: 'scroll', itemWidth: 10, itemHeight: 10 },
  series: [{
    type: 'pie',
    radius: ['50%', '72%'],
    center: ['50%', '42%'],
    avoidLabelOverlap: true,
    itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
    label: { formatter: '{b}\n{d}%', fontSize: 11, lineHeight: 16 },
    labelLine: { length: 8, length2: 8 },
    data: [
      { value: 38, name: '完整性', itemStyle: { color: '#1664ff' } },
      { value: 32, name: '准确性', itemStyle: { color: '#00b42a' } },
      { value: 24, name: '一致性', itemStyle: { color: '#ff7d00' } },
      { value: 18, name: '唯一性', itemStyle: { color: '#722ed1' } },
      { value: 10, name: '时效性', itemStyle: { color: '#0fc6c2' } },
      { value: 6, name: '有效性', itemStyle: { color: '#f759ab' } }
    ]
  }]
}))

// 最近异常数据
const recentAnomalies = ref([
  { dataId: 'ID-10234', tableName: 'dwd_trade_db.order_detail', ruleName: '非空校验', level: '高', foundTime: '2026-09-26 10:23', status: '未处理' },
  { dataId: 'ID-10233', tableName: 'ods_customer_db.customer_info', ruleName: '格式校验', level: '中', foundTime: '2026-09-26 09:45', status: '处理中' },
  { dataId: 'ID-10232', tableName: 'dwd_trade_db.payment_record', ruleName: '值域校验', level: '高', foundTime: '2026-09-26 09:12', status: '未处理' },
  { dataId: 'ID-10231', tableName: 'dim_public_db.product_sku', ruleName: '唯一性校验', level: '低', foundTime: '2026-09-26 08:30', status: '已处理' },
  { dataId: 'ID-10230', tableName: 'dws_summary_db.sale_day', ruleName: '一致性校验', level: '中', foundTime: '2026-09-26 08:05', status: '已处理' }
])

// 质量维度雷达图
const radarOption = computed(() => ({
  tooltip: {},
  radar: {
    indicator: [
      { name: '完整性', max: 100 },
      { name: '准确性', max: 100 },
      { name: '一致性', max: 100 },
      { name: '唯一性', max: 100 },
      { name: '时效性', max: 100 }
    ],
    radius: '65%',
    center: ['50%', '50%'],
    splitNumber: 4,
    axisName: { color: '#4e5969', fontSize: 12 },
    splitArea: {
      areaStyle: {
        color: ['rgba(22, 100, 255, 0.02)', 'rgba(22, 100, 255, 0.04)', 'rgba(22, 100, 255, 0.06)', 'rgba(22, 100, 255, 0.08)']
      }
    },
    axisLine: { lineStyle: { color: '#e5e6eb' } },
    splitLine: { lineStyle: { color: '#e5e6eb' } }
  },
  series: [{
    type: 'radar',
    data: [
      {
        value: [97, 95.5, 94.5, 96, 93],
        name: '质量得分',
        areaStyle: { color: 'rgba(22, 100, 255, 0.2)' },
        lineStyle: { color: '#1664ff', width: 2 },
        itemStyle: { color: '#1664ff' },
        symbol: 'circle',
        symbolSize: 6
      }
    ]
  }]
}))

function handleRefresh() {
  loading.value = true
  ElMessage.success('数据已刷新')
  setTimeout(() => {
    loading.value = false
  }, 500)
}

function goToResults() {
  router.push('/quality/results')
}

onMounted(() => {})
</script>

<style scoped>
.overview-stat-grid {
  grid-template-columns: repeat(4, 1fr);
}

.ring-progress {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

.circular-chart {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.circle-bg {
  fill: none;
  stroke: var(--dp-border-light);
  stroke-width: 3;
}

.circle {
  fill: none;
  stroke: var(--dp-success);
  stroke-width: 3;
  stroke-linecap: round;
  transition: stroke-dasharray 0.5s ease;
}

.level-dot {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
}

.level-dot i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.level-dot.高 { color: var(--dp-danger); }
.level-dot.高 i { background: var(--dp-danger); box-shadow: 0 0 4px var(--dp-danger); }
.level-dot.中 { color: var(--dp-warning); }
.level-dot.中 i { background: var(--dp-warning); box-shadow: 0 0 4px var(--dp-warning); }
.level-dot.低 { color: var(--dp-text-3); }
.level-dot.低 i { background: var(--dp-text-3); }

@media (max-width: 1200px) {
  .overview-stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
