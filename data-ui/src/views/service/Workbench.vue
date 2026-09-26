<template>
  <div class="dp-page workbench-page">
    <!-- 顶部工具栏 -->
    <div class="wb-topbar">
      <div class="wb-topbar-left">
        <el-button :icon="FolderOpened" size="default">项目：数据服务平台</el-button>
        <el-divider direction="vertical" />
        <el-button type="primary" :icon="Plus" @click="createNewApi">新建 API</el-button>
        <el-button :icon="Upload" @click="importApi">导入</el-button>
      </div>
      <div class="wb-topbar-center">
        <el-tag v-if="currentApi" :type="statusTagType(currentApi.status)" effect="plain">
          {{ currentApi.status }}
        </el-tag>
      </div>
      <div class="wb-topbar-right">
        <el-button :icon="View" @click="previewApi">预览</el-button>
        <el-button :icon="DocumentChecked" @click="saveApi">保存</el-button>
        <el-button type="success" :icon="Promotion" @click="publishApi">发布</el-button>
      </div>
    </div>

    <div class="wb-body">
      <!-- 左侧：API目录树 -->
      <div class="wb-sidebar">
        <div class="wb-sidebar-header">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索 API"
            clearable
            size="small"
            :prefix-icon="Search"
          />
        </div>
        <div class="wb-tree">
          <div v-for="group in apiGroups" :key="group.id" class="tree-group">
            <div class="tree-group-title" @click="toggleGroup(group.id)">
              <el-icon class="group-arrow" :class="{ expanded: expandedGroups.includes(group.id) }">
                <CaretRight />
              </el-icon>
              <el-icon :size="14" :color="group.color"><component :is="group.icon" /></el-icon>
              <span>{{ group.name }}</span>
              <span class="group-count">{{ group.children.length }}</span>
            </div>
            <div v-show="expandedGroups.includes(group.id)" class="tree-children">
              <div
                v-for="api in group.children"
                :key="api.id"
                class="tree-item"
                :class="{ active: activeTabId === api.id }"
                @click="openApiTab(api)"
              >
                <span class="method-dot" :class="api.method.toLowerCase()">{{ api.method.charAt(0) }}</span>
                <span class="tree-item-name">{{ api.name }}</span>
                <span class="tree-item-status" :class="api.status === '已发布' ? 'published' : 'draft'"></span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间：编辑器区域 -->
      <div class="wb-editor">
        <!-- Tab 页签 -->
        <div class="wb-tabs">
          <div
            v-for="tab in openTabs"
            :key="tab.id"
            class="wb-tab"
            :class="{ active: activeTabId === tab.id, modified: tab.modified }"
            @click="switchTab(tab.id)"
          >
            <span class="tab-method" :class="tab.method.toLowerCase()">{{ tab.method }}</span>
            <span class="tab-name">{{ tab.name }}</span>
            <el-icon class="tab-close" @click.stop="closeTab(tab.id)"><Close /></el-icon>
          </div>
        </div>

        <!-- 编辑器内容 -->
        <div v-if="currentApi" class="wb-editor-content">
          <el-tabs v-model="editorTab" type="border-card" class="editor-tabs">
            <!-- 基本信息 -->
            <el-tab-pane label="基本信息" name="basic">
              <div class="editor-section">
                <div class="section-title">基础配置</div>
                <el-form :model="apiForm" label-width="120px" class="config-form">
                  <el-row :gutter="24">
                    <el-col :span="12">
                      <el-form-item label="API 名称">
                        <el-input v-model="apiForm.name" placeholder="请输入 API 名称" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="请求路径">
                        <el-input v-model="apiForm.path" prefix="/api/v1/" />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-row :gutter="24">
                    <el-col :span="12">
                      <el-form-item label="请求方法">
                        <el-radio-group v-model="apiForm.method">
                          <el-radio-button value="GET">GET</el-radio-button>
                          <el-radio-button value="POST">POST</el-radio-button>
                          <el-radio-button value="PUT">PUT</el-radio-button>
                          <el-radio-button value="DELETE">DELETE</el-radio-button>
                        </el-radio-group>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="协议类型">
                        <el-radio-group v-model="apiForm.protocol">
                          <el-radio-button value="REST">REST</el-radio-button>
                          <el-radio-button value="GraphQL">GraphQL</el-radio-button>
                        </el-radio-group>
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item label="功能描述">
                    <el-input v-model="apiForm.desc" type="textarea" :rows="3" placeholder="请输入 API 功能描述" />
                  </el-form-item>
                  <el-row :gutter="24">
                    <el-col :span="8">
                      <el-form-item label="负责人">
                        <el-select v-model="apiForm.owner" filterable style="width: 100%">
                          <el-option v-for="o in ownerOptions" :key="o" :label="o" :value="o" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="版本号">
                        <el-input v-model="apiForm.version" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="标签">
                        <el-select v-model="apiForm.tags" multiple filterable allow-create style="width: 100%" placeholder="添加标签">
                          <el-option label="数据查询" value="数据查询" />
                          <el-option label="统计分析" value="统计分析" />
                          <el-option label="数据写入" value="数据写入" />
                          <el-option label="AI服务" value="AI服务" />
                          <el-option label="报表导出" value="报表导出" />
                          <el-option label="核心接口" value="核心接口" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </el-form>
              </div>
            </el-tab-pane>

            <!-- 请求参数 -->
            <el-tab-pane label="请求参数" name="params">
              <div class="editor-section">
                <div class="section-header">
                  <div class="section-title">参数配置</div>
                  <div class="section-actions">
                    <el-button type="primary" :icon="Plus" size="small" @click="addParam('query')">添加 Query 参数</el-button>
                    <el-button type="primary" :icon="Plus" size="small" @click="addParam('body')">添加 Body 参数</el-button>
                    <el-button :icon="Upload" size="small">导入参数</el-button>
                  </div>
                </div>

                <el-tabs v-model="paramSubTab" size="default">
                  <el-tab-pane label="Query 参数" name="query">
                    <div class="param-table-wrap">
                      <el-table :data="queryParams" border size="default">
                        <el-table-column prop="name" label="参数名" width="150">
                          <template #default="{ row }">
                            <el-input v-model="row.name" size="small" placeholder="参数名" />
                          </template>
                        </el-table-column>
                        <el-table-column prop="type" label="类型" width="110">
                          <template #default="{ row }">
                            <el-select v-model="row.type" size="small" style="width: 100%">
                              <el-option label="string" value="string" />
                              <el-option label="number" value="number" />
                              <el-option label="boolean" value="boolean" />
                              <el-option label="integer" value="integer" />
                              <el-option label="array" value="array" />
                            </el-select>
                          </template>
                        </el-table-column>
                        <el-table-column prop="required" label="必填" width="70" align="center">
                          <template #default="{ row }">
                            <el-switch v-model="row.required" size="small" />
                          </template>
                        </el-table-column>
                        <el-table-column prop="default" label="默认值" width="130">
                          <template #default="{ row }">
                            <el-input v-model="row.default" size="small" placeholder="默认值" />
                          </template>
                        </el-table-column>
                        <el-table-column prop="desc" label="参数说明">
                          <template #default="{ row }">
                            <el-input v-model="row.desc" size="small" placeholder="参数说明" />
                          </template>
                        </el-table-column>
                        <el-table-column label="操作" width="80" fixed="right" align="center">
                          <template #default="{ $index }">
                            <el-button link type="danger" size="small" @click="removeParam('query', $index)">删除</el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                    </div>
                  </el-tab-pane>

                  <el-tab-pane label="Body 参数" name="body">
                    <div class="param-table-wrap">
                      <el-table :data="bodyParams" border size="default">
                        <el-table-column prop="name" label="参数名" width="150">
                          <template #default="{ row }">
                            <el-input v-model="row.name" size="small" placeholder="参数名" />
                          </template>
                        </el-table-column>
                        <el-table-column prop="type" label="类型" width="110">
                          <template #default="{ row }">
                            <el-select v-model="row.type" size="small" style="width: 100%">
                              <el-option label="string" value="string" />
                              <el-option label="number" value="number" />
                              <el-option label="boolean" value="boolean" />
                              <el-option label="object" value="object" />
                              <el-option label="array" value="array" />
                            </el-select>
                          </template>
                        </el-table-column>
                        <el-table-column prop="required" label="必填" width="70" align="center">
                          <template #default="{ row }">
                            <el-switch v-model="row.required" size="small" />
                          </template>
                        </el-table-column>
                        <el-table-column prop="desc" label="参数说明">
                          <template #default="{ row }">
                            <el-input v-model="row.desc" size="small" placeholder="参数说明" />
                          </template>
                        </el-table-column>
                        <el-table-column label="操作" width="80" fixed="right" align="center">
                          <template #default="{ $index }">
                            <el-button link type="danger" size="small" @click="removeParam('body', $index)">删除</el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                    </div>
                  </el-tab-pane>

                  <el-tab-pane label="请求头" name="header">
                    <div class="param-table-wrap">
                      <el-table :data="headerParams" border size="default">
                        <el-table-column prop="name" label="Header 名称" width="180">
                          <template #default="{ row }">
                            <el-input v-model="row.name" size="small" placeholder="Header 名称" />
                          </template>
                        </el-table-column>
                        <el-table-column prop="required" label="必填" width="70" align="center">
                          <template #default="{ row }">
                            <el-switch v-model="row.required" size="small" />
                          </template>
                        </el-table-column>
                        <el-table-column prop="default" label="默认值" width="200">
                          <template #default="{ row }">
                            <el-input v-model="row.default" size="small" placeholder="默认值" />
                          </template>
                        </el-table-column>
                        <el-table-column prop="desc" label="说明">
                          <template #default="{ row }">
                            <el-input v-model="row.desc" size="small" placeholder="说明" />
                          </template>
                        </el-table-column>
                        <el-table-column label="操作" width="80" fixed="right" align="center">
                          <template #default="{ $index }">
                            <el-button link type="danger" size="small" @click="removeParam('header', $index)">删除</el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                    </div>
                  </el-tab-pane>
                </el-tabs>
              </div>
            </el-tab-pane>

            <!-- 响应配置 -->
            <el-tab-pane label="响应配置" name="response">
              <div class="editor-section">
                <div class="section-header">
                  <div class="section-title">响应结构</div>
                  <div class="section-actions">
                    <el-button :icon="MagicStick" size="small">AI 生成</el-button>
                    <el-button :icon="Upload" size="small">导入 Schema</el-button>
                  </div>
                </div>
                <div class="response-config">
                  <div class="schema-visual">
                    <div class="schema-node root">
                      <div class="schema-node-header">
                        <el-icon><Document /></el-icon>
                        <span>Response</span>
                        <el-tag size="small" type="info">object</el-tag>
                      </div>
                      <div class="schema-children">
                        <div v-for="field in responseSchema" :key="field.name" class="schema-field">
                          <div class="field-type" :class="field.type">{{ field.type }}</div>
                          <div class="field-name">{{ field.name }}</div>
                          <el-tag v-if="field.required" type="danger" size="small" effect="dark">必填</el-tag>
                          <div class="field-desc">{{ field.desc }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="response-example">
                    <div class="example-header">
                      <span>示例响应</span>
                      <div class="example-actions">
                        <el-button link type="primary" size="small" :icon="CopyDocument" @click="copyResponse">复制</el-button>
                        <el-button link type="primary" size="small" :icon="Refresh" @click="formatResponse">格式化</el-button>
                      </div>
                    </div>
                    <pre class="code-editor">{{ responseExample }}</pre>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <!-- 数据源配置 -->
            <el-tab-pane label="数据源配置" name="datasource">
              <div class="editor-section">
                <div class="section-title">数据源选择</div>
                <el-form :model="dsForm" label-width="120px" class="config-form">
                  <el-row :gutter="24">
                    <el-col :span="12">
                      <el-form-item label="数据源">
                        <el-select v-model="dsForm.source" style="width: 100%" placeholder="选择数据源">
                          <el-option v-for="d in dataSourceOptions" :key="d" :label="d" :value="d" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="查询类型">
                        <el-radio-group v-model="dsForm.queryType">
                          <el-radio-button value="sql">SQL 查询</el-radio-button>
                          <el-radio-button value="template">模板查询</el-radio-button>
                          <el-radio-button value="stored">存储过程</el-radio-button>
                        </el-radio-group>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </el-form>

                <div class="sql-editor-wrap">
                  <div class="sql-editor-header">
                    <span class="sql-title">SQL 语句</span>
                    <div class="sql-actions">
                      <el-button size="small" :icon="MagicStick" @click="aiOptimizeSql">AI 优化</el-button>
                      <el-button size="small" :icon="DocumentChecked" @click="formatSql">格式化</el-button>
                      <el-button size="small" :icon="VideoPlay" @click="previewSql">预编译</el-button>
                    </div>
                  </div>
                  <textarea v-model="dsForm.sql" class="sql-editor" spellcheck="false"></textarea>
                  <div class="sql-result">
                    <div class="sql-result-header">
                      <el-tag size="small" type="success">执行成功</el-tag>
                      <span class="result-info">返回 25 行，耗时 42ms</span>
                    </div>
                    <el-table :data="sqlPreviewData" border size="small" max-height="150">
                      <el-table-column prop="id" label="id" width="80" />
                      <el-table-column prop="name" label="name" width="120" />
                      <el-table-column prop="value" label="value" width="100" />
                      <el-table-column prop="status" label="status" width="100" />
                      <el-table-column prop="create_time" label="create_time" />
                    </el-table>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <!-- 高级配置 -->
            <el-tab-pane label="高级配置" name="advanced">
              <div class="editor-section">
                <div class="section-title">限流配置</div>
                <el-form :model="advancedForm" label-width="140px" class="config-form">
                  <el-row :gutter="24">
                    <el-col :span="8">
                      <el-form-item label="QPS 限制">
                        <el-input-number v-model="advancedForm.qps" :min="1" :max="10000" />
                        <span class="form-unit">次/秒</span>
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="单应用日限额">
                        <el-input-number v-model="advancedForm.dailyLimit" :min="0" :max="10000000" />
                        <span class="form-unit">次/天</span>
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="超时时间">
                        <el-input-number v-model="advancedForm.timeout" :min="100" :max="60000" :step="100" />
                        <span class="form-unit">毫秒</span>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </el-form>

                <div class="section-title" style="margin-top: 24px">缓存配置</div>
                <el-form :model="advancedForm" label-width="140px" class="config-form">
                  <el-row :gutter="24">
                    <el-col :span="8">
                      <el-form-item label="是否开启缓存">
                        <el-switch v-model="advancedForm.cacheEnabled" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="缓存过期时间">
                        <el-input-number v-model="advancedForm.cacheTTL" :min="1" :max="86400" :disabled="!advancedForm.cacheEnabled" />
                        <span class="form-unit">秒</span>
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="缓存策略">
                        <el-select v-model="advancedForm.cacheStrategy" style="width: 100%" :disabled="!advancedForm.cacheEnabled">
                          <el-option label="LRU" value="LRU" />
                          <el-option label="LFU" value="LFU" />
                          <el-option label="FIFO" value="FIFO" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </el-form>

                <div class="section-title" style="margin-top: 24px">熔断降级</div>
                <el-form :model="advancedForm" label-width="140px" class="config-form">
                  <el-row :gutter="24">
                    <el-col :span="8">
                      <el-form-item label="熔断错误率">
                        <el-input-number v-model="advancedForm.circuitErrorRate" :min="0" :max="100" />
                        <span class="form-unit">%</span>
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="熔断窗口">
                        <el-input-number v-model="advancedForm.circuitWindow" :min="1" :max="3600" />
                        <span class="form-unit">秒</span>
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="降级策略">
                        <el-select v-model="advancedForm.degradeStrategy" style="width: 100%">
                          <el-option label="返回默认值" value="default" />
                          <el-option label="返回缓存数据" value="cache" />
                          <el-option label="快速失败" value="failfast" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </el-form>

                <div class="section-title" style="margin-top: 24px">权限配置</div>
                <el-form :model="advancedForm" label-width="140px" class="config-form">
                  <el-row :gutter="24">
                    <el-col :span="8">
                      <el-form-item label="鉴权方式">
                        <el-select v-model="advancedForm.authType" style="width: 100%">
                          <el-option label="AppKey/Secret" value="appkey" />
                          <el-option label="Bearer Token" value="token" />
                          <el-option label="OAuth 2.0" value="oauth" />
                          <el-option label="无鉴权" value="none" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="IP 白名单">
                        <el-switch v-model="advancedForm.ipWhitelistEnabled" />
                      </el-form-item>
                    </el-col>
                    <el-col :span="8">
                      <el-form-item label="访问级别">
                        <el-select v-model="advancedForm.accessLevel" style="width: 100%">
                          <el-option label="公开" value="public" />
                          <el-option label="内部" value="internal" />
                          <el-option label="机密" value="confidential" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-form-item v-if="advancedForm.ipWhitelistEnabled" label="白名单 IP">
                    <el-input
                      v-model="advancedForm.ipWhitelist"
                      type="textarea"
                      :rows="3"
                      placeholder="每行一个 IP，支持通配符，如：192.168.1.*"
                    />
                  </el-form-item>
                </el-form>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <div v-else class="wb-editor-empty">
          <el-empty description="请从左侧选择或新建 API 开始编辑">
            <el-button type="primary" :icon="Plus" @click="createNewApi">新建 API</el-button>
          </el-empty>
        </div>
      </div>

      <!-- 右侧：调试面板 -->
      <div class="wb-debug">
        <div class="debug-header">
          <span class="debug-title">
            <el-icon><Cpu /></el-icon>
            API 调试
          </span>
          <el-button type="primary" :icon="VideoPlay" :loading="debugLoading" @click="sendDebugRequest">
            发送请求
          </el-button>
        </div>

        <div class="debug-content">
          <!-- 请求 URL -->
          <div class="debug-section">
            <div class="debug-url-bar">
              <span class="method-badge" :class="currentApi?.method?.toLowerCase() || 'get'">
                {{ currentApi?.method || 'GET' }}
              </span>
              <div class="debug-url dp-mono">{{ debugUrl }}</div>
            </div>
          </div>

          <!-- 调试参数 -->
          <el-tabs v-model="debugTab" size="small" class="debug-tabs">
            <el-tab-pane label="请求参数" name="params">
              <div class="debug-params">
                <div v-for="(p, i) in debugQueryParams" :key="i" class="debug-param-row">
                  <el-checkbox v-model="p.enabled" />
                  <el-input v-model="p.name" placeholder="参数名" size="small" style="width: 120px" />
                  <el-input v-model="p.value" placeholder="参数值" size="small" style="flex: 1" />
                  <el-button link type="danger" size="small" @click="removeDebugParam(i)">
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>
                <el-button link type="primary" size="small" :icon="Plus" @click="addDebugParam">添加参数</el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="请求头" name="headers">
              <div class="debug-params">
                <div v-for="(h, i) in debugHeaders" :key="i" class="debug-param-row">
                  <el-checkbox v-model="h.enabled" />
                  <el-input v-model="h.name" placeholder="Header名" size="small" style="width: 140px" />
                  <el-input v-model="h.value" placeholder="Header值" size="small" style="flex: 1" />
                  <el-button link type="danger" size="small" @click="removeDebugHeader(i)">
                    <el-icon><Close /></el-icon>
                  </el-button>
                </div>
                <el-button link type="primary" size="small" :icon="Plus" @click="addDebugHeader">添加 Header</el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="Body" name="body">
              <div v-if="currentApi?.method === 'GET'" class="debug-empty-body">
                <el-empty description="GET 请求不支持 Body 参数" :image-size="80" />
              </div>
              <textarea v-else v-model="debugBody" class="debug-body-editor" spellcheck="false" placeholder='{"key": "value"}'></textarea>
            </el-tab-pane>
          </el-tabs>

          <!-- 响应结果 -->
          <div class="debug-response">
            <div class="response-header-bar">
              <span class="response-title">响应结果</span>
              <div v-if="debugResponse" class="response-meta">
                <el-tag :type="debugResponse.status === 200 ? 'success' : 'danger'" size="small">
                  {{ debugResponse.status }}
                </el-tag>
                <span class="meta-item">耗时 {{ debugResponse.time }}ms</span>
                <span class="meta-item">{{ debugResponse.size }}</span>
              </div>
            </div>
            <div v-if="!debugResponse" class="debug-empty-response">
              <el-empty description="点击发送按钮查看响应结果" :image-size="80" />
            </div>
            <div v-else class="response-body">
              <el-tabs v-model="responseSubTab" size="small">
                <el-tab-pane label="响应数据" name="data">
                  <pre class="response-code">{{ debugResponse.data }}</pre>
                </el-tab-pane>
                <el-tab-pane label="响应头" name="headers">
                  <el-table :data="debugResponse.headers" border size="small">
                    <el-table-column prop="name" label="名称" width="180" />
                    <el-table-column prop="value" label="值" />
                  </el-table>
                </el-tab-pane>
              </el-tabs>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { services, dataSources, owners } from '@/mock'
import { CopyDocument, DocumentChecked, FolderOpened, MagicStick, Plus, Promotion, Refresh, Search, Upload, VideoPlay, View } from '@element-plus/icons-vue'

// 状态
const searchKeyword = ref('')
const activeTabId = ref(null)
const editorTab = ref('basic')
const paramSubTab = ref('query')
const debugTab = ref('params')
const responseSubTab = ref('data')
const debugLoading = ref(false)
const debugResponse = ref(null)

// 目录树
const expandedGroups = ref(['query', 'write', 'stat'])
const apiGroups = ref([])

// 打开的标签
const openTabs = ref([])

// 当前 API 表单
const apiForm = reactive({
  name: '',
  path: '',
  method: 'GET',
  protocol: 'REST',
  desc: '',
  owner: '',
  version: '1.0.0',
  tags: []
})

// 参数
const queryParams = ref([])
const bodyParams = ref([])
const headerParams = ref([])

// 响应
const responseSchema = ref([
  { name: 'code', type: 'number', required: true, desc: '响应码，0表示成功' },
  { name: 'message', type: 'string', required: true, desc: '响应消息' },
  { name: 'requestId', type: 'string', required: false, desc: '请求追踪ID' },
  { name: 'data', type: 'object', required: false, desc: '响应数据' }
])

const responseExample = ref(JSON.stringify({
  code: 0,
  message: 'success',
  requestId: 'req_abc123',
  data: {
    total: 100,
    list: [{ id: 1, name: '示例数据' }]
  }
}, null, 2))

// 数据源
const dsForm = reactive({
  source: '',
  queryType: 'sql',
  sql: `SELECT
  id,
  name,
  status,
  create_time
FROM dwd_customer_db.customer_info
WHERE status = 'active'
  AND name LIKE CONCAT('%', :keyword, '%')
ORDER BY create_time DESC
LIMIT :offset, :limit;`
})

const sqlPreviewData = ref([
  { id: 1, name: '张三', value: 1500, status: 'active', create_time: '2024-01-15 10:30:00' },
  { id: 2, name: '李四', value: 2300, status: 'active', create_time: '2024-01-15 10:25:00' },
  { id: 3, name: '王五', value: 890, status: 'inactive', create_time: '2024-01-15 10:20:00' }
])

// 高级配置
const advancedForm = reactive({
  qps: 100,
  dailyLimit: 100000,
  timeout: 5000,
  cacheEnabled: true,
  cacheTTL: 300,
  cacheStrategy: 'LRU',
  circuitErrorRate: 30,
  circuitWindow: 60,
  degradeStrategy: 'default',
  authType: 'appkey',
  ipWhitelistEnabled: false,
  ipWhitelist: '',
  accessLevel: 'internal'
})

// 调试
const debugUrl = computed(() => {
  if (!currentApi.value) return 'https://api.example.com/api/v1/'
  return 'https://api.dataplatform.com' + currentApi.value.path
})

const debugQueryParams = ref([
  { enabled: true, name: 'page', value: '1' },
  { enabled: true, name: 'pageSize', value: '20' },
  { enabled: false, name: 'keyword', value: '' }
])

const debugHeaders = ref([
  { enabled: true, name: 'Content-Type', value: 'application/json' },
  { enabled: true, name: 'Authorization', value: 'Bearer xxxxxxxx' },
  { enabled: false, name: 'X-Request-Id', value: '' }
])

const debugBody = ref('{\n  "name": "test",\n  "type": 1\n}')

const dataSourceOptions = computed(() => dataSources.map(d => d.name))
const ownerOptions = owners

const currentApi = computed(() => {
  if (!activeTabId.value) return null
  return openTabs.value.find(t => t.id === activeTabId.value)
})

function statusTagType(status) {
  const map = { '已发布': 'success', '开发中': 'warning', '已下线': 'info' }
  return map[status] || 'info'
}

// 目录树操作
function toggleGroup(id) {
  const idx = expandedGroups.value.indexOf(id)
  if (idx > -1) {
    expandedGroups.value.splice(idx, 1)
  } else {
    expandedGroups.value.push(id)
  }
}

// Tab 操作
function openApiTab(api) {
  const existing = openTabs.value.find(t => t.id === api.id)
  if (!existing) {
    openTabs.value.push({ ...api, modified: false })
  }
  activeTabId.value = api.id
  loadApiForm(api)
}

function switchTab(id) {
  activeTabId.value = id
  const api = openTabs.value.find(t => t.id === id)
  if (api) loadApiForm(api)
}

function closeTab(id) {
  const idx = openTabs.value.findIndex(t => t.id === id)
  if (idx > -1) {
    const tab = openTabs.value[idx]
    if (tab.modified) {
      ElMessageBox.confirm('该 API 有未保存的更改，确定关闭吗？', '关闭确认', {
        type: 'warning'
      }).then(() => {
        openTabs.value.splice(idx, 1)
        if (activeTabId.value === id) {
          activeTabId.value = openTabs.value[idx]?.id || openTabs.value[idx - 1]?.id || null
        }
      }).catch(() => {})
    } else {
      openTabs.value.splice(idx, 1)
      if (activeTabId.value === id) {
        activeTabId.value = openTabs.value[idx]?.id || openTabs.value[idx - 1]?.id || null
      }
    }
  }
}

function loadApiForm(api) {
  Object.assign(apiForm, {
    name: api.name,
    path: api.path.replace('/api/v1/', ''),
    method: api.method,
    protocol: api.protocol,
    desc: api.desc || '',
    owner: api.owner,
    version: '1.0.' + Math.floor(Math.random() * 10),
    tags: api.tags || []
  })
}

// 新建 API
function createNewApi() {
  const newId = Date.now()
  const newApi = {
    id: newId,
    name: 'api_new_' + (openTabs.value.length + 1),
    path: '/api/v1/new/api',
    method: 'GET',
    protocol: 'REST',
    status: '开发中',
    owner: '张伟',
    desc: '',
    tags: [],
    modified: false
  }
  openTabs.value.push(newApi)
  activeTabId.value = newId
  loadApiForm(newApi)
  ElMessage.success('已创建新 API')
}

function importApi() {
  ElMessage.info('导入 API 功能')
}

function previewApi() {
  ElMessage.info('预览 API 文档')
}

function saveApi() {
  if (!currentApi.value) return
  const tab = openTabs.value.find(t => t.id === activeTabId.value)
  if (tab) {
    tab.modified = false
    tab.name = apiForm.name
  }
  ElMessage.success('保存成功')
}

function publishApi() {
  ElMessageBox.confirm('确定发布此 API 吗？发布后将对外提供服务。', '发布确认', {
    type: 'warning',
    confirmButtonText: '确认发布'
  }).then(() => {
    const tab = openTabs.value.find(t => t.id === activeTabId.value)
    if (tab) tab.status = '已发布'
    ElMessage.success('发布成功，约 1 分钟后生效')
  }).catch(() => {})
}

// 参数操作
function addParam(type) {
  const newParam = { name: 'param_' + Date.now(), type: 'string', required: false, default: '', desc: '' }
  if (type === 'query') {
    queryParams.value.push(newParam)
    paramSubTab.value = 'query'
  } else if (type === 'body') {
    bodyParams.value.push(newParam)
    paramSubTab.value = 'body'
  }
}

function removeParam(type, index) {
  if (type === 'query') {
    queryParams.value.splice(index, 1)
  } else if (type === 'body') {
    bodyParams.value.splice(index, 1)
  } else if (type === 'header') {
    headerParams.value.splice(index, 1)
  }
}

// 响应操作
function copyResponse() {
  navigator.clipboard.writeText(responseExample.value)
  ElMessage.success('已复制到剪贴板')
}

function formatResponse() {
  try {
    responseExample.value = JSON.stringify(JSON.parse(responseExample.value), null, 2)
    ElMessage.success('格式化成功')
  } catch {
    ElMessage.error('JSON 格式错误，无法格式化')
  }
}

// SQL 操作
function aiOptimizeSql() {
  ElMessage.success('AI 优化建议：建议添加索引，预计性能提升 40%')
}

function formatSql() {
  ElMessage.success('SQL 已格式化')
}

function previewSql() {
  ElMessage.success('预编译通过，参数绑定正常')
}

// 调试
function addDebugParam() {
  debugQueryParams.value.push({ enabled: true, name: '', value: '' })
}

function removeDebugParam(index) {
  debugQueryParams.value.splice(index, 1)
}

function addDebugHeader() {
  debugHeaders.value.push({ enabled: true, name: '', value: '' })
}

function removeDebugHeader(index) {
  debugHeaders.value.splice(index, 1)
}

function sendDebugRequest() {
  if (!currentApi.value) {
    return ElMessage.warning('请先选择一个 API')
  }
  debugLoading.value = true
  const startTime = Date.now()

  setTimeout(() => {
    debugLoading.value = false
    const time = Date.now() - startTime + Math.floor(Math.random() * 200)
    debugResponse.value = {
      status: 200,
      time,
      size: (Math.random() * 5 + 1).toFixed(2) + ' KB',
      data: JSON.stringify({
        code: 0,
        message: 'success',
        requestId: 'req_' + Math.random().toString(36).substring(2, 15),
        data: {
          total: 100,
          page: 1,
          pageSize: 20,
          list: Array.from({ length: 5 }, (_, i) => ({
            id: i + 1,
            name: '数据项_' + (i + 1),
            value: Math.floor(Math.random() * 10000),
            status: i % 2 === 0 ? 'active' : 'inactive',
            createTime: new Date().toISOString().slice(0, 19).replace('T', ' ')
          }))
        }
      }, null, 2),
      headers: [
        { name: 'Content-Type', value: 'application/json; charset=utf-8' },
        { name: 'X-Request-Id', value: 'req_' + Math.random().toString(36).substring(2, 10) },
        { name: 'X-RateLimit-Limit', value: '100' },
        { name: 'X-RateLimit-Remaining', value: '97' },
        { name: 'Server', value: 'DataPlatform-Gateway/2.3.0' }
      ]
    }
    ElMessage.success('请求成功')
  }, 600 + Math.random() * 400)
}

// 初始化
onMounted(() => {
  // 构建目录树
  const groups = [
    { id: 'query', name: '数据查询', icon: 'Search', color: '#00b42a', children: [] },
    { id: 'write', name: '数据写入', icon: 'Edit', color: '#ff7d00', children: [] },
    { id: 'stat', name: '统计分析', icon: 'DataLine', color: '#722ed1', children: [] },
    { id: 'ai', name: 'AI 服务', icon: 'MagicStick', color: '#f759ab', children: [] }
  ]

  services.forEach((s, i) => {
    const groupIdx = i % groups.length
    groups[groupIdx].children.push(s)
  })

  apiGroups.value = groups

  // 默认打开第一个 API
  if (services.length > 0) {
    openApiTab(services[0])
  }

  // 初始化参数
  queryParams.value = [
    { name: 'page', type: 'number', required: false, default: '1', desc: '页码' },
    { name: 'pageSize', type: 'number', required: false, default: '20', desc: '每页条数' },
    { name: 'keyword', type: 'string', required: false, default: '', desc: '搜索关键词' }
  ]

  bodyParams.value = [
    { name: 'name', type: 'string', required: true, desc: '名称' },
    { name: 'type', type: 'number', required: false, desc: '类型' }
  ]

  headerParams.value = [
    { name: 'Authorization', required: true, default: 'Bearer xxx', desc: '鉴权令牌' },
    { name: 'Content-Type', required: true, default: 'application/json', desc: '内容类型' }
  ]

  if (services.length > 0 && dataSources.length > 0) {
    dsForm.source = dataSources[0].name
  }
})
</script>

<style scoped>
.workbench-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
  padding: 0;
  overflow: hidden;
}

/* 顶部工具栏 */
.wb-topbar {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid var(--dp-border-light);
  padding: 10px 16px;
  flex-shrink: 0;
}

.wb-topbar-left,
.wb-topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wb-topbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

/* 主体布局 */
.wb-body {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
}

/* 左侧边栏 */
.wb-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid var(--dp-border-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.wb-sidebar-header {
  padding: 12px;
  border-bottom: 1px solid var(--dp-border-light);
  flex-shrink: 0;
}

.wb-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.tree-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-2);
  cursor: pointer;
  user-select: none;
}

.group-arrow {
  transition: transform 0.2s;
  font-size: 12px;
  color: var(--dp-text-4);
}

.group-arrow.expanded {
  transform: rotate(90deg);
}

.group-count {
  margin-left: auto;
  font-size: 11px;
  color: var(--dp-text-4);
  background: var(--dp-bg-page);
  padding: 1px 6px;
  border-radius: 10px;
}

.tree-children {
  padding-left: 12px;
}

.tree-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px 7px 18px;
  font-size: 12.5px;
  color: var(--dp-text-2);
  cursor: pointer;
  border-radius: 0 6px 6px 0;
  margin-right: 8px;
  transition: all 0.15s;
}

.tree-item:hover {
  background: var(--dp-bg-page);
}

.tree-item.active {
  background: var(--dp-primary-bg);
  color: var(--dp-primary);
  font-weight: 500;
}

.method-dot {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  font-family: Consolas, monospace;
  flex-shrink: 0;
}

.method-dot.g { background: var(--dp-success-light); color: var(--dp-success); }
.method-dot.p { background: var(--dp-primary-light); color: var(--dp-primary); }
.method-dot.u { background: var(--dp-warning-light); color: var(--dp-warning); }
.method-dot.d { background: var(--dp-danger-light); color: var(--dp-danger); }

.tree-item-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-item-status {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.tree-item-status.published {
  background: var(--dp-success);
}

.tree-item-status.draft {
  background: var(--dp-warning);
}

/* 中间编辑器 */
.wb-editor {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--dp-bg-page);
  overflow: hidden;
}

.wb-tabs {
  display: flex;
  background: #fff;
  border-bottom: 1px solid var(--dp-border-light);
  padding: 0 8px;
  flex-shrink: 0;
  overflow-x: auto;
}

.wb-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 12px;
  height: 38px;
  font-size: 12.5px;
  color: var(--dp-text-2);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  white-space: nowrap;
  transition: all 0.15s;
}

.wb-tab:hover {
  color: var(--dp-primary);
}

.wb-tab.active {
  color: var(--dp-primary);
  border-bottom-color: var(--dp-primary);
  font-weight: 500;
}

.wb-tab.modified .tab-name::after {
  content: '*';
  color: var(--dp-warning);
  margin-left: 2px;
}

.tab-method {
  font-size: 10px;
  font-weight: 700;
  font-family: Consolas, monospace;
  padding: 1px 5px;
  border-radius: 3px;
}

.tab-method.get { background: var(--dp-success-light); color: var(--dp-success); }
.tab-method.post { background: var(--dp-primary-light); color: var(--dp-primary); }
.tab-method.put { background: var(--dp-warning-light); color: var(--dp-warning); }
.tab-method.delete { background: var(--dp-danger-light); color: var(--dp-danger); }

.tab-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tab-close {
  font-size: 14px;
  opacity: 0.6;
  padding: 2px;
  border-radius: 4px;
  transition: all 0.15s;
}

.tab-close:hover {
  opacity: 1;
  background: var(--dp-danger-light);
  color: var(--dp-danger);
}

.wb-editor-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.editor-tabs {
  background: #fff;
  border-radius: var(--dp-radius-sm);
}

.editor-tabs :deep(.el-tabs__content) {
  padding: 16px;
}

.editor-section {
  min-height: 400px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 14px;
  padding-left: 10px;
  border-left: 3px solid var(--dp-primary);
}

.section-header .section-title {
  margin-bottom: 0;
  border-left: none;
  padding-left: 0;
}

.section-actions {
  display: flex;
  gap: 8px;
}

.config-form {
  max-width: 100%;
}

.form-unit {
  margin-left: 6px;
  font-size: 12px;
  color: var(--dp-text-3);
}

.param-table-wrap {
  margin-top: 12px;
}

.wb-editor-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 响应配置 */
.response-config {
  display: flex;
  gap: 16px;
  margin-top: 12px;
}

.schema-visual {
  flex: 1;
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  padding: 14px;
  background: var(--dp-bg-page);
}

.schema-node-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--dp-text-1);
  margin-bottom: 10px;
}

.schema-children {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-left: 16px;
  border-left: 1px dashed var(--dp-border);
  margin-left: 8px;
}

.schema-field {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12.5px;
  padding: 6px 8px;
  background: #fff;
  border-radius: 4px;
  border: 1px solid var(--dp-border-light);
}

.field-type {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 3px;
  font-family: Consolas, monospace;
  font-weight: 600;
}

.field-type.string { background: #e8f3ff; color: #1664ff; }
.field-type.number { background: #e8ffea; color: #00b42a; }
.field-type.boolean { background: #fff3e8; color: #ff7d00; }
.field-type.object { background: #f5e8ff; color: #722ed1; }
.field-type.array { background: #e0fffa; color: #0fc6c2; }

.field-name {
  font-weight: 500;
  color: var(--dp-text-1);
  font-family: Consolas, monospace;
}

.field-desc {
  margin-left: auto;
  color: var(--dp-text-3);
  font-size: 11.5px;
}

.response-example {
  flex: 1;
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.example-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--dp-bg-page);
  border-bottom: 1px solid var(--dp-border-light);
  font-size: 12.5px;
  font-weight: 500;
}

.example-actions {
  display: flex;
  gap: 4px;
}

.code-editor {
  flex: 1;
  margin: 0;
  padding: 12px;
  background: #0d1117;
  color: #cdd6f4;
  font-family: Consolas, 'JetBrains Mono', monospace;
  font-size: 12.5px;
  line-height: 1.7;
  min-height: 200px;
  max-height: 350px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

/* SQL 编辑器 */
.sql-editor-wrap {
  margin-top: 12px;
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  overflow: hidden;
}

.sql-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px;
  background: var(--dp-bg-page);
  border-bottom: 1px solid var(--dp-border-light);
}

.sql-title {
  font-size: 13px;
  font-weight: 500;
}

.sql-actions {
  display: flex;
  gap: 6px;
}

.sql-editor {
  width: 100%;
  height: 180px;
  resize: none;
  border: none;
  outline: none;
  background: #1e1e2e;
  color: #cdd6f4;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
  padding: 14px;
  tab-size: 2;
}

.sql-result {
  border-top: 1px solid var(--dp-border-light);
  padding: 10px 14px;
  background: #fff;
}

.sql-result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.result-info {
  font-size: 12px;
  color: var(--dp-text-3);
}

/* 右侧调试面板 */
.wb-debug {
  width: 360px;
  flex-shrink: 0;
  background: #fff;
  border-left: 1px solid var(--dp-border-light);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.debug-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--dp-border-light);
  flex-shrink: 0;
}

.debug-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.debug-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
}

.debug-section {
  margin-bottom: 14px;
}

.debug-url-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: var(--dp-bg-page);
  border-radius: 6px;
  border: 1px solid var(--dp-border-light);
}

.method-badge {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
  font-family: Consolas, monospace;
  flex-shrink: 0;
}

.method-badge.get { background: var(--dp-success-light); color: var(--dp-success); }
.method-badge.post { background: var(--dp-primary-light); color: var(--dp-primary); }
.method-badge.put { background: var(--dp-warning-light); color: var(--dp-warning); }
.method-badge.delete { background: var(--dp-danger-light); color: var(--dp-danger); }

.debug-url {
  flex: 1;
  font-size: 12px;
  color: var(--dp-text-2);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.debug-tabs {
  margin-bottom: 12px;
}

.debug-params {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 8px;
}

.debug-param-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.debug-body-editor {
  width: 100%;
  height: 150px;
  resize: none;
  border: 1px solid var(--dp-border);
  border-radius: 6px;
  outline: none;
  background: #0d1117;
  color: #cdd6f4;
  font-family: 'JetBrains Mono', Consolas, monospace;
  font-size: 12.5px;
  line-height: 1.6;
  padding: 10px;
  tab-size: 2;
}

.debug-empty-body {
  padding: 20px 0;
}

.debug-response {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed var(--dp-border-light);
}

.response-header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.response-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.response-meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta-item {
  font-size: 12px;
  color: var(--dp-text-3);
}

.debug-empty-response {
  padding: 30px 0;
}

.response-body {
  margin-top: 8px;
}

.response-code {
  background: #0d1117;
  color: #cdd6f4;
  padding: 12px;
  margin: 0;
  font-family: Consolas, 'JetBrains Mono', monospace;
  font-size: 12px;
  line-height: 1.6;
  max-height: 250px;
  overflow: auto;
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
