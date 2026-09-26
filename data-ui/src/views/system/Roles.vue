<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">ROLE MANAGEMENT</div>
      <h1 class="dp-page-title">角色管理</h1>
      <p class="dp-page-desc">管理系统角色及其菜单权限。</p>
    </div>

    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索角色编码 / 名称" clearable style="width: 240px" :prefix-icon="Search" @input="load" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="load">
          <el-option label="启用" value="启用" />
          <el-option label="停用" value="停用" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="load">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="openEdit()">新增角色</el-button>
        </div>
      </div>

      <el-table :data="rows" border stripe>
        <el-table-column prop="code" label="角色编码" width="150" class-name="dp-mono" />
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="users" label="用户数" width="90" sortable />
        <el-table-column prop="menus" label="菜单数" width="90" sortable />
        <el-table-column prop="desc" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === '启用' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openPermission(row)">权限配置</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="load" />
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑角色' : '新增角色'" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="角色编码" required>
          <el-input v-model="form.code" placeholder="如：admin" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="角色名称" required>
          <el-input v-model="form.name" placeholder="如：超级管理员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="角色权限说明" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button value="启用">启用</el-radio-button>
            <el-radio-button value="停用">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置抽屉 -->
    <el-drawer v-model="permissionVisible" title="菜单权限配置" size="480px">
      <div class="perm-header">
        <span>角色：<strong>{{ currentRole?.name }}</strong></span>
        <el-checkbox v-model="checkAll" :indeterminate="isIndeterminate" @change="handleCheckAllChange">
          全选         </el-checkbox>
      </div>
      <el-tree
        ref="menuTreeRef"
        :data="menuTree"
        show-checkbox
        node-key="id"
        default-expand-all
        :default-checked-keys="checkedKeys"
        @check="handleCheck"
      >
        <template #default="{ node, data }">
          <span class="custom-tree-node">
            <el-icon><component :is="data.icon || 'Menu'" /></el-icon>
            <span class="node-label">{{ node.label }}</span>
          </span>
        </template>
      </el-tree>
      <div class="perm-footer">
        <el-button @click="permissionVisible = false">取消</el-button>
        <el-button type="primary" @click="savePermission">保存权限</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sysRoles } from '@/mock'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const all = ref([...sysRoles])
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', status: '', page: 1, size: 10 })
const editVisible = ref(false)
const form = reactive({})

function load() {
  let list = all.value.filter(r =>
    (!query.keyword || r.code.includes(query.keyword) || r.name.includes(query.keyword)) &&
    (!query.status || r.status === query.status)
  )
  total.value = list.length
  rows.value = list.slice((query.page - 1) * query.size, query.page * query.size)
}

function openEdit(row) {
  Object.assign(form, row || {
    id: null, code: '', name: '', desc: '', status: '启用', users: 0, menus: 0
  })
  editVisible.value = true
}

function save() {
  if (!form.code || !form.name) return ElMessage.warning('请填写角色编码和名称')
  if (form.id) {
    const i = all.value.findIndex(r => r.id === form.id)
    all.value[i] = { ...all.value[i], ...form, updatedAt: '刚刚' }
  } else {
    if (all.value.some(r => r.code === form.code)) {
      return ElMessage.warning('角色编码已存在')
    }
    all.value.unshift({
      ...form,
      id: Date.now(),
      users: 0,
      menus: 0,
      updatedAt: '刚刚'
    })
  }
  editVisible.value = false
  ElMessage.success('角色已保存')
  load()
}

function remove(row) {
  if (row.users > 0) {
    return ElMessage.warning(`该角色下有 ${row.users} 个用户，请先调整用户角色后再删除`)
  }
  ElMessageBox.confirm(`确定删除角色「${row.name}」吗？`, '删除确认', { type: 'warning' })
    .then(() => {
      all.value = all.value.filter(r => r.id !== row.id)
      ElMessage.success('删除成功')
      load()
    })
    .catch(() => {})
}

// 权限配置
const permissionVisible = ref(false)
const currentRole = ref(null)
const menuTreeRef = ref(null)
const checkedKeys = ref([])
const checkAll = ref(false)
const isIndeterminate = ref(false)

const menuTree = [
  {
    id: 'overview', label: '总览', icon: 'DataLine',
    children: [
      { id: 'overview:view', label: '查看总览', icon: 'View' }
    ]
  },
  {
    id: 'access', label: '数据接入', icon: 'Files',
    children: [
      { id: 'access:overview', label: '总览', icon: 'View' },
      { id: 'access:datasources', label: '数据源', icon: 'Service' },
      { id: 'access:tasks', label: '接入任务', icon: 'Operation' },
      { id: 'access:files', label: '文件管理', icon: 'Document' },
      { id: 'access:survey', label: '数据调研', icon: 'Search' }
    ]
  },
  {
    id: 'develop', label: '数据开发', icon: 'Cpu',
    children: [
      { id: 'develop:overview', label: '总览', icon: 'View' },
      { id: 'develop:workbench', label: '开发工作台', icon: 'Operation' },
      { id: 'develop:processes', label: '加工流程', icon: 'Service' },
      { id: 'develop:instances', label: '实例监控', icon: 'PieChart' }
    ]
  },
  {
    id: 'modeling', label: '数据建模', icon: 'MagicStick',
    children: [
      { id: 'modeling:overview', label: '总览', icon: 'View' },
      { id: 'modeling:logical', label: '逻辑模型', icon: 'Menu' },
      { id: 'modeling:physical', label: '物理模型', icon: 'Service' },
      { id: 'modeling:deploy', label: '部署管理', icon: 'Setting' }
    ]
  },
  {
    id: 'quality', label: '质量管控', icon: 'PieChart',
    children: [
      { id: 'quality:overview', label: '总览', icon: 'View' },
      { id: 'quality:rules', label: '规则管理', icon: 'Operation' },
      { id: 'quality:strategies', label: '质量策略', icon: 'Setting' },
      { id: 'quality:tasks', label: '质量任务', icon: 'Service' },
      { id: 'quality:results', label: '质检结果', icon: 'Document' }
    ]
  },
  {
    id: 'service', label: '数据服务', icon: 'Service',
    children: [
      { id: 'service:overview', label: '总览', icon: 'View' },
      { id: 'service:catalog', label: '服务目录', icon: 'Menu' },
      { id: 'service:apps', label: '应用管理', icon: 'User' },
      { id: 'service:auth', label: '授权审批', icon: 'Lock' },
      { id: 'service:audit', label: '调用审计', icon: 'Document' }
    ]
  },
  {
    id: 'search', label: '资产目录', icon: 'Search',
    children: [
      { id: 'search:search', label: '资产搜索', icon: 'Search' },
      { id: 'search:business', label: '业务资产', icon: 'Menu' },
      { id: 'search:model', label: '技术资产', icon: 'Service' }
    ]
  },
  {
    id: 'system', label: '系统管理', icon: 'Setting',
    children: [
      { id: 'system:users', label: '用户管理', icon: 'User' },
      { id: 'system:roles', label: '角色管理', icon: 'Lock' },
      { id: 'system:projects', label: '项目管理', icon: 'Menu' },
      { id: 'system:models', label: '模型管理', icon: 'Cpu' },
      { id: 'system:knowledge', label: '知识库', icon: 'Document' },
      { id: 'system:services', label: '服务管理', icon: 'Service' },
      { id: 'system:license', label: '许可证', icon: 'Lock' }
    ]
  }
]

function openPermission(row) {
  currentRole.value = row
  permissionVisible.value = true
  // 模拟已有权限
  const allKeys = []
  function collect(arr) {
    arr.forEach(item => {
      allKeys.push(item.id)
      if (item.children) collect(item.children)
    })
  }
  collect(menuTree)
  // 根据角色名称模拟不同的权限   let count = row.menus || 30
  checkedKeys.value = allKeys.slice(0, count)
  nextTick(() => {
    updateCheckAllState()
  })
}

function handleCheck() {
  updateCheckAllState()
}

function updateCheckAllState() {
  if (!menuTreeRef.value) return
  const checkedNodes = menuTreeRef.value.getCheckedNodes()
  const allLeafNodes = []
  function collectLeaves(arr) {
    arr.forEach(item => {
      if (item.children && item.children.length > 0) {
        collectLeaves(item.children)
      } else {
        allLeafNodes.push(item)
      }
    })
  }
  collectLeaves(menuTree)
  checkAll.value = checkedNodes.length === allLeafNodes.length
  isIndeterminate.value = checkedNodes.length > 0 && checkedNodes.length < allLeafNodes.length
}

function handleCheckAllChange(val) {
  if (!menuTreeRef.value) return
  if (val) {
    const allKeys = []
    function collect(arr) {
      arr.forEach(item => {
        allKeys.push(item.id)
        if (item.children) collect(item.children)
      })
    }
    collect(menuTree)
    menuTreeRef.value.setCheckedKeys(allKeys)
  } else {
    menuTreeRef.value.setCheckedKeys([])
  }
  isIndeterminate.value = false
}

function savePermission() {
  if (!menuTreeRef.value) return
  const checkedNodes = menuTreeRef.value.getCheckedNodes()
  const leafCount = checkedNodes.filter(n => !n.children || n.children.length === 0).length
  if (currentRole.value) {
    const i = all.value.findIndex(r => r.id === currentRole.value.id)
    if (i >= 0) {
      all.value[i].menus = leafCount
    }
  }
  ElMessage.success('权限配置已保存')
  permissionVisible.value = false
  load()
}

load()
</script>

<style scoped>
.perm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--dp-border-light);
  margin-bottom: 12px;
}
.custom-tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.node-label {
  flex: 1;
}
.perm-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 14px 20px;
  border-top: 1px solid var(--dp-border-light);
  background: #fff;
  text-align: right;
}
</style>
