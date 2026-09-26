<template>
  <div class="governance-map">
    <PageHeader title="数据地图" desc="全局资产检索与导航，一目了然地发现、理解和使用数据资产">
      <el-radio-group v-model="viewMode" size="small">
        <el-radio-button value="card">卡片视图</el-radio-button>
        <el-radio-button value="list">列表视图</el-radio-button>
      </el-radio-group>
    </PageHeader>

    <div class="map-layout">
      <!-- 左侧分类导航 -->
      <div class="map-sidebar dp-fade-up">
        <div class="sidebar-tabs">
          <div
            v-for="tab in navTabs"
            :key="tab.key"
            class="sidebar-tab"
            :class="{ active: activeNav === tab.key }"
            @click="activeNav = tab.key"
          >
            <el-icon :size="16"><component :is="tab.icon" /></el-icon>
            <span>{{ tab.name }}</span>
          </div>
        </div>

        <div class="sidebar-tree">
          <div class="tree-search">
            <el-input
              v-model="treeKeyword"
              placeholder="搜索分类"
              size="small"
              clearable
              :prefix-icon="Search"
            />
          </div>
          <el-tree
            :data="currentTreeData"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <span class="tree-label">{{ data.name }}</span>
                <span class="tree-count">{{ data.count }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 中间主区域 -->
      <div class="map-main">
        <!-- 顶部搜索栏 -->
        <div class="search-bar dp-card dp-fade-up">
          <div class="search-input-wrap">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索表名、字段名、指标名..."
              size="large"
              clearable
              :prefix-icon="Search"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #append>
                <el-button type="primary" @click="handleSearch">搜索</el-button>
              </template>
            </el-input>
          </div>
          <div class="search-filters">
            <div class="filter-label">类型筛选：</div>
            <div class="filter-tags">
              <span
                v-for="t in typeFilters"
                :key="t.key"
                class="filter-tag"
                :class="{ active: activeTypes.includes(t.key) }"
                @click="toggleType(t.key)"
              >
                <el-icon :size="14"><component :is="t.icon" /></el-icon>
                {{ t.name }}
              </span>
            </div>
          </div>
        </div>

        <!-- 资产统计 -->
        <div class="asset-stats dp-fade-up" style="animation-delay: 0.05s">
          <div class="stat-item">
            <span class="stat-num">{{ filteredAssets.length }}</span>
            <span class="stat-label">个资产</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ tableCount }}</span>
            <span class="stat-label">数据表</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ fieldCount }}</span>
            <span class="stat-label">字段</span>
          </div>
          <div class="stat-item">
            <span class="stat-num">{{ metricCount }}</span>
            <span class="stat-label">指标</span>
          </div>
          <div class="stat-right">
            <el-radio-group v-model="sortBy" size="small">
              <el-radio-button value="hot">热度排序</el-radio-button>
              <el-radio-button value="time">更新时间</el-radio-button>
              <el-radio-button value="name">名称排序</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 资产卡片列表 -->
        <div v-if="viewMode === 'card'" class="asset-grid dp-fade-up" style="animation-delay: 0.1s">
          <div
            v-for="item in pagedAssets"
            :key="item.id"
            class="asset-card"
            :class="{ active: selectedAsset?.id === item.id }"
            @click="selectAsset(item)"
          >
            <div class="card-header">
              <div class="asset-icon" :style="{ background: typeBg(item.type), color: typeColor(item.type) }">
                <el-icon :size="20"><component :is="typeIcon(item.type)" /></el-icon>
              </div>
              <div class="asset-title">
                <div class="asset-name" :title="item.name">{{ item.name }}</div>
                <div class="asset-code dp-mono">{{ item.code }}</div>
              </div>
              <el-tag size="small" :type="typeTagType(item.type)" effect="plain" class="asset-type-tag">
                {{ item.type }}
              </el-tag>
            </div>
            <div class="card-desc">{{ item.desc }}</div>
            <div class="card-meta">
              <div class="meta-item">
                <el-icon :size="12"><Coin /></el-icon>
                <span>{{ item.database }}</span>
              </div>
              <div class="meta-item">
                <el-icon :size="12"><User /></el-icon>
                <span>{{ item.owner }}</span>
              </div>
            </div>
            <div class="card-footer">
              <div class="footer-stats">
                <div class="footer-stat">
                  <span class="footer-stat-num">{{ item.fieldCount }}</span>
                  <span class="footer-stat-label">字段</span>
                </div>
                <div class="footer-stat">
                  <span class="footer-stat-num">{{ formatNum(item.dataSize) }}</span>
                  <span class="footer-stat-label">数据量</span>
                </div>
                <div class="footer-stat hot">
                  <el-icon :size="12"><TrendCharts /></el-icon>
                  <span class="footer-stat-num">{{ item.hotScore }}</span>
                </div>
              </div>
              <div class="footer-quality">
                <div class="quality-score" :style="{ color: qualityColor(item.qualityScore) }">
                  {{ item.qualityScore }}
                </div>
                <div class="quality-label">质量分</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 列表视图 -->
        <div v-else class="asset-list-table dp-card dp-fade-up" style="animation-delay: 0.1s">
          <el-table :data="pagedAssets" border stripe @row-click="selectAsset">
            <el-table-column prop="name" label="资产名称" min-width="200">
              <template #default="{ row }">
                <div class="list-asset-name">
                  <div class="asset-icon-sm" :style="{ background: typeBg(row.type), color: typeColor(row.type) }">
                    <el-icon :size="14"><component :is="typeIcon(row.type)" /></el-icon>
                  </div>
                  <div>
                    <div class="name-text">{{ row.name }}</div>
                    <div class="code-text dp-mono">{{ row.code }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="typeTagType(row.type)" effect="plain">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="database" label="所属数据源" width="160" class-name="dp-mono" />
            <el-table-column prop="fieldCount" label="字段数" width="80" align="center" />
            <el-table-column label="数据量" width="100" align="center">
              <template #default="{ row }">{{ formatNum(row.dataSize) }}</template>
            </el-table-column>
            <el-table-column label="热度" width="90" align="center">
              <template #default="{ row }">
                <el-rate :model-value="Math.ceil(row.hotScore / 20)" disabled size="small" />
              </template>
            </el-table-column>
            <el-table-column label="质量分" width="90" align="center">
              <template #default="{ row }">
                <span class="quality-score-sm" :style="{ color: qualityColor(row.qualityScore) }">
                  {{ row.qualityScore }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="owner" label="负责人" width="90" />
            <el-table-column prop="updateTime" label="更新时间" width="140" />
          </el-table>
        </div>

        <el-pagination
          v-if="filteredAssets.length > pageSize"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="filteredAssets.length"
          layout="prev, pager, next, jumper"
          class="asset-pagination"
        />
      </div>

      <!-- 右侧资产详情面板 -->
      <div class="map-detail" :class="{ show: selectedAsset }">
        <div v-if="selectedAsset" class="detail-panel">
          <div class="detail-header">
            <div class="detail-close" @click="selectedAsset = null">
              <el-icon><Close /></el-icon>
            </div>
            <div class="detail-title-row">
              <div class="asset-icon-lg" :style="{ background: typeBg(selectedAsset.type), color: typeColor(selectedAsset.type) }">
                <el-icon :size="24"><component :is="typeIcon(selectedAsset.type)" /></el-icon>
              </div>
              <div class="detail-title-info">
                <h3 class="detail-name">{{ selectedAsset.name }}</h3>
                <div class="detail-code dp-mono">{{ selectedAsset.code }}</div>
              </div>
            </div>
            <div class="detail-tags">
              <el-tag size="small" :type="typeTagType(selectedAsset.type)" effect="plain">
                {{ selectedAsset.type }}
              </el-tag>
              <el-tag size="small" type="success" effect="plain">
                质量分 {{ selectedAsset.qualityScore }}
              </el-tag>
              <el-tag size="small" type="warning" effect="plain">
                热度 {{ selectedAsset.hotScore }}
              </el-tag>
            </div>
            <p class="detail-desc">{{ selectedAsset.desc }}</p>
          </div>

          <el-tabs v-model="detailTab" class="detail-tabs">
            <!-- 基本信息 -->
            <el-tab-pane label="基本信息" name="basic">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="资产编码">
                  <span class="dp-mono">{{ selectedAsset.code }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="资产类型">{{ selectedAsset.type }}</el-descriptions-item>
                <el-descriptions-item label="所属数据源">{{ selectedAsset.database }}</el-descriptions-item>
                <el-descriptions-item label="所属主题域">{{ selectedAsset.domain }}</el-descriptions-item>
                <el-descriptions-item label="负责人">{{ selectedAsset.owner }}</el-descriptions-item>
                <el-descriptions-item label="所属部门">{{ selectedAsset.dept }}</el-descriptions-item>
                <el-descriptions-item label="字段数量">{{ selectedAsset.fieldCount }} 个</el-descriptions-item>
                <el-descriptions-item label="数据量">{{ formatNum(selectedAsset.dataSize) }} 条</el-descriptions-item>
                <el-descriptions-item label="存储大小">{{ selectedAsset.storageSize }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ selectedAsset.createTime }}</el-descriptions-item>
                <el-descriptions-item label="更新时间">{{ selectedAsset.updateTime }}</el-descriptions-item>
              </el-descriptions>

              <div class="detail-section">
                <div class="section-title">资产标签</div>
                <div class="tag-list">
                  <el-tag v-for="tag in selectedAsset.tags" :key="tag" size="small" effect="plain" style="margin: 4px">
                    {{ tag }}
                  </el-tag>
                </div>
              </div>
            </el-tab-pane>

            <!-- 字段信息 -->
            <el-tab-pane :label="`字段信息 (${selectedAsset.fieldCount})`" name="fields">
              <div class="field-search">
                <el-input v-model="fieldKeyword" placeholder="搜索字段名" size="small" clearable :prefix-icon="Search" />
              </div>
              <el-table :data="filteredFields" border stripe size="small" max-height="400">
                <el-table-column prop="name" label="字段名" width="140" class-name="dp-mono" />
                <el-table-column prop="chineseName" label="中文名" width="110" />
                <el-table-column prop="dataType" label="类型" width="90" />
                <el-table-column prop="nullable" label="可空" width="60" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.nullable ? 'info' : 'danger'" effect="plain">
                      {{ row.nullable ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="desc" label="描述" min-width="120" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>

            <!-- 血缘关系 -->
            <el-tab-pane label="血缘关系" name="lineage">
              <div class="lineage-section">
                <div class="lineage-stat">
                  <div class="lineage-stat-item">
                    <span class="num" style="color: var(--dp-warning)">{{ upstreamCount }}</span>
                    <span class="label">上游资产</span>
                  </div>
                  <div class="lineage-stat-item">
                    <span class="num" style="color: var(--dp-success)">{{ downstreamCount }}</span>
                    <span class="label">下游资产</span>
                  </div>
                </div>

                <div class="lineage-list-title">上游依赖</div>
                <div class="lineage-list">
                  <div v-for="item in upstreamList" :key="item.id" class="lineage-item">
                    <div class="lineage-icon up">
                      <el-icon :size="14"><ArrowUp /></el-icon>
                    </div>
                    <div class="lineage-info">
                      <div class="lineage-name">{{ item.name }}</div>
                      <div class="lineage-code dp-mono">{{ item.code }}</div>
                    </div>
                    <el-tag size="small" :type="typeTagType(item.type)" effect="plain">{{ item.type }}</el-tag>
                  </div>
                </div>

                <div class="lineage-list-title">下游影响</div>
                <div class="lineage-list">
                  <div v-for="item in downstreamList" :key="item.id" class="lineage-item">
                    <div class="lineage-icon down">
                      <el-icon :size="14"><ArrowDown /></el-icon>
                    </div>
                    <div class="lineage-info">
                      <div class="lineage-name">{{ item.name }}</div>
                      <div class="lineage-code dp-mono">{{ item.code }}</div>
                    </div>
                    <el-tag size="small" :type="typeTagType(item.type)" effect="plain">{{ item.type }}</el-tag>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <!-- 质量信息 -->
            <el-tab-pane label="质量信息" name="quality">
              <div class="quality-overview">
                <div class="quality-score-lg" :style="{ color: qualityColor(selectedAsset.qualityScore) }">
                  {{ selectedAsset.qualityScore }}
                </div>
                <div class="quality-level">
                  {{ selectedAsset.qualityScore >= 95 ? '优秀' : selectedAsset.qualityScore >= 85 ? '良好' : '待改进' }}
                </div>
              </div>

              <div class="quality-dimensions">
                <div v-for="dim in qualityDimensions" :key="dim.name" class="quality-dim">
                  <div class="dim-header">
                    <span class="dim-name">{{ dim.name }}</span>
                    <span class="dim-score" :style="{ color: qualityColor(dim.score) }">{{ dim.score }}</span>
                  </div>
                  <el-progress :percentage="dim.score" :color="qualityColor(dim.score)" :stroke-width="6" />
                </div>
              </div>

              <div class="detail-section">
                <div class="section-title">最近检测结果</div>
                <el-table :data="qualityRecords" border stripe size="small">
                  <el-table-column prop="ruleName" label="规则名称" min-width="120" />
                  <el-table-column prop="category" label="类别" width="80" />
                  <el-table-column prop="result" label="结果" width="70">
                    <template #default="{ row }">
                      <el-tag size="small" :type="row.result === '通过' ? 'success' : 'danger'" effect="plain">
                        {{ row.result }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="passRate" label="通过率" width="80" />
                  <el-table-column prop="checkTime" label="检测时间" width="130" />
                </el-table>
              </div>
            </el-tab-pane>
          </el-tabs>

          <div class="detail-actions">
            <el-button type="primary" :icon="View">查看详情</el-button>
            <el-button :icon="Share">申请权限</el-button>
            <el-button :icon="Star">收藏</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { Search, Share, Star, View } from '@element-plus/icons-vue'

const viewMode = ref('card')
const activeNav = ref('datasource')
const searchKeyword = ref('')
const treeKeyword = ref('')
const sortBy = ref('hot')
const detailTab = ref('basic')
const fieldKeyword = ref('')
const selectedAsset = ref(null)
const currentPage = ref(1)
const pageSize = 8

const activeTypes = ref(['table', 'field', 'metric', 'api', 'report'])

const navTabs = [
  { key: 'datasource', name: '按数据源', icon: 'Coin' },
  { key: 'domain', name: '按主题域', icon: 'Grid' },
  { key: 'business', name: '按业务板块', icon: 'Briefcase' }
]

const typeFilters = [
  { key: 'table', name: '数据表', icon: 'Grid' },
  { key: 'field', name: '字段', icon: 'Menu' },
  { key: 'metric', name: '指标', icon: 'DataLine' },
  { key: 'api', name: 'API', icon: 'Connection' },
  { key: 'report', name: '报表', icon: 'DataAnalysis' }
]

const dataSourceTree = [
  { id: 'all', name: '全部数据源', count: 256, children: [
    { id: 'ods_customer', name: '客户主题库', count: 68 },
    { id: 'ods_trade', name: '交易主题库', count: 52 },
    { id: 'dwd_trade', name: '交易明细库', count: 45 },
    { id: 'dws_summary', name: '汇总主题库', count: 38 },
    { id: 'ads_report', name: '报表应用库', count: 32 },
    { id: 'dim_public', name: '公共维度库', count: 21 }
  ]}
]

const domainTree = [
  { id: 'all', name: '全部主题域', count: 256, children: [
    { id: 'customer', name: '客户域', count: 62 },
    { id: 'trade', name: '交易域', count: 58 },
    { id: 'product', name: '商品域', count: 42 },
    { id: 'marketing', name: '营销域', count: 36 },
    { id: 'supply', name: '供应链域', count: 30 },
    { id: 'finance', name: '财务域', count: 28 }
  ]}
]

const businessTree = [
  { id: 'all', name: '全部业务板块', count: 256, children: [
    { id: 'retail', name: '零售业务', count: 78 },
    { id: 'wholesale', name: '批发业务', count: 56 },
    { id: 'finance', name: '金融业务', count: 48 },
    { id: 'logistics', name: '物流业务', count: 42 },
    { id: 'service', name: '服务业务', count: 32 }
  ]}
]

const currentTreeData = computed(() => {
  const map = { datasource: dataSourceTree, domain: domainTree, business: businessTree }
  return map[activeNav.value]
})

const assetList = ref([
  { id: 1, name: '客户信息表', code: 'ods_customer.customer_info', type: '数据表', database: '客户主题库', domain: '客户域', owner: '张伟', dept: '数据治理部', fieldCount: 32, dataSize: 1256800, storageSize: '256.8 MB', hotScore: 95, qualityScore: 96.5, createTime: '2025-03-15 10:00', updateTime: '2026-09-26 02:30', desc: '存储客户的基本信息，包括个人信息、联系信息、账户信息等。', tags: ['核心资产', '客户主题', '高频访问', '高质量'] },
  { id: 2, name: '订单明细表', code: 'ods_trade.order_detail', type: '数据表', database: '交易主题库', domain: '交易域', owner: '李娜', dept: '数据开发部', fieldCount: 28, dataSize: 8956200, storageSize: '1.2 GB', hotScore: 98, qualityScore: 94.2, createTime: '2025-03-20 14:30', updateTime: '2026-09-26 02:15', desc: '存储订单的明细信息，包括商品明细、金额明细、物流明细等。', tags: ['核心资产', '交易主题', '高频访问'] },
  { id: 3, name: '商品库存表', code: 'ods_product.goods_stock', type: '数据表', database: '商品主题库', domain: '商品域', owner: '王强', dept: '数据开发部', fieldCount: 18, dataSize: 456800, storageSize: '89.5 MB', hotScore: 78, qualityScore: 91.8, createTime: '2025-04-10 09:00', updateTime: '2026-09-26 03:00', desc: '存储商品库存信息，包括库存数量、库存位置、库存状态等。', tags: ['商品主题', '供应链'] },
  { id: 4, name: '日销售汇总表', code: 'dws_summary.sale_day', type: '数据表', database: '汇总主题库', domain: '交易域', owner: '刘洋', dept: '数据分析部', fieldCount: 45, dataSize: 125600, storageSize: '45.2 MB', hotScore: 92, qualityScore: 97.3, createTime: '2025-05-08 11:20', updateTime: '2026-09-26 04:00', desc: '按日维度汇总销售数据，包括销售额、订单量、客单价等指标。', tags: ['汇总表', '核心资产', '经营分析'] },
  { id: 5, name: 'GMV指标', code: 'metric.gmv_total', type: '指标', database: '指标平台', domain: '交易域', owner: '陈静', dept: '数据分析部', fieldCount: 0, dataSize: 0, storageSize: '-', hotScore: 88, qualityScore: 95.0, createTime: '2025-06-12 15:00', updateTime: '2026-09-26 05:00', desc: '商品交易总额，指一定时间内的成交总金额。', tags: ['核心指标', '交易主题'] },
  { id: 6, name: '客户查询API', code: 'api.customer.query', type: 'API', database: '数据服务平台', domain: '客户域', owner: '赵磊', dept: '数据服务部', fieldCount: 0, dataSize: 0, storageSize: '-', hotScore: 85, qualityScore: 93.6, createTime: '2025-07-15 10:30', updateTime: '2026-09-20 14:00', desc: '根据客户ID查询客户详细信息的API接口。', tags: ['数据服务', '客户主题'] },
  { id: 7, name: '经营驾驶舱报表', code: 'report.business_dashboard', type: '报表', database: '报表平台', domain: '交易域', owner: '孙悦', dept: '数据分析部', fieldCount: 0, dataSize: 0, storageSize: '-', hotScore: 90, qualityScore: 92.1, createTime: '2025-08-20 09:00', updateTime: '2026-09-25 18:30', desc: '企业经营数据总览报表，展示核心经营指标和趋势。', tags: ['核心报表', '管理层'] },
  { id: 8, name: '用户行为明细表', code: 'dwd_log.user_behavior', type: '数据表', database: '日志明细库', domain: '营销域', owner: '周杰', dept: '数据开发部', fieldCount: 36, dataSize: 25680000, storageSize: '3.5 GB', hotScore: 72, qualityScore: 88.5, createTime: '2025-09-10 16:00', updateTime: '2026-09-26 01:00', desc: '存储用户在APP和网站上的行为日志明细数据。', tags: ['行为分析', '日志数据'] },
  { id: 9, name: '会员画像表', code: 'dws_user.member_profile', type: '数据表', database: '用户主题库', domain: '客户域', owner: '吴敏', dept: '数据治理部', fieldCount: 68, dataSize: 856000, storageSize: '320 MB', hotScore: 82, qualityScore: 90.8, createTime: '2025-10-12 11:00', updateTime: '2026-09-26 03:30', desc: '会员画像宽表，包含会员的基本属性、行为属性、价值属性等。', tags: ['用户画像', '客户主题'] },
  { id: 10, name: '渠道转化漏斗', code: 'metric.channel_conversion', type: '指标', database: '指标平台', domain: '营销域', owner: '郑浩', dept: '数据分析部', fieldCount: 0, dataSize: 0, storageSize: '-', hotScore: 76, qualityScore: 94.5, createTime: '2025-11-08 14:00', updateTime: '2026-09-26 06:00', desc: '各营销渠道的转化漏斗指标，包括曝光、点击、访问、下单、支付。', tags: ['营销分析', '漏斗分析'] },
  { id: 11, name: '支付流水表', code: 'ods_finance.payment_record', type: '数据表', database: '财务系统库', domain: '财务域', owner: '张伟', dept: '数据开发部', fieldCount: 24, dataSize: 5680000, storageSize: '680 MB', hotScore: 68, qualityScore: 97.8, createTime: '2025-12-05 10:00', updateTime: '2026-09-26 02:45', desc: '支付流水记录表，包含所有支付交易的详细信息。', tags: ['财务主题', '核心资产'] },
  { id: 12, name: '商品销量排名报表', code: 'report.product_sales_rank', type: '报表', database: '报表平台', domain: '商品域', owner: '李娜', dept: '数据分析部', fieldCount: 0, dataSize: 0, storageSize: '-', hotScore: 70, qualityScore: 91.2, createTime: '2026-01-15 09:30', updateTime: '2026-09-25 20:00', desc: '商品销量排名报表，按分类、时间维度展示商品销售排行。', tags: ['商品分析', '销售报表'] }
])

const fieldList = ref([
  { name: 'customer_id', chineseName: '客户ID', dataType: 'VARCHAR(32)', nullable: false, desc: '客户唯一标识' },
  { name: 'customer_name', chineseName: '客户姓名', dataType: 'VARCHAR(64)', nullable: false, desc: '客户真实姓名' },
  { name: 'gender', chineseName: '性别', dataType: 'VARCHAR(1)', nullable: true, desc: '性别：M男 F女 U未知' },
  { name: 'birthday', chineseName: '出生日期', dataType: 'DATE', nullable: true, desc: '客户出生日期' },
  { name: 'phone', chineseName: '手机号码', dataType: 'VARCHAR(11)', nullable: true, desc: '联系手机号码' },
  { name: 'email', chineseName: '邮箱', dataType: 'VARCHAR(128)', nullable: true, desc: '联系邮箱地址' },
  { name: 'address', chineseName: '地址', dataType: 'VARCHAR(256)', nullable: true, desc: '联系地址' },
  { name: 'level', chineseName: '客户等级', dataType: 'VARCHAR(10)', nullable: false, desc: '客户价值等级' },
  { name: 'register_time', chineseName: '注册时间', dataType: 'DATETIME', nullable: false, desc: '客户注册时间' },
  { name: 'last_login_time', chineseName: '最后登录时间', dataType: 'DATETIME', nullable: true, desc: '最后一次登录时间' },
  { name: 'status', chineseName: '状态', dataType: 'VARCHAR(20)', nullable: false, desc: '客户状态：正常/冻结/注销' },
  { name: 'create_time', chineseName: '创建时间', dataType: 'DATETIME', nullable: false, desc: '记录创建时间' },
  { name: 'update_time', chineseName: '更新时间', dataType: 'DATETIME', nullable: false, desc: '记录更新时间' }
])

const upstreamList = ref([
  { id: 101, name: '客户原始数据表', code: 'ods_raw.customer_raw', type: '数据表' },
  { id: 102, name: '会员注册表', code: 'ods_member.register_info', type: '数据表' }
])

const downstreamList = ref([
  { id: 201, name: '会员画像表', code: 'dws_user.member_profile', type: '数据表' },
  { id: 202, name: '客户价值指标', code: 'metric.customer_value', type: '指标' },
  { id: 203, name: '客户查询API', code: 'api.customer.query', type: 'API' },
  { id: 204, name: '客户分析报表', code: 'report.customer_analysis', type: '报表' }
])

const qualityDimensions = ref([
  { name: '完整性', score: 98 },
  { name: '准确性', score: 95 },
  { name: '一致性', score: 96 },
  { name: '及时性', score: 94 },
  { name: '唯一性', score: 99 },
  { name: '有效性', score: 97 }
])

const qualityRecords = ref([
  { ruleName: '非空校验', category: '完整性', result: '通过', passRate: '100%', checkTime: '2026-09-26 03:00' },
  { ruleName: '主键唯一性', category: '唯一性', result: '通过', passRate: '100%', checkTime: '2026-09-26 03:05' },
  { ruleName: '手机号格式', category: '有效性', result: '通过', passRate: '99.8%', checkTime: '2026-09-26 03:10' },
  { ruleName: '枚举值校验', category: '准确性', result: '告警', passRate: '98.5%', checkTime: '2026-09-26 03:15' }
])

const upstreamCount = computed(() => upstreamList.value.length)
const downstreamCount = computed(() => downstreamList.value.length)

const filteredFields = computed(() => {
  if (!fieldKeyword.value) return fieldList.value
  const kw = fieldKeyword.value.toLowerCase()
  return fieldList.value.filter(f =>
    f.name.toLowerCase().includes(kw) || f.chineseName.includes(kw)
  )
})

const tableCount = computed(() => assetList.value.filter(a => a.type === '数据表').length)
const fieldCount = 328
const metricCount = computed(() => assetList.value.filter(a => a.type === '指标').length)

const filteredAssets = computed(() => {
  let list = [...assetList.value]
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(a =>
      a.name.toLowerCase().includes(kw) ||
      a.code.toLowerCase().includes(kw) ||
      a.desc.toLowerCase().includes(kw)
    )
  }
  const typeKeyMap = { '数据表': 'table', '字段': 'field', '指标': 'metric', 'API': 'api', '报表': 'report' }
  list = list.filter(a => activeTypes.value.includes(typeKeyMap[a.type]))
  if (sortBy.value === 'hot') {
    list.sort((a, b) => b.hotScore - a.hotScore)
  } else if (sortBy.value === 'name') {
    list.sort((a, b) => a.name.localeCompare(b.name))
  } else {
    list.sort((a, b) => b.updateTime.localeCompare(a.updateTime))
  }
  return list
})

const pagedAssets = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredAssets.value.slice(start, start + pageSize)
})

function typeIcon(type) {
  const map = { '数据表': 'Grid', '字段': 'Menu', '指标': 'DataLine', 'API': 'Connection', '报表': 'DataAnalysis' }
  return map[type] || 'Document'
}

function typeColor(type) {
  const map = { '数据表': '#1664ff', '字段': '#00b42a', '指标': '#ff7d00', 'API': '#722ed1', '报表': '#0fc6c2' }
  return map[type] || '#86909c'
}

function typeBg(type) {
  const map = { '数据表': '#e8f0ff', '字段': '#e8ffea', '指标': '#fff3e8', 'API': '#f5e8ff', '报表': '#e0fffa' }
  return map[type] || '#f7f8fa'
}

function typeTagType(type) {
  const map = { '数据表': 'primary', '字段': 'success', '指标': 'warning', 'API': 'danger', '报表': 'info' }
  return map[type] || 'info'
}

function qualityColor(score) {
  if (score >= 95) return '#00b42a'
  if (score >= 85) return '#ff7d00'
  return '#f53f3f'
}

function formatNum(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n.toString()
}

function toggleType(key) {
  const idx = activeTypes.value.indexOf(key)
  if (idx > -1) {
    if (activeTypes.value.length > 1) activeTypes.value.splice(idx, 1)
  } else {
    activeTypes.value.push(key)
  }
}

function handleSearch() {
  currentPage.value = 1
}

function handleNodeClick(data) {
  // 模拟分类筛选
  currentPage.value = 1
}

function selectAsset(item) {
  selectedAsset.value = item
  detailTab.value = 'basic'
}
</script>

<style scoped>
.governance-map {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.map-layout {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}

/* 左侧边栏 */
.map-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: var(--dp-radius);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-tabs {
  display: flex;
  border-bottom: 1px solid var(--dp-border-light);
}

.sidebar-tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  font-size: 12px;
  color: var(--dp-text-3);
  cursor: pointer;
  transition: all 0.2s;
}

.sidebar-tab:hover {
  color: var(--dp-text-2);
  background: var(--dp-bg-page);
}

.sidebar-tab.active {
  color: var(--dp-primary);
  background: var(--dp-primary-light);
  font-weight: 500;
}

.sidebar-tree {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.tree-search {
  margin-bottom: 12px;
}

.tree-node {
  display: flex;
  align-items: center;
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

/* 中间主区域 */
.map-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.search-bar {
  padding: 16px;
  margin-bottom: 0;
}

.search-input-wrap {
  margin-bottom: 12px;
}

.search-filters {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 13px;
  color: var(--dp-text-3);
  flex-shrink: 0;
}

.filter-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  font-size: 12px;
  color: var(--dp-text-2);
  background: var(--dp-bg-page);
  border: 1px solid var(--dp-border-light);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tag:hover {
  border-color: var(--dp-primary);
  color: var(--dp-primary);
}

.filter-tag.active {
  background: var(--dp-primary-light);
  border-color: var(--dp-primary);
  color: var(--dp-primary);
  font-weight: 500;
}

.asset-stats {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: var(--dp-radius);
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--dp-primary);
  font-family: 'DIN Alternate', sans-serif;
}

.stat-label {
  font-size: 12px;
  color: var(--dp-text-3);
}

.stat-right {
  margin-left: auto;
}

/* 资产卡片网格 */
.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}

.asset-card {
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: var(--dp-radius);
  padding: 16px;
  cursor: pointer;
  transition: all 0.25s;
}

.asset-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 4px 12px rgba(22, 100, 255, 0.1);
  transform: translateY(-2px);
}

.asset-card.active {
  border-color: var(--dp-primary);
  box-shadow: 0 0 0 2px var(--dp-primary-light);
}

.card-header {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
  align-items: flex-start;
}

.asset-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.asset-icon-sm {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.asset-icon-lg {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.asset-title {
  flex: 1;
  min-width: 0;
}

.asset-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.asset-code {
  font-size: 11px;
  color: var(--dp-text-3);
}

.asset-type-tag {
  flex-shrink: 0;
}

.card-desc {
  font-size: 12px;
  color: var(--dp-text-3);
  line-height: 1.5;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 12px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid var(--dp-border-light);
}

.footer-stats {
  display: flex;
  gap: 16px;
}

.footer-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.footer-stat.hot {
  flex-direction: row;
  color: var(--dp-warning);
}

.footer-stat-num {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', sans-serif;
}

.footer-stat-label {
  font-size: 11px;
  color: var(--dp-text-3);
}

.footer-quality {
  text-align: center;
}

.quality-score {
  font-size: 22px;
  font-weight: 700;
  font-family: 'DIN Alternate', sans-serif;
  line-height: 1;
}

.quality-label {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

.quality-score-sm {
  font-weight: 700;
  font-family: 'DIN Alternate', sans-serif;
}

/* 列表视图 */
.asset-list-table {
  padding: 0;
  overflow: hidden;
}

.asset-list-table :deep(.el-table) {
  border-radius: 0;
}

.list-asset-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name-text {
  font-weight: 500;
  color: var(--dp-text-1);
}

.code-text {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

.asset-pagination {
  justify-content: center;
  margin-top: 4px;
}

/* 右侧详情面板 */
.map-detail {
  width: 0;
  overflow: hidden;
  transition: width 0.3s ease;
}

.map-detail.show {
  width: 420px;
}

.detail-panel {
  width: 420px;
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: var(--dp-radius);
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-header {
  padding: 16px;
  border-bottom: 1px solid var(--dp-border-light);
  position: relative;
}

.detail-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  cursor: pointer;
  color: var(--dp-text-3);
  transition: all 0.2s;
}

.detail-close:hover {
  background: var(--dp-bg-page);
  color: var(--dp-text-1);
}

.detail-title-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-title-info {
  flex: 1;
  min-width: 0;
}

.detail-name {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.detail-code {
  font-size: 12px;
  color: var(--dp-text-3);
}

.detail-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.detail-desc {
  margin: 0;
  font-size: 13px;
  color: var(--dp-text-2);
  line-height: 1.6;
}

.detail-tabs {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.detail-tabs :deep(.el-tabs__content) {
  flex: 1;
  overflow-y: auto;
  padding: 0 16px 16px;
}

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

.field-search {
  margin-bottom: 12px;
}

/* 血缘列表 */
.lineage-section {
  padding: 4px 0;
}

.lineage-stat {
  display: flex;
  gap: 24px;
  padding: 12px;
  background: var(--dp-bg-page);
  border-radius: 8px;
  margin-bottom: 16px;
}

.lineage-stat-item {
  flex: 1;
  text-align: center;
}

.lineage-stat-item .num {
  font-size: 24px;
  font-weight: 700;
  font-family: 'DIN Alternate', sans-serif;
  display: block;
}

.lineage-stat-item .label {
  font-size: 12px;
  color: var(--dp-text-3);
}

.lineage-list-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--dp-text-2);
  margin: 12px 0 8px;
}

.lineage-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.lineage-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: var(--dp-bg-page);
  border-radius: 6px;
}

.lineage-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.lineage-icon.up {
  background: var(--dp-warning-light);
  color: var(--dp-warning);
}

.lineage-icon.down {
  background: var(--dp-success-light);
  color: var(--dp-success);
}

.lineage-info {
  flex: 1;
  min-width: 0;
}

.lineage-name {
  font-size: 13px;
  color: var(--dp-text-1);
  font-weight: 500;
}

.lineage-code {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

/* 质量信息 */
.quality-overview {
  text-align: center;
  padding: 20px 0;
  background: var(--dp-bg-page);
  border-radius: 8px;
  margin-bottom: 16px;
}

.quality-score-lg {
  font-size: 48px;
  font-weight: 700;
  font-family: 'DIN Alternate', sans-serif;
  line-height: 1;
}

.quality-level {
  font-size: 14px;
  color: var(--dp-text-2);
  margin-top: 4px;
}

.quality-dimensions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quality-dim .dim-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.quality-dim .dim-name {
  font-size: 12px;
  color: var(--dp-text-2);
}

.quality-dim .dim-score {
  font-size: 12px;
  font-weight: 600;
  font-family: 'DIN Alternate', sans-serif;
}

.detail-actions {
  padding: 12px 16px;
  border-top: 1px solid var(--dp-border-light);
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
