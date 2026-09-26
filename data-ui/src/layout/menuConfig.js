export const modules = [
  {
    key: 'overview', name: '总览', icon: 'Odometer', path: '/overview',
    menus: [{ path: '/overview', title: '运营总览', icon: 'DataAnalysis' }]
  },
  {
    key: 'search', name: '资产检索', icon: 'Search', path: '/search',
    menus: [
      { path: '/search', title: '元数据检索', icon: 'Search' },
      { path: '/search/business', title: '业务数据检索', icon: 'List' },
      { path: '/search/model', title: '模型管理', icon: 'Cpu' }
    ]
  },
  {
    key: 'access', name: '数据接入', icon: 'Upload', path: '/access/overview',
    menus: [
      { path: '/access/overview', title: '接入概览', icon: 'DataAnalysis' },
      { path: '/access/datasources', title: '数据源管理', icon: 'Coin' },
      { path: '/access/survey', title: '数据调研', icon: 'View' },
      { path: '/access/tasks', title: '接入任务', icon: 'List' },
      { path: '/access/files', title: '文件上传', icon: 'Folder' },
      { path: '/access/model', title: '模型管理', icon: 'Cpu' }
    ]
  },
  {
    key: 'quality', name: '质量管控', icon: 'CircleCheck', path: '/quality/overview',
    menus: [
      { path: '/quality/overview', title: '质量总览', icon: 'DataAnalysis' },
      { path: '/quality/datasources', title: '数据源管理', icon: 'Coin' },
      { path: '/quality/rules', title: '规则管理', icon: 'Collection' },
      { path: '/quality/strategies', title: '策略管理', icon: 'Setting' },
      { path: '/quality/tasks', title: '任务调度', icon: 'Timer' },
      { path: '/quality/results', title: '结果分析', icon: 'PieChart' },
      { path: '/quality/model', title: '模型管理', icon: 'Cpu' }
    ]
  },
  {
    key: 'develop', name: '数据开发', icon: 'Monitor', path: '/develop/overview',
    menus: [
      { path: '/develop/overview', title: '开发概览', icon: 'DataAnalysis' },
      { path: '/develop/datasources', title: '开发数据源', icon: 'Coin' },
      { path: '/develop/workbench', title: 'SQL 工作台', icon: 'Edit' },
      { path: '/develop/processes', title: '数据处理', icon: 'Operation' },
      { path: '/develop/orchestration', title: '数据编排', icon: 'Share' },
      { path: '/develop/instances', title: '实例监控', icon: 'VideoPlay' },
      { path: '/develop/model', title: '模型管理', icon: 'Cpu' }
    ]
  },
  {
    key: 'modeling', name: '数据建模', icon: 'Grid', path: '/modeling/overview',
    menus: [
      { path: '/modeling/overview', title: '模型概览', icon: 'DataAnalysis' },
      { path: '/modeling/survey', title: '数据调研', icon: 'View' },
      { path: '/modeling/logical', title: '逻辑模型', icon: 'Guide' },
      { path: '/modeling/physical', title: '物理模型', icon: 'Files' },
      { path: '/modeling/deploy', title: '模型部署', icon: 'Promotion' },
      { path: '/modeling/ai-data', title: 'AI 业务数据', icon: 'MagicStick' },
      { path: '/modeling/model', title: '模型管理', icon: 'Cpu' }
    ]
  },
  {
    key: 'service', name: '数据服务', icon: 'Connection', path: '/service/operation',
    menus: [
      { path: '/service/operation', title: '服务运营', icon: 'DataAnalysis' },
      { path: '/service/datasources', title: '数据源管理', icon: 'Coin' },
      { path: '/service/catalog', title: '服务目录', icon: 'Menu' },
      { path: '/service/workbench', title: '服务开发', icon: 'Edit' },
      { path: '/service/apps', title: '应用管理', icon: 'Platform' },
      { path: '/service/auth', title: '授权审批', icon: 'Key' },
      { path: '/service/audit', title: '服务审计', icon: 'Document' },
      { path: '/service/test', title: '服务测试', icon: 'ChatDotRound' },
      { path: '/service/model', title: '模型管理', icon: 'Cpu' }
    ]
  },
  {
    key: 'governance', name: '数据治理', icon: 'SetUp', path: '/governance/standards',
    menus: [
      { path: '/governance/standards', title: '数据标准', icon: 'Document' },
      { path: '/governance/catalog', title: '标准目录', icon: 'Menu' },
      { path: '/governance/elements', title: '数据元', icon: 'Coin' },
      { path: '/governance/value-domains', title: '值域管理', icon: 'Grid' },
      { path: '/governance/refs', title: '引用标准', icon: 'Link' },
      { path: '/governance/map', title: '数据地图', icon: 'Location' },
      { path: '/governance/resources', title: '资源注册', icon: 'FolderOpened' },
      { path: '/governance/lineage', title: '血缘分析', icon: 'Share' },
      { path: '/governance/impact', title: '影响分析', icon: 'TrendCharts' }
    ]
  },
  {
    key: 'report', name: '报表平台', icon: 'DataAnalysis', path: '/report/overview',
    menus: [
      { path: '/report/overview', title: '报表概览', icon: 'DataAnalysis' },
      { path: '/report/datasources', title: '数据源管理', icon: 'Coin' },
      { path: '/report/marts', title: '数据集市', icon: 'Grid' },
      { path: '/report/charts', title: '图表资产', icon: 'PieChart' },
      { path: '/report/editor', title: '报表开发', icon: 'Edit' },
      { path: '/report/templates', title: '模板中心', icon: 'Picture' },
      { path: '/report/model', title: '模型管理', icon: 'Cpu' }
    ]
  },
  {
    key: 'docs', name: '文档中心', icon: 'Document', path: '/docs/overview',
    menus: [
      { path: '/docs/overview', title: '文档总览', icon: 'DataAnalysis' },
      { path: '/docs/mine', title: '我的文档', icon: 'User' },
      { path: '/docs/shared', title: '共享给我', icon: 'Share' },
      { path: '/docs/templates', title: '模板中心', icon: 'Picture' },
      { path: '/docs/spaces', title: '空间与目录', icon: 'FolderOpened' }
    ]
  },
  {
    key: 'agent', name: '智能体平台', icon: 'MagicStick', path: '/agent/knowledge',
    menus: [
      { path: '/agent/knowledge', title: '知识资产', icon: 'Reading' },
      { path: '/agent/planning', title: '自主规划', icon: 'Guide' },
      { path: '/agent/workbench', title: '工作台调试', icon: 'ChatDotRound' },
      { path: '/agent/apps', title: '发布应用', icon: 'Promotion' },
      { path: '/agent/square', title: '应用广场', icon: 'Shop' }
    ]
  },
  {
    key: 'lab', name: '数据实验室', icon: 'Lightning', path: '/lab/overview',
    menus: [
      { path: '/lab/overview', title: '实验总览', icon: 'DataAnalysis' },
      { path: '/lab/gov', title: '政务领域', icon: 'OfficeBuilding' },
      { path: '/lab/ecommerce', title: '电商零售', icon: 'ShoppingCart' },
      { path: '/lab/model-config', title: '模型配置', icon: 'Cpu' },
      { path: '/lab/datasources', title: '数据源管理', icon: 'Coin' }
    ]
  },
  {
    key: 'system', name: '系统管理', icon: 'Setting', path: '/system/services',
    menus: [
      { path: '/system/services', title: '服务管理', icon: 'Service' },
      { path: '/system/knowledge', title: '知识库管理', icon: 'Reading' },
      { path: '/system/license', title: '许可证管理', icon: 'Key' },
      { path: '/system/models', title: '模型管理', icon: 'Cpu' },
      { path: '/system/users', title: '用户管理', icon: 'User' },
      { path: '/system/roles', title: '角色管理', icon: 'Avatar' },
      { path: '/system/projects', title: '项目管理', icon: 'Folder' }
    ]
  }
]

export function findModuleByPath(path) {
  return modules.find(m => path === m.path || path.startsWith(m.path + '/') || path.startsWith('/' + m.key + '/')) || modules[0]
}

export const moduleColors = {
  overview: { primary: '#1664ff', light: '#e8f0ff', bg: '#f5f9ff' },
  search: { primary: '#00b42a', light: '#e8ffea', bg: '#f0fff4' },
  access: { primary: '#1664ff', light: '#e8f0ff', bg: '#f5f9ff' },
  quality: { primary: '#00b42a', light: '#e8ffea', bg: '#f0fff4' },
  develop: { primary: '#722ed1', light: '#f5e8ff', bg: '#faf5ff' },
  modeling: { primary: '#ff7d00', light: '#fff3e8', bg: '#fffaf5' },
  service: { primary: '#0fc6c2', light: '#e0fffa', bg: '#f0fffd' },
  governance: { primary: '#722ed1', light: '#f5e8ff', bg: '#faf5ff' },
  report: { primary: '#f759ab', light: '#ffe8f4', bg: '#fff5fa' },
  docs: { primary: '#ff7d00', light: '#fff3e8', bg: '#fffaf5' },
  agent: { primary: '#722ed1', light: '#f5e8ff', bg: '#faf5ff' },
  lab: { primary: '#f759ab', light: '#ffe8f4', bg: '#fff5fa' },
  system: { primary: '#86909c', light: '#f2f3f5', bg: '#f7f8fa' }
}
