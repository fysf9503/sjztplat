<template>
  <div class="dp-page square-page">
    <div class="dp-page-header dp-fade-up">
      <div>
        <div class="dp-page-eyebrow">APP SQUARE</div>
        <h2 class="dp-page-title">应用广场</h2>
        <p class="dp-page-desc">探索已发布的智能体应用，快速启用适合您业务场景的智能助手。</p>
      </div>
      <div class="dp-page-header-actions">
        <el-button :icon="View" @click="viewMode = 'grid'">
          <el-icon><Grid /></el-icon>
        </el-button>
        <el-button :icon="List" @click="viewMode = 'list'">
          <el-icon><List /></el-icon>
        </el-button>
      </div>
    </div>

    <!-- 搜索和分类 -->
    <div class="dp-card square-search-card">
      <div class="square-search">
        <el-input v-model="keyword" placeholder="搜索智能体应用..." size="large" :prefix-icon="Search" style="max-width: 520px">
          <template #append>
            <el-button type="primary" :icon="Search">搜索</el-button>
          </template>
        </el-input>
      </div>
      <div class="square-categories">
        <div
          v-for="cat in categories"
          :key="cat.key"
          class="square-cat"
          :class="{ active: activeCategory === cat.key }"
          @click="activeCategory = cat.key"
        >
          <el-icon :size="16"><component :is="cat.icon" /></el-icon>
          <span>{{ cat.name }}</span>
          <span class="square-cat-count">{{ cat.count }}</span>
        </div>
      </div>
    </div>

    <!-- 热门推荐 -->
    <div class="dp-card">
      <div class="dp-card-header">
        <div class="dp-card-title">
          <el-icon color="var(--dp-warning)"><Star /></el-icon>
          <span style="margin-left: 6px">热门推荐</span>
        </div>
        <el-button link type="primary" size="small">查看全部</el-button>
      </div>
      <div class="hot-list">
        <div v-for="(app, idx) in hotApps" :key="app.id" class="hot-item" @click="useApp(app)">
          <div class="hot-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</div>
          <div class="hot-icon" :style="{ background: getColor(app.id) }">
            <el-icon :size="18"><Cpu /></el-icon>
          </div>
          <div class="hot-info">
            <div class="hot-name">{{ app.name }}</div>
            <div class="hot-desc">{{ app.model }} · {{ app.callsMonth }} 次调用</div>
          </div>
          <el-button type="primary" size="small" plain>立即使用</el-button>
        </div>
      </div>
    </div>

    <!-- 应用卡片列表 -->
    <div class="dp-card">
      <div class="dp-card-header">
        <div class="dp-card-title">全部应用 <span class="dp-card-extra">{{ filteredApps.length }} 个应用</span></div>
        <div class="dp-flex dp-gap-8">
          <el-select v-model="sortBy" size="small" style="width: 120px">
            <el-option label="最热门" value="hot" />
            <el-option label="最新发布" value="new" />
            <el-option label="评分最高" value="rating" />
          </el-select>
        </div>
      </div>

      <!-- 卡片视图 -->
      <div v-if="viewMode === 'grid'" class="app-grid">
        <div v-for="app in filteredApps" :key="app.id" class="app-card dp-fade-up" @click="useApp(app)">
          <div class="app-card-header">
            <div class="app-icon" :style="{ background: getColor(app.id) }">
              <el-icon :size="22"><Cpu /></el-icon>
            </div>
            <el-tag size="small" :type="catTagType(app.category)">{{ getCategoryName(app.category) }}</el-tag>
          </div>
          <div class="app-name">{{ app.name }}</div>
          <div class="app-desc">{{ app.description }}</div>
          <div class="app-stats">
            <div class="app-stat">
              <el-icon :size="12" color="#86909c"><User /></el-icon>
              <span>{{ formatCalls(app.callsMonth) }}</span>
            </div>
            <div class="app-stat">
              <el-icon :size="12" color="#ff7d00"><Star /></el-icon>
              <span>{{ app.satisfaction.toFixed(1) }}</span>
            </div>
          </div>
          <div class="app-footer">
            <div class="app-creator">
              <el-avatar :size="20">{{ app.owner[0] }}</el-avatar>
              <span>{{ app.owner }}</span>
            </div>
            <el-button type="primary" size="small">立即使用</el-button>
          </div>
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-else class="app-list">
        <div v-for="app in filteredApps" :key="app.id" class="app-list-item" @click="useApp(app)">
          <div class="app-icon" :style="{ background: getColor(app.id) }">
            <el-icon :size="20"><Cpu /></el-icon>
          </div>
          <div class="app-list-info">
            <div class="app-list-name">
              {{ app.name }}
              <el-tag size="small" :type="catTagType(app.category)" style="margin-left: 8px">{{ getCategoryName(app.category) }}</el-tag>
            </div>
            <div class="app-list-desc">{{ app.description }}</div>
            <div class="app-list-meta">
              <span>模型：{{ app.model }}</span>
              <span>知识库：{{ app.knowledge }} 个</span>
              <span>工具：{{ app.tools }} 个</span>
            </div>
          </div>
          <div class="app-list-stats">
            <div class="app-stat-item">
              <div class="app-stat-value">{{ formatCalls(app.callsMonth) }}</div>
              <div class="app-stat-label">调用量</div>
            </div>
            <div class="app-stat-item">
              <div class="app-stat-value" style="color: var(--dp-warning)">{{ app.satisfaction.toFixed(1) }}</div>
              <div class="app-stat-label">满意度</div>
            </div>
          </div>
          <el-button type="primary" size="default">立即使用</el-button>
        </div>
      </div>

      <el-pagination v-model:current-page="page" :page-size="pageSize" :total="filteredApps.length" layout="prev, pager, next" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { agents } from '@/mock'
import { List, Search, View } from '@element-plus/icons-vue'

const colors = ['#165dff', '#00b42a', '#722ed1', '#ff7d00', '#f759ab', '#0fc6c2', '#14c9c9', '#f53f3f']

const categories = [
  { key: 'all', name: '全部', icon: 'Grid', count: 12 },
  { key: 'query', name: '数据查询', icon: 'Search', count: 4 },
  { key: 'business', name: '业务助手', icon: 'ChatDotRound', count: 3 },
  { key: 'analysis', name: '分析助手', icon: 'Histogram', count: 3 },
  { key: 'document', name: '文档处理', icon: 'Document', count: 2 }
]

const activeCategory = ref('all')
const keyword = ref('')
const viewMode = ref('grid')
const sortBy = ref('hot')
const page = ref(1)
const pageSize = ref(8)

const catMap = {
  '数据查询助手-1': 'query', '数据查询助手-2': 'query', 'SQL开发助手-4': 'query', '标准咨询助手-5': 'query',
  '智能客服-7': 'business', '经营分析助手-8': 'business', '指标问答助手-2': 'business',
  '质量分析助手-3': 'analysis', '报表解读助手-6': 'analysis', '经营分析助手-8': 'analysis'
}

const descMap = {
  '数据查询助手': '智能理解自然语言查询需求，自动生成 SQL 并返回结果',
  '指标问答助手': '快速回答业务指标相关问题，支持钻取和对比分析',
  '质量分析助手': '自动分析数据质量问题，定位根因并给出优化建议',
  'SQL开发助手': '辅助编写和优化 SQL 代码，支持多种数据库方言',
  '标准咨询助手': '解答数据标准相关问题，提供标准查询和映射服务',
  '报表解读助手': '智能解读报表数据，自动生成分析报告和洞察',
  '智能客服': '7x24 小时智能客服，解答常见问题并引导人工服务',
  '经营分析助手': '深度分析经营数据，提供决策支持和趋势预测'
}

const apps = computed(() => agents.map(a => {
  const baseName = a.name.replace(/-\d+$/, '')
  return {
    ...a,
    category: catMap[a.name] || (Math.random() > 0.5 ? 'document' : 'business'),
    description: descMap[baseName] || '智能体应用，助力业务效率提升'
  }
}))

const hotApps = computed(() => [...apps.value].sort((a, b) => b.callsMonth - a.callsMonth).slice(0, 5))

const filteredApps = computed(() => {
  let list = apps.value.filter(a =>
    (!keyword.value || a.name.includes(keyword.value) || a.description.includes(keyword.value)) &&
    (activeCategory.value === 'all' || a.category === activeCategory.value)
  )
  if (sortBy.value === 'hot') list.sort((a, b) => b.callsMonth - a.callsMonth)
  else if (sortBy.value === 'rating') list.sort((a, b) => b.satisfaction - a.satisfaction)
  return list
})

function getColor(id) {
  return colors[Math.abs(id) % colors.length]
}

function getCategoryName(key) {
  const cat = categories.find(c => c.key === key)
  return cat ? cat.name : '其他'
}

function catTagType(cat) {
  return { query: 'primary', business: 'success', analysis: 'warning', document: 'info' }[cat] || 'info'
}

function formatCalls(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toLocaleString()
}

function useApp(app) {
  ElMessage.success(`正在打开应用「${app.name}」...`)
}
</script>

<style scoped>
.square-page { min-height: 100%; }

.square-search-card {
  background: linear-gradient(135deg, #e8f0ff 0%, #f5f9ff 50%, #f5e8ff 100%);
  border: none;
}
.square-search {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}
.square-search :deep(.el-input__wrapper) {
  box-shadow: 0 2px 8px rgba(22, 93, 255, 0.15);
}
.square-categories {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}
.square-cat {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #fff;
  border-radius: 20px;
  font-size: 13px;
  color: var(--dp-text-2);
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--dp-border-light);
}
.square-cat:hover {
  color: var(--dp-primary);
  border-color: var(--dp-primary);
}
.square-cat.active {
  background: var(--dp-primary);
  color: #fff;
  border-color: var(--dp-primary);
}
.square-cat-count {
  font-size: 11px;
  opacity: 0.7;
}

.hot-list { display: flex; flex-direction: column; gap: 8px; }
.hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid var(--dp-border-light);
}
.hot-item:hover {
  background: var(--dp-primary-bg);
  border-color: var(--dp-primary);
}
.hot-rank {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: #c9cdd4;
  flex-shrink: 0;
}
.hot-rank.rank-1 { background: linear-gradient(135deg, #ff7d00, #f53f3f); }
.hot-rank.rank-2 { background: linear-gradient(135deg, #ff9d2e, #ff7d00); }
.hot-rank.rank-3 { background: linear-gradient(135deg, #f7ba1e, #ff9d2e); }
.hot-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.hot-info { flex: 1; min-width: 0; }
.hot-name { font-size: 14px; font-weight: 600; color: var(--dp-text-1); }
.hot-desc { font-size: 12px; color: var(--dp-text-3); margin-top: 2px; }

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
  margin-bottom: 12px;
}
.app-card {
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: 10px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.25s;
  display: flex;
  flex-direction: column;
}
.app-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 6px 16px rgba(22, 93, 255, 0.1);
  transform: translateY(-3px);
}
.app-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 12px;
}
.app-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.app-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 6px;
}
.app-desc {
  font-size: 12px;
  color: var(--dp-text-3);
  line-height: 1.5;
  min-height: 36px;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.app-stats {
  display: flex;
  gap: 16px;
  padding: 10px 0;
  border-top: 1px dashed var(--dp-border-light);
  border-bottom: 1px dashed var(--dp-border-light);
  margin-bottom: 12px;
}
.app-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--dp-text-2);
}
.app-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
}
.app-creator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--dp-text-3);
}

.app-list { display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px; }
.app-list-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--dp-border-light);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.app-list-item:hover {
  border-color: var(--dp-primary);
  background: var(--dp-primary-bg);
}
.app-list-info { flex: 1; min-width: 0; }
.app-list-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 4px;
}
.app-list-desc {
  font-size: 13px;
  color: var(--dp-text-3);
  margin-bottom: 6px;
}
.app-list-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--dp-text-3);
}
.app-list-stats {
  display: flex;
  gap: 24px;
  text-align: center;
}
.app-stat-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', sans-serif;
}
.app-stat-label {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-top: 2px;
}
</style>
