<template>
  <div class="dp-page apps-page">
    <PageHeader title="应用管理" desc="管理接入数据服务的业务应用，包括应用注册、密钥管理和调用配额配置">
      <el-button type="primary" :icon="Plus" @click="openEdit()">新建应用</el-button>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="app-stats">
      <div class="app-stat-card">
        <div class="stat-icon" style="background: var(--dp-primary-light); color: var(--dp-primary)">
          <el-icon :size="20"><Grid /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ totalApps }}</div>
          <div class="stat-label">应用总数</div>
        </div>
      </div>
      <div class="app-stat-card">
        <div class="stat-icon" style="background: var(--dp-success-light); color: var(--dp-success)">
          <el-icon :size="20"><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ activeApps }}</div>
          <div class="stat-label">正常运行</div>
        </div>
      </div>
      <div class="app-stat-card">
        <div class="stat-icon" style="background: var(--dp-purple-light); color: var(--dp-purple)">
          <el-icon :size="20"><Connection /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ totalSubscribed }}</div>
          <div class="stat-label">已订阅API</div>
        </div>
      </div>
      <div class="app-stat-card">
        <div class="stat-icon" style="background: var(--dp-warning-light); color: var(--dp-warning)">
          <el-icon :size="20"><DataLine /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ formatCalls(totalCalls) }}</div>
          <div class="stat-label">本月调用量</div>
        </div>
      </div>
    </div>

    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索应用名称 / AppKey"
          clearable
          style="width: 260px"
          :prefix-icon="Search"
          @input="load"
        />
        <el-select v-model="query.type" placeholder="应用类型" clearable style="width: 140px" @change="load">
          <el-option label="内部应用" value="内部应用" />
          <el-option label="外部应用" value="外部应用" />
          <el-option label="第三方合作" value="第三方合作" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
          <el-option label="正常" value="正常" />
          <el-option label="已禁用" value="已禁用" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="refresh">刷新</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="name" label="应用名称" min-width="160">
          <template #default="{ row }">
            <div class="app-name-cell">
              <div class="app-avatar" :class="typeClass(row.type)">
                {{ row.name.charAt(0) }}
              </div>
              <div>
                <div class="app-name">{{ row.name }}</div>
                <div class="app-desc">{{ row.desc || '暂无描述' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="appKey" label="AppKey" width="140" class-name="dp-mono">
          <template #default="{ row }">
            <span class="app-key">{{ row.appKey }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="应用类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="boundServices" label="已订阅API数" width="110" sortable>
          <template #default="{ row }">
            <span class="service-count">{{ row.boundServices }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="callsMonth" label="本月调用量" width="120" sortable>
          <template #default="{ row }">{{ formatCalls(row.callsMonth) }}</template>
        </el-table-column>
        <el-table-column prop="rateLimit" label="限流配额" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <span class="status-dot" :class="row.status === '正常' ? 'ok' : 'disabled'">
              <span class="dot"></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewSecret(row)">密钥管理</el-button>
            <el-button link type="primary" size="small" @click="viewPermission(row)">权限配置</el-button>
            <el-button link type="primary" size="small" @click="viewStats(row)">查看统计</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
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

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑应用' : '新增应用'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="110px" ref="formRef" :rules="rules">
        <el-form-item label="应用名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入应用名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="应用类型">
              <el-select v-model="form.type" style="width: 100%">
                <el-option label="内部应用" value="内部应用" />
                <el-option label="外部应用" value="外部应用" />
                <el-option label="第三方合作" value="第三方合作" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-select v-model="form.owner" filterable style="width: 100%">
                <el-option v-for="o in ownerOptions" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="限流配额 (QPS)">
              <el-input-number v-model="form.qpsNum" :min="1" :max="10000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="日调用限额">
              <el-input-number v-model="form.dailyLimit" :min="0" :max="10000000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="应用描述">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="请输入应用用途说明" />
        </el-form-item>
        <el-form-item label="回调地址">
          <el-input v-model="form.callbackUrl" placeholder="https://example.com/callback" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 密钥管理抽屉 -->
    <el-drawer v-model="secretVisible" title="密钥管理" size="480px" :destroy-on-close="true">
      <div v-if="currentApp" class="secret-manage">
        <div class="secret-section">
          <div class="section-title">应用凭证</div>
          <div class="secret-item">
            <div class="secret-label">AppKey</div>
            <div class="secret-value dp-mono">{{ currentApp.appKey }}</div>
            <el-button link type="primary" size="small" @click="copyText(currentApp.appKey)">复制</el-button>
          </div>
          <div class="secret-item">
            <div class="secret-label">AppSecret</div>
            <div class="secret-value dp-mono">{{ showSecret ? appSecret : '****************' }}</div>
            <el-button link type="primary" size="small" @click="toggleSecret">{{ showSecret ? '隐藏' : '显示' }}</el-button>
            <el-button link type="primary" size="small" @click="copyText(appSecret)">复制</el-button>
          </div>
          <el-alert
            type="warning"
            :closable="false"
            show-icon
            title="请妥善保管密钥，泄露将导致数据安全风险"
            style="margin-top: 12px"
          />
        </div>

        <div class="secret-section">
          <div class="section-title">
            密钥操作
            <div class="section-actions">
              <el-button type="danger" :icon="RefreshRight" size="small" @click="resetSecret">重置密钥</el-button>
            </div>
          </div>
          <div class="secret-history">
            <div class="history-item">
              <span class="history-key dp-mono">{{ currentApp.appKey }}</span>
              <span class="history-status active">当前使用</span>
              <span class="history-time">创建于 {{ currentApp.createdAt }}</span>
            </div>
          </div>
        </div>

        <div class="secret-section">
          <div class="section-title">IP 白名单</div>
          <div class="ip-whitelist">
            <div class="ip-item" v-for="(ip, i) in ipWhitelist" :key="i">
              <span class="ip-text dp-mono">{{ ip }}</span>
              <el-button link type="danger" size="small" @click="removeIp(i)">删除</el-button>
            </div>
            <div class="add-ip">
              <el-input v-model="newIp" placeholder="输入 IP 地址，支持通配符 *" size="small" style="flex: 1" />
              <el-button type="primary" size="small" @click="addIp">添加</el-button>
            </div>
          </div>
          <div class="ip-tip">提示：白名单为空表示不限制 IP。支持通配符，如 192.168.1.*</div>
        </div>

        <div class="secret-section">
          <div class="section-title">签名配置</div>
          <el-form label-width="100px">
            <el-form-item label="签名算法">
              <el-radio-group v-model="signConfig.algorithm">
                <el-radio value="HMAC-SHA256">HMAC-SHA256</el-radio>
                <el-radio value="MD5">MD5</el-radio>
                <el-radio value="RSA">RSA</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="签名有效期">
              <el-input-number v-model="signConfig.expire" :min="60" :max="3600" />
              <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">秒</span>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { serviceApps, owners } from '@/mock'
import { Plus, Refresh, RefreshRight, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const editVisible = ref(false)
const secretVisible = ref(false)
const formRef = ref(null)
const currentApp = ref({})
const appSecret = ref('')
const showSecret = ref(false)

const all = ref([])
const rows = ref([])
const total = ref(0)
const ownerOptions = owners

const query = reactive({
  keyword: '',
  type: '',
  status: '',
  page: 1,
  size: 10
})

const form = reactive({
  id: null,
  name: '',
  type: '内部应用',
  qpsNum: 500,
  dailyLimit: 100000,
  owner: '张伟',
  desc: '',
  callbackUrl: ''
})

const rules = {
  name: [{ required: true, message: '请输入应用名称', trigger: 'blur' }]
}

// IP 白名单
const ipWhitelist = ref(['192.168.1.100', '10.0.0.*'])
const newIp = ref('')

const signConfig = reactive({
  algorithm: 'HMAC-SHA256',
  expire: 300
})

const totalApps = computed(() => all.value.length)
const activeApps = computed(() => all.value.filter(a => a.status === '正常').length)
const totalSubscribed = computed(() => all.value.reduce((sum, a) => sum + a.boundServices, 0))
const totalCalls = computed(() => all.value.reduce((sum, a) => sum + a.callsMonth, 0))

function typeTagType(type) {
  const map = { '内部应用': 'primary', '外部应用': 'warning', '第三方合作': 'danger' }
  return map[type] || 'info'
}

function typeClass(type) {
  const map = { '内部应用': 'internal', '外部应用': 'external', '第三方合作': 'thirdparty' }
  return map[type] || 'internal'
}

function formatCalls(num) {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toLocaleString()
}

function load() {
  loading.value = true
  setTimeout(() => {
    let list = all.value.filter(d => {
      const matchKw = !query.keyword ||
        d.name.includes(query.keyword) ||
        d.appKey.includes(query.keyword)
      const matchType = !query.type || d.type === query.type
      const matchStatus = !query.status || d.status === query.status
      return matchKw && matchType && matchStatus
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

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      type: row.type,
      qpsNum: parseInt(row.rateLimit) || 500,
      dailyLimit: 100000,
      owner: row.owner,
      desc: row.desc || '',
      callbackUrl: ''
    })
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      type: '内部应用',
      qpsNum: 500,
      dailyLimit: 100000,
      owner: '张伟',
      desc: '',
      callbackUrl: ''
    })
  }
  editVisible.value = true
}

function save() {
  formRef.value?.validate(valid => {
    if (!valid) return
    if (form.id) {
      const i = all.value.findIndex(d => d.id === form.id)
      all.value[i] = {
        ...all.value[i],
        name: form.name,
        type: form.type,
        rateLimit: form.qpsNum + ' QPS',
        owner: form.owner,
        desc: form.desc
      }
      ElMessage.success('应用已更新')
    } else {
      all.value.unshift({
        id: Date.now(),
        name: form.name,
        appKey: 'AK' + Math.floor(100000 + Math.random() * 900000),
        type: form.type,
        boundServices: 0,
        callsMonth: 0,
        rateLimit: form.qpsNum + ' QPS',
        status: '正常',
        owner: form.owner,
        desc: form.desc,
        createdAt: '刚刚'
      })
      ElMessage.success('应用创建成功')
    }
    editVisible.value = false
    load()
  })
}

function viewSecret(row) {
  currentApp.value = row
  appSecret.value = 'SK-' + Math.random().toString(36).substring(2, 18) + Math.random().toString(36).substring(2, 10)
  showSecret.value = false
  secretVisible.value = true
}

function toggleSecret() {
  showSecret.value = !showSecret.value
}

function copyText(text) {
  navigator.clipboard.writeText(text)
  ElMessage.success('已复制到剪贴板')
}

function resetSecret() {
  ElMessageBox.confirm(
    '确定重置密钥吗？重置后旧密钥将立即失效，需要更新应用配置。',
    '重置确认',
    { type: 'warning', confirmButtonText: '确认重置' }
  ).then(() => {
    appSecret.value = 'SK-' + Math.random().toString(36).substring(2, 18) + Math.random().toString(36).substring(2, 10)
    showSecret.value = true
    ElMessage.success('密钥已重置，请及时更新应用配置')
  }).catch(() => {})
}

function viewPermission(row) {
  ElMessage.info(`打开「${row.name}」权限配置页面`)
}

function viewStats(row) {
  ElMessage.info(`查看「${row.name}」调用统计`)
}

function remove(row) {
  ElMessageBox.confirm(
    `确定删除应用「${row.name}」吗？删除后关联的授权和密钥将全部失效，此操作不可撤销。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确定删除' }
  ).then(() => {
    all.value = all.value.filter(d => d.id !== row.id)
    ElMessage.success('删除成功')
    load()
  }).catch(() => {})
}

function addIp() {
  if (!newIp.value) return ElMessage.warning('请输入 IP 地址')
  if (ipWhitelist.value.includes(newIp.value)) return ElMessage.warning('该 IP 已存在')
  ipWhitelist.value.push(newIp.value)
  newIp.value = ''
  ElMessage.success('添加成功')
}

function removeIp(index) {
  ipWhitelist.value.splice(index, 1)
}

onMounted(() => {
  all.value = serviceApps.map(app => ({
    ...app,
    desc: app.type === '内部应用' ? '公司内部系统使用' :
          app.type === '外部应用' ? '对外提供的业务应用' :
          '第三方合作伙伴应用'
  }))
  load()
})
</script>

<style scoped>
.apps-page {
  min-height: 100%;
}

/* 统计卡片 */
.app-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.app-stat-card {
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

.app-stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
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

/* 应用名称单元格 */
.app-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.app-avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
}

.app-avatar.internal { background: linear-gradient(135deg, #1664ff, #5b8def); }
.app-avatar.external { background: linear-gradient(135deg, #ff7d00, #ffb366); }
.app-avatar.thirdparty { background: linear-gradient(135deg, #f53f3f, #ff7875); }

.app-name {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--dp-text-1);
  margin-bottom: 2px;
}

.app-desc {
  font-size: 11.5px;
  color: var(--dp-text-3);
}

.app-key {
  font-family: Consolas, monospace;
  font-size: 12px;
  color: var(--dp-text-2);
}

.service-count {
  font-weight: 500;
  color: var(--dp-primary);
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

.status-dot.ok {
  color: var(--dp-success);
}
.status-dot.ok .dot {
  background: var(--dp-success);
  box-shadow: 0 0 0 3px var(--dp-success-light);
}

.status-dot.disabled {
  color: var(--dp-text-3);
}
.status-dot.disabled .dot {
  background: var(--dp-text-4);
  box-shadow: 0 0 0 3px var(--dp-border-light);
}

/* 密钥管理 */
.secret-manage {
  padding-right: 8px;
}

.secret-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 14px;
  padding-left: 10px;
  border-left: 3px solid var(--dp-primary);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.secret-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: var(--dp-bg-page);
  border-radius: 8px;
  margin-bottom: 10px;
}

.secret-label {
  font-size: 13px;
  color: var(--dp-text-2);
  width: 80px;
  flex-shrink: 0;
}

.secret-value {
  flex: 1;
  font-size: 12.5px;
  color: var(--dp-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
}

.secret-history {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--dp-border-light);
  border-radius: 6px;
}

.history-key {
  flex: 1;
  font-size: 12.5px;
  color: var(--dp-text-2);
}

.history-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  font-weight: 500;
}

.history-status.active {
  background: var(--dp-success-light);
  color: var(--dp-success);
}

.history-time {
  font-size: 11px;
  color: var(--dp-text-3);
}

/* IP 白名单 */
.ip-whitelist {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}

.ip-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: var(--dp-bg-page);
  border-radius: 6px;
}

.ip-text {
  flex: 1;
  font-size: 12.5px;
  color: var(--dp-text-2);
}

.add-ip {
  display: flex;
  gap: 8px;
}

.ip-tip {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 8px;
}
</style>
