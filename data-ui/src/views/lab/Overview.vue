<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div>
        <div class="dp-page-eyebrow">LAB OVERVIEW</div>
        <h2 class="dp-page-title">实验总览</h2>
        <p class="dp-page-desc">行业化实验与样本验证中心，支持政务领域、电商零售等场景化实验。</p>
      </div>
      <div class="dp-page-header-actions">
        <el-button :icon="Refresh" @click="refresh">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="createExperiment">新建实验</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid">
      <StatCard label="实验总数" :value="stats.total" unit="个" icon="Experiment" color="#165dff" bg="#e8f3ff" :trend="12" />
      <StatCard label="运行中" :value="stats.running" unit="个" icon="Loading" color="#00b42a" bg="#e8ffea" :trend="5" />
      <StatCard label="已完成" :value="stats.completed" unit="个" icon="CircleCheck" color="#722ed1" bg="#f5e8ff" :trend="8" />
      <StatCard label="场景数" :value="stats.scenes" unit="个" icon="Grid" color="#ff7d00" bg="#fff3e8" />
    </div>

    <!-- 图表区域 -->
    <div class="dp-grid-2">
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">实验趋势<span class="dp-card-extra">近 30 天</span></div>
          <el-radio-group v-model="trendRange" size="small">
            <el-radio-button value="7d">近 7 天</el-radio-button>
            <el-radio-button value="30d">近 30 天</el-radio-button>
            <el-radio-button value="90d">近 90 天</el-radio-button>
          </el-radio-group>
        </div>
        <EChart :option="trendOption" height="300px" />
      </div>
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">场景分布</div>
          <span class="dp-card-extra">按领域统计</span>
        </div>
        <EChart :option="scenePieOption" height="300px" />
      </div>
    </div>

    <!-- 最近实验列表 -->
    <div class="dp-card">
      <div class="dp-card-header">
        <div class="dp-card-title">最近实验<span class="dp-card-extra">最近 10 条</span></div>
        <el-button link type="primary" size="small" @click="router.push('/lab/gov')">查看全部</el-button>
      </div>
      <el-table :data="recentExperiments" border stripe>
        <el-table-column prop="name" label="实验名称" min-width="220" />
        <el-table-column prop="scene" label="场景" width="120">
          <template #default="{ row }">
            <el-tag :type="sceneTagType(row.scene)" size="small" effect="plain">{{ row.scene }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataset" label="数据量" width="180" class-name="dp-mono" />
        <el-table-column prop="params" label="参数数量" width="100" align="center" />
        <el-table-column prop="rounds" label="实验轮次" width="100" align="center" />
        <el-table-column prop="bestScore" label="最佳得分" width="110" sortable>
          <template #default="{ row }">
            <span class="score-text">{{ row.bestScore }}<small>分</small></span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column prop="updatedAt" label="更新时间" width="140" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="runExperiment(row)" :disabled="row.status === '运行中'">运行</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import EChart from '@/components/EChart.vue'
import { labExperiments } from '@/mock'
import { Plus, Refresh } from '@element-plus/icons-vue'

const router = useRouter()
const trendRange = ref('30d')

const stats = computed(() => ({
  total: labExperiments.length,
  running: labExperiments.filter(e => e.status === '运行中').length,
  completed: labExperiments.filter(e => e.status === '已完成').length,
  scenes: new Set(labExperiments.map(e => e.scene)).size
}))

const recentExperiments = computed(() => [...labExperiments].slice(0, 10))

const sceneTagType = (s) => ({
  '政务领域': 'primary',
  '电商零售': 'success'
}[s] || 'info')

const statusTagType = (s) => ({
  '运行中': 'primary',
  '已完成': 'success',
  '待运行': 'info',
  '已失败': 'danger'
}[s] || 'info')

// 实验趋势图
const trendOption = computed(() => {
  const days = trendRange.value === '7d' ? 7 : trendRange.value === '30d' ? 30 : 90
  const labels = Array.from({ length: days }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() - (days - 1 - i))
    return `${d.getMonth() + 1}/${d.getDate()}`
  })
  const created = labels.map(() => Math.floor(Math.random() * 8) + 2)
  const completed = labels.map(() => Math.floor(Math.random() * 6) + 1)
  const failed = labels.map(() => Math.floor(Math.random() * 2))

  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['新建实验', '完成实验', '失败实验'], top: 0 },
    grid: { left: 40, right: 20, top: 36, bottom: 30 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { fontSize: 11, interval: Math.floor(days / 10) },
      axisLine: { lineStyle: { color: '#e5e6eb' } },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { fontSize: 11 }
    },
    series: [
      {
        name: '新建实验', type: 'line', smooth: true,
        data: created,
        itemStyle: { color: '#165dff' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(22, 93, 255, 0.25)' },
              { offset: 1, color: 'rgba(22, 93, 255, 0.02)' }
            ]
          }
        }
      },
      {
        name: '完成实验', type: 'line', smooth: true,
        data: completed,
        itemStyle: { color: '#00b42a' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(0, 180, 42, 0.2)' },
              { offset: 1, color: 'rgba(0, 180, 42, 0.02)' }
            ]
          }
        }
      },
      {
        name: '失败实验', type: 'line', smooth: true,
        data: failed,
        itemStyle: { color: '#f53f3f' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(245, 63, 63, 0.15)' },
              { offset: 1, color: 'rgba(245, 63, 63, 0.02)' }
            ]
          }
        }
      }
    ]
  }
})

// 场景分布饼图
const scenePieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
  legend: { bottom: 0, itemWidth: 10, itemHeight: 10 },
  series: [{
    type: 'pie',
    radius: ['50%', '75%'],
    center: ['50%', '42%'],
    avoidLabelOverlap: true,
    itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
    label: { show: true, formatter: '{b}\n{d}%', fontSize: 12 },
    labelLine: { length: 12, length2: 12 },
    data: [
      { value: 6, name: '政务领域', itemStyle: { color: '#165dff' } },
      { value: 5, name: '电商零售', itemStyle: { color: '#00b42a' } },
      { value: 2, name: '金融风控', itemStyle: { color: '#722ed1' } },
      { value: 1, name: '医疗健康', itemStyle: { color: '#ff7d00' } }
    ]
  }]
}))

function createExperiment() {
  ElMessage.success('跳转到新建实验页面')
}

function refresh() {
  ElMessage.success('已刷新')
}

function viewDetail(row) {
  ElMessage.info(`查看实验「${row.name}」详情`)
}

function runExperiment(row) {
  ElMessage.success(`实验「${row.name}」已启动`)
}
</script>

<style scoped>
.score-text {
  font-size: 15px;
  font-weight: 700;
  color: var(--dp-primary);
  font-family: 'DIN Alternate', sans-serif;
}
.score-text small {
  font-size: 11px;
  font-weight: 400;
  color: var(--dp-text-3);
  margin-left: 2px;
}
</style>
