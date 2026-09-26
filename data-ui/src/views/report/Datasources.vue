<template>
  <div class="dp-page">
    <PageHeader title="报表数据源" desc="管理报表平台接入的各类数据源，支持连接测试与元数据采集">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增数据源</el-button>
    </PageHeader>

    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="keyword" placeholder="搜索数据源名称" clearable style="width: 220px" :prefix-icon="Search" />
        <el-select v-model="typeFilter" placeholder="数据源类型" clearable style="width: 140px">
          <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="连接状态" clearable style="width: 130px">
          <el-option label="已连接" value="已连接" />
          <el-option label="未连接" value="未连接" />
          <el-option label="连接异常" value="连接异常" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        </div>
      </div>

      <el-table :data="pagedData" border stripe v-loading="loading">
        <el-table-column type="selection" width="44" />
        <el-table-column prop="name" label="数据源名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTagType(row.type)" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="连接状态" width="110">
          <template #default="{ row }">
            <span class="status-wrap">
              <span class="status-dot" :class="statusClass(row.status)"></span>
              <span>{{ row.status }}</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="datasets" label="数据集数" width="100" align="center">
          <template #default="{ row }">
            <span class="dataset-count">{{ row.datasets || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="90" />
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleTest(row)">测试连接</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="filteredData.length"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form :model="form" label-width="100px" label-position="right">
        <el-form-item label="数据源名称" required>
          <el-input v-model="form.name" placeholder="请输入数据源名称" />
        </el-form-item>
        <el-form-item label="数据源类型" required>
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="主机地址" required>
          <el-input v-model="form.host" placeholder="例如：192.168.1.100:3306" />
        </el-form-item>
        <el-form-item label="数据库名" required>
          <el-input v-model="form.database" placeholder="请输入数据库名称" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.owner" placeholder="请选择负责人" style="width: 100%">
            <el-option v-for="o in owners" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.desc" type="textarea" :rows="2" placeholder="请输入描述信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { dataSources, owners } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const keyword = ref('')
const typeFilter = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增数据源')
const form = ref({
  name: '',
  type: '',
  host: '',
  database: '',
  username: '',
  password: '',
  owner: '',
  desc: ''
})

const typeOptions = ['MySQL', 'Oracle', 'PostgreSQL', 'Hive', 'Kafka', 'API', 'FTP', 'ClickHouse', 'Elasticsearch']

const dataList = computed(() => dataSources.map(d => ({ ...d, datasets: Math.floor(Math.random() * 20) + 1 })))

const filteredData = computed(() =>
  dataList.value.filter(d =>
    (!keyword.value || d.name.includes(keyword.value)) &&
    (!typeFilter.value || d.type === typeFilter.value) &&
    (!statusFilter.value || d.status === statusFilter.value)
  )
)

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

const typeTagType = type => {
  const map = { MySQL: 'primary', Oracle: 'warning', PostgreSQL: 'success', Hive: '', Kafka: 'danger', API: 'info', FTP: 'info', ClickHouse: '', Elasticsearch: 'warning' }
  return map[type] || 'info'
}

const statusClass = status => {
  const map = { '已连接': 'success', '未连接': 'info', '连接异常': 'danger' }
  return map[status] || 'info'
}

function handleRefresh() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('刷新成功')
  }, 600)
}

function handleTest(row) {
  const loadingMsg = ElMessage({
    message: `正在测试 ${row.name} 的连接...`,
    type: 'info',
    duration: 0
  })
  setTimeout(() => {
    loadingMsg.close()
    if (row.status === '已连接') {
      ElMessage.success('连接测试成功，响应时间 23ms')
    } else {
      ElMessage.warning('连接测试失败，请检查网络配置或账号密码')
    }
  }, 1000)
}

function handleAdd() {
  dialogTitle.value = '新增数据源'
  form.value = { name: '', type: '', host: '', database: '', username: '', password: '', owner: '', desc: '' }
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑数据源'
  form.value = { ...row }
  dialogVisible.value = true
}

function handleSave() {
  if (!form.value.name || !form.value.type || !form.value.host) {
    ElMessage.warning('请填写必填项')
    return
  }
  ElMessage.success(dialogTitle.value.includes('新增') ? '新增成功' : '保存成功')
  dialogVisible.value = false
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定要删除数据源「${row.name}」吗？删除后相关报表可能无法正常使用。`, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('删除成功')
  }).catch(() => {})
}
</script>

<style scoped>
.status-wrap {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.success { background: #00b42a; box-shadow: 0 0 4px rgba(0,180,42,0.5); }
.status-dot.info { background: #86909c; }
.status-dot.danger { background: #f53f3f; box-shadow: 0 0 4px rgba(245,63,63,0.5); }

.dataset-count {
  font-weight: 600;
  color: var(--dp-primary);
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}
</style>
