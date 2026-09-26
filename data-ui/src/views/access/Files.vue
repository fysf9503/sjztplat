<template>
  <div class="dp-page">
    <PageHeader title="文件上传" desc="支持 CSV、Excel、JSON、TXT 等多种格式文件的上传与解析接入">
      <el-button type="primary" :icon="UploadFilled" @click="triggerUpload">上传文件</el-button>
    </PageHeader>

    <!-- 上传区域 -->
    <div class="dp-card upload-card dp-fade-up">
      <div
        class="upload-area"
        :class="{ 'is-dragover': isDragover }"
        @dragover.prevent="isDragover = true"
        @dragleave.prevent="isDragover = false"
        @drop.prevent="handleDrop"
        @click="triggerUpload"
      >
        <input
          ref="fileInput"
          type="file"
          :accept="acceptTypes"
          multiple
          hidden
          @change="handleFileSelect"
        />
        <div class="upload-icon">
          <el-icon :size="48" color="#1664ff"><UploadFilled /></el-icon>
        </div>
        <div class="upload-title">拖拽文件到此处，或 <span class="upload-link">点击上传</span></div>
        <div class="upload-tips">
          支持 CSV、Excel、JSON、TXT 格式，单个文件不超过 500MB
        </div>
        <div class="upload-formats">
          <span v-for="fmt in formatList" :key="fmt" class="format-tag">{{ fmt }}</span>
        </div>
      </div>

      <!-- 上传进度 -->
      <div v-if="uploadingList.length > 0" class="upload-progress-list">
        <div v-for="item in uploadingList" :key="item.uid" class="upload-progress-item">
          <div class="progress-icon">
            <el-icon :size="18" color="#1664ff"><Document /></el-icon>
          </div>
          <div class="progress-info">
            <div class="progress-name">{{ item.name }}</div>
            <el-progress :percentage="item.progress" :stroke-width="6" :show-text="false" />
          </div>
          <div class="progress-percent">{{ item.progress }}%</div>
        </div>
      </div>
    </div>

    <!-- 文件列表 -->
    <div class="dp-card dp-fade-up">
      <div class="dp-card-header">
        <h3 class="dp-card-title">已上传文件</h3>
        <div class="dp-card-extra">
          <el-select v-model="filterStatus" placeholder="状态筛选" clearable size="small" style="width: 120px; margin-right: 10px">
            <el-option label="解析中" value="解析中" />
            <el-option label="已完成" value="已完成" />
            <el-option label="失败" value="失败" />
          </el-select>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文件名"
            clearable
            size="small"
            style="width: 200px"
            :prefix-icon="Search"
          />
        </div>
      </div>

      <el-table :data="filteredFiles" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="name" label="文件名" min-width="240">
          <template #default="{ row }">
            <div class="file-name-cell">
              <div class="file-icon" :class="row.format.toLowerCase()">
                <el-icon :size="16"><component :is="getFileIcon(row.format)" /></el-icon>
              </div>
              <span class="file-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="format" label="格式" width="80">
          <template #default="{ row }">
            <span class="file-format-tag" :class="row.format.toLowerCase()">{{ row.format }}</span>
          </template>
        </el-table-column>
        <el-table-column label="文件大小" width="100" align="right">
          <template #default="{ row }">
            <span class="file-size">{{ row.sizeMB }} MB</span>
          </template>
        </el-table-column>
        <el-table-column label="数据行数" width="110" align="right">
          <template #default="{ row }">
            <span class="file-rows" v-if="row.rows">{{ formatNumber(row.rows) }}</span>
            <span v-else class="file-rows-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="file-status">
              <span class="file-status-dot" :class="statusDotClass(row.status)"></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="uploader" label="上传人" width="80" />
        <el-table-column prop="time" label="上传时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" :icon="View" @click="previewFile(row)">预览</el-button>
            <el-button link type="primary" size="small" :icon="Setting" @click="openParseConfig(row)">解析配置</el-button>
            <el-button link type="primary" size="small" :icon="Download" @click="downloadFile(row)">下载</el-button>
            <el-button link type="danger" size="small" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="filteredFiles.length"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- 文件解析配置弹窗 -->
    <el-dialog v-model="parseDialogVisible" title="文件解析配置" width="560px" :close-on-click-modal="false">
      <el-form :model="parseConfig" label-width="100px">
        <el-form-item label="源文件">
          <span class="parse-file-name">{{ currentFileName }}</span>
        </el-form-item>
        <el-form-item label="编码格式">
          <el-select v-model="parseConfig.encoding" style="width: 100%">
            <el-option label="UTF-8" value="UTF-8" />
            <el-option label="GBK" value="GBK" />
            <el-option label="GB2312" value="GB2312" />
            <el-option label="ISO-8859-1" value="ISO-8859-1" />
          </el-select>
        </el-form-item>
        <el-form-item label="分隔符">
          <el-radio-group v-model="parseConfig.delimiter">
            <el-radio value=",">逗号 (,)</el-radio>
            <el-radio value=";">分号 (;)</el-radio>
            <el-radio value="\t">制表符 (Tab)</el-radio>
            <el-radio value="|">竖线 (|)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="表头行">
          <el-switch v-model="parseConfig.hasHeader" active-text="包含表头" inactive-text="无表头" />
        </el-form-item>
        <el-form-item label="跳过行数">
          <el-input-number v-model="parseConfig.skipRows" :min="0" :max="100" />
          <span class="form-tip">跳过文件开头的指定行数</span>
        </el-form-item>
        <el-form-item label="目标表名">
          <el-input v-model="parseConfig.targetTable" placeholder="请输入目标表名" />
        </el-form-item>
      </el-form>
      <div class="parse-preview">
        <div class="parse-preview-title">数据预览（前5行）</div>
        <el-table :data="previewData" size="small" border max-height="180">
          <el-table-column v-for="(col, idx) in previewColumns" :key="idx" :prop="col" :label="col" min-width="120" />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="parseDialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Check" @click="saveParseConfig">确认解析</el-button>
      </template>
    </el-dialog>

    <!-- 文件预览弹窗 -->
    <el-dialog v-model="previewDialogVisible" title="文件预览" width="900px">
      <div class="preview-header">
        <span class="preview-file-name">{{ currentFileName }}</span>
        <span class="preview-file-info">共 {{ previewRows.length }} 行数据</span>
      </div>
      <el-table :data="previewRows" size="small" border max-height="400">
        <el-table-column v-for="(col, idx) in previewCols" :key="idx" :prop="col" :label="col" min-width="140" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  UploadFilled, Document, Search, View, Setting,
  Download, Delete, Check, Files, Grid
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { fileUploads, owners } from '@/mock'

const loading = ref(false)
const isDragover = ref(false)
const fileInput = ref(null)
const parseDialogVisible = ref(false)
const previewDialogVisible = ref(false)
const currentFileName = ref('')
const searchKeyword = ref('')
const filterStatus = ref('')

const acceptTypes = '.csv,.xlsx,.xls,.json,.txt'
const formatList = ['CSV', 'Excel', 'JSON', 'TXT']

const pagination = reactive({
  page: 1,
  size: 10
})

const uploadingList = ref([])

const parseConfig = reactive({
  encoding: 'UTF-8',
  delimiter: ',',
  hasHeader: true,
  skipRows: 0,
  targetTable: ''
})

const dataList = ref([...fileUploads].map(f => ({
  ...f,
  status: f.status === '已入库' ? '已完成' : f.status === '上传失败' ? '失败' : f.status
})))

const filteredFiles = computed(() => {
  return dataList.value.filter(item => {
    if (searchKeyword.value && !item.name.toLowerCase().includes(searchKeyword.value.toLowerCase())) return false
    if (filterStatus.value && item.status !== filterStatus.value) return false
    return true
  })
})

const previewColumns = ['id', 'name', 'type', 'status', 'amount', 'create_time']
const previewData = ref([
  { id: 1, name: '张三', type: 'VIP', status: '正常', amount: '1280.00', create_time: '2026-09-01 10:30:00' },
  { id: 2, name: '李四', type: '普通', status: '正常', amount: '560.50', create_time: '2026-09-02 14:20:00' },
  { id: 3, name: '王五', type: 'VIP', status: '冻结', amount: '3200.00', create_time: '2026-09-03 09:15:00' },
  { id: 4, name: '赵六', type: '普通', status: '正常', amount: '890.00', create_time: '2026-09-04 16:45:00' },
  { id: 5, name: '孙七', type: 'VIP', status: '正常', amount: '2100.00', create_time: '2026-09-05 11:00:00' }
])

const previewCols = ref(previewColumns)
const previewRows = ref([
  { id: 1, name: '张伟', type: 'VIP会员', status: '活跃', amount: '5,280.00', create_time: '2026-09-01 10:30:00' },
  { id: 2, name: '李娜', type: '普通会员', status: '活跃', amount: '1,560.50', create_time: '2026-09-02 14:20:00' },
  { id: 3, name: '王强', type: 'VIP会员', status: '休眠', amount: '8,320.00', create_time: '2026-09-03 09:15:00' },
  { id: 4, name: '刘洋', type: '普通会员', status: '活跃', amount: '2,890.00', create_time: '2026-09-04 16:45:00' },
  { id: 5, name: '陈静', type: 'VIP会员', status: '活跃', amount: '6,210.00', create_time: '2026-09-05 11:00:00' },
  { id: 6, name: '赵磊', type: '普通会员', status: '新注册', amount: '580.00', create_time: '2026-09-06 08:30:00' },
  { id: 7, name: '孙悦', type: 'VIP会员', status: '活跃', amount: '9,450.00', create_time: '2026-09-07 13:20:00' },
  { id: 8, name: '周杰', type: '普通会员', status: '休眠', amount: '320.00', create_time: '2026-09-08 15:50:00' },
  { id: 9, name: '吴敏', type: 'VIP会员', status: '活跃', amount: '7,680.00', create_time: '2026-09-09 10:10:00' },
  { id: 10, name: '郑浩', type: '普通会员', status: '活跃', amount: '1,200.00', create_time: '2026-09-10 17:30:00' }
])

function getFileIcon(format) {
  const map = { CSV: 'Grid', Excel: 'Grid', JSON: 'Files', TXT: 'Document' }
  return map[format] || 'Document'
}

function statusDotClass(status) {
  const map = {
    '已完成': 'success',
    '解析中': 'running',
    '失败': 'error'
  }
  return map[status] || 'running'
}

function formatNumber(num) {
  if (num >= 10000) return (num / 10000).toFixed(1) + '万'
  return num.toLocaleString()
}

function triggerUpload() {
  fileInput.value?.click()
}

function handleFileSelect(e) {
  const files = Array.from(e.target.files || [])
  processFiles(files)
  e.target.value = ''
}

function handleDrop(e) {
  isDragover.value = false
  const files = Array.from(e.dataTransfer?.files || [])
  if (files.length === 0) return
  processFiles(files)
}

function processFiles(files) {
  files.forEach(file => {
    const ext = file.name.split('.').pop()?.toUpperCase()
    const validExts = ['CSV', 'XLSX', 'XLS', 'JSON', 'TXT']
    if (!validExts.includes(ext)) {
      ElMessage.warning(`不支持的文件格式：${ext}`)
      return
    }
    const uploadItem = {
      uid: Date.now() + Math.random(),
      name: file.name,
      size: (file.size / 1024 / 1024).toFixed(2),
      progress: 0,
      format: ext === 'XLSX' || ext === 'XLS' ? 'Excel' : ext
    }
    uploadingList.value.push(uploadItem)
    simulateUpload(uploadItem)
  })
}

function simulateUpload(item) {
  const timer = setInterval(() => {
    item.progress += Math.floor(Math.random() * 15) + 5
    if (item.progress >= 100) {
      item.progress = 100
      clearInterval(timer)
      setTimeout(() => {
        const idx = uploadingList.value.findIndex(u => u.uid === item.uid)
        if (idx > -1) {
          uploadingList.value.splice(idx, 1)
        }
        dataList.value.unshift({
          id: Date.now(),
          name: item.name,
          format: item.format,
          sizeMB: parseFloat(item.size),
          rows: 0,
          target: '-',
          status: '解析中',
          uploader: owners[Math.floor(Math.random() * owners.length)],
          time: new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
        })
        ElMessage.success(`${item.name} 上传成功，正在解析...`)
        setTimeout(() => {
          const fileIdx = dataList.value.findIndex(f => f.name === item.name)
          if (fileIdx > -1) {
            dataList.value[fileIdx].status = '已完成'
            dataList.value[fileIdx].rows = Math.floor(Math.random() * 50000) + 1000
          }
        }, 2000)
      }, 300)
    }
  }, 200)
}

function previewFile(row) {
  currentFileName.value = row.name
  previewDialogVisible.value = true
}

function openParseConfig(row) {
  currentFileName.value = row.name
  parseConfig.targetTable = row.target || ''
  parseDialogVisible.value = true
}

function saveParseConfig() {
  parseDialogVisible.value = false
  ElMessage.success('解析配置已保存，文件正在重新解析')
  const idx = dataList.value.findIndex(f => f.name === currentFileName.value)
  if (idx > -1) {
    dataList.value[idx].status = '解析中'
    setTimeout(() => {
      dataList.value[idx].status = '已完成'
      dataList.value[idx].rows = Math.floor(Math.random() * 80000) + 5000
      ElMessage.success('文件解析完成')
    }, 1500)
  }
}

function downloadFile(row) {
  ElMessage.info(`正在下载 ${row.name}...`)
}

function handleDelete(row) {
  ElMessageBox.confirm(
    `确定要删除文件 "${row.name}" 吗？删除后将无法恢复。`,
    '删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(() => {
    const idx = dataList.value.findIndex(item => item.id === row.id)
    if (idx > -1) {
      dataList.value.splice(idx, 1)
    }
    ElMessage.success('删除成功')
  }).catch(() => {})
}

onMounted(() => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 300)
})
</script>

<style scoped>
.upload-card {
  margin-bottom: 16px;
}

.upload-area {
  border: 2px dashed var(--dp-border);
  border-radius: 12px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  background: var(--dp-bg-page);
}
.upload-area:hover {
  border-color: var(--dp-primary);
  background: var(--dp-primary-bg);
}
.upload-area.is-dragover {
  border-color: var(--dp-primary);
  background: var(--dp-primary-bg);
  transform: scale(1.01);
}

.upload-icon {
  margin-bottom: 16px;
}
.upload-title {
  font-size: 16px;
  color: var(--dp-text-1);
  margin-bottom: 8px;
}
.upload-link {
  color: var(--dp-primary);
  font-weight: 500;
}
.upload-tips {
  font-size: 13px;
  color: var(--dp-text-3);
  margin-bottom: 16px;
}
.upload-formats {
  display: flex;
  justify-content: center;
  gap: 10px;
}
.format-tag {
  display: inline-block;
  padding: 4px 12px;
  font-size: 12px;
  border-radius: 4px;
  background: #fff;
  border: 1px solid var(--dp-border);
  color: var(--dp-text-2);
  font-weight: 500;
}

.upload-progress-list {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--dp-border-light);
}
.upload-progress-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
}
.progress-icon {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  background: var(--dp-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.progress-info {
  flex: 1;
  min-width: 0;
}
.progress-name {
  font-size: 13px;
  color: var(--dp-text-1);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.progress-percent {
  font-size: 13px;
  color: var(--dp-primary);
  font-weight: 500;
  width: 50px;
  text-align: right;
  flex-shrink: 0;
}

.file-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.file-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.file-icon.csv { background: #e8ffea; color: #00b42a; }
.file-icon.excel { background: #e8ffea; color: #00b42a; }
.file-icon.json { background: #fff3e8; color: #ff7d00; }
.file-icon.txt { background: #e8f0ff; color: #1664ff; }

.file-name {
  font-weight: 500;
  color: var(--dp-text-1);
}

.file-format-tag {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 4px;
  font-weight: 500;
}
.file-format-tag.csv { background: #e8ffea; color: #00b42a; }
.file-format-tag.excel { background: #e8ffea; color: #00b42a; }
.file-format-tag.json { background: #fff3e8; color: #ff7d00; }
.file-format-tag.txt { background: #e8f0ff; color: #1664ff; }

.file-size {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--dp-text-2);
}
.file-rows {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--dp-text-2);
}
.file-rows-empty {
  color: var(--dp-text-4);
}

.file-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.file-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.file-status-dot.success {
  background: var(--dp-success);
  box-shadow: 0 0 0 3px rgba(0, 180, 42, 0.15);
}
.file-status-dot.running {
  background: var(--dp-primary);
  box-shadow: 0 0 0 3px rgba(22, 100, 255, 0.15);
  animation: pulse 2s infinite;
}
.file-status-dot.error {
  background: var(--dp-danger);
  box-shadow: 0 0 0 3px rgba(245, 63, 63, 0.15);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.parse-file-name {
  font-weight: 500;
  color: var(--dp-text-1);
}
.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--dp-text-3);
}

.parse-preview {
  margin-top: 10px;
  padding-top: 16px;
  border-top: 1px solid var(--dp-border-light);
}
.parse-preview-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-2);
  margin-bottom: 10px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--dp-border-light);
}
.preview-file-name {
  font-weight: 500;
  color: var(--dp-text-1);
}
.preview-file-info {
  font-size: 12px;
  color: var(--dp-text-3);
}
</style>
