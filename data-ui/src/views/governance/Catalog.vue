<template>
  <div class="governance-catalog">
    <PageHeader title="标准目录" desc="管理企业级数据标准，支持标准的全生命周期管控">
      <el-button type="primary" :icon="Plus" @click="handleCreate">新建标准</el-button>
    </PageHeader>

    <div class="main-layout">
      <!-- 左侧分类树 -->
      <div class="sidebar dp-card dp-fade-up">
        <div class="sidebar-header">
          <span class="sidebar-title">标准分类</span>
          <el-button link size="small" :icon="Refresh" @click="refreshTree" />
        </div>
        <el-tree
          :data="categoryTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          default-expand-all
          highlight-current
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <span class="tree-node">
              <span class="tree-icon" :style="{ background: data.bg, color: data.color }">
                <el-icon :size="14"><component :is="data.icon" /></el-icon>
              </span>
              <span class="tree-label">{{ data.name }}</span>
              <span class="tree-count">{{ data.count }}</span>
            </span>
          </template>
        </el-tree>
      </div>

      <!-- 右侧主区域 -->
      <div class="main-content">
        <div class="dp-card dp-fade-up">
          <!-- 搜索筛选 -->
          <div class="dp-toolbar">
            <el-input
              v-model="query.keyword"
              placeholder="搜索标准编号/名称"
              clearable
              style="width: 240px"
              :prefix-icon="Search"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
            <el-select v-model="query.type" placeholder="标准类型" clearable style="width: 140px" @change="handleSearch">
              <el-option label="基础标准" value="基础标准" />
              <el-option label="编码标准" value="编码标准" />
              <el-option label="指标标准" value="指标标准" />
              <el-option label="命名标准" value="命名标准" />
            </el-select>
            <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="handleSearch">
              <el-option label="已发布" value="已发布" />
              <el-option label="草稿" value="草稿" />
              <el-option label="审核中" value="审核中" />
              <el-option label="已废止" value="已废止" />
            </el-select>
            <div class="toolbar-right">
              <el-button :icon="Refresh" @click="handleSearch">刷新</el-button>
              <el-button type="primary" :icon="Plus" @click="handleCreate">新建标准</el-button>
            </div>
          </div>

          <!-- 表格 -->
          <el-table :data="pagedList" border stripe v-loading="loading">
            <el-table-column prop="code" label="标准编号" width="150" class-name="dp-mono" />
            <el-table-column prop="name" label="标准名称" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="name-link" @click="handleView(row)">{{ row.name }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="标准类型" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="typeTagType(row.type)" effect="plain">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="dataType" label="数据类型" width="100" />
            <el-table-column prop="refCount" label="引用次数" width="100" sortable>
              <template #default="{ row }">
                <span class="ref-count">{{ row.refCount }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <span class="status-dot" :class="statusDotClass(row.status)"></span>
                {{ row.status }}
              </template>
            </el-table-column>
            <el-table-column prop="publishDate" label="发布时间" width="120" />
            <el-table-column prop="owner" label="负责人" width="90" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
                <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
                <el-button
                  v-if="row.status === '草稿'"
                  link
                  type="success"
                  size="small"
                  @click="handlePublish(row)"
                >发布</el-button>
                <el-button
                  v-if="row.status === '已发布'"
                  link
                  type="warning"
                  size="small"
                  @click="handleOffline(row)"
                >下线</el-button>
                <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSearch"
            @current-change="handleSearch"
          />
        </div>
      </div>
    </div>

    <!-- 标准详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="currentStandard?.name" size="600px" destroy-on-close>
      <template v-if="currentStandard">
        <el-tabs v-model="activeTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="标准编号">
                <span class="dp-mono">{{ currentStandard.code }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="标准名称">{{ currentStandard.name }}</el-descriptions-item>
              <el-descriptions-item label="标准类型">{{ currentStandard.type }}</el-descriptions-item>
              <el-descriptions-item label="数据类型">{{ currentStandard.dataType }}</el-descriptions-item>
              <el-descriptions-item label="当前版本">{{ currentStandard.version }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <span class="status-dot" :class="statusDotClass(currentStandard.status)"></span>
                {{ currentStandard.status }}
              </el-descriptions-item>
              <el-descriptions-item label="负责人">{{ currentStandard.owner }}</el-descriptions-item>
              <el-descriptions-item label="发布日期">{{ currentStandard.publishDate }}</el-descriptions-item>
              <el-descriptions-item label="引用次数">{{ currentStandard.refCount }} 次</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ currentStandard.updateTime }}</el-descriptions-item>
              <el-descriptions-item label="标准描述" :span="2">{{ currentStandard.desc }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- 数据元列表 -->
          <el-tab-pane label="数据元列表" name="elements">
            <div class="tab-stats">
              <div class="tab-stat-item">
                <span class="stat-num">{{ elementList.length }}</span>
                <span class="stat-label">关联数据元</span>
              </div>
            </div>
            <el-table :data="elementList" border stripe size="small">
              <el-table-column prop="code" label="数据元编码" width="120" class-name="dp-mono" />
              <el-table-column prop="name" label="数据元名称" min-width="140" />
              <el-table-column prop="dataType" label="数据类型" width="110" />
              <el-table-column prop="length" label="长度" width="80" />
              <el-table-column prop="status" label="状态" width="80">
                <template #default="{ row }">
                  <span class="status-dot" :class="statusDotClass(row.status)"></span>{{ row.status }}
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 引用记录 -->
          <el-tab-pane label="引用记录" name="refs">
            <el-table :data="refList" border stripe size="small">
              <el-table-column prop="refName" label="引用对象" min-width="180" />
              <el-table-column prop="refType" label="类型" width="100" />
              <el-table-column prop="refTable" label="所属表" width="160" class-name="dp-mono" />
              <el-table-column prop="refTime" label="引用时间" width="140" />
              <el-table-column prop="refUser" label="操作人" width="90" />
            </el-table>
          </el-tab-pane>

          <!-- 版本历史 -->
          <el-tab-pane label="版本历史" name="version">
            <el-timeline>
              <el-timeline-item
                v-for="(v, idx) in versionList"
                :key="v.version"
                :timestamp="v.date"
                :type="idx === 0 ? 'primary' : ''"
                :hollow="idx === 0"
              >
                <div class="version-item">
                  <div class="version-header">
                    <span class="version-tag dp-mono">{{ v.version }}</span>
                    <el-tag v-if="idx === 0" size="small" type="primary">当前版本</el-tag>
                    <span class="version-user">{{ v.user }}</span>
                  </div>
                  <div class="version-desc">{{ v.desc }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="560px" destroy-on-close>
      <el-form :model="form" label-width="100px" label-position="right">
        <el-form-item label="标准名称" required>
          <el-input v-model="form.name" placeholder="请输入标准名称" />
        </el-form-item>
        <el-form-item label="标准编号" required>
          <el-input v-model="form.code" placeholder="如：STD-BASE-1001" class="dp-mono" />
        </el-form-item>
        <el-form-item label="标准类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="基础标准" value="基础标准" />
            <el-option label="编码标准" value="编码标准" />
            <el-option label="指标标准" value="指标标准" />
            <el-option label="命名标准" value="命名标准" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据类型">
          <el-select v-model="form.dataType" style="width: 100%">
            <el-option label="字符串" value="字符串" />
            <el-option label="数值" value="数值" />
            <el-option label="日期" value="日期" />
            <el-option label="布尔" value="布尔" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.owner" placeholder="请输入负责人" />
        </el-form-item>
        <el-form-item label="标准描述">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="请输入标准描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const detailVisible = ref(false)
const formVisible = ref(false)
const activeTab = ref('basic')
const currentStandard = ref(null)
const isEdit = ref(false)

const query = reactive({
  keyword: '',
  category: '',
  type: '',
  status: '',
  page: 1,
  size: 10
})

const categoryTree = [
  {
    id: 'all',
    name: '全部标准',
    icon: 'Collection',
    color: '#1664ff',
    bg: '#e8f0ff',
    count: 1286,
    children: [
      { id: '基础标准', name: '基础标准', icon: 'Document', color: '#1664ff', bg: '#e8f0ff', count: 486 },
      { id: '编码标准', name: '编码标准', icon: 'Key', color: '#00b42a', bg: '#e8ffea', count: 328 },
      { id: '指标标准', name: '指标标准', icon: 'DataLine', color: '#ff7d00', bg: '#fff3e8', count: 256 },
      { id: '命名标准', name: '命名标准', icon: 'PriceTag', color: '#722ed1', bg: '#f5e8ff', count: 142 },
      { id: '其他标准', name: '其他标准', icon: 'Grid', color: '#0fc6c2', bg: '#e0fffa', count: 74 }
    ]
  }
]

const standardList = ref([
  { id: 1, code: 'STD-BASE-1001', name: '客户身份标识规范', type: '基础标准', dataType: '字符串', refCount: 56, status: '已发布', version: 'V2.1', owner: '张伟', publishDate: '2026-09-25', updateTime: '2026-09-25 14:30', desc: '规定客户身份标识的编码规则、格式要求和唯一性校验标准，确保客户在各系统中的身份标识统一。' },
  { id: 2, code: 'STD-CODE-1002', name: '行政区划代码标准', type: '编码标准', dataType: '字符串', refCount: 128, status: '已发布', version: 'V3.0', owner: '李娜', publishDate: '2026-09-22', updateTime: '2026-09-22 10:15', desc: '依据国家标准GB/T 2260制定的行政区划代码标准，包含省、市、区县三级行政区划编码。' },
  { id: 3, code: 'STD-IDX-1003', name: '客户价值指标口径', type: '指标标准', dataType: '数值', refCount: 42, status: '已发布', version: 'V1.5', owner: '王强', publishDate: '2026-09-20', updateTime: '2026-09-20 16:45', desc: '定义客户价值（RFM）指标的计算口径，包括最近一次消费、消费频率、消费金额三个维度。' },
  { id: 4, code: 'STD-NAME-1004', name: '数据表命名规范', type: '命名标准', dataType: '字符串', refCount: 89, status: '已发布', version: 'V2.0', owner: '刘洋', publishDate: '2026-09-18', updateTime: '2026-09-18 09:20', desc: '规范数据仓库各层数据表的命名规则，包括ODS、DWD、DWS、ADS各层表名前缀及后缀约定。' },
  { id: 5, code: 'STD-BASE-1005', name: '日期时间格式规范', type: '基础标准', dataType: '日期', refCount: 234, status: '已发布', version: 'V1.2', owner: '陈静', publishDate: '2026-09-15', updateTime: '2026-09-15 11:30', desc: '统一规定日期、时间、日期时间的存储格式和展示格式，确保全平台日期时间格式一致。' },
  { id: 6, code: 'STD-CODE-1006', name: '币种代码标准', type: '编码标准', dataType: '字符串', refCount: 67, status: '已发布', version: 'V1.8', owner: '赵磊', publishDate: '2026-09-12', updateTime: '2026-09-12 15:00', desc: '依据ISO 4217标准制定的币种代码标准，包含全球主要货币的三位字母代码和数字代码。' },
  { id: 7, code: 'STD-IDX-1007', name: 'GMV指标口径定义', type: '指标标准', dataType: '数值', refCount: 156, status: '审核中', version: 'V2.0', owner: '孙悦', publishDate: '-', updateTime: '2026-09-10 14:20', desc: '商品交易总额（GMV）的统一定义和计算口径，包含各业务线GMV的计算规则。' },
  { id: 8, code: 'STD-BASE-1008', name: '性别代码标准', type: '基础标准', dataType: '字符串', refCount: 198, status: '已发布', version: 'V1.0', owner: '周杰', publishDate: '2026-09-08', updateTime: '2026-09-08 10:00', desc: '规定性别的编码方式，采用国家标准GB/T 2261.1中的性别代码。' },
  { id: 9, code: 'STD-NAME-1009', name: '字段命名规范', type: '命名标准', dataType: '字符串', refCount: 245, status: '已发布', version: 'V1.5', owner: '吴敏', publishDate: '2026-09-05', updateTime: '2026-09-05 16:30', desc: '规范数据库字段的命名规则，采用下划线分隔的小写字母命名方式。' },
  { id: 10, code: 'STD-CODE-1010', name: '证件类型代码', type: '编码标准', dataType: '字符串', refCount: 78, status: '草稿', version: 'V0.9', owner: '郑浩', publishDate: '-', updateTime: '2026-09-28 09:15', desc: '定义各类身份证件的类型代码，包括身份证、护照、军官证等。' },
  { id: 11, code: 'STD-BASE-1011', name: '手机号格式规范', type: '基础标准', dataType: '字符串', refCount: 167, status: '已发布', version: 'V1.1', owner: '张伟', publishDate: '2026-09-01', updateTime: '2026-09-01 11:45', desc: '规定中国大陆手机号码的格式要求和校验规则，支持11位数字手机号格式验证。' },
  { id: 12, code: 'STD-IDX-1012', name: '转化率指标定义', type: '指标标准', dataType: '数值', refCount: 34, status: '已废止', version: 'V1.0', owner: '李娜', publishDate: '2026-08-20', updateTime: '2026-08-25 14:00', desc: '已废止版本，请使用V2.0版本。' }
])

const elementList = ref([
  { code: 'DE-1001', name: '客户编号', dataType: 'VARCHAR(32)', length: 32, status: '已发布' },
  { code: 'DE-1002', name: '客户姓名', dataType: 'VARCHAR(64)', length: 64, status: '已发布' },
  { code: 'DE-1003', name: '证件类型', dataType: 'VARCHAR(10)', length: 10, status: '已发布' },
  { code: 'DE-1004', name: '证件号码', dataType: 'VARCHAR(32)', length: 32, status: '已发布' },
  { code: 'DE-1005', name: '手机号码', dataType: 'VARCHAR(11)', length: 11, status: '草稿' }
])

const refList = ref([
  { refName: 'customer_info', refType: '数据表', refTable: 'ods_customer_db.customer_info', refTime: '2026-09-25 14:30', refUser: '张伟' },
  { refName: '用户画像标签表', refType: '数据表', refTable: 'dws_user_db.user_profile', refTime: '2026-09-22 10:15', refUser: '李娜' },
  { refName: '客户价值计算SQL', refType: 'SQL脚本', refTable: 'dws_summary_db', refTime: '2026-09-20 16:45', refUser: '王强' },
  { refName: 'api_customer_query', refType: 'API服务', refTable: '-', refTime: '2026-09-18 09:20', refUser: '刘洋' },
  { refName: '客户分析报表', refType: '报表', refTable: 'ads_report_db', refTime: '2026-09-15 11:30', refUser: '陈静' }
])

const versionList = ref([
  { version: 'V2.1', date: '2026-09-25', user: '张伟', desc: '新增企业客户类型支持，优化标识编码规则' },
  { version: 'V2.0', date: '2026-06-15', user: '张伟', desc: '重构标识体系，支持多维度客户身份合并' },
  { version: 'V1.2', date: '2026-03-10', user: '李娜', desc: '补充证件类型字段，完善校验规则' },
  { version: 'V1.1', date: '2025-12-20', user: '李娜', desc: '新增手机号格式校验，优化文档说明' },
  { version: 'V1.0', date: '2025-09-01', user: '王强', desc: '初始版本发布，建立基础客户标识规范' }
])

const form = reactive({
  id: null,
  name: '',
  code: '',
  type: '基础标准',
  dataType: '字符串',
  owner: '',
  desc: ''
})

const formTitle = computed(() => isEdit.value ? '编辑标准' : '新建标准')

const filteredList = computed(() => {
  return standardList.value.filter(item => {
    if (query.keyword && !item.name.includes(query.keyword) && !item.code.includes(query.keyword)) return false
    if (query.category && item.type !== query.category) return false
    if (query.type && item.type !== query.type) return false
    if (query.status && item.status !== query.status) return false
    return true
  })
})

const total = computed(() => filteredList.value.length)

const pagedList = computed(() => {
  const start = (query.page - 1) * query.size
  return filteredList.value.slice(start, start + query.size)
})

function typeTagType(type) {
  const map = { '基础标准': 'primary', '编码标准': 'success', '指标标准': 'warning', '命名标准': 'danger' }
  return map[type] || 'info'
}

function statusDotClass(status) {
  const map = { '已发布': 'success', '草稿': 'warning', '审核中': 'primary', '已废止': 'info' }
  return map[status] || 'info'
}

function handleNodeClick(data) {
  query.category = data.id === 'all' ? '' : data.id
  query.page = 1
}

function handleSearch() {
  query.page = 1
  loading.value = true
  setTimeout(() => { loading.value = false }, 300)
}

function refreshTree() {
  ElMessage.success('分类树已刷新')
}

function handleView(row) {
  currentStandard.value = row
  activeTab.value = 'basic'
  detailVisible.value = true
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, { id: null, name: '', code: '', type: '基础标准', dataType: '字符串', owner: '', desc: '' })
  formVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, row)
  formVisible.value = true
}

function handleSave() {
  if (!form.name || !form.code) {
    ElMessage.warning('请填写标准名称和编号')
    return
  }
  if (isEdit.value) {
    const idx = standardList.value.findIndex(r => r.id === form.id)
    if (idx > -1) {
      standardList.value[idx] = { ...standardList.value[idx], ...form, updateTime: '刚刚' }
    }
    ElMessage.success('标准已更新')
  } else {
    standardList.value.unshift({
      ...form,
      id: Date.now(),
      refCount: 0,
      status: '草稿',
      version: 'V0.1',
      publishDate: '-',
      updateTime: '刚刚'
    })
    ElMessage.success('标准已创建')
  }
  formVisible.value = false
}

function handlePublish(row) {
  ElMessageBox.confirm(`确定发布标准「${row.name}」吗？`, '确认发布', { type: 'warning' }).then(() => {
    row.status = '已发布'
    row.publishDate = '2026-09-26'
    ElMessage.success('标准已发布')
  }).catch(() => {})
}

function handleOffline(row) {
  ElMessageBox.confirm(`确定下线标准「${row.name}」吗？下线后将不可被引用。`, '确认下线', { type: 'warning' }).then(() => {
    row.status = '已废止'
    ElMessage.success('标准已下线')
  }).catch(() => {})
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除标准「${row.name}」吗？此操作不可恢复。`, '确认删除', { type: 'error' }).then(() => {
    standardList.value = standardList.value.filter(r => r.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}
</script>

<style scoped>
.governance-catalog {
  padding: 20px;
}

.main-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.sidebar {
  width: 260px;
  flex-shrink: 0;
  padding: 16px;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.sidebar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.main-content {
  flex: 1;
  min-width: 0;
}

.toolbar-right {
  margin-left: auto;
  display: flex;
  gap: 10px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 2px 0;
}

.tree-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tree-label {
  flex: 1;
  font-size: 13px;
}

.tree-count {
  font-size: 11px;
  color: var(--dp-text-3);
  background: var(--dp-bg-page);
  padding: 1px 8px;
  border-radius: 10px;
}

.name-link {
  color: var(--dp-primary);
  cursor: pointer;
  font-weight: 500;
}

.name-link:hover {
  opacity: 0.75;
}

.ref-count {
  font-weight: 600;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', sans-serif;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.status-dot.primary { background: var(--dp-primary); }
.status-dot.success { background: var(--dp-success); }
.status-dot.warning { background: var(--dp-warning); }
.status-dot.danger { background: var(--dp-danger); }
.status-dot.info { background: var(--dp-text-4); }

.tab-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--dp-bg-page);
  border-radius: 8px;
}

.tab-stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--dp-primary);
  font-family: 'DIN Alternate', sans-serif;
}

.stat-label {
  font-size: 12px;
  color: var(--dp-text-3);
}

.version-item {
  padding: 4px 0;
}

.version-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.version-tag {
  font-weight: 600;
  color: var(--dp-text-1);
  font-size: 13px;
}

.version-user {
  margin-left: auto;
  font-size: 12px;
  color: var(--dp-text-3);
}

.version-desc {
  font-size: 13px;
  color: var(--dp-text-2);
  line-height: 1.5;
}

.dp-mono {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
}

@keyframes dp-fade-up {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.dp-fade-up { animation: dp-fade-up 0.35s ease both; }
</style>
