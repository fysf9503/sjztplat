<template>
  <div class="dp-page workbench-page">
    <div class="dp-page-header dp-fade-up">
      <div>
        <div class="dp-page-eyebrow">WORKBENCH DEBUG</div>
        <h2 class="dp-page-title">工作台调试</h2>
        <p class="dp-page-desc">调试智能体问答效果，支持流式输出和知识库召回查看。</p>
      </div>
      <div class="dp-page-header-actions">
        <el-select v-model="selectedAgent" style="width: 200px" size="default">
          <el-option v-for="a in agentList" :key="a.id" :label="a.name" :value="a.id" />
        </el-select>
        <el-button type="primary" :icon="VideoPlay" @click="startDebug">开始调试</el-button>
      </div>
    </div>

    <div class="wb-body">
      <!-- 左侧对话框 -->
      <div class="dp-card wb-chat-wrap">
        <div class="wb-chat-header">
          <div class="wb-chat-title">
            <el-icon color="var(--dp-primary)"><ChatDotRound /></el-icon>
            <span>对话调试</span>
          </div>
          <div class="wb-chat-actions">
            <el-button text size="small" :icon="Delete" @click="clearChat">清空对话</el-button>
            <el-switch v-model="streamMode" active-text="流式输出" />
          </div>
        </div>

        <div ref="chatBodyRef" class="wb-chat-body">
          <div v-if="messages.length === 0" class="wb-chat-empty">
            <el-icon :size="48" color="#c9cdd4"><ChatDotRound /></el-icon>
            <p>开始与智能体对话，测试问答效果</p>
            <div class="wb-suggestions">
              <div v-for="s in suggestions" :key="s" class="wb-suggestion" @click="sendMessage(s)">
                {{ s }}
              </div>
            </div>
          </div>

          <div v-for="(msg, idx) in messages" :key="idx" class="wb-msg" :class="'wb-msg-' + msg.role">
            <div class="wb-msg-avatar" :class="'avatar-' + msg.role">
              <el-icon :size="18">
                <component :is="msg.role === 'user' ? 'User' : 'Cpu'" />
              </el-icon>
            </div>
            <div class="wb-msg-bubble">
              <div class="wb-msg-content">{{ msg.content }}</div>

              <!-- 引用来源（仅AI消息） -->
              <div v-if="msg.role === 'assistant' && msg.sources && msg.sources.length" class="wb-sources">
                <div class="wb-sources-title">
                  <el-icon :size="12"><Document /></el-icon>
                  <span>引用来源 ({{ msg.sources.length }})</span>
                </div>
                <div class="wb-source-list">
                  <div v-for="(src, sIdx) in msg.sources" :key="sIdx" class="wb-source-item" @click="viewSource(src)">
                    <span class="wb-source-index">[{{ sIdx + 1 }}]</span>
                    <span class="wb-source-name">{{ src.title }}</span>
                    <span class="wb-source-score">{{ (src.score * 100).toFixed(1) }}%</span>
                  </div>
                </div>
              </div>

              <div class="wb-msg-meta">
                <span>{{ msg.time }}</span>
                <span v-if="msg.role === 'assistant' && msg.tokens">
                  tokens: {{ msg.tokens }} · 耗时: {{ msg.cost }}s
                </span>
              </div>
            </div>
          </div>

          <div v-if="isTyping" class="wb-msg wb-msg-assistant">
            <div class="wb-msg-avatar avatar-assistant">
              <el-icon :size="18"><Cpu /></el-icon>
            </div>
            <div class="wb-msg-bubble">
              <div class="wb-typing">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>
        </div>

        <div class="wb-chat-input">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="输入问题，按 Enter 发送，Shift+Enter 换行"
            @keydown.enter.exact.prevent="handleSend"
          />
          <div class="wb-input-actions">
            <el-button :icon="Upload" circle />
            <el-button type="primary" :icon="Promotion" :loading="isTyping" @click="handleSend">发送</el-button>
          </div>
        </div>
      </div>

      <!-- 右侧配置面板 -->
      <div class="wb-side">
        <!-- 模型参数 -->
        <div class="dp-card">
          <div class="dp-card-title" style="margin-bottom: 12px">
            <el-icon color="var(--dp-primary)"><Cpu /></el-icon>
            <span style="margin-left: 6px">模型参数</span>
          </div>
          <el-form label-width="90px" size="small">
            <el-form-item label="模型">
              <el-select v-model="params.model" style="width: 100%">
                <el-option label="DeepSeek-V3" value="DeepSeek-V3" />
                <el-option label="Qwen-Max" value="Qwen-Max" />
                <el-option label="GLM-4-Plus" value="GLM-4-Plus" />
                <el-option label="本地Llama3-70B" value="本地Llama3-70B" />
              </el-select>
            </el-form-item>
            <el-form-item label="Temperature">
              <el-slider v-model="params.temperature" :min="0" :max="2" :step="0.1" show-input />
            </el-form-item>
            <el-form-item label="Top P">
              <el-slider v-model="params.topP" :min="0" :max="1" :step="0.05" show-input />
            </el-form-item>
            <el-form-item label="最大长度">
              <el-input-number v-model="params.maxTokens" :min="256" :max="8192" :step="256" style="width: 100%" />
            </el-form-item>
          </el-form>
        </div>

        <!-- 提示词 -->
        <div class="dp-card">
          <div class="dp-card-title" style="margin-bottom: 12px">
            <el-icon color="var(--dp-warning)"><EditPen /></el-icon>
            <span style="margin-left: 6px">系统提示词</span>
          </div>
          <el-input
            v-model="systemPrompt"
            type="textarea"
            :rows="5"
            placeholder="系统提示词..."
          />
        </div>

        <!-- 知识库选择 -->
        <div class="dp-card">
          <div class="dp-card-title" style="margin-bottom: 12px">
            <el-icon color="var(--dp-purple)"><Collection /></el-icon>
            <span style="margin-left: 6px">知识库</span>
            <span class="dp-card-extra">已选 {{ selectedKbs.length }} 个</span>
          </div>
          <div class="wb-kb-list">
            <div v-for="kb in kbList" :key="kb.id" class="wb-kb-item">
              <el-checkbox v-model="selectedKbs" :label="kb.id" />
              <div class="wb-kb-info">
                <div class="wb-kb-name">{{ kb.name }}</div>
                <div class="wb-kb-meta">{{ kb.docs }} 文档 · {{ kb.chunks }} chunks</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 召回设置 -->
        <div class="dp-card">
          <div class="dp-card-title" style="margin-bottom: 12px">
            <el-icon color="var(--dp-cyan)"><Search /></el-icon>
            <span style="margin-left: 6px">召回设置</span>
          </div>
          <el-form label-width="90px" size="small">
            <el-form-item label="召回数量">
              <el-input-number v-model="recall.topK" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
            <el-form-item label="相似度阈值:">
              <el-slider v-model="recall.threshold" :min="0.5" :max="1" :step="0.05" show-input />
            </el-form-item>
            <el-form-item label="重排序:">
              <el-switch v-model="recall.rerank" />
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 引用详情抽屉 -->
    <el-drawer v-model="sourceDrawerVisible" title="引用来源详情" size="480px">
      <div v-if="currentSource">
        <h3 style="margin-top: 0">{{ currentSource.title }}</h3>
        <div class="wb-source-meta">
          <el-tag size="small" type="info">{{ currentSource.type }}</el-tag>
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 12px">
            相似度: {{ (currentSource.score * 100).toFixed(1) }}%
          </span>
        </div>
        <div class="wb-source-content">{{ currentSource.content }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { agents, knowledgeBases } from '@/mock'
import { Delete, Promotion, Upload, VideoPlay } from '@element-plus/icons-vue'

const agentList = ref(agents)
const selectedAgent = ref(agents[0]?.id)
const streamMode = ref(true)
const inputText = ref('')
const isTyping = ref(false)
const chatBodyRef = ref(null)
const sourceDrawerVisible = ref(false)
const currentSource = ref(null)

const suggestions = [
  '本月销售额最高的渠道是哪个？',
  '帮我分析一下客户流失的原因',
  '查询最近30天的活跃用户趋势',
  '数据质量合格率是多少？'
]

const messages = ref([])

const kbList = ref(knowledgeBases.slice(0, 5))
const selectedKbs = ref([knowledgeBases[0]?.id, knowledgeBases[1]?.id])

const params = reactive({
  model: 'DeepSeek-V3',
  temperature: 0.7,
  topP: 0.9,
  maxTokens: 2048
})

const systemPrompt = ref('你是一个专业的数据助手，能够帮助用户查询数据、分析指标、解读报表。请基于提供的知识库内容准确回答问题，如果不确定请如实告知。')

const recall = reactive({
  topK: 5,
  threshold: 0.7,
  rerank: true
})

const mockSources = [
  { title: '数据中台操作手册-第三章', type: 'PDF', score: 0.92, content: '数据质量合格率是衡量数据质量的核心指标，计算公式为：合格数据 ÷ 总数据量 × 100%。根据最新统计，平台综合数据质量合格率为 96.8%，较上月提升 2.1 个百分点...' },
  { title: '质量管控实施细则', type: 'Word', score: 0.87, content: '质量管控模块包含完整性、准确性、一致性、及时性、唯一性、有效性六大维度。其中完整性权重最高，占比 25%；准确性次之，占比 20%...' },
  { title: '指标口径说明文档', type: 'Markdown', score: 0.81, content: '数据质量合格率指标定义：统计周期内通过质量规则校验的数据记录数占总记录数的比例。统计粒度：自然月。数据来源：quality_results 表...' }
]

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

function handleSend() {
  if (!inputText.value.trim()) return
  sendMessage(inputText.value.trim())
  inputText.value = ''
}

function sendMessage(text) {
  messages.value.push({
    role: 'user',
    content: text,
    time: formatTime(new Date())
  })
  scrollToBottom()

  isTyping.value = true
  const response = generateMockResponse(text)

  if (streamMode.value) {
    setTimeout(() => {
      isTyping.value = false
      const aiMsg = {
        role: 'assistant',
        content: '',
        sources: mockSources,
        time: formatTime(new Date()),
        tokens: 0,
        cost: 0
      }
      messages.value.push(aiMsg)
      scrollToBottom()

      let i = 0
      const fullText = response
      const timer = setInterval(() => {
        if (i < fullText.length) {
          aiMsg.content += fullText[i]
          i++
          scrollToBottom()
        } else {
          clearInterval(timer)
          aiMsg.tokens = Math.floor(fullText.length * 1.5)
          aiMsg.cost = (1.2 + Math.random() * 0.8).toFixed(2)
        }
      }, 20)
    }, 600)
  } else {
    setTimeout(() => {
      isTyping.value = false
      messages.value.push({
        role: 'assistant',
        content: response,
        sources: mockSources,
        time: formatTime(new Date()),
        tokens: Math.floor(response.length * 1.5),
        cost: (1.5 + Math.random()).toFixed(2)
      })
      scrollToBottom()
    }, 1500)
  }
}

function generateMockResponse(question) {
  if (question.includes('销售') || question.includes('渠道')) {
    return '根据最新数据统计，本月销售额最高的渠道是**APP 端**，GMV 达到 1,283.4 万元，占总销售额的 43.2%。\n\n其次是小程序渠道，销售额 862.1 万元，占比 29.0%。\n\n各渠道销售排名：\n1. APP - 1,283.4 万元 (43.2%)\n2. 小程序 - 862.1 万元 (29.0%)\n3. WEB - 421.6 万元 (14.2%)\n4. H5 - 211.1 万元 (7.1%)\n5. 线下扫码 - 158.3 万元 (5.3%)\n\n同比上月，APP 渠道增长 12.5%，小程序增长 8.3%，整体销售额呈上升趋势。'
  }
  if (question.includes('质量') || question.includes('合格率')) {
    return '当前平台综合数据质量合格率为 **96.8%**，较上周提升 0.3 个百分点。\n\n各维度合格率详情：\n- 完整性：98.2%\n- 准确性：95.6%\n- 一致性：97.1%\n- 及时性：96.4%\n- 唯一性：99.1%\n- 有效性：94.8%\n\n本月共执行质量校验任务 1,286 次，发现问题数据 3.2 万条，已整改 2.8 万条，整改率 87.5%。'
  }
  return `好的，关于「${question}」这个问题，我来为您解答：\n\n根据知识库中的相关信息和数据分析结果，以下是详细说明：\n\n1. **核心结论**：当前相关指标处于正常区间，整体表现良好。\n\n2. **关键数据**：\n   - 相关数据量：约 128 万条\n   - 处理时效：平均 2.3 秒\n   - 准确率：95.6%\n\n3. **建议**：\n   - 建议持续监控相关指标\n   - 可进一步优化相关流程\n\n如需更详细的信息，请告诉我您具体关注的方面。`
}

function formatTime(d) {
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

function clearChat() {
  messages.value = []
}

function viewSource(src) {
  currentSource.value = src
  sourceDrawerVisible.value = true
}

function startDebug() {
  ElMessage.success('已进入调试模板')
}
</script>

<style scoped>
.workbench-page { min-height: 100%; }
.wb-body {
  display: flex;
  gap: 16px;
  align-items: stretch;
  height: calc(100vh - 160px);
}
.wb-chat-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin-bottom: 0;
}
.wb-side {
  width: 320px;
  flex-shrink: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.wb-side .dp-card { margin-bottom: 0; }

.wb-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--dp-border-light);
  margin-bottom: 12px;
}
.wb-chat-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
}
.wb-chat-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.wb-chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
}

.wb-chat-empty {
  text-align: center;
  padding: 60px 20px;
  color: var(--dp-text-3);
}
.wb-chat-empty p { margin: 12px 0 20px; }
.wb-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 600px;
  margin: 0 auto;
}
.wb-suggestion {
  padding: 8px 14px;
  background: var(--dp-primary-bg);
  color: var(--dp-primary);
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.wb-suggestion:hover { background: var(--dp-primary-light); }

.wb-msg {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}
.wb-msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
}
.avatar-user { background: var(--dp-primary); }
.avatar-assistant { background: linear-gradient(135deg, #722ed1, #165dff); }

.wb-msg-bubble {
  max-width: 75%;
  background: var(--dp-bg-page);
  border-radius: 12px;
  padding: 12px 14px;
  border: 1px solid var(--dp-border-light);
}
.wb-msg-user { flex-direction: row-reverse; }
.wb-msg-user .wb-msg-bubble {
  background: var(--dp-primary);
  color: #fff;
  border-color: var(--dp-primary);
}

.wb-msg-content {
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.wb-sources {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--dp-border-light);
}
.wb-sources-title {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.wb-source-list { display: flex; flex-direction: column; gap: 4px; }
.wb-source-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  background: #fff;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  border: 1px solid var(--dp-border-light);
}
.wb-source-item:hover { border-color: var(--dp-primary); }
.wb-source-index { color: var(--dp-primary); font-weight: 600; }
.wb-source-name { flex: 1; color: var(--dp-text-2); }
.wb-source-score { color: var(--dp-text-3); font-family: 'DIN Alternate', sans-serif; }

.wb-msg-meta {
  margin-top: 8px;
  font-size: 11px;
  color: var(--dp-text-3);
  display: flex;
  gap: 12px;
}
.wb-msg-user .wb-msg-meta { color: rgba(255,255,255,0.7); }

.wb-typing {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}
.wb-typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--dp-text-3);
  animation: typing 1.4s infinite;
}
.wb-typing span:nth-child(2) { animation-delay: 0.2s; }
.wb-typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-4px); opacity: 1; }
}

.wb-chat-input {
  padding-top: 12px;
  border-top: 1px solid var(--dp-border-light);
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.wb-chat-input :deep(.el-textarea__inner) { border-radius: 10px; }
.wb-input-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  padding-bottom: 4px;
}

.wb-kb-list { display: flex; flex-direction: column; gap: 8px; }
.wb-kb-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px;
  border-radius: 6px;
  transition: background 0.2s;
}
.wb-kb-item:hover { background: var(--dp-bg-page); }
.wb-kb-info { flex: 1; min-width: 0; }
.wb-kb-name { font-size: 13px; color: var(--dp-text-1); }
.wb-kb-meta { font-size: 11px; color: var(--dp-text-3); margin-top: 2px; }

.wb-source-meta { margin-bottom: 12px; }
.wb-source-content {
  padding: 14px;
  background: var(--dp-bg-page);
  border-radius: 8px;
  line-height: 1.8;
  font-size: 13px;
  color: var(--dp-text-2);
}
</style>
