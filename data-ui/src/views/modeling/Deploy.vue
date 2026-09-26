<template>
  <div class="dp-page">
    <PageHeader title="模型部署" desc="将物理模型部署到目标数据源，支持DDL执行、数据回填与部署验证">
      <el-button type="primary" :icon="Promotion" @click="createVisible = true">新建部署任务</el-button>
    </PageHeader>

    <!-- 部署统计 -->
    <div class="dp-stat-grid dp-fade-up">
      <StatCard label="部署任务总数" :value="stats.total" unit="个" icon="Promotion" color="var(--dp-primary)" bg="var(--dp-primary-light)" :trend="8" />
      <StatCard label="成功部署" :value="stats.success" unit="个" icon="CircleCheck" color="var(--dp-success)" bg="var(--dp-success-light)" :trend="12" />
      <StatCard label="部署中" :value="stats.running" unit="个" icon="Loading" color="var(--dp-warning)" bg="var(--dp-warning-light)" />
      <StatCard label="部署成功率" :value="stats.successRate" unit="%" icon="TrendCharts" color="var(--dp-purple)" bg="var(--dp-purple-light)" :trend="2.5" />
    </div>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索任务名称/模型名称" clearable style="width: 240px" :prefix-icon="Search" />
        <el-select v-model="query.deployType" placeholder="部署类型" clearable style="width: 120px">
          <el-option label="全量部署" value="全量" />
          <el-option label="增量部署" value="增量" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="待部署" value="待部署" />
          <el-option label="部署中" value="部署中" />
          <el-option label="成功" value="成功" />
          <el-option label="失败" value="失败" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredTasks" border stripe v-loading="loading">
        <el-table-column prop="taskName" label="任务名称" min-width="180">
          <template #default="{ row }">
            <div class="deploy-task-name">
              <el-icon class="dt-icon" :class="statusIconClass(row.status)"><component :is="statusIcon(row.status)" /></el-icon>
              <div>
                <div class="dt-title">{{ row.taskName }}</div>
                <div class="dt-sub">{{ row.modelName }} · {{ row.version }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="targetDataSource" label="目标数据源" min-width="160" />
        <el-table-column prop="deployType" label="部署类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="row.deployType === '全量' ? 'primary' : 'success'">{{ row.deployType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tables" label="表数量" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="status-dot" :class="statusDotClass(row.status)"></span>
            {{ row.status }}
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="duration" label="耗时" width="90" align="center" />
        <el-table-column prop="operator" label="操作人" width="80" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">查看详情</el-button>
            <el-button link type="primary" size="small" @click="viewLog(row)">日志</el-button>
            <el-button v-if="row.status === '失败'" link type="warning" size="small" @click="redeploy(row)">重新部署</el-button>
            <el-button v-if="row.status === '成功'" link type="danger" size="small" @click="rollback(row)">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
      />
    </div>

    <!-- 新建部署任务弹窗 -->
    <el-dialog v-model="createVisible" title="新建部署任务" width="560px" destroy-on-close>
      <el-form :model="deployForm" label-width="110px">
        <el-form-item label="任务名称" required>
          <el-input v-model="deployForm.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="选择模型" required>
          <el-select v-model="deployForm.modelId" style="width: 100%" placeholder="请选择物理模型">
            <el-option label="dwd_trade_v3 - 交易主题域物理模型" value="1" />
            <el-option label="dwd_customer_v3 - 客户主题域物理模型" value="2" />
            <el-option label="dws_summary_v2 - 汇总层物理模型" value="3" />
            <el-option label="dim_public_v2 - 公共维度模型" value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择版本" required>
          <el-select v-model="deployForm.version" style="width: 100%" placeholder="请选择版本">
            <el-option label="V3.0 (最新)" value="V3.0" />
            <el-option label="V2.1" value="V2.1" />
            <el-option label="V2.0" value="V2.0" />
            <el-option label="V1.2" value="V1.2" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标数据源" required>
          <el-select v-model="deployForm.targetDataSource" style="width: 100%" placeholder="请选择目标数据源">
            <el-option label="生产Hive集群 - dwd_trade_db" value="生产Hive集群" />
            <el-option label="测试Hive集群 - dwd_test_db" value="测试Hive集群" />
            <el-option label="ClickHouse集群 - ads_db" value="ClickHouse集群" />
            <el-option label="MySQL主库 - report_db" value="MySQL主库" />
          </el-select>
        </el-form-item>
        <el-form-item label="部署类型">
          <el-radio-group v-model="deployForm.deployType">
            <el-radio value="全量">全量部署</el-radio>
            <el-radio value="增量">增量部署</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="部署策略">
          <el-radio-group v-model="deployForm.strategy">
            <el-radio value="backup">先备份再部署</el-radio>
            <el-radio value="overwrite">直接覆盖</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="表冲突处理">
          <el-radio-group v-model="deployForm.conflict">
            <el-radio value="skip">跳过</el-radio>
            <el-radio value="overwrite">覆盖</el-radio>
            <el-radio value="append">追加</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="deployForm.remark" type="textarea" :rows="2" placeholder="请输入部署说明（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDeploy">提交部署</el-button>
      </template>
    </el-dialog>

    <!-- 部署详情抽屉 -->
    <el-drawer v-model="detailVisible" title="部署详情" size="560px" destroy-on-close>
      <template v-if="currentTask">
        <div class="deploy-detail">
          <!-- 基本信息 -->
          <div class="dd-section">
            <div class="dd-section-title">基本信息</div>
            <div class="dd-info-grid">
              <div class="dd-info-item">
                <span class="dd-label">任务名称</span>
                <span class="dd-value">{{ currentTask.taskName }}</span>
              </div>
              <div class="dd-info-item">
                <span class="dd-label">部署状态</span>
                <el-tag :type="statusTagType(currentTask.status)" size="small">{{ currentTask.status }}</el-tag>
              </div>
              <div class="dd-info-item">
                <span class="dd-label">模型名称</span>
                <span class="dd-value">{{ currentTask.modelName }}</span>
              </div>
              <div class="dd-info-item">
                <span class="dd-label">版本</span>
                <span class="dd-value dp-mono">{{ currentTask.version }}</span>
              </div>
              <div class="dd-info-item">
                <span class="dd-label">目标数据源</span>
                <span class="dd-value">{{ currentTask.targetDataSource }}</span>
              </div>
              <div class="dd-info-item">
                <span class="dd-label">部署类型</span>
                <span class="dd-value">{{ currentTask.deployType }}</span>
              </div>
              <div class="dd-info-item">
                <span class="dd-label">开始时间</span>
                <span class="dd-value">{{ currentTask.startTime }}</span>
              </div>
              <div class="dd-info-item">
                <span class="dd-label">耗时</span>
                <span class="dd-value">{{ currentTask.duration }}</span>
              </div>
              <div class="dd-info-item">
                <span class="dd-label">操作人</span>
                <span class="dd-value">{{ currentTask.operator }}</span>
              </div>
              <div class="dd-info-item">
                <span class="dd-label">表数量</span>
                <span class="dd-value">{{ currentTask.tables }} 张</span>
              </div>
            </div>
          </div>

          <!-- 部署进度 -->
          <div class="dd-section">
            <div class="dd-section-title">部署进度</div>
            <el-steps :active="currentStage" finish-status="success" simple>
              <el-step title="结构检查" description="DDL预检与冲突扫描" />
              <el-step title="建表执行" description="目标库执行DDL" />
              <el-step title="数据回填" description="历史数据初始化" />
              <el-step title="部署验证" description="行数校验与抽样比对" />
              <el-step title="发布完成" description="登记资产并通知" />
            </el-steps>
          </div>

          <!-- 部署结果 -->
          <div class="dd-section">
            <div class="dd-section-title">
              部署结果
              <el-tag v-if="currentTask.status === '成功'" type="success" size="small" style="margin-left: 8px">全部成功</el-tag>
              <el-tag v-else-if="currentTask.status === '失败'" type="danger" size="small" style="margin-left: 8px">存在失败</el-tag>
            </div>
            <el-table :data="deployResultList" border stripe size="small">
              <el-table-column prop="tableName" label="表名" min-width="160" class-name="dp-mono" />
              <el-table-column prop="status" label="状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === '成功' ? 'success' : row.status === '跳过' ? 'info' : 'danger'">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="rows" label="数据行数" width="110" align="right">
                <template #default="{ row }">{{ row.rows.toLocaleString() }}</template>
              </el-table-column>
              <el-table-column prop="duration" label="耗时" width="80" align="center" />
              <el-table-column prop="message" label="说明" min-width="120" show-overflow-tooltip />
            </el-table>
          </div>
        </div>
      </template>
      <template #footer>
        <el-button v-if="currentTask?.status === '失败'" type="warning" @click="redeploy(currentTask)">重新部署</el-button>
        <el-button v-if="currentTask?.status === '成功'" type="danger" @click="rollback(currentTask)">回滚</el-button>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-drawer>

    <!-- 日志抽屉 -->
    <el-drawer v-model="logVisible" title="部署日志" size="600px" destroy-on-close>
      <div class="deploy-log">
        <div class="log-toolbar">
          <el-select v-model="logLevel" size="small" style="width: 120px">
            <el-option label="全部日志" value="all" />
            <el-option label="INFO" value="info" />
            <el-option label="WARN" value="warn" />
            <el-option label="ERROR" value="error" />
          </el-select>
          <el-button size="small" :icon="Download">下载日志</el-button>
        </div>
        <div class="log-content">
          <div v-for="(log, idx) in logList" :key="idx" class="log-line" :class="'log-' + log.level">
            <span class="log-time">[{{ log.time }}]</span>
            <span class="log-level" :class="'level-' + log.level">[{{ log.level.toUpperCase() }}]</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { Download, Promotion, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const createVisible = ref(false)
const detailVisible = ref(false)
const logVisible = ref(false)
const currentTask = ref(null)
const logLevel = ref('all')
const currentStage = ref(4)

const query = reactive({
  keyword: '',
  deployType: '',
  status: '',
  page: 1,
  size: 10
})

const stats = ref({
  total: 28,
  success: 24,
  running: 2,
  successRate: 92.3
})

const tasks = ref([
  { id: 1, taskName: '交易主题域模型-生产部署', modelName: 'dwd_trade_v3', version: 'V3.0', targetDataSource: '生产Hive集群', deployType: '全量', tables: 12, status: '成功', startTime: '2024-09-25 10:15:00', duration: '12m30s', operator: '李娜' },
  { id: 2, taskName: '客户主题域模型-测试环境部署', modelName: 'dwd_customer_v3', version: 'V3.0', targetDataSource: '测试Hive集群', deployType: '全量', tables: 8, status: '成功', startTime: '2024-09-24 14:20:00', duration: '8m15s', operator: '张伟' },
  { id: 3, taskName: '汇总层模型-增量部署', modelName: 'dws_summary_v2', version: 'V2.1', targetDataSource: 'ClickHouse集群', deployType: '增量', tables: 6, status: '部署中', startTime: '2024-09-26 09:30:00', duration: '-', operator: '赵敏' },
  { id: 4, taskName: '广告报表模型-生产部署', modelName: 'ads_report_v1', version: 'V1.0', targetDataSource: 'MySQL主库', deployType: '全量', tables: 5, status: '失败', startTime: '2024-09-23 16:45:00', duration: '5m20s', operator: '孙浩' },
  { id: 5, taskName: '公共维度模型-生产部署', modelName: 'dim_public_v2', version: 'V2.0', targetDataSource: '生产Hive集群', deployType: '全量', tables: 9, status: '成功', startTime: '2024-09-22 11:00:00', duration: '15m40s', operator: '周明' },
  { id: 6, taskName: '财务模型-测试部署', modelName: 'dwd_finance_v1', version: 'V1.2', targetDataSource: '测试Oracle库', deployType: '增量', tables: 5, status: '成功', startTime: '2024-09-21 13:30:00', duration: '6m45s', operator: '吴丽' },
  { id: 7, taskName: '日志模型-生产部署', modelName: 'dwd_log_v1', version: 'V1.0', targetDataSource: 'ClickHouse集群', deployType: '全量', tables: 4, status: '待部署', startTime: '-', duration: '-', operator: '张伟' },
  { id: 8, taskName: '渠道分析模型-生产部署', modelName: 'dws_channel_v1', version: 'V1.0', targetDataSource: 'Doris集群', deployType: '全量', tables: 3, status: '成功', startTime: '2024-09-20 10:00:00', duration: '4m10s', operator: '孙浩' }
])

const deployResultList = ref([
  { tableName: 'dwd_trade_order_di', status: '成功', rows: 1258634, duration: '2m30s', message: '建表成功，数据回填完成' },
  { tableName: 'dwd_trade_order_detail_di', status: '成功', rows: 3856210, duration: '4m15s', message: '建表成功，数据回填完成' },
  { tableName: 'dwd_trade_payment_di', status: '成功', rows: 985420, duration: '1m50s', message: '建表成功，数据回填完成' },
  { tableName: 'dwd_trade_refund_di', status: '成功', rows: 125680, duration: '1m20s', message: '建表成功，数据回填完成' },
  { tableName: 'dim_date', status: '成功', rows: 3652, duration: '10s', message: '建表成功，数据回填完成' }
])

const logList = ref([
  { time: '2024-09-25 10:15:00', level: 'info', message: '开始执行部署任务：交易主题域模型-生产部署' },
  { time: '2024-09-25 10:15:02', level: 'info', message: '正在连接目标数据源：生产Hive集群' },
  { time: '2024-09-25 10:15:05', level: 'info', message: '数据源连接成功，开始执行结构检查' },
  { time: '2024-09-25 10:15:08', level: 'info', message: '检查到 12 张待创建表，0 张冲突表' },
  { time: '2024-09-25 10:15:10', level: 'warn', message: '表 dwd_trade_order_di 已存在，将按策略执行覆盖' },
  { time: '2024-09-25 10:15:12', level: 'info', message: '开始执行建表DDL...' },
  { time: '2024-09-25 10:15:30', level: 'info', message: '已完成 5/12 张表创建' },
  { time: '2024-09-25 10:16:00', level: 'info', message: '所有表创建完成，开始数据回填' },
  { time: '2024-09-25 10:18:00', level: 'info', message: '数据回填进度：50%' },
  { time: '2024-09-25 10:22:00', level: 'info', message: '数据回填完成，开始部署验证' },
  { time: '2024-09-25 10:24:30', level: 'info', message: '验证通过：行数一致率 99.98%，字段匹配 100%' },
  { time: '2024-09-25 10:25:00', level: 'info', message: '部署任务执行完成，状态：成功' },
  { time: '2024-09-25 10:25:02', level: 'info', message: '已登记数据资产，通知已发送' }
])

const filteredTasks = computed(() => {
  return tasks.value.filter(t =>
    (!query.keyword || t.taskName.includes(query.keyword) || t.modelName.includes(query.keyword)) &&
    (!query.deployType || t.deployType === query.deployType) &&
    (!query.status || t.status === query.status)
  )
})

const total = computed(() => filteredTasks.value.length)

function statusDotClass(status) {
  return { 成功: 'success', 部署中: 'warning', 失败: 'danger', 待部署: 'info' }[status] || 'info'
}
function statusTagType(status) {
  return { 成功: 'success', 部署中: 'warning', 失败: 'danger', 待部署: 'info' }[status] || 'info'
}
function statusIcon(status) {
  return { 成功: 'CircleCheckFilled', 部署中: 'Loading', 失败: 'CircleCloseFilled', 待部署: 'Clock' }[status] || 'Clock'
}
function statusIconClass(status) {
  return { 成功: 'icon-success', 部署中: 'icon-warning', 失败: 'icon-danger', 待部署: 'icon-info' }[status] || 'icon-info'
}

function handleRefresh() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('数据已刷新')
  }, 800)
}

function viewDetail(row) {
  currentTask.value = row
  if (row.status === '成功') currentStage.value = 5
  else if (row.status === '部署中') currentStage.value = 2
  else if (row.status === '失败') currentStage.value = 2
  else currentStage.value = 0
  detailVisible.value = true
}

function viewLog(row) {
  currentTask.value = row
  logVisible.value = true
}

function redeploy(row) {
  ElMessageBox.confirm(`确定重新部署「${row.taskName}」吗？`, '重新部署确认', {
    type: 'warning',
    confirmButtonText: '确定重新部署',
    cancelButtonText: '取消'
  }).then(() => {
    row.status = '部署中'
    row.startTime = '刚刚'
    row.duration = '-'
    ElMessage.success('已重新提交部署任务')
    detailVisible.value = false
    logVisible.value = false
  }).catch(() => {})
}

function rollback(row) {
  ElMessageBox.confirm(`确定回滚「${row.taskName}」吗？回滚将恢复到上一版本。`, '回滚确认', {
    type: 'warning',
    confirmButtonText: '确定回滚',
    cancelButtonText: '取消'
  }).then(() => {
    ElMessage.success('回滚任务已提交')
    detailVisible.value = false
  }).catch(() => {})
}

// 新建部署
const deployForm = reactive({
  taskName: '',
  modelId: '',
  version: 'V3.0',
  targetDataSource: '',
  deployType: '全量',
  strategy: 'backup',
  conflict: 'skip',
  remark: ''
})

function submitDeploy() {
  if (!deployForm.taskName) {
    ElMessage.warning('请输入任务名称')
    return
  }
  if (!deployForm.modelId) {
    ElMessage.warning('请选择模型')
    return
  }
  if (!deployForm.targetDataSource) {
    ElMessage.warning('请选择目标数据源')
    return
  }
  tasks.value.unshift({
    id: Date.now(),
    taskName: deployForm.taskName,
    modelName: 'dwd_trade_v3',
    version: deployForm.version,
    targetDataSource: deployForm.targetDataSource,
    deployType: deployForm.deployType,
    tables: 12,
    status: '待部署',
    startTime: '-',
    duration: '-',
    operator: '当前用户'
  })
  createVisible.value = false
  ElMessage.success('部署任务已提交')
}
</script>

<style scoped>
.deploy-task-name {
  display: flex;
  align-items: center;
  gap: 10px;
}
.dt-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
}
.dt-icon.icon-success { background: var(--dp-success-light); color: var(--dp-success); }
.dt-icon.icon-warning { background: var(--dp-warning-light); color: var(--dp-warning); }
.dt-icon.icon-danger { background: var(--dp-danger-light); color: var(--dp-danger); }
.dt-icon.icon-info { background: #f2f3f5; color: var(--dp-text-3); }
.dt-title {
  font-weight: 500;
  color: var(--dp-text-1);
  font-size: 13px;
}
.dt-sub {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 2px;
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
.status-dot.danger { background: var(--dp-danger); }
.status-dot.info { background: var(--dp-text-4); }

/* 详情 */
.deploy-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.dd-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}
.dd-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 14px;
  background: var(--dp-bg-page);
  border-radius: 8px;
}
.dd-info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.dd-label {
  font-size: 12px;
  color: var(--dp-text-3);
}
.dd-value {
  font-size: 13px;
  color: var(--dp-text-1);
  font-weight: 500;
}

/* 日志 */
.deploy-log {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.log-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.log-content {
  flex: 1;
  background: #1e1e2e;
  border-radius: 8px;
  padding: 12px;
  overflow-y: auto;
  font-family: Consolas, monospace;
  font-size: 12px;
  line-height: 1.8;
}
.log-line {
  display: flex;
  gap: 8px;
  color: #cdd6f4;
}
.log-time { color: #6c7086; flex-shrink: 0; }
.log-level { flex-shrink: 0; font-weight: 600; }
.log-level.level-info { color: #89b4fa; }
.log-level.level-warn { color: #f9e2af; }
.log-level.level-error { color: #f38ba8; }
.log-message { flex: 1; word-break: break-all; }
</style>
