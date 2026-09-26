<template>
  <div class="dp-page gov-page">
    <div class="dp-page-header dp-fade-up">
      <div>
        <div class="dp-page-eyebrow">GOVERNMENT DOMAIN</div>
        <h2 class="dp-page-title">政务领域实验</h2>
        <p class="dp-page-desc">面向政务场景的数据实验与仿真验证，支持公共治理、人口结构、迁入来源等专题。</p>
      </div>
      <div class="dp-page-header-actions">
        <el-button :icon="Download" @click="exportData">导出数据</el-button>
        <el-button type="primary" :icon="VideoPlay" @click="generateData" :loading="generating">生成数据</el-button>
      </div>
    </div>

    <div class="gov-body">
      <!-- 左侧菜单 -->
      <div class="dp-card gov-left">
        <div class="dp-card-title" style="margin-bottom: 12px">实验专题</div>
        <div class="gov-menu">
          <div
            v-for="item in menuItems"
            :key="item.key"
            class="gov-menu-item"
            :class="{ active: activeMenu === item.key }"
            @click="activeMenu = item.key"
          >
            <el-icon :size="16"><component :is="item.icon" /></el-icon>
            <span>{{ item.name }}</span>
            <el-tag v-if="item.tag" size="small" type="info" effect="plain">{{ item.tag }}</el-tag>
          </div>
        </div>
      </div>

      <!-- 右侧内容区 -->
      <div class="gov-right">
        <!-- Tab 切换 -->
        <div class="dp-card gov-tabs-card">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="参数配置" name="params" />
            <el-tab-pane label="数据结果" name="result" />
            <el-tab-pane label="数据分析" name="analysis" />
          </el-tabs>
        </div>

        <!-- 参数配置 -->
        <div v-show="activeTab === 'params'" class="dp-fade-in">
          <div class="dp-card">
            <div class="dp-card-title" style="margin-bottom: 16px">
              <el-icon color="var(--dp-primary)"><Setting /></el-icon>
              <span style="margin-left: 6px">地方参数</span>
            </div>
            <el-form :model="localParams" label-width="140px" class="param-form">
              <el-form-item label="模拟地区">
                <el-select v-model="localParams.region" style="width: 260px">
                  <el-option label="北京市" value="beijing" />
                  <el-option label="上海市:" value="shanghai" />
                  <el-option label="广州市:" value="guangzhou" />
                  <el-option label="深圳市:" value="shenzhen" />
                  <el-option label="杭州市:" value="hangzhou" />
                  <el-option label="成都市:" value="chengdu" />
                </el-select>
              </el-form-item>
              <el-form-item label="人口规模">
                <el-input-number v-model="localParams.population" :min="10000" :max="50000000" :step="10000" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">人</span>
              </el-form-item>
              <el-form-item label="行政区划层级">
                <el-radio-group v-model="localParams.adminLevel">
                  <el-radio value="province">省级</el-radio>
                  <el-radio value="city">市级</el-radio>
                  <el-radio value="district">区县级</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="脏数据比例:">
                <el-slider v-model="localParams.dirtyRate" :min="0" :max="20" :step="0.5" show-input style="width: 360px" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">%</span>
              </el-form-item>
              <el-form-item label="数据时间范围">
                <el-date-picker
                  v-model="localParams.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-form>
          </div>

          <div class="dp-card">
            <div class="dp-card-title" style="margin-bottom: 16px">
              <el-icon color="var(--dp-warning)"><Operation /></el-icon>
              <span style="margin-left: 6px">核心参数</span>
            </div>
            <el-form :model="coreParams" label-width="140px" class="param-form">
              <el-form-item :label="currentMenuConfig.coreParam1">
                <el-input-number v-model="coreParams.param1" :min="1" :max="100" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">{{ currentMenuConfig.coreParam1Unit }}</span>
              </el-form-item>
              <el-form-item :label="currentMenuConfig.coreParam2">
                <el-input-number v-model="coreParams.param2" :min="1" :max="1000" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">{{ currentMenuConfig.coreParam2Unit }}</span>
              </el-form-item>
              <el-form-item :label="currentMenuConfig.coreParam3">
                <el-slider v-model="coreParams.param3" :min="0" :max="100" style="width: 360px" />
              </el-form-item>
              <el-form-item label="随机种子">
                <el-input-number v-model="coreParams.seed" :min="1" :max="999999" />
                <el-button style="margin-left: 12px" :icon="RefreshRight" size="small" @click="randomSeed">随机</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <!-- 数据结果 -->
        <div v-show="activeTab === 'result'" class="dp-fade-in">
          <div class="dp-card">
            <div class="dp-card-header">
              <div class="dp-card-title">生成数据概览</div>
              <span class="dp-card-extra">共 {{ resultData.length }} 条记录</span>
            </div>

            <div class="result-stats">
              <div class="result-stat-item">
                <div class="result-stat-label">数据总量</div>
                <div class="result-stat-value">{{ formatNumber(localParams.population) }}<span>人</span></div>
              </div>
              <div class="result-stat-item">
                <div class="result-stat-label">字段数量</div>
                <div class="result-stat-value">{{ resultColumns.length }}<span>个</span></div>
              </div>
              <div class="result-stat-item">
                <div class="result-stat-label">脏数据量</div>
                <div class="result-stat-value" style="color: var(--dp-warning)">{{ formatNumber(Math.floor(localParams.population * localParams.dirtyRate / 100)) }}<span>条</span></div>
              </div>
              <div class="result-stat-item">
                <div class="result-stat-label">生成耗时</div>
                <div class="result-stat-value" style="color: var(--dp-success)">{{ generateCost }}<span>s</span></div>
              </div>
            </div>
          </div>

          <div class="dp-card">
            <div class="dp-toolbar">
              <el-input v-model="resultKeyword" placeholder="搜索数据" clearable style="width: 220px" :prefix-icon="Search" size="small" />
              <div class="dp-toolbar-right">
                <el-button size="small" :icon="Download">导出 CSV</el-button>
              </div>
            </div>
            <el-table :data="pagedResultData" border stripe size="small" height="400">
              <el-table-column v-for="col in resultColumns" :key="col.prop" :prop="col.prop" :label="col.label" :min-width="col.width || 120" />
            </el-table>
            <el-pagination v-model:current-page="resultPage" :page-size="20" :total="resultData.length" layout="total, prev, pager, next" />
          </div>
        </div>

        <!-- 数据分析 -->
        <div v-show="activeTab === 'analysis'" class="dp-fade-in">
          <div class="dp-grid-2">
            <div class="dp-card">
              <div class="dp-card-title">分布统计</div>
              <EChart :option="analysisChart1" height="280px" />
            </div>
            <div class="dp-card">
              <div class="dp-card-title">指标对比</div>
              <EChart :option="analysisChart2" height="280px" />
            </div>
          </div>
          <div class="dp-card">
            <div class="dp-card-title">数据质量评估</div>
            <div class="quality-grid">
              <div v-for="q in qualityMetrics" :key="q.name" class="quality-item">
                <div class="quality-name">{{ q.name }}</div>
                <el-progress :percentage="q.value" :color="q.color" :stroke-width="8" />
                <div class="quality-value">{{ q.value }}%</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import EChart from '@/components/EChart.vue'
import { Download, RefreshRight, Search, VideoPlay } from '@element-plus/icons-vue'

const activeMenu = ref('public')
const activeTab = ref('params')
const generating = ref(false)
const generateCost = ref(0)
const resultKeyword = ref('')
const resultPage = ref(1)

const menuItems = [
  { key: 'public', name: '公共治理数据', icon: 'OfficeBuilding', tag: '热门' },
  { key: 'population', name: '人口家庭结构', icon: 'User' },
  { key: 'migration', name: '迁入来源地权重', icon: 'Connection' },
  { key: 'economic', name: '经济运行指标', icon: 'DataLine' },
  { key: 'education', name: '教育资源分布', icon: 'Histogram' }
]

const menuConfig = {
  public: {
    coreParam1: '部门数量', coreParam1Unit: '个',
    coreParam2: '事项类型数', coreParam2Unit: '个',
    coreParam3: '办结率目标(%)'
  },
  population: {
    coreParam1: '年龄分段数', coreParam1Unit: '个',
    coreParam2: '家庭户规模', coreParam2Unit: '人 / 户',
    coreParam3: '城镇化率 (%)'
  },
  migration: {
    coreParam1: '来源省份数', coreParam1Unit: '个',
    coreParam2: '迁移规模', coreParam2Unit: '万人',
    coreParam3: '省内迁移占比 (%)'
  },
  economic: {
    coreParam1: '产业类别数', coreParam1Unit: '个',
    coreParam2: 'GDP 增速', coreParam2Unit: '%',
    coreParam3: '三产占比 (%)'
  },
  education: {
    coreParam1: '学校数量', coreParam1Unit: '所',
    coreParam2: '师生比例', coreParam2Unit: '',
    coreParam3: '入学率(%)'
  }
}

const currentMenuConfig = computed(() => menuConfig[activeMenu.value] || menuConfig.public)

const localParams = reactive({
  region: 'beijing',
  population: 21890000,
  adminLevel: 'city',
  dirtyRate: 3.5,
  dateRange: ['2023-01-01', '2025-12-31']
})

const coreParams = reactive({
  param1: 35,
  param2: 120,
  param3: 85,
  seed: 42
})

const resultColumns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'name', label: '名称', width: 160 },
  { prop: 'category', label: '类别', width: 120 },
  { prop: 'region', label: '所属区县', width: 120 },
  { prop: 'value', label: '数值', width: 120 },
  { prop: 'rate', label: '占比(%)', width: 100 },
  { prop: 'status', label: '状态', width: 90 },
  { prop: 'updateTime', label: '更新时间', width: 160 }
]

const resultData = ref([])
for (let i = 1; i <= 200; i++) {
  resultData.value.push({
    id: i,
    name: `数据项_${String(i).padStart(5, '0')}`,
    category: ['社会治理', '公共服务', '经济发展', '城市建设'][i % 4],
    region: ['东城区', '西城区', '朝阳区', '海淀区', '丰台区', '石景山区'][i % 6],
    value: Math.floor(Math.random() * 100000),
    rate: (Math.random() * 100).toFixed(2),
    status: ['正常', '正常', '正常', '异常'][i % 4],
    updateTime: `2025-${String((i % 12) + 1).padStart(2, '0')}-${String((i % 28) + 1).padStart(2, '0')}`
  })
}

const pagedResultData = computed(() => {
  let list = resultData.value
  if (resultKeyword.value) {
    list = list.filter(d => d.name.includes(resultKeyword.value) || d.category.includes(resultKeyword.value))
  }
  return list.slice((resultPage.value - 1) * 20, resultPage.value * 20)
})

const analysisChart1 = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0, data: ['数量', '占比'] },
  grid: { left: 50, right: 50, top: 36, bottom: 30 },
  xAxis: {
    type: 'category',
    data: ['类别A', '类别B', '类别C', '类别D', '类别E', '类别F'],
    axisLabel: { fontSize: 11 }
  },
  yAxis: [
    { type: 'value', name: '数量', axisLabel: { fontSize: 11 } },
    { type: 'value', name: '占比(%)', axisLabel: { fontSize: 11 } }
  ],
  series: [
    {
      name: '数量', type: 'bar',
      data: [3200, 4500, 2800, 5100, 3900, 2400],
      itemStyle: { color: '#165dff', borderRadius: [4, 4, 0, 0] },
      barWidth: 24
    },
    {
      name: '占比', type: 'line', yAxisIndex: 1, smooth: true,
      data: [15.2, 21.4, 13.3, 24.3, 18.6, 11.4],
      itemStyle: { color: '#00b42a' }
    }
  ]
}))

const analysisChart2 = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0, itemWidth: 10, itemHeight: 10 },
  series: [{
    type: 'pie',
    radius: ['45%', '70%'],
    center: ['50%', '42%'],
    itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
    label: { fontSize: 12 },
    data: [
      { value: 35, name: '维度A', itemStyle: { color: '#165dff' } },
      { value: 28, name: '维度B', itemStyle: { color: '#00b42a' } },
      { value: 20, name: '维度C', itemStyle: { color: '#722ed1' } },
      { value: 17, name: '维度D', itemStyle: { color: '#ff7d00' } }
    ]
  }]
}))

const qualityMetrics = [
  { name: '完整性', value: 96.5, color: '#165dff' },
  { name: '准确性', value: 94.2, color: '#00b42a' },
  { name: '一致性', value: 97.8, color: '#722ed1' },
  { name: '及时性', value: 95.6, color: '#ff7d00' },
  { name: '唯一性', value: 99.1, color: '#0fc6c2' },
  { name: '有效性', value: 93.8, color: '#f759ab' }
]

function formatNumber(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

function randomSeed() {
  coreParams.seed = Math.floor(Math.random() * 999999) + 1
}

function generateData() {
  generating.value = true
  const startTime = Date.now()
  setTimeout(() => {
    generating.value = false
    generateCost.value = ((Date.now() - startTime) / 1000).toFixed(2)
    activeTab.value = 'result'
    ElMessage.success('数据生成完成')
  }, 2000)
}

function exportData() {
  ElMessage.success('数据导出中...')
}
</script>

<style scoped>
.gov-page { min-height: 100%; }
.gov-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.gov-left {
  width: 220px;
  flex-shrink: 0;
  position: sticky;
  top: 16px;
}
.gov-right { flex: 1; min-width: 0; }

.gov-menu { display: flex; flex-direction: column; gap: 4px; }
.gov-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  color: var(--dp-text-2);
}
.gov-menu-item:hover { background: var(--dp-primary-bg); color: var(--dp-primary); }
.gov-menu-item.active {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  font-weight: 600;
}

.gov-tabs-card { padding: 0 16px; }
.gov-tabs-card :deep(.el-tabs__header) { margin-bottom: 0; }

.param-form { max-width: 640px; }

.result-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-top: 4px;
}
.result-stat-item {
  padding: 14px;
  background: var(--dp-bg-page);
  border-radius: 8px;
  text-align: center;
}
.result-stat-label {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 6px;
}
.result-stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', sans-serif;
}
.result-stat-value span {
  font-size: 12px;
  font-weight: 400;
  color: var(--dp-text-3);
  margin-left: 3px;
}

.quality-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.quality-item {
  text-align: center;
  padding: 16px;
}
.quality-name {
  font-size: 13px;
  color: var(--dp-text-2);
  margin-bottom: 8px;
}
.quality-value {
  font-size: 20px;
  font-weight: 700;
  font-family: 'DIN Alternate', sans-serif;
  margin-top: 8px;
}
</style>
