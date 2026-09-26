<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">KNOWLEDGE BASE</div>
      <h1 class="dp-page-title">知识库管理</h1>
      <p class="dp-page-desc">管理全平台共享知识库，用于智能体和文档中心。</p>
    </div>

    <!-- 统计概览 -->
    <div class="dp-stat-grid">
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>知识库总数</span>
          <div class="dp-stat-icon" style="background: var(--dp-primary-light); color: var(--dp-primary)">
            <el-icon :size="18"><Reading /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ total }}<span class="dp-stat-unit">个</span></div>
        <div class="dp-stat-sub">
          <span class="dp-stat-trend up"><el-icon><Top /></el-icon> 2</span>
          <span>较上周</span>
        </div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>文档总数</span>
          <div class="dp-stat-icon" style="background: var(--dp-success-light); color: var(--dp-success)">
            <el-icon :size="18"><Document /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ totalDocs }}<span class="dp-stat-unit">个</span></div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>向量分片</span>
          <div class="dp-stat-icon" style="background: var(--dp-purple-light); color: var(--dp-purple)">
            <el-icon :size="18"><Grid /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ totalChunks }}<span class="dp-stat-unit">个</span></div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>已构建</span>
          <div class="dp-stat-icon" style="background: var(--dp-cyan-light); color: var(--dp-cyan)">
            <el-icon :size="18"><CircleCheck /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ builtCount }}<span class="dp-stat-unit">个</span></div>
        <div class="dp-stat-sub">
          <span class="dp-tag success">构建中 {{ Math.round(builtCount / total * 100) }}%</span>
        </div>
      </div>
    </div>

    <!-- 知识库列表 -->
    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索知识库名称" clearable style="width: 240px" :prefix-icon="Search" @input="load" />
        <el-select v-model="query.status" placeholder="构建状态" clearable style="width: 130px" @change="load">
          <el-option label="已构建" value="已构建" />
          <el-option label="构建中" value="构建中" />
          <el-option label="待构建" value="待构建" />
        </el-select>
        <el-select v-model="query.model" placeholder="嵌入模型" clearable style="width: 180px" @change="load">
          <el-option label="text-embedding-v3" value="text-embedding-v3" />
          <el-option label="bge-large-zh" value="bge-large-zh" />
          <el-option label="text-embedding-v2" value="text-embedding-v2" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="load">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="openEdit()">创建知识库</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="name" label="知识库名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="docs" label="文档数" width="100" sortable />
        <el-table-column prop="chunks" label="分片数" width="110" sortable>
          <template #default="{ row }">{{ row.chunks.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="embeddedModel" label="嵌入模型" width="160" class-name="dp-mono" />
        <el-table-column prop="status" label="构建状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="plain">
              <el-icon v-if="row.status === '构建中'" style="animation: spin 1s linear infinite; margin-right: 3px"><Loading /></el-icon>
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="90" />
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDocs(row)">文档</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="rebuild(row)" :disabled="row.status === '构建中'">重新构建</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑知识库' : '创建知识库'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="知识库名称" required>
          <el-input v-model="form.name" placeholder="如：平台操作知识库:" />
        </el-form-item>
        <el-form-item label="嵌入模型">
          <el-select v-model="form.embeddedModel" style="width: 100%">
            <el-option label="text-embedding-v3" value="text-embedding-v3" />
            <el-option label="bge-large-zh" value="bge-large-zh" />
            <el-option label="text-embedding-v2" value="text-embedding-v2" />
          </el-select>
        </el-form-item>
        <el-form-item label="分片策略">
          <el-select model-value="auto" style="width: 100%">
            <el-option value="auto" label="自动分片（推荐）" />
            <el-option value="fixed" label="固定长度分片" />
            <el-option value="semantic" label="语义分片" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.owner" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="知识库用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeBases } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const all = ref([...knowledgeBases])
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: '', model: '', page: 1, size: 10 })
const editVisible = ref(false)
const form = reactive({})

const totalDocs = computed(() => all.value.reduce((acc, k) => acc + k.docs, 0))
const totalChunks = computed(() => all.value.reduce((acc, k) => acc + k.chunks, 0))
const builtCount = computed(() => all.value.filter(k => k.status === '已构建').length)

const statusTagType = (s) => ({ '已构建': 'success', '构建中': 'warning', '待构建': 'info' }[s] || 'info')

function load() {
  let list = all.value.filter(r =>
    (!query.keyword || r.name.includes(query.keyword)) &&
    (!query.status || r.status === query.status) &&
    (!query.model || r.embeddedModel === query.model)
  )
  total.value = list.length
  rows.value = list.slice((query.page - 1) * query.size, query.page * query.size)
}

function openEdit(row) {
  Object.assign(form, row || { id: null, name: '', embeddedModel: 'text-embedding-v3', owner: '张伟', desc: '' })
  editVisible.value = true
}

function save() {
  if (!form.name) return ElMessage.warning('请填写知识库名称')
  if (form.id) {
    const i = all.value.findIndex(r => r.id === form.id)
    all.value[i] = { ...all.value[i], ...form, updatedAt: '刚刚' }
  } else {
    all.value.unshift({
      ...form,
      id: Date.now(),
      docs: 0,
      chunks: 0,
      status: '待构建',
      updatedAt: '刚刚'
    })
  }
  editVisible.value = false
  ElMessage.success('知识库已保存')
  load()
}

function rebuild(row) {
  ElMessageBox.confirm(`确定重新构建知识库「${row.name}」吗？构建期间可能影响检索效果。`, '构建确认', { type: 'warning' })
    .then(() => {
      row.status = '构建中'
      ElMessage.success(`知识库「${row.name}」开始重新构建`)
      load()
      // 模拟构建完成
      setTimeout(() => {
        row.status = '已构建'
        row.chunks = row.docs * Math.floor(Math.random() * 30 + 20)
        ElMessage.success(`知识库「${row.name}」构建完成`)
        load()
      }, 3000)
    })
    .catch(() => {})
}

function remove(row) {
  ElMessageBox.confirm(`确定删除知识库「${row.name}」吗？删除后无法恢复。`, '删除确认', { type: 'warning' })
    .then(() => {
      all.value = all.value.filter(r => r.id !== row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

function viewDocs(row) {
  ElMessage.info(`查看知识库「${row.name}」的文档列表（共 ${row.docs} 篇）`)
}

load()
</script>

<style scoped>
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
