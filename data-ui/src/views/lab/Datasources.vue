<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div>
        <div class="dp-page-eyebrow">DATASOURCE MANAGEMENT</div>
        <h2 class="dp-page-title">数据源管理</h2>
        <p class="dp-page-desc">管理实验环境中的数据源连接，支持多种数据库和文件类型的接入与配置。</p>
      </div>
      <div class="dp-page-header-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增数据源</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid">
      <StatCard label="数据源总数" :value="stats.total" unit="个" icon="Coin" color="#165dff" bg="#e8f3ff" :trend="6" />
      <StatCard label="已连接" :value="stats.connected" unit="个" icon="CircleCheck" color="#00b42a" bg="#e8ffea" :trend="4" />
      <StatCard label="异常连接" :value="stats.error" unit="个" icon="Warning" color="#f53f3f" bg="#ffece8" :trend="-1" />
      <StatCard label="数据总量" :value="stats.totalSize" unit="TB" icon="Files" color="#722ed1" bg="#f5e8ff" />
    </div>

    <!-- 表格 -->
    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索数据源名称" clearable style="width: 260px" :prefix-icon="Search" @input="load" />
        <el-select v-model="query.type" placeholder="数据源类型" clearable style="width: 150px" @change="load">
          <el-option label="MySQL" value="MySQL" />
          <el-option label="Oracle" value="Oracle" />
          <el-option label="PostgreSQL" value="PostgreSQL" />
          <el-option label="Hive" value="Hive" />
          <el-option label="Kafka" value="Kafka" />
          <el-option label="ClickHouse" value="ClickHouse" />
          <el-option label="API" value="API" />
          <el-option label="FTP" value="FTP" />
        </el-select>
        <el-select v-model="query.env" placeholder="环境" clearable style="width: 120px" @change="load">
          <el-option label="生产" value="生产" />
          <el-option label="测试" value="测试" />
          <el-option label="开发" value="开发" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
          <el-option label="已连接" value="已连接" />
          <el-option label="未连接" value="未连接" />
          <el-option label="连接异常" value="连接异常" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Upload" @click="batchImport">批量导入</el-button>
          <el-button :icon="Refresh" @click="load">刷新</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="name" label="数据源名称" min-width="200">
          <template #default="{ row }">
            <div class="ds-cell">
              <div class="ds-icon" :class="'ds-' + typeClass(row.type)">
                <el-icon :size="16"><component :is="typeIcon(row.type)" /></el-icon>
              </div>
              <div class="ds-info">
                <div class="ds-name">{{ row.name }}</div>
                <div class="ds-db dp-desc dp-mono">{{ row.database }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="env" label="环境" width="80">
          <template #default="{ row }">
            <el-tag :type="envTagType(row.env)" size="small" effect="plain">{{ row.env }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机地址" width="180" class-name="dp-mono" />
        <el-table-column prop="tables" label="表数量" width="90" align="center" sortable />
        <el-table-column prop="sizeGB" label="数据量" width="100" sortable>
          <template #default="{ row }">
            <span v-if="row.sizeGB >= 1000">{{ (row.sizeGB / 1000).toFixed(2) }} TB</span>
            <span v-else>{{ row.sizeGB }} GB</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              <span class="status-dot" :class="statusDotClass(row.status)"></span>
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column prop="updatedAt" label="更新时间" width="140" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleTest(row)">测试</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-dropdown @command="cmd => handleMore(cmd, row)" trigger="click">
              <el-button link type="primary" size="small">
                更多<el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="detail">详情</el-dropdown-item>
                  <el-dropdown-item command="sync">立即同步</el-dropdown-item>
                  <el-dropdown-item command="clone">克隆</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑数据源' : '新增数据源'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="数据源名称" required>
          <el-input v-model="form.name" placeholder="请输入数据源名称" />
        </el-form-item>
        <el-form-item label="数据源类型" required>
          <el-select v-model="form.type" style="width: 100%" @change="onTypeChange">
            <el-option label="MySQL" value="MySQL" />
            <el-option label="Oracle" value="Oracle" />
            <el-option label="PostgreSQL" value="PostgreSQL" />
            <el-option label="Hive" value="Hive" />
            <el-option label="ClickHouse" value="ClickHouse" />
            <el-option label="Kafka" value="Kafka" />
            <el-option label="API" value="API" />
          </el-select>
        </el-form-item>
        <el-form-item label="环境">
          <el-radio-group v-model="form.env">
            <el-radio value="开发">开发</el-radio>
            <el-radio value="测试">测试</el-radio>
            <el-radio value="生产">生产</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.type !== 'API'" label="主机地址" required>
          <el-input v-model="form.host" placeholder="host:port" />
        </el-form-item>
        <el-form-item v-if="form.type !== 'API' && form.type !== 'Kafka'" label="数据库名">
          <el-input v-model="form.database" placeholder="database name" />
        </el-form-item>
        <el-form-item v-if="form.type === 'API'" label="API 地址">
          <el-input v-model="form.host" placeholder="https://api.example.com" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.owner" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.desc" type="textarea" :rows="2" placeholder="数据源用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testConnection">测试连接</el-button>
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
import { dataSources } from '@/mock'
import { Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'

const all = ref([...dataSources])
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', type: '', env: '', status: '', page: 1, size: 10 })
const dialogVisible = ref(false)
const form = reactive({})

const stats = computed(() => ({
  total: all.value.length,
  connected: all.value.filter(d => d.status === '已连接').length,
  error: all.value.filter(d => d.status === '连接异常').length,
  totalSize: (all.value.reduce((s, d) => s + d.sizeGB, 0) / 1000).toFixed(1)
}))

function typeClass(type) {
  const map = {
    MySQL: 'mysql', Oracle: 'oracle', PostgreSQL: 'pg',
    Hive: 'hive', Kafka: 'kafka', ClickHouse: 'ck',
    API: 'api', FTP: 'ftp'
  }
  return map[type] || 'mysql'
}

function typeIcon(type) {
  const map = {
    MySQL: 'Coin', Oracle: 'Coin', PostgreSQL: 'Coin',
    Hive: 'Coin', Kafka: 'Connection', ClickHouse: 'Coin',
    API: 'Link', FTP: 'Upload'
  }
  return map[type] || 'Coin'
}

function envTagType(env) {
  return { '生产': 'danger', '测试': 'warning', '开发': 'success' }[env] || 'info'
}

function statusTagType(status) {
  return { '已连接': 'success', '未连接': 'info', '连接异常': 'danger' }[status] || 'info'
}

function statusDotClass(status) {
  return { '已连接': 'success', '未连接': 'idle', '连接异常': 'error' }[status] || ''
}

function load() {
  let list = all.value.filter(d =>
    (!query.keyword || d.name.includes(query.keyword) || d.host.includes(query.keyword)) &&
    (!query.type || d.type === query.type) &&
    (!query.env || d.env === query.env) &&
    (!query.status || d.status === query.status)
  )
  total.value = list.length
  rows.value = list.slice((query.page - 1) * query.size, query.page * query.size)
}

function openAdd() {
  Object.assign(form, {
    id: null, name: '', type: 'MySQL', env: '开发',
    host: '', database: '', username: '', password: '',
    owner: '张伟', desc: ''
  })
  dialogVisible.value = true
}

function onTypeChange(type) {
  if (type === 'MySQL') form.host = 'localhost:3306'
  else if (type === 'Oracle') form.host = 'localhost:1521'
  else if (type === 'PostgreSQL') form.host = 'localhost:5432'
  else if (type === 'Hive') form.host = 'localhost:10000'
  else if (type === 'ClickHouse') form.host = 'localhost:8123'
  else if (type === 'Kafka') form.host = 'localhost:9092'
  else if (type === 'API') form.host = 'https://'
}

function save() {
  if (!form.name) return ElMessage.warning('请输入数据源名称')
  if (!form.host) return ElMessage.warning('请输入主机地址')
  if (form.id) {
    const i = all.value.findIndex(d => d.id === form.id)
    all.value[i] = { ...all.value[i], ...form, updatedAt: '刚刚' }
    ElMessage.success('数据源已更新')
  } else {
    all.value.unshift({
      ...form,
      id: Date.now(),
      tables: 0,
      sizeGB: 0,
      status: '未连接',
      updatedAt: '刚刚'
    })
    ElMessage.success('数据源已添加')
  }
  dialogVisible.value = false
  load()
}

function handleEdit(row) {
  Object.assign(form, { ...row, username: 'admin', password: '******' })
  dialogVisible.value = true
}

function handleTest(row) {
  ElMessage.info(`正在测试「${row.name}」连接...`)
  setTimeout(() => {
    ElMessage.success('连接测试成功')
  }, 1000)
}

function testConnection() {
  if (!form.host) return ElMessage.warning('请先填写连接信息')
  ElMessage.info('正在测试连接...')
  setTimeout(() => {
    ElMessage.success('连接测试成功')
  }, 1000)
}

function batchImport() {
  ElMessage.info('批量导入功能开发中')
}

function handleMore(cmd, row) {
  if (cmd === 'detail') {
    ElMessage.info(`查看「${row.name}」详情`)
  } else if (cmd === 'sync') {
    ElMessage.success(`数据源「${row.name}」同步任务已启动`)
  } else if (cmd === 'clone') {
    const newDs = { ...row, id: Date.now(), name: row.name + '-副本', status: '未连接' }
    all.value.unshift(newDs)
    ElMessage.success('已克隆数据源')
    load()
  } else if (cmd === 'delete') {
    ElMessageBox.confirm(`确定删除数据源「${row.name}」吗？`, '删除确认', { type: 'warning' })
      .then(() => {
        all.value = all.value.filter(d => d.id !== row.id)
        ElMessage.success('数据源已删除')
        load()
      })
      .catch(() => {})
  }
}

load()
</script>

<style scoped>
.ds-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ds-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.ds-icon.ds-mysql { background: linear-gradient(135deg, #00758f, #00a3c4); }
.ds-icon.ds-oracle { background: linear-gradient(135deg, #e11d48, #f43f5e); }
.ds-icon.ds-pg { background: linear-gradient(135deg, #336791, #4479a1); }
.ds-icon.ds-hive { background: linear-gradient(135deg, #6b7280, #9ca3af); }
.ds-icon.ds-kafka { background: linear-gradient(135deg, #231f20, #4a4a4a); }
.ds-icon.ds-ck { background: linear-gradient(135deg, #ffcc01, #ffe066); color: #1d2129; }
.ds-icon.ds-api { background: linear-gradient(135deg, #722ed1, #9254de); }
.ds-icon.ds-ftp { background: linear-gradient(135deg, #0fc6c2, #14c9c9); }
.ds-info { min-width: 0; }
.ds-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
}
.ds-db {
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
.status-dot.success {
  background: var(--dp-success);
  box-shadow: 0 0 0 2px rgba(0, 180, 42, 0.2);
}
.status-dot.error {
  background: var(--dp-danger);
  box-shadow: 0 0 0 2px rgba(245, 63, 63, 0.2);
}
.status-dot.idle {
  background: var(--dp-text-4);
}
</style>
