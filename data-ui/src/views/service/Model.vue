<template>
  <div class="dp-page model-page">
    <PageHeader title="服务模型配置" desc="管理数据服务网关模板、限流规则和熔断降级策略，保障服务稳定高效运行">
      <el-button type="primary" :icon="Plus" @click="openTemplateEdit()">新建模板</el-button>
    </PageHeader>

    <!-- 统计概览 -->
    <div class="model-stats">
      <div class="model-stat-card">
        <div class="stat-icon" style="background: var(--dp-primary-light); color: var(--dp-primary)">
          <el-icon :size="20"><Collection /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ templates.length }}</div>
          <div class="stat-label">网关模板</div>
        </div>
      </div>
      <div class="model-stat-card">
        <div class="stat-icon" style="background: var(--dp-warning-light); color: var(--dp-warning)">
          <el-icon :size="20"><Odometer /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ rateRules.length }}</div>
          <div class="stat-label">限流规则</div>
        </div>
      </div>
      <div class="model-stat-card">
        <div class="stat-icon" style="background: var(--dp-danger-light); color: var(--dp-danger)">
          <el-icon :size="20"><Connection /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ circuitRules.length }}</div>
          <div class="stat-label">熔断策略</div>
        </div>
      </div>
      <div class="model-stat-card">
        <div class="stat-icon" style="background: var(--dp-success-light); color: var(--dp-success)">
          <el-icon :size="20"><Coin /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ activeTemplates }}</div>
          <div class="stat-label">已启用模板</div>
        </div>
      </div>
    </div>

    <!-- Tab 切换 -->
    <div class="dp-card">
      <el-tabs v-model="activeTab" class="model-tabs">
        <!-- 网关模板 -->
        <el-tab-pane label="网关模板" name="templates">
          <div class="dp-toolbar">
            <el-input
              v-model="templateQuery.keyword"
              placeholder="搜索模板名称 / 标识"
              clearable
              style="width: 240px"
              :prefix-icon="Search"
              @input="loadTemplates"
            />
            <el-select v-model="templateQuery.type" placeholder="模板类型" clearable style="width: 140px" @change="loadTemplates">
              <el-option label="标准模板" value="标准模板" />
              <el-option label="高性能模板" value="高性能模板" />
              <el-option label="安全模板" value="安全模板" />
              <el-option label="自定义模板" value="自定义模板" />
            </el-select>
            <el-select v-model="templateQuery.status" placeholder="状态" clearable style="width: 120px" @change="loadTemplates">
              <el-option label="已启用" value="已启用" />
              <el-option label="已停用" value="已停用" />
            </el-select>
            <div class="dp-toolbar-right">
              <el-button :icon="Refresh" @click="loadTemplates">刷新</el-button>
              <el-button type="primary" :icon="Plus" @click="openTemplateEdit()">新建模板</el-button>
            </div>
          </div>

          <el-table :data="templateRows" border stripe v-loading="loading">
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="name" label="模板名称" min-width="140">
              <template #default="{ row }">
                <div class="template-name-cell">
                  <div class="template-icon" :class="templateIconClass(row.type)">
                    <el-icon><Grid /></el-icon>
                  </div>
                  <div class="template-name-info">
                    <div class="t-name">{{ row.name }}</div>
                    <div class="t-code dp-mono">{{ row.code }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="类型" width="110">
              <template #default="{ row }">
                <el-tag :type="typeTagType(row.type)" size="small" effect="plain">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="关联服务" width="100" align="center">
              <template #default="{ row }">
                <span class="link-count">{{ row.bindCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="限流规则" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.rateRule" type="warning" size="small">{{ row.rateRule }}</el-tag>
                <span v-else class="dp-desc">未配置</span>
              </template>
            </el-table-column>
            <el-table-column label="熔断策略" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.circuitRule" type="danger" size="small">{{ row.circuitRule }}</el-tag>
                <span v-else class="dp-desc">未配置</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <div class="status-cell">
                  <span class="status-dot" :class="row.status === '已启用' ? 'dot-success' : 'dot-info'"></span>
                  <span>{{ row.status }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="owner" label="负责人" width="80" />
            <el-table-column prop="updatedAt" label="更新时间" width="160" />
            <el-table-column label="操作" width="220" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="viewTemplate(row)">详情</el-button>
                <el-button link type="primary" size="small" @click="openTemplateEdit(row)">编辑</el-button>
                <el-button link :type="row.status === '已启用' ? 'warning' : 'success'" size="small" @click="toggleTemplate(row)">
                  {{ row.status === '已启用' ? '停用' : '启用' }}
                </el-button>
                <el-button link type="danger" size="small" @click="removeTemplate(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="templateQuery.page"
            :page-size="templateQuery.size"
            :total="templateTotal"
            layout="total, prev, pager, next"
            @current-change="loadTemplates"
          />
        </el-tab-pane>

        <!-- 限流规则 -->
        <el-tab-pane label="限流规则" name="rateRules">
          <div class="dp-toolbar">
            <el-input
              v-model="rateQuery.keyword"
              placeholder="搜索规则名称"
              clearable
              style="width: 240px"
              :prefix-icon="Search"
              @input="loadRateRules"
            />
            <el-select v-model="rateQuery.mode" placeholder="限流模式" clearable style="width: 140px" @change="loadRateRules">
              <el-option label="令牌桶" value="令牌桶" />
              <el-option label="漏桶" value="漏桶" />
              <el-option label="滑动窗口" value="滑动窗口" />
            </el-select>
            <div class="dp-toolbar-right">
              <el-button :icon="Refresh" @click="loadRateRules">刷新</el-button>
              <el-button type="primary" :icon="Plus" @click="openRateEdit()">新建规则</el-button>
            </div>
          </div>

          <el-table :data="rateRows" border stripe v-loading="loading">
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="name" label="规则名称" min-width="140" />
            <el-table-column prop="mode" label="限流模式" width="110">
              <template #default="{ row }">
                <el-tag type="warning" size="small" effect="plain">{{ row.mode }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="限流阈值" width="180">
              <template #default="{ row }">
                <div class="rate-threshold">
                  <span class="rate-value">{{ row.qps }}</span>
                  <span class="rate-unit">QPS</span>
                  <span class="rate-sep">/</span>
                  <span class="rate-value">{{ row.concurrent }}</span>
                  <span class="rate-unit">并发</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="burst" label="突发流量" width="100">
              <template #default="{ row }">{{ row.burst }} 请求</template>
            </el-table-column>
            <el-table-column prop="warmup" label="预热时长" width="100">
              <template #default="{ row }">{{ row.warmup }}s</template>
            </el-table-column>
            <el-table-column label="关联模板" width="100" align="center">
              <template #default="{ row }">{{ row.bindCount }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <div class="status-cell">
                  <span class="status-dot" :class="row.status === '已启用' ? 'dot-success' : 'dot-info'"></span>
                  <span>{{ row.status }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" width="160" />
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openRateEdit(row)">编辑</el-button>
                <el-button link type="primary" size="small" @click="viewRateDetail(row)">详情</el-button>
                <el-button link type="danger" size="small" @click="removeRateRule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="rateQuery.page"
            :page-size="rateQuery.size"
            :total="rateTotal"
            layout="total, prev, pager, next"
            @current-change="loadRateRules"
          />
        </el-tab-pane>

        <!-- 熔断降级 -->
        <el-tab-pane label="熔断降级" name="circuitRules">
          <div class="dp-toolbar">
            <el-input
              v-model="circuitQuery.keyword"
              placeholder="搜索策略名称"
              clearable
              style="width: 240px"
              :prefix-icon="Search"
              @input="loadCircuitRules"
            />
            <el-select v-model="circuitQuery.strategy" placeholder="降级策略" clearable style="width: 140px" @change="loadCircuitRules">
              <el-option label="返回默认值" value="返回默认值" />
              <el-option label="调用备用服务" value="调用备用服务" />
              <el-option label="返回缓存" value="返回缓存" />
              <el-option label="直接失败" value="直接失败" />
            </el-select>
            <div class="dp-toolbar-right">
              <el-button :icon="Refresh" @click="loadCircuitRules">刷新</el-button>
              <el-button type="primary" :icon="Plus" @click="openCircuitEdit()">新建策略</el-button>
            </div>
          </div>

          <el-table :data="circuitRows" border stripe v-loading="loading">
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="name" label="策略名称" min-width="140" />
            <el-table-column label="熔断条件" width="260">
              <template #default="{ row }">
                <div class="circuit-condition">
                  <el-tag size="small" type="danger">错误率 ≥ {{ row.errorRate }}%</el-tag>
                  <span class="condition-sep">且</span>
                  <el-tag size="small" type="warning">请求数 ≥ {{ row.minRequests }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="openDuration" label="熔断时长" width="100">
              <template #default="{ row }">{{ row.openDuration }}s</template>
            </el-table-column>
            <el-table-column prop="halfOpenRequests" label="半开请求数" width="110">
              <template #default="{ row }">{{ row.halfOpenRequests }} 个</template>
            </el-table-column>
            <el-table-column prop="strategy" label="降级策略" width="120">
              <template #default="{ row }">
                <el-tag type="info" size="small" effect="plain">{{ row.strategy }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="关联模板" width="100" align="center">
              <template #default="{ row }">{{ row.bindCount }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <div class="status-cell">
                  <span class="status-dot" :class="row.status === '已启用' ? 'dot-success' : 'dot-info'"></span>
                  <span>{{ row.status }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="updatedAt" label="更新时间" width="160" />
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openCircuitEdit(row)">编辑</el-button>
                <el-button link type="primary" size="small" @click="viewCircuitDetail(row)">详情</el-button>
                <el-button link type="danger" size="small" @click="removeCircuitRule(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="circuitQuery.page"
            :page-size="circuitQuery.size"
            :total="circuitTotal"
            layout="total, prev, pager, next"
            @current-change="loadCircuitRules"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 模板编辑弹窗 -->
    <el-dialog v-model="templateEditVisible" :title="templateForm.id ? '编辑网关模板' : '新建网关模板'" width="640px" destroy-on-close>
      <el-form :model="templateForm" label-width="100px">
        <el-form-item label="模板名称" required>
          <el-input v-model="templateForm.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="模板标识" required>
          <el-input v-model="templateForm.code" placeholder="如：standard-gateway" />
        </el-form-item>
        <el-form-item label="模板类型">
          <el-select v-model="templateForm.type" style="width: 100%">
            <el-option label="标准模板" value="标准模板" />
            <el-option label="高性能模板" value="高性能模板" />
            <el-option label="安全模板" value="安全模板" />
            <el-option label="自定义模板" value="自定义模板" />
          </el-select>
        </el-form-item>
        <el-form-item label="限流规则">
          <el-select v-model="templateForm.rateRuleId" clearable style="width: 100%" placeholder="选择限流规则">
            <el-option v-for="r in rateRules" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="熔断策略">
          <el-select v-model="templateForm.circuitRuleId" clearable style="width: 100%" placeholder="选择熔断策略">
            <el-option v-for="c in circuitRules" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="超时时间">
          <el-input-number v-model="templateForm.timeout" :min="100" :max="60000" :step="100" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 13px">毫秒</span>
        </el-form-item>
        <el-form-item label="缓存配置">
          <el-switch v-model="templateForm.enableCache" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 13px">启用响应缓存</span>
        </el-form-item>
        <el-form-item v-if="templateForm.enableCache" label="缓存时长">
          <el-input-number v-model="templateForm.cacheTtl" :min="1" :max="3600" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 13px">秒</span>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="templateForm.status" active-value="已启用" inactive-value="已停用" />
        </el-form-item>
        <el-form-item label="模板描述">
          <el-input v-model="templateForm.desc" type="textarea" :rows="3" placeholder="模板描述信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>

    <!-- 限流规则编辑弹窗 -->
    <el-dialog v-model="rateEditVisible" :title="rateForm.id ? '编辑限流规则' : '新建限流规则'" width="560px" destroy-on-close>
      <el-form :model="rateForm" label-width="110px">
        <el-form-item label="规则名称" required>
          <el-input v-model="rateForm.name" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="限流模式">
          <el-radio-group v-model="rateForm.mode">
            <el-radio value="令牌桶">令牌桶</el-radio>
            <el-radio value="漏桶">漏桶</el-radio>
            <el-radio value="滑动窗口">滑动窗口</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="QPS 限制" required>
          <el-input-number v-model="rateForm.qps" :min="1" :max="100000" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 13px">次/秒</span>
        </el-form-item>
        <el-form-item label="并发限制">
          <el-input-number v-model="rateForm.concurrent" :min="0" :max="10000" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 13px">个请求</span>
        </el-form-item>
        <el-form-item label="突发流量">
          <el-input-number v-model="rateForm.burst" :min="0" :max="10000" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 13px">个请求</span>
        </el-form-item>
        <el-form-item label="预热时长">
          <el-input-number v-model="rateForm.warmup" :min="0" :max="3600" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 13px">秒</span>
        </el-form-item>
        <el-form-item label="限流响应">
          <el-select v-model="rateForm.response" style="width: 100%">
            <el-option label="返回 429 状态码" value="429" />
            <el-option label="排队等待" value="queue" />
            <el-option label="降级处理" value="fallback" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="rateForm.status" active-value="已启用" inactive-value="已停用" />
        </el-form-item>
        <el-form-item label="规则描述">
          <el-input v-model="rateForm.desc" type="textarea" :rows="2" placeholder="规则描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rateEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRateRule">保存</el-button>
      </template>
    </el-dialog>

    <!-- 熔断策略编辑弹窗 -->
    <el-dialog v-model="circuitEditVisible" :title="circuitForm.id ? '编辑熔断策略' : '新建熔断策略'" width="600px" destroy-on-close>
      <el-form :model="circuitForm" label-width="120px">
        <el-form-item label="策略名称" required>
          <el-input v-model="circuitForm.name" placeholder="请输入策略名称" />
        </el-form-item>
        <el-form-item label="错误率阈值">
          <el-input-number v-model="circuitForm.errorRate" :min="1" :max="100" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 13px">%</span>
        </el-form-item>
        <el-form-item label="最小请求数">
          <el-input-number v-model="circuitForm.minRequests" :min="1" :max="10000" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 13px">个</span>
        </el-form-item>
        <el-form-item label="熔断时长">
          <el-input-number v-model="circuitForm.openDuration" :min="1" :max="3600" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 13px">秒</span>
        </el-form-item>
        <el-form-item label="半开请求数">
          <el-input-number v-model="circuitForm.halfOpenRequests" :min="1" :max="1000" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 13px">个</span>
        </el-form-item>
        <el-form-item label="统计窗口">
          <el-input-number v-model="circuitForm.windowSize" :min="1" :max="300" />
          <span style="margin-left: 8px; color: var(--dp-text-3); font-size: 13px">秒</span>
        </el-form-item>
        <el-form-item label="降级策略">
          <el-select v-model="circuitForm.strategy" style="width: 100%">
            <el-option label="返回默认值" value="返回默认值" />
            <el-option label="调用备用服务" value="调用备用服务" />
            <el-option label="返回缓存" value="返回缓存" />
            <el-option label="直接失败" value="直接失败" />
          </el-select>
        </el-form-item>
        <el-form-item label="降级内容">
          <el-input v-model="circuitForm.fallbackContent" type="textarea" :rows="3" placeholder="降级返回内容（JSON 格式）" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="circuitForm.status" active-value="已启用" inactive-value="已停用" />
        </el-form-item>
        <el-form-item label="策略描述">
          <el-input v-model="circuitForm.desc" type="textarea" :rows="2" placeholder="策略描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="circuitEditVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCircuitRule">保存</el-button>
      </template>
    </el-dialog>

    <!-- 模板详情抽屉 -->
    <el-drawer v-model="templateDetailVisible" title="模板详情" size="480px" destroy-on-close>
      <template v-if="currentTemplate.id">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="模板名称">{{ currentTemplate.name }}</el-descriptions-item>
          <el-descriptions-item label="模板标识"><span class="dp-mono">{{ currentTemplate.code }}</span></el-descriptions-item>
          <el-descriptions-item label="模板类型">
            <el-tag :type="typeTagType(currentTemplate.type)" size="small">{{ currentTemplate.type }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="关联服务数">{{ currentTemplate.bindCount }} 个</el-descriptions-item>
          <el-descriptions-item label="限流规则">{{ currentTemplate.rateRule || '未配置' }}</el-descriptions-item>
          <el-descriptions-item label="熔断策略">{{ currentTemplate.circuitRule || '未配置' }}</el-descriptions-item>
          <el-descriptions-item label="超时时间">{{ currentTemplate.timeout }} ms</el-descriptions-item>
          <el-descriptions-item label="缓存配置">
            {{ currentTemplate.enableCache ? ('启用，' + currentTemplate.cacheTtl + '秒') : '未启用' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <span class="status-dot" :class="currentTemplate.status === '已启用' ? 'dot-success' : 'dot-info'"></span>
            {{ currentTemplate.status }}
          </el-descriptions-item>
          <el-descriptions-item label="负责人">{{ currentTemplate.owner }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ currentTemplate.updatedAt }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px">
          <div class="detail-label">模板描述</div>
          <div class="detail-content">{{ currentTemplate.desc || '暂无描述' }}</div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const activeTab = ref('templates')

// --- 网关模板 ---
const templates = ref([])
const templateRows = ref([])
const templateTotal = ref(0)
const templateEditVisible = ref(false)
const templateDetailVisible = ref(false)
const currentTemplate = ref({})

const templateQuery = reactive({
  keyword: '',
  type: '',
  status: '',
  page: 1,
  size: 10
})

const templateForm = reactive({
  id: null,
  name: '',
  code: '',
  type: '标准模板',
  rateRuleId: null,
  circuitRuleId: null,
  timeout: 3000,
  enableCache: false,
  cacheTtl: 60,
  status: '已启用',
  desc: ''
})

const activeTemplates = computed(() => templates.value.filter(t => t.status === '已启用').length)

function typeTagType(t) {
  if (t === '标准模板') return 'primary'
  if (t === '高性能模板') return 'success'
  if (t === '安全模板') return 'warning'
  if (t === '自定义模板') return 'info'
  return 'info'
}

function templateIconClass(t) {
  if (t === '标准模板') return 'tpl-primary'
  if (t === '高性能模板') return 'tpl-success'
  if (t === '安全模板') return 'tpl-warning'
  return 'tpl-info'
}

function loadTemplates() {
  loading.value = true
  setTimeout(() => {
    let list = templates.value.filter(t => {
      const matchKw = !templateQuery.keyword ||
        t.name.includes(templateQuery.keyword) ||
        t.code.includes(templateQuery.keyword)
      const matchType = !templateQuery.type || t.type === templateQuery.type
      const matchStatus = !templateQuery.status || t.status === templateQuery.status
      return matchKw && matchType && matchStatus
    })
    templateTotal.value = list.length
    templateRows.value = list.slice((templateQuery.page - 1) * templateQuery.size, templateQuery.page * templateQuery.size)
    loading.value = false
  }, 200)
}

function openTemplateEdit(row) {
  if (row) {
    Object.assign(templateForm, row)
  } else {
    Object.assign(templateForm, {
      id: null,
      name: '',
      code: '',
      type: '标准模板',
      rateRuleId: null,
      circuitRuleId: null,
      timeout: 3000,
      enableCache: false,
      cacheTtl: 60,
      status: '已启用',
      desc: ''
    })
  }
  templateEditVisible.value = true
}

function saveTemplate() {
  if (!templateForm.name) return ElMessage.warning('请输入模板名称')
  if (!templateForm.code) return ElMessage.warning('请输入模板标识')

  const rateRule = rateRules.value.find(r => r.id === templateForm.rateRuleId)
  const circuitRule = circuitRules.value.find(c => c.id === templateForm.circuitRuleId)

  if (templateForm.id) {
    const i = templates.value.findIndex(t => t.id === templateForm.id)
    templates.value[i] = {
      ...templates.value[i],
      ...templateForm,
      rateRule: rateRule?.name || '',
      circuitRule: circuitRule?.name || '',
      updatedAt: '刚刚'
    }
    ElMessage.success('模板更新成功')
  } else {
    templates.value.unshift({
      ...templateForm,
      id: Date.now(),
      rateRule: rateRule?.name || '',
      circuitRule: circuitRule?.name || '',
      bindCount: 0,
      owner: '张伟',
      updatedAt: '刚刚'
    })
    ElMessage.success('模板创建成功')
  }
  templateEditVisible.value = false
  loadTemplates()
}

function toggleTemplate(row) {
  const action = row.status === '已启用' ? '停用' : '启用'
  ElMessageBox.confirm(`确定${action}模板「${row.name}」吗？`, '操作确认', { type: 'warning' })
    .then(() => {
      const i = templates.value.findIndex(t => t.id === row.id)
      templates.value[i].status = action === '启用' ? '已启用' : '已停用'
      ElMessage.success(`已${action}`)
      loadTemplates()
    })
    .catch(() => {})
}

function removeTemplate(row) {
  ElMessageBox.confirm(`确定删除模板「${row.name}」吗？关联服务将失去网关配置`, '删除确认', { type: 'warning' })
    .then(() => {
      templates.value = templates.value.filter(t => t.id !== row.id)
      ElMessage.success('删除成功')
      loadTemplates()
    })
    .catch(() => {})
}

function viewTemplate(row) {
  currentTemplate.value = row
  templateDetailVisible.value = true
}

// --- 限流规则 ---
const rateRules = ref([])
const rateRows = ref([])
const rateTotal = ref(0)
const rateEditVisible = ref(false)

const rateQuery = reactive({
  keyword: '',
  mode: '',
  page: 1,
  size: 10
})

const rateForm = reactive({
  id: null,
  name: '',
  mode: '令牌桶',
  qps: 100,
  concurrent: 50,
  burst: 50,
  warmup: 10,
  response: '429',
  status: '已启用',
  desc: ''
})

function loadRateRules() {
  loading.value = true
  setTimeout(() => {
    let list = rateRules.value.filter(r => {
      const matchKw = !rateQuery.keyword || r.name.includes(rateQuery.keyword)
      const matchMode = !rateQuery.mode || r.mode === rateQuery.mode
      return matchKw && matchMode
    })
    rateTotal.value = list.length
    rateRows.value = list.slice((rateQuery.page - 1) * rateQuery.size, rateQuery.page * rateQuery.size)
    loading.value = false
  }, 200)
}

function openRateEdit(row) {
  if (row) {
    Object.assign(rateForm, row)
  } else {
    Object.assign(rateForm, {
      id: null,
      name: '',
      mode: '令牌桶',
      qps: 100,
      concurrent: 50,
      burst: 50,
      warmup: 10,
      response: '429',
      status: '已启用',
      desc: ''
    })
  }
  rateEditVisible.value = true
}

function saveRateRule() {
  if (!rateForm.name) return ElMessage.warning('请输入规则名称')
  if (rateForm.id) {
    const i = rateRules.value.findIndex(r => r.id === rateForm.id)
    rateRules.value[i] = { ...rateRules.value[i], ...rateForm, updatedAt: '刚刚' }
    ElMessage.success('规则更新成功')
  } else {
    rateRules.value.unshift({
      ...rateForm,
      id: Date.now(),
      bindCount: 0,
      updatedAt: '刚刚'
    })
    ElMessage.success('规则创建成功')
  }
  rateEditVisible.value = false
  loadRateRules()
}

function removeRateRule(row) {
  ElMessageBox.confirm(`确定删除限流规则「${row.name}」吗？`, '删除确认', { type: 'warning' })
    .then(() => {
      rateRules.value = rateRules.value.filter(r => r.id !== row.id)
      ElMessage.success('删除成功')
      loadRateRules()
    })
    .catch(() => {})
}

function viewRateDetail(row) {
  ElMessage.info(`规则「${row.name}」详情：QPS ${row.qps}，并发 ${row.concurrent}，模式 ${row.mode}`)
}

// --- 熔断降级 ---
const circuitRules = ref([])
const circuitRows = ref([])
const circuitTotal = ref(0)
const circuitEditVisible = ref(false)

const circuitQuery = reactive({
  keyword: '',
  strategy: '',
  page: 1,
  size: 10
})

const circuitForm = reactive({
  id: null,
  name: '',
  errorRate: 50,
  minRequests: 20,
  openDuration: 30,
  halfOpenRequests: 5,
  windowSize: 10,
  strategy: '返回默认值',
  fallbackContent: '{"code": 503, "message": "服务暂时不可用"}',
  status: '已启用',
  desc: ''
})

function loadCircuitRules() {
  loading.value = true
  setTimeout(() => {
    let list = circuitRules.value.filter(c => {
      const matchKw = !circuitQuery.keyword || c.name.includes(circuitQuery.keyword)
      const matchStrategy = !circuitQuery.strategy || c.strategy === circuitQuery.strategy
      return matchKw && matchStrategy
    })
    circuitTotal.value = list.length
    circuitRows.value = list.slice((circuitQuery.page - 1) * circuitQuery.size, circuitQuery.page * circuitQuery.size)
    loading.value = false
  }, 200)
}

function openCircuitEdit(row) {
  if (row) {
    Object.assign(circuitForm, row)
  } else {
    Object.assign(circuitForm, {
      id: null,
      name: '',
      errorRate: 50,
      minRequests: 20,
      openDuration: 30,
      halfOpenRequests: 5,
      windowSize: 10,
      strategy: '返回默认值',
      fallbackContent: '{"code": 503, "message": "服务暂时不可用"}',
      status: '已启用',
      desc: ''
    })
  }
  circuitEditVisible.value = true
}

function saveCircuitRule() {
  if (!circuitForm.name) return ElMessage.warning('请输入策略名称')
  if (circuitForm.id) {
    const i = circuitRules.value.findIndex(c => c.id === circuitForm.id)
    circuitRules.value[i] = { ...circuitRules.value[i], ...circuitForm, updatedAt: '刚刚' }
    ElMessage.success('策略更新成功')
  } else {
    circuitRules.value.unshift({
      ...circuitForm,
      id: Date.now(),
      bindCount: 0,
      updatedAt: '刚刚'
    })
    ElMessage.success('策略创建成功')
  }
  circuitEditVisible.value = false
  loadCircuitRules()
}

function removeCircuitRule(row) {
  ElMessageBox.confirm(`确定删除熔断策略「${row.name}」吗？`, '删除确认', { type: 'warning' })
    .then(() => {
      circuitRules.value = circuitRules.value.filter(c => c.id !== row.id)
      ElMessage.success('删除成功')
      loadCircuitRules()
    })
    .catch(() => {})
}

function viewCircuitDetail(row) {
  ElMessage.info(`策略「${row.name}」：错误率${row.errorRate}%触发，熔断${row.openDuration}秒`)
}

// --- 初始化 mock 数据 ---
function initMockData() {
  rateRules.value = [
    { id: 1, name: '标准限流-100QPS', mode: '令牌桶', qps: 100, concurrent: 50, burst: 50, warmup: 10, response: '429', status: '已启用', bindCount: 12, desc: '标准限流规则，适用于普通查询接口', updatedAt: '2024-01-15 10:30' },
    { id: 2, name: '高并发-500QPS', mode: '令牌桶', qps: 500, concurrent: 200, burst: 200, warmup: 30, response: '429', status: '已启用', bindCount: 5, desc: '高并发限流，适用于核心业务接口', updatedAt: '2024-01-14 16:20' },
    { id: 3, name: '轻量限流-20QPS', mode: '漏桶', qps: 20, concurrent: 10, burst: 10, warmup: 5, response: 'queue', status: '已启用', bindCount: 8, desc: '轻量级限流，适用于管理后台接口', updatedAt: '2024-01-13 09:15' },
    { id: 4, name: '严格限流-10QPS', mode: '滑动窗口', qps: 10, concurrent: 5, burst: 5, warmup: 0, response: '429', status: '已启用', bindCount: 3, desc: '严格限流，适用于计费类接口', updatedAt: '2024-01-12 14:00' },
    { id: 5, name: '测试限流规则', mode: '令牌桶', qps: 5, concurrent: 2, burst: 2, warmup: 0, response: '429', status: '已停用', bindCount: 0, desc: '仅供测试使用', updatedAt: '2024-01-10 11:30' },
    { id: 6, name: '批量接口限流', mode: '滑动窗口', qps: 30, concurrent: 15, burst: 20, warmup: 15, response: 'fallback', status: '已启用', bindCount: 4, desc: '批量数据接口专用限流', updatedAt: '2024-01-08 15:45' }
  ]

  circuitRules.value = [
    { id: 1, name: '标准熔断策略', errorRate: 50, minRequests: 20, openDuration: 30, halfOpenRequests: 5, windowSize: 10, strategy: '返回默认值', fallbackContent: '{"code":503,"message":"服务降级中"}', status: '已启用', bindCount: 10, desc: '标准熔断降级策略', updatedAt: '2024-01-15 10:00' },
    { id: 2, name: '快速熔断策略', errorRate: 30, minRequests: 10, openDuration: 60, halfOpenRequests: 3, windowSize: 5, strategy: '返回缓存', fallbackContent: '{}', status: '已启用', bindCount: 6, desc: '快速熔断，适用于核心接口', updatedAt: '2024-01-14 14:30' },
    { id: 3, name: '宽松熔断策略', errorRate: 80, minRequests: 50, openDuration: 15, halfOpenRequests: 10, windowSize: 30, strategy: '调用备用服务', fallbackContent: '', status: '已启用', bindCount: 4, desc: '宽松阈值，适用于非核心接口', updatedAt: '2024-01-13 11:20' },
    { id: 4, name: '直接失败策略', errorRate: 60, minRequests: 30, openDuration: 45, halfOpenRequests: 5, windowSize: 15, strategy: '直接失败', fallbackContent: '', status: '已停用', bindCount: 0, desc: '熔断后直接返回失败', updatedAt: '2024-01-11 09:00' }
  ]

  templates.value = [
    { id: 1, name: '标准网关模板', code: 'standard-gateway', type: '标准模板', rateRule: '标准限流-100QPS', rateRuleId: 1, circuitRule: '标准熔断策略', circuitRuleId: 1, timeout: 3000, enableCache: true, cacheTtl: 60, status: '已启用', bindCount: 15, owner: '张伟', desc: '标准网关配置模板，适用于大多数数据服务接口', updatedAt: '2024-01-15 10:30' },
    { id: 2, name: '高性能网关模板', code: 'high-perf-gateway', type: '高性能模板', rateRule: '高并发-500QPS', rateRuleId: 2, circuitRule: '快速熔断策略', circuitRuleId: 2, timeout: 1000, enableCache: true, cacheTtl: 30, status: '已启用', bindCount: 8, owner: '李娜', desc: '高性能网关配置，适用于高并发核心接口', updatedAt: '2024-01-14 16:00' },
    { id: 3, name: '安全网关模板', code: 'secure-gateway', type: '安全模板', rateRule: '严格限流-10QPS', rateRuleId: 4, circuitRule: '标准熔断策略', circuitRuleId: 1, timeout: 5000, enableCache: false, cacheTtl: 0, status: '已启用', bindCount: 5, owner: '王强', desc: '高安全级别网关，适用于敏感数据接口', updatedAt: '2024-01-13 14:20' },
    { id: 4, name: '管理后台模板', code: 'admin-gateway', type: '自定义模板', rateRule: '轻量限流-20QPS', rateRuleId: 3, circuitRule: '宽松熔断策略', circuitRuleId: 3, timeout: 10000, enableCache: false, cacheTtl: 0, status: '已启用', bindCount: 6, owner: '张伟', desc: '管理后台专用网关模板', updatedAt: '2024-01-12 11:00' },
    { id: 5, name: '批量处理模板', code: 'batch-gateway', type: '自定义模板', rateRule: '批量接口限流', rateRuleId: 6, circuitRule: '宽松熔断策略', circuitRuleId: 3, timeout: 30000, enableCache: false, cacheTtl: 0, status: '已启用', bindCount: 3, owner: '刘洋', desc: '批量数据处理专用网关', updatedAt: '2024-01-10 15:30' },
    { id: 6, name: '测试模板', code: 'test-gateway', type: '自定义模板', rateRule: '测试限流规则', rateRuleId: 5, circuitRule: '', circuitRuleId: null, timeout: 5000, enableCache: false, cacheTtl: 0, status: '已停用', bindCount: 0, owner: '测试员', desc: '测试环境专用', updatedAt: '2024-01-08 09:00' }
  ]
}

// 初始化
initMockData()
loadTemplates()
loadRateRules()
loadCircuitRules()
</script>

<style scoped>
.model-page {
  padding-bottom: 20px;
}

.model-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.model-stat-card {
  flex: 1;
  background: var(--dp-bg-card);
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: all 0.2s;
}

.model-stat-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-info {
  flex: 1;
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: var(--dp-text-1);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--dp-text-3);
  margin-top: 4px;
}

.model-tabs {
  margin-top: 0;
}

.model-tabs :deep(.el-tabs__header) {
  margin: 0 0 16px 0;
  padding: 0 4px;
}

.template-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.template-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tpl-primary {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
}

.tpl-success {
  background: var(--dp-success-light);
  color: var(--dp-success);
}

.tpl-warning {
  background: var(--dp-warning-light);
  color: var(--dp-warning);
}

.tpl-info {
  background: #f0f2f5;
  color: #909399;
}

.template-name-info {
  min-width: 0;
}

.t-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-1);
}

.t-code {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

.link-count {
  font-weight: 600;
  color: var(--dp-primary);
}

.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

.dot-success {
  background: var(--dp-success);
  box-shadow: 0 0 4px var(--dp-success);
}

.dot-warning {
  background: var(--dp-warning);
  box-shadow: 0 0 4px var(--dp-warning);
}

.dot-info {
  background: #909399;
}

.rate-threshold {
  display: flex;
  align-items: baseline;
  gap: 4px;
  flex-wrap: wrap;
}

.rate-value {
  font-size: 15px;
  font-weight: 700;
  color: var(--dp-primary);
}

.rate-unit {
  font-size: 11px;
  color: var(--dp-text-3);
}

.rate-sep {
  color: var(--dp-border);
  margin: 0 4px;
}

.circuit-condition {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.condition-sep {
  font-size: 12px;
  color: var(--dp-text-3);
}

.detail-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-2);
  margin-bottom: 8px;
}

.detail-content {
  font-size: 13px;
  color: var(--dp-text-1);
  line-height: 1.6;
  padding: 12px;
  background: var(--dp-bg-light);
  border-radius: 6px;
}
</style>
