<template>
  <div class="dp-page">
    <PageHeader title="共享文档" desc="管理与他人共享的文档，包括共享给我的和我共享的文档">
      <el-button type="primary" :icon="Share">共享文档</el-button>
    </PageHeader>

    <div class="dp-card">
      <div class="dc-tabs-header">
        <el-tabs v-model="activeTab" class="dc-main-tabs">
          <el-tab-pane label="共享给我的" name="received" />
          <el-tab-pane label="我共享的" name="shared" />
        </el-tabs>
      </div>

      <div class="dp-toolbar">
        <el-input v-model="keyword" placeholder="搜索文档名称" clearable style="width: 220px" :prefix-icon="Search" />
        <el-select v-model="typeFilter" placeholder="类型" clearable style="width: 120px">
          <el-option v-for="t in docTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="folderFilter" placeholder="分类" clearable style="width: 130px">
          <el-option v-for="f in folders" :key="f" :label="f" :value="f" />
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
      </div>

      <!-- 列表视图 -->
      <div v-if="viewMode === 'list'">
        <el-table :data="pagedData" border stripe v-loading="loading">
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
          <el-table-column v-if="activeTab === 'received'" prop="owner" label="共享人" width="100" />
          <el-table-column v-else prop="sharedTo" label="共享给" width="120">
            <template #default="{ row }">
              <el-avatar-group :size="20">
                <el-avatar v-for="(u, i) in row.sharedTo.slice(0, 3)" :key="i" :style="{ background: avatarColor(u) }">{{ u.charAt(0) }}</el-avatar>
                <el-avatar v-if="row.sharedTo.length > 3">+{{ row.sharedTo.length - 3 }}</el-avatar>
              </el-avatar-group>
            </template>
          </el-table-column>
          <el-table-column label="权限" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="permissionType(row.permission)" effect="plain">{{ row.permission }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sharedAt" label="共享时间" width="160" />
          <el-table-column prop="views" label="浏览量" width="90" align="right" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="handlePreview(row)">预览</el-button>
              <el-button link type="primary" size="small" @click="handleComment(row)">评论</el-button>
              <el-button v-if="activeTab === 'shared'" link type="danger" size="small" @click="handleCancelShare(row)">取消共享</el-button>
              <el-button v-else link type="primary" size="small" @click="handleFavorite(row)">收藏</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 卡片视图 -->
      <div v-else class="dc-card-grid">
        <div v-for="doc in pagedData" :key="doc.id" class="dc-doc-card dp-fade-up">
          <div class="dc-card-icon" :class="'card-' + getTypeClass(doc.type)">
            <el-icon :size="32"><component :is="getTypeIcon(doc.type)" /></el-icon>
            <div class="dc-shared-badge">
              <el-tag size="small" type="success" effect="dark">
                <el-icon><Share /></el-icon>
                {{ activeTab === 'received' ? '共享给我' : '我共享' }}
              </el-tag>
            </div>
          </div>
          <div class="dc-card-body">
            <div class="dc-card-title dp-ellipsis" :title="doc.title">{{ doc.title }}</div>
            <div class="dc-card-meta">
              <el-tag size="small" effect="plain">{{ doc.type }}</el-tag>
              <span class="dc-card-size">{{ formatSize(doc.sizeKB) }}</span>
            </div>
            <div class="dc-card-owner">
              <el-icon><User /></el-icon>
              <span>{{ activeTab === 'received' ? doc.owner : ('共享给 ' + doc.sharedTo.length + ' 人') }}</span>
            </div>
            <div class="dc-card-footer">
              <span><el-icon><View /></el-icon> {{ doc.views }}</span>
              <span>{{ doc.sharedAt.slice(5, 10) }}</span>
            </div>
            <div class="dc-card-actions">
              <el-button type="primary" size="small" :icon="View">预览</el-button>
              <el-button size="small" :icon="Edit">编辑</el-button>
              <el-dropdown>
                <el-button size="small">
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :icon="ChatDotRound">评论</el-dropdown-item>
                    <el-dropdown-item :icon="Star">收藏</el-dropdown-item>
                    <el-dropdown-item v-if="activeTab === 'shared'" divided :icon="Delete" style="color: #f53f3f">取消共享</el-dropdown-item>
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
        :total="filteredData.length"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { documents, owners } from '@/mock'
import { ChatDotRound, Delete, Edit, Search, Share, Star, View } from '@element-plus/icons-vue'

const keyword = ref('')
const typeFilter = ref('')
const folderFilter = ref('')
const viewMode = ref('list')
const currentPage = ref(1)
const pageSize = ref(10)
const activeTab = ref('received')
const loading = ref(false)

const currentUser = owners[0]

const docTypes = ['Word', 'Excel', 'PPT', 'PDF', 'Markdown', '思维笔记']
const folders = ['操作手册', '实施方案', '规范制度', '培训资料', '会议纪要']

const receivedDocs = computed(() =>
  documents.filter(d => d.shared === '共享' && d.owner !== currentUser).map(d => ({
    ...d,
    permission: ['可编辑', '仅查看', '可评论'][d.id % 3],
    sharedAt: d.updatedAt
  }))
)

const sharedDocs = computed(() =>
  documents.filter(d => d.owner === currentUser).map(d => ({
    ...d,
    permission: ['可编辑', '仅查看', '可评论'][d.id % 3],
    sharedAt: d.updatedAt,
    sharedTo: owners.slice(0, (d.id % 5) + 1)
  }))
)

const currentList = computed(() => activeTab.value === 'received' ? receivedDocs.value : sharedDocs.value)

const filteredData = computed(() =>
  currentList.value.filter(d =>
    (!keyword.value || d.title.includes(keyword.value)) &&
    (!typeFilter.value || d.type === typeFilter.value) &&
    (!folderFilter.value || d.folder === folderFilter.value)
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

const permissionType = p => {
  const map = { '可编辑': 'success', '仅查看': 'info', '可评论': 'warning' }
  return map[p] || 'info'
}

function formatSize(kb) {
  if (kb < 1024) return kb + ' KB'
  return (kb / 1024).toFixed(1) + ' MB'
}

function avatarColor(name) {
  const colors = ['#165dff', '#00b42a', '#ff7d00', '#722ed1', '#0fc6c2', '#f759ab', '#f53f3f']
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

function handlePreview(doc) {
  ElMessage.info(`正在预览「${doc.title}」`)
}

function handleComment(doc) {
  ElMessage.info(`打开「${doc.title}」评论区`)
}

function handleFavorite(doc) {
  ElMessage.success(`已收藏「${doc.title}」`)
}

function handleCancelShare(doc) {
  ElMessageBox.confirm(`确定要取消共享「${doc.title}」吗？`, '取消共享确认', {
    confirmButtonText: '确认取消',
    cancelButtonText: '保留共享',
    type: 'warning'
  }).then(() => {
    ElMessage.success('已取消共享')
  }).catch(() => {})
}
</script>

<style scoped>
.dc-tabs-header {
  margin: -18px -18px 0;
  padding: 0 18px;
  border-bottom: 1px solid var(--dp-border-light);
}

.dc-main-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
  height: 48px;
  line-height: 48px;
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
  position: relative;
}

.dc-card-icon.card-word { background: linear-gradient(135deg, #165dff, #4080ff); }
.dc-card-icon.card-excel { background: linear-gradient(135deg, #00b42a, #23c343); }
.dc-card-icon.card-ppt { background: linear-gradient(135deg, #ff7d00, #ff9a2e); }
.dc-card-icon.card-pdf { background: linear-gradient(135deg, #f53f3f, #ff7875); }
.dc-card-icon.card-md { background: linear-gradient(135deg, #722ed1, #9254de); }
.dc-card-icon.card-note { background: linear-gradient(135deg, #0fc6c2, #3ddbd9); }

.dc-shared-badge {
  position: absolute;
  top: 10px;
  right: 10px;
}

.dc-shared-badge .el-icon {
  vertical-align: -1px;
  margin-right: 2px;
}

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
  margin-bottom: 8px;
}

.dc-card-size {
  font-size: 12px;
  color: var(--dp-text-3);
}

.dc-card-owner {
  font-size: 12px;
  color: var(--dp-text-2);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.dc-card-footer {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--dp-text-3);
  padding-top: 10px;
  border-top: 1px solid var(--dp-border-light);
  align-items: center;
  margin-bottom: 12px;
}

.dc-card-footer .el-icon {
  vertical-align: -2px;
}

.dc-card-actions {
  display: flex;
  gap: 6px;
}

.dc-card-actions .el-button:first-child {
  flex: 1;
}

@media (max-width: 1200px) {
  .dc-card-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 900px) {
  .dc-card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
