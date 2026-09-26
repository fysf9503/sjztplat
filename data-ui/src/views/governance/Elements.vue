<template>
  <div class="governance-elements">
    <PageHeader title="数据元管理" desc="统一管理企业数据元，定义数据的业务含义、技术属性和值域约束">
      <el-button type="primary" :icon="Plus" @click="handleCreate">新建数据元</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <!-- 搜索筛选 -->
      <div class="dp-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索数据元编码/名称"
          clearable
          style="width: 240px"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.standard" placeholder="所属标准" clearable style="width: 180px" @change="handleSearch">
          <el-option label="客户身份标识规范" value="STD-BASE-1001" />
          <el-option label="行政区划代码标准" value="STD-CODE-1002" />
          <el-option label="客户价值指标口径" value="STD-IDX-1003" />
          <el-option label="日期时间格式规范" value="STD-BASE-1005" />
          <el-option label="性别代码标准" value="STD-BASE-1008" />
        </el-select>
        <el-select v-model="query.dataType" placeholder="数据类型" clearable style="width: 140px" @change="handleSearch">
          <el-option label="字符串" value="字符串" />
          <el-option label="数值" value="数值" />
          <el-option label="日期" value="日期" />
          <el-option label="整数" value="整数" />
          <el-option label="布尔" value="布尔" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="handleSearch">
          <el-option label="已发布" value="已发布" />
          <el-option label="草稿" value="草稿" />
          <el-option label="已停用" value="已停用" />
        </el-select>
        <div class="toolbar-right">
          <el-button :icon="Refresh" @click="handleSearch">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="handleCreate">新建数据元</el-button>
        </div>
      </div>

      <!-- 表格 -->
      <el-table :data="pagedList" border stripe v-loading="loading">
        <el-table-column prop="code" label="数据元编码" width="130" class-name="dp-mono" />
        <el-table-column prop="name" label="数据元名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="name-link" @click="handleView(row)">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="dataType" label="数据类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ row.dataType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="length" label="长度" width="80" align="center" />
        <el-table-column prop="precision" label="精度" width="80" align="center">
          <template #default="{ row }">{{ row.precision || '-' }}</template>
        </el-table-column>
        <el-table-column prop="domain" label="允许值/值域" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.domainType === '枚举'" class="domain-text">
              <el-tag size="small" type="success" effect="plain">枚举</el-tag>
              {{ row.domainPreview }}
            </span>
            <span v-else-if="row.domainType === '范围'" class="domain-text">
              <el-tag size="small" type="warning" effect="plain">范围</el-tag>
              {{ row.domain }}
            </span>
            <span v-else class="domain-text">
              <el-tag size="small" type="info" effect="plain">正则</el-tag>
              {{ row.domain }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="standard" label="所属标准" width="170" class-name="dp-mono">
          <template #default="{ row }">{{ row.standard }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <span class="status-dot" :class="statusDotClass(row.status)"></span>
            {{ row.status }}
          </template>
        </el-table-column>
        <el-table-column prop="creator" label="创建人" width="90" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === '已发布'"
              link
              type="warning"
              size="small"
              @click="handleDisable(row)"
            >停用</el-button>
            <el-button
              v-else-if="row.status === '已停用'"
              link
              type="success"
              size="small"
              @click="handleEnable(row)"
            >启用</el-button>
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

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="640px" destroy-on-close>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="form" label-width="110px" label-position="right">
            <el-form-item label="数据元编码" required>
              <el-input v-model="form.code" placeholder="如：DE-1001" class="dp-mono" />
            </el-form-item>
            <el-form-item label="数据元名称" required>
              <el-input v-model="form.name" placeholder="请输入数据元名称" />
            </el-form-item>
            <el-form-item label="所属标准">
              <el-select v-model="form.standard" style="width: 100%">
                <el-option label="客户身份标识规范 (STD-BASE-1001)" value="STD-BASE-1001" />
                <el-option label="行政区划代码标准 (STD-CODE-1002)" value="STD-CODE-1002" />
                <el-option label="客户价值指标口径 (STD-IDX-1003)" value="STD-IDX-1003" />
                <el-option label="日期时间格式规范 (STD-BASE-1005)" value="STD-BASE-1005" />
              </el-select>
            </el-form-item>
            <el-form-item label="业务描述">
              <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="请输入业务描述" />
            </el-form-item>
            <el-form-item label="创建人">
              <el-input v-model="form.creator" placeholder="请输入创建人" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="数据类型配置" name="datatype">
          <el-form :model="form" label-width="110px" label-position="right">
            <el-form-item label="数据类型">
              <el-radio-group v-model="form.dataType">
                <el-radio value="字符串">字符串</el-radio>
                <el-radio value="整数">整数</el-radio>
                <el-radio value="数值">数值</el-radio>
                <el-radio value="日期">日期</el-radio>
                <el-radio value="布尔">布尔</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="长度">
              <el-input-number v-model="form.length" :min="1" :max="10000" />
              <span class="form-tip">字符长度或数值总位数</span>
            </el-form-item>
            <el-form-item v-if="form.dataType === '数值'" label="小数精度">
              <el-input-number v-model="form.precision" :min="0" :max="10" />
              <span class="form-tip">小数位数</span>
            </el-form-item>
            <el-form-item label="默认值">
              <el-input v-model="form.defaultValue" placeholder="可选，字段默认值" />
            </el-form-item>
            <el-form-item label="是否必填">
              <el-switch v-model="form.required" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="值域约束" name="domain">
          <el-form :model="form" label-width="110px" label-position="right">
            <el-form-item label="值域类型">
              <el-radio-group v-model="form.domainType">
                <el-radio value="无">无约束</el-radio>
                <el-radio value="枚举">枚举值</el-radio>
                <el-radio value="范围">数值范围</el-radio>
                <el-radio value="正则">正则表达式</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="form.domainType === '枚举'" label="枚举值列表">
              <div class="enum-list">
                <div v-for="(item, idx) in form.enumValues" :key="idx" class="enum-item">
                  <el-input v-model="item.code" placeholder="编码" size="small" style="width: 100px" />
                  <el-input v-model="item.name" placeholder="名称" size="small" style="width: 140px" />
                  <el-input v-model="item.desc" placeholder="描述（可选）" size="small" style="flex: 1" />
                  <el-button link type="danger" size="small" @click="removeEnum(idx)">删除</el-button>
                </div>
                <el-button type="primary" plain size="small" :icon="Plus" @click="addEnum">添加枚举值</el-button>
              </div>
            </el-form-item>
            <el-form-item v-if="form.domainType === '范围'" label="范围">
              <el-input-number v-model="form.minValue" placeholder="最小值" size="small" style="width: 140px" />
              <span class="range-sep">~</span>
              <el-input-number v-model="form.maxValue" placeholder="最大值" size="small" style="width: 140px" />
            </el-form-item>
            <el-form-item v-if="form.domainType === '正则'" label="正则表达式">
              <el-input v-model="form.regex" placeholder="如：^[0-9]{11}$" class="dp-mono" />
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
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
const formVisible = ref(false)
const activeTab = ref('basic')
const isEdit = ref(false)

const query = reactive({
  keyword: '',
  standard: '',
  dataType: '',
  status: '',
  page: 1,
  size: 10
})

const elementList = ref([
  { id: 1, code: 'DE-1001', name: '客户编号', dataType: '字符串', length: 32, precision: 0, domainType: '无', domain: '无约束', domainPreview: '-', standard: 'STD-BASE-1001', status: '已发布', creator: '张伟', desc: '客户唯一标识符，系统自动生成' },
  { id: 2, code: 'DE-1002', name: '客户姓名', dataType: '字符串', length: 64, precision: 0, domainType: '无', domain: '无约束', domainPreview: '-', standard: 'STD-BASE-1001', status: '已发布', creator: '张伟', desc: '客户真实姓名' },
  { id: 3, code: 'DE-1003', name: '性别代码', dataType: '字符串', length: 1, precision: 0, domainType: '枚举', domain: 'M=男, F=女, U=未知', domainPreview: 'M=男, F=女, U=未知', standard: 'STD-BASE-1008', status: '已发布', creator: '李娜', desc: '客户性别代码，参照国家标准GB/T 2261.1' },
  { id: 4, code: 'DE-1004', name: '证件类型', dataType: '字符串', length: 10, precision: 0, domainType: '枚举', domain: '01=身份证, 02=护照, 03=军官证', domainPreview: '01=身份证, 02=护照...', standard: 'STD-CODE-1002', status: '已发布', creator: '李娜', desc: '身份证件类型代码' },
  { id: 5, code: 'DE-1005', name: '证件号码', dataType: '字符串', length: 32, precision: 0, domainType: '正则', domain: '^[0-9A-Za-z]{6,32}$', domainPreview: '^[0-9A-Za-z]{6,32}$', standard: 'STD-BASE-1001', status: '已发布', creator: '王强', desc: '身份证件号码，根据证件类型校验格式' },
  { id: 6, code: 'DE-1006', name: '手机号码', dataType: '字符串', length: 11, precision: 0, domainType: '正则', domain: '^1[3-9]\\d{9}$', domainPreview: '^1[3-9]\\d{9}$', standard: 'STD-BASE-1001', status: '已发布', creator: '王强', desc: '中国大陆手机号码，11位数字' },
  { id: 7, code: 'DE-1007', name: '出生日期', dataType: '日期', length: 10, precision: 0, domainType: '范围', domain: '1900-01-01 ~ 当前日期', domainPreview: '1900-01-01 ~ 当前日期', standard: 'STD-BASE-1005', status: '已发布', creator: '刘洋', desc: '客户出生日期，格式YYYY-MM-DD' },
  { id: 8, code: 'DE-1008', name: '客户等级', dataType: '字符串', length: 10, precision: 0, domainType: '枚举', domain: 'V1=普通, V2=银卡, V3=金卡, V4=钻石', domainPreview: 'V1=普通, V2=银卡, V3=金卡...', standard: 'STD-IDX-1003', status: '已发布', creator: '刘洋', desc: '客户价值等级划分' },
  { id: 9, code: 'DE-1009', name: '订单金额', dataType: '数值', length: 16, precision: 2, domainType: '范围', domain: '0.00 ~ 999999999999.99', domainPreview: '0.00 ~ 999999999999.99', standard: 'STD-IDX-1003', status: '已发布', creator: '陈静', desc: '订单总金额，单位元，保留2位小数' },
  { id: 10, code: 'DE-1010', name: '订单状态', dataType: '字符串', length: 20, precision: 0, domainType: '枚举', domain: 'PENDING=待支付, PAID=已支付, SHIPPED=已发货, COMPLETED=已完成, CANCELLED=已取消', domainPreview: '待支付, 已支付, 已发货...', standard: 'STD-CODE-1002', status: '已发布', creator: '陈静', desc: '订单生命周期状态' },
  { id: 11, code: 'DE-1011', name: '下单时间', dataType: '日期', length: 19, precision: 0, domainType: '无', domain: '无约束', domainPreview: '-', standard: 'STD-BASE-1005', status: '草稿', creator: '赵磊', desc: '订单创建时间，格式YYYY-MM-DD HH:mm:ss' },
  { id: 12, code: 'DE-1012', name: '商品编码', dataType: '字符串', length: 32, precision: 0, domainType: '正则', domain: '^SKU[0-9]{8}$', domainPreview: '^SKU[0-9]{8}$', standard: 'STD-CODE-1002', status: '已停用', creator: '赵磊', desc: '商品SKU编码，SKU前缀+8位数字' }
])

const form = reactive({
  id: null,
  code: '',
  name: '',
  standard: '',
  dataType: '字符串',
  length: 64,
  precision: 0,
  defaultValue: '',
  required: false,
  desc: '',
  creator: '',
  domainType: '无',
  enumValues: [],
  minValue: 0,
  maxValue: 100,
  regex: ''
})

const formTitle = computed(() => isEdit.value ? '编辑数据元' : '新建数据元')

const filteredList = computed(() => {
  return elementList.value.filter(item => {
    if (query.keyword && !item.name.includes(query.keyword) && !item.code.includes(query.keyword)) return false
    if (query.standard && item.standard !== query.standard) return false
    if (query.dataType && item.dataType !== query.dataType) return false
    if (query.status && item.status !== query.status) return false
    return true
  })
})

const total = computed(() => filteredList.value.length)

const pagedList = computed(() => {
  const start = (query.page - 1) * query.size
  return filteredList.value.slice(start, start + query.size)
})

function statusDotClass(status) {
  const map = { '已发布': 'success', '草稿': 'warning', '已停用': 'info' }
  return map[status] || 'info'
}

function handleSearch() {
  query.page = 1
  loading.value = true
  setTimeout(() => { loading.value = false }, 300)
}

function handleView(row) {
  ElMessage.info(`查看数据元「${row.name}」详情`)
}

function handleCreate() {
  isEdit.value = false
  activeTab.value = 'basic'
  Object.assign(form, {
    id: null, code: '', name: '', standard: '', dataType: '字符串',
    length: 64, precision: 0, defaultValue: '', required: false,
    desc: '', creator: '', domainType: '无', enumValues: [],
    minValue: 0, maxValue: 100, regex: ''
  })
  formVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  activeTab.value = 'basic'
  Object.assign(form, row)
  formVisible.value = true
}

function handleSave() {
  if (!form.name || !form.code) {
    ElMessage.warning('请填写数据元编码和名称')
    return
  }
  if (isEdit.value) {
    const idx = elementList.value.findIndex(r => r.id === form.id)
    if (idx > -1) {
      elementList.value[idx] = { ...elementList.value[idx], ...form }
    }
    ElMessage.success('数据元已更新')
  } else {
    elementList.value.unshift({
      ...form,
      id: Date.now(),
      status: '草稿',
      domainPreview: form.domainType === '枚举' ? form.enumValues.map(e => `${e.code}=${e.name}`).join(', ') : (form.domainType === '范围' ? `${form.minValue} ~ ${form.maxValue}` : form.regex)
    })
    ElMessage.success('数据元已创建')
  }
  formVisible.value = false
}

function handleDisable(row) {
  ElMessageBox.confirm(`确定停用数据元「${row.name}」吗？停用后将不可被引用。`, '确认停用', { type: 'warning' }).then(() => {
    row.status = '已停用'
    ElMessage.success('数据元已停用')
  }).catch(() => {})
}

function handleEnable(row) {
  row.status = '已发布'
  ElMessage.success('数据元已启用')
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除数据元「${row.name}」吗？此操作不可恢复。`, '确认删除', { type: 'error' }).then(() => {
    elementList.value = elementList.value.filter(r => r.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

function addEnum() {
  form.enumValues.push({ code: '', name: '', desc: '' })
}

function removeEnum(idx) {
  form.enumValues.splice(idx, 1)
}
</script>

<style scoped>
.governance-elements {
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

.domain-text {
  display: flex;
  align-items: center;
  gap: 6px;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--dp-text-3);
}

.enum-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.enum-item {
  display: flex;
  gap: 8px;
  align-items: center;
}

.range-sep {
  margin: 0 10px;
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
