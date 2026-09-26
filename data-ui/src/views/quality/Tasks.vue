<template>
  <div class="dp-page">
    <PageHeader title="质量任务调度" desc="质检任务的调度运行与手动运维：补数、重跑、任务详情查看。">
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索任务名称 / 策略名称" clearable style="width: 240px" :prefix-icon="Search" @input="loadData" />
        <el-select v-model="query.status" placeholder="执行状态" clearable style="width: 130px" @change="loadData">
          <el-option label="运行中" value="运行中" />
          <el-option label="成功" value="成功" />
          <el-option label="失败" value="失败" />
          <el-option label="部分成功" value="部分成功" />
        </el-select>
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          @change="loadData"
        />
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="name" label="任务名称" min-width="180" class-name="dp-mono" show-overflow-tooltip />
        <el-table-column prop="strategy" label="关联策略" min-width="160" show-overflow-tooltip />
        <el-table-column prop="trigger" label="触发方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.trigger === '定时' ? 'primary' : 'success'" size="small" effect="plain">{{ row.trigger }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="执行状态" width="110">
          <template #default="{ row }">
            <span class="status-dot" :class="row.status">
              <i></i>{{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160">
          <template #default="{ row }">
            {{ row.status === '运行中' ? '-' : row.endTime }}
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="耗时" width="90" align="center">
          <template #default="{ row }">
            {{ row.status === '运行中' ? '执行中...' : row.duration }}
          </template>
        </el-table-column>
        <el-table-column prop="passRate" label="执行结果" width="130">
          <template #default="{ row }">
            <template v-if="row.status === '运行中'">
              <el-progress :percentage="row.progress" :stroke-width="5" status="success" />
            </template>
            <template v-else>
              <el-progress :percentage="row.passRate" :stroke-width="5" :color="getPassRateColor(row.passRate)" />
            </template>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="80" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">查看详情</el-button>
            <el-button link type="primary" size="small" @click="viewReport(row)">查看报告</el-button>
            <el-button v-if="row.status === '运行中'" link type="danger" size="small" @click="stopTask(row)">停止</el-button>
            <el-button v-else link type="warning" size="small" @click="rerunTask(row)">重新执行</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>

    <!-- 任务详情抽屉 -->
    <el-drawer v-model="detailVisible" title="任务详情" size="600px" destroy-on-close>
      <template v-if="currentTask">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="任务名称" :span="2">
              <span class="dp-mono">{{ currentTask.name }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="关联策略">{{ currentTask.strategy }}</el-descriptions-item>
            <el-descriptions-item label="触发方式">
              <el-tag :type="currentTask.trigger === '定时' ? 'primary' : 'success'" size="small" effect="plain">{{ currentTask.trigger }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="执行状态">
              <span class="status-dot" :class="currentTask.status">
                <i></i>{{ currentTask.status }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="操作人">{{ currentTask.operator }}</el-descriptions-item>
            <el-descriptions-item label="开始时间" :span="2">{{ currentTask.startTime }}</el-descriptions-item>
            <el-descriptions-item label="结束时间" :span="2">
              {{ currentTask.status === '运行中' ? '执行中...' : currentTask.endTime }}
            </el-descriptions-item>
            <el-descriptions-item label="总耗时">
              {{ currentTask.status === '运行中' ? '执行中...' : currentTask.duration }}
            </el-descriptions-item>
            <el-descriptions-item label="合格率">
              <span :style="{ color: getPassRateColor(currentTask.passRate), fontWeight: 600 }">{{ currentTask.passRate }}%</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 规则执行进度 -->
        <div class="detail-section">
          <div class="section-title">规则执行进度</div>
          <div class="rule-progress-list">
            <div v-for="(rule, idx) in ruleProgress" :key="idx" class="rule-progress-item">
              <div class="rule-progress-header">
                <span class="rule-name">{{ rule.name }}</span>
                <span class="rule-status" :class="rule.status">
                  <i></i>{{ rule.status }}
                </span>
              </div>
              <div class="rule-progress-body">
                <el-progress
                  :percentage="rule.status === '执行中' ? rule.progress : rule.passRate"
                  :stroke-width="6"
                  :color="rule.status === '失败' ? '#f53f3f' : rule.status === '执行中' ? '#1664ff' : getPassRateColor(rule.passRate)"
                />
              </div>
              <div class="rule-progress-footer">
                <span>通过率: {{ rule.passRate }}%</span>
                <span>耗时: {{ rule.duration }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 执行日志 -->
        <div class="detail-section">
          <div class="section-title">
            执行日志
            <el-button link type="primary" size="small" style="margin-left: auto">下载日志</el-button>
          </div>
          <div class="log-container">
            <div v-for="(log, idx) in execLogs" :key="idx" class="log-line">
              <span class="log-time">{{ log.time }}</span>
              <span class="log-level" :class="log.level">[{{ log.level }}]</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const detailVisible = ref(false)
const currentTask = ref(null)
const ruleProgress = ref([])
const execLogs = ref([])

const query = reactive({
  keyword: '',
  status: '',
  dateRange: null,
  page: 1,
  size: 10
})

const total = ref(0)
const tableData = ref([])

// Mock 数据
const allData = ref([
  { id: 1, name: 'quality_job_001', strategy: '交易数据质量策略', trigger: '定时', status: '成功', startTime: '2026-09-26 01:00:00', endTime: '2026-09-26 01:23:45', duration: '23m45s', passRate: 97.2, progress: 100, operator: '系统' },
  { id: 2, name: 'quality_job_002', strategy: '客户数据质量策略', trigger: '定时', status: '成功', startTime: '2026-09-26 02:00:00', endTime: '2026-09-26 02:35:20', duration: '35m20s', passRate: 95.8, progress: 100, operator: '系统' },
  { id: 3, name: 'quality_job_003', strategy: '财务数据质量策略', trigger: '定时', status: '失败', startTime: '2026-09-26 03:30:00', endTime: '2026-09-26 03:42:15', duration: '12m15s', passRate: 88.5, progress: 100, operator: '系统' },
  { id: 4, name: 'quality_job_004', strategy: '日志数据质量策略', trigger: '定时', status: '运行中', startTime: '2026-09-26 10:00:00', endTime: '-', duration: '-', passRate: 0, progress: 65, operator: '系统' },
  { id: 5, name: 'quality_job_005', strategy: '商品数据质量策略', trigger: '手动', status: '成功', startTime: '2026-09-25 14:30:00', endTime: '2026-09-25 14:52:30', duration: '22m30s', passRate: 96.7, progress: 100, operator: '张三' },
  { id: 6, name: 'quality_job_006', strategy: '营销活动质量策略', trigger: '定时', status: '部分成功', startTime: '2026-09-26 05:00:00', endTime: '2026-09-26 05:15:40', duration: '15m40s', passRate: 94.2, progress: 100, operator: '系统' },
  { id: 7, name: 'quality_job_007', strategy: '供应链质量策略', trigger: '定时', status: '成功', startTime: '2026-09-26 06:00:00', endTime: '2026-09-26 06:18:22', duration: '18m22s', passRate: 96.1, progress: 100, operator: '系统' },
  { id: 8, name: 'quality_job_008', strategy: '会员质量监控策略', trigger: '手动', status: '成功', startTime: '2026-09-25 16:00:00', endTime: '2026-09-25 16:12:18', duration: '12m18s', passRate: 97.8, progress: 100, operator: '李四' },
  { id: 9, name: 'quality_job_009', strategy: '交易数据质量策略', trigger: '定时', status: '成功', startTime: '2026-09-25 01:00:00', endTime: '2026-09-25 01:22:10', duration: '22m10s', passRate: 96.9, progress: 100, operator: '系统' },
  { id: 10, name: 'quality_job_010', strategy: '客户数据质量策略', trigger: '定时', status: '成功', startTime: '2026-09-25 02:00:00', endTime: '2026-09-25 02:34:05', duration: '34m05s', passRate: 95.2, progress: 100, operator: '系统' },
  { id: 11, name: 'quality_job_011', strategy: '财务数据质量策略', trigger: '手动', status: '失败', startTime: '2026-09-24 10:30:00', endTime: '2026-09-24 10:38:45', duration: '8m45s', passRate: 85.6, progress: 100, operator: '王五' },
  { id: 12, name: 'quality_job_012', strategy: '日志数据质量策略', trigger: '定时', status: '成功', startTime: '2026-09-26 08:00:00', endTime: '2026-09-26 08:08:30', duration: '8m30s', passRate: 98.5, progress: 100, operator: '系统' }
])

function getPassRateColor(rate) {
  if (rate >= 95) return '#00b42a'
  if (rate >= 90) return '#ff7d00'
  return '#f53f3f'
}

function loadData() {
  loading.value = true
  setTimeout(() => {
    let list = allData.value.filter(t =>
      (!query.keyword || t.name.includes(query.keyword) || t.strategy.includes(query.keyword)) &&
      (!query.status || t.status === query.status)
    )
    total.value = list.length
    const start = (query.page - 1) * query.size
    tableData.value = list.slice(start, start + query.size)
    loading.value = false
  }, 300)
}

function viewDetail(row) {
  currentTask.value = row
  // 生成规则执行进度数据
  ruleProgress.value = [
    { name: '订单金额非空校验', status: '成功', passRate: 99.2, progress: 100, duration: '2m15s' },
    { name: '用户手机号格式校验', status: '成功', passRate: 95.8, progress: 100, duration: '3m30s' },
    { name: '订单号唯一性校验', status: '成功', passRate: 99.9, progress: 100, duration: '4m20s' },
    { name: '支付金额范围校验', status: row.status === '失败' ? '失败' : '成功', passRate: row.status === '失败' ? 78.5 : 97.1, progress: 100, duration: '2m45s' },
    { name: '订单状态枚举校验', status: '成功', passRate: 99.5, progress: 100, duration: '1m50s' },
    { name: '客户ID参照完整性', status: row.status === '运行中' ? '执行中' : '成功', passRate: row.status === '运行中' ? 0 : 96.8, progress: row.status === '运行中' ? 45 : 100, duration: row.status === '运行中' ? '执行中' : '5m10s' },
    { name: '数据延迟及时性校验', status: row.status === '运行中' ? '等待中' : '成功', passRate: row.status === '运行中' ? 0 : 93.5, progress: row.status === '运行中' ? 0 : 100, duration: row.status === '运行中' ? '-' : '1m30s' }
  ]

  // 生成执行日志
  execLogs.value = [
    { time: '01:00:00', level: 'INFO', message: '任务开始执行，策略：交易数据质量策略' },
    { time: '01:00:02', level: 'INFO', message: '加载规则配置，共 7 条规则' },
    { time: '01:00:05', level: 'INFO', message: '开始执行规则：订单金额非空校验' },
    { time: '01:02:20', level: 'INFO', message: '规则执行完成：订单金额非空校验，校验 125,680 行，异常 1,005 行，通过率 99.2%' },
    { time: '01:02:20', level: 'INFO', message: '开始执行规则：用户手机号格式校验' },
    { time: '01:05:50', level: 'WARN', message: '规则执行完成：用户手机号格式校验，校验 98,450 行，异常 4,126 行，通过率 95.8%' },
    { time: '01:05:50', level: 'INFO', message: '开始执行规则：订单号唯一性校验' },
    { time: '01:10:10', level: 'INFO', message: '规则执行完成：订单号唯一性校验，校验 125,680 行，异常 12 行，通过率 99.9%' },
    { time: '01:10:10', level: 'INFO', message: '开始执行规则：支付金额范围校验' },
    { time: '01:12:55', level: row.status === '失败' ? 'ERROR' : 'INFO', message: row.status === '失败' ? '规则执行失败：支付金额范围校验，连接数据库超时' : '规则执行完成：支付金额范围校验，校验 89,320 行，异常 2,587 行，通过率 97.1%' },
    { time: '01:12:55', level: 'INFO', message: '开始执行规则：订单状态枚举校验' },
    { time: '01:14:45', level: 'INFO', message: '规则执行完成：订单状态枚举校验，校验 125,680 行，异常 628 行，通过率 99.5%' },
    { time: '01:14:45', level: 'INFO', message: '开始执行规则：客户ID参照完整性' },
    { time: '01:19:55', level: 'WARN', message: '规则执行完成：客户ID参照完整性，校验 125,680 行，异常 4,022 行，通过率 96.8%' },
    { time: '01:19:55', level: 'INFO', message: '开始执行规则：数据延迟及时性校验' },
    { time: '01:21:25', level: 'WARN', message: '规则执行完成：数据延迟及时性校验，延迟数据 8,169 行，通过率 93.5%' },
    { time: '01:23:45', level: row.status === '失败' ? 'ERROR' : 'INFO', message: row.status === '失败' ? '任务执行失败，1 条规则执行异常' : `任务执行完成，共 7 条规则，总通过率 97.2%` }
  ]

  detailVisible.value = true
}

function viewReport(row) {
  ElMessage.info(`正在打开「${row.name}」的质量报告...`)
}

function stopTask(row) {
  ElMessageBox.confirm(
    `确定停止任务「${row.name}」吗？停止后已执行的结果将被保留。`,
    '停止确认',
    { type: 'warning', confirmButtonText: '确定停止' }
  ).then(() => {
    row.status = '失败'
    row.endTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
    row.duration = '已停止'
    row.passRate = 65
    ElMessage.success('任务已停止')
    loadData()
  }).catch(() => {})
}

function rerunTask(row) {
  ElMessageBox.confirm(
    `确定重新执行任务「${row.name}」吗？`,
    '重新执行确认',
    { type: 'info' }
  ).then(() => {
    row.status = '运行中'
    row.startTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
    row.endTime = '-'
    row.duration = '-'
    row.progress = 0
    ElMessage.success(`任务「${row.name}」重新执行已提交`)
    loadData()

    // 模拟进度
    let progress = 0
    const timer = setInterval(() => {
      progress += Math.floor(Math.random() * 15) + 5
      if (progress >= 100) {
        progress = 100
        clearInterval(timer)
        row.status = '成功'
        row.endTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
        row.duration = `${Math.floor(Math.random() * 20) + 10}m${Math.floor(Math.random() * 60)}s`
        row.passRate = +(93 + Math.random() * 7).toFixed(1)
        row.progress = 100
        ElMessage.success(`任务「${row.name}」执行完成`)
      } else {
        row.progress = progress
      }
    }, 800)
  }).catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.status-dot {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
}

.status-dot i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.status-dot.成功 { color: var(--dp-success); }
.status-dot.成功 i { background: var(--dp-success); box-shadow: 0 0 6px var(--dp-success); }
.status-dot.运行中 { color: var(--dp-primary); }
.status-dot.运行中 i { background: var(--dp-primary); box-shadow: 0 0 6px var(--dp-primary); animation: pulse 1.5s infinite; }
.status-dot.失败 { color: var(--dp-danger); }
.status-dot.失败 i { background: var(--dp-danger); box-shadow: 0 0 6px var(--dp-danger); }
.status-dot.部分成功 { color: var(--dp-warning); }
.status-dot.部分成功 i { background: var(--dp-warning); box-shadow: 0 0 6px var(--dp-warning); }

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.4; }
  100% { opacity: 1; }
}

.detail-section {
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid var(--dp-primary);
}

.rule-progress-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rule-progress-item {
  background: var(--dp-bg-page);
  border-radius: 8px;
  padding: 12px 14px;
}

.rule-progress-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.rule-name {
  font-weight: 500;
  font-size: 13px;
  color: var(--dp-text-1);
}

.rule-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
}

.rule-status i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.rule-status.成功 { color: var(--dp-success); }
.rule-status.成功 i { background: var(--dp-success); }
.rule-status.失败 { color: var(--dp-danger); }
.rule-status.失败 i { background: var(--dp-danger); }
.rule-status.执行中 { color: var(--dp-primary); }
.rule-status.执行中 i { background: var(--dp-primary); animation: pulse 1.5s infinite; }
.rule-status.等待中 { color: var(--dp-text-3); }
.rule-status.等待中 i { background: var(--dp-text-3); }

.rule-progress-body {
  margin-bottom: 6px;
}

.rule-progress-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--dp-text-3);
}

.log-container {
  background: #1e1e1e;
  border-radius: 8px;
  padding: 12px;
  max-height: 300px;
  overflow-y: auto;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  line-height: 1.8;
}

.log-line {
  display: flex;
  gap: 8px;
  white-space: pre-wrap;
  word-break: break-all;
}

.log-time {
  color: #858585;
  flex-shrink: 0;
}

.log-level {
  flex-shrink: 0;
  font-weight: 600;
  min-width: 50px;
}

.log-level.INFO { color: #4ec9b0; }
.log-level.WARN { color: #dcdcaa; }
.log-level.ERROR { color: #f53f3f; }

.log-message {
  color: #d4d4d4;
}
</style>
