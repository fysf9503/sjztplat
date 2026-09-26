<template>
  <div class="dp-page">
    <PageHeader title="接入任务管理" desc="管理数据接入同步任务，支持全量、增量、实时多种同步模式">
      <el-button type="primary" :icon="Plus" @click="openEdit()">新建任务</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索任务名称"
          clearable
          style="width: 260px"
          :prefix-icon="Search"
          @input="handleSearch"
        />
        <el-select v-model="query.mode" placeholder="任务类型" clearable style="width: 130px" @change="handleSearch">
          <el-option label="全量同步" value="全量" />
          <el-option label="增量同步" value="增量" />
          <el-option label="实时同步" value="实时" />
        </el-select>
        <el-select v-model="query.status" placeholder="运行状态" clearable style="width: 130px" @change="handleSearch">
          <el-option label="运行中" value="运行中" />
          <el-option label="已停止" value="已停止" />
          <el-option label="异常" value="异常" />
          <el-option label="已完成" value="已完成" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        </div>
      </div>

      <el-table :data="pagedList" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="name" label="任务名称" min-width="200">
          <template #default="{ row }">
            <div class="task-name-cell">
              <div class="task-icon" :class="row.mode.toLowerCase()">
                <el-icon :size="16"><component :is="getModeIcon(row.mode)" /></el-icon>
              </div>
              <div>
                <div class="task-name">{{ row.name }}</div>
                <div class="task-sub">{{ row.source }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="mode" label="任务类型" width="100">
          <template #default="{ row }">
            <span class="task-mode-tag" :class="row.mode.toLowerCase()">{{ row.mode }}同步</span>
          </template>
        </el-table-column>
        <el-table-column label="源数据源" min-width="160" class-name="dp-mono">
          <template #default="{ row }">
            <span class="task-source">{{ row.source }}</span>
          </template>
        </el-table-column>
        <el-table-column label="目标数据源" min-width="160" class-name="dp-mono">
          <template #default="{ row }">
            <span class="task-target">{{ row.target }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="tables" label="同步表数" width="100" align="center">
          <template #default="{ row }">
            <span class="task-tables">{{ row.tables }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="freq" label="调度策略" width="130">
          <template #default="{ row }">
            <span class="task-freq">
              <el-icon :size="12"><Timer /></el-icon>
              {{ row.freq }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="task-status">
              <span class="task-status-dot" :class="statusDotClass(row.status)"></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="lastRun" label="最近运行时间" width="160" />
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" :icon="VideoPlay" @click="runNow(row)">立即执行</el-button>
            <el-button link type="primary" size="small" :icon="Document" @click="viewLogs(row)">日志</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === '运行中' ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === '运行中' ? '停止' : '启动' }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="filteredList.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑接入任务' : '新建接入任务'" width="780px" :close-on-click-modal="false" class="task-dialog">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="14">
            <el-form-item label="任务名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入任务名称" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="任务类型" prop="mode">
              <el-radio-group v-model="form.mode">
                <el-radio-button value="全量">全量</el-radio-button>
                <el-radio-button value="增量">增量</el-radio-button>
                <el-radio-button value="实时">实时</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">源端配置</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="源数据源" prop="sourceDs">
              <el-select v-model="form.sourceDs" placeholder="请选择源数据源" style="width: 100%" filterable>
                <el-option v-for="ds in dsOptions" :key="ds.id" :label="ds.name" :value="ds.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="源表选择" prop="sourceTables">
              <el-select v-model="form.sourceTables" multiple collapse-tags collapse-tags-tooltip placeholder="选择同步的表" style="width: 100%" filterable>
                <el-option v-for="t in sourceTableOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">目标端配置</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="目标数据源" prop="targetDs">
              <el-select v-model="form.targetDs" placeholder="请选择目标数据源" style="width: 100%" filterable>
                <el-option v-for="ds in dsOptions" :key="ds.id" :label="ds.name" :value="ds.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标库表" prop="targetTable">
              <el-input v-model="form.targetTable" placeholder="目标表名前缀" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">同步策略</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="同步方式" prop="syncType">
              <el-select v-model="form.syncType" style="width: 100%">
                <el-option label="一次性执行" value="once" />
                <el-option label="定时调度" value="schedule" />
                <el-option label="Cron表达式" value="cron" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item v-if="form.syncType === 'cron'" label="Cron表达式" prop="cronExpr">
              <el-input v-model="form.cronExpr" placeholder="例如：0 0 2 * * ?" />
            </el-form-item>
            <el-form-item v-else-if="form.syncType === 'schedule'" label="执行周期" prop="scheduleFreq">
              <el-radio-group v-model="form.scheduleFreq">
                <el-radio value="hourly">每小时</el-radio>
                <el-radio value="daily">每天</el-radio>
                <el-radio value="weekly">每周</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-else label="执行时间">
              <span class="form-tip">任务创建后立即执行一次</span>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">字段映射</el-divider>
        <div class="field-mapping">
          <el-table :data="fieldMapping" size="small" border>
            <el-table-column prop="sourceField" label="源字段" width="160">
              <template #default="{ row }">
                <el-input v-model="row.sourceField" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="映射关系" width="60" align="center">
              <template #default>
                <el-icon color="var(--dp-primary)"><Right /></el-icon>
              </template>
            </el-table-column>
            <el-table-column prop="targetField" label="目标字段" width="160">
              <template #default="{ row }">
                <el-input v-model="row.targetField" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small">
                  <el-option label="VARCHAR(64)" value="VARCHAR(64)" />
                  <el-option label="INT" value="INT" />
                  <el-option label="DECIMAL(16,2)" value="DECIMAL(16,2)" />
                  <el-option label="DATETIME" value="DATETIME" />
                  <el-option label="TEXT" value="TEXT" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="60" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" size="small" @click="removeField($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button class="add-field-btn" :icon="Plus" size="small" @click="addField">添加字段映射</el-button>
        </div>

        <el-divider content-position="left">高级配置</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="并发数">
              <el-input-number v-model="form.concurrency" :min="1" :max="32" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="错误阈值(%)">
              <el-input-number v-model="form.errorThreshold" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="批次大小">
              <el-input-number v-model="form.batchSize" :min="100" :max="50000" :step="500" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="负责人" prop="owner">
          <el-select v-model="form.owner" placeholder="请选择负责人" filterable style="width: 240px">
            <el-option v-for="o in ownerList" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 执行日志弹窗 -->
    <el-dialog v-model="logDialogVisible" title="执行日志" width="800px">
      <div class="log-toolbar">
        <span class="log-task-name">{{ currentLogTask }}</span>
        <el-select v-model="logFilter" size="small" style="width: 120px">
          <el-option label="全部日志" value="all" />
          <el-option label="仅错误" value="error" />
          <el-option label="仅警告" value="warn" />
        </el-select>
      </div>
      <div class="log-list">
        <div v-for="(log, idx) in filteredLogs" :key="idx" class="log-item" :class="log.level">
          <span class="log-time">{{ log.time }}</span>
          <span class="log-level">{{ log.level.toUpperCase() }}</span>
          <span class="log-msg">{{ log.message }}</span>
        </div>
        <div v-if="filteredLogs.length === 0" class="log-empty">暂无日志记录</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, VideoPlay, Document, Timer, Check, Right } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { accessTasks, dataSources, owners } from '@/mock'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const logDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const currentLogTask = ref('')
const logFilter = ref('all')
const ownerList = owners
const dsOptions = dataSources.slice(0, 8)

const sourceTableOptions = [
  'customer_info', 'order_detail', 'product_sku', 'trade_flow',
  'user_behavior', 'payment_record', 'refund_order', 'member_profile'
]

const query = reactive({
  keyword: '',
  mode: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10
})

const form = reactive({
  id: null,
  name: '',
  mode: '全量',
  sourceDs: '',
  sourceTables: [],
  targetDs: '',
  targetTable: '',
  syncType: 'schedule',
  cronExpr: '',
  scheduleFreq: 'daily',
  concurrency: 4,
  errorThreshold: 5,
  batchSize: 1000,
  owner: ''
})

const formRules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  mode: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  sourceDs: [{ required: true, message: '请选择源数据源', trigger: 'change' }],
  sourceTables: [{ required: true, message: '请至少选择一张源表', trigger: 'change' }],
  targetDs: [{ required: true, message: '请选择目标数据源', trigger: 'change' }],
  owner: [{ required: true, message: '请选择负责人', trigger: 'change' }]
}

const fieldMapping = ref([
  { sourceField: 'id', targetField: 'id', type: 'INT' },
  { sourceField: 'name', targetField: 'name', type: 'VARCHAR(64)' },
  { sourceField: 'create_time', targetField: 'create_time', type: 'DATETIME' }
])

function normalizeTask(t) {
  const statusMap = { '成功': '已完成', '失败': '异常' }
  return {
    ...t,
    tables: Math.floor(Math.random() * 18) + 1,
    status: statusMap[t.status] || t.status
  }
}

const dataList = ref(accessTasks.map(normalizeTask))

const filteredList = computed(() => {
  return dataList.value.filter(item => {
    if (query.keyword && !item.name.toLowerCase().includes(query.keyword.toLowerCase())) return false
    if (query.mode && item.mode !== query.mode) return false
    if (query.status && item.status !== query.status) return false
    return true
  })
})

const pagedList = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filteredList.value.slice(start, start + pagination.size)
})

const logs = ref([
  { time: '2026-09-26 10:30:01', level: 'info', message: '任务开始执行，同步模式：全量同步' },
  { time: '2026-09-26 10:30:02', level: 'info', message: '连接源数据源成功' },
  { time: '2026-09-26 10:30:03', level: 'info', message: '连接目标数据源成功' },
  { time: '2026-09-26 10:30:05', level: 'info', message: '开始同步表：customer_info' },
  { time: '2026-09-26 10:30:12', level: 'info', message: '已同步 10,000 条记录，进度 25%' },
  { time: '2026-09-26 10:30:20', level: 'warn', message: '字段 phone 存在空值，已跳过 3 条记录' },
  { time: '2026-09-26 10:30:25', level: 'info', message: '已同步 30,000 条记录，进度 75%' },
  { time: '2026-09-26 10:30:30', level: 'info', message: '表 customer_info 同步完成，共 40,256 条记录' },
  { time: '2026-09-26 10:30:31', level: 'info', message: '开始同步表：order_detail' },
  { time: '2026-09-26 10:30:45', level: 'error', message: '表 order_detail 同步失败：主键冲突，错误码 1062' },
  { time: '2026-09-26 10:30:46', level: 'info', message: '任务执行结束，成功 1 表，失败 1 表' }
])

const filteredLogs = computed(() => {
  if (logFilter.value === 'all') return logs.value
  return logs.value.filter(l => l.level === logFilter.value)
})

function getModeIcon(mode) {
  const map = { '全量': 'Download', '增量': 'TopRight', '实时': 'Lightning' }
  return map[mode] || 'Download'
}

function statusDotClass(status) {
  const map = {
    '运行中': 'running',
    '已停止': 'stopped',
    '异常': 'error',
    '已完成': 'success',
    '等待调度': 'waiting'
  }
  return map[status] || 'waiting'
}

function int(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

function handleSearch() {
  pagination.page = 1
}

function handleRefresh() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('数据已刷新')
  }, 600)
}

function openEdit(row) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      mode: row.mode,
      sourceDs: row.source.split('.')[0],
      sourceTables: [row.source.split('.')[1]],
      targetDs: row.target.split('.')[0],
      targetTable: row.target.split('.')[1],
      syncType: 'schedule',
      cronExpr: '0 0 2 * * ?',
      scheduleFreq: 'daily',
      concurrency: 4,
      errorThreshold: 5,
      batchSize: 1000,
      owner: row.owner
    })
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      mode: '全量',
      sourceDs: '',
      sourceTables: [],
      targetDs: '',
      targetTable: '',
      syncType: 'schedule',
      cronExpr: '',
      scheduleFreq: 'daily',
      concurrency: 4,
      errorThreshold: 5,
      batchSize: 1000,
      owner: ''
    })
  }
  dialogVisible.value = true
}

function addField() {
  fieldMapping.value.push({ sourceField: '', targetField: '', type: 'VARCHAR(64)' })
}

function removeField(index) {
  fieldMapping.value.splice(index, 1)
}

function handleSave() {
  formRef.value.validate(valid => {
    if (!valid) return
    saving.value = true
    setTimeout(() => {
      saving.value = false
      dialogVisible.value = false
      if (isEdit.value) {
        const idx = dataList.value.findIndex(item => item.id === form.id)
        if (idx > -1) {
          dataList.value[idx] = {
            ...dataList.value[idx],
            name: form.name,
            mode: form.mode,
            source: `${form.sourceDs}.${form.sourceTables[0] || ''}`,
            target: `${form.targetDs}.${form.targetTable}`,
            owner: form.owner,
            lastRun: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
          }
        }
        ElMessage.success('任务更新成功')
      } else {
        dataList.value.unshift({
          id: Date.now(),
          name: form.name,
          mode: form.mode,
          source: `${form.sourceDs}.${form.sourceTables[0] || ''}`,
          target: `${form.targetDs}.${form.targetTable}`,
          freq: form.syncType === 'cron' ? form.cronExpr : form.scheduleFreq === 'hourly' ? '每小时' : form.scheduleFreq === 'daily' ? '每天 00:30' : '每周一 02:00',
          status: '等待调度',
          tables: form.sourceTables.length,
          lastRun: '-',
          duration: '-',
          rows: 0,
          owner: form.owner
        })
        ElMessage.success('任务创建成功')
      }
    }, 800)
  })
}

function runNow(row) {
  ElMessage.info(`任务 ${row.name} 已开始执行`)
  const idx = dataList.value.findIndex(item => item.id === row.id)
  if (idx > -1) {
    dataList.value[idx].status = '运行中'
    dataList.value[idx].lastRun = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
  }
}

function viewLogs(row) {
  currentLogTask.value = row.name
  logDialogVisible.value = true
}

function toggleStatus(row) {
  const isRunning = row.status === '运行中'
  const action = isRunning ? '停止' : '启动'
  ElMessageBox.confirm(
    `确定要${action}任务 "${row.name}" 吗？`,
    `${action}确认`,
    {
      confirmButtonText: `确定${action}`,
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    const idx = dataList.value.findIndex(item => item.id === row.id)
    if (idx > -1) {
      dataList.value[idx].status = isRunning ? '已停止' : '运行中'
    }
    ElMessage.success(`任务已${action}`)
  }).catch(() => {})
}

function handleDelete(row) {
  ElMessageBox.confirm(
    `确定要删除任务 "${row.name}" 吗？删除后任务配置将无法恢复。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(() => {
    const idx = dataList.value.findIndex(item => item.id === row.id)
    if (idx > -1) {
      dataList.value.splice(idx, 1)
    }
    ElMessage.success('删除成功')
  }).catch(() => {})
}

onMounted(() => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 300)
})
</script>

<style scoped>
.task-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.task-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.task-icon.全量 { background: #e8f0ff; color: #1664ff; }
.task-icon.增量 { background: #e8ffea; color: #00b42a; }
.task-icon.实时 { background: #fff3e8; color: #ff7d00; }

.task-name {
  font-weight: 500;
  color: var(--dp-text-1);
  font-size: 13px;
}
.task-sub {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

.task-mode-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 4px;
  font-weight: 500;
}
.task-mode-tag.全量 { background: #e8f0ff; color: #1664ff; }
.task-mode-tag.增量 { background: #e8ffea; color: #00b42a; }
.task-mode-tag.实时 { background: #fff3e8; color: #ff7d00; }

.task-source, .task-target {
  font-size: 12px;
  color: var(--dp-text-2);
}

.task-tables {
  font-family: 'JetBrains Mono', Consolas, monospace;
  color: var(--dp-text-2);
}

.task-freq {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--dp-text-2);
}

.task-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.task-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.task-status-dot.running {
  background: var(--dp-primary);
  box-shadow: 0 0 0 3px rgba(22, 100, 255, 0.15);
  animation: pulse 2s infinite;
}
.task-status-dot.success {
  background: var(--dp-success);
  box-shadow: 0 0 0 3px rgba(0, 180, 42, 0.15);
}
.task-status-dot.error {
  background: var(--dp-danger);
  box-shadow: 0 0 0 3px rgba(245, 63, 63, 0.15);
}
.task-status-dot.stopped {
  background: var(--dp-text-4);
  box-shadow: 0 0 0 3px rgba(201, 205, 212, 0.15);
}
.task-status-dot.waiting {
  background: var(--dp-warning);
  box-shadow: 0 0 0 3px rgba(255, 125, 0, 0.15);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.field-mapping {
  margin-bottom: 16px;
}
.add-field-btn {
  margin-top: 10px;
  width: 100%;
}

.form-tip {
  font-size: 12px;
  color: var(--dp-text-3);
}

.log-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--dp-border-light);
}
.log-task-name {
  font-weight: 500;
  color: var(--dp-text-1);
}
.log-list {
  max-height: 400px;
  overflow-y: auto;
  background: #f7f8fa;
  border-radius: 6px;
  padding: 12px;
}
.log-item {
  display: flex;
  gap: 10px;
  padding: 6px 8px;
  font-size: 12px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  border-radius: 4px;
  margin-bottom: 2px;
}
.log-item:hover {
  background: rgba(0, 0, 0, 0.03);
}
.log-time {
  color: var(--dp-text-3);
  flex-shrink: 0;
}
.log-level {
  font-weight: 600;
  flex-shrink: 0;
  width: 50px;
}
.log-item.info .log-level { color: #1664ff; }
.log-item.warn .log-level { color: #ff7d00; }
.log-item.error .log-level { color: #f53f3f; }
.log-msg {
  color: var(--dp-text-2);
  word-break: break-all;
}
.log-empty {
  text-align: center;
  color: var(--dp-text-3);
  padding: 40px 0;
}
</style>
