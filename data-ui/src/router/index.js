import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/login/Login.vue') },
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    redirect: '/overview',
    children: [
      // 总览
      { path: 'overview', name: 'Overview', component: () => import('@/views/overview/Overview.vue') },

      // 资产检索
      { path: 'search', name: 'Search', component: () => import('@/views/search/Search.vue') },
      { path: 'search/business', name: 'SearchBusiness', component: () => import('@/views/search/Business.vue') },
      { path: 'search/model', name: 'SearchModel', component: () => import('@/views/search/Model.vue') },

      // 数据接入
      { path: 'access/overview', name: 'AccessOverview', component: () => import('@/views/access/Overview.vue') },
      { path: 'access/datasources', name: 'AccessDatasources', component: () => import('@/views/access/Datasources.vue') },
      { path: 'access/survey', name: 'AccessSurvey', component: () => import('@/views/access/Survey.vue') },
      { path: 'access/tasks', name: 'AccessTasks', component: () => import('@/views/access/Tasks.vue') },
      { path: 'access/files', name: 'AccessFiles', component: () => import('@/views/access/Files.vue') },
      { path: 'access/model', name: 'AccessModel', component: () => import('@/views/access/Model.vue') },

      // 质量管控
      { path: 'quality/overview', name: 'QualityOverview', component: () => import('@/views/quality/Overview.vue') },
      { path: 'quality/datasources', name: 'QualityDatasources', component: () => import('@/views/quality/Datasources.vue') },
      { path: 'quality/rules', name: 'QualityRules', component: () => import('@/views/quality/Rules.vue') },
      { path: 'quality/strategies', name: 'QualityStrategies', component: () => import('@/views/quality/Strategies.vue') },
      { path: 'quality/tasks', name: 'QualityTasks', component: () => import('@/views/quality/Tasks.vue') },
      { path: 'quality/results', name: 'QualityResults', component: () => import('@/views/quality/Results.vue') },
      { path: 'quality/model', name: 'QualityModel', component: () => import('@/views/quality/Model.vue') },

      // 数据开发
      { path: 'develop/overview', name: 'DevelopOverview', component: () => import('@/views/develop/Overview.vue') },
      { path: 'develop/datasources', name: 'DevelopDatasources', component: () => import('@/views/develop/Datasources.vue') },
      { path: 'develop/workbench', name: 'SqlWorkbench', component: () => import('@/views/develop/Workbench.vue') },
      { path: 'develop/processes', name: 'DevelopProcesses', component: () => import('@/views/develop/Processes.vue') },
      { path: 'develop/orchestration', name: 'Orchestration', component: () => import('@/views/develop/Orchestration.vue') },
      { path: 'develop/instances', name: 'Instances', component: () => import('@/views/develop/Instances.vue') },
      { path: 'develop/model', name: 'DevelopModel', component: () => import('@/views/develop/Model.vue') },

      // 数据建模
      { path: 'modeling/overview', name: 'ModelingOverview', component: () => import('@/views/modeling/Overview.vue') },
      { path: 'modeling/survey', name: 'ModelingSurvey', component: () => import('@/views/modeling/Survey.vue') },
      { path: 'modeling/logical', name: 'LogicalModels', component: () => import('@/views/modeling/Logical.vue') },
      { path: 'modeling/physical', name: 'PhysicalModels', component: () => import('@/views/modeling/Physical.vue') },
      { path: 'modeling/deploy', name: 'ModelDeploy', component: () => import('@/views/modeling/Deploy.vue') },
      { path: 'modeling/ai-data', name: 'AiBizData', component: () => import('@/views/modeling/AiData.vue') },
      { path: 'modeling/model', name: 'ModelingModel', component: () => import('@/views/modeling/Model.vue') },

      // 数据服务
      { path: 'service/operation', name: 'ServiceOperation', component: () => import('@/views/service/Operation.vue') },
      { path: 'service/datasources', name: 'ServiceDatasources', component: () => import('@/views/service/Datasources.vue') },
      { path: 'service/catalog', name: 'ServiceCatalog', component: () => import('@/views/service/Catalog.vue') },
      { path: 'service/workbench', name: 'ServiceWorkbench', component: () => import('@/views/service/Workbench.vue') },
      { path: 'service/apps', name: 'ServiceApps', component: () => import('@/views/service/Apps.vue') },
      { path: 'service/auth', name: 'ServiceAuth', component: () => import('@/views/service/Auth.vue') },
      { path: 'service/audit', name: 'ServiceAudit', component: () => import('@/views/service/Audit.vue') },
      { path: 'service/test', name: 'ServiceTest', component: () => import('@/views/service/Test.vue') },
      { path: 'service/model', name: 'ServiceModel', component: () => import('@/views/service/Model.vue') },

      // 数据治理
      { path: 'governance/standards', name: 'Standards', component: () => import('@/views/governance/Standards.vue') },
      { path: 'governance/catalog', name: 'StandardCatalog', component: () => import('@/views/governance/Catalog.vue') },
      { path: 'governance/elements', name: 'DataElements', component: () => import('@/views/governance/Elements.vue') },
      { path: 'governance/value-domains', name: 'ValueDomains', component: () => import('@/views/governance/ValueDomains.vue') },
      { path: 'governance/refs', name: 'RefStandards', component: () => import('@/views/governance/Refs.vue') },
      { path: 'governance/map', name: 'DataMap', component: () => import('@/views/governance/Map.vue') },
      { path: 'governance/resources', name: 'Resources', component: () => import('@/views/governance/Resources.vue') },
      { path: 'governance/lineage', name: 'Lineage', component: () => import('@/views/governance/Lineage.vue') },
      { path: 'governance/impact', name: 'Impact', component: () => import('@/views/governance/Impact.vue') },

      // 报表平台
      { path: 'report/overview', name: 'ReportOverview', component: () => import('@/views/report/Overview.vue') },
      { path: 'report/datasources', name: 'ReportDatasources', component: () => import('@/views/report/Datasources.vue') },
      { path: 'report/marts', name: 'DataMarts', component: () => import('@/views/report/Marts.vue') },
      { path: 'report/charts', name: 'ChartAssets', component: () => import('@/views/report/Charts.vue') },
      { path: 'report/editor', name: 'ReportEditor', component: () => import('@/views/report/Editor.vue') },
      { path: 'report/templates', name: 'ReportTemplates', component: () => import('@/views/report/Templates.vue') },
      { path: 'report/model', name: 'ReportModel', component: () => import('@/views/report/Model.vue') },

      // 文档中心
      { path: 'docs/overview', name: 'DocsOverview', component: () => import('@/views/docs/Overview.vue') },
      { path: 'docs/mine', name: 'MyDocs', component: () => import('@/views/docs/Mine.vue') },
      { path: 'docs/shared', name: 'SharedDocs', component: () => import('@/views/docs/Shared.vue') },
      { path: 'docs/templates', name: 'DocTemplates', component: () => import('@/views/docs/Templates.vue') },
      { path: 'docs/spaces', name: 'DocSpaces', component: () => import('@/views/docs/Spaces.vue') },

      // 智能体平台
      { path: 'agent/knowledge', name: 'AgentKnowledge', component: () => import('@/views/agent/Knowledge.vue') },
      { path: 'agent/planning', name: 'AgentPlanning', component: () => import('@/views/agent/Planning.vue') },
      { path: 'agent/workbench', name: 'AgentWorkbench', component: () => import('@/views/agent/Workbench.vue') },
      { path: 'agent/apps', name: 'AgentApps', component: () => import('@/views/agent/Apps.vue') },
      { path: 'agent/square', name: 'AgentSquare', component: () => import('@/views/agent/Square.vue') },

      // 数据实验室
      { path: 'lab/overview', name: 'LabOverview', component: () => import('@/views/lab/Overview.vue') },
      { path: 'lab/gov', name: 'LabGov', component: () => import('@/views/lab/Gov.vue') },
      { path: 'lab/ecommerce', name: 'LabEcommerce', component: () => import('@/views/lab/Ecommerce.vue') },
      { path: 'lab/model-config', name: 'LabModelConfig', component: () => import('@/views/lab/ModelConfig.vue') },
      { path: 'lab/datasources', name: 'LabDatasources', component: () => import('@/views/lab/Datasources.vue') },

      // 系统管理
      { path: 'system/services', name: 'SysServices', component: () => import('@/views/system/Services.vue') },
      { path: 'system/knowledge', name: 'SysKnowledge', component: () => import('@/views/system/Knowledge.vue') },
      { path: 'system/license', name: 'SysLicense', component: () => import('@/views/system/License.vue') },
      { path: 'system/models', name: 'SysModels', component: () => import('@/views/system/Models.vue') },
      { path: 'system/users', name: 'SysUsers', component: () => import('@/views/system/Users.vue') },
      { path: 'system/roles', name: 'SysRoles', component: () => import('@/views/system/Roles.vue') },
      { path: 'system/projects', name: 'SysProjects', component: () => import('@/views/system/Projects.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/overview' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

const whiteList = ['Login']

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const hasToken = !!token

  if (hasToken) {
    // 已登录
    if (to.name === 'Login') {
      // 已登录用户访问登录页，重定向到首页
      next({ path: '/overview' })
    } else {
      next()
    }
  } else {
    // 未登录
    if (whiteList.includes(to.name)) {
      // 在白名单中，直接放行
      next()
    } else {
      // 不在白名单，重定向到登录页，携带回跳地址
      next({ name: 'Login', query: { redirect: to.fullPath } })
    }
  }
})

// 懒加载模块在开发服务器重启或发布切换时可能取不到旧 chunk。
// 只自动刷新一次，避免页面停在空白状态或出现循环刷新。
router.onError(error => {
  if (!/Failed to fetch dynamically imported module|Importing a module script failed/i.test(error.message)) return
  const reloadKey = 'router-chunk-reload'
  if (sessionStorage.getItem(reloadKey)) return
  sessionStorage.setItem(reloadKey, '1')
  window.location.reload()
})

router.afterEach(() => {
  sessionStorage.removeItem('router-chunk-reload')
})

export default router
