<template>
  <div class="dp-page">
    <PageHeader title="物理模型" desc="逻辑模型落地为物理库表，支持DDL生成、字段映射与数据源同步">
      <el-button :icon="DocumentChecked" @click="batchGenerateVisible = true">批量生成DDL</el-button>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建物理模型</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索模型名称" clearable style="width: 220px" :prefix-icon="Search" />
        <el-select v-model="query.dbType" placeholder="数据源类型" clearable style="width: 140px">
          <el-option v-for="t in dbTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="已部署" value="已部署" />
          <el-option label="未部署" value="未部署" />
          <el-option label="同步中" value="同步中" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        </div>
      </div>

      <el-table :data="filteredModels" border stripe v-loading="loading">
        <el-table-column prop="name" label="模型名称" min-width="180" class-name="dp-mono">
          <template #default="{ row }">
            <div class="pm-name-cell">
              <el-icon class="pm-icon"><Coin /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sourceModel" label="关联逻辑模型" min-width="160" />
        <el-table-column prop="dbType" label="数据源类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" :type="dbTypeTag(row.dbType)">{{ row.dbType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="tables" label="表数量" width="80" align="center" sortable />
        <el-table-column prop="fields" label="字段总数" width="90" align="center" sortable />
        <el-table-column prop="version" label="版本" width="80" align="center">
          <template #default="{ row }"><span class="dp-mono">{{ row.version }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="status-dot" :class="statusDotClass(row.status)"></span>
            {{ row.status }}
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openTableDetail(row)">查看表结构</el-button>
            <el-button link type="primary" size="small" @click="syncDataSource(row)">同步结构</el-button>
            <el-button link type="primary" size="small" @click="openVersion(row)">版本管理</el-button>
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

    <!-- 表结构详情抽屉 -->
    <el-drawer v-model="tableDetailVisible" :title="'表结构详情 · ' + currentModel?.name" size="720px" destroy-on-close>
      <template v-if="currentModel">
        <!-- 模型信息 -->
        <div class="pm-model-info">
          <div class="pmi-item">
            <span class="pmi-label">关联逻辑模型</span>
            <span class="pmi-value">{{ currentModel.sourceModel }}</span>
          </div>
          <div class="pmi-item">
            <span class="pmi-label">数据源类型</span>
            <el-tag size="small" effect="plain" :type="dbTypeTag(currentModel.dbType)">{{ currentModel.dbType }}</el-tag>
          </div>
          <div class="pmi-item">
            <span class="pmi-label">表数量</span>
            <span class="pmi-value">{{ tableList.length }} 张</span>
          </div>
          <div class="pmi-item">
            <span class="pmi-label">字段总数</span>
            <span class="pmi-value">{{ totalFields }} 个</span>
          </div>
        </div>

        <!-- 表列表 Tabs -->
        <el-tabs v-model="activeTable" type="border-card">
          <el-tab-pane v-for="table in tableList" :key="table.name" :label="table.name" :name="table.name">
            <div class="pm-table-meta">
              <span class="pm-table-desc">{{ table.comment }}</span>
              <span class="pm-table-storage">存储引擎：{{ table.engine }} | 字符集：{{ table.charset }}</span>
            </div>

            <el-table :data="table.fields" border stripe size="small">
              <el-table-column prop="name" label="字段名" min-width="140" class-name="dp-mono">
                <template #default="{ row }">
                  <span class="field-name">
                    <el-icon v-if="row.isPK" class="pk-icon"><Key /></el-icon>
                    {{ row.name }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="type" label="类型" width="130" class-name="dp-mono" />
              <el-table-column prop="length" label="长度" width="80" align="center" />
              <el-table-column label="主键" width="70" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.isPK" size="small" type="warning">PK</el-tag>
                  <span v-else class="dp-desc">-</span>
                </template>
              </el-table-column>
              <el-table-column label="可空" width="70" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.nullable" size="small" effect="plain" type="info">是</el-tag>
                  <span v-else class="dp-desc">否</span>
                </template>
              </el-table-column>
              <el-table-column prop="defaultValue" label="默认值" width="100" show-overflow-tooltip />
              <el-table-column prop="comment" label="注释" min-width="150" show-overflow-tooltip />
            </el-table>

            <!-- 索引信息 -->
            <div class="pm-index-section">
              <div class="pm-index-title">
                <el-icon><Collection /></el-icon>
                索引信息
              </div>
              <el-table :data="table.indexes" border stripe size="small">
                <el-table-column prop="name" label="索引名称" min-width="140" class-name="dp-mono" />
                <el-table-column prop="fields" label="索引字段" min-width="180">
                  <template #default="{ row }">
                    <el-tag v-for="f in row.fields" :key="f" size="small" effect="plain" style="margin-right: 4px">{{ f }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="type" label="类型" width="100">
                  <template #default="{ row }">
                    <el-tag size="small" effect="plain" :type="row.type === 'UNIQUE' ? 'warning' : 'primary'">{{ row.type }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="method" label="索引方式" width="100" />
                <el-table-column prop="comment" label="备注" min-width="120" show-overflow-tooltip />
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <template #footer>
        <el-button :icon="Files" @click="copyDDL">复制DDL</el-button>
        <el-button :icon="Download" @click="exportDDL">导出DDL</el-button>
        <el-button type="primary" @click="syncFromDrawer">同步到数据源</el-button>
      </template>
    </el-drawer>

    <!-- 批量生成DDL弹窗 -->
    <el-dialog v-model="batchGenerateVisible" title="批量生成DDL" width="520px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="选择模型">
          <el-select v-model="batchForm.models" multiple style="width: 100%" placeholder="请选择要生成DDL的物理模型">
            <el-option v-for="m in models" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="引擎方言">
          <el-radio-group v-model="batchForm.dialect">
            <el-radio-button value="hive">Hive</el-radio-button>
            <el-radio-button value="mysql">MySQL</el-radio-button>
            <el-radio-button value="clickhouse">ClickHouse</el-radio-button>
            <el-radio-button value="oracle">Oracle</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="命名规则">
          <el-input v-model="batchForm.namingRule" placeholder="如：dwd_{domain}_{table}_di" />
        </el-form-item>
        <el-form-item label="字符集">
          <el-select v-model="batchForm.charset" style="width: 100%">
            <el-option label="utf8mb4" value="utf8mb4" />
            <el-option label="utf8" value="utf8" />
            <el-option label="gbk" value="gbk" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchGenerateVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchGenerate">开始生成</el-button>
      </template>
    </el-dialog>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="isEdit ? '编辑物理模型' : '新建物理模型'" width="520px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="模型名称" required>
          <el-input v-model="editForm.name" placeholder="如：dwd_trade_v1" />
        </el-form-item>
        <el-form-item label="关联逻辑模型" required>
          <el-select v-model="editForm.sourceModel" style="width: 100%">
            <el-option label="客户主题域模型-V3" value="客户主题域模型-V3" />
            <el-option label="交易主题域模型-V3" value="交易主题域模型-V3" />
            <el-option label="商品主题域模型-V2" value="商品主题域模型-V2" />
            <el-option label="营销主题域模型-V1" value="营销主题域模型-V1" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据源类型" required>
          <el-select v-model="editForm.dbType" style="width: 100%">
            <el-option v-for="t in dbTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本">
          <el-input v-model="editForm.version" placeholder="V1.0" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="editForm.owner" />
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
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { DocumentChecked, Download, Files, Plus, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const tableDetailVisible = ref(false)
const batchGenerateVisible = ref(false)
const editVisible = ref(false)
const isEdit = ref(false)
const currentModel = ref(null)
const activeTable = ref('')

const query = reactive({
  keyword: '',
  dbType: '',
  status: '',
  page: 1,
  size: 10
})

const dbTypes = ['MySQL', 'Hive', 'ClickHouse', 'Oracle', 'PostgreSQL', 'Doris']

const models = ref([
  { id: 1, name: 'dwd_customer_v3', sourceModel: '客户主题域模型-V3', dbType: 'Hive', tables: 8, fields: 96, version: 'V3.0', status: '已部署', owner: '张伟', updatedAt: '2024-09-20 14:30' },
  { id: 2, name: 'dwd_trade_v3', sourceModel: '交易主题域模型-V3', dbType: 'Hive', tables: 12, fields: 128, version: 'V3.0', status: '已部署', owner: '李娜', updatedAt: '2024-09-25 10:15' },
  { id: 3, name: 'dws_summary_v2', sourceModel: '商品主题域模型-V2', dbType: 'ClickHouse', tables: 6, fields: 72, version: 'V2.1', status: '已部署', owner: '赵敏', updatedAt: '2024-09-18 16:45' },
  { id: 4, name: 'ads_report_v1', sourceModel: '营销主题域模型-V1', dbType: 'MySQL', tables: 5, fields: 58, version: 'V1.0', status: '同步中', owner: '孙浩', updatedAt: '2024-09-22 11:30' },
  { id: 5, name: 'dim_public_v2', sourceModel: '供应链主题域模型-V2', dbType: 'Hive', tables: 9, fields: 88, version: 'V2.0', status: '未部署', owner: '周明', updatedAt: '2024-09-24 09:20' },
  { id: 6, name: 'dwd_finance_v1', sourceModel: '财务主题域模型-V1', dbType: 'Oracle', tables: 5, fields: 48, version: 'V1.2', status: '已部署', owner: '吴丽', updatedAt: '2024-09-15 08:45' },
  { id: 7, name: 'dwd_log_v1', sourceModel: '用户行为模型-V1', dbType: 'ClickHouse', tables: 4, fields: 36, version: 'V1.0', status: '未部署', owner: '张伟', updatedAt: '2024-09-26 15:00' },
  { id: 8, name: 'dws_channel_v1', sourceModel: '渠道分析模型-V1', dbType: 'Doris', tables: 3, fields: 28, version: 'V1.0', status: '已部署', owner: '孙浩', updatedAt: '2024-08-10 14:00' }
])

const tableList = ref([
  {
    name: 'dwd_trade_order_di',
    comment: '订单明细表（日增量）',
    engine: 'ORC',
    charset: 'utf8',
    fields: [
      { name: 'order_id', type: 'BIGINT', length: '20', isPK: true, nullable: false, defaultValue: '', comment: '订单ID' },
      { name: 'order_no', type: 'VARCHAR', length: '32', isPK: false, nullable: false, defaultValue: '', comment: '订单编号' },
      { name: 'customer_id', type: 'BIGINT', length: '20', isPK: false, nullable: false, defaultValue: '', comment: '客户ID' },
      { name: 'order_amount', type: 'DECIMAL', length: '16,2', isPK: false, nullable: false, defaultValue: '0.00', comment: '订单金额' },
      { name: 'pay_amount', type: 'DECIMAL', length: '16,2', isPK: false, nullable: true, defaultValue: '0.00', comment: '实付金额' },
      { name: 'order_status', type: 'TINYINT', length: '1', isPK: false, nullable: false, defaultValue: '0', comment: '订单状态' },
      { name: 'channel_code', type: 'VARCHAR', length: '20', isPK: false, nullable: true, defaultValue: '', comment: '渠道编码' },
      { name: 'create_time', type: 'DATETIME', length: '', isPK: false, nullable: false, defaultValue: '', comment: '下单时间' },
      { name: 'pay_time', type: 'DATETIME', length: '', isPK: false, nullable: true, defaultValue: '', comment: '支付时间' },
      { name: 'dt', type: 'VARCHAR', length: '10', isPK: false, nullable: false, defaultValue: '', comment: '业务日期分区' }
    ],
    indexes: [
      { name: 'pk_order_id', fields: ['order_id'], type: 'PRIMARY', method: 'BTREE', comment: '主键索引' },
      { name: 'idx_customer_id', fields: ['customer_id'], type: 'NORMAL', method: 'BTREE', comment: '客户ID索引' },
      { name: 'idx_create_time', fields: ['create_time'], type: 'NORMAL', method: 'BTREE', comment: '创建时间索引' },
      { name: 'uk_order_no', fields: ['order_no'], type: 'UNIQUE', method: 'BTREE', comment: '订单号唯一索引' }
    ]
  },
  {
    name: 'dwd_trade_order_detail_di',
    comment: '订单商品明细表（日增量）',
    engine: 'ORC',
    charset: 'utf8',
    fields: [
      { name: 'detail_id', type: 'BIGINT', length: '20', isPK: true, nullable: false, defaultValue: '', comment: '明细ID' },
      { name: 'order_id', type: 'BIGINT', length: '20', isPK: false, nullable: false, defaultValue: '', comment: '订单ID' },
      { name: 'sku_id', type: 'BIGINT', length: '20', isPK: false, nullable: false, defaultValue: '', comment: 'SKU ID' },
      { name: 'sku_name', type: 'VARCHAR', length: '200', isPK: false, nullable: false, defaultValue: '', comment: '商品名称' },
      { name: 'qty', type: 'INT', length: '11', isPK: false, nullable: false, defaultValue: '1', comment: '购买数量' },
      { name: 'price', type: 'DECIMAL', length: '10,2', isPK: false, nullable: false, defaultValue: '0.00', comment: '单价' },
      { name: 'subtotal', type: 'DECIMAL', length: '16,2', isPK: false, nullable: false, defaultValue: '0.00', comment: '小计金额' },
      { name: 'dt', type: 'VARCHAR', length: '10', isPK: false, nullable: false, defaultValue: '', comment: '业务日期分区' }
    ],
    indexes: [
      { name: 'pk_detail_id', fields: ['detail_id'], type: 'PRIMARY', method: 'BTREE', comment: '主键索引' },
      { name: 'idx_order_id', fields: ['order_id'], type: 'NORMAL', method: 'BTREE', comment: '订单ID索引' },
      { name: 'idx_sku_id', fields: ['sku_id'], type: 'NORMAL', method: 'BTREE', comment: 'SKU索引' }
    ]
  },
  {
    name: 'dwd_trade_payment_di',
    comment: '支付记录表（日增量）',
    engine: 'ORC',
    charset: 'utf8',
    fields: [
      { name: 'pay_id', type: 'BIGINT', length: '20', isPK: true, nullable: false, defaultValue: '', comment: '支付ID' },
      { name: 'order_id', type: 'BIGINT', length: '20', isPK: false, nullable: false, defaultValue: '', comment: '订单ID' },
      { name: 'pay_no', type: 'VARCHAR', length: '64', isPK: false, nullable: false, defaultValue: '', comment: '支付流水号' },
      { name: 'pay_type', type: 'VARCHAR', length: '20', isPK: false, nullable: false, defaultValue: '', comment: '支付方式' },
      { name: 'pay_amount', type: 'DECIMAL', length: '16,2', isPK: false, nullable: false, defaultValue: '0.00', comment: '支付金额' },
      { name: 'pay_status', type: 'TINYINT', length: '1', isPK: false, nullable: false, defaultValue: '0', comment: '支付状态' },
      { name: 'pay_time', type: 'DATETIME', length: '', isPK: false, nullable: true, defaultValue: '', comment: '支付完成时间' },
      { name: 'dt', type: 'VARCHAR', length: '10', isPK: false, nullable: false, defaultValue: '', comment: '业务日期分区' }
    ],
    indexes: [
      { name: 'pk_pay_id', fields: ['pay_id'], type: 'PRIMARY', method: 'BTREE', comment: '主键索引' },
      { name: 'idx_order_id', fields: ['order_id'], type: 'NORMAL', method: 'BTREE', comment: '订单ID索引' }
    ]
  },
  {
    name: 'dwd_trade_refund_di',
    comment: '退款记录表（日增量）',
    engine: 'ORC',
    charset: 'utf8',
    fields: [
      { name: 'refund_id', type: 'BIGINT', length: '20', isPK: true, nullable: false, defaultValue: '', comment: '退款ID' },
      { name: 'order_id', type: 'BIGINT', length: '20', isPK: false, nullable: false, defaultValue: '', comment: '订单ID' },
      { name: 'refund_amount', type: 'DECIMAL', length: '16,2', isPK: false, nullable: false, defaultValue: '0.00', comment: '退款金额' },
      { name: 'reason', type: 'VARCHAR', length: '200', isPK: false, nullable: true, defaultValue: '', comment: '退款原因' },
      { name: 'refund_status', type: 'TINYINT', length: '1', isPK: false, nullable: false, defaultValue: '0', comment: '退款状态' },
      { name: 'dt', type: 'VARCHAR', length: '10', isPK: false, nullable: false, defaultValue: '', comment: '业务日期分区' }
    ],
    indexes: [
      { name: 'pk_refund_id', fields: ['refund_id'], type: 'PRIMARY', method: 'BTREE', comment: '主键索引' },
      { name: 'idx_order_id', fields: ['order_id'], type: 'NORMAL', method: 'BTREE', comment: '订单ID索引' }
    ]
  }
])

const filteredModels = computed(() => {
  return models.value.filter(m =>
    (!query.keyword || m.name.includes(query.keyword)) &&
    (!query.dbType || m.dbType === query.dbType) &&
    (!query.status || m.status === query.status)
  )
})

const total = computed(() => filteredModels.value.length)

const totalFields = computed(() => tableList.value.reduce((sum, t) => sum + t.fields.length, 0))

function statusDotClass(status) {
  return { 已部署: 'success', 未部署: 'info', 同步中: 'warning' }[status] || 'info'
}

function dbTypeTag(type) {
  const map = { MySQL: 'primary', Hive: 'success', ClickHouse: 'warning', Oracle: 'danger', PostgreSQL: 'info', Doris: '' }
  return map[type] || 'info'
}

function handleRefresh() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('数据已刷新')
  }, 800)
}

function openTableDetail(row) {
  currentModel.value = row
  activeTable.value = tableList.value[0]?.name || ''
  tableDetailVisible.value = true
}

function syncDataSource(row) {
  row.status = '同步中'
  ElMessage.info(`正在同步「${row.name}」表结构到数据源...`)
  setTimeout(() => {
    row.status = '已部署'
    ElMessage.success(`「${row.name}」结构同步完成`)
  }, 1500)
}

function openVersion(row) {
  ElMessage.info(`打开「${row.name}」的版本管理`)
}

function syncFromDrawer() {
  if (currentModel.value) {
    syncDataSource(currentModel.value)
  }
}

function copyDDL() {
  ElMessage.success('DDL已复制到剪贴板')
}

function exportDDL() {
  ElMessage.success('DDL已导出')
}

// 批量生成
const batchForm = reactive({
  models: [],
  dialect: 'hive',
  namingRule: 'dwd_{domain}_{table}_di',
  charset: 'utf8mb4'
})

function handleBatchGenerate() {
  if (batchForm.models.length === 0) {
    ElMessage.warning('请选择要生成DDL的模型')
    return
  }
  batchGenerateVisible.value = false
  ElMessage.success(`已提交 ${batchForm.models.length} 个模型的DDL生成任务`)
}

// 新建/编辑
const editForm = reactive({})

function openCreate() {
  isEdit.value = false
  Object.assign(editForm, { name: '', sourceModel: '', dbType: 'Hive', version: 'V1.0', owner: '张伟' })
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
      tables: 0,
      fields: 0,
      status: '未部署',
      updatedAt: '刚刚'
    })
    ElMessage.success('物理模型创建成功')
  }
  editVisible.value = false
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除物理模型「${row.name}」吗？删除后不可恢复。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(() => {
    models.value = models.value.filter(m => m.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}
</script>

<style scoped>
.pm-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.pm-icon {
  color: var(--dp-purple);
  font-size: 16px;
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

.pm-model-info {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px;
  background: var(--dp-bg-page);
  border-radius: 8px;
}
.pmi-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.pmi-label {
  font-size: 12px;
  color: var(--dp-text-3);
}
.pmi-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--dp-text-1);
}
.pm-table-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.pm-table-desc {
  font-size: 13px;
  color: var(--dp-text-2);
}
.pm-table-storage {
  font-size: 12px;
  color: var(--dp-text-3);
}
.field-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}
.pk-icon {
  color: var(--dp-warning);
  font-size: 12px;
}
.pm-index-section {
  margin-top: 16px;
}
.pm-index-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 10px;
}
</style>
