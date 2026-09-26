<template>
  <div class="dp-page">
    <PageHeader title="开发模型配置" desc="管理数据开发的 AI 模型配置，包括 SQL 生成、优化建议、错误诊断等模型的版本管理与部署">
      <el-button :icon="Refresh" @click="loadData" :loading="loading">刷新</el-button>
      <el-button type="primary" :icon="Plus" @click="openEdit()">新增模型</el-button>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="md-stat-grid">
      <StatCard label="模型总数" :value="statData.total" unit="个" icon="Cpu" color="var(--dp-primary)" bg="var(--dp-primary-light)" />
      <StatCard label="运行中" :value="statData.running" unit="个" icon="CircleCheck" color="var(--dp-success)" bg="var(--dp-success-light)" />
      <StatCard label="版本总数" :value="statData.versions" unit="个" icon="Collection" color="var(--dp-purple)" bg="var(--dp-purple-light)" />
      <StatCard label="今日调用量" :value="statData.calls" unit="次" icon="TrendCharts" color="var(--dp-warning)" bg="var(--dp-warning-light)" :trend="12.5" />
    </div>

    <div class="dp-card">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索模型名称/提供商" clearable style="width: 240px" :prefix-icon="Search" @input="loadData" />
        <el-select v-model="query.type" placeholder="模型类型" clearable style="width: 140px" @change="loadData">
          <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="loadData">
          <el-option label="运行中" value="运行中" />
          <el-option label="已停用" value="已停用" />
          <el-option label="部署中" value="部署中" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="name" label="模型名称" min-width="180">
          <template #default="{ row }">
            <div class="md-model-name">
              <span class="md-model-icon" :class="typeIconClass(row.type)">
                <el-icon><Cpu /></el-icon>
              </span>
              <div>
                <div class="md-name-text">{{ row.name }}</div>
                <div class="md-version-text">当前版本：v{{ row.currentVersion }}</div>
              </div>
              <el-tag v-if="row.isDefault" size="small" type="primary" effect="dark" style="margin-left: 8px">默认</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="模型类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="provider" label="提供商" min-width="140" />
        <el-table-column prop="taskCount" label="关联任务数" width="100" align="center">
          <template #default="{ row }">
            <span class="md-task-count">{{ row.taskCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="context" label="上下文长度" width="100" align="center" />
        <el-table-column label="适用场景" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="u in row.scenarios" :key="u" size="small" effect="plain" style="margin-right: 4px; margin-bottom: 2px">
              {{ u }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="md-status">
              <span class="md-status-dot" :class="statusDotClass(row.status)"></span>
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" size="small" @click="openVersion(row)">版本管理</el-button>
            <el-button link type="success" size="small" @click="deployModel(row)">部署</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="!row.isDefault" link type="danger" size="small" @click="removeModel(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>

    <!-- 模型详情抽屉 -->
    <el-drawer v-model="detailVisible" title="模型详情" size="640px" destroy-on-close>
      <template v-if="currentModel">
        <el-tabs v-model="detailTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <div class="md-detail-section">
              <div class="md-detail-header">
                <div class="md-detail-icon" :class="typeIconClass(currentModel.type)">
                  <el-icon :size="28"><Cpu /></el-icon>
                </div>
                <div>
                  <h3 class="md-detail-name">{{ currentModel.name }}</h3>
                  <div class="md-detail-sub">
                    <el-tag :type="typeTagType(currentModel.type)" size="small" effect="plain">{{ currentModel.type }}</el-tag>
                    <span class="md-status" style="margin-left: 10px">
                      <span class="md-status-dot" :class="statusDotClass(currentModel.status)"></span>
                      {{ currentModel.status }}
                    </span>
                  </div>
                </div>
              </div>

              <el-descriptions :column="2" border size="default" style="margin-top: 16px">
                <el-descriptions-item label="模型ID">{{ currentModel.id }}</el-descriptions-item>
                <el-descriptions-item label="当前版本">v{{ currentModel.currentVersion }}</el-descriptions-item>
                <el-descriptions-item label="提供商">{{ currentModel.provider }}</el-descriptions-item>
                <el-descriptions-item label="上下文长度">{{ currentModel.context }}</el-descriptions-item>
                <el-descriptions-item label="QPS 限制">{{ currentModel.qpsLimit }} 次/秒</el-descriptions-item>
                <el-descriptions-item label="关联任务数">{{ currentModel.taskCount }} 个</el-descriptions-item>
                <el-descriptions-item label="负责人">{{ currentModel.owner }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ currentModel.createdAt }}</el-descriptions-item>
                <el-descriptions-item label="适用场景" :span="2">
                  <el-tag v-for="s in currentModel.scenarios" :key="s" size="small" effect="plain" style="margin-right: 6px">
                    {{ s }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="模型描述" :span="2">
                  {{ currentModel.description || '暂无描述' }}
                </el-descriptions-item>
              </el-descriptions>

              <h4 class="md-section-title">性能指标</h4>
              <div class="md-metric-grid">
                <div class="md-metric-item">
                  <div class="md-metric-value">{{ currentModel.accuracy }}%</div>
                  <div class="md-metric-label">准确率</div>
                </div>
                <div class="md-metric-item">
                  <div class="md-metric-value">{{ currentModel.latency }}ms</div>
                  <div class="md-metric-label">平均延迟</div>
                </div>
                <div class="md-metric-item">
                  <div class="md-metric-value">{{ currentModel.todayCalls }}</div>
                  <div class="md-metric-label">今日调用</div>
                </div>
                <div class="md-metric-item">
                  <div class="md-metric-value">{{ currentModel.totalCalls }}</div>
                  <div class="md-metric-label">累计调用</div>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 版本历史 -->
          <el-tab-pane label="版本历史" name="version">
            <div class="md-detail-section">
              <div class="md-version-toolbar">
                <el-button type="primary" size="small" :icon="Plus">新增版本</el-button>
              </div>
              <el-timeline>
                <el-timeline-item
                  v-for="(v, idx) in currentModel.versions"
                  :key="v.version"
                  :timestamp="v.releaseTime"
                  placement="top"
                  :type="idx === 0 ? 'primary' : ''"
                  :icon="idx === 0 ? Star : ''"
                >
                  <div class="md-version-item">
                    <div class="md-version-header">
                      <span class="md-version-num">v{{ v.version }}</span>
                      <el-tag v-if="v.status === '已发布'" type="success" size="small">已发布</el-tag>
                      <el-tag v-else-if="v.status === '测试中'" type="warning" size="small">测试中</el-tag>
                      <el-tag v-else type="info" size="small">草稿</el-tag>
                      <span v-if="v.isCurrent" class="md-current-badge">当前版本</span>
                    </div>
                    <div class="md-version-desc">{{ v.description }}</div>
                    <div class="md-version-meta">
                      <span>发布人：{{ v.releaser }}</span>
                      <span>模型大小：{{ v.size }}</span>
                    </div>
                    <div class="md-version-actions">
                      <el-button size="small" link type="primary">部署</el-button>
                      <el-button size="small" link type="primary">回滚到此版本</el-button>
                      <el-button size="small" link type="primary">查看变更</el-button>
                    </div>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-tab-pane>

          <!-- 关联任务 -->
          <el-tab-pane label="关联任务" name="tasks">
            <div class="md-detail-section">
              <el-table :data="currentModel.tasks" border stripe size="small">
                <el-table-column prop="name" label="任务名称" min-width="160" show-overflow-tooltip />
                <el-table-column prop="type" label="任务类型" width="100">
                  <template #default="{ row }">
                    <el-tag size="small" effect="plain">{{ row.type }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="80">
                  <template #default="{ row }">
                    <span class="md-status">
                      <span class="md-status-dot" :class="statusDotClass(row.status)"></span>
                      {{ row.status }}
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="owner" label="负责人" width="80" />
                <el-table-column prop="updatedAt" label="更新时间" width="150" />
                <el-table-column label="操作" width="80">
                  <template #default>
                    <el-button link type="primary" size="small">查看</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑模型' : '新增模型'" width="600px" destroy-on-close append-to-body>
      <el-form :model="form" label-width="100px">
        <el-form-item label="模型名称" required>
          <el-input v-model="form.name" placeholder="如 DeepSeek-V3" />
        </el-form-item>
        <el-form-item label="模型类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option v-for="t in typeOptions" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="提供商">
          <el-select v-model="form.provider" style="width: 100%">
            <el-option v-for="p in providerOptions" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="API 地址">
          <el-input v-model="form.api" placeholder="http://localhost:11434/v1" class="dp-mono" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.key" type="password" show-password placeholder="sk-****" />
        </el-form-item>
        <el-form-item label="上下文长度">
          <el-select v-model="form.context" style="width: 100%">
            <el-option label="8K" value="8K" />
            <el-option label="32K" value="32K" />
            <el-option label="64K" value="64K" />
            <el-option label="128K" value="128K" />
            <el-option label="256K" value="256K" />
          </el-select>
        </el-form-item>
        <el-form-item label="QPS 限制">
          <el-input-number v-model="form.qpsLimit" :min="1" :max="500" />
        </el-form-item>
        <el-form-item label="适用场景">
          <el-checkbox-group v-model="form.scenarios">
            <el-checkbox label="SQL生成">SQL 生成</el-checkbox>
            <el-checkbox label="优化建议">优化建议</el-checkbox>
            <el-checkbox label="错误诊断">错误诊断</el-checkbox>
            <el-checkbox label="数据解释">数据解释</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="模型描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入模型描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveModel">保存</el-button>
      </template>
    </el-dialog>

    <!-- 部署弹窗 -->
    <el-dialog v-model="deployVisible" title="部署模型" width="480px" destroy-on-close append-to-body>
      <el-form label-width="100px">
        <el-form-item label="目标模型">
          <el-input :value="deployModelName" disabled />
        </el-form-item>
        <el-form-item label="部署版本">
          <el-select v-model="deployForm.version" style="width: 100%">
            <el-option v-for="v in deployVersions" :key="v" :label="'v' + v" :value="v" />
          </el-select>
        </el-form-item>
        <el-form-item label="部署环境">
          <el-radio-group v-model="deployForm.env">
            <el-radio label="开发环境">开发环境</el-radio>
            <el-radio label="测试环境">测试环境</el-radio>
            <el-radio label="生产环境">生产环境</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="副本数">
          <el-input-number v-model="deployForm.replicas" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deployVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDeploy">开始部署</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Cpu, Star } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import { aiModels, owners } from '@/mock'
import { int, pick, dateStr, genRows } from '@/mock/helpers'

const typeOptions = ['对话模型', '代码模型', 'Embedding', '多模态']
const providerOptions = ['本地部署', 'DeepSeek开放平台', '阿里云百炼', '智谱AI', 'OpenAI', '火山方舟']

// 生成完整的模型 mock 数据
const allModels = ref(aiModels.map((m, i) => ({
  ...m,
  currentVersion: (2 + int(0, 3)).toFixed(1),
  taskCount: int(1, 20),
  scenarios: pick([
    ['SQL生成', '优化建议'],
    ['SQL生成', '优化建议', '错误诊断'],
    ['SQL生成', '数据解释'],
    ['错误诊断', '优化建议']
  ]),
  description: '用于数据开发场景的智能辅助模型，支持SQL生成、查询优化和错误诊断等功能。',
  accuracy: (90 + Math.random() * 8).toFixed(1),
  latency: int(120, 500),
  todayCalls: int(100, 5000),
  totalCalls: int(10000, 500000),
  createdAt: dateStr(int(30, 180)),
  versions: genRows(int(2, 5), vi => ({
    version: (2 - vi * 0.1).toFixed(1),
    status: vi === 0 ? '已发布' : vi === 1 ? '测试中' : '草稿',
    isCurrent: vi === 0,
    description: pick([
      '优化SQL生成准确率，修复多表关联查询问题',
      '新增优化建议功能，支持索引推荐',
      '提升错误诊断能力，支持更多异常类型',
      '性能优化，降低推理延迟30%',
      '新增多轮对话能力，支持上下文理解'
    ]),
    releaseTime: dateStr(int(vi * 10, vi * 20)),
    releaser: pick(owners),
    size: pick(['1.2GB', '2.5GB', '4.8GB', '7.2GB', '13.5GB'])
  })),
  tasks: genRows(int(3, 8), ti => ({
    id: int(10000, 99999),
    name: pick(['用户行为分析任务', '订单数据ETL', '用户画像构建', '销售报表生成', '数据质量检测', '日志分析任务']),
    type: pick(['SQL脚本', '处理流程', '工作流', '数据同步']),
    status: pick(['运行中', '已停止', '运行中', '运行中']),
    owner: pick(owners),
    updatedAt: dateStr(int(0, 30))
  }))
})))

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ keyword: '', type: '', status: '', page: 1, size: 10 })

// 统计数据
const statData = reactive({
  total: 0,
  running: 0,
  versions: 0,
  calls: 0
})

function computeStats() {
  const list = allModels.value
  statData.total = list.length
  statData.running = list.filter(m => m.status === '运行中').length
  statData.versions = list.reduce((sum, m) => sum + (m.versions?.length || 0), 0)
  statData.calls = list.reduce((sum, m) => sum + (m.todayCalls || 0), 0)
}

function loadData() {
  loading.value = true
  setTimeout(() => {
    let list = allModels.value.filter(m =>
      (!query.keyword || m.name.includes(query.keyword) || m.provider.includes(query.keyword)) &&
      (!query.type || m.type === query.type) &&
      (!query.status || m.status === query.status)
    )
    total.value = list.length
    tableData.value = list.slice((query.page - 1) * query.size, query.page * query.size)
    computeStats()
    loading.value = false
  }, 300)
}

const typeTagType = t => ({ 对话模型: 'primary', 代码模型: 'success', Embedding: 'warning', 多模态: 'info' }[t] || '')
const typeIconClass = t => ({ 对话模型: 'md-icon-chat', 代码模型: 'md-icon-code', Embedding: 'md-icon-embed', 多模态: 'md-icon-multi' }[t] || '')

const statusDotClass = s => ({
  '运行中': 'md-dot-success',
  '已停用': 'md-dot-gray',
  '部署中': 'md-dot-warning',
  '成功': 'md-dot-success',
  '已停止': 'md-dot-gray'
}[s] || 'md-dot-gray')

// 详情抽屉
const detailVisible = ref(false)
const detailTab = ref('basic')
const currentModel = ref(null)

function openDetail(row) {
  currentModel.value = row
  detailTab.value = 'basic'
  detailVisible.value = true
}

function openVersion(row) {
  currentModel.value = row
  detailTab.value = 'version'
  detailVisible.value = true
}

// 编辑弹窗
const editVisible = ref(false)
const form = reactive({
  id: null, name: '', type: '代码模型', provider: '本地部署',
  api: '', key: '', context: '64K', qpsLimit: 20,
  scenarios: ['SQL生成'], description: ''
})

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      ...row,
      scenarios: row.scenarios || ['SQL生成']
    })
  } else {
    Object.assign(form, {
      id: null, name: '', type: '代码模型', provider: '本地部署',
      api: '', key: '', context: '64K', qpsLimit: 20,
      scenarios: ['SQL生成'], description: ''
    })
  }
  editVisible.value = true
}

function saveModel() {
  if (!form.name.trim()) return ElMessage.warning('请输入模型名称')
  if (form.id) {
    const idx = allModels.value.findIndex(m => m.id === form.id)
    if (idx !== -1) {
      allModels.value[idx] = { ...allModels.value[idx], ...form, updatedAt: dateStr(0) }
      ElMessage.success('模型已更新')
    }
  } else {
    allModels.value.unshift({
      ...form,
      id: Date.now(),
      status: '运行中',
      owner: '当前用户',
      createdAt: dateStr(0),
      updatedAt: dateStr(0),
      isDefault: false,
      currentVersion: '1.0',
      taskCount: 0,
      accuracy: '92.5',
      latency: 300,
      todayCalls: 0,
      totalCalls: 0,
      versions: [{
        version: '1.0',
        status: '已发布',
        isCurrent: true,
        description: '初始版本',
        releaseTime: dateStr(0),
        releaser: '当前用户',
        size: '2.5GB'
      }],
      tasks: []
    })
    ElMessage.success('模型已创建')
  }
  editVisible.value = false
  loadData()
}

function removeModel(row) {
  ElMessageBox.confirm(`确定删除模型「${row.name}」吗？删除后不可恢复。`, '确认删除', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(() => {
    allModels.value = allModels.value.filter(m => m.id !== row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

// 部署弹窗
const deployVisible = ref(false)
const deployModelName = ref('')
const deployVersions = ref([])
const deployForm = reactive({
  version: '',
  env: '开发环境',
  replicas: 1
})

function deployModel(row) {
  deployModelName.value = row.name
  deployVersions.value = row.versions?.map(v => v.version) || ['1.0']
  deployForm.version = deployVersions.value[0]
  deployForm.env = '开发环境'
  deployForm.replicas = 1
  deployVisible.value = true
}

function confirmDeploy() {
  ElMessage.success(`模型 ${deployModelName.value} v${deployForm.version} 已开始部署到${deployForm.env}`)
  deployVisible.value = false
}

loadData()
</script>

<style scoped>
.md-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.md-model-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

.md-model-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  flex-shrink: 0;
}

.md-icon-chat { background: linear-gradient(135deg, #409eff, #66b1ff); }
.md-icon-code { background: linear-gradient(135deg, #67c23a, #85ce61); }
.md-icon-embed { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.md-icon-multi { background: linear-gradient(135deg, #909399, #a6a9ad); }

.md-name-text {
  font-weight: 600;
  color: var(--dp-text-primary);
  font-size: 14px;
}

.md-version-text {
  font-size: 12px;
  color: var(--dp-text-secondary);
  margin-top: 2px;
}

.md-task-count {
  font-weight: 600;
  color: var(--dp-primary);
}

.md-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.md-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.md-dot-success { background: var(--dp-success); box-shadow: 0 0 6px var(--dp-success); }
.md-dot-warning { background: var(--dp-warning); box-shadow: 0 0 6px var(--dp-warning); }
.md-dot-gray { background: var(--dp-text-tertiary); }

/* 详情抽屉 */
.md-detail-section {
  padding: 4px 0;
}

.md-detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--dp-bg-light);
  border-radius: 8px;
}

.md-detail-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.md-detail-name {
  margin: 0 0 6px 0;
  font-size: 18px;
  color: var(--dp-text-primary);
}

.md-detail-sub {
  display: flex;
  align-items: center;
  font-size: 13px;
}

.md-section-title {
  margin: 20px 0 12px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-primary);
  padding-left: 8px;
  border-left: 3px solid var(--dp-primary);
}

.md-metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.md-metric-item {
  text-align: center;
  padding: 16px 8px;
  background: var(--dp-bg-light);
  border-radius: 8px;
}

.md-metric-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--dp-primary);
  margin-bottom: 4px;
}

.md-metric-label {
  font-size: 12px;
  color: var(--dp-text-secondary);
}

/* 版本历史 */
.md-version-toolbar {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.md-version-item {
  padding: 12px 0;
}

.md-version-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.md-version-num {
  font-weight: 600;
  color: var(--dp-text-primary);
  font-size: 14px;
}

.md-current-badge {
  margin-left: auto;
  font-size: 12px;
  color: var(--dp-primary);
  font-weight: 500;
}

.md-version-desc {
  font-size: 13px;
  color: var(--dp-text-secondary);
  margin-bottom: 6px;
  line-height: 1.5;
}

.md-version-meta {
  font-size: 12px;
  color: var(--dp-text-tertiary);
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
}

.md-version-actions {
  display: flex;
  gap: 12px;
}

@media (max-width: 1200px) {
  .md-stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .md-metric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
