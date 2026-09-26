<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div>
        <div class="dp-page-eyebrow">KNOWLEDGE BASE</div>
        <h2 class="dp-page-title">知识资产</h2>
        <p class="dp-page-desc">管理知识库和文档，为智能体提供业务知识和平台知识支撑。</p>
      </div>
      <div class="dp-page-header-actions">
        <el-button type="primary" :icon="Plus" @click="openCreate">新建知识库</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid">
      <StatCard label="知识库数量" :value="kbStats.total" unit="个" icon="Collection" color="#165dff" bg="#e8f3ff" />
      <StatCard label="文档总数" :value="kbStats.docs" unit="个" icon="Document" color="#00b42a" bg="#e8ffea" />
      <StatCard label="已构建" :value="kbStats.built" unit="个" icon="CircleCheck" color="#722ed1" bg="#f5e8ff" />
      <StatCard label="构建中" :value="kbStats.building" unit="个" icon="Loading" color="#ff7d00" bg="#fff3e8" />
    </div>

    <!-- 工具栏 + 知识库卡片列表 -->
    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索知识库名称" clearable style="width: 260px" :prefix-icon="Search" @input="load" />
        <el-select v-model="query.status" placeholder="状态筛选" clearable style="width: 140px" @change="load">
          <el-option label="已构建" value="已构建" />
          <el-option label="构建中" value="构建中" />
          <el-option label="待构建" value="待构建" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </div>

      <div class="kb-grid">
        <div v-for="kb in filteredList" :key="kb.id" class="kb-card dp-fade-up">
          <div class="kb-card-header">
            <div class="kb-icon">
              <el-icon :size="20"><Collection /></el-icon>
            </div>
            <div class="kb-title">{{ kb.name }}</div>
            <el-tag :type="statusTagType(kb.status)" size="small" effect="plain">{{ kb.status }}</el-tag>
          </div>
          <div class="kb-stats">
            <div class="kb-stat-item">
              <div class="kb-stat-value">{{ kb.docs }}</div>
              <div class="kb-stat-label">文档数</div>
            </div>
            <div class="kb-stat-divider"></div>
            <div class="kb-stat-item">
              <div class="kb-stat-value">{{ kb.chunks.toLocaleString() }}</div>
              <div class="kb-stat-label">Chunk 数</div>
            </div>
            <div class="kb-stat-divider"></div>
            <div class="kb-stat-item">
              <div class="kb-stat-value kb-model">{{ kb.embeddedModel }}</div>
              <div class="kb-stat-label">嵌入模型</div>
            </div>
          </div>
          <div class="kb-footer">
            <div class="kb-meta">
              <el-icon :size="12"><User /></el-icon>
              <span>{{ kb.owner }}</span>
              <el-icon :size="12" style="margin-left: 10px"><Clock /></el-icon>
              <span>{{ kb.updatedAt }}</span>
            </div>
            <div class="kb-actions">
              <el-button link type="primary" size="small" @click="handleEdit(kb)">编辑</el-button>
              <el-button link type="primary" size="small" @click="handleRebuild(kb)">重新解析</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(kb)">删除</el-button>
            </div>
          </div>
        </div>
      </div>

      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <!-- 新建/编辑知识库对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑知识库' : '新建知识库'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="知识库名称" required>
          <el-input v-model="form.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="嵌入模型">
          <el-select v-model="form.embeddedModel" style="width: 100%">
            <el-option label="text-embedding-v3" value="text-embedding-v3" />
            <el-option label="bge-large-zh" value="bge-large-zh" />
            <el-option label="text-embedding-v2" value="text-embedding-v2" />
          </el-select>
        </el-form-item>
        <el-form-item label="Chunk 大小">
          <el-input-number v-model="form.chunkSize" :min="100" :max="2000" :step="50" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">token</span>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.owner" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="知识库用途和内容说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import { knowledgeBases } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const all = ref([...knowledgeBases])
const query = reactive({ keyword: '', status: '', page: 1, size: 8 })
const total = ref(0)
const filteredList = ref([])
const dialogVisible = ref(false)
const form = reactive({})

const kbStats = computed(() => ({
  total: all.value.length,
  docs: all.value.reduce((s, k) => s + k.docs, 0),
  built: all.value.filter(k => k.status === '已构建').length,
  building: all.value.filter(k => k.status === '构建中').length
}))

const statusTagType = (s) => ({
  '已构建': 'success',
  '构建中': 'warning',
  '待构建': 'info'
}[s] || 'info')

function load() {
  let list = all.value.filter(k =>
    (!query.keyword || k.name.includes(query.keyword)) &&
    (!query.status || k.status === query.status)
  )
  total.value = list.length
  filteredList.value = list.slice((query.page - 1) * query.size, query.page * query.size)
}

function openCreate() {
  Object.assign(form, { id: null, name: '', embeddedModel: 'text-embedding-v3', chunkSize: 500, owner: '张伟', desc: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  Object.assign(form, { ...row, chunkSize: 500 })
  dialogVisible.value = true
}

function save() {
  if (!form.name) return ElMessage.warning('请输入知识库名称')
  if (form.id) {
    const i = all.value.findIndex(k => k.id === form.id)
    all.value[i] = { ...all.value[i], ...form }
    ElMessage.success('知识库已更新')
  } else {
    all.value.unshift({
      ...form,
      id: Date.now(),
      docs: 0,
      chunks: 0,
      status: '待构建',
      updatedAt: '刚刚'
    })
    ElMessage.success('知识库已创建')
  }
  dialogVisible.value = false
  load()
}

function handleRebuild(row) {
  ElMessageBox.confirm(`确定重新解析知识库「${row.name}」吗？`, '重新解析确认', { type: 'warning' })
    .then(() => {
      const i = all.value.findIndex(k => k.id === row.id)
      all.value[i].status = '构建中'
      ElMessage.success('已开始重新解析')
      load()
      setTimeout(() => {
        all.value[i].status = '已构建'
        load()
      }, 3000)
    })
    .catch(() => {})
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除知识库「${row.name}」吗？此操作不可恢复。`, '删除确认', { type: 'warning' })
    .then(() => {
      all.value = all.value.filter(k => k.id !== row.id)
      ElMessage.success('知识库已删除')
      load()
    })
    .catch(() => {})
}

load()
</script>

<style scoped>
.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 14px;
  margin-bottom: 8px;
}
.kb-card {
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: 10px;
  padding: 16px;
  transition: all 0.25s;
  cursor: pointer;
}
.kb-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 4px 12px rgba(22, 93, 255, 0.1);
  transform: translateY(-2px);
}
.kb-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.kb-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.kb-title {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kb-stats {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-top: 1px dashed var(--dp-border-light);
  border-bottom: 1px dashed var(--dp-border-light);
  margin-bottom: 12px;
}
.kb-stat-item {
  flex: 1;
  text-align: center;
  min-width: 0;
}
.kb-stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
  margin-bottom: 4px;
}
.kb-stat-value.kb-model {
  font-size: 12px;
  font-weight: 500;
  color: var(--dp-text-2);
  font-family: 'JetBrains Mono', Consolas, monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 4px;
}
.kb-stat-label {
  font-size: 12px;
  color: var(--dp-text-3);
}
.kb-stat-divider {
  width: 1px;
  height: 30px;
  background: var(--dp-border-light);
}
.kb-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.kb-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--dp-text-3);
}
.kb-actions {
  display: flex;
  gap: 4px;
}
</style>
