<template>
  <div class="governance-domains">
    <PageHeader title="值域管理" desc="统一管理数据值域，定义数据的允许取值范围和约束规则">
      <el-button type="primary" :icon="Plus" @click="handleCreate">新建值域</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索值域编码/名称"
          clearable
          style="width: 240px"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.type" placeholder="值域类型" clearable style="width: 160px" @change="handleSearch">
          <el-option label="枚举型" value="枚举型" />
          <el-option label="数值范围" value="数值范围" />
          <el-option label="正则表达式" value="正则表达式" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="handleSearch">
          <el-option label="生效中" value="生效中" />
          <el-option label="草稿" value="草稿" />
          <el-option label="已废止" value="已废止" />
        </el-select>
        <div class="toolbar-right">
          <el-button :icon="Refresh" @click="handleSearch">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="handleCreate">新建值域</el-button>
        </div>
      </div>

      <el-table :data="pagedList" border stripe v-loading="loading">
        <el-table-column prop="code" label="值域编码" width="130" class-name="dp-mono" />
        <el-table-column prop="name" label="值域名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="name-link" @click="handleView(row)">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="typeTagType(row.type)" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="valueCount" label="取值数量" width="100" align="center">
          <template #default="{ row }">
            <span class="count-badge">{{ row.valueCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="refElements" label="被引用数据元" width="120" align="center">
          <template #default="{ row }">
            <span class="ref-count">{{ row.refElements }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <span class="status-dot" :class="statusDotClass(row.status)"></span>
            {{ row.status }}
          </template>
        </el-table-column>
        <el-table-column prop="creator" label="创建人" width="90" />
        <el-table-column prop="updateTime" label="更新时间" width="140" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">查看值</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
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

    <!-- 值域详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="currentDomain?.name" size="600px" destroy-on-close>
      <template v-if="currentDomain">
        <el-tabs v-model="activeTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="值域编码">
                <span class="dp-mono">{{ currentDomain.code }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="值域名称">{{ currentDomain.name }}</el-descriptions-item>
              <el-descriptions-item label="值域类型">{{ currentDomain.type }}</el-descriptions-item>
              <el-descriptions-item label="取值数量">{{ currentDomain.valueCount }} 个</el-descriptions-item>
              <el-descriptions-item label="状态">
                <span class="status-dot" :class="statusDotClass(currentDomain.status)"></span>
                {{ currentDomain.status }}
              </el-descriptions-item>
              <el-descriptions-item label="创建人">{{ currentDomain.creator }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ currentDomain.createTime }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ currentDomain.updateTime }}</el-descriptions-item>
              <el-descriptions-item label="值域描述" :span="2">{{ currentDomain.desc }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- 取值列表 -->
          <el-tab-pane :label="`取值列表 (${currentDomain.valueCount})`" name="values">
            <template v-if="currentDomain.type === '枚举型'">
              <el-table :data="enumValues" border stripe size="small">
                <el-table-column type="index" label="序号" width="60" />
                <el-table-column prop="code" label="编码" width="120" class-name="dp-mono" />
                <el-table-column prop="name" label="名称" width="140" />
                <el-table-column prop="desc" label="描述" min-width="160" show-overflow-tooltip />
                <el-table-column prop="sort" label="排序" width="70" align="center" />
                <el-table-column prop="status" label="状态" width="80">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.enabled ? 'success' : 'info'" effect="plain">
                      {{ row.enabled ? '启用' : '停用' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </template>
            <template v-else-if="currentDomain.type === '数值范围'">
              <div class="range-info">
                <div class="range-item">
                  <span class="range-label">最小值</span>
                  <span class="range-value">{{ currentDomain.minValue }}</span>
                </div>
                <div class="range-item">
                  <span class="range-label">最大值</span>
                  <span class="range-value">{{ currentDomain.maxValue }}</span>
                </div>
                <div class="range-item">
                  <span class="range-label">步长</span>
                  <span class="range-value">{{ currentDomain.step || '无限制' }}</span>
                </div>
                <div class="range-item">
                  <span class="range-label">包含边界</span>
                  <span class="range-value">{{ currentDomain.includeBoundary ? '是' : '否' }}</span>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="regex-info">
                <div class="regex-label">正则表达式</div>
                <div class="regex-pattern dp-mono">{{ currentDomain.pattern }}</div>
                <div class="regex-examples">
                  <div class="examples-title">示例</div>
                  <div v-for="(ex, idx) in currentDomain.examples" :key="idx" class="example-item">
                    <el-icon :size="12" color="var(--dp-success)"><CircleCheck /></el-icon>
                    <span class="dp-mono">{{ ex }}</span>
                  </div>
                </div>
              </div>
            </template>
          </el-tab-pane>

          <!-- 被引用的数据元 -->
          <el-tab-pane label="被引用数据元" name="refs">
            <div class="ref-stats">
              <div class="ref-stat">
                <span class="ref-num">{{ refElements.length }}</span>
                <span class="ref-label">个数据元引用</span>
              </div>
            </div>
            <el-table :data="refElements" border stripe size="small">
              <el-table-column prop="code" label="数据元编码" width="120" class-name="dp-mono" />
              <el-table-column prop="name" label="数据元名称" min-width="140" />
              <el-table-column prop="dataType" label="数据类型" width="100" />
              <el-table-column prop="standard" label="所属标准" width="150" class-name="dp-mono" />
              <el-table-column prop="status" label="状态" width="80">
                <template #default="{ row }">
                  <span class="status-dot" :class="statusDotClass(row.status)"></span>{{ row.status }}
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="560px" destroy-on-close>
      <el-form :model="form" label-width="100px" label-position="right">
        <el-form-item label="值域编码" required>
          <el-input v-model="form.code" placeholder="如：VD-101" class="dp-mono" />
        </el-form-item>
        <el-form-item label="值域名称" required>
          <el-input v-model="form.name" placeholder="请输入值域名称" />
        </el-form-item>
        <el-form-item label="值域类型">
          <el-radio-group v-model="form.type">
            <el-radio value="枚举型">枚举型</el-radio>
            <el-radio value="数值范围">数值范围</el-radio>
            <el-radio value="正则表达式">正则表达式</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="值域描述">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="请输入值域描述" />
        </el-form-item>
        <el-form-item label="创建人">
          <el-input v-model="form.creator" placeholder="请输入创建人" />
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
const currentDomain = ref(null)
const isEdit = ref(false)

const query = reactive({
  keyword: '',
  type: '',
  status: '',
  page: 1,
  size: 10
})

const domainList = ref([
  { id: 1, code: 'VD-101', name: '性别代码值域', type: '枚举型', valueCount: 3, refElements: 156, status: '生效中', creator: '张伟', createTime: '2026-01-15 10:00', updateTime: '2026-09-20 14:30', desc: '客户性别代码值域，参照国家标准GB/T 2261.1' },
  { id: 2, code: 'VD-102', name: '币种代码值域', type: '枚举型', valueCount: 28, refElements: 45, status: '生效中', creator: '李娜', createTime: '2026-02-10 09:15', updateTime: '2026-08-15 11:20', desc: '全球主要货币代码值域，依据ISO 4217标准' },
  { id: 3, code: 'VD-103', name: '证件类型值域', type: '枚举型', valueCount: 12, refElements: 78, status: '生效中', creator: '李娜', createTime: '2026-02-12 14:00', updateTime: '2026-09-01 16:45', desc: '各类身份证件类型代码值域' },
  { id: 4, code: 'VD-104', name: '订单状态值域', type: '枚举型', valueCount: 8, refElements: 234, status: '生效中', creator: '王强', createTime: '2026-03-05 11:30', updateTime: '2026-09-10 10:00', desc: '订单全生命周期状态代码值域' },
  { id: 5, code: 'VD-105', name: '客户等级值域', type: '枚举型', valueCount: 5, refElements: 89, status: '生效中', creator: '刘洋', createTime: '2026-03-20 15:00', updateTime: '2026-07-25 14:30', desc: '客户价值等级划分值域' },
  { id: 6, code: 'VD-106', name: '手机号码格式', type: '正则表达式', valueCount: 0, refElements: 167, status: '生效中', creator: '王强', createTime: '2026-04-10 09:00', updateTime: '2026-09-05 11:15', desc: '中国大陆手机号码格式校验正则' },
  { id: 7, code: 'VD-107', name: '金额取值范围', type: '数值范围', valueCount: 0, refElements: 56, status: '生效中', creator: '陈静', createTime: '2026-04-25 14:30', updateTime: '2026-08-20 09:45', desc: '订单金额等数值类型字段的取值范围' },
  { id: 8, code: 'VD-108', name: '渠道类型值域', type: '枚举型', valueCount: 15, refElements: 42, status: '生效中', creator: '赵磊', createTime: '2026-05-08 10:15', updateTime: '2026-09-15 16:00', desc: '营销渠道类型代码值域' },
  { id: 9, code: 'VD-109', name: '年龄取值范围', type: '数值范围', valueCount: 0, refElements: 34, status: '草稿', creator: '孙悦', createTime: '2026-09-20 11:00', updateTime: '2026-09-25 14:30', desc: '客户年龄字段的取值范围约束' },
  { id: 10, code: 'VD-110', name: '行政区划代码', type: '枚举型', valueCount: 3128, refElements: 128, status: '生效中', creator: '李娜', createTime: '2026-01-20 08:30', updateTime: '2026-06-30 10:00', desc: '全国省市区县行政区划代码值域' },
  { id: 11, code: 'VD-111', name: '邮箱格式校验', type: '正则表达式', valueCount: 0, refElements: 67, status: '已废止', creator: '周杰', createTime: '2026-02-28 15:00', updateTime: '2026-08-10 09:30', desc: '已废止，已合并至通用格式校验值域' },
  { id: 12, code: 'VD-112', name: '行业分类值域', type: '枚举型', valueCount: 97, refElements: 23, status: '生效中', creator: '吴敏', createTime: '2026-06-15 10:45', updateTime: '2026-09-12 14:00', desc: '国民经济行业分类代码值域' }
])

const enumValues = ref([
  { code: 'M', name: '男', desc: '男性', sort: 1, enabled: true },
  { code: 'F', name: '女', desc: '女性', sort: 2, enabled: true },
  { code: 'U', name: '未知', desc: '性别未知', sort: 3, enabled: true }
])

const refElements = ref([
  { code: 'DE-1003', name: '性别代码', dataType: '字符串', standard: 'STD-BASE-1008', status: '已发布' },
  { code: 'DE-1025', name: '用户性别', dataType: '字符串', standard: 'STD-BASE-1001', status: '已发布' },
  { code: 'DE-1046', name: '会员性别', dataType: '字符串', standard: 'STD-IDX-1003', status: '已发布' },
  { code: 'DE-1068', name: '联系人性别', dataType: '字符串', standard: 'STD-BASE-1001', status: '草稿' }
])

const form = reactive({
  id: null,
  code: '',
  name: '',
  type: '枚举型',
  desc: '',
  creator: ''
})

const formTitle = computed(() => isEdit.value ? '编辑值域' : '新建值域')

const filteredList = computed(() => {
  return domainList.value.filter(item => {
    if (query.keyword && !item.name.includes(query.keyword) && !item.code.includes(query.keyword)) return false
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
  const map = { '枚举型': 'success', '数值范围': 'warning', '正则表达式': 'primary' }
  return map[type] || 'info'
}

function statusDotClass(status) {
  const map = { '生效中': 'success', '草稿': 'warning', '已废止': 'info' }
  return map[status] || 'info'
}

function handleSearch() {
  query.page = 1
  loading.value = true
  setTimeout(() => { loading.value = false }, 300)
}

function handleView(row) {
  currentDomain.value = {
    ...row,
    minValue: 0.01,
    maxValue: 999999999999.99,
    step: 0.01,
    includeBoundary: true,
    pattern: '^1[3-9]\\d{9}$',
    examples: ['13800138000', '15912345678', '18600001111']
  }
  activeTab.value = 'basic'
  detailVisible.value = true
}

function handleCreate() {
  isEdit.value = false
  Object.assign(form, { id: null, code: '', name: '', type: '枚举型', desc: '', creator: '' })
  formVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  Object.assign(form, row)
  formVisible.value = true
}

function handleSave() {
  if (!form.name || !form.code) {
    ElMessage.warning('请填写值域编码和名称')
    return
  }
  if (isEdit.value) {
    const idx = domainList.value.findIndex(r => r.id === form.id)
    if (idx > -1) {
      domainList.value[idx] = { ...domainList.value[idx], ...form, updateTime: '刚刚' }
    }
    ElMessage.success('值域已更新')
  } else {
    domainList.value.unshift({
      ...form,
      id: Date.now(),
      valueCount: 0,
      refElements: 0,
      status: '草稿',
      createTime: '刚刚',
      updateTime: '刚刚'
    })
    ElMessage.success('值域已创建')
  }
  formVisible.value = false
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除值域「${row.name}」吗？此操作不可恢复。`, '确认删除', { type: 'error' }).then(() => {
    domainList.value = domainList.value.filter(r => r.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}
</script>

<style scoped>
.governance-domains {
  padding: 20px;
}

.toolbar-right {
  margin-left: auto;
  display: flex;
  gap: 10px;
}

.name-link {
  color: var(--dp-primary);
  cursor: pointer;
  font-weight: 500;
}

.name-link:hover {
  opacity: 0.75;
}

.count-badge {
  display: inline-block;
  min-width: 32px;
  padding: 2px 8px;
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  font-family: 'DIN Alternate', sans-serif;
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

.status-dot.success { background: var(--dp-success); }
.status-dot.warning { background: var(--dp-warning); }
.status-dot.info { background: var(--dp-text-4); }

.range-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.range-item {
  padding: 16px;
  background: var(--dp-bg-page);
  border-radius: 8px;
  text-align: center;
}

.range-label {
  display: block;
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 6px;
}

.range-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--dp-primary);
  font-family: 'DIN Alternate', sans-serif;
}

.regex-info {
  padding: 16px;
  background: var(--dp-bg-page);
  border-radius: 8px;
}

.regex-label {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 8px;
}

.regex-pattern {
  padding: 12px;
  background: #fff;
  border: 1px solid var(--dp-border);
  border-radius: 6px;
  font-size: 13px;
  color: var(--dp-danger);
  word-break: break-all;
}

.regex-examples {
  margin-top: 16px;
}

.examples-title {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 8px;
}

.example-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 0;
  font-size: 13px;
  color: var(--dp-text-2);
}

.ref-stats {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--dp-bg-page);
  border-radius: 8px;
}

.ref-stat {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.ref-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--dp-primary);
  font-family: 'DIN Alternate', sans-serif;
}

.ref-label {
  font-size: 12px;
  color: var(--dp-text-3);
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
