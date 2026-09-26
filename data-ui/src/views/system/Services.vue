<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">SERVICE MANAGEMENT</div>
      <h1 class="dp-page-title">服务管理</h1>
      <p class="dp-page-desc">管理平台各微服务的运行状态、配置和健康检查。</p>
    </div>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid">
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>服务总数</span>
          <div class="dp-stat-icon" style="background: var(--dp-primary-light); color: var(--dp-primary)">
            <el-icon :size="18"><Cpu /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ serviceStats.total }}<span class="dp-stat-unit">个</span></div>
        <div class="dp-stat-sub">
          <span class="dp-stat-trend up"><el-icon><Top /></el-icon> 2</span>
          <span>较上周</span>
        </div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>运行中</span>
          <div class="dp-stat-icon" style="background: var(--dp-success-light); color: var(--dp-success)">
            <el-icon :size="18"><CircleCheck /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ serviceStats.running }}<span class="dp-stat-unit">个</span></div>
        <div class="dp-stat-sub">
          <span class="dp-tag success">健康率 {{ healthRate }}%</span>
        </div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>异常</span>
          <div class="dp-stat-icon" style="background: var(--dp-danger-light); color: var(--dp-danger)">
            <el-icon :size="18"><Warning /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ serviceStats.abnormal }}<span class="dp-stat-unit">个</span></div>
        <div class="dp-stat-sub">
          <span class="dp-tag danger">需关注</span>
        </div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>CPU 使用率</span>
          <div class="dp-stat-icon" style="background: var(--dp-warning-light); color: var(--dp-warning)">
            <el-icon :size="18"><Odometer /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ serviceStats.cpuUsage }}<span class="dp-stat-unit">%</span></div>
        <div class="dp-stat-sub">
          <el-progress :percentage="serviceStats.cpuUsage" :stroke-width="6" :show-text="false" style="width: 120px" />
        </div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>内存使用率</span>
          <div class="dp-stat-icon" style="background: var(--dp-purple-light); color: var(--dp-purple)">
            <el-icon :size="18"><Coin /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ serviceStats.memUsage }}<span class="dp-stat-unit">%</span></div>
        <div class="dp-stat-sub">
          <el-progress :percentage="serviceStats.memUsage" :stroke-width="6" :show-text="false" status="warning" style="width: 120px" />
        </div>
      </div>
    </div>

    <!-- 服务列表 -->
    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索服务名称" clearable style="width: 240px" :prefix-icon="Search" @input="load" />
        <el-select v-model="query.type" placeholder="服务类型" clearable style="width: 140px" @change="load">
          <el-option label="网关服务" value="网关" />
          <el-option label="认证服务" value="认证" />
          <el-option label="业务服务" value="业务" />
          <el-option label="基础服务" value="基础" />
        </el-select>
        <el-select v-model="query.status" placeholder="运行状态" clearable style="width: 120px" @change="load">
          <el-option label="运行中" value="运行中" />
          <el-option label="已停止" value="已停止" />
          <el-option label="异常" value="异常" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="load">刷新</el-button>
          <el-button type="primary" :icon="Plus">新增服务</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="name" label="服务名称" min-width="220" class-name="dp-mono" show-overflow-tooltip />
        <el-table-column prop="type" label="服务类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="instances" label="实例数" width="90" sortable />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              <el-icon style="margin-right: 3px"><component :is="statusIcon(row.status)" /></el-icon>
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="CPU" width="130">
          <template #default="{ row }">
            <div class="dp-flex dp-gap-8">
              <el-progress :percentage="row.cpu" :stroke-width="6" :show-text="false" :status="row.cpu > 80 ? 'exception' : row.cpu > 60 ? 'warning' : ''" style="width: 70px" />
              <span style="font-size: 12px; color: var(--dp-text-2)">{{ row.cpu }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="内存" width="130">
          <template #default="{ row }">
            <div class="dp-flex dp-gap-8">
              <el-progress :percentage="row.memory" :stroke-width="6" :show-text="false" :status="row.memory > 85 ? 'exception' : row.memory > 70 ? 'warning' : ''" style="width: 70px" />
              <span style="font-size: 12px; color: var(--dp-text-2)">{{ row.memory }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="启动时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="viewLogs(row)">日志</el-button>
            <el-button link :type="row.status === '运行中' ? 'warning' : 'success'" size="small" @click="restart(row)">
              {{ row.status === '运行中' ? '重启' : '启动' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <!-- 服务详情抽屉 -->
    <el-drawer v-model="detailVisible" title="服务详情" size="500px">
      <el-descriptions v-if="currentService" :column="1" border size="small">
        <el-descriptions-item label="服务名称">{{ currentService.name }}</el-descriptions-item>
        <el-descriptions-item label="服务类型">{{ currentService.type }}</el-descriptions-item>
        <el-descriptions-item label="实例数">{{ currentService.instances }}</el-descriptions-item>
        <el-descriptions-item label="运行状态">
          <el-tag :type="statusTagType(currentService.status)" size="small">{{ currentService.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="CPU 使用率">{{ currentService.cpu }}%</el-descriptions-item>
        <el-descriptions-item label="内存使用率">{{ currentService.memory }}%</el-descriptions-item>
        <el-descriptions-item label="启动时间">{{ currentService.startTime }}</el-descriptions-item>
        <el-descriptions-item label="服务端口">{{ currentService.port }}</el-descriptions-item>
        <el-descriptions-item label="版本号">{{ currentService.version }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ currentService.owner }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <!-- 日志查看对话框 -->
    <el-dialog v-model="logsVisible" title="服务日志" width="700px" top="8vh">
      <div class="log-toolbar dp-flex dp-gap-8 dp-mb-12">
        <el-select model-value="info" style="width: 120px" size="small">
          <el-option label="全部级别" value="all" />
          <el-option label="INFO" value="info" />
          <el-option label="WARN" value="warn" />
          <el-option label="ERROR" value="error" />
        </el-select>
        <el-input placeholder="搜索关键词:" size="small" style="width: 200px" />
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" size="small">刷新</el-button>
        </div>
      </div>
      <div class="log-content">
        <div v-for="(log, idx) in logLines" :key="idx" class="log-line">
          <span class="log-time">[{{ log.time }}]</span>
          <span class="log-level" :class="'log-' + log.level">{{ log.level.toUpperCase() }}</span>
          <span class="log-msg">{{ log.msg }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

// 模拟服务数据
const serviceList = [
  { id: 1, name: 'data-gateway', type: '网关', instances: 3, status: '运行中', cpu: 45, memory: 62, startTime: '2026-09-20 08:30:00', port: '8080', version: 'v2.3.1', owner: '运维保障部' },
  { id: 2, name: 'data-auth', type: '认证', instances: 2, status: '运行中', cpu: 28, memory: 45, startTime: '2026-09-20 08:30:15', port: '8081', version: 'v2.3.1', owner: '运维保障部' },
  { id: 3, name: 'system-service', type: '基础', instances: 2, status: '运行中', cpu: 35, memory: 52, startTime: '2026-09-20 08:30:30', port: '8082', version: 'v2.3.0', owner: '数据开发部' },
  { id: 4, name: 'data-integration-service', type: '业务', instances: 4, status: '运行中', cpu: 62, memory: 71, startTime: '2026-09-20 08:31:00', port: '8083', version: 'v2.3.1', owner: '数据开发部' },
  { id: 5, name: 'data-quality-service', type: '业务', instances: 2, status: '运行中', cpu: 38, memory: 48, startTime: '2026-09-20 08:31:20', port: '8084', version: 'v2.2.5', owner: '数据治理部' },
  { id: 6, name: 'data-governance-service', type: '业务', instances: 2, status: '运行中', cpu: 42, memory: 55, startTime: '2026-09-20 08:31:40', port: '8085', version: 'v2.3.0', owner: '数据治理部' },
  { id: 7, name: 'data-lineage-service', type: '业务', instances: 1, status: '异常', cpu: 92, memory: 88, startTime: '2026-09-22 14:20:00', port: '8086', version: 'v2.3.0', owner: '数据开发部' },
  { id: 8, name: 'data-realtime-service', type: '业务', instances: 3, status: '运行中', cpu: 71, memory: 76, startTime: '2026-09-20 08:32:00', port: '8087', version: 'v2.3.1', owner: '数据开发部' },
  { id: 9, name: 'data-modeling-service', type: '业务', instances: 2, status: '运行中', cpu: 33, memory: 47, startTime: '2026-09-20 08:32:20', port: '8088', version: 'v2.2.8', owner: '数据开发部' },
  { id: 10, name: 'data-service-gateway', type: '网关', instances: 2, status: '运行中', cpu: 55, memory: 60, startTime: '2026-09-20 08:32:40', port: '8089', version: 'v2.3.0', owner: 'AI应用部' },
  { id: 11, name: 'ai-agent-service', type: '业务', instances: 2, status: '运行中', cpu: 48, memory: 65, startTime: '2026-09-21 10:00:00', port: '8090', version: 'v1.2.0', owner: 'AI应用部' },
  { id: 12, name: 'file-service', type: '基础', instances: 1, status: '已停止', cpu: 0, memory: 0, startTime: '-', port: '8091', version: 'v2.3.0', owner: '运维保障部' },
]

const all = ref([...serviceList])
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', type: '', status: '', page: 1, size: 10 })

const serviceStats = computed(() => {
  const list = all.value
  return {
    total: list.length,
    running: list.filter(s => s.status === '运行中').length,
    abnormal: list.filter(s => s.status === '异常' || s.status === '已停止').length,
    cpuUsage: Math.round(list.filter(s => s.status === '运行中').reduce((acc, s) => acc + s.cpu, 0) / list.filter(s => s.status === '运行中').length),
    memUsage: Math.round(list.filter(s => s.status === '运行中').reduce((acc, s) => acc + s.memory, 0) / list.filter(s => s.status === '运行中').length)
  }
})

const healthRate = computed(() => {
  if (serviceStats.value.total === 0) return 0
  return Math.round(serviceStats.value.running / serviceStats.value.total * 100)
})

const typeTagType = (t) => ({ 网关: 'primary', 认证: 'success', 业务: 'warning', 基础: 'info' }[t] || 'info')
const statusTagType = (s) => ({ '运行中': 'success', '已停止': 'info', 异常: 'danger' }[s] || 'info')
const statusIcon = (s) => ({ '运行中': 'CircleCheck', '已停止': 'VideoPause', 异常: 'WarningFilled' }[s] || 'InfoFilled')

function load() {
  let list = all.value.filter(r =>
    (!query.keyword || r.name.includes(query.keyword)) &&
    (!query.type || r.type === query.type) &&
    (!query.status || r.status === query.status)
  )
  total.value = list.length
  rows.value = list.slice((query.page - 1) * query.size, query.page * query.size)
}

// 详情
const detailVisible = ref(false)
const currentService = ref(null)
function viewDetail(row) {
  currentService.value = row
  detailVisible.value = true
}

// 日志
const logsVisible = ref(false)
const logLines = ref([])
function viewLogs(row) {
  currentService.value = row
  logLines.value = generateLogs(row.name)
  logsVisible.value = true
}

function generateLogs(serviceName) {
  const levels = ['info', 'info', 'info', 'warn', 'info', 'error', 'info']
  const msgs = [
    `[${serviceName}] 服务启动完成，监听端口 8080`,
    `[${serviceName}] 连接数据库成功`,
    `[${serviceName}] 处理请求 /api/v1/data/list`,
    `[${serviceName}] 慢查询警告：SQL 执行耗时 2.3s`,
    `[${serviceName}] 定时任务执行完成`,
    `[${serviceName}] 连接池异常：连接数已达上限`,
    `[${serviceName}] 健康检查通过`,
    `[${serviceName}] 缓存命中率  95.2%`,
    `[${serviceName}] 接收请求 1000 条，成功 998 条`,
    `[${serviceName}] 配置热加载成功`,
  ]
  const lines = []
  for (let i = 0; i < 15; i++) {
    const level = levels[i % levels.length]
    const msg = msgs[i % msgs.length]
    const d = new Date(2026, 8, 26, 10 + Math.floor(i / 5), (i * 7) % 60, (i * 13) % 60)
    lines.push({
      time: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`,
      level,
      msg
    })
  }
  return lines
}

// 重启
function restart(row) {
  const action = row.status === '运行中' ? '重启' : '启动'
  ElMessageBox.confirm(`确定${action}服务「${row.name}」吗？`, `${action}确认`, { type: 'warning' })
    .then(() => {
      ElMessage.success(`服务「${row.name}」正「${action}...`)
      row.status = '运行中'
      row.cpu = Math.floor(Math.random() * 40) + 20
      row.memory = Math.floor(Math.random() * 30) + 40
      load()
    })
    .catch(() => {})
}

load()
</script>

<style scoped>
.log-toolbar {
  padding: 10px 0;
  border-bottom: 1px solid var(--dp-border-light);
}
.log-content {
  background: #1e1e1e;
  border-radius: 6px;
  padding: 12px;
  max-height: 400px;
  overflow-y: auto;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  line-height: 1.8;
}
.log-line {
  display: flex;
  gap: 10px;
  color: #d4d4d4;
}
.log-time { color: #6a9955; flex-shrink: 0; }
.log-level {
  flex-shrink: 0;
  font-weight: 600;
  width: 50px;
}
.log-info { color: #4fc1ff; }
.log-warn { color: #ce9178; }
.log-error { color: #f53f3f; }
.log-msg { color: #d4d4d4; word-break: break-all; }
</style>
