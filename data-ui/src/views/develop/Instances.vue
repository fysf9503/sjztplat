<template>
  <div class="dp-page">
    <PageHeader title="任务实例" desc="工作流与处理流程的运行实例监控，支持状态追踪、日志查看与重跑操作">
      <el-button :icon="Refresh" @click="loadData" :loading="loading">刷新</el-button>
      <el-button type="primary" :icon="VideoPlay" @click="rerunFailed">重跑失败实例</el-button>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="ins-stat-grid">
      <StatCard label="今日实例总数" :value="statData.total" unit="个" icon="List" color="var(--dp-primary)" bg="var(--dp-primary-light)" />
      <StatCard label="运行中" :value="statData.running" unit="个" icon="Loading" color="var(--dp-warning)" bg="var(--dp-warning-light)" />
      <StatCard label="成功率" :value="statData.successRate" unit="%" icon="CircleCheck" color="var(--dp-success)" bg="var(--dp-success-light)" :trend="2.3" />
      <StatCard label="平均耗时" :value="statData.avgDuration" unit="分钟" icon="Timer" color="var(--dp-purple)" bg="var(--dp-purple-light)" :trend="-5.2" />
    </div>

    <div class="dp-card">
      <!-- 搜索工具栏 -->
      <div class="dp-toolbar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索实例ID / 工作流名称"
          clearable
          style="width: 260px"
          :prefix-icon="Search"
          @input="loadData"
        />
        <el-select
          v-model="searchForm.status"
          placeholder="实例状态"
          clearable
          style="width: 130px"
          @change="loadData"
        >
          <el-option label="运行中" value="运行中" />
          <el-option label="成功" value="成功" />
          <el-option label="失败" value="失败" />
          <el-option label="排队" value="排队" />
          <el-option label="等待依赖" value="等待依赖" />
          <el-option label="已取消" value="已取消" />
        </el-select>
        <el-select
          v-model="searchForm.type"
          placeholder="实例类型"
          clearable
          style="width: 130px"
          @change="loadData"
        >
          <el-option label="工作流" value="workflow" />
          <el-option label="处理流程" value="process" />
        </el-select>
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          value-format="YYYY-MM-DD"
        />
        <div class="dp-toolbar-right">
          <span class="ins-auto-refresh">
            <el-icon :size="12" :class="{ spinning: autoRefresh }"><Refresh /></el-icon>
            自动刷新
            <el-switch v-model="autoRefresh" size="small" style="margin-left: 6px" />
          </span>
        </div>
      </div>

      <!-- 实例表格 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="实例ID" min-width="200" class-name="dp-mono" show-overflow-tooltip />
        <el-table-column prop="workflow" label="工作流/流程" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="ins-wf-name">{{ row.workflow }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="trigger" label="触发方式" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="triggerTagType(row.trigger)">
              {{ row.trigger }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <span class="ins-status">
              <span class="ins-status-dot" :class="statusDotClass(row.status)"></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="start" label="开始时间" width="160" />
        <el-table-column prop="duration" label="耗时" width="90" align="center" />
        <el-table-column label="执行进度" width="180">
          <template #default="{ row }">
            <div class="ins-progress">
              <el-progress
                :percentage="row.progress || (row.status === '成功' ? 100 : row.status === '失败' ? 100 : 35)"
                :status="row.status === '失败' ? 'exception' : row.status === '成功' ? 'success' : ''"
                :stroke-width="8"
                :show-text="false"
              />
              <span class="ins-progress-text">
                {{ row.nodes - row.failedNodes }}/{{ row.nodes }} 节点
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="80" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">查看详情</el-button>
            <el-button link type="primary" size="small" @click="viewLog(row)">查看日志</el-button>
            <el-button v-if="row.status === '运行中' || row.status === '排队'" link type="danger" size="small" @click="stopInstance(row)">停止</el-button>
            <el-button v-if="row.status === '失败'" link type="warning" size="small" @click="rerunInstance(row)">重跑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next, jumper"
        @current-change="loadData"
      />
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="实例详情" size="600px" direction="rtl">
      <template v-if="currentInstance">
        <!-- 基本信息 -->
        <div class="ins-detail-section">
          <div class="ins-detail-title">基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="实例ID">
              <span class="dp-mono">{{ currentInstance.id }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="工作流">
              {{ currentInstance.workflow }}
            </el-descriptions-item>
            <el-descriptions-item label="触发方式">
              <el-tag size="small" effect="plain">{{ currentInstance.trigger }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <span class="ins-status">
                <span class="ins-status-dot" :class="statusDotClass(currentInstance.status)"></span>
                {{ currentInstance.status }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ currentInstance.start }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ currentInstance.endTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="耗时">{{ currentInstance.duration }}</el-descriptions-item>
            <el-descriptions-item label="操作人">{{ currentInstance.operator }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- DAG执行图 -->
        <div class="ins-detail-section">
          <div class="ins-detail-title">DAG执行图</div>
          <div class="ins-dag">
            <svg class="ins-dag-svg" viewBox="0 0 560 280">
              <!-- 连线 -->
              <path d="M 80 70 L 200 40" stroke="#00b42a" stroke-width="2" fill="none" />
              <path d="M 80 70 L 200 100" stroke="#00b42a" stroke-width="2" fill="none" />
              <path d="M 280 40 L 400 70" stroke="#00b42a" stroke-width="2" fill="none" />
              <path d="M 280 100 L 400 70" stroke="#00b42a" stroke-width="2" fill="none" />
              <path d="M 480 70 L 520 70" stroke="#1664ff" stroke-width="2" fill="none" stroke-dasharray="5 3">
                <animate attributeName="stroke-dashoffset" from="0" to="-16" dur="0.8s" repeatCount="indefinite" />
              </path>
              <!-- 节点 -->
              <g>
                <rect x="20" y="50" width="60" height="40" rx="6" fill="#fff" stroke="#00b42a" stroke-width="2" />
                <text x="50" y="75" text-anchor="middle" font-size="12" fill="#1d2129">开始</text>
              </g>
              <g>
                <rect x="200" y="20" width="80" height="40" rx="6" fill="#fff" stroke="#00b42a" stroke-width="2" />
                <text x="240" y="45" text-anchor="middle" font-size="12" fill="#1d2129">订单抽取</text>
              </g>
              <g>
                <rect x="200" y="80" width="80" height="40" rx="6" fill="#fff" stroke="#00b42a" stroke-width="2" />
                <text x="240" y="105" text-anchor="middle" font-size="12" fill="#1d2129">会员抽取</text>
              </g>
              <g>
                <rect x="400" y="50" width="80" height="40" rx="6" fill="#fff" stroke="#1664ff" stroke-width="2">
                  <animate attributeName="stroke-width" values="2;3;2" dur="1.5s" repeatCount="indefinite" />
                </rect>
                <text x="440" y="75" text-anchor="middle" font-size="12" fill="#1d2129">聚合计算</text>
              </g>
              <g>
                <rect x="520" y="50" width="40" height="40" rx="6" fill="#fff" stroke="#c9cdd4" stroke-width="2" stroke-dasharray="4 2" />
                <text x="540" y="75" text-anchor="middle" font-size="12" fill="#86909c">结束</text>
              </g>
            </svg>

            <!-- 节点状态列表 -->
            <div class="ins-dag-nodes">
              <div v-for="node in dagNodes" :key="node.name" class="ins-dag-node">
                <span class="ins-dag-node-dot" :class="node.status"></span>
                <span class="ins-dag-node-name">{{ node.name }}</span>
                <span class="ins-dag-node-time">{{ node.time }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 执行日志 -->
        <div class="ins-detail-section">
          <div class="ins-detail-title">
            执行日志
            <el-button text size="small" style="margin-left: auto" :icon="Download">下载日志</el-button>
          </div>
          <div class="ins-log-panel">
            <div v-for="(log, idx) in instanceLogs" :key="idx" class="ins-log-item" :class="log.type">
              <span class="ins-log-time">[{{ log.time }}]</span>
              <span class="ins-log-level">{{ log.level }}</span>
              <span class="ins-log-msg">{{ log.msg }}</span>
            </div>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { workflowInstances, owners } from '@/mock'
import { Download, Refresh, Search, VideoPlay } from '@element-plus/icons-vue'

const loading = ref(false)
const detailVisible = ref(false)
const currentInstance = ref(null)
const autoRefresh = ref(false)
let refreshTimer = null

const searchForm = reactive({
  keyword: '',
  status: '',
  type: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const statData = ref({
  total: 312,
  running: 12,
  successRate: 96.5,
  avgDuration: 18.6
})

const allData = ref([])
const tableData = ref([])

const dagNodes = ref([
  { name: '开始节点', status: 'success', time: '00:30:01' },
  { name: '订单数据抽取', status: 'success', time: '00:30:01 - 00:31:12' },
  { name: '会员数据抽取', status: 'success', time: '00:30:01 - 00:32:40' },
  { name: '数据清洗过滤', status: 'success', time: '00:32:45 - 00:34:20' },
  { name: '维度关联', status: 'success', time: '00:34:22 - 00:38:10' },
  { name: '聚合计算', status: 'running', time: '00:38:12 - 进行中' },
  { name: '写入宽表', status: 'pending', time: '等待中' },
  { name: '结束节点', status: 'pending', time: '等待中' }
])

const instanceLogs = ref([
  { time: '2026-09-26 00:30:01.234', level: 'INFO', type: 'info', msg: '工作流实例启动，实例ID: wf_20260926_0001' },
  { time: '2026-09-26 00:30:01.456', level: 'INFO', type: 'info', msg: '开始执行节点: 订单数据抽取' },
  { time: '2026-09-26 00:30:01.678', level: 'INFO', type: 'info', msg: '开始执行节点: 会员数据抽取' },
  { time: '2026-09-26 00:31:12.345', level: 'INFO', type: 'info', msg: '节点[订单数据抽取]执行完成，处理 1,248,300 条数据' },
  { time: '2026-09-26 00:32:40.567', level: 'INFO', type: 'info', msg: '节点[会员数据抽取]执行完成，处理 356,800 条数据' },
  { time: '2026-09-26 00:32:45.789', level: 'INFO', type: 'info', msg: '开始执行节点: 数据清洗过滤' },
  { time: '2026-09-26 00:34:20.123', level: 'INFO', type: 'info', msg: '节点[数据清洗过滤]执行完成，过滤脏数据 2,340 条' },
  { time: '2026-09-26 00:34:22.345', level: 'INFO', type: 'info', msg: '开始执行节点: 维度关联' },
  { time: '2026-09-26 00:38:10.567', level: 'INFO', type: 'info', msg: '节点[维度关联]执行完成' },
  { time: '2026-09-26 00:38:12.789', level: 'INFO', type: 'info', msg: '开始执行节点: 聚合计算' },
  { time: '2026-09-26 00:38:15.000', level: 'INFO', type: 'info', msg: '聚合计算进度: 35%，预计还需 2 分钟' }
])

function statusDotClass(status) {
  return {
    '运行中': 'running',
    '成功': 'success',
    '失败': 'failed',
    '排队': 'queued',
    '等待依赖': 'waiting',
    '已取消': 'cancelled'
  }[status] || ''
}

function triggerTagType(trigger) {
  return {
    '调度触发': 'primary',
    '手动触发': 'success',
    '补数运行': 'warning',
    '重跑': 'danger'
  }[trigger] || 'info'
}

function loadData() {
  loading.value = true
  setTimeout(() => {
    let list = allData.value.filter(item =>
      (!searchForm.keyword ||
        item.id.includes(searchForm.keyword) ||
        item.workflow.includes(searchForm.keyword)) &&
      (!searchForm.status || item.status === searchForm.status)
    )
    pagination.total = list.length
    const start = (pagination.page - 1) * pagination.size
    tableData.value = list.slice(start, start + pagination.size)
    loading.value = false
  }, 300)
}

function openDetail(row) {
  currentInstance.value = row
  detailVisible.value = true
}

function viewLog(row) {
  currentInstance.value = row
  detailVisible.value = true
  // 滚动到日志部分
  setTimeout(() => {
    const logPanel = document.querySelector('.ins-log-panel')
    if (logPanel) logPanel.scrollIntoView({ behavior: 'smooth' })
  }, 300)
}

function stopInstance(row) {
  ElMessageBox.confirm(`确定停止实例「${row.id}」吗？`, '确认停止', {
    type: 'warning',
    confirmButtonText: '停止',
    cancelButtonText: '取消'
  }).then(() => {
    row.status = '已取消'
    ElMessage.warning('实例已停止')
    loadData()
  }).catch(() => {})
}

function rerunInstance(row) {
  row.status = '排队'
  row.failedNodes = 0
  ElMessage.success(`实例「${row.id}」已提交重跑`)
  setTimeout(() => {
    row.status = '运行中'
  }, 1000)
  setTimeout(() => {
    row.status = '成功'
    ElMessage.success(`实例「${row.id}」重跑成功`)
    loadData()
  }, 3000)
}

function rerunFailed() {
  const failedCount = allData.value.filter(i => i.status === '失败').length
  if (!failedCount) {
    ElMessage.info('当前没有失败的实例')
    return
  }
  ElMessageBox.confirm(`确定重跑所有 ${failedCount} 个失败实例吗？`, '批量重跑', {
    type: 'warning',
    confirmButtonText: '确认重跑',
    cancelButtonText: '取消'
  }).then(() => {
    allData.value.forEach(i => {
      if (i.status === '失败') {
        i.status = '排队'
        i.failedNodes = 0
      }
    })
    ElMessage.success(`已提交 ${failedCount} 个失败实例重跑`)
    loadData()
  }).catch(() => {})
}

// 自动刷新
function startAutoRefresh() {
  if (refreshTimer) clearInterval(refreshTimer)
  refreshTimer = setInterval(() => {
    loadData()
  }, 30000)
}

function stopAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

// 监听自动刷新开关
// 使用 watch 或直接在模板中绑定

onMounted(() => {
  // 初始化数据，添加进度等字段
  allData.value = workflowInstances.map((item, idx) => ({
    ...item,
    operator: owners[idx % owners.length],
    progress: item.status === '成功' ? 100 : item.status === '失败' ? 68 : item.status === '运行中' ? 45 : 0,
    endTime: item.status === '成功' || item.status === '失败'
      ? item.start.split(' ')[0] + ' ' + String(parseInt(item.start.split(' ')[1].split(':')[0]) + 1).padStart(2, '0') + ':' + item.start.split(' ')[1].split(':')[1]
      : ''
  }))
  loadData()
})

onUnmounted(() => {
  stopAutoRefresh()
})
</script>

<style scoped>
.ins-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.ins-wf-name {
  font-weight: 500;
  color: var(--dp-text-1);
}

.ins-auto-refresh {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--dp-text-3);
}

.ins-auto-refresh .spinning {
  animation: ins-spin 1s linear infinite;
}

@keyframes ins-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 状态指示灯 */
.ins-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--dp-text-2);
}

.ins-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--dp-text-4);
  flex-shrink: 0;
}

.ins-status-dot.running {
  background: var(--dp-primary);
  box-shadow: 0 0 0 3px rgba(22, 100, 255, 0.15);
  animation: ins-pulse 2s infinite;
}

.ins-status-dot.success {
  background: var(--dp-success);
}

.ins-status-dot.failed {
  background: var(--dp-danger);
}

.ins-status-dot.queued {
  background: var(--dp-warning);
}

.ins-status-dot.waiting {
  background: var(--dp-purple);
}

.ins-status-dot.cancelled {
  background: var(--dp-text-4);
}

@keyframes ins-pulse {
  0%, 100% { box-shadow: 0 0 0 3px rgba(22, 100, 255, 0.15); }
  50% { box-shadow: 0 0 0 6px rgba(22, 100, 255, 0.08); }
}

/* 进度条 */
.ins-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ins-progress :deep(.el-progress) {
  flex: 1;
}

.ins-progress-text {
  font-size: 11px;
  color: var(--dp-text-3);
  white-space: nowrap;
}

/* 详情抽屉 */
.ins-detail-section {
  margin-bottom: 24px;
}

.ins-detail-title {
  display: flex;
  align-items: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 12px;
}

/* DAG图 */
.ins-dag {
  background: #fafbfc;
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  padding: 16px;
}

.ins-dag-svg {
  width: 100%;
  height: 180px;
  margin-bottom: 12px;
}

.ins-dag-nodes {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}

.ins-dag-node {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: 4px;
  font-size: 12px;
}

.ins-dag-node-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--dp-text-4);
  flex-shrink: 0;
}

.ins-dag-node-dot.success { background: var(--dp-success); }
.ins-dag-node-dot.running {
  background: var(--dp-primary);
  animation: ins-pulse 2s infinite;
}
.ins-dag-node-dot.pending { background: var(--dp-text-4); }
.ins-dag-node-dot.failed { background: var(--dp-danger); }

.ins-dag-node-name {
  flex: 1;
  color: var(--dp-text-2);
}

.ins-dag-node-time {
  font-size: 11px;
  color: var(--dp-text-3);
  font-family: 'JetBrains Mono', Consolas, monospace;
}

/* 日志面板 */
.ins-log-panel {
  background: #0d1117;
  border-radius: 6px;
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
  font-family: Consolas, monospace;
  font-size: 12px;
  line-height: 1.8;
}

.ins-log-item {
  display: flex;
  gap: 10px;
  color: #8b949e;
}

.ins-log-item.info { color: #8b949e; }
.ins-log-item.success { color: #3fb950; }
.ins-log-item.error { color: #f85149; }
.ins-log-item.warning { color: #d29922; }

.ins-log-time {
  flex-shrink: 0;
  color: #6e7681;
}

.ins-log-level {
  flex-shrink: 0;
  width: 60px;
  font-weight: 600;
}

.ins-log-msg {
  flex: 1;
  word-break: break-all;
}

@media (max-width: 1200px) {
  .ins-stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
