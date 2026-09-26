<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">MY DOCUMENTS</div>
      <h1 class="dp-page-title">我的文档</h1>
      <p class="dp-page-desc">管理我创建和拥有的所有文档。</p>
    </div>

    <div class="dc-docs-layout">
      <div class="dp-card dc-docs-tree">
        <div class="dp-card-header">
          <div class="dp-card-title">文件列表</div>
          <el-button :icon="Plus" circle size="small" />
        </div>
        <el-tree
          :data="folderTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          default-expand-all
          :expand-on-click-node="false"
          @node-click="handleFolderClick"
        >
          <template #default="{ data }">
            <span class="tree-node">
              <el-icon class="tree-icon"><component :is="data.children ? 'FolderOpened' : 'Folder'" /></el-icon>
              <span>{{ data.name }}</span>
              <span v-if="data.count !== undefined" class="tree-count">{{ data.count }}</span>
            </span>
          </template>
        </el-tree>
      </div>

      <div class="dc-docs-main">
        <div class="dp-card">
          <div class="dp-toolbar">
            <el-input v-model="keyword" placeholder="搜索文档名称" clearable style="width: 220px" :prefix-icon="Search" />
            <el-select v-model="typeFilter" placeholder="类型" clearable style="width: 120px">
              <el-option v-for="t in docTypes" :key="t" :label="t" :value="t" />
            </el-select>
            <el-radio-group v-model="viewMode" size="default">
              <el-radio-button value="list">
                <el-icon><List /></el-icon>
                <span style="margin-left: 4px">列表</span>
              </el-radio-button>
              <el-radio-button value="card">
                <el-icon><Grid /></el-icon>
                <span style="margin-left: 4px">卡片</span>
              </el-radio-button>
            </el-radio-group>
            <div class="dp-toolbar-right">
              <el-button :icon="Upload">上传</el-button>
              <el-button type="primary" :icon="Plus">新建文档</el-button>
            </div>
          </div>

          <div v-if="viewMode === 'list'">
            <el-table :data="pagedData" border stripe>
              <el-table-column type="selection" width="44" />
              <el-table-column prop="title" label="文档名称" min-width="200">
                <template #default="{ row }">
                  <div class="dc-doc-name">
                    <el-icon :class="'doc-icon-' + getTypeClass(row.type)"><component :is="getTypeIcon(row.type)" /></el-icon>
                    <span class="dp-ellipsis">{{ row.title }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="type" label="类型" width="90">
                <template #default="{ row }">
                  <el-tag size="small" effect="plain">{{ row.type }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="folder" label="分类" width="100" />
              <el-table-column label="大小" width="100" align="right">
                <template #default="{ row }">{{ formatSize(row.sizeKB) }}</template>
              </el-table-column>
              <el-table-column prop="views" label="浏览量" width="90" align="right" />
              <el-table-column prop="shared" label="共享状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.shared === '共享' ? 'success' : 'info'" size="small" effect="plain">{{ row.shared }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="updatedAt" label="更新时间" width="160" />
              <el-table-column label="操作" width="180" fixed="right">
                <template #default>
                  <el-button link type="primary" size="small">预览</el-button>
                  <el-button link type="primary" size="small">编辑</el-button>
                  <el-button link type="danger" size="small">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-else class="dc-card-grid">
            <div v-for="doc in pagedData" :key="doc.id" class="dc-doc-card dp-fade-up">
              <div class="dc-card-icon" :class="'card-' + getTypeClass(doc.type)">
                <el-icon :size="32"><component :is="getTypeIcon(doc.type)" /></el-icon>
              </div>
              <div class="dc-card-body">
                <div class="dc-card-title dp-ellipsis" :title="doc.title">{{ doc.title }}</div>
                <div class="dc-card-meta">
                  <el-tag size="small" effect="plain">{{ doc.type }}</el-tag>
                  <span class="dc-card-size">{{ formatSize(doc.sizeKB) }}</span>
                </div>
                <div class="dc-card-footer">
                  <span><el-icon><View /></el-icon> {{ doc.views }}</span>
                  <span>{{ doc.updatedAt.slice(5, 10) }}</span>
                </div>
              </div>
            </div>
          </div>

          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="filteredData.length"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { documents, owners } from '@/mock'
import { Plus, Search, Upload } from '@element-plus/icons-vue'

const keyword = ref('')
const typeFilter = ref('')
const viewMode = ref('list')
const currentPage = ref(1)
const pageSize = ref(10)
const activeFolder = ref('all')

const currentUser = owners[0]

const docTypes = ['Word', 'Excel', 'PPT', 'PDF', 'Markdown', '思维笔记']

const myDocs = computed(() => documents.filter(d => d.owner === currentUser))

const folderTree = computed(() => [
  {
    id: 'all', name: '全部文档', count: myDocs.value.length,
    children: ['操作手册', '实施方案', '规范制度', '培训资料', '会议纪要'].map(f => ({
      id: f, name: f, count: myDocs.value.filter(d => d.folder === f).length
    }))
  }
])

const filteredData = computed(() =>
  myDocs.value.filter(d =>
    (!keyword.value || d.title.includes(keyword.value)) &&
    (!typeFilter.value || d.type === typeFilter.value) &&
    (activeFolder.value === 'all' || d.folder === activeFolder.value)
  )
)

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

const getTypeClass = type => {
  const map = { Word: 'word', Excel: 'excel', PPT: 'ppt', PDF: 'pdf', Markdown: 'md', 思维笔记: 'note' }
  return map[type] || 'default'
}

const getTypeIcon = type => {
  const map = { Word: 'Document', Excel: 'Grid', PPT: 'Presentation', PDF: 'Files', Markdown: 'EditPen', 思维笔记: 'Notebook' }
  return map[type] || 'Document'
}

function formatSize(kb) {
  if (kb < 1024) return kb + ' KB'
  return (kb / 1024).toFixed(1) + ' MB'
}

function handleFolderClick(data) {
  activeFolder.value = data.id
}
</script>

<style scoped>
.dc-docs-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.dc-docs-tree {
  width: 220px;
  flex-shrink: 0;
}

.dc-docs-main {
  flex: 1;
  min-width: 0;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  flex: 1;
}

.tree-icon {
  color: var(--dp-text-3);
  flex-shrink: 0;
}

.tree-count {
  margin-left: auto;
  font-size: 11px;
  color: var(--dp-text-3);
  background: #f2f3f5;
  padding: 1px 6px;
  border-radius: 10px;
}

.dc-doc-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.doc-icon-word { color: #165dff; }
.doc-icon-excel { color: #00b42a; }
.doc-icon-ppt { color: #ff7d00; }
.doc-icon-pdf { color: #f53f3f; }
.doc-icon-md { color: #722ed1; }
.doc-icon-note { color: #0fc6c2; }

.dc-card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 16px;
}

.dc-doc-card {
  border: 1px solid var(--dp-border-light);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  transition: all 0.25s;
}

.dc-doc-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 4px 16px rgba(22, 93, 255, 0.1);
  transform: translateY(-2px);
}

.dc-card-icon {
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.dc-card-icon.card-word { background: linear-gradient(135deg, #165dff, #4080ff); }
.dc-card-icon.card-excel { background: linear-gradient(135deg, #00b42a, #23c343); }
.dc-card-icon.card-ppt { background: linear-gradient(135deg, #ff7d00, #ff9a2e); }
.dc-card-icon.card-pdf { background: linear-gradient(135deg, #f53f3f, #ff7875); }
.dc-card-icon.card-md { background: linear-gradient(135deg, #722ed1, #9254de); }
.dc-card-icon.card-note { background: linear-gradient(135deg, #0fc6c2, #3ddbd9); }

.dc-card-body {
  padding: 12px 14px;
}

.dc-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 8px;
}

.dc-card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.dc-card-size {
  font-size: 12px;
  color: var(--dp-text-3);
}

.dc-card-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--dp-text-3);
  padding-top: 10px;
  border-top: 1px solid var(--dp-border-light);
  align-items: center;
}

.dc-card-footer .el-icon {
  vertical-align: -2px;
}

@media (max-width: 1200px) {
  .dc-card-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 900px) {
  .dc-docs-layout {
    flex-direction: column;
  }
  .dc-docs-tree {
    width: 100%;
  }
  .dc-card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
