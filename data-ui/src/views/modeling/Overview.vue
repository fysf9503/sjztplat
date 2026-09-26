<template>
  <div class="dp-page">
    <PageHeader title="建模总览" desc="覆盖概念模型、逻辑设计、物理实现到部署验证的完整建模建设链路">
      <el-radio-group v-model="timeRange" size="default">
        <el-radio-button value="week">近7天</el-radio-button>
        <el-radio-button value="month">近30天</el-radio-button>
        <el-radio-button value="quarter">近3个月</el-radio-button>
        <el-radio-button value="half">近6个月</el-radio-button>
      </el-radio-group>
      <el-button :icon="Refresh" @click="handleRefresh">
        <span v-if="loading">刷新中</span>
        <span v-else>刷新</span>
      </el-button>
    </PageHeader>

    <!-- 统计卡片 -->
    <div class="dp-stat-grid dp-fade-up">
      <StatCard
        label="模型总数"
        :value="stats.totalModels"
        unit="个"
        icon="DataLine"
        color="var(--dp-primary)"
        bg="var(--dp-primary-light)"
        :trend="stats.totalTrend"
      />
      <StatCard
        label="逻辑模型数"
        :value="stats.logicalModels"
        unit="个"
        icon="Share"
        color="var(--dp-success)"
        bg="var(--dp-success-light)"
        :trend="stats.logicalTrend"
      />
      <StatCard
        label="物理模型数"
        :value="stats.physicalModels"
        unit="个"
        icon="Coin"
        color="var(--dp-purple)"
        bg="var(--dp-purple-light)"
        :trend="stats.physicalTrend"
      />
      <StatCard
        label="已发布模型数"
        :value="stats.publishedModels"
        unit="个"
        icon="CircleCheck"
        color="var(--dp-warning)"
        bg="var(--dp-warning-light)"
        :trend="stats.publishedTrend"
      />
    </div>

    <!-- 图表区域 -->
    <div class="dp-grid-2">
      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">模型类型分布</div>
          <span class="dp-card-extra">共 {{ stats.totalModels }} 个模型</span>
        </div>
        <EChart :option="typePieOption" height="280px" />
      </div>
      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">建模趋势</div>
          <span class="dp-card-extra">近6个月新增模型数</span>
        </div>
        <EChart :option="trendOption" height="280px" />
      </div>
    </div>

    <!-- 最近活动 + 主题域分布 -->
    <div class="dp-grid-2">
      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">最近建模活动</div>
          <el-button link type="primary" size="small" @click="$router.push('/modeling/logical')">查看全部</el-button>
        </div>
        <div class="activity-list">
          <div v-for="(item, index) in recentActivities" :key="index" class="activity-item">
            <div class="activity-dot" :class="item.typeClass"></div>
            <div class="activity-content">
              <div class="activity-title">
                <span class="activity-action">{{ item.action }}</span>
                <span class="activity-target">{{ item.target }}</span>
              </div>
              <div class="activity-meta">
                <span>{{ item.user }}</span>
                <span class="activity-dot-sep">·</span>
                <span>{{ item.time }}</span>
              </div>
            </div>
            <el-tag :type="item.tagType" size="small" effect="plain">{{ item.status }}</el-tag>
          </div>
        </div>
      </div>
      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <div class="dp-card-title">模型按主题域分布</div>
        </div>
        <EChart :option="domainBarOption" height="280px" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import EChart from '@/components/EChart.vue'
import { Refresh } from '@element-plus/icons-vue'

const timeRange = ref('month')
const loading = ref(false)

const stats = ref({
  totalModels: 86,
  logicalModels: 32,
  physicalModels: 54,
  publishedModels: 68,
  totalTrend: 12.5,
  logicalTrend: 8.3,
  physicalTrend: 15.2,
  publishedTrend: 6.8
})

const recentActivities = ref([
  { action: '创建模型', target: '客户主题域模型-V3', user: '张伟', time: '10分钟前', status: '设计中', typeClass: 'primary', tagType: 'warning' },
  { action: '发布模型', target: '交易主题域模型-V2', user: '李娜', time: '2小时前', status: '已发布', typeClass: 'success', tagType: 'success' },
  { action: '更新实体', target: '商品主题域模型-V1', user: '王强', time: '3小时前', status: '设计中', typeClass: 'warning', tagType: 'warning' },
  { action: '部署模型', target: 'dwd_trade_v3', user: '赵敏', time: '5小时前', status: '成功', typeClass: 'success', tagType: 'success' },
  { action: '创建模型', target: '营销主题域模型-V2', user: '孙浩', time: '昨天 14:30', status: '设计中', typeClass: 'primary', tagType: 'warning' },
  { action: '归档模型', target: '财务主题域模型-V1', user: '周明', time: '昨天 10:15', status: '已归档', typeClass: 'info', tagType: 'info' },
  { action: '同步结构', target: 'dim_public_v2', user: '吴丽', time: '2天前', status: '已同步', typeClass: 'success', tagType: 'success' }
])

const typePieOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: '{b}: {c}个 ({d}%)'
  },
  legend: {
    bottom: 0,
    itemWidth: 12,
    itemHeight: 12,
    textStyle: { color: '#4e5969', fontSize: 12 }
  },
  series: [
    {
      type: 'pie',
      radius: ['50%', '72%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 6,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        formatter: '{b}\n{c}个',
        fontSize: 11,
        color: '#4e5969'
      },
      labelLine: {
        length: 8,
        length2: 8
      },
      data: [
        { value: 32, name: '逻辑模型', itemStyle: { color: '#1664ff' } },
        { value: 54, name: '物理模型', itemStyle: { color: '#00b42a' } },
        { value: 18, name: '维度模型', itemStyle: { color: '#722ed1' } },
        { value: 12, name: '概念模型', itemStyle: { color: '#ff7d00' } },
        { value: 8, name: '其他', itemStyle: { color: '#0fc6c2' } }
      ]
    }
  ]
}))

const trendOption = computed(() => {
  const months = ['4月', '5月', '6月', '7月', '8月', '9月']
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    legend: {
      data: ['逻辑模型', '物理模型'],
      bottom: 0,
      itemWidth: 12,
      itemHeight: 12,
      textStyle: { color: '#4e5969', fontSize: 12 }
    },
    grid: {
      left: 50,
      right: 20,
      top: 20,
      bottom: 40
    },
    xAxis: {
      type: 'category',
      data: months,
      axisLine: { lineStyle: { color: '#e5e6eb' } },
      axisLabel: { color: '#86909c', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f2f3f5' } },
      axisLabel: { color: '#86909c', fontSize: 12 }
    },
    series: [
      {
        name: '逻辑模型',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: [4, 6, 5, 7, 6, 8],
        lineStyle: { color: '#1664ff', width: 2 },
        itemStyle: { color: '#1664ff' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(22, 100, 255, 0.25)' },
              { offset: 1, color: 'rgba(22, 100, 255, 0.02)' }
            ]
          }
        }
      },
      {
        name: '物理模型',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 6,
        data: [6, 8, 9, 11, 10, 13],
        lineStyle: { color: '#00b42a', width: 2 },
        itemStyle: { color: '#00b42a' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(0, 180, 42, 0.2)' },
              { offset: 1, color: 'rgba(0, 180, 42, 0.02)' }
            ]
          }
        }
      }
    ]
  }
})

const domainBarOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' }
  },
  grid: {
    left: 80,
    right: 30,
    top: 10,
    bottom: 20
  },
  xAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f2f3f5' } },
    axisLabel: { color: '#86909c', fontSize: 12 }
  },
  yAxis: {
    type: 'category',
    data: ['财务域', '供应链域', '营销域', '商品域', '交易域', '客户域'],
    axisLine: { show: false },
    axisTick: { show: false },
    axisLabel: { color: '#4e5969', fontSize: 12 }
  },
  series: [
    {
      type: 'bar',
      barWidth: 16,
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [
            { offset: 0, color: '#4080ff' },
            { offset: 1, color: '#1664ff' }
          ]
        }
      },
      data: [10, 12, 14, 16, 18, 16]
    }
  ]
}))

function handleRefresh() {
  loading.value = true
  ElMessage.info('数据刷新中...')
  setTimeout(() => {
    loading.value = false
    ElMessage.success('数据已刷新')
  }, 1000)
}

onMounted(() => {
  // 模拟初始加载
})
</script>

<style scoped>
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.activity-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}
.activity-item:hover {
  background: var(--dp-bg-page);
}
.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 2px;
}
.activity-dot.primary { background: var(--dp-primary); }
.activity-dot.success { background: var(--dp-success); }
.activity-dot.warning { background: var(--dp-warning); }
.activity-dot.info { background: var(--dp-text-4); }
.activity-content {
  flex: 1;
  min-width: 0;
}
.activity-title {
  font-size: 13px;
  color: var(--dp-text-1);
  line-height: 1.5;
}
.activity-action {
  color: var(--dp-text-2);
  margin-right: 6px;
}
.activity-target {
  font-weight: 500;
  color: var(--dp-text-1);
}
.activity-meta {
  font-size: 12px;
  color: var(--dp-text-3);
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.activity-dot-sep {
  color: var(--dp-text-4);
}
</style>
