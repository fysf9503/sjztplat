<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">DOC TEMPLATES</div>
      <h1 class="dp-page-title">模板中心</h1>
      <p class="dp-page-desc">丰富的文档模板库，涵盖操作手册、实施方案、规范制度、培训资料等分类。</p>
    </div>

    <div class="dp-card">
      <div class="dp-toolbar">
        <div class="dc-category-tabs">
          <el-tag
            v-for="cat in categories"
            :key="cat.value"
            :type="activeCategory === cat.value ? 'primary' : 'info'"
            :effect="activeCategory === cat.value ? 'dark' : 'plain'"
            class="category-tag"
            @click="activeCategory = cat.value"
          >
            <el-icon><component :is="cat.icon" /></el-icon>
            <span style="margin-left: 4px">{{ cat.label }}</span>
          </el-tag>
        </div>
        <el-input v-model="keyword" placeholder="搜索模板" clearable style="width: 200px" :prefix-icon="Search" />
        <div class="dp-toolbar-right">
          <el-button type="primary" :icon="Plus">新建模板</el-button>
        </div>
      </div>

      <div class="dc-tpl-grid">
        <div
          v-for="(tpl, idx) in pagedTemplates"
          :key="tpl.id"
          class="dc-tpl-card dp-fade-up"
        >
          <div class="dc-tpl-cover" :class="'tpl-' + getTypeClass(tpl.type)">
            <div class="dc-tpl-icon">
              <el-icon :size="36"><component :is="getTypeIcon(tpl.type)" /></el-icon>
            </div>
            <div class="dc-tpl-type-tag">
              <el-tag size="small" effect="dark">{{ tpl.type }}</el-tag>
            </div>
          </div>
          <div class="dc-tpl-body">
            <div class="dc-tpl-name dp-ellipsis" :title="tpl.name">{{ tpl.name }}</div>
            <div class="dc-tpl-desc">{{ tpl.desc }}</div>
            <div class="dc-tpl-stats">
              <span>
                <el-icon><View /></el-icon>
                {{ tpl.views }} 次浏览'
              </span>
              <span>
                <el-icon><CopyDocument /></el-icon>
                {{ tpl.usageCount }} 次使用'
              </span>
            </div>
            <div class="dc-tpl-footer">
              <span class="dc-tpl-cat">{{ tpl.category }}</span>
              <span class="dc-tpl-author">
                <el-icon><User /></el-icon>
                {{ tpl.owner }}
              </span>
            </div>
            <div class="dc-tpl-actions">
              <el-button type="primary" size="small" :icon="CopyDocument">使用模板</el-button>
              <el-button size="small" :icon="View">预览</el-button>
              <el-button size="small" :icon="Star">收藏</el-button>
            </div>
          </div>
        </div>
      </div>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="filteredTemplates.length"
        :page-sizes="[8, 16, 32]"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { genRows, pick, int, dateStr, owners } from '@/mock/helpers'
import { CopyDocument, Plus, Search, Star, View } from '@element-plus/icons-vue'

const keyword = ref('')
const activeCategory = ref('all')
const currentPage = ref(1)
const pageSize = ref(8)

const categories = [
  { value: 'all', label: '全部', icon: 'Collection' },
  { value: '操作手册', label: '操作手册', icon: 'Guide' },
  { value: '实施方案', label: '实施方案', icon: 'Reading' },
  { value: '规范制度', label: '规范制度', icon: 'Document' },
  { value: '培训资料', label: '培训资料', icon: 'Notebook' }
]

const templates = genRows(20, i => ({
  id: 2000 + i,
  name: pick(['数据中台操作手册模板', '数据标准管理办法模板', '质量管控实施细则模板', '数据接入实施规范模板', 'API服务接入指南模板', '指标口径说明模板', '客户主题域设计文档模板', '营销活动分析方案模板', '平台部署手册模板', '权限管理规范模板', '血缘分析使用指引模板', '报表开发规范模板']) + (i < 10 ? '' : '-V' + int(2, 3)),
  type: pick(['Word', 'Excel', 'PPT', 'PDF', 'Markdown']),
  category: pick(['操作手册', '实施方案', '规范制度', '培训资料']),
  desc: pick(['标准化文档模板，直接套用即可', '包含完整章节结构和示例内容', '符合企业规范要求，开箱即用', '专业排版设计，提升文档质量', '附带详细使用说明和注意事项']),
  owner: pick(owners),
  views: int(100, 5000),
  usageCount: int(10, 500),
  updatedAt: dateStr(int(0, 90))
}))

const filteredTemplates = computed(() =>
  templates.filter(t =>
    (!keyword.value || t.name.includes(keyword.value)) &&
    (activeCategory.value === 'all' || t.category === activeCategory.value)
  )
)

const pagedTemplates = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredTemplates.value.slice(start, start + pageSize.value)
})

const getTypeClass = type => {
  const map = { Word: 'word', Excel: 'excel', PPT: 'ppt', PDF: 'pdf', Markdown: 'md' }
  return map[type] || 'default'
}

const getTypeIcon = type => {
  const map = { Word: 'Document', Excel: 'Grid', PPT: 'Presentation', PDF: 'Files', Markdown: 'EditPen' }
  return map[type] || 'Document'
}
</script>

<style scoped>
.dc-category-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.category-tag {
  cursor: pointer;
  user-select: none;
  padding: 4px 12px;
}

.dc-tpl-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.dc-tpl-card {
  border: 1px solid var(--dp-border-light);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  transition: all 0.25s;
}

.dc-tpl-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 6px 20px rgba(22, 93, 255, 0.12);
  transform: translateY(-3px);
}

.dc-tpl-cover {
  height: 110px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.dc-tpl-cover.tpl-word { background: linear-gradient(135deg, #165dff, #4080ff); }
.dc-tpl-cover.tpl-excel { background: linear-gradient(135deg, #00b42a, #23c343); }
.dc-tpl-cover.tpl-ppt { background: linear-gradient(135deg, #ff7d00, #ff9a2e); }
.dc-tpl-cover.tpl-pdf { background: linear-gradient(135deg, #f53f3f, #ff7875); }
.dc-tpl-cover.tpl-md { background: linear-gradient(135deg, #722ed1, #9254de); }

.dc-tpl-icon {
  opacity: 0.9;
}

.dc-tpl-type-tag {
  position: absolute;
  top: 10px;
  right: 10px;
}

.dc-tpl-body {
  padding: 12px 14px;
}

.dc-tpl-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 6px;
}

.dc-tpl-desc {
  font-size: 12px;
  color: var(--dp-text-3);
  line-height: 1.5;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 36px;
}

.dc-tpl-stats {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--dp-border-light);
}

.dc-tpl-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.dc-tpl-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 12px;
}

.dc-tpl-cat {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
}

.dc-tpl-author {
  display: flex;
  align-items: center;
  gap: 4px;
}

.dc-tpl-actions {
  display: flex;
  gap: 6px;
}

.dc-tpl-actions .el-button:first-child {
  flex: 1;
}

@media (max-width: 1400px) {
  .dc-tpl-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 900px) {
  .dc-tpl-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
