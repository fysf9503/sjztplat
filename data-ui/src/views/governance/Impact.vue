<template>
  <div class="governance-impact">
    <PageHeader title="影响分析" desc="分析数据资产变更对下游表、字段、任务和报表的影响范围，辅助变更决策和风险评估">
      <template #extra>
        <el-button :icon="Download" @click="handleExport">导出报告</el-button>
        <el-button type="primary" :icon="Bell" @click="handleNotify">通知负责人</el-button>
      </template>
    </PageHeader>

    <!-- 分析配置区 -->
    <div class="config-card dp-card dp-fade-up">
      <div class="config-row">
        <div class="config-item">
          <label class="config-label">分析对象</label>
          <el-select v-model="analyzeTarget" filterable placeholder="选择表或字段" style="width: 100%">
            <el-option-group label="数据表">
              <el-option v-for="t in tableOptions" :key="t.value" :label="t.label" :value="t.value">
                <span style="display: flex; align-items: center; gap: 8px">
                  <el-icon :size="14" color="#165dff"><Grid /></el-icon>
                  <span>{{ t.label }}</span>
                </span>
              </el-option>
            </el-option-group>
            <el-option-group label="字段">
              <el-option v-for="f in fieldOptions" :key="f.value" :label="f.label" :value="f.value">
                <span style="display: flex; align-items: center; gap: 8px">
                  <el-icon :size="14" color="#ff7d00"><Menu /></el-icon>
                  <span>{{ f.label }}</span>
                </span>
              </el-option>
            </el-option-group>
          </el-select>
        </div>
        <div class="config-item">
          <label class="config-label">分析方向</label>
          <el-radio-group v-model="direction" size="default">
            <el-radio-button value="downstream">
              <el-icon style="margin-right: 4px"><ArrowDown /></el-icon>
              下游影响
            </el-radio-button>
            <el-radio-button value="upstream">
              <el-icon style="margin-right: 4px"><ArrowUp /></el-icon>
              上游依赖
            </el-radio-button>
            <el-radio-button value="both">
              <el-icon style="margin-right: 4px"><Sort /></el-icon>
              双向
            </el-radio-button>
          </el-radio-group>
        </div>
        <div class="config-item">
          <label class="config-label">分析深度</label>
          <el-slider v-model="depth" :min="1" :max="5" :step="1" show-stops style="width: 180px" />
        </div>
        <div class="config-item">
          <label class="config-label">&nbsp;</label>
          <el-button type="primary" :icon="Search" @click="doAnalyze" :loading="analyzing">
            开始分析
          </el-button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row dp-fade-up" style="animation-delay: 0.05s">
      <div class="stat-card">
        <div class="stat-icon primary">
          <el-icon :size="22"><Grid /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.tableCount }}</div>
          <div class="stat-label">影响表数</div>
        </div>
        <div class="stat-trend up">
          <el-icon><TrendCharts /></el-icon>
          <span>+2</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon warning">
          <el-icon :size="22"><Menu /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.fieldCount }}</div>
          <div class="stat-label">影响字段数</div>
        </div>
        <div class="stat-trend up">
          <el-icon><TrendCharts /></el-icon>
          <span>+15</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon success">
          <el-icon :size="22"><Cpu /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.taskCount }}</div>
          <div class="stat-label">影响任务数</div>
        </div>
        <div class="stat-trend down">
          <el-icon><TrendCharts /></el-icon>
          <span>-1</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">
          <el-icon :size="22"><DataAnalysis /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.reportCount }}</div>
          <div class="stat-label">影响报表数</div>
        </div>
        <div class="stat-trend up">
          <el-icon><TrendCharts /></el-icon>
          <span>+3</span>
        </div>
      </div>
      <div class="stat-card risk-card">
        <div class="risk-level">
          <div class="risk-badge" :class="riskLevelClass">{{ riskLevelText }}</div>
          <div class="risk-desc">综合风险等级</div>
        </div>
        <div class="risk-bar">
          <div v-for="r in riskItems" :key="r.level" class="risk-seg" :class="r.level">
            <span>{{ r.count }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-grid">
      <!-- 影响路径图 -->
      <div class="graph-section dp-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">
            <el-icon :size="16" style="margin-right: 6px; color: var(--dp-primary)"><Share /></el-icon>
            影响路径图
          </div>
          <div class="dp-card-extra">
            <el-radio-group v-model="graphType" size="small" @change="handleGraphTypeChange">
              <el-radio-button value="tree">树状图</el-radio-button>
              <el-radio-button value="force">力导向图</el-radio-button>
              <el-radio-button value="sankey">桑基图</el-radio-button>
            </el-radio-group>
            <el-divider direction="vertical" />
            <el-button size="small" :icon="ZoomIn" @click="zoomIn">放大</el-button>
            <el-button size="small" :icon="ZoomOut" @click="zoomOut">缩小</el-button>
            <el-button size="small" :icon="RefreshRight" @click="resetView">重置</el-button>
          </div>
        </div>
        <div class="graph-legend">
          <div v-for="item in legendItems" :key="item.key" class="legend-item">
            <span class="legend-dot" :style="{ background: item.color }"></span>
            <span class="legend-text">{{ item.name }}</span>
            <span class="legend-count">{{ item.count }}</span>
          </div>
        </div>
        <div class="graph-container" ref="graphContainerRef">
          <EChart :option="graphOption" :height="graphHeight + 'px'" @click="handleGraphClick" />
        </div>
      </div>

      <!-- 影响资产列表 -->
      <div class="list-section dp-card dp-fade-up" style="animation-delay: 0.05s">
        <div class="dp-card-header">
          <div class="dp-card-title">
            <el-icon :size="16" style="margin-right: 6px; color: var(--dp-primary)"><List /></el-icon>
            影响资产列表
          </div>
          <div class="dp-card-extra">
            <el-tag size="small" type="info" effect="plain">共 {{ filteredAssets.length }} 条</el-tag>
          </div>
        </div>

        <!-- 筛选标签 -->
        <div class="filter-tabs">
          <div
            v-for="tab in typeTabs"
            :key="tab.key"
            class="filter-tab"
            :class="{ active: activeType === tab.key }"
            @click="activeType = tab.key"
          >
            <el-icon :size="14"><component :is="tab.icon" /></el-icon>
            <span>{{ tab.name }}</span>
            <span class="tab-count">{{ tab.count }}</span>
          </div>
        </div>

        <!-- 搜索和风险筛选 -->
        <div class="list-toolbar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索资产名称"
            size="small"
            clearable
            style="width: 200px"
            :prefix-icon="Search"
          />
          <el-select v-model="riskFilter" size="small" placeholder="风险等级" style="width: 120px" clearable>
            <el-option label="高风险" value="high" />
            <el-option label="中风险" value="medium" />
            <el-option label="低风险" value="low" />
          </el-select>
        </div>

        <!-- 表格 -->
        <el-table
          :data="pagedAssets"
          border
          stripe
          size="small"
          style="width: 100%"
          height="420"
          @row-click="handleRowClick"
        >
          <el-table-column prop="name" label="资产名称" min-width="180">
            <template #default="{ row }">
              <div class="asset-cell">
                <div class="asset-icon-sm" :style="{ background: row.bg, color: row.color }">
                  <el-icon :size="13"><component :is="row.icon" /></el-icon>
                </div>
                <div class="asset-name-wrap">
                  <div class="asset-name-text">{{ row.name }}</div>
                  <div class="asset-path-text dp-mono">{{ row.path }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" effect="plain" :style="{ color: row.color, borderColor: row.color, background: row.bg }">
                {{ typeName(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="风险等级" width="100">
            <template #default="{ row }">
              <span class="risk-indicator">
                <span class="risk-dot" :class="row.risk"></span>
                {{ riskName(row.risk) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="impactDegree" label="影响度" width="110">
            <template #default="{ row }">
              <div class="impact-degree">
                <el-progress
                  :percentage="row.impactDegree"
                  :stroke-width="6"
                  :color="degreeColor(row.impactDegree)"
                  :show-text="false"
                  style="flex: 1; margin-right: 8px"
                />
                <span class="degree-text">{{ row.impactDegree }}%</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="owner" label="负责人" width="90" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click.stop="viewDetail(row)">查看</el-button>
              <el-button link type="primary" size="small" @click.stop="notifyOwner(row)">通知</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 20]"
            :total="filteredAssets.length"
            layout="total, sizes, prev, pager, next"
            small
          />
        </div>
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="资产详情" size="520px" :with-header="false">
      <div v-if="currentAsset" class="detail-drawer">
        <div class="drawer-header">
          <div class="drawer-back" @click="detailVisible = false">
            <el-icon><ArrowLeft /></el-icon>
          </div>
          <div class="drawer-title-wrap">
            <div class="drawer-icon" :style="{ background: currentAsset.bg, color: currentAsset.color }">
              <el-icon :size="20"><component :is="currentAsset.icon" /></el-icon>
            </div>
            <div>
              <h3 class="drawer-title">{{ currentAsset.name }}</h3>
              <div class="drawer-subtitle dp-mono">{{ currentAsset.path }}</div>
            </div>
          </div>
          <el-tag :type="riskTagType(currentAsset.risk)" effect="plain">
            {{ riskName(currentAsset.risk) }}风险
          </el-tag>
        </div>

        <el-tabs v-model="detailTab" class="drawer-tabs">
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="资产类型">{{ typeName(currentAsset.type) }}</el-descriptions-item>
              <el-descriptions-item label="所属层级">{{ currentAsset.layer }}</el-descriptions-item>
              <el-descriptions-item label="负责人">{{ currentAsset.owner }}</el-descriptions-item>
              <el-descriptions-item label="影响度">
                <el-progress :percentage="currentAsset.impactDegree" :stroke-width="8" :color="degreeColor(currentAsset.impactDegree)" />
              </el-descriptions-item>
              <el-descriptions-item label="最近更新">{{ currentAsset.lastUpdate }}</el-descriptions-item>
              <el-descriptions-item label="影响路径长度">{{ currentAsset.pathLength }} 跳</el-descriptions-item>
            </el-descriptions>

            <div class="section-title">影响说明</div>
            <div class="impact-desc">
              <p>{{ currentAsset.description }}</p>
              <el-alert
                title="变更建议"
                type="warning"
                :closable="false"
                show-icon
                style="margin-top: 12px"
              >
                <template #default>
                  建议在变更前通知下游负责人，并在低峰期执行变更操作。变更后请验证数据一致性。
                </template>
              </el-alert>
            </div>
          </el-tab-pane>

          <el-tab-pane label="血缘链路" name="lineage">
            <div class="lineage-path">
              <div class="path-title">影响路径</div>
              <div class="path-flow">
                <div
                  v-for="(node, idx) in currentAsset.pathNodes"
                  :key="idx"
                  class="path-node"
                >
                  <div class="path-node-icon" :style="{ background: node.bg, color: node.color }">
                    <el-icon :size="12"><component :is="node.icon" /></el-icon>
                  </div>
                  <span class="path-node-name">{{ node.name }}</span>
                  <el-icon v-if="idx < currentAsset.pathNodes.length - 1" class="path-arrow" :size="14" color="#c9cdd4">
                    <ArrowRight />
                  </el-icon>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="负责人信息" name="owner">
            <div class="owner-card">
              <el-avatar :size="56" style="background: var(--dp-primary)">{{ currentAsset.owner.charAt(0) }}</el-avatar>
              <div class="owner-info">
                <div class="owner-name">{{ currentAsset.owner }}</div>
                <div class="owner-dept">数据开发部 / 数据治理组</div>
              </div>
            </div>
            <div class="contact-list">
              <div class="contact-item">
                <el-icon><Message /></el-icon>
                <span>飞书：{{ currentAsset.owner }}</span>
              </div>
              <div class="contact-item">
                <el-icon><Phone /></el-icon>
                <span>电话：138****{{ 1000 + Math.floor(Math.random() * 9000) }}</span>
              </div>
              <div class="contact-item">
                <el-icon><Message /></el-icon>
                <span>邮箱：{{ currentAsset.owner.toLowerCase() }}@company.com</span>
              </div>
            </div>
            <el-button type="primary" style="width: 100%; margin-top: 16px" :icon="Bell">
              发送变更通知
            </el-button>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import {
  Search, Download, Bell, Grid, Menu, Cpu, DataAnalysis,
  ArrowDown, ArrowUp, ArrowLeft, ArrowRight, Sort, Share,
  List, TrendCharts, ZoomIn, ZoomOut, RefreshRight,
  Message, Phone, Warning
} from '@element-plus/icons-vue'
import EChart from '@/components/EChart.vue'
import PageHeader from '@/components/PageHeader.vue'
import { rnd, int, pick, owners, dateStr } from '@/mock/helpers'

const analyzing = ref(false)
const analyzeTarget = ref('dws_summary_db.sale_day')
const direction = ref('downstream')
const depth = ref(3)
const graphType = ref('tree')
const activeType = ref('all')
const searchKeyword = ref('')
const riskFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailTab = ref('basic')
const currentAsset = ref(null)
const graphContainerRef = ref(null)
const graphHeight = ref(380)

// 分析对象选项
const tableOptions = [
  { label: 'dws_summary_db.sale_day', value: 'dws_summary_db.sale_day' },
  { label: 'dws_summary_db.order_day', value: 'dws_summary_db.order_day' },
  { label: 'dwd_trade_db.fact_order', value: 'dwd_trade_db.fact_order' },
  { label: 'dwd_user_db.dim_user', value: 'dwd_user_db.dim_user' },
  { label: 'dwd_product_db.dim_product', value: 'dwd_product_db.dim_product' },
]

const fieldOptions = [
  { label: 'dws_summary_db.sale_day.sale_amount', value: 'dws_summary_db.sale_day.sale_amount' },
  { label: 'dws_summary_db.sale_day.order_count', value: 'dws_summary_db.sale_day.order_count' },
  { label: 'dwd_trade_db.fact_order.pay_amount', value: 'dwd_trade_db.fact_order.pay_amount' },
  { label: 'dwd_user_db.dim_user.user_level', value: 'dwd_user_db.dim_user.user_level' },
]

// 影响资产数据
const tableAssets = Array.from({ length: 6 }, (_, i) => {
  const names = ['ads_report_db.rpt_sale_day', 'ads_report_db.rpt_channel', 'ads_report_db.rpt_member',
    'dws_analysis_db.sale_month', 'dws_analysis_db.user_retention', 'dws_bi_db.cockpit_sale']
  return {
    id: 'tbl_' + i,
    type: 'table',
    name: names[i],
    path: names[i],
    icon: 'Grid',
    color: '#165dff',
    bg: '#e8f3ff',
    risk: ['high', 'high', 'medium', 'medium', 'low', 'low'][i],
    owner: pick(owners),
    impactDegree: int(40, 98),
    layer: ['ADS层', 'ADS层', 'ADS层', 'DWS层', 'DWS层', 'DWS层'][i],
    lastUpdate: dateStr(int(0, 3)),
    pathLength: i + 1,
    description: `该表直接依赖 ${analyzeTarget.value}，变更可能导致数据不一致，影响下游报表展示。表数据量约 ${int(10, 500)} 万条，日更新频率。`
  }
})

const fieldAssets = Array.from({ length: 12 }, (_, i) => {
  const fields = ['sale_amount', 'order_cnt', 'pay_amount', 'refund_amount', 'gmv_amount',
    'user_cnt', 'new_user_cnt', 'active_user_cnt', 'product_cnt', 'channel_cnt',
    'avg_price', 'total_qty']
  const tables = ['ads_report_db.rpt_sale_day', 'ads_report_db.rpt_channel', 'dws_analysis_db.sale_month']
  return {
    id: 'fld_' + i,
    type: 'field',
    name: fields[i],
    path: tables[i % 3] + '.' + fields[i],
    icon: 'Menu',
    color: '#ff7d00',
    bg: '#fff3e8',
    risk: i < 4 ? 'high' : i < 8 ? 'medium' : 'low',
    owner: pick(owners),
    impactDegree: int(25, 95),
    layer: '字段级',
    lastUpdate: dateStr(int(0, 5)),
    pathLength: int(1, 4),
    description: `该字段依赖源表 ${analyzeTarget.value} 中的对应字段，变更可能导致计算逻辑错误。`
  }
})

const taskAssets = Array.from({ length: 8 }, (_, i) => {
  const names = ['销售日汇总任务', '渠道分析任务', '会员统计任务', '经营指标计算',
    '商品销量统计', '库存同步任务', '退款对账任务', '客户标签更新']
  return {
    id: 'task_' + i,
    type: 'task',
    name: names[i],
    path: 'task/dw/' + names[i],
    icon: 'Cpu',
    color: '#00b42a',
    bg: '#e8ffea',
    risk: i < 3 ? 'high' : i < 6 ? 'medium' : 'low',
    owner: pick(owners),
    impactDegree: int(30, 100),
    layer: '任务层',
    lastUpdate: dateStr(int(0, 2)),
    pathLength: int(1, 3),
    description: `该ETL任务依赖 ${analyzeTarget.value} 作为输入，变更可能导致任务运行失败或产出数据异常。任务调度周期：每日凌晨 ${int(1, 6)} 点。`
  }
})

const reportAssets = Array.from({ length: 6 }, (_, i) => {
  const names = ['经营驾驶舱', '销售专题看板', '会员分析看板', '商品分析看板', '渠道分析报表', '财务总览报表']
  return {
    id: 'rpt_' + i,
    type: 'report',
    name: names[i],
    path: 'dashboard/' + ['cockpit', 'sales', 'member', 'product', 'channel', 'finance'][i],
    icon: 'DataAnalysis',
    color: '#722ed1',
    bg: '#f5e8ff',
    risk: i < 2 ? 'high' : i < 4 ? 'medium' : 'low',
    owner: pick(owners),
    impactDegree: int(35, 92),
    layer: '应用层',
    lastUpdate: dateStr(int(0, 1)),
    pathLength: int(2, 4),
    description: `该报表的数据来源于 ${analyzeTarget.value}，变更可能导致报表指标计算错误，影响业务决策。报表日访问量约 ${int(100, 5000)} 次。`
  }
})

const allAssets = computed(() => [...tableAssets, ...fieldAssets, ...taskAssets, ...reportAssets])

// 类型标签
const typeTabs = computed(() => [
  { key: 'all', name: '全部', icon: 'List', count: allAssets.value.length },
  { key: 'table', name: '数据表', icon: 'Grid', count: tableAssets.length },
  { key: 'field', name: '字段', icon: 'Menu', count: fieldAssets.length },
  { key: 'task', name: '任务', icon: 'Cpu', count: taskAssets.length },
  { key: 'report', name: '报表', icon: 'DataAnalysis', count: reportAssets.length },
])

// 筛选后的数据
const filteredAssets = computed(() => {
  let list = allAssets.value
  if (activeType.value !== 'all') {
    list = list.filter(i => i.type === activeType.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(i => i.name.toLowerCase().includes(kw) || i.path.toLowerCase().includes(kw))
  }
  if (riskFilter.value) {
    list = list.filter(i => i.risk === riskFilter.value)
  }
  return list
})

const pagedAssets = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredAssets.value.slice(start, start + pageSize.value)
})

// 统计数据
const stats = computed(() => ({
  tableCount: tableAssets.length,
  fieldCount: fieldAssets.length,
  taskCount: taskAssets.length,
  reportCount: reportAssets.length,
}))

const riskItems = computed(() => [
  { level: 'high', count: allAssets.value.filter(i => i.risk === 'high').length },
  { level: 'medium', count: allAssets.value.filter(i => i.risk === 'medium').length },
  { level: 'low', count: allAssets.value.filter(i => i.risk === 'low').length },
])

const riskLevelClass = computed(() => {
  const high = riskItems.value[0].count
  if (high >= 8) return 'high'
  if (high >= 4) return 'medium'
  return 'low'
})

const riskLevelText = computed(() => {
  return riskLevelClass.value === 'high' ? '高风险' : riskLevelClass.value === 'medium' ? '中风险' : '低风险'
})

const legendItems = computed(() => [
  { key: 'table', name: '数据表', color: '#165dff', count: tableAssets.length },
  { key: 'field', name: '字段', color: '#ff7d00', count: fieldAssets.length },
  { key: 'task', name: '任务', color: '#00b42a', count: taskAssets.length },
  { key: 'report', name: '报表', color: '#722ed1', count: reportAssets.length },
])

// 工具函数
const typeName = t => ({ table: '数据表', field: '字段', task: '任务', report: '报表' }[t] || t)
const riskName = r => ({ high: '高', medium: '中', low: '低' }[r] || r)
const riskTagType = r => ({ high: 'danger', medium: 'warning', low: 'success' }[r] || 'info')

const degreeColor = val => {
  if (val >= 70) return '#f53f3f'
  if (val >= 40) return '#ff7d00'
  return '#00b42a'
}

// 图配置 - 树状图
const treeOption = computed(() => {
  const sourceNode = {
    name: analyzeTarget.value,
    children: [
      {
        name: '数据表 (6)',
        itemStyle: { color: '#165dff' },
        children: tableAssets.slice(0, 4).map(t => ({
          name: t.name,
          itemStyle: { color: t.risk === 'high' ? '#f53f3f' : t.risk === 'medium' ? '#ff7d00' : '#00b42a' },
          value: t.impactDegree
        }))
      },
      {
        name: '字段 (12)',
        itemStyle: { color: '#ff7d00' },
        children: fieldAssets.slice(0, 4).map(f => ({
          name: f.name,
          itemStyle: { color: f.risk === 'high' ? '#f53f3f' : f.risk === 'medium' ? '#ff7d00' : '#00b42a' },
          value: f.impactDegree
        }))
      },
      {
        name: '任务 (8)',
        itemStyle: { color: '#00b42a' },
        children: taskAssets.slice(0, 4).map(t => ({
          name: t.name,
          itemStyle: { color: t.risk === 'high' ? '#f53f3f' : t.risk === 'medium' ? '#ff7d00' : '#00b42a' },
          value: t.impactDegree
        }))
      },
      {
        name: '报表 (6)',
        itemStyle: { color: '#722ed1' },
        children: reportAssets.slice(0, 4).map(r => ({
          name: r.name,
          itemStyle: { color: r.risk === 'high' ? '#f53f3f' : r.risk === 'medium' ? '#ff7d00' : '#00b42a' },
          value: r.impactDegree
        }))
      }
    ]
  }

  return {
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove',
      formatter: params => {
        if (params.value) {
          return `<strong>${params.name}</strong><br/>影响度：${params.value}%`
        }
        return params.name
      }
    },
    series: [{
      type: 'tree',
      data: [sourceNode],
      top: '5%',
      left: '10%',
      bottom: '5%',
      right: '15%',
      symbolSize: 12,
      orient: direction.value === 'upstream' ? 'RL' : 'LR',
      label: {
        position: 'right',
        verticalAlign: 'middle',
        align: 'left',
        fontSize: 12,
        color: '#1d2129'
      },
      leaves: {
        label: {
          position: 'right',
          verticalAlign: 'middle',
          align: 'left'
        }
      },
      emphasis: {
        focus: 'descendant'
      },
      expandAndCollapse: true,
      initialTreeDepth: 2,
      animationDuration: 550,
      animationDurationUpdate: 750,
      lineStyle: {
        color: '#c9cdd4',
        curveness: 0.5
      },
      itemStyle: {
        color: '#165dff',
        borderColor: '#fff',
        borderWidth: 2,
        shadowBlur: 6,
        shadowColor: 'rgba(22, 93, 255, 0.2)'
      }
    }]
  }
})

// 图配置 - 力导向图
const forceOption = computed(() => {
  const nodes = [
    { id: 'source', name: analyzeTarget.value, category: 0, symbolSize: 55 },
    ...tableAssets.slice(0, 5).map((t, i) => ({ id: t.id, name: t.name, category: 1, symbolSize: 38, risk: t.risk })),
    ...fieldAssets.slice(0, 6).map((f, i) => ({ id: f.id, name: f.name, category: 2, symbolSize: 28, risk: f.risk })),
    ...taskAssets.slice(0, 5).map((t, i) => ({ id: t.id, name: t.name, category: 3, symbolSize: 35, risk: t.risk })),
    ...reportAssets.slice(0, 4).map((r, i) => ({ id: r.id, name: r.name, category: 4, symbolSize: 36, risk: r.risk })),
  ]

  const links = [
    ...tableAssets.slice(0, 5).map(t => ({ source: 'source', target: t.id })),
    ...tableAssets.slice(0, 3).map((t, i) => ({ source: t.id, target: fieldAssets[i].id })),
    ...tableAssets.slice(0, 4).map((t, i) => ({ source: t.id, target: taskAssets[i].id })),
    ...taskAssets.slice(0, 4).map((t, i) => ({ source: t.id, target: reportAssets[i].id })),
    ...fieldAssets.slice(0, 3).map((f, i) => ({ source: f.id, target: reportAssets[i].id })),
  ]

  return {
    tooltip: {
      formatter: params => {
        if (params.dataType === 'node') {
          return `<strong>${params.name}</strong><br/>类型：${['变更源', '数据表', '字段', '任务', '报表'][params.data.category]}`
        }
        return ''
      }
    },
    legend: {
      data: ['变更源', '数据表', '字段', '任务', '报表'],
      top: 10,
      right: 20,
      orient: 'vertical',
      textStyle: { fontSize: 11 }
    },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      data: nodes.map((n, i) => ({
        ...n,
        itemStyle: {
          borderColor: '#fff',
          borderWidth: 2,
          shadowBlur: 10,
          shadowColor: 'rgba(0,0,0,0.12)'
        },
        label: {
          show: true,
          position: 'bottom',
          distance: 5,
          fontSize: 10,
          color: '#4e5969',
          formatter: p => p.name.length > 12 ? p.name.slice(0, 12) + '...' : p.name
        }
      })),
      links: links.map(l => ({
        ...l,
        lineStyle: {
          curveness: 0.15,
          opacity: 0.6,
          color: '#c9cdd4'
        }
      })),
      categories: [
        { name: '变更源', itemStyle: { color: '#f53f3f' } },
        { name: '数据表', itemStyle: { color: '#165dff' } },
        { name: '字段', itemStyle: { color: '#ff7d00' } },
        { name: '任务', itemStyle: { color: '#00b42a' } },
        { name: '报表', itemStyle: { color: '#722ed1' } }
      ],
      force: {
        repulsion: 280,
        gravity: 0.1,
        edgeLength: [80, 150],
        friction: 0.6
      },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [0, 6],
      emphasis: {
        focus: 'adjacency',
        lineStyle: { width: 3 }
      }
    }]
  }
})

// 图配置 - 桑基图
const sankeyOption = computed(() => {
  const data = [
    { name: analyzeTarget.value, itemStyle: { color: '#f53f3f' } },
    { name: '数据表', itemStyle: { color: '#165dff' } },
    { name: '字段', itemStyle: { color: '#ff7d00' } },
    { name: '任务', itemStyle: { color: '#00b42a' } },
    { name: '报表', itemStyle: { color: '#722ed1' } },
  ]

  const links = [
    { source: analyzeTarget.value, target: '数据表', value: 6, lineStyle: { color: '#165dff', opacity: 0.25 } },
    { source: analyzeTarget.value, target: '字段', value: 12, lineStyle: { color: '#ff7d00', opacity: 0.25 } },
    { source: '数据表', target: '任务', value: 8, lineStyle: { color: '#00b42a', opacity: 0.25 } },
    { source: '数据表', target: '报表', value: 4, lineStyle: { color: '#722ed1', opacity: 0.2 } },
    { source: '字段', target: '报表', value: 6, lineStyle: { color: '#722ed1', opacity: 0.2 } },
    { source: '任务', target: '报表', value: 5, lineStyle: { color: '#722ed1', opacity: 0.2 } },
  ]

  return {
    tooltip: {
      trigger: 'item',
      triggerOn: 'mousemove'
    },
    series: [{
      type: 'sankey',
      left: '5%',
      right: '15%',
      top: '10%',
      bottom: '10%',
      data,
      links,
      orient: 'horizontal',
      nodeWidth: 18,
      nodeGap: 20,
      nodeAlign: 'justify',
      emphasis: {
        focus: 'adjacency'
      },
      label: {
        fontSize: 12,
        color: '#1d2129',
        fontWeight: 500
      },
      lineStyle: {
        curveness: 0.5
      }
    }]
  }
})

const graphOption = computed(() => {
  if (graphType.value === 'tree') return treeOption.value
  if (graphType.value === 'force') return forceOption.value
  return sankeyOption.value
})

// 交互方法
function doAnalyze() {
  analyzing.value = true
  setTimeout(() => {
    analyzing.value = false
  }, 800)
}

function handleGraphTypeChange() {
  // 触发重绘
}

function zoomIn() {
  // 图表缩放
}

function zoomOut() {
  // 图表缩放
}

function resetView() {
  // 重置视图
}

function handleGraphClick(params) {
  // 点击图节点
}

function handleRowClick(row) {
  viewDetail(row)
}

function viewDetail(row) {
  // 生成路径节点
  const pathNodes = [
    { name: analyzeTarget.value, icon: 'Grid', color: '#f53f3f', bg: '#ffece8' },
    { name: row.type === 'table' ? '数据表' : row.type === 'field' ? '字段' : row.type === 'task' ? '任务' : '报表',
      icon: row.icon, color: row.color, bg: row.bg },
    { name: row.name, icon: row.icon, color: row.color, bg: row.bg },
  ]
  currentAsset.value = { ...row, pathNodes }
  detailVisible.value = true
}

function notifyOwner(row) {
  ElMessage.success(`已向 ${row.owner} 发送变更通知`)
}

function handleExport() {
  ElMessage.success('分析报告导出中，请稍后在消息中心查看')
}

function handleNotify() {
  ElMessage.success('已向所有受影响资产的负责人发送通知')
}

import { ElMessage } from 'element-plus'

onMounted(() => {
  nextTick(() => {
    // 初始化图表高度
  })
})
</script>

<style scoped>
.governance-impact {
  padding: 16px 20px 24px;
}

/* 配置卡片 */
.config-card {
  margin-bottom: 16px;
  padding: 18px 20px;
  background: linear-gradient(135deg, #f7f9ff 0%, #fff 100%);
}

.config-row {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  flex-wrap: wrap;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.config-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-2);
}

/* 统计卡片行 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr) 1.2fr;
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  border: 1px solid var(--dp-border-light);
  transition: all 0.25s;
}

.stat-card:hover {
  border-color: var(--dp-primary-light);
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.08);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.primary {
  background: linear-gradient(135deg, #e8f3ff, #d4e4ff);
  color: var(--dp-primary);
}

.stat-icon.success {
  background: linear-gradient(135deg, #e8ffea, #d0f7d4);
  color: var(--dp-success);
}

.stat-icon.warning {
  background: linear-gradient(135deg, #fff3e8, #ffe2cc);
  color: var(--dp-warning);
}

.stat-icon.purple {
  background: linear-gradient(135deg, #f5e8ff, #e8d4ff);
  color: #722ed1;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--dp-text-1);
  line-height: 1.2;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}

.stat-label {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  font-weight: 500;
}

.stat-trend.up {
  color: var(--dp-danger);
}

.stat-trend.down {
  color: var(--dp-success);
}

/* 风险卡片 */
.risk-card {
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
}

.risk-level {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.risk-badge {
  font-size: 18px;
  font-weight: 700;
  padding: 4px 14px;
  border-radius: 20px;
}

.risk-badge.high {
  background: #ffece8;
  color: #f53f3f;
}

.risk-badge.medium {
  background: #fff7e8;
  color: #ff7d00;
}

.risk-badge.low {
  background: #e8ffea;
  color: #00b42a;
}

.risk-desc {
  font-size: 12px;
  color: var(--dp-text-3);
}

.risk-bar {
  display: flex;
  height: 10px;
  border-radius: 5px;
  overflow: hidden;
}

.risk-seg {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
}

.risk-seg.high { background: #f53f3f; }
.risk-seg.medium { background: #ff7d00; }
.risk-seg.low { background: #00b42a; }

/* 主内容区 */
.main-grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 16px;
}

.graph-section {
  display: flex;
  flex-direction: column;
}

.graph-legend {
  display: flex;
  gap: 20px;
  padding: 0 16px 12px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--dp-text-2);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-count {
  font-weight: 600;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', sans-serif;
}

.graph-container {
  padding: 0 8px 8px;
  flex: 1;
}

/* 列表区域 */
.list-section {
  display: flex;
  flex-direction: column;
}

.filter-tabs {
  display: flex;
  gap: 4px;
  padding: 0 16px 12px;
  border-bottom: 1px solid var(--dp-border-light);
  margin-bottom: 12px;
}

.filter-tab {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  font-size: 13px;
  color: var(--dp-text-2);
  border-radius: 8px 8px 0 0;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}

.filter-tab:hover {
  color: var(--dp-primary);
}

.filter-tab.active {
  color: var(--dp-primary);
  background: rgba(22, 93, 255, 0.05);
  border-bottom-color: var(--dp-primary);
  font-weight: 500;
}

.tab-count {
  background: var(--dp-bg-page);
  color: var(--dp-text-3);
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  font-family: 'DIN Alternate', sans-serif;
}

.filter-tab.active .tab-count {
  background: var(--dp-primary);
  color: #fff;
}

.list-toolbar {
  display: flex;
  gap: 10px;
  padding: 0 16px 12px;
}

/* 资产单元格 */
.asset-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.asset-icon-sm {
  width: 30px;
  height: 30px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.asset-name-wrap {
  min-width: 0;
  flex: 1;
}

.asset-name-text {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-path-text {
  font-size: 11px;
  color: var(--dp-text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

/* 风险指示器 */
.risk-indicator {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--dp-text-2);
}

.risk-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.risk-dot.high {
  background: #f53f3f;
  box-shadow: 0 0 4px rgba(245, 63, 63, 0.5);
}

.risk-dot.medium {
  background: #ff7d00;
  box-shadow: 0 0 4px rgba(255, 125, 0, 0.5);
}

.risk-dot.low {
  background: #00b42a;
  box-shadow: 0 0 4px rgba(0, 180, 42, 0.5);
}

/* 影响度 */
.impact-degree {
  display: flex;
  align-items: center;
  gap: 6px;
}

.degree-text {
  font-size: 12px;
  font-weight: 600;
  color: var(--dp-text-2);
  min-width: 32px;
  font-family: 'DIN Alternate', sans-serif;
}

.pagination-wrap {
  padding: 12px 16px 0;
  display: flex;
  justify-content: flex-end;
}

/* 详情抽屉 */
.detail-drawer {
  padding: 20px;
}

.drawer-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--dp-border-light);
  margin-bottom: 16px;
}

.drawer-back {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--dp-bg-page);
  cursor: pointer;
  color: var(--dp-text-2);
  transition: all 0.2s;
}

.drawer-back:hover {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
}

.drawer-title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.drawer-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.drawer-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin: 0;
}

.drawer-subtitle {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 3px;
}

.drawer-tabs {
  margin-top: 8px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin: 20px 0 10px;
}

.impact-desc p {
  font-size: 13px;
  color: var(--dp-text-2);
  line-height: 1.7;
  margin: 0;
}

/* 血缘路径 */
.lineage-path {
  padding: 8px 0;
}

.path-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-2);
  margin-bottom: 14px;
}

.path-flow {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.path-node {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--dp-bg-page);
  border-radius: 8px;
  position: relative;
}

.path-node-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.path-node-name {
  flex: 1;
  font-size: 13px;
  color: var(--dp-text-1);
  font-weight: 500;
}

.path-arrow {
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%) rotate(90deg);
  z-index: 1;
}

/* 负责人信息 */
.owner-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: var(--dp-bg-page);
  border-radius: 10px;
  margin-bottom: 16px;
}

.owner-info {
  flex: 1;
}

.owner-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.owner-dept {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 3px;
}

.contact-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  font-size: 13px;
  color: var(--dp-text-2);
}

.contact-item .el-icon {
  color: var(--dp-primary);
  font-size: 16px;
}

/* 动画 */
.dp-fade-up {
  animation: dpFadeUp 0.4s ease-out both;
}

@keyframes dpFadeUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 1400px) {
  .main-grid {
    grid-template-columns: 1fr;
  }
  .stats-row {
    grid-template-columns: repeat(3, 1fr);
  }
  .risk-card {
    grid-column: span 3;
  }
}

@media (max-width: 1100px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .risk-card {
    grid-column: span 2;
  }
}
</style>
