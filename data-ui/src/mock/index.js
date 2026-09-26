import { rnd, pick, int, intId, dateStr, genRows, genTrend, owners, depts } from './helpers'

const DB_NAMES = ['ods_customer_db', 'dwd_trade_db', 'dws_summary_db', 'ads_report_db', 'dim_public_db', 'ods_finance_db', 'dwd_log_db', 'dws_user_db']
const TABLE_NAMES = ['customer_info', 'order_detail', 'product_sku', 'trade_flow', 'user_behavior', 'payment_record', 'refund_order', 'member_profile', 'shop_daily_stat', 'goods_stock', 'channel_visit', 'coupon_usage', 'invoice_info', 'address_book', 'login_log']
const FIELDS = ['id', 'name', 'code', 'type', 'status', 'amount', 'create_time', 'update_time', 'phone', 'address', 'level', 'sex', 'age', 'balance', 'order_no', 'user_id', 'price', 'qty', 'channel', 'city']

export const dataSources = genRows(18, i => ({
  id: intId(), name: pick(['核心交易库', '客户主数据库', '日志采集库', '财务系统库', '会员中心库', '商品中心库', '营销活动库', '供应链库', 'OA系统库', 'HR系统库']) + '-' + (i + 1),
  type: pick(['MySQL', 'Oracle', 'PostgreSQL', 'Hive', 'Kafka', 'API', 'FTP', 'ClickHouse']),
  env: pick(['生产', '测试', '开发']),
  host: `${int(10, 255)}.${int(10, 255)}.${int(1, 254)}.${int(1, 254)}:${int(1000, 9999)}`,
  database: pick(DB_NAMES),
  tables: int(12, 480),
  sizeGB: int(5, 2000),
  status: pick(['已连接', '已连接', '已连接', '未连接', '连接异常']),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 30))
}))

export const surveyTasks = genRows(12, i => ({
  id: intId(), name: pick(['营销域数据摸底', '客户域数据调研', '交易域数据盘点', '财务域数据调研', '供应链数据摸底', '用户行为数据调研']) + '-' + (i + 1),
  target: pick(['核心交易库', '客户主数据库', '日志采集库', '财务系统库']),
  scope: pick(['全库扫描', '指定表', '抽样分析']),
  tables: int(10, 120),
  fields: int(80, 900),
  progress: int(5, 100),
  status: pick(['调研中', '已完成', '已完成', '待启动']),
  findings: int(3, 26),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 20))
}))

export const accessTasks = genRows(20, i => ({
  id: intId(), name: 'sync_' + pick(TABLE_NAMES) + '_' + (i + 1),
  source: pick(DB_NAMES) + '.' + pick(TABLE_NAMES),
  target: pick(DB_NAMES) + '.' + pick(TABLE_NAMES),
  mode: pick(['全量', '增量', '实时']),
  freq: pick(['每小时', '每天 00:30', '每周一 02:00', '每5分钟', '实时']),
  status: pick(['运行中', '成功', '成功', '成功', '失败', '等待调度']),
  lastRun: dateStr(int(0, 3)),
  duration: `${int(1, 58)}s`,
  rows: int(1000, 9999999),
  owner: pick(owners)
}))

export const fileUploads = genRows(15, i => ({
  id: intId(), name: pick(['客户清单', '订单导出', '商品资料', '退货明细', '会员数据', '门店销售', '库存快照']) + '_' + dateStr(int(1, 60), false) + pick(['.csv', '.xlsx', '.json']),
  format: pick(['CSV', 'Excel', 'JSON']),
  sizeMB: +(rnd() * 500 + 1).toFixed(1),
  rows: int(100, 900000),
  target: pick(DB_NAMES) + '.' + pick(TABLE_NAMES),
  status: pick(['已入库', '已入库', '解析中', '上传失败']),
  uploader: pick(owners),
  time: dateStr(int(0, 30))
}))

export const qualityRules = genRows(24, i => ({
  id: intId(), name: ['非空校验', '值域校验', '唯一性校验', '格式校验', '一致性校验', '及时性校验', '参照完整性', '波动校验'][i % 8] + '-' + pick(TABLE_NAMES),
  category: pick(['完整性', '准确性', '一致性', '及时性', '唯一性', '有效性']),
  table: pick(DB_NAMES) + '.' + pick(TABLE_NAMES),
  field: pick(FIELDS),
  severity: pick(['高', '中', '中', '低']),
  status: pick(['启用', '启用', '启用', '停用']),
  hitRate: +(rnd() * 5).toFixed(2),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 40))
}))

export const qualityStrategies = genRows(10, i => ({
  id: intId(), name: pick(['交易数据质量策略', '客户数据质量策略', '财务数据质量策略', '日志数据质量策略', '商品数据质量策略']) + '-' + (i + 1),
  scene: pick(['核心交易', '客户域', '财务域', '日志域']),
  rules: int(5, 40),
  tables: int(3, 30),
  schedule: pick(['每天 01:00', '每小时', '每周一 03:00']),
  status: pick(['启用', '启用', '停用']),
  passRate: +(92 + rnd() * 8).toFixed(2),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 30))
}))

export const qualityTasks = genRows(16, i => ({
  id: intId(), name: 'quality_job_' + (i + 1),
  strategy: pick(['交易数据质量策略', '客户数据质量策略', '财务数据质量策略', '日志数据质量策略']),
  schedule: pick(['每天 01:00', '每小时', '每周一 03:00']),
  lastRun: dateStr(int(0, 2)),
  duration: `${int(1, 45)}m${int(0, 59)}s`,
  status: pick(['成功', '成功', '成功', '运行中', '失败']),
  totalChecks: int(20, 300),
  failedChecks: int(0, 8),
  passRate: +(90 + rnd() * 10).toFixed(2)
}))

export const qualityResults = genRows(30, i => ({
  id: intId(), task: 'quality_job_' + int(1, 16),
  time: dateStr(int(0, 14)),
  table: pick(DB_NAMES) + '.' + pick(TABLE_NAMES),
  rule: pick(['非空校验', '值域校验', '唯一性校验', '格式校验', '一致性校验', '及时性校验']),
  totalRows: int(1000, 9999999),
  hitRows: int(0, 9999),
  passRate: +(95 + rnd() * 5).toFixed(2),
  severity: pick(['高', '中', '低']),
  status: pick(['通过', '告警', '不通过'])
}))

export const sqlScripts = genRows(16, i => ({
  id: intId(), name: pick(['交易宽表加工', '客户标签计算', '日销售汇总', '退款率统计', '会员等级更新', '商品销量排名', '渠道转化分析', '库存预警计算']) + '.sql',
  database: pick(DB_NAMES),
  lines: int(20, 500),
  status: pick(['已发布', '开发中', '已发布', '已下线']),
  creator: pick(owners),
  updatedAt: dateStr(int(0, 30))
}))

export const dataProcesses = genRows(14, i => ({
  id: intId(), name: pick(['交易宽表加工流程', '客户标签日更新', '销售主题汇总', '会员积分计算', '商品库存同步', '退款对账流程']) + '-' + (i + 1),
  type: pick(['SQL任务', '脚本任务', '数据同步', '混合流程']),
  nodes: int(3, 15),
  schedule: pick(['每天 00:30', '每小时', '每月1日 01:00']),
  status: pick(['运行中', '已发布', '开发中', '已暂停']),
  avgDuration: `${int(1, 40)}m`,
  owner: pick(owners),
  updatedAt: dateStr(int(0, 20))
}))

export const workflowInstances = genRows(25, i => ({
  id: 'wf_' + dateStr(int(0, 2), false).replaceAll('-', '') + '_' + int(1000, 9999),
  workflow: pick(['交易宽表加工流程', '客户标签日更新', '销售主题汇总', '会员积分计算', '商品库存同步']),
  bizDate: dateStr(int(1, 3), false),
  start: dateStr(int(0, 2)),
  duration: `${int(1, 50)}m${int(10, 59)}s`,
  status: pick(['成功', '成功', '成功', '失败', '运行中', '等待依赖']),
  nodes: int(4, 14),
  failedNodes: pick([0, 0, 0, 1, 2]),
  trigger: pick(['调度触发', '手动触发', '补数运行'])
}))

export const logicalModels = genRows(12, i => ({
  id: intId(), name: pick(['客户主题域模型', '交易主题域模型', '商品主题域模型', '营销主题域模型', '供应链主题域模型', '财务主题域模型']) + '-V' + int(1, 3),
  domain: pick(['客户域', '交易域', '商品域', '营销域', '供应链域', '财务域']),
  entities: int(6, 24),
  relations: int(5, 30),
  fields: int(40, 260),
  status: pick(['已发布', '设计中', '已发布', '已归档']),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 45))
}))

export const physicalModels = genRows(12, i => ({
  id: intId(), name: pick(['dwd_customer', 'dwd_trade', 'dws_summary', 'ads_report', 'dim_public']) + '_v' + int(1, 5),
  source: pick(['客户主题域模型-V2', '交易主题域模型-V3', '商品主题域模型-V1']),
  database: pick(DB_NAMES),
  tables: int(5, 20),
  fields: int(40, 200),
  ddlGenerated: pick(['已生成', '已生成', '未生成']),
  deployed: pick(['已部署', '已部署', '未部署']),
  syncStatus: pick(['已同步', '同步中', '未同步']),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 30))
}))

export const services = genRows(20, i => ({
  id: intId(), name: 'api_' + pick(['customer', 'order', 'product', 'stat', 'report', 'member']) + '_' + (i + 1),
  path: '/api/v1/' + pick(['customer', 'order', 'product', 'stat', 'report', 'member']) + '/' + pick(['query', 'detail', 'list', 'aggregate']),
  method: pick(['GET', 'GET', 'POST']),
  source: pick(DB_NAMES) + '.' + pick(TABLE_NAMES),
  protocol: pick(['REST', 'REST', 'GraphQL']),
  qps: int(10, 2000),
  callsToday: int(1000, 999999),
  avgCost: int(10, 500),
  status: pick(['已发布', '已发布', '已发布', '开发中', '已下线']),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 20))
}))

export const serviceApps = genRows(12, i => ({
  id: intId(), name: pick(['营销中台', 'CRM系统', '数据看板', '风控系统', '小程序', 'OA门户', 'ERP系统', 'BI平台']) + '-' + (i + 1),
  appKey: 'AK' + int(100000, 999999),
  type: pick(['内部应用', '外部应用', '第三方合作']),
  boundServices: int(1, 12),
  callsMonth: int(10000, 9999999),
  rateLimit: int(100, 5000) + ' QPS',
  status: pick(['正常', '正常', '正常', '已禁用']),
  owner: pick(owners),
  createdAt: dateStr(int(30, 300))
}))

export const authRequests = genRows(15, i => ({
  id: intId(), applicant: pick(owners),
  dept: pick(depts),
  service: 'api_' + pick(['customer', 'order', 'product', 'stat']) + '_' + int(1, 20),
  appName: pick(['营销中台', 'CRM系统', '数据看板', '风控系统']),
  reason: pick(['营销活动数据分析', '客户画像查询', '经营日报取数', '风控模型特征', '专题报表开发']),
  expire: dateStr(-int(30, 365), false),
  status: pick(['待审批', '已通过', '已通过', '已拒绝', '已过期']),
  applyTime: dateStr(int(1, 30))
}))

export const auditLogs = genRows(30, i => ({
  id: intId(), time: dateStr(int(0, 2)),
  app: pick(['营销中台', 'CRM系统', '数据看板', '风控系统', '小程序']),
  service: '/api/v1/' + pick(['customer', 'order', 'product', 'stat']) + '/' + pick(['query', 'detail', 'list']),
  method: pick(['GET', 'POST']),
  cost: int(5, 800),
  status: pick([200, 200, 200, 200, 429, 500]),
  ip: `${int(10, 255)}.${int(10, 255)}.${int(1, 254)}.${int(1, 254)}`
}))

export const standards = genRows(24, i => ({
  id: intId(), code: 'STD-' + pick(['BASE', 'CODE', 'IDX', 'NAME']) + '-' + (1000 + i),
  name: pick(['性别代码', '行政区划代码', '币种代码', '证件类型代码', '行业分类代码', '客户状态代码', '订单状态代码', '渠道类型代码', '计量单位代码', '手机号格式规范', '统一社会信用代码', '日期时间格式规范']),
  category: pick(['基础标准', '编码标准', '指标标准', '命名标准']),
  level: pick(['国家标准', '行业标准', '企业标准', '国家标准', '企业标准']),
  version: 'V' + int(1, 3) + '.' + int(0, 9),
  refs: int(3, 60),
  status: pick(['生效', '生效', '生效', '草稿', '已废止']),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 60))
}))

export const dataElements = genRows(24, i => ({
  id: intId(), code: 'DE-' + (1000 + i),
  name: pick(['客户名称', '证件号码', '手机号码', '联系地址', '客户性别', '出生日期', '客户等级', '订单金额', '订单编号', '下单时间', '支付方式', '退款金额', '商品编码', '商品名称', '销售数量', '渠道编码']),
  standard: pick(['STD-BASE-1001', 'STD-CODE-1002', 'STD-IDX-1003', 'STD-NAME-1004']),
  dataType: pick(['VARCHAR(64)', 'VARCHAR(18)', 'DECIMAL(16,2)', 'DATE', 'DATETIME', 'INT']),
  domain: pick(['客户域', '交易域', '商品域', '营销域']),
  binds: int(1, 20),
  status: pick(['已发布', '草稿', '已发布', '已停用']),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 60))
}))

export const valueDomains = genRows(16, i => ({
  id: intId(), code: 'VD-' + (100 + i),
  name: pick(['性别代码值域', '币种代码值域', '证件类型值域', '订单状态值域', '客户等级值域', '渠道类型值域', '行业分类值域', '计量单位值域']),
  type: pick(['枚举型', '区间型', '公式型']),
  values: int(2, 50),
  boundElements: int(1, 10),
  status: pick(['生效', '草稿', '已废止']),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 60))
}))

export const refStandards = genRows(15, i => ({
  id: intId(), name: 'GB/T ' + int(20000, 39999) + ' ' + pick(['个人信息交换格式', '行政区划代码', '货币代码', '日期时间表示法', '性别表示法', '组织机构代码', '全国主要产品分类']),
  category: pick(['国家标准', '行业标准', '地方标准']),
  refs: int(1, 15),
  mapped: pick(['已映射', '映射中', '未映射']),
  status: pick(['生效', '生效', '草稿']),
  updatedAt: dateStr(int(0, 90))
}))

export const resources = genRows(20, i => ({
  id: intId(), name: pick(TABLE_NAMES),
  type: pick(['数据表', '数据表', 'API服务', '指标', '报表', '标签']),
  domain: pick(['客户域', '交易域', '商品域', '营销域', '供应链域', '财务域']),
  database: pick(DB_NAMES),
  owner: pick(depts),
  qualityScore: +(85 + rnd() * 15).toFixed(1),
  hotScore: int(1, 100),
  refs: int(1, 60),
  standardsBound: int(0, 15),
  status: pick(['已注册', '已注册', '待审核', '已下线']),
  registeredAt: dateStr(int(10, 300))
}))

export const documents = genRows(24, i => ({
  id: intId(), title: pick(['数据中台操作手册', '数据标准管理办法', '质量管控实施细则', '数据接入实施规范', 'API服务接入指南', '指标口径说明', '客户主题域设计文档', '营销活动分析方案', '平台部署手册', '权限管理规范', '血缘分析使用指引', '报表开发规范']) + (i < 12 ? '' : '-V' + int(2, 4)),
  type: pick(['Word', 'Excel', 'PPT', 'PDF', 'Markdown', '思维笔记']),
  folder: pick(['操作手册', '实施方案', '规范制度', '培训资料', '会议纪要']),
  sizeKB: int(50, 20000),
  pages: int(3, 120),
  owner: pick(owners),
  shared: pick(['私有', '共享', '共享']),
  views: int(10, 5000),
  updatedAt: dateStr(int(0, 60))
}))

export const knowledgeBases = genRows(10, i => ({
  id: intId(), name: pick(['平台操作知识库', '数据标准知识库', '业务规则知识库', 'API文档知识库', '行业方案知识库', '故障处理知识库']) + '-' + (i + 1),
  docs: int(10, 200),
  chunks: int(100, 5000),
  embeddedModel: pick(['text-embedding-v3', 'bge-large-zh', 'text-embedding-v2']),
  status: pick(['已构建', '已构建', '构建中', '待构建']),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 30))
}))

export const agents = genRows(12, i => ({
  id: intId(), name: pick(['数据查询助手', '指标问答助手', '质量分析助手', 'SQL开发助手', '标准咨询助手', '报表解读助手', '智能客服', '经营分析助手']) + '-' + (i + 1),
  model: pick(['DeepSeek-V3', 'Qwen-Max', 'GLM-4-Plus', 'GPT-4o', '本地Llama3-70B']),
  knowledge: int(1, 5),
  tools: int(0, 6),
  callsMonth: int(500, 80000),
  satisfaction: +(88 + rnd() * 12).toFixed(1),
  status: pick(['已发布', '调试中', '已发布', '已下线']),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 20))
}))

export const aiModels = genRows(10, i => ({
  id: intId(), name: pick(['DeepSeek-V3', 'Qwen-Max', 'GLM-4-Plus', 'GPT-4o', 'bge-large-zh', 'text-embedding-v3', '本地Llama3-70B', 'Doubao-Pro']),
  provider: pick(['本地部署', '本地部署', 'DeepSeek开放平台', '阿里云百炼', '智谱AI', 'OpenAI', '火山方舟']),
  type: pick(['对话模型', '对话模型', 'Embedding', '多模态']),
  context: pick(['32K', '64K', '128K', '256K']),
  qpsLimit: int(5, 100),
  status: pick(['运行中', '运行中', '运行中', '已停用']),
  isDefault: i === 0,
  owner: pick(owners),
  updatedAt: dateStr(int(0, 15))
}))

export const sysUsers = genRows(20, i => ({
  id: intId(), username: 'user_' + (i + 1),
  nickname: pick(owners),
  dept: pick(depts),
  email: 'user' + (i + 1) + '@company.com',
  phone: '13' + int(100000000, 999999999),
  roles: [pick(['管理员', '数据治理员', '数据开发', '数据分析', '普通用户'])],
  status: pick(['正常', '正常', '正常', '已锁定']),
  lastLogin: dateStr(int(0, 7)),
  createdAt: dateStr(int(30, 400))
}))

export const sysRoles = genRows(10, i => ({
  id: intId(), code: pick(['admin', 'governor', 'developer', 'analyst', 'auditor', 'viewer']) + (i > 5 ? '_' + i : ''),
  name: pick(['超级管理员', '数据治理员', '数据开发工程师', '数据分析师', '审计员', '普通用户', '项目经理', '运营人员']),
  users: int(1, 30),
  menus: int(10, 80),
  desc: pick(['平台全部权限', '标准与质量模块权限', '开发模块读写权限', '报表与服务查看权限', '审计日志查看权限', '只读权限']),
  status: pick(['启用', '启用', '停用']),
  updatedAt: dateStr(int(0, 60))
}))

export const sysProjects = genRows(10, i => ({
  id: intId(), name: pick(['数据中台一期', '数据中台二期', '客户画像专题', '营销分析专题', '供应链优化专题', '财务数据治理', '实时数仓建设']) + (i > 6 ? '-扩展' : ''),
  code: 'PRJ-' + (100 + i),
  owner: pick(owners),
  members: int(3, 20),
  resources: int(10, 200),
  progress: int(20, 100),
  status: pick(['进行中', '进行中', '已验收', '已归档']),
  deadline: dateStr(-int(10, 200), false)
}))

export const reportCharts = genRows(20, i => ({
  id: intId(), name: pick(['日销售趋势', '渠道转化漏斗', '会员等级分布', '品类销售占比', '门店业绩排名', '退款原因分析', '月度GMV对比', '新老客占比', '区域销售地图', '活跃度趋势']) + '-' + (i + 1),
  type: pick(['折线图', '柱状图', '饼图', '漏斗图', '地图', '指标卡', '表格', '散点图']),
  source: pick(DB_NAMES),
  dataset: pick(['dws_sale_day', 'dws_member_stat', 'ads_channel_stat', 'dws_region_stat']),
  dashboard: pick(['经营驾驶舱', '销售专题看板', '会员分析看板', '商品分析看板']),
  owner: pick(owners),
  status: pick(['已发布', '编辑中', '已发布', '已下线']),
  updatedAt: dateStr(int(0, 30))
}))

export const dashboards = genRows(10, i => ({
  id: intId(), name: pick(['经营驾驶舱', '销售专题看板', '会员分析看板', '商品分析看板', '供应链看板', '财务总览看板', '营销活动看板', '实时监控大屏']) + (i > 7 ? '-V2' : ''),
  charts: int(4, 16),
  viewers: int(10, 500),
  visitsWeek: int(50, 5000),
  status: pick(['已发布', '已发布', '编辑中', '已下线']),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 20))
}))

export const labExperiments = genRows(14, i => ({
  id: intId(), name: pick(['公共数据授权运营实验', '政务数据开放评估', '电商用户画像实验', '零售销量预测实验', '智能推荐仿真', '客户流失预测', '价格敏感度分析']) + '-' + (i + 1),
  scene: pick(['政务领域', '电商零售']),
  dataset: pick(['public_gov_sample', 'ecommerce_trade_sample', 'retail_member_sample']),
  params: int(4, 12),
  rounds: int(1, 20),
  bestScore: +(70 + rnd() * 30).toFixed(1),
  status: pick(['运行中', '已完成', '已完成', '待运行', '已失败']),
  owner: pick(owners),
  updatedAt: dateStr(int(0, 15))
}))

export const notices = genRows(6, i => ({
  id: intId(),
  title: pick(['平台将于本周六 02:00 进行例行维护', '新版数据标准管理办法已发布', '质量管控模块新增 AI 规则推荐能力', '报表平台上线大屏模板中心', '数据服务网关升级至 v2.3', '智能体平台支持本地模型接入']),
  level: pick(['info', 'warning', 'success']),
  time: dateStr(int(0, 10))
}))

export const trend = {
  access: genTrend(30, 4200, 2600, v => Math.round(v)),
  calls: genTrend(30, 85000, 50000, v => Math.round(v)),
  quality: genTrend(30, 96, 4, v => +v.toFixed(1)),
  storage: genTrend(30, 680, 60, v => +v.toFixed(1))
}

export const moduleStats = [
  { key: 'access', name: '数据接入', value: 48, unit: '个接入任务', trend: 12 },
  { key: 'quality', name: '质量管控', value: 96.8, unit: '% 综合合格率', trend: 2.1 },
  { key: 'develop', name: '数据开发', value: 126, unit: '个加工任务', trend: 8 },
  { key: 'modeling', name: '数据建模', value: 12, unit: '个主题域模型', trend: 3 },
  { key: 'service', name: '数据服务', value: 86, unit: '个API服务', trend: 15 },
  { key: 'governance', name: '数据治理', value: 128, unit: '项标准资产', trend: 6 },
  { key: 'report', name: '报表平台', value: 42, unit: '个看板/图表', trend: 9 },
  { key: 'agent', name: '智能体平台', value: 12, unit: '个智能体应用', trend: 4 }
]

export const lineageData = {
  nodes: [
    { id: 'n1', name: 'ods_order_db.order_info', layer: 'ODS', x: 60, y: 120 },
    { id: 'n2', name: 'ods_order_db.order_detail', layer: 'ODS', x: 60, y: 260 },
    { id: 'n3', name: 'ods_member_db.user_info', layer: 'ODS', x: 60, y: 400 },
    { id: 'n4', name: 'dwd_trade_db.trade_flow', layer: 'DWD', x: 340, y: 190 },
    { id: 'n5', name: 'dwd_trade_db.refund_flow', layer: 'DWD', x: 340, y: 330 },
    { id: 'n6', name: 'dws_summary_db.sale_day', layer: 'DWS', x: 620, y: 150 },
    { id: 'n7', name: 'dws_summary_db.member_stat', layer: 'DWS', x: 620, y: 300 },
    { id: 'n8', name: 'ads_report_db.gmv_report', layer: 'ADS', x: 900, y: 180 },
    { id: 'n9', name: 'ads_report_db.member_profile', layer: 'ADS', x: 900, y: 330 },
    { id: 'n10', name: 'API /api/v1/stat/gmv', layer: 'API', x: 1160, y: 255 }
  ],
  edges: [
    { from: 'n1', to: 'n4' }, { from: 'n2', to: 'n4' }, { from: 'n3', to: 'n4' },
    { from: 'n2', to: 'n5' }, { from: 'n4', to: 'n6' }, { from: 'n4', to: 'n7' },
    { from: 'n5', to: 'n6' }, { from: 'n3', to: 'n7' }, { from: 'n6', to: 'n8' },
    { from: 'n7', to: 'n9' }, { from: 'n8', to: 'n10' }, { from: 'n9', to: 'n10' }
  ]
}

export const erModel = {
  entities: [
    { id: 'e1', name: '客户 customer', x: 80, y: 100, fields: ['id PK', 'name', 'phone', 'level', 'created_at'] },
    { id: 'e2', name: '订单 order', x: 400, y: 80, fields: ['id PK', 'customer_id FK', 'amount', 'status', 'pay_time'] },
    { id: 'e3', name: '订单明细 order_item', x: 400, y: 300, fields: ['id PK', 'order_id FK', 'sku_id FK', 'qty', 'price'] },
    { id: 'e4', name: '商品 product', x: 720, y: 300, fields: ['id PK', 'sku_code', 'name', 'category_id FK', 'price'] },
    { id: 'e5', name: '商品类目 category', x: 720, y: 80, fields: ['id PK', 'name', 'parent_id'] },
    { id: 'e6', name: '退款 refund', x: 720, y: 480, fields: ['id PK', 'order_id FK', 'amount', 'reason'] }
  ],
  relations: [
    { from: 'e1', to: 'e2', label: '1:N' }, { from: 'e2', to: 'e3', label: '1:N' },
    { from: 'e4', to: 'e3', label: '1:N' }, { from: 'e5', to: 'e4', label: '1:N' },
    { from: 'e2', to: 'e6', label: '1:N' }
  ]
}

export { owners, depts }
