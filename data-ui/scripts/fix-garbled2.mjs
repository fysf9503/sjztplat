import { readFileSync, writeFileSync } from 'node:fs'

// [file, lineNo(1-based, 0 = whole file), from, to, expectedCount]
const FIX = [
  // ===== system/Users.vue =====
  ['src/views/system/Users.vue', 0, 'placeholder="搜索用户名</ 姓名 / 邮箱"', 'placeholder="搜索用户名 / 姓名 / 邮箱"', 1],
  ['src/views/system/Users.vue', 0, 'label="最后登�"', 'label="最后登录"', 1],
  ['src/views/system/Users.vue', 0, "'数据治理�'", "'数据治理员'", 2],
  ['src/views/system/Users.vue', 0, "'数据分析�'", "'数据分析师'", 2],
  ['src/views/system/Users.vue', 0, "'审计�'", "'审计员'", 2],

  // ===== system/Services.vue =====
  ['src/views/system/Services.vue', 0, 'dp-stat-unit">�</span>', 'dp-stat-unit">个</span>', 3],
  ['src/views/system/Services.vue', 0, '健康� {{', '健康率 {{', 1],
  ['src/views/system/Services.vue', 0, "'运维保障�'", "'运维保障部'", 3],
  ['src/views/system/Services.vue', 0, "'数据治理�'", "'数据治理部'", 2],
  ['src/views/system/Services.vue', 0, "'AI应用�'", "'AI应用部'", 2],

  // ===== system/Projects.vue =====
  ['src/views/system/Projects.vue', 0, 'dp-stat-unit">�</span>', 'dp-stat-unit">个</span>', 4],
  ['src/views/system/Projects.vue', 0, 'label="成员�:"', 'label="成员数"', 1],
  ['src/views/system/Projects.vue', 0, 'label="资源�:"', 'label="资源数"', 1],
  ['src/views/system/Projects.vue', 0, '如：数据中台一�', '如：数据中台一期', 1],
  ['src/views/system/Projects.vue', 0, '项目�<strong>', '项目：<strong>', 1],
  ['src/views/system/Projects.vue', 0, "role: '数据分析�', dept: '数据分析�'", "role: '数据分析师', dept: '数据分析部'", 1],
  ['src/views/system/Projects.vue', 0, 'label="数据分析�" value="数据分析�:"', 'label="数据分析师" value="数据分析师"', 1],
  ['src/views/system/Projects.vue', 0, 'label="测试工程�:" value="测试工程�:"', 'label="测试工程师" value="测试工程师"', 1],
  ['src/views/system/Projects.vue', 0, 'label="观察�:" value="观察�:"', 'label="观察员" value="观察员"', 1],
  ['src/views/system/Projects.vue', 0, "'数据分析�'", "'数据分析师'", 1],
  ['src/views/system/Projects.vue', 0, "'测试工程�'", "'测试工程师'", 1],
  ['src/views/system/Projects.vue', 0, "'观察�'", "'观察员'", 1],
  ['src/views/system/Projects.vue', 0, "dept: '数据治理�'", "dept: '数据治理部'", 1],

  // ===== system/Models.vue =====
  ['src/views/system/Models.vue', 0, 'dp-stat-unit">�</span>', 'dp-stat-unit">个</span>', 4],
  ['src/views/system/Models.vue', 0, '可用� {{', '可用率 {{', 1],

  // ===== system/Knowledge.vue =====
  ['src/views/system/Knowledge.vue', 0, '知识库管�</h1>', '知识库管理</h1>', 1],
  ['src/views/system/Knowledge.vue', 0, 'dp-stat-unit">�</span>', 'dp-stat-unit">个</span>', 4],

  // ===== system/License.vue =====
  ['src/views/system/License.vue', 0, 'label="有效�:"', 'label="有效期"', 1],
  ['src/views/system/License.vue', 0, '{{ licenseInfo.startDate }} � {{ licenseInfo.endDate }}', '{{ licenseInfo.startDate }} 至 {{ licenseInfo.endDate }}', 1],
  ['src/views/system/License.vue', 0, '剩余 {{ daysRemaining }} �', '剩余 {{ daysRemaining }} 天', 2],
  ['src/views/system/License.vue', 0, 'label="用户数限制:"', 'label="用户数限制"', 1],
  ['src/views/system/License.vue', 0, '{{ licenseInfo.userLimit }} �', '{{ licenseInfo.userLimit }} 人', 1],
  ['src/views/system/License.vue', 0, '<span class="usage-unit">�</span>', '<span class="usage-unit">个</span>', 3],
  ['src/views/system/License.vue', 0, '重新激活�"', '重新激活。"', 1],
  ['src/views/system/License.vue', 0, 'label="授权�:"', 'label="授权码"', 1],
  ['src/views/system/License.vue', 0, '将授权文件拖到此处，�<em>', '将授权文件拖到此处，或<em>', 1],
  ['src/views/system/License.vue', 0, '格式的授权文�</div>', '格式的授权文件</div>', 1],
  ['src/views/system/License.vue', 0, '>更新授权�</el-button>', '>更新授权码</el-button>', 1],
  ['src/views/system/License.vue', 0, '>查看机器�</el-button>', '>查看机器码</el-button>', 1],
  ['src/views/system/License.vue', 0, 'title="机器码信息:"', 'title="机器码信息"', 1],
  ['src/views/system/License.vue', 0, '授权文件�:"', '授权文件。"', 1],
  ['src/views/system/License.vue', 0, 'label="机器�:"', 'label="机器码"', 1],
  ['src/views/system/License.vue', 0, "'企业版（Enterprise�'", "'企业版（Enterprise）'", 1],
  ['src/views/system/License.vue', 0, "'数据开�'", "'数据开发'", 1],

  // ===== system/Roles.vue =====
  ['src/views/system/Roles.vue', 0, "该角色下�'${row.users} 个用户", '该角色下有 ${row.users} 个用户', 1],

  // ===== agent/Workbench.vue =====
  ['src/views/agent/Workbench.vue', 0, "'数据质量合格率是多少�'", "'数据质量合格率是多少？'", 1],
  ['src/views/agent/Workbench.vue', 0, "请如实告知�'", "请如实告知。'", 1],
  ['src/views/agent/Workbench.vue', 0, "操作手册-第三�'", "操作手册-第三章'", 1],
  ['src/views/agent/Workbench.vue', 0, '统计粒度：� �?月。', '统计粒度：自然月。', 1],
  ['src/views/agent/Workbench.vue', 0, 'quality_results �...', 'quality_results 表...', 1],
  ['src/views/agent/Workbench.vue', 0, "上升趋势�'", "上升趋势。'", 1],
  ['src/views/agent/Workbench.vue', 0, "87.5%�'", "87.5%。'", 1],

  // ===== agent/Apps.vue =====
  ['src/views/agent/Apps.vue', 127, 'font-size: 12px">� �</span>', 'font-size: 12px">次 / 分钟</span>', 1],
  ['src/views/agent/Apps.vue', 0, ".toFixed(1) + '�'", ".toFixed(1) + '万'", 1],

  // ===== agent/Planning.vue =====
  ['src/views/agent/Planning.vue', 114, 'font-size: 12px">�</span>', 'font-size: 12px">次</span>', 1],
  ['src/views/agent/Planning.vue', 131, 'font-size: 12px">�</span>', 'font-size: 12px">次</span>', 1],
  ['src/views/agent/Planning.vue', 135, 'font-size: 12px">�</span>', 'font-size: 12px">秒</span>', 1],
  ['src/views/agent/Planning.vue', 0, "检索相关文�'", "检索相关文档'", 1],

  // ===== agent/Square.vue =====
  ['src/views/agent/Square.vue', 0, "'SQL开发助�'", "'SQL开发助手'", 1],
  ['src/views/agent/Square.vue', 0, ".toFixed(1) + '�'", ".toFixed(1) + '万'", 1],

  // ===== lab/Ecommerce.vue =====
  ['src/views/lab/Ecommerce.vue', 54, 'font-size: 12px">�</span>', 'font-size: 12px">人</span>', 1],
  ['src/views/lab/Ecommerce.vue', 0, "label=\"性别比例 (�')\"", 'label="性别比例 (%)"', 1],
  ['src/views/lab/Ecommerce.vue', 0, '>年轻�</el-radio>', '>年轻型</el-radio>', 1],
  ['src/views/lab/Ecommerce.vue', 0, '>均衡�</el-radio>', '>均衡型</el-radio>', 1],
  ['src/views/lab/Ecommerce.vue', 0, '>成熟�</el-radio>', '>成熟型</el-radio>', 1],
  ['src/views/lab/Ecommerce.vue', 89, 'font-size: 12px">�</span>', 'font-size: 12px">个</span>', 1],
  ['src/views/lab/Ecommerce.vue', 95, 'font-size: 12px">�</span>', 'font-size: 12px">元</span>', 1],
  ['src/views/lab/Ecommerce.vue', 0, 'label="� 30 �:" value="30d"', 'label="近 30 天" value="30d"', 1],
  ['src/views/lab/Ecommerce.vue', 0, 'label="� 90 �:" value="90d"', 'label="近 90 天" value="90d"', 1],
  ['src/views/lab/Ecommerce.vue', 0, 'label="� 180 �:" value="180d"', 'label="近 180 天" value="180d"', 1],
  ['src/views/lab/Ecommerce.vue', 0, 'label="� 1 �:" value="1y"', 'label="近 1 年" value="1y"', 1],
  ['src/views/lab/Ecommerce.vue', 116, 'font-size: 12px">�</span>', 'font-size: 12px">单</span>', 1],
  ['src/views/lab/Ecommerce.vue', 147, '<span>�</span>', '<span>元</span>', 1],
  ['src/views/lab/Ecommerce.vue', 151, '<span>�</span>', '<span>单</span>', 1],
  ['src/views/lab/Ecommerce.vue', 0, '用户数<//div>', '用户数</div>', 1],
  ['src/views/lab/Ecommerce.vue', 155, '<span>�</span>', '<span>人</span>', 1],
  ['src/views/lab/Ecommerce.vue', 159, '<span>�</span>', '<span>元</span>', 1],
  ['src/views/lab/Ecommerce.vue', 0, ".toFixed(2) + '�'", ".toFixed(2) + '亿'", 1],
  ['src/views/lab/Ecommerce.vue', 0, ".toFixed(1) + '�'", ".toFixed(1) + '万'", 2],
  ['src/views/lab/Ecommerce.vue', 0, "name: '销售额(�:'", "name: '销售额(元)'", 1],

  // ===== lab/Gov.vue =====
  ['src/views/lab/Gov.vue', 65, 'font-size: 12px">�</span>', 'font-size: 12px">人</span>', 1],
  ['src/views/lab/Gov.vue', 0, 'range-separator="�"', 'range-separator="至"', 1],
  ['src/views/lab/Gov.vue', 0, '>� {{ resultData.length }}', '>共 {{ resultData.length }}', 1],
  ['src/views/lab/Gov.vue', 127, '<span>�</span>', '<span>人</span>', 1],
  ['src/views/lab/Gov.vue', 131, '<span>�</span>', '<span>个</span>', 1],
  ['src/views/lab/Gov.vue', 135, '<span>�</span>', '<span>条</span>', 1],
  ['src/views/lab/Gov.vue', 0, "coreParam2: '事项类型�'", "coreParam2: '事项类型数'", 1],
  ['src/views/lab/Gov.vue', 0, "coreParam1: '年龄分段�'", "coreParam1: '年龄分段数'", 1],
  ['src/views/lab/Gov.vue', 0, "coreParam2Unit: '� �'", "coreParam2Unit: '人 / 户'", 1],
  ['src/views/lab/Gov.vue', 0, "coreParam1: '来源省份�'", "coreParam1: '来源省份数'", 1],
  ['src/views/lab/Gov.vue', 0, "coreParam1: '产业类别�'", "coreParam1: '产业类别数'", 1],
  ['src/views/lab/Gov.vue', 0, "coreParam2: 'GDP 增�'", "coreParam2: 'GDP 增速'", 1],
  ['src/views/lab/Gov.vue', 0, "coreParam1Unit: '�'", "coreParam1Unit: '个'", 4],
  ['src/views/lab/Gov.vue', 0, "coreParam2Unit: '�'", "coreParam2Unit: '个'", 1],
  ['src/views/lab/Gov.vue', 0, "label: '数�'", "label: '数值'", 1],
  ['src/views/lab/Gov.vue', 0, "'完整�'", "'完整性'", 1],
  ['src/views/lab/Gov.vue', 0, "'准确�'", "'准确性'", 1],
  ['src/views/lab/Gov.vue', 0, "'及时�'", "'及时性'", 1],
  ['src/views/lab/Gov.vue', 0, "'有效�'", "'有效性'", 1],
  ['src/views/lab/Gov.vue', 0, ".toFixed(1) + '�'", ".toFixed(1) + '万'", 1],

  // ===== lab/Overview.vue =====
  ['src/views/lab/Overview.vue', 0, 'label="场景�:"', 'label="场景数"', 1],
  ['src/views/lab/Overview.vue', 0, '<span class="dp-card-extra">� 30 �</span>', '<span class="dp-card-extra">近 30 天</span>', 1],
  ['src/views/lab/Overview.vue', 0, 'value="7d">� 7 �</el-radio-button>', 'value="7d">近 7 天</el-radio-button>', 1],
  ['src/views/lab/Overview.vue', 0, 'value="30d">� 30 �</el-radio-button>', 'value="30d">近 30 天</el-radio-button>', 1],
  ['src/views/lab/Overview.vue', 0, 'value="90d">� 90 �</el-radio-button>', 'value="90d">近 90 天</el-radio-button>', 1],
  ['src/views/lab/Overview.vue', 0, '最近实�<span class="dp-card-extra">最� 10 �</span>', '最近实验<span class="dp-card-extra">最近 10 条</span>', 1],
  ['src/views/lab/Overview.vue', 0, 'label="最佳得�:"', 'label="最佳得分"', 1],
  ['src/views/lab/Overview.vue', 0, '<small>�</small>', '<small>分</small>', 1],
  ['src/views/lab/Overview.vue', 0, "'{b}: {c} � ({d}%)'", "'{b}: {c} 次 ({d}%)'", 1],

  // ===== lab/ModelConfig.vue =====
  ['src/views/lab/ModelConfig.vue', 147, 'font-size: 12px">� �</span>', 'font-size: 12px">次 / 秒</span>', 1],
  ['src/views/lab/ModelConfig.vue', 0, "介绍一下你自己�'", "介绍一下你自己。'", 1],

  // ===== lab/Datasources.vue =====
  ['src/views/lab/Datasources.vue', 0, '连�...', '连接...', 1],

  // ===== search/Model.vue =====
  ['src/views/search/Model.vue', 0, "{{ row.qpsLimit }} / �'", '{{ row.qpsLimit }} / 秒', 1],
  ['src/views/search/Model.vue', 0, "调用参数�'", "调用参数。'", 1],

  // ===== search/Search.vue =====
  ['src/views/search/Search.vue', 0, '规则、服� API', '规则、服务 API', 1],
  ['src/views/search/Search.vue', 0, '「{{ lastKeyword }}�</span>', '「{{ lastKeyword }}」</span>', 1],
  ['src/views/search/Search.vue', 0, "多粒度结果�'", "多粒度结果。'", 1],
  ['src/views/search/Search.vue', 0, "'数据�'", "'数据表'", 2],
  ['src/views/search/Search.vue', 0, "['客户�', '交易�', '商品�', '营销�', '供应链域', '财务�']", "['客户域', '交易域', '商品域', '营销域', '供应链域', '财务域']", 1],
  ['src/views/search/Search.vue', 0, "'唯一性校�<order_no'", "'唯一性校验-order_no'", 1],
  ['src/views/search/Search.vue', 0, "'一致性校�<refund_amount'", "'一致性校验-refund_amount'", 1],

  // ===== search/Business.vue =====
  ['src/views/search/Business.vue', 0, '复购率�"', '复购率等"', 1],
  ['src/views/search/Business.vue', 0, '业务�</span>', '业务域</span>', 1],
  ['src/views/search/Business.vue', 0, 'label="� 7 �:" value="7d"', 'label="近 7 天" value="7d"', 1],
  ['src/views/search/Business.vue', 0, 'label="� 30 �:" value="30d"', 'label="近 30 天" value="30d"', 1],
  ['src/views/search/Business.vue', 0, 'label="� 90 �:" value="90d"', 'label="近 90 天" value="90d"', 1],
  ['src/views/search/Business.vue', 0, '「{{ lastKeyword }}�</span>', '「{{ lastKeyword }}」</span>', 1],
  ['src/views/search/Business.vue', 0, '当前�</div>', '当前值</div>', 1],
  ['src/views/search/Business.vue', 0, 'label="业务�"', 'label="业务域"', 1],
  ['src/views/search/Business.vue', 0, 'label="当前�:"', 'label="当前值"', 1],
  ['src/views/search/Business.vue', 0, '趋势图（� 30 天）', '趋势图（近 30 天）', 1],
  ['src/views/search/Business.vue', 0, "结果预览�'", "结果预览。'", 1],
  ['src/views/search/Business.vue', 0, "['客户�', '交易�', '商品�', '营销�', '供应链域', '财务�']", "['客户域', '交易域', '商品域', '营销域', '供应链域', '财务域']", 1],
  ['src/views/search/Business.vue', 0, "'GMV（商品交易总额�'", "'GMV（商品交易总额）'", 1],
  ['src/views/search/Business.vue', 223, "unit: '�'", "unit: '单'", 1],
  ['src/views/search/Business.vue', 0, "'客户�', unit: '�'", "'客户数', unit: '人'", 1],
  ['src/views/search/Business.vue', 225, "unit: '�'", "unit: '元'", 1],
  ['src/views/search/Business.vue', 0, '活跃会员�</ 总会员数', '活跃会员数 / 总会员数', 1],
  ['src/views/search/Business.vue', 233, "unit: '�'", "unit: '个'", 1],
  ['src/views/search/Business.vue', 0, "'销售分析主�', unit: '�'", "'销售分析主题', unit: '个'", 1],
  ['src/views/search/Business.vue', 235, "unit: '�'", "unit: '个'", 1],
  ['src/views/search/Business.vue', 236, "unit: '�'", "unit: '个'", 1],
  ['src/views/search/Business.vue', 0, '效果追踪� ROI 分析', '效果追踪与 ROI 分析', 1],
  ['src/views/search/Business.vue', 240, "unit: '�'", "unit: '个'", 1],
  ['src/views/search/Business.vue', 241, "unit: '�'", "unit: '个'", 1],
  ['src/views/search/Business.vue', 242, "unit: '�'", "unit: '个'", 1],
  ['src/views/search/Business.vue', 246, "unit: '�'", "unit: '个'", 1],
  ['src/views/search/Business.vue', 247, "unit: '�'", "unit: '个'", 1],
  ['src/views/search/Business.vue', 248, "unit: '�'", "unit: '个'", 1],

  // ===== docs/Mine.vue =====
  ['src/views/docs/Mine.vue', 0, '文件�</div>', '文件列表</div>', 1],
]

const byFile = new Map()
for (const [f, line, from, to, exp] of FIX) {
  if (!byFile.has(f)) byFile.set(f, [])
  byFile.get(f).push([line, from, to, exp])
}

let ok = 0, fail = 0
for (const [file, ops] of byFile) {
  const lines = readFileSync(file, 'utf8').split(/\r?\n/)
  const eol = readFileSync(file, 'utf8').includes('\r\n') ? '\r\n' : '\n'
  for (const [line, from, to, exp] of ops) {
    let count = 0
    if (line === 0) {
      for (let i = 0; i < lines.length; i++) {
        const before = lines[i]
        lines[i] = lines[i].split(from).join(to)
        count += before.split(from).length - 1
      }
    } else {
      const i = line - 1
      if (i >= lines.length || !lines[i].includes(from)) {
        console.log(`MISS(line) ${file}:${line} :: ${from}`)
        fail++
        continue
      }
      count = lines[i].split(from).length - 1
      lines[i] = lines[i].split(from).join(to)
    }
    if (count === exp) ok++
    else { console.log(`COUNT ${file} "${from}" expected ${exp} got ${count}`); fail++ }
  }
  writeFileSync(file, lines.join(eol), 'utf8')
}
console.log(`\nops ok=${ok} fail=${fail}`)
