<template>
  <div class="dp-page">
    <PageHeader title="数据处理流程" desc="管理各类数据处理流程，支持可视化编排与调度配置">
      <el-button type="primary" :icon="Plus" @click="openCreate">新建流程</el-button>
    </PageHeader>

    <div class="dp-card">
      <!-- 工具栏 -->
      <div class="dp-toolbar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索流程名称"
          clearable
          style="width: 240px"
          :prefix-icon="Search"
          @input="handleSearch"
        />
        <el-select
          v-model="searchForm.type"
          placeholder="流程类型"
          clearable
          style="width: 130px"
          @change="handleSearch"
        >
          <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select
          v-model="searchForm.status"
          placeholder="状态"
          clearable
          style="width: 120px"
          @change="handleSearch"
        >
          <el-option label="运行中" value="运行中" />
          <el-option label="已发布" value="已发布" />
          <el-option label="开发中" value="开发中" />
          <el-option label="已暂停" value="已暂停" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="handleSearch">刷新</el-button>
          <el-button type="primary" :icon="VideoPlay" @click="batchRun">批量运行</el-button>
        </div>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="selection" width="44" />
        <el-table-column prop="name" label="流程名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="pr-name-wrap">
              <span class="pr-name">{{ row.name }}</span>
              <span class="dp-tag" :class="typeTagClass(row.type)">{{ row.type }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="nodes" label="节点数" width="80" align="center" />
        <el-table-column prop="schedule" label="调度周期" width="130">
          <template #default="{ row }">
            <span class="pr-schedule">
              <el-icon :size="12" color="#86909c"><Timer /></el-icon>
              {{ row.schedule }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="avgDuration" label="平均耗时" width="90" align="center" />
        <el-table-column label="最近运行" width="160">
          <template #default="{ row }">
            <div class="pr-last-run">
              <span class="pr-last-time">{{ row.updatedAt }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="pr-status">
              <span class="pr-status-dot" :class="statusDotClass(row.status)"></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditor(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="runProcess(row)">运行</el-button>
            <el-button link type="primary" size="small" @click="viewRecords(row)">记录</el-button>
            <el-button link type="primary" size="small" @click="copyProcess(row)">复制</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="handleSearch"
      />
    </div>

    <!-- 流程编辑器弹窗 -->
    <el-dialog v-model="editorVisible" :title="isEdit ? '编辑流程' : '新建流程'" width="900px" destroy-on-close class="pr-editor-dialog">
      <div class="pr-editor">
        <!-- 左侧节点面板 -->
        <div class="pr-node-panel">
          <div class="pr-panel-title">节点组件</div>
          <div class="pr-node-group">
            <div class="pr-node-group-title">数据源</div>
            <div v-for="n in nodeTypes.source" :key="n.type" class="pr-node-item" draggable="true" @click="addNode(n.type)">
              <el-icon :size="14" :color="n.color"><component :is="n.icon" /></el-icon>
              <span>{{ n.label }}</span>
            </div>
          </div>
          <div class="pr-node-group">
            <div class="pr-node-group-title">数据转换</div>
            <div v-for="n in nodeTypes.transform" :key="n.type" class="pr-node-item" draggable="true" @click="addNode(n.type)">
              <el-icon :size="14" :color="n.color"><component :is="n.icon" /></el-icon>
              <span>{{ n.label }}</span>
            </div>
          </div>
          <div class="pr-node-group">
            <div class="pr-node-group-title">数据输出</div>
            <div v-for="n in nodeTypes.output" :key="n.type" class="pr-node-item" draggable="true" @click="addNode(n.type)">
              <el-icon :size="14" :color="n.color"><component :is="n.icon" /></el-icon>
              <span>{{ n.label }}</span>
            </div>
          </div>
          <div class="pr-node-group">
            <div class="pr-node-group-title">控制节点</div>
            <div v-for="n in nodeTypes.control" :key="n.type" class="pr-node-item" draggable="true" @click="addNode(n.type)">
              <el-icon :size="14" :color="n.color"><component :is="n.icon" /></el-icon>
              <span>{{ n.label }}</span>
            </div>
          </div>
        </div>

        <!-- 中间画布 -->
        <div class="pr-canvas-wrap">
          <div class="pr-canvas-toolbar">
            <el-input v-model="processName" placeholder="请输入流程名称" style="width: 200px" size="small" />
            <span class="pr-canvas-info">{{ canvasNodes.length }} 个节点</span>
            <div style="margin-left: auto" class="dp-flex dp-gap-8">
              <el-button size="small" :icon="ZoomOut" @click="zoomOut">缩小</el-button>
              <span class="pr-zoom-text">{{ Math.round(scale * 100) }}%</span>
              <el-button size="small" :icon="ZoomIn" @click="zoomIn">放大</el-button>
            </div>
          </div>
          <div ref="canvasRef" class="pr-canvas" @click="selectedNode = null">
            <svg class="pr-edges">
              <path
                v-for="e in canvasEdges" :key="e.id"
                :d="getEdgePath(e)"
                fill="none"
                stroke="#a9aeb8"
                stroke-width="1.6"
                marker-end="url(#arrowhead)"
              />
              <defs>
                <marker id="arrowhead" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto">
                  <polygon points="0 0, 8 3, 0 6" fill="#a9aeb8" />
                </marker>
              </defs>
            </svg>

            <div
              v-for="node in canvasNodes" :key="node.id"
              class="pr-flow-node"
              :class="{ selected: selectedNode?.id === node.id }"
              :style="{ left: node.x + 'px', top: node.y + 'px' }"
              @mousedown.stop="startDragNode(node, $event)"
              @click.stop="selectNode(node)"
            >
              <div class="pr-node-head" :style="{ borderLeftColor: getNodeColor(node.type) }">
                <el-icon :size="14" :color="getNodeColor(node.type)"><component :is="getNodeIcon(node.type)" /></el-icon>
                <span class="pr-node-title">{{ node.name }}</span>
              </div>
              <div class="pr-node-body">
                <span class="pr-node-desc">{{ node.desc }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧属性面板 -->
        <div class="pr-prop-panel">
          <div class="pr-panel-title">节点属性</div>
          <template v-if="selectedNode">
            <el-form label-width="70px" size="small">
              <el-form-item label="节点名称">
                <el-input v-model="selectedNode.name" />
              </el-form-item>
              <el-form-item label="节点类型">
                <el-input :model-value="getNodeLabel(selectedNode.type)" disabled />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="selectedNode.desc" type="textarea" :rows="2" />
              </el-form-item>
              <el-form-item v-if="selectedNode.type === 'sql'" label="SQL语句">
                <el-input v-model="selectedNode.sql" type="textarea" :rows="5" class="dp-mono" placeholder="SELECT * FROM ..." />
              </el-form-item>
              <el-form-item v-if="selectedNode.type === 'filter'" label="过滤条件">
                <el-input v-model="selectedNode.condition" type="textarea" :rows="3" class="dp-mono" placeholder="amount > 100" />
              </el-form-item>
              <el-form-item label="失败策略">
                <el-radio-group model-value="retry">
                  <el-radio value="retry">重试</el-radio>
                  <el-radio value="skip">跳过</el-radio>
                  <el-radio value="stop">阻断</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-form>
            <el-button
              type="danger"
              plain
              size="small"
              style="width: 100%; margin-top: 12px"
              :icon="Delete"
              @click="removeNode"
            >
              删除节点
            </el-button>
          </template>
          <div v-else class="pr-empty-prop">
            <el-icon :size="32" color="#c9cdd4"><Pointer /></el-icon>
            <span>请选择节点查看属性</span>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button @click="saveDraft">保存草稿</el-button>
        <el-button type="primary" @click="saveProcess">保存并发布</el-button>
      </template>
    </el-dialog>

    <!-- 运行记录抽屉 -->
    <el-drawer v-model="recordVisible" title="流程运行记录" size="600px">
      <template v-if="currentProcess">
        <div class="pr-record-header">
          <div class="pr-record-title">{{ currentProcess.name }}</div>
          <el-tag :type="typeTagType(currentProcess.type)" size="small" effect="plain">{{ currentProcess.type }}</el-tag>
        </div>
        <el-table :data="runRecords" border size="small">
          <el-table-column prop="time" label="运行时间" width="170" />
          <el-table-column prop="trigger" label="触发方式" width="100" />
          <el-table-column prop="duration" label="耗时" width="80" align="center" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === '成功' ? 'success' : row.status === '运行中' ? 'primary' : 'danger'" size="small">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default>
              <el-button link type="primary" size="small">查看日志</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { dataProcesses } from '@/mock'
import { Delete, Plus, Refresh, Search, VideoPlay, ZoomIn, ZoomOut } from '@element-plus/icons-vue'

const loading = ref(false)
const editorVisible = ref(false)
const recordVisible = ref(false)
const isEdit = ref(false)
const currentProcess = ref(null)
const selectedNode = ref(null)
const canvasRef = ref(null)
const processName = ref('')
const scale = ref(1)

const typeOptions = ['SQL任务', '脚本任务', '数据同步', '混合流程']

const searchForm = reactive({
  keyword: '',
  type: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const allData = ref([])
const tableData = ref([])

// 节点类型定义
const nodeTypes = {
  source: [
    { type: 'mysql_input', label: 'MySQL抽取', icon: 'Coin', color: '#4479a1' },
    { type: 'file_input', label: '文件抽取', icon: 'Folder', color: '#00b42a' },
    { type: 'api_input', label: 'API抽取', icon: 'Connection', color: '#722ed1' }
  ],
  transform: [
    { type: 'filter', label: '过滤', icon: 'Filter', color: '#1664ff' },
    { type: 'map', label: '映射', icon: 'SetUp', color: '#1664ff' },
    { type: 'aggregate', label: '聚合', icon: 'Histogram', color: '#1664ff' },
    { type: 'join', label: '关联', icon: 'Link', color: '#1664ff' },
    { type: 'sort', label: '排序', icon: 'Sort', color: '#1664ff' }
  ],
  output: [
    { type: 'mysql_output', label: '写入MySQL', icon: 'UploadFilled', color: '#4479a1' },
    { type: 'hive_output', label: '写入Hive', icon: 'Upload', color: '#f7ba1e' },
    { type: 'file_output', label: '写入文件', icon: 'Document', color: '#00b42a' }
  ],
  control: [
    { type: 'start', label: '开始', icon: 'VideoPlay', color: '#00b42a' },
    { type: 'end', label: '结束', icon: 'CircleCheck', color: '#86909c' },
    { type: 'condition', label: '条件判断', icon: 'Switch', color: '#ff7d00' },
    { type: 'loop', label: '循环', icon: 'RefreshRight', color: '#722ed1' },
    { type: 'wait', label: '等待', icon: 'Clock', color: '#86909c' }
  ]
}

// 获取所有节点的映射
const allNodeMap = computed(() => {
  const map = {}
  Object.values(nodeTypes).forEach(group => {
    group.forEach(n => { map[n.type] = n })
  })
  return map
})

// 画布节点和边
const canvasNodes = ref([])
const canvasEdges = ref([])
let nodeIdCounter = 0
let dragState = null

function getNodeColor(type) {
  return allNodeMap.value[type]?.color || '#1664ff'
}

function getNodeIcon(type) {
  return allNodeMap.value[type]?.icon || 'Document'
}

function getNodeLabel(type) {
  return allNodeMap.value[type]?.label || type
}

function typeTagClass(type) {
  return {
    'SQL任务': 'primary',
    '脚本任务': 'warning',
    '数据同步': 'success',
    '混合流程': 'purple'
  }[type] || 'info'
}

function typeTagType(type) {
  return {
    'SQL任务': 'primary',
    '脚本任务': 'warning',
    '数据同步': 'success',
    '混合流程': 'danger'
  }[type] || 'info'
}

function statusDotClass(status) {
  return {
    '运行中': 'running',
    '已发布': 'success',
    '开发中': 'warning',
    '已暂停': 'paused'
  }[status] || ''
}

function handleSearch() {
  loading.value = true
  setTimeout(() => {
    let list = allData.value.filter(d =>
      (!searchForm.keyword || d.name.includes(searchForm.keyword)) &&
      (!searchForm.type || d.type === searchForm.type) &&
      (!searchForm.status || d.status === searchForm.status)
    )
    pagination.total = list.length
    const start = (pagination.page - 1) * pagination.size
    tableData.value = list.slice(start, start + pagination.size)
    loading.value = false
  }, 300)
}

function openCreate() {
  isEdit.value = false
  processName.value = ''
  selectedNode.value = null
  nodeIdCounter = 0
  canvasNodes.value = [
    { id: 'n_start', type: 'start', name: '开始', desc: '流程起点', x: 60, y: 120 },
    { id: 'n_end', type: 'end', name: '结束', desc: '流程终点', x: 560, y: 120 }
  ]
  canvasEdges.value = []
  editorVisible.value = true
}

function openEditor(row) {
  isEdit.value = true
  currentProcess.value = row
  processName.value = row.name
  selectedNode.value = null
  nodeIdCounter = 0
  // 模拟生成节点
  canvasNodes.value = [
    { id: 'n_start', type: 'start', name: '开始', desc: '流程起点', x: 40, y: 100 },
    { id: 'n1', type: 'mysql_input', name: '抽取订单表', desc: 'ods.order_info', x: 180, y: 60 },
    { id: 'n2', type: 'mysql_input', name: '抽取会员表', desc: 'dim.customer', x: 180, y: 160 },
    { id: 'n3', type: 'filter', name: '数据过滤', desc: '过滤脏数据', x: 340, y: 60 },
    { id: 'n4', type: 'join', name: '维度关联', desc: 'JOIN customer', x: 340, y: 160 },
    { id: 'n5', type: 'aggregate', name: '聚合计算', desc: 'GROUP BY', x: 500, y: 110 },
    { id: 'n6', type: 'hive_output', name: '写入宽表', desc: 'dwd.trade_wide', x: 660, y: 110 },
    { id: 'n_end', type: 'end', name: '结束', desc: '流程终点', x: 820, y: 110 }
  ]
  canvasEdges.value = [
    { id: 'e1', from: 'n_start', to: 'n1' },
    { id: 'e2', from: 'n_start', to: 'n2' },
    { id: 'e3', from: 'n1', to: 'n3' },
    { id: 'e4', from: 'n2', to: 'n4' },
    { id: 'e5', from: 'n3', to: 'n5' },
    { id: 'e6', from: 'n4', to: 'n5' },
    { id: 'e7', from: 'n5', to: 'n6' },
    { id: 'e8', from: 'n6', to: 'n_end' }
  ]
  editorVisible.value = true
}

function addNode(type) {
  nodeIdCounter++
  const node = {
    id: 'n_' + nodeIdCounter,
    type,
    name: getNodeLabel(type),
    desc: '新节点',
    x: 200 + Math.random() * 200,
    y: 80 + Math.random() * 100,
    sql: '',
    condition: ''
  }
  canvasNodes.value.push(node)
  selectedNode.value = node
  ElMessage.success(`已添加节点：${getNodeLabel(type)}`)
}

function selectNode(node) {
  selectedNode.value = node
}

function removeNode() {
  if (!selectedNode.value) return
  const id = selectedNode.value.id
  if (id === 'n_start' || id === 'n_end') {
    ElMessage.warning('开始和结束节点不可删除')
    return
  }
  canvasNodes.value = canvasNodes.value.filter(n => n.id !== id)
  canvasEdges.value = canvasEdges.value.filter(e => e.from !== id && e.to !== id)
  selectedNode.value = null
  ElMessage.success('节点已删除')
}

function startDragNode(node, e) {
  dragState = {
    node,
    startX: e.clientX,
    startY: e.clientY,
    origX: node.x,
    origY: node.y
  }
  const move = ev => {
    if (!dragState) return
    dragState.node.x = dragState.origX + (ev.clientX - dragState.startX)
    dragState.node.y = dragState.origY + (ev.clientY - dragState.startY)
  }
  const up = () => {
    dragState = null
    document.removeEventListener('mousemove', move)
    document.removeEventListener('mouseup', up)
  }
  document.addEventListener('mousemove', move)
  document.addEventListener('mouseup', up)
}

function getEdgePath(edge) {
  const from = canvasNodes.value.find(n => n.id === edge.from)
  const to = canvasNodes.value.find(n => n.id === edge.to)
  if (!from || !to) return ''
  const x1 = from.x + 160
  const y1 = from.y + 25
  const x2 = to.x
  const y2 = to.y + 25
  const mx = (x1 + x2) / 2
  return `M ${x1} ${y1} C ${mx} ${y1}, ${mx} ${y2}, ${x2} ${y2}`
}

function zoomIn() {
  scale.value = Math.min(1.5, +(scale.value + 0.1).toFixed(1))
}

function zoomOut() {
  scale.value = Math.max(0.5, +(scale.value - 0.1).toFixed(1))
}

function saveDraft() {
  if (!processName.value) {
    ElMessage.warning('请输入流程名称')
    return
  }
  ElMessage.success('草稿已保存')
}

function saveProcess() {
  if (!processName.value) {
    ElMessage.warning('请输入流程名称')
    return
  }
  if (isEdit.value) {
    ElMessage.success('流程已更新并发布')
  } else {
    allData.value.unshift({
      id: Date.now(),
      name: processName.value,
      type: '混合流程',
      nodes: canvasNodes.value.length,
      schedule: '每天 00:30',
      status: '已发布',
      avgDuration: '5m',
      owner: '当前用户',
      updatedAt: '刚刚'
    })
    ElMessage.success('流程创建成功')
  }
  editorVisible.value = false
  handleSearch()
}

function runProcess(row) {
  row.status = '运行中'
  ElMessage.success(`流程「${row.name}」已提交运行`)
  setTimeout(() => {
    row.status = '已发布'
    ElMessage.success(`流程「${row.name}」运行完成`)
  }, 2000)
}

function batchRun() {
  ElMessage.info('已提交批量运行任务')
}

function viewRecords(row) {
  currentProcess.value = row
  recordVisible.value = true
}

const runRecords = ref([
  { time: '2026-09-26 00:30:02', trigger: '调度触发', duration: '3m20s', status: '成功' },
  { time: '2026-09-25 00:30:01', trigger: '调度触发', duration: '3m15s', status: '成功' },
  { time: '2026-09-24 00:30:03', trigger: '调度触发', duration: '3m28s', status: '成功' },
  { time: '2026-09-23 00:30:02', trigger: '手动触发', duration: '5m10s', status: '失败' },
  { time: '2026-09-22 00:30:01', trigger: '调度触发', duration: '3m05s', status: '成功' }
])

function copyProcess(row) {
  const newProcess = {
    ...row,
    id: Date.now(),
    name: row.name + ' - 副本',
    status: '开发中',
    updatedAt: '刚刚'
  }
  allData.value.unshift(newProcess)
  ElMessage.success('流程已复制')
  handleSearch()
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除流程「${row.name}」吗？删除后不可恢复。`, '确认删除', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(() => {
    allData.value = allData.value.filter(d => d.id !== row.id)
    ElMessage.success('删除成功')
    handleSearch()
  }).catch(() => {})
}

onMounted(() => {
  allData.value = [...dataProcesses]
  handleSearch()
})
</script>

<style scoped>
.pr-name-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pr-name {
  font-weight: 500;
  color: var(--dp-text-1);
}

.pr-schedule {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--dp-text-2);
}

.pr-last-run {
  font-size: 12px;
}

.pr-last-time {
  color: var(--dp-text-2);
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11.5px;
}

/* 状态指示灯 */
.pr-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--dp-text-2);
}

.pr-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--dp-text-4);
}

.pr-status-dot.running {
  background: var(--dp-primary);
  box-shadow: 0 0 0 2px rgba(22, 100, 255, 0.15);
  animation: pr-pulse 2s infinite;
}

.pr-status-dot.success {
  background: var(--dp-success);
}

.pr-status-dot.warning {
  background: var(--dp-warning);
}

.pr-status-dot.paused {
  background: var(--dp-text-4);
}

@keyframes pr-pulse {
  0%, 100% { box-shadow: 0 0 0 2px rgba(22, 100, 255, 0.15); }
  50% { box-shadow: 0 0 0 5px rgba(22, 100, 255, 0.08); }
}

/* 流程编辑器弹窗 */
.pr-editor-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.pr-editor {
  display: flex;
  height: 520px;
}

/* 节点面板 */
.pr-node-panel {
  width: 160px;
  border-right: 1px solid var(--dp-border-light);
  padding: 12px;
  overflow-y: auto;
  background: #fafbfc;
  flex-shrink: 0;
}

.pr-panel-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 12px;
}

.pr-node-group {
  margin-bottom: 14px;
}

.pr-node-group-title {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-bottom: 8px;
  font-weight: 500;
}

.pr-node-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: 6px;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--dp-text-2);
  cursor: grab;
  transition: all 0.15s;
}

.pr-node-item:hover {
  border-color: var(--dp-primary);
  color: var(--dp-primary);
  background: var(--dp-primary-bg);
}

/* 画布区域 */
.pr-canvas-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.pr-canvas-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--dp-border-light);
  flex-shrink: 0;
}

.pr-canvas-info {
  font-size: 12px;
  color: var(--dp-text-3);
}

.pr-zoom-text {
  font-size: 12px;
  color: var(--dp-text-3);
  min-width: 40px;
  text-align: center;
}

.pr-canvas {
  flex: 1;
  position: relative;
  overflow: auto;
  background-color: #fafbfc;
  background-image: radial-gradient(circle, #dfe2e8 1px, transparent 1px);
  background-size: 20px 20px;
}

.pr-edges {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

/* 流程节点 */
.pr-flow-node {
  position: absolute;
  width: 160px;
  background: #fff;
  border: 1.5px solid var(--dp-border);
  border-radius: 8px;
  cursor: move;
  user-select: none;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
  z-index: 2;
  transition: all 0.15s;
}

.pr-flow-node:hover {
  box-shadow: 0 4px 12px rgba(22, 100, 255, 0.15);
}

.pr-flow-node.selected {
  border-color: var(--dp-primary);
  box-shadow: 0 0 0 3px rgba(22, 100, 255, 0.12);
}

.pr-node-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-left: 3px solid var(--dp-primary);
  border-bottom: 1px solid var(--dp-border-light);
  border-radius: 6px 6px 0 0;
}

.pr-node-title {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--dp-text-1);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pr-node-body {
  padding: 8px 10px;
}

.pr-node-desc {
  font-size: 11px;
  color: var(--dp-text-3);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 属性面板 */
.pr-prop-panel {
  width: 240px;
  border-left: 1px solid var(--dp-border-light);
  padding: 12px;
  overflow-y: auto;
  flex-shrink: 0;
  background: #fafbfc;
}

.pr-empty-prop {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  gap: 10px;
  color: var(--dp-text-4);
  font-size: 12px;
}

/* 运行记录 */
.pr-record-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.pr-record-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--dp-text-1);
}
</style>
