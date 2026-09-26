<template>
  <div class="governance-lineage">
    <PageHeader title="数据血缘" desc="可视化展示数据从来源到加工、再到输出服务的完整链路，支持影响分析和回溯">
      <el-button :icon="Download">导出血缘</el-button>
      <el-button type="primary" :icon="MagicStick">智能解析</el-button>
    </PageHeader>

    <div class="lineage-layout">
      <!-- 左侧资产列表 -->
      <div class="lineage-sidebar dp-fade-up">
        <div class="sidebar-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索表名/字段名"
            size="small"
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="sidebar-list-title">
          <span>资产列表</span>
          <span class="count-badge">{{ filteredAssets.length }}</span>
        </div>
        <div class="sidebar-list">
          <div
            v-for="item in filteredAssets"
            :key="item.id"
            class="asset-item"
            :class="{ active: selectedNode?.id === item.id }"
            @click="selectNode(item)"
          >
            <div class="asset-icon-sm" :style="{ background: layerBg(item.layer), color: layerColor(item.layer) }">
              <el-icon :size="14"><component :is="typeIcon(item.type)" /></el-icon>
            </div>
            <div class="asset-info">
              <div class="asset-name">{{ item.name }}</div>
              <div class="asset-layer">
                <el-tag size="small" :type="layerTagType(item.layer)" effect="plain">{{ item.layer }}</el-tag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间血缘图区域 -->
      <div class="lineage-main">
        <div class="graph-toolbar dp-card dp-fade-up">
          <div class="toolbar-left">
            <el-select v-model="lineageType" size="small" style="width: 130px" @change="handleTypeChange">
              <el-option label="表级血缘" value="table" />
              <el-option label="字段级血缘" value="field" />
            </el-select>
            <el-select v-model="direction" size="small" style="width: 120px">
              <el-option label="全链路" value="both" />
              <el-option label="仅上游" value="up" />
              <el-option label="仅下游" value="down" />
            </el-select>
            <el-select v-model="depth" size="small" style="width: 110px">
              <el-option label="1层" :value="1" />
              <el-option label="2层" :value="2" />
              <el-option label="3层" :value="3" />
              <el-option label="全部" :value="99" />
            </el-select>
          </div>
          <div class="toolbar-right">
            <el-button-group size="small">
              <el-button :icon="ZoomIn" @click="zoomIn">放大</el-button>
              <el-button :icon="ZoomOut" @click="zoomOut">缩小</el-button>
              <el-button :icon="RefreshRight" @click="resetView">重置</el-button>
            </el-button-group>
            <el-divider direction="vertical" />
            <el-radio-group v-model="layoutMode" size="small">
              <el-radio-button value="horizontal">横向</el-radio-button>
              <el-radio-button value="vertical">纵向</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <div class="graph-container dp-card dp-fade-up" style="animation-delay: 0.05s">
          <div class="layer-legend">
            <div v-for="layer in layers" :key="layer.key" class="layer-item">
              <span class="layer-dot" :style="{ background: layer.color }"></span>
              <span class="layer-name">{{ layer.name }}</span>
            </div>
          </div>
          <EChart :option="graphOption" :height="graphHeight + 'px'" />
        </div>

        <!-- 血缘统计 -->
        <div class="stats-row dp-fade-up" style="animation-delay: 0.1s">
          <div class="stat-card">
            <div class="stat-icon primary">
              <el-icon :size="20"><Grid /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ nodes.length }}</div>
              <div class="stat-label">节点总数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon success">
              <el-icon :size="20"><Connection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ edges.length }}</div>
              <div class="stat-label">血缘关系</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon warning">
              <el-icon :size="20"><Files /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ layers.length }}</div>
              <div class="stat-label">数据层级</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon purple">
              <el-icon :size="20"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">98.5%</div>
              <div class="stat-label">血缘覆盖率</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧节点详情 -->
      <div class="lineage-detail dp-card dp-fade-up">
        <div v-if="selectedNode" class="detail-content">
          <div class="detail-header">
            <div class="detail-icon" :style="{ background: layerBg(selectedNode.layer), color: layerColor(selectedNode.layer) }">
              <el-icon :size="22"><component :is="typeIcon(selectedNode.type)" /></el-icon>
            </div>
            <div class="detail-title-info">
              <h3 class="detail-name">{{ selectedNode.name }}</h3>
              <div class="detail-code dp-mono">{{ selectedNode.code }}</div>
            </div>
          </div>

          <el-tabs v-model="activeTab" class="detail-tabs">
            <!-- 基本信息 -->
            <el-tab-pane label="基本信息" name="basic">
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="节点类型">{{ selectedNode.type }}</el-descriptions-item>
                <el-descriptions-item label="所属层级">
                  <el-tag size="small" :type="layerTagType(selectedNode.layer)" effect="plain">
                    {{ selectedNode.layer }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="所属库">{{ selectedNode.database }}</el-descriptions-item>
                <el-descriptions-item label="负责人">{{ selectedNode.owner }}</el-descriptions-item>
                <el-descriptions-item label="数据量">{{ formatNum(selectedNode.dataSize) }} 条</el-descriptions-item>
                <el-descriptions-item label="存储大小">{{ selectedNode.storageSize }}</el-descriptions-item>
                <el-descriptions-item label="更新频率">{{ selectedNode.updateFreq }}</el-descriptions-item>
                <el-descriptions-item label="最近更新">{{ selectedNode.lastUpdate }}</el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>

            <!-- 上游依赖 -->
            <el-tab-pane :label="`上游依赖 (${upstreamNodes.length})`" name="upstream">
              <div class="node-list">
                <div v-for="node in upstreamNodes" :key="node.id" class="node-item" @click="selectNode(node)">
                  <div class="node-icon up">
                    <el-icon :size="14"><ArrowUp /></el-icon>
                  </div>
                  <div class="node-info">
                    <div class="node-name">{{ node.name }}</div>
                    <div class="node-layer">{{ node.layer }}</div>
                  </div>
                  <el-icon :size="16" color="var(--dp-text-3)"><ArrowRight /></el-icon>
                </div>
                <el-empty v-if="!upstreamNodes.length" description="无上游依赖" :image-size="60" />
              </div>
            </el-tab-pane>

            <!-- 下游影响 -->
            <el-tab-pane :label="`下游影响 (${downstreamNodes.length})`" name="downstream">
              <div class="node-list">
                <div v-for="node in downstreamNodes" :key="node.id" class="node-item" @click="selectNode(node)">
                  <div class="node-icon down">
                    <el-icon :size="14"><ArrowDown /></el-icon>
                  </div>
                  <div class="node-info">
                    <div class="node-name">{{ node.name }}</div>
                    <div class="node-layer">{{ node.layer }}</div>
                  </div>
                  <el-icon :size="16" color="var(--dp-text-3)"><ArrowRight /></el-icon>
                </div>
                <el-empty v-if="!downstreamNodes.length" description="无下游影响" :image-size="60" />
              </div>
            </el-tab-pane>
          </el-tabs>

          <div class="detail-actions">
            <el-button type="primary" :icon="Warning" @click="goToImpact">影响分析</el-button>
            <el-button :icon="View">查看详情</el-button>
          </div>
        </div>
        <el-empty v-else description="点击图谱节点查看详情" :image-size="80" class="empty-detail" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EChart from '@/components/EChart.vue'
import { Download, MagicStick, RefreshRight, Search, View, Warning, ZoomIn, ZoomOut } from '@element-plus/icons-vue'

const searchKeyword = ref('')
const lineageType = ref('table')
const direction = ref('both')
const depth = ref(3)
const layoutMode = ref('horizontal')
const activeTab = ref('basic')
const selectedNode = ref(null)
const graphHeight = ref(500)

const layers = [
  { key: 'ODS', name: 'ODS 贴源层', color: '#1664ff' },
  { key: 'DWD', name: 'DWD 明细层', color: '#00b42a' },
  { key: 'DWS', name: 'DWS 汇总层', color: '#ff7d00' },
  { key: 'ADS', name: 'ADS 应用层', color: '#722ed1' },
  { key: 'API', name: 'API 服务层', color: '#f53f3f' }
]

const nodes = ref([
  { id: 'n1', name: 'order_info', code: 'ods_order.order_info', type: '数据表', layer: 'ODS', database: '订单数据库', owner: '张伟', dataSize: 8560000, storageSize: '1.2 GB', updateFreq: '每日 02:00', lastUpdate: '2026-09-26 02:15' },
  { id: 'n2', name: 'order_detail', code: 'ods_order.order_detail', type: '数据表', layer: 'ODS', database: '订单数据库', owner: '李娜', dataSize: 12580000, storageSize: '2.1 GB', updateFreq: '每日 02:00', lastUpdate: '2026-09-26 02:20' },
  { id: 'n3', name: 'user_info', code: 'ods_member.user_info', type: '数据表', layer: 'ODS', database: '会员数据库', owner: '王强', dataSize: 1256000, storageSize: '256 MB', updateFreq: '每日 02:30', lastUpdate: '2026-09-26 02:35' },
  { id: 'n4', name: 'product_info', code: 'ods_product.product_info', type: '数据表', layer: 'ODS', database: '商品数据库', owner: '刘洋', dataSize: 456800, storageSize: '89 MB', updateFreq: '每日 03:00', lastUpdate: '2026-09-26 03:05' },
  { id: 'n5', name: 'trade_flow', code: 'dwd_trade.trade_flow', type: '数据表', layer: 'DWD', database: '交易明细库', owner: '李娜', dataSize: 9850000, storageSize: '1.5 GB', updateFreq: '每日 03:30', lastUpdate: '2026-09-26 03:45' },
  { id: 'n6', name: 'refund_flow', code: 'dwd_trade.refund_flow', type: '数据表', layer: 'DWD', database: '交易明细库', owner: '陈静', dataSize: 568000, storageSize: '128 MB', updateFreq: '每日 04:00', lastUpdate: '2026-09-26 04:10' },
  { id: 'n7', name: 'sale_day', code: 'dws_summary.sale_day', type: '数据表', layer: 'DWS', database: '汇总主题库', owner: '刘洋', dataSize: 125600, storageSize: '45 MB', updateFreq: '每日 05:00', lastUpdate: '2026-09-26 05:15' },
  { id: 'n8', name: 'member_stat', code: 'dws_summary.member_stat', type: '数据表', layer: 'DWS', database: '汇总主题库', owner: '吴敏', dataSize: 89600, storageSize: '32 MB', updateFreq: '每日 05:30', lastUpdate: '2026-09-26 05:45' },
  { id: 'n9', name: 'gmv_report', code: 'ads_report.gmv_report', type: '数据表', layer: 'ADS', database: '报表应用库', owner: '孙悦', dataSize: 25600, storageSize: '12 MB', updateFreq: '每日 06:00', lastUpdate: '2026-09-26 06:10' },
  { id: 'n10', name: 'member_profile', code: 'ads_report.member_profile', type: '数据表', layer: 'ADS', database: '报表应用库', owner: '周杰', dataSize: 45800, storageSize: '18 MB', updateFreq: '每日 06:30', lastUpdate: '2026-09-26 06:40' },
  { id: 'n11', name: 'stat_gmv', code: 'api.stat.gmv', type: 'API', layer: 'API', database: '数据服务平台', owner: '赵磊', dataSize: 0, storageSize: '-', updateFreq: '实时', lastUpdate: '2026-09-26 10:30' },
  { id: 'n12', name: 'user_query', code: 'api.user.query', type: 'API', layer: 'API', database: '数据服务平台', owner: '郑浩', dataSize: 0, storageSize: '-', updateFreq: '实时', lastUpdate: '2026-09-26 10:25' }
])

const edges = ref([
  { from: 'n1', to: 'n5' },
  { from: 'n2', to: 'n5' },
  { from: 'n3', to: 'n5' },
  { from: 'n4', to: 'n5' },
  { from: 'n2', to: 'n6' },
  { from: 'n5', to: 'n7' },
  { from: 'n6', to: 'n7' },
  { from: 'n5', to: 'n8' },
  { from: 'n3', to: 'n8' },
  { from: 'n7', to: 'n9' },
  { from: 'n8', to: 'n10' },
  { from: 'n9', to: 'n11' },
  { from: 'n10', to: 'n12' }
])

const filteredAssets = computed(() => {
  if (!searchKeyword.value) return nodes.value
  const kw = searchKeyword.value.toLowerCase()
  return nodes.value.filter(n =>
    n.name.toLowerCase().includes(kw) || n.code.toLowerCase().includes(kw)
  )
})

const upstreamNodes = computed(() => {
  if (!selectedNode.value) return []
  const upIds = edges.value.filter(e => e.to === selectedNode.value.id).map(e => e.from)
  return nodes.value.filter(n => upIds.includes(n.id))
})

const downstreamNodes = computed(() => {
  if (!selectedNode.value) return []
  const downIds = edges.value.filter(e => e.from === selectedNode.value.id).map(e => e.to)
  return nodes.value.filter(n => downIds.includes(n.id))
})

const graphOption = computed(() => {
  const layerX = { ODS: 80, DWD: 280, DWS: 480, ADS: 680, API: 880 }
  const layerYMap = {}
  layers.forEach(l => { layerYMap[l.key] = 50 })

  const graphNodes = nodes.value.map(n => {
    const y = layerYMap[n.layer]
    layerYMap[n.layer] += 70
    return {
      id: n.id,
      name: n.name,
      x: layoutMode.value === 'horizontal' ? layerX[n.layer] : y + 100,
      y: layoutMode.value === 'horizontal' ? y : layerX[n.layer],
      symbolSize: [110, 44],
      itemStyle: {
        color: layerColor(n.layer),
        borderColor: '#fff',
        borderWidth: 2,
        borderRadius: [8, 8, 8, 8],
        shadowBlur: 10,
        shadowColor: 'rgba(0,0,0,0.12)'
      },
      label: {
        show: true,
        position: 'inside',
        fontSize: 12,
        fontWeight: 500,
        color: '#fff',
        formatter: p => p.name.length > 12 ? p.name.slice(0, 12) + '...' : p.name
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 20,
          shadowColor: layerColor(n.layer)
        },
        scale: 1.1
      }
    }
  })

  const graphLinks = edges.value.map(e => ({
    source: e.from,
    target: e.to,
    lineStyle: {
      color: '#c9cdd4',
      width: 1.5,
      curveness: 0.2
    },
    emphasis: {
      lineStyle: {
        color: '#1664ff',
        width: 2.5
      }
    }
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: params => {
        if (params.dataType === 'node') {
          const node = nodes.value.find(n => n.id === params.data.id)
          return `<div style="font-weight:600;margin-bottom:4px">${node.name}</div>
                  <div style="font-size:12px;color:#86909c">层级：${node.layer}</div>
                  <div style="font-size:12px;color:#86909c">负责人：${node.owner}</div>`
        }
        return ''
      }
    },
    series: [{
      type: 'graph',
      layout: 'none',
      roam: true,
      draggable: true,
      focusNodeAdjacency: true,
      data: graphNodes,
      links: graphLinks,
      lineStyle: { curveness: 0.2 },
      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [0, 8],
      animationDuration: 800,
      animationEasingUpdate: 'quinticInOut'
    }]
  }
})

function typeIcon(type) {
  const map = { '数据表': 'Grid', 'API': 'Connection', '字段': 'Menu' }
  return map[type] || 'Document'
}

function layerColor(layer) {
  const map = { ODS: '#1664ff', DWD: '#00b42a', DWS: '#ff7d00', ADS: '#722ed1', API: '#f53f3f' }
  return map[layer] || '#86909c'
}

function layerBg(layer) {
  const map = { ODS: '#e8f0ff', DWD: '#e8ffea', DWS: '#fff3e8', ADS: '#f5e8ff', API: '#ffece8' }
  return map[layer] || '#f7f8fa'
}

function layerTagType(layer) {
  const map = { ODS: 'primary', DWD: 'success', DWS: 'warning', ADS: 'danger', API: 'info' }
  return map[layer] || 'info'
}

function formatNum(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n.toString()
}

function selectNode(node) {
  selectedNode.value = node
  activeTab.value = 'basic'
}

function handleSearch() {
  if (filteredAssets.value.length > 0) {
    selectNode(filteredAssets.value[0])
  }
}

function handleTypeChange() {
  ElMessage.info(`已切换为${lineageType.value === 'table' ? '表级' : '字段级'}血缘`)
}

function zoomIn() {
  ElMessage.info('放大视图')
}

function zoomOut() {
  ElMessage.info('缩小视图')
}

function resetView() {
  ElMessage.info('重置视图')
}

function goToImpact() {
  ElMessage.info('跳转到影响分析页面')
}

onMounted(() => {
  nextTick(() => {
    selectNode(nodes.value[6])
  })
})
</script>

<style scoped>
.governance-lineage {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.lineage-layout {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}

/* 左侧边栏 */
.lineage-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: var(--dp-radius);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 12px;
}

.sidebar-search {
  margin-bottom: 12px;
}

.sidebar-list-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
  border-bottom: 1px solid var(--dp-border-light);
}

.count-badge {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  margin-top: 8px;
}

.asset-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 4px;
}

.asset-item:hover {
  background: var(--dp-bg-page);
}

.asset-item.active {
  background: var(--dp-primary-light);
}

.asset-icon-sm {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.asset-info {
  flex: 1;
  min-width: 0;
}

.asset-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.asset-layer {
  margin-top: 2px;
}

/* 中间主区域 */
.lineage-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.graph-toolbar {
  padding: 12px 16px;
  margin-bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.graph-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  margin-bottom: 0;
}

.layer-legend {
  position: absolute;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 20px;
  z-index: 1;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px 16px;
  border-radius: 20px;
  backdrop-filter: blur(10px);
}

.layer-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--dp-text-2);
}

.layer-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: var(--dp-radius);
  padding: 14px;
  display: flex;
  gap: 12px;
  align-items: center;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.primary { background: var(--dp-primary-light); color: var(--dp-primary); }
.stat-icon.success { background: var(--dp-success-light); color: var(--dp-success); }
.stat-icon.warning { background: var(--dp-warning-light); color: var(--dp-warning); }
.stat-icon.purple { background: var(--dp-purple-light); color: var(--dp-purple); }

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', sans-serif;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

/* 右侧详情面板 */
.lineage-detail {
  width: 300px;
  flex-shrink: 0;
  padding: 16px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.detail-header {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--dp-border-light);
}

.detail-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.detail-title-info {
  flex: 1;
  min-width: 0;
}

.detail-name {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.detail-code {
  font-size: 11px;
  color: var(--dp-text-3);
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
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.node-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: var(--dp-bg-page);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.node-item:hover {
  background: var(--dp-primary-light);
}

.node-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.node-icon.up {
  background: var(--dp-warning-light);
  color: var(--dp-warning);
}

.node-icon.down {
  background: var(--dp-success-light);
  color: var(--dp-success);
}

.node-info {
  flex: 1;
  min-width: 0;
}

.node-name {
  font-size: 13px;
  color: var(--dp-text-1);
  font-weight: 500;
}

.node-layer {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

.detail-actions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--dp-border-light);
  display: flex;
  gap: 10px;
}

.detail-actions .el-button {
  flex: 1;
}

.empty-detail {
  margin: auto;
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
