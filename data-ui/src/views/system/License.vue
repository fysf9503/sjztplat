<template>
  <div class="dp-page">
    <div class="dp-page-header dp-fade-up">
      <div class="dp-page-eyebrow">LICENSE MANAGEMENT</div>
      <h1 class="dp-page-title">许可证管理</h1>
      <p class="dp-page-desc">查看和管理平台授权许可证信息。</p>
    </div>

    <div class="dp-grid-2">
      <!-- 许可证信息卡片 -->
      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <h3 class="dp-card-title">
            <el-icon :size="18" style="margin-right: 6px; color: var(--dp-primary)"><Key /></el-icon>
            许可证信息           </h3>
          <span class="dp-card-tag" :class="licenseStatus === '有效' ? 'primary' : 'danger'">
            {{ licenseStatus }}
          </span>
        </div>

        <el-descriptions :column="1" border size="default">
          <el-descriptions-item label="授权类型">
            <el-tag type="primary" effect="plain">{{ licenseInfo.type }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="许可证编号">
            <span class="dp-mono">{{ licenseInfo.licenseKey }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="授权公司">{{ licenseInfo.company }}</el-descriptions-item>
          <el-descriptions-item label="有效期">
            <span :style="{ color: daysRemaining <= 30 ? 'var(--dp-danger)' : 'inherit' }">
              {{ licenseInfo.startDate }} 至 {{ licenseInfo.endDate }}
              <el-tag v-if="daysRemaining <= 30" type="danger" size="small" style="margin-left: 8px">
                剩余 {{ daysRemaining }} 天               </el-tag>
              <el-tag v-else type="success" size="small" style="margin-left: 8px">
                剩余 {{ daysRemaining }} 天               </el-tag>
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="用户数限制">
            {{ licenseInfo.userLimit }} 人             <el-progress :percentage="Math.round(currentUsers / licenseInfo.userLimit * 100)" :stroke-width="6" style="margin-top: 6px" />
          </el-descriptions-item>
          <el-descriptions-item label="授权模块">
            <div class="module-list">
              <el-tag v-for="m in licenseInfo.modules" :key="m" type="success" effect="plain" size="small" style="margin: 3px">
                <el-icon style="margin-right: 3px"><CircleCheck /></el-icon>
                {{ m }}
              </el-tag>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="版本号">{{ licenseInfo.version }}</el-descriptions-item>
          <el-descriptions-item label="授权时间">{{ licenseInfo.issuedDate }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 使用统计 -->
      <div class="dp-card dp-fade-up">
        <div class="dp-card-header">
          <h3 class="dp-card-title">
            <el-icon :size="18" style="margin-right: 6px; color: var(--dp-success)"><DataAnalysis /></el-icon>
            使用统计
          </h3>
        </div>

        <div class="usage-stats">
          <div class="usage-item">
            <div class="usage-icon" style="background: var(--dp-primary-light); color: var(--dp-primary)">
              <el-icon><User /></el-icon>
            </div>
            <div class="usage-info">
              <div class="usage-label">当前用户数</div>
              <div class="usage-value">{{ currentUsers }}<span class="usage-unit">/ {{ licenseInfo.userLimit }} 个</span></div>
              <el-progress :percentage="Math.round(currentUsers / licenseInfo.userLimit * 100)" :stroke-width="4" :show-text="false" />
            </div>
          </div>

          <div class="usage-item">
            <div class="usage-icon" style="background: var(--dp-success-light); color: var(--dp-success)">
              <el-icon><Grid /></el-icon>
            </div>
            <div class="usage-info">
              <div class="usage-label">已用模块数</div>
              <div class="usage-value">{{ usedModules }}<span class="usage-unit">/ {{ totalModules }} 个</span></div>
              <el-progress :percentage="Math.round(usedModules / totalModules * 100)" :stroke-width="4" :show-text="false" status="success" />
            </div>
          </div>

          <div class="usage-item">
            <div class="usage-icon" style="background: var(--dp-warning-light); color: var(--dp-warning)">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="usage-info">
              <div class="usage-label">已接入数据源</div>
              <div class="usage-value">28<span class="usage-unit">个</span></div>
            </div>
          </div>

          <div class="usage-item">
            <div class="usage-icon" style="background: var(--dp-purple-light); color: var(--dp-purple)">
              <el-icon><Service /></el-icon>
            </div>
            <div class="usage-info">
              <div class="usage-label">API 服务数</div>
              <div class="usage-value">86<span class="usage-unit">个</span></div>
            </div>
          </div>

          <div class="usage-item">
            <div class="usage-icon" style="background: var(--dp-cyan-light); color: var(--dp-cyan)">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="usage-info">
              <div class="usage-label">智能体数量</div>
              <div class="usage-value">12<span class="usage-unit">个</span></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 授权码管理 -->
    <div class="dp-card dp-fade-up">
      <div class="dp-card-header">
        <h3 class="dp-card-title">
          <el-icon :size="18" style="margin-right: 6px; color: var(--dp-warning)"><Lock /></el-icon>
          授权码管理         </h3>
      </div>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="请妥善保管您的授权码，更换服务器或升级系统后可能需要重新激活。"
        style="margin-bottom: 16px"
      />

      <el-form :model="licenseForm" label-width="120px" style="max-width: 600px">
        <el-form-item label="授权码">
          <el-input v-model="licenseForm.licenseKey" placeholder="请输入授权码" show-password>
            <template #append>
              <el-button @click="updateLicense" type="primary">更新授权</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="授权文件">
          <el-upload drag :auto-upload="false" :limit="1" accept=".lic,.key">
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">将授权文件拖到此处，或<em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 .lic / .key 格式的授权文件</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="updateLicense">更新授权码</el-button>
          <el-button @click="exportLicense">导出授权信息</el-button>
          <el-button type="info" @click="viewMachineCode">查看机器码</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 机器码对话框 -->
    <el-dialog v-model="machineCodeVisible" title="机器码信息" width="480px">
      <el-alert type="warning" :closable="false" show-icon title="请将以下机器码提供给供应商，以生成对应的授权文件。" style="margin-bottom: 16px" />
      <el-form label-width="100px">
        <el-form-item label="机器码">
          <el-input v-model="machineCode" readonly type="textarea" :rows="3" class="dp-mono">
            <template #append>
              <el-button :icon="CopyDocument" @click="copyMachineCode">复制</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="硬件指纹">
          <el-input v-model="hardwareFingerprint" readonly class="dp-mono" />
        </el-form-item>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'

const licenseInfo = reactive({
  type: '企业版（Enterprise）',
  licenseKey: 'DP-ENT-2026-8A7F-3B2C-9D1E',
  company: '某科技有限公司',
  startDate: '2026-01-01',
  endDate: '2027-01-01',
  userLimit: 100,
  modules: ['数据接入', '数据开发', '数据建模', '数据质量', '数据服务', '数据治理', '报表平台', '智能体平台', '系统管理'],
  version: 'v2.3.1',
  issuedDate: '2025-12-15'
})

const licenseStatus = computed(() => {
  return daysRemaining.value > 0 ? '有效' : '已过期'
})

const daysRemaining = computed(() => {
  const end = new Date(licenseInfo.endDate)
  const now = new Date(2026, 8, 26)
  return Math.ceil((end - now) / (1000 * 60 * 60 * 24))
})

const currentUsers = ref(68)
const usedModules = computed(() => licenseInfo.modules.length)
const totalModules = 10

const licenseForm = reactive({
  licenseKey: ''
})

const machineCodeVisible = ref(false)
const machineCode = ref('MACH-2026-8A7F3B2C9D1E4F5A-6B7C8D9E')
const hardwareFingerprint = ref('HW-FP-0x8A7B3C2D9E1F4A5B')

function updateLicense() {
  if (!licenseForm.licenseKey) {
    return ElMessage.warning('请输入授权码')
  }
  ElMessage.success('授权码更新成功，许可证信息已刷新')
  licenseForm.licenseKey = ''
}

function exportLicense() {
  ElMessage.success('授权信息已导出为 license_info.txt')
}

function viewMachineCode() {
  machineCodeVisible.value = true
}

function copyMachineCode() {
  navigator.clipboard.writeText(machineCode.value)
  ElMessage.success('机器码已复制到剪贴板')
}
</script>

<style scoped>
.module-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.usage-stats {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.usage-item {
  display: flex;
  gap: 14px;
  align-items: center;
}
.usage-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.usage-info {
  flex: 1;
}
.usage-label {
  font-size: 13px;
  color: var(--dp-text-3);
  margin-bottom: 4px;
}
.usage-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--dp-text-1);
  line-height: 1.2;
  font-family: 'DIN Alternate', 'Helvetica Neue', sans-serif;
}
.usage-unit {
  font-size: 12px;
  font-weight: 400;
  color: var(--dp-text-3);
  margin-left: 4px;
}
</style>
