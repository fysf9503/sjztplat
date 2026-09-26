<template>
  <div class="app-layout">
    <!-- 顶部导航 -->
    <header class="app-header">
      <div class="header-left">
        <div class="logo" @click="router.push('/overview')">
          <div class="logo-icon">
            <el-icon :size="16"><DataBoard /></el-icon>
          </div>
          <span class="logo-text">AI数据中台</span>
        </div>
      </div>

      <nav class="header-nav">
        <div
          v-for="m in modules"
          :key="m.key"
          class="nav-item"
          :class="{ active: activeModule.key === m.key }"
          @click="router.push(m.path)"
        >
          {{ m.name }}
        </div>
      </nav>

      <div class="header-right">
        <el-select v-model="currentProject" size="default" class="project-select" placeholder="选择项目">
          <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-button text class="logout-btn" @click="logout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出系统</span>
        </el-button>
      </div>
    </header>

    <div class="app-body">
      <!-- 左侧菜单 -->
      <aside class="app-sidebar">
        <div class="sidebar-title">
          {{ activeModule.name }}
        </div>
        <div class="sidebar-menu">
          <div
            v-for="menu in activeModule.menus"
            :key="menu.path"
            class="menu-item"
            :class="{ active: isMenuActive(menu.path) }"
            @click="router.push(menu.path)"
          >
            <span class="menu-bar" />
            <el-icon class="menu-icon"><component :is="menu.icon" /></el-icon>
            <span class="menu-text">{{ menu.title }}</span>
          </div>
        </div>
      </aside>

      <!-- 主内容区 -->
      <main class="app-main">
        <router-view v-slot="{ Component }">
          <keep-alive :max="15">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </main>
    </div>

    <!-- 欢迎弹窗 -->
    <el-dialog
      v-model="welcomeVisible"
      width="720px"
      :show-close="true"
      :close-on-click-modal="false"
      class="welcome-dialog"
    >
      <template #header>
        <div class="welcome-header">
          <div class="welcome-badge">WELCOME</div>
          <el-button text size="small" class="docs-link" @click="goDocs">
            文档中心
            <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </template>

      <div class="welcome-content">
        <h2 class="welcome-title">欢迎使用，{{ user.name }}</h2>
        <p class="welcome-desc">
          AI 智能中台定位为面向数据学习、实验实践和原型验证的一体化数据智能平台，聚焦数据治理、开发分析、可视化展示与智能应用验证等核心场景，通过大模型能力提升个人学习、日常开发辅助和原型验证效率。如需了解各模块职责与详细功能，可前往文档中心继续查看。
        </p>

        <div class="welcome-modules">
          <div
            v-for="m in welcomeModules"
            :key="m.key"
            class="welcome-module"
            :class="{ active: activeModule.key === m.key }"
            @click="goModule(m.path)"
          >
            {{ m.name }}
          </div>
        </div>

        <div class="welcome-sections">
          <div class="welcome-section">
            <div class="section-icon blue">
              <el-icon><User /></el-icon>
            </div>
            <div class="section-content">
              <div class="section-title">主要服务对象</div>
              <ul class="section-list">
                <li>面向产品设计、功能建设、流程优化、原型验证的产品研发人员</li>
                <li>面向数据接入、清洗处理、分析建模、可视化服务的数据开发人员</li>
                <li>面向数据标准、数据质量、数据安全、运维运营的数据治理人员</li>
                <li>面向模型配置、知识库建设、智能体编排、场景落地的大模型应用人员</li>
              </ul>
            </div>
          </div>

          <div class="welcome-section">
            <div class="section-icon green">
              <el-icon><InfoFilled /></el-icon>
            </div>
            <div class="section-content">
              <div class="section-title">使用说明</div>
              <ul class="section-list">
                <li>平台当前更适合个人学习、场景演示、能力体验和日常开发辅助使用。</li>
                <li>当前版本仍在持续迭代，尚未进行系统性的安全、性能测试，正式业务使用前请结合人工复核、专项验证和场景测试。</li>
                <li>如需系统了解模块职责、操作路径和功能边界，建议优先前往文档中心查看详细说明。</li>
              </ul>
            </div>
          </div>
        </div>

        <div class="welcome-feedback-title">反馈与合作</div>
        <p class="welcome-feedback-desc">
          欢迎围绕需求建议、问题修复、平台合作与能力升级持续沟通。
        </p>

        <div class="feedback-cards">
          <div class="feedback-card">
            <div class="feedback-icon blue">
              <el-icon><MagicStick /></el-icon>
            </div>
            <div class="feedback-info">
              <div class="feedback-title">提需求</div>
              <div class="feedback-desc">欢迎提出新功能想法、流程优化建议和页面体验改进点。</div>
            </div>
          </div>
          <div class="feedback-card">
            <div class="feedback-icon green">
              <el-icon><Bell /></el-icon>
            </div>
            <div class="feedback-info">
              <div class="feedback-title">报 Bug</div>
              <div class="feedback-desc">发现异常、文案问题、交互缺陷或数据结果不符合预期时，可直接反馈。</div>
            </div>
          </div>
          <div class="feedback-card">
            <div class="feedback-icon purple">
              <el-icon><OfficeBuilding /></el-icon>
            </div>
            <div class="feedback-info">
              <div class="feedback-title">平台合作</div>
              <div class="feedback-desc">支持围绕商业推广、功能升级、场景共建和平台持续运营开展合作。</div>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  SwitchButton, DataBoard, ArrowRight, User, InfoFilled,
  MagicStick, Bell, OfficeBuilding
} from '@element-plus/icons-vue'
import { modules, findModuleByPath } from './menuConfig'
import { sysProjects } from '@/mock'

const route = useRoute()
const router = useRouter()
const welcomeVisible = ref(false)
const currentProject = ref('default')
const projects = ref([{ id: 'default', name: '默认项目' }, ...sysProjects.slice(0, 4).map(p => ({ id: p.code, name: p.name }))])

const user = ref({ name: localStorage.getItem('username') || '系统管理员' })

const activeModule = computed(() => findModuleByPath(route.path))

const welcomeModules = computed(() => modules.filter(m => m.key !== 'overview' && m.key !== 'system'))

function isMenuActive(path) {
  if (path === route.path) return true
  // 处理搜索模块的特殊情况
  if (route.path.startsWith(path + '/')) return true
  return false
}

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('welcomeDismissed')
  ElMessage.success('已退出登录')
  router.push('/login')
}

function goDocs() {
  welcomeVisible.value = false
  router.push('/docs/overview')
}

function goModule(path) {
  welcomeVisible.value = false
  router.push(path)
}

onMounted(() => {
  if (!localStorage.getItem('welcomeDismissed')) {
    setTimeout(() => { welcomeVisible.value = true }, 300)
  }
})

watch(welcomeVisible, (v) => {
  if (!v) localStorage.setItem('welcomeDismissed', '1')
})
</script>

<style scoped>
.app-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--dp-bg-page);
}

/* ---------- Header ---------- */
.app-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid var(--dp-border-light);
  display: flex;
  align-items: center;
  padding: 0 16px;
  flex-shrink: 0;
  gap: 16px;
}

.header-left {
  flex-shrink: 0;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 8px;
  transition: background 0.2s;
}
.logo:hover {
  background: var(--dp-bg-page);
}
.logo-icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(135deg, #1664ff, #4080ff);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.logo-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.header-nav {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 2px;
  overflow-x: auto;
  scrollbar-width: none;
  height: 100%;
  padding: 0 8px;
}
.header-nav::-webkit-scrollbar { display: none; }

.nav-item {
  height: 36px;
  padding: 0 14px;
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-2);
  border-radius: 8px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.nav-item:hover {
  background: var(--dp-bg-page);
  color: var(--dp-text-1);
}
.nav-item.active {
  background: var(--dp-primary);
  color: #fff;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.project-select {
  width: 140px;
}
.project-select :deep(.el-input__wrapper) {
  height: 34px;
  box-shadow: 0 0 0 1px var(--dp-border) inset;
  background: var(--dp-bg-page);
}
.logout-btn {
  height: 34px;
  padding: 0 12px;
  border-radius: 8px;
  color: var(--dp-text-2);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.logout-btn:hover {
  background: var(--dp-bg-page);
  color: var(--dp-danger);
}

/* ---------- Body ---------- */
.app-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

/* ---------- Sidebar ---------- */
.app-sidebar {
  width: 210px;
  background: #fff;
  border-right: 1px solid var(--dp-border-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-title {
  height: 52px;
  display: flex;
  align-items: center;
  padding: 0 20px;
  font-size: 15px;
  font-weight: 600;
  color: var(--dp-text-1);
  border-bottom: 1px solid var(--dp-border-light);
}

.sidebar-menu {
  flex: 1;
  padding: 10px 10px;
  overflow-y: auto;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 38px;
  padding: 0 12px;
  margin-bottom: 2px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--dp-text-2);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.15s;
  position: relative;
}
.menu-item:hover {
  background: var(--dp-bg-page);
  color: var(--dp-text-1);
}
.menu-item.active {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  font-weight: 600;
}
.menu-bar {
  width: 3px;
  height: 0;
  border-radius: 2px;
  background: var(--dp-primary);
  position: absolute;
  left: -10px;
  top: 50%;
  transform: translateY(-50%);
  transition: height 0.2s;
}
.menu-item.active .menu-bar {
  height: 16px;
}
.menu-icon {
  font-size: 15px;
  opacity: 0.85;
}

/* ---------- Main ---------- */
.app-main {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

/* ---------- Welcome Dialog ---------- */
.welcome-dialog :deep(.el-dialog) {
  border-radius: 18px;
}
.welcome-dialog :deep(.el-dialog__header) {
  border-bottom: none;
  padding: 20px 24px 0;
  margin-right: 0;
}
.welcome-dialog :deep(.el-dialog__body) {
  padding: 12px 24px 24px;
}

.welcome-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.welcome-badge {
  padding: 4px 12px;
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 1px;
  border-radius: 12px;
}
.docs-link {
  color: var(--dp-text-2);
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.docs-link:hover {
  color: var(--dp-primary);
}

.welcome-content {
  max-height: 70vh;
  overflow-y: auto;
  padding-right: 4px;
}

.welcome-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--dp-text-1);
  margin: 0 0 10px;
  letter-spacing: -0.5px;
}

.welcome-desc {
  font-size: 13px;
  color: var(--dp-text-2);
  line-height: 1.8;
  margin: 0 0 18px;
}

.welcome-modules {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 20px;
}
.welcome-module {
  padding: 5px 12px;
  background: var(--dp-bg-page);
  color: var(--dp-text-2);
  font-size: 12px;
  font-weight: 500;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}
.welcome-module:hover {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
}
.welcome-module.active {
  background: var(--dp-primary-light);
  color: var(--dp-primary);
  border-color: var(--dp-primary);
}

.welcome-sections {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-bottom: 20px;
}
.welcome-section {
  background: var(--dp-bg-page);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  gap: 12px;
}
.section-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #fff;
  font-size: 16px;
}
.section-icon.blue {
  background: linear-gradient(135deg, #1664ff, #4080ff);
}
.section-icon.green {
  background: linear-gradient(135deg, #00b42a, #23c348);
}
.section-content {
  flex: 1;
  min-width: 0;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 8px;
}
.section-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.section-list li {
  font-size: 12px;
  color: var(--dp-text-2);
  line-height: 1.7;
  padding-left: 14px;
  position: relative;
  margin-bottom: 2px;
}
.section-list li::before {
  content: '';
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--dp-primary);
  position: absolute;
  left: 0;
  top: 8px;
}

.welcome-feedback-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 4px;
}
.welcome-feedback-desc {
  font-size: 12px;
  color: var(--dp-text-3);
  margin: 0 0 12px;
}

.feedback-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.feedback-card {
  display: flex;
  gap: 10px;
  padding: 14px;
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}
.feedback-card:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 4px 12px rgba(22, 100, 255, 0.1);
  transform: translateY(-2px);
}
.feedback-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.feedback-icon.blue { background: linear-gradient(135deg, #1664ff, #4080ff); }
.feedback-icon.green { background: linear-gradient(135deg, #00b42a, #23c348); }
.feedback-icon.purple { background: linear-gradient(135deg, #722ed1, #9254de); }

.feedback-info { flex: 1; min-width: 0; }
.feedback-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 4px;
}
.feedback-desc {
  font-size: 12px;
  color: var(--dp-text-3);
  line-height: 1.6;
}
</style>
