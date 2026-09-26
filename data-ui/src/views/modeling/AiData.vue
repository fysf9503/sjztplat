<template>
  <div class="dp-page">
    <PageHeader title="AI业务数据" desc="管理AI业务数据资产，包括结构化数据、非结构化数据和向量数据，支撑AI模型训练与推理">
      <el-button type="primary" :icon="Plus" @click="createVisible = true">新建数据集</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索数据集名称" clearable style="width: 240px" :prefix-icon="Search" />
        <el-select v-model="query.type" placeholder="数据类型" clearable style="width: 140px">
          <el-option label="结构化数据" value="结构化" />
          <el-option label="非结构化数据" value="非结构化" />
          <el-option label="向量数据" value="向量" />
        </el-select>
        <el-select v-model="query.source" placeholder="数据来源" clearable style="width: 140px">
          <el-option label="业务数据库" value="业务数据库" />
          <el-option label="文件上传" value="文件上传" />
          <el-option label="AI生成" value="AI生成" />
          <el-option label="外部接入" value="外部接入" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredData" border stripe v-loading="loading">
        <el-table-column prop="name" label="数据集名称" min-width="200">
          <template #default="{ row }">
            <div class="aidata-name-cell">
              <div class="aidata-icon" :class="'icon-' + typeClass(row.type)">
                <el-icon><component :is="typeIcon(row.type)" /></el-icon>
              </div>
              <div>
                <div class="aidata-title">{{ row.name }}</div>
                <div class="aidata-sub">{{ row.description }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="typeTagType(row.type)">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataSize" label="数据量" width="120" align="right">
          <template #default="{ row }">
            <span v-if="row.type === '结构化'">{{ formatNumber(row.dataSize) }} 行</span>
            <span v-else-if="row.type === '非结构化'">{{ formatNumber(row.dataSize) }} 份</span>
            <span v-else>{{ formatNumber(row.dataSize) }} 条</span>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="120" />
        <el-table-column prop="tags" label="标签" min-width="160">
          <template #default="{ row }">
            <el-tag v-for="tag in row.tags" :key="tag" size="small" effect="plain" style="margin-right: 4px; margin-bottom: 2px">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="qualityScore" label="质量评分" width="100" align="center">
          <template #default="{ row }">
            <el-progress type="dashboard" :percentage="row.qualityScore" :width="50" :stroke-width="6" />
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
      />
    </div>

    <!-- 数据集详情抽屉 -->
    <el-drawer v-model="detailVisible" title="数据集详情" size="640px" destroy-on-close>
      <template v-if="currentDataset">
        <div class="aidata-detail">
          <!-- 基本信息 -->
          <div class="add-section">
            <div class="add-section-title">基本信息</div>
            <div class="add-info-grid">
              <div class="add-info-item">
                <span class="add-label">数据集名称</span>
                <span class="add-value">{{ currentDataset.name }}</span>
              </div>
              <div class="add-info-item">
                <span class="add-label">数据类型</span>
                <el-tag size="small" effect="plain" :type="typeTagType(currentDataset.type)">{{ currentDataset.type }}</el-tag>
              </div>
              <div class="add-info-item">
                <span class="add-label">数据量</span>
                <span class="add-value">{{ formatNumber(currentDataset.dataSize) }} {{ currentDataset.type === '结构化' ? '行' : currentDataset.type === '非结构化' ? '份' : '条' }}</span>
              </div>
              <div class="add-info-item">
                <span class="add-label">数据来源</span>
                <span class="add-value">{{ currentDataset.source }}</span>
              </div>
              <div class="add-info-item">
                <span class="add-label">负责人</span>
                <span class="add-value">{{ currentDataset.owner }}</span>
              </div>
              <div class="add-info-item">
                <span class="add-label">更新时间</span>
                <span class="add-value">{{ currentDataset.updatedAt }}</span>
              </div>
            </div>
          </div>

          <!-- 质量评分 -->
          <div class="add-section">
            <div class="add-section-title">数据质量评分</div>
            <div class="quality-overview">
              <div class="quality-score">
                <el-progress type="dashboard" :percentage="currentDataset.qualityScore" :width="100" :stroke-width="8" color="#1664ff" />
                <div class="quality-label">综合评分</div>
              </div>
              <div class="quality-items">
                <div v-for="item in qualityItems" :key="item.name" class="quality-item">
                  <span class="qi-name">{{ item.name }}</span>
                  <el-progress :percentage="item.score" :stroke-width="6" :show-text="false" style="width: 180px" />
                  <span class="qi-score">{{ item.score }}%</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 数据预览 -->
          <div class="add-section">
            <div class="add-section-title">数据预览</div>
            <el-tabs v-model="previewTab" type="card" size="small">
              <el-tab-pane label="表格预览" name="table">
                <el-table :data="previewData" border stripe size="small" max-height="280">
                  <el-table-column v-for="col in previewColumns" :key="col" :prop="col" :label="col" min-width="100" />
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="样例数据" name="sample">
                <div class="sample-list">
                  <div v-for="(sample, idx) in sampleData" :key="idx" class="sample-item">
                    <div class="sample-header">
                      <el-icon class="sample-icon"><Document /></el-icon>
                      <span class="sample-name">{{ sample.name }}</span>
                      <el-tag size="small" effect="plain">{{ sample.type }}</el-tag>
                    </div>
                    <div class="sample-content">{{ sample.content }}</div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>

          <!-- 使用记录 -->
          <div class="add-section">
            <div class="add-section-title">使用记录</div>
            <el-table :data="usageRecords" border stripe size="small">
              <el-table-column prop="time" label="使用时间" width="160" />
              <el-table-column prop="user" label="使用人" width="80" />
              <el-table-column prop="action" label="操作" width="100" />
              <el-table-column prop="purpose" label="用途" min-width="180" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="80">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.status === '成功' ? 'success' : 'danger'" effect="plain">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </template>

      <template #footer>
        <el-button :icon="Download" @click="exportData">导出数据</el-button>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-drawer>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="createVisible" :title="isEdit ? '编辑数据集' : '新建数据集'" width="520px" destroy-on-close>
      <el-form :model="dataForm" label-width="100px">
        <el-form-item label="数据集名称" required>
          <el-input v-model="dataForm.name" placeholder="请输入数据集名称" />
        </el-form-item>
        <el-form-item label="数据类型" required>
          <el-radio-group v-model="dataForm.type">
            <el-radio value="结构化">结构化数据</el-radio>
            <el-radio value="非结构化">非结构化数据</el-radio>
            <el-radio value="向量">向量数据</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数据来源" required>
          <el-select v-model="dataForm.source" style="width: 100%">
            <el-option label="业务数据库" value="业务数据库" />
            <el-option label="文件上传" value="文件上传" />
            <el-option label="AI生成" value="AI生成" />
            <el-option label="外部接入" value="外部接入" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="dataForm.tags" multiple filterable allow-create style="width: 100%" placeholder="输入标签后回车添加">
            <el-option label="客户" value="客户" />
            <el-option label="交易" value="交易" />
            <el-option label="商品" value="商品" />
            <el-option label="营销" value="营销" />
            <el-option label="训练数据" value="训练数据" />
            <el-option label="测试数据" value="测试数据" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="dataForm.owner" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dataForm.description" type="textarea" :rows="2" placeholder="请输入数据集描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="saveData">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { Download, Plus, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const detailVisible = ref(false)
const createVisible = ref(false)
const isEdit = ref(false)
const currentDataset = ref(null)
const previewTab = ref('table')

const query = reactive({
  keyword: '',
  type: '',
  source: '',
  page: 1,
  size: 10
})

const dataList = ref([
  { id: 1, name: '客户画像训练数据集', type: '结构化', dataSize: 1258600, source: '业务数据库', tags: ['客户', '训练数据', '画像'], qualityScore: 96, owner: '张伟', updatedAt: '2024-09-25 14:30', description: '用于客户画像模型训练的结构化数据' },
  { id: 2, name: '商品评论语料库', type: '非结构化', dataSize: 58620, source: '文件上传', tags: ['商品', 'NLP', '评论'], qualityScore: 88, owner: '李娜', updatedAt: '2024-09-24 10:15', description: '电商平台商品评论语料，用于情感分析' },
  { id: 3, name: '用户行为向量库', type: '向量', dataSize: 2580000, source: 'AI生成', tags: ['用户行为', '向量', '推荐'], qualityScore: 92, owner: '赵敏', updatedAt: '2024-09-26 09:20', description: '用户行为Embedding向量数据，用于推荐系统' },
  { id: 4, name: '交易风控训练集', type: '结构化', dataSize: 896500, source: '业务数据库', tags: ['交易', '风控', '训练数据'], qualityScore: 94, owner: '孙浩', updatedAt: '2024-09-23 16:45', description: '交易风控模型训练数据集' },
  { id: 5, name: '客服对话数据集', type: '非结构化', dataSize: 32600, source: '外部接入', tags: ['客服', '对话', 'NLP'], qualityScore: 85, owner: '周明', updatedAt: '2024-09-22 11:30', description: '客服对话记录，用于对话模型训练' },
  { id: 6, name: '商品图像向量库', type: '向量', dataSize: 1560000, source: 'AI生成', tags: ['商品', '图像', '向量'], qualityScore: 90, owner: '吴丽', updatedAt: '2024-09-21 13:30', description: '商品图像Embedding向量库' },
  { id: 7, name: '营销转化数据集', type: '结构化', dataSize: 425800, source: '业务数据库', tags: ['营销', '转化', '分析'], qualityScore: 93, owner: '张伟', updatedAt: '2024-09-20 10:00', description: '营销活动转化效果数据集' },
  { id: 8, name: '文档知识库语料', type: '非结构化', dataSize: 15800, source: '文件上传', tags: ['知识库', '文档', 'RAG'], qualityScore: 87, owner: '李娜', updatedAt: '2024-09-19 15:00', description: '知识库文档语料，用于RAG检索增强' }
])

const qualityItems = ref([
  { name: '完整性', score: 98 },
  { name: '准确性', score: 95 },
  { name: '一致性', score: 92 },
  { name: '及时性', score: 96 },
  { name: '唯一性', score: 99 }
])

const previewColumns = ['customer_id', 'name', 'level', 'city', 'balance', 'last_active']
const previewData = ref([
  { customer_id: 'C10001', name: '王小明', level: 'VIP3', city: '杭州', balance: '12,580.50', last_active: '2024-09-25' },
  { customer_id: 'C10002', name: '李小红', level: 'VIP2', city: '成都', balance: '8,600.00', last_active: '2024-09-24' },
  { customer_id: 'C10003', name: '张大伟', level: 'VIP1', city: '西安', balance: '3,200.80', last_active: '2024-09-23' },
  { customer_id: 'C10004', name: '刘思琪', level: 'VIP3', city: '深圳', balance: '22,100.30', last_active: '2024-09-25' },
  { customer_id: 'C10005', name: '陈浩然', level: '普通', city: '武汉', balance: '952.00', last_active: '2024-09-20' }
])

const sampleData = ref([
  { name: 'comment_001.txt', type: 'TXT', content: '这款产品真的很好用，质量不错，物流也很快，下次还会回购！推荐给大家...' },
  { name: 'review_002.json', type: 'JSON', content: '{"product_id": "P10001", "rating": 5, "content": "非常满意的一次购物体验，客服态度很好，商品质量也很棒...' },
  { name: 'feedback_003.txt', type: 'TXT', content: '包装有点简陋，但是产品本身还是不错的，性价比很高，希望以后能改进包装...' }
])

const usageRecords = ref([
  { time: '2024-09-25 14:30:00', user: '张伟', action: '查询', purpose: '客户画像模型训练', status: '成功' },
  { time: '2024-09-24 10:15:00', user: '李娜', action: '导出', purpose: '模型训练数据准备', status: '成功' },
  { time: '2024-09-23 16:45:00', user: '赵敏', action: '更新', purpose: '补充新数据', status: '成功' },
  { time: '2024-09-22 11:30:00', user: '孙浩', action: '查询', purpose: '风控模型验证', status: '成功' },
  { time: '2024-09-21 09:00:00', user: '周明', action: '删除', purpose: '清理过期数据', status: '成功' }
])

const filteredData = computed(() => {
  return dataList.value.filter(d =>
    (!query.keyword || d.name.includes(query.keyword)) &&
    (!query.type || d.type === query.type) &&
    (!query.source || d.source === query.source)
  )
})

const total = computed(() => filteredData.value.length)

function typeClass(type) {
  return { 结构化: 'structured', 非结构化: 'unstructured', 向量: 'vector' }[type] || 'structured'
}
function typeIcon(type) {
  return { 结构化: 'Grid', 非结构化: 'Document', 向量: 'Connection' }[type] || 'Grid'
}
function typeTagType(type) {
  return { 结构化: 'primary', 非结构化: 'success', 向量: 'warning' }[type] || 'primary'
}

function formatNumber(num) {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toLocaleString()
}

function handleRefresh() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('数据已刷新')
  }, 800)
}

function viewDetail(row) {
  currentDataset.value = row
  detailVisible.value = true
}

function exportData() {
  ElMessage.success('数据导出任务已提交')
}

// 新建/编辑
const dataForm = reactive({
  name: '',
  type: '结构化',
  source: '业务数据库',
  tags: [],
  owner: '张伟',
  description: ''
})

function openEdit(row) {
  isEdit.value = true
  Object.assign(dataForm, { ...row })
  createVisible.value = true
}

function saveData() {
  if (!dataForm.name) {
    ElMessage.warning('请输入数据集名称')
    return
  }
  if (isEdit.value) {
    const idx = dataList.value.findIndex(d => d.id === dataForm.id)
    if (idx > -1) {
      dataList.value[idx] = { ...dataList.value[idx], ...dataForm, updatedAt: '刚刚' }
    }
    ElMessage.success('数据集已更新')
  } else {
    dataList.value.unshift({
      id: Date.now(),
      ...dataForm,
      dataSize: 0,
      qualityScore: 0,
      updatedAt: '刚刚'
    })
    ElMessage.success('数据集创建成功')
  }
  createVisible.value = false
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除数据集「${row.name}」吗？删除后不可恢复。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(() => {
    dataList.value = dataList.value.filter(d => d.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}
</script>

<style scoped>
.aidata-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.aidata-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}
.aidata-icon.icon-structured { background: var(--dp-primary-light); color: var(--dp-primary); }
.aidata-icon.icon-unstructured { background: var(--dp-success-light); color: var(--dp-success); }
.aidata-icon.icon-vector { background: var(--dp-warning-light); color: var(--dp-warning); }
.aidata-title {
  font-weight: 500;
  color: var(--dp-text-1);
  font-size: 13px;
}
.aidata-sub {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

/* 详情 */
.aidata-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.add-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 12px;
}
.add-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 14px;
  background: var(--dp-bg-page);
  border-radius: 8px;
}
.add-info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.add-label {
  font-size: 12px;
  color: var(--dp-text-3);
}
.add-value {
  font-size: 13px;
  color: var(--dp-text-1);
  font-weight: 500;
}

/* 质量评分 */
.quality-overview {
  display: flex;
  gap: 30px;
  align-items: center;
  padding: 16px;
  background: var(--dp-bg-page);
  border-radius: 8px;
}
.quality-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.quality-label {
  font-size: 12px;
  color: var(--dp-text-3);
}
.quality-items {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.quality-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.qi-name {
  width: 60px;
  font-size: 12px;
  color: var(--dp-text-2);
}
.qi-score {
  width: 40px;
  font-size: 12px;
  color: var(--dp-text-2);
  text-align: right;
}

/* 样例数据 */
.sample-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.sample-item {
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  padding: 12px;
}
.sample-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.sample-icon {
  color: var(--dp-primary);
  font-size: 16px;
}
.sample-name {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-1);
  font-family: Consolas, monospace;
}
.sample-content {
  font-size: 12px;
  color: var(--dp-text-2);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
