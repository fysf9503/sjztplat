<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">{{ eyebrow }}</div>
      <h1 class="dp-page-title">{{ title }}</h1>
      <p class="dp-page-desc">{{ desc }}</p>
    </div>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <div class="dp-flex dp-gap-8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索模型名称"
            size="default"
            style="width: 220px"
            clearable
            @input="doSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select v-model="filterType" placeholder="模型类型" size="default" style="width: 140px" clearable @change="doSearch">
            <el-option label="对话模型" value="对话模型" />
            <el-option label="Embedding" value="Embedding" />
            <el-option label="多模型" value="多模型" />
          </el-select>
          <el-select v-model="filterStatus" placeholder="状态" size="default" style="width: 120px" clearable @change="doSearch">
            <el-option label="运行中" value="运行中" />
            <el-option label="已停止" value="已停止" />
          </el-select>
        </div>
        <div class="dp-toolbar-right">
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增模型</el-button>
        </div>
      </div>

      <el-table :data="pagedData" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="模型名称" min-width="180">
          <template #default="{ row }">
            <div class="model-name-cell">
              <div class="model-icon" :class="getTypeClass(row.type)">
                <el-icon :size="16"><component :is="getTypeIcon(row.type)" /></el-icon>
              </div>
              <div>
                <div class="model-name">{{ row.name }}</div>
                <div class="model-provider dp-desc">{{ row.provider }}</div>
              </div>
              <el-tag v-if="row.isDefault" size="small" type="primary" effect="dark" style="margin-left: 8px">默认</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="模型类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)" size="small" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="context" label="上下文长度" width="120" align="center" />
        <el-table-column prop="qpsLimit" label="QPS 限制" width="100" align="center">
          <template #default="{ row }">
            {{ row.qpsLimit }} / 秒
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === '运行中' ? 'success' : 'info'" size="small" effect="plain">
              <span class="status-dot" :class="row.status === '运行中' ? 'active' : 'inactive'"></span>
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="100" />
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleConfig(row)">配置</el-button>
            <el-button link type="primary" size="small" @click="handleTest(row)">测试</el-button>
            <el-button link :type="row.status === '运行中' ? 'danger' : 'success'" size="small" @click="handleToggle(row)">
              {{ row.status === '运行中' ? '停用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="filteredData.length"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50, 100]"
      />
    </div>

    <el-dialog v-model="configVisible" :title="'模型配置 - ' + currentModel?.name" width="560px">
      <template v-if="currentModel">
        <el-form :model="configForm" label-width="110px">
          <el-form-item label="模型名称">
            <el-input v-model="configForm.name" disabled />
          </el-form-item>
          <el-form-item label="服务提供商">
            <el-input v-model="configForm.provider" disabled />
          </el-form-item>
          <el-form-item label="模型类型">
            <el-input v-model="configForm.type" disabled />
          </el-form-item>
          <el-form-item label="API Endpoint">
            <el-input v-model="configForm.endpoint" placeholder="请输入 API 地址" />
          </el-form-item>
          <el-form-item label="API Key">
            <el-input v-model="configForm.apiKey" type="password" show-password placeholder="请输入 API Key" />
          </el-form-item>
          <el-form-item label="上下文长度">
            <el-select v-model="configForm.context" style="width: 100%">
              <el-option label="32K" value="32K" />
              <el-option label="64K" value="64K" />
              <el-option label="128K" value="128K" />
              <el-option label="256K" value="256K" />
            </el-select>
          </el-form-item>
          <el-form-item label="QPS 限制">
            <el-input-number v-model="configForm.qpsLimit" :min="1" :max="1000" style="width: 100%" />
          </el-form-item>
          <el-form-item label="温度 (Temperature)">
            <el-slider v-model="configForm.temperature" :min="0" :max="2" :step="0.1" />
          </el-form-item>
          <el-form-item label="设为默认">
            <el-switch v-model="configForm.isDefault" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="configVisible = false">取消</el-button>
        <el-button type="primary" @click="saveConfig">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="testVisible" title="模型连通性测试" width="500px">
      <template v-if="currentModel">
        <p style="color: var(--dp-text-2); margin-bottom: 16px">
          正在测试模型 <b>{{ currentModel.name }}</b> 的连通性...
        </p>
        <div v-if="testResult" class="test-result" :class="testResult.success ? 'success' : 'fail'">
          <el-icon :size="20">
            <component :is="testResult.success ? 'CircleCheck' : 'CircleClose'" />
          </el-icon>
          <div>
            <div class="test-result-title">{{ testResult.success ? '测试通过' : '测试失败' }}</div>
            <div class="test-result-desc">{{ testResult.message }}</div>
            <div v-if="testResult.latency" class="test-result-latency">
              响应耗时：{{ testResult.latency }}ms
            </div>
          </div>
        </div>
      </template>
      <template #footer>
        <el-button @click="testVisible = false">关闭</el-button>
        <el-button type="primary" :icon="Refresh" :loading="testing" @click="runTest">重新测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { aiModels } from '@/mock'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'

const eyebrow = 'AI MODEL MANAGEMENT'
const title = '模型管理'
const desc = '管理资产检索相关的 AI 模型配置和调用参数。'

const loading = ref(false)
const searchKeyword = ref('')
const filterType = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const configVisible = ref(false)
const testVisible = ref(false)
const currentModel = ref(null)
const testing = ref(false)
const testResult = ref(null)

const configForm = ref({
  name: '',
  provider: '',
  type: '',
  endpoint: '',
  apiKey: '',
  context: '32K',
  qpsLimit: 10,
  temperature: 0.7,
  isDefault: false
})

const filteredData = computed(() => {
  return aiModels.filter(m => {
    if (searchKeyword.value && !m.name.toLowerCase().includes(searchKeyword.value.toLowerCase())) return false
    if (filterType.value && m.type !== filterType.value) return false
    if (filterStatus.value && m.status !== filterStatus.value) return false
    return true
  })
})

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

function doSearch() {
  currentPage.value = 1
}

function getTypeIcon(type) {
  if (type === '对话模型') return 'ChatDotRound'
  if (type === 'Embedding') return 'Cpu'
  if (type === '多模型') return 'Picture'
  return 'Cpu'
}

function getTypeClass(type) {
  if (type === '对话模型') return 'type-chat'
  if (type === 'Embedding') return 'type-embed'
  if (type === '多模型') return 'type-multimodal'
  return ''
}

function getTypeTag(type) {
  if (type === '对话模型') return 'primary'
  if (type === 'Embedding') return 'success'
  if (type === '多模型') return 'warning'
  return 'info'
}

function handleAdd() {
  ElMessage.info('新增模型功能开发中')
}

function handleConfig(row) {
  currentModel.value = row
  configForm.value = {
    name: row.name,
    provider: row.provider,
    type: row.type,
    endpoint: 'https://api.example.com/v1/chat/completions',
    apiKey: 'sk-**********',
    context: row.context,
    qpsLimit: row.qpsLimit,
    temperature: 0.7,
    isDefault: row.isDefault
  }
  configVisible.value = true
}

function saveConfig() {
  ElMessage.success('模型配置已保存')
  configVisible.value = false
}

function handleTest(row) {
  currentModel.value = row
  testResult.value = null
  testVisible.value = true
  runTest()
}

function runTest() {
  testing.value = true
  testResult.value = null
  setTimeout(() => {
    testing.value = false
    const success = Math.random() > 0.2
    testResult.value = {
      success,
      message: success ? '模型连接正常，可正常响应请求' : '连接超时，请检查网络配置和 API Key',
      latency: success ? Math.floor(Math.random() * 500 + 100) : null
    }
  }, 1500)
}

function handleToggle(row) {
  const action = row.status === '运行中' ? '停用' : '启用'
  ElMessageBox.confirm(`确定「${action}模型「${row.name}」吗？`, '确认操作', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    row.status = row.status === '运行中' ? '已停止' : '运行中'
    ElMessage.success(`模型${action}成功`)
  }).catch(() => {})
}
</script>

<style scoped>
.model-name-cell {
  display: flex;
  align-items: center;
}

.model-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  flex-shrink: 0;
}

.model-icon.type-chat {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
}

.model-icon.type-embed {
  background: var(--dp-success-light);
  color: var(--dp-success);
}

.model-icon.type-multimodal {
  background: var(--dp-warning-light);
  color: var(--dp-warning);
}

.model-name {
  font-weight: 600;
  color: var(--dp-text-1);
  font-size: 13px;
}

.model-provider {
  font-size: 12px;
  margin-top: 2px;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 4px;
  vertical-align: middle;
}

.status-dot.active {
  background: var(--dp-success);
  box-shadow: 0 0 4px var(--dp-success);
}

.status-dot.inactive {
  background: var(--dp-text-4);
}

.test-result {
  display: flex;
  gap: 14px;
  padding: 20px;
  border-radius: 10px;
  align-items: flex-start;
}

.test-result.success {
  background: var(--dp-success-light);
  color: var(--dp-success);
}

.test-result.fail {
  background: var(--dp-danger-light);
  color: var(--dp-danger);
}

.test-result-title {
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 4px;
}

.test-result-desc {
  font-size: 13px;
  color: var(--dp-text-2);
}

.test-result-latency {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 6px;
}
</style>
