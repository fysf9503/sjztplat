<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">{{ eyebrow }}</div>
      <h1 class="dp-page-title">{{ title }}</h1>
      <p class="dp-page-desc">{{ desc }}</p>
    </div>

    <div class="dp-card biz-search-card dp-fade-up">
      <div class="biz-search-row">
        <el-input
          v-model="keyword"
          size="large"
          placeholder="输入业务关键词，如：GMV、客户数、销售额、复购率等"
          clearable
          class="biz-search-input"
          @keyup.enter="doSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" size="large" :icon="Search" @click="doSearch">
          搜索
        </el-button>
      </div>

      <div class="biz-search-options">
        <div class="biz-option-item">
          <span class="biz-option-label">AI 辅助</span>
          <el-switch v-model="aiEnabled" size="small" />
        </div>
        <div class="biz-option-item">
          <span class="biz-option-label">业务域</span>
          <el-select v-model="selectedDomain" size="small" style="width: 140px" clearable placeholder="全部">
            <el-option v-for="d in domainOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </div>
        <div class="biz-option-item">
          <span class="biz-option-label">数据类型</span>
          <el-select v-model="selectedType" size="small" style="width: 140px" clearable placeholder="全部">
            <el-option label="业务指标" value="metric" />
            <el-option label="业务主题" value="topic" />
            <el-option label="维度" value="dimension" />
            <el-option label="报表" value="report" />
          </el-select>
        </div>
        <div class="biz-option-item">
          <span class="biz-option-label">时间范围</span>
          <el-select v-model="timeRange" size="small" style="width: 140px">
            <el-option label="近 7 天" value="7d" />
            <el-option label="近 30 天" value="30d" />
            <el-option label="近 90 天" value="90d" />
            <el-option label="本年度" value="ytd" />
          </el-select>
        </div>
      </div>
    </div>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <span class="dp-desc">
          共找到 <b style="color: var(--dp-text-1)">{{ filteredResults.length }}</b> 条业务数据           <span v-if="lastKeyword">，关键词：「{{ lastKeyword }}」</span>
        </span>
        <div class="dp-toolbar-right">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="list">列表视图</el-radio-button>
            <el-radio-button label="card">卡片视图</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div v-if="viewMode === 'card'" class="biz-card-grid">
        <div v-for="item in pagedResults" :key="item.id" class="biz-card dp-fade-up" @click="openDetail(item)">
          <div class="biz-card-head">
            <div class="biz-card-icon" :style="{ background: item.color + '18', color: item.color }">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
            <el-tag size="small" :type="item.tagType" effect="plain">{{ item.typeLabel }}</el-tag>
          </div>
          <div class="biz-card-name" v-html="highlight(item.name)"></div>
          <div class="biz-card-desc">{{ item.desc }}</div>
          <div class="biz-card-meta">
            <span>业务域：{{ item.domain }}</span>
            <span>负责人：{{ item.owner }}</span>
          </div>
          <div class="biz-card-preview">
            <div class="biz-preview-label">当前值</div>
            <div class="biz-preview-value">
              {{ item.currentValue }}<span class="biz-preview-unit">{{ item.unit }}</span>
            </div>
            <div class="biz-preview-trend" :class="item.trend >= 0 ? 'up' : 'down'">
              <el-icon><component :is="item.trend >= 0 ? 'Top' : 'Bottom'" /></el-icon>
              {{ Math.abs(item.trend) }}%
            </div>
          </div>
        </div>
      </div>

      <div v-else class="biz-list">
        <div v-for="item in pagedResults" :key="item.id" class="biz-list-item dp-fade-up" @click="openDetail(item)">
          <div class="biz-list-left">
            <div class="biz-list-icon" :style="{ background: item.color + '18', color: item.color }">
              <el-icon :size="18"><component :is="item.icon" /></el-icon>
            </div>
          </div>
          <div class="biz-list-main">
            <div class="biz-list-header">
              <span class="biz-list-name" v-html="highlight(item.name)"></span>
              <el-tag size="small" :type="item.tagType" effect="plain">{{ item.typeLabel }}</el-tag>
              <span class="biz-list-domain">{{ item.domain }}</span>
            </div>
            <div class="biz-list-desc">{{ item.desc }}</div>
            <div class="biz-list-meta">
              <span>
                <el-icon><User /></el-icon>
                {{ item.owner }}
              </span>
              <span>
                <el-icon><Clock /></el-icon>
                更新：{{ item.updatedAt }}
              </span>
              <span>
                <el-icon><View /></el-icon>
                {{ item.views }} 次查询               </span>
            </div>
          </div>
          <div class="biz-list-right">
            <div class="biz-list-value">
              {{ item.currentValue }}<span class="biz-list-unit">{{ item.unit }}</span>
            </div>
            <div class="biz-list-trend" :class="item.trend >= 0 ? 'up' : 'down'">
              <el-icon><component :is="item.trend >= 0 ? 'Top' : 'Bottom'" /></el-icon>
              {{ Math.abs(item.trend) }}%
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="!filteredResults.length" description="未找到匹配的业务数据，请调整搜索条件" />

      <el-pagination
        v-if="filteredResults.length"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="filteredResults.length"
        layout="total, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
      />
    </div>

    <el-drawer v-model="detailVisible" :title="detail?.name" size="560px">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="业务名称">{{ detail.name }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detail.typeLabel }}</el-descriptions-item>
          <el-descriptions-item label="业务域">{{ detail.domain }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ detail.owner }}</el-descriptions-item>
          <el-descriptions-item label="当前值">
            <span style="font-size: 18px; font-weight: 700; color: var(--dp-primary)">
              {{ detail.currentValue }}{{ detail.unit }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="环比">
            <span :class="detail.trend >= 0 ? 'biz-trend-up' : 'biz-trend-down'">
              {{ detail.trend >= 0 ? '+' : '' }}{{ detail.trend }}%
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detail.updatedAt }}</el-descriptions-item>
          <el-descriptions-item label="查看次数">{{ detail.views }}</el-descriptions-item>
        </el-descriptions>

        <div class="dp-card-title" style="margin-top: 20px; margin-bottom: 12px">指标描述</div>
        <p style="color: var(--dp-text-2); line-height: 1.8; font-size: 13px; margin: 0 0 16px">{{ detail.desc }}</p>

        <div class="dp-card-title" style="margin-top: 20px; margin-bottom: 12px">趋势图（近 30 天）</div>
        <EChart :option="detailChartOption" height="240px" />

        <div class="dp-card-title" style="margin-top: 20px; margin-bottom: 12px">快捷操作</div>
        <div class="dp-flex dp-gap-12">
          <el-button type="primary" :icon="DataAnalysis">查看完整报表</el-button>
          <el-button :icon="Download">导出数据</el-button>
          <el-button :icon="Share">分享</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import EChart from '@/components/EChart.vue'
import { rnd, int, dateStr, pick, genTrend } from '@/mock/helpers'
import { DataAnalysis, Download, Search, Share } from '@element-plus/icons-vue'

const eyebrow = 'BUSINESS DATA EXPLORATION'
const title = '业务数据检索'
const desc = '面向业务主题的数据检索能力，支持按业务域、主题、指标进行快速查询和结果预览。'

const keyword = ref('')
const lastKeyword = ref('')
const aiEnabled = ref(false)
const selectedDomain = ref('')
const selectedType = ref('')
const timeRange = ref('30d')
const viewMode = ref('card')
const currentPage = ref(1)
const pageSize = ref(12)
const detailVisible = ref(false)
const detail = ref(null)

const domainOptions = ['客户域', '交易域', '商品域', '营销域', '供应链域', '财务域']

const typeMeta = {
  metric: { icon: 'DataLine', color: '#165dff', tagType: 'primary', label: '业务指标' },
  topic: { icon: 'Grid', color: '#00b42a', tagType: 'success', label: '业务主题' },
  dimension: { icon: 'Guide', color: '#ff7d00', tagType: 'warning', label: '分析维度' },
  report: { icon: 'DataAnalysis', color: '#722ed1', tagType: 'danger', label: '报表看板' }
}

const metricNames = [
  { name: 'GMV（商品交易总额）', unit: '万元', desc: '统计周期内所有订单的商品金额总和，不含退款' },
  { name: '有效订单数', unit: '单', desc: '支付成功且未全额退款的订单数量' },
  { name: '客户数', unit: '人', desc: '统计周期内有过交易行为的去重客户数量' },
  { name: '客单价', unit: '元', desc: '平均每笔订单的交易金额，GMV / 有效订单数' },
  { name: '复购率', unit: '%', desc: '统计周期内购买两次及以上的客户占比' },
  { name: '新客占比', unit: '%', desc: '首次购买客户占总交易客户的比例' },
  { name: '退款率', unit: '%', desc: '退款订单数 / 总订单数' },
  { name: '会员活跃率', unit: '%', desc: '活跃会员数 / 总会员数' }
]

const topicNames = [
  { name: '客户生命周期主题', unit: '个', desc: '覆盖客户从注册、活跃、消费到流失的全生命周期数据' },
  { name: '销售分析主题', unit: '个', desc: '多维度销售数据分析，支持渠道、区域、品类等下钻' },
  { name: '商品运营主题', unit: '个', desc: '商品销售、库存、动销率等核心运营指标汇总' },
  { name: '营销效果主题', unit: '个', desc: '营销活动投放效果追踪与 ROI 分析' }
]

const dimensionNames = [
  { name: '时间维度', unit: '个', desc: '年、季、月、周、日等时间粒度维度表' },
  { name: '区域维度', unit: '个', desc: '省、市、区县等行政区划维度，支持地理层级下钻' },
  { name: '渠道维度', unit: '个', desc: '线上线下、APP/小程序 H5 等流量渠道维度' }
]

const reportNames = [
  { name: '经营驾驶舱', unit: '个', desc: '管理层核心经营指标一览，支持关键预警' },
  { name: '销售专题看板', unit: '个', desc: '销售业绩全维度分析看板' },
  { name: '会员分析看板', unit: '个', desc: '会员画像、活跃度、价值分层分析' }
]

function buildItems(arr, type) {
  return arr.map((item, i) => {
    const trend = +(rnd() * 30 - 10).toFixed(1)
    const baseValue = int(100, 99999)
    return {
      id: type + '_' + i,
      name: item.name,
      type,
      ...typeMeta[type],
      unit: item.unit,
      desc: item.desc,
      domain: pick(domainOptions),
      owner: pick(['张伟', '李娜', '王强', '刘洋', '陈静', '赵磊']),
      updatedAt: dateStr(int(0, 15)),
      views: int(100, 50000),
      currentValue: type === 'metric' ? baseValue.toLocaleString() : int(1, 50),
      trend,
      trendData: genTrend(30, baseValue, baseValue * 0.2, v => Math.round(v))
    }
  })
}

const mockResults = [
  ...buildItems(metricNames, 'metric'),
  ...buildItems(topicNames, 'topic'),
  ...buildItems(dimensionNames, 'dimension'),
  ...buildItems(reportNames, 'report')
]

const filteredResults = computed(() => {
  let list = mockResults.filter(r => {
    if (selectedDomain.value && r.domain !== selectedDomain.value) return false
    if (selectedType.value && r.type !== selectedType.value) return false
    if (lastKeyword.value) {
      const kw = lastKeyword.value.toLowerCase()
      return r.name.toLowerCase().includes(kw) || r.desc.includes(lastKeyword.value)
    }
    return true
  })
  list.sort((a, b) => b.views - a.views)
  return list
})

const pagedResults = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredResults.value.slice(start, start + pageSize.value)
})

const detailChartOption = computed(() => {
  if (!detail.value) return {}
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: detail.value.trendData.map(d => d.date), axisLabel: { color: '#86909c' } },
    yAxis: { type: 'value', axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#f2f3f5' } }, axisLabel: { color: '#86909c' } },
    series: [{
      type: 'line', smooth: true, symbol: 'none',
      data: detail.value.trendData.map(d => d.value),
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(22,93,255,0.25)' }, { offset: 1, color: 'rgba(22,93,255,0)' }] } },
      lineStyle: { color: '#165dff', width: 2 }
    }]
  }
})

function highlight(text) {
  if (!lastKeyword.value) return text
  const regex = new RegExp(`(${lastKeyword.value})`, 'gi')
  return text.replace(regex, '<span style="color:#1664ff;font-weight:700">$1</span>')
}

function doSearch() {
  lastKeyword.value = keyword.value.trim()
  currentPage.value = 1
}

function openDetail(item) {
  detail.value = item
  detailVisible.value = true
}

doSearch()
</script>

<style scoped>
.biz-search-card {
  background: linear-gradient(135deg, #f0fff4 0%, #e8ffea 100%);
  border: 1px solid #c7f0cc;
}

.biz-search-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.biz-search-input {
  flex: 1;
}

.biz-search-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--dp-success) inset;
}

.biz-search-options {
  display: flex;
  gap: 24px;
  align-items: center;
  flex-wrap: wrap;
}

.biz-option-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.biz-option-label {
  color: var(--dp-text-3);
  white-space: nowrap;
}

.biz-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.biz-card {
  border: 1px solid var(--dp-border);
  border-radius: 10px;
  padding: 16px;
  background: #fff;
  cursor: pointer;
  transition: all 0.25s;
}

.biz-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.1);
  transform: translateY(-2px);
}

.biz-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.biz-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.biz-card-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 6px;
  line-height: 1.4;
}

.biz-card-desc {
  font-size: 12px;
  color: var(--dp-text-3);
  line-height: 1.5;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.biz-card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--dp-text-3);
  padding-top: 10px;
  border-top: 1px dashed var(--dp-border);
  margin-bottom: 12px;
}

.biz-card-preview {
  background: var(--dp-bg-page);
  border-radius: 8px;
  padding: 12px;
}

.biz-preview-label {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 4px;
}

.biz-preview-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--dp-text-1);
  line-height: 1.2;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}

.biz-preview-unit {
  font-size: 12px;
  font-weight: 400;
  color: var(--dp-text-3);
  margin-left: 2px;
}

.biz-preview-trend {
  margin-top: 6px;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.biz-preview-trend.up { color: var(--dp-success); }
.biz-preview-trend.down { color: var(--dp-danger); }

.biz-list-item {
  display: flex;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--dp-border);
  border-radius: 8px;
  margin-bottom: 10px;
  background: #fff;
  transition: all 0.2s;
  cursor: pointer;
  align-items: center;
}

.biz-list-item:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 2px 12px rgba(22, 93, 255, 0.08);
}

.biz-list-left { flex-shrink: 0; }

.biz-list-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.biz-list-main { flex: 1; min-width: 0; }

.biz-list-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.biz-list-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.biz-list-domain {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-left: 4px;
}

.biz-list-desc {
  font-size: 13px;
  color: var(--dp-text-2);
  margin-bottom: 6px;
}

.biz-list-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--dp-text-3);
}

.biz-list-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.biz-list-right {
  flex-shrink: 0;
  text-align: right;
}

.biz-list-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}

.biz-list-unit {
  font-size: 12px;
  font-weight: 400;
  color: var(--dp-text-3);
  margin-left: 2px;
}

.biz-list-trend {
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-top: 4px;
}

.biz-list-trend.up { color: var(--dp-success); }
.biz-list-trend.down { color: var(--dp-danger); }

.biz-trend-up { color: var(--dp-success); font-weight: 600; }
.biz-trend-down { color: var(--dp-danger); font-weight: 600; }
</style>
