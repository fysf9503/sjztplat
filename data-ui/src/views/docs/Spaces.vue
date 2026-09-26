<template>
  <div class="dp-page">
    <PageHeader title="文档空间" desc="按团队和项目组织的文档空间，支持成员管理和权限配置">
      <el-button type="primary" :icon="Plus" @click="openCreate">创建空间</el-button>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid dp-stat-grid-4">
      <StatCard label="空间总数" :value="spaceList.length" unit="个" icon="OfficeBuilding" color="#165dff" bg="#e8f3ff" :trend="3" />
      <StatCard label="我管理的" :value="myManagedCount" unit="个" icon="Setting" color="#00b42a" bg="#e8ffea" />
      <StatCard label="文档总数" :value="totalDocs" unit="份" icon="Document" color="#ff7d00" bg="#fff3e8" :trend="8" />
      <StatCard label="成员总数" :value="totalMembers" unit="人" icon="User" color="#722ed1" bg="#f5e8ff" />
    </div>

    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="keyword" placeholder="搜索空间名称" clearable style="width: 220px" :prefix-icon="Search" />
        <el-select v-model="roleFilter" placeholder="我的角色" clearable style="width: 140px">
          <el-option label="管理员" value="admin" />
          <el-option label="成员" value="member" />
          <el-option label="访客" value="visitor" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="handleRefresh">刷新</el-button>
        </div>
      </div>

      <div v-loading="loading" class="dc-space-grid">
        <div
          v-for="space in filteredSpaces"
          :key="space.id"
          class="dc-space-card dp-fade-up"
        >
          <div class="dc-space-header" :class="'space-bg-' + space.id % 6">
            <div class="dc-space-icon">
              <el-icon :size="28"><component :is="space.icon" /></el-icon>
            </div>
            <div class="dc-space-info">
              <div class="dc-space-name">{{ space.name }}</div>
              <div class="dc-space-role">
                <el-tag size="small" :type="roleTagType(space.myRole)" effect="dark">{{ space.myRole }}</el-tag>
              </div>
            </div>
          </div>
          <div class="dc-space-body">
            <div class="dc-space-desc">{{ space.desc }}</div>
            <div class="dc-space-stats">
              <div class="dc-stat-item">
                <el-icon color="#165dff"><Document /></el-icon>
                <span class="dc-stat-value">{{ space.docs }}</span>
                <span class="dc-stat-label">文档</span>
              </div>
              <div class="dc-stat-divider"></div>
              <div class="dc-stat-item">
                <el-icon color="#00b42a"><User /></el-icon>
                <span class="dc-stat-value">{{ space.members }}</span>
                <span class="dc-stat-label">成员</span>
              </div>
              <div class="dc-stat-divider"></div>
              <div class="dc-stat-item">
                <el-icon color="#ff7d00"><Folder /></el-icon>
                <span class="dc-stat-value">{{ space.folders }}</span>
                <span class="dc-stat-label">目录</span>
              </div>
            </div>
            <div class="dc-space-admin">
              <el-icon><Star /></el-icon>
              <span>管理员：{{ space.admin }}</span>
            </div>
          </div>
          <div class="dc-space-footer">
            <el-button type="primary" :icon="View" @click="handleEnter(space)">进入空间</el-button>
            <el-dropdown>
              <el-button>
                <el-icon><MoreFilled /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :icon="User" @click="handleMembers(space)">成员管理</el-dropdown-item>
                  <el-dropdown-item :icon="Setting" @click="handleSettings(space)">空间设置</el-dropdown-item>
                  <el-dropdown-item divided :icon="Delete" style="color: #f53f3f" v-if="space.myRole === '管理员'" @click="handleDelete(space)">
                    删除空间
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="filteredSpaces.length"
        :page-sizes="[6, 12, 24]"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>

    <!-- 创建空间弹窗 -->
    <el-dialog v-model="createVisible" title="创建文档空间" width="520px" destroy-on-close>
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="空间名称" required>
          <el-input v-model="createForm.name" placeholder="请输入空间名称" />
        </el-form-item>
        <el-form-item label="空间图标">
          <el-radio-group v-model="createForm.icon">
            <el-radio value="OfficeBuilding"><el-icon><OfficeBuilding /></el-icon></el-radio>
            <el-radio value="DataLine"><el-icon><DataLine /></el-icon></el-radio>
            <el-radio value="ShoppingCart"><el-icon><ShoppingCart /></el-icon></el-radio>
            <el-radio value="Wallet"><el-icon><Wallet /></el-icon></el-radio>
            <el-radio value="TrendCharts"><el-icon><TrendCharts /></el-icon></el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="可见范围">
          <el-radio-group v-model="createForm.visibility">
            <el-radio value="public">公开（全员可见）</el-radio>
            <el-radio value="private">私有（仅受邀成员）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="空间描述">
          <el-input v-model="createForm.desc" type="textarea" :rows="3" placeholder="请输入空间描述" />
        </el-form-item>
        <el-form-item label="初始管理员">
          <el-select v-model="createForm.admin" placeholder="请选择管理员" style="width: 100%">
            <el-option v-for="o in owners" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { owners } from '@/mock'
import { Delete, Plus, Refresh, Search, Setting, User, View } from '@element-plus/icons-vue'

const keyword = ref('')
const roleFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(6)
const loading = ref(false)
const createVisible = ref(false)

const createForm = reactive({
  name: '',
  icon: 'OfficeBuilding',
  visibility: 'private',
  desc: '',
  admin: owners[0]
})

const spaceList = [
  { id: 1, name: '数据治理空间', icon: 'DataLine', desc: '数据治理相关文档，包括标准、规范、质量等主题文档', docs: 48, members: 12, folders: 8, admin: '张伟', myRole: '管理员' },
  { id: 2, name: '数据分析空间', icon: 'TrendCharts', desc: '数据分析报告和方案，各业务线分析成果共享', docs: 36, members: 18, folders: 6, admin: '李娜', myRole: '成员' },
  { id: 3, name: '项目管理空间', icon: 'FolderOpened', desc: '项目管理过程文档，包括计划、进度、会议纪要等', docs: 24, members: 8, folders: 5, admin: '王强', myRole: '成员' },
  { id: 4, name: '财务共享空间', icon: 'Wallet', desc: '财务制度规范、报销流程、预算管理等财务文档', docs: 32, members: 6, folders: 7, admin: '赵敏', myRole: '访客' },
  { id: 5, name: '运营中心空间', icon: 'Promotion', desc: '运营相关文档，包括活动方案、运营数据、用户运营等', docs: 28, members: 15, folders: 6, admin: '孙磊', myRole: '管理员' },
  { id: 6, name: '技术研发空间', icon: 'Cpu', desc: '技术架构、开发规范、接口文档、技术分享等', docs: 56, members: 22, folders: 10, admin: '周婷', myRole: '成员' },
  { id: 7, name: '人力资源空间', icon: 'User', desc: '人事制度、培训资料、招聘信息、组织架构等', docs: 18, members: 5, folders: 4, admin: '吴昊', myRole: '访客' },
  { id: 8, name: '市场营销空间', icon: 'ShoppingCart', desc: '市场推广方案、品牌资料、竞品分析、营销案例等', docs: 22, members: 10, folders: 5, admin: '郑洁', myRole: '成员' }
]

const filteredSpaces = computed(() =>
  spaceList.filter(s =>
    (!keyword.value || s.name.includes(keyword.value)) &&
    (!roleFilter.value || s.myRole === roleMap[roleFilter.value])
  )
)

const roleMap = { admin: '管理员', member: '成员', visitor: '访客' }

const myManagedCount = computed(() => spaceList.filter(s => s.myRole === '管理员').length)
const totalDocs = computed(() => spaceList.reduce((sum, s) => sum + s.docs, 0))
const totalMembers = computed(() => spaceList.reduce((sum, s) => sum + s.members, 0))

function roleTagType(role) {
  const map = { '管理员': 'danger', '成员': 'primary', '访客': 'info' }
  return map[role] || 'info'
}

function handleRefresh() {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('刷新成功')
  }, 600)
}

function handleEnter(space) {
  ElMessage.info(`进入空间「${space.name}」`)
}

function handleMembers(space) {
  ElMessage.info(`打开「${space.name}」成员管理`)
}

function handleSettings(space) {
  ElMessage.info(`打开「${space.name}」空间设置`)
}

function openCreate() {
  createForm.name = ''
  createForm.icon = 'OfficeBuilding'
  createForm.visibility = 'private'
  createForm.desc = ''
  createForm.admin = owners[0]
  createVisible.value = true
}

function handleCreate() {
  if (!createForm.name) {
    ElMessage.warning('请输入空间名称')
    return
  }
  ElMessage.success('空间创建成功')
  createVisible.value = false
}

function handleDelete(space) {
  ElMessageBox.confirm(`确定要删除空间「${space.name}」吗？删除后所有文档将无法恢复。`, '删除确认', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    ElMessage.success('空间已删除')
  }).catch(() => {})
}
</script>

<style scoped>
.dp-stat-grid-4 {
  grid-template-columns: repeat(4, 1fr);
}

.dc-space-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.dc-space-card {
  border: 1px solid var(--dp-border-light);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  transition: all 0.25s;
}

.dc-space-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 6px 20px rgba(22, 93, 255, 0.12);
  transform: translateY(-3px);
}

.dc-space-header {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  color: #fff;
}

.dc-space-header.space-bg-1 { background: linear-gradient(135deg, #165dff 0%, #4080ff 100%); }
.dc-space-header.space-bg-2 { background: linear-gradient(135deg, #00b42a 0%, #23c343 100%); }
.dc-space-header.space-bg-3 { background: linear-gradient(135deg, #722ed1 0%, #9254de 100%); }
.dc-space-header.space-bg-4 { background: linear-gradient(135deg, #ff7d00 0%, #ff9a2e 100%); }
.dc-space-header.space-bg-5 { background: linear-gradient(135deg, #0fc6c2 0%, #3ddbd9 100%); }
.dc-space-header.space-bg-0 { background: linear-gradient(135deg, #f53f3f 0%, #ff7875 100%); }

.dc-space-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dc-space-info {
  flex: 1;
  min-width: 0;
}

.dc-space-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
}

.dc-space-body {
  padding: 16px 20px;
}

.dc-space-desc {
  font-size: 13px;
  color: var(--dp-text-3);
  line-height: 1.5;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 39px;
}

.dc-space-stats {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-top: 1px solid var(--dp-border-light);
  border-bottom: 1px solid var(--dp-border-light);
  margin-bottom: 12px;
}

.dc-stat-item {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.dc-stat-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', sans-serif;
}

.dc-stat-label {
  font-size: 11px;
  color: var(--dp-text-3);
}

.dc-stat-divider {
  width: 1px;
  height: 32px;
  background: var(--dp-border-light);
}

.dc-space-admin {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--dp-text-3);
}

.dc-space-admin .el-icon {
  color: var(--dp-warning);
}

.dc-space-footer {
  display: flex;
  gap: 8px;
  padding: 0 20px 16px;
}

.dc-space-footer .el-button:first-child {
  flex: 1;
}

@media (max-width: 1200px) {
  .dp-stat-grid-4 {
    grid-template-columns: repeat(2, 1fr);
  }
  .dc-space-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .dc-space-grid {
    grid-template-columns: 1fr;
  }
}
</style>
