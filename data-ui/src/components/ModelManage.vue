<template>
  <el-drawer v-model="visible" title="模型管理" size="720px" destroy-on-close>
    <div class="dp-toolbar">
      <el-button type="primary" :icon="Plus" @click="openEdit()">新增模型</el-button>
      <span class="dp-desc">配置当前模块使用的 AI 模型，支持本地部署与云端 API 接入</span>
    </div>
    <el-table :data="models" border stripe>
      <el-table-column prop="name" label="模型名称" min-width="140">
        <template #default="{ row }">
          <span class="dp-mono">{{ row.name }}</span>
          <el-tag v-if="row.isDefault" size="small" type="primary" style="margin-left: 6px">默认</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="provider" label="提供方" min-width="130" />
      <el-table-column prop="context" label="上下文" width="80" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === '运行中' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="!row.isDefault" link type="primary" size="small" @click="setDefault(row)">设为默认</el-button>
          <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑模型' : '新增模型'" width="520px" append-to-body destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="模型名称" required>
          <el-input v-model="form.name" placeholder="如 DeepSeek-V3" />
        </el-form-item>
        <el-form-item label="模型类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="t in ['对话模型', 'Embedding', '多模态', '代码模型']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="提供方">
          <el-select v-model="form.provider" style="width: 100%">
            <el-option v-for="p in ['本地部署', 'DeepSeek开放平台', '阿里云百炼', '智谱AI', 'OpenAI', '火山方舟']" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="API地址">
          <el-input v-model="form.api" placeholder="http://localhost:11434/v1" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.key" type="password" show-password placeholder="sk-****" />
        </el-form-item>
        <el-form-item label="QPS限制">
          <el-input-number v-model="form.qpsLimit" :min="1" :max="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { aiModels } from '@/mock'

const visible = ref(false)
const models = ref([...aiModels])
const editVisible = ref(false)
const form = reactive({ id: null, name: '', type: '对话模型', provider: '本地部署', api: '', key: '', qpsLimit: 20 })

function open() { visible.value = true }

function openEdit(row) {
  Object.assign(form, row || { id: null, name: '', type: '对话模型', provider: '本地部署', api: '', key: '', qpsLimit: 20 })
  editVisible.value = true
}

function save() {
  if (!form.name) return ElMessage.warning('请输入模型名称')
  if (form.id) {
    const idx = models.value.findIndex(m => m.id === form.id)
    models.value[idx] = { ...models.value[idx], ...form }
  } else {
    models.value.unshift({ ...form, id: Date.now(), context: '64K', status: '运行中', owner: '当前用户', updatedAt: '刚刚', isDefault: false })
  }
  editVisible.value = false
  ElMessage.success('保存成功')
}

function setDefault(row) {
  models.value.forEach(m => (m.isDefault = m.id === row.id))
  ElMessage.success(`已将 ${row.name} 设为默认模型`)
}

function remove(row) {
  ElMessageBox.confirm(`确定删除模型「${row.name}」吗？`, '提示', { type: 'warning' }).then(() => {
    models.value = models.value.filter(m => m.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

defineExpose({ open })
</script>