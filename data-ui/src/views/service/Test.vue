<template>
  <div class="dp-page test-page">
    <PageHeader title="服务测试" desc="在线调试数据服务接口，支持参数配置、请求发送和响应结果查看，帮助开发者快速验证接口可用性">
      <el-button :icon="Refresh" @click="resetTest">重置</el-button>
      <el-button :icon="Document" @click="saveTestCase">保存用例</el-button>
    </PageHeader>

    <div class="test-body">
      <!-- 左侧：API 列表 -->
      <div class="dp-card test-left">
        <div class="test-sidebar-header">
          <span class="sidebar-title">API 列表</span>
          <el-tag size="small" type="info">{{ filteredServices.length }}</el-tag>
        </div>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索服务名称 / 路径"
          clearable
          size="small"
          :prefix-icon="Search"
          style="margin-bottom: 10px"
        />
        <div class="test-service-list">
          <div
            v-for="s in filteredServices"
            :key="s.id"
            class="test-service-item"
            :class="{ active: s.id === selectedService?.id }"
            @click="selectService(s)"
          >
            <div class="tsvc-header">
              <span class="method-tag" :class="s.method.toLowerCase()">{{ s.method }}</span>
              <span class="tsvc-name">{{ s.name }}</span>
            </div>
            <div class="tsvc-path dp-mono">{{ s.path }}</div>
            <div class="tsvc-footer">
              <span class="status-dot" :class="statusDotClass(s.status)"></span>
              <span class="tsvc-status">{{ s.status }}</span>
              <span class="tsvc-cost">{{ s.avgCost }}ms</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：测试区 -->
      <div class="test-right">
        <!-- 请求配置 -->
        <div class="dp-card">
          <div class="dp-card-title">
            请求配置
            <span class="dp-card-extra">
              <el-tag v-if="selectedService" :type="statusTagType(selectedService.status)" size="small" effect="plain">
                {{ selectedService.status }}
              </el-tag>
              <span v-else class="dp-desc">请选择服务</span>
            </span>
          </div>

          <div v-if="selectedService" class="request-bar">
            <el-select v-model="requestMethod" class="method-select">
              <el-option label="GET" value="GET" />
              <el-option label="POST" value="POST" />
              <el-option label="PUT" value="PUT" />
              <el-option label="DELETE" value="DELETE" />
            </el-select>
            <el-input v-model="requestUrl" class="url-input" placeholder="请求 URL" />
            <el-button type="primary" :icon="CaretRight" :loading="sending" @click="sendRequest">发送</el-button>
          </div>
          <div v-else class="empty-select">
            <el-empty description="请从左侧选择一个 API 开始测试" :image-size="60" />
          </div>

          <el-tabs v-if="selectedService" v-model="requestTab" style="margin-top: 12px">
            <!-- 请求参数 -->
            <el-tab-pane label="Query 参数" name="params">
              <div class="params-toolbar">
                <span class="params-count">共 {{ paramList.length }} 个参数</span>
                <el-button link type="primary" size="small" :icon="Plus" @click="addParam">添加参数</el-button>
              </div>
              <el-table :data="paramList" border size="small" class="params-table">
                <el-table-column type="index" label="#" width="40" align="center" />
                <el-table-column prop="name" label="参数名" width="140">
                  <template #default="{ row }">
                    <el-input v-model="row.name" size="small" placeholder="参数名" />
                  </template>
                </el-table-column>
                <el-table-column prop="type" label="类型" width="90">
                  <template #default="{ row }">
                    <el-select v-model="row.type" size="small">
                      <el-option label="string" value="string" />
                      <el-option label="number" value="number" />
                      <el-option label="boolean" value="boolean" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column prop="required" label="必填" width="60" align="center">
                  <template #default="{ row }">
                    <el-checkbox v-model="row.required" />
                  </template>
                </el-table-column>
                <el-table-column label="参数值" min-width="180">
                  <template #default="{ row }">
                    <el-input v-model="row.value" size="small" :placeholder="'请输入' + row.name" />
                  </template>
                </el-table-column>
                <el-table-column prop="desc" label="说明" min-width="140">
                  <template #default="{ row }">
                    <el-input v-model="row.desc" size="small" placeholder="参数说明" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="60" align="center">
                  <template #default="{ $index }">
                    <el-button link type="danger" size="small" @click="removeParam($index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- 请求头 -->
            <el-tab-pane label="请求头" name="headers">
              <div class="params-toolbar">
                <span class="params-count">共 {{ headerList.length }} 个 Header</span>
                <el-button link type="primary" size="small" :icon="Plus" @click="addHeader">添加 Header</el-button>
              </div>
              <el-table :data="headerList" border size="small" class="params-table">
                <el-table-column type="index" label="#" width="40" align="center" />
                <el-table-column prop="name" label="Header 名称" width="180">
                  <template #default="{ row }">
                    <el-input v-model="row.name" size="small" placeholder="Header 名称" />
                  </template>
                </el-table-column>
                <el-table-column label="值">
                  <template #default="{ row }">
                    <el-input v-model="row.value" size="small" placeholder="Header 值" />
                  </template>
                </el-table-column>
                <el-table-column prop="desc" label="说明" width="160">
                  <template #default="{ row }">
                    <el-input v-model="row.desc" size="small" placeholder="说明" />
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="60" align="center">
                  <template #default="{ $index }">
                    <el-button link type="danger" size="small" @click="removeHeader($index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <!-- Body -->
            <el-tab-pane label="Body" name="body">
              <div v-if="requestMethod === 'GET'" class="dp-desc" style="padding: 30px; text-align: center">
                GET 请求不支持 Body 参数
              </div>
              <div v-else>
                <el-radio-group v-model="bodyType" size="small" style="margin-bottom: 10px">
                  <el-radio-button value="json">JSON</el-radio-button>
                  <el-radio-button value="form">form-data</el-radio-button>
                  <el-radio-button value="xform">x-www-form-urlencoded</el-radio-button>
                  <el-radio-button value="raw">raw</el-radio-button>
                </el-radio-group>
                <textarea
                  v-if="bodyType === 'json' || bodyType === 'raw'"
                  v-model="requestBody"
                  class="body-editor"
                  spellcheck="false"
                  :placeholder="bodyEditorPlaceholder"
                />
                <el-table v-else :data="formDataList" border size="small" class="params-table">
                  <el-table-column type="index" label="#" width="40" align="center" />
                  <el-table-column prop="key" label="Key" width="160">
                    <template #default="{ row }">
                      <el-input v-model="row.key" size="small" placeholder="字段名" />
                    </template>
                  </el-table-column>
                  <el-table-column label="Value">
                    <template #default="{ row }">
                      <el-input v-model="row.value" size="small" placeholder="字段值" />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="60" align="center">
                    <template #default="{ $index }">
                      <el-button link type="danger" size="small" @click="removeFormItem($index)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <el-button
                  v-if="bodyType === 'form' || bodyType === 'xform'"
                  link
                  type="primary"
                  size="small"
                  :icon="Plus"
                  style="margin-top: 8px"
                  @click="addFormItem"
                >
                  添加字段
                </el-button>
              </div>
            </el-tab-pane>

            <!-- 认证 -->
            <el-tab-pane label="认证" name="auth">
              <el-form label-width="120px" style="max-width: 520px; padding-top: 10px">
                <el-form-item label="认证方式">
                  <el-radio-group v-model="authType">
                    <el-radio value="none">无认证</el-radio>
                    <el-radio value="appkey">AppKey/Secret</el-radio>
                    <el-radio value="token">Bearer Token</el-radio>
                    <el-radio value="oauth">OAuth 2.0</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item v-if="authType === 'appkey'" label="AppKey">
                  <el-input v-model="authForm.appKey" style="width: 360px" />
                </el-form-item>
                <el-form-item v-if="authType === 'appkey'" label="AppSecret">
                  <el-input v-model="authForm.appSecret" type="password" show-password style="width: 360px" />
                </el-form-item>
                <el-form-item v-if="authType === 'token'" label="Token">
                  <el-input v-model="authForm.token" type="textarea" :rows="3" style="width: 420px" />
                </el-form-item>
                <el-form-item v-if="authType === 'oauth'" label="Access Token">
                  <el-input v-model="authForm.accessToken" type="textarea" :rows="2" style="width: 420px" />
                </el-form-item>
                <el-form-item v-if="authType === 'oauth'" label="刷新令牌">
                  <el-input v-model="authForm.refreshToken" style="width: 360px" />
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 响应结果 -->
        <div class="dp-card" style="margin-top: 12px">
          <div class="dp-card-title">
            响应结果
            <span class="dp-card-extra">
              <template v-if="responseData">
                <el-tag :type="responseStatus === 200 ? 'success' : 'danger'" size="small" effect="dark">
                  {{ responseStatus }} {{ responseStatusText }}
                </el-tag>
                <span class="response-meta">
                  <el-icon><Timer /></el-icon> {{ responseTime }}ms
                </span>
                <span class="response-meta">
                  <el-icon><Files /></el-icon> {{ responseSize }}
                </span>
              </template>
              <span v-else class="dp-desc">未发送请求</span>
            </span>
          </div>

          <div v-if="!responseData" class="empty-result">
            <el-empty description="选择服务并点击发送查看响应结果" :image-size="80">
              <el-button type="primary" :icon="CaretRight" :disabled="!selectedService" @click="sendRequest">
                发送请求
              </el-button>
            </el-empty>
          </div>

          <div v-else class="response-area">
            <el-tabs v-model="responseTab">
              <el-tab-pane label="响应数据" name="data">
                <div class="response-toolbar">
                  <el-radio-group v-model="responseView" size="small">
                    <el-radio-button value="pretty">美化</el-radio-button>
                    <el-radio-button value="raw">原始</el-radio-button>
                  </el-radio-group>
                  <el-button link type="primary" size="small" :icon="CopyDocument" @click="copyResponse">
                    复制
                  </el-button>
                </div>
                <pre class="response-code">{{ responseView === 'pretty' ? formatJson(responseData) : responseData }}</pre>
              </el-tab-pane>
              <el-tab-pane label="响应头" name="headers">
                <el-table :data="responseHeaders" border size="small">
                  <el-table-column prop="name" label="名称" width="220" />
                  <el-table-column prop="value" label="值" />
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="请求信息" name="requestInfo">
                <el-descriptions :column="1" border size="small">
                  <el-descriptions-item label="请求 URL">
                    <span class="dp-mono">{{ requestUrl }}</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="请求方法">{{ requestMethod }}</el-descriptions-item>
                  <el-descriptions-item label="状态码">
                    <el-tag :type="responseStatus === 200 ? 'success' : 'danger'">{{ responseStatus }}</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="响应时间">{{ responseTime }} ms</el-descriptions-item>
                  <el-descriptions-item label="响应大小">{{ responseSize }}</el-descriptions-item>
                </el-descriptions>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { services } from '@/mock'
import { CaretRight, CopyDocument, Document, Plus, Refresh, Search } from '@element-plus/icons-vue'

const searchKeyword = ref('')
const selectedService = ref(null)
const requestTab = ref('params')
const responseTab = ref('data')
const responseView = ref('pretty')
const sending = ref(false)
const responseData = ref('')
const responseStatus = ref(200)
const responseTime = ref(0)
const responseSize = ref('0 B')
const authType = ref('appkey')
const requestMethod = ref('GET')
const bodyType = ref('json')
const requestBody = ref('{\n  "page": 1,\n  "pageSize": 20,\n  "keyword": ""\n}')

const authForm = reactive({
  appKey: 'AK2024010100001',
  appSecret: 'SK-xxxxxxxxxxxxxxxxxxxx',
  token: '',
  accessToken: '',
  refreshToken: ''
})

const headerList = ref([
  { name: 'Content-Type', value: 'application/json', desc: '请求内容类型' },
  { name: 'X-App-Key', value: 'AK2024010100001', desc: '应用密钥' },
  { name: 'X-Request-Id', value: '', desc: '请求追踪 ID' }
])

const paramList = ref([
  { name: 'keyword', type: 'string', required: false, value: '', desc: '搜索关键词' },
  { name: 'page', type: 'number', required: false, value: '1', desc: '页码' },
  { name: 'pageSize', type: 'number', required: false, value: '20', desc: '每页条数' }
])

const formDataList = ref([
  { key: 'username', value: 'admin' },
  { key: 'password', value: '123456' }
])

const responseHeaders = ref([])

const filteredServices = computed(() => {
  if (!searchKeyword.value) return services
  const kw = searchKeyword.value.toLowerCase()
  return services.filter(s =>
    s.name.toLowerCase().includes(kw) ||
    s.path.toLowerCase().includes(kw)
  )
})

const requestUrl = computed(() => {
  if (!selectedService.value) return ''
  const base = 'https://api.dataplatform.com'
  return base + selectedService.value.path
})

const responseStatusText = computed(() => {
  if (responseStatus.value === 200) return 'OK'
  if (responseStatus.value === 429) return 'Too Many Requests'
  if (responseStatus.value === 500) return 'Internal Server Error'
  if (responseStatus.value === 404) return 'Not Found'
  return ''
})

const bodyEditorPlaceholder = computed(() =>
  bodyType.value === 'json' ? '{"key": "value"}' : '原始文本内容'
)

function statusTagType(s) {
  if (s === '已发布') return 'success'
  if (s === '开发中') return 'warning'
  if (s === '已下线') return 'info'
  return 'info'
}

function statusDotClass(s) {
  if (s === '已发布') return 'dot-success'
  if (s === '开发中') return 'dot-warning'
  if (s === '已下线') return 'dot-info'
  return 'dot-info'
}

function formatJson(str) {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

function selectService(s) {
  selectedService.value = s
  requestMethod.value = s.method
  responseData.value = ''
  responseStatus.value = 200
  responseTime.value = 0
  responseSize.value = '0 B'

  if (s.method === 'GET') {
    paramList.value = [
      { name: 'keyword', type: 'string', required: false, value: '', desc: '搜索关键词' },
      { name: 'page', type: 'number', required: false, value: '1', desc: '页码' },
      { name: 'pageSize', type: 'number', required: false, value: '20', desc: '每页条数' }
    ]
  } else {
    paramList.value = [
      { name: 'id', type: 'number', required: true, value: '1', desc: '记录 ID' },
      { name: 'name', type: 'string', required: false, value: '', desc: '名称' }
    ]
  }
}

function sendRequest() {
  if (!selectedService.value) return ElMessage.warning('请先选择服务')
  sending.value = true
  const startTime = Date.now()

  setTimeout(() => {
    sending.value = false
    responseTime.value = Date.now() - startTime + Math.floor(Math.random() * 150 + 50)

    const isSuccess = Math.random() > 0.1
    responseStatus.value = isSuccess ? 200 : (Math.random() > 0.5 ? 429 : 500)

    if (isSuccess) {
      const data = {
        code: 0,
        message: 'success',
        requestId: 'req_' + Math.random().toString(36).substring(2, 15),
        timestamp: Date.now(),
        data: {
          total: 156,
          page: 1,
          pageSize: 20,
          list: Array.from({ length: 5 }, (_, i) => ({
            id: i + 1,
            name: '示例数据_' + (i + 1),
            value: Math.floor(Math.random() * 10000),
            status: ['active', 'inactive', 'pending'][i % 3],
            createTime: new Date(Date.now() - i * 86400000).toISOString().replace('T', ' ').substring(0, 19)
          }))
        }
      }
      responseData.value = JSON.stringify(data, null, 2)
    } else {
      const errData = {
        code: responseStatus.value === 429 ? 429001 : 500001,
        message: responseStatus.value === 429 ? '请求过于频繁，请稍后再试' : '服务器内部错误',
        requestId: 'req_' + Math.random().toString(36).substring(2, 15),
        timestamp: Date.now()
      }
      responseData.value = JSON.stringify(errData, null, 2)
    }

    responseSize.value = formatBytes(responseData.value.length)

    responseHeaders.value = [
      { name: 'Content-Type', value: 'application/json; charset=utf-8' },
      { name: 'X-Request-Id', value: 'req_' + Math.random().toString(36).substring(2, 15) },
      { name: 'X-RateLimit-Limit', value: '100' },
      { name: 'X-RateLimit-Remaining', value: String(Math.floor(Math.random() * 80 + 20)) },
      { name: 'X-RateLimit-Reset', value: '3600' },
      { name: 'Server', value: 'DataPlatform-Gateway/2.3.0' },
      { name: 'Date', value: new Date().toUTCString() }
    ]

    ElMessage[isSuccess ? 'success' : 'warning'](isSuccess ? '请求成功' : '请求失败')
  }, 400 + Math.random() * 400)
}

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

function resetTest() {
  responseData.value = ''
  responseStatus.value = 200
  responseTime.value = 0
  responseSize.value = '0 B'
  responseHeaders.value = []
  paramList.value.forEach(p => p.value = '')
  headerList.value.forEach(h => { if (h.name !== 'Content-Type' && h.name !== 'X-App-Key') h.value = '' })
  ElMessage.success('已重置测试环境')
}

function copyResponse() {
  navigator.clipboard.writeText(responseData.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function saveTestCase() {
  ElMessage.success('测试用例已保存')
}

function addParam() {
  paramList.value.push({ name: '', type: 'string', required: false, value: '', desc: '' })
}

function removeParam(index) {
  paramList.value.splice(index, 1)
}

function addHeader() {
  headerList.value.push({ name: '', value: '', desc: '' })
}

function removeHeader(index) {
  headerList.value.splice(index, 1)
}

function addFormItem() {
  formDataList.value.push({ key: '', value: '' })
}

function removeFormItem(index) {
  formDataList.value.splice(index, 1)
}
</script>

<style scoped>
.test-page {
  padding-bottom: 0;
}

.test-body {
  display: flex;
  gap: 12px;
  height: calc(100vh - 56px - 100px);
  min-height: 600px;
}

/* 左侧 */
.test-left {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.test-sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.sidebar-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.test-service-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-right: 4px;
}

.test-service-item {
  padding: 10px 12px;
  border: 1px solid var(--dp-border-light);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--dp-bg-card);
}

.test-service-item:hover {
  border-color: var(--dp-primary);
  background: var(--dp-primary-bg);
}

.test-service-item.active {
  border-color: var(--dp-primary);
  background: var(--dp-primary-bg);
  box-shadow: 0 0 0 1px var(--dp-primary);
}

.tsvc-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
}

.tsvc-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tsvc-path {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tsvc-footer {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: var(--dp-text-3);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.dot-success {
  background: var(--dp-success);
}

.dot-warning {
  background: var(--dp-warning);
}

.dot-info {
  background: #909399;
}

.tsvc-cost {
  margin-left: auto;
  font-family: Consolas, monospace;
}

/* 右侧 */
.test-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.empty-select {
  padding: 30px 0;
}

.method-tag {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 700;
  font-family: Consolas, monospace;
  flex-shrink: 0;
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

.request-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 8px;
}

.method-select {
  width: 110px;
}

.url-input {
  flex: 1;
}

.params-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.params-count {
  font-size: 12px;
  color: var(--dp-text-3);
}

.params-table {
  margin-bottom: 8px;
}

.body-editor {
  width: 100%;
  height: 220px;
  resize: none;
  border: 1px solid var(--dp-border);
  border-radius: 6px;
  outline: none;
  background: #0d1117;
  color: #cdd6f4;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  line-height: 1.6;
  padding: 12px;
  tab-size: 2;
}

.body-editor:focus {
  border-color: var(--dp-primary);
}

.empty-result {
  padding: 40px 0;
}

.response-area {
  margin-top: 8px;
}

.response-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.response-meta {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--dp-text-2);
  margin-left: 12px;
}

.response-code {
  background: #0d1117;
  color: #cdd6f4;
  padding: 16px;
  border-radius: 6px;
  font-family: Consolas, 'JetBrains Mono', monospace;
  font-size: 12.5px;
  line-height: 1.6;
  margin: 0;
  max-height: 320px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
