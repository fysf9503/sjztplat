<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div>
        <div class="dp-page-eyebrow">MODEL CONFIG</div>
        <h2 class="dp-page-title">模型配置</h2>
        <p class="dp-page-desc">管理实验环境中的 AI 模型配置，包括对话模型、Embedding 模型和多模态模型。</p>
      </div>
      <div class="dp-page-header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">添加模型</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid">
      <StatCard label="模型总数" :value="stats.total" unit="个" icon="Cpu" color="#165dff" bg="#e8f3ff" />
      <StatCard label="对话模型" :value="stats.chat" unit="个" icon="ChatDotRound" color="#722ed1" bg="#f5e8ff" />
      <StatCard label="Embedding" :value="stats.embedding" unit="个" icon="Connection" color="#00b42a" bg="#e8ffea" />
      <StatCard label="运行中" :value="stats.running" unit="个" icon="CircleCheck" color="#ff7d00" bg="#fff3e8" />
    </div>

    <!-- 表格 -->
    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索模型名称" clearable style="width: 260px" :prefix-icon="Search" @input="load" />
        <el-select v-model="query.type" placeholder="模型类型" clearable style="width: 140px" @change="load">
          <el-option label="对话模型" value="对话模型" />
          <el-option label="Embedding" value="Embedding" />
          <el-option label="多模型" value="多模型" />
        </el-select>
        <el-select v-model="query.provider" placeholder="供应商" clearable style="width: 160px" @change="load">
          <el-option label="本地部署" value="本地部署" />
          <el-option label="DeepSeek开放平台" value="DeepSeek开放平台" />
          <el-option label="阿里云百炼" value="阿里云百炼" />
          <el-option label="智谱AI" value="智谱AI" />
          <el-option label="OpenAI" value="OpenAI" />
          <el-option label="火山方舟" value="火山方舟" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
          <el-option label="运行中" value="运行中" />
          <el-option label="已停止" value="已停止" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="name" label="模型名称" min-width="180">
          <template #default="{ row }">
            <div class="model-cell">
              <div class="model-icon" :class="'model-' + typeClass(row.type)">
                <el-icon :size="16"><component :is="typeIcon(row.type)" /></el-icon>
              </div>
              <div class="model-info">
                <div class="model-name">
                  {{ row.name }}
                  <el-tag v-if="row.isDefault" size="small" type="primary" effect="dark" style="margin-left: 6px">默认</el-tag>
                </div>
                <div class="model-provider dp-desc">{{ row.provider }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="context" label="上下文长度" width="110" align="center" />
        <el-table-column prop="qpsLimit" label="QPS 限制" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '运行中' ? 'success' : 'info'" size="small">
              <span class="status-dot" :class="{ running: row.status === '运行中' }"></span>
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column prop="updatedAt" label="更新时间" width="140" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleConfig(row)">配置</el-button>
            <el-button link type="primary" size="small" @click="handleTest(row)">测试</el-button>
            <el-dropdown @command="cmd => handleMore(cmd, row)" trigger="click">
              <el-button link type="primary" size="small">
                更多<el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="default" :disabled="row.isDefault">设为默认</el-dropdown-item>
                  <el-dropdown-item :command="row.status === '运行中' ? 'stop' : 'start'">
                    {{ row.status === '运行中' ? '停用' : '启用' }}
                  </el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <!-- 添加/编辑模型对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑模型' : '添加模型'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="模型名称" required>
          <el-input v-model="form.name" placeholder="如：DeepSeek-V3" />
        </el-form-item>
        <el-form-item label="模型类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="对话模型" value="对话模型" />
            <el-option label="Embedding" value="Embedding" />
            <el-option label="多模型" value="多模型" />
          </el-select>
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="form.provider" style="width: 100%">
            <el-option label="本地部署" value="本地部署" />
            <el-option label="DeepSeek开放平台" value="DeepSeek开放平台" />
            <el-option label="阿里云百炼" value="阿里云百炼" />
            <el-option label="智谱AI" value="智谱AI" />
            <el-option label="OpenAI" value="OpenAI" />
            <el-option label="火山方舟" value="火山方舟" />
          </el-select>
        </el-form-item>
        <el-form-item label="API 端点">
          <el-input v-model="form.endpoint" placeholder="https://api.example.com/v1" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="sk-xxxx" />
        </el-form-item>
        <el-form-item label="上下文长度">
          <el-select v-model="form.context" style="width: 100%">
            <el-option label="32K" value="32K" />
            <el-option label="64K" value="64K" />
            <el-option label="128K" value="128K" />
            <el-option label="256K" value="256K" />
          </el-select>
        </el-form-item>
        <el-form-item label="QPS 限制">
          <el-input-number v-model="form.qpsLimit" :min="1" :max="1000" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">次 / 秒</span>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.owner" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 测试对话框 -->
    <el-dialog v-model="testVisible" title="模型连通性测试" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="测试模型">
          <el-input v-model="testModelName" readonly />
        </el-form-item>
        <el-form-item label="测试提示">
          <el-input v-model="testPrompt" type="textarea" :rows="3" placeholder="输入测试提示词:" />
        </el-form-item>
      </el-form>
      <div v-if="testResult" class="test-result">
        <div class="test-result-label">测试结果</div>
        <div class="test-result-content">{{ testResult }}</div>
      </div>
      <template #footer>
        <el-button @click="testVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testing" @click="runTest">开始测试</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import { aiModels } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const all = ref([...aiModels])
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', type: '', provider: '', status: '', page: 1, size: 10 })
const dialogVisible = ref(false)
const form = reactive({})
const testVisible = ref(false)
const testModelName = ref('')
const testPrompt = ref('你好，请简单介绍一下你自己。')
const testResult = ref('')
const testing = ref(false)

const stats = computed(() => ({
  total: all.value.length,
  chat: all.value.filter(m => m.type === '对话模型').length,
  embedding: all.value.filter(m => m.type === 'Embedding').length,
  running: all.value.filter(m => m.status === '运行中').length
}))

function typeClass(type) {
  return { '对话模型': 'chat', 'Embedding': 'embedding', '多模型': 'multimodal' }[type] || 'chat'
}

function typeIcon(type) {
  return { '对话模型': 'ChatDotRound', 'Embedding': 'Connection', '多模型': 'Picture' }[type] || 'Cpu'
}

function typeTagType(type) {
  return { '对话模型': 'primary', 'Embedding': 'success', '多模型': 'warning' }[type] || 'info'
}

function load() {
  let list = all.value.filter(m =>
    (!query.keyword || m.name.includes(query.keyword) || m.provider.includes(query.keyword)) &&
    (!query.type || m.type === query.type) &&
    (!query.provider || m.provider === query.provider) &&
    (!query.status || m.status === query.status)
  )
  total.value = list.length
  rows.value = list.slice((query.page - 1) * query.size, query.page * query.size)
}

function openAdd() {
  Object.assign(form, {
    id: null, name: '', type: '对话模型', provider: '本地部署',
    endpoint: '', apiKey: '', context: '32K', qpsLimit: 10,
    owner: '张伟', isDefault: false
  })
  dialogVisible.value = true
}

function save() {
  if (!form.name) return ElMessage.warning('请输入模型名称')
  if (form.id) {
    const i = all.value.findIndex(m => m.id === form.id)
    all.value[i] = { ...all.value[i], ...form }
    ElMessage.success('模型已更新')
  } else {
    all.value.unshift({
      ...form,
      id: Date.now(),
      status: '运行中',
      updatedAt: '刚刚'
    })
    ElMessage.success('模型已添加')
  }
  dialogVisible.value = false
  load()
}

function handleConfig(row) {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

function handleTest(row) {
  testModelName.value = row.name
  testResult.value = ''
  testVisible.value = true
}

function runTest() {
  if (!testPrompt.value.trim()) return ElMessage.warning('请输入测试提示词')
  testing.value = true
  setTimeout(() => {
    testing.value = false
    testResult.value = '连接成功！模型响应正常。\n\n你好！我是一个 AI 语言模型，可以帮助你回答问题、生成文本、分析数据等。有什么我可以帮助你的吗？\n\n耗时: 1.23s  Tokens: 128'
  }, 1500)
}

function handleMore(cmd, row) {
  if (cmd === 'edit') {
    Object.assign(form, { ...row })
    dialogVisible.value = true
  } else if (cmd === 'default') {
    all.value.forEach(m => m.isDefault = false)
    const i = all.value.findIndex(m => m.id === row.id)
    all.value[i].isDefault = true
    ElMessage.success(`已将「${row.name}」设为默认模型`)
    load()
  } else if (cmd === 'stop' || cmd === 'start') {
    const action = cmd === 'stop' ? '停用' : '启用'
    ElMessageBox.confirm(`确定${action}模型「${row.name}」吗？`, `${action}确认`, { type: 'warning' })
      .then(() => {
        const i = all.value.findIndex(m => m.id === row.id)
        all.value[i].status = cmd === 'stop' ? '已停止' : '运行中'
        ElMessage.success(`模型「${action}`)
        load()
      })
      .catch(() => {})
  } else if (cmd === 'delete') {
    ElMessageBox.confirm(`确定删除模型「${row.name}」吗？`, '删除确认', { type: 'warning' })
      .then(() => {
        all.value = all.value.filter(m => m.id !== row.id)
        ElMessage.success('模型已删除')
        load()
      })
      .catch(() => {})
  }
}

load()
</script>

<style scoped>
.model-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.model-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.model-icon.model-chat { background: linear-gradient(135deg, #165dff, #4080ff); }
.model-icon.model-embedding { background: linear-gradient(135deg, #00b42a, #23c343); }
.model-icon.model-multimodal { background: linear-gradient(135deg, #722ed1, #9254de); }
.model-info { min-width: 0; }
.model-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
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
  background: #c9cdd4;
  margin-right: 4px;
  vertical-align: middle;
}
.status-dot.running {
  background: var(--dp-success);
  box-shadow: 0 0 0 2px rgba(0, 180, 42, 0.2);
}

.test-result {
  margin-top: 12px;
  padding: 14px;
  background: var(--dp-bg-page);
  border-radius: 8px;
  border: 1px solid var(--dp-border-light);
}
.test-result-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 8px;
}
.test-result-content {
  font-size: 13px;
  color: var(--dp-text-2);
  line-height: 1.7;
  white-space: pre-wrap;
  font-family: 'JetBrains Mono', Consolas, monospace;
}
</style>
