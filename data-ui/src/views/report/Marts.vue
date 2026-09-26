<template>
  <div class="dp-page">
    <PageHeader title="数据集市" desc="按业务域组织的数据集市场，支持搜索、预览和收藏">
      <el-button type="primary" :icon="Plus">新建数据集</el-button>
    </PageHeader>

    <div class="rp-marts-layout">
      <!-- 左侧分类树 -->
      <div class="rp-marts-sidebar">
        <div class="dp-card" style="height: 100%; margin-bottom: 0;">
          <div class="rp-sidebar-title">
            <el-icon><FolderOpened /></el-icon>
            <span>集市分类</span>
          </div>
          <el-input
            v-model="treeKeyword"
            placeholder="搜索分类"
            size="small"
            clearable
            :prefix-icon="Search"
            style="margin-bottom: 12px"
          />
          <el-tree
            :data="categoryTree"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            default-expand-all
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            @node-click="handleNodeClick"
            highlight-current
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <el-icon :size="14" :color="data.color">
                  <component :is="data.icon || 'Folder'" />
                </el-icon>
                <span class="tree-label">{{ data.name }}</span>
                <span class="tree-count">{{ data.count }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧数据集列表 -->
      <div class="rp-marts-main">
        <div class="dp-card">
          <div class="dp-toolbar">
            <el-input v-model="keyword" placeholder="搜索数据集名称" clearable style="width: 260px" :prefix-icon="Search" />
            <el-select v-model="typeFilter" placeholder="数据类型" clearable style="width: 130px">
              <el-option label="数据表" value="table" />
              <el-option label="指标集" value="metric" />
              <el-option label="维度表" value="dimension" />
              <el-option label="API数据集" value="api" />
            </el-select>
            <el-select v-model="sortBy" placeholder="排序方式" style="width: 130px">
              <el-option label="更新时间" value="updatedAt" />
              <el-option label="访问量" value="visits" />
              <el-option label="数据量" value="rows" />
            </el-select>
            <div class="dp-toolbar-right">
              <el-radio-group v-model="viewMode" size="small">
                <el-radio-button value="card">
                  <el-icon><Grid /></el-icon>
                </el-radio-button>
                <el-radio-button value="list">
                  <el-icon><List /></el-icon>
                </el-radio-button>
              </el-radio-group>
            </div>
          </div>

          <!-- 卡片视图 -->
          <div v-if="viewMode === 'card'" class="rp-dataset-grid">
            <div
              v-for="item in pagedData"
              :key="item.id"
              class="rp-dataset-card dp-fade-up"
              @click="handlePreview(item)"
            >
              <div class="rp-card-header">
                <div class="rp-card-icon" :style="{ background: item.bgColor, color: item.iconColor }">
                  <el-icon :size="20"><component :is="item.iconName" /></el-icon>
                </div>
                <el-tag size="small" :type="item.typeTag" effect="plain">{{ item.typeName }}</el-tag>
              </div>
              <div class="rp-card-title">{{ item.name }}</div>
              <div class="rp-card-desc">{{ item.desc }}</div>
              <div class="rp-card-stats">
                <div class="rp-stat-item">
                  <el-icon><DataLine /></el-icon>
                  <span>{{ formatNum(item.rows) }} 行</span>
                </div>
                <div class="rp-stat-item">
                  <el-icon><Menu /></el-icon>
                  <span>{{ item.fields }} 字段</span>
                </div>
              </div>
              <div class="rp-card-footer">
                <div class="rp-card-owner">
                  <el-avatar :size="20" :style="{ background: avatarColor(item.owner) }">
                    {{ item.owner.charAt(0) }}
                  </el-avatar>
                  <span>{{ item.owner }}</span>
                </div>
                <div class="rp-card-time">{{ item.updatedAt.slice(5, 10) }}</div>
              </div>
              <div class="rp-card-actions">
                <el-button size="small" type="primary" plain @click.stop="handlePreview(item)">
                  <el-icon><View /></el-icon>
                  预览
                </el-button>
                <el-button size="small" @click.stop="handleFavorite(item)">
                  <el-icon :color="item.favorited ? '#f53f3f' : ''">
                    <component :is="item.favorited ? 'StarFilled' : 'Star'" />
                  </el-icon>
                  {{ item.favorited ? '已收藏' : '收藏' }}
                </el-button>
              </div>
            </div>
          </div>

          <!-- 列表视图 -->
          <el-table v-else :data="pagedData" border stripe>
            <el-table-column prop="name" label="数据集名称" min-width="200" show-overflow-tooltip />
            <el-table-column prop="typeName" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.typeTag" effect="plain">{{ row.typeName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="rows" label="数据量" width="120" align="right">
              <template #default="{ row }">{{ formatNum(row.rows) }}</template>
            </el-table-column>
            <el-table-column prop="fields" label="字段数" width="100" align="center" />
            <el-table-column prop="visits" label="访问量" width="100" align="center" />
            <el-table-column prop="owner" label="负责人" width="90" />
            <el-table-column prop="updatedAt" label="更新时间" width="160" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handlePreview(row)">预览</el-button>
                <el-button link type="primary" size="small">
                  {{ row.favorited ? '已收藏' : '收藏' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="filteredData.length"
            :page-sizes="[12, 24, 48]"
            layout="total, sizes, prev, pager, next, jumper"
          />
        </div>
      </div>
    </div>

    <!-- 数据集详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="数据集详情" size="560px" destroy-on-close>
      <div v-if="currentDataset" class="rp-dataset-detail">
        <div class="rp-detail-header">
          <div class="rp-detail-icon" :style="{ background: currentDataset.bgColor, color: currentDataset.iconColor }">
            <el-icon :size="28"><component :is="currentDataset.iconName" /></el-icon>
          </div>
          <div class="rp-detail-info">
            <h3 class="rp-detail-title">{{ currentDataset.name }}</h3>
            <div class="rp-detail-meta">
              <el-tag size="small" :type="currentDataset.typeTag" effect="plain">{{ currentDataset.typeName }}</el-tag>
              <span class="rp-meta-item">
                <el-icon><User /></el-icon>
                {{ currentDataset.owner }}
              </span>
              <span class="rp-meta-item">
                <el-icon><Clock /></el-icon>
                {{ currentDataset.updatedAt }}
              </span>
            </div>
          </div>
        </div>

        <div class="rp-detail-desc">{{ currentDataset.desc }}</div>

        <!-- 质量评分 -->
        <div class="rp-quality-section">
          <div class="rp-quality-title">数据质量评分</div>
          <div class="rp-quality-score">
            <div class="rp-score-ring">
              <svg viewBox="0 0 100 100" width="80" height="80">
                <circle cx="50" cy="50" r="42" fill="none" stroke="#f2f3f5" stroke-width="8" />
                <circle
                  cx="50" cy="50" r="42" fill="none"
                  :stroke="qualityColor(currentDataset.quality)"
                  stroke-width="8"
                  stroke-linecap="round"
                  :stroke-dasharray="`${currentDataset.quality * 2.64} 264`"
                  transform="rotate(-90 50 50)"
                />
              </svg>
              <span class="rp-score-value">{{ currentDataset.quality }}</span>
            </div>
            <div class="rp-quality-items">
              <div class="rp-quality-item">
                <span>完整性</span>
                <el-progress :percentage="95" :stroke-width="6" :color="'#00b42a'" />
              </div>
              <div class="rp-quality-item">
                <span>准确性</span>
                <el-progress :percentage="92" :stroke-width="6" :color="'#165dff'" />
              </div>
              <div class="rp-quality-item">
                <span>及时性</span>
                <el-progress :percentage="88" :stroke-width="6" :color="'#ff7d00'" />
              </div>
            </div>
          </div>
        </div>

        <!-- 字段列表 -->
        <div class="rp-fields-section">
          <div class="rp-section-header">
            <span class="rp-section-title">字段列表</span>
            <span class="rp-section-count">共 {{ fieldList.length }} 个字段</span>
          </div>
          <el-table :data="fieldList.slice(0, 10)" border stripe size="small">
            <el-table-column prop="name" label="字段名" width="140" class-name="dp-mono" />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" type="info" effect="plain">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="desc" label="描述" min-width="120" show-overflow-tooltip />
          </el-table>
        </div>

        <!-- 数据样例 -->
        <div class="rp-sample-section">
          <div class="rp-section-header">
            <span class="rp-section-title">数据样例</span>
            <el-button link type="primary" size="small">查看全部</el-button>
          </div>
          <el-table :data="sampleData" border stripe size="small" max-height="200">
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="名称" min-width="100" />
            <el-table-column prop="value" label="数值" width="100" align="right" />
            <el-table-column prop="date" label="日期" width="110" />
          </el-table>
        </div>

        <div class="rp-detail-footer">
          <el-button @click="drawerVisible = false">关闭</el-button>
          <el-button type="primary" :icon="View">预览数据</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { dataSources, owners } from '@/mock'
import { Plus, Search, View } from '@element-plus/icons-vue'

const keyword = ref('')
const treeKeyword = ref('')
const typeFilter = ref('')
const sortBy = ref('updatedAt')
const viewMode = ref('card')
const currentPage = ref(1)
const pageSize = ref(12)
const currentCategory = ref('all')
const drawerVisible = ref(false)
const currentDataset = ref(null)

const categoryTree = [
  { id: 'all', name: '全部数据集', count: 36, icon: 'FolderOpened', color: '#165dff' },
  {
    id: 'sales', name: '销售域', count: 12, icon: 'ShoppingCart', color: '#ff7d00',
    children: [
      { id: 'sales-order', name: '订单数据', count: 5, icon: 'Document', color: '#ff7d00' },
      { id: 'sales-product', name: '商品数据', count: 4, icon: 'Goods', color: '#ff7d00' },
      { id: 'sales-channel', name: '渠道数据', count: 3, icon: 'Connection', color: '#ff7d00' }
    ]
  },
  {
    id: 'customer', name: '客户域', count: 8, icon: 'User', color: '#722ed1',
    children: [
      { id: 'customer-base', name: '客户基础', count: 3, icon: 'UserFilled', color: '#722ed1' },
      { id: 'customer-tag', name: '客户标签', count: 5, icon: 'CollectionTag', color: '#722ed1' }
    ]
  },
  {
    id: 'finance', name: '财务域', count: 6, icon: 'Wallet', color: '#00b42a',
    children: [
      { id: 'finance-income', name: '收入数据', count: 3, icon: 'Money', color: '#00b42a' },
      { id: 'finance-cost', name: '成本数据', count: 3, icon: 'Cost', color: '#00b42a' }
    ]
  },
  { id: 'operation', name: '运营域', count: 5, icon: 'TrendCharts', color: '#0fc6c2' },
  { id: 'supply', name: '供应链域', count: 5, icon: 'Box', color: '#f759ab' }
]

const typeMap = {
  table: { name: '数据表', tag: 'primary', icon: 'Grid', bg: '#e8f3ff', color: '#165dff' },
  metric: { name: '指标集', tag: 'success', icon: 'DataLine', bg: '#e8ffea', color: '#00b42a' },
  dimension: { name: '维度表', tag: 'warning', icon: 'Menu', bg: '#fff3e8', color: '#ff7d00' },
  api: { name: 'API数据集', tag: 'danger', icon: 'Connection', bg: '#ffece8', color: '#f53f3f' }
}

const datasetList = computed(() => {
  const types = Object.keys(typeMap)
  const names = [
    '销售订单明细表', '客户画像宽表', '商品销售汇总表', '月度GMV指标集',
    '渠道转化漏斗表', '会员等级维度表', '区域销售统计表', '日活用户指标集',
    '财务收入明细表', '库存周转统计表', '用户行为明细表', '营销活动效果表',
    '退款原因分析表', '门店业绩排名表', '新老客占比指标集', '品类销售占比表',
    '客户价值分层表', '订单履约时效表', '供应商交付统计表', '采购成本明细表'
  ]
  const descs = [
    '包含所有销售订单的明细数据，支持按时间、渠道、区域多维度分析',
    '基于客户基础信息和行为数据构建的360度客户画像',
    '按商品维度汇总的销售数据，含销量、销售额、毛利等指标',
    '月度GMV核心经营指标集合，含同比环比分析',
    '用户从访问到下单的全链路转化数据',
    '会员等级体系维度数据，支持会员权益分析',
    '按区域维度统计的销售数据，含省市县三级',
    '日活跃用户相关指标，含新增、留存、流失等',
    '财务确认收入明细，按业务线、产品、区域维度',
    '库存周转效率统计，含周转天数、动销率等指标',
    '用户在平台内的全链路行为埋点数据',
    '营销活动投入产出效果分析数据',
    '退款申请明细及原因分类统计',
    '门店维度业绩排名及对比分析',
    '新老客户占比趋势及价值对比',
    '商品品类销售结构及占比分析',
    '客户RFM价值分层结果数据',
    '订单履约全流程时效统计',
    '供应商交付及时率及质量评分',
    '采购成本明细及比价分析'
  ]
  return names.map((name, i) => {
    const type = types[i % types.length]
    const t = typeMap[type]
    return {
      id: 1000 + i,
      name,
      type,
      typeName: t.name,
      typeTag: t.tag,
      iconName: t.icon,
      bgColor: t.bg,
      iconColor: t.color,
      desc: descs[i],
      rows: Math.floor(Math.random() * 9000000) + 10000,
      fields: Math.floor(Math.random() * 40) + 8,
      visits: Math.floor(Math.random() * 5000) + 100,
      owner: owners[i % owners.length],
      updatedAt: `2026-0${(i % 9) + 1}-${String((i % 28) + 1).padStart(2, '0')} ${String((i % 24)).padStart(2, '0')}:${String((i * 7) % 60).padStart(2, '0')}:00`,
      favorited: i % 5 === 0,
      quality: Math.floor(Math.random() * 15) + 85,
      category: ['sales', 'customer', 'finance', 'operation', 'supply'][i % 5]
    }
  })
})

const filteredData = computed(() => {
  let list = datasetList.value
  if (keyword.value) {
    list = list.filter(d => d.name.includes(keyword.value) || d.desc.includes(keyword.value))
  }
  if (typeFilter.value) {
    list = list.filter(d => d.type === typeFilter.value)
  }
  if (currentCategory.value && currentCategory.value !== 'all') {
    list = list.filter(d => d.category === currentCategory.value || d.category.startsWith(currentCategory.value))
  }
  return [...list].sort((a, b) => {
    if (sortBy.value === 'visits') return b.visits - a.visits
    if (sortBy.value === 'rows') return b.rows - a.rows
    return new Date(b.updatedAt) - new Date(a.updatedAt)
  })
})

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

const fieldList = [
  { name: 'id', type: 'BIGINT', desc: '主键ID' },
  { name: 'order_no', type: 'VARCHAR(32)', desc: '订单编号' },
  { name: 'customer_id', type: 'BIGINT', desc: '客户ID' },
  { name: 'product_id', type: 'BIGINT', desc: '商品ID' },
  { name: 'quantity', type: 'INT', desc: '购买数量' },
  { name: 'amount', type: 'DECIMAL(16,2)', desc: '订单金额' },
  { name: 'channel', type: 'VARCHAR(20)', desc: '下单渠道' },
  { name: 'status', type: 'TINYINT', desc: '订单状态' },
  { name: 'create_time', type: 'DATETIME', desc: '创建时间' },
  { name: 'update_time', type: 'DATETIME', desc: '更新时间' },
  { name: 'pay_time', type: 'DATETIME', desc: '支付时间' },
  { name: 'remark', type: 'VARCHAR(255)', desc: '备注' }
]

const sampleData = [
  { id: 1, name: 'iPhone 15 Pro', value: '8999.00', date: '2026-09-25' },
  { id: 2, name: 'MacBook Air', value: '9499.00', date: '2026-09-24' },
  { id: 3, name: 'AirPods Pro', value: '1899.00', date: '2026-09-24' },
  { id: 4, name: 'iPad Pro', value: '6799.00', date: '2026-09-23' },
  { id: 5, name: 'Apple Watch', value: '2999.00', date: '2026-09-22' }
]

function filterNode(value, data) {
  if (!value) return true
  return data.name.includes(value)
}

watch(treeKeyword, val => {
  // tree filter handled by element tree ref if needed
})

function handleNodeClick(data) {
  currentCategory.value = data.id
  currentPage.value = 1
}

function formatNum(num) {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}

function avatarColor(name) {
  const colors = ['#165dff', '#00b42a', '#ff7d00', '#722ed1', '#0fc6c2', '#f759ab', '#f53f3f']
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

function qualityColor(score) {
  if (score >= 90) return '#00b42a'
  if (score >= 80) return '#ff7d00'
  return '#f53f3f'
}

function handlePreview(item) {
  currentDataset.value = item
  drawerVisible.value = true
}

function handleFavorite(item) {
  item.favorited = !item.favorited
  ElMessage.success(item.favorited ? '已收藏' : '已取消收藏')
}
</script>

<style scoped>
.rp-marts-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 140px);
  min-height: 600px;
}

.rp-marts-sidebar {
  width: 240px;
  flex-shrink: 0;
}

.rp-marts-main {
  flex: 1;
  min-width: 0;
  overflow: auto;
}

.rp-sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 12px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  font-size: 13px;
}

.tree-label {
  flex: 1;
}

.tree-count {
  font-size: 11px;
  color: var(--dp-text-3);
  background: #f2f3f5;
  padding: 1px 6px;
  border-radius: 10px;
}

.rp-dataset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.rp-dataset-card {
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.25s;
  position: relative;
}

.rp-dataset-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 4px 12px rgba(22, 93, 255, 0.12);
  transform: translateY(-2px);
}

.rp-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.rp-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rp-card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rp-card-desc {
  font-size: 12px;
  color: var(--dp-text-3);
  line-height: 1.5;
  height: 36px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 12px;
}

.rp-card-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  padding: 10px 0;
  border-top: 1px solid var(--dp-border-light);
  border-bottom: 1px solid var(--dp-border-light);
}

.rp-stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--dp-text-2);
}

.rp-stat-item .el-icon {
  color: var(--dp-text-3);
}

.rp-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 12px;
}

.rp-card-owner {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rp-card-actions {
  display: flex;
  gap: 8px;
}

.rp-card-actions .el-button {
  flex: 1;
}

.rp-dataset-detail {
  padding: 0 4px;
}

.rp-detail-header {
  display: flex;
  gap: 14px;
  margin-bottom: 16px;
}

.rp-detail-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rp-detail-info {
  flex: 1;
  min-width: 0;
}

.rp-detail-title {
  margin: 0 0 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.rp-detail-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: var(--dp-text-3);
}

.rp-meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rp-detail-desc {
  font-size: 13px;
  color: var(--dp-text-2);
  line-height: 1.6;
  padding: 12px;
  background: #fafbfc;
  border-radius: 8px;
  margin-bottom: 20px;
}

.rp-quality-section,
.rp-fields-section,
.rp-sample-section {
  margin-bottom: 20px;
}

.rp-quality-title,
.rp-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 12px;
}

.rp-quality-score {
  display: flex;
  align-items: center;
  gap: 24px;
}

.rp-score-ring {
  position: relative;
  width: 80px;
  height: 80px;
  flex-shrink: 0;
}

.rp-score-value {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 20px;
  font-weight: 700;
  color: var(--dp-text-1);
}

.rp-quality-items {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rp-quality-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
  color: var(--dp-text-2);
}

.rp-quality-item span {
  width: 50px;
  flex-shrink: 0;
}

.rp-quality-item .el-progress {
  flex: 1;
}

.rp-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.rp-section-count {
  font-size: 12px;
  color: var(--dp-text-3);
}

.rp-detail-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid var(--dp-border-light);
}

@media (max-width: 1200px) {
  .rp-marts-sidebar {
    display: none;
  }
}
</style>
