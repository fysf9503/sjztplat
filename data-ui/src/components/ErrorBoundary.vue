<template>
  <div v-if="error" class="eb-fallback">
    <div class="eb-card dp-fade-up">
      <div class="eb-icon">
        <el-icon :size="40"><CircleCloseFilled /></el-icon>
      </div>
      <div class="eb-title">页面渲染出错</div>
      <div class="eb-desc">当前页面组件执行异常，其他功能不受影响，可从左侧菜单或顶部导航继续访问。</div>
      <div class="eb-error">{{ error?.message || error }}</div>
      <div class="eb-actions">
        <el-button type="primary" :icon="RefreshRight" @click="retry">重试本页</el-button>
        <el-button :icon="HomeFilled" @click="goHome">返回总览</el-button>
      </div>
    </div>
  </div>
  <slot v-else />
</template>

<script setup>
import { ref, watch, onErrorCaptured } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CircleCloseFilled, RefreshRight, HomeFilled } from '@element-plus/icons-vue'

const emit = defineEmits(['reset'])
const route = useRoute()
const router = useRouter()
const error = ref(null)

// 捕获子组件（页面）的错误，返回 false 阻止冒泡打断整个应用外壳。
// 渲染/初始化阶段错误会打断页面显示，需要兜底页；
// 事件处理器错误（如点击回调抛异常）页面仍然可用，仅记录。
onErrorCaptured((err, instance, info) => {
  console.error('[页面错误]', err, '\n错误来源:', info)
  if (!/native event handler|vnode hook|directive hook/.test(info)) {
    error.value = err
  }
  return false
})

// 切换路由时自动恢复，让新页面正常渲染
watch(() => route.fullPath, () => {
  if (error.value) {
    error.value = null
    emit('reset')
  }
})

function retry() {
  error.value = null
  emit('reset')
}

function goHome() {
  router.push('/overview')
}
</script>

<style scoped>
.eb-fallback {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.eb-card {
  max-width: 560px;
  width: 100%;
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: 14px;
  padding: 40px 36px;
  text-align: center;
}

.eb-icon {
  color: var(--dp-danger, #f53f3f);
  margin-bottom: 14px;
}

.eb-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--dp-text-1, #1d2129);
  margin-bottom: 8px;
}

.eb-desc {
  font-size: 13px;
  color: var(--dp-text-3, #86909c);
  line-height: 1.7;
  margin-bottom: 16px;
}

.eb-error {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: var(--dp-danger, #f53f3f);
  background: var(--dp-bg-page, #f7f8fa);
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 20px;
  word-break: break-all;
  text-align: left;
  max-height: 120px;
  overflow-y: auto;
}

.eb-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}
</style>
