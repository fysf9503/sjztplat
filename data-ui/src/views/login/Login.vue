<template>
  <div class="login-page">
    <div class="login-left">
      <div class="login-brand">
        <div class="brand-icon">
          <el-icon :size="20"><DataBoard /></el-icon>
        </div>
        <div class="brand-text">
          <div class="brand-name">ME DATA PLATFORM</div>
          <div class="brand-slogan">Unified Data Operating Console</div>
        </div>
        <div class="enterprise-badge">Enterprise Console</div>
      </div>

      <div class="login-hero">
        <h1 class="hero-title">AI数据中台运营控制台</h1>
        <p class="hero-desc">
          围绕数据全生命周期建设统一入口，已覆盖接入采集、质量管控、开发编排、资源地图、建模实验、数据服务、报表分析、智能体应用与系统治理，支撑从数据生产到服务发布的闭环运营。
        </p>
        <div class="hero-tags">
          <span class="hero-tag">数据全生命周期</span>
          <span class="hero-tag">AI 模型增强</span>
          <span class="hero-tag">资源画像与血缘</span>
          <span class="hero-tag">服务化与可视化输出</span>
        </div>
      </div>

      <div class="module-cards">
        <div v-for="m in leftModules" :key="m.key" class="module-card">
          <div class="module-icon" :style="{ background: m.iconBg, color: m.iconColor }">
            <el-icon :size="18"><component :is="m.icon" /></el-icon>
          </div>
          <div class="module-info">
            <div class="module-name">{{ m.name }}</div>
            <div class="module-desc">{{ m.desc }}</div>
          </div>
        </div>
      </div>

      <div class="login-footer">
        <div class="footer-title">平台能力覆盖</div>
        <div class="footer-modules">
          <span v-for="m in modules" :key="m.key" class="footer-module">{{ m.name }}</span>
        </div>
      </div>
    </div>

    <div class="login-right">
      <div class="login-form-wrap dp-fade-up">
        <div class="form-header">
          <div class="welcome-badge">WELCOME BACK</div>
          <div class="safe-entry">安全接入</div>
        </div>
        <h2 class="form-title">登录系统</h2>
        <p class="form-sub">请输入账号信息进入控制台</p>

        <el-form :model="form" size="large" @keyup.enter="doLogin" class="login-form">
          <el-form-item>
            <label class="form-label"><span class="required">*</span>用户名</label>
            <el-input v-model="form.username" placeholder="admin" :prefix-icon="User" clearable class="form-input" />
          </el-form-item>
          <el-form-item>
            <label class="form-label"><span class="required">*</span>密码</label>
            <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password class="form-input" />
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="doLogin">
            {{ loading ? '登录中...' : '登录并进入控制台' }}
          </el-button>
        </el-form>

        <div class="demo-account">
          <div class="demo-title">演示环境默认账号</div>
          <div class="demo-creds">admin / Admin@123</div>
        </div>

        <div class="form-footer">
          <span class="footer-item">统一入口</span>
          <span class="footer-dot">·</span>
          <span class="footer-item">权限可控</span>
          <span class="footer-dot">·</span>
          <span class="footer-item">审计留痕</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, DataBoard } from '@element-plus/icons-vue'
import { modules } from '@/layout/menuConfig'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'Admin@123' })

const leftModules = [
  { key: 'access', name: '数据接入', icon: 'Upload', desc: '统一纳管数据源、库表对象、接入任务、文件上传、接入概览与接入模型配置。', iconBg: 'rgba(22, 100, 255, 0.1)', iconColor: '#1664ff' },
  { key: 'quality', name: '质量管控', icon: 'CircleCheck', desc: '覆盖质量数据源、规则管理、策略配置、任务调度、结果分析和模型管理。', iconBg: 'rgba(0, 180, 42, 0.1)', iconColor: '#00b42a' },
  { key: 'develop', name: '数据开发', icon: 'Monitor', desc: '提供 SQL 工作台、数据编排、实例监控、开发数据源和开发模型管理。', iconBg: 'rgba(114, 46, 209, 0.1)', iconColor: '#722ed1' },
  { key: 'map', name: '数据地图', icon: 'Location', desc: '管理部门、业务系统、独立数据源、组织分类、资源检索、画像与血缘分析。', iconBg: 'rgba(15, 198, 194, 0.1)', iconColor: '#0fc6c2' },
  { key: 'modeling', name: '数据建模', icon: 'Grid', desc: '支撑行业调研、逻辑模型、物理模型、模型部署、数据预览和质量报告。', iconBg: 'rgba(255, 125, 0, 0.1)', iconColor: '#ff7d00' },
  { key: 'service', name: '数据服务', icon: 'Connection', desc: '沉淀服务运营、服务目录、应用授权、调用审计、服务测试与模型配置。', iconBg: 'rgba(0, 180, 42, 0.1)', iconColor: '#00b42a' },
  { key: 'report', name: '报表平台', icon: 'DataAnalysis', desc: '覆盖报表数据源、数据集市、图表资产、报表开发、主题模板和模型管理。', iconBg: 'rgba(247, 89, 171, 0.1)', iconColor: '#f759ab' },
  { key: 'agent', name: '智能体与治理', icon: 'MagicStick', desc: '整合智能体规划、应用广场、用户角色、许可证、模型服务和知识库治理。', iconBg: 'rgba(114, 46, 209, 0.1)', iconColor: '#722ed1' }
]

function doLogin() {
  if (!form.username || !form.password) return ElMessage.warning('请输入用户名和密码')
  loading.value = true
  setTimeout(() => {
    localStorage.setItem('token', 'mock-token-' + Date.now())
    localStorage.setItem('username', form.username === 'admin' ? '系统管理员' : form.username)
    ElMessage.success('登录成功，欢迎回来')
    router.push('/overview')
  }, 600)
}

onMounted(() => {})
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  background: linear-gradient(135deg, #eef3fb 0%, #f5f7fa 50%, #e8edf5 100%);
  padding: 24px;
  gap: 24px;
}

.login-left {
  flex: 1.2;
  background: #fff;
  border-radius: 18px;
  padding: 40px 48px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.04);
  position: relative;
  overflow: hidden;
}
.login-left::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 400px;
  height: 300px;
  background: radial-gradient(ellipse at top right, rgba(22, 100, 255, 0.06), transparent 70%);
  pointer-events: none;
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
  z-index: 1;
}
.brand-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1664ff, #4080ff);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 12px rgba(22, 100, 255, 0.3);
}
.brand-text {
  display: flex;
  flex-direction: column;
}
.brand-name {
  font-size: 13px;
  font-weight: 700;
  color: #1d2129;
  letter-spacing: 1px;
}
.brand-slogan {
  font-size: 11px;
  color: #86909c;
  margin-top: 2px;
  letter-spacing: 0.5px;
}
.enterprise-badge {
  margin-left: auto;
  padding: 5px 14px;
  border-radius: 20px;
  background: #f5f9ff;
  color: #1664ff;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid #e8f0ff;
}

.login-hero {
  margin-top: 40px;
  position: relative;
  z-index: 1;
}
.hero-title {
  font-size: 34px;
  font-weight: 700;
  color: #1d2129;
  margin: 0 0 14px;
  line-height: 1.3;
  letter-spacing: -0.5px;
}
.hero-desc {
  font-size: 14px;
  color: #4e5969;
  line-height: 1.8;
  margin: 0 0 20px;
  max-width: 620px;
}
.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.hero-tag {
  padding: 6px 14px;
  background: #f7f8fa;
  border: 1px solid #e5e6eb;
  border-radius: 20px;
  font-size: 12px;
  color: #4e5969;
  font-weight: 500;
}

.module-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 30px;
  position: relative;
  z-index: 1;
}
.module-card {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fafbfc;
  border: 1px solid #f2f3f5;
  border-radius: 12px;
  transition: all 0.25s;
}
.module-card:hover {
  background: #fff;
  border-color: #e8f0ff;
  box-shadow: 0 4px 12px rgba(22, 100, 255, 0.08);
  transform: translateY(-2px);
}
.module-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.module-info {
  flex: 1;
  min-width: 0;
}
.module-name {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  margin-bottom: 4px;
}
.module-desc {
  font-size: 12px;
  color: #86909c;
  line-height: 1.6;
}

.login-footer {
  margin-top: auto;
  padding-top: 24px;
  border-top: 1px dashed #e5e6eb;
  position: relative;
  z-index: 1;
}
.footer-title {
  font-size: 12px;
  color: #86909c;
  margin-bottom: 10px;
  font-weight: 500;
}
.footer-modules {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.footer-module {
  padding: 4px 12px;
  background: #f7f8fa;
  border-radius: 12px;
  font-size: 12px;
  color: #4e5969;
}

/* ---------- Right side ---------- */
.login-right {
  width: 480px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-form-wrap {
  width: 400px;
  background: #fff;
  border-radius: 16px;
  padding: 36px 32px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
  border: 1px solid #f2f3f5;
}

.form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.welcome-badge {
  font-size: 11px;
  font-weight: 600;
  color: #1664ff;
  letter-spacing: 1.5px;
  text-transform: uppercase;
}
.safe-entry {
  padding: 4px 10px;
  background: #f5f9ff;
  color: #1664ff;
  font-size: 11px;
  font-weight: 500;
  border-radius: 12px;
  border: 1px solid #e8f0ff;
}

.form-title {
  font-size: 24px;
  font-weight: 600;
  color: #1d2129;
  margin: 0 0 6px;
}
.form-sub {
  font-size: 13px;
  color: #86909c;
  margin: 0 0 28px;
}

.login-form {
  margin-bottom: 20px;
}
.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #4e5969;
  margin-bottom: 8px;
}
.form-label .required {
  color: #f53f3f;
  margin-right: 2px;
}
.form-input :deep(.el-input__wrapper) {
  height: 44px;
  box-shadow: 0 0 0 1px #e5e6eb inset;
}
.form-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #1664ff inset;
}
.form-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #1664ff inset;
}

.login-btn {
  width: 100%;
  height: 46px;
  margin-top: 8px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  background: linear-gradient(135deg, #1664ff, #4080ff);
  border: none;
  box-shadow: 0 4px 12px rgba(22, 100, 255, 0.25);
}
.login-btn:hover {
  box-shadow: 0 6px 16px rgba(22, 100, 255, 0.35);
  transform: translateY(-1px);
}

.demo-account {
  background: #f7f8fa;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 20px;
}
.demo-title {
  font-size: 12px;
  color: #86909c;
  margin-bottom: 6px;
}
.demo-creds {
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
  font-family: 'SF Mono', Consolas, monospace;
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  color: #86909c;
}
.footer-item {
  font-weight: 500;
}
.footer-dot {
  opacity: 0.5;
}
</style>
