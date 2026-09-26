<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">MODEL MANAGEMENT</div>
      <h1 class="dp-page-title">模型管理</h1>
      <p class="dp-page-desc">管理全平台可用的 AI 模型，包括对话模型、Embedding 模型等。</p>
    </div>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid">
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>模型总数</span>
          <div class="dp-stat-icon" style="background: var(--dp-primary-light); color: var(--dp-primary)">
            <el-icon :size="18"><Cpu /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ total }}<span class="dp-stat-unit">个</span></div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>运行中</span>
          <div class="dp-stat-icon" style="background: var(--dp-success-light); color: var(--dp-success)">
            <el-icon :size="18"><CircleCheck /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ runningCount }}<span class="dp-stat-unit">个</span></div>
        <div class="dp-stat-sub">
          <span class="dp-tag success">可用率 {{ Math.round(runningCount / total * 100) }}%</span>
        </div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>对话模型</span>
          <div class="dp-stat-icon" style="background: var(--dp-purple-light); color: var(--dp-purple)">
            <el-icon :size="18"><ChatDotRound /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ chatCount }}<span class="dp-stat-unit">个</span></div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>Embedding 模型</span>
          <div class="dp-stat-icon" style="background: var(--dp-cyan-light); color: var(--dp-cyan)">
            <el-icon :size="18"><MagicStick /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ embeddingCount }}<span class="dp-stat-unit">个</span></div>
      </div>
    </div>

    <!-- 模型列表 -->
    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索模型名称 / 提供商" clearable style="width: 260px" :prefix-icon="Search" @input="load" />
        <el-select v-model="query.type" placeholder="模型类型" clearable style="width: 130px" @change="load">
          <el-option label="对话模型" value="对话模型" />
          <el-option label="Embedding" value="Embedding" />
          <el-option label="多模型" value="多模型" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
          <el-option label="运行中" value="运行中" />
          <el-option label="已停止" value="已停止" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="load">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="openEdit()">新增模型</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="name" label="模型名称" min-width="160">
          <template #default="{ row }">
            <div class="dp-flex dp-gap-8">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.isDefault" type="warning" size="small" effect="dark">默认</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="provider" label="提供商" width="140" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="context" label="上下文长度" width="110" />
        <el-table-column prop="qpsLimit" label="QPS 限制" width="100" sortable />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '运行中' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="是否默认" width="90">
          <template #default="{ row }">
            <el-switch :model-value="row.isDefault" size="small" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="testConnection(row)">测试连接</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              type="warning"
              size="small"
              @click="setDefault(row)"
              :disabled="row.isDefault || row.status !== '运行中'"
            >设为默认</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑模型' : '新增模型'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="110px">
        <el-form-item label="模型名称" required>
          <el-input v-model="form.name" placeholder="如：DeepSeek-V3" />
        </el-form-item>
        <el-form-item label="提供商" required>
          <el-select v-model="form.provider" style="width: 100%">
            <el-option label="本地部署" value="本地部署" />
            <el-option label="DeepSeek开放平台" value="DeepSeek开放平台" />
            <el-option label="阿里云百炼" value="阿里云百炼" />
            <el-option label="智谱AI" value="智谱AI" />
            <el-option label="OpenAI" value="OpenAI" />
            <el-option label="火山方舟" value="火山方舟" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型类型">
          <el-radio-group v-model="form.type">
            <el-radio-button value="对话模型">对话模型</el-radio-button>
            <el-radio-button value="Embedding">Embedding</el-radio-button>
            <el-radio-button value="多模型">多模型</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="上下文长度">
          <el-select v-model="form.context" style="width: 100%">
            <el-option label="4K" value="4K" />
            <el-option label="8K" value="8K" />
            <el-option label="32K" value="32K" />
            <el-option label="64K" value="64K" />
            <el-option label="128K" value="128K" />
            <el-option label="256K" value="256K" />
          </el-select>
        </el-form-item>
        <el-form-item label="QPS 限制">
          <el-input-number v-model="form.qpsLimit" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" placeholder="请输入 API Key" show-password />
        </el-form-item>
        <el-form-item label="Endpoint">
          <el-input v-model="form.endpoint" placeholder="https://api.example.com/v1" class="dp-mono" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.owner" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button @click="testConnection(form)">测试连接</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiModels } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const all = ref([...aiModels])
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', type: '', status: '', page: 1, size: 10 })
const editVisible = ref(false)
const form = reactive({})

const runningCount = computed(() => all.value.filter(m => m.status === '运行中').length)
const chatCount = computed(() => all.value.filter(m => m.type === '对话模型').length)
const embeddingCount = computed(() => all.value.filter(m => m.type === 'Embedding').length)

const typeTagType = (t) => ({ 对话模型: 'primary', Embedding: 'success', '多模型': 'warning' }[t] || 'info')

function load() {
  let list = all.value.filter(r =>
    (!query.keyword || r.name.includes(query.keyword) || r.provider.includes(query.keyword)) &&
    (!query.type || r.type === query.type) &&
    (!query.status || r.status === query.status)
  )
  total.value = list.length
  rows.value = list.slice((query.page - 1) * query.size, query.page * query.size)
}

function openEdit(row) {
  Object.assign(form, row || {
    id: null, name: '', provider: '本地部署', type: '对话模型',
    context: '32K', qpsLimit: 10, apiKey: '', endpoint: '', owner: '张伟'
  })
  editVisible.value = true
}

function save() {
  if (!form.name || !form.provider) return ElMessage.warning('请填写模型名称和提供商')
  if (form.id) {
    const i = all.value.findIndex(r => r.id === form.id)
    all.value[i] = { ...all.value[i], ...form, updatedAt: '刚刚' }
  } else {
    all.value.unshift({
      ...form,
      id: Date.now(),
      status: '运行中',
      isDefault: false,
      updatedAt: '刚刚'
    })
  }
  editVisible.value = false
  ElMessage.success('模型已保存')
  load()
}

function testConnection(row) {
  ElMessage({ message: `正在测试模型「${row.name}」的连接...`, type: 'info', duration: 1500 })
  setTimeout(() => {
    ElMessage.success(`模型「${row.name}」连接测试成功，响应时间 128ms`)
  }, 1500)
}

function setDefault(row) {
  ElMessageBox.confirm(`确定将「${row.name}」设为默认模型吗？`, '提示', { type: 'warning' })
    .then(() => {
      all.value.forEach(m => { m.isDefault = false })
      row.isDefault = true
      ElMessage.success(`「${row.name}」已设为默认模型`)
      load()
    })
    .catch(() => {})
}

function remove(row) {
  if (row.isDefault) return ElMessage.warning('默认模型不可删除，请先设置其他模型为默认')
  ElMessageBox.confirm(`确定删除模型「${row.name}」吗？`, '删除确认', { type: 'warning' })
    .then(() => {
      all.value = all.value.filter(r => r.id !== row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

load()
</script>

<style scoped>
</style>
