<template>
  <div class="dp-page">
    <PageHeader title="报表总览" desc="全方位展示报表平台的核心数据指标、访问趋势和热门资源">
      <el-button type="primary" :icon="Plus">新建报表</el-button>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid dp-stat-grid-4">
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

    <!-- 图表区域 -->
    <div class="dp-grid-2">
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">报表访问趋势</div>
          <el-radio-group v-model="trendPeriod" size="small">
            <el-radio-button value="7">近7天</el-radio-button>
            <el-radio-button value="30">近30天</el-radio-button>
            <el-radio-button value="90">近90天</el-radio-button>
          </el-radio-group>
        </div>
        <EChart :option="visitTrendOption" height="280px" />
      </div>
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">报表分类统计</div>
        </div>
        <EChart :option="categoryOption" height="280px" />
      </div>
    </div>

    <!-- 热门报表 + 最近更新 -->
    <div class="dp-grid-2">
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">热门报表排行</div>
          <span class="dp-card-extra">按周访问量</span>
        </div>
        <div class="rp-hot-list">
          <div v-for="(item, idx) in hotReports" :key="item.id" class="rp-hot-item">
            <div class="rp-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</div>
            <div class="rp-hot-info">
              <div class="rp-hot-name">{{ item.name }}</div>
              <div class="rp-hot-meta">
                <el-icon><User /></el-icon>
                <span>{{ item.owner }}</span>
                <span class="rp-dot">·</span>
                <span>{{ item.charts }} 个图表</span>
              </div>
            </div>
            <div class="rp-hot-visits">
              <span class="rp-visit-num">{{ item.visitsWeek }}</span>
              <span class="rp-visit-label">次/周</span>
            </div>
          </div>
        </div>
      </div>
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">最近更新报表</div>
          <el-button link type="primary" size="small" @click="$router.push('/report/templates')">查看全部</el-button>
        </div>
        <el-table :data="recentReports" border stripe size="small">
          <el-table-column prop="name" label="报表名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="typeTagType(row.type)" effect="plain">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="owner" label="负责人" width="80" />
          <el-table-column prop="updatedAt" label="更新时间" width="150" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import EChart from '@/components/EChart.vue'
import { reportCharts, dashboards, trend, owners } from '@/mock'
import { Plus } from '@element-plus/icons-vue'

const trendPeriod = ref('30')

const statCards = [
  { label: '报表总数', value: dashboards.length + 42, unit: '份', icon: 'Document', color: '#165dff', bg: '#e8f3ff', trend: 12 },
  { label: '数据集数', value: 36, unit: '个', icon: 'DataLine', color: '#00b42a', bg: '#e8ffea', trend: 8 },
  { label: '图表数', value: reportCharts.length, unit: '个', icon: 'PieChart', color: '#722ed1', bg: '#f5e8ff', trend: 5 },
  { label: '浏览次数', value: '12.8', unit: '万次', icon: 'View', color: '#ff7d00', bg: '#fff3e8', trend: 15 }
]

const hotReports = computed(() =>
  [...dashboards].sort((a, b) => b.visitsWeek - a.visitsWeek).slice(0, 6)
)

const recentReports = computed(() => {
  const list = [...dashboards, ...reportCharts.slice(0, 4).map(c => ({
    ...c,
    type: c.type,
    name: c.name
  }))]
  return list.sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt)).slice(0, 6)
})

const typeTagType = type => {
  const map = { '折线图': 'primary', '柱状图': 'success', '饼图': 'warning', '漏斗图': 'danger', '地图': 'info', '指标卡': '', '表格': 'info', '散点图': '' }
  return map[type] || 'info'
}

const visitTrendOption = computed(() => {
  const days = parseInt(trendPeriod.value)
  const data = trend.access.slice(-days)
  return {
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.95)', borderColor: '#e5e6eb', textStyle: { color: '#1d2129' } },
    grid: { left: 50, right: 20, top: 30, bottom: 30 },
    legend: { data: ['访问量', '浏览人数'], top: 0, right: 0, textStyle: { color: '#86909c', fontSize: 12 } },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date),
      axisLine: { lineStyle: { color: '#e5e6eb' } },
      axisLabel: { color: '#86909c', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '次数',
      nameTextStyle: { color: '#86909c', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#86909c', fontSize: 11 }
    },
    series: [
      {
        name: '访问量',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: data.map(d => d.value),
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(22,93,255,0.2)' },
              { offset: 1, color: 'rgba(22,93,255,0)' }
            ]
          }
        },
        lineStyle: { color: '#165dff', width: 2 },
        itemStyle: { color: '#165dff' }
      },
      {
        name: '浏览人数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: data.map(d => Math.round(d.value * 0.6)),
        lineStyle: { color: '#00b42a', width: 2 },
        itemStyle: { color: '#00b42a' }
      }
    ]
  }
})

const categoryOption = computed(() => {
  const data = [
    { name: '经营分析', value: 18 },
    { name: '销售报表', value: 24 },
    { name: '财务报表', value: 12 },
    { name: '运营报表', value: 16 },
    { name: '技术监控', value: 8 },
    { name: '其他', value: 6 }
  ]
  const colors = ['#165dff', '#00b42a', '#ff7d00', '#722ed1', '#0fc6c2', '#86909c']
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}份 ({d}%)' },
    legend: { bottom: 0, type: 'scroll', textStyle: { color: '#4e5969', fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{d}%', fontSize: 11, lineHeight: 16 },
      labelLine: { length: 8, length2: 8 },
      data: data.map((d, i) => ({ ...d, itemStyle: { color: colors[i % colors.length] } }))
    }]
  }
})
</script>

<style scoped>
.dp-stat-grid-4 {
  grid-template-columns: repeat(4, 1fr);
}

.rp-hot-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rp-hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fafbfc;
  transition: all 0.2s;
  cursor: pointer;
}

.rp-hot-item:hover {
  background: #f0f5ff;
}

.rp-rank {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: #f2f3f5;
  color: var(--dp-text-3);
  flex-shrink: 0;
}

.rp-rank.rank-1 { background: #fff3e8; color: #ff7d00; }
.rp-rank.rank-2 { background: #e8f3ff; color: #165dff; }
.rp-rank.rank-3 { background: #e0fffa; color: #0fc6c2; }

.rp-hot-info {
  flex: 1;
  min-width: 0;
}

.rp-hot-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--dp-text-1);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rp-hot-meta {
  font-size: 12px;
  color: var(--dp-text-3);
  display: flex;
  align-items: center;
  gap: 4px;
}

.rp-dot {
  color: var(--dp-text-4);
}

.rp-hot-visits {
  text-align: right;
  flex-shrink: 0;
}

.rp-visit-num {
  font-size: 18px;
  font-weight: 700;
  color: var(--dp-primary);
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}

.rp-visit-label {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-left: 2px;
}

@media (max-width: 1200px) {
  .dp-stat-grid-4 {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
