<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div>
        <div class="dp-page-eyebrow">PUBLISH APPS</div>
        <h2 class="dp-page-title">发布应用</h2>
        <p class="dp-page-desc">将智能体发布为独立页面、应用广场或 API 接口，供业务方使用。</p>
      </div>
      <div class="dp-page-header-actions">
        <el-button type="primary" :icon="Plus" @click="openPublish">发布新应用</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid">
      <StatCard label="智能体总数" :value="stats.total" unit="个" icon="Cpu" color="#165dff" bg="#e8f3ff" :trend="8" />
      <StatCard label="已发布" :value="stats.published" unit="个" icon="CircleCheck" color="#00b42a" bg="#e8ffea" :trend="5" />
      <StatCard label="月调用量" :value="stats.totalCalls" unit="万次" icon="Histogram" color="#722ed1" bg="#f5e8ff" :trend="12" />
      <StatCard label="平均满意度" :value="stats.avgSatisfaction" unit="个" icon="Star" color="#ff7d00" bg="#fff3e8" :trend="2" />
    </div>

    <!-- 表格 -->
    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索智能体名称" clearable style="width: 260px" :prefix-icon="Search" @input="load" />
        <el-select v-model="query.model" placeholder="绑定模型" clearable style="width: 160px" @change="load">
          <el-option label="DeepSeek-V3" value="DeepSeek-V3" />
          <el-option label="Qwen-Max" value="Qwen-Max" />
          <el-option label="GLM-4-Plus" value="GLM-4-Plus" />
          <el-option label="GPT-4o" value="GPT-4o" />
          <el-option label="本地Llama3-70B" value="本地Llama3-70B" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
          <el-option label="已发布" value="已发布" />
          <el-option label="调试中" value="调试中" />
          <el-option label="已下线" value="已下线" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="name" label="智能体名称" min-width="180">
          <template #default="{ row }">
            <div class="agent-cell">
              <div class="agent-cell-icon" :style="{ background: getAgentColor(row.id) }">
                <el-icon :size="14"><Cpu /></el-icon>
              </div>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="model" label="绑定模型" width="140">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain" class="dp-mono">{{ row.model }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="knowledge" label="知识库数量" width="100" sortable>
          <template #default="{ row }">{{ row.knowledge }} 个</template>
        </el-table-column>
        <el-table-column prop="tools" label="工具数" width="90" sortable>
          <template #default="{ row }">{{ row.tools }} 个</template>
        </el-table-column>
        <el-table-column prop="callsMonth" label="月调用量" width="120" sortable>
          <template #default="{ row }">{{ formatNumber(row.callsMonth) }}</template>
        </el-table-column>
        <el-table-column prop="satisfaction" label="满意度" width="110" sortable>
          <template #default="{ row }">
            <div class="satisfaction-cell">
              <el-rate v-model="row.satisfaction" disabled :max="5" :show-text="false" size="small" />
              <span>{{ row.satisfaction.toFixed(1) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column prop="updatedAt" label="更新时间" width="140" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handlePublishPage(row)">独立页面</el-button>
            <el-button link type="primary" size="small" @click="handlePublishSquare(row)">应用广场</el-button>
            <el-button link type="primary" size="small" @click="handlePublishApi(row)">API接口</el-button>
            <el-dropdown @command="cmd => handleMore(cmd, row)" trigger="click">
              <el-button link type="primary" size="small">
                更多<el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="offline" :disabled="row.status === '已下线'">下线</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <!-- 发布对话框 -->
    <el-dialog v-model="publishVisible" :title="publishTitle" width="520px" destroy-on-close>
      <el-form :model="publishForm" label-width="100px">
        <el-form-item label="智能体">
          <el-input v-model="publishForm.agentName" readonly />
        </el-form-item>
        <el-form-item label="发布方式">
          <el-radio-group v-model="publishForm.type">
            <el-radio value="page">独立页面</el-radio>
            <el-radio value="square">应用广场</el-radio>
            <el-radio value="api">API接口</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="应用名称">
          <el-input v-model="publishForm.appName" placeholder="展示给用户的名称" />
        </el-form-item>
        <el-form-item label="应用描述">
          <el-input v-model="publishForm.desc" type="textarea" :rows="3" placeholder="简要描述应用功能" />
        </el-form-item>
        <el-form-item v-if="publishForm.type === 'api'" label="调用限额">
          <el-input-number v-model="publishForm.rateLimit" :min="10" :max="10000" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">次 / 分钟</span>
        </el-form-item>
        <el-form-item label="访问权限">
          <el-radio-group v-model="publishForm.access">
            <el-radio value="public">公开</el-radio>
            <el-radio value="internal">内部</el-radio>
            <el-radio value="private">指定人员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="publishVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPublish">确认发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatCard from '@/components/StatCard.vue'
import { agents } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const colors = ['#165dff', '#00b42a', '#722ed1', '#ff7d00', '#f759ab', '#0fc6c2', '#f53f3f', '#86909c']

const all = ref([...agents])
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', model: '', status: '', page: 1, size: 10 })
const publishVisible = ref(false)
const publishTitle = ref('发布应用')
const publishForm = reactive({})

const stats = computed(() => ({
  total: all.value.length,
  published: all.value.filter(a => a.status === '已发布').length,
  totalCalls: (all.value.reduce((s, a) => s + a.callsMonth, 0) / 10000).toFixed(1),
  avgSatisfaction: (all.value.reduce((s, a) => s + a.satisfaction, 0) / all.value.length).toFixed(1)
}))

const statusTagType = (s) => ({
  '已发布': 'success',
  '调试中': 'warning',
  '已下线': 'info'
}[s] || 'info')

function getAgentColor(id) {
  return colors[Math.abs(id) % colors.length]
}

function formatNumber(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

function load() {
  let list = all.value.filter(a =>
    (!query.keyword || a.name.includes(query.keyword)) &&
    (!query.model || a.model === query.model) &&
    (!query.status || a.status === query.status)
  )
  total.value = list.length
  rows.value = list.slice((query.page - 1) * query.size, query.page * query.size)
}

function openPublish() {
  publishTitle.value = '发布新应用'
  Object.assign(publishForm, {
    agentId: null,
    agentName: '',
    type: 'page',
    appName: '',
    desc: '',
    rateLimit: 1000,
    access: 'internal'
  })
  publishVisible.value = true
}

function handlePublishPage(row) {
  publishTitle.value = '发布为独立页面'
  Object.assign(publishForm, {
    agentId: row.id,
    agentName: row.name,
    type: 'page',
    appName: row.name,
    desc: '',
    rateLimit: 1000,
    access: 'internal'
  })
  publishVisible.value = true
}

function handlePublishSquare(row) {
  publishTitle.value = '发布到应用广场'
  Object.assign(publishForm, {
    agentId: row.id,
    agentName: row.name,
    type: 'square',
    appName: row.name,
    desc: '',
    rateLimit: 1000,
    access: 'public'
  })
  publishVisible.value = true
}

function handlePublishApi(row) {
  publishTitle.value = '发布为 API 接口'
  Object.assign(publishForm, {
    agentId: row.id,
    agentName: row.name,
    type: 'api',
    appName: row.name,
    desc: '',
    rateLimit: 1000,
    access: 'internal'
  })
  publishVisible.value = true
}

function confirmPublish() {
  if (!publishForm.appName) return ElMessage.warning('请输入应用名称')
  ElMessage.success('发布成功')
  publishVisible.value = false
}

function handleMore(cmd, row) {
  if (cmd === 'edit') {
    ElMessage.info('编辑功能开发中')
  } else if (cmd === 'offline') {
    ElMessageBox.confirm(`确定下线智能体「${row.name}」吗？`, '下线确认', { type: 'warning' })
      .then(() => {
        const i = all.value.findIndex(a => a.id === row.id)
        all.value[i].status = '已下线'
        ElMessage.success('已下线')
        load()
      })
      .catch(() => {})
  } else if (cmd === 'delete') {
    ElMessageBox.confirm(`确定删除智能体「${row.name}」吗？`, '删除确认', { type: 'warning' })
      .then(() => {
        all.value = all.value.filter(a => a.id !== row.id)
        ElMessage.success('已删除')
        load()
      })
      .catch(() => {})
  }
}

load()
</script>

<style scoped>
.agent-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.agent-cell-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.satisfaction-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}
.satisfaction-cell :deep(.el-rate) { height: auto; }
.satisfaction-cell span {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-warning);
  font-family: 'DIN Alternate', sans-serif;
}
</style>
