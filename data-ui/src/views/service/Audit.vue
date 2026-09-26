<template>
  <div class="dp-page audit-page">
    <PageHeader title="调用审计" desc="记录所有数据服务调用日志，支持按时间、应用、API和状态码多维度筛选与审计追溯">
      <el-button :icon="Download" @click="exportLogs">导出日志</el-button>
      <el-button :icon="Refresh" @click="load">刷新</el-button>
    </PageHeader>

    <!-- 统计概览 -->
    <div class="audit-stats">
      <div class="audit-stat-item">
        <div class="stat-icon primary">
          <el-icon :size="18"><Connection /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-num">{{ totalCalls }}</div>
          <div class="stat-label">总调用次数</div>
        </div>
      </div>
      <div class="audit-stat-item">
        <div class="stat-icon success">
          <el-icon :size="18"><CircleCheck /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-num">{{ successRate }}%</div>
          <div class="stat-label">成功率</div>
        </div>
      </div>
      <div class="audit-stat-item">
        <div class="stat-icon warning">
          <el-icon :size="18"><Warning /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-num">{{ rateLimitCount }}</div>
          <div class="stat-label">限流次数</div>
        </div>
      </div>
      <div class="audit-stat-item">
        <div class="stat-icon danger">
          <el-icon :size="18"><CircleClose /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-num">{{ errorCount }}</div>
          <div class="stat-label">错误次数</div>
        </div>
      </div>
      <div class="audit-stat-item">
        <div class="stat-icon info">
          <el-icon :size="18"><Timer /></el-icon>
        </div>
        <div class="stat-content">
          <div class="stat-num">{{ avgCost }}ms</div>
          <div class="stat-label">平均响应</div>
        </div>
      </div>
    </div>

    <div class="dp-card">
      <div class="dp-toolbar">
        <el-date-picker
          v-model="query.dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          style="width: 360px"
          @change="load"
        />
        <el-select v-model="query.app" placeholder="选择应用" clearable style="width: 140px" @change="load">
          <el-option v-for="app in appOptions" :key="app" :label="app" :value="app" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="搜索API路径 / IP"
          clearable
          style="width: 220px"
          :prefix-icon="Search"
          @input="load"
        />
        <el-select v-model="query.status" placeholder="状态码" clearable style="width: 120px" @change="load">
          <el-option label="200 成功" :value="200" />
          <el-option label="429 限流" :value="429" />
          <el-option label="500 错误" :value="500" />
        </el-select>
        <el-select v-model="query.method" placeholder="请求方法" clearable style="width: 100px" @change="load">
          <el-option label="GET" value="GET" />
          <el-option label="POST" value="POST" />
          <el-option label="PUT" value="PUT" />
          <el-option label="DELETE" value="DELETE" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="id" label="请求ID" width="100" class-name="dp-mono" show-overflow-tooltip />
        <el-table-column prop="time" label="调用时间" width="170" />
        <el-table-column prop="app" label="应用" width="110">
          <template #default="{ row }">
            <el-tag type="primary" size="small" effect="plain">{{ row.app }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="service" label="API 接口" min-width="240" class-name="dp-mono" show-overflow-tooltip />
        <el-table-column prop="method" label="方法" width="80" align="center">
          <template #default="{ row }">
            <span class="method-tag" :class="row.method.toLowerCase()">{{ row.method }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="cost" label="响应时间(ms)" width="120" sortable align="right">
          <template #default="{ row }">
            <span :class="costClass(row.cost)">{{ row.cost }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态码" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="dark">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="来源 IP" width="140" class-name="dp-mono" />
        <el-table-column label="操作" width="90" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="调用详情" size="560px" destroy-on-close>
      <template v-if="currentRow.id">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="请求ID" :span="2">
            <span class="dp-mono">{{ currentRow.id }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="调用时间">{{ currentRow.time }}</el-descriptions-item>
          <el-descriptions-item label="响应耗时">
            <span :class="costClass(currentRow.cost)">{{ currentRow.cost }} ms</span>
          </el-descriptions-item>
          <el-descriptions-item label="应用名称">{{ currentRow.app }}</el-descriptions-item>
          <el-descriptions-item label="请求方法">
            <span class="method-tag" :class="currentRow.method.toLowerCase()">{{ currentRow.method }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态码" :span="2">
            <el-tag :type="statusTagType(currentRow.status)" effect="dark">{{ currentRow.status }} {{ statusText(currentRow.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="API 接口" :span="2">
            <span class="dp-mono">{{ currentRow.service }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="来源 IP" :span="2">
            <span class="dp-mono">{{ currentRow.ip }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <el-tabs v-model="detailTab" style="margin-top: 20px">
          <el-tab-pane label="请求参数" name="request">
            <pre class="detail-code">{{ formatJson(currentRow.requestBody || mockRequestBody) }}</pre>
          </el-tab-pane>
          <el-tab-pane label="请求头" name="reqHeaders">
            <el-table :data="reqHeaders" border size="small">
              <el-table-column prop="name" label="Header 名称" width="200" />
              <el-table-column prop="value" label="值" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="响应数据" name="response">
            <pre class="detail-code">{{ formatJson(currentRow.responseBody || mockResponseBody) }}</pre>
          </el-tab-pane>
          <el-tab-pane label="响应头" name="resHeaders">
            <el-table :data="resHeaders" border size="small">
              <el-table-column prop="name" label="Header 名称" width="200" />
              <el-table-column prop="value" label="值" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { auditLogs, serviceApps } from '@/mock'
import { Download, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const detailVisible = ref(false)
const detailTab = ref('request')
const currentRow = ref({})

const all = ref([])
const rows = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  app: '',
  status: '',
  method: '',
  dateRange: null,
  page: 1,
  size: 10
})

const appOptions = computed(() => [...new Set(serviceApps.map(a => a.name.split('-')[0]))])

const totalCalls = computed(() => all.value.length)
const successRate = computed(() => {
  if (!all.value.length) return 0
  const success = all.value.filter(d => d.status === 200).length
  return ((success / all.value.length) * 100).toFixed(1)
})
const rateLimitCount = computed(() => all.value.filter(d => d.status === 429).length)
const errorCount = computed(() => all.value.filter(d => d.status === 500).length)
const avgCost = computed(() => {
  if (!all.value.length) return 0
  const sum = all.value.reduce((acc, d) => acc + d.cost, 0)
  return Math.round(sum / all.value.length)
})

const reqHeaders = [
  { name: 'Content-Type', value: 'application/json' },
  { name: 'Authorization', value: 'Bearer eyJhbGciOiJIUzI1NiIs...' },
  { name: 'X-App-Key', value: 'AK2024010100001' },
  { name: 'X-Request-Id', value: 'req_abc123xyz' },
  { name: 'User-Agent', value: 'DataPlatform-SDK/2.3.0' },
  { name: 'X-Forwarded-For', value: '10.0.1.55, 192.168.1.1' }
]

const resHeaders = [
  { name: 'Content-Type', value: 'application/json; charset=utf-8' },
  { name: 'X-Request-Id', value: 'req_abc123xyz' },
  { name: 'X-RateLimit-Limit', value: '100' },
  { name: 'X-RateLimit-Remaining', value: '98' },
  { name: 'X-RateLimit-Reset', value: '3600' },
  { name: 'Server', value: 'DataPlatform-Gateway/2.3.0' },
  { name: 'Date', value: new Date().toUTCString() }
]

const mockRequestBody = {
  page: 1,
  pageSize: 20,
  keyword: '',
  filters: { status: 'active', category: 'vip' }
}

const mockResponseBody = {
  code: 0,
  message: 'success',
  requestId: 'req_abc123xyz',
  data: {
    total: 156,
    page: 1,
    pageSize: 20,
    list: [
      { id: 1, name: '客户A', level: 'VIP', createTime: '2024-01-15 10:30:00' },
      { id: 2, name: '客户B', level: '普通', createTime: '2024-01-16 14:20:00' }
    ]
  }
}

function statusTagType(s) {
  if (s === 200) return 'success'
  if (s === 429) return 'warning'
  if (s === 500) return 'danger'
  return 'info'
}

function statusText(s) {
  if (s === 200) return '成功'
  if (s === 429) return '限流'
  if (s === 500) return '错误'
  return '未知'
}

function costClass(cost) {
  if (cost > 500) return 'cost-slow'
  if (cost > 200) return 'cost-medium'
  return 'cost-fast'
}

function formatJson(obj) {
  try {
    return JSON.stringify(obj, null, 2)
  } catch {
    return JSON.stringify(mockResponseBody, null, 2)
  }
}

function load() {
  loading.value = true
  setTimeout(() => {
    let list = all.value.filter(d => {
      const matchKeyword = !query.keyword ||
        d.service.includes(query.keyword) ||
        d.ip.includes(query.keyword)
      const matchApp = !query.app || d.app === query.app
      const matchStatus = query.status === '' || query.status === null || d.status === query.status
      const matchMethod = !query.method || d.method === query.method
      return matchKeyword && matchApp && matchStatus && matchMethod
    })
    total.value = list.length
    rows.value = list.slice((query.page - 1) * query.size, query.page * query.size)
    loading.value = false
  }, 200)
}

function viewDetail(row) {
  currentRow.value = row
  detailVisible.value = true
  detailTab.value = 'request'
}

function exportLogs() {
  ElMessage.success('日志导出任务已提交，请稍后在下载中心查看')
}

onMounted(() => {
  all.value = [...auditLogs]
  load()
})
</script>

<style scoped>
.audit-page {
  padding-bottom: 20px;
}

.audit-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.audit-stat-item {
  flex: 1;
  background: var(--dp-bg-card);
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.2s;
}

.audit-stat-item:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-icon.primary {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
}

.stat-icon.success {
  background: var(--dp-success-light);
  color: var(--dp-success);
}

.stat-icon.warning {
  background: var(--dp-warning-light);
  color: var(--dp-warning);
}

.stat-icon.danger {
  background: var(--dp-danger-light);
  color: var(--dp-danger);
}

.stat-icon.info {
  background: #e6f4ff;
  color: #1677ff;
}

.stat-content {
  flex: 1;
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--dp-text-1);
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 4px;
}

.method-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  font-family: Consolas, 'JetBrains Mono', monospace;
}

.method-tag.get {
  background: #e8ffea;
  color: #00b42a;
}

.method-tag.post {
  background: #e8f3ff;
  color: #165dff;
}

.method-tag.put {
  background: #fff7e6;
  color: #ff7d00;
}

.method-tag.delete {
  background: #ffece8;
  color: #f53f3f;
}

.cost-fast {
  color: var(--dp-success);
  font-weight: 500;
}

.cost-medium {
  color: var(--dp-warning);
  font-weight: 500;
}

.cost-slow {
  color: var(--dp-danger);
  font-weight: 600;
}

.detail-code {
  background: #0d1117;
  color: #cdd6f4;
  padding: 16px;
  border-radius: 6px;
  font-family: Consolas, 'JetBrains Mono', monospace;
  font-size: 12.5px;
  line-height: 1.6;
  margin: 0;
  max-height: 400px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
