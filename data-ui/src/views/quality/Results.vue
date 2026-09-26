<template>
  <div class="dp-page">
    <PageHeader title="质量结果分析" desc="质检结果明细、趋势分析与问题处理闭环。">
    </PageHeader>

    <!-- 顶部筛选 -->
    <div class="dp-card dp-fade-up filter-card">
      <div class="dp-toolbar">
        <el-radio-group v-model="timeRange" size="default" @change="loadData">
          <el-radio-button value="today">今日</el-radio-button>
          <el-radio-button value="7d">近7天</el-radio-button>
          <el-radio-button value="30d">近30天</el-radio-button>
        </el-radio-group>
        <el-select v-model="filter.datasource" placeholder="数据源" clearable style="width: 160px" @change="loadData">
          <el-option v-for="d in datasourceOptions" :key="d" :label="d" :value="d" />
        </el-select>
        <el-select v-model="filter.ruleType" placeholder="规则类型" clearable style="width: 130px" @change="loadData">
          <el-option v-for="t in ruleTypeOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="filter.dimension" placeholder="质量维度" clearable style="width: 120px" @change="loadData">
          <el-option v-for="d in dimensionOptions" :key="d" :label="d" :value="d" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Download" @click="exportReport">导出报告</el-button>
          <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </div>
    </div>

    <!-- 数据质量总览卡片 -->
    <div class="dp-stat-grid results-stat-grid">
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>整体合格率</span>
          <div class="dp-stat-icon" style="background: var(--dp-success-light); color: var(--dp-success)">
            <el-icon :size="18"><CircleCheck /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">
          {{ overviewData.passRate }}<span class="dp-stat-unit">%</span>
        </div>
        <div class="dp-stat-sub">
          <span class="dp-stat-trend" :class="overviewData.passRateTrend >= 0 ? 'up' : 'down'">
            <el-icon><component :is="overviewData.passRateTrend >= 0 ? 'Top' : 'Bottom'" /></el-icon>
            {{ Math.abs(overviewData.passRateTrend) }}%
          </span>
          <span>较上周</span>
        </div>
      </div>

      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>检测总记录数</span>
          <div class="dp-stat-icon" style="background: var(--dp-primary-light); color: var(--dp-primary)">
            <el-icon :size="18"><DataLine /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">
          {{ overviewData.totalRecords.toLocaleString() }}<span class="dp-stat-unit">条</span>
        </div>
        <div class="dp-stat-sub">
          <span class="dp-stat-trend up">
            <el-icon><Top /></el-icon>
            {{ overviewData.recordsTrend }}%
          </span>
          <span>较上周</span>
        </div>
      </div>

      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>异常记录数</span>
          <div class="dp-stat-icon" style="background: var(--dp-danger-light); color: var(--dp-danger)">
            <el-icon :size="18"><Warning /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">
          {{ overviewData.anomalyRecords.toLocaleString() }}<span class="dp-stat-unit">条</span>
        </div>
        <div class="dp-stat-sub">
          <span class="dp-stat-trend" :class="overviewData.anomalyTrend >= 0 ? 'up' : 'down'">
            <el-icon><component :is="overviewData.anomalyTrend >= 0 ? 'Top' : 'Bottom'" /></el-icon>
            {{ Math.abs(overviewData.anomalyTrend) }}%
          </span>
          <span>较上周</span>
        </div>
      </div>

      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>待处理问题</span>
          <div class="dp-stat-icon" style="background: var(--dp-warning-light); color: var(--dp-warning)">
            <el-icon :size="18"><Bell /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">
          {{ overviewData.pendingIssues }}<span class="dp-stat-unit">个</span>
        </div>
        <div class="dp-stat-sub">
          <span class="dp-stat-trend down">
            <el-icon><Bottom /></el-icon>
            {{ overviewData.pendingTrend }}%
          </span>
          <span>较上周</span>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="dp-grid-2">
      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">质量趋势</div>
        </div>
        <EChart :option="trendOption" height="300px" />
      </div>

      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">各维度得分</div>
        </div>
        <EChart :option="dimensionOption" height="300px" />
      </div>
    </div>

    <!-- 异常数据明细表格 -->
    <div class="dp-card dp-fade-up">
      <div class="dp-card-header">
        <div class="dp-card-title">异常数据明细</div>
        <div class="dp-card-extra">
          <el-radio-group v-model="filter.status" size="small" @change="loadData">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="未处理">未处理</el-radio-button>
            <el-radio-button value="处理中">处理中</el-radio-button>
            <el-radio-button value="已处理">已处理</el-radio-button>
            <el-radio-button value="已忽略">已忽略</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="dataId" label="数据ID" width="100" class-name="dp-mono" />
        <el-table-column prop="tableName" label="所属表" min-width="180" class-name="dp-mono" show-overflow-tooltip />
        <el-table-column prop="ruleName" label="异常规则" min-width="150" show-overflow-tooltip />
        <el-table-column prop="anomalyType" label="异常类型" width="110">
          <template #default="{ row }">
            <el-tag :type="anomalyTypeTag(row.anomalyType)" size="small" effect="plain">{{ row.anomalyType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="异常级别" width="90">
          <template #default="{ row }">
            <span class="level-dot" :class="row.level">
              <i></i>{{ row.level }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="foundTime" label="发现时间" width="160" />
        <el-table-column prop="status" label="处理状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="plain">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">查看详情</el-button>
            <el-button v-if="row.status === '未处理' || row.status === '处理中'" link type="success" size="small" @click="markProcessed(row)">标记已处理</el-button>
            <el-button v-if="row.status !== '已忽略'" link type="warning" size="small" @click="ignoreAnomaly(row)">忽略</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EChart from '@/components/EChart.vue'
import { Download, Refresh } from '@element-plus/icons-vue'

const loading = ref(false)
const timeRange = ref('30d')

const filter = reactive({
  datasource: '',
  ruleType: '',
  dimension: '',
  status: ''
})

const query = reactive({
  page: 1,
  size: 10
})

const total = ref(0)
const tableData = ref([])

const datasourceOptions = ['核心交易库', '客户主数据库', '日志采集库', '财务系统库', '会员中心库', '商品中心库']
const ruleTypeOptions = ['非空校验', '值域校验', '唯一性校验', '格式校验', '一致性校验', '及时性校验']
const dimensionOptions = ['完整性', '准确性', '一致性', '唯一性', '时效性', '有效性']

const overviewData = reactive({
  passRate: 96.8,
  passRateTrend: 2.1,
  totalRecords: 2845600,
  recordsTrend: 12.5,
  anomalyRecords: 91056,
  anomalyTrend: -5.3,
  pendingIssues: 23,
  pendingTrend: -15.2
})

// Mock 异常数据
const allData = ref([
  { dataId: 'ID-10234', tableName: 'dwd_trade_db.order_detail', ruleName: '订单金额非空校验', anomalyType: '空值', level: '高', foundTime: '2026-09-26 10:23:45', status: '未处理' },
  { dataId: 'ID-10233', tableName: 'ods_customer_db.customer_info', ruleName: '用户手机号格式校验', anomalyType: '格式错误', level: '中', foundTime: '2026-09-26 09:45:12', status: '处理中' },
  { dataId: 'ID-10232', tableName: 'dwd_trade_db.payment_record', ruleName: '支付金额范围校验', anomalyType: '值域超限', level: '高', foundTime: '2026-09-26 09:12:33', status: '未处理' },
  { dataId: 'ID-10231', tableName: 'dim_public_db.product_sku', ruleName: '商品编码唯一性校验', anomalyType: '重复值', level: '低', foundTime: '2026-09-26 08:30:18', status: '已处理' },
  { dataId: 'ID-10230', tableName: 'dws_summary_db.sale_day', ruleName: '销售金额汇总一致性', anomalyType: '数据不一致', level: '中', foundTime: '2026-09-26 08:05:42', status: '已处理' },
  { dataId: 'ID-10229', tableName: 'dwd_trade_db.order_detail', ruleName: '订单状态枚举校验', anomalyType: '枚举不符', level: '低', foundTime: '2026-09-26 07:50:20', status: '已忽略' },
  { dataId: 'ID-10228', tableName: 'ods_customer_db.member_profile', ruleName: '会员等级值域校验', anomalyType: '值域超限', level: '中', foundTime: '2026-09-26 07:30:55', status: '未处理' },
  { dataId: 'ID-10227', tableName: 'dwd_log_db.access_log', ruleName: '数据延迟及时性校验', anomalyType: '延迟过高', level: '中', foundTime: '2026-09-26 07:00:10', status: '处理中' },
  { dataId: 'ID-10226', tableName: 'dwd_trade_db.trade_flow', ruleName: '订单号唯一性校验', anomalyType: '重复值', level: '高', foundTime: '2026-09-26 06:45:33', status: '已处理' },
  { dataId: 'ID-10225', tableName: 'ods_customer_db.customer_info', ruleName: '邮箱格式校验', anomalyType: '格式错误', level: '低', foundTime: '2026-09-26 06:20:18', status: '未处理' },
  { dataId: 'ID-10224', tableName: 'dwd_trade_db.refund_order', ruleName: '退款金额合理性校验', anomalyType: '逻辑错误', level: '高', foundTime: '2026-09-25 23:15:42', status: '未处理' },
  { dataId: 'ID-10223', tableName: 'dws_summary_db.user_behavior_stat', ruleName: '数据完整性校验', anomalyType: '空值', level: '中', foundTime: '2026-09-25 22:00:05', status: '已处理' },
  { dataId: 'ID-10222', tableName: 'dim_public_db.product_sku', ruleName: '商品编码格式校验', anomalyType: '格式错误', level: '低', foundTime: '2026-09-25 20:30:27', status: '已忽略' },
  { dataId: 'ID-10221', tableName: 'dwd_trade_db.order_detail', ruleName: '客户ID参照完整性', anomalyType: '参照缺失', level: '高', foundTime: '2026-09-25 18:45:12', status: '处理中' },
  { dataId: 'ID-10220', tableName: 'ods_finance_db.invoice_info', ruleName: '发票号唯一性校验', anomalyType: '重复值', level: '高', foundTime: '2026-09-25 16:20:38', status: '未处理' }
])

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

// 质量趋势图（面积图）
const trendOption = computed(() => {
  const baseValue = 94
  const generateLine = (offset, volatility) => {
    return dates30.map((_, i) => {
      const wave = Math.sin(i / 4 + offset) * volatility
      const noise = (Math.random() - 0.5) * volatility * 0.4
      return +(baseValue + offset + wave + noise).toFixed(1)
    })
  }

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['合格率', '异常率'], top: 0, right: 0 },
    grid: { left: 50, right: 30, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: dates30,
      axisLabel: { fontSize: 11, interval: 4 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: { formatter: '{value}%', fontSize: 11 }
    },
    series: [
      {
        name: '合格率',
        type: 'line',
        smooth: true,
        symbol: 'none',
        data: generateLine(2.5, 2),
        lineStyle: { color: '#00b42a', width: 2 },
        itemStyle: { color: '#00b42a' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(0, 180, 42, 0.3)' },
              { offset: 1, color: 'rgba(0, 180, 42, 0.02)' }
            ]
          }
        }
      },
      {
        name: '异常率',
        type: 'line',
        smooth: true,
        symbol: 'none',
        data: generateLine(-2.5, 1.5).map(v => Math.max(0.5, 100 - v)),
        lineStyle: { color: '#f53f3f', width: 2 },
        itemStyle: { color: '#f53f3f' },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(245, 63, 63, 0.2)' },
              { offset: 1, color: 'rgba(245, 63, 63, 0.02)' }
            ]
          }
        }
      }
    ]
  }
})

// 各维度得分柱状图
const dimensionOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter: '{b}: {c}分'
  },
  grid: { left: 60, right: 30, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    data: ['完整性', '准确性', '一致性', '唯一性', '时效性', '有效性'],
    axisLabel: { fontSize: 12, color: '#4e5969' }
  },
  yAxis: {
    type: 'value',
    min: 80,
    max: 100,
    axisLabel: { formatter: '{value}分', fontSize: 11 }
  },
  series: [{
    type: 'bar',
    barMaxWidth: 40,
    data: [
      { value: 97.2, itemStyle: { color: '#1664ff', borderRadius: [6, 6, 0, 0] } },
      { value: 95.8, itemStyle: { color: '#00b42a', borderRadius: [6, 6, 0, 0] } },
      { value: 94.5, itemStyle: { color: '#ff7d00', borderRadius: [6, 6, 0, 0] } },
      { value: 96.8, itemStyle: { color: '#722ed1', borderRadius: [6, 6, 0, 0] } },
      { value: 93.2, itemStyle: { color: '#0fc6c2', borderRadius: [6, 6, 0, 0] } },
      { value: 98.1, itemStyle: { color: '#f759ab', borderRadius: [6, 6, 0, 0] } }
    ],
    label: {
      show: true,
      position: 'top',
      formatter: '{c}分',
      fontSize: 12,
      color: '#4e5969'
    }
  }]
}))

function anomalyTypeTag(type) {
  const map = {
    '空值': 'danger',
    '格式错误': 'warning',
    '值域超限': 'warning',
    '重复值': 'danger',
    '数据不一致': 'primary',
    '枚举不符': 'info',
    '延迟过高': 'warning',
    '逻辑错误': 'danger',
    '参照缺失': 'primary'
  }
  return map[type] || 'info'
}

function statusTag(status) {
  const map = {
    '未处理': 'danger',
    '处理中': 'warning',
    '已处理': 'success',
    '已忽略': 'info'
  }
  return map[status] || 'info'
}

function loadData() {
  loading.value = true
  setTimeout(() => {
    let list = allData.value.filter(d =>
      (!filter.status || d.status === filter.status)
    )
    total.value = list.length
    const start = (query.page - 1) * query.size
    tableData.value = list.slice(start, start + query.size)
    loading.value = false
  }, 300)
}

function viewDetail(row) {
  ElMessage.info(`正在查看数据「${row.dataId}」的详情...`)
}

function markProcessed(row) {
  ElMessageBox.confirm(
    `确定将数据「${row.dataId}」标记为已处理吗？`,
    '处理确认',
    { type: 'info' }
  ).then(() => {
    row.status = '已处理'
    ElMessage.success('已标记为已处理')
    loadData()
  }).catch(() => {})
}

function ignoreAnomaly(row) {
  ElMessageBox.confirm(
    `确定忽略此异常数据吗？忽略后将不再告警。`,
    '忽略确认',
    { type: 'warning' }
  ).then(() => {
    row.status = '已忽略'
    ElMessage.success('已忽略此异常')
    loadData()
  }).catch(() => {})
}

function exportReport() {
  ElMessage.success('质量报告导出成功，正在下载...')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.filter-card {
  padding-bottom: 0;
}

.results-stat-grid {
  grid-template-columns: repeat(4, 1fr);
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
  .results-stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
