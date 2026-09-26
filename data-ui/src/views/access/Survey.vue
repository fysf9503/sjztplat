<template>
  <div class="dp-page">
    <PageHeader title="接入探查" desc="对数据源进行结构探查与数据采样，快速了解数据分布与质量状况">
      <el-button type="primary" :icon="Search" @click="startSurvey">开始探查</el-button>
    </PageHeader>

    <div class="survey-container dp-fade-up">
      <!-- 左侧数据源树 -->
      <div class="survey-sidebar">
        <div class="dp-card survey-tree-card">
          <div class="survey-tree-header">
            <el-input
              v-model="treeSearch"
              placeholder="搜索数据源/表"
              size="small"
              clearable
              :prefix-icon="Search"
            />
          </div>
          <el-tree
            :data="treeData"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            :default-expanded-keys="defaultExpandedKeys"
            :filter-node-method="filterNode"
            highlight-current
            @node-click="handleNodeClick"
            class="survey-tree"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <span v-if="data.type === 'datasource'" class="tree-icon ds-icon">
                  <el-icon :size="14"><Coin /></el-icon>
                </span>
                <span v-else-if="data.type === 'database'" class="tree-icon db-icon">
                  <el-icon :size="14"><FolderOpened /></el-icon>
                </span>
                <span v-else class="tree-icon table-icon">
                  <el-icon :size="14"><Grid /></el-icon>
                </span>
                <span class="tree-label">{{ data.name }}</span>
                <span v-if="data.type === 'table'" class="tree-count">{{ data.rows }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧探查结果 -->
      <div class="survey-content">
        <div v-if="!selectedTable" class="survey-empty">
          <el-icon :size="64" color="#c9cdd4"><DataBoard /></el-icon>
          <p>请从左侧选择一张表进行探查</p>
        </div>

        <template v-else>
          <!-- 表基本信息 -->
          <div class="dp-card">
            <div class="dp-card-header">
              <h3 class="dp-card-title">
                <el-icon color="#1664ff"><Grid /></el-icon>
                {{ selectedTable.name }}
              </h3>
              <span class="dp-card-extra">
                <el-tag size="small" type="info">{{ selectedTable.database }}</el-tag>
              </span>
            </div>
            <div class="table-info-grid">
              <div class="table-info-item">
                <div class="info-label">数据行数</div>
                <div class="info-value">{{ formatNumber(selectedTable.rowCount) }}</div>
              </div>
              <div class="table-info-item">
                <div class="info-label">数据大小</div>
                <div class="info-value">{{ selectedTable.sizeMB }} MB</div>
              </div>
              <div class="table-info-item">
                <div class="info-label">字段数量</div>
                <div class="info-value">{{ fieldList.length }}</div>
              </div>
              <div class="table-info-item">
                <div class="info-label">更新时间</div>
                <div class="info-value small">{{ selectedTable.updateTime }}</div>
              </div>
            </div>
          </div>

          <!-- 字段列表 -->
          <div class="dp-card">
            <div class="dp-card-header">
              <h3 class="dp-card-title">字段列表</h3>
              <span class="dp-card-extra">共 {{ fieldList.length }} 个字段</span>
            </div>
            <el-table :data="fieldList" border stripe size="small" style="width: 100%">
              <el-table-column prop="name" label="字段名" width="160" class-name="dp-mono" />
              <el-table-column prop="type" label="类型" width="140">
                <template #default="{ row }">
                  <span class="field-type">{{ row.type }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="length" label="长度" width="80" align="center" />
              <el-table-column label="可空" width="70" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.nullable" size="small" type="success" effect="plain">是</el-tag>
                  <el-tag v-else size="small" type="info" effect="plain">否</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="主键" width="70" align="center">
                <template #default="{ row }">
                  <el-tag v-if="row.primaryKey" size="small" type="warning" effect="plain">PK</el-tag>
                  <span v-else class="text-muted">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="comment" label="注释" min-width="200">
                <template #default="{ row }">
                  <span v-if="row.comment">{{ row.comment }}</span>
                  <span v-else class="text-muted">-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 数据样例 -->
          <div class="dp-card">
            <div class="dp-card-header">
              <h3 class="dp-card-title">数据样例</h3>
              <span class="dp-card-extra">前 10 行数据预览</span>
            </div>
            <el-table :data="sampleData" border stripe size="small" style="width: 100%" max-height="320">
              <el-table-column
                v-for="field in fieldList.slice(0, 8)"
                :key="field.name"
                :prop="field.name"
                :label="field.name"
                min-width="140"
                class-name="dp-mono"
              >
                <template #default="{ row }">
                  <span class="sample-value">{{ row[field.name] }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 数据分布统计 -->
          <div class="dp-card">
            <div class="dp-card-header">
              <h3 class="dp-card-title">数据分布统计</h3>
              <span class="dp-card-extra">数值字段分布</span>
            </div>
            <div class="dist-charts">
              <div v-for="field in numericFields" :key="field.name" class="dist-chart-item">
                <div class="dist-chart-title">
                  <span>{{ field.name }}</span>
                  <span class="dist-chart-sub">{{ field.comment }}</span>
                </div>
                <EChart :option="getDistChartOption(field)" height="160px" />
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Coin, FolderOpened, Grid, DataBoard } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import EChart from '@/components/EChart.vue'
import { dataSources } from '@/mock'

const treeSearch = ref('')
const selectedTable = ref(null)
const treeRef = ref(null)
const defaultExpandedKeys = ref([])

const treeData = computed(() => {
  const dsList = dataSources.slice(0, 6)
  return dsList.map((ds, dsIdx) => ({
    id: `ds-${ds.id}`,
    name: ds.name,
    type: 'datasource',
    dsType: ds.type,
    children: [
      {
        id: `db-${ds.id}`,
        name: ds.database,
        type: 'database',
        children: generateTables(ds.id, dsIdx)
      }
    ]
  }))
})

function generateTables(dsId, dsIdx) {
  const tableNames = [
    'customer_info', 'order_detail', 'product_sku', 'trade_flow',
    'user_behavior', 'payment_record', 'refund_order', 'member_profile'
  ]
  const baseCount = (dsIdx + 1) * 1000
  return tableNames.slice(0, 5 + dsIdx % 3).map((name, idx) => ({
    id: `table-${dsId}-${idx}`,
    name,
    type: 'table',
    database: name,
    rows: formatNumberShort(baseCount + idx * 2500),
    rowCount: baseCount + idx * 2500,
    sizeMB: (baseCount + idx * 2500) * 0.02 + 10,
    updateTime: `2026-09-${26 - idx} 10:30:00`
  }))
}

function formatNumberShort(num) {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num.toString()
}

const fieldList = ref([
  { name: 'id', type: 'BIGINT', length: 20, nullable: false, primaryKey: true, comment: '主键ID' },
  { name: 'name', type: 'VARCHAR', length: 64, nullable: false, primaryKey: false, comment: '名称' },
  { name: 'type', type: 'VARCHAR', length: 32, nullable: true, primaryKey: false, comment: '类型' },
  { name: 'status', type: 'TINYINT', length: 4, nullable: false, primaryKey: false, comment: '状态' },
  { name: 'amount', type: 'DECIMAL', length: '16,2', nullable: true, primaryKey: false, comment: '金额' },
  { name: 'quantity', type: 'INT', length: 11, nullable: true, primaryKey: false, comment: '数量' },
  { name: 'phone', type: 'VARCHAR', length: 20, nullable: true, primaryKey: false, comment: '手机号' },
  { name: 'email', type: 'VARCHAR', length: 128, nullable: true, primaryKey: false, comment: '邮箱' },
  { name: 'address', type: 'VARCHAR', length: 255, nullable: true, primaryKey: false, comment: '地址' },
  { name: 'create_time', type: 'DATETIME', length: 0, nullable: false, primaryKey: false, comment: '创建时间' },
  { name: 'update_time', type: 'DATETIME', length: 0, nullable: true, primaryKey: false, comment: '更新时间' }
])

const numericFields = computed(() => [
  { name: 'amount', comment: '金额分布', min: 0, max: 10000 },
  { name: 'quantity', comment: '数量分布', min: 0, max: 500 }
])

const sampleData = ref([
  { id: 10001, name: '北京客户A', type: 'VIP', status: 1, amount: '5280.00', quantity: 12, phone: '138****1234', email: 'a@example.com' },
  { id: 10002, name: '上海客户B', type: '普通', status: 1, amount: '1560.50', quantity: 5, phone: '139****5678', email: 'b@example.com' },
  { id: 10003, name: '广州客户C', type: 'VIP', status: 0, amount: '8320.00', quantity: 28, phone: '137****9012', email: 'c@example.com' },
  { id: 10004, name: '深圳客户D', type: '普通', status: 1, amount: '2890.00', quantity: 8, phone: '136****3456', email: 'd@example.com' },
  { id: 10005, name: '杭州客户E', type: 'VIP', status: 1, amount: '6210.00', quantity: 18, phone: '135****7890', email: 'e@example.com' },
  { id: 10006, name: '成都客户F', type: '普通', status: 2, amount: '580.00', quantity: 2, phone: '134****2345', email: 'f@example.com' },
  { id: 10007, name: '武汉客户G', type: 'VIP', status: 1, amount: '9450.00', quantity: 35, phone: '133****6789', email: 'g@example.com' },
  { id: 10008, name: '南京客户H', type: '普通', status: 0, amount: '320.00', quantity: 1, phone: '132****0123', email: 'h@example.com' },
  { id: 10009, name: '西安客户I', type: 'VIP', status: 1, amount: '7680.00', quantity: 22, phone: '131****4567', email: 'i@example.com' },
  { id: 10010, name: '重庆客户J', type: '普通', status: 1, amount: '1200.00', quantity: 4, phone: '130****8901', email: 'j@example.com' }
])

function filterNode(value, data) {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

function handleNodeClick(data) {
  if (data.type === 'table') {
    selectedTable.value = data
  }
}

function formatNumber(num) {
  return num.toLocaleString()
}

function getDistChartOption(field) {
  const buckets = 8
  const data = []
  for (let i = 0; i < buckets; i++) {
    data.push(Math.floor(Math.random() * 5000) + 500)
  }
  const labels = []
  const step = (field.max - field.min) / buckets
  for (let i = 0; i < buckets; i++) {
    labels.push(`${Math.round(field.min + i * step)}-${Math.round(field.min + (i + 1) * step)}`)
  }
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e5e6eb',
      borderWidth: 1,
      textStyle: { color: '#1d2129' },
      axisPointer: { type: 'shadow' }
    },
    grid: { left: 40, right: 10, top: 10, bottom: 25 },
    xAxis: {
      type: 'category',
      data: labels,
      axisLine: { lineStyle: { color: '#e5e6eb' } },
      axisLabel: { color: '#86909c', fontSize: 10, rotate: 30 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#86909c', fontSize: 10 }
    },
    series: [{
      type: 'bar',
      data,
      barWidth: '60%',
      itemStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#1664ff' },
            { offset: 1, color: '#69b1ff' }
          ]
        },
        borderRadius: [3, 3, 0, 0]
      }
    }]
  }
}

function startSurvey() {
  ElMessage.info('正在启动探查任务，请稍候...')
}

onMounted(() => {
  nextTick(() => {
    if (treeData.value.length > 0 && treeData.value[0].children?.length > 0) {
      defaultExpandedKeys.value = [treeData.value[0].id, treeData.value[0].children[0].id]
    }
  })
})
</script>

<style scoped>
.survey-container {
  display: flex;
  gap: 16px;
  min-height: calc(100vh - 160px);
}

.survey-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.survey-tree-card {
  height: 100%;
  margin-bottom: 0;
  padding: 12px;
}

.survey-tree-header {
  margin-bottom: 12px;
}

.survey-tree {
  height: calc(100% - 50px);
  overflow-y: auto;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  font-size: 13px;
}
.tree-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.ds-icon { color: #1664ff; }
.db-icon { color: #ff7d00; }
.table-icon { color: #00b42a; }

.tree-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tree-count {
  font-size: 11px;
  color: var(--dp-text-3);
  background: var(--dp-border-light);
  padding: 1px 6px;
  border-radius: 10px;
  flex-shrink: 0;
}

.survey-content {
  flex: 1;
  min-width: 0;
}

.survey-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  color: var(--dp-text-3);
  background: #fff;
  border-radius: var(--dp-radius);
  border: 1px solid var(--dp-border-light);
}
.survey-empty p {
  margin-top: 16px;
  font-size: 14px;
}

.table-info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.table-info-item {
  text-align: center;
  padding: 16px;
  background: var(--dp-bg-page);
  border-radius: 8px;
}
.info-label {
  font-size: 13px;
  color: var(--dp-text-3);
  margin-bottom: 8px;
}
.info-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}
.info-value.small {
  font-size: 14px;
  font-weight: 500;
  font-family: inherit;
}

.field-type {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: #722ed1;
  background: #f5e8ff;
  padding: 2px 6px;
  border-radius: 4px;
}

.text-muted {
  color: var(--dp-text-4);
}

.sample-value {
  font-size: 12px;
  color: var(--dp-text-2);
}

.dist-charts {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.dist-chart-item {
  background: var(--dp-bg-page);
  border-radius: 8px;
  padding: 12px;
}
.dist-chart-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-1);
}
.dist-chart-sub {
  font-size: 12px;
  font-weight: 400;
  color: var(--dp-text-3);
}
</style>
