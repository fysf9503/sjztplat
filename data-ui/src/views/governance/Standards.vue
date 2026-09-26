<template>
  <div class="governance-standards">
    <PageHeader title="数据标准总览" desc="统一管理数据标准资产，支撑标准制定、发布、引用和落地全流程">
      <el-button type="primary" :icon="Plus">新建标准</el-button>
    </PageHeader>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <div class="stat-card dp-fade-up">
          <div class="stat-icon primary">
            <el-icon :size="22"><Collection /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">标准总数</div>
            <div class="stat-value">1,286<span class="stat-unit">项</span></div>
            <div class="stat-trend up">
              <el-icon><Top /></el-icon> 6.2% <span class="trend-label">较上月</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card dp-fade-up" style="animation-delay: 0.05s">
          <div class="stat-icon success">
            <el-icon :size="22"><DataLine /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">数据元数</div>
            <div class="stat-value">3,458<span class="stat-unit">个</span></div>
            <div class="stat-trend up">
              <el-icon><Top /></el-icon> 4.8% <span class="trend-label">较上月</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card dp-fade-up" style="animation-delay: 0.1s">
          <div class="stat-icon warning">
            <el-icon :size="22"><Grid /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">值域数</div>
            <div class="stat-value">856<span class="stat-unit">个</span></div>
            <div class="stat-trend up">
              <el-icon><Top /></el-icon> 3.1% <span class="trend-label">较上月</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card dp-fade-up" style="animation-delay: 0.15s">
          <div class="stat-icon purple">
            <el-icon :size="22"><Link /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-label">引用标准数</div>
            <div class="stat-value">128<span class="stat-unit">项</span></div>
            <div class="stat-trend up">
              <el-icon><Top /></el-icon> 12.5% <span class="trend-label">较上月</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="16">
      <el-col :span="8">
        <div class="dp-card dp-fade-up">
          <div class="dp-card-header">
            <div class="dp-card-title">标准分类统计</div>
          </div>
          <EChart :option="categoryPieOption" height="280px" />
        </div>
      </el-col>
      <el-col :span="16">
        <div class="dp-card dp-fade-up">
          <div class="dp-card-header">
            <div class="dp-card-title">标准发布趋势</div>
            <el-radio-group v-model="trendType" size="small">
              <el-radio-button value="month">近12个月</el-radio-button>
              <el-radio-button value="quarter">近8季度</el-radio-button>
            </el-radio-group>
          </div>
          <EChart :option="trendLineOption" height="280px" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 最近发布标准 -->
      <el-col :span="14">
        <div class="dp-card dp-fade-up">
          <div class="dp-card-header">
            <div class="dp-card-title">最近发布标准</div>
            <el-button link type="primary" @click="viewAll">查看全部</el-button>
          </div>
          <el-table :data="recentStandards" border stripe size="small">
            <el-table-column prop="code" label="标准编号" width="150" class-name="dp-mono" />
            <el-table-column prop="name" label="标准名称" min-width="180" show-overflow-tooltip />
            <el-table-column prop="category" label="分类" width="100">
              <template #default="{ row }">
                <span class="status-dot" :class="categoryDotClass(row.category)"></span>
                {{ row.category }}
              </template>
            </el-table-column>
            <el-table-column prop="version" label="版本" width="80" class-name="dp-mono" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <span class="status-dot" :class="statusDotClass(row.status)"></span>
                {{ row.status }}
              </template>
            </el-table-column>
            <el-table-column prop="owner" label="负责人" width="90" />
            <el-table-column prop="publishDate" label="发布时间" width="120" />
          </el-table>
        </div>
      </el-col>

      <!-- 标准质量评估雷达图 -->
      <el-col :span="10">
        <div class="dp-card dp-fade-up">
          <div class="dp-card-header">
            <div class="dp-card-title">标准质量评估</div>
            <el-tag size="small" type="success">综合得分 92.5</el-tag>
          </div>
          <EChart :option="radarOption" height="300px" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EChart from '@/components/EChart.vue'
import { Plus } from '@element-plus/icons-vue'

const trendType = ref('month')

const recentStandards = ref([
  { code: 'STD-BASE-1086', name: '客户身份标识规范', category: '基础标准', version: 'V2.1', status: '已发布', owner: '张伟', publishDate: '2026-09-25' },
  { code: 'STD-CODE-1085', name: '行政区划代码标准', category: '编码标准', version: 'V3.0', status: '已发布', owner: '李娜', publishDate: '2026-09-22' },
  { code: 'STD-IDX-1084', name: '客户价值指标口径', category: '指标标准', version: 'V1.5', status: '已发布', owner: '王强', publishDate: '2026-09-20' },
  { code: 'STD-NAME-1083', name: '数据表命名规范', category: '命名标准', version: 'V2.0', status: '已发布', owner: '刘洋', publishDate: '2026-09-18' },
  { code: 'STD-BASE-1082', name: '日期时间格式规范', category: '基础标准', version: 'V1.2', status: '已发布', owner: '陈静', publishDate: '2026-09-15' },
  { code: 'STD-CODE-1081', name: '币种代码标准', category: '编码标准', version: 'V1.8', status: '已发布', owner: '赵磊', publishDate: '2026-09-12' }
])

const categoryPieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c}项 ({d}%)' },
  legend: { bottom: 0, left: 'center', itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12, color: '#4e5969' } },
  color: ['#1664ff', '#00b42a', '#ff7d00', '#722ed1', '#0fc6c2'],
  series: [{
    type: 'pie',
    radius: ['45%', '70%'],
    center: ['50%', '45%'],
    avoidLabelOverlap: true,
    itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
    label: { show: false },
    emphasis: {
      label: { show: true, fontSize: 14, fontWeight: 'bold', formatter: '{b}\n{d}%' }
    },
    labelLine: { show: false },
    data: [
      { value: 486, name: '基础标准' },
      { value: 328, name: '编码标准' },
      { value: 256, name: '指标标准' },
      { value: 142, name: '命名标准' },
      { value: 74, name: '其他标准' }
    ]
  }]
}))

const trendLineOption = computed(() => {
  const months = ['10月', '11月', '12月', '1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月']
  const baseData = [62, 78, 95, 88, 102, 115, 98, 120, 135, 128, 142, 156]
  const elementData = [120, 145, 168, 155, 182, 210, 195, 228, 256, 245, 278, 312]
  const domainData = [28, 35, 42, 38, 48, 55, 52, 62, 68, 65, 72, 85]
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { top: 0, right: 0, itemWidth: 12, itemHeight: 8, textStyle: { fontSize: 12, color: '#4e5969' } },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: months,
      axisLine: { lineStyle: { color: '#e5e6eb' } },
      axisLabel: { fontSize: 11, color: '#86909c' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { fontSize: 11, color: '#86909c' }
    },
    series: [
      {
        name: '标准发布',
        type: 'line',
        smooth: true,
        data: baseData,
        lineStyle: { width: 2.5, color: '#1664ff' },
        itemStyle: { color: '#1664ff' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(22, 100, 255, 0.25)' },
              { offset: 1, color: 'rgba(22, 100, 255, 0.02)' }
            ]
          }
        }
      },
      {
        name: '数据元',
        type: 'line',
        smooth: true,
        data: elementData,
        lineStyle: { width: 2.5, color: '#00b42a' },
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
        name: '值域',
        type: 'line',
        smooth: true,
        data: domainData,
        lineStyle: { width: 2.5, color: '#ff7d00' },
        itemStyle: { color: '#ff7d00' }
      }
    ]
  }
})

const radarOption = computed(() => ({
  tooltip: { trigger: 'item' },
  radar: {
    indicator: [
      { name: '完整性', max: 100 },
      { name: '准确性', max: 100 },
      { name: '一致性', max: 100 },
      { name: '唯一性', max: 100 },
      { name: '及时性', max: 100 },
      { name: '有效性', max: 100 }
    ],
    radius: '65%',
    center: ['50%', '55%'],
    splitNumber: 4,
    axisName: { color: '#4e5969', fontSize: 12 },
    splitLine: { lineStyle: { color: '#f2f3f5' } },
    splitArea: { areaStyle: { color: ['#fff', '#f7f8fa'] } },
    axisLine: { lineStyle: { color: '#f2f3f5' } }
  },
  series: [{
    type: 'radar',
    data: [{
      value: [95, 92, 88, 96, 90, 94],
      name: '质量评分',
      areaStyle: { color: 'rgba(22, 100, 255, 0.2)' },
      lineStyle: { color: '#1664ff', width: 2 },
      itemStyle: { color: '#1664ff' }
    }]
  }]
}))

function categoryDotClass(cat) {
  const map = {
    '基础标准': 'primary',
    '编码标准': 'success',
    '指标标准': 'warning',
    '命名标准': 'purple'
  }
  return map[cat] || 'info'
}

function statusDotClass(status) {
  const map = {
    '已发布': 'success',
    '草稿': 'warning',
    '已废止': 'danger',
    '审核中': 'primary'
  }
  return map[status] || 'info'
}

function viewAll() {
  ElMessage.info('跳转到标准目录页面')
}
</script>

<style scoped>
.governance-standards {
  padding: 20px;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: var(--dp-radius);
  padding: 18px;
  display: flex;
  gap: 14px;
  align-items: flex-start;
  box-shadow: var(--dp-shadow-sm);
  transition: all 0.25s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.primary {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
}

.stat-icon.success {
  background: var(--dp-success-light);
  color: var(--dp-success);
}

.stat-icon.warning {
  background: var(--dp-warning-light);
  color: var(--dp-warning);
}

.stat-icon.purple {
  background: var(--dp-purple-light);
  color: var(--dp-purple);
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 13px;
  color: var(--dp-text-2);
  margin-bottom: 6px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--dp-text-1);
  line-height: 1.2;
  letter-spacing: -0.5px;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}

.stat-unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--dp-text-3);
  margin-left: 4px;
}

.stat-trend {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 500;
  margin-top: 6px;
}

.stat-trend.up {
  color: var(--dp-success);
}

.stat-trend.down {
  color: var(--dp-danger);
}

.trend-label {
  color: var(--dp-text-3);
  font-weight: 400;
  margin-left: 2px;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.status-dot.primary {
  background: var(--dp-primary);
}

.status-dot.success {
  background: var(--dp-success);
}

.status-dot.warning {
  background: var(--dp-warning);
}

.status-dot.danger {
  background: var(--dp-danger);
}

.status-dot.info {
  background: var(--dp-text-4);
}

.status-dot.purple {
  background: var(--dp-purple);
}

@keyframes dp-fade-up {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.dp-fade-up {
  animation: dp-fade-up 0.35s ease both;
}
</style>
