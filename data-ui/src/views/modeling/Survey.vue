<template>
  <div class="dp-page survey-page">
    <PageHeader title="模型探查" desc="浏览和探查数据模型结构，支持按主题域、模型层级展开查看实体与属性详情">
      <el-input v-model="searchKeyword" placeholder="搜索模型/实体" clearable style="width: 240px" :prefix-icon="Search" />
      <el-button type="primary" :icon="Plus">新建探查</el-button>
    </PageHeader>

    <div class="survey-container">
      <!-- 左侧模型树 -->
      <div class="survey-tree-panel dp-card dp-fade-up">
        <div class="tree-header">
          <span class="dp-card-title">模型目录</span>
          <el-tooltip content="展开全部" placement="top">
            <el-button link :icon="Expand" size="small" @click="expandAll" />
          </el-tooltip>
        </div>
        <el-tree
          ref="treeRef"
          :data="modelTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          :default-expanded-keys="defaultExpanded"
          :highlight-current="true"
          :filter-node-method="filterNode"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <span class="tree-node">
              <el-icon v-if="data.type === 'domain'" class="tree-icon domain"><FolderOpened /></el-icon>
              <el-icon v-else-if="data.type === 'model'" class="tree-icon model"><Share /></el-icon>
              <el-icon v-else class="tree-icon entity"><Grid /></el-icon>
              <span class="tree-label">{{ data.name }}</span>
              <el-tag v-if="data.type === 'model'" size="small" effect="plain" :type="modelTagType(data.status)" class="tree-tag">{{ data.status }}</el-tag>
              <span v-if="data.type === 'domain'" class="tree-count">{{ data.count }}</span>
            </span>
          </template>
        </el-tree>
      </div>

      <!-- 右侧详情区 -->
      <div class="survey-detail-panel">
        <template v-if="currentModel">
          <!-- 模型基本信息 -->
          <div class="dp-card dp-fade-up">
            <div class="model-info-header">
              <div class="model-info-icon">
                <el-icon :size="28"><Share /></el-icon>
              </div>
              <div class="model-info-main">
                <h3 class="model-name">{{ currentModel.name }}</h3>
                <div class="model-meta">
                  <el-tag size="small" effect="plain" :type="modelTagType(currentModel.status)">{{ currentModel.status }}</el-tag>
                  <span class="meta-item">主题域：{{ currentModel.domain }}</span>
                  <span class="meta-item">版本：{{ currentModel.version }}</span>
                  <span class="meta-item">负责人：{{ currentModel.owner }}</span>
                  <span class="meta-item">更新时间：{{ currentModel.updatedAt }}</span>
                </div>
              </div>
              <div class="model-info-stats">
                <div class="stat-item">
                  <span class="stat-num">{{ currentModel.entities }}</span>
                  <span class="stat-label">实体</span>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                  <span class="stat-num">{{ currentModel.relations }}</span>
                  <span class="stat-label">关系</span>
                </div>
                <div class="stat-divider"></div>
                <div class="stat-item">
                  <span class="stat-num">{{ currentModel.fields }}</span>
                  <span class="stat-label">字段</span>
                </div>
              </div>
            </div>
            <div class="model-desc">
              <span class="desc-label">模型描述：</span>
              {{ currentModel.description }}
            </div>
          </div>

          <!-- 实体关系图 + 实体列表 -->
          <div class="dp-grid-2" style="align-items: stretch">
            <div class="dp-card dp-fade-up">
              <div class="dp-card-header">
                <div class="dp-card-title">实体关系图</div>
                <el-radio-group v-model="graphLayout" size="small">
                  <el-radio-button value="force">力导向</el-radio-button>
                  <el-radio-button value="circular">环形</el-radio-button>
                </el-radio-group>
              </div>
              <EChart :option="relationOption" height="360px" />
            </div>

            <div class="dp-card dp-fade-up">
              <div class="dp-card-header">
                <div class="dp-card-title">实体列表</div>
                <span class="dp-card-extra">共 {{ entityList.length }} 个实体</span>
              </div>
              <el-table :data="entityList" border stripe size="small" highlight-current-row @row-click="handleEntityClick">
                <el-table-column prop="name" label="实体名称" min-width="140">
                  <template #default="{ row }">
                    <span class="entity-name">
                      <el-icon class="entity-icon"><Grid /></el-icon>
                      {{ row.name }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="fieldCount" label="属性数" width="80" align="center" sortable />
                <el-table-column prop="description" label="描述" min-width="120" show-overflow-tooltip />
                <el-table-column prop="status" label="状态" width="80">
                  <template #default="{ row }">
                    <span class="status-dot" :class="row.status === '已发布' ? 'success' : 'warning'"></span>
                    {{ row.status }}
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <!-- 属性详情 -->
          <div class="dp-card dp-fade-up" v-if="selectedEntity">
            <div class="dp-card-header">
              <div class="dp-card-title">
                属性详情
                <span class="dp-card-tag">{{ selectedEntity.name }}</span>
              </div>
              <span class="dp-card-extra">共 {{ selectedEntity.fields.length }} 个属性</span>
            </div>
            <el-table :data="selectedEntity.fields" border stripe size="small">
              <el-table-column prop="name" label="属性名称" min-width="140" class-name="dp-mono" />
              <el-table-column prop="code" label="属性编码" min-width="140" class-name="dp-mono" />
              <el-table-column prop="dataType" label="数据类型" width="120" />
              <el-table-column prop="length" label="长度" width="80" align="center" />
              <el-table-column label="主键" width="70" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.isPK" type="warning" size="small">PK</el-tag>
                  <span v-else class="dp-desc">-</span>
                </template>
              </el-table-column>
              <el-table-column label="可空" width="70" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.nullable" type="info" size="small">是</el-tag>
                  <span v-else class="dp-desc">否</span>
                </template>
              </el-table-column>
              <el-table-column prop="defaultValue" label="默认值" width="100" show-overflow-tooltip />
              <el-table-column prop="description" label="注释" min-width="160" show-overflow-tooltip />
            </el-table>
          </div>
        </template>

        <el-empty v-else description="请从左侧选择一个模型查看详情" :image-size="100" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import EChart from '@/components/EChart.vue'
import { Expand, Plus, Search } from '@element-plus/icons-vue'

const treeRef = ref(null)
const searchKeyword = ref('')
const graphLayout = ref('force')

const defaultExpanded = ref(['domain-1', 'domain-2'])

const modelTree = ref([
  {
    id: 'domain-1', name: '交易域', type: 'domain', count: 8,
    children: [
      {
        id: 'model-1', name: '交易主题域模型-V3', type: 'model', status: '已发布',
        children: [
          { id: 'entity-1', name: '订单实体', type: 'entity' },
          { id: 'entity-2', name: '订单明细实体', type: 'entity' },
          { id: 'entity-3', name: '支付实体', type: 'entity' },
          { id: 'entity-4', name: '退款实体', type: 'entity' }
        ]
      },
      {
        id: 'model-2', name: '交易宽表模型-V2', type: 'model', status: '设计中',
        children: [
          { id: 'entity-5', name: '交易宽表', type: 'entity' },
          { id: 'entity-6', name: '渠道维度', type: 'entity' }
        ]
      }
    ]
  },
  {
    id: 'domain-2', name: '客户域', type: 'domain', count: 6,
    children: [
      {
        id: 'model-3', name: '客户主题域模型-V2', type: 'model', status: '已发布',
        children: [
          { id: 'entity-7', name: '客户实体', type: 'entity' },
          { id: 'entity-8', name: '客户标签实体', type: 'entity' },
          { id: 'entity-9', name: '客户等级实体', type: 'entity' }
        ]
      }
    ]
  },
  {
    id: 'domain-3', name: '商品域', type: 'domain', count: 5,
    children: [
      {
        id: 'model-4', name: '商品主题域模型-V1', type: 'model', status: '已发布',
        children: [
          { id: 'entity-10', name: '商品实体', type: 'entity' },
          { id: 'entity-11', name: '类目实体', type: 'entity' },
          { id: 'entity-12', name: 'SKU实体', type: 'entity' }
        ]
      }
    ]
  },
  {
    id: 'domain-4', name: '营销域', type: 'domain', count: 4,
    children: [
      {
        id: 'model-5', name: '营销主题域模型-V1', type: 'model', status: '设计中',
        children: [
          { id: 'entity-13', name: '活动实体', type: 'entity' },
          { id: 'entity-14', name: '优惠券实体', type: 'entity' }
        ]
      }
    ]
  },
  {
    id: 'domain-5', name: '供应链域', type: 'domain', count: 3,
    children: [
      {
        id: 'model-6', name: '供应链主题域模型-V1', type: 'model', status: '已归档',
        children: [
          { id: 'entity-15', name: '库存实体', type: 'entity' },
          { id: 'entity-16', name: '入库实体', type: 'entity' }
        ]
      }
    ]
  },
  {
    id: 'domain-6', name: '财务域', type: 'domain', count: 2,
    children: [
      {
        id: 'model-7', name: '财务主题域模型-V1', type: 'model', status: '已发布',
        children: [
          { id: 'entity-17', name: '发票实体', type: 'entity' },
          { id: 'entity-18', name: '收款实体', type: 'entity' }
        ]
      }
    ]
  }
])

const modelDetails = ref({
  'model-1': {
    id: 'model-1', name: '交易主题域模型-V3', domain: '交易域', version: 'V3.0',
    status: '已发布', owner: '李娜', updatedAt: '2024-09-20 14:30',
    entities: 6, relations: 8, fields: 86,
    description: '交易主题域模型涵盖订单、支付、退款等核心交易实体，支撑交易全链路数据分析。采用星型模型设计，以订单明细为事实表，关联客户、商品、渠道等维度表。'
  },
  'model-2': {
    id: 'model-2', name: '交易宽表模型-V2', domain: '交易域', version: 'V2.1',
    status: '设计中', owner: '王强', updatedAt: '2024-09-25 10:15',
    entities: 3, relations: 2, fields: 52,
    description: '交易宽表模型面向报表和分析场景，将订单、支付、客户等数据整合为宽表，提升查询性能。'
  },
  'model-3': {
    id: 'model-3', name: '客户主题域模型-V2', domain: '客户域', version: 'V2.0',
    status: '已发布', owner: '张伟', updatedAt: '2024-09-18 16:45',
    entities: 5, relations: 6, fields: 72,
    description: '客户主题域模型以客户为中心，整合客户基本信息、标签、等级、行为等数据，构建客户360度视图。'
  },
  'model-4': {
    id: 'model-4', name: '商品主题域模型-V1', domain: '商品域', version: 'V1.2',
    status: '已发布', owner: '赵敏', updatedAt: '2024-09-15 09:20',
    entities: 4, relations: 5, fields: 64,
    description: '商品主题域模型涵盖商品、类目、SKU、价格等实体，支撑商品全生命周期管理。'
  },
  'model-5': {
    id: 'model-5', name: '营销主题域模型-V1', domain: '营销域', version: 'V1.0',
    status: '设计中', owner: '孙浩', updatedAt: '2024-09-22 11:30',
    entities: 4, relations: 3, fields: 48,
    description: '营销主题域模型涵盖活动、优惠券、投放等营销相关实体，支撑营销效果分析。'
  },
  'model-6': {
    id: 'model-6', name: '供应链主题域模型-V1', domain: '供应链域', version: 'V1.0',
    status: '已归档', owner: '周明', updatedAt: '2024-08-10 15:00',
    entities: 5, relations: 4, fields: 56,
    description: '供应链主题域模型涵盖库存、入库、出库、供应商等实体，支撑供应链数据分析。'
  },
  'model-7': {
    id: 'model-7', name: '财务主题域模型-V1', domain: '财务域', version: 'V1.0',
    status: '已发布', owner: '吴丽', updatedAt: '2024-09-12 08:45',
    entities: 4, relations: 3, fields: 42,
    description: '财务主题域模型涵盖发票、收款、账期等财务实体，支撑财务核算与分析。'
  }
})

const entityData = ref({
  'model-1': [
    {
      id: 'entity-1', name: '订单实体', code: 'order_info', fieldCount: 18, status: '已发布', description: '订单主表，存储订单头信息',
      fields: [
        { name: '订单ID', code: 'order_id', dataType: 'BIGINT', length: '20', isPK: true, nullable: false, defaultValue: '', description: '订单唯一标识' },
        { name: '订单编号', code: 'order_no', dataType: 'VARCHAR', length: '32', isPK: false, nullable: false, defaultValue: '', description: '订单业务编号' },
        { name: '客户ID', code: 'customer_id', dataType: 'BIGINT', length: '20', isPK: false, nullable: false, defaultValue: '', description: '关联客户ID' },
        { name: '订单金额', code: 'order_amount', dataType: 'DECIMAL', length: '16,2', isPK: false, nullable: false, defaultValue: '0.00', description: '订单总金额' },
        { name: '实付金额', code: 'pay_amount', dataType: 'DECIMAL', length: '16,2', isPK: false, nullable: true, defaultValue: '0.00', description: '实际支付金额' },
        { name: '订单状态', code: 'order_status', dataType: 'TINYINT', length: '1', isPK: false, nullable: false, defaultValue: '0', description: '0待支付1已支付2已发货3已完成4已取消' },
        { name: '下单时间', code: 'create_time', dataType: 'DATETIME', length: '', isPK: false, nullable: false, defaultValue: '', description: '订单创建时间' },
        { name: '支付时间', code: 'pay_time', dataType: 'DATETIME', length: '', isPK: false, nullable: true, defaultValue: '', description: '支付完成时间' },
        { name: '渠道编码', code: 'channel_code', dataType: 'VARCHAR', length: '20', isPK: false, nullable: true, defaultValue: '', description: '订单来源渠道' },
        { name: '备注', code: 'remark', dataType: 'VARCHAR', length: '500', isPK: false, nullable: true, defaultValue: '', description: '订单备注信息' }
      ]
    },
    {
      id: 'entity-2', name: '订单明细实体', code: 'order_detail', fieldCount: 12, status: '已发布', description: '订单明细表，存储订单商品行',
      fields: [
        { name: '明细ID', code: 'detail_id', dataType: 'BIGINT', length: '20', isPK: true, nullable: false, defaultValue: '', description: '明细唯一标识' },
        { name: '订单ID', code: 'order_id', dataType: 'BIGINT', length: '20', isPK: false, nullable: false, defaultValue: '', description: '关联订单ID' },
        { name: '商品ID', code: 'sku_id', dataType: 'BIGINT', length: '20', isPK: false, nullable: false, defaultValue: '', description: '关联SKU ID' },
        { name: '商品名称', code: 'sku_name', dataType: 'VARCHAR', length: '200', isPK: false, nullable: false, defaultValue: '', description: '商品名称快照' },
        { name: '购买数量', code: 'qty', dataType: 'INT', length: '11', isPK: false, nullable: false, defaultValue: '1', description: '购买数量' },
        { name: '单价', code: 'price', dataType: 'DECIMAL', length: '10,2', isPK: false, nullable: false, defaultValue: '0.00', description: '商品单价' },
        { name: '小计金额', code: 'subtotal', dataType: 'DECIMAL', length: '16,2', isPK: false, nullable: false, defaultValue: '0.00', description: '小计金额' }
      ]
    },
    {
      id: 'entity-3', name: '支付实体', code: 'payment_record', fieldCount: 14, status: '已发布', description: '支付记录表',
      fields: [
        { name: '支付ID', code: 'pay_id', dataType: 'BIGINT', length: '20', isPK: true, nullable: false, defaultValue: '', description: '支付唯一标识' },
        { name: '订单ID', code: 'order_id', dataType: 'BIGINT', length: '20', isPK: false, nullable: false, defaultValue: '', description: '关联订单ID' },
        { name: '支付流水号', code: 'pay_no', dataType: 'VARCHAR', length: '64', isPK: false, nullable: false, defaultValue: '', description: '第三方支付流水号' },
        { name: '支付方式', code: 'pay_type', dataType: 'VARCHAR', length: '20', isPK: false, nullable: false, defaultValue: '', description: '支付方式' },
        { name: '支付金额', code: 'pay_amount', dataType: 'DECIMAL', length: '16,2', isPK: false, nullable: false, defaultValue: '0.00', description: '支付金额' },
        { name: '支付状态', code: 'pay_status', dataType: 'TINYINT', length: '1', isPK: false, nullable: false, defaultValue: '0', description: '0待支付1成功2失败' }
      ]
    },
    {
      id: 'entity-4', name: '退款实体', code: 'refund_order', fieldCount: 10, status: '已发布', description: '退款单表',
      fields: [
        { name: '退款ID', code: 'refund_id', dataType: 'BIGINT', length: '20', isPK: true, nullable: false, defaultValue: '', description: '退款唯一标识' },
        { name: '订单ID', code: 'order_id', dataType: 'BIGINT', length: '20', isPK: false, nullable: false, defaultValue: '', description: '关联订单ID' },
        { name: '退款金额', code: 'refund_amount', dataType: 'DECIMAL', length: '16,2', isPK: false, nullable: false, defaultValue: '0.00', description: '退款金额' },
        { name: '退款原因', code: 'reason', dataType: 'VARCHAR', length: '200', isPK: false, nullable: true, defaultValue: '', description: '退款原因' },
        { name: '退款状态', code: 'refund_status', dataType: 'TINYINT', length: '1', isPK: false, nullable: false, defaultValue: '0', description: '0申请中1已退款2已拒绝' }
      ]
    },
    {
      id: 'entity-5', name: '客户实体', code: 'customer_info', fieldCount: 16, status: '已发布', description: '客户基本信息表',
      fields: [
        { name: '客户ID', code: 'customer_id', dataType: 'BIGINT', length: '20', isPK: true, nullable: false, defaultValue: '', description: '客户唯一标识' },
        { name: '客户姓名', code: 'customer_name', dataType: 'VARCHAR', length: '64', isPK: false, nullable: false, defaultValue: '', description: '客户姓名' },
        { name: '手机号', code: 'phone', dataType: 'VARCHAR', length: '11', isPK: false, nullable: true, defaultValue: '', description: '手机号码' },
        { name: '客户等级', code: 'customer_level', dataType: 'VARCHAR', length: '10', isPK: false, nullable: true, defaultValue: '普通', description: '客户等级' }
      ]
    },
    {
      id: 'entity-6', name: '商品实体', code: 'product_sku', fieldCount: 16, status: '已发布', description: '商品SKU表',
      fields: [
        { name: 'SKU ID', code: 'sku_id', dataType: 'BIGINT', length: '20', isPK: true, nullable: false, defaultValue: '', description: 'SKU唯一标识' },
        { name: '商品名称', code: 'sku_name', dataType: 'VARCHAR', length: '200', isPK: false, nullable: false, defaultValue: '', description: '商品名称' },
        { name: '类目ID', code: 'category_id', dataType: 'BIGINT', length: '20', isPK: false, nullable: false, defaultValue: '', description: '所属类目ID' },
        { name: '销售价格', code: 'sale_price', dataType: 'DECIMAL', length: '10,2', isPK: false, nullable: false, defaultValue: '0.00', description: '销售价格' }
      ]
    }
  ]
})

const currentModel = ref(null)
const selectedEntity = ref(null)
const entityList = ref([])

function modelTagType(status) {
  return { 已发布: 'success', 设计中: 'warning', 已归档: 'info' }[status] || 'info'
}

function handleNodeClick(data) {
  if (data.type === 'model') {
    currentModel.value = modelDetails.value[data.id]
    entityList.value = entityData.value[data.id] || []
    selectedEntity.value = null
  } else if (data.type === 'entity') {
    // 找到所属model
    const modelId = findParentModel(data.id)
    if (modelId) {
      currentModel.value = modelDetails.value[modelId]
      entityList.value = entityData.value[modelId] || []
      const entity = entityList.value.find(e => e.id === data.id)
      if (entity) {
        selectedEntity.value = entity
      }
    }
  }
}

function findParentModel(entityId) {
  for (const domain of modelTree.value) {
    for (const model of domain.children || []) {
      if (model.children?.some(e => e.id === entityId)) {
        return model.id
      }
    }
  }
  return null
}

function handleEntityClick(row) {
  selectedEntity.value = row
}

function filterNode(value, data) {
  if (!value) return true
  return data.name.includes(value)
}

function expandAll() {
  const nodes = treeRef.value?.store?.nodesMap || {}
  Object.keys(nodes).forEach(key => {
    if (nodes[key].data.type === 'domain') {
      nodes[key].expanded = true
    }
  })
}

watch(searchKeyword, val => {
  treeRef.value?.filter(val)
})

const relationOption = computed(() => {
  if (!currentModel.value) return {}
  const entities = entityList.value
  const nodes = entities.map((e, i) => ({
    name: e.name,
    symbolSize: 50 + e.fieldCount * 0.5,
    itemStyle: { color: i % 2 === 0 ? '#1664ff' : '#00b42a' },
    label: { show: true, fontSize: 11, color: '#fff' },
    value: e.fieldCount
  }))

  const links = []
  if (entities.length >= 2) {
    links.push({ source: '订单实体', target: '订单明细实体', label: { show: true, formatter: '1:N', fontSize: 10 } })
    links.push({ source: '订单实体', target: '支付实体', label: { show: true, formatter: '1:1', fontSize: 10 } })
    links.push({ source: '订单实体', target: '退款实体', label: { show: true, formatter: '1:N', fontSize: 10 } })
    links.push({ source: '订单明细实体', target: '商品实体', label: { show: true, formatter: 'N:1', fontSize: 10 } })
    links.push({ source: '订单实体', target: '客户实体', label: { show: true, formatter: 'N:1', fontSize: 10 } })
  }

  return {
    tooltip: {
      formatter: '{b}: {c} 个属性'
    },
    series: [
      {
        type: 'graph',
        layout: graphLayout.value,
        roam: true,
        draggable: true,
        data: nodes,
        links: links,
        lineStyle: {
          color: '#c9cdd4',
          width: 1.5,
          curveness: 0.15
        },
        label: {
          position: 'inside',
          fontSize: 11
        },
        edgeLabel: {
          fontSize: 10,
          color: '#4e5969'
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 3 }
        },
        force: {
          repulsion: 300,
          gravity: 0.1,
          edgeLength: 150
        },
        circular: {
          rotateLabel: true
        }
      }
    ]
  }
})
</script>

<style scoped>
.survey-page {
  height: 100%;
}
.survey-container {
  display: flex;
  gap: 16px;
  height: calc(100vh - 160px);
  min-height: 600px;
}
.survey-tree-panel {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
}
.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  width: 100%;
}
.tree-icon {
  flex-shrink: 0;
}
.tree-icon.domain { color: var(--dp-warning); }
.tree-icon.model { color: var(--dp-primary); }
.tree-icon.entity { color: var(--dp-success); }
.tree-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tree-tag {
  margin-left: auto;
}
.tree-count {
  margin-left: auto;
  font-size: 11px;
  color: var(--dp-text-3);
  background: var(--dp-bg-page);
  padding: 1px 6px;
  border-radius: 10px;
}
.survey-detail-panel {
  flex: 1;
  overflow-y: auto;
  min-width: 0;
}
.model-info-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
}
.model-info-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.model-info-main {
  flex: 1;
  min-width: 0;
}
.model-name {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 600;
  color: var(--dp-text-1);
}
.model-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 16px;
  font-size: 12px;
  color: var(--dp-text-3);
}
.meta-item {
  color: var(--dp-text-3);
}
.model-info-stats {
  display: flex;
  align-items: center;
  gap: 0;
  background: var(--dp-bg-page);
  border-radius: 10px;
  padding: 10px 16px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 14px;
}
.stat-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--dp-primary);
  font-family: 'DIN Alternate', sans-serif;
}
.stat-label {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-top: 2px;
}
.stat-divider {
  width: 1px;
  height: 30px;
  background: var(--dp-border);
}
.model-desc {
  font-size: 13px;
  color: var(--dp-text-2);
  line-height: 1.8;
  padding-top: 12px;
  border-top: 1px solid var(--dp-border-light);
}
.desc-label {
  color: var(--dp-text-3);
}
.entity-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}
.entity-icon {
  color: var(--dp-primary);
  font-size: 14px;
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
</style>
