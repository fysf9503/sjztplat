<template>
  <div class="dp-page">
    <PageHeader title="文档总览" desc="知识沉淀与协同空间，统一管理操作手册、方案文档和知识资料">
      <el-button type="primary" :icon="Plus">新建文档</el-button>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid dp-stat-grid-4">
      <StatCard
        v-for="stat in statCards"
        :key="stat.label"
        :label="stat.label"
        :value="stat.value"
        :unit="stat.unit"
        :icon="stat.icon"
        :color="stat.color"
        :bg="stat.bg"
        :trend="stat.trend"
      />
    </div>

    <div class="dp-grid-2">
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">最近更新文档</div>
          <el-button link type="primary" size="small" @click="$router.push('/docs/mine')">查看全部</el-button>
        </div>
        <div class="dc-doc-list">
          <div v-for="doc in recentDocs" :key="doc.id" class="dc-doc-item" @click="handleOpen(doc)">
            <div class="dc-doc-icon" :class="'icon-' + getDocTypeClass(doc.type)">
              <el-icon :size="20"><component :is="getDocIcon(doc.type)" /></el-icon>
            </div>
            <div class="dc-doc-info">
              <div class="dc-doc-title dp-ellipsis">{{ doc.title }}</div>
              <div class="dc-doc-meta">
                <el-tag size="small" effect="plain">{{ doc.type }}</el-tag>
                <span class="dc-doc-folder">{{ doc.folder }}</span>
              </div>
            </div>
            <div class="dc-doc-right">
              <div class="dc-doc-owner">
                <el-icon><User /></el-icon>
                <span>{{ doc.owner }}</span>
              </div>
              <div class="dc-doc-time">{{ doc.updatedAt }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="dp-card">
        <div class="dp-card-header">
          <div class="dp-card-title">文档类型分布</div>
        </div>
        <EChart :option="typePieOption" height="300px" />
      </div>
    </div>

    <div class="dp-card">
      <div class="dp-card-header">
        <div class="dp-card-title">文档浏览趋势（近 30 天）</div>
      </div>
      <EChart :option="viewTrendOption" height="260px" />
    </div>

    <div class="dp-card">
      <div class="dp-card-header">
        <div class="dp-card-title">热门文档 Top 10</div>
        <span class="dp-card-extra">按浏览量排序</span>
      </div>
      <el-table :data="hotDocs" border stripe>
        <el-table-column label="排名" width="70" align="center">
          <template #default="{ $index }">
            <span class="dc-rank" :class="'rank-' + ($index + 1)">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="文档名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="90">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="folder" label="分类" width="100" />
        <el-table-column prop="owner" label="负责人" width="90" />
        <el-table-column prop="views" label="浏览量" width="100" sortable align="right" />
        <el-table-column prop="updatedAt" label="更新时间" width="160" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import EChart from '@/components/EChart.vue'
import { documents, trend } from '@/mock'
import { Plus } from '@element-plus/icons-vue'

const statCards = [
  { label: '文档总数', value: 128, unit: '份', icon: 'Document', color: '#165dff', bg: '#e8f3ff', trend: 8 },
  { label: '我的文档', value: 24, unit: '份', icon: 'User', color: '#00b42a', bg: '#e8ffea', trend: 5 },
  { label: '共享文档', value: 36, unit: '份', icon: 'Share', color: '#ff7d00', bg: '#fff3e8', trend: 12 },
  { label: '模板数量', value: 18, unit: '个', icon: 'Collection', color: '#722ed1', bg: '#f5e8ff', trend: 3 }
]

const recentDocs = computed(() =>
  [...documents].sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt)).slice(0, 6)
)

const hotDocs = computed(() =>
  [...documents].sort((a, b) => b.views - a.views).slice(0, 10)
)

const getDocTypeClass = type => {
  const map = { Word: 'word', Excel: 'excel', PPT: 'ppt', PDF: 'pdf', Markdown: 'md', 思维笔记: 'note' }
  return map[type] || 'default'
}

const getDocIcon = type => {
  const map = { Word: 'Document', Excel: 'Grid', PPT: 'Presentation', PDF: 'Files', Markdown: 'EditPen', 思维笔记: 'Notebook' }
  return map[type] || 'Document'
}

const typePieOption = computed(() => {
  const types = {}
  documents.forEach(d => {
    types[d.type] = (types[d.type] || 0) + 1
  })
  const data = Object.entries(types).map(([name, value]) => ({ name, value }))
  const colors = ['#165dff', '#00b42a', '#ff7d00', '#f53f3f', '#722ed1', '#0fc6c2']
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}份 ({d}%)' },
    legend: { bottom: 0, type: 'scroll', textStyle: { color: '#4e5969', fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { formatter: '{b}\n{d}%', fontSize: 11, lineHeight: 16 },
      labelLine: { length: 8, length2: 8 },
      data: data.map((d, i) => ({ ...d, itemStyle: { color: colors[i % colors.length] } }))
    }]
  }
})

const viewTrendOption = computed(() => ({
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.95)', borderColor: '#e5e6eb' },
  legend: { data: ['文档浏览量', '活跃用户'], top: 0, right: 0, textStyle: { color: '#86909c', fontSize: 12 } },
  grid: { left: 50, right: 50, top: 36, bottom: 30 },
  xAxis: {
    type: 'category',
    data: trend.calls.map(d => d.date),
    axisLine: { lineStyle: { color: '#e5e6eb' } },
    axisLabel: { color: '#86909c', fontSize: 11 }
  },
  yAxis: [
    {
      type: 'value',
      name: '浏览量',
      nameTextStyle: { color: '#86909c', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#86909c', fontSize: 11 }
    },
    {
      type: 'value',
      name: '用户数',
      nameTextStyle: { color: '#86909c', fontSize: 11 },
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { show: false },
      axisLabel: { color: '#86909c', fontSize: 11 }
    }
  ],
  series: [
    {
      name: '文档浏览量',
      type: 'line',
      smooth: true,
      symbol: 'none',
      data: trend.calls.map(d => Math.round(d.value / 30)),
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(22,93,255,0.25)' },
            { offset: 1, color: 'rgba(22,93,255,0)' }
          ]
        }
      },
      lineStyle: { color: '#165dff', width: 2 }
    },
    {
      name: '活跃用户',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      data: trend.calls.map(d => Math.round(d.value / 200)),
      lineStyle: { color: '#00b42a', width: 2 },
      itemStyle: { color: '#00b42a' }
    }
  ]
}))

function handleOpen(doc) {
  // 打开文档逻辑
}
</script>

<style scoped>
.dp-stat-grid-4 {
  grid-template-columns: repeat(4, 1fr);
}

.dc-doc-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.dc-doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fafbfc;
  transition: all 0.2s;
  cursor: pointer;
}

.dc-doc-item:hover {
  background: #f0f5ff;
}

.dc-doc-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.dc-doc-icon.icon-word { background: #e8f3ff; color: #165dff; }
.dc-doc-icon.icon-excel { background: #e8ffea; color: #00b42a; }
.dc-doc-icon.icon-ppt { background: #fff3e8; color: #ff7d00; }
.dc-doc-icon.icon-pdf { background: #ffece8; color: #f53f3f; }
.dc-doc-icon.icon-md { background: #f5e8ff; color: #722ed1; }
.dc-doc-icon.icon-note { background: #e0fffa; color: #0fc6c2; }

.dc-doc-info {
  flex: 1;
  min-width: 0;
}

.dc-doc-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--dp-text-1);
  margin-bottom: 6px;
}

.dc-doc-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dc-doc-folder {
  font-size: 12px;
  color: var(--dp-text-3);
}

.dc-doc-right {
  text-align: right;
  flex-shrink: 0;
}

.dc-doc-owner {
  font-size: 12px;
  color: var(--dp-text-2);
  display: flex;
  align-items: center;
  gap: 4px;
  justify-content: flex-end;
  margin-bottom: 4px;
}

.dc-doc-time {
  font-size: 12px;
  color: var(--dp-text-3);
}

.dc-rank {
  display: inline-block;
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  background: #f2f3f5;
  color: var(--dp-text-3);
}

.dc-rank.rank-1 { background: #fff3e8; color: #ff7d00; }
.dc-rank.rank-2 { background: #e8f3ff; color: #165dff; }
.dc-rank.rank-3 { background: #e0fffa; color: #0fc6c2; }

@media (max-width: 1200px) {
  .dp-stat-grid-4 {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
