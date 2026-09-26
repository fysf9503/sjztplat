<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">PROJECT MANAGEMENT</div>
      <h1 class="dp-page-title">项目管理</h1>
      <p class="dp-page-desc">管理平台项目，支持多项目隔离和资源分配。</p>
    </div>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid">
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>项目总数</span>
          <div class="dp-stat-icon" style="background: var(--dp-primary-light); color: var(--dp-primary)">
            <el-icon :size="18"><Folder /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ total }}<span class="dp-stat-unit">个</span></div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>进行中</span>
          <div class="dp-stat-icon" style="background: var(--dp-success-light); color: var(--dp-success)">
            <el-icon :size="18"><Clock /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ ongoingCount }}<span class="dp-stat-unit">个</span></div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>总成员数</span>
          <div class="dp-stat-icon" style="background: var(--dp-warning-light); color: var(--dp-warning)">
            <el-icon :size="18"><User /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ totalMembers }}<span class="dp-stat-unit">个</span></div>
      </div>
      <div class="dp-stat-card dp-fade-up">
        <div class="dp-stat-label">
          <span>资源总数</span>
          <div class="dp-stat-icon" style="background: var(--dp-purple-light); color: var(--dp-purple)">
            <el-icon :size="18"><DataLine /></el-icon>
          </div>
        </div>
        <div class="dp-stat-value">{{ totalResources }}<span class="dp-stat-unit">个</span></div>
      </div>
    </div>

    <!-- 项目列表 -->
    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索项目名称 / 编码" clearable style="width: 240px" :prefix-icon="Search" @input="load" />
        <el-select v-model="query.status" placeholder="项目状态" clearable style="width: 130px" @change="load">
          <el-option label="进行中" value="进行中" />
          <el-option label="已验收" value="已验收" />
          <el-option label="已归档" value="已归档" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="load">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="openEdit()">新增项目</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="name" label="项目名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="code" label="项目编码" width="120" class-name="dp-mono" />
        <el-table-column prop="owner" label="负责人" width="90" />
        <el-table-column prop="members" label="成员数" width="90" sortable />
        <el-table-column prop="resources" label="资源数" width="90" sortable />
        <el-table-column label="进度" width="160">
          <template #default="{ row }">
            <div class="dp-flex dp-gap-8">
              <el-progress :percentage="row.progress" :stroke-width="8" :show-text="false" style="width: 100px" />
              <span style="font-size: 12px; color: var(--dp-text-2)">{{ row.progress }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="plain">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="manageMembers(row)">成员管理</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑项目' : '新增项目'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="项目名称" required>
          <el-input v-model="form.name" placeholder="如：数据中台一期" />
        </el-form-item>
        <el-form-item label="项目编码" required>
          <el-input v-model="form.code" placeholder="如：PRJ-101" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="form.owner" filterable style="width: 100%">
            <el-option v-for="o in ownerList" :key="o" :label="o" :value="o" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目状态">
          <el-radio-group v-model="form.status">
            <el-radio-button value="进行中">进行中</el-radio-button>
            <el-radio-button value="已验收">已验收</el-radio-button>
            <el-radio-button value="已归档">已归档</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="form.deadline" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="项目目标和范围说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 成员管理对话框 -->
    <el-dialog v-model="memberVisible" title="项目成员管理" width="520px" destroy-on-close>
      <div class="member-header">
        <span>项目：<strong>{{ currentProject?.name }}</strong></span>
        <el-button type="primary" size="small" :icon="Plus" @click="addMemberVisible = true">添加成员</el-button>
      </div>
      <el-table :data="projectMembers" border size="small">
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="role" label="项目角色" width="120">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)" size="small" effect="plain">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dept" label="部门" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="removeMember(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 添加成员对话框 -->
    <el-dialog v-model="addMemberVisible" title="添加项目成员" width="420px" destroy-on-close>
      <el-form label-width="90px">
        <el-form-item label="选择成员">
          <el-select v-model="selectedMember" filterable placeholder="搜索并选择成员" style="width: 100%">
            <el-option v-for="u in availableMembers" :key="u.id" :label="u.nickname + ' (' + u.dept + ')'" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="项目角色">
          <el-select v-model="newMemberRole" style="width: 100%">
            <el-option label="项目经理" value="项目经理" />
            <el-option label="开发工程师" value="开发工程师" />
            <el-option label="数据分析师" value="数据分析师" />
            <el-option label="测试工程师" value="测试工程师" />
            <el-option label="观察员" value="观察员" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addMemberVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddMember">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sysProjects, sysUsers, owners } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const ownerList = owners

const all = ref([...sysProjects])
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })
const editVisible = ref(false)
const form = reactive({})

const ongoingCount = computed(() => all.value.filter(p => p.status === '进行中').length)
const totalMembers = computed(() => all.value.reduce((acc, p) => acc + p.members, 0))
const totalResources = computed(() => all.value.reduce((acc, p) => acc + p.resources, 0))

const statusTagType = (s) => ({ '进行中': 'primary', '已验收': 'success', '已归档': 'info' }[s] || 'info')

function load() {
  let list = all.value.filter(r =>
    (!query.keyword || r.name.includes(query.keyword) || r.code.includes(query.keyword)) &&
    (!query.status || r.status === query.status)
  )
  total.value = list.length
  rows.value = list.slice((query.page - 1) * query.size, query.page * query.size)
}

function openEdit(row) {
  Object.assign(form, row || {
    id: null, name: '', code: '', owner: ownerList[0],
    status: '进行中', deadline: '', desc: ''
  })
  editVisible.value = true
}

function save() {
  if (!form.name || !form.code) return ElMessage.warning('请填写项目名称和编码')
  if (form.id) {
    const i = all.value.findIndex(r => r.id === form.id)
    all.value[i] = { ...all.value[i], ...form }
  } else {
    if (all.value.some(r => r.code === form.code)) {
      return ElMessage.warning('项目编码已存在')
    }
    all.value.unshift({
      ...form,
      id: Date.now(),
      members: 1,
      resources: 0,
      progress: 0
    })
  }
  editVisible.value = false
  ElMessage.success('项目已保存')
  load()
}

function remove(row) {
  ElMessageBox.confirm(`确定删除项目「${row.name}」吗？项目下的资源将被解除关联。`, '删除确认', { type: 'warning' })
    .then(() => {
      all.value = all.value.filter(r => r.id !== row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

// 成员管理
const memberVisible = ref(false)
const addMemberVisible = ref(false)
const currentProject = ref(null)
const projectMembers = ref([])
const selectedMember = ref(null)
const newMemberRole = ref('开发工程师')
const availableMembers = sysUsers

const roleTagType = (r) => ({
  '项目经理': 'warning',
  '开发工程师': 'primary',
  '数据分析师': 'success',
  '测试工程师': 'info',
  '观察员': ''
}[r] || 'info')

function manageMembers(row) {
  currentProject.value = row
  // 模拟项目成员
  projectMembers.value = [
    { id: 1, name: row.owner, role: '项目经理', dept: '数据开发部' },
    { id: 2, name: '李娜', role: '开发工程师', dept: '数据开发部' },
    { id: 3, name: '王强', role: '开发工程师', dept: '数据治理部' },
    { id: 4, name: '陈静', role: '数据分析师', dept: '数据分析部' },
  ]
  memberVisible.value = true
}

function removeMember(row) {
  ElMessageBox.confirm(`确定将「${row.name}」从项目中移除吗？`, '移除确认', { type: 'warning' })
    .then(() => {
      projectMembers.value = projectMembers.value.filter(m => m.id !== row.id)
      if (currentProject.value) {
        const i = all.value.findIndex(p => p.id === currentProject.value.id)
        if (i >= 0) all.value[i].members = projectMembers.value.length
      }
      ElMessage.success('成员已移除')
      load()
    })
    .catch(() => {})
}

function confirmAddMember() {
  if (!selectedMember.value) return ElMessage.warning('请选择成员')
  const user = availableMembers.find(u => u.id === selectedMember.value)
  if (user && !projectMembers.value.some(m => m.id === user.id)) {
    projectMembers.value.push({
      id: user.id,
      name: user.nickname,
      role: newMemberRole.value,
      dept: user.dept
    })
    if (currentProject.value) {
      const i = all.value.findIndex(p => p.id === currentProject.value.id)
      if (i >= 0) all.value[i].members = projectMembers.value.length
    }
    ElMessage.success('成员添加成功')
    load()
  } else {
    ElMessage.warning('该成员已在项目中')
  }
  addMemberVisible.value = false
  selectedMember.value = null
  newMemberRole.value = '开发工程师'
}

load()
</script>

<style scoped>
.member-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
