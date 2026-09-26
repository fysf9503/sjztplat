<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">USER MANAGEMENT</div>
      <h1 class="dp-page-title">用户管理</h1>
      <p class="dp-page-desc">管理平台用户账号、角色和权限。</p>
    </div>

    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索用户名 / 姓名 / 邮箱" clearable style="width: 260px" :prefix-icon="Search" @input="load" />
        <el-select v-model="query.dept" placeholder="部门" clearable style="width: 140px" @change="load">
          <el-option v-for="d in deptList" :key="d" :label="d" :value="d" />
        </el-select>
        <el-select v-model="query.role" placeholder="角色" clearable style="width: 140px" @change="load">
          <el-option v-for="r in roleList" :key="r" :label="r" :value="r" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
          <el-option label="正常" value="正常" />
          <el-option label="已锁定" value="已锁定" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="load">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="openEdit()">新增用户</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="username" label="用户名:" width="110" class-name="dp-mono" />
        <el-table-column prop="nickname" label="姓名" width="90" />
        <el-table-column prop="dept" label="部门" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机" width="130" class-name="dp-mono" />
        <el-table-column label="角色" min-width="140">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" :type="roleTagType(r)" size="small" effect="plain" style="margin-right: 4px">
              {{ r }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === '正常' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLogin" label="最后登录" width="160" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="resetPassword(row)">重置密码</el-button>
            <el-button
              link
              :type="row.status === '正常' ? 'danger' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === '正常' ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑用户' : '新增用户'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名" required>
          <el-input v-model="form.username" placeholder="登录账号" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="姓名" required>
          <el-input v-model="form.nickname" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.dept" style="width: 100%">
            <el-option v-for="d in deptList" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="user@company.com" />
        </el-form-item>
        <el-form-item label="手机">
          <el-input v-model="form.phone" placeholder="手机号码" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roles" multiple style="width: 100%">
            <el-option v-for="r in roleList" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码">
          <el-input v-model="form.password" type="password" show-password placeholder="默认 123456" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button value="正常">正常</el-radio-button>
            <el-radio-button value="已锁定">已锁定</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sysUsers, depts } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const deptList = depts
const roleList = ['超级管理员', '数据治理员', '数据开发工程师', '数据分析师', '审计员', '普通用户', '项目经理', '运营人员']

const all = ref([...sysUsers])
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', dept: '', role: '', status: '', page: 1, size: 10 })
const editVisible = ref(false)
const form = reactive({})

const roleTagType = (r) => ({
  '超级管理员': 'danger',
  '数据治理员': 'primary',
  '数据开发工程师': 'success',
  '数据分析师': 'warning',
  '审计员': 'info',
  '普通用户': 'info',
  '项目经理': 'warning',
  '运营人员': ''
}[r] || 'info')

function load() {
  let list = all.value.filter(r =>
    (!query.keyword || r.username.includes(query.keyword) || r.nickname.includes(query.keyword) || r.email.includes(query.keyword)) &&
    (!query.dept || r.dept === query.dept) &&
    (!query.role || r.roles.some(role => role.includes(query.role))) &&
    (!query.status || r.status === query.status)
  )
  total.value = list.length
  rows.value = list.slice((query.page - 1) * query.size, query.page * query.size)
}

function openEdit(row) {
  Object.assign(form, row || {
    id: null, username: '', nickname: '', dept: deptList[0],
    email: '', phone: '', roles: ['普通用户'], status: '正常', password: '123456'
  })
  editVisible.value = true
}

function save() {
  if (!form.username || !form.nickname) return ElMessage.warning('请填写用户名和姓名')
  if (form.id) {
    const i = all.value.findIndex(r => r.id === form.id)
    all.value[i] = { ...all.value[i], ...form }
  } else {
    if (all.value.some(r => r.username === form.username)) {
      return ElMessage.warning('用户名已存在')
    }
    all.value.unshift({
      ...form,
      id: Date.now(),
      lastLogin: '-',
      createdAt: '刚刚'
    })
  }
  editVisible.value = false
  ElMessage.success('用户已保存')
  load()
}

function resetPassword(row) {
  ElMessageBox.confirm(`确定重置用户「${row.nickname}」的密码吗？重置后密码为默认密码。`, '重置密码', { type: 'warning' })
    .then(() => {
      ElMessage.success(`用户「${row.nickname}」密码已重置为默认密码`)
    })
    .catch(() => {})
}

function toggleStatus(row) {
  const action = row.status === '正常' ? '禁用' : '启用'
  ElMessageBox.confirm(`确定${action}用户「${row.nickname}」吗？`, `${action}确认`, { type: 'warning' })
    .then(() => {
      const i = all.value.findIndex(r => r.id === row.id)
      all.value[i].status = row.status === '正常' ? '已锁定' : '正常'
      ElMessage.success(`用户「${action}`)
      load()
    })
    .catch(() => {})
}

function remove(row) {
  ElMessageBox.confirm(`确定删除用户「${row.nickname}」吗？删除后无法恢复。`, '删除确认', { type: 'warning' })
    .then(() => {
      all.value = all.value.filter(r => r.id !== row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

load()
</script>

<style scoped>
</style>
