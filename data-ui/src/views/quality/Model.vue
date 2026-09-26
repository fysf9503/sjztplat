<template>
  <div class="dp-page">
    <PageHeader title="质量模型管理" desc="构建数据质量评估模型，配置多维度指标权重，量化评估数据资产质量水平。">
      <el-button type="primary" :icon="Plus" @click="openEdit()">新建模型</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索模型名称" clearable style="width: 220px" :prefix-icon="Search" @input="loadData" />
        <el-select v-model="query.type" placeholder="模型类型" clearable style="width: 140px" @change="loadData">
          <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="loadData">
          <el-option label="启用" value="启用" />
          <el-option label="停用" value="停用" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="name" label="模型名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="model-name">
              <el-icon class="model-icon"><Histogram /></el-icon>
              <span>{{ row.name }}</span>
              <el-tag v-if="row.isDefault" size="small" type="primary" effect="plain" style="margin-left: 8px">默认</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="模型类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ruleCount" label="关联规则数" width="110" align="center" />
        <el-table-column prop="evalCount" label="评估次数" width="100" align="center" />
        <el-table-column prop="avgScore" label="平均得分" width="130">
          <template #default="{ row }">
            <div class="score-display">
              <span class="score-value" :style="{ color: getScoreColor(row.avgScore) }">{{ row.avgScore }}</span>
              <span class="score-max">/100</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <span class="ds-status" :class="row.status">
              <i></i>{{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="evaluate(row)">评估</el-button>
            <el-button link type="primary" size="small" @click="viewReport(row)">查看报告</el-button>
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="removeModel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>

    <!-- 模型详情抽屉 -->
    <el-drawer v-model="detailVisible" title="模型详情" size="640px" destroy-on-close>
      <template v-if="currentModel">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="模型名称" :span="2">{{ currentModel.name }}</el-descriptions-item>
            <el-descriptions-item label="模型类型">{{ currentModel.type }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <span class="ds-status" :class="currentModel.status">
                <i></i>{{ currentModel.status }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="关联规则数">{{ currentModel.ruleCount }} 条</el-descriptions-item>
            <el-descriptions-item label="评估次数">{{ currentModel.evalCount }} 次</el-descriptions-item>
            <el-descriptions-item label="平均得分">
              <span :style="{ color: getScoreColor(currentModel.avgScore), fontWeight: 600 }">{{ currentModel.avgScore }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ currentModel.createdAt }}</el-descriptions-item>
            <el-descriptions-item label="负责人" :span="2">{{ currentModel.owner }}</el-descriptions-item>
            <el-descriptions-item label="模型描述" :span="2">{{ currentModel.desc || '暂无描述' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 指标配置 -->
        <div class="detail-section">
          <div class="section-title">
            指标配置
            <span style="margin-left: auto; font-size: 12px; color: var(--dp-text-3); font-weight: normal">
              权重合计: {{ totalWeight }}%
            </span>
          </div>
          <div class="indicators-list">
            <div v-for="(ind, idx) in indicators" :key="idx" class="indicator-item">
              <div class="indicator-header">
                <span class="indicator-name">{{ ind.name }}</span>
                <span class="indicator-weight">权重 {{ ind.weight }}%</span>
              </div>
              <div class="indicator-body">
                <div class="indicator-rules">
                  <el-tag v-for="r in ind.rules" :key="r" size="small" effect="plain">{{ r }}</el-tag>
                </div>
                <div class="indicator-score">
                  得分: <span :style="{ color: getScoreColor(ind.score), fontWeight: 600 }">{{ ind.score }}</span>
                </div>
              </div>
              <el-progress :percentage="ind.score" :stroke-width="4" :color="getScoreColor(ind.score)" />
            </div>
          </div>
        </div>

        <!-- 评估历史 -->
        <div class="detail-section">
          <div class="section-title">评估历史</div>
          <EChart :option="evalTrendOption" height="220px" />
          <el-table :data="evalHistory" border stripe size="small" style="margin-top: 12px">
            <el-table-column prop="time" label="评估时间" width="160" />
            <el-table-column prop="scope" label="评估范围" min-width="140" />
            <el-table-column prop="score" label="综合得分" width="100">
              <template #default="{ row }">
                <span :style="{ color: getScoreColor(row.score), fontWeight: 600 }">{{ row.score }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="level" label="等级" width="80">
              <template #default="{ row }">
                <el-tag :type="levelTagType(row.level)" size="small">{{ row.level }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="operator" label="操作人" width="80" />
          </el-table>
        </div>
      </template>
    </el-drawer>

    <!-- 新建/编辑模型弹窗 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑模型' : '新建模型'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="模型名称" required>
          <el-input v-model="form.name" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="模型类型" required>
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型描述">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="请描述模型的用途和适用范围" />
        </el-form-item>
        <el-form-item label="负责人" required>
          <el-select v-model="form.owner" style="width: 100%">
            <el-option v-for="o in ownerOptions" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveModel">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EChart from '@/components/EChart.vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const detailVisible = ref(false)
const editVisible = ref(false)
const currentModel = ref(null)

const typeOptions = ['综合质量模型', '交易域模型', '客户域模型', '财务域模型', '供应链模型', '自定义模型']
const ownerOptions = ['张三', '李四', '王五', '赵六', '钱七']

const query = reactive({
  keyword: '',
  type: '',
  status: '',
  page: 1,
  size: 10
})

const total = ref(0)
const tableData = ref([])

// Mock 数据
const allData = ref([
  { id: 1, name: '企业级数据质量综合模型', type: '综合质量模型', ruleCount: 128, evalCount: 56, avgScore: 96.2, status: '启用', createdAt: '2026-03-15 10:30:00', owner: '张三', isDefault: true, desc: '覆盖所有业务域的综合质量评估模型，包含6大维度、28类指标。' },
  { id: 2, name: '交易域质量评估模型', type: '交易域模型', ruleCount: 45, evalCount: 89, avgScore: 97.5, status: '启用', createdAt: '2026-04-02 14:20:00', owner: '李四', isDefault: false, desc: '针对交易数据域的专项质量评估，重点关注订单、支付、退款等核心表。' },
  { id: 3, name: '客户域质量评估模型', type: '客户域模型', ruleCount: 38, evalCount: 72, avgScore: 95.8, status: '启用', createdAt: '2026-04-10 09:15:00', owner: '王五', isDefault: false, desc: '客户数据质量评估模型，覆盖客户信息、会员、画像等数据。' },
  { id: 4, name: '财务域质量评估模型', type: '财务域模型', ruleCount: 32, evalCount: 45, avgScore: 94.3, status: '启用', createdAt: '2026-05-08 11:45:00', owner: '赵六', isDefault: false, desc: '财务数据质量评估，重点关注准确性和一致性。' },
  { id: 5, name: '供应链质量评估模型', type: '供应链模型', ruleCount: 28, evalCount: 34, avgScore: 96.7, status: '启用', createdAt: '2026-06-12 16:00:00', owner: '钱七', isDefault: false, desc: '供应链数据质量评估，覆盖商品、库存、采购等数据。' },
  { id: 6, name: '日志数据质量模型', type: '自定义模型', ruleCount: 15, evalCount: 120, avgScore: 98.2, status: '启用', createdAt: '2026-07-01 08:30:00', owner: '张三', isDefault: false, desc: '日志数据质量专项评估，主要关注时效性和完整性。' },
  { id: 7, name: '营销活动质量模型', type: '自定义模型', ruleCount: 20, evalCount: 28, avgScore: 95.1, status: '停用', createdAt: '2026-08-15 13:20:00', owner: '李四', isDefault: false, desc: '营销活动相关数据质量评估模型。' }
])

// 指标配置
const indicators = ref([
  { name: '完整性', weight: 25, score: 97.2, rules: ['非空校验', '记录完整性', '字段完整性'] },
  { name: '准确性', weight: 25, score: 95.8, rules: ['值域校验', '格式校验', '逻辑校验'] },
  { name: '一致性', weight: 20, score: 94.5, rules: ['参照完整性', '汇总一致性', '跨表一致性'] },
  { name: '唯一性', weight: 15, score: 96.8, rules: ['主键唯一性', '业务键唯一性'] },
  { name: '时效性', weight: 10, score: 93.2, rules: ['数据延迟', '更新及时性'] },
  { name: '有效性', weight: 5, score: 98.1, rules: ['枚举有效性', '格式有效性'] }
])

const totalWeight = computed(() => indicators.value.reduce((sum, i) => sum + i.weight, 0))

// 评估历史
const evalHistory = ref([
  { time: '2026-09-25 02:00:00', scope: '全量评估', score: 96.2, level: '优秀', operator: '系统' },
  { time: '2026-09-24 02:00:00', scope: '全量评估', score: 95.8, level: '优秀', operator: '系统' },
  { time: '2026-09-23 02:00:00', scope: '全量评估', score: 96.5, level: '优秀', operator: '系统' },
  { time: '2026-09-22 02:00:00', scope: '全量评估', score: 95.2, level: '良好', operator: '系统' },
  { time: '2026-09-21 02:00:00', scope: '全量评估', score: 96.8, level: '优秀', operator: '系统' },
  { time: '2026-09-20 02:00:00', scope: '全量评估', score: 97.1, level: '优秀', operator: '系统' },
  { time: '2026-09-19 02:00:00', scope: '全量评估', score: 94.5, level: '良好', operator: '系统' }
])

// 表单
const form = reactive({
  id: null,
  name: '',
  type: '综合质量模型',
  desc: '',
  owner: '',
  isDefault: false
})

const evalTrendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 45, right: 20, top: 20, bottom: 30 },
  xAxis: {
    type: 'category',
    data: evalHistory.value.map(h => h.time.slice(5, 10)).reverse(),
    axisLabel: { fontSize: 11 }
  },
  yAxis: {
    type: 'value',
    min: 90,
    max: 100,
    axisLabel: { formatter: '{value}', fontSize: 11 }
  },
  series: [{
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    data: evalHistory.value.map(h => h.score).reverse(),
    lineStyle: { color: '#1664ff', width: 2 },
    itemStyle: { color: '#1664ff' },
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(22, 100, 255, 0.2)' },
          { offset: 1, color: 'rgba(22, 100, 255, 0.02)' }
        ]
      }
    }
  }]
}))

function typeTagType(type) {
  const map = {
    '综合质量模型': 'primary',
    '交易域模型': 'success',
    '客户域模型': 'warning',
    '财务域模型': 'danger',
    '供应链模型': 'purple',
    '自定义模型': 'info'
  }
  return map[type] || 'info'
}

function getScoreColor(score) {
  if (score >= 95) return '#00b42a'
  if (score >= 90) return '#ff7d00'
  return '#f53f3f'
}

function levelTagType(level) {
  const map = { '优秀': 'success', '良好': 'primary', '合格': 'warning', '不合格': 'danger' }
  return map[level] || 'info'
}

function loadData() {
  loading.value = true
  setTimeout(() => {
    let list = allData.value.filter(m =>
      (!query.keyword || m.name.includes(query.keyword)) &&
      (!query.type || m.type === query.type) &&
      (!query.status || m.status === query.status)
    )
    total.value = list.length
    const start = (query.page - 1) * query.size
    tableData.value = list.slice(start, start + query.size)
    loading.value = false
  }, 300)
}

function openDetail(row) {
  currentModel.value = row
  detailVisible.value = true
}

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      type: row.type,
      desc: row.desc,
      owner: row.owner,
      isDefault: row.isDefault
    })
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      type: '综合质量模型',
      desc: '',
      owner: '',
      isDefault: false
    })
  }
  editVisible.value = true
}

function saveModel() {
  if (!form.name) return ElMessage.warning('请输入模型名称')
  if (!form.owner) return ElMessage.warning('请选择负责人')

  if (form.id) {
    const idx = allData.value.findIndex(m => m.id === form.id)
    if (idx !== -1) {
      allData.value[idx] = { ...allData.value[idx], ...form }
      if (form.isDefault) {
        allData.value.forEach(m => { if (m.id !== form.id) m.isDefault = false })
      }
    }
    ElMessage.success('模型已更新')
  } else {
    allData.value.unshift({
      id: Date.now(),
      name: form.name,
      type: form.type,
      ruleCount: 0,
      evalCount: 0,
      avgScore: 100,
      status: '启用',
      createdAt: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
      owner: form.owner,
      isDefault: form.isDefault,
      desc: form.desc
    })
    if (form.isDefault) {
      allData.value.forEach(m => { if (m.id !== form.id) m.isDefault = false })
    }
    ElMessage.success('模型创建成功')
  }
  editVisible.value = false
  loadData()
}

function evaluate(row) {
  ElMessageBox.confirm(
    `确定对模型「${row.name}」执行一次评估吗？评估可能需要几分钟时间。`,
    '评估确认',
    { type: 'info', confirmButtonText: '开始评估' }
  ).then(() => {
    ElMessage.info(`模型「${row.name}」评估任务已提交，正在执行中...`)
    setTimeout(() => {
      row.evalCount += 1
      row.avgScore = +(94 + Math.random() * 5).toFixed(1)
      ElMessage.success(`模型「${row.name}」评估完成，得分 ${row.avgScore}`)
      loadData()
    }, 2000)
  }).catch(() => {})
}

function viewReport(row) {
  ElMessage.info(`正在打开「${row.name}」的评估报告...`)
}

function removeModel(row) {
  ElMessageBox.confirm(
    `确定删除模型「${row.name}」吗？删除后相关的评估历史也将被清除。`,
    '删除确认',
    { type: 'warning' }
  ).then(() => {
    allData.value = allData.value.filter(m => m.id !== row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.model-name {
  display: flex;
  align-items: center;
}

.model-icon {
  color: var(--dp-primary);
  font-size: 16px;
  margin-right: 8px;
}

.score-display {
  display: flex;
  align-items: baseline;
}

.score-value {
  font-size: 16px;
  font-weight: 700;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}

.score-max {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-left: 2px;
}

.ds-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
}

.ds-status i {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.ds-status.启用 { color: var(--dp-success); }
.ds-status.启用 i { background: var(--dp-success); box-shadow: 0 0 6px var(--dp-success); }
.ds-status.停用 { color: var(--dp-text-3); }
.ds-status.停用 i { background: var(--dp-text-3); }

.detail-section {
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid var(--dp-primary);
}

.indicators-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.indicator-item {
  background: var(--dp-bg-page);
  border-radius: 8px;
  padding: 12px 14px;
}

.indicator-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.indicator-name {
  font-weight: 600;
  font-size: 13px;
  color: var(--dp-text-1);
}

.indicator-weight {
  font-size: 12px;
  color: var(--dp-primary);
  background: var(--dp-primary-light);
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.indicator-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.indicator-rules {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.indicator-score {
  font-size: 12px;
  color: var(--dp-text-3);
  flex-shrink: 0;
  margin-left: 10px;
}
</style>
