<template>
  <div class="governance-refs">
    <PageHeader title="引用标准" desc="管理外部引用的国家标准、行业标准和地方标准，支持标准映射和同步">
      <el-button type="primary" :icon="Plus">新增引用标准</el-button>
    </PageHeader>

    <div class="dp-card dp-fade-up">
      <div class="dp-toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="搜索标准编号/名称/发布机构"
          clearable
          style="width: 280px"
          :prefix-icon="Search"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="query.category" placeholder="标准类别" clearable style="width: 140px" @change="handleSearch">
          <el-option label="国家标准" value="国家标准" />
          <el-option label="行业标准" value="行业标准" />
          <el-option label="地方标准" value="地方标准" />
          <el-option label="国际标准" value="国际标准" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="handleSearch">
          <el-option label="已生效" value="已生效" />
          <el-option label="草稿" value="草稿" />
          <el-option label="已废止" value="已废止" />
        </el-select>
        <div class="toolbar-right">
          <el-button :icon="Refresh" @click="handleSearch">刷新</el-button>
          <el-button type="primary" :icon="Plus">新增引用标准</el-button>
        </div>
      </div>

      <el-table :data="pagedList" border stripe v-loading="loading">
        <el-table-column prop="code" label="标准编号" width="180" class-name="dp-mono" />
        <el-table-column prop="name" label="标准名称" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="name-link" @click="handleView(row)">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="publisher" label="发布机构" width="140" />
        <el-table-column prop="category" label="标准类别" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="categoryTagType(row.category)" effect="plain">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishDate" label="发布日期" width="110" />
        <el-table-column prop="effectiveDate" label="实施日期" width="110" />
        <el-table-column prop="refElements" label="关联数据元" width="100" align="center">
          <template #default="{ row }">
            <span class="ref-count">{{ row.refElements }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <span class="status-dot" :class="statusDotClass(row.status)"></span>
            {{ row.status }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">查看详情</el-button>
            <el-button link type="primary" size="small" @click="handleSync(row)">同步</el-button>
            <el-button link type="primary" size="small">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" :title="currentRef?.name" size="600px" destroy-on-close>
      <template v-if="currentRef">
        <el-tabs v-model="activeTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="标准编号">
                <span class="dp-mono">{{ currentRef.code }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="标准名称">{{ currentRef.name }}</el-descriptions-item>
              <el-descriptions-item label="标准类别">{{ currentRef.category }}</el-descriptions-item>
              <el-descriptions-item label="发布机构">{{ currentRef.publisher }}</el-descriptions-item>
              <el-descriptions-item label="发布日期">{{ currentRef.publishDate }}</el-descriptions-item>
              <el-descriptions-item label="实施日期">{{ currentRef.effectiveDate }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <span class="status-dot" :class="statusDotClass(currentRef.status)"></span>
                {{ currentRef.status }}
              </el-descriptions-item>
              <el-descriptions-item label="关联数据元">{{ currentRef.refElements }} 个</el-descriptions-item>
              <el-descriptions-item label="标准简介" :span="2">{{ currentRef.desc }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- 条款列表 -->
          <el-tab-pane label="条款列表" name="clauses">
            <el-table :data="clauseList" border stripe size="small">
              <el-table-column prop="clauseNo" label="条款号" width="100" class-name="dp-mono" />
              <el-table-column prop="title" label="条款标题" min-width="160" />
              <el-table-column prop="content" label="内容摘要" min-width="200" show-overflow-tooltip />
              <el-table-column prop="mapped" label="映射状态" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.mapped ? 'success' : 'warning'" effect="plain">
                    {{ row.mapped ? '已映射' : '未映射' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <!-- 关联数据元 -->
          <el-tab-pane label="关联数据元" name="elements">
            <div class="ref-stats">
              <div class="ref-stat">
                <span class="ref-num">{{ refElements.length }}</span>
                <span class="ref-label">个已关联数据元</span>
              </div>
            </div>
            <el-table :data="refElements" border stripe size="small">
              <el-table-column prop="code" label="数据元编码" width="120" class-name="dp-mono" />
              <el-table-column prop="name" label="数据元名称" min-width="140" />
              <el-table-column prop="standard" label="所属企业标准" width="160" class-name="dp-mono" />
              <el-table-column prop="status" label="状态" width="80">
                <template #default="{ row }">
                  <span class="status-dot" :class="statusDotClass(row.status)"></span>{{ row.status }}
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
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
const detailVisible = ref(false)
const activeTab = ref('basic')
const currentRef = ref(null)

const query = reactive({
  keyword: '',
  category: '',
  status: '',
  page: 1,
  size: 10
})

const refList = ref([
  { id: 1, code: 'GB/T 2260-2007', name: '中华人民共和国行政区划代码', category: '国家标准', publisher: '国家质检总局', publishDate: '2007-02-06', effectiveDate: '2007-05-01', refElements: 156, status: '已生效', desc: '规定了中华人民共和国县及县以上行政区划代码，适用于对行政区划进行编码、信息处理和交换等。' },
  { id: 2, code: 'GB/T 2261.1-2003', name: '个人基本信息分类与代码 第1部分：人的性别代码', category: '国家标准', publisher: '国家质检总局', publishDate: '2003-07-25', effectiveDate: '2003-12-01', refElements: 89, status: '已生效', desc: '规定了人的性别代码，适用于对人的性别信息进行分类、编码、信息处理和交换等。' },
  { id: 3, code: 'GB/T 7408-2005', name: '数据元和交换格式 信息交换 日期和时间表示法', category: '国家标准', publisher: '国家质检总局', publishDate: '2005-09-07', effectiveDate: '2006-01-01', refElements: 234, status: '已生效', desc: '规定了日期和时间的表示法，适用于信息处理系统之间的信息交换。' },
  { id: 4, code: 'GB/T 12406-2008', name: '表示货币和资金的代码', category: '国家标准', publisher: '国家质检总局', publishDate: '2008-06-17', effectiveDate: '2008-12-01', refElements: 67, status: '已生效', desc: '规定了表示货币和资金的三位字母代码和三位数字代码，与ISO 4217一致。' },
  { id: 5, code: 'GB/T 17710-2008', name: '信息技术 安全技术 校验字符系统', category: '国家标准', publisher: '国家质检总局', publishDate: '2008-10-07', effectiveDate: '2009-04-01', refElements: 34, status: '已生效', desc: '规定了校验字符系统的通用要求，包括算法、字符集和校验方法等。' },
  { id: 6, code: 'JR/T 0062-2011', name: '金融机构编码规范', category: '行业标准', publisher: '中国人民银行', publishDate: '2011-12-12', effectiveDate: '2012-01-01', refElements: 45, status: '已生效', desc: '规定了金融机构的编码规则和方法，适用于金融机构的信息系统和数据交换。' },
  { id: 7, code: 'YD/T 2282-2011', name: '电信网码号资源分类与编码', category: '行业标准', publisher: '工信部', publishDate: '2011-05-18', effectiveDate: '2011-06-01', refElements: 28, status: '已生效', desc: '规定了电信网码号资源的分类与编码方法，适用于电信网码号资源的管理。' },
  { id: 8, code: 'DB11/T 1214-2015', name: '政务信息资源目录体系 第1部分：总体框架', category: '地方标准', publisher: '北京市质监局', publishDate: '2015-08-12', effectiveDate: '2015-12-01', refElements: 23, status: '已生效', desc: '规定了北京市政务信息资源目录体系的总体框架，适用于政务信息资源的目录建设。' },
  { id: 9, code: 'GB/T 35295-2017', name: '信息安全技术 大数据安全管理指南', category: '国家标准', publisher: '国家质检总局', publishDate: '2017-12-29', effectiveDate: '2018-07-01', refElements: 56, status: '已生效', desc: '给出了大数据安全管理的指南，包括数据安全分类分级、数据生命周期安全管理等。' },
  { id: 10, code: 'GB/T 36073-2018', name: '数据管理能力成熟度评估模型', category: '国家标准', publisher: '国家质检总局', publishDate: '2018-03-15', effectiveDate: '2018-08-01', refElements: 78, status: '已生效', desc: '规定了数据管理能力成熟度评估模型，适用于组织的数据管理能力评估和改进。' },
  { id: 11, code: 'ISO 8601:2019', name: 'Date and time - Representations for information interchange', category: '国际标准', publisher: 'ISO', publishDate: '2019-02-25', effectiveDate: '2019-02-25', refElements: 12, status: '草稿', desc: 'ISO标准，规定了日期和时间的表示方法，用于信息交换。' },
  { id: 12, code: 'GB/T 4754-2017', name: '国民经济行业分类', category: '国家标准', publisher: '国家统计局', publishDate: '2017-06-30', effectiveDate: '2017-10-01', refElements: 97, status: '已废止', desc: '已废止，被GB/T 4754-2023替代。' }
])

const clauseList = ref([
  { clauseNo: '1', title: '范围', content: '本标准规定了...', mapped: true },
  { clauseNo: '2', title: '规范性引用文件', content: '下列文件对于本文件的应用是必不可少的...', mapped: false },
  { clauseNo: '3', title: '术语和定义', content: '下列术语和定义适用于本文件...', mapped: true },
  { clauseNo: '4', title: '编码规则', content: '行政区划代码采用六位数字编码...', mapped: true },
  { clauseNo: '5', title: '代码表', content: '详见附录A中的代码表...', mapped: true },
  { clauseNo: '6', title: '代码的维护', content: '代码的维护由相关部门负责...', mapped: false }
])

const refElements = ref([
  { code: 'DE-1003', name: '性别代码', standard: 'STD-BASE-1008', status: '已发布' },
  { code: 'DE-1025', name: '用户性别', standard: 'STD-BASE-1001', status: '已发布' },
  { code: 'DE-1046', name: '会员性别', standard: 'STD-IDX-1003', status: '已发布' }
])

const filteredList = computed(() => {
  return refList.value.filter(item => {
    if (query.keyword && !item.name.includes(query.keyword) && !item.code.includes(query.keyword) && !item.publisher.includes(query.keyword)) return false
    if (query.category && item.category !== query.category) return false
    if (query.status && item.status !== query.status) return false
    return true
  })
})

const total = computed(() => filteredList.value.length)

const pagedList = computed(() => {
  const start = (query.page - 1) * query.size
  return filteredList.value.slice(start, start + query.size)
})

function categoryTagType(cat) {
  const map = { '国家标准': 'primary', '行业标准': 'success', '地方标准': 'warning', '国际标准': 'danger' }
  return map[cat] || 'info'
}

function statusDotClass(status) {
  const map = { '已生效': 'success', '草稿': 'warning', '已废止': 'info' }
  return map[status] || 'info'
}

function handleSearch() {
  query.page = 1
  loading.value = true
  setTimeout(() => { loading.value = false }, 300)
}

function handleView(row) {
  currentRef.value = row
  activeTab.value = 'basic'
  detailVisible.value = true
}

function handleSync(row) {
  ElMessage.success(`正在同步标准「${row.name}」...`)
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除引用标准「${row.name}」吗？此操作不可恢复。`, '确认删除', { type: 'error' }).then(() => {
    refList.value = refList.value.filter(r => r.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}
</script>

<style scoped>
.governance-refs {
  padding: 20px;
}

.toolbar-right {
  margin-left: auto;
  display: flex;
  gap: 10px;
}

.name-link {
  color: var(--dp-primary);
  cursor: pointer;
  font-weight: 500;
}

.name-link:hover {
  opacity: 0.75;
}

.ref-count {
  font-weight: 600;
  color: var(--dp-text-1);
  font-family: 'DIN Alternate', sans-serif;
}

.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.status-dot.success { background: var(--dp-success); }
.status-dot.warning { background: var(--dp-warning); }
.status-dot.info { background: var(--dp-text-4); }

.ref-stats {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: var(--dp-bg-page);
  border-radius: 8px;
}

.ref-stat {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.ref-num {
  font-size: 22px;
  font-weight: 700;
  color: var(--dp-primary);
  font-family: 'DIN Alternate', sans-serif;
}

.ref-label {
  font-size: 12px;
  color: var(--dp-text-3);
}

.dp-mono {
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12px;
}

@keyframes dp-fade-up {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.dp-fade-up { animation: dp-fade-up 0.35s ease both; }
</style>
