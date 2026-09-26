<template>
  <div class="dp-page">
    <PageHeader title="图表资产" desc="统一管理报表平台的图表资产，支持预览、编辑和发布">
      <el-button type="primary" :icon="Plus">新建图表</el-button>
    </PageHeader>

    <div class="dp-card">
      <div class="dp-toolbar">
        <div class="rp-chart-filters">
          <el-tag
            v-for="t in chartTypes"
            :key="t.value"
            :type="activeType === t.value ? 'primary' : 'info'"
            :effect="activeType === t.value ? 'dark' : 'plain'"
            class="filter-tag"
            @click="activeType = t.value"
          >
            {{ t.label }}
          </el-tag>
        </div>
        <el-select v-model="martFilter" placeholder="所属集市" clearable style="width: 140px">
          <el-option v-for="m in martOptions" :key="m" :label="m" :value="m" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索图表" clearable style="width: 200px" :prefix-icon="Search" />
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        </div>
      </div>

      <div v-loading="loading" class="rp-chart-grid">
        <div
          v-for="chart in pagedCharts"
          :key="chart.id"
          class="rp-chart-card dp-fade-up"
          @click="handlePreview(chart)"
        >
          <div class="rp-chart-preview" :class="'chart-' + getChartClass(chart.type)">
            <div class="chart-placeholder">
              <el-icon :size="36"><component :is="getChartIcon(chart.type)" /></el-icon>
            </div>
            <div class="rp-chart-type-tag">
              <el-tag size="small" effect="dark" :type="getTagType(chart.type)">{{ chart.type }}</el-tag>
            </div>
            <div v-if="chart.status === '已发布'" class="rp-chart-status-badge">
              <el-icon color="#00b42a"><CircleCheck /></el-icon>
            </div>
          </div>
          <div class="rp-chart-body">
            <div class="rp-chart-name dp-ellipsis" :title="chart.name">{{ chart.name }}</div>
            <div class="rp-chart-meta">
              <span class="rp-chart-mart">{{ chart.mart }}</span>
            </div>
            <div class="rp-chart-footer">
              <div class="rp-chart-views">
                <el-icon><View /></el-icon>
                <span>{{ chart.visits }}</span>
              </div>
              <el-tag size="small" :type="statusType(chart.status)" effect="plain">{{ chart.status }}</el-tag>
            </div>
            <div class="rp-chart-actions">
              <el-button link type="primary" size="small" @click.stop="handlePreview(chart)">
                <el-icon><View /></el-icon>
                <span>预览</span>
              </el-button>
              <el-button link type="primary" size="small" @click.stop="handleEdit(chart)">
                <el-icon><Edit /></el-icon>
                <span>编辑</span>
              </el-button>
              <el-button link type="danger" size="small" @click.stop="handleDelete(chart)">
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="filteredCharts.length"
        :page-sizes="[8, 16, 32]"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { reportCharts } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const keyword = ref('')
const activeType = ref('all')
const martFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(8)
const loading = ref(false)

const martOptions = ['销售集市', '客户集市', '财务集市', '运营集市', '供应链集市']

const chartTypes = [
  { value: 'all', label: '全部' },
  { value: '折线图', label: '折线图' },
  { value: '柱状图', label: '柱状图' },
  { value: '饼图', label: '饼图' },
  { value: '漏斗图', label: '漏斗图' },
  { value: '地图', label: '地图' },
  { value: '指标卡', label: '指标卡' },
  { value: '表格', label: '表格' },
  { value: '散点图', label: '散点图' }
]

const chartList = computed(() =>
  reportCharts.map((c, i) => ({
    ...c,
    mart: martOptions[i % martOptions.length],
    visits: Math.floor(Math.random() * 5000) + 100
  }))
)

const filteredCharts = computed(() => {
  let list = chartList.value
  if (activeType.value !== 'all') {
    list = list.filter(c => c.type === activeType.value)
  }
  if (martFilter.value) {
    list = list.filter(c => c.mart === martFilter.value)
  }
  if (keyword.value) {
    list = list.filter(c => c.name.includes(keyword.value))
  }
  return list
})

const pagedCharts = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredCharts.value.slice(start, start + pageSize.value)
})

const statusType = s => ({ '已发布': 'success', '编辑中': 'warning', '已下线': 'info' }[s] || 'info')

const getChartClass = type => {
  const map = { '折线图': 'line', '柱状图': 'bar', '饼图': 'pie', '漏斗图': 'funnel', '地图': 'map', '指标卡': 'metric', '表格': 'table', '散点图': 'scatter' }
  return map[type] || 'default'
}

const getChartIcon = type => {
  const map = { '折线图': 'TrendCharts', '柱状图': 'Histogram', '饼图': 'PieChart', '漏斗图': 'Funnel', '地图': 'Location', '指标卡': 'DataAnalysis', '表格': 'Grid', '散点图': 'DataLine' }
  return map[type] || 'PieChart'
}

const getTagType = type => {
  const map = { '折线图': 'primary', '柱状图': 'success', '饼图': 'warning', '漏斗图': 'danger', '地图': 'info', '指标卡': 'purple', '表格': '', '散点图': 'cyan' }
  return map[type] || 'info'
}

function handleRefresh() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('刷新成功')
  }, 600)
}

function handlePreview(chart) {
  ElMessage.info(`正在预览图表「${chart.name}」`)
}

function handleEdit(chart) {
  ElMessage.info(`正在打开图表「${chart.name}」编辑器`)
}

function handleDelete(chart) {
  ElMessageBox.confirm(`确定要删除图表「${chart.name}」吗？删除后相关报表可能受影响。`, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
  }).catch(() => {})
}
</script>

<style scoped>
.rp-chart-filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tag {
  cursor: pointer;
  user-select: none;
}

.rp-chart-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.rp-chart-card {
  border: 1px solid var(--dp-border-light);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  transition: all 0.25s;
  cursor: pointer;
}

.rp-chart-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.12);
  transform: translateY(-2px);
}

.rp-chart-preview {
  height: 140px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f5ff 0%, #f5f9ff 100%);
}

.rp-chart-preview.chart-line { background: linear-gradient(135deg, #e8f3ff 0%, #f0f7ff 100%); color: #165dff; }
.rp-chart-preview.chart-bar { background: linear-gradient(135deg, #e8ffea 0%, #f0fff4 100%); color: #00b42a; }
.rp-chart-preview.chart-pie { background: linear-gradient(135deg, #fff3e8 0%, #fffaf5 100%); color: #ff7d00; }
.rp-chart-preview.chart-funnel { background: linear-gradient(135deg, #ffece8 0%, #fff5f3 100%); color: #f53f3f; }
.rp-chart-preview.chart-map { background: linear-gradient(135deg, #e0fffa 0%, #f0fffd 100%); color: #0fc6c2; }
.rp-chart-preview.chart-metric { background: linear-gradient(135deg, #f5e8ff 0%, #faf5ff 100%); color: #722ed1; }
.rp-chart-preview.chart-table { background: linear-gradient(135deg, #f2f3f5 0%, #fafbfc 100%); color: #4e5969; }
.rp-chart-preview.chart-scatter { background: linear-gradient(135deg, #ffe8f4 0%, #fff5fa 100%); color: #f759ab; }

.chart-placeholder {
  opacity: 0.6;
}

.rp-chart-type-tag {
  position: absolute;
  top: 10px;
  right: 10px;
}

.rp-chart-status-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: #fff;
  border-radius: 50%;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0,0,0,0.1);
}

.rp-chart-body {
  padding: 12px 14px;
}

.rp-chart-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 6px;
}

.rp-chart-meta {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rp-chart-mart {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.rp-chart-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid var(--dp-border-light);
  margin-bottom: 10px;
}

.rp-chart-views {
  font-size: 12px;
  color: var(--dp-text-3);
  display: flex;
  align-items: center;
  gap: 4px;
}

.rp-chart-actions {
  display: flex;
  gap: 4px;
}

.rp-chart-actions .el-button {
  flex: 1;
  justify-content: center;
}

@media (max-width: 1400px) {
  .rp-chart-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 900px) {
  .rp-chart-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
