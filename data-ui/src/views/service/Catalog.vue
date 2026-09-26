<template>
  <div class="dp-page catalog-page">
    <PageHeader title="服务目录" desc="浏览和管理已发布的数据服务，支持按分类筛选、在线调试与文档查看">
      <el-button type="primary" :icon="Plus" @click="goToWorkbench">新建服务</el-button>
    </PageHeader>

    <div class="catalog-body">
      <!-- 左侧分类树 -->
      <div class="catalog-sidebar dp-card">
        <div class="sidebar-title">
          <el-icon><Menu /></el-icon>
          <span>API 分类目录</span>
        </div>
        <div class="category-tree">
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="cat-item"
            :class="{ active: activeCategory === cat.id }"
            @click="selectCategory(cat.id)"
          >
            <div class="cat-icon" :style="{ background: cat.bg, color: cat.color }">
              <el-icon><component :is="cat.icon" /></el-icon>
            </div>
            <div class="cat-info">
              <div class="cat-name">{{ cat.name }}</div>
              <div class="cat-count">{{ cat.count }} 个API</div>
            </div>
            <el-icon class="cat-arrow"><CaretRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 右侧内容区 -->
      <div class="catalog-main">
        <!-- 搜索工具栏 -->
        <div class="dp-card catalog-toolbar-card">
          <div class="dp-toolbar">
            <el-input
              v-model="query.keyword"
              placeholder="搜索API名称 / 路径 / 描述"
              clearable
              style="width: 300px"
              :prefix-icon="Search"
              @input="load"
            />
            <el-select v-model="query.method" placeholder="请求方式" clearable style="width: 120px" @change="load">
              <el-option label="GET" value="GET" />
              <el-option label="POST" value="POST" />
              <el-option label="PUT" value="PUT" />
              <el-option label="DELETE" value="DELETE" />
            </el-select>
            <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
              <el-option label="已发布" value="已发布" />
              <el-option label="草稿" value="开发中" />
              <el-option label="已下线" value="已下线" />
            </el-select>
            <div class="dp-toolbar-right">
              <el-radio-group v-model="viewMode" size="default">
                <el-radio-button value="card">
                  <el-icon><Grid /></el-icon>
                </el-radio-button>
                <el-radio-button value="list">
                  <el-icon><List /></el-icon>
                </el-radio-button>
              </el-radio-group>
              <el-button :icon="Refresh" @click="refresh">刷新</el-button>
            </div>
          </div>
          <div class="tag-filter">
            <span class="tag-label">标签筛选：</span>
            <el-tag
              v-for="tag in hotTags"
              :key="tag"
              :type="selectedTags.includes(tag) ? 'primary' : 'info'"
              effect="plain"
              class="filter-tag"
              @click="toggleTag(tag)"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>

        <!-- 卡片视图 -->
        <div v-if="viewMode === 'card'" class="api-card-grid">
          <div
            v-for="api in filteredApis"
            :key="api.id"
            class="api-card dp-fade-up"
            @click="openDetail(api)"
          >
            <div class="api-card-header">
              <span class="method-tag" :class="api.method.toLowerCase()">{{ api.method }}</span>
              <el-tag :type="statusTagType(api.status)" size="small" effect="light">{{ api.status }}</el-tag>
            </div>
            <div class="api-card-title">{{ api.name }}</div>
            <div class="api-card-path dp-mono">{{ api.path }}</div>
            <div class="api-card-desc">{{ api.desc }}</div>
            <div class="api-card-meta">
              <div class="meta-item">
                <el-icon :size="12"><Star /></el-icon>
                <span>v{{ api.version || '1.0' }}</span>
              </div>
              <div class="meta-item">
                <el-icon :size="12"><DataLine /></el-icon>
                <span>{{ formatCalls(api.callsToday) }}</span>
              </div>
              <div class="meta-item">
                <el-icon :size="12"><Clock /></el-icon>
                <span>{{ api.avgCost }}ms</span>
              </div>
              <div class="meta-item">
                <el-rate v-model="api.rating" disabled :max="5" size="small" style="--el-rate-star-height: 12px" />
              </div>
            </div>
            <div class="api-card-tags">
              <el-tag v-for="tag in api.tags?.slice(0, 3)" :key="tag" size="small" effect="plain" type="info">
                {{ tag }}
              </el-tag>
            </div>
            <div class="api-card-footer">
              <span class="owner">负责人：{{ api.owner }}</span>
              <span class="update-time">{{ api.updatedAt }}</span>
            </div>
          </div>
        </div>

        <!-- 列表视图 -->
        <div v-else class="dp-card">
          <el-table :data="filteredApis" border stripe>
            <el-table-column prop="name" label="API名称" min-width="180">
              <template #default="{ row }">
                <div class="api-name-cell">
                  <span class="method-tag" :class="row.method.toLowerCase()">{{ row.method }}</span>
                  <span class="api-name-text">{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="path" label="请求路径" min-width="220" class-name="dp-mono" />
            <el-table-column prop="version" label="版本" width="80">
              <template #default="{ row }">v{{ row.version || '1.0' }}</template>
            </el-table-column>
            <el-table-column prop="callsToday" label="调用次数" width="110" sortable>
              <template #default="{ row }">{{ formatCalls(row.callsToday) }}</template>
            </el-table-column>
            <el-table-column prop="avgCost" label="响应时间" width="100" sortable>
              <template #default="{ row }">{{ row.avgCost }}ms</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="评分" width="120">
              <template #default="{ row }">
                <el-rate v-model="row.rating" disabled :max="5" size="small" style="--el-rate-star-height: 12px" />
              </template>
            </el-table-column>
            <el-table-column prop="owner" label="负责人" width="80" />
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click.stop="goDebug(row)">在线调试</el-button>
                <el-button link type="primary" size="small" @click.stop="openDetail(row)">查看文档</el-button>
                <el-button link type="primary" size="small" @click.stop="subscribe(row)">订阅</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="query.page"
            :page-size="query.size"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="load"
          />
        </div>
      </div>
    </div>

    <!-- API详情抽屉 -->
    <el-drawer v-model="detailVisible" title="API 详情" size="600px" :destroy-on-close="true">
      <template v-if="currentApi" #header>
        <div class="drawer-header">
          <span class="method-tag" :class="currentApi.method.toLowerCase()">{{ currentApi.method }}</span>
          <span class="drawer-title">{{ currentApi.name }}</span>
          <el-tag :type="statusTagType(currentApi.status)" size="small" effect="light">{{ currentApi.status }}</el-tag>
        </div>
      </template>

      <div v-if="currentApi" class="api-detail">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="detail-section-title">基本信息</div>
          <div class="detail-path dp-mono">
            <span class="method-badge" :class="currentApi.method.toLowerCase()">{{ currentApi.method }}</span>
            {{ currentApi.path }}
          </div>
          <el-descriptions :column="2" border size="small" style="margin-top: 12px">
            <el-descriptions-item label="版本号">v{{ currentApi.version || '1.0.0' }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ currentApi.owner }}</el-descriptions-item>
            <el-descriptions-item label="数据来源">{{ currentApi.source }}</el-descriptions-item>
            <el-descriptions-item label="协议类型">{{ currentApi.protocol }}</el-descriptions-item>
            <el-descriptions-item label="QPS限制">{{ currentApi.qps }} 次/秒</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ currentApi.updatedAt }}</el-descriptions-item>
          </el-descriptions>
          <div class="detail-desc">
            <div class="desc-label">功能描述</div>
            <div class="desc-text">{{ currentApi.desc || '暂无描述' }}</div>
          </div>
        </div>

        <!-- 请求参数 -->
        <div class="detail-section">
          <div class="detail-section-title">请求参数</div>
          <el-tabs v-model="paramTab" size="small">
            <el-tab-pane label="Query 参数" name="query">
              <el-table :data="queryParams" border size="small">
                <el-table-column prop="name" label="参数名" width="140" />
                <el-table-column prop="type" label="类型" width="80" />
                <el-table-column prop="required" label="必填" width="60">
                  <template #default="{ row }">
                    <el-tag v-if="row.required" type="danger" size="small">是</el-tag>
                    <span v-else class="dp-desc">否</span>
                  </template>
                </el-table-column>
                <el-table-column prop="default" label="默认值" width="100" />
                <el-table-column prop="desc" label="说明" />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="Body 参数" name="body">
              <el-table :data="bodyParams" border size="small">
                <el-table-column prop="name" label="参数名" width="140" />
                <el-table-column prop="type" label="类型" width="80" />
                <el-table-column prop="required" label="必填" width="60">
                  <template #default="{ row }">
                    <el-tag v-if="row.required" type="danger" size="small">是</el-tag>
                    <span v-else class="dp-desc">否</span>
                  </template>
                </el-table-column>
                <el-table-column prop="desc" label="说明" />
              </el-table>
            </el-tab-pane>
            <el-tab-pane label="请求头" name="header">
              <el-table :data="headerParams" border size="small">
                <el-table-column prop="name" label="Header名称" width="160" />
                <el-table-column prop="required" label="必填" width="60">
                  <template #default="{ row }">
                    <el-tag v-if="row.required" type="danger" size="small">是</el-tag>
                    <span v-else class="dp-desc">否</span>
                  </template>
                </el-table-column>
                <el-table-column prop="desc" label="说明" />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 响应示例 -->
        <div class="detail-section">
          <div class="detail-section-title">响应示例</div>
          <div class="response-preview">
            <div class="response-status success">
              <el-icon><CircleCheck /></el-icon>
              <span>200 OK</span>
            </div>
            <pre class="response-code">{{ responseExample }}</pre>
          </div>
        </div>

        <!-- 错误码 -->
        <div class="detail-section">
          <div class="detail-section-title">错误码说明</div>
          <el-table :data="errorCodes" border size="small">
            <el-table-column prop="code" label="错误码" width="100" />
            <el-table-column prop="message" label="说明" width="180" />
            <el-table-column prop="solution" label="解决方案" />
          </el-table>
        </div>

        <!-- 操作按钮 -->
        <div class="detail-actions">
          <el-button type="primary" :icon="VideoPlay" @click="goDebug(currentApi)">在线调试</el-button>
          <el-button :icon="Bell" @click="subscribe(currentApi)">订阅服务</el-button>
          <el-button :icon="CopyDocument" @click="copyPath">复制路径</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { services } from '@/mock'
import { Bell, CopyDocument, Plus, Refresh, Search, VideoPlay } from '@element-plus/icons-vue'

const router = useRouter()

const viewMode = ref('card')
const activeCategory = ref('all')
const detailVisible = ref(false)
const currentApi = ref(null)
const paramTab = ref('query')

const categories = [
  { id: 'all', name: '全部服务', icon: 'Grid', color: '#1664ff', bg: '#e8f0ff', count: 0 },
  { id: 'query', name: '数据查询', icon: 'Search', color: '#00b42a', bg: '#e8ffea', count: 0 },
  { id: 'write', name: '数据写入', icon: 'Edit', color: '#ff7d00', bg: '#fff3e8', count: 0 },
  { id: 'stat', name: '统计分析', icon: 'DataLine', color: '#722ed1', bg: '#f5e8ff', count: 0 },
  { id: 'ai', name: 'AI服务', icon: 'MagicStick', color: '#f759ab', bg: '#ffe8f4', count: 0 },
  { id: 'report', name: '报表导出', icon: 'Document', color: '#0fc6c2', bg: '#e0fffa', count: 0 }
]

const hotTags = ['高频调用', '新上线', '推荐', '稳定', '限时免费', '会员专属']
const selectedTags = ref([])

const allApis = ref([])
const total = ref(0)

const query = reactive({
  keyword: '',
  method: '',
  status: '',
  page: 1,
  size: 12
})

const queryParams = ref([
  { name: 'page', type: 'number', required: false, default: '1', desc: '页码，从1开始' },
  { name: 'pageSize', type: 'number', required: false, default: '20', desc: '每页条数，最大100' },
  { name: 'keyword', type: 'string', required: false, default: '', desc: '搜索关键词' },
  { name: 'sortBy', type: 'string', required: false, default: 'create_time', desc: '排序字段' },
  { name: 'sortOrder', type: 'string', required: false, default: 'desc', desc: '排序方式：asc/desc' }
])

const bodyParams = ref([
  { name: 'name', type: 'string', required: true, desc: '名称' },
  { name: 'type', type: 'string', required: true, desc: '类型' },
  { name: 'status', type: 'number', required: false, desc: '状态：1启用 0禁用' }
])

const headerParams = ref([
  { name: 'Authorization', required: true, desc: '鉴权令牌，Bearer Token格式' },
  { name: 'Content-Type', required: true, desc: '请求内容类型，application/json' },
  { name: 'X-Request-Id', required: false, desc: '请求追踪ID，用于问题排查' },
  { name: 'X-App-Key', required: true, desc: '应用密钥标识' }
])

const errorCodes = [
  { code: 0, message: '成功', solution: '请求成功' },
  { code: 400, message: '参数错误', solution: '请检查请求参数是否符合接口规范' },
  { code: 401, message: '未授权', solution: '请检查 Authorization 令牌是否有效' },
  { code: 403, message: '权限不足', solution: '请申请该接口的调用权限' },
  { code: 404, message: '资源不存在', solution: '请检查请求路径是否正确' },
  { code: 429, message: '请求过于频繁', solution: '请求已限流，请稍后重试' },
  { code: 500, message: '服务器内部错误', solution: '请联系管理员排查' },
  { code: 503, message: '服务不可用', solution: '服务暂时不可用，请稍后重试' }
]

const responseExample = JSON.stringify({
  code: 0,
  message: 'success',
  requestId: 'req_' + Math.random().toString(36).substring(2, 15),
  data: {
    total: 100,
    page: 1,
    pageSize: 20,
    list: [
      { id: 1, name: '示例数据1', value: 2350, status: 'active', createTime: '2024-01-15 10:30:00' },
      { id: 2, name: '示例数据2', value: 1820, status: 'active', createTime: '2024-01-15 10:25:00' },
      { id: 3, name: '示例数据3', value: 3100, status: 'inactive', createTime: '2024-01-15 10:20:00' }
    ]
  }
}, null, 2)

const filteredApis = computed(() => {
  let list = [...allApis.value]

  // 分类筛选
  if (activeCategory.value !== 'all') {
    const catMap = { query: 'query', write: 'write', stat: 'stat', ai: 'ai', report: 'report' }
    list = list.filter(a => a.category === catMap[activeCategory.value])
  }

  // 关键词搜索
  if (query.keyword) {
    const kw = query.keyword.toLowerCase()
    list = list.filter(a =>
      a.name.toLowerCase().includes(kw) ||
      a.path.toLowerCase().includes(kw) ||
      (a.desc && a.desc.toLowerCase().includes(kw))
    )
  }

  // 请求方式
  if (query.method) {
    list = list.filter(a => a.method === query.method)
  }

  // 状态
  if (query.status) {
    list = list.filter(a => a.status === query.status)
  }

  // 标签筛选
  if (selectedTags.value.length > 0) {
    list = list.filter(a =>
      a.tags && selectedTags.value.some(t => a.tags.includes(t))
    )
  }

  total.value = list.length

  if (viewMode.value === 'list') {
    return list.slice((query.page - 1) * query.size, query.page * query.size)
  }
  return list
})

function statusTagType(status) {
  const map = { '已发布': 'success', '开发中': 'warning', '已下线': 'info' }
  return map[status] || 'info'
}

function formatCalls(num) {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toLocaleString()
}

function selectCategory(id) {
  activeCategory.value = id
  query.page = 1
}

function toggleTag(tag) {
  const idx = selectedTags.value.indexOf(tag)
  if (idx > -1) {
    selectedTags.value.splice(idx, 1)
  } else {
    selectedTags.value.push(tag)
  }
}

function load() {
  query.page = 1
}

function refresh() {
  ElMessage.success('刷新成功')
}

function openDetail(api) {
  currentApi.value = api
  detailVisible.value = true
}

function goDebug(api) {
  router.push({ path: '/service/test', query: { name: api.name } })
}

function goToWorkbench() {
  router.push('/service/workbench')
}

function subscribe(api) {
  ElMessage.success(`已订阅「${api.name}」，将在服务变更时通知您`)
}

function copyPath() {
  if (currentApi.value) {
    navigator.clipboard.writeText(currentApi.value.path)
    ElMessage.success('路径已复制到剪贴板')
  }
}

onMounted(() => {
  const categoriesList = ['query', 'write', 'stat', 'ai', 'report']
  const tagPool = ['高频调用', '新上线', '推荐', '稳定', '限时免费', '会员专属', '热门', '精选']

  allApis.value = services.map((s, i) => ({
    ...s,
    desc: s.path.includes('query') ? '查询数据列表，支持分页、搜索和排序' :
          s.path.includes('detail') ? '根据ID获取详细信息' :
          s.path.includes('aggregate') ? '聚合统计数据，支持多维度分析' :
          '数据服务接口',
    version: '1.' + (i % 5),
    rating: 3.5 + Math.random() * 1.5,
    category: categoriesList[i % categoriesList.length],
    tags: [tagPool[i % tagPool.length], tagPool[(i + 2) % tagPool.length]]
  }))

  // 更新分类计数
  categories.forEach(cat => {
    if (cat.id === 'all') {
      cat.count = allApis.value.length
    } else {
      cat.count = allApis.value.filter(a => a.category === cat.id).length
    }
  })
})
</script>

<style scoped>
.catalog-page {
  min-height: 100%;
  padding-bottom: 0;
}

.catalog-body {
  display: flex;
  gap: 16px;
  padding-bottom: 20px;
}

/* 侧边栏 */
.catalog-sidebar {
  width: 220px;
  flex-shrink: 0;
  margin-bottom: 0;
  padding: 16px;
  max-height: calc(100vh - 160px);
  overflow-y: auto;
  position: sticky;
  top: 20px;
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--dp-border-light);
}

.category-tree {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.cat-item:hover {
  background: var(--dp-bg-page);
}

.cat-item.active {
  background: var(--dp-primary-bg);
}

.cat-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cat-info {
  flex: 1;
  min-width: 0;
}

.cat-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-1);
  margin-bottom: 2px;
}

.cat-count {
  font-size: 11px;
  color: var(--dp-text-3);
}

.cat-arrow {
  font-size: 12px;
  color: var(--dp-text-4);
  opacity: 0;
  transition: opacity 0.2s;
}

.cat-item:hover .cat-arrow,
.cat-item.active .cat-arrow {
  opacity: 1;
  color: var(--dp-primary);
}

/* 主内容区 */
.catalog-main {
  flex: 1;
  min-width: 0;
}

.catalog-toolbar-card {
  margin-bottom: 16px;
}

.tag-filter {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--dp-border-light);
}

.tag-label {
  font-size: 12px;
  color: var(--dp-text-3);
  flex-shrink: 0;
}

.filter-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.filter-tag:hover {
  transform: translateY(-1px);
}

/* 卡片视图 */
.api-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}

.api-card {
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: var(--dp-radius);
  padding: 16px;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: var(--dp-shadow-sm);
}

.api-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  border-color: var(--dp-primary);
}

.api-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.api-card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.api-card-path {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.api-card-desc {
  font-size: 12.5px;
  color: var(--dp-text-2);
  line-height: 1.6;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.api-card-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--dp-border-light);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--dp-text-3);
}

.api-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.api-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11.5px;
  color: var(--dp-text-4);
}

/* 方法标签 */
.method-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
  font-family: Consolas, 'JetBrains Mono', monospace;
}

.method-tag.get {
  background: var(--dp-success-light);
  color: var(--dp-success);
}

.method-tag.post {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
}

.method-tag.put {
  background: var(--dp-warning-light);
  color: var(--dp-warning);
}

.method-tag.delete {
  background: var(--dp-danger-light);
  color: var(--dp-danger);
}

/* 列表视图名称 */
.api-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.api-name-text {
  font-weight: 500;
}

/* 详情抽屉 */
.drawer-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.drawer-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--dp-text-1);
  flex: 1;
}

.api-detail {
  padding-right: 8px;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 12px;
  padding-left: 10px;
  border-left: 3px solid var(--dp-primary);
}

.detail-path {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: var(--dp-bg-page);
  border-radius: 8px;
  font-size: 13px;
}

.method-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
  font-family: Consolas, monospace;
  flex-shrink: 0;
}

.method-badge.get {
  background: var(--dp-success-light);
  color: var(--dp-success);
}

.method-badge.post {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
}

.detail-desc {
  margin-top: 12px;
}

.desc-label {
  font-size: 13px;
  color: var(--dp-text-2);
  margin-bottom: 6px;
  font-weight: 500;
}

.desc-text {
  font-size: 13px;
  color: var(--dp-text-3);
  line-height: 1.7;
  padding: 10px 12px;
  background: var(--dp-bg-page);
  border-radius: 6px;
}

.response-preview {
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  overflow: hidden;
}

.response-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--dp-success-light);
  color: var(--dp-success);
  font-size: 13px;
  font-weight: 500;
}

.response-code {
  background: #0d1117;
  color: #cdd6f4;
  padding: 14px;
  margin: 0;
  font-family: Consolas, 'JetBrains Mono', monospace;
  font-size: 12.5px;
  line-height: 1.7;
  max-height: 300px;
  overflow: auto;
}

.detail-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid var(--dp-border-light);
}
</style>
