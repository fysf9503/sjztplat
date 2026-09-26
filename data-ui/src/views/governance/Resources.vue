<template>
  <div class="governance-resources">
    <PageHeader title="资源目录" desc="企业数据资源统一目录，支持按业务域、数据域分类浏览和检索">
      <el-button type="primary" :icon="Plus">新增资源</el-button>
    </PageHeader>

    <div class="main-layout">
      <!-- 左侧分类树 -->
      <div class="sidebar dp-card dp-fade-up">
        <div class="sidebar-header">
          <span class="sidebar-title">资源分类</span>
          <el-radio-group v-model="treeMode" size="small">
            <el-radio-button value="biz">业务域</el-radio-button>
            <el-radio-button value="data">数据域</el-radio-button>
          </el-radio-group>
        </div>
        <div class="tree-search">
          <el-input v-model="treeKeyword" placeholder="搜索分类" size="small" clearable :prefix-icon="Search" />
        </div>
        <el-tree
          :data="currentTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          default-expand-all
          highlight-current
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <span class="tree-node">
              <el-icon v-if="data.icon" :size="14" :style="{ color: data.color }">
                <component :is="data.icon" />
              </el-icon>
              <span class="tree-label">{{ data.name }}</span>
              <span class="tree-count">{{ data.count }}</span>
            </span>
          </template>
        </el-tree>
      </div>

      <!-- 右侧主区域 -->
      <div class="main-content">
        <div class="dp-card dp-fade-up">
          <div class="dp-toolbar">
            <el-input
              v-model="query.keyword"
              placeholder="搜索资源名称/编码"
              clearable
              style="width: 240px"
              :prefix-icon="Search"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
            <el-select v-model="query.type" placeholder="资源类型" clearable style="width: 140px" @change="handleSearch">
              <el-option label="数据表" value="数据表" />
              <el-option label="数据接口" value="数据接口" />
              <el-option label="报表资源" value="报表资源" />
              <el-option label="指标资源" value="指标资源" />
              <el-option label="文件资源" value="文件资源" />
            </el-select>
            <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="handleSearch">
              <el-option label="已发布" value="已发布" />
              <el-option label="待审核" value="待审核" />
              <el-option label="已下线" value="已下线" />
            </el-select>
            <div class="toolbar-right">
              <span class="total-info">共 <b>{{ total }}</b> 项资源</span>
              <el-button :icon="Refresh" @click="handleSearch">刷新</el-button>
            </div>
          </div>

          <el-table :data="pagedList" border stripe v-loading="loading">
            <el-table-column prop="name" label="资源名称" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="resource-name-cell">
                  <div class="resource-icon-sm" :style="{ background: typeBg(row.type), color: typeColor(row.type) }">
                    <el-icon :size="14"><component :is="typeIcon(row.type)" /></el-icon>
                  </div>
                  <div>
                    <div class="name-text dp-link" @click="handleView(row)">{{ row.name }}</div>
                    <div class="code-text dp-mono">{{ row.code }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="资源类型" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="typeTagType(row.type)" effect="plain">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="category" label="所属分类" width="120" />
            <el-table-column prop="dataSize" label="数据量" width="100" align="right">
              <template #default="{ row }">{{ formatSize(row.dataSize) }}</template>
            </el-table-column>
            <el-table-column prop="visitCount" label="访问量" width="100" align="right">
              <template #default="{ row }">
                <span class="visit-count">{{ formatNum(row.visitCount) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <span class="status-dot" :class="statusDotClass(row.status)"></span>
                {{ row.status }}
              </template>
            </el-table-column>
            <el-table-column prop="owner" label="负责人" width="90" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleView(row)">查看详情</el-button>
                <el-button link type="primary" size="small" @click="handleApply(row)">申请权限</el-button>
                <el-button link type="warning" size="small" @click="handleFavorite(row)">
                  {{ row.favorited ? '已收藏' : '收藏' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSearch"
            @current-change="handleSearch"
          />
        </div>
      </div>
    </div>

    <!-- 资源详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="currentResource?.name" size="560px" destroy-on-close>
      <template v-if="currentResource">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="资源名称">{{ currentResource.name }}</el-descriptions-item>
          <el-descriptions-item label="资源编码">
            <span class="dp-mono">{{ currentResource.code }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="资源类型">{{ currentResource.type }}</el-descriptions-item>
          <el-descriptions-item label="所属分类">{{ currentResource.category }}</el-descriptions-item>
          <el-descriptions-item label="数据量">{{ formatSize(currentResource.dataSize) }}</el-descriptions-item>
          <el-descriptions-item label="访问量">{{ formatNum(currentResource.visitCount) }} 次</el-descriptions-item>
          <el-descriptions-item label="状态">
            <span class="status-dot" :class="statusDotClass(currentResource.status)"></span>
            {{ currentResource.status }}
          </el-descriptions-item>
          <el-descriptions-item label="负责人">{{ currentResource.owner }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentResource.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ currentResource.updateTime }}</el-descriptions-item>
          <el-descriptions-item label="资源描述" :span="2">{{ currentResource.desc }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <div class="section-title">资源标签</div>
          <div class="tag-list">
            <el-tag v-for="tag in currentResource.tags" :key="tag" size="small" effect="plain" style="margin: 4px">
              {{ tag }}
            </el-tag>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">访问统计（近30天）</div>
          <EChart :option="visitChartOption" height="180px" />
        </div>

        <div class="detail-actions">
          <el-button type="primary" :icon="View">查看详情</el-button>
          <el-button :icon="Key">申请权限</el-button>
          <el-button :icon="Star">收藏</el-button>
          <el-button :icon="Download">下载</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EChart from '@/components/EChart.vue'
import { Download, Key, Plus, Refresh, Search, Star, View } from '@element-plus/icons-vue'

const loading = ref(false)
const detailVisible = ref(false)
const currentResource = ref(null)
const treeMode = ref('biz')
const treeKeyword = ref('')

const query = reactive({
  keyword: '',
  type: '',
  status: '',
  category: '',
  page: 1,
  size: 10
})

const bizTree = [
  { id: 'all', name: '全部业务域', icon: 'Briefcase', color: '#1664ff', count: 168, children: [
    { id: '零售业务', name: '零售业务', icon: 'ShoppingCart', color: '#1664ff', count: 52 },
    { id: '批发业务', name: '批发业务', icon: 'Shop', color: '#00b42a', count: 36 },
    { id: '金融业务', name: '金融业务', icon: 'Money', color: '#ff7d00', count: 28 },
    { id: '物流业务', name: '物流业务', icon: 'Van', color: '#722ed1', count: 24 },
    { id: '服务业务', name: '服务业务', icon: 'Service', color: '#0fc6c2', count: 28 }
  ]}
]

const dataTree = [
  { id: 'all', name: '全部数据域', icon: 'Grid', color: '#1664ff', count: 168, children: [
    { id: '客户域', name: '客户域', icon: 'User', color: '#1664ff', count: 42 },
    { id: '交易域', name: '交易域', icon: 'Wallet', color: '#00b42a', count: 38 },
    { id: '商品域', name: '商品域', icon: 'Goods', color: '#ff7d00', count: 26 },
    { id: '营销域', name: '营销域', icon: 'Promotion', color: '#722ed1', count: 22 },
    { id: '财务域', name: '财务域', icon: 'Money', color: '#0fc6c2', count: 20 },
    { id: '供应链域', name: '供应链域', icon: 'Box', color: '#f759ab', count: 20 }
  ]}
]

const currentTree = computed(() => treeMode.value === 'biz' ? bizTree : dataTree)

const resourceList = ref([
  { id: 1, name: '客户信息主表', code: 'RES-CUS-001', type: '数据表', category: '客户域', dataSize: 256 * 1024 * 1024, visitCount: 15680, status: '已发布', owner: '张伟', createTime: '2025-03-15', updateTime: '2026-09-26', desc: '客户核心信息表，包含客户基本信息、联系信息、账户信息等。', tags: ['核心资产', '客户主题', '高频访问'], favorited: false },
  { id: 2, name: '订单明细表', code: 'RES-TRA-002', type: '数据表', category: '交易域', dataSize: 1.2 * 1024 * 1024 * 1024, visitCount: 28960, status: '已发布', owner: '李娜', createTime: '2025-03-20', updateTime: '2026-09-26', desc: '订单明细数据表，包含订单商品明细、金额明细、优惠明细等。', tags: ['核心资产', '交易主题'], favorited: true },
  { id: 3, name: '日销售汇总数据', code: 'RES-SUM-003', type: '数据表', category: '交易域', dataSize: 45 * 1024 * 1024, visitCount: 12450, status: '已发布', owner: '王强', createTime: '2025-05-08', updateTime: '2026-09-26', desc: '按日维度汇总的销售数据，支撑经营分析和报表取数。', tags: ['汇总表', '经营分析'], favorited: false },
  { id: 4, name: '客户查询服务', code: 'RES-API-004', type: '数据接口', category: '客户域', dataSize: 0, visitCount: 89600, status: '已发布', owner: '赵磊', createTime: '2025-07-15', updateTime: '2026-09-25', desc: '客户信息查询API服务，支持多维度客户信息检索。', tags: ['数据服务', 'API'], favorited: false },
  { id: 5, name: '经营驾驶舱', code: 'RES-RPT-005', type: '报表资源', category: '交易域', dataSize: 0, visitCount: 6580, status: '已发布', owner: '孙悦', createTime: '2025-08-20', updateTime: '2026-09-25', desc: '企业经营数据总览报表，展示核心经营指标和趋势。', tags: ['核心报表', '管理层'], favorited: true },
  { id: 6, name: 'GMV总指标', code: 'RES-IDX-006', type: '指标资源', category: '交易域', dataSize: 0, visitCount: 18920, status: '已发布', owner: '陈静', createTime: '2025-06-12', updateTime: '2026-09-26', desc: '商品交易总额（Gross Merchandise Volume）指标定义。', tags: ['核心指标', '交易主题'], favorited: false },
  { id: 7, name: '会员画像宽表', code: 'RES-CUS-007', type: '数据表', category: '客户域', dataSize: 320 * 1024 * 1024, visitCount: 9870, status: '已发布', owner: '吴敏', createTime: '2025-10-12', updateTime: '2026-09-26', desc: '会员画像数据宽表，包含基本属性、行为属性、价值属性等。', tags: ['用户画像', '客户主题'], favorited: false },
  { id: 8, name: '商品库存数据', code: 'RES-PRD-008', type: '数据表', category: '商品域', dataSize: 89 * 1024 * 1024, visitCount: 7650, status: '已发布', owner: '刘洋', createTime: '2025-04-10', updateTime: '2026-09-26', desc: '商品库存数据，支持库存查询和库存预警。', tags: ['商品主题', '供应链'], favorited: false },
  { id: 9, name: '销售数据导出', code: 'RES-FIL-009', type: '文件资源', category: '交易域', dataSize: 56 * 1024 * 1024, visitCount: 3240, status: '已发布', owner: '周杰', createTime: '2026-01-15', updateTime: '2026-09-20', desc: '销售数据导出文件，按日更新，支持Excel和CSV格式。', tags: ['数据导出'], favorited: false },
  { id: 10, name: '渠道转化漏斗', code: 'RES-IDX-010', type: '指标资源', category: '营销域', dataSize: 0, visitCount: 5680, status: '已发布', owner: '郑浩', createTime: '2025-11-08', updateTime: '2026-09-26', desc: '各营销渠道的转化漏斗指标，包括曝光、点击、访问、下单、支付。', tags: ['营销分析', '漏斗分析'], favorited: false },
  { id: 11, name: '财务报表数据', code: 'RES-RPT-011', type: '报表资源', category: '财务域', dataSize: 0, visitCount: 2340, status: '待审核', owner: '张伟', createTime: '2026-08-10', updateTime: '2026-09-25', desc: '财务月度报表数据，包含资产负债表、利润表、现金流量表。', tags: ['财务报表', '待审核'], favorited: false },
  { id: 12, name: '用户行为日志', code: 'RES-LOG-012', type: '数据表', category: '营销域', dataSize: 3.5 * 1024 * 1024 * 1024, visitCount: 4560, status: '已下线', owner: '周杰', createTime: '2025-09-10', updateTime: '2026-08-15', desc: '用户行为日志明细数据，已下线，历史数据可申请查看。', tags: ['行为分析', '已下线'], favorited: false }
])

const filteredList = computed(() => {
  return resourceList.value.filter(item => {
    if (query.keyword && !item.name.includes(query.keyword) && !item.code.includes(query.keyword)) return false
    if (query.type && item.type !== query.type) return false
    if (query.status && item.status !== query.status) return false
    if (query.category && query.category !== 'all' && item.category !== query.category) return false
    return true
  })
})

const total = computed(() => filteredList.value.length)

const pagedList = computed(() => {
  const start = (query.page - 1) * query.size
  return filteredList.value.slice(start, start + query.size)
})

const visitChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 40, right: 10, top: 10, bottom: 24 },
  xAxis: {
    type: 'category',
    data: ['09-01', '09-05', '09-10', '09-15', '09-20', '09-25', '09-26'],
    axisLine: { lineStyle: { color: '#e5e6eb' } },
    axisLabel: { fontSize: 11, color: '#86909c' }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f2f3f5' } },
    axisLabel: { fontSize: 11, color: '#86909c' }
  },
  series: [{
    type: 'line',
    smooth: true,
    data: [120, 180, 240, 200, 280, 350, 320],
    lineStyle: { width: 2, color: '#1664ff' },
    itemStyle: { color: '#1664ff' },
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(22, 100, 255, 0.2)' },
          { offset: 1, color: 'rgba(22, 100, 255, 0.02)' }
        ]
      }
    }
  }]
}))

function typeIcon(type) {
  const map = { '数据表': 'Grid', '数据接口': 'Connection', '报表资源': 'DataAnalysis', '指标资源': 'DataLine', '文件资源': 'Document' }
  return map[type] || 'Document'
}

function typeColor(type) {
  const map = { '数据表': '#1664ff', '数据接口': '#00b42a', '报表资源': '#722ed1', '指标资源': '#ff7d00', '文件资源': '#0fc6c2' }
  return map[type] || '#86909c'
}

function typeBg(type) {
  const map = { '数据表': '#e8f0ff', '数据接口': '#e8ffea', '报表资源': '#f5e8ff', '指标资源': '#fff3e8', '文件资源': '#e0fffa' }
  return map[type] || '#f7f8fa'
}

function typeTagType(type) {
  const map = { '数据表': 'primary', '数据接口': 'success', '报表资源': 'danger', '指标资源': 'warning', '文件资源': 'info' }
  return map[type] || 'info'
}

function statusDotClass(status) {
  const map = { '已发布': 'success', '待审核': 'warning', '已下线': 'info' }
  return map[status] || 'info'
}

function formatSize(bytes) {
  if (bytes === 0) return '-'
  if (bytes >= 1024 * 1024 * 1024) return (bytes / 1024 / 1024 / 1024).toFixed(1) + ' GB'
  if (bytes >= 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  if (bytes >= 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return bytes + ' B'
}

function formatNum(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n.toString()
}

function handleNodeClick(data) {
  query.category = data.id === 'all' ? '' : data.id
  query.page = 1
}

function handleSearch() {
  query.page = 1
  loading.value = true
  setTimeout(() => { loading.value = false }, 300)
}

function handleView(row) {
  currentResource.value = row
  detailVisible.value = true
}

function handleApply(row) {
  ElMessage.success(`已提交「${row.name}」的权限申请`)
}

function handleFavorite(row) {
  row.favorited = !row.favorited
  ElMessage.success(row.favorited ? '已收藏' : '已取消收藏')
}
</script>

<style scoped>
.governance-resources {
  padding: 20px;
}

.main-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.sidebar {
  width: 260px;
  flex-shrink: 0;
  padding: 16px;
}

.sidebar-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 12px;
}

.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.tree-search {
  margin-bottom: 12px;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.toolbar-right {
  margin-left: auto;
  display: flex;
  gap: 10px;
  align-items: center;
}

.total-info {
  font-size: 13px;
  color: var(--dp-text-3);
}

.total-info b {
  color: var(--dp-primary);
  font-weight: 600;
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
  background: var(--dp-bg-page);
  padding: 1px 8px;
  border-radius: 10px;
}

.resource-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.resource-icon-sm {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.name-text {
  font-weight: 500;
}

.code-text {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

.visit-count {
  font-weight: 600;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', sans-serif;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.status-dot.success { background: var(--dp-success); }
.status-dot.warning { background: var(--dp-warning); }
.status-dot.info { background: var(--dp-text-4); }

.dp-link {
  color: var(--dp-primary);
  cursor: pointer;
  text-decoration: none;
  font-weight: 500;
}

.dp-link:hover { opacity: 0.75; }

.detail-section {
  margin-top: 16px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 10px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
}

.detail-actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

.detail-actions .el-button {
  flex: 1;
}

.dp-mono {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
}

@keyframes dp-fade-up {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.dp-fade-up { animation: dp-fade-up 0.35s ease both; }
</style>
