<template>
  <div class="dp-page">
    <PageHeader title="数据源管理" desc="纳管外部数据库、消息队列与 API 数据源，支持连通性测试与统一配置">
      <el-button type="primary" :icon="Plus" @click="openEdit()">新增数据源</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索数据源名称 / 主机地址"
          clearable
          style="width: 260px"
          :prefix-icon="Search"
          @input="handleSearch"
        />
        <el-select v-model="query.type" placeholder="数据源类型" clearable style="width: 150px" @change="handleSearch">
          <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-select v-model="query.env" placeholder="所属环境" clearable style="width: 120px" @change="handleSearch">
          <el-option label="生产环境" value="生产" />
          <el-option label="测试环境" value="测试" />
          <el-option label="开发环境" value="开发" />
        </el-select>
        <el-select v-model="query.status" placeholder="连接状态" clearable style="width: 130px" @change="handleSearch">
          <el-option label="已连接" value="已连接" />
          <el-option label="未连接" value="未连接" />
          <el-option label="连接异常" value="连接异常" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        </div>
      </div>

      <el-table :data="pagedList" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="name" label="数据源名称" min-width="180">
          <template #default="{ row }">
            <div class="ds-name-cell">
              <div class="ds-icon" :class="row.type.toLowerCase()">
                <el-icon :size="16"><component :is="getTypeIcon(row.type)" /></el-icon>
              </div>
              <span class="ds-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <span class="ds-type-tag" :class="row.type.toLowerCase()">{{ row.type }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="env" label="环境" width="90">
          <template #default="{ row }">
            <el-tag :type="envTagType(row.env)" size="small" effect="plain">{{ row.env }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="连接地址" min-width="170" class-name="dp-mono" />
        <el-table-column prop="database" label="数据库" min-width="150" class-name="dp-mono" />
        <el-table-column prop="tables" label="表数量" width="90" sortable align="center" />
        <el-table-column prop="sizeGB" label="容量(GB)" width="100" sortable align="center">
          <template #default="{ row }">
            <span class="ds-size">{{ formatSize(row.sizeGB) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <span class="ds-status">
              <span class="ds-status-dot" :class="statusDotClass(row.status)"></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="90" />
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="testConn(row)">测试连接</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="filteredList.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑数据源' : '新增数据源'" width="680px" :close-on-click-modal="false">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="数据源名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入数据源名称" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据源类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择数据源类型" style="width: 100%">
                <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value">
                  <span class="option-icon">
                    <el-icon><component :is="getTypeIcon(t.value)" /></el-icon>
                  </span>
                  {{ t.label }}
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="所属环境" prop="env">
          <el-radio-group v-model="form.env">
            <el-radio-button value="生产">生产环境</el-radio-button>
            <el-radio-button value="测试">测试环境</el-radio-button>
            <el-radio-button value="开发">开发环境</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="主机地址" prop="host">
              <el-input v-model="form.host" placeholder="例如：192.168.1.100" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="端口" prop="port">
              <el-input v-model="form.port" placeholder="端口号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="数据库名" prop="database">
              <el-input v-model="form.database" placeholder="请输入数据库名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="负责人" prop="owner">
              <el-select v-model="form.owner" placeholder="请选择负责人" filterable style="width: 100%">
                <el-option v-for="o in ownerList" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="连接超时(秒)" prop="timeout">
              <el-input-number v-model="form.timeout" :min="1" :max="300" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="描述">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="请输入数据源描述信息" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :icon="Connection" :loading="testing" @click="handleTestConn">测试连接</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Connection, Check } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { dataSources, owners } from '@/mock'

const loading = ref(false)
const testing = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const ownerList = owners

const query = reactive({
  keyword: '',
  type: '',
  env: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10
})

const typeOptions = [
  { value: 'MySQL', label: 'MySQL' },
  { value: 'Oracle', label: 'Oracle' },
  { value: 'PostgreSQL', label: 'PostgreSQL' },
  { value: 'Hive', label: 'Hive' },
  { value: 'Kafka', label: 'Kafka' },
  { value: 'API', label: 'API' },
  { value: 'FTP', label: 'FTP' },
  { value: 'ClickHouse', label: 'ClickHouse' }
]

const form = reactive({
  id: null,
  name: '',
  type: '',
  env: '开发',
  host: '',
  port: '3306',
  database: '',
  username: '',
  password: '',
  owner: '',
  timeout: 30,
  desc: ''
})

const formRules = {
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  env: [{ required: true, message: '请选择所属环境', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  database: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  owner: [{ required: true, message: '请选择负责人', trigger: 'change' }]
}

const dataList = ref([...dataSources])

const filteredList = computed(() => {
  return dataList.filter(item => {
    if (query.keyword) {
      const kw = query.keyword.toLowerCase()
      if (!item.name.toLowerCase().includes(kw) && !item.host.toLowerCase().includes(kw)) {
        return false
      }
    }
    if (query.type && item.type !== query.type) return false
    if (query.env && item.env !== query.env) return false
    if (query.status && item.status !== query.status) return false
    return true
  })
})

const pagedList = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filteredList.value.slice(start, start + pagination.size)
})

function getTypeIcon(type) {
  const map = {
    MySQL: 'Coin',
    Oracle: 'Coin',
    PostgreSQL: 'Coin',
    Hive: 'Histogram',
    Kafka: 'VideoPlay',
    API: 'Link',
    FTP: 'FolderOpened',
    ClickHouse: 'DataBoard'
  }
  return map[type] || 'Database'
}

function envTagType(env) {
  const map = { '生产': 'danger', '测试': 'warning', '开发': 'info' }
  return map[env] || 'info'
}

function statusDotClass(status) {
  const map = { '已连接': 'ok', '未连接': 'warn', '连接异常': 'err' }
  return map[status] || 'warn'
}

function formatSize(gb) {
  if (gb >= 1000) return (gb / 1000).toFixed(1) + ' TB'
  return gb + ' GB'
}

function handleSearch() {
  pagination.page = 1
}

function handleRefresh() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('数据已刷新')
  }, 600)
}

function openEdit(row) {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      type: row.type,
      env: row.env,
      host: row.host.split(':')[0],
      port: row.host.split(':')[1] || '3306',
      database: row.database,
      username: 'admin',
      password: '******',
      owner: row.owner,
      timeout: 30,
      desc: ''
    })
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      type: '',
      env: '开发',
      host: '',
      port: '3306',
      database: '',
      username: '',
      password: '',
      owner: '',
      timeout: 30,
      desc: ''
    })
  }
  dialogVisible.value = true
}

function handleTestConn() {
  if (!form.host || !form.port) {
    ElMessage.warning('请先填写主机地址和端口')
    return
  }
  testing.value = true
  setTimeout(() => {
    testing.value = false
    const success = Math.random() > 0.2
    if (success) {
      ElMessage.success('连接测试成功！')
    } else {
      ElMessage.error('连接失败：连接超时，请检查网络配置')
    }
  }, 1500)
}

function testConn(row) {
  ElMessage.info(`正在测试 ${row.name} 的连接...`)
  setTimeout(() => {
    const success = row.status === '已连接' || Math.random() > 0.3
    if (success) {
      ElMessage.success(`${row.name} 连接测试成功！`)
    } else {
      ElMessage.error(`${row.name} 连接失败：${row.status === '连接异常' ? '认证失败' : '网络不可达'}`)
    }
  }, 1000)
}

function handleSave() {
  formRef.value.validate(valid => {
    if (!valid) return
    saving.value = true
    setTimeout(() => {
      saving.value = false
      dialogVisible.value = false
      if (isEdit.value) {
        const idx = dataList.value.findIndex(item => item.id === form.id)
        if (idx > -1) {
          dataList.value[idx] = {
            ...dataList.value[idx],
            name: form.name,
            type: form.type,
            env: form.env,
            host: `${form.host}:${form.port}`,
            database: form.database,
            owner: form.owner,
            updatedAt: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
          }
        }
        ElMessage.success('数据源更新成功')
      } else {
        dataList.value.unshift({
          id: Date.now(),
          name: form.name,
          type: form.type,
          env: form.env,
          host: `${form.host}:${form.port}`,
          database: form.database,
          tables: 0,
          sizeGB: 0,
          status: '未连接',
          owner: form.owner,
          updatedAt: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
        })
        ElMessage.success('数据源创建成功')
      }
    }, 800)
  })
}

function handleDelete(row) {
  ElMessageBox.confirm(
    `确定要删除数据源 "${row.name}" 吗？删除后相关配置将无法恢复。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(() => {
    const idx = dataList.value.findIndex(item => item.id === row.id)
    if (idx > -1) {
      dataList.value.splice(idx, 1)
    }
    ElMessage.success('删除成功')
  }).catch(() => {})
}

onMounted(() => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 300)
})
</script>

<style scoped>
.ds-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ds-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.ds-icon.mysql { background: #e8f0ff; color: #1664ff; }
.ds-icon.oracle { background: #fff3e8; color: #ff7d00; }
.ds-icon.postgresql { background: #e8ffea; color: #00b42a; }
.ds-icon.hive { background: #f5e8ff; color: #722ed1; }
.ds-icon.kafka { background: #e0fffa; color: #0fc6c2; }
.ds-icon.api { background: #ffe8f4; color: #f759ab; }
.ds-icon.ftp { background: #fff3e8; color: #ff7d00; }
.ds-icon.clickhouse { background: #e8f0ff; color: #1664ff; }

.ds-name {
  font-weight: 500;
  color: var(--dp-text-1);
}

.ds-type-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 4px;
  font-weight: 500;
}
.ds-type-tag.mysql { background: #e8f0ff; color: #1664ff; }
.ds-type-tag.oracle { background: #fff3e8; color: #ff7d00; }
.ds-type-tag.postgresql { background: #e8ffea; color: #00b42a; }
.ds-type-tag.hive { background: #f5e8ff; color: #722ed1; }
.ds-type-tag.kafka { background: #e0fffa; color: #0fc6c2; }
.ds-type-tag.api { background: #ffe8f4; color: #f759ab; }
.ds-type-tag.ftp { background: #fff3e8; color: #ff7d00; }
.ds-type-tag.clickhouse { background: #e8f0ff; color: #1664ff; }

.ds-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.ds-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.ds-status-dot.ok {
  background: var(--dp-success);
  box-shadow: 0 0 0 3px rgba(0, 180, 42, 0.15);
}
.ds-status-dot.warn {
  background: var(--dp-warning);
  box-shadow: 0 0 0 3px rgba(255, 125, 0, 0.15);
}
.ds-status-dot.err {
  background: var(--dp-danger);
  box-shadow: 0 0 0 3px rgba(245, 63, 63, 0.15);
}

.ds-size {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--dp-text-2);
}

.option-icon {
  margin-right: 6px;
  color: var(--dp-primary);
}
</style>
