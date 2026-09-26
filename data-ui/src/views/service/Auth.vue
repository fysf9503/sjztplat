<template>
  <div class="dp-page auth-page">
    <PageHeader title="授权管理" desc="管理数据服务的授权关系，支持授权申请、审批、续期和撤销操作">
      <el-button type="primary" :icon="Plus" @click="openApply()">新增授权</el-button>
    </PageHeader>

    <!-- 统计概览 -->
    <div class="auth-stats">
      <div class="auth-stat-item">
        <div class="stat-icon primary"><el-icon :size="18"><Key /></el-icon></div>
        <div class="stat-content">
          <div class="stat-num">{{ totalAuth }}</div>
          <div class="stat-label">授权总数</div>
        </div>
      </div>
      <div class="auth-stat-item">
        <div class="stat-icon success"><el-icon :size="18"><CircleCheck /></el-icon></div>
        <div class="stat-content">
          <div class="stat-num">{{ validAuth }}</div>
          <div class="stat-label">有效授权</div>
        </div>
      </div>
      <div class="auth-stat-item">
        <div class="stat-icon warning"><el-icon :size="18"><Clock /></el-icon></div>
        <div class="stat-content">
          <div class="stat-num">{{ pendingAuth }}</div>
          <div class="stat-label">待审批</div>
        </div>
      </div>
      <div class="auth-stat-item">
        <div class="stat-icon danger"><el-icon :size="18"><Warning /></el-icon></div>
        <div class="stat-content">
          <div class="stat-num">{{ expiredAuth }}</div>
          <div class="stat-label">已过期</div>
        </div>
      </div>
    </div>

    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索应用 / API / 申请人"
          clearable
          style="width: 260px"
          :prefix-icon="Search"
          @input="load"
        />
        <el-select v-model="query.app" placeholder="应用名称" clearable style="width: 150px" @change="load">
          <el-option v-for="a in appOptions" :key="a" :label="a" :value="a" />
        </el-select>
        <el-select v-model="query.status" placeholder="授权状态" clearable style="width: 130px" @change="load">
          <el-option label="待审批" value="待审批" />
          <el-option label="已通过" value="已通过" />
          <el-option label="已拒绝" value="已拒绝" />
          <el-option label="已过期" value="已过期" />
        </el-select>
        <el-select v-model="query.authType" placeholder="授权方式" clearable style="width: 140px" @change="load">
          <el-option label="密钥认证" value="密钥认证" />
          <el-option label="Token认证" value="Token认证" />
          <el-option label="OAuth 2.0" value="OAuth 2.0" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="refresh">刷新</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="appName" label="应用名称" min-width="140">
          <template #default="{ row }">
            <div class="app-cell">
              <div class="app-icon" :class="appIconClass(row.appName)">
                {{ row.appName.charAt(0) }}
              </div>
              <span>{{ row.appName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="service" label="API 服务" min-width="160" class-name="dp-mono" />
        <el-table-column prop="authType" label="授权方式" width="120">
          <template #default="{ row }">
            <el-tag :type="authTypeTag(row.authType)" size="small" effect="plain">{{ row.authType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicant" label="申请人" width="90" />
        <el-table-column prop="callsTotal" label="调用次数" width="110" sortable>
          <template #default="{ row }">{{ (row.callsTotal || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="expire" label="有效期至" width="120">
          <template #default="{ row }">
            <span :class="{ 'expire-soon': isExpireSoon(row.expire) }">{{ row.expire }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="status-dot" :class="statusClass(row.status)">
              <span class="dot"></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status === '待审批'" link type="success" size="small" @click="approve(row)">通过</el-button>
            <el-button v-if="row.status === '待审批'" link type="danger" size="small" @click="reject(row)">拒绝</el-button>
            <el-button v-if="row.status === '已通过'" link type="primary" size="small" @click="extend(row)">续期</el-button>
            <el-button v-if="row.status === '已通过'" link type="danger" size="small" @click="revoke(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @current-change="load"
      />
    </div>

    <!-- 新增授权弹窗 -->
    <el-dialog v-model="applyVisible" title="新增授权" width="560px" destroy-on-close>
      <el-form :model="applyForm" label-width="110px" ref="formRef" :rules="rules">
        <el-form-item label="应用名称" prop="appName">
          <el-select v-model="applyForm.appName" style="width: 100%" filterable placeholder="选择应用">
            <el-option v-for="a in appOptions" :key="a" :label="a" :value="a" />
          </el-select>
        </el-form-item>
        <el-form-item label="API 服务" prop="service">
          <el-select v-model="applyForm.service" style="width: 100%" filterable placeholder="选择 API 服务">
            <el-option v-for="s in serviceOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="授权方式">
              <el-select v-model="applyForm.authType" style="width: 100%">
                <el-option label="密钥认证" value="密钥认证" />
                <el-option label="Token认证" value="Token认证" />
                <el-option label="OAuth 2.0" value="OAuth 2.0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有效期至" prop="expire">
              <el-date-picker v-model="applyForm.expire" type="date" style="width: 100%" placeholder="选择有效期" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="申请事由" prop="reason">
          <el-input v-model="applyForm.reason" type="textarea" :rows="3" placeholder="请详细说明使用目的和场景" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 授权详情抽屉 -->
    <el-drawer v-model="detailVisible" title="授权详情" size="500px" :destroy-on-close="true">
      <div v-if="currentRow" class="auth-detail">
        <div class="detail-status">
          <el-tag :type="statusTagType(currentRow.status)" size="large">{{ currentRow.status }}</el-tag>
        </div>

        <el-descriptions :column="1" border size="default">
          <el-descriptions-item label="应用名称">{{ currentRow.appName }}</el-descriptions-item>
          <el-descriptions-item label="API 服务">{{ currentRow.service }}</el-descriptions-item>
          <el-descriptions-item label="授权方式">{{ currentRow.authType }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentRow.applicant }}（{{ currentRow.dept }}）</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ currentRow.applyTime }}</el-descriptions-item>
          <el-descriptions-item label="有效期至">{{ currentRow.expire }}</el-descriptions-item>
          <el-descriptions-item label="累计调用">{{ (currentRow.callsTotal || 0).toLocaleString() }} 次</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <div class="detail-section-title">申请事由</div>
          <div class="detail-text">{{ currentRow.reason }}</div>
        </div>

        <div class="detail-section">
          <div class="detail-section-title">操作记录</div>
          <div class="timeline">
            <div class="timeline-item">
              <div class="timeline-dot"></div>
              <div class="timeline-content">
                <div class="timeline-title">提交申请</div>
                <div class="timeline-desc">{{ currentRow.applicant }} 提交授权申请</div>
                <div class="timeline-time">{{ currentRow.applyTime }}</div>
              </div>
            </div>
            <div v-if="currentRow.status !== '待审批'" class="timeline-item">
              <div class="timeline-dot" :class="currentRow.status === '已通过' ? 'success' : 'danger'"></div>
              <div class="timeline-content">
                <div class="timeline-title">{{ currentRow.status === '已通过' ? '审批通过' : '审批拒绝' }}</div>
                <div class="timeline-desc">管理员 {{ currentRow.status === '已通过' ? '已批准' : '已拒绝' }} 该授权申请</div>
                <div class="timeline-time">{{ currentRow.applyTime }}</div>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-actions">
          <el-button v-if="currentRow.status === '待审批'" type="success" @click="approve(currentRow)">通过</el-button>
          <el-button v-if="currentRow.status === '待审批'" type="danger" @click="reject(currentRow)">拒绝</el-button>
          <el-button v-if="currentRow.status === '已通过'" type="primary" @click="extend(currentRow)">续期</el-button>
          <el-button v-if="currentRow.status === '已通过'" type="danger" @click="revoke(currentRow)">撤销授权</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { authRequests, serviceApps, services } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const applyVisible = ref(false)
const detailVisible = ref(false)
const formRef = ref(null)
const currentRow = ref({})

const all = ref([])
const rows = ref([])
const total = ref(0)

const appOptions = computed(() => serviceApps.map(a => a.name))
const serviceOptions = computed(() => services.map(s => s.name))

const query = reactive({
  keyword: '',
  app: '',
  status: '',
  authType: '',
  page: 1,
  size: 10
})

const applyForm = reactive({
  appName: '',
  service: '',
  authType: '密钥认证',
  expire: '',
  reason: ''
})

const rules = {
  appName: [{ required: true, message: '请选择应用', trigger: 'change' }],
  service: [{ required: true, message: '请选择 API 服务', trigger: 'change' }],
  expire: [{ required: true, message: '请选择有效期', trigger: 'change' }],
  reason: [{ required: true, message: '请填写申请事由', trigger: 'blur' }]
}

const totalAuth = computed(() => all.value.length)
const validAuth = computed(() => all.value.filter(a => a.status === '已通过').length)
const pendingAuth = computed(() => all.value.filter(a => a.status === '待审批').length)
const expiredAuth = computed(() => all.value.filter(a => a.status === '已过期').length)

function authTypeTag(type) {
  const map = { '密钥认证': 'primary', 'Token认证': 'success', 'OAuth 2.0': 'warning' }
  return map[type] || 'info'
}

function statusTagType(status) {
  const map = { '待审批': 'warning', '已通过': 'success', '已拒绝': 'danger', '已过期': 'info' }
  return map[status] || 'info'
}

function statusClass(status) {
  const map = { '待审批': 'pending', '已通过': 'ok', '已拒绝': 'rejected', '已过期': 'expired' }
  return map[status] || 'pending'
}

function appIconClass(name) {
  const colors = ['blue', 'green', 'orange', 'purple', 'cyan']
  const index = name.charCodeAt(0) % colors.length
  return colors[index]
}

function isExpireSoon(dateStr) {
  if (!dateStr) return false
  const expire = new Date(dateStr)
  const now = new Date()
  const diff = expire - now
  return diff > 0 && diff < 7 * 24 * 60 * 60 * 1000
}

function load() {
  loading.value = true
  setTimeout(() => {
    let list = all.value.filter(d => {
      const matchKw = !query.keyword ||
        d.appName.includes(query.keyword) ||
        d.service.includes(query.keyword) ||
        d.applicant.includes(query.keyword)
      const matchApp = !query.app || d.appName === query.app
      const matchStatus = !query.status || d.status === query.status
      const matchType = !query.authType || d.authType === query.authType
      return matchKw && matchApp && matchStatus && matchType
    })
    total.value = list.length
    rows.value = list.slice((query.page - 1) * query.size, query.page * query.size)
    loading.value = false
  }, 300)
}

function refresh() {
  query.page = 1
  load()
  ElMessage.success('刷新成功')
}

function openApply() {
  Object.assign(applyForm, {
    appName: '',
    service: '',
    authType: '密钥认证',
    expire: '',
    reason: ''
  })
  applyVisible.value = true
}

function submitApply() {
  formRef.value?.validate(valid => {
    if (!valid) return
    all.value.unshift({
      id: Date.now(),
      appName: applyForm.appName,
      service: applyForm.service,
      authType: applyForm.authType,
      applicant: '张伟',
      dept: '数据平台部',
      reason: applyForm.reason,
      expire: applyForm.expire || '2025-12-31',
      status: '待审批',
      applyTime: '刚刚',
      callsTotal: 0
    })
    applyVisible.value = false
    ElMessage.success('授权申请已提交，等待审批')
    load()
  })
}

function viewDetail(row) {
  currentRow.value = row
  detailVisible.value = true
}

function approve(row) {
  ElMessageBox.confirm(
    `确定通过「${row.appName}」对「${row.service}」的授权申请吗？`,
    '审批确认',
    { type: 'success', confirmButtonText: '确认通过' }
  ).then(() => {
    const i = all.value.findIndex(d => d.id === row.id)
    all.value[i].status = '已通过'
    ElMessage.success('授权已通过')
    load()
    if (detailVisible.value) detailVisible.value = false
  }).catch(() => {})
}

function reject(row) {
  ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', {
    confirmButtonText: '确认拒绝',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputPlaceholder: '请说明拒绝原因...',
    type: 'warning'
  }).then(() => {
    const i = all.value.findIndex(d => d.id === row.id)
    all.value[i].status = '已拒绝'
    ElMessage.success('已拒绝申请')
    load()
    if (detailVisible.value) detailVisible.value = false
  }).catch(() => {})
}

function extend(row) {
  ElMessageBox.confirm(
    `确定为「${row.appName}」的「${row.service}」授权续期 90 天吗？`,
    '续期确认',
    { type: 'warning' }
  ).then(() => {
    ElMessage.success('授权已续期 90 天')
  }).catch(() => {})
}

function revoke(row) {
  ElMessageBox.confirm(
    `确定撤销「${row.appName}」对「${row.service}」的授权吗？撤销后应用将无法调用该服务。`,
    '撤销确认',
    { type: 'warning', confirmButtonText: '确认撤销' }
  ).then(() => {
    const i = all.value.findIndex(d => d.id === row.id)
    all.value[i].status = '已拒绝'
    ElMessage.success('授权已撤销')
    load()
  }).catch(() => {})
}

onMounted(() => {
  const authTypes = ['密钥认证', 'Token认证', 'OAuth 2.0']
  all.value = authRequests.map((r, i) => ({
    ...r,
    appName: r.appName,
    authType: authTypes[i % authTypes.length],
    callsTotal: Math.floor(Math.random() * 50000) + 1000
  }))
  load()
})
</script>

<style scoped>
.auth-page {
  min-height: 100%;
}

/* 统计卡片 */
.auth-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.auth-stat-item {
  background: #fff;
  border-radius: var(--dp-radius);
  padding: 16px 18px;
  border: 1px solid var(--dp-border-light);
  box-shadow: var(--dp-shadow-sm);
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.25s;
}

.auth-stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon.primary { background: var(--dp-primary-light); color: var(--dp-primary); }
.stat-icon.success { background: var(--dp-success-light); color: var(--dp-success); }
.stat-icon.warning { background: var(--dp-warning-light); color: var(--dp-warning); }
.stat-icon.danger { background: var(--dp-danger-light); color: var(--dp-danger); }

.stat-content {
  flex: 1;
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--dp-text-1);
  line-height: 1.2;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}

.stat-label {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 4px;
}

/* 应用单元格 */
.app-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
}

.app-icon.blue { background: linear-gradient(135deg, #1664ff, #5b8def); }
.app-icon.green { background: linear-gradient(135deg, #00b42a, #3dd598); }
.app-icon.orange { background: linear-gradient(135deg, #ff7d00, #ffb366); }
.app-icon.purple { background: linear-gradient(135deg, #722ed1, #9e77ed); }
.app-icon.cyan { background: linear-gradient(135deg, #0fc6c2, #5ce1e6); }

.expire-soon {
  color: var(--dp-warning);
  font-weight: 500;
}

/* 状态圆点 */
.status-dot {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.status-dot .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.ok { color: var(--dp-success); }
.status-dot.ok .dot { background: var(--dp-success); box-shadow: 0 0 0 3px var(--dp-success-light); }

.status-dot.pending { color: var(--dp-warning); }
.status-dot.pending .dot { background: var(--dp-warning); box-shadow: 0 0 0 3px var(--dp-warning-light); animation: pulse 1.5s infinite; }

.status-dot.rejected { color: var(--dp-danger); }
.status-dot.rejected .dot { background: var(--dp-danger); box-shadow: 0 0 0 3px var(--dp-danger-light); }

.status-dot.expired { color: var(--dp-text-3); }
.status-dot.expired .dot { background: var(--dp-text-4); box-shadow: 0 0 0 3px var(--dp-border-light); }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 详情抽屉 */
.auth-detail {
  padding-right: 8px;
}

.detail-status {
  margin-bottom: 16px;
  text-align: center;
  padding: 16px;
  background: var(--dp-bg-page);
  border-radius: 8px;
}

.detail-section {
  margin-top: 20px;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 12px;
  padding-left: 10px;
  border-left: 3px solid var(--dp-primary);
}

.detail-text {
  font-size: 13px;
  color: var(--dp-text-2);
  line-height: 1.7;
  padding: 12px 14px;
  background: var(--dp-bg-page);
  border-radius: 6px;
}

/* 时间线 */
.timeline {
  padding-left: 8px;
}

.timeline-item {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  position: relative;
}

.timeline-item:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 16px;
  bottom: 0;
  width: 1px;
  background: var(--dp-border-light);
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--dp-primary);
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px var(--dp-primary-light);
  flex-shrink: 0;
  margin-top: 2px;
  z-index: 1;
}

.timeline-dot.success {
  background: var(--dp-success);
  box-shadow: 0 0 0 2px var(--dp-success-light);
}

.timeline-dot.danger {
  background: var(--dp-danger);
  box-shadow: 0 0 0 2px var(--dp-danger-light);
}

.timeline-content {
  flex: 1;
}

.timeline-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-1);
  margin-bottom: 4px;
}

.timeline-desc {
  font-size: 12.5px;
  color: var(--dp-text-3);
  margin-bottom: 4px;
}

.timeline-time {
  font-size: 11.5px;
  color: var(--dp-text-4);
}

.detail-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  padding-top: 20px;
  margin-top: 20px;
  border-top: 1px solid var(--dp-border-light);
}
</style>
