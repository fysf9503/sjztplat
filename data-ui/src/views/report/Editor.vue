<template>
  <div class="dp-page rp-editor-page">
    <!-- 顶部工具栏 -->
    <div class="rp-editor-topbar">
      <div class="rp-topbar-left">
        <el-button :icon="ArrowLeft" text @click="handleBack">返回</el-button>
        <el-divider direction="vertical" />
        <el-input v-model="reportName" class="rp-report-name-input" size="large" />
        <el-tag size="small" type="info" effect="plain">{{ statusText }}</el-tag>
      </div>
      <div class="rp-topbar-center">
        <div class="rp-zoom-control">
          <el-button :icon="ZoomOut" circle size="small" @click="zoomOut" />
          <span class="rp-zoom-value">{{ zoomLevel }}%</span>
          <el-button :icon="ZoomIn" circle size="small" @click="zoomIn" />
        </div>
      </div>
      <div class="rp-topbar-right">
        <el-button :icon="RefreshLeft" :disabled="!canUndo" @click="handleUndo">撤销</el-button>
        <el-button :icon="RefreshRight" :disabled="!canRedo" @click="handleRedo">重做</el-button>
        <el-divider direction="vertical" />
        <el-button :icon="View" @click="handlePreview">预览</el-button>
        <el-button :icon="Download" @click="handleExport">导出</el-button>
        <el-button type="primary" :icon="Check" @click="handleSave">保存</el-button>
        <el-button type="success" :icon="Promotion" @click="handlePublish">发布</el-button>
      </div>
    </div>

    <div class="rp-editor-body">
      <!-- 左侧组件面板 -->
      <div class="rp-editor-left">
        <el-tabs v-model="leftTab" class="rp-left-tabs">
          <el-tab-pane label="组件" name="components">
            <div class="rp-comp-section">
              <div class="rp-comp-section-title">图表组件</div>
              <div class="rp-comp-grid">
                <div
                  v-for="comp in chartComponents"
                  :key="comp.type"
                  class="rp-comp-item"
                  draggable="true"
                  @dragstart="onDragStart($event, comp)"
                  @click="addComponent(comp)"
                >
                  <div class="rp-comp-icon" :style="{ background: comp.bg, color: comp.color }">
                    <el-icon :size="20"><component :is="comp.icon" /></el-icon>
                  </div>
                  <span class="rp-comp-name">{{ comp.name }}</span>
                </div>
              </div>
            </div>
            <div class="rp-comp-section">
              <div class="rp-comp-section-title">文本组件</div>
              <div class="rp-comp-grid">
                <div
                  v-for="comp in textComponents"
                  :key="comp.type"
                  class="rp-comp-item"
                  draggable="true"
                  @dragstart="onDragStart($event, comp)"
                  @click="addComponent(comp)"
                >
                  <div class="rp-comp-icon" :style="{ background: comp.bg, color: comp.color }">
                    <el-icon :size="20"><component :is="comp.icon" /></el-icon>
                  </div>
                  <span class="rp-comp-name">{{ comp.name }}</span>
                </div>
              </div>
            </div>
            <div class="rp-comp-section">
              <div class="rp-comp-section-title">筛选器组件</div>
              <div class="rp-comp-grid">
                <div
                  v-for="comp in filterComponents"
                  :key="comp.type"
                  class="rp-comp-item"
                  draggable="true"
                  @dragstart="onDragStart($event, comp)"
                  @click="addComponent(comp)"
                >
                  <div class="rp-comp-icon" :style="{ background: comp.bg, color: comp.color }">
                    <el-icon :size="20"><component :is="comp.icon" /></el-icon>
                  </div>
                  <span class="rp-comp-name">{{ comp.name }}</span>
                </div>
              </div>
            </div>
          </el-tab-pane>
          <el-tab-pane label="图层" name="layers">
            <div class="rp-layer-list">
              <div
                v-for="(widget, idx) in widgets"
                :key="widget.id"
                class="rp-layer-item"
                :class="{ active: selectedWidget?.id === widget.id }"
                @click="selectWidget(widget)"
              >
                <el-icon class="rp-layer-icon"><component :is="widget.icon" /></el-icon>
                <span class="rp-layer-name">{{ widget.title }}</span>
                <div class="rp-layer-actions">
                  <el-button text size="small" :icon="Top" @click.stop="moveLayerUp(idx)" :disabled="idx === 0" />
                  <el-button text size="small" :icon="Bottom" @click.stop="moveLayerDown(idx)" :disabled="idx === widgets.length - 1" />
                  <el-button text size="small" type="danger" :icon="Delete" @click.stop="removeWidget(widget)" />
                </div>
              </div>
              <el-empty v-if="widgets.length === 0" description="暂无组件" :image-size="60" />
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 中间画布区域 -->
      <div class="rp-editor-center">
        <div
          class="rp-canvas-container"
          @dragover.prevent
          @drop="onDrop"
          @click="deselectWidget"
        >
          <div
            class="rp-canvas"
            :style="{ transform: `scale(${zoomLevel / 100})` }"
          >
            <div class="rp-canvas-header">
              <div class="rp-canvas-title">{{ reportName }}</div>
              <div class="rp-canvas-subtitle">数据更新时间：{{ updateTime }}</div>
            </div>
            <div
              v-for="widget in widgets"
              :key="widget.id"
              class="rp-widget"
              :class="{ selected: selectedWidget?.id === widget.id }"
              :style="widgetStyle(widget)"
              @click.stop="selectWidget(widget)"
            >
              <div class="rp-widget-header">
                <span class="rp-widget-title">{{ widget.title }}</span>
                <div class="rp-widget-toolbar">
                  <el-button text size="small" :icon="Refresh" />
                  <el-button text size="small" :icon="MoreFilled" />
                </div>
              </div>
              <div class="rp-widget-body">
                <EChart v-if="widget.category === 'chart'" :option="widget.option" height="100%" />
                <div v-else-if="widget.category === 'text'" class="rp-widget-text">
                  <div v-if="widget.type === 'title'" class="rp-text-title">{{ widget.content || '标题文本' }}</div>
                  <div v-else-if="widget.type === 'paragraph'" class="rp-text-paragraph">{{ widget.content || '这是一段说明文本，用于展示报表的说明信息。' }}</div>
                  <div v-else-if="widget.type === 'metric'" class="rp-text-metric">
                    <div class="rp-metric-value">{{ widget.value || '12,580' }}</div>
                    <div class="rp-metric-label">{{ widget.label || '指标名称' }}</div>
                  </div>
                </div>
                <div v-else-if="widget.category === 'filter'" class="rp-widget-filter">
                  <el-select placeholder="请选择" size="small" style="width: 100%">
                    <el-option label="选项1" value="1" />
                    <el-option label="选项2" value="2" />
                    <el-option label="选项3" value="3" />
                  </el-select>
                </div>
              </div>
              <div v-if="selectedWidget?.id === widget.id" class="rp-widget-resize-handle"></div>
            </div>
            <el-empty v-if="widgets.length === 0" class="rp-canvas-empty" description="从左侧拖拽组件到画布开始设计">
              <template #image>
                <el-icon :size="64" color="#c9cdd4"><Grid /></el-icon>
              </template>
            </el-empty>
          </div>
        </div>
      </div>

      <!-- 右侧属性面板 -->
      <div class="rp-editor-right">
        <div class="rp-prop-panel">
          <div class="rp-prop-header">
            <el-icon><Setting /></el-icon>
            <span>属性配置</span>
          </div>
          <div v-if="selectedWidget" class="rp-prop-content">
            <el-form label-width="80px" size="small">
              <el-form-item label="标题">
                <el-input v-model="selectedWidget.title" />
              </el-form-item>
              <el-form-item label="宽度">
                <el-input-number v-model="selectedWidget.w" :min="100" :max="1200" :step="50" style="width: 100%" />
              </el-form-item>
              <el-form-item label="高度">
                <el-input-number v-model="selectedWidget.h" :min="80" :max="800" :step="50" style="width: 100%" />
              </el-form-item>
              <el-divider>样式</el-divider>
              <el-form-item label="背景色">
                <el-color-picker v-model="selectedWidget.bgColor" />
              </el-form-item>
              <el-form-item label="圆角">
                <el-input-number v-model="selectedWidget.radius" :min="0" :max="24" style="width: 100%" />
              </el-form-item>
              <el-form-item label="显示边框">
                <el-switch v-model="selectedWidget.showBorder" />
              </el-form-item>
              <el-divider>数据</el-divider>
              <el-form-item label="数据集">
                <el-select v-model="selectedWidget.dataset" style="width: 100%">
                  <el-option v-for="ds in datasetOptions" :key="ds" :label="ds" :value="ds" />
                </el-select>
              </el-form-item>
              <el-form-item label="维度字段">
                <el-select v-model="selectedWidget.dimension" multiple style="width: 100%">
                  <el-option label="日期" value="date" />
                  <el-option label="地区" value="region" />
                  <el-option label="产品" value="product" />
                  <el-option label="渠道" value="channel" />
                </el-select>
              </el-form-item>
              <el-form-item label="指标字段">
                <el-select v-model="selectedWidget.metric" multiple style="width: 100%">
                  <el-option label="销售额" value="sales" />
                  <el-option label="订单量" value="orders" />
                  <el-option label="用户数" value="users" />
                  <el-option label="利润率" value="profit" />
                </el-select>
              </el-form-item>
              <el-form-item label="筛选条件">
                <el-button type="primary" link size="small">配置筛选</el-button>
              </el-form-item>
            </el-form>
          </div>
          <el-empty v-else description="请选择组件进行配置" :image-size="80" />
        </div>
      </div>
    </div>

    <!-- 底部数据集面板 -->
    <div class="rp-editor-bottom">
      <div class="rp-bottom-tabs">
        <div
          v-for="tab in bottomTabs"
          :key="tab.key"
          class="rp-bottom-tab"
          :class="{ active: activeBottomTab === tab.key }"
          @click="activeBottomTab = tab.key"
        >
          <el-icon><component :is="tab.icon" /></el-icon>
          <span>{{ tab.label }}</span>
        </div>
        <div class="rp-bottom-toggle" @click="toggleBottomPanel">
          <el-icon><component :is="bottomPanelCollapsed ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
        </div>
      </div>
      <div v-show="!bottomPanelCollapsed" class="rp-bottom-content">
        <div v-if="activeBottomTab === 'dataset'" class="rp-dataset-panel">
          <div class="rp-dataset-list">
            <div
              v-for="ds in datasetOptions"
              :key="ds"
              class="rp-dataset-item"
              :class="{ active: selectedDataset === ds }"
              @click="selectDataset(ds)"
            >
              <el-icon><DataLine /></el-icon>
              <span>{{ ds }}</span>
            </div>
          </div>
          <div class="rp-dataset-preview">
            <div class="rp-dataset-preview-header">
              <span>数据预览</span>
              <span class="rp-dataset-count">共 1,258 条数据</span>
            </div>
            <el-table :data="previewData" border stripe size="small" max-height="180">
              <el-table-column prop="date" label="日期" width="110" />
              <el-table-column prop="region" label="地区" width="90" />
              <el-table-column prop="product" label="产品" width="120" />
              <el-table-column prop="sales" label="销售额" width="100" align="right" />
              <el-table-column prop="orders" label="订单量" width="90" align="right" />
              <el-table-column prop="users" label="用户数" width="90" align="right" />
            </el-table>
          </div>
        </div>
        <div v-else-if="activeBottomTab === 'filter'" class="rp-filter-panel">
          <div class="rp-filter-list">
            <div v-for="f in globalFilters" :key="f.id" class="rp-filter-item">
              <span class="rp-filter-name">{{ f.name }}</span>
              <el-select v-model="f.value" size="small" placeholder="全部" clearable style="width: 160px">
                <el-option v-for="opt in f.options" :key="opt" :label="opt" :value="opt" />
              </el-select>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import EChart from '@/components/EChart.vue'
import { ArrowLeft, Bottom, Check, Delete, Download, MoreFilled, Promotion, Refresh, RefreshLeft, RefreshRight, Top, View, ZoomIn, ZoomOut } from '@element-plus/icons-vue'

const reportName = ref('销售经营分析报表')
const statusText = ref('编辑中')
const zoomLevel = ref(100)
const leftTab = ref('components')
const activeBottomTab = ref('dataset')
const bottomPanelCollapsed = ref(false)
const selectedWidget = ref(null)
const selectedDataset = ref('销售订单明细')
const canUndo = ref(true)
const canRedo = ref(false)
const updateTime = ref('2026-09-26 10:30:00')

const bottomTabs = [
  { key: 'dataset', label: '数据集', icon: 'DataLine' },
  { key: 'filter', label: '全局筛选', icon: 'Filter' }
]

const chartComponents = [
  { type: 'line', name: '折线图', icon: 'TrendCharts', category: 'chart', bg: '#e8f3ff', color: '#165dff' },
  { type: 'bar', name: '柱状图', icon: 'Histogram', category: 'chart', bg: '#e8ffea', color: '#00b42a' },
  { type: 'pie', name: '饼图', icon: 'PieChart', category: 'chart', bg: '#fff3e8', color: '#ff7d00' },
  { type: 'area', name: '面积图', icon: 'Crop', category: 'chart', bg: '#f5e8ff', color: '#722ed1' },
  { type: 'funnel', name: '漏斗图', icon: 'Funnel', category: 'chart', bg: '#ffece8', color: '#f53f3f' },
  { type: 'map', name: '地图', icon: 'Location', category: 'chart', bg: '#e0fffa', color: '#0fc6c2' },
  { type: 'scatter', name: '散点图', icon: 'DataLine', category: 'chart', bg: '#ffe8f4', color: '#f759ab' },
  { type: 'table', name: '表格', icon: 'Grid', category: 'chart', bg: '#f2f3f5', color: '#4e5969' }
]

const textComponents = [
  { type: 'title', name: '标题', icon: 'EditPen', category: 'text', bg: '#e8f3ff', color: '#165dff' },
  { type: 'paragraph', name: '文本', icon: 'Document', category: 'text', bg: '#e8ffea', color: '#00b42a' },
  { type: 'metric', name: '指标卡', icon: 'DataAnalysis', category: 'text', bg: '#fff3e8', color: '#ff7d00' }
]

const filterComponents = [
  { type: 'select', name: '下拉筛选', icon: 'ArrowDown', category: 'filter', bg: '#f5e8ff', color: '#722ed1' },
  { type: 'date', name: '日期筛选', icon: 'Calendar', category: 'filter', bg: '#e0fffa', color: '#0fc6c2' },
  { type: 'search', name: '搜索框', icon: 'Search', category: 'filter', bg: '#ffe8f4', color: '#f759ab' }
]

const datasetOptions = ['销售订单明细', '客户画像数据', '商品销售汇总', '渠道转化数据', '会员等级数据', '财务收入明细']

const globalFilters = [
  { id: 1, name: '时间范围', value: '', options: ['近7天', '近30天', '近90天', '本月', '上月', '本年'] },
  { id: 2, name: '地区', value: '', options: ['华东', '华北', '华南', '西南', '西北', '东北'] },
  { id: 3, name: '产品分类', value: '', options: ['电子产品', '服装鞋帽', '食品饮料', '家居用品', '美妆个护'] }
]

const previewData = [
  { date: '2026-09-25', region: '华东', product: 'iPhone 15', sales: 125800, orders: 156, users: 142 },
  { date: '2026-09-25', region: '华北', product: 'MacBook Air', sales: 284700, orders: 89, users: 85 },
  { date: '2026-09-25', region: '华南', product: 'AirPods Pro', sales: 45600, orders: 240, users: 218 },
  { date: '2026-09-24', region: '华东', product: 'iPad Pro', sales: 169800, orders: 125, users: 118 },
  { date: '2026-09-24', region: '西南', product: 'Apple Watch', sales: 89700, orders: 186, users: 172 }
]

const widgets = reactive([
  {
    id: 1, type: 'line', category: 'chart', icon: 'TrendCharts',
    title: '销售趋势', x: 20, y: 20, w: 480, h: 280,
    bgColor: '#ffffff', radius: 8, showBorder: true,
    dataset: '销售订单明细', dimension: ['date'], metric: ['sales'],
    option: {
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', data: ['09-19', '09-20', '09-21', '09-22', '09-23', '09-24', '09-25'], axisLabel: { color: '#86909c', fontSize: 10 } },
      yAxis: { type: 'value', axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#f2f3f5' } }, axisLabel: { color: '#86909c', fontSize: 10 } },
      series: [{ name: '销售额', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5, data: [120, 132, 101, 134, 90, 230, 210], lineStyle: { color: '#165dff', width: 2 }, itemStyle: { color: '#165dff' }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(22,93,255,0.2)' }, { offset: 1, color: 'rgba(22,93,255,0)' }] } } }]
    }
  },
  {
    id: 2, type: 'bar', category: 'chart', icon: 'Histogram',
    title: '各地区销售对比', x: 520, y: 20, w: 420, h: 280,
    bgColor: '#ffffff', radius: 8, showBorder: true,
    dataset: '销售订单明细', dimension: ['region'], metric: ['sales', 'orders'],
    option: {
      tooltip: { trigger: 'axis' },
      legend: { top: 0, right: 0, textStyle: { color: '#86909c', fontSize: 11 } },
      grid: { left: 40, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', data: ['华东', '华北', '华南', '西南', '西北'], axisLabel: { color: '#86909c', fontSize: 10 } },
      yAxis: { type: 'value', axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#f2f3f5' } }, axisLabel: { color: '#86909c', fontSize: 10 } },
      series: [
        { name: '销售额', type: 'bar', data: [320, 280, 250, 180, 120], itemStyle: { color: '#165dff', borderRadius: [4, 4, 0, 0] }, barWidth: 16 },
        { name: '订单量', type: 'bar', data: [220, 190, 170, 120, 80], itemStyle: { color: '#00b42a', borderRadius: [4, 4, 0, 0] }, barWidth: 16 }
      ]
    }
  },
  {
    id: 3, type: 'pie', category: 'chart', icon: 'PieChart',
    title: '产品分类占比', x: 20, y: 320, w: 380, h: 260,
    bgColor: '#ffffff', radius: 8, showBorder: true,
    dataset: '商品销售汇总', dimension: ['category'], metric: ['sales'],
    option: {
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0, textStyle: { color: '#86909c', fontSize: 11 } },
      series: [{
        type: 'pie', radius: ['45%', '68%'], center: ['50%', '42%'],
        itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}\n{d}%', fontSize: 10 },
        data: [
          { value: 335, name: '电子产品', itemStyle: { color: '#165dff' } },
          { value: 310, name: '服装鞋帽', itemStyle: { color: '#00b42a' } },
          { value: 234, name: '食品饮料', itemStyle: { color: '#ff7d00' } },
          { value: 135, name: '家居用品', itemStyle: { color: '#722ed1' } },
          { value: 148, name: '美妆个护', itemStyle: { color: '#0fc6c2' } }
        ]
      }]
    }
  },
  {
    id: 4, type: 'metric', category: 'text', icon: 'DataAnalysis',
    title: '总销售额', x: 420, y: 320, w: 260, h: 120,
    bgColor: '#ffffff', radius: 8, showBorder: true,
    content: '', value: '¥1,258,430', label: '较上周 +12.5%'
  },
  {
    id: 5, type: 'metric', category: 'text', icon: 'DataAnalysis',
    title: '订单总数', x: 700, y: 320, w: 240, h: 120,
    bgColor: '#ffffff', radius: 8, showBorder: true,
    content: '', value: '8,452', label: '较上周 +8.3%'
  }
])

function widgetStyle(widget) {
  return {
    left: widget.x + 'px',
    top: widget.y + 'px',
    width: widget.w + 'px',
    height: widget.h + 'px'
  }
}

function addComponent(comp) {
  const newWidget = {
    id: Date.now(),
    type: comp.type,
    category: comp.category,
    icon: comp.icon,
    title: comp.name,
    x: 100 + widgets.length * 20,
    y: 100 + widgets.length * 20,
    w: comp.category === 'chart' ? 400 : 200,
    h: comp.category === 'chart' ? 260 : 120,
    bgColor: '#ffffff',
    radius: 8,
    showBorder: true,
    dataset: '销售订单明细',
    dimension: [],
    metric: []
  }
  if (comp.category === 'chart') {
    newWidget.option = {
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', data: ['A', 'B', 'C', 'D', 'E', 'F'], axisLabel: { color: '#86909c' } },
      yAxis: { type: 'value', axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#f2f3f5' } }, axisLabel: { color: '#86909c' } },
      series: [{ type: comp.type === 'pie' ? 'pie' : 'bar', data: [120, 200, 150, 80, 70, 110], itemStyle: { color: '#165dff' } }]
    }
  }
  widgets.push(newWidget)
  selectedWidget.value = newWidget
  ElMessage.success(`已添加${comp.name}组件`)
}

function onDragStart(e, comp) {
  e.dataTransfer.setData('component', JSON.stringify(comp))
}

function onDrop(e) {
  const data = e.dataTransfer.getData('component')
  if (data) {
    const comp = JSON.parse(data)
    addComponent(comp)
  }
}

function selectWidget(widget) {
  selectedWidget.value = widget
}

function deselectWidget() {
  selectedWidget.value = null
}

function removeWidget(widget) {
  const idx = widgets.findIndex(w => w.id === widget.id)
  if (idx > -1) {
    widgets.splice(idx, 1)
    if (selectedWidget.value?.id === widget.id) {
      selectedWidget.value = null
    }
    ElMessage.success('组件已删除')
  }
}

function moveLayerUp(idx) {
  if (idx > 0) {
    const temp = widgets[idx]
    widgets[idx] = widgets[idx - 1]
    widgets[idx - 1] = temp
  }
}

function moveLayerDown(idx) {
  if (idx < widgets.length - 1) {
    const temp = widgets[idx]
    widgets[idx] = widgets[idx + 1]
    widgets[idx + 1] = temp
  }
}

function zoomIn() {
  if (zoomLevel.value < 200) zoomLevel.value += 10
}

function zoomOut() {
  if (zoomLevel.value > 50) zoomLevel.value -= 10
}

function selectDataset(ds) {
  selectedDataset.value = ds
}

function toggleBottomPanel() {
  bottomPanelCollapsed.value = !bottomPanelCollapsed.value
}

function handleBack() {
  ElMessageBox.confirm('报表尚未保存，确定要离开吗？', '确认离开', {
    confirmButtonText: '确定离开',
    cancelButtonText: '继续编辑',
    type: 'warning'
  }).then(() => {
    ElMessage.info('返回列表页')
  }).catch(() => {})
}

function handleSave() {
  ElMessage.success('保存成功')
  statusText.value = '已保存'
}

function handlePreview() {
  ElMessage.info('正在打开预览...')
}

function handlePublish() {
  ElMessageBox.confirm('确定要发布此报表吗？发布后用户即可查看。', '发布确认', {
    confirmButtonText: '确认发布',
    cancelButtonText: '取消',
    type: 'success'
  }).then(() => {
    statusText.value = '已发布'
    ElMessage.success('发布成功')
  }).catch(() => {})
}

function handleExport() {
  ElMessage.success('导出成功')
}

function handleUndo() {
  ElMessage.info('已撤销')
}

function handleRedo() {
  ElMessage.info('已重做')
}
</script>

<style scoped>
.rp-editor-page {
  margin: -20px;
  height: calc(100vh - 56px + 40px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--dp-bg-page);
}

/* 顶部工具栏 */
.rp-editor-topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid var(--dp-border-light);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
  flex-shrink: 0;
}

.rp-topbar-left,
.rp-topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rp-topbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.rp-report-name-input {
  width: 280px;
}

.rp-report-name-input :deep(.el-input__wrapper) {
  box-shadow: none;
  background: var(--dp-bg-page);
}

.rp-zoom-control {
  display: flex;
  align-items: center;
  gap: 10px;
  background: var(--dp-bg-page);
  padding: 4px 12px;
  border-radius: 20px;
}

.rp-zoom-value {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-2);
  min-width: 50px;
  text-align: center;
}

/* 主体区域 */
.rp-editor-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* 左侧面板 */
.rp-editor-left {
  width: 240px;
  background: #fff;
  border-right: 1px solid var(--dp-border-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.rp-left-tabs {
  flex: 1;
}

.rp-left-tabs :deep(.el-tabs__content) {
  height: calc(100% - 40px);
  overflow-y: auto;
}

.rp-comp-section {
  padding: 12px;
  border-bottom: 1px solid var(--dp-border-light);
}

.rp-comp-section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--dp-text-2);
  margin-bottom: 10px;
}

.rp-comp-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.rp-comp-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 4px;
  border-radius: 8px;
  cursor: grab;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.rp-comp-item:hover {
  background: var(--dp-primary-bg);
  border-color: var(--dp-primary-light);
}

.rp-comp-item:active {
  cursor: grabbing;
}

.rp-comp-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rp-comp-name {
  font-size: 11px;
  color: var(--dp-text-2);
  text-align: center;
}

.rp-layer-list {
  padding: 8px;
}

.rp-layer-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.rp-layer-item:hover {
  background: var(--dp-bg-page);
}

.rp-layer-item.active {
  background: var(--dp-primary-light);
}

.rp-layer-icon {
  color: var(--dp-text-3);
  flex-shrink: 0;
}

.rp-layer-item.active .rp-layer-icon {
  color: var(--dp-primary);
}

.rp-layer-name {
  flex: 1;
  font-size: 13px;
  color: var(--dp-text-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rp-layer-actions {
  display: flex;
  opacity: 0;
  transition: opacity 0.2s;
}

.rp-layer-item:hover .rp-layer-actions {
  opacity: 1;
}

/* 中间画布 */
.rp-editor-center {
  flex: 1;
  overflow: auto;
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
}

.rp-canvas-container {
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  padding: 20px;
  min-width: 960px;
  min-height: 600px;
}

.rp-canvas {
  position: relative;
  width: 920px;
  min-height: 560px;
  transform-origin: top left;
}

.rp-canvas-header {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--dp-border-light);
}

.rp-canvas-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--dp-text-1);
  margin-bottom: 4px;
}

.rp-canvas-subtitle {
  font-size: 12px;
  color: var(--dp-text-3);
}

.rp-canvas-empty {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.rp-widget {
  position: absolute;
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  padding: 10px 12px;
  cursor: move;
  transition: box-shadow 0.2s;
}

.rp-widget:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.rp-widget.selected {
  border-color: var(--dp-primary);
  box-shadow: 0 0 0 2px rgba(22, 93, 255, 0.2);
}

.rp-widget-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.rp-widget-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.rp-widget-toolbar {
  display: flex;
  opacity: 0;
  transition: opacity 0.2s;
}

.rp-widget:hover .rp-widget-toolbar,
.rp-widget.selected .rp-widget-toolbar {
  opacity: 1;
}

.rp-widget-body {
  height: calc(100% - 36px);
  min-height: 60px;
}

.rp-widget-text {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.rp-text-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--dp-text-1);
}

.rp-text-paragraph {
  font-size: 13px;
  color: var(--dp-text-2);
  line-height: 1.6;
}

.rp-text-metric {
  text-align: center;
}

.rp-metric-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--dp-primary);
  font-family: 'DIN Alternate', sans-serif;
  margin-bottom: 4px;
}

.rp-metric-label {
  font-size: 12px;
  color: var(--dp-success);
}

.rp-widget-filter {
  display: flex;
  align-items: center;
  height: 100%;
}

.rp-widget-resize-handle {
  position: absolute;
  right: -4px;
  bottom: -4px;
  width: 12px;
  height: 12px;
  background: var(--dp-primary);
  border: 2px solid #fff;
  border-radius: 50%;
  cursor: se-resize;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
}

/* 右侧属性面板 */
.rp-editor-right {
  width: 280px;
  background: #fff;
  border-left: 1px solid var(--dp-border-light);
  flex-shrink: 0;
  overflow-y: auto;
}

.rp-prop-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--dp-border-light);
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.rp-prop-content {
  padding: 16px;
}

.rp-prop-content :deep(.el-divider) {
  margin: 12px 0;
}

.rp-prop-content :deep(.el-divider__text) {
  font-size: 12px;
  color: var(--dp-text-3);
  font-weight: 500;
}

/* 底部面板 */
.rp-editor-bottom {
  background: #fff;
  border-top: 1px solid var(--dp-border-light);
  flex-shrink: 0;
}

.rp-bottom-tabs {
  display: flex;
  align-items: center;
  height: 36px;
  padding: 0 12px;
  border-bottom: 1px solid var(--dp-border-light);
  position: relative;
}

.rp-bottom-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 16px;
  height: 100%;
  font-size: 13px;
  color: var(--dp-text-3);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.rp-bottom-tab:hover {
  color: var(--dp-text-1);
}

.rp-bottom-tab.active {
  color: var(--dp-primary);
  border-bottom-color: var(--dp-primary);
  font-weight: 500;
}

.rp-bottom-toggle {
  position: absolute;
  right: 12px;
  cursor: pointer;
  color: var(--dp-text-3);
}

.rp-bottom-toggle:hover {
  color: var(--dp-primary);
}

.rp-bottom-content {
  height: 240px;
  overflow: hidden;
}

.rp-dataset-panel {
  display: flex;
  height: 100%;
}

.rp-dataset-list {
  width: 180px;
  border-right: 1px solid var(--dp-border-light);
  padding: 8px 0;
  overflow-y: auto;
}

.rp-dataset-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 13px;
  color: var(--dp-text-2);
  cursor: pointer;
  transition: all 0.2s;
}

.rp-dataset-item:hover {
  background: var(--dp-bg-page);
}

.rp-dataset-item.active {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  font-weight: 500;
}

.rp-dataset-preview {
  flex: 1;
  padding: 12px 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.rp-dataset-preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.rp-dataset-count {
  font-size: 12px;
  font-weight: 400;
  color: var(--dp-text-3);
}

.rp-filter-panel {
  padding: 16px;
}

.rp-filter-list {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.rp-filter-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rp-filter-name {
  font-size: 13px;
  color: var(--dp-text-2);
  white-space: nowrap;
}
</style>
