<template>
  <div class="dp-page">
    <PageHeader title="报表模型配置" desc="管理数据集模型、表关联关系和字段配置，为报表提供数据模型支撑">
      <el-button type="primary" :icon="Plus" @click="openCreate">新建模型</el-button>
    </PageHeader>

    <div class="rp-model-layout">
      <!-- 左侧模型列表 -->
      <div class="rp-model-sidebar">
        <div class="dp-card" style="height: 100%; margin-bottom: 0;">
          <div class="rp-sidebar-title">
            <el-icon><DataLine /></el-icon>
            <span>数据集模型</span>
          </div>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索模型"
            size="small"
            clearable
            :prefix-icon="Search"
            style="margin-bottom: 12px"
          />
          <div class="rp-model-list">
            <div
              v-for="model in filteredModels"
              :key="model.id"
              class="rp-model-item"
              :class="{ active: currentModel?.id === model.id }"
              @click="selectModel(model)"
            >
              <div class="rp-model-icon" :style="{ background: model.bgColor, color: model.iconColor }">
                <el-icon :size="18"><component :is="model.icon" /></el-icon>
              </div>
              <div class="rp-model-info">
                <div class="rp-model-name">{{ model.name }}</div>
                <div class="rp-model-meta">{{ model.tables }} 张表 · {{ model.fields }} 字段</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧配置区域 -->
      <div class="rp-model-main">
        <div v-if="currentModel" class="dp-fade-in">
          <!-- 模型信息 -->
          <div class="dp-card">
            <div class="dp-card-header">
              <div class="dp-card-title">
                <el-icon color="var(--dp-primary)"><DataLine /></el-icon>
                <span style="margin-left: 6px">{{ currentModel.name }}</span>
                <el-tag size="small" type="primary" effect="plain" style="margin-left: 8px">{{ currentModel.type }}</el-tag>
              </div>
              <div class="dp-flex dp-gap-8">
                <el-button :icon="Edit" size="small">编辑模型</el-button>
                <el-button type="primary" :icon="Plus" size="small" @click="addTableDialog = true">添加表</el-button>
              </div>
            </div>
            <div class="rp-model-desc">{{ currentModel.desc }}</div>
            <div class="rp-model-stats">
              <div class="rp-stat-item">
                <div class="rp-stat-value">{{ currentModel.tables }}</div>
                <div class="rp-stat-label">数据表</div>
              </div>
              <div class="rp-stat-divider"></div>
              <div class="rp-stat-item">
                <div class="rp-stat-value">{{ currentModel.fields }}</div>
                <div class="rp-stat-label">字段数</div>
              </div>
              <div class="rp-stat-divider"></div>
              <div class="rp-stat-item">
                <div class="rp-stat-value">{{ currentModel.relations }}</div>
                <div class="rp-stat-label">关联关系</div>
              </div>
              <div class="rp-stat-divider"></div>
              <div class="rp-stat-item">
                <div class="rp-stat-value">{{ currentModel.owner }}</div>
                <div class="rp-stat-label">负责人</div>
              </div>
            </div>
          </div>

          <!-- 表关联配置 -->
          <div class="dp-card">
            <div class="dp-card-header">
              <div class="dp-card-title">
                <el-icon color="var(--dp-success)"><Connection /></el-icon>
                <span style="margin-left: 6px">表关联配置</span>
              </div>
              <el-button link type="primary" size="small" :icon="Plus">添加关联</el-button>
            </div>
            <div class="rp-relation-graph">
              <div v-for="table in modelTables" :key="table.id" class="rp-table-node" :style="{ left: table.x + 'px', top: table.y + 'px' }">
                <div class="rp-table-node-header">
                  <el-icon :size="14"><Grid /></el-icon>
                  <span class="rp-table-name">{{ table.name }}</span>
                </div>
                <div class="rp-table-node-body">
                  <div v-for="field in table.fields.slice(0, 4)" :key="field.name" class="rp-field-item">
                    <span class="rp-field-name">{{ field.name }}</span>
                    <span class="rp-field-type">{{ field.type }}</span>
                  </div>
                  <div v-if="table.fields.length > 4" class="rp-field-more">
                    还有 {{ table.fields.length - 4 }} 个字段
                  </div>
                </div>
              </div>
              <!-- 关联线（简化展示） -->
              <svg class="rp-relation-lines" viewBox="0 0 600 300">
                <line x1="160" y1="80" x2="340" y2="80" stroke="#165dff" stroke-width="2" stroke-dasharray="4,4" />
                <line x1="460" y1="80" x2="340" y2="200" stroke="#00b42a" stroke-width="2" stroke-dasharray="4,4" />
                <line x1="160" y1="200" x2="340" y2="200" stroke="#ff7d00" stroke-width="2" stroke-dasharray="4,4" />
              </svg>
            </div>
            <el-table :data="relationList" border stripe size="small" style="margin-top: 16px">
              <el-table-column prop="leftTable" label="左表" width="160" />
              <el-table-column prop="leftField" label="左表字段" width="140" class-name="dp-mono" />
              <el-table-column label="关联类型" width="100" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="joinTypeTag(row.joinType)" effect="plain">{{ row.joinType }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="rightField" label="右表字段" width="140" class-name="dp-mono" />
              <el-table-column prop="rightTable" label="右表" width="160" />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default>
                  <el-button link type="primary" size="small">编辑</el-button>
                  <el-button link type="danger" size="small">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 字段配置 -->
          <div class="dp-card">
            <div class="dp-card-header">
              <div class="dp-card-title">
                <el-icon color="var(--dp-warning)"><Menu /></el-icon>
                <span style="margin-left: 6px">字段配置</span>
              </div>
              <div class="dp-flex dp-gap-8">
                <el-select v-model="selectedTable" size="small" style="width: 180px">
                  <el-option v-for="t in modelTables" :key="t.id" :label="t.name" :value="t.id" />
                </el-select>
                <el-button :icon="Plus" size="small">新增字段</el-button>
              </div>
            </div>
            <el-table :data="fieldList" border stripe>
              <el-table-column prop="name" label="字段名" width="160" class-name="dp-mono" />
              <el-table-column prop="label" label="显示名" width="140" />
              <el-table-column prop="type" label="数据类型" width="120">
                <template #default="{ row }">
                  <el-tag size="small" type="info" effect="plain">{{ row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="aggregation" label="聚合方式" width="110">
                <template #default="{ row }">
                  <span v-if="row.aggregation" class="dp-tag primary">{{ row.aggregation }}</span>
                  <span v-else class="dp-desc">-</span>
                </template>
              </el-table-column>
              <el-table-column label="维度/指标" width="100" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.role === '维度' ? 'warning' : 'success'" effect="plain">{{ row.role }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="format" label="格式" width="120" />
              <el-table-column label="可见" width="70" align="center">
                <template #default="{ row }">
                  <el-switch v-model="row.visible" size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="desc" label="描述" min-width="150" show-overflow-tooltip />
              <el-table-column label="操作" width="120" fixed="right">
                <template #default>
                  <el-button link type="primary" size="small">编辑</el-button>
                  <el-button link type="danger" size="small">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
        <el-empty v-else description="请选择左侧模型进行配置" :image-size="100" />
      </div>
    </div>

    <!-- 新建模型弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新建数据集模型" width="560px" destroy-on-close>
      <el-form :model="createForm" label-width="100px" label-position="right">
        <el-form-item label="模型名称" required>
          <el-input v-model="createForm.name" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="模型类型" required>
          <el-select v-model="createForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="星型模型" value="星型模型" />
            <el-option label="雪花模型" value="雪花模型" />
            <el-option label="宽表模型" value="宽表模型" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源" required>
          <el-select v-model="createForm.datasource" placeholder="请选择数据源" style="width: 100%">
            <el-option label="核心交易库" value="核心交易库" />
            <el-option label="客户主数据库" value="客户主数据库" />
            <el-option label="财务系统库" value="财务系统库" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="createForm.owner" placeholder="请选择负责人" style="width: 100%">
            <el-option v-for="o in owners" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.desc" type="textarea" :rows="3" placeholder="请输入模型描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 添加表弹窗 -->
    <el-dialog v-model="addTableDialog" title="添加数据表" width="560px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="选择表" required>
          <el-select placeholder="请选择要添加的表" style="width: 100%" filterable>
            <el-option label="ods_order_detail" value="ods_order_detail" />
            <el-option label="dim_product" value="dim_product" />
            <el-option label="dim_customer" value="dim_customer" />
            <el-option label="dim_date" value="dim_date" />
            <el-option label="dws_sales_summary" value="dws_sales_summary" />
          </el-select>
        </el-form-item>
        <el-form-item label="表别名">
          <el-input placeholder="请输入表别名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addTableDialog = false">取消</el-button>
        <el-button type="primary">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { owners } from '@/mock'
import { Edit, Plus, Search } from '@element-plus/icons-vue'

const searchKeyword = ref('')
const currentModel = ref(null)
const createDialogVisible = ref(false)
const addTableDialog = ref(false)
const selectedTable = ref(1)

const createForm = reactive({
  name: '',
  type: '',
  datasource: '',
  owner: '',
  desc: ''
})

const models = [
  {
    id: 1, name: '销售分析模型', type: '星型模型', icon: 'DataLine', bgColor: '#e8f3ff', iconColor: '#165dff',
    tables: 5, fields: 48, relations: 4, owner: '张伟',
    desc: '基于销售订单数据构建的星型模型，包含订单事实表和产品、客户、日期、渠道维度表，支持多维度销售分析。'
  },
  {
    id: 2, name: '客户画像模型', type: '宽表模型', icon: 'User', bgColor: '#f5e8ff', iconColor: '#722ed1',
    tables: 3, fields: 36, relations: 2, owner: '李娜',
    desc: '客户360度画像宽表模型，整合客户基础信息、行为数据、交易数据、偏好标签等多维度数据。'
  },
  {
    id: 3, name: '财务分析模型', type: '雪花模型', icon: 'Wallet', bgColor: '#e8ffea', iconColor: '#00b42a',
    tables: 7, fields: 62, relations: 6, owner: '王强',
    desc: '财务主题雪花模型，包含收入、成本、费用、利润等多级财务维度，支持精细化财务分析。'
  },
  {
    id: 4, name: '运营监控模型', type: '星型模型', icon: 'TrendCharts', bgColor: '#fff3e8', iconColor: '#ff7d00',
    tables: 4, fields: 28, relations: 3, owner: '赵敏',
    desc: '运营指标监控模型，涵盖用户活跃、留存、转化等核心运营指标，支持实时监控与分析。'
  },
  {
    id: 5, name: '供应链模型', type: '雪花模型', icon: 'Box', bgColor: '#e0fffa', iconColor: '#0fc6c2',
    tables: 6, fields: 42, relations: 5, owner: '孙磊',
    desc: '供应链主题模型，包含供应商、库存、采购、物流等数据，支持供应链全链路分析。'
  },
  {
    id: 6, name: '营销效果模型', type: '宽表模型', icon: 'Promotion', bgColor: '#ffe8f4', iconColor: '#f759ab',
    tables: 4, fields: 38, relations: 3, owner: '周婷',
    desc: '营销活动效果分析模型，整合活动投入、转化、ROI等数据，支持多渠道营销效果评估。'
  }
]

const filteredModels = computed(() =>
  models.filter(m => !searchKeyword.value || m.name.includes(searchKeyword.value))
)

const modelTables = [
  { id: 1, name: 'dws_sales_fact', x: 20, y: 40, fields: [
    { name: 'order_id', type: 'BIGINT' },
    { name: 'product_id', type: 'BIGINT' },
    { name: 'customer_id', type: 'BIGINT' },
    { name: 'date_id', type: 'DATE' },
    { name: 'sales_amount', type: 'DECIMAL' },
    { name: 'quantity', type: 'INT' }
  ]},
  { id: 2, name: 'dim_product', x: 380, y: 40, fields: [
    { name: 'product_id', type: 'BIGINT' },
    { name: 'product_name', type: 'VARCHAR' },
    { name: 'category', type: 'VARCHAR' },
    { name: 'brand', type: 'VARCHAR' },
    { name: 'price', type: 'DECIMAL' }
  ]},
  { id: 3, name: 'dim_customer', x: 20, y: 180, fields: [
    { name: 'customer_id', type: 'BIGINT' },
    { name: 'customer_name', type: 'VARCHAR' },
    { name: 'level', type: 'VARCHAR' },
    { name: 'region', type: 'VARCHAR' },
    { name: 'phone', type: 'VARCHAR' }
  ]},
  { id: 4, name: 'dim_date', x: 380, y: 180, fields: [
    { name: 'date_id', type: 'DATE' },
    { name: 'year', type: 'INT' },
    { name: 'month', type: 'INT' },
    { name: 'quarter', type: 'VARCHAR' },
    { name: 'weekday', type: 'VARCHAR' }
  ]}
]

const relationList = [
  { leftTable: 'dws_sales_fact', leftField: 'product_id', joinType: 'LEFT JOIN', rightTable: 'dim_product', rightField: 'product_id' },
  { leftTable: 'dws_sales_fact', leftField: 'customer_id', joinType: 'LEFT JOIN', rightTable: 'dim_customer', rightField: 'customer_id' },
  { leftTable: 'dws_sales_fact', leftField: 'date_id', joinType: 'LEFT JOIN', rightTable: 'dim_date', rightField: 'date_id' }
]

const fieldList = [
  { name: 'order_id', label: '订单ID', type: 'BIGINT', aggregation: '', role: '维度', format: '', visible: true, desc: '订单唯一标识' },
  { name: 'product_name', label: '商品名称', type: 'VARCHAR', aggregation: '', role: '维度', format: '', visible: true, desc: '商品名称' },
  { name: 'category', label: '商品分类', type: 'VARCHAR', aggregation: '', role: '维度', format: '', visible: true, desc: '商品所属分类' },
  { name: 'customer_name', label: '客户名称', type: 'VARCHAR', aggregation: '', role: '维度', format: '', visible: true, desc: '客户姓名' },
  { name: 'region', label: '地区', type: 'VARCHAR', aggregation: '', role: '维度', format: '', visible: true, desc: '客户所在地区' },
  { name: 'date_id', label: '日期', type: 'DATE', aggregation: '', role: '维度', format: 'yyyy-MM-dd', visible: true, desc: '订单日期' },
  { name: 'sales_amount', label: '销售金额', type: 'DECIMAL', aggregation: 'SUM', role: '指标', format: '¥#,##0.00', visible: true, desc: '订单销售金额' },
  { name: 'quantity', label: '销售数量', type: 'INT', aggregation: 'SUM', role: '指标', format: '#,##0', visible: true, desc: '商品销售数量' },
  { name: 'order_count', label: '订单数', type: 'INT', aggregation: 'COUNT', role: '指标', format: '#,##0', visible: true, desc: '订单数量' },
  { name: 'avg_price', label: '平均单价', type: 'DECIMAL', aggregation: 'AVG', role: '指标', format: '¥#,##0.00', visible: false, desc: '商品平均单价' }
]

function selectModel(model) {
  currentModel.value = model
  selectedTable.value = modelTables[0]?.id
}

function joinTypeTag(type) {
  const map = { 'LEFT JOIN': 'primary', 'RIGHT JOIN': 'success', 'INNER JOIN': 'warning', 'FULL JOIN': 'danger' }
  return map[type] || 'info'
}

function openCreate() {
  createForm.name = ''
  createForm.type = ''
  createForm.datasource = ''
  createForm.owner = owners[0]
  createForm.desc = ''
  createDialogVisible.value = true
}

function handleCreate() {
  if (!createForm.name || !createForm.type || !createForm.datasource) {
    ElMessage.warning('请填写必填项')
    return
  }
  ElMessage.success('模型创建成功')
  createDialogVisible.value = false
}
</script>

<style scoped>
.rp-model-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 140px);
  min-height: 600px;
}

.rp-model-sidebar {
  width: 260px;
  flex-shrink: 0;
}

.rp-model-main {
  flex: 1;
  min-width: 0;
  overflow: auto;
}

.rp-sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 12px;
}

.rp-model-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rp-model-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.rp-model-item:hover {
  background: var(--dp-primary-bg);
}

.rp-model-item.active {
  background: var(--dp-primary-light);
  border-color: var(--dp-primary);
}

.rp-model-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rp-model-info {
  flex: 1;
  min-width: 0;
}

.rp-model-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.rp-model-meta {
  font-size: 11px;
  color: var(--dp-text-3);
}

.rp-model-desc {
  font-size: 13px;
  color: var(--dp-text-2);
  line-height: 1.6;
  padding: 12px;
  background: var(--dp-bg-page);
  border-radius: 8px;
  margin-bottom: 16px;
}

.rp-model-stats {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-top: 1px solid var(--dp-border-light);
}

.rp-stat-item {
  flex: 1;
  text-align: center;
}

.rp-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', sans-serif;
  margin-bottom: 4px;
}

.rp-stat-label {
  font-size: 12px;
  color: var(--dp-text-3);
}

.rp-stat-divider {
  width: 1px;
  height: 40px;
  background: var(--dp-border-light);
}

/* 表关联图 */
.rp-relation-graph {
  position: relative;
  height: 300px;
  background: var(--dp-bg-page);
  border-radius: 8px;
  overflow: hidden;
}

.rp-table-node {
  position: absolute;
  width: 180px;
  background: #fff;
  border: 1px solid var(--dp-border);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  z-index: 1;
}

.rp-table-node-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  font-size: 13px;
  font-weight: 600;
  border-bottom: 1px solid var(--dp-border-light);
}

.rp-table-name {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
}

.rp-table-node-body {
  padding: 8px 12px;
}

.rp-field-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 3px 0;
  font-size: 12px;
}

.rp-field-name {
  color: var(--dp-text-2);
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 11px;
}

.rp-field-type {
  color: var(--dp-text-3);
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 10px;
}

.rp-field-more {
  font-size: 11px;
  color: var(--dp-primary);
  text-align: center;
  padding-top: 4px;
  cursor: pointer;
}

.rp-relation-lines {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

@media (max-width: 1200px) {
  .rp-model-sidebar {
    display: none;
  }
}
</style>
