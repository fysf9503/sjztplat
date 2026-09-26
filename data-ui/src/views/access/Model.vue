<template>
  <div class="dp-page">
    <PageHeader title="接入模型配置" desc="定义数据接入模型，配置字段映射、同步规则与清洗转换规则">
      <el-button type="primary" :icon="Plus" @click="openCreate()">新建模型</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索模型名称"
          clearable
          style="width: 260px"
          :prefix-icon="Search"
          @input="handleSearch"
        />
        <el-select v-model="query.type" placeholder="模型类型" clearable style="width: 140px" @change="handleSearch">
          <el-option label="维度模型" value="维度模型" />
          <el-option label="事实模型" value="事实模型" />
          <el-option label="汇总模型" value="汇总模型" />
          <el-option label="标签模型" value="标签模型" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="handleSearch">
          <el-option label="已发布" value="已发布" />
          <el-option label="设计中" value="设计中" />
          <el-option label="已归档" value="已归档" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        </div>
      </div>

      <el-table :data="pagedList" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="name" label="模型名称" min-width="220">
          <template #default="{ row }">
            <div class="model-name-cell">
              <div class="model-icon" :class="row.type.toLowerCase()">
                <el-icon :size="16"><component :is="getTypeIcon(row.type)" /></el-icon>
              </div>
              <div>
                <div class="model-name">{{ row.name }}</div>
                <div class="model-code">{{ row.code }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sourceDs" label="关联数据源" min-width="160" class-name="dp-mono" />
        <el-table-column prop="type" label="模型类型" width="100">
          <template #default="{ row }">
            <span class="model-type-tag" :class="getTypeClass(row.type)">{{ row.type }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="tableCount" label="表数量" width="90" align="center" />
        <el-table-column prop="fieldCount" label="字段数" width="90" align="center" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="model-status">
              <span class="model-status-dot" :class="statusDotClass(row.status)"></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" :icon="CopyDocument" @click="copyModel(row)">复制</el-button>
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

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑接入模型' : '新建接入模型'" width="640px" :close-on-click-modal="false">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="14">
            <el-form-item label="模型名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入模型名称" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="模型编码" prop="code">
              <el-input v-model="form.code" placeholder="英文编码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模型类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择模型类型" style="width: 100%">
                <el-option label="维度模型" value="维度模型" />
                <el-option label="事实模型" value="事实模型" />
                <el-option label="汇总模型" value="汇总模型" />
                <el-option label="标签模型" value="标签模型" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联数据源" prop="sourceDs">
              <el-select v-model="form.sourceDs" placeholder="请选择数据源" style="width: 100%" filterable>
                <el-option v-for="ds in dsOptions" :key="ds.id" :label="ds.name" :value="ds.name" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="负责人" prop="owner">
          <el-select v-model="form.owner" placeholder="请选择负责人" filterable style="width: 240px">
            <el-option v-for="o in ownerList" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型描述">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="请输入模型描述" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 模型详情抽屉 -->
    <el-drawer v-model="detailVisible" title="模型详情" size="720px">
      <template v-if="currentModel">
        <div class="detail-header">
          <div class="detail-title">
            <div class="model-icon lg" :class="getTypeClass(currentModel.type)">
              <el-icon :size="22"><component :is="getTypeIcon(currentModel.type)" /></el-icon>
            </div>
            <div>
              <h3>{{ currentModel.name }}</h3>
              <p>{{ currentModel.code }}</p>
            </div>
          </div>
          <el-tag :type="statusTagType(currentModel.status)" effect="plain">{{ currentModel.status }}</el-tag>
        </div>

        <el-tabs v-model="activeTab" class="detail-tabs">
          <!-- 字段配置 -->
          <el-tab-pane label="字段配置" name="fields">
            <div class="tab-toolbar">
              <el-button size="small" :icon="Plus" type="primary">添加字段</el-button>
              <el-button size="small" :icon="Upload">批量导入</el-button>
            </div>
            <el-table :data="modelFields" border stripe size="small">
              <el-table-column type="index" width="50" label="#" />
              <el-table-column prop="fieldName" label="字段名" width="140" class-name="dp-mono" />
              <el-table-column prop="fieldType" label="类型" width="120">
                <template #default="{ row }">
                  <span class="field-type-tag">{{ row.fieldType }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="sourceField" label="源字段" width="140" class-name="dp-mono" />
              <el-table-column prop="comment" label="注释" min-width="150" />
              <el-table-column label="操作" width="100" align="center">
                <template #default>
                  <el-button link type="primary" size="small">编辑</el-button>
                  <el-button link type="danger" size="small">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 同步规则 -->
          <el-tab-pane label="同步规则" name="sync">
            <div class="rule-list">
              <div class="rule-item">
                <div class="rule-header">
                  <span class="rule-name">同步频率</span>
                  <el-tag size="small" type="primary">定时调度</el-tag>
                </div>
                <div class="rule-content">
                  <el-descriptions :column="2" border size="small">
                    <el-descriptions-item label="调度方式">Cron 表达式</el-descriptions-item>
                    <el-descriptions-item label="Cron">0 0 2 * * ?</el-descriptions-item>
                    <el-descriptions-item label="超时时间">3600 秒</el-descriptions-item>
                    <el-descriptions-item label="失败重试">3 次</el-descriptions-item>
                  </el-descriptions>
                </div>
              </div>
              <div class="rule-item">
                <div class="rule-header">
                  <span class="rule-name">增量策略</span>
                  <el-tag size="small" type="success">基于时间戳</el-tag>
                </div>
                <div class="rule-content">
                  <el-descriptions :column="2" border size="small">
                    <el-descriptions-item label="增量字段">update_time</el-descriptions-item>
                    <el-descriptions-item label="粒度">天</el-descriptions-item>
                    <el-descriptions-item label="回溯天数">7 天</el-descriptions-item>
                    <el-descriptions-item label="延迟窗口">30 分钟</el-descriptions-item>
                  </el-descriptions>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 清洗转换规则 -->
          <el-tab-pane label="清洗转换规则" name="clean">
            <div class="tab-toolbar">
              <el-button size="small" :icon="Plus" type="primary">添加规则</el-button>
            </div>
            <div class="clean-rules">
              <div v-for="(rule, idx) in cleanRules" :key="idx" class="clean-rule-item">
                <div class="clean-rule-header">
                  <span class="clean-rule-name">{{ rule.name }}</span>
                  <el-tag size="small" :type="rule.type === '必填' ? 'danger' : rule.type === '格式' ? 'warning' : 'info'" effect="plain">
                    {{ rule.type }}
                  </el-tag>
                  <div class="clean-rule-actions">
                    <el-button link type="primary" size="small">编辑</el-button>
                    <el-button link type="danger" size="small">删除</el-button>
                  </div>
                </div>
                <div class="clean-rule-body">
                  <div class="clean-rule-field">目标字段：<code>{{ rule.field }}</code></div>
                  <div class="clean-rule-desc">{{ rule.description }}</div>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Coin, Collection, CopyDocument, DataLine, Histogram, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { dataSources, owners, logicalModels } from '@/mock'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const ownerList = owners
const dsOptions = dataSources.slice(0, 8)
const currentModel = ref(null)
const activeTab = ref('fields')

const query = reactive({
  keyword: '',
  type: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10
})

const form = reactive({
  id: null,
  name: '',
  code: '',
  type: '',
  sourceDs: '',
  owner: '',
  desc: ''
})

const formRules = {
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入模型编码', trigger: 'blur' }],
  type: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  sourceDs: [{ required: true, message: '请选择关联数据源', trigger: 'change' }],
  owner: [{ required: true, message: '请选择负责人', trigger: 'change' }]
}

const modelTypes = ['维度模型', '事实模型', '汇总模型', '标签模型']

const dataList = ref([
  ...logicalModels.slice(0, 12).map((m, idx) => ({
    id: m.id,
    name: m.name,
    code: `MODEL-${1000 + idx}`,
    type: modelTypes[idx % 4],
    sourceDs: dataSources[idx % dataSources.length]?.name || '核心交易库',
    tableCount: m.entities,
    fieldCount: m.fields,
    status: m.status,
    owner: m.owner,
    createdAt: m.updatedAt
  }))
])

const filteredList = computed(() => {
  return dataList.value.filter(item => {
    if (query.keyword) {
      const kw = query.keyword.toLowerCase()
      if (!item.name.toLowerCase().includes(kw) && !item.code.toLowerCase().includes(kw)) return false
    }
    if (query.type && item.type !== query.type) return false
    if (query.status && item.status !== query.status) return false
    return true
  })
})

const pagedList = computed(() => {
  const start = (pagination.page - 1) * pagination.size
  return filteredList.value.slice(start, start + pagination.size)
})

const modelFields = ref([
  { fieldName: 'id', fieldType: 'BIGINT', sourceField: 'id', comment: '主键ID' },
  { fieldName: 'customer_name', fieldType: 'VARCHAR(64)', sourceField: 'name', comment: '客户名称' },
  { fieldName: 'customer_type', fieldType: 'VARCHAR(32)', sourceField: 'type', comment: '客户类型' },
  { fieldName: 'status', fieldType: 'TINYINT', sourceField: 'status', comment: '状态' },
  { fieldName: 'total_amount', fieldType: 'DECIMAL(16,2)', sourceField: 'amount', comment: '累计金额' },
  { fieldName: 'create_time', fieldType: 'DATETIME', sourceField: 'create_time', comment: '创建时间' },
  { fieldName: 'update_time', fieldType: 'DATETIME', sourceField: 'update_time', comment: '更新时间' }
])

const cleanRules = ref([
  { name: '客户名称去空格', type: '清洗', field: 'customer_name', description: '去除字段前后空格，将连续空格替换为单个空格' },
  { name: '手机号格式校验', type: '格式', field: 'phone', description: '校验手机号格式是否符合中国大陆手机号规则，不符合则置空并记录' },
  { name: '金额非负校验', type: '必填', field: 'total_amount', description: '金额字段必须大于等于0，负数记录为异常数据' },
  { name: '状态值枚举', type: '清洗', field: 'status', description: '将状态值映射为标准枚举：1-正常，0-无效，2-冻结' },
  { name: '时间格式统一', type: '清洗', field: 'create_time', description: '将各种时间格式统一为 YYYY-MM-DD HH:mm:ss 格式' }
])

function getTypeIcon(type) {
  const map = { '维度模型': 'Collection', '事实模型': 'DataLine', '汇总模型': 'Histogram', '标签模型': 'Coin' }
  return map[type] || 'Collection'
}

function getTypeClass(type) {
  const map = { '维度模型': 'dim', '事实模型': 'fact', '汇总模型': 'summary', '标签模型': 'tag' }
  return map[type] || 'dim'
}

function statusDotClass(status) {
  const map = { '已发布': 'published', '设计中': 'design', '已归档': 'archived' }
  return map[status] || 'design'
}

function statusTagType(status) {
  const map = { '已发布': 'success', '设计中': 'warning', '已归档': 'info' }
  return map[status] || 'info'
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

function openCreate() {
  isEdit.value = false
  Object.assign(form, {
    id: null, name: '', code: '', type: '', sourceDs: '', owner: '', desc: ''
  })
  dialogVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    code: row.code,
    type: row.type,
    sourceDs: row.sourceDs,
    owner: row.owner,
    desc: ''
  })
  dialogVisible.value = true
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
            code: form.code,
            type: form.type,
            sourceDs: form.sourceDs,
            owner: form.owner
          }
        }
        ElMessage.success('模型更新成功')
      } else {
        dataList.value.unshift({
          id: Date.now(),
          name: form.name,
          code: form.code,
          type: form.type,
          sourceDs: form.sourceDs,
          tableCount: 0,
          fieldCount: 0,
          status: '设计中',
          owner: form.owner,
          createdAt: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
        })
        ElMessage.success('模型创建成功')
      }
    }, 800)
  })
}

function viewDetail(row) {
  currentModel.value = row
  activeTab.value = 'fields'
  detailVisible.value = true
}

function copyModel(row) {
  ElMessageBox.confirm(
    `确定要复制模型 "${row.name}" 吗？`,
    '复制确认',
    { confirmButtonText: '确定复制', cancelButtonText: '取消', type: 'info' }
  ).then(() => {
    dataList.value.unshift({
      ...row,
      id: Date.now(),
      name: row.name + '-副本',
      code: row.code + '_COPY',
      status: '设计中',
      createdAt: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
    })
    ElMessage.success('模型复制成功')
  }).catch(() => {})
}

function handleDelete(row) {
  ElMessageBox.confirm(
    `确定要删除模型 "${row.name}" 吗？删除后配置将无法恢复。`,
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
.model-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.model-icon {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.model-icon.lg {
  width: 48px;
  height: 48px;
  border-radius: 8px;
}
.model-icon.dim { background: #e8f0ff; color: #1664ff; }
.model-icon.fact { background: #e8ffea; color: #00b42a; }
.model-icon.summary { background: #fff3e8; color: #ff7d00; }
.model-icon.tag { background: #f5e8ff; color: #722ed1; }

.model-name {
  font-weight: 500;
  color: var(--dp-text-1);
  font-size: 13px;
}
.model-code {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 2px;
  font-family: 'JetBrains Mono', Consolas, monospace;
}

.model-type-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 4px;
  font-weight: 500;
}
.model-type-tag.dim { background: #e8f0ff; color: #1664ff; }
.model-type-tag.fact { background: #e8ffea; color: #00b42a; }
.model-type-tag.summary { background: #fff3e8; color: #ff7d00; }
.model-type-tag.tag { background: #f5e8ff; color: #722ed1; }

.model-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.model-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.model-status-dot.published {
  background: var(--dp-success);
  box-shadow: 0 0 0 3px rgba(0, 180, 42, 0.15);
}
.model-status-dot.design {
  background: var(--dp-warning);
  box-shadow: 0 0 0 3px rgba(255, 125, 0, 0.15);
}
.model-status-dot.archived {
  background: var(--dp-text-4);
  box-shadow: 0 0 0 3px rgba(201, 205, 212, 0.15);
}

.field-type-tag {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11px;
  color: #722ed1;
  background: #f5e8ff;
  padding: 2px 6px;
  border-radius: 4px;
}

/* 详情抽屉 */
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--dp-border-light);
}
.detail-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.detail-title h3 {
  margin: 0;
  font-size: 18px;
  color: var(--dp-text-1);
}
.detail-title p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--dp-text-3);
  font-family: 'JetBrains Mono', Consolas, monospace;
}

.detail-tabs {
  margin-top: 10px;
}

.tab-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.rule-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.rule-item {
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  overflow: hidden;
}
.rule-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--dp-bg-page);
  border-bottom: 1px solid var(--dp-border-light);
}
.rule-name {
  font-weight: 500;
  color: var(--dp-text-1);
  font-size: 14px;
}
.rule-content {
  padding: 12px 16px;
}

.clean-rules {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.clean-rule-item {
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  padding: 14px 16px;
  transition: all 0.2s;
}
.clean-rule-item:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 2px 8px rgba(22, 100, 255, 0.1);
}
.clean-rule-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.clean-rule-name {
  font-weight: 500;
  color: var(--dp-text-1);
  font-size: 14px;
}
.clean-rule-actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}
.clean-rule-body {
  font-size: 13px;
  color: var(--dp-text-2);
}
.clean-rule-field {
  margin-bottom: 4px;
}
.clean-rule-field code {
  background: var(--dp-bg-page);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: #722ed1;
}
.clean-rule-desc {
  color: var(--dp-text-3);
  font-size: 12px;
}
</style>
