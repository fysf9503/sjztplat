<template>
  <div class="dp-page">
    <PageHeader title="质量规则管理" desc="配置数据质量校验规则：完整性、准确性、一致性、唯一性、时效性、有效性。">
      <el-button type="primary" :icon="Plus" @click="openEdit()">新建规则</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input v-model="query.keyword" placeholder="搜索规则名称 / 规则编码 / 表名" clearable style="width: 260px" :prefix-icon="Search" @input="loadData" />
        <el-select v-model="query.category" placeholder="规则类型" clearable style="width: 130px" @change="loadData">
          <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px" @change="loadData">
          <el-option label="启用" value="启用" />
          <el-option label="停用" value="停用" />
        </el-select>
        <el-select v-model="query.level" placeholder="告警级别" clearable style="width: 110px" @change="loadData">
          <el-option label="高" value="高" />
          <el-option label="中" value="中" />
          <el-option label="低" value="低" />
        </el-select>
        <div class="dp-toolbar-right">
          <el-button :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </div>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="name" label="规则名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="code" label="规则编码" width="140" class-name="dp-mono">
          <template #default="{ row }">
            <span class="rule-code">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="规则类型" width="100">
          <template #default="{ row }">
            <el-tag :type="catTagType(row.category)" size="small" effect="plain">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dimension" label="质量维度" width="90">
          <template #default="{ row }">
            <span class="dim-tag">{{ row.dimension }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="datasource" label="关联数据源/表" min-width="200" class-name="dp-mono" show-overflow-tooltip />
        <el-table-column prop="logic" label="校验逻辑" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="dp-desc">{{ row.logic }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="告警级别" width="80">
          <template #default="{ row }">
            <span class="level-dot" :class="row.level">
              <i></i>{{ row.level }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === '启用'"
              size="small"
              @change="toggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastExecTime" label="最近执行" width="150" />
        <el-table-column prop="execCount" label="执行次数" width="90" align="center" />
        <el-table-column prop="passRate" label="通过率" width="110">
          <template #default="{ row }">
            <el-progress :percentage="row.passRate" :stroke-width="5" :color="getPassRateColor(row.passRate)" />
          </template>
        </el-table-column>
        <el-table-column prop="owner" label="负责人" width="80" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="execNow(row)">立即执行</el-button>
            <el-button link type="primary" size="small" @click="viewHistory(row)">历史</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="removeRule(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @current-change="loadData"
        @size-change="loadData"
      />
    </div>

    <!-- 新建/编辑规则弹窗 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑规则' : '新建规则'" width="720px" destroy-on-close top="5vh">
      <el-tabs v-model="activeTab" class="rule-tabs">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="form" label-width="100px">
            <el-form-item label="规则名称" required>
              <el-input v-model="form.name" placeholder="请输入规则名称，如：订单金额非空校验" />
            </el-form-item>
            <el-form-item label="规则编码" required>
              <el-input v-model="form.code" placeholder="RULE_XXX_001" class="dp-mono" />
            </el-form-item>
            <el-form-item label="规则类型" required>
              <el-select v-model="form.category" style="width: 100%" @change="onCategoryChange">
                <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
            <el-form-item label="质量维度" required>
              <el-select v-model="form.dimension" style="width: 100%">
                <el-option label="完整性" value="完整性" />
                <el-option label="准确性" value="准确性" />
                <el-option label="一致性" value="一致性" />
                <el-option label="唯一性" value="唯一性" />
                <el-option label="时效性" value="时效性" />
                <el-option label="有效性" value="有效性" />
              </el-select>
            </el-form-item>
            <el-form-item label="告警级别" required>
              <el-radio-group v-model="form.level">
                <el-radio-button value="高">
                  <span style="color: var(--dp-danger)">高</span>
                </el-radio-button>
                <el-radio-button value="中">
                  <span style="color: var(--dp-warning)">中</span>
                </el-radio-button>
                <el-radio-button value="低">
                  <span style="color: var(--dp-text-3)">低</span>
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="规则描述">
              <el-input v-model="form.desc" type="textarea" :rows="3" placeholder="请描述规则的业务含义和使用场景" />
            </el-form-item>
          </el-form>
          <div class="tab-footer">
            <el-button type="primary" @click="activeTab = 'config'">下一步</el-button>
          </div>
        </el-tab-pane>

        <!-- 规则配置 -->
        <el-tab-pane label="规则配置" name="config">
          <el-form :model="form" label-width="100px">
            <el-form-item label="数据源" required>
              <el-select v-model="form.datasource" placeholder="请选择数据源" style="width: 100%" filterable>
                <el-option v-for="ds in datasourceOptions" :key="ds" :label="ds" :value="ds" />
              </el-select>
            </el-form-item>
            <el-form-item label="选择表" required>
              <el-select v-model="form.tableName" placeholder="请选择数据表" style="width: 100%" filterable class="dp-mono">
                <el-option v-for="t in tableOptions" :key="t" :label="t" :value="t" />
              </el-select>
            </el-form-item>
            <el-form-item label="选择字段">
              <el-select v-model="form.fieldName" placeholder="请选择字段（可选）" style="width: 100%" multiple collapse-tags collapse-tags-tooltip class="dp-mono">
                <el-option v-for="f in fieldOptions" :key="f" :label="f" :value="f" />
              </el-select>
            </el-form-item>
            <el-form-item label="抽样方式">
              <el-radio-group v-model="form.sampleType">
                <el-radio value="all">全量检测</el-radio>
                <el-radio value="percent">按比例抽样</el-radio>
                <el-radio value="count">按数量抽样</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="form.sampleType === 'percent'" label="抽样比例">
              <el-slider v-model="form.samplePercent" :min="1" :max="100" :step="5" style="width: 60%" />
              <span style="margin-left: 12px; color: var(--dp-text-2)">{{ form.samplePercent }}%</span>
            </el-form-item>
            <el-form-item v-if="form.sampleType === 'count'" label="抽样数量">
              <el-input-number v-model="form.sampleCount" :min="100" :max="100000" :step="100" />
            </el-form-item>
          </el-form>
          <div class="tab-footer">
            <el-button @click="activeTab = 'basic'">上一步</el-button>
            <el-button type="primary" @click="activeTab = 'logic'">下一步</el-button>
          </div>
        </el-tab-pane>

        <!-- 校验逻辑 -->
        <el-tab-pane label="校验逻辑" name="logic">
          <el-form :model="form" label-width="100px">
            <el-form-item label="校验模板" required>
              <el-select v-model="form.template" style="width: 100%" @change="onTemplateChange">
                <el-option v-for="t in templateOptions" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>

            <!-- 非空校验配置 -->
            <template v-if="form.template === 'notnull'">
              <el-form-item label="校验字段">
                <el-select v-model="form.notNullField" style="width: 100%" class="dp-mono">
                  <el-option v-for="f in fieldOptions" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="校验规则">
                <el-alert type="info" :closable="false" show-icon title="字段值不允许为 NULL 或空字符串" />
              </el-form-item>
            </template>

            <!-- 唯一值检查配置 -->
            <template v-if="form.template === 'unique'">
              <el-form-item label="校验字段">
                <el-select v-model="form.uniqueField" style="width: 100%" multiple class="dp-mono" collapse-tags collapse-tags-tooltip>
                  <el-option v-for="f in fieldOptions" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="校验规则">
                <el-alert type="info" :closable="false" show-icon title="所选字段组合值必须唯一，不允许重复" />
              </el-form-item>
            </template>

            <!-- 范围检查配置 -->
            <template v-if="form.template === 'range'">
              <el-form-item label="校验字段">
                <el-select v-model="form.rangeField" style="width: 100%" class="dp-mono">
                  <el-option v-for="f in fieldOptions" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="最小值">
                <el-input v-model="form.minValue" placeholder="请输入最小值" />
              </el-form-item>
              <el-form-item label="最大值">
                <el-input v-model="form.maxValue" placeholder="请输入最大值" />
              </el-form-item>
              <el-form-item label="是否包含边界">
                <el-radio-group v-model="form.includeBoundary">
                  <el-radio value="both">包含两端</el-radio>
                  <el-radio value="left">仅包含左</el-radio>
                  <el-radio value="right">仅包含右</el-radio>
                  <el-radio value="none">都不包含</el-radio>
                </el-radio-group>
              </el-form-item>
            </template>

            <!-- 正则校验配置 -->
            <template v-if="form.template === 'regex'">
              <el-form-item label="校验字段">
                <el-select v-model="form.regexField" style="width: 100%" class="dp-mono">
                  <el-option v-for="f in fieldOptions" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="正则表达式">
                <el-input v-model="form.regexPattern" placeholder="请输入正则表达式" class="dp-mono" />
              </el-form-item>
              <el-form-item label="常用正则">
                <div style="display: flex; flex-wrap: wrap; gap: 8px;">
                  <el-tag v-for="r in commonRegex" :key="r.pattern" style="cursor: pointer" effect="plain" @click="form.regexPattern = r.pattern">
                    {{ r.name }}
                  </el-tag>
                </div>
              </el-form-item>
            </template>

            <!-- 枚举值校验配置 -->
            <template v-if="form.template === 'enum'">
              <el-form-item label="校验字段">
                <el-select v-model="form.enumField" style="width: 100%" class="dp-mono">
                  <el-option v-for="f in fieldOptions" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="允许值列表">
                <el-select v-model="form.enumValues" style="width: 100%" multiple allow-create filterable default-first-option placeholder="输入后按回车添加">
                  <el-option v-for="v in ['1', '2', '3', 'Y', 'N', 'SUCCESS', 'FAIL']" :key="v" :label="v" :value="v" />
                </el-select>
              </el-form-item>
            </template>

            <!-- 及时性校验配置 -->
            <template v-if="form.template === 'timeliness'">
              <el-form-item label="时间字段">
                <el-select v-model="form.timeField" style="width: 100%" class="dp-mono">
                  <el-option v-for="f in fieldOptions.filter(f => f.includes('time') || f.includes('date'))" :key="f" :label="f" :value="f" />
                </el-select>
              </el-form-item>
              <el-form-item label="延迟阈值">
                <el-input-number v-model="form.delayThreshold" :min="1" :max="86400" />
                <span style="margin-left: 8px; color: var(--dp-text-2)">分钟</span>
              </el-form-item>
            </template>

            <el-form-item label="校验SQL预览">
              <div class="sql-preview">{{ getSqlPreview() }}</div>
            </el-form-item>
          </el-form>
          <div class="tab-footer">
            <el-button @click="activeTab = 'config'">上一步</el-button>
            <el-button type="primary" @click="activeTab = 'alert'">下一步</el-button>
          </div>
        </el-tab-pane>

        <!-- 告警配置 -->
        <el-tab-pane label="告警配置" name="alert">
          <el-form :model="form" label-width="100px">
            <el-form-item label="告警方式">
              <el-checkbox-group v-model="form.alertMethods">
                <el-checkbox value="message">站内消息</el-checkbox>
                <el-checkbox value="email">邮件通知</el-checkbox>
                <el-checkbox value="sms">短信通知</el-checkbox>
                <el-checkbox value="webhook">WebHook</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="告警接收人">
              <el-select v-model="form.alertReceivers" style="width: 100%" multiple placeholder="请选择接收人">
                <el-option v-for="o in ownerOptions" :key="o" :label="o" :value="o" />
              </el-select>
            </el-form-item>
            <el-form-item label="告警阈值">
              <el-input-number v-model="form.alertThreshold" :min="0" :max="100" :step="1" />
              <span style="margin-left: 8px; color: var(--dp-text-2)">%（通过率低于此值时告警）</span>
            </el-form-item>
            <el-form-item label="告警静默期">
              <el-input-number v-model="form.silentPeriod" :min="0" :max="1440" :step="10" />
              <span style="margin-left: 8px; color: var(--dp-text-2)">分钟</span>
            </el-form-item>
            <el-form-item label="是否升级告警">
              <el-switch v-model="form.enableEscalation" />
            </el-form-item>
            <el-form-item v-if="form.enableEscalation" label="升级阈值">
              <el-input-number v-model="form.escalationCount" :min="1" :max="10" />
              <span style="margin-left: 8px; color: var(--dp-text-2)">次连续告警后升级</span>
            </el-form-item>
          </el-form>
          <div class="tab-footer">
            <el-button @click="activeTab = 'logic'">上一步</el-button>
            <el-button type="primary" @click="saveRule">保存规则</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 执行历史弹窗 -->
    <el-dialog v-model="historyVisible" title="执行历史" width="700px" destroy-on-close>
      <el-table :data="historyData" border stripe size="small">
        <el-table-column prop="execTime" label="执行时间" width="160" />
        <el-table-column prop="totalRows" label="校验行数" width="100" align="right" />
        <el-table-column prop="failedRows" label="异常行数" width="100" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.failedRows > 0 ? 'var(--dp-danger)' : '' }">{{ row.failedRows }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="passRate" label="通过率" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.passRate" :stroke-width="4" :color="getPassRateColor(row.passRate)" />
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="耗时" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === '成功' ? 'success' : row.status === '失败' ? 'danger' : 'warning'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

const loading = ref(false)
const editVisible = ref(false)
const historyVisible = ref(false)
const activeTab = ref('basic')

const categoryOptions = ['完整性', '准确性', '一致性', '唯一性', '时效性', '有效性']

const ownerOptions = ['张三', '李四', '王五', '赵六', '钱七']

const datasourceOptions = ['核心交易库', '客户主数据库', '日志采集库', '财务系统库', '会员中心库', '商品中心库']

const tableOptions = [
  'dwd_trade_db.order_detail',
  'dwd_trade_db.trade_flow',
  'dwd_trade_db.payment_record',
  'ods_customer_db.customer_info',
  'ods_customer_db.member_profile',
  'dws_summary_db.sale_day',
  'dim_public_db.product_sku'
]

const fieldOptions = [
  'id', 'order_no', 'user_id', 'amount', 'status', 'create_time',
  'update_time', 'phone', 'email', 'name', 'level', 'pay_channel',
  'refund_amount', 'sku_code', 'qty', 'price', 'city', 'channel'
]

const templateOptions = [
  { value: 'notnull', label: '非空校验 - 字段不允许为空' },
  { value: 'unique', label: '唯一性校验 - 字段值不可重复' },
  { value: 'range', label: '值域校验 - 字段值需在指定范围内' },
  { value: 'regex', label: '正则校验 - 字段值需匹配正则表达式' },
  { value: 'enum', label: '枚举校验 - 字段值需在指定枚举内' },
  { value: 'timeliness', label: '及时性校验 - 数据延迟不超过阈值' }
]

const commonRegex = [
  { name: '手机号', pattern: '^1[3-9]\\d{9}$' },
  { name: '邮箱', pattern: '^[\\w.-]+@[\\w.-]+\\.\\w+$' },
  { name: '身份证号', pattern: '^\\d{17}[\\dXx]$' },
  { name: '日期格式', pattern: '^\\d{4}-\\d{2}-\\d{2}$' },
  { name: '金额', pattern: '^\\d+(\\.\\d{1,2})?$' }
]

const query = reactive({
  keyword: '',
  category: '',
  status: '',
  level: '',
  page: 1,
  size: 10
})

const total = ref(0)
const tableData = ref([])

// Mock 数据
const allData = ref([
  { id: 1, name: '订单金额非空校验', code: 'RULE_TRADE_001', category: '完整性', dimension: '完整性', datasource: 'dwd_trade_db.order_detail', logic: 'order_amount IS NOT NULL', level: '高', status: '启用', lastExecTime: '2026-09-26 08:00:12', execCount: 1256, passRate: 98.5, owner: '张三' },
  { id: 2, name: '用户手机号格式校验', code: 'RULE_CUST_001', category: '准确性', dimension: '准确性', datasource: 'ods_customer_db.customer_info', logic: 'phone 匹配手机号正则', level: '中', status: '启用', lastExecTime: '2026-09-26 08:05:30', execCount: 2341, passRate: 95.2, owner: '李四' },
  { id: 3, name: '订单号唯一性校验', code: 'RULE_TRADE_002', category: '唯一性', dimension: '唯一性', datasource: 'dwd_trade_db.trade_flow', logic: 'order_no 字段唯一', level: '高', status: '启用', lastExecTime: '2026-09-26 08:10:08', execCount: 1890, passRate: 99.8, owner: '张三' },
  { id: 4, name: '支付金额范围校验', code: 'RULE_TRADE_003', category: '准确性', dimension: '准确性', datasource: 'dwd_trade_db.payment_record', logic: 'pay_amount BETWEEN 0 AND 100000', level: '中', status: '启用', lastExecTime: '2026-09-26 08:15:22', execCount: 1560, passRate: 97.1, owner: '王五' },
  { id: 5, name: '订单状态枚举校验', code: 'RULE_TRADE_004', category: '有效性', dimension: '有效性', datasource: 'dwd_trade_db.order_detail', logic: 'status IN (0,1,2,3,4,5)', level: '低', status: '启用', lastExecTime: '2026-09-26 08:20:45', execCount: 980, passRate: 99.2, owner: '赵六' },
  { id: 6, name: '数据延迟及时性校验', code: 'RULE_LOG_001', category: '时效性', dimension: '时效性', datasource: 'dwd_log_db.access_log', logic: '数据延迟 < 30分钟', level: '中', status: '启用', lastExecTime: '2026-09-26 08:25:10', execCount: 720, passRate: 93.5, owner: '钱七' },
  { id: 7, name: '客户ID参照完整性', code: 'RULE_CUST_002', category: '一致性', dimension: '一致性', datasource: 'dwd_trade_db.order_detail', logic: 'customer_id 存在于客户表', level: '高', status: '启用', lastExecTime: '2026-09-26 08:30:55', execCount: 1100, passRate: 96.8, owner: '李四' },
  { id: 8, name: '会员等级值域校验', code: 'RULE_CUST_003', category: '准确性', dimension: '准确性', datasource: 'ods_customer_db.member_profile', logic: 'level BETWEEN 1 AND 10', level: '低', status: '停用', lastExecTime: '2026-09-25 20:00:00', execCount: 450, passRate: 98.9, owner: '王五' },
  { id: 9, name: '商品编码格式校验', code: 'RULE_PROD_001', category: '准确性', dimension: '准确性', datasource: 'dim_public_db.product_sku', logic: 'sku_code 匹配编码规则', level: '中', status: '启用', lastExecTime: '2026-09-26 07:00:30', execCount: 680, passRate: 94.6, owner: '赵六' },
  { id: 10, name: '销售金额汇总一致性', code: 'RULE_SUM_001', category: '一致性', dimension: '一致性', datasource: 'dws_summary_db.sale_day', logic: '日汇总=明细汇总', level: '高', status: '启用', lastExecTime: '2026-09-26 06:00:00', execCount: 230, passRate: 97.5, owner: '钱七' },
  { id: 11, name: '用户邮箱非空校验', code: 'RULE_CUST_004', category: '完整性', dimension: '完整性', datasource: 'ods_customer_db.customer_info', logic: 'email IS NOT NULL', level: '低', status: '启用', lastExecTime: '2026-09-26 08:35:20', execCount: 1890, passRate: 88.3, owner: '张三' },
  { id: 12, name: '退款金额合理性校验', code: 'RULE_TRADE_005', category: '准确性', dimension: '准确性', datasource: 'dwd_trade_db.refund_order', logic: 'refund_amount <= order_amount', level: '高', status: '启用', lastExecTime: '2026-09-26 08:40:15', execCount: 560, passRate: 99.1, owner: '李四' }
])

const historyData = ref([])

// 表单数据
const form = reactive({
  id: null,
  name: '',
  code: '',
  category: '完整性',
  dimension: '完整性',
  level: '中',
  desc: '',
  datasource: '',
  tableName: '',
  fieldName: [],
  sampleType: 'all',
  samplePercent: 100,
  sampleCount: 1000,
  template: 'notnull',
  notNullField: '',
  uniqueField: [],
  rangeField: '',
  minValue: '',
  maxValue: '',
  includeBoundary: 'both',
  regexField: '',
  regexPattern: '',
  enumField: '',
  enumValues: [],
  timeField: '',
  delayThreshold: 30,
  alertMethods: ['message'],
  alertReceivers: [],
  alertThreshold: 95,
  silentPeriod: 60,
  enableEscalation: false,
  escalationCount: 3
})

function catTagType(cat) {
  const map = {
    '完整性': 'primary',
    '准确性': 'success',
    '一致性': 'warning',
    '唯一性': 'danger',
    '时效性': 'purple',
    '有效性': 'info'
  }
  return map[cat] || 'info'
}

function getPassRateColor(rate) {
  if (rate >= 95) return '#00b42a'
  if (rate >= 90) return '#ff7d00'
  return '#f53f3f'
}

function loadData() {
  loading.value = true
  setTimeout(() => {
    let list = allData.value.filter(r =>
      (!query.keyword || r.name.includes(query.keyword) || r.code.includes(query.keyword) || r.datasource.includes(query.keyword)) &&
      (!query.category || r.category === query.category) &&
      (!query.status || r.status === query.status) &&
      (!query.level || r.level === query.level)
    )
    total.value = list.length
    const start = (query.page - 1) * query.size
    tableData.value = list.slice(start, start + query.size)
    loading.value = false
  }, 300)
}

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      code: row.code,
      category: row.category,
      dimension: row.dimension,
      level: row.level,
      desc: row.logic,
      datasource: row.datasource.split('.')[0],
      tableName: row.datasource,
      fieldName: [],
      sampleType: 'all',
      samplePercent: 100,
      sampleCount: 1000,
      template: 'notnull',
      notNullField: '',
      uniqueField: [],
      rangeField: '',
      minValue: '',
      maxValue: '',
      includeBoundary: 'both',
      regexField: '',
      regexPattern: '',
      enumField: '',
      enumValues: [],
      timeField: '',
      delayThreshold: 30,
      alertMethods: ['message', 'email'],
      alertReceivers: [row.owner],
      alertThreshold: 95,
      silentPeriod: 60,
      enableEscalation: false,
      escalationCount: 3
    })
  } else {
    Object.assign(form, {
      id: null,
      name: '',
      code: 'RULE_' + Date.now().toString().slice(-6),
      category: '完整性',
      dimension: '完整性',
      level: '中',
      desc: '',
      datasource: '',
      tableName: '',
      fieldName: [],
      sampleType: 'all',
      samplePercent: 100,
      sampleCount: 1000,
      template: 'notnull',
      notNullField: '',
      uniqueField: [],
      rangeField: '',
      minValue: '',
      maxValue: '',
      includeBoundary: 'both',
      regexField: '',
      regexPattern: '',
      enumField: '',
      enumValues: [],
      timeField: '',
      delayThreshold: 30,
      alertMethods: ['message'],
      alertReceivers: [],
      alertThreshold: 95,
      silentPeriod: 60,
      enableEscalation: false,
      escalationCount: 3
    })
  }
  activeTab.value = 'basic'
  editVisible.value = true
}

function onCategoryChange() {
  const map = {
    '完整性': 'notnull',
    '准确性': 'range',
    '一致性': 'notnull',
    '唯一性': 'unique',
    '时效性': 'timeliness',
    '有效性': 'enum'
  }
  form.template = map[form.category] || 'notnull'
}

function onTemplateChange() {
  // 模板变化时可以做一些联动
}

function getSqlPreview() {
  const table = form.tableName || 'table_name'
  switch (form.template) {
    case 'notnull':
      const field = form.notNullField || 'field_name'
      return `SELECT COUNT(*) AS invalid_count\nFROM ${table}\nWHERE ${field} IS NULL OR ${field} = '';`
    case 'unique':
      const uFields = form.uniqueField.length ? form.uniqueField.join(', ') : 'field_name'
      return `SELECT ${uFields}, COUNT(*) AS cnt\nFROM ${table}\nGROUP BY ${uFields}\nHAVING COUNT(*) > 1;`
    case 'range':
      const rField = form.rangeField || 'field_name'
      const min = form.minValue || 'min_val'
      const max = form.maxValue || 'max_val'
      return `SELECT COUNT(*) AS invalid_count\nFROM ${table}\nWHERE ${rField} < ${min} OR ${rField} > ${max};`
    case 'regex':
      const reField = form.regexField || 'field_name'
      const pattern = form.regexPattern || 'pattern'
      return `SELECT COUNT(*) AS invalid_count\nFROM ${table}\nWHERE ${reField} NOT REGEXP '${pattern}';`
    case 'enum':
      const eField = form.enumField || 'field_name'
      const values = form.enumValues.length ? `'${form.enumValues.join("', '")}'` : "'v1', 'v2'"
      return `SELECT COUNT(*) AS invalid_count\nFROM ${table}\nWHERE ${eField} NOT IN (${values});`
    case 'timeliness':
      const tField = form.timeField || 'create_time'
      return `SELECT COUNT(*) AS delayed_count\nFROM ${table}\nWHERE ${tField} < NOW() - INTERVAL ${form.delayThreshold} MINUTE;`
    default:
      return '-- 请选择校验模板'
  }
}

function saveRule() {
  if (!form.name) return ElMessage.warning('请输入规则名称')
  if (!form.code) return ElMessage.warning('请输入规则编码')
  if (!form.tableName) return ElMessage.warning('请选择数据表')

  if (form.id) {
    const idx = allData.value.findIndex(r => r.id === form.id)
    if (idx !== -1) {
      allData.value[idx] = {
        ...allData.value[idx],
        name: form.name,
        code: form.code,
        category: form.category,
        dimension: form.dimension,
        level: form.level,
        datasource: form.tableName,
        logic: getLogicDesc()
      }
    }
    ElMessage.success('规则已更新')
  } else {
    allData.value.unshift({
      id: Date.now(),
      name: form.name,
      code: form.code,
      category: form.category,
      dimension: form.dimension,
      datasource: form.tableName,
      logic: getLogicDesc(),
      level: form.level,
      status: '启用',
      lastExecTime: '未执行',
      execCount: 0,
      passRate: 100,
      owner: form.alertReceivers[0] || '当前用户'
    })
    ElMessage.success('规则创建成功')
  }
  editVisible.value = false
  loadData()
}

function getLogicDesc() {
  const template = templateOptions.find(t => t.value === form.template)
  return template ? template.label.split(' - ')[0] : '自定义校验'
}

function toggleStatus(row) {
  row.status = row.status === '启用' ? '停用' : '启用'
  ElMessage.success(`规则「${row.name}」已${row.status}`)
}

function execNow(row) {
  ElMessage.info(`规则「${row.name}」开始执行...`)
  setTimeout(() => {
    row.lastExecTime = new Date().toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
    row.execCount += 1
    ElMessage.success(`规则「${row.name}」执行完成`)
  }, 1500)
}

function viewHistory(row) {
  historyData.value = Array.from({ length: 10 }, (_, i) => {
    const passRate = +(93 + Math.random() * 7).toFixed(1)
    const totalRows = 10000 + Math.floor(Math.random() * 50000)
    const failedRows = Math.floor(totalRows * (100 - passRate) / 100)
    const d = new Date()
    d.setHours(d.getHours() - i * 4)
    return {
      execTime: d.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-'),
      totalRows,
      failedRows,
      passRate,
      duration: `${Math.floor(Math.random() * 60) + 10}s`,
      status: passRate > 90 ? '成功' : '失败'
    }
  })
  historyVisible.value = true
}

function removeRule(row) {
  ElMessageBox.confirm(
    `确定删除规则「${row.name}」吗？删除后相关的执行历史也将被清除。`,
    '删除确认',
    { type: 'warning' }
  ).then(() => {
    allData.value = allData.value.filter(r => r.id !== row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.rule-code {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  color: var(--dp-primary);
  background: var(--dp-primary-light);
  padding: 2px 6px;
  border-radius: 4px;
}

.dim-tag {
  font-size: 12px;
  color: var(--dp-text-2);
  background: #f7f8fa;
  padding: 2px 8px;
  border-radius: 4px;
}

.level-dot {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
}

.level-dot i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  display: inline-block;
}

.level-dot.高 { color: var(--dp-danger); }
.level-dot.高 i { background: var(--dp-danger); box-shadow: 0 0 4px var(--dp-danger); }
.level-dot.中 { color: var(--dp-warning); }
.level-dot.中 i { background: var(--dp-warning); box-shadow: 0 0 4px var(--dp-warning); }
.level-dot.低 { color: var(--dp-text-3); }
.level-dot.低 i { background: var(--dp-text-3); }

.rule-tabs {
  margin: -20px -20px 0;
}

.rule-tabs :deep(.el-tabs__header) {
  margin: 0 0 20px 0;
  padding: 0 20px;
}

.tab-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--dp-border-light);
}

.sql-preview {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px 16px;
  border-radius: 8px;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  width: 100%;
}
</style>
