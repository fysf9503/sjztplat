<template>
  <div class="dp-page ec-page">
    <div class="dp-page-header dp-fade-up">
      <div>
        <div class="dp-page-eyebrow">E-COMMERCE RETAIL</div>
        <h2 class="dp-page-title">电商零售</h2>
        <p class="dp-page-desc">电商场景化数据实验，支持用户画像、商品分析、销售预测等零售行业仿真验证。</p>
      </div>
      <div class="dp-page-header-actions">
        <el-button :icon="Download" @click="exportData">导出数据</el-button>
        <el-button type="primary" :icon="VideoPlay" @click="generateData" :loading="generating">生成数据</el-button>
      </div>
    </div>

    <div class="ec-body">
      <!-- 左侧菜单 -->
      <div class="dp-card ec-left">
        <div class="dp-card-title" style="margin-bottom: 12px">实验专题</div>
        <div class="ec-menu">
          <div
            v-for="item in menuItems"
            :key="item.key"
            class="ec-menu-item"
            :class="{ active: activeMenu === item.key }"
            @click="activeMenu = item.key"
          >
            <el-icon :size="16"><component :is="item.icon" /></el-icon>
            <span>{{ item.name }}</span>
            <el-tag v-if="item.tag" size="small" type="info" effect="plain">{{ item.tag }}</el-tag>
          </div>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="ec-right">
        <div class="dp-card ec-tabs-card">
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
              <el-icon color="var(--dp-primary)"><User /></el-icon>
              <span style="margin-left: 6px">用户规模</span>
            </div>
            <el-form :model="userParams" label-width="140px" class="param-form">
              <el-form-item label="用户规模">
                <el-input-number v-model="userParams.userCount" :min="1000" :max="10000000" :step="1000" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">人</span>
              </el-form-item>
              <el-form-item label="用户分层">
                <el-checkbox-group v-model="userParams.tiers">
                  <el-checkbox label="新用户:">新用户</el-checkbox>
                  <el-checkbox label="活跃用户">活跃用户</el-checkbox>
                  <el-checkbox label="沉睡用户">沉睡用户</el-checkbox>
                  <el-checkbox label="流失用户">流失用户</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="性别比例 (%)">
                <el-slider v-model="userParams.maleRatio" :min="20" :max="80" show-input style="width: 360px" />
              </el-form-item>
              <el-form-item label="年龄分布">
                <el-radio-group v-model="userParams.ageDist">
                  <el-radio value="young">年轻型</el-radio>
                  <el-radio value="balanced">均衡型</el-radio>
                  <el-radio value="mature">成熟型</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-form>
          </div>

          <div class="dp-card">
            <div class="dp-card-title" style="margin-bottom: 16px">
              <el-icon color="var(--dp-warning)"><Goods /></el-icon>
              <span style="margin-left: 6px">商品配置</span>
            </div>
            <el-form :model="productParams" label-width="140px" class="param-form">
              <el-form-item label="商品数量">
                <el-input-number v-model="productParams.productCount" :min="100" :max="100000" :step="100" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">SKU</span>
              </el-form-item>
              <el-form-item label="商品类目数:">
                <el-input-number v-model="productParams.categoryCount" :min="5" :max="200" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">个</span>
              </el-form-item>
              <el-form-item label="价格区间">
                <el-input-number v-model="productParams.minPrice" :min="1" :max="1000" controls-position="right" style="width: 140px" />
                <span style="margin: 0 10px">~</span>
                <el-input-number v-model="productParams.maxPrice" :min="10" :max="100000" controls-position="right" style="width: 140px" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">元</span>
              </el-form-item>
            </el-form>
          </div>

          <div class="dp-card">
            <div class="dp-card-title" style="margin-bottom: 16px">
              <el-icon color="var(--dp-success)"><ShoppingCart /></el-icon>
              <span style="margin-left: 6px">订单配置</span>
            </div>
            <el-form :model="orderParams" label-width="140px" class="param-form">
              <el-form-item label="订单周期">
                <el-select v-model="orderParams.period" style="width: 200px">
                  <el-option label="近 30 天" value="30d" />
                  <el-option label="近 90 天" value="90d" />
                  <el-option label="近 180 天" value="180d" />
                  <el-option label="近 1 年" value="1y" />
                </el-select>
              </el-form-item>
              <el-form-item label="日均订单量:">
                <el-input-number v-model="orderParams.avgDailyOrders" :min="100" :max="100000" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">单</span>
              </el-form-item>
              <el-form-item label="促销活动">
                <el-switch v-model="orderParams.hasPromotion" />
                <span style="margin-left: 12px; color: var(--dp-text-3); font-size: 12px">启用后将模拟大促数据波动</span>
              </el-form-item>
              <el-form-item v-if="orderParams.hasPromotion" label="大促频次">
                <el-radio-group v-model="orderParams.promoFreq">
                  <el-radio value="monthly">每月一次</el-radio>
                  <el-radio value="quarterly">每季度一次</el-radio>
                  <el-radio value="holiday">仅节假日</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="退换货率:">
                <el-slider v-model="orderParams.returnRate" :min="0" :max="30" :step="0.5" show-input style="width: 360px" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">%</span>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <!-- 数据结果 -->
        <div v-show="activeTab === 'result'" class="dp-fade-in">
          <div class="dp-card">
            <div class="dp-card-header">
              <div class="dp-card-title">销售数据概览</div>
              <span class="dp-card-extra">模拟周期：{{ orderParams.period }}</span>
            </div>
            <div class="result-stats">
              <div class="result-stat-item">
                <div class="result-stat-label">GMV</div>
                <div class="result-stat-value">¥{{ formatMoney(gmv) }}<span>元</span></div>
              </div>
              <div class="result-stat-item">
                <div class="result-stat-label">订单总数</div>
                <div class="result-stat-value">{{ formatNumber(totalOrders) }}<span>单</span></div>
              </div>
              <div class="result-stat-item">
                <div class="result-stat-label">用户数</div>
                <div class="result-stat-value" style="color: var(--dp-purple)">{{ formatNumber(userParams.userCount) }}<span>人</span></div>
              </div>
              <div class="result-stat-item">
                <div class="result-stat-label">客单价</div>
                <div class="result-stat-value" style="color: var(--dp-warning)">¥{{ avgOrderValue.toFixed(0) }}<span>元</span></div>
              </div>
            </div>
          </div>

          <div class="dp-card">
            <div class="dp-toolbar">
              <el-input v-model="resultKeyword" placeholder="搜索商品/订单" clearable style="width: 220px" :prefix-icon="Search" size="small" />
              <el-select v-model="resultFilter" size="small" style="width: 140px">
                <el-option label="全部" value="all" />
                <el-option label="销售Top" value="top" />
                <el-option label="新品" value="new" />
              </el-select>
              <div class="dp-toolbar-right">
                <el-button size="small" :icon="Download">导出 Excel</el-button>
              </div>
            </div>
            <el-table :data="pagedResultData" border stripe size="small" height="400">
              <el-table-column prop="sku" label="SKU编码" width="120" class-name="dp-mono" />
              <el-table-column prop="productName" label="商品名称" min-width="180" />
              <el-table-column prop="category" label="类目" width="100" />
              <el-table-column prop="price" label="价格" width="100">
                <template #default="{ row }">¥{{ row.price.toFixed(2) }}</template>
              </el-table-column>
              <el-table-column prop="sales" label="销量" width="100" sortable>
                <template #default="{ row }">{{ row.sales.toLocaleString() }}</template>
              </el-table-column>
              <el-table-column prop="revenue" label="销售额" width="120" sortable>
                <template #default="{ row }">¥{{ formatMoney(row.revenue) }}</template>
              </el-table-column>
              <el-table-column prop="returnRate" label="退换率" width="90">
                <template #default="{ row }">{{ row.returnRate }}%</template>
              </el-table-column>
              <el-table-column prop="stock" label="库存" width="90" />
            </el-table>
            <el-pagination v-model:current-page="resultPage" :page-size="20" :total="resultData.length" layout="total, prev, pager, next" />
          </div>
        </div>

        <!-- 数据分析 -->
        <div v-show="activeTab === 'analysis'" class="dp-fade-in">
          <div class="dp-grid-2">
            <div class="dp-card">
              <div class="dp-card-title">销售趋势</div>
              <EChart :option="salesTrendOption" height="280px" />
            </div>
            <div class="dp-card">
              <div class="dp-card-title">类目销售占比</div>
              <EChart :option="categoryPieOption" height="280px" />
            </div>
          </div>
          <div class="dp-grid-2">
            <div class="dp-card">
              <div class="dp-card-title">用户画像</div>
              <EChart :option="userProfileOption" height="280px" />
            </div>
            <div class="dp-card">
              <div class="dp-card-title">商品分析 Top10</div>
              <EChart :option="topProductsOption" height="280px" />
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
import { Download, Search, VideoPlay } from '@element-plus/icons-vue'

const activeMenu = ref('sales')
const activeTab = ref('params')
const generating = ref(false)
const resultKeyword = ref('')
const resultFilter = ref('all')
const resultPage = ref(1)

const menuItems = [
  { key: 'sales', name: '销售数据', icon: 'DataLine', tag: '热门' },
  { key: 'user', name: '用户画像', icon: 'User' },
  { key: 'product', name: '商品分析', icon: 'Goods' },
  { key: 'marketing', name: '营销活动', icon: 'Star' },
  { key: 'trend', name: '趋势预测', icon: 'TrendCharts' }
]

const userParams = reactive({
  userCount: 500000,
  tiers: ['新用户', '活跃用户', '沉睡用户', '流失用户'],
  maleRatio: 48,
  ageDist: 'balanced'
})

const productParams = reactive({
  productCount: 10000,
  categoryCount: 50,
  minPrice: 9.9,
  maxPrice: 9999
})

const orderParams = reactive({
  period: '90d',
  avgDailyOrders: 5000,
  hasPromotion: true,
  promoFreq: 'monthly',
  returnRate: 5.2
})

const categories = ['服饰鞋包', '数码电子', '美妆个护', '食品生鲜', '家居日用', '母婴玩具', '运动户外', '图书文娱']
const resultData = ref([])

for (let i = 1; i <= 200; i++) {
  const price = +(Math.random() * 500 + 10).toFixed(2)
  const sales = Math.floor(Math.random() * 50000 + 100)
  resultData.value.push({
    sku: 'SKU' + String(100000 + i),
    productName: `商品_${String(i).padStart(5, '0')}`,
    category: categories[i % categories.length],
    price,
    sales,
    revenue: +(price * sales).toFixed(2),
    returnRate: +(Math.random() * 8 + 1).toFixed(1),
    stock: Math.floor(Math.random() * 10000 + 100)
  })
}

const pagedResultData = computed(() => {
  let list = resultData.value
  if (resultKeyword.value) {
    list = list.filter(d => d.productName.includes(resultKeyword.value) || d.sku.includes(resultKeyword.value))
  }
  if (resultFilter.value === 'top') {
    list = [...list].sort((a, b) => b.sales - a.sales).slice(0, 50)
  }
  return list.slice((resultPage.value - 1) * 20, resultPage.value * 20)
})

const gmv = computed(() => resultData.value.reduce((s, d) => s + d.revenue, 0))
const totalOrders = computed(() => Math.floor(resultData.value.reduce((s, d) => s + d.sales, 0) / 2.5))
const avgOrderValue = computed(() => gmv.value / totalOrders.value)

function formatMoney(n) {
  if (n >= 100000000) return (n / 100000000).toFixed(2) + '亿'
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toFixed(2)
}

function formatNumber(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

const salesTrendOption = computed(() => {
  const days = 30
  const labels = Array.from({ length: days }, (_, i) => `${i + 1}日`)
  const sales = labels.map(() => Math.floor(Math.random() * 500000 + 200000))
  const orders = labels.map(() => Math.floor(Math.random() * 5000 + 2000))
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '订单量'], top: 0 },
    grid: { left: 60, right: 60, top: 36, bottom: 30 },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 11, interval: 4 } },
    yAxis: [
      { type: 'value', name: '销售额(元)', axisLabel: { fontSize: 11 } },
      { type: 'value', name: '订单量', axisLabel: { fontSize: 11 } }
    ],
    series: [
      {
        name: '销售额', type: 'line', smooth: true,
        data: sales,
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
        name: '订单量', type: 'line', yAxisIndex: 1, smooth: true,
        data: orders,
        itemStyle: { color: '#00b42a' }
      }
    ]
  }
})

const categoryPieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: {d}%' },
  legend: { bottom: 0, itemWidth: 10, itemHeight: 10 },
  series: [{
    type: 'pie',
    radius: ['45%', '70%'],
    center: ['50%', '42%'],
    itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
    label: { fontSize: 12, formatter: '{b}\n{d}%' },
    data: categories.map((c, i) => ({
      value: Math.floor(Math.random() * 300 + 100),
      name: c,
      itemStyle: { color: ['#165dff', '#00b42a', '#722ed1', '#ff7d00', '#f759ab', '#0fc6c2', '#f53f3f', '#86909c'][i] }
    }))
  }]
}))

const userProfileOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { data: ['男性', '女性'], top: 0 },
  grid: { left: 50, right: 20, top: 36, bottom: 30 },
  xAxis: {
    type: 'category',
    data: ['18-24', '25-30', '31-35', '36-40', '41-45', '46-50', '50+'],
    axisLabel: { fontSize: 11 }
  },
  yAxis: { type: 'value', axisLabel: { fontSize: 11 } },
  series: [
    {
      name: '男性', type: 'bar', stack: 'total',
      data: [12000, 28000, 35000, 25000, 18000, 12000, 8000],
      itemStyle: { color: '#4080ff' },
      barWidth: 20
    },
    {
      name: '女性', type: 'bar', stack: 'total',
      data: [15000, 32000, 38000, 22000, 15000, 10000, 6000],
      itemStyle: { color: '#f759ab' },
      barWidth: 20
    }
  ]
}))

const topProductsOption = computed(() => {
  const top = [...resultData.value].sort((a, b) => b.sales - a.sales).slice(0, 10).reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 120, right: 40, top: 10, bottom: 20 },
    xAxis: { type: 'value', axisLabel: { fontSize: 11 } },
    yAxis: {
      type: 'category',
      data: top.map(t => t.productName),
      axisLabel: { fontSize: 11, width: 100, overflow: 'truncate' }
    },
    series: [{
      type: 'bar',
      data: top.map(t => t.sales),
      itemStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [{ offset: 0, color: '#4080ff' }, { offset: 1, color: '#165dff' }]
        },
        borderRadius: [0, 4, 4, 0]
      },
      barWidth: 16
    }]
  }
})

function generateData() {
  generating.value = true
  setTimeout(() => {
    generating.value = false
    activeTab.value = 'result'
    ElMessage.success('数据生成完成')
  }, 2000)
}

function exportData() {
  ElMessage.success('数据导出中...')
}
</script>

<style scoped>
.ec-page { min-height: 100%; }
.ec-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.ec-left {
  width: 220px;
  flex-shrink: 0;
  position: sticky;
  top: 16px;
}
.ec-right { flex: 1; min-width: 0; }

.ec-menu { display: flex; flex-direction: column; gap: 4px; }
.ec-menu-item {
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
.ec-menu-item:hover { background: var(--dp-primary-bg); color: var(--dp-primary); }
.ec-menu-item.active {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  font-weight: 600;
}

.ec-tabs-card { padding: 0 16px; }
.ec-tabs-card :deep(.el-tabs__header) { margin-bottom: 0; }

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
</style>
