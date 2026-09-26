<template>
  <div class="dp-page logical-page">
    <!-- 列表视图 -->
    <template v-if="!designerVisible">
      <PageHeader title="逻辑模型" desc="业务主题的逻辑实体与关系设计（ER图），与业务概念对齐，支撑后续物理建模">
        <el-button type="primary" :icon="Plus" @click="openCreate">新建模型</el-button>
      </PageHeader>

      <div class="dp-card dp-fade-up">
        <div class="dp-toolbar">
          <el-input v-model="query.keyword" placeholder="搜索模型名称" clearable style="width: 220px" :prefix-icon="Search" />
          <el-select v-model="query.domain" placeholder="主题域" clearable style="width: 140px">
            <el-option v-for="d in domainOptions" :key="d" :label="d" :value="d" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
            <el-option label="已发布" value="已发布" />
            <el-option label="设计中" value="设计中" />
            <el-option label="已归档" value="已归档" />
          </el-select>
          <div class="dp-toolbar-right">
            <el-radio-group v-model="viewMode" size="default">
              <el-radio-button value="list">
                <el-icon><List /></el-icon>
              </el-radio-button>
              <el-radio-button value="card">
                <el-icon><Grid /></el-icon>
              </el-radio-button>
            </el-radio-group>
            <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
          </div>
        </div>

        <!-- 表格视图 -->
        <template v-if="viewMode === 'list'">
          <el-table :data="filteredModels" border stripe v-loading="loading">
            <el-table-column prop="name" label="模型名称" min-width="200">
              <template #default="{ row }">
                <div class="model-name-cell">
                  <div class="model-icon"><el-icon><Share /></el-icon></div>
                  <div>
                    <div class="model-title">{{ row.name }}</div>
                    <div class="model-sub">{{ row.description }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="domain" label="主题域" width="100">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" type="primary">{{ row.domain }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="entities" label="实体数" width="80" sortable align="center" />
            <el-table-column prop="relations" label="关系数" width="80" sortable align="center" />
            <el-table-column prop="version" label="版本" width="80" align="center">
              <template #default="{ row }">
                <span class="dp-mono">{{ row.version }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <span class="status-dot" :class="statusDotClass(row.status)"></span>
                {{ row.status }}
              </template>
            </el-table-column>
            <el-table-column prop="owner" label="负责人" width="80" />
            <el-table-column prop="updatedAt" label="更新时间" width="160" />
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openDesigner(row)">设计</el-button>
                <el-button link type="primary" size="small" @click="handleVersion(row)">版本</el-button>
                <el-button v-if="row.status !== '已发布'" link type="success" size="small" @click="handlePublish(row)">发布</el-button>
                <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </template>

        <!-- 卡片视图 -->
        <template v-else>
          <div class="model-card-grid">
            <div v-for="model in filteredModels" :key="model.id" class="model-card dp-fade-up" @click="openDesigner(model)">
              <div class="model-card-header">
                <div class="model-card-icon" :style="{ background: statusBgClass(model.status), color: statusColorClass(model.status) }">
                  <el-icon :size="20"><Share /></el-icon>
                </div>
                <el-tag size="small" effect="plain" :type="statusTagType(model.status)">{{ model.status }}</el-tag>
              </div>
              <div class="model-card-title">{{ model.name }}</div>
              <div class="model-card-desc">{{ model.description }}</div>
              <div class="model-card-meta">
                <span><el-icon><FolderOpened /></el-icon> {{ model.domain }}</span>
                <span><el-icon><User /></el-icon> {{ model.owner }}</span>
              </div>
              <div class="model-card-stats">
                <div class="mc-stat">
                  <span class="mc-num">{{ model.entities }}</span>
                  <span class="mc-label">实体</span>
                </div>
                <div class="mc-stat">
                  <span class="mc-num">{{ model.relations }}</span>
                  <span class="mc-label">关系</span>
                </div>
                <div class="mc-stat">
                  <span class="mc-num">{{ model.fields }}</span>
                  <span class="mc-label">字段</span>
                </div>
                <div class="mc-stat">
                  <span class="mc-label dp-mono">{{ model.version }}</span>
                  <span class="mc-label">版本</span>
                </div>
              </div>
              <div class="model-card-footer">
                <span class="mc-time">更新于 {{ model.updatedAt }}</span>
                <div class="mc-actions" @click.stop>
                  <el-button link type="primary" size="small" @click.stop="openEdit(model)">编辑</el-button>
                  <el-button link type="danger" size="small" @click.stop="handleDelete(model)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </template>

        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </template>

    <!-- 设计器视图 -->
    <template v-else>
      <div class="designer-container">
        <!-- 顶部工具栏 -->
        <div class="designer-toolbar">
          <div class="dt-left">
            <el-button :icon="Back" @click="closeDesigner">返回</el-button>
            <span class="dt-divider"></span>
            <span class="dt-title">{{ currentDesignModel?.name }}</span>
            <el-tag size="small" effect="plain" :type="statusTagType(currentDesignModel?.status)">{{ currentDesignModel?.status }}</el-tag>
          </div>
          <div class="dt-center">
            <el-button-group>
              <el-button :icon="Finished" @click="selectAll">全选</el-button>
              <el-button :icon="CopyDocument" @click="copyEntity">复制</el-button>
              <el-button :icon="Delete" @click="deleteSelected">删除</el-button>
            </el-button-group>
            <span class="dt-divider"></span>
            <el-button :icon="Rank" @click="autoLayout">自动布局</el-button>
            <el-button :icon="ZoomIn" @click="zoomIn">放大</el-button>
            <el-button :icon="ZoomOut" @click="zoomOut">缩小</el-button>
            <span style="font-size: 12px; color: var(--dp-text-3)">{{ zoomLevel }}%</span>
          </div>
          <div class="dt-right">
            <el-button @click="saveDesign">
              <el-icon><Document /></el-icon>
              保存
            </el-button>
            <el-button type="primary" @click="publishDesign">
              <el-icon><Promotion /></el-icon>
              发布
            </el-button>
          </div>
        </div>

        <div class="designer-body">
          <!-- 左侧实体列表 -->
          <div class="designer-left">
            <div class="dl-header">
              <span class="dl-title">实体列表</span>
              <el-button link type="primary" size="small" :icon="Plus" @click="addEntity">新建</el-button>
            </div>
            <div class="dl-search">
              <el-input v-model="entitySearch" placeholder="搜索实体" size="small" clearable :prefix-icon="Search" />
            </div>
            <div class="dl-entity-list">
              <div
                v-for="entity in filteredEntities"
                :key="entity.id"
                class="dl-entity-item"
                :class="{ active: selectedEntityId === entity.id }"
                @click="selectEntity(entity)"
              >
                <el-icon class="dei-icon"><Grid /></el-icon>
                <span class="dei-name">{{ entity.name }}</span>
                <span class="dei-count">{{ entity.fields.length }}</span>
              </div>
            </div>
          </div>

          <!-- 中间画布 -->
          <div class="designer-canvas" ref="canvasRef">
            <div class="canvas-inner" :style="{ transform: `scale(${zoomLevel / 100})` }">
              <svg class="canvas-edges">
                <path
                  v-for="(rel, idx) in designRelations"
                  :key="'rel-' + idx"
                  :d="getRelationPath(rel)"
                  fill="none"
                  stroke="#a9aeb8"
                  stroke-width="1.5"
                  marker-end="url(#arrowhead)"
                />
                <defs>
                  <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                    <polygon points="0 0, 10 3.5, 0 7" fill="#a9aeb8" />
                  </marker>
                </defs>
              </svg>
              <div
                v-for="entity in designEntities"
                :key="entity.id"
                class="er-entity"
                :class="{ selected: selectedEntityId === entity.id }"
                :style="{ left: entity.x + 'px', top: entity.y + 'px' }"
                @mousedown.stop="startDrag(entity, $event)"
                @click.stop="selectEntity(entity)"
              >
                <div class="er-entity-head">
                  <el-icon><Grid /></el-icon>
                  {{ entity.name }}
                </div>
                <div class="er-entity-body">
                  <div v-for="field in entity.fields" :key="field.name" class="er-field">
                    <span class="er-field-name">{{ field.name }}</span>
                    <span v-if="field.isPK" class="er-field-badge pk">PK</span>
                    <span v-else-if="field.isFK" class="er-field-badge fk">FK</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧属性面板 -->
          <div class="designer-right">
            <div class="dr-header">
              <span class="dr-title">属性面板</span>
            </div>
            <template v-if="selectedEntity">
              <div class="dr-section">
                <div class="dr-section-title">实体信息</div>
                <el-form size="small" label-width="70px">
                  <el-form-item label="实体名称">
                    <el-input v-model="selectedEntity.name" />
                  </el-form-item>
                  <el-form-item label="实体编码">
                    <el-input v-model="selectedEntity.code" />
                  </el-form-item>
                  <el-form-item label="描述">
                    <el-input v-model="selectedEntity.description" type="textarea" :rows="2" />
                  </el-form-item>
                </el-form>
              </div>
              <div class="dr-section">
                <div class="dr-section-title">
                  属性列表
                  <el-button link type="primary" size="small" :icon="Plus" style="margin-left: auto" @click="addField">添加</el-button>
                </div>
                <div class="dr-field-list">
                  <div v-for="(field, idx) in selectedEntity.fields" :key="idx" class="dr-field-item">
                    <div class="dfi-main">
                      <span class="dfi-name">{{ field.name }}</span>
                      <span class="dfi-type">{{ field.dataType }}</span>
                    </div>
                    <div class="dfi-badges">
                      <el-tag v-if="field.isPK" size="small" type="warning">PK</el-tag>
                      <el-tag v-if="field.isFK" size="small" type="primary">FK</el-tag>
                      <el-tag v-if="field.nullable" size="small" effect="plain">可空</el-tag>
                    </div>
                    <el-button link type="danger" size="small" @click="removeField(idx)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
              </div>
            </template>
            <el-empty v-else description="请选择一个实体查看属性" :image-size="80" />
          </div>
        </div>
      </div>
    </template>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="isEdit ? '编辑逻辑模型' : '新建逻辑模型'" width="520px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="模型名称" required>
          <el-input v-model="editForm.name" placeholder="请输入模型名称" />
        </el-form-item>
        <el-form-item label="主题域" required>
          <el-select v-model="editForm.domain" style="width: 100%">
            <el-option v-for="d in domainOptions" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="editForm.version" placeholder="V1.0" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="editForm.owner" />
        </el-form-item>
        <el-form-item label="模型描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入模型描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { Back, CopyDocument, Delete, Finished, Plus, Rank, Refresh, Search, ZoomIn, ZoomOut } from '@element-plus/icons-vue'

const loading = ref(false)
const viewMode = ref('list')
const designerVisible = ref(false)
const currentDesignModel = ref(null)
const canvasRef = ref(null)

const query = reactive({
  keyword: '',
  domain: '',
  status: '',
  page: 1,
  size: 10
})

const domainOptions = ['客户域', '交易域', '商品域', '营销域', '供应链域', '财务域']

const models = ref([
  { id: 1, name: '客户主题域模型-V3', domain: '客户域', entities: 8, relations: 10, fields: 96, version: 'V3.0', status: '已发布', owner: '张伟', updatedAt: '2024-09-20 14:30', description: '客户360度视图模型' },
  { id: 2, name: '交易主题域模型-V3', domain: '交易域', entities: 12, relations: 15, fields: 128, version: 'V3.0', status: '已发布', owner: '李娜', updatedAt: '2024-09-25 10:15', description: '交易全链路核心模型' },
  { id: 3, name: '商品主题域模型-V2', domain: '商品域', entities: 7, relations: 8, fields: 72, version: 'V2.1', status: '已发布', owner: '赵敏', updatedAt: '2024-09-18 16:45', description: '商品类目与SKU模型' },
  { id: 4, name: '营销主题域模型-V1', domain: '营销域', entities: 6, relations: 5, fields: 58, version: 'V1.0', status: '设计中', owner: '孙浩', updatedAt: '2024-09-22 11:30', description: '营销活动与效果分析模型' },
  { id: 5, name: '供应链主题域模型-V2', domain: '供应链域', entities: 9, relations: 11, fields: 88, version: 'V2.0', status: '设计中', owner: '周明', updatedAt: '2024-09-24 09:20', description: '库存与供应链模型' },
  { id: 6, name: '财务主题域模型-V1', domain: '财务域', entities: 5, relations: 4, fields: 48, version: 'V1.2', status: '已发布', owner: '吴丽', updatedAt: '2024-09-15 08:45', description: '财务核算与分析模型' },
  { id: 7, name: '用户行为模型-V1', domain: '客户域', entities: 4, relations: 3, fields: 36, version: 'V1.0', status: '设计中', owner: '张伟', updatedAt: '2024-09-26 15:00', description: '用户行为分析模型' },
  { id: 8, name: '渠道分析模型-V1', domain: '营销域', entities: 3, relations: 2, fields: 28, version: 'V1.0', status: '已归档', owner: '孙浩', updatedAt: '2024-08-10 14:00', description: '渠道效果分析模型' }
])

const filteredModels = computed(() => {
  return models.value.filter(m =>
    (!query.keyword || m.name.includes(query.keyword)) &&
    (!query.domain || m.domain === query.domain) &&
    (!query.status || m.status === query.status)
  )
})

const total = computed(() => filteredModels.value.length)

function statusDotClass(status) {
  return { 已发布: 'success', 设计中: 'warning', 已归档: 'info' }[status] || 'info'
}
function statusTagType(status) {
  return { 已发布: 'success', 设计中: 'warning', 已归档: 'info' }[status] || 'info'
}
function statusBgClass(status) {
  return { 已发布: '#e8ffea', 设计中: '#fff3e8', 已归档: '#f2f3f5' }[status] || '#f2f3f5'
}
function statusColorClass(status) {
  return { 已发布: '#00b42a', 设计中: '#ff7d00', 已归档: '#86909c' }[status] || '#86909c'
}

function handleRefresh() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('数据已刷新')
  }, 800)
}

function handlePageChange() {}

// 新建/编辑
const editVisible = ref(false)
const isEdit = ref(false)
const editForm = reactive({})

function openCreate() {
  isEdit.value = false
  Object.assign(editForm, { name: '', domain: '交易域', version: 'V1.0', owner: '张伟', description: '' })
  editVisible.value = true
}

function openEdit(row) {
  isEdit.value = true
  Object.assign(editForm, { ...row })
  editVisible.value = true
}

function saveEdit() {
  if (!editForm.name) {
    ElMessage.warning('请输入模型名称')
    return
  }
  if (isEdit.value) {
    const idx = models.value.findIndex(m => m.id === editForm.id)
    if (idx > -1) {
      models.value[idx] = { ...models.value[idx], ...editForm, updatedAt: '刚刚' }
    }
    ElMessage.success('模型已更新')
  } else {
    models.value.unshift({
      id: Date.now(),
      ...editForm,
      entities: 0,
      relations: 0,
      fields: 0,
      status: '设计中',
      updatedAt: '刚刚'
    })
    ElMessage.success('模型创建成功')
  }
  editVisible.value = false
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除模型「${row.name}」吗？删除后不可恢复。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(() => {
    models.value = models.value.filter(m => m.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

function handlePublish(row) {
  ElMessageBox.confirm(`确定发布模型「${row.name}」吗？发布后将生成新版本。`, '发布确认', {
    type: 'info',
    confirmButtonText: '确定发布',
    cancelButtonText: '取消'
  }).then(() => {
    row.status = '已发布'
    row.updatedAt = '刚刚'
    ElMessage.success('发布成功')
  }).catch(() => {})
}

function handleVersion(row) {
  ElMessage.info(`打开「${row.name}」的版本管理`)
}

// 设计器
const zoomLevel = ref(100)
const entitySearch = ref('')
const selectedEntityId = ref(null)

const designEntities = ref([])
const designRelations = ref([])

const selectedEntity = computed(() => designEntities.value.find(e => e.id === selectedEntityId.value))

const filteredEntities = computed(() => {
  if (!entitySearch.value) return designEntities.value
  return designEntities.value.filter(e => e.name.includes(entitySearch.value))
})

function openDesigner(row) {
  currentDesignModel.value = row
  // 初始化设计器数据
  designEntities.value = [
    { id: 'e1', name: '客户', code: 'customer', x: 60, y: 80, description: '客户基本信息实体',
      fields: [
        { name: 'customer_id', dataType: 'BIGINT', isPK: true, isFK: false, nullable: false },
        { name: 'customer_name', dataType: 'VARCHAR(64)', isPK: false, isFK: false, nullable: false },
        { name: 'phone', dataType: 'VARCHAR(11)', isPK: false, isFK: false, nullable: true },
        { name: 'level', dataType: 'VARCHAR(10)', isPK: false, isFK: false, nullable: true },
        { name: 'create_time', dataType: 'DATETIME', isPK: false, isFK: false, nullable: false }
      ]
    },
    { id: 'e2', name: '订单', code: 'order_info', x: 360, y: 60, description: '订单主实体',
      fields: [
        { name: 'order_id', dataType: 'BIGINT', isPK: true, isFK: false, nullable: false },
        { name: 'order_no', dataType: 'VARCHAR(32)', isPK: false, isFK: false, nullable: false },
        { name: 'customer_id', dataType: 'BIGINT', isPK: false, isFK: true, nullable: false },
        { name: 'order_amount', dataType: 'DECIMAL(16,2)', isPK: false, isFK: false, nullable: false },
        { name: 'order_status', dataType: 'TINYINT', isPK: false, isFK: false, nullable: false },
        { name: 'create_time', dataType: 'DATETIME', isPK: false, isFK: false, nullable: false }
      ]
    },
    { id: 'e3', name: '订单明细', code: 'order_detail', x: 360, y: 300, description: '订单明细实体',
      fields: [
        { name: 'detail_id', dataType: 'BIGINT', isPK: true, isFK: false, nullable: false },
        { name: 'order_id', dataType: 'BIGINT', isPK: false, isFK: true, nullable: false },
        { name: 'sku_id', dataType: 'BIGINT', isPK: false, isFK: true, nullable: false },
        { name: 'qty', dataType: 'INT', isPK: false, isFK: false, nullable: false },
        { name: 'price', dataType: 'DECIMAL(10,2)', isPK: false, isFK: false, nullable: false }
      ]
    },
    { id: 'e4', name: '商品', code: 'product', x: 680, y: 280, description: '商品实体',
      fields: [
        { name: 'sku_id', dataType: 'BIGINT', isPK: true, isFK: false, nullable: false },
        { name: 'sku_name', dataType: 'VARCHAR(200)', isPK: false, isFK: false, nullable: false },
        { name: 'category_id', dataType: 'BIGINT', isPK: false, isFK: true, nullable: false },
        { name: 'price', dataType: 'DECIMAL(10,2)', isPK: false, isFK: false, nullable: false }
      ]
    },
    { id: 'e5', name: '类目', code: 'category', x: 680, y: 60, description: '商品类目实体',
      fields: [
        { name: 'category_id', dataType: 'BIGINT', isPK: true, isFK: false, nullable: false },
        { name: 'category_name', dataType: 'VARCHAR(100)', isPK: false, isFK: false, nullable: false },
        { name: 'parent_id', dataType: 'BIGINT', isPK: false, isFK: true, nullable: true }
      ]
    }
  ]
  designRelations.value = [
    { from: 'e1', to: 'e2', label: '1:N' },
    { from: 'e2', to: 'e3', label: '1:N' },
    { from: 'e4', to: 'e3', label: '1:N' },
    { from: 'e5', to: 'e4', label: '1:N' }
  ]
  selectedEntityId.value = null
  designerVisible.value = true
}

function closeDesigner() {
  designerVisible.value = false
  currentDesignModel.value = null
}

function selectEntity(entity) {
  selectedEntityId.value = entity.id
}

function addEntity() {
  const id = 'e' + Date.now()
  designEntities.value.push({
    id,
    name: '新实体',
    code: 'new_entity',
    x: 200 + Math.random() * 200,
    y: 200 + Math.random() * 200,
    description: '',
    fields: [
      { name: 'id', dataType: 'BIGINT', isPK: true, isFK: false, nullable: false },
      { name: 'name', dataType: 'VARCHAR(100)', isPK: false, isFK: false, nullable: false }
    ]
  })
  selectedEntityId.value = id
  ElMessage.success('已添加新实体')
}

function addField() {
  if (!selectedEntity.value) return
  selectedEntity.value.fields.push({
    name: 'new_field',
    dataType: 'VARCHAR(100)',
    isPK: false,
    isFK: false,
    nullable: true
  })
}

function removeField(idx) {
  if (!selectedEntity.value) return
  selectedEntity.value.fields.splice(idx, 1)
}

function deleteSelected() {
  if (!selectedEntityId.value) {
    ElMessage.warning('请先选择要删除的实体')
    return
  }
  ElMessageBox.confirm('确定删除选中的实体吗？', '删除确认', { type: 'warning' })
    .then(() => {
      designEntities.value = designEntities.value.filter(e => e.id !== selectedEntityId.value)
      designRelations.value = designRelations.value.filter(
        r => r.from !== selectedEntityId.value && r.to !== selectedEntityId.value
      )
      selectedEntityId.value = null
      ElMessage.success('删除成功')
    }).catch(() => {})
}

function selectAll() {
  ElMessage.info('已全选所有实体')
}

function copyEntity() {
  if (!selectedEntityId.value) {
    ElMessage.warning('请先选择要复制的实体')
    return
  }
  const src = designEntities.value.find(e => e.id === selectedEntityId.value)
  if (src) {
    const newId = 'e' + Date.now()
    designEntities.value.push({
      ...JSON.parse(JSON.stringify(src)),
      id: newId,
      name: src.name + '_副本',
      x: src.x + 30,
      y: src.y + 30
    })
    selectedEntityId.value = newId
    ElMessage.success('已复制实体')
  }
}

function autoLayout() {
  const cols = Math.ceil(Math.sqrt(designEntities.value.length))
  designEntities.value.forEach((e, i) => {
    const row = Math.floor(i / cols)
    const col = i % cols
    e.x = 80 + col * 280
    e.y = 80 + row * 220
  })
  ElMessage.success('已自动布局')
}

function zoomIn() {
  if (zoomLevel.value < 150) zoomLevel.value += 10
}

function zoomOut() {
  if (zoomLevel.value > 50) zoomLevel.value -= 10
}

function saveDesign() {
  ElMessage.success('模型设计已保存')
}

function publishDesign() {
  ElMessageBox.confirm('确定发布当前模型设计吗？', '发布确认', { type: 'info' })
    .then(() => {
      if (currentDesignModel.value) {
        currentDesignModel.value.status = '已发布'
      }
      ElMessage.success('模型发布成功')
    }).catch(() => {})
}

// 拖拽
const ENTITY_W = 180

function entityHeight(e) {
  return 34 + e.fields.length * 24 + 6
}

function startDrag(entity, e) {
  selectedEntityId.value = entity.id
  const startX = e.clientX
  const startY = e.clientY
  const origX = entity.x
  const origY = entity.y

  function onMove(ev) {
    entity.x = origX + ev.clientX - startX
    entity.y = origY + ev.clientY - startY
  }
  function onUp() {
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

function getRelationPath(rel) {
  const from = designEntities.value.find(e => e.id === rel.from)
  const to = designEntities.value.find(e => e.id === rel.to)
  if (!from || !to) return ''
  const x1 = from.x + ENTITY_W
  const y1 = from.y + entityHeight(from) / 2
  const x2 = to.x
  const y2 = to.y + entityHeight(to) / 2
  const mx = (x1 + x2) / 2
  return `M ${x1} ${y1} C ${mx} ${y1}, ${mx} ${y2}, ${x2 - 10} ${y2}`
}
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
  border-radius: 8px;
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.model-title {
  font-weight: 500;
  color: var(--dp-text-1);
  font-size: 13px;
}
.model-sub {
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
.status-dot.warning { background: var(--dp-warning); }
.status-dot.info { background: var(--dp-text-4); }

/* 卡片视图 */
.model-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}
.model-card {
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: var(--dp-radius);
  padding: 16px;
  cursor: pointer;
  transition: all 0.25s;
  display: flex;
  flex-direction: column;
}
.model-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 4px 16px rgba(22, 100, 255, 0.12);
  transform: translateY(-2px);
}
.model-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.model-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.model-card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 4px;
}
.model-card-desc {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.model-card-meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 12px;
}
.model-card-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}
.model-card-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 10px 0;
  border-top: 1px solid var(--dp-border-light);
  border-bottom: 1px solid var(--dp-border-light);
  margin-bottom: 10px;
}
.mc-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.mc-num {
  font-size: 16px;
  font-weight: 700;
  color: var(--dp-primary);
  font-family: 'DIN Alternate', sans-serif;
}
.mc-label {
  font-size: 11px;
  color: var(--dp-text-3);
}
.model-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}
.mc-time {
  font-size: 11px;
  color: var(--dp-text-3);
}
.mc-actions {
  display: flex;
  gap: 4px;
}

/* 设计器 */
.designer-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--dp-bg-page);
  z-index: 100;
  display: flex;
  flex-direction: column;
}
.designer-toolbar {
  height: 52px;
  background: #fff;
  border-bottom: 1px solid var(--dp-border-light);
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 12px;
}
.dt-left, .dt-center, .dt-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.dt-center {
  flex: 1;
  justify-content: center;
}
.dt-divider {
  width: 1px;
  height: 20px;
  background: var(--dp-border);
  margin: 0 4px;
}
.dt-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
}
.designer-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}
.designer-left {
  width: 240px;
  background: #fff;
  border-right: 1px solid var(--dp-border-light);
  display: flex;
  flex-direction: column;
}
.dl-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--dp-border-light);
}
.dl-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--dp-text-1);
}
.dl-search {
  padding: 10px 12px;
}
.dl-entity-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}
.dl-entity-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  font-size: 13px;
}
.dl-entity-item:hover {
  background: var(--dp-bg-page);
}
.dl-entity-item.active {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
}
.dei-icon {
  flex-shrink: 0;
  font-size: 14px;
}
.dei-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dei-count {
  font-size: 11px;
  color: var(--dp-text-3);
  background: var(--dp-bg-page);
  padding: 1px 6px;
  border-radius: 10px;
}
.dl-entity-item.active .dei-count {
  background: #fff;
  color: var(--dp-primary);
}
.designer-canvas {
  flex: 1;
  overflow: auto;
  background: #fafbfc;
  background-image: radial-gradient(circle, #dfe2e8 1px, transparent 1px);
  background-size: 20px 20px;
  position: relative;
}
.canvas-inner {
  position: relative;
  width: 1600px;
  height: 1000px;
  transform-origin: top left;
}
.canvas-edges {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}
.er-entity {
  position: absolute;
  width: 180px;
  background: #fff;
  border: 1.5px solid var(--dp-border);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: move;
  z-index: 2;
  user-select: none;
  overflow: hidden;
}
.er-entity.selected {
  border-color: var(--dp-primary);
  box-shadow: 0 0 0 3px rgba(22, 100, 255, 0.15);
}
.er-entity-head {
  background: linear-gradient(135deg, #1664ff, #4080ff);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.er-entity-body {
  padding: 4px 0;
}
.er-field {
  display: flex;
  align-items: center;
  padding: 4px 12px;
  font-size: 12px;
  font-family: Consolas, monospace;
  color: var(--dp-text-2);
}
.er-field:hover {
  background: #f2f7ff;
}
.er-field-name {
  flex: 1;
}
.er-field-badge {
  font-size: 10px;
  border-radius: 3px;
  padding: 0 4px;
  font-weight: 700;
}
.er-field-badge.pk {
  background: #fff3e8;
  color: #ff7d00;
}
.er-field-badge.fk {
  background: #e8f0ff;
  color: #1664ff;
}
.designer-right {
  width: 280px;
  background: #fff;
  border-left: 1px solid var(--dp-border-light);
  display: flex;
  flex-direction: column;
}
.dr-header {
  padding: 12px 14px;
  border-bottom: 1px solid var(--dp-border-light);
}
.dr-title {
  font-weight: 600;
  font-size: 14px;
  color: var(--dp-text-1);
}
.dr-section {
  padding: 12px 14px;
  border-bottom: 1px solid var(--dp-border-light);
}
.dr-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}
.dr-field-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 300px;
  overflow-y: auto;
}
.dr-field-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: var(--dp-bg-page);
  border-radius: 6px;
}
.dfi-main {
  flex: 1;
  min-width: 0;
}
.dfi-name {
  font-size: 12px;
  font-weight: 500;
  color: var(--dp-text-1);
  font-family: Consolas, monospace;
  display: block;
}
.dfi-type {
  font-size: 11px;
  color: var(--dp-text-3);
  font-family: Consolas, monospace;
}
.dfi-badges {
  display: flex;
  gap: 3px;
}
</style>
