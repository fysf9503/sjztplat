<template>
  <div class="dp-page datasources-page">
    <PageHeader title="服务数据源" desc="管理服务模块关联的数据源，支持数据源注册、连通性测试与权限配置">
      <el-button type="primary" :icon="Plus" @click="openEdit()">新增数据源</el-button>
    </PageHeader>

    <!-- 统计概览 -->
    <div class="ds-overview">
      <div class="ds-overview-item">
        <div class="ds-ov-num" style="color: var(--dp-primary)">{{ totalCount }}</div>
        <div class="ds-ov-label">数据源总数</div>
      </div>
      <div class="ds-overview-divider"></div>
      <div class="ds-overview-item">
        <div class="ds-ov-num" style="color: var(--dp-success)">{{ connectedCount }}</div>
        <div class="ds-ov-label">已连接</div>
      </div>
      <div class="ds-overview-divider"></div>
      <div class="ds-overview-item">
        <div class="ds-ov-num" style="color: var(--dp-warning)">{{ publishedApiCount }}</div>
        <div class="ds-ov-label">已发布API</div>
      </div>
      <div class="ds-overview-divider"></div>
      <div class="ds-overview-item">
        <div class="ds-ov-num" style="color: var(--dp-danger)">{{ errorCount }}</div>
        <div class="ds-ov-label">连接异常</div>
      </div>
    </div>

    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索数据源名称 / 主机地址"
          clearable
          style="width: 260px"
          :prefix-icon="Search"
          @input="load"
        />
        <el-select v-model="query.type" placeholder="数据源类型" clearable style="width: 150px" @change="load">
          <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-select v-model="query.status" placeholder="连接状态" clearable style="width: 130px" @change="load">
          <el-option label="已连接" value="已连接" />
          <el-option label="未连接" value="未连接" />
          <el-option label="连接异常" value="连接异常" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" :loading="loading" @click="refresh">刷新</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="name" label="数据源名称" min-width="180">
          <template #default="{ row }">
            <div class="ds-name-cell">
              <div class="ds-icon" :class="row.type.toLowerCase()">
                <el-icon :size="16"><Coin /></el-icon>
              </div>
              <div>
                <div class="ds-name">{{ row.name }}</div>
                <div class="ds-db dp-mono">{{ row.database }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <span class="ds-type-tag" :class="row.type.toLowerCase()">{{ row.type }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="env" label="环境" width="80">
          <template #default="{ row }">
            <el-tag :type="envTagType(row.env)" size="small" effect="plain">{{ row.env }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="连接地址" min-width="160" class-name="dp-mono" />
        <el-table-column prop="apiCount" label="已发布API数" width="110" sortable>
          <template #default="{ row }">
            <span class="api-count">{{ row.apiCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="callCount" label="今日调用量" width="110" sortable>
          <template #default="{ row }">
            {{ (row.callCount || 0).toLocaleString() }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="status-dot" :class="statusClass(row.status)">
              <span class="dot"></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="configDs(row)">配置</el-button>
            <el-button link type="primary" size="small" @click="testConn(row)">测试连接</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @current-change="load"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑数据源' : '新增数据源'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="110px" ref="formRef" :rules="rules">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="数据源名称" prop="name">
              <el-input v-model="form.name" placeholder="如：核心交易库" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="数据源类型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="t in typeOptions" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="所属环境">
              <el-radio-group v-model="form.env">
                <el-radio-button value="生产">生产</el-radio-button>
                <el-radio-button value="测试">测试</el-radio-button>
                <el-radio-button value="开发">开发</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-select v-model="form.owner" filterable style="width: 100%">
                <el-option v-for="o in ownerOptions" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="16">
            <el-form-item label="主机地址" prop="host">
              <el-input v-model="form.host" placeholder="192.168.1.100:3306" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="端口" prop="port">
              <el-input v-model="form.port" placeholder="3306" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="数据库名" prop="database">
              <el-input v-model="form.database" placeholder="ods_trade_db" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input v-model="form.username" placeholder="dataplat" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入数据库密码" />
        </el-form-item>
        <el-form-item label="连接参数">
          <el-input v-model="form.params" type="textarea" :rows="2" placeholder='{"useSSL": false, "characterEncoding": "utf8"}' />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.desc" type="textarea" :rows="2" placeholder="数据源用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button :loading="testing" @click="doTest">测试连接</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { dataSources, owners } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const editVisible = ref(false)
const testing = ref(false)
const formRef = ref(null)

const typeOptions = [
  { label: 'MySQL', value: 'MySQL' },
  { label: 'Oracle', value: 'Oracle' },
  { label: 'PostgreSQL', value: 'PostgreSQL' },
  { label: 'Hive', value: 'Hive' },
  { label: 'Kafka', value: 'Kafka' },
  { label: 'ClickHouse', value: 'ClickHouse' },
  { label: 'API', value: 'API' },
  { label: 'FTP', value: 'FTP' }
]

const ownerOptions = owners

const all = ref([])
const rows = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  type: '',
  status: '',
  page: 1,
  size: 10
})

const form = reactive({
  id: null,
  name: '',
  type: 'MySQL',
  env: '生产',
  host: '',
  port: '3306',
  database: '',
  username: '',
  password: '',
  params: '',
  owner: '张伟',
  desc: ''
})

const rules = {
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  database: [{ required: true, message: '请输入数据库名', trigger: 'blur' }]
}

const totalCount = computed(() => all.value.length)
const connectedCount = computed(() => all.value.filter(d => d.status === '已连接').length)
const errorCount = computed(() => all.value.filter(d => d.status === '连接异常').length)
const publishedApiCount = computed(() => all.value.reduce((sum, d) => sum + (d.apiCount || 0), 0))

function envTagType(env) {
  const map = { '生产': 'danger', '测试': 'warning', '开发': 'info' }
  return map[env] || 'info'
}

function statusClass(status) {
  const map = { '已连接': 'ok', '未连接': 'idle', '连接异常': 'err' }
  return map[status] || 'idle'
}

function load() {
  loading.value = true
  setTimeout(() => {
    let list = all.value.filter(d => {
      const matchKeyword = !query.keyword ||
        d.name.includes(query.keyword) ||
        d.host.includes(query.keyword) ||
        d.database.includes(query.keyword)
      const matchType = !query.type || d.type === query.type
      const matchStatus = !query.status || d.status === query.status
      return matchKeyword && matchType && matchStatus
    })
    total.value = list.length
    rows.value = list.slice((query.page - 1) * query.size, query.page * query.size)
    loading.value = false
  }, 300)
}

function refresh() {
  query.page = 1
  load()
  ElMessage.success('刷新成功')
}

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      type: row.type,
      env: row.env,
      host: row.host.split(':')[0],
      port: row.host.split(':')[1] || '3306',
      database: row.database,
      username: 'dataplat',
      password: '',
      params: '{"useSSL": false}',
      owner: row.owner,
      desc: ''
    })
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      type: 'MySQL',
      env: '生产',
      host: '',
      port: '3306',
      database: '',
      username: 'dataplat',
      password: '',
      params: '{"useSSL": false}',
      owner: '张伟',
      desc: ''
    })
  }
  editVisible.value = true
}

function doTest() {
  if (!form.host || !form.database) {
    return ElMessage.warning('请先填写主机地址和数据库名')
  }
  testing.value = true
  setTimeout(() => {
    testing.value = false
    const cost = 50 + Math.floor(Math.random() * 150)
    ElMessage.success(`连接成功，数据库响应正常（耗时 ${cost}ms）`)
  }, 1000)
}

function testConn(row) {
  ElMessage.info(`正在测试「${row.name}」连通性...`)
  setTimeout(() => {
    if (row.status === '连接异常') {
      ElMessage.error(`「${row.name}」连接失败：Connection refused`)
    } else {
      const cost = 30 + Math.floor(Math.random() * 100)
      ElMessage.success(`「${row.name}」连接成功，耗时 ${cost}ms`)
    }
  }, 800)
}

function configDs(row) {
  ElMessage.info(`打开「${row.name}」配置页面`)
}

function save() {
  formRef.value?.validate(valid => {
    if (!valid) return
    const host = form.host + (form.port ? ':' + form.port : '')
    if (form.id) {
      const i = all.value.findIndex(d => d.id === form.id)
      all.value[i] = {
        ...all.value[i],
        name: form.name,
        type: form.type,
        env: form.env,
        host,
        database: form.database,
        owner: form.owner,
        updatedAt: '刚刚'
      }
      ElMessage.success('数据源已更新')
    } else {
      all.value.unshift({
        id: Date.now(),
        name: form.name,
        type: form.type,
        env: form.env,
        host,
        database: form.database,
        tables: 0,
        sizeGB: 0,
        apiCount: 0,
        callCount: 0,
        status: '已连接',
        owner: form.owner,
        updatedAt: '刚刚'
      })
      ElMessage.success('数据源创建成功')
    }
    editVisible.value = false
    load()
  })
}

function remove(row) {
  ElMessageBox.confirm(
    `确定删除数据源「${row.name}」吗？删除后关联的 API 服务将无法正常调用，此操作不可撤销。`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
  ).then(() => {
    all.value = all.value.filter(d => d.id !== row.id)
    ElMessage.success('删除成功')
    load()
  }).catch(() => {})
}

onMounted(() => {
  all.value = dataSources.map((ds, i) => ({
    ...ds,
    apiCount: Math.floor(Math.random() * 15),
    callCount: Math.floor(Math.random() * 50000) + 1000
  }))
  load()
})
</script>

<style scoped>
.datasources-page {
  min-height: 100%;
}

/* 概览统计 */
.ds-overview {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: var(--dp-radius);
  padding: 16px 24px;
  border: 1px solid var(--dp-border-light);
  box-shadow: var(--dp-shadow-sm);
  margin-bottom: 16px;
}

.ds-overview-item {
  flex: 1;
  text-align: center;
}

.ds-ov-num {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.2;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}

.ds-ov-label {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 4px;
}

.ds-overview-divider {
  width: 1px;
  height: 32px;
  background: var(--dp-border-light);
}

/* 名称单元格 */
.ds-name-cell {
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
  flex-shrink: 0;
  color: #fff;
}

.ds-icon.mysql { background: #4479a1; }
.ds-icon.oracle { background: #e11d48; }
.ds-icon.postgresql { background: #336791; }
.ds-icon.hive { background: #f7ba1e; color: #4e5969; }
.ds-icon.kafka { background: #231f20; }
.ds-icon.clickhouse { background: #ffcc01; color: #4e5969; }
.ds-icon.api { background: #722ed1; }
.ds-icon.ftp { background: #14c9c9; }

.ds-name {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--dp-text-1);
  margin-bottom: 2px;
}

.ds-db {
  font-size: 11.5px;
  color: var(--dp-text-3);
}

/* 类型标签 */
.ds-type-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
}

.ds-type-tag.mysql { background: #4479a1; }
.ds-type-tag.oracle { background: #e11d48; }
.ds-type-tag.postgresql { background: #336791; }
.ds-type-tag.hive { background: #f7ba1e; color: #4e5969; }
.ds-type-tag.kafka { background: #231f20; }
.ds-type-tag.clickhouse { background: #ffcc01; color: #4e5969; }
.ds-type-tag.api { background: #722ed1; }
.ds-type-tag.ftp { background: #14c9c9; }

/* 状态圆点 */
.status-dot {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.status-dot .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  position: relative;
}

.status-dot.ok {
  color: var(--dp-success);
}
.status-dot.ok .dot {
  background: var(--dp-success);
  box-shadow: 0 0 0 3px var(--dp-success-light);
}

.status-dot.idle {
  color: var(--dp-text-3);
}
.status-dot.idle .dot {
  background: var(--dp-text-4);
  box-shadow: 0 0 0 3px var(--dp-border-light);
}

.status-dot.err {
  color: var(--dp-danger);
}
.status-dot.err .dot {
  background: var(--dp-danger);
  box-shadow: 0 0 0 3px var(--dp-danger-light);
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.api-count {
  font-weight: 500;
  color: var(--dp-primary);
}
</style>
