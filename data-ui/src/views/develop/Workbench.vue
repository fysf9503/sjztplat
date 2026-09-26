<template>
  <div class="wb-page">
    <!-- 顶部工具栏 -->
    <div class="wb-topbar">
      <div class="wb-topbar-left">
        <el-select v-model="currentDatasource" style="width: 220px" size="default">
          <el-option v-for="d in datasourceOptions" :key="d.value" :label="d.label" :value="d.value">
            <span style="display: flex; align-items: center; gap: 8px">
              <span class="wb-ds-icon" :class="d.type.toLowerCase()">{{ d.type.slice(0, 2) }}</span>
              {{ d.label }}
            </span>
          </el-option>
        </el-select>
        <el-select v-model="currentDatabase" style="width: 160px" size="default" placeholder="选择数据库">
          <el-option v-for="db in databaseOptions" :key="db" :label="db" :value="db" />
        </el-select>
        <el-divider direction="vertical" />
        <el-button type="primary" :icon="CaretRight" :loading="running" @click="runSql">运行</el-button>
        <el-button :icon="VideoPause" :disabled="!running" @click="stopSql">停止</el-button>
        <el-button :icon="MagicStick" @click="formatSql">格式化</el-button>
        <el-button :icon="DocumentChecked" @click="saveScript">保存</el-button>
        <el-button :icon="Download" @click="exportResult">导出</el-button>
        <el-button :icon="Delete" @click="clearEditor">清空</el-button>
      </div>
      <div class="wb-topbar-right">
        <span class="wb-lines-info">行数: {{ currentLines }}</span>
        <el-button type="primary" plain :icon="Plus" @click="addTab">新建查询</el-button>
      </div>
    </div>

    <div class="wb-body">
      <!-- 左侧：数据库对象树 -->
      <div class="wb-left">
        <div class="wb-left-header">
          <span class="wb-left-title">数据库对象</span>
          <el-button text size="small" :icon="Refresh" @click="refreshTree" />
        </div>
        <el-input
          v-model="treeKeyword"
          placeholder="搜索表 / 字段"
          clearable
          size="small"
          :prefix-icon="Search"
          style="margin-bottom: 10px"
        />
        <div class="wb-tree">
          <div v-for="db in filteredTree" :key="db.name" class="wb-tree-db">
            <div class="wb-tree-db-name" @click="toggleDb(db)">
              <el-icon :size="12" class="wb-tree-arrow" :class="{ open: db.open }">
                <ArrowRight />
              </el-icon>
              <el-icon color="var(--dp-warning)" :size="16"><FolderOpened /></el-icon>
              <span class="dp-mono wb-tree-label">{{ db.name }}</span>
            </div>
            <div v-show="db.open" class="wb-tree-tables">
              <div
                v-for="table in db.tables"
                :key="table.name"
                class="wb-tree-table"
                @click="selectTable(table)"
                @contextmenu.prevent="showTableMenu($event, table)"
              >
                <el-icon color="var(--dp-primary)" :size="14"><Document /></el-icon>
                <span class="dp-mono wb-tree-label">{{ table.name }}</span>
                <span class="wb-table-meta">{{ table.fields.length }}字段</span>
              </div>
              <div v-if="tableDetail && tableDetail.db === db.name" class="wb-tree-fields">
                <div
                  v-for="field in tableDetail.fields"
                  :key="field.name"
                  class="wb-tree-field"
                >
                  <el-icon :size="12" color="#86909c"><Rank /></el-icon>
                  <span class="dp-mono wb-field-name">{{ field.name }}</span>
                  <span class="wb-field-type">{{ field.type }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间主体 -->
      <div class="wb-center">
        <!-- SQL编辑器Tab -->
        <div class="wb-editor-tabs">
          <div
            v-for="(tab, idx) in tabs"
            :key="tab.id"
            class="wb-editor-tab"
            :class="{ active: activeTab === tab.id, modified: tab.modified }"
            @click="activeTab = tab.id"
          >
            <el-icon :size="12" color="#86909c"><Document /></el-icon>
            <span class="wb-tab-name">{{ tab.name }}</span>
            <el-icon
              v-if="tabs.length > 1"
              class="wb-tab-close"
              :size="12"
              @click.stop="closeTab(tab.id)"
            >
              <Close />
            </el-icon>
          </div>
        </div>

        <!-- SQL编辑器 -->
        <div class="wb-editor-wrap">
          <div class="wb-editor-gutter">
            <div v-for="n in currentLines" :key="n" class="wb-gutter-line">{{ n }}</div>
          </div>
          <textarea
            ref="editorRef"
            v-model="currentTabSql"
            class="wb-editor"
            spellcheck="false"
            @keydown.tab.prevent="handleTab"
            @keydown.ctrl.enter.prevent="runSql"
            @scroll="syncGutterScroll"
          />
        </div>

        <!-- 底部结果区 -->
        <div class="wb-result">
          <div class="wb-result-tabs">
            <div
              v-for="t in resultTabs"
              :key="t.key"
              class="wb-result-tab"
              :class="{ active: resultActiveTab === t.key }"
              @click="resultActiveTab = t.key"
            >
              <el-icon :size="13"><component :is="t.icon" /></el-icon>
              {{ t.label }}
              <el-badge v-if="t.badge" :value="t.badge" class="wb-tab-badge" />
            </div>
            <div class="wb-result-tools">
              <el-button text size="small" @click="toggleResultHeight">
                <el-icon><component :is="resultExpanded ? 'ArrowDown' : 'ArrowUp'" /></el-icon>
              </el-button>
            </div>
          </div>

          <div class="wb-result-content" :class="{ expanded: resultExpanded }">
            <!-- 结果Tab -->
            <div v-show="resultActiveTab === 'result'" class="wb-result-panel">
              <div class="wb-result-header">
                <span class="wb-result-info">
                  共 <b>{{ resultData.length }}</b> 行
                  <span v-if="executionCost">，耗时 {{ executionCost }}s</span>
                </span>
                <div class="wb-result-actions">
                  <el-button size="small" text :icon="Download">导出CSV</el-button>
                  <el-button size="small" text :icon="Download">导出Excel</el-button>
                  <el-button size="small" text :icon="CopyDocument" @click="copyResult">复制</el-button>
                </div>
              </div>
              <el-table :data="resultPageData" border size="small" height="100%" class="wb-result-table">
                <el-table-column
                  v-for="col in resultColumns"
                  :key="col"
                  :prop="col"
                  :label="col"
                  min-width="120"
                  show-overflow-tooltip
                />
              </el-table>
              <el-pagination
                v-model:current-page="resultPage"
                :page-size="resultPageSize"
                :total="resultData.length"
                layout="prev, pager, next, total"
                size="small"
                class="wb-result-pager"
              />
            </div>

            <!-- 消息Tab -->
            <div v-show="resultActiveTab === 'message'" class="wb-result-panel">
              <div class="wb-message-list">
                <div v-for="(msg, idx) in messages" :key="idx" class="wb-message-item" :class="msg.type">
                  <span class="wb-message-time">[{{ msg.time }}]</span>
                  <span class="wb-message-level">{{ msg.level }}</span>
                  <span class="wb-message-text">{{ msg.text }}</span>
                </div>
                <div v-if="!messages.length" class="wb-empty">暂无执行日志</div>
              </div>
            </div>

            <!-- 执行历史Tab -->
            <div v-show="resultActiveTab === 'history'" class="wb-result-panel">
              <el-table :data="historyList" border size="small" height="100%">
                <el-table-column prop="time" label="执行时间" width="170" />
                <el-table-column prop="sql" label="SQL语句" min-width="200" show-overflow-tooltip class-name="dp-mono" />
                <el-table-column prop="duration" label="耗时" width="80" align="center" />
                <el-table-column prop="rows" label="行数" width="80" align="center" />
                <el-table-column prop="status" label="状态" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.status === '成功' ? 'success' : 'danger'" size="small">
                      {{ row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" align="center">
                  <template #default="{ row }">
                    <el-button link type="primary" size="small" @click="loadHistory(row)">载入</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：SQL模板面板 -->
      <div class="wb-right">
        <div class="wb-right-header">
          <span class="wb-right-title">SQL模板</span>
        </div>
        <div class="wb-template-categories">
          <div
            v-for="cat in templateCategories"
            :key="cat.key"
            class="wb-template-cat"
            :class="{ active: activeCategory === cat.key }"
            @click="activeCategory = cat.key"
          >
            <el-icon :size="14"><component :is="cat.icon" /></el-icon>
            {{ cat.label }}
          </div>
        </div>
        <div class="wb-template-list">
          <div
            v-for="tpl in filteredTemplates"
            :key="tpl.name"
            class="wb-template-item"
            @click="insertTemplate(tpl)"
          >
            <div class="wb-template-name">{{ tpl.name }}</div>
            <div class="wb-template-desc">{{ tpl.desc }}</div>
          </div>
        </div>

        <el-divider />

        <div class="wb-right-header">
          <span class="wb-right-title">常用函数</span>
        </div>
        <div class="wb-function-list">
          <div v-for="fn in functions" :key="fn.name" class="wb-function-item" @click="insertFunction(fn)">
            <span class="wb-func-name dp-mono">{{ fn.name }}()</span>
            <span class="wb-func-desc">{{ fn.desc }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 表右键菜单 -->
    <ul
      v-show="contextMenu.visible"
      class="wb-context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @click="contextMenu.visible = false"
    >
      <li @click="viewTableStructure">
        <el-icon><Document /></el-icon>查看表结构
      </li>
      <li @click="previewTableData">
        <el-icon><View /></el-icon>预览数据
      </li>
      <li @click="generateSelect">
        <el-icon><DocumentCopy /></el-icon>生成SELECT语句
      </li>
      <li class="wb-menu-divider"></li>
      <li @click="copyTableName">
        <el-icon><CopyDocument /></el-icon>复制表名
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CaretRight, CopyDocument, Delete, DocumentChecked, Download, MagicStick, Plus, Refresh, Search, VideoPause } from '@element-plus/icons-vue'

// 数据源
const datasourceOptions = [
  { label: '核心交易库', value: 'core_trade', type: 'MySQL' },
  { label: '客户主数据库', value: 'customer_db', type: 'MySQL' },
  { label: '日志采集库', value: 'log_db', type: 'ClickHouse' },
  { label: '财务系统库', value: 'finance_db', type: 'Oracle' },
  { label: '数据仓库', value: 'data_warehouse', type: 'Hive' }
]
const currentDatasource = ref('core_trade')
const currentDatabase = ref('dwd_trade_db')
const databaseOptions = ['ods_trade_db', 'dwd_trade_db', 'dws_summary_db', 'ads_report_db', 'dim_public_db']

// Tab管理
const tabs = ref([
  {
    id: 'tab_1',
    name: '查询1.sql',
    sql: `-- 近30天各渠道GMV与退款率统计
SELECT
  channel,
  SUM(order_amount) AS gmv,
  COUNT(DISTINCT customer_id) AS buyers,
  ROUND(SUM(refund_amount) / NULLIF(SUM(order_amount), 0) * 100, 2) AS refund_rate
FROM dwd_trade_db.trade_flow
WHERE pay_time >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)
GROUP BY channel
ORDER BY gmv DESC;`,
    modified: false
  },
  {
    id: 'tab_2',
    name: '客户画像.sql',
    sql: `-- VIP客户画像标签统计
SELECT
  c.customer_id,
  c.level,
  COUNT(o.order_id) AS order_count,
  SUM(o.amount) AS total_amount,
  MAX(o.pay_time) AS last_pay_time
FROM dim_public_db.customer_dim c
LEFT JOIN dwd_trade_db.trade_wide o ON o.customer_id = c.customer_id
WHERE c.level IN ('VIP2', 'VIP3')
GROUP BY c.customer_id, c.level
HAVING SUM(o.amount) > 5000
ORDER BY total_amount DESC;`,
    modified: false
  }
])
const activeTab = ref('tab_1')
let tabCounter = 2

const currentTabSql = computed({
  get() {
    const tab = tabs.value.find(t => t.id === activeTab.value)
    return tab ? tab.sql : ''
  },
  set(val) {
    const tab = tabs.value.find(t => t.id === activeTab.value)
    if (tab) {
      tab.sql = val
      tab.modified = true
    }
  }
})

const currentLines = computed(() => {
  return currentTabSql.value.split('\n').length || 1
})

const editorRef = ref(null)

function addTab() {
  tabCounter++
  const id = 'tab_' + tabCounter
  tabs.value.push({
    id,
    name: `查询${tabCounter}.sql`,
    sql: '-- 新建查询\nSELECT * FROM table_name LIMIT 100;',
    modified: false
  })
  activeTab.value = id
}

function closeTab(id) {
  if (tabs.value.length === 1) return
  const idx = tabs.value.findIndex(t => t.id === id)
  tabs.value.splice(idx, 1)
  if (activeTab.value === id) {
    activeTab.value = tabs.value[Math.max(0, idx - 1)].id
  }
}

// 数据库树
const treeKeyword = ref('')
const tableDetail = ref(null)

const dbTree = ref([
  {
    name: 'ods_trade_db',
    open: false,
    tables: [
      { name: 'order_info', fields: [{ name: 'order_id', type: 'BIGINT' }, { name: 'customer_id', type: 'BIGINT' }, { name: 'order_amount', type: 'DECIMAL(16,2)' }, { name: 'status', type: 'TINYINT' }, { name: 'create_time', type: 'DATETIME' }] },
      { name: 'order_detail', fields: [{ name: 'detail_id', type: 'BIGINT' }, { name: 'order_id', type: 'BIGINT' }, { name: 'sku_id', type: 'BIGINT' }, { name: 'qty', type: 'INT' }, { name: 'price', type: 'DECIMAL(10,2)' }] },
      { name: 'payment_record', fields: [{ name: 'pay_id', type: 'BIGINT' }, { name: 'order_id', type: 'BIGINT' }, { name: 'amount', type: 'DECIMAL(16,2)' }, { name: 'pay_time', type: 'DATETIME' }] },
      { name: 'refund_order', fields: [{ name: 'refund_id', type: 'BIGINT' }, { name: 'order_id', type: 'BIGINT' }, { name: 'amount', type: 'DECIMAL(16,2)' }, { name: 'reason', type: 'VARCHAR(200)' }] }
    ]
  },
  {
    name: 'dwd_trade_db',
    open: true,
    tables: [
      { name: 'trade_flow', fields: [{ name: 'id', type: 'BIGINT' }, { name: 'order_id', type: 'BIGINT' }, { name: 'customer_id', type: 'BIGINT' }, { name: 'channel', type: 'VARCHAR(32)' }, { name: 'order_amount', type: 'DECIMAL(16,2)' }, { name: 'refund_amount', type: 'DECIMAL(16,2)' }, { name: 'pay_time', type: 'DATETIME' }] },
      { name: 'trade_wide', fields: [{ name: 'id', type: 'BIGINT' }, { name: 'order_id', type: 'BIGINT' }, { name: 'customer_id', type: 'BIGINT' }, { name: 'amount', type: 'DECIMAL(16,2)' }, { name: 'pay_time', type: 'DATETIME' }] },
      { name: 'refund_flow', fields: [{ name: 'id', type: 'BIGINT' }, { name: 'refund_id', type: 'BIGINT' }, { name: 'order_id', type: 'BIGINT' }, { name: 'amount', type: 'DECIMAL(16,2)' }] }
    ]
  },
  {
    name: 'dws_summary_db',
    open: false,
    tables: [
      { name: 'sale_day', fields: [{ name: 'stat_date', type: 'DATE' }, { name: 'channel', type: 'VARCHAR(32)' }, { name: 'gmv', type: 'DECIMAL(18,2)' }, { name: 'order_cnt', type: 'BIGINT' }] },
      { name: 'member_stat', fields: [{ name: 'stat_date', type: 'DATE' }, { name: 'level', type: 'VARCHAR(16)' }, { name: 'member_cnt', type: 'BIGINT' }] }
    ]
  },
  {
    name: 'ads_report_db',
    open: false,
    tables: [
      { name: 'gmv_report', fields: [{ name: 'report_date', type: 'DATE' }, { name: 'channel', type: 'VARCHAR(32)' }, { name: 'gmv', type: 'DECIMAL(18,2)' }] },
      { name: 'member_profile', fields: [{ name: 'customer_id', type: 'BIGINT' }, { name: 'level', type: 'VARCHAR(16)' }, { name: 'total_amount', type: 'DECIMAL(18,2)' }] }
    ]
  },
  {
    name: 'dim_public_db',
    open: false,
    tables: [
      { name: 'customer_dim', fields: [{ name: 'customer_id', type: 'BIGINT' }, { name: 'name', type: 'VARCHAR(64)' }, { name: 'level', type: 'VARCHAR(16)' }, { name: 'phone', type: 'VARCHAR(16)' }] },
      { name: 'product_dim', fields: [{ name: 'sku_id', type: 'BIGINT' }, { name: 'sku_name', type: 'VARCHAR(128)' }, { name: 'category', type: 'VARCHAR(64)' }] }
    ]
  }
])

const filteredTree = computed(() => {
  if (!treeKeyword.value) return dbTree.value
  const kw = treeKeyword.value.toLowerCase()
  return dbTree.value.map(db => ({
    ...db,
    open: true,
    tables: db.tables.filter(t =>
      t.name.toLowerCase().includes(kw) ||
      t.fields.some(f => f.name.toLowerCase().includes(kw))
    )
  })).filter(db => db.name.toLowerCase().includes(kw) || db.tables.length > 0)
})

function toggleDb(db) {
  db.open = !db.open
}

function selectTable(table) {
  const db = dbTree.value.find(d => d.tables.some(t => t.name === table.name))
  if (tableDetail.value && tableDetail.value.name === table.name) {
    tableDetail.value = null
  } else {
    tableDetail.value = { ...table, db: db?.name }
  }
}

function refreshTree() {
  ElMessage.success('数据库对象已刷新')
}

// 右键菜单
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  table: null
})

function showTableMenu(e, table) {
  contextMenu.visible = true
  contextMenu.x = e.clientX
  contextMenu.y = e.clientY
  contextMenu.table = table
  document.addEventListener('click', hideContextMenu, { once: true })
}

function hideContextMenu() {
  contextMenu.visible = false
}

function viewTableStructure() {
  const table = contextMenu.table
  if (!table) return
  const fieldsInfo = table.fields.map(f => `${f.name} ${f.type}`).join('\n')
  insertToEditor(`-- ${table.name} 表结构\n-- ${fieldsInfo.replace(/\n/g, '\n-- ')}\nDESC ${table.name};`)
  ElMessage.info('已生成表结构查询语句')
}

function previewTableData() {
  const table = contextMenu.table
  if (!table) return
  insertToEditor(`-- 预览 ${table.name} 前100条数据\nSELECT * FROM ${table.name} LIMIT 100;`)
}

function generateSelect() {
  const table = contextMenu.table
  if (!table) return
  const fields = table.fields.map(f => f.name).join(',\n  ')
  insertToEditor(`SELECT\n  ${fields}\nFROM ${table.name}\nWHERE 1=1\nLIMIT 100;`)
  ElMessage.success('已生成SELECT语句')
}

function copyTableName() {
  const table = contextMenu.table
  if (!table) return
  navigator.clipboard?.writeText(table.name)
  ElMessage.success('表名已复制')
}

// SQL执行
const running = ref(false)
const executionCost = ref('')

const resultTabs = [
  { key: 'result', label: '结果', icon: 'Grid', badge: 0 },
  { key: 'message', label: '消息', icon: 'ChatDotRound', badge: 0 },
  { key: 'history', label: '执行历史', icon: 'Clock', badge: 0 }
]
const resultActiveTab = ref('result')
const resultExpanded = ref(false)

const resultColumns = ref(['channel', 'gmv', 'buyers', 'refund_rate', 'avg_order'])
const resultData = ref([
  { channel: 'APP', gmv: 12834200.5, buyers: 48210, refund_rate: 2.31, avg_order: 266.2 },
  { channel: '小程序', gmv: 8621400.8, buyers: 35120, refund_rate: 3.05, avg_order: 245.6 },
  { channel: 'WEB', gmv: 4215600.2, buyers: 12340, refund_rate: 4.12, avg_order: 341.6 },
  { channel: 'H5', gmv: 2110800.4, buyers: 8940, refund_rate: 3.86, avg_order: 236.1 },
  { channel: '线下扫码', gmv: 1583200.6, buyers: 6210, refund_rate: 1.98, avg_order: 254.9 }
])
const resultPage = ref(1)
const resultPageSize = 20
const resultPageData = computed(() => {
  const start = (resultPage.value - 1) * resultPageSize
  return resultData.value.slice(start, start + resultPageSize)
})

const messages = ref([
  { time: '10:32:01', level: 'INFO', type: 'info', text: '任务提交到 Spark 引擎（队列：etl_prod）' },
  { time: '10:32:03', level: 'INFO', type: 'info', text: 'SQL 解析通过，扫描分区 30 个' },
  { time: '10:32:05', level: 'INFO', type: 'info', text: 'Shuffle 阶段完成，数据量 1.2GB' },
  { time: '10:32:07', level: 'SUCCESS', type: 'success', text: '执行成功，返回 5 行，耗时 0.86s' }
])

const historyList = ref([
  { time: '2026-09-26 10:32:07', sql: 'SELECT channel, SUM(order_amount) AS gmv FROM trade_flow GROUP BY channel', duration: '0.86s', rows: 5, status: '成功' },
  { time: '2026-09-26 10:28:15', sql: 'SELECT * FROM customer_dim WHERE level = "VIP2"', duration: '0.32s', rows: 128, status: '成功' },
  { time: '2026-09-26 10:15:42', sql: 'SELECT COUNT(*) FROM order_info WHERE create_time >= "2026-09-25"', duration: '1.24s', rows: 1, status: '成功' },
  { time: '2026-09-26 09:58:30', sql: 'SELECT * FROM non_exist_table', duration: '0.12s', rows: 0, status: '失败' }
])

function runSql() {
  if (!currentTabSql.value.trim()) {
    ElMessage.warning('请输入SQL语句')
    return
  }
  running.value = true
  resultActiveTab.value = 'message'
  const now = new Date()
  const timeStr = now.toTimeString().slice(0, 8)
  messages.value = [
    { time: timeStr, level: 'INFO', type: 'info', text: '开始执行 SQL 语句...' },
    { time: timeStr, level: 'INFO', type: 'info', text: '数据源: ' + currentDatasource.value }
  ]

  setTimeout(() => {
    const midTime = new Date().toTimeString().slice(0, 8)
    messages.value.push({ time: midTime, level: 'INFO', type: 'info', text: 'SQL 解析通过，正在执行...' })
  }, 400)

  setTimeout(() => {
    running.value = false
    const endTime = new Date()
    const cost = ((endTime - now) / 1000).toFixed(2)
    executionCost.value = cost
    const endTimeStr = endTime.toTimeString().slice(0, 8)
    messages.value.push({
      time: endTimeStr,
      level: 'SUCCESS',
      type: 'success',
      text: `执行成功，返回 ${resultData.value.length} 行，耗时 ${cost}s`
    })
    resultActiveTab.value = 'result'
    resultTabs[0].badge = resultData.value.length
    ElMessage.success('SQL 执行成功')

    // 添加到历史
    historyList.value.unshift({
      time: endTime.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
      sql: currentTabSql.value.split('\n')[0].substring(0, 60),
      duration: cost + 's',
      rows: resultData.value.length,
      status: '成功'
    })
  }, 1200)
}

function stopSql() {
  running.value = false
  const time = new Date().toTimeString().slice(0, 8)
  messages.value.push({ time, level: 'WARN', type: 'warning', text: '用户已中止执行' })
  ElMessage.warning('SQL 执行已中止')
}

function formatSql() {
  const sql = currentTabSql.value
  if (!sql.trim()) return
  // 简单格式化
  let formatted = sql
    .replace(/\s+/g, ' ')
    .replace(/\s*,\s*/g, ', ')
    .replace(/\s*\(\s*/g, '(')
    .replace(/\s*\)\s*/g, ')')
  // 简单美化：关键字换行
  formatted = formatted
    .replace(/\bSELECT\b/gi, '\nSELECT')
    .replace(/\bFROM\b/gi, '\nFROM')
    .replace(/\bWHERE\b/gi, '\nWHERE')
    .replace(/\bGROUP BY\b/gi, '\nGROUP BY')
    .replace(/\bORDER BY\b/gi, '\nORDER BY')
    .replace(/\bHAVING\b/gi, '\nHAVING')
    .replace(/\bLIMIT\b/gi, '\nLIMIT')
    .replace(/^\n/, '')
  currentTabSql.value = formatted
  ElMessage.success('SQL 已格式化')
}

function saveScript() {
  ElMessage.success('脚本已保存')
  const tab = tabs.value.find(t => t.id === activeTab.value)
  if (tab) tab.modified = false
}

function exportResult() {
  ElMessage.success('结果导出中...')
}

function clearEditor() {
  currentTabSql.value = ''
}

function copyResult() {
  ElMessage.success('结果已复制到剪贴板')
}

function loadHistory(row) {
  currentTabSql.value = row.sql
  ElMessage.success('已载入历史SQL')
}

function toggleResultHeight() {
  resultExpanded.value = !resultExpanded.value
}

// SQL模板
const templateCategories = [
  { key: 'query', label: '查询统计', icon: 'DataAnalysis' },
  { key: 'dml', label: '数据操作', icon: 'Edit' },
  { key: 'ddl', label: '建表语句', icon: 'Tickets' },
  { key: 'window', label: '窗口函数', icon: 'TrendCharts' }
]
const activeCategory = ref('query')

const sqlTemplates = [
  { category: 'query', name: '按渠道统计GMV', desc: '按渠道分组统计交易金额', sql: 'SELECT\n  channel,\n  SUM(order_amount) AS gmv,\n  COUNT(DISTINCT customer_id) AS buyers\nFROM dwd_trade_db.trade_flow\nWHERE pay_time >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY)\nGROUP BY channel\nORDER BY gmv DESC;' },
  { category: 'query', name: 'TOP N排名', desc: '分组取TOP N记录', sql: 'SELECT * FROM (\n  SELECT\n    category,\n    product_name,\n    sale_amount,\n    ROW_NUMBER() OVER (PARTITION BY category ORDER BY sale_amount DESC) AS rn\n  FROM dws_summary_db.product_sale\n) t WHERE rn <= 10;' },
  { category: 'query', name: '同比环比', desc: '计算同比环比增长率', sql: 'SELECT\n  stat_date,\n  gmv,\n  LAG(gmv, 1) OVER (ORDER BY stat_date) AS prev_day_gmv,\n  ROUND((gmv - LAG(gmv, 1) OVER (ORDER BY stat_date)) / LAG(gmv, 1) OVER (ORDER BY stat_date) * 100, 2) AS mom_rate\nFROM dws_summary_db.sale_day\nORDER BY stat_date;' },
  { category: 'dml', name: '插入数据', desc: 'INSERT INTO SELECT 语句', sql: 'INSERT INTO dws_summary_db.target_table\nSELECT\n  col1,\n  col2,\n  SUM(col3) AS total\nFROM dwd_trade_db.source_table\nWHERE dt = "2026-09-26"\nGROUP BY col1, col2;' },
  { category: 'dml', name: '更新数据', desc: 'UPDATE JOIN 更新语句', sql: 'UPDATE dwd_trade_db.order_info o\nJOIN dim_public_db.customer_dim c ON o.customer_id = c.customer_id\nSET o.customer_level = c.level\nWHERE o.dt = "2026-09-26";' },
  { category: 'dml', name: '删除数据', desc: '按条件删除数据', sql: 'DELETE FROM dwd_trade_db.temp_table\nWHERE create_time < DATE_SUB(NOW(), INTERVAL 7 DAY);' },
  { category: 'ddl', name: '创建表', desc: '标准建表语句模板', sql: 'CREATE TABLE IF NOT EXISTS dwd_trade_db.table_name (\n  id BIGINT COMMENT "主键ID",\n  name VARCHAR(64) COMMENT "名称",\n  amount DECIMAL(16,2) COMMENT "金额",\n  create_time DATETIME COMMENT "创建时间",\n  update_time DATETIME COMMENT "更新时间"\n)\nCOMMENT "表注释"\nPARTITIONED BY (dt STRING COMMENT "日期分区")\nSTORED AS PARQUET;' },
  { category: 'ddl', name: '添加字段', desc: 'ALTER TABLE ADD COLUMN', sql: 'ALTER TABLE dwd_trade_db.table_name\nADD COLUMN new_column DECIMAL(16,2) COMMENT "新增字段" AFTER amount;' },
  { category: 'window', name: '累计求和', desc: 'SUM() OVER() 累计', sql: 'SELECT\n  stat_date,\n  daily_gmv,\n  SUM(daily_gmv) OVER (ORDER BY stat_date) AS cumulative_gmv\nFROM dws_summary_db.sale_day\nORDER BY stat_date;' },
  { category: 'window', name: '移动平均', desc: 'AVG() 移动平均计算', sql: 'SELECT\n  stat_date,\n  daily_gmv,\n  AVG(daily_gmv) OVER (ORDER BY stat_date ROWS BETWEEN 6 PRECEDING AND CURRENT ROW) AS ma_7day\nFROM dws_summary_db.sale_day\nORDER BY stat_date;' }
]

const filteredTemplates = computed(() =>
  sqlTemplates.filter(t => t.category === activeCategory.value)
)

function insertTemplate(tpl) {
  insertToEditor('\n' + tpl.sql + '\n')
  ElMessage.success(`已插入模板：${tpl.name}`)
}

// 常用函数
const functions = [
  { name: 'COUNT', desc: '计数函数' },
  { name: 'SUM', desc: '求和函数' },
  { name: 'AVG', desc: '平均值函数' },
  { name: 'MAX', desc: '最大值函数' },
  { name: 'MIN', desc: '最小值函数' },
  { name: 'DATE_FORMAT', desc: '日期格式化' },
  { name: 'DATE_SUB', desc: '日期减法' },
  { name: 'DATEDIFF', desc: '日期差值' },
  { name: 'CONCAT', desc: '字符串拼接' },
  { name: 'SUBSTRING', desc: '字符串截取' },
  { name: 'COALESCE', desc: '空值替换' },
  { name: 'CASE WHEN', desc: '条件分支' },
  { name: 'ROW_NUMBER', desc: '行号函数' },
  { name: 'RANK', desc: '排名函数' },
  { name: 'LAG', desc: '偏移访问函数' }
]

function insertFunction(fn) {
  insertToEditor(fn.name + '()')
}

// 工具函数
function insertToEditor(text) {
  currentTabSql.value += text
  nextTick(() => {
    if (editorRef.value) {
      editorRef.value.scrollTop = editorRef.value.scrollHeight
    }
  })
}

function handleTab(e) {
  const start = e.target.selectionStart
  const end = e.target.selectionEnd
  const value = currentTabSql.value
  currentTabSql.value = value.substring(0, start) + '  ' + value.substring(end)
  nextTick(() => {
    e.target.selectionStart = e.target.selectionEnd = start + 2
  })
}

function syncGutterScroll(e) {
  const gutter = e.target.previousElementSibling
  if (gutter) {
    gutter.scrollTop = e.target.scrollTop
  }
}

onMounted(() => {
  // 初始化
})
</script>

<style scoped>
.wb-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
  background: var(--dp-bg-page);
  overflow: hidden;
}

/* 顶部工具栏 */
.wb-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid var(--dp-border-light);
  flex-shrink: 0;
}

.wb-topbar-left,
.wb-topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.wb-lines-info {
  font-size: 12px;
  color: var(--dp-text-3);
  font-family: 'JetBrains Mono', Consolas, monospace;
}

.wb-ds-icon {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  background: var(--dp-primary);
}

.wb-ds-icon.mysql { background: #4479a1; }
.wb-ds-icon.oracle { background: #e11d48; }
.wb-ds-icon.clickhouse { background: #ffcc01; color: #4e5969; }
.wb-ds-icon.hive { background: #f7ba1e; color: #4e5969; }

/* 主体布局 */
.wb-body {
  flex: 1;
  display: flex;
  gap: 0;
  min-height: 0;
}

/* 左侧数据库树 */
.wb-left {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid var(--dp-border-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.wb-left-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px 8px;
}

.wb-left-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.wb-tree {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 12px;
}

.wb-tree-db {
  margin-bottom: 2px;
}

.wb-tree-db-name {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 12.5px;
}

.wb-tree-db-name:hover {
  background: var(--dp-primary-light);
}

.wb-tree-arrow {
  transition: transform 0.2s;
  color: var(--dp-text-3);
}

.wb-tree-arrow.open {
  transform: rotate(90deg);
}

.wb-tree-label {
  font-size: 12px;
  color: var(--dp-text-2);
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wb-tree-tables {
  padding-left: 4px;
}

.wb-tree-table {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px 5px 24px;
  cursor: pointer;
  border-radius: 4px;
  font-size: 12px;
}

.wb-tree-table:hover {
  background: var(--dp-primary-light);
}

.wb-table-meta {
  margin-left: auto;
  font-size: 10px;
  color: var(--dp-text-4);
}

.wb-tree-fields {
  padding-left: 36px;
  background: #fafbfc;
  border-radius: 4px;
  margin: 2px 0;
}

.wb-tree-field {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  font-size: 11.5px;
}

.wb-field-name {
  color: var(--dp-text-2);
  flex: 1;
}

.wb-field-type {
  font-size: 10px;
  color: var(--dp-text-4);
  font-family: 'JetBrains Mono', Consolas, monospace;
}

/* 中间主体 */
.wb-center {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* SQL编辑器Tab */
.wb-editor-tabs {
  display: flex;
  background: #f7f8fa;
  border-bottom: 1px solid var(--dp-border-light);
  padding: 6px 8px 0;
  gap: 2px;
  flex-shrink: 0;
  overflow-x: auto;
}

.wb-editor-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-bottom: none;
  border-radius: 6px 6px 0 0;
  font-size: 12px;
  color: var(--dp-text-3);
  cursor: pointer;
  white-space: nowrap;
  position: relative;
}

.wb-editor-tab.active {
  color: var(--dp-text-1);
  font-weight: 500;
  background: #fff;
  border-color: var(--dp-border);
  z-index: 1;
}

.wb-editor-tab.modified .wb-tab-name::after {
  content: '*';
  color: var(--dp-warning);
  margin-left: 2px;
}

.wb-tab-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.wb-tab-close {
  opacity: 0.5;
  cursor: pointer;
}

.wb-tab-close:hover {
  opacity: 1;
  color: var(--dp-danger);
}

/* SQL编辑器 */
.wb-editor-wrap {
  display: flex;
  flex: 1;
  min-height: 200px;
  background: #1e1e2e;
  overflow: hidden;
}

.wb-editor-gutter {
  width: 48px;
  flex-shrink: 0;
  background: #181825;
  text-align: right;
  padding: 12px 8px;
  color: #6c7086;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  line-height: 1.7;
  user-select: none;
  overflow: hidden;
  border-right: 1px solid #313244;
}

.wb-gutter-line {
  height: 20.4px;
  line-height: 20.4px;
}

.wb-editor {
  flex: 1;
  resize: none;
  border: none;
  outline: none;
  background: #1e1e2e;
  color: #cdd6f4;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
  padding: 12px 14px;
  tab-size: 2;
  white-space: pre;
  overflow: auto;
}

.wb-editor::selection {
  background: rgba(137, 180, 250, 0.3);
}

/* 底部结果区 */
.wb-result {
  border-top: 1px solid var(--dp-border-light);
  background: #fff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.wb-result-tabs {
  display: flex;
  align-items: center;
  padding: 0 12px;
  border-bottom: 1px solid var(--dp-border-light);
  background: #fafbfc;
  flex-shrink: 0;
}

.wb-result-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  font-size: 13px;
  color: var(--dp-text-3);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}

.wb-result-tab.active {
  color: var(--dp-primary);
  border-bottom-color: var(--dp-primary);
  font-weight: 500;
}

.wb-tab-badge {
  margin-left: -2px;
}

.wb-result-tools {
  margin-left: auto;
}

.wb-result-content {
  height: 240px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: height 0.2s;
}

.wb-result-content.expanded {
  height: calc(100vh - 300px);
}

.wb-result-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.wb-result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid var(--dp-border-light);
  flex-shrink: 0;
}

.wb-result-info {
  font-size: 12px;
  color: var(--dp-text-3);
}

.wb-result-info b {
  color: var(--dp-text-1);
  font-weight: 600;
}

.wb-result-actions {
  display: flex;
  gap: 4px;
}

.wb-result-table {
  flex: 1;
}

.wb-result-pager {
  padding: 8px 12px;
  border-top: 1px solid var(--dp-border-light);
  flex-shrink: 0;
}

/* 消息列表 */
.wb-message-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px 14px;
  background: #0d1117;
  font-family: Consolas, monospace;
  font-size: 12px;
  line-height: 2;
}

.wb-message-item {
  display: flex;
  gap: 10px;
  color: #8b949e;
}

.wb-message-item.success { color: #3fb950; }
.wb-message-item.warning { color: #d29922; }
.wb-message-item.error { color: #f85149; }

.wb-message-time {
  color: #6e7681;
  flex-shrink: 0;
}

.wb-message-level {
  flex-shrink: 0;
  width: 60px;
  font-weight: 600;
}

.wb-empty {
  text-align: center;
  color: #6e7681;
  padding: 40px 0;
}

/* 右侧面板 */
.wb-right {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-left: 1px solid var(--dp-border-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.wb-right-header {
  padding: 12px 14px 8px;
  flex-shrink: 0;
}

.wb-right-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.wb-template-categories {
  display: flex;
  gap: 6px;
  padding: 0 12px 10px;
  flex-wrap: wrap;
  flex-shrink: 0;
}

.wb-template-cat {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  font-size: 11px;
  color: var(--dp-text-3);
  background: #f2f3f5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.wb-template-cat:hover {
  color: var(--dp-primary);
  background: var(--dp-primary-light);
}

.wb-template-cat.active {
  color: #fff;
  background: var(--dp-primary);
}

.wb-template-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 10px;
}

.wb-template-item {
  padding: 10px 12px;
  border: 1px solid var(--dp-border-light);
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.wb-template-item:hover {
  border-color: var(--dp-primary);
  background: var(--dp-primary-bg);
}

.wb-template-name {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--dp-text-1);
  margin-bottom: 4px;
}

.wb-template-desc {
  font-size: 11px;
  color: var(--dp-text-3);
  line-height: 1.5;
}

.wb-function-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 12px;
}

.wb-function-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.wb-function-item:hover {
  background: var(--dp-primary-light);
}

.wb-func-name {
  color: var(--dp-primary);
  font-weight: 500;
  font-size: 11.5px;
}

.wb-func-desc {
  color: var(--dp-text-3);
  font-size: 11px;
}

/* 右键菜单 */
.wb-context-menu {
  position: fixed;
  z-index: 9999;
  background: #fff;
  border: 1px solid var(--dp-border);
  border-radius: 6px;
  padding: 4px 0;
  min-width: 140px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  list-style: none;
  margin: 0;
}

.wb-context-menu li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  font-size: 12.5px;
  color: var(--dp-text-2);
  cursor: pointer;
}

.wb-context-menu li:hover {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
}

.wb-menu-divider {
  height: 1px;
  background: var(--dp-border-light);
  padding: 0 !important;
  margin: 4px 0;
}

.wb-menu-divider:hover {
  background: var(--dp-border-light) !important;
}
</style>
