<template>
  <div class="dp-page">
    <PageHeader title="质量数据源" desc="纳入质量管控范围的数据源，可配置质检范围与采集方案。">
      <el-button type="primary" :icon="Plus" @click="openAddDialog">纳管数据源</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索数据源名称" clearable style="width: 220px" :prefix-icon="Search" @input="loadData" />
        <el-select v-model="query.type" placeholder="类型筛选" clearable style="width: 140px" @change="loadData">
          <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width: 120px" @change="loadData">
          <el-option label="运行中" value="运行中" />
          <el-option label="已停用" value="已停用" />
          <el-option label="异常" value="异常" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="name" label="数据源名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="ds-name">
              <div class="ds-icon" :class="row.type">
                <el-icon><Coin /></el-icon>
              </div>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="env" label="环境" width="80">
          <template #default="{ row }">
            <el-tag :type="row.env === '生产' ? 'danger' : row.env === '测试' ? 'warning' : 'info'" size="small" effect="plain">{{ row.env }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ruleCount" label="已配置规则数" width="120" align="center" />
        <el-table-column prop="execCount" label="已执行次数" width="110" align="center" />
        <el-table-column prop="passRate" label="平均合格率" width="130">
          <template #default="{ row }">
            <el-progress :percentage="row.passRate" :stroke-width="6" :color="getPassRateColor(row.passRate)" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="ds-status" :class="row.status">
              <i></i>{{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="90" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="configRules(row)">配置规则</el-button>
            <el-button link type="primary" size="small" @click="viewReport(row)">查看报告</el-button>
            <el-button link type="danger" size="small" @click="removeDatasource(row)">移除</el-button>
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

    <!-- 纳管数据源弹窗 -->
    <el-dialog v-model="addDialogVisible" title="纳管数据源" width="680px" destroy-on-close>
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="选择数据源" required>
          <el-select v-model="addForm.datasourceId" placeholder="请选择要纳管的数据源" style="width: 100%" filterable>
            <el-option v-for="ds in availableDatasources" :key="ds.id" :label="ds.name" :value="ds.id">
              <span>{{ ds.name }}</span>
              <span style="float: right; color: var(--dp-text-3); font-size: 12px">{{ ds.type }}</span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-divider content-position="left">质量检测范围</el-divider>

        <el-form-item label="选择库表">
          <el-tree
            ref="treeRef"
            :data="tableTreeData"
            show-checkbox
            node-key="id"
            :default-expanded-keys="['db1', 'db2']"
            :props="{ label: 'name', children: 'children' }"
            style="width: 100%; max-height: 300px; overflow-y: auto; border: 1px solid var(--dp-border); border-radius: 8px; padding: 8px;"
          />
        </el-form-item>

        <el-form-item label="检测频率">
          <el-radio-group v-model="addForm.frequency">
            <el-radio value="daily">每日检测</el-radio>
            <el-radio value="hourly">每小时检测</el-radio>
            <el-radio value="weekly">每周检测</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="负责人">
          <el-select v-model="addForm.owner" placeholder="请选择负责人" style="width: 100%">
            <el-option v-for="o in ownerOptions" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdd">确认纳管</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const addDialogVisible = ref(false)
const treeRef = ref(null)

const typeOptions = ['MySQL', 'Oracle', 'PostgreSQL', 'Hive', 'Kafka', 'ClickHouse', 'API']

const ownerOptions = ['张三', '李四', '王五', '赵六', '钱七']

const query = reactive({
  keyword: '',
  type: '',
  status: '',
  page: 1,
  size: 10
})

const total = ref(0)

// Mock 数据
const allData = ref([
  { id: 1, name: '核心交易库', type: 'MySQL', env: '生产', ruleCount: 28, execCount: 12560, passRate: 97.2, status: '运行中', owner: '张三' },
  { id: 2, name: '客户主数据库', type: 'Oracle', env: '生产', ruleCount: 35, execCount: 18920, passRate: 95.8, status: '运行中', owner: '李四' },
  { id: 3, name: '日志采集库', type: 'Kafka', env: '生产', ruleCount: 12, execCount: 8650, passRate: 98.5, status: '运行中', owner: '王五' },
  { id: 4, name: '财务系统库', type: 'PostgreSQL', env: '生产', ruleCount: 22, execCount: 9870, passRate: 94.3, status: '异常', owner: '赵六' },
  { id: 5, name: '会员中心库', type: 'MySQL', env: '测试', ruleCount: 18, execCount: 5420, passRate: 96.1, status: '运行中', owner: '钱七' },
  { id: 6, name: '商品中心库', type: 'Hive', env: '生产', ruleCount: 15, execCount: 7230, passRate: 97.8, status: '运行中', owner: '张三' },
  { id: 7, name: '营销活动库', type: 'MySQL', env: '测试', ruleCount: 8, execCount: 3210, passRate: 99.1, status: '已停用', owner: '李四' },
  { id: 8, name: '供应链库', type: 'ClickHouse', env: '生产', ruleCount: 20, execCount: 10560, passRate: 96.7, status: '运行中', owner: '王五' },
  { id: 9, name: '数据服务API', type: 'API', env: '生产', ruleCount: 6, execCount: 2890, passRate: 93.5, status: '运行中', owner: '赵六' },
  { id: 10, name: '报表数据集市', type: 'Hive', env: '生产', ruleCount: 16, execCount: 6780, passRate: 98.2, status: '运行中', owner: '钱七' },
  { id: 11, name: '用户行为库', type: 'Kafka', env: '测试', ruleCount: 10, execCount: 4120, passRate: 95.6, status: '已停用', owner: '张三' },
  { id: 12, name: '订单明细库', type: 'MySQL', env: '生产', ruleCount: 25, execCount: 15680, passRate: 97.4, status: '运行中', owner: '李四' }
])

const tableData = ref([])

// 可选数据源（未纳管的）
const availableDatasources = ref([
  { id: 101, name: 'OA系统库', type: 'MySQL' },
  { id: 102, name: 'HR系统库', type: 'Oracle' },
  { id: 103, name: 'CRM系统库', type: 'PostgreSQL' },
  { id: 104, name: 'ERP系统库', type: 'MySQL' },
  { id: 105, name: '客服系统库', type: 'MongoDB' }
])

// 库表树
const tableTreeData = ref([
  {
    id: 'db1',
    name: 'ods_customer_db',
    children: [
      { id: 't1', name: 'customer_info' },
      { id: 't2', name: 'customer_addr' },
      { id: 't3', name: 'customer_level' },
      { id: 't4', name: 'member_profile' }
    ]
  },
  {
    id: 'db2',
    name: 'dwd_trade_db',
    children: [
      { id: 't5', name: 'order_detail' },
      { id: 't6', name: 'trade_flow' },
      { id: 't7', name: 'payment_record' },
      { id: 't8', name: 'refund_order' }
    ]
  },
  {
    id: 'db3',
    name: 'dws_summary_db',
    children: [
      { id: 't9', name: 'sale_day' },
      { id: 't10', name: 'user_behavior_stat' },
      { id: 't11', name: 'shop_daily_stat' }
    ]
  }
])

const addForm = reactive({
  datasourceId: '',
  frequency: 'daily',
  owner: ''
})

function typeTagType(type) {
  const map = {
    MySQL: 'primary',
    Oracle: 'warning',
    PostgreSQL: 'success',
    Hive: 'purple',
    Kafka: 'danger',
    ClickHouse: 'cyan',
    API: 'info'
  }
  return map[type] || 'info'
}

function getPassRateColor(rate) {
  if (rate >= 95) return '#00b42a'
  if (rate >= 90) return '#ff7d00'
  return '#f53f3f'
}

function loadData() {
  loading.value = true
  setTimeout(() => {
    let list = allData.value.filter(d =>
      (!query.keyword || d.name.includes(query.keyword)) &&
      (!query.type || d.type === query.type) &&
      (!query.status || d.status === query.status)
    )
    total.value = list.length
    const start = (query.page - 1) * query.size
    tableData.value = list.slice(start, start + query.size)
    loading.value = false
  }, 300)
}

function openAddDialog() {
  addForm.datasourceId = ''
  addForm.frequency = 'daily'
  addForm.owner = ''
  addDialogVisible.value = true
}

function confirmAdd() {
  if (!addForm.datasourceId) {
    ElMessage.warning('请选择数据源')
    return
  }
  const ds = availableDatasources.value.find(d => d.id === addForm.datasourceId)
  if (!ds) return

  const checkedNodes = treeRef.value?.getCheckedNodes() || []
  const tableCount = checkedNodes.filter(n => n.children === undefined).length

  allData.value.unshift({
    id: Date.now(),
    name: ds.name,
    type: ds.type,
    env: '生产',
    ruleCount: 0,
    execCount: 0,
    passRate: 100,
    status: '运行中',
    owner: addForm.owner || '未指定'
  })

  ElMessage.success(`数据源「${ds.name}」纳管成功，已选择 ${tableCount} 张表`)
  addDialogVisible.value = false
  loadData()
}

function configRules(row) {
  ElMessage.info(`正在跳转到「${row.name}」的规则配置页面...`)
}

function viewReport(row) {
  ElMessage.info(`正在打开「${row.name}」的质量报告...`)
}

function removeDatasource(row) {
  ElMessageBox.confirm(
    `确定要移除数据源「${row.name}」吗？移除后相关的质量规则和检测任务将停止运行。`,
    '移除确认',
    {
      type: 'warning',
      confirmButtonText: '确定移除',
      cancelButtonText: '取消'
    }
  ).then(() => {
    allData.value = allData.value.filter(d => d.id !== row.id)
    ElMessage.success('数据源已移除')
    loadData()
  }).catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.ds-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ds-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}

.ds-icon.MySQL { background: var(--dp-primary-light); color: var(--dp-primary); }
.ds-icon.Oracle { background: var(--dp-warning-light); color: var(--dp-warning); }
.ds-icon.PostgreSQL { background: var(--dp-success-light); color: var(--dp-success); }
.ds-icon.Hive { background: var(--dp-purple-light); color: var(--dp-purple); }
.ds-icon.Kafka { background: var(--dp-danger-light); color: var(--dp-danger); }
.ds-icon.ClickHouse { background: var(--dp-cyan-light); color: var(--dp-cyan); }
.ds-icon.API { background: #f2f3f5; color: var(--dp-text-2); }

.ds-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
}

.ds-status i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.ds-status.运行中 { color: var(--dp-success); }
.ds-status.运行中 i { background: var(--dp-success); box-shadow: 0 0 6px var(--dp-success); }
.ds-status.已停用 { color: var(--dp-text-3); }
.ds-status.已停用 i { background: var(--dp-text-3); }
.ds-status.异常 { color: var(--dp-danger); }
.ds-status.异常 i { background: var(--dp-danger); box-shadow: 0 0 6px var(--dp-danger); animation: blink 1.5s infinite; }

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
