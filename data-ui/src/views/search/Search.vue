<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">{{ eyebrow }}</div>
      <h1 class="dp-page-title">{{ title }}</h1>
      <p class="dp-page-desc">{{ desc }}</p>
    </div>

    <div class="dp-card se-search-card dp-fade-up">
      <div class="se-search-row">
        <el-input
          v-model="keyword"
          size="large"
          placeholder="输入关键词搜索表、字段、任务、规则、服务 API 等元数据资产"
          clearable
          class="se-search-input"
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

      <div class="se-search-options">
        <div class="se-option-item">
          <span class="se-option-label">AI 辅助</span>
          <el-switch v-model="aiEnabled" size="small" />
        </div>
        <div class="se-option-item">
          <span class="se-option-label">搜索范围</span>
          <el-select v-model="searchScope" size="small" style="width: 160px">
            <el-option label="模糊匹配" value="fuzzy" />
            <el-option label="精确匹配" value="exact" />
            <el-option label="前缀匹配" value="prefix" />
          </el-select>
        </div>
        <div class="se-option-item">
          <span class="se-option-label">来源模块</span>
          <el-select v-model="sourceModules" multiple size="small" style="width: 280px" placeholder="请选择来源模块">
            <el-option label="数据地图" value="map" />
            <el-option label="数据接入" value="access" />
            <el-option label="质量管控" value="quality" />
            <el-option label="数据服务" value="service" />
          </el-select>
        </div>
      </div>
    </div>

    <div class="dp-card dp-fade-up">
      <el-collapse v-model="activeFilters" class="se-filter-collapse">
        <el-collapse-item title="筛选条件:" name="filters">
          <div class="se-filter-grid">
            <div class="se-filter-block">
              <div class="se-filter-title">资产类型</div>
              <div class="se-filter-tags">
                <el-tag
                  v-for="t in typeOptions"
                  :key="t.value"
                  :type="selectedTypes.includes(t.value) ? 'primary' : 'info'"
                  :effect="selectedTypes.includes(t.value) ? 'dark' : 'plain'"
                  class="se-filter-tag"
                  @click="toggleType(t.value)"
                >
                  {{ t.label }}
                </el-tag>
              </div>
            </div>
            <div class="se-filter-block">
              <div class="se-filter-title">数据分层</div>
              <div class="se-filter-tags">
                <el-tag
                  v-for="l in layerOptions"
                  :key="l"
                  :type="selectedLayers.includes(l) ? 'primary' : 'info'"
                  :effect="selectedLayers.includes(l) ? 'dark' : 'plain'"
                  class="se-filter-tag"
                  @click="toggleLayer(l)"
                >
                  {{ l }}
                </el-tag>
              </div>
            </div>
            <div class="se-filter-block">
              <div class="se-filter-title">所属业务域</div>
              <div class="se-filter-tags">
                <el-tag
                  v-for="d in domainOptions"
                  :key="d"
                  :type="selectedDomains.includes(d) ? 'primary' : 'info'"
                  :effect="selectedDomains.includes(d) ? 'dark' : 'plain'"
                  class="se-filter-tag"
                  @click="toggleDomain(d)"
                >
                  {{ d }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>

    <div class="dp-card dp-fade-up">
      <el-tabs v-model="activeTab" class="se-result-tabs" @tab-change="doSearch">
        <el-tab-pane label="全部" name="all">
          <template #label>
            <span>全部 <span class="se-tab-count">{{ totalCount }}</span></span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="表资产" name="table">
          <template #label>
            <span>表资产 <span class="se-tab-count">{{ tableCount }}</span></span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="字段" name="field">
          <template #label>
            <span>字段 <span class="se-tab-count">{{ fieldCount }}</span></span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="接入" name="access">
          <template #label>
            <span>接入 <span class="se-tab-count">{{ accessCount }}</span></span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="质量" name="quality">
          <template #label>
            <span>质量 <span class="se-tab-count">{{ qualityCount }}</span></span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="服务" name="service">
          <template #label>
            <span>服务 <span class="se-tab-count">{{ serviceCount }}</span></span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <div class="dp-toolbar">
        <span class="dp-desc">
          共找到 <b style="color: var(--dp-text-1)">{{ filteredResults.length }}</b> 条结果           <span v-if="lastKeyword">，关键词：「{{ lastKeyword }}」</span>
        </span>
        <div class="dp-toolbar-right">
          <el-select v-model="sortBy" size="small" style="width: 140px" @change="doSearch">
            <el-option label="按相关度排序" value="relevance" />
            <el-option label="按热度排序:" value="hot" />
            <el-option label="按更新时间排序:" value="time" />
          </el-select>
        </div>
      </div>

      <div v-for="item in pagedResults" :key="item.id" class="se-result-item dp-fade-up">
        <div class="se-result-left">
          <div class="se-result-icon" :style="{ background: item.color + '18', color: item.color }">
            <el-icon :size="18"><component :is="item.icon" /></el-icon>
          </div>
        </div>
        <div class="se-result-main">
          <div class="se-result-header">
            <span class="se-result-name" v-html="highlight(item.name)"></span>
            <el-tag size="small" :type="item.tagType" effect="plain">{{ item.typeLabel }}</el-tag>
            <el-tag v-if="item.layer" size="small" effect="plain" type="info">{{ item.layer }}</el-tag>
            <span class="se-result-score">
              得分 <b>{{ item.score }}</b>
            </span>
          </div>
          <div class="se-result-comment">{{ item.comment }}</div>
          <div class="se-result-meta">
            <span v-if="item.fieldType">
              <el-icon><Histogram /></el-icon>
              字段类型：{{ item.fieldType }}
            </span>
            <span v-if="item.tableName">
              <el-icon><Grid /></el-icon>
              所属表：{{ item.tableName }}
            </span>
            <span v-if="item.database">
              <el-icon><Coin /></el-icon>
              数据库：{{ item.database }}
            </span>
            <span>
              <el-icon><User /></el-icon>
              负责人：{{ item.owner }}
            </span>
            <span>
              <el-icon><Clock /></el-icon>
              更新：{{ item.updatedAt }}
            </span>
          </div>
        </div>
        <div class="se-result-right">
          <el-button link type="primary">查看详情</el-button>
        </div>
      </div>

      <el-empty v-if="!filteredResults.length" description="未找到匹配的元数据资产，请调整搜索条件" />

      <el-pagination
        v-if="filteredResults.length"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="filteredResults.length"
        layout="total, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { rnd, int, dateStr, pick, genRows } from '@/mock/helpers'
import { Search } from '@element-plus/icons-vue'

const eyebrow = 'GLOBAL METADATA DISCOVERY'
const title = '元数据检索'
const desc = '统一检索数据地图、数据接入、质量管控和数据服务资产，支持表、字段、任务、规则、策略和服务 API 多粒度结果。'

const keyword = ref('')
const lastKeyword = ref('')
const aiEnabled = ref(false)
const searchScope = ref('fuzzy')
const sourceModules = ref(['map', 'access', 'quality', 'service'])
const activeFilters = ref(['filters'])
const activeTab = ref('all')
const selectedTypes = ref([])
const selectedLayers = ref([])
const selectedDomains = ref([])
const sortBy = ref('relevance')
const currentPage = ref(1)
const pageSize = ref(10)

const typeOptions = [
  { label: '数据表', value: 'table' },
  { label: '字段', value: 'field' },
  { label: '接入任务', value: 'access' },
  { label: '质量规则', value: 'quality' },
  { label: '服务API', value: 'service' }
]

const layerOptions = ['ODS', 'DWD', 'DWS', 'ADS', 'DIM']
const domainOptions = ['客户域', '交易域', '商品域', '营销域', '供应链域', '财务域']

const typeMeta = {
  table: { icon: 'Grid', color: '#165dff', tagType: 'primary', label: '数据表' },
  field: { icon: 'Histogram', color: '#14c9c9', tagType: 'success', label: '字段' },
  access: { icon: 'Upload', color: '#00b42a', tagType: 'success', label: '接入任务' },
  quality: { icon: 'CircleCheck', color: '#ff7d00', tagType: 'warning', label: '质量规则' },
  service: { icon: 'Promotion', color: '#722ed1', tagType: 'danger', label: '服务API' }
}

const tableNames = [
  'ods_customer_db.customer_info',
  'ods_order_db.order_detail',
  'dwd_trade_db.trade_flow',
  'dws_summary_db.sale_day',
  'ads_report_db.gmv_report',
  'dim_public_db.product_dim',
  'dwd_trade_db.refund_flow',
  'dws_user_db.member_stat',
  'ods_finance_db.payment_record',
  'dws_summary_db.channel_stat'
]

const fieldNames = ['customer_id', 'order_amount', 'pay_channel', 'member_level', 'refund_reason', 'sku_price', 'user_age', 'visit_source', 'order_no', 'create_time']
const fieldTypes = ['BIGINT', 'DECIMAL(18,2)', 'VARCHAR(32)', 'INT', 'VARCHAR(255)', 'DECIMAL(10,2)', 'INT', 'VARCHAR(64)', 'VARCHAR(64)', 'DATETIME']

const accessTaskNames = ['sync_customer_info_full', 'sync_order_detail_incr', 'sync_trade_flow_rt', 'sync_payment_record_hourly', 'sync_member_level_daily']
const qualityRuleNames = ['非空校验-customer_id', '值域校验-order_amount', '唯一性校验-order_no', '格式校验-phone', '一致性校验-refund_amount']
const serviceNames = ['/api/v1/customer/query', '/api/v1/order/list', '/api/v1/stat/gmv', '/api/v1/member/profile', '/api/v1/product/detail']

const mockResults = [
  ...tableNames.map((name, i) => ({
    id: 'table_' + i,
    name,
    type: 'table',
    ...typeMeta.table,
    layer: pick(layerOptions),
    database: name.split('.')[0],
    comment: pick(['客户基础信息表，存储客户全量画像数据', '订单明细表，记录每笔订单的商品明细', '交易流水事实表，支撑经营分析', '销售日汇总表，按天聚合销售指标', 'GMV报表表，面向管理层经营看板']),
    owner: pick(['张伟', '李娜', '王强', '刘洋', '陈静']),
    updatedAt: dateStr(int(0, 60)),
    hot: int(1, 99),
    score: +(80 + rnd() * 20).toFixed(1),
    domain: pick(domainOptions),
    fieldType: '',
    tableName: ''
  })),
  ...fieldNames.map((name, i) => ({
    id: 'field_' + i,
    name,
    type: 'field',
    ...typeMeta.field,
    fieldType: fieldTypes[i],
    tableName: pick(tableNames),
    database: '',
    layer: '',
    comment: pick(['客户唯一标识，关联主题', '订单总金额，单位：元', '支付渠道编码', '会员等级编码', '退款原因说明', '商品SKU售价', '用户年龄', '访问来源渠道']),
    owner: pick(['张伟', '李娜', '王强', '刘洋', '陈静']),
    updatedAt: dateStr(int(0, 60)),
    hot: int(1, 99),
    score: +(75 + rnd() * 20).toFixed(1),
    domain: pick(domainOptions)
  })),
  ...accessTaskNames.map((name, i) => ({
    id: 'access_' + i,
    name,
    type: 'access',
    ...typeMeta.access,
    layer: '',
    database: '',
    tableName: pick(tableNames),
    comment: pick(['全量同步客户基础信息', '增量同步订单明细数据', '实时同步交易流水', '每小时同步支付记录', '每日同步会员等级']),
    owner: pick(['张伟', '李娜', '王强', '刘洋', '陈静']),
    updatedAt: dateStr(int(0, 30)),
    hot: int(1, 99),
    score: +(70 + rnd() * 25).toFixed(1),
    domain: pick(domainOptions),
    fieldType: ''
  })),
  ...qualityRuleNames.map((name, i) => ({
    id: 'quality_' + i,
    name,
    type: 'quality',
    ...typeMeta.quality,
    layer: '',
    database: '',
    tableName: pick(tableNames),
    comment: pick(['校验关键字段非空', '校验数值字段取值范围', '校验主键唯一性', '校验手机号格式合法性', '校验退款金额一致性']),
    owner: pick(['张伟', '李娜', '王强', '刘洋', '陈静']),
    updatedAt: dateStr(int(0, 40)),
    hot: int(1, 99),
    score: +(72 + rnd() * 22).toFixed(1),
    domain: pick(domainOptions),
    fieldType: ''
  })),
  ...serviceNames.map((name, i) => ({
    id: 'service_' + i,
    name,
    type: 'service',
    ...typeMeta.service,
    layer: '',
    database: '',
    tableName: pick(tableNames),
    comment: pick(['客户信息查询服务', '订单列表查询服务', 'GMV统计聚合服务', '会员画像查询服务', '商品详情查询服务']),
    owner: pick(['张伟', '李娜', '王强', '刘洋', '陈静']),
    updatedAt: dateStr(int(0, 20)),
    hot: int(1, 99),
    score: +(78 + rnd() * 20).toFixed(1),
    domain: pick(domainOptions),
    fieldType: ''
  }))
]

const totalCount = computed(() => mockResults.length)
const tableCount = computed(() => mockResults.filter(r => r.type === 'table').length)
const fieldCount = computed(() => mockResults.filter(r => r.type === 'field').length)
const accessCount = computed(() => mockResults.filter(r => r.type === 'access').length)
const qualityCount = computed(() => mockResults.filter(r => r.type === 'quality').length)
const serviceCount = computed(() => mockResults.filter(r => r.type === 'service').length)

const filteredResults = computed(() => {
  let list = mockResults.filter(r => {
    if (activeTab.value !== 'all' && r.type !== activeTab.value) return false
    if (selectedTypes.value.length && !selectedTypes.value.includes(r.type)) return false
    if (selectedLayers.value.length && r.layer && !selectedLayers.value.includes(r.layer)) return false
    if (selectedDomains.value.length && !selectedDomains.value.includes(r.domain)) return false
    if (lastKeyword.value) {
      const kw = lastKeyword.value.toLowerCase()
      return r.name.toLowerCase().includes(kw) || r.comment.includes(lastKeyword.value)
    }
    return true
  })
  if (sortBy.value === 'relevance') list.sort((a, b) => b.score - a.score)
  if (sortBy.value === 'hot') list.sort((a, b) => b.hot - a.hot)
  if (sortBy.value === 'time') list.sort((a, b) => b.updatedAt.localeCompare(a.updatedAt))
  return list
})

const pagedResults = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredResults.value.slice(start, start + pageSize.value)
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

function toggleType(val) {
  const idx = selectedTypes.value.indexOf(val)
  if (idx > -1) selectedTypes.value.splice(idx, 1)
  else selectedTypes.value.push(val)
  doSearch()
}

function toggleLayer(val) {
  const idx = selectedLayers.value.indexOf(val)
  if (idx > -1) selectedLayers.value.splice(idx, 1)
  else selectedLayers.value.push(val)
  doSearch()
}

function toggleDomain(val) {
  const idx = selectedDomains.value.indexOf(val)
  if (idx > -1) selectedDomains.value.splice(idx, 1)
  else selectedDomains.value.push(val)
  doSearch()
}

doSearch()
</script>

<style scoped>
.se-search-card {
  background: linear-gradient(135deg, #f5f9ff 0%, #e8f0ff 100%);
  border: 1px solid #d6e4ff;
}

.se-search-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.se-search-input {
  flex: 1;
}

.se-search-input :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px var(--dp-primary) inset;
}

.se-search-options {
  display: flex;
  gap: 24px;
  align-items: center;
  flex-wrap: wrap;
}

.se-option-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.se-option-label {
  color: var(--dp-text-3);
  white-space: nowrap;
}

.se-filter-collapse {
  --el-collapse-border-color: transparent;
}

.se-filter-collapse :deep(.el-collapse-item__header) {
  font-weight: 600;
  color: var(--dp-text-1);
  border-bottom: none;
}

.se-filter-collapse :deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

.se-filter-collapse :deep(.el-collapse-item__content) {
  padding-bottom: 0;
}

.se-filter-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.se-filter-block {
  background: var(--dp-bg-page);
  border-radius: 8px;
  padding: 12px;
}

.se-filter-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-2);
  margin-bottom: 10px;
}

.se-filter-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.se-filter-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.se-result-tabs {
  margin-bottom: 4px;
}

.se-result-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.se-tab-count {
  display: inline-block;
  background: #f2f3f5;
  color: var(--dp-text-3);
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  margin-left: 4px;
  font-weight: 400;
}

.se-result-item {
  display: flex;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--dp-border);
  border-radius: 8px;
  margin-bottom: 10px;
  background: #fff;
  transition: all 0.2s;
  cursor: pointer;
}

.se-result-item:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 2px 12px rgba(22, 93, 255, 0.08);
}

.se-result-left {
  flex-shrink: 0;
}

.se-result-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.se-result-main {
  flex: 1;
  min-width: 0;
}

.se-result-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.se-result-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  font-family: 'JetBrains Mono', Consolas, monospace;
}

.se-result-score {
  margin-left: auto;
  font-size: 12px;
  color: var(--dp-text-3);
}

.se-result-score b {
  color: var(--dp-primary);
  font-size: 14px;
  margin-left: 2px;
}

.se-result-comment {
  font-size: 13px;
  color: var(--dp-text-2);
  margin-bottom: 8px;
  line-height: 1.5;
}

.se-result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: var(--dp-text-3);
}

.se-result-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.se-result-right {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

@media (max-width: 992px) {
  .se-filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
