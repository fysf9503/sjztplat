<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">TEMPLATE CENTER</div>
      <h1 class="dp-page-title">主题模板中心</h1>
      <p class="dp-page-desc">丰富的仪表板主题模板，一键应用，快速构建专业报表。</p>
    </div>

    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="keyword" placeholder="搜索模板" clearable style="width: 220px" :prefix-icon="Search" />
        <el-select v-model="categoryFilter" placeholder="分类" clearable style="width: 140px">
          <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button type="primary" :icon="Plus">新建模板</el-button>
        </div>
      </div>

      <div class="rp-template-grid">
        <div
          v-for="(tpl, idx) in pagedTemplates"
          :key="tpl.id"
          class="rp-template-card dp-fade-up"
        >
          <div class="rp-tpl-preview" :class="'tpl-theme-' + (idx % 6)">
            <div class="tpl-mock-layout">
              <div class="tpl-mock-header">
                <div class="tpl-mock-title"></div>
                <div class="tpl-mock-tabs">
                  <span></span><span></span><span></span>
                </div>
              </div>
              <div class="tpl-mock-body">
                <div class="tpl-mock-chart chart-a"></div>
                <div class="tpl-mock-chart chart-b"></div>
                <div class="tpl-mock-chart chart-c"></div>
                <div class="tpl-mock-chart chart-d"></div>
              </div>
            </div>
            <div class="rp-tpl-badge" v-if="tpl.isHot">
              <el-tag size="small" type="danger" effect="dark">热门</el-tag>
            </div>
          </div>
          <div class="rp-tpl-body">
            <div class="rp-tpl-name-row">
              <div class="rp-tpl-name dp-ellipsis" :title="tpl.name">{{ tpl.name }}</div>
              <el-tag size="small" :type="statusType(tpl.status)" effect="plain">{{ tpl.status }}</el-tag>
            </div>
            <div class="rp-tpl-desc">{{ tpl.desc }}</div>
            <div class="rp-tpl-meta">
              <span>分类：{{ tpl.category }}</span>
              <span>使用：{{ tpl.usageCount }} 次</span>
            </div>
            <div class="rp-tpl-footer">
              <span class="rp-tpl-author">
                <el-icon><User /></el-icon>
                {{ tpl.owner }}
              </span>
              <span class="rp-tpl-date">{{ tpl.updatedAt }}</span>
            </div>
            <div class="rp-tpl-actions">
              <el-button type="primary" size="small" :icon="CopyDocument">复制使用</el-button>
              <el-button size="small" :icon="Edit">编辑</el-button>
              <el-dropdown>
                <el-button size="small">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :icon="Promotion">发布</el-dropdown-item>
                    <el-dropdown-item :icon="View">预览</el-dropdown-item>
                    <el-dropdown-item :icon="Download">导出</el-dropdown-item>
                    <el-dropdown-item divided :icon="SwitchButton">停用</el-dropdown-item>
                    <el-dropdown-item :icon="Delete" style="color: #f53f3f">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </div>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="filteredTemplates.length"
        :page-sizes="[6, 12, 24]"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { genRows, pick, int, dateStr, owners } from '@/mock/helpers'
import { CopyDocument, Delete, Download, Edit, Plus, Promotion, Search, SwitchButton, View } from '@element-plus/icons-vue'

const keyword = ref('')
const categoryFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(6)

const categories = ['经营分析', '销售专题', '会员分析', '财务总览', '供应链', '运营监控']

const templates = genRows(12, i => ({
  id: 1000 + i,
  name: pick(['极简商务模板', '科技蓝模板', '深色大屏模板', '清新绿主题', '暖橙商务风', '紫色梦幻', '极简白模板', '政府蓝主题', '金融风控模板', '制造业看板', '零售分析模板', '教育数据看板']) + '-V' + int(1, 3),
  category: pick(categories),
  desc: pick(['适合高层驾驶舱，核心指标一目了然', '多维度分析模板，支持钻取联动', '大屏展示专用，炫酷数据可视化', '日常运营监控，实时数据刷新', '专题分析模板，深度数据洞察']),
  status: pick(['已发布', '已发布', '已发布', '停用']),
  owner: pick(owners),
  usageCount: int(10, 500),
  isHot: i < 3,
  updatedAt: dateStr(int(0, 60))
}))

const filteredTemplates = computed(() =>
  templates.filter(t =>
    (!keyword.value || t.name.includes(keyword.value)) &&
    (!categoryFilter.value || t.category === categoryFilter.value)
  )
)

const pagedTemplates = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredTemplates.value.slice(start, start + pageSize.value)
})

const statusType = s => ({ 已发布: 'success', 停用: 'info' }[s] || 'info')
</script>

<style scoped>
.rp-template-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.rp-template-card {
  border: 1px solid var(--dp-border-light);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  transition: all 0.25s;
}

.rp-template-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 6px 20px rgba(22, 93, 255, 0.12);
  transform: translateY(-3px);
}

.rp-tpl-preview {
  height: 160px;
  position: relative;
  padding: 10px;
  overflow: hidden;
}

.rp-tpl-preview.tpl-theme-0 { background: linear-gradient(135deg, #e8f3ff 0%, #f0f7ff 100%); }
.rp-tpl-preview.tpl-theme-1 { background: linear-gradient(135deg, #1d2129 0%, #4e5969 100%); }
.rp-tpl-preview.tpl-theme-2 { background: linear-gradient(135deg, #e8ffea 0%, #f0fff4 100%); }
.rp-tpl-preview.tpl-theme-3 { background: linear-gradient(135deg, #fff3e8 0%, #fffaf5 100%); }
.rp-tpl-preview.tpl-theme-4 { background: linear-gradient(135deg, #f5e8ff 0%, #faf5ff 100%); }
.rp-tpl-preview.tpl-theme-5 { background: linear-gradient(135deg, #e0fffa 0%, #f0fffd 100%); }

.tpl-mock-layout {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tpl-mock-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.tpl-mock-title {
  width: 80px;
  height: 12px;
  background: rgba(22, 93, 255, 0.3);
  border-radius: 2px;
}

.tpl-theme-1 .tpl-mock-title { background: rgba(255, 255, 255, 0.3); }

.tpl-mock-tabs {
  display: flex;
  gap: 4px;
}

.tpl-mock-tabs span {
  width: 30px;
  height: 8px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

.tpl-theme-1 .tpl-mock-tabs span { background: rgba(255, 255, 255, 0.2); }

.tpl-mock-body {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: 6px;
}

.tpl-mock-chart {
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.5);
  position: relative;
}

.tpl-theme-1 .tpl-mock-chart { background: rgba(255, 255, 255, 0.1); }

.chart-a { grid-column: 1 / 2; grid-row: 1 / 2; background: rgba(22, 93, 255, 0.2); }
.chart-b { grid-column: 2 / 3; grid-row: 1 / 2; background: rgba(0, 180, 42, 0.2); }
.chart-c { grid-column: 1 / 2; grid-row: 2 / 3; background: rgba(255, 125, 0, 0.2); }
.chart-d { grid-column: 2 / 3; grid-row: 2 / 3; background: rgba(114, 46, 209, 0.2); }

.rp-tpl-badge {
  position: absolute;
  top: 10px;
  right: 10px;
}

.rp-tpl-body {
  padding: 14px 16px;
}

.rp-tpl-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 6px;
}

.rp-tpl-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
  flex: 1;
}

.rp-tpl-desc {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 8px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rp-tpl-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--dp-border-light);
}

.rp-tpl-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 12px;
}

.rp-tpl-author {
  display: flex;
  align-items: center;
  gap: 4px;
}

.rp-tpl-actions {
  display: flex;
  gap: 8px;
}

.rp-tpl-actions .el-button:first-child {
  flex: 1;
}

@media (max-width: 1200px) {
  .rp-template-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .rp-template-grid {
    grid-template-columns: 1fr;
  }
}
</style>
