<template>
  <div class="dp-page">
    <PageHeader title="开发数据源" desc="管理数据开发使用的各类数据源，支持连接配置、权限管理与连接测试">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增数据源</el-button>
    </PageHeader>

    <div class="dp-card">
      <!-- 工具栏 -->
      <div class="dp-toolbar">
        <el-input
          v-model="searchForm.keyword"
          placeholder="搜索数据源名称 / 地址"
          clearable
          style="width: 240px"
          :prefix-icon="Search"
          @input="handleSearch"
        />
        <el-select
          v-model="searchForm.type"
          placeholder="数据源类型"
          clearable
          style="width: 140px"
          @change="handleSearch"
        >
          <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select
          v-model="searchForm.status"
          placeholder="状态"
          clearable
          style="width: 120px"
          @change="handleSearch"
        >
          <el-option label="已连接" value="已连接" />
          <el-option label="未连接" value="未连接" />
          <el-option label="连接异常" value="连接异常" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="handleSearch">刷新</el-button>
        </div>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="name" label="数据源名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="ds-name-cell">
              <div class="ds-type-icon" :class="getTypeClass(row.type)">
                {{ row.type.slice(0, 2) }}
              </div>
              <div class="ds-name-info">
                <div class="ds-name">{{ row.name }}</div>
                <div class="ds-db dp-mono">{{ row.database }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <span class="dp-tag" :class="getTypeTagClass(row.type)">{{ row.type }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="env" label="环境" width="80">
          <template #default="{ row }">
            <el-tag :type="envTagType(row.env)" size="small" effect="plain">{{ row.env }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="连接地址" min-width="160" class-name="dp-mono" show-overflow-tooltip />
        <el-table-column prop="tables" label="库表数" width="90" align="center" sortable />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="ds-status">
              <span class="ds-status-dot" :class="getStatusDotClass(row.status)"></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="permission" label="权限等级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.permission === '读写'" type="primary" size="small" effect="plain">读写</el-tag>
            <el-tag v-else-if="row.permission === '只读'" type="success" size="small" effect="plain">只读</el-tag>
            <el-tag v-else type="info" size="small" effect="plain">{{ row.permission }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goWorkbench(row)">SQL查询</el-button>
            <el-button link type="primary" size="small" @click="openPermission(row)">权限配置</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="testConnection(row)">测试连接</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="total, prev, pager, next"
        @current-change="handleSearch"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑数据源' : '新增数据源'" width="640px" destroy-on-close>
      <el-form :model="formData" label-width="100px" ref="formRef">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-form-item label="数据源名称" prop="name" required>
              <el-input v-model="formData.name" placeholder="请输入数据源名称" />
            </el-form-item>
            <el-form-item label="数据源类型" prop="type" required>
              <el-select v-model="formData.type" style="width: 100%">
                <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
            <el-form-item label="环境" prop="env">
              <el-radio-group v-model="formData.env">
                <el-radio value="生产">生产</el-radio>
                <el-radio value="测试">测试</el-radio>
                <el-radio value="开发">开发</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="负责人">
              <el-select v-model="formData.owner" placeholder="请选择负责人" style="width: 100%">
                <el-option v-for="o in ownerOptions" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="formData.desc" type="textarea" :rows="2" placeholder="请输入描述信息" />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="连接配置" name="connection">
            <el-form-item label="主机地址" prop="host" required>
              <el-input v-model="formData.host" placeholder="如：192.168.1.100:3306" />
            </el-form-item>
            <el-form-item label="数据库名" prop="database" required>
              <el-input v-model="formData.database" placeholder="请输入数据库名称" />
            </el-form-item>
            <el-form-item label="用户名" prop="username">
              <el-input v-model="formData.username" placeholder="请输入用户名" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="formData.password" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-form-item label="权限等级">
              <el-radio-group v-model="formData.permission">
                <el-radio value="读写">读写</el-radio>
                <el-radio value="只读">只读</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="高级配置" name="advanced">
            <el-form-item label="连接池大小">
              <el-input-number v-model="formData.poolSize" :min="1" :max="100" />
            </el-form-item>
            <el-form-item label="连接超时(秒)">
              <el-input-number v-model="formData.timeout" :min="1" :max="300" />
            </el-form-item>
            <el-form-item label="字符集">
              <el-select v-model="formData.charset" style="width: 100%">
                <el-option label="utf8mb4" value="utf8mb4" />
                <el-option label="utf8" value="utf8" />
                <el-option label="gbk" value="gbk" />
                <el-option label="latin1" value="latin1" />
              </el-select>
            </el-form-item>
            <el-form-item label="额外参数">
              <el-input v-model="formData.extraParams" type="textarea" :rows="3" placeholder="key=value，每行一个" class="dp-mono" />
            </el-form-item>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button @click="testCurrentConnection">测试连接</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置抽屉 -->
    <el-drawer v-model="permissionVisible" title="权限配置" size="480px">
      <template v-if="currentDs">
        <div class="ds-perm-header">
          <div class="ds-perm-title">{{ currentDs.name }}</div>
          <div class="ds-perm-sub dp-mono">{{ currentDs.host }}</div>
        </div>
        <el-divider />
        <div class="ds-perm-section">
          <div class="ds-perm-section-title">用户权限</div>
          <el-table :data="permUsers" border size="small">
            <el-table-column prop="user" label="用户" width="120" />
            <el-table-column prop="role" label="角色" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.role === '管理员' ? 'danger' : row.role === '开发者' ? 'primary' : 'success'">
                  {{ row.role }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="权限" min-width="140">
              <template #default="{ row }">
                <el-checkbox-group v-model="row.perms">
                  <el-checkbox value="查询">查询</el-checkbox>
                  <el-checkbox value="写入">写入</el-checkbox>
                  <el-checkbox value="DDL">DDL</el-checkbox>
                </el-checkbox-group>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="ds-perm-section">
          <div class="ds-perm-section-title">IP白名单</div>
          <el-input type="textarea" :rows="4" placeholder="每行一个IP地址，支持通配符*" class="dp-mono"
            model-value="192.168.1.0/24&#10;10.0.0.0/8&#10;127.0.0.1" />
        </div>
        <div style="margin-top: 20px; text-align: right">
          <el-button @click="permissionVisible = false">取消</el-button>
          <el-button type="primary" @click="savePermission">保存配置</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { dataSources, owners } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const dialogVisible = ref(false)
const permissionVisible = ref(false)
const isEdit = ref(false)
const activeTab = ref('basic')
const currentDs = ref(null)
const formRef = ref(null)

const typeOptions = ['MySQL', 'Oracle', 'PostgreSQL', 'Hive', 'Kafka', 'ClickHouse', 'API', 'FTP']
const ownerOptions = owners

const searchForm = reactive({
  keyword: '',
  type: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const allData = ref([])
const tableData = ref([])

const formData = reactive({
  id: null,
  name: '',
  type: 'MySQL',
  env: '开发',
  host: '',
  database: '',
  username: '',
  password: '',
  permission: '读写',
  owner: '',
  desc: '',
  poolSize: 10,
  timeout: 30,
  charset: 'utf8mb4',
  extraParams: ''
})

const permUsers = ref([
  { user: '张伟', role: '管理员', perms: ['查询', '写入', 'DDL'] },
  { user: '李娜', role: '开发者', perms: ['查询', '写入'] },
  { user: '王强', role: '开发者', perms: ['查询', '写入'] },
  { user: '刘洋', role: '只读用户', perms: ['查询'] },
  { user: '陈静', role: '只读用户', perms: ['查询'] }
])

const getTypeClass = type => type.toLowerCase().replace(/[^a-z]/g, '')

const getTypeTagClass = type => ({
  MySQL: 'primary',
  Oracle: 'danger',
  PostgreSQL: 'primary',
  Hive: 'warning',
  Kafka: 'info',
  ClickHouse: 'cyan',
  API: 'purple',
  FTP: 'success'
}[type] || 'info')

const envTagType = env => ({
  生产: 'danger',
  测试: 'warning',
  开发: 'success'
}[env] || 'info')

const getStatusDotClass = status => ({
  '已连接': 'success',
  '未连接': 'default',
  '连接异常': 'error'
}[status] || 'default')

function handleSearch() {
  loading.value = true
  setTimeout(() => {
    let list = allData.value.filter(d =>
      (!searchForm.keyword || d.name.includes(searchForm.keyword) || d.host.includes(searchForm.keyword)) &&
      (!searchForm.type || d.type === searchForm.type) &&
      (!searchForm.status || d.status === searchForm.status)
    )
    pagination.total = list.length
    const start = (pagination.page - 1) * pagination.size
    tableData.value = list.slice(start, start + pagination.size)
    loading.value = false
  }, 300)
}

function openCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: null,
    name: '',
    type: 'MySQL',
    env: '开发',
    host: '',
    database: '',
    username: '',
    password: '',
    permission: '读写',
    owner: '',
    desc: '',
    poolSize: 10,
    timeout: 30,
    charset: 'utf8mb4',
    extraParams: ''
  })
  activeTab.value = 'basic'
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(formData, {
    id: row.id,
    name: row.name,
    type: row.type,
    env: row.env,
    host: row.host,
    database: row.database,
    username: 'admin',
    password: '******',
    permission: row.permission || '读写',
    owner: row.owner,
    desc: '',
    poolSize: 10,
    timeout: 30,
    charset: 'utf8mb4',
    extraParams: ''
  })
  activeTab.value = 'basic'
  dialogVisible.value = true
}

function handleSave() {
  if (!formData.name) {
    ElMessage.warning('请输入数据源名称')
    return
  }
  if (!formData.host) {
    ElMessage.warning('请输入主机地址')
    return
  }
  if (!formData.database) {
    ElMessage.warning('请输入数据库名')
    return
  }

  if (isEdit.value) {
    const idx = allData.value.findIndex(d => d.id === formData.id)
    if (idx > -1) {
      allData.value[idx] = {
        ...allData.value[idx],
        name: formData.name,
        type: formData.type,
        env: formData.env,
        host: formData.host,
        database: formData.database,
        permission: formData.permission,
        owner: formData.owner
      }
    }
    ElMessage.success('数据源更新成功')
  } else {
    allData.value.unshift({
      id: Date.now(),
      name: formData.name,
      type: formData.type,
      env: formData.env,
      host: formData.host,
      database: formData.database,
      tables: 0,
      sizeGB: 0,
      status: '未连接',
      permission: formData.permission,
      owner: formData.owner,
      updatedAt: '刚刚'
    })
    ElMessage.success('数据源创建成功')
  }
  dialogVisible.value = false
  handleSearch()
}

function testConnection(row) {
  const target = row || formData
  ElMessage.info(`正在测试 ${target.name || '数据源'} 连接...`)
  setTimeout(() => {
    if (Math.random() > 0.2) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.error('连接失败：连接超时，请检查网络和配置')
    }
  }, 1000)
}

function testCurrentConnection() {
  testConnection(formData)
}

function openPermission(row) {
  currentDs.value = row
  permissionVisible.value = true
}

function savePermission() {
  ElMessage.success('权限配置已保存')
  permissionVisible.value = false
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除数据源「${row.name}」吗？删除后不可恢复。`, '确认删除', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(() => {
    allData.value = allData.value.filter(d => d.id !== row.id)
    ElMessage.success('删除成功')
    handleSearch()
  }).catch(() => {})
}

function goWorkbench(row) {
  router.push('/develop/workbench')
  ElMessage.success(`已跳转到SQL工作台，当前数据源：${row.name}`)
}

onMounted(() => {
  allData.value = dataSources.map(d => ({
    ...d,
    permission: Math.random() > 0.5 ? '读写' : '只读'
  }))
  handleSearch()
})
</script>

<style scoped>
.ds-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ds-type-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  background: var(--dp-primary);
}

.ds-type-icon.mysql { background: #4479a1; }
.ds-type-icon.oracle { background: #e11d48; }
.ds-type-icon.postgresql { background: #336791; }
.ds-type-icon.hive { background: #f7ba1e; color: #4e5969; }
.ds-type-icon.kafka { background: #231f20; }
.ds-type-icon.clickhouse { background: #ffcc01; color: #4e5969; }
.ds-type-icon.api { background: #722ed1; }
.ds-type-icon.ftp { background: #14c9c9; }

.ds-name-info {
  min-width: 0;
}

.ds-name {
  font-weight: 600;
  color: var(--dp-text-1);
  font-size: 13px;
}

.ds-db {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

/* 状态指示灯 */
.ds-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--dp-text-2);
}

.ds-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--dp-text-4);
}

.ds-status-dot.success {
  background: var(--dp-success);
  box-shadow: 0 0 0 2px rgba(0, 180, 42, 0.15);
}

.ds-status-dot.error {
  background: var(--dp-danger);
  box-shadow: 0 0 0 2px rgba(245, 63, 63, 0.15);
}

.ds-status-dot.default {
  background: var(--dp-text-4);
}

/* 权限配置 */
.ds-perm-header {
  padding: 4px 0;
}

.ds-perm-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.ds-perm-sub {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 4px;
}

.ds-perm-section {
  margin-bottom: 20px;
}

.ds-perm-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-2);
  margin-bottom: 10px;
}
</style>
