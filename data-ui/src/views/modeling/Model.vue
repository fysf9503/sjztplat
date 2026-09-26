<template>
  <div class="dp-page">
    <PageHeader title="建模模型配置" desc="管理数据建模引擎与模板，包括行业模板、模型推荐和结构优化配置">
      <el-button type="primary" :icon="Plus" @click="openCreate">新增模板</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索模板名称/适用场景" clearable style="width: 260px" :prefix-icon="Search" />
        <el-select v-model="query.type" placeholder="模板类型" clearable style="width: 140px">
          <el-option label="行业模板" value="行业模板" />
          <el-option label="主题域模板" value="主题域模板" />
          <el-option label="通用模板" value="通用模板" />
          <el-option label="自定义" value="自定义" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="启用" value="启用" />
          <el-option label="停用" value="停用" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredTemplates" border stripe v-loading="loading">
        <el-table-column prop="name" label="模板名称" min-width="200">
          <template #default="{ row }">
            <div class="tpl-name-cell">
              <div class="tpl-icon" :class="'type-' + typeClass(row.type)">
                <el-icon><component :is="typeIcon(row.type)" /></el-icon>
              </div>
              <div>
                <div class="tpl-title">{{ row.name }}</div>
                <div class="tpl-desc">{{ row.description }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="typeTagType(row.type)">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scene" label="适用场景" min-width="160" show-overflow-tooltip />
        <el-table-column prop="useCount" label="使用次数" width="100" align="center" sortable>
          <template #default="{ row }">{{ row.useCount.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="entities" label="实体数" width="80" align="center" />
        <el-table-column prop="relations" label="关系数" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <span class="status-dot" :class="row.status === '启用' ? 'success' : 'info'"></span>
            {{ row.status }}
          </template>
        </el-table-column>
        <el-table-column prop="creator" label="创建人" width="80" />
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="useTemplate(row)">使用模板</el-button>
            <el-button link type="primary" size="small" @click="viewDetail(row)">查看详情</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
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

    <!-- 模板详情弹窗 -->
    <el-dialog v-model="detailVisible" title="模板详情" width="680px" destroy-on-close>
      <template v-if="currentTemplate">
        <div class="tpl-detail">
          <!-- 头部信息 -->
          <div class="tpl-detail-header">
            <div class="tdh-icon" :class="'type-' + typeClass(currentTemplate.type)">
              <el-icon :size="28"><component :is="typeIcon(currentTemplate.type)" /></el-icon>
            </div>
            <div class="tdh-info">
              <h3 class="tdh-title">{{ currentTemplate.name }}</h3>
              <div class="tdh-meta">
                <el-tag size="small" effect="plain" :type="typeTagType(currentTemplate.type)">{{ currentTemplate.type }}</el-tag>
                <span class="tdh-scene">{{ currentTemplate.scene }}</span>
              </div>
            </div>
            <div class="tdh-stats">
              <div class="tdh-stat">
                <span class="tdh-num">{{ currentTemplate.useCount }}</span>
                <span class="tdh-label">使用次数</span>
              </div>
            </div>
          </div>

          <!-- Tabs -->
          <el-tabs v-model="detailTab" type="border-card">
            <!-- 模板说明 -->
            <el-tab-pane label="模板说明" name="desc">
              <div class="tpl-desc-content">
                <p>{{ currentTemplate.description }}</p>
                <h4 class="tpl-sub-title">核心特点</h4>
                <ul class="tpl-feature-list">
                  <li v-for="(f, idx) in currentTemplate.features" :key="idx">
                    <el-icon class="feature-icon"><Select /></el-icon>
                    {{ f }}
                  </li>
                </ul>
                <h4 class="tpl-sub-title">适用行业</h4>
                <div class="tpl-tags">
                  <el-tag v-for="ind in currentTemplate.industries" :key="ind" size="small" effect="plain">{{ ind }}</el-tag>
                </div>
              </div>
            </el-tab-pane>

            <!-- 配置参数 -->
            <el-tab-pane label="配置参数" name="params">
              <el-table :data="currentTemplate.params" border stripe size="small">
                <el-table-column prop="name" label="参数名称" min-width="140" class-name="dp-mono" />
                <el-table-column prop="label" label="显示名称" min-width="120" />
                <el-table-column prop="type" label="参数类型" width="100">
                  <template #default="{ row }">
                    <el-tag size="small" effect="plain">{{ row.type }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="defaultValue" label="默认值" width="120" show-overflow-tooltip />
                <el-table-column prop="required" label="必填" width="70" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.required" size="small" type="danger">是</el-tag>
                    <span v-else class="dp-desc">否</span>
                  </template>
                </el-table-column>
                <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
              </el-table>
            </el-tab-pane>

            <!-- 使用示例 -->
            <el-tab-pane label="使用示例" name="example">
              <div class="tpl-example">
                <div class="example-header">
                  <span class="example-title">示例：快速创建电商交易域模型</span>
                  <el-button size="small" :icon="Files" @click="copyExample">复制代码</el-button>
                </div>
                <div class="example-code">
<pre>// 1. 选择模板
const template = getTemplate('ecommerce-trade');

// 2. 配置参数
const config = {
  domain: '交易域',
  includeRefund: true,
  includePayment: true,
  timePartition: 'dt'
};

// 3. 生成模型
const model = template.generate(config);

// 4. 输出结果
// - 实体：订单、订单明细、支付、退款、客户、商品
// - 关系：1:N 关联关系完整
// - 字段：86个标准字段，含主键外键
</pre>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="useTemplate(currentTemplate)">使用此模板</el-button>
      </template>
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="560px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="模板名称" required>
          <el-input v-model="editForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板类型" required>
          <el-select v-model="editForm.type" style="width: 100%">
            <el-option label="行业模板" value="行业模板" />
            <el-option label="主题域模板" value="主题域模板" />
            <el-option label="通用模板" value="通用模板" />
            <el-option label="自定义" value="自定义" />
          </el-select>
        </el-form-item>
        <el-form-item label="适用场景">
          <el-input v-model="editForm.scene" placeholder="请输入适用场景描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editForm.status">
            <el-radio value="启用">启用</el-radio>
            <el-radio value="停用">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="创建人">
          <el-input v-model="editForm.creator" />
        </el-form-item>
        <el-form-item label="模板描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入模板描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { Files, Plus, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const detailVisible = ref(false)
const editVisible = ref(false)
const isEdit = ref(false)
const currentTemplate = ref(null)
const detailTab = ref('desc')

const query = reactive({
  keyword: '',
  type: '',
  status: '',
  page: 1,
  size: 10
})

const templates = ref([
  {
    id: 1, name: '电商交易域模型模板', type: '行业模板', scene: '电商行业交易域建模',
    useCount: 1256, entities: 12, relations: 18, status: '启用', creator: '平台官方',
    updatedAt: '2024-09-20 14:30', description: '电商行业标准交易域模型，涵盖订单、支付、退款、购物车等核心实体',
    features: [
      '星型模型设计，以订单明细为事实表',
      '内置12个标准实体，18种关联关系',
      '支持日增量/全量两种分区策略',
      '预定义86个标准字段，含完整主键外键',
      '兼容Hive/ClickHouse多种引擎'
    ],
    industries: ['电商', '零售', '跨境电商'],
    params: [
      { name: 'domainName', label: '主题域名称', type: 'string', defaultValue: '交易域', required: true, description: '生成模型的主题域名称' },
      { name: 'includeRefund', label: '包含退款', type: 'boolean', defaultValue: 'true', required: false, description: '是否包含退款相关实体' },
      { name: 'includePayment', label: '包含支付', type: 'boolean', defaultValue: 'true', required: false, description: '是否包含支付相关实体' },
      { name: 'timePartition', label: '时间分区字段', type: 'string', defaultValue: 'dt', required: true, description: '分区字段名称' },
      { name: 'tablePrefix', label: '表名前缀', type: 'string', defaultValue: 'dwd_', required: false, description: '物理表名前缀' }
    ]
  },
  {
    id: 2, name: '客户360视图模板', type: '主题域模板', scene: '客户画像与客户视图',
    useCount: 892, entities: 8, relations: 10, status: '启用', creator: '平台官方',
    updatedAt: '2024-09-18 16:45', description: '客户360度视图模型，整合客户基本信息、标签、等级、行为等数据',
    features: [
      '雪花模型设计，客户为核心实体',
      '包含客户、标签、等级、行为等8个实体',
      '支持慢变维SCD Type2处理',
      '预定义客户标签体系框架'
    ],
    industries: ['金融', '电商', '运营商'],
    params: [
      { name: 'customerEntity', label: '客户实体名', type: 'string', defaultValue: 'customer', required: true, description: '客户主实体名称' },
      { name: 'includeTags', label: '包含标签', type: 'boolean', defaultValue: 'true', required: false, description: '是否包含客户标签实体' },
      { name: 'includeBehavior', label: '包含行为', type: 'boolean', defaultValue: 'true', required: false, description: '是否包含客户行为实体' }
    ]
  },
  {
    id: 3, name: '商品类目模型模板', type: '主题域模板', scene: '商品类目与SKU管理',
    useCount: 658, entities: 6, relations: 7, status: '启用', creator: '平台官方',
    updatedAt: '2024-09-15 09:20', description: '商品类目模型，支持多级类目、SKU、价格等实体',
    features: [
      '支持无限级类目树',
      '包含SKU、SPU、类目、品牌等实体',
      '支持价格快照管理',
      '预定义属性值体系'
    ],
    industries: ['电商', '零售', '快消'],
    params: [
      { name: 'maxLevel', label: '类目最大层级', type: 'number', defaultValue: '3', required: true, description: '类目树的最大层级数' },
      { name: 'includeBrand', label: '包含品牌', type: 'boolean', defaultValue: 'true', required: false, description: '是否包含品牌实体' }
    ]
  },
  {
    id: 4, name: '营销活动分析模板', type: '行业模板', scene: '营销活动效果分析',
    useCount: 524, entities: 7, relations: 8, status: '启用', creator: '李娜',
    updatedAt: '2024-09-22 11:30', description: '营销活动效果分析模型，支持活动、优惠券、投放等实体',
    features: [
      '活动全链路分析模型',
      '包含活动、优惠券、投放、转化等实体',
      '支持多维度效果归因',
      'ROI分析指标预定义'
    ],
    industries: ['电商', '教育', '金融'],
    params: [
      { name: 'includeCoupon', label: '包含优惠券', type: 'boolean', defaultValue: 'true', required: false, description: '是否包含优惠券实体' },
      { name: 'includeChannel', label: '包含渠道', type: 'boolean', defaultValue: 'true', required: false, description: '是否包含投放渠道实体' }
    ]
  },
  {
    id: 5, name: '供应链库存模板', type: '行业模板', scene: '供应链库存管理',
    useCount: 436, entities: 9, relations: 11, status: '启用', creator: '周明',
    updatedAt: '2024-09-24 09:20', description: '供应链库存模型，涵盖库存、入库、出库、供应商等实体',
    features: [
      '库存快照+流水双模型',
      '包含库存、入库、出库、供应商等9个实体',
      '支持批次管理',
      '库存预警指标预定义'
    ],
    industries: ['零售', '制造', '物流'],
    params: [
      { name: 'includeBatch', label: '批次管理', type: 'boolean', defaultValue: 'false', required: false, description: '是否启用批次管理' },
      { name: 'snapshotFrequency', label: '快照频率', type: 'string', defaultValue: 'daily', required: true, description: '库存快照生成频率' }
    ]
  },
  {
    id: 6, name: '财务核算模板', type: '行业模板', scene: '财务核算与分析',
    useCount: 312, entities: 5, relations: 4, status: '启用', creator: '吴丽',
    updatedAt: '2024-09-12 08:45', description: '财务核算模型，涵盖发票、收款、账期等实体',
    features: [
      '财务三表模型设计',
      '包含发票、收款、账期等实体',
      '支持多账套管理',
      '高敏感数据默认脱敏'
    ],
    industries: ['金融', '企业服务', '电商'],
    params: [
      { name: 'includeInvoice', label: '包含发票', type: 'boolean', defaultValue: 'true', required: false, description: '是否包含发票实体' },
      { name: 'multiLedger', label: '多账套', type: 'boolean', defaultValue: 'false', required: false, description: '是否支持多账套' }
    ]
  },
  {
    id: 7, name: '星型模型通用模板', type: '通用模板', scene: '通用星型模型设计',
    useCount: 1568, entities: 1, relations: 0, status: '启用', creator: '平台官方',
    updatedAt: '2024-09-10 10:00', description: '通用星型模型模板，包含一个事实表框架，可自定义维度',
    features: [
      '标准星型模型框架',
      '可自定义事实表和维度表',
      '内置标准字段模板',
      '支持多种分区策略'
    ],
    industries: ['通用'],
    params: [
      { name: 'factTable', label: '事实表名', type: 'string', defaultValue: 'fact_table', required: true, description: '事实表名称' },
      { name: 'dimensions', label: '维度数量', type: 'number', defaultValue: '3', required: true, description: '初始维度表数量' }
    ]
  },
  {
    id: 8, name: '雪花模型通用模板', type: '通用模板', scene: '通用雪花模型设计',
    useCount: 756, entities: 1, relations: 0, status: '启用', creator: '平台官方',
    updatedAt: '2024-09-08 15:30', description: '通用雪花模型模板，支持多层级维度',
    features: [
      '标准雪花模型框架',
      '支持多层级维度',
      '内置维度规范化模板',
      '适合复杂维度场景'
    ],
    industries: ['通用'],
    params: [
      { name: 'factTable', label: '事实表名', type: 'string', defaultValue: 'fact_table', required: true, description: '事实表名称' },
      { name: 'dimLevels', label: '维度层级', type: 'number', defaultValue: '2', required: true, description: '维度最大层级' }
    ]
  }
])

const filteredTemplates = computed(() => {
  return templates.value.filter(t =>
    (!query.keyword || t.name.includes(query.keyword) || t.scene.includes(query.keyword)) &&
    (!query.type || t.type === query.type) &&
    (!query.status || t.status === query.status)
  )
})

const total = computed(() => filteredTemplates.value.length)

function typeClass(type) {
  return { '行业模板': 'industry', '主题域模板': 'domain', '通用模板': 'general', '自定义': 'custom' }[type] || 'general'
}
function typeIcon(type) {
  return { '行业模板': 'OfficeBuilding', '主题域模板': 'Share', '通用模板': 'Grid', '自定义': 'Setting' }[type] || 'Grid'
}
function typeTagType(type) {
  return { '行业模板': 'primary', '主题域模板': 'success', '通用模板': 'warning', '自定义': 'info' }[type] || 'info'
}

function handleRefresh() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('数据已刷新')
  }, 800)
}

function useTemplate(row) {
  ElMessage.success(`已加载模板「${row.name}」，即将跳转到逻辑模型设计器`)
  detailVisible.value = false
}

function viewDetail(row) {
  currentTemplate.value = row
  detailTab.value = 'desc'
  detailVisible.value = true
}

function copyExample() {
  ElMessage.success('示例代码已复制到剪贴板')
}

// 新建/编辑
const editForm = reactive({})

function openCreate() {
  isEdit.value = false
  Object.assign(editForm, { name: '', type: '行业模板', scene: '', status: '启用', creator: '当前用户', description: '' })
  editVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(editForm, { ...row })
  editVisible.value = true
}

function saveTemplate() {
  if (!editForm.name) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (isEdit.value) {
    const idx = templates.value.findIndex(t => t.id === editForm.id)
    if (idx > -1) {
      templates.value[idx] = { ...templates.value[idx], ...editForm, updatedAt: '刚刚' }
    }
    ElMessage.success('模板已更新')
  } else {
    templates.value.unshift({
      id: Date.now(),
      ...editForm,
      useCount: 0,
      entities: 0,
      relations: 0,
      features: [],
      industries: [],
      params: [],
      updatedAt: '刚刚'
    })
    ElMessage.success('模板创建成功')
  }
  editVisible.value = false
}
</script>

<style scoped>
.tpl-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tpl-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 16px;
}
.tpl-icon.type-industry { background: var(--dp-primary-light); color: var(--dp-primary); }
.tpl-icon.type-domain { background: var(--dp-success-light); color: var(--dp-success); }
.tpl-icon.type-general { background: var(--dp-warning-light); color: var(--dp-warning); }
.tpl-icon.type-custom { background: #f2f3f5; color: var(--dp-text-3); }
.tpl-title {
  font-weight: 500;
  color: var(--dp-text-1);
  font-size: 13px;
}
.tpl-desc {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 2px;
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
.status-dot.info { background: var(--dp-text-4); }

/* 详情弹窗 */
.tpl-detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}
.tdh-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.tdh-icon.type-industry { background: var(--dp-primary-light); color: var(--dp-primary); }
.tdh-icon.type-domain { background: var(--dp-success-light); color: var(--dp-success); }
.tdh-icon.type-general { background: var(--dp-warning-light); color: var(--dp-warning); }
.tdh-icon.type-custom { background: #f2f3f5; color: var(--dp-text-3); }
.tdh-info {
  flex: 1;
  min-width: 0;
}
.tdh-title {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 600;
  color: var(--dp-text-1);
}
.tdh-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}
.tdh-scene {
  font-size: 12px;
  color: var(--dp-text-3);
}
.tdh-stats {
  text-align: center;
  padding: 0 20px;
  border-left: 1px solid var(--dp-border-light);
}
.tdh-num {
  display: block;
  font-size: 24px;
  font-weight: 700;
  color: var(--dp-primary);
  font-family: 'DIN Alternate', sans-serif;
}
.tdh-label {
  font-size: 12px;
  color: var(--dp-text-3);
}

/* 模板说明 */
.tpl-desc-content {
  font-size: 13px;
  color: var(--dp-text-2);
  line-height: 1.8;
}
.tpl-sub-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin: 16px 0 10px;
}
.tpl-feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.tpl-feature-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 4px 0;
}
.feature-icon {
  color: var(--dp-success);
  flex-shrink: 0;
  margin-top: 4px;
}
.tpl-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

/* 使用示例 */
.tpl-example {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.example-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.example-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-1);
}
.example-code {
  background: #1e1e2e;
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;
}
.example-code pre {
  margin: 0;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  line-height: 1.8;
  color: #cdd6f4;
}
</style>
