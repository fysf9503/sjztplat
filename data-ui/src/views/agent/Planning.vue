<template>
  <div class="dp-page planning-page">
    <div class="dp-page-header dp-fade-up">
      <div>
        <div class="dp-page-eyebrow">AUTONOMOUS PLANNING</div>
        <h2 class="dp-page-title">自主规划</h2>
        <p class="dp-page-desc">配置智能体的自主规划能力，包括工具调用、任务分解和执行策略。</p>
      </div>
      <div class="dp-page-header-actions">
        <el-button :icon="Refresh" @click="refresh">刷新</el-button>
        <el-button type="primary" :icon="Check" @click="saveConfig">保存配置</el-button>
      </div>
    </div>

    <div class="planning-body">
      <!-- 左侧智能体列表 -->
      <div class="dp-card planning-left">
        <div class="dp-card-title" style="margin-bottom: 12px">智能体列表</div>
        <div class="agent-list">
          <div
            v-for="agent in agentList"
            :key="agent.id"
            class="agent-item"
            :class="{ active: currentAgent?.id === agent.id }"
            @click="selectAgent(agent)"
          >
            <div class="agent-avatar" :style="{ background: agent.color }">
              <el-icon :size="18"><Cpu /></el-icon>
            </div>
            <div class="agent-info">
              <div class="agent-name">{{ agent.name }}</div>
              <div class="agent-desc">{{ agent.model }}</div>
            </div>
            <el-tag v-if="agent.planningEnabled" type="success" size="small" effect="plain">已启用</el-tag>
            <el-tag v-else type="info" size="small" effect="plain">未启用</el-tag>
          </div>
        </div>
      </div>

      <!-- 右侧配置面板 -->
      <div class="planning-right">
        <div v-if="currentAgent" class="dp-fade-in">
          <!-- 步骤模板 -->
          <div class="dp-card">
            <div class="dp-card-header">
              <div class="dp-card-title">
                <el-icon color="var(--dp-primary)"><List /></el-icon>
                <span style="margin-left: 6px">步骤模板</span>
              </div>
              <el-button type="primary" link size="small" :icon="Plus" @click="addStep">添加步骤</el-button>
            </div>
            <div class="step-list">
              <div v-for="(step, idx) in config.steps" :key="idx" class="step-item">
                <div class="step-number">{{ idx + 1 }}</div>
                <div class="step-content">
                  <el-input v-model="step.name" placeholder="步骤名称" size="small" style="margin-bottom: 8px" />
                  <el-select v-model="step.type" size="small" placeholder="步骤类型" style="width: 100%">
                    <el-option label="知识库检索:" value="knowledge" />
                    <el-option label="工具调用" value="tool" />
                    <el-option label="模型推理" value="inference" />
                    <el-option label="数据查询" value="query" />
                    <el-option label="结果整合" value="aggregate" />
                  </el-select>
                </div>
                <div class="step-actions">
                  <el-button text :icon="Top" size="small" :disabled="idx === 0" @click="moveStep(idx, -1)" />
                  <el-button text :icon="Bottom" size="small" :disabled="idx === config.steps.length - 1" @click="moveStep(idx, 1)" />
                  <el-button text type="danger" :icon="Delete" size="small" @click="removeStep(idx)" />
                </div>
              </div>
            </div>
          </div>

          <!-- 工具选择 -->
          <div class="dp-card">
            <div class="dp-card-header">
              <div class="dp-card-title">
                <el-icon color="var(--dp-warning)"><Setting /></el-icon>
                <span style="margin-left: 6px">工具选择</span>
              </div>
              <span class="dp-card-extra">已选 {{ selectedTools.length }} / {{ tools.length }} 个工具</span>
            </div>
            <div class="tool-grid">
              <div
                v-for="tool in tools"
                :key="tool.id"
                class="tool-item"
                :class="{ selected: selectedTools.includes(tool.id) }"
                @click="toggleTool(tool.id)"
              >
                <div class="tool-icon" :style="{ background: tool.bg, color: tool.color }">
                  <el-icon :size="18"><component :is="tool.icon" /></el-icon>
                </div>
                <div class="tool-info">
                  <div class="tool-name">{{ tool.name }}</div>
                  <div class="tool-desc">{{ tool.desc }}</div>
                </div>
                <el-checkbox :model-value="selectedTools.includes(tool.id)" @change="toggleTool(tool.id)" />
              </div>
            </div>
          </div>

          <!-- 执行策略 -->
          <div class="dp-card">
            <div class="dp-card-header">
              <div class="dp-card-title">
                <el-icon color="var(--dp-purple)"><Operation /></el-icon>
                <span style="margin-left: 6px">执行策略</span>
              </div>
            </div>
            <el-form :model="config.strategy" label-width="140px" class="strategy-form">
              <el-form-item label="最大迭代次数">
                <el-input-number v-model="config.strategy.maxIterations" :min="1" :max="20" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">次</span>
              </el-form-item>
              <el-form-item label="任务分解模式">
                <el-radio-group v-model="config.strategy.decomposeMode">
                  <el-radio value="auto">自动分解</el-radio>
                  <el-radio value="manual">手动定义</el-radio>
                  <el-radio value="hybrid">混合模式</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="工具调用策略">
                <el-radio-group v-model="config.strategy.toolStrategy">
                  <el-radio value="parallel">并行调用</el-radio>
                  <el-radio value="sequential">顺序调用</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="失败重试次数">
                <el-input-number v-model="config.strategy.retryCount" :min="0" :max="10" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">次</span>
              </el-form-item>
              <el-form-item label="超时时间">
                <el-input-number v-model="config.strategy.timeout" :min="10" :max="600" />
                <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">秒</span>
              </el-form-item>
              <el-form-item label="启用自主规划">
                <el-switch v-model="config.strategy.enabled" />
              </el-form-item>
            </el-form>
          </div>
        </div>
        <el-empty v-else description="请选择左侧智能体进行配置" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { agents } from '@/mock'
import { Bottom, Check, Delete, Plus, Refresh, Top } from '@element-plus/icons-vue'

const colors = ['#165dff', '#00b42a', '#722ed1', '#ff7d00', '#f759ab', '#0fc6c2']
const agentList = ref(agents.slice(0, 8).map((a, i) => ({
  ...a,
  color: colors[i % colors.length],
  planningEnabled: i % 3 !== 2
})))

const currentAgent = ref(null)

const tools = [
  { id: 1, name: '数据查询', desc: '查询数据库和数据服务', icon: 'Search', bg: '#e8f3ff', color: '#165dff' },
  { id: 2, name: '知识库检索', desc: '从知识库中检索相关文档', icon: 'Document', bg: '#e8ffea', color: '#00b42a' },
  { id: 3, name: 'SQL 生成', desc: '根据自然语言生成 SQL', icon: 'DataLine', bg: '#f5e8ff', color: '#722ed1' },
  { id: 4, name: '图表生成', desc: '自动生成数据可视化图表', icon: 'Histogram', bg: '#fff3e8', color: '#ff7d00' },
  { id: 5, name: 'API 调用', desc: '调用外部 API 接口', icon: 'Connection', bg: '#e0fffa', color: '#0fc6c2' },
  { id: 6, name: '代码执行', desc: '执行 Python/JS 代码片段', icon: 'ChatDotRound', bg: '#ffe8f4', color: '#f759ab' }
]

const selectedTools = ref([1, 2, 3])

const config = reactive({
  steps: [
    { name: '理解用户意图', type: 'inference' },
    { name: '知识库检索', type: 'knowledge' },
    { name: '数据查询', type: 'query' },
    { name: '结果整合与回复', type: 'aggregate' }
  ],
  strategy: {
    maxIterations: 5,
    decomposeMode: 'auto',
    toolStrategy: 'sequential',
    retryCount: 2,
    timeout: 120,
    enabled: true
  }
})

function selectAgent(agent) {
  currentAgent.value = agent
}

function addStep() {
  config.steps.push({ name: '新步骤', type: 'tool' })
}

function removeStep(idx) {
  config.steps.splice(idx, 1)
}

function moveStep(idx, direction) {
  const newIdx = idx + direction
  if (newIdx < 0 || newIdx >= config.steps.length) return
  const temp = config.steps[idx]
  config.steps[idx] = config.steps[newIdx]
  config.steps[newIdx] = temp
}

function toggleTool(id) {
  const idx = selectedTools.value.indexOf(id)
  if (idx > -1) {
    selectedTools.value.splice(idx, 1)
  } else {
    selectedTools.value.push(id)
  }
}

function saveConfig() {
  ElMessage.success('配置已保存')
}

function refresh() {
  ElMessage.success('已刷新')
}
</script>

<style scoped>
.planning-page { min-height: 100%; }
.planning-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.planning-left {
  width: 280px;
  flex-shrink: 0;
  max-height: calc(100vh - 180px);
  overflow-y: auto;
  position: sticky;
  top: 16px;
}
.planning-right { flex: 1; min-width: 0; }

.agent-list { display: flex; flex-direction: column; gap: 6px; }
.agent-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}
.agent-item:hover { background: var(--dp-primary-bg); }
.agent-item.active {
  background: var(--dp-primary-light);
  border-color: var(--dp-primary);
}
.agent-avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.agent-info { flex: 1; min-width: 0; }
.agent-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.agent-desc {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-top: 2px;
  font-family: 'JetBrains Mono', Consolas, monospace;
}

.step-list { display: flex; flex-direction: column; gap: 10px; }
.step-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: var(--dp-bg-page);
  border-radius: 8px;
  border: 1px solid var(--dp-border-light);
}
.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--dp-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}
.step-content { flex: 1; min-width: 0; }
.step-actions {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex-shrink: 0;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 10px;
}
.tool-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.tool-item:hover { border-color: var(--dp-primary); }
.tool-item.selected {
  border-color: var(--dp-primary);
  background: var(--dp-primary-bg);
}
.tool-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.tool-info { flex: 1; min-width: 0; }
.tool-name { font-size: 13px; font-weight: 600; color: var(--dp-text-1); }
.tool-desc { font-size: 11px; color: var(--dp-text-3); margin-top: 2px; }

.strategy-form { max-width: 600px; }
</style>
