<template>
  <div class="dp-page">
    <PageHeader title="质量策略配置" desc="将规则组合为质检策略，按场景批量应用到多张表，支持灵活调度与告警配置。">
      <el-button type="primary" :icon="Plus" @click="openEdit()">新建策略</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索策略名称" clearable style="width: 220px" :prefix-icon="Search" @input="loadData" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="loadData">
          <el-option label="启用" value="启用" />
          <el-option label="停用" value="停用" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="name" label="策略名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="strategy-name">
              <el-icon class="strategy-icon"><Setting /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="ruleCount" label="关联规则数" width="110" align="center">
          <template #default="{ row }">
            <span class="rule-count">{{ row.ruleCount }}</span>
            <span style="color: var(--dp-text-3); font-size: 12px"> 条</span>
          </template>
        </el-table-column>
        <el-table-column prop="cronExpr" label="执行周期" width="180">
          <template #default="{ row }">
            <div class="cron-display">
              <el-icon><Clock /></el-icon>
              <span class="dp-mono" style="font-size: 12px">{{ row.cronExpr }}</span>
            </div>
            <div class="cron-desc">{{ row.cronDesc }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="execMode" label="执行方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.execMode === '串行' ? 'primary' : 'success'" size="small" effect="plain">{{ row.execMode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <span class="ds-status" :class="row.status">
              <i></i>{{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="lastExecTime" label="最近执行时间" width="160" />
        <el-table-column prop="passRate" label="平均合格率" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.passRate" :stroke-width="5" :color="getPassRateColor(row.passRate)" />
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="execNow(row)">立即执行</el-button>
            <el-button link type="primary" size="small" @click="viewRecords(row)">执行记录</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="toggleStatus(row)">{{ row.status === '启用' ? '停用' : '启用' }}</el-button>
            <el-button link type="danger" size="small" @click="removeStrategy(row)">删除</el-button>
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

    <!-- 新建/编辑策略弹窗 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑策略' : '新建策略'" width="760px" destroy-on-close top="5vh">
      <el-tabs v-model="activeTab" class="strategy-tabs">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="form" label-width="100px">
            <el-form-item label="策略名称" required>
              <el-input v-model="form.name" placeholder="请输入策略名称，如：交易数据质量策略" />
            </el-form-item>
            <el-form-item label="策略描述">
              <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="请描述策略的用途和适用场景" />
            </el-form-item>
            <el-form-item label="负责人" required>
              <el-select v-model="form.owner" placeholder="请选择负责人" style="width: 100%">
                <el-option v-for="o in ownerOptions" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
          </el-form>
          <div class="tab-footer">
            <el-button type="primary" @click="activeTab = 'rules'">下一步</el-button>
          </div>
        </el-tab-pane>

        <!-- 规则选择 -->
        <el-tab-pane label="规则选择" name="rules">
          <div class="rule-select-header">
            <el-input v-model="ruleSearch" placeholder="搜索规则" clearable style="width: 240px" :prefix-icon="Search" size="small" />
            <span style="color: var(--dp-text-3); font-size: 12px">
              已选择 <span style="color: var(--dp-primary); font-weight: 600">{{ selectedRuleCount }}</span> 条规则
            </span>
          </div>
          <div class="rule-select-tree">
            <el-tree
              ref="ruleTreeRef"
              :data="ruleTreeData"
              show-checkbox
              node-key="id"
              :props="{ label: 'name', children: 'children' }"
              :default-expanded-keys="['完整性', '准确性', '一致性']"
              :filter-node-method="filterRuleNode"
              style="height: 340px; overflow-y: auto; border: 1px solid var(--dp-border); border-radius: 8px; padding: 8px;"
            />
          </div>
          <div class="tab-footer">
            <el-button @click="activeTab = 'basic'">上一步</el-button>
            <el-button type="primary" @click="activeTab = 'schedule'">下一步</el-button>
          </div>
        </el-tab-pane>

        <!-- 调度配置 -->
        <el-tab-pane label="调度配置" name="schedule">
          <el-form :model="form" label-width="100px">
            <el-form-item label="执行周期" required>
              <el-radio-group v-model="form.scheduleType">
                <el-radio value="daily">每日</el-radio>
                <el-radio value="weekly">每周</el-radio>
                <el-radio value="monthly">每月</el-radio>
                <el-radio value="cron">Cron 表达式</el-radio>
              </el-radio-group>
            </el-form-item>

            <template v-if="form.scheduleType === 'daily'">
              <el-form-item label="执行时间">
                <el-time-picker v-model="form.execTime" placeholder="选择执行时间" format="HH:mm" value-format="HH:mm" />
              </el-form-item>
            </template>

            <template v-if="form.scheduleType === 'weekly'">
              <el-form-item label="执行星期">
                <el-checkbox-group v-model="form.weekDays">
                  <el-checkbox v-for="d in ['周一', '周二', '周三', '周四', '周五', '周六', '周日']" :key="d" :label="d">{{ d }}</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="执行时间">
                <el-time-picker v-model="form.execTime" placeholder="选择执行时间" format="HH:mm" value-format="HH:mm" />
              </el-form-item>
            </template>

            <template v-if="form.scheduleType === 'monthly'">
              <el-form-item label="执行日期">
                <el-select v-model="form.monthDays" multiple placeholder="选择每月几号执行" style="width: 100%">
                  <el-option v-for="d in 31" :key="d" :label="d + '号'" :value="d" />
                </el-select>
              </el-form-item>
              <el-form-item label="执行时间">
                <el-time-picker v-model="form.execTime" placeholder="选择执行时间" format="HH:mm" value-format="HH:mm" />
              </el-form-item>
            </template>

            <template v-if="form.scheduleType === 'cron'">
              <el-form-item label="Cron 表达式" required>
                <el-input v-model="form.cronExpr" placeholder="0 0 1 * * ?" class="dp-mono" />
              </el-form-item>
              <el-form-item label="常用表达式">
                <div style="display: flex; flex-wrap: wrap; gap: 8px;">
                  <el-tag v-for="c in commonCron" :key="c.expr" style="cursor: pointer" effect="plain" @click="form.cronExpr = c.expr">
                    {{ c.name }}
                  </el-tag>
                </div>
              </el-form-item>
            </template>
          </el-form>
          <div class="tab-footer">
            <el-button @click="activeTab = 'rules'">上一步</el-button>
            <el-button type="primary" @click="activeTab = 'exec'">下一步</el-button>
          </div>
        </el-tab-pane>

        <!-- 执行配置 -->
        <el-tab-pane label="执行配置" name="exec">
          <el-form :model="form" label-width="100px">
            <el-form-item label="执行方式">
              <el-radio-group v-model="form.execMode">
                <el-radio value="串行">串行执行（按顺序逐条执行）</el-radio>
                <el-radio value="并行">并行执行（同时执行所有规则）</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="失败处理策略">
              <el-radio-group v-model="form.failStrategy">
                <el-radio value="continue">继续执行后续规则</el-radio>
                <el-radio value="abort">中止执行并告警</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="超时时间">
              <el-input-number v-model="form.timeout" :min="1" :max="1440" :step="10" />
              <span style="margin-left: 8px; color: var(--dp-text-2)">分钟（单条规则超时时间）</span>
            </el-form-item>
            <el-form-item label="最大重试次数">
              <el-input-number v-model="form.retryCount" :min="0" :max="5" />
              <span style="margin-left: 8px; color: var(--dp-text-2)">次（规则执行失败后的重试次数）</span>
            </el-form-item>
            <el-form-item label="失败重试间隔">
              <el-input-number v-model="form.retryInterval" :min="1" :max="60" />
              <span style="margin-left: 8px; color: var(--dp-text-2)">分钟</span>
            </el-form-item>
          </el-form>
          <div class="tab-footer">
            <el-button @click="activeTab = 'schedule'">上一步</el-button>
            <el-button type="primary" @click="activeTab = 'alert'">下一步</el-button>
          </div>
        </el-tab-pane>

        <!-- 告警配置 -->
        <el-tab-pane label="告警配置" name="alert">
          <el-form :model="form" label-width="120px">
            <el-form-item label="执行失败告警">
              <el-switch v-model="form.enableFailAlert" />
            </el-form-item>
            <template v-if="form.enableFailAlert">
              <el-form-item label="告警方式">
                <el-checkbox-group v-model="form.failAlertMethods">
                  <el-checkbox value="message">站内消息</el-checkbox>
                  <el-checkbox value="email">邮件通知</el-checkbox>
                  <el-checkbox value="sms">短信通知</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="告警接收人">
                <el-select v-model="form.failAlertReceivers" multiple style="width: 100%" placeholder="请选择接收人">
                  <el-option v-for="o in ownerOptions" :key="o" :label="o" :value="o" />
                </el-select>
              </el-form-item>
            </template>

            <el-divider />

            <el-form-item label="质量下降告警">
              <el-switch v-model="form.enableQualityAlert" />
            </el-form-item>
            <template v-if="form.enableQualityAlert">
              <el-form-item label="下降阈值">
                <el-input-number v-model="form.qualityDropThreshold" :min="1" :max="50" :step="1" />
                <span style="margin-left: 8px; color: var(--dp-text-2)">%（合格率下降超过此值时告警）</span>
              </el-form-item>
              <el-form-item label="告警方式">
                <el-checkbox-group v-model="form.qualityAlertMethods">
                  <el-checkbox value="message">站内消息</el-checkbox>
                  <el-checkbox value="email">邮件通知</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="告警接收人">
                <el-select v-model="form.qualityAlertReceivers" multiple style="width: 100%" placeholder="请选择接收人">
                  <el-option v-for="o in ownerOptions" :key="o" :label="o" :value="o" />
                </el-select>
              </el-form-item>
            </template>
          </el-form>
          <div class="tab-footer">
            <el-button @click="activeTab = 'exec'">上一步</el-button>
            <el-button type="primary" @click="saveStrategy">保存策略</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 执行记录弹窗 -->
    <el-dialog v-model="recordsVisible" title="执行记录" width="700px" destroy-on-close>
      <el-table :data="recordData" border stripe size="small">
        <el-table-column prop="execTime" label="执行时间" width="160" />
        <el-table-column prop="trigger" label="触发方式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.trigger === '定时' ? 'primary' : 'success'" size="small" effect="plain">{{ row.trigger }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ruleCount" label="规则数" width="80" align="center" />
        <el-table-column prop="duration" label="耗时" width="80" align="center" />
        <el-table-column prop="passRate" label="合格率" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.passRate" :stroke-width="4" :color="getPassRateColor(row.passRate)" />
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '成功' ? 'success' : row.status === '失败' ? 'danger' : 'warning'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const editVisible = ref(false)
const recordsVisible = ref(false)
const activeTab = ref('basic')
const ruleTreeRef = ref(null)
const ruleSearch = ref('')

const ownerOptions = ['张三', '李四', '王五', '赵六', '钱七']

const commonCron = [
  { name: '每天凌晨1点', expr: '0 0 1 * * ?' },
  { name: '每天凌晨2点', expr: '0 0 2 * * ?' },
  { name: '每小时', expr: '0 0 * * * ?' },
  { name: '每周一凌晨3点', expr: '0 0 3 ? * MON' },
  { name: '每月1号凌晨1点', expr: '0 0 1 1 * ?' },
  { name: '每5分钟', expr: '0 0/5 * * * ?' }
]

const query = reactive({
  keyword: '',
  status: '',
  page: 1,
  size: 10
})

const total = ref(0)
const tableData = ref([])
const recordData = ref([])

// Mock 数据
const allData = ref([
  { id: 1, name: '交易数据质量策略', ruleCount: 28, cronExpr: '0 0 1 * * ?', cronDesc: '每天 01:00 执行', execMode: '串行', status: '启用', lastExecTime: '2026-09-26 01:00:12', passRate: 97.2, owner: '张三' },
  { id: 2, name: '客户数据质量策略', ruleCount: 35, cronExpr: '0 0 2 * * ?', cronDesc: '每天 02:00 执行', execMode: '并行', status: '启用', lastExecTime: '2026-09-26 02:00:30', passRate: 95.8, owner: '李四' },
  { id: 3, name: '财务数据质量策略', ruleCount: 22, cronExpr: '0 30 3 * * ?', cronDesc: '每天 03:30 执行', execMode: '串行', status: '启用', lastExecTime: '2026-09-26 03:30:45', passRate: 94.3, owner: '王五' },
  { id: 4, name: '日志数据质量策略', ruleCount: 12, cronExpr: '0 0 */2 * * ?', cronDesc: '每 2 小时执行', execMode: '并行', status: '启用', lastExecTime: '2026-09-26 08:00:00', passRate: 98.5, owner: '赵六' },
  { id: 5, name: '商品数据质量策略', ruleCount: 18, cronExpr: '0 0 4 * * MON', cronDesc: '每周一 04:00 执行', execMode: '串行', status: '启用', lastExecTime: '2026-09-22 04:00:22', passRate: 96.7, owner: '钱七' },
  { id: 6, name: '会员质量监控策略', ruleCount: 15, cronExpr: '0 0/30 * * * ?', cronDesc: '每 30 分钟执行', execMode: '并行', status: '停用', lastExecTime: '2026-09-25 18:30:00', passRate: 97.8, owner: '张三' },
  { id: 7, name: '营销活动质量策略', ruleCount: 10, cronExpr: '0 0 5 * * ?', cronDesc: '每天 05:00 执行', execMode: '串行', status: '启用', lastExecTime: '2026-09-26 05:00:10', passRate: 99.1, owner: '李四' },
  { id: 8, name: '供应链质量策略', ruleCount: 20, cronExpr: '0 0 6 * * ?', cronDesc: '每天 06:00 执行', execMode: '串行', status: '启用', lastExecTime: '2026-09-26 06:00:35', passRate: 96.1, owner: '王五' }
])

// 规则树数据
const ruleTreeData = ref([
  {
    id: '完整性',
    name: '完整性规则',
    children: [
      { id: 101, name: '订单金额非空校验' },
      { id: 102, name: '用户手机号非空校验' },
      { id: 103, name: '商品编码非空校验' },
      { id: 104, name: '创建时间非空校验' },
      { id: 105, name: '用户邮箱非空校验' }
    ]
  },
  {
    id: '准确性',
    name: '准确性规则',
    children: [
      { id: 201, name: '用户手机号格式校验' },
      { id: 202, name: '支付金额范围校验' },
      { id: 203, name: '会员等级值域校验' },
      { id: 204, name: '商品编码格式校验' },
      { id: 205, name: '退款金额合理性校验' },
      { id: 206, name: '邮箱格式校验' }
    ]
  },
  {
    id: '一致性',
    name: '一致性规则',
    children: [
      { id: 301, name: '客户ID参照完整性' },
      { id: 302, name: '销售金额汇总一致性' },
      { id: 303, name: '订单状态流转一致性' },
      { id: 304, name: '库存数量一致性' }
    ]
  },
  {
    id: '唯一性',
    name: '唯一性规则',
    children: [
      { id: 401, name: '订单号唯一性校验' },
      { id: 402, name: '用户ID唯一性校验' },
      { id: 403, name: '商品编码唯一性校验' }
    ]
  },
  {
    id: '时效性',
    name: '时效性规则',
    children: [
      { id: 501, name: '数据延迟及时性校验' },
      { id: 502, name: '日志上报及时性校验' }
    ]
  },
  {
    id: '有效性',
    name: '有效性规则',
    children: [
      { id: 601, name: '订单状态枚举校验' },
      { id: 602, name: '支付渠道枚举校验' },
      { id: 603, name: '性别代码有效性校验' }
    ]
  }
])

const selectedRuleCount = computed(() => {
  if (!ruleTreeRef.value) return 0
  const checked = ruleTreeRef.value.getCheckedNodes()
  return checked.filter(n => !n.children || n.children.length === 0).length
})

const form = reactive({
  id: null,
  name: '',
  desc: '',
  owner: '',
  scheduleType: 'daily',
  execTime: '01:00',
  weekDays: ['周一', '周三', '周五'],
  monthDays: [1, 15],
  cronExpr: '0 0 1 * * ?',
  execMode: '串行',
  failStrategy: 'continue',
  timeout: 30,
  retryCount: 1,
  retryInterval: 5,
  enableFailAlert: true,
  failAlertMethods: ['message', 'email'],
  failAlertReceivers: [],
  enableQualityAlert: true,
  qualityDropThreshold: 5,
  qualityAlertMethods: ['message'],
  qualityAlertReceivers: []
})

function getPassRateColor(rate) {
  if (rate >= 95) return '#00b42a'
  if (rate >= 90) return '#ff7d00'
  return '#f53f3f'
}

function loadData() {
  loading.value = true
  setTimeout(() => {
    let list = allData.value.filter(s =>
      (!query.keyword || s.name.includes(query.keyword)) &&
      (!query.status || s.status === query.status)
    )
    total.value = list.length
    const start = (query.page - 1) * query.size
    tableData.value = list.slice(start, start + query.size)
    loading.value = false
  }, 300)
}

watch(ruleSearch, (val) => {
  ruleTreeRef.value?.filter(val)
})

function filterRuleNode(value, data) {
  if (!value) return true
  return data.name.includes(value)
}

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      desc: '',
      owner: row.owner,
      scheduleType: 'daily',
      execTime: '01:00',
      weekDays: ['周一', '周三', '周五'],
      monthDays: [1, 15],
      cronExpr: row.cronExpr,
      execMode: row.execMode,
      failStrategy: 'continue',
      timeout: 30,
      retryCount: 1,
      retryInterval: 5,
      enableFailAlert: true,
      failAlertMethods: ['message', 'email'],
      failAlertReceivers: [row.owner],
      enableQualityAlert: true,
      qualityDropThreshold: 5,
      qualityAlertMethods: ['message'],
      qualityAlertReceivers: [row.owner]
    })
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      desc: '',
      owner: '',
      scheduleType: 'daily',
      execTime: '01:00',
      weekDays: ['周一', '周三', '周五'],
      monthDays: [1, 15],
      cronExpr: '0 0 1 * * ?',
      execMode: '串行',
      failStrategy: 'continue',
      timeout: 30,
      retryCount: 1,
      retryInterval: 5,
      enableFailAlert: true,
      failAlertMethods: ['message'],
      failAlertReceivers: [],
      enableQualityAlert: true,
      qualityDropThreshold: 5,
      qualityAlertMethods: ['message'],
      qualityAlertReceivers: []
    })
  }
  activeTab.value = 'basic'
  editVisible.value = true
}

function saveStrategy() {
  if (!form.name) return ElMessage.warning('请输入策略名称')
  if (!form.owner) return ElMessage.warning('请选择负责人')

  const cronInfo = getCronInfo()

  if (form.id) {
    const idx = allData.value.findIndex(s => s.id === form.id)
    if (idx !== -1) {
      allData.value[idx] = {
        ...allData.value[idx],
        name: form.name,
        execMode: form.execMode,
        cronExpr: cronInfo.expr,
        cronDesc: cronInfo.desc,
        owner: form.owner
      }
    }
    ElMessage.success('策略已更新')
  } else {
    allData.value.unshift({
      id: Date.now(),
      name: form.name,
      ruleCount: selectedRuleCount.value,
      cronExpr: cronInfo.expr,
      cronDesc: cronInfo.desc,
      execMode: form.execMode,
      status: '启用',
      lastExecTime: '未执行',
      passRate: 100,
      owner: form.owner
    })
    ElMessage.success('策略创建成功')
  }
  editVisible.value = false
  loadData()
}

function getCronInfo() {
  switch (form.scheduleType) {
    case 'daily':
      const [h, m] = (form.execTime || '01:00').split(':')
      return { expr: `0 ${m} ${h} * * ?`, desc: `每天 ${form.execTime} 执行` }
    case 'weekly':
      const [wh, wm] = (form.execTime || '01:00').split(':')
      const days = form.weekDays.join('、') || '周一'
      return { expr: `0 ${wm} ${wh} ? * MON-FRI`, desc: `${days} ${form.execTime} 执行` }
    case 'monthly':
      const [mh, mm] = (form.execTime || '01:00').split(':')
      const md = form.monthDays.join(',') || '1'
      return { expr: `0 ${mm} ${mh} ${md} * ?`, desc: `每月 ${md} 号 ${form.execTime} 执行` }
    case 'cron':
    default:
      return { expr: form.cronExpr || '0 0 1 * * ?', desc: '自定义 Cron 表达式' }
  }
}

function toggleStatus(row) {
  row.status = row.status === '启用' ? '停用' : '启用'
  ElMessage.success(`策略「${row.name}」已${row.status}`)
}

function execNow(row) {
  ElMessage.info(`策略「${row.name}」开始执行...`)
  setTimeout(() => {
    row.lastExecTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
    ElMessage.success(`策略「${row.name}」执行完成`)
  }, 2000)
}

function viewRecords(row) {
  recordData.value = Array.from({ length: 10 }, (_, i) => {
    const passRate = +(93 + Math.random() * 7).toFixed(1)
    const d = new Date()
    d.setDate(d.getDate() - i)
    return {
      execTime: d.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
      trigger: i % 3 === 0 ? '手动' : '定时',
      ruleCount: row.ruleCount,
      duration: `${Math.floor(Math.random() * 30) + 5}m${Math.floor(Math.random() * 60)}s`,
      passRate,
      status: passRate > 92 ? '成功' : '失败'
    }
  })
  recordsVisible.value = true
}

function removeStrategy(row) {
  ElMessageBox.confirm(
    `确定删除策略「${row.name}」吗？删除后相关的调度任务将被取消。`,
    '删除确认',
    { type: 'warning' }
  ).then(() => {
    allData.value = allData.value.filter(s => s.id !== row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.strategy-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.strategy-icon {
  color: var(--dp-primary);
  font-size: 16px;
}

.rule-count {
  font-weight: 600;
  color: var(--dp-text-1);
  font-size: 14px;
}

.cron-display {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--dp-text-2);
}

.cron-desc {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

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

.ds-status.启用 { color: var(--dp-success); }
.ds-status.启用 i { background: var(--dp-success); box-shadow: 0 0 6px var(--dp-success); }
.ds-status.停用 { color: var(--dp-text-3); }
.ds-status.停用 i { background: var(--dp-text-3); }

.strategy-tabs {
  margin: -20px -20px 0;
}

.strategy-tabs :deep(.el-tabs__header) {
  margin: 0 0 20px 0;
  padding: 0 20px;
}

.rule-select-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.rule-select-tree {
  margin-bottom: 16px;
}

.tab-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--dp-border-light);
}
</style>
