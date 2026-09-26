<template>
  <div class="or-page">
    <div class="or-workspace">
    <!-- 顶部工作流列表切换 -->
    <div class="or-workflow-list">
      <div class="or-wf-header">
        <span class="or-wf-title">工作流</span>
        <div class="or-wf-actions">
          <el-button text size="small" @click="showList = !showList">{{ showList ? '收起' : '展开' }}</el-button>
          <el-button type="primary" plain size="small" @click="createWorkflow">新建</el-button>
        </div>
      </div>
      <div v-show="showList" class="or-wf-items">
        <div
          v-for="wf in workflowList"
          :key="wf.id"
          class="or-wf-item"
          :class="{ active: currentWorkflow?.id === wf.id }"
          @click="selectWorkflow(wf)"
        >
          <div class="or-wf-item-name">{{ wf.name }}</div>
          <div class="or-wf-item-desc">{{ wf.desc }}</div>
          <div class="or-wf-item-meta">
            <span class="or-wf-item-tasks">
              <el-icon :size="11"><Operation /></el-icon>{{ wf.tasks }}个任务
            </span>
            <span class="or-wf-item-status">
              <span class="or-wf-dot" :class="wf.status === '运行中' ? 'running' : 'idle'"></span>
              {{ wf.status }}
            </span>
            <button
              v-if="workflowList.length > 1"
              class="or-wf-remove"
              type="button"
              title="删除工作流"
              @click.stop="removeWorkflow(wf)"
            >删除</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 主设计器区域 -->
    <div class="or-designer">
      <!-- 顶部工具栏 -->
      <div class="or-toolbar">
        <div class="or-toolbar-left">
          <el-input v-model="workflowName" size="default" style="width: 240px" />
          <el-tag size="small" effect="plain" type="primary">{{ currentWorkflow?.schedule || '每日调度' }}</el-tag>
        </div>
        <div class="or-toolbar-center">
          <el-button :icon="RefreshLeft" :disabled="!canUndo" @click="undo">撤销</el-button>
          <el-button :icon="RefreshRight" :disabled="!canRedo" @click="redo">重做</el-button>
          <el-divider direction="vertical" />
          <el-button :icon="ZoomOut" @click="zoomOut">缩小</el-button>
          <span class="or-zoom-value">{{ Math.round(scale * 100) }}%</span>
          <el-button :icon="ZoomIn" @click="zoomIn">放大</el-button>
          <el-button :icon="FullScreen" @click="resetView">适应画布</el-button>
        </div>
        <div class="or-toolbar-right">
          <el-button
            :type="configOpen ? 'primary' : 'default'"
            :icon="Setting"
            @click="configOpen = !configOpen"
          >全局配置</el-button>
          <el-button :icon="DocumentChecked" @click="saveWorkflow">保存</el-button>
          <el-button :icon="MagicStick" @click="autoLayout">自动布局</el-button>
          <el-button type="primary" :icon="VideoPlay" @click="runWorkflow">运行</el-button>
        </div>
      </div>

      <div class="or-designer-body">
        <!-- 左侧节点面板 -->
        <div class="or-node-panel">
          <div class="or-panel-tip">
            <el-icon :size="12"><Pointer /></el-icon>
            拖拽组件到画布 / 双击添加
          </div>

          <div v-for="group in nodeGroups" :key="group.key" class="or-node-section">
            <div class="or-section-title" @click="group.open = !group.open">
              <el-icon :size="14" :color="group.color"><component :is="group.icon" /></el-icon>
              <span class="or-section-name">{{ group.label }}</span>
              <span class="or-section-count">{{ group.items.length }}</span>
              <el-icon class="or-section-arrow" :class="{ open: group.open }" :size="12"><ArrowRight /></el-icon>
            </div>
            <div v-show="group.open" class="or-node-list">
              <div
                v-for="n in group.items"
                :key="n.type"
                class="or-node-item"
                draggable="true"
                @dragstart="onItemDragStart($event, n.type)"
                @dblclick="addNode(n.type)"
              >
                <div class="or-node-icon" :style="{ background: n.color + '18', color: n.color }">
                  <el-icon :size="15"><component :is="n.icon" /></el-icon>
                </div>
                <div class="or-node-info">
                  <div class="or-node-name">{{ n.label }}</div>
                  <div class="or-node-desc">{{ n.desc }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 中间画布 -->
        <div
          class="or-canvas-container"
          ref="canvasContainer"
          :class="{ 'or-connecting': !!connecting }"
          @click="selectedNode = null; selectedEdgeId = null"
          @dragover.prevent
          @drop.prevent="onCanvasDrop"
        >
          <div
            class="or-canvas"
            ref="canvasRef"
            :style="{
              width: canvasSize.width + 'px',
              height: canvasSize.height + 'px',
              backgroundSize: `${20 * scale}px ${20 * scale}px`,
              transform: `scale(${scale})`,
              transformOrigin: 'top left'
            }"
          >
            <!-- SVG连线层 - 和节点同一坐标系，固定像素大小 -->
            <svg
              class="or-svg-layer"
              :width="canvasSize.width"
              :height="canvasSize.height"
            >
              <defs>
                <marker id="arrow-default" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto">
                  <path d="M0,0 L10,4 L0,8 Z" fill="#a9aeb8" />
                </marker>
                <marker id="arrow-success" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto">
                  <path d="M0,0 L10,4 L0,8 Z" fill="#00b42a" />
                </marker>
                <marker id="arrow-running" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto">
                  <path d="M0,0 L10,4 L0,8 Z" fill="#1664ff" />
                </marker>
                <marker id="arrow-failed" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto">
                  <path d="M0,0 L10,4 L0,8 Z" fill="#f53f3f" />
                </marker>
                <marker id="arrow-select" markerWidth="10" markerHeight="8" refX="9" refY="4" orient="auto">
                  <path d="M0,0 L10,4 L0,8 Z" fill="#f53f3f" />
                </marker>
              </defs>
              <g
                v-for="edge in edges"
                :key="edge.id"
                class="or-edge-group"
                :class="{ selected: selectedEdgeId === edge.id }"
              >
                <!-- 宽透明命中层，方便点击 -->
                <path
                  class="or-edge-hit"
                  :d="getEdgePath(edge)"
                  fill="none"
                  stroke="transparent"
                  stroke-width="18"
                  @click.stop="selectEdge(edge)"
                  @dblclick.stop="deleteEdge(edge)"
                />
                <path
                  class="or-edge-line"
                  :d="getEdgePath(edge)"
                  fill="none"
                  :stroke="getEdgeColor(edge)"
                  stroke-width="2"
                  :marker-end="selectedEdgeId === edge.id ? 'url(#arrow-select)' : getEdgeMarker(edge)"
                  :class="{ 'or-edge-running': edge.status === 'running' }"
                  @click.stop="selectEdge(edge)"
                  @dblclick.stop="deleteEdge(edge)"
                />
              </g>
              <!-- 连线拖拽预览 -->
              <path v-if="connecting" class="or-connecting-line" :d="connectingPath" fill="none" />
            </svg>

            <!-- 节点层 -->
            <div
              v-for="node in nodes"
              :key="node.id"
              class="or-design-node"
              :class="{
                selected: selectedNode?.id === node.id,
                'connect-target': hoverTargetId === node.id && connecting,
                running: node.status === 'running',
                success: node.status === 'success',
                failed: node.status === 'failed'
              }"
              :data-id="node.id"
              :style="{ left: node.x + 'px', top: node.y + 'px' }"
              @mousedown.stop="startDragNode(node, $event)"
              @click.stop="selectNode(node)"
            >
              <div class="or-design-node-head" :style="{ borderTopColor: getNodeColor(node.type) }">
                <div class="or-design-node-icon" :style="{ background: getNodeColor(node.type) + '18', color: getNodeColor(node.type) }">
                  <el-icon :size="16"><component :is="getNodeIcon(node.type)" /></el-icon>
                </div>
                <div class="or-design-node-title">{{ node.name }}</div>
                <div v-if="node.status === 'success'" class="or-node-status success">
                  <el-icon><CircleCheckFilled /></el-icon>
                </div>
                <div v-else-if="node.status === 'running'" class="or-node-status running">
                  <el-icon class="is-loading"><Loading /></el-icon>
                </div>
                <div v-else-if="node.status === 'failed'" class="or-node-status failed">
                  <el-icon><CircleCloseFilled /></el-icon>
                </div>
              </div>
              <div class="or-design-node-body">
                <span class="or-design-node-desc">{{ node.desc }}</span>
              </div>
              <!-- 连接锚点 -->
              <div class="or-anchor or-anchor-left" title="连线入口" @mousedown.stop="startConnect(node, 'in', $event)"></div>
              <div class="or-anchor or-anchor-right" title="按住拖动到目标节点创建连线" @mousedown.stop="startConnect(node, 'out', $event)"></div>
            </div>
          </div>

          <!-- 缩放悬浮按钮 -->
          <div class="or-zoom-controls">
            <el-button circle size="small" :icon="ZoomIn" @click="zoomIn" />
            <div class="or-zoom-percent">{{ Math.round(scale * 100) }}%</div>
            <el-button circle size="small" :icon="ZoomOut" @click="zoomOut" />
            <el-button circle size="small" :icon="FullScreen" @click="resetView" />
          </div>
        </div>

        <!-- 右侧节点属性面板 -->
        <div v-if="selectedNode" class="or-prop-panel" :style="{ width: propPanelWidth + 'px' }">
          <div class="or-panel-resizer" title="拖动调整宽度" @mousedown.prevent="startResizePanel('prop', $event)" />
          <div class="or-prop-header">
            <span class="or-prop-title">节点配置</span>
            <el-button text size="small" :icon="Close" @click="selectedNode = null" />
          </div>
          <div class="or-prop-body">
            <el-form label-position="top" size="small" class="or-prop-form">
              <el-form-item label="节点名称">
                <el-input v-model="selectedNode.name" />
              </el-form-item>
              <el-form-item label="节点类型">
                <el-input :model-value="getNodeLabel(selectedNode.type)" disabled />
              </el-form-item>
              <el-form-item label="节点描述">
                <el-input v-model="selectedNode.desc" type="textarea" :rows="2" />
              </el-form-item>

              <!-- SQL类节点 -->
              <template v-if="kindOf(selectedNode.type) === 'sql'">
                <el-divider content-position="left">数据配置</el-divider>
                <el-form-item label="数据源">
                  <el-select v-model="selectedNode.datasource" style="width: 100%">
                    <el-option
                      v-for="ds in datasourceOptions(selectedNode.type)"
                      :key="ds.value"
                      :label="ds.label"
                      :value="ds.value"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="SQL语句">
                  <el-input
                    v-model="selectedNode.sql"
                    type="textarea"
                    :autosize="{ minRows: 8, maxRows: 20 }"
                    class="dp-mono"
                    placeholder="SELECT * FROM ..."
                  />
                </el-form-item>
                <el-form-item label="前置SQL（可选）">
                  <el-input v-model="selectedNode.preSql" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" class="dp-mono" />
                </el-form-item>
              </template>

              <!-- 脚本类节点 -->
              <template v-else-if="kindOf(selectedNode.type) === 'script'">
                <el-divider content-position="left">脚本配置</el-divider>
                <el-form-item :label="`${getNodeLabel(selectedNode.type)}脚本内容`">
                  <el-input
                    v-model="selectedNode.script"
                    type="textarea"
                    :autosize="{ minRows: 10, maxRows: 24 }"
                    class="dp-mono"
                    :placeholder="scriptPlaceholder(selectedNode.type)"
                  />
                </el-form-item>
              </template>

              <!-- 大数据类节点 -->
              <template v-else-if="kindOf(selectedNode.type) === 'bigdata'">
                <el-divider content-position="left">任务配置</el-divider>
                <el-form-item label="部署方式">
                  <el-radio-group v-model="selectedNode.deployMode">
                    <el-radio value="local">local</el-radio>
                    <el-radio value="cluster">cluster</el-radio>
                    <el-radio value="yarn">yarn</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="主程序包（jar）">
                  <el-input v-model="selectedNode.mainJar" class="dp-mono" placeholder="hdfs:///apps/etl-job.jar" />
                </el-form-item>
                <el-form-item label="主类（Main Class）">
                  <el-input v-model="selectedNode.mainClass" class="dp-mono" placeholder="com.platform.etl.Main" />
                </el-form-item>
                <el-form-item label="运行参数">
                  <el-input v-model="selectedNode.args" class="dp-mono" placeholder="--date ${biz_date} --env prod" />
                </el-form-item>
              </template>

              <!-- HTTP 节点 -->
              <template v-else-if="selectedNode.type === 'http'">
                <el-divider content-position="left">HTTP 配置</el-divider>
                <el-form-item label="请求地址">
                  <el-input v-model="selectedNode.url" class="dp-mono" placeholder="https://api.example.com/v1/data" />
                </el-form-item>
                <el-form-item label="请求方法">
                  <el-radio-group v-model="selectedNode.method">
                    <el-radio value="GET">GET</el-radio>
                    <el-radio value="POST">POST</el-radio>
                    <el-radio value="PUT">PUT</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="请求参数（JSON）">
                  <el-input v-model="selectedNode.params" type="textarea" :autosize="{ minRows: 4, maxRows: 10 }" class="dp-mono" />
                </el-form-item>
              </template>

              <!-- DataX 节点 -->
              <template v-else-if="selectedNode.type === 'datax'">
                <el-divider content-position="left">DataX 配置</el-divider>
                <el-form-item label="JSON 配置">
                  <el-input
                    v-model="selectedNode.jobJson"
                    type="textarea"
                    :autosize="{ minRows: 12, maxRows: 28 }"
                    class="dp-mono"
                    placeholder='{"job":{"content":[{"reader":{...},"writer":{...}}]}}'
                  />
                </el-form-item>
              </template>

              <!-- SQOOP 节点 -->
              <template v-else-if="selectedNode.type === 'sqoop'">
                <el-divider content-position="left">SQOOP 配置</el-divider>
                <el-form-item label="命令">
                  <el-input
                    v-model="selectedNode.command"
                    type="textarea"
                    :autosize="{ minRows: 6, maxRows: 16 }"
                    class="dp-mono"
                    placeholder="sqoop import --connect jdbc:mysql://..."
                  />
                </el-form-item>
              </template>

              <!-- Dinky 节点 -->
              <template v-else-if="selectedNode.type === 'dinky'">
                <el-divider content-position="left">Dinky 配置</el-divider>
                <el-form-item label="Dinky 地址">
                  <el-input v-model="selectedNode.address" class="dp-mono" placeholder="http://dinky:8888" />
                </el-form-item>
                <el-form-item label="任务ID">
                  <el-input v-model="selectedNode.dinkyTaskId" class="dp-mono" placeholder="123" />
                </el-form-item>
              </template>

              <!-- 条件分支节点 -->
              <template v-else-if="selectedNode.type === 'condition'">
                <el-divider content-position="left">分支条件</el-divider>
                <el-form-item label="条件表达式">
                  <el-input
                    v-model="selectedNode.expression"
                    type="textarea"
                    :autosize="{ minRows: 4, maxRows: 10 }"
                    class="dp-mono"
                    placeholder="${var1} > 100"
                  />
                </el-form-item>
              </template>

              <!-- 子流程节点 -->
              <template v-else-if="selectedNode.type === 'sub_process'">
                <el-divider content-position="left">子流程配置</el-divider>
                <el-form-item label="选择子工作流">
                  <el-select v-model="selectedNode.subWorkflow" style="width: 100%" filterable>
                    <el-option
                      v-for="wf in workflowList.filter(w => w.id !== currentWorkflow?.id)"
                      :key="wf.id"
                      :label="wf.name"
                      :value="String(wf.id)"
                    />
                  </el-select>
                </el-form-item>
              </template>

              <!-- 依赖节点 -->
              <template v-else-if="selectedNode.type === 'dependent'">
                <el-divider content-position="left">依赖配置</el-divider>
                <el-form-item label="依赖工作流">
                  <el-select v-model="selectedNode.dependentWf" style="width: 100%" filterable>
                    <el-option
                      v-for="wf in workflowList.filter(w => w.id !== currentWorkflow?.id)"
                      :key="wf.id"
                      :label="wf.name"
                      :value="String(wf.id)"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="依赖类型">
                  <el-radio-group v-model="selectedNode.dependencyType">
                    <el-radio value="强依赖">强依赖</el-radio>
                    <el-radio value="弱依赖">弱依赖</el-radio>
                  </el-radio-group>
                </el-form-item>
              </template>

              <!-- 等待节点 -->
              <template v-else-if="selectedNode.type === 'wait'">
                <el-divider content-position="left">等待配置</el-divider>
                <el-form-item label="等待时长（分钟）">
                  <el-input-number v-model="selectedNode.waitMinutes" :min="1" :max="1440" style="width: 100%" />
                </el-form-item>
              </template>

              <!-- 循环节点 -->
              <template v-else-if="selectedNode.type === 'loop'">
                <el-divider content-position="left">循环配置</el-divider>
                <el-form-item label="最大循环次数">
                  <el-input-number v-model="selectedNode.loopCount" :min="1" :max="100" style="width: 100%" />
                </el-form-item>
              </template>

              <el-divider content-position="left">高级配置</el-divider>
              <el-form-item label="失败策略">
                <el-radio-group v-model="selectedNode.failStrategy">
                  <el-radio value="retry">重试</el-radio>
                  <el-radio value="skip">跳过</el-radio>
                  <el-radio value="stop">阻断</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="重试次数">
                <el-input-number v-model="selectedNode.retryCount" :min="0" :max="10" style="width: 100%" />
              </el-form-item>
              <el-form-item label="超时时间（分钟）">
                <el-input-number v-model="selectedNode.timeout" :min="1" :max="1440" style="width: 100%" />
              </el-form-item>
            </el-form>

            <div class="or-prop-footer">
              <el-button
                type="danger"
                plain
                size="small"
                :icon="Delete"
                @click="deleteSelectedNode"
              >
                删除节点
              </el-button>
            </div>
          </div>
        </div>

        <!-- 右侧工作流配置面板 -->
        <div v-if="configOpen" class="or-config-panel" :style="{ width: configPanelWidth + 'px' }">
          <div class="or-panel-resizer" title="拖动调整宽度" @mousedown.prevent="startResizePanel('config', $event)" />
          <div class="or-config-header">
            <span class="or-config-title">工作流配置</span>
            <el-button text size="small" :icon="Close" @click="configOpen = false" />
          </div>
          <div class="or-config-tabs">
            <div
              v-for="tab in configTabs"
              :key="tab.key"
              class="or-config-tab"
              :class="{ active: configTab === tab.key }"
              @click="configTab = tab.key"
            >
              <el-icon :size="13"><component :is="tab.icon" /></el-icon>
              {{ tab.label }}
            </div>
          </div>
          <div class="or-config-content">
            <!-- 变量管理 -->
            <div v-show="configTab === 'variables'" class="or-tab-content">
              <div class="or-tab-toolbar">
                <span class="or-tab-title">全局变量</span>
                <el-button size="small" type="primary" plain :icon="Plus" @click="addVariable">添加变量</el-button>
              </div>
              <el-table :data="variables" border size="small">
                <el-table-column label="变量名" min-width="120">
                  <template #default="{ row }">
                    <div class="or-var-name dp-mono">{{ row.name }}</div>
                    <el-tag size="small" effect="plain">{{ row.type }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="value" label="默认值" min-width="90" class-name="dp-mono" />
                <el-table-column prop="desc" label="描述" min-width="80" show-overflow-tooltip />
                <el-table-column label="操作" width="90" align="center">
                  <template #default="{ $index }">
                    <el-button link type="danger" size="small" @click="removeVariable($index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <!-- 参数配置 -->
            <div v-show="configTab === 'params'" class="or-tab-content">
              <div class="or-tab-toolbar">
                <span class="or-tab-title">调度参数</span>
              </div>
              <el-form label-width="80px" size="small" class="or-param-form">
                <el-form-item label="调度周期">
                  <el-select v-model="scheduleConfig.cycle" style="width: 100%">
                    <el-option label="每分钟" value="min" />
                    <el-option label="每小时" value="hour" />
                    <el-option label="每天" value="day" />
                    <el-option label="每周" value="week" />
                    <el-option label="每月" value="month" />
                  </el-select>
                </el-form-item>
                <el-form-item label="执行时间">
                  <el-time-picker v-model="scheduleConfig.time" format="HH:mm" value-format="HH:mm" style="width: 100%" />
                </el-form-item>
                <el-form-item label="超时时间">
                  <el-input-number v-model="scheduleConfig.timeout" :min="1" :max="1440" style="width: 100%" />
                </el-form-item>
                <el-form-item label="失败重试">
                  <el-input-number v-model="scheduleConfig.retry" :min="0" :max="10" style="width: 100%" />
                </el-form-item>
                <el-form-item label="重试间隔">
                  <el-input-number v-model="scheduleConfig.retryInterval" :min="1" :max="60" style="width: 100%" />
                </el-form-item>
              </el-form>
            </div>

            <!-- 依赖配置 -->
            <div v-show="configTab === 'dependencies'" class="or-tab-content">
              <div class="or-tab-toolbar">
                <span class="or-tab-title">上游依赖</span>
                <el-button size="small" type="primary" plain :icon="Plus">添加依赖</el-button>
              </div>
              <el-table :data="dependencies" border size="small">
                <el-table-column prop="name" label="依赖工作流" min-width="130" show-overflow-tooltip />
                <el-table-column prop="type" label="类型" width="76">
                  <template #default="{ row }">
                    <el-tag size="small" effect="plain" :type="row.type === '强依赖' ? 'danger' : 'warning'">{{ row.type }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="offset" label="偏移" width="56" align="center" />
                <el-table-column prop="status" label="状态" width="72">
                  <template #default="{ row }">
                    <el-tag :type="row.status === '成功' ? 'success' : 'warning'" size="small">{{ row.status }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="60" align="center">
                  <template #default>
                    <el-button link type="danger" size="small">移除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, Delete, DocumentChecked, FullScreen, MagicStick, Plus, RefreshLeft, RefreshRight, Setting, VideoPlay, ZoomIn, ZoomOut } from '@element-plus/icons-vue'

const showList = ref(true)
const scale = ref(1)
const selectedNode = ref(null)
const workflowName = ref('交易宽表加工工作流')
const canvasContainer = ref(null)
const canvasRef = ref(null)
const configOpen = ref(false)
const configTab = ref('variables')
const propPanelWidth = ref(320)
const configPanelWidth = ref(440)
const canUndo = ref(false)
const canRedo = ref(false)
const NODE_WIDTH = 180
const NODE_HEIGHT = 72

// 动态画布尺寸
const canvasSize = computed(() => {
  let maxX = 0
  let maxY = 0
  nodes.value.forEach(n => {
    maxX = Math.max(maxX, n.x + NODE_WIDTH)
    maxY = Math.max(maxY, n.y + NODE_HEIGHT)
  })
  return {
    width: Math.max(1200, maxX + 200),
    height: Math.max(600, maxY + 150)
  }
})

const configTabs = [
  { key: 'variables', label: '变量管理', icon: 'Coin' },
  { key: 'params', label: '参数配置', icon: 'Setting' },
  { key: 'dependencies', label: '依赖配置', icon: 'Link' }
]

// 工作流列表
const workflowList = ref([
  { id: 1, name: '交易宽表加工工作流', desc: 'ODS->DWD->DWS全链路加工', tasks: 12, schedule: '每日00:30', status: '运行中' },
  { id: 2, name: '客户标签计算工作流', desc: '客户画像标签体系构建', tasks: 8, schedule: '每日01:00', status: '已发布' },
  { id: 3, name: '销售主题汇总工作流', desc: '销售数据多维度汇总统计', tasks: 15, schedule: '每日02:00', status: '已发布' },
  { id: 4, name: '会员积分计算工作流', desc: '会员积分计算与同步', tasks: 6, schedule: '每小时', status: '开发中' },
  { id: 5, name: '商品库存同步工作流', desc: '库存数据实时同步', tasks: 4, schedule: '每5分钟', status: '已暂停' }
])

const currentWorkflow = ref(workflowList.value[0])

function selectWorkflow(wf) {
  currentWorkflow.value = wf
  workflowName.value = wf.name
  ElMessage.info(`已切换到工作流：${wf.name}`)
}

function createWorkflow() {
  const id = Date.now()
  const workflow = {
    id,
    name: `新建工作流 ${workflowList.value.length + 1}`,
    desc: '请补充处理说明',
    tasks: 0,
    schedule: '未配置调度',
    status: '开发中'
  }
  workflowList.value.unshift(workflow)
  selectWorkflow(workflow)
  ElMessage.success('已创建工作流，请设置节点和调度参数')
}

function removeWorkflow(workflow) {
  if (currentWorkflow.value?.id === workflow.id) {
    currentWorkflow.value = workflowList.value.find(item => item.id !== workflow.id) || null
    workflowName.value = currentWorkflow.value?.name || ''
  }
  workflowList.value = workflowList.value.filter(item => item.id !== workflow.id)
  ElMessage.success(`已删除工作流：${workflow.name}`)
}

// 节点分组（对齐 DolphinScheduler 任务类型体系）
const nodeGroups = ref([
  {
    key: 'sql', label: 'SQL 任务', icon: 'Coin', color: '#1664ff', open: true,
    items: [
      { type: 'sql_mysql', label: 'MySQL', desc: 'MySQL SQL 任务', icon: 'Coin', color: '#4479a1', kind: 'sql' },
      { type: 'sql_postgresql', label: 'PostgreSQL', desc: 'PostgreSQL SQL 任务', icon: 'Box', color: '#699eca', kind: 'sql' },
      { type: 'sql_oracle', label: 'Oracle', desc: 'Oracle SQL 任务', icon: 'Notebook', color: '#c74634', kind: 'sql' },
      { type: 'sql_hive', label: 'Hive', desc: 'Hive SQL 任务', icon: 'Collection', color: '#fdb813', kind: 'sql' },
      { type: 'sql_spark', label: 'SparkSQL', desc: 'Spark SQL 任务', icon: 'Lightning', color: '#e25a1c', kind: 'sql' },
      { type: 'sql_clickhouse', label: 'ClickHouse', desc: 'ClickHouse SQL 任务', icon: 'Grid', color: '#2dc100', kind: 'sql' },
      { type: 'sql_doris', label: 'Doris', desc: 'Doris SQL 任务', icon: 'Odometer', color: '#00b4cf', kind: 'sql' }
    ]
  },
  {
    key: 'script', label: '脚本任务', icon: 'Monitor', color: '#4e5969', open: false,
    items: [
      { type: 'shell', label: 'SHELL', desc: 'Shell 脚本执行', icon: 'Monitor', color: '#4e5969', kind: 'script' },
      { type: 'python', label: 'PYTHON', desc: 'Python 脚本执行', icon: 'EditPen', color: '#3776ab', kind: 'script' },
      { type: 'java', label: 'JAVA', desc: 'Java 程序执行', icon: 'Coffee', color: '#f89820', kind: 'script' },
      { type: 'procedure', label: 'PROCEDURE', desc: '存储过程调用', icon: 'Tickets', color: '#6b4fbb', kind: 'script' },
      { type: 'remoteshell', label: 'REMOTESHELL', desc: '远程 Shell 执行', icon: 'Position', color: '#86909c', kind: 'script' }
    ]
  },
  {
    key: 'bigdata', label: '大数据任务', icon: 'Lightning', color: '#e25a1c', open: false,
    items: [
      { type: 'spark', label: 'SPARK', desc: 'Spark 批任务', icon: 'Lightning', color: '#e25a1c', kind: 'bigdata' },
      { type: 'flink', label: 'FLINK', desc: 'Flink 批任务', icon: 'Ship', color: '#e6526f', kind: 'bigdata' },
      { type: 'flink_stream', label: 'FLINK_STREAM', desc: 'Flink 流任务', icon: 'Cloudy', color: '#d24259', kind: 'bigdata' },
      { type: 'mr', label: 'MR', desc: 'MapReduce 任务', icon: 'Histogram', color: '#f7ba1e', kind: 'bigdata' },
      { type: 'hivecli', label: 'HIVECLI', desc: 'Hive CLI 脚本', icon: 'House', color: '#fdb813', kind: 'bigdata' }
    ]
  },
  {
    key: 'integration', label: '数据集成', icon: 'Connection', color: '#00b42a', open: false,
    items: [
      { type: 'datax', label: 'DataX', desc: 'DataX 数据同步', icon: 'Finished', color: '#00b42a', kind: 'integration' },
      { type: 'sqoop', label: 'SQOOP', desc: 'Sqoop 数据迁移', icon: 'Van', color: '#66c18f', kind: 'integration' },
      { type: 'http', label: 'HTTP', desc: 'HTTP 请求任务', icon: 'Connection', color: '#1664ff', kind: 'integration' },
      { type: 'dinky', label: 'DINKY', desc: 'Dinky 实时任务', icon: 'MagicStick', color: '#722ed1', kind: 'integration' }
    ]
  },
  {
    key: 'logic', label: '逻辑节点', icon: 'Switch', color: '#ff7d00', open: false,
    items: [
      { type: 'condition', label: '条件分支', desc: '条件判断节点', icon: 'Switch', color: '#ff7d00', kind: 'logic' },
      { type: 'sub_process', label: '子流程', desc: '嵌套子工作流', icon: 'Folder', color: '#1664ff', kind: 'logic' },
      { type: 'dependent', label: '依赖节点', desc: '跨工作流依赖', icon: 'Link', color: '#722ed1', kind: 'logic' },
      { type: 'wait', label: '等待', desc: '等待依赖完成', icon: 'Clock', color: '#86909c', kind: 'logic' },
      { type: 'loop', label: '循环', desc: '循环执行节点', icon: 'RefreshRight', color: '#722ed1', kind: 'logic' }
    ]
  },
  {
    key: 'control', label: '通用控制', icon: 'VideoPlay', color: '#86909c', open: false,
    items: [
      { type: 'start', label: '开始', desc: '工作流起点', icon: 'VideoPlay', color: '#00b42a', kind: 'control' },
      { type: 'end', label: '结束', desc: '工作流终点', icon: 'CircleCheck', color: '#86909c', kind: 'control' }
    ]
  }
])

// 所有节点映射
const allNodeMap = computed(() => {
  const map = {}
  nodeGroups.value.forEach(group => {
    group.items.forEach(n => { map[n.type] = n })
  })
  return map
})

function kindOf(type) {
  return allNodeMap.value[type]?.kind || 'common'
}

function scriptPlaceholder(type) {
  const map = {
    shell: '#!/bin/bash\necho "hello dolphin"',
    python: 'def main():\n    print("hello")',
    java: 'public class Main {\n    public static void main(String[] args) {}\n}',
    procedure: 'CALL proc_sync_orders(${biz_date})',
    remoteshell: 'sh /opt/app/sync.sh'
  }
  return map[type] || ''
}

function datasourceOptions(type) {
  const map = {
    sql_mysql: [
      { label: '核心交易库 core_trade', value: 'core_trade' },
      { label: '客户主数据库 customer_db', value: 'customer_db' }
    ],
    sql_postgresql: [{ label: '分析库 pg_analytics', value: 'pg_analytics' }],
    sql_oracle: [{ label: 'ERP 库 erp_oracle', value: 'erp_oracle' }],
    sql_hive: [{ label: '数据仓库 data_warehouse', value: 'data_warehouse' }],
    sql_spark: [{ label: 'SparkThrift spark_catalog', value: 'spark_catalog' }],
    sql_clickhouse: [{ label: '日志集群 ck_log', value: 'ck_log' }],
    sql_doris: [{ label: 'Doris 集群 doris_ods', value: 'doris_ods' }]
  }
  return map[type] || []
}

function defaultsFor(type) {
  const base = { status: 'idle', failStrategy: 'retry', retryCount: 3, timeout: 60 }
  const kind = kindOf(type)
  if (kind === 'sql') return { ...base, datasource: '', sql: '', preSql: '' }
  if (kind === 'script') return { ...base, script: '' }
  if (kind === 'bigdata') return { ...base, deployMode: 'local', mainJar: '', mainClass: '', args: '' }
  if (type === 'http') return { ...base, url: '', method: 'GET', params: '' }
  if (type === 'datax') return { ...base, jobJson: '' }
  if (type === 'sqoop') return { ...base, command: '' }
  if (type === 'dinky') return { ...base, address: '', dinkyTaskId: '' }
  if (type === 'condition') return { ...base, expression: '' }
  if (type === 'sub_process') return { ...base, subWorkflow: '' }
  if (type === 'dependent') return { ...base, dependentWf: '', dependencyType: '强依赖' }
  if (type === 'wait') return { ...base, waitMinutes: 10 }
  if (type === 'loop') return { ...base, loopCount: 3 }
  return base
}

function getNodeColor(type) {
  return allNodeMap.value[type]?.color || '#1664ff'
}

function getNodeIcon(type) {
  return allNodeMap.value[type]?.icon || 'Document'
}

function getNodeLabel(type) {
  return allNodeMap.value[type]?.label || type
}

// 画布节点
const nodes = ref([
  { id: 'start', type: 'start', name: '开始', desc: '工作流起点', x: 60, y: 180, status: 'success', failStrategy: 'stop', retryCount: 0, timeout: 30 },
  { id: 'n1', type: 'sql_mysql', name: '抽取订单数据', desc: 'ods.order_info', x: 240, y: 80, status: 'success', datasource: 'core_trade', sql: 'SELECT * FROM order_info', failStrategy: 'retry', retryCount: 3, timeout: 60 },
  { id: 'n2', type: 'sql_mysql', name: '抽取会员数据', desc: 'dim.customer_dim', x: 240, y: 280, status: 'success', datasource: 'customer_db', sql: 'SELECT * FROM customer_dim', failStrategy: 'retry', retryCount: 3, timeout: 60 },
  { id: 'n3', type: 'sql_spark', name: '数据清洗过滤', desc: '过滤脏数据', x: 420, y: 80, status: 'success', datasource: 'spark_catalog', sql: 'SELECT * FROM t WHERE amount > 0', failStrategy: 'skip', retryCount: 0, timeout: 30 },
  { id: 'n4', type: 'sql_spark', name: '维度关联', desc: '关联客户维度', x: 420, y: 280, status: 'success', datasource: 'spark_catalog', sql: 'JOIN dim.customer', failStrategy: 'stop', retryCount: 2, timeout: 120 },
  { id: 'n5', type: 'sql_spark', name: '聚合计算', desc: '按维度聚合', x: 600, y: 180, status: 'running', datasource: 'spark_catalog', sql: 'GROUP BY ...', failStrategy: 'retry', retryCount: 3, timeout: 180 },
  { id: 'n6', type: 'sql_hive', name: '写入宽表', desc: 'dwd.trade_wide', x: 780, y: 180, status: 'idle', datasource: 'data_warehouse', sql: 'INSERT OVERWRITE TABLE ...', failStrategy: 'stop', retryCount: 0, timeout: 120 },
  { id: 'end', type: 'end', name: '结束', desc: '工作流终点', x: 960, y: 180, status: 'idle', failStrategy: 'stop', retryCount: 0, timeout: 30 }
])

// 边
const edges = ref([
  { id: 'e1', from: 'start', to: 'n1', status: 'success' },
  { id: 'e2', from: 'start', to: 'n2', status: 'success' },
  { id: 'e3', from: 'n1', to: 'n3', status: 'success' },
  { id: 'e4', from: 'n2', to: 'n4', status: 'success' },
  { id: 'e5', from: 'n3', to: 'n5', status: 'success' },
  { id: 'e6', from: 'n4', to: 'n5', status: 'success' },
  { id: 'e7', from: 'n5', to: 'n6', status: 'running' },
  { id: 'e8', from: 'n6', to: 'end', status: 'idle' }
])

let nodeIdCounter = 10
let edgeIdCounter = 100
let dragState = null

// 面板拖拽添加节点
const dragType = ref(null)

function onItemDragStart(e, type) {
  dragType.value = type
  e.dataTransfer.effectAllowed = 'copy'
  e.dataTransfer.setData('text/plain', type)
}

function clientToCanvas(clientX, clientY) {
  const rect = canvasRef.value.getBoundingClientRect()
  return {
    x: (clientX - rect.left) / scale.value,
    y: (clientY - rect.top) / scale.value
  }
}

function onCanvasDrop(e) {
  const type = dragType.value || e.dataTransfer.getData('text/plain')
  if (!type) return
  const p = clientToCanvas(e.clientX, e.clientY)
  addNodeAt(type, Math.max(0, p.x - NODE_WIDTH / 2), Math.max(0, p.y - NODE_HEIGHT / 2))
  dragType.value = null
}

function addNodeAt(type, x, y) {
  nodeIdCounter++
  const node = {
    id: 'n' + nodeIdCounter,
    type,
    name: getNodeLabel(type),
    desc: allNodeMap.value[type]?.desc || '',
    x: Math.round(x),
    y: Math.round(y),
    ...defaultsFor(type)
  }
  nodes.value.push(node)
  selectedNode.value = node
  canUndo.value = true
  ElMessage.success(`已添加节点：${getNodeLabel(type)}`)
}

function addNode(type) {
  const rect = canvasContainer.value?.getBoundingClientRect()
  const p = rect ? clientToCanvas(rect.left + rect.width / 2, rect.top + rect.height / 2) : { x: 300, y: 180 }
  addNodeAt(type, p.x - NODE_WIDTH / 2 + (Math.random() * 80 - 40), p.y - NODE_HEIGHT / 2 + (Math.random() * 80 - 40))
}

function selectNode(node) {
  selectedNode.value = node
}

function deleteSelectedNode() {
  if (!selectedNode.value) return
  const id = selectedNode.value.id
  if (id === 'start' || id === 'end') {
    ElMessage.warning('开始和结束节点不可删除')
    return
  }
  nodes.value = nodes.value.filter(n => n.id !== id)
  edges.value = edges.value.filter(e => e.from !== id && e.to !== id)
  selectedNode.value = null
  ElMessage.success('节点已删除')
}

function startDragNode(node, e) {
  dragState = {
    node,
    startX: e.clientX,
    startY: e.clientY,
    origX: node.x,
    origY: node.y
  }
  const move = ev => {
    if (!dragState) return
    dragState.node.x = dragState.origX + (ev.clientX - dragState.startX) / scale.value
    dragState.node.y = dragState.origY + (ev.clientY - dragState.startY) / scale.value
  }
  const up = () => {
    dragState = null
    document.removeEventListener('mousemove', move)
    document.removeEventListener('mouseup', up)
  }
  document.addEventListener('mousemove', move)
  document.addEventListener('mouseup', up)
}

// ===== 连线交互 =====
const connecting = ref(null) // { fromId, toX, toY }
const hoverTargetId = ref(null)
const selectedEdgeId = ref(null)

const connectingPath = computed(() => {
  if (!connecting.value) return ''
  const from = nodes.value.find(n => n.id === connecting.value.fromId)
  if (!from) return ''
  const x1 = from.x + NODE_WIDTH
  const y1 = from.y + NODE_HEIGHT / 2
  const x2 = connecting.value.toX
  const y2 = connecting.value.toY
  const dx = Math.abs(x2 - x1)
  const offset = Math.min(dx * 0.5, 80)
  return `M ${x1} ${y1} C ${x1 + offset} ${y1}, ${x2 - offset} ${y2}, ${x2} ${y2}`
})

function startConnect(node, direction, e) {
  if (direction !== 'out') {
    ElMessage.info('请从节点的右侧锚点（圆点）按下并拖动到目标节点')
    return
  }
  e.preventDefault()
  const p = clientToCanvas(e.clientX, e.clientY)
  connecting.value = { fromId: node.id, toX: p.x, toY: p.y }
  selectedNode.value = null
  selectedEdgeId.value = null

  const move = ev => {
    if (!connecting.value) return
    const q = clientToCanvas(ev.clientX, ev.clientY)
    connecting.value.toX = q.x
    connecting.value.toY = q.y
    const el = document.elementFromPoint(ev.clientX, ev.clientY)
    const nodeEl = el?.closest?.('.or-design-node')
    hoverTargetId.value = nodeEl?.dataset.id || null
  }
  const up = ev => {
    document.removeEventListener('mousemove', move)
    document.removeEventListener('mouseup', up)
    const fromId = connecting.value?.fromId
    const el = document.elementFromPoint(ev.clientX, ev.clientY)
    const nodeEl = el?.closest?.('.or-design-node')
    const toId = nodeEl?.dataset.id || null
    connecting.value = null
    hoverTargetId.value = null
    if (!fromId || !toId || toId === fromId) {
      if (toId === fromId) ElMessage.warning('不能连接到节点自身')
      return
    }
    if (edges.value.some(ed => ed.from === fromId && ed.to === toId)) {
      ElMessage.warning('两个节点之间已存在连线')
      return
    }
    // 防止形成环（A→B 且 B→A 也会阻塞调度）
    if (wouldCreateCycle(fromId, toId)) {
      ElMessage.warning('该连线会产生循环依赖，DolphinScheduler 不允许')
      return
    }
    edges.value.push({ id: 'e' + (++edgeIdCounter), from: fromId, to: toId, status: 'idle' })
    canUndo.value = true
    ElMessage.success('连线已创建')
  }
  document.addEventListener('mousemove', move)
  document.addEventListener('mouseup', up)
}

function wouldCreateCycle(fromId, toId) {
  // 加边 fromId→toId 后，若从 toId 能走回 fromId 则成环
  const adj = {}
  edges.value.forEach(e => {
    (adj[e.from] = adj[e.from] || []).push(e.to)
  })
  ;(adj[fromId] = adj[fromId] || []).push(toId)
  const visited = new Set()
  const stack = [toId]
  while (stack.length) {
    const cur = stack.pop()
    if (cur === fromId) return true
    if (visited.has(cur)) continue
    visited.add(cur)
    ;(adj[cur] || []).forEach(n => stack.push(n))
  }
  return false
}

function selectEdge(edge) {
  selectedNode.value = null
  if (selectedEdgeId.value === edge.id) {
    selectedEdgeId.value = null
  } else {
    selectedEdgeId.value = edge.id
    ElMessage({ message: '连线已选中：按 Delete 删除，或双击直接删除', type: 'info', duration: 2500 })
  }
}

function deleteEdge(edge) {
  edges.value = edges.value.filter(e => e.id !== edge.id)
  if (selectedEdgeId.value === edge.id) selectedEdgeId.value = null
  ElMessage.success('连线已删除')
}

function onKeydown(e) {
  if (e.key !== 'Delete' && e.key !== 'Backspace') return
  const tag = e.target?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || e.target?.isContentEditable) return
  if (selectedEdgeId.value) {
    const edge = edges.value.find(x => x.id === selectedEdgeId.value)
    if (edge) deleteEdge(edge)
    e.preventDefault()
  } else if (selectedNode.value) {
    deleteSelectedNode()
    e.preventDefault()
  }
}

function getEdgePath(edge) {
  const from = nodes.value.find(n => n.id === edge.from)
  const to = nodes.value.find(n => n.id === edge.to)
  if (!from || !to) return ''
  // 从源节点右侧中心点到目标节点左侧中心点
  const x1 = from.x + NODE_WIDTH
  const y1 = from.y + NODE_HEIGHT / 2
  const x2 = to.x
  const y2 = to.y + NODE_HEIGHT / 2
  const dx = Math.abs(x2 - x1)
  const offset = Math.min(dx * 0.5, 80)
  const cx1 = x1 + offset
  const cx2 = x2 - offset
  return `M ${x1} ${y1} C ${cx1} ${y1}, ${cx2} ${y2}, ${x2} ${y2}`
}

function getEdgeColor(edge) {
  return {
    success: '#00b42a',
    running: '#1664ff',
    failed: '#f53f3f',
    idle: '#a9aeb8'
  }[edge.status] || '#a9aeb8'
}

function getEdgeMarker(edge) {
  return `url(#arrow-${edge.status || 'default'})`
}

// 缩放控制
function zoomIn() {
  scale.value = Math.min(1.5, +(scale.value + 0.1).toFixed(1))
}

function zoomOut() {
  scale.value = Math.max(0.5, +(scale.value - 0.1).toFixed(1))
}

function resetView() {
  scale.value = 1
  ElMessage.success('画布已重置')
}

function startResizePanel(which, event) {
  const startX = event.clientX
  const startW = which === 'prop' ? propPanelWidth.value : configPanelWidth.value
  const min = which === 'prop' ? 240 : 320
  const move = e => {
    const w = startW + (startX - e.clientX)
    const clamped = Math.min(760, Math.max(min, w))
    if (which === 'prop') propPanelWidth.value = clamped
    else configPanelWidth.value = clamped
  }
  const up = () => {
    document.removeEventListener('mousemove', move)
    document.removeEventListener('mouseup', up)
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  document.addEventListener('mousemove', move)
  document.addEventListener('mouseup', up)
}

function undo() {
  ElMessage.info('撤销操作')
}

function redo() {
  ElMessage.info('重做操作')
}

function saveWorkflow() {
  ElMessage.success('工作流已保存')
}

function autoLayout() {
  const GAP_X = 80
  const GAP_Y = 40
  const PADDING = 60

  const inDeg = {}
  const children = {}
  nodes.value.forEach(n => {
    inDeg[n.id] = 0
    children[n.id] = []
  })
  edges.value.forEach(e => {
    if (inDeg[e.to] === undefined || !children[e.from]) return
    inDeg[e.to]++
    children[e.from].push(e.to)
  })

  // Kahn 分层：入度为 0 的节点为第一层，逐层向下
  const levelOf = {}
  let current = nodes.value.filter(n => inDeg[n.id] === 0).map(n => n.id)
  let level = 0
  while (current.length) {
    current.forEach(id => { levelOf[id] = level })
    const next = []
    current.forEach(id => {
      children[id].forEach(c => {
        inDeg[c]--
        if (inDeg[c] === 0) next.push(c)
      })
    })
    current = next
    level++
  }
  // 兜底：成环节点统一放最后一层
  nodes.value.forEach(n => {
    if (levelOf[n.id] === undefined) levelOf[n.id] = level
  })

  // 按层分组，计算每层垂直跨度，整体垂直居中
  const levels = []
  for (let i = 0; i <= level; i++) levels.push([])
  nodes.value.forEach(n => levels[levelOf[n.id]].push(n))

  const spans = levels.map(ns => ns.length * NODE_HEIGHT + Math.max(0, ns.length - 1) * GAP_Y)
  const maxSpan = Math.max(...spans, NODE_HEIGHT)
  const centerAxis = PADDING + maxSpan / 2

  levels.forEach((ns, lv) => {
    const span = spans[lv]
    let y = centerAxis - span / 2
    const x = PADDING + lv * (NODE_WIDTH + GAP_X)
    ns.forEach(n => {
      n.x = x
      n.y = Math.round(y)
      y += NODE_HEIGHT + GAP_Y
    })
  })

  ElMessage.success('自动布局完成')
}

function runWorkflow() {
  ElMessage.info('工作流已提交运行')
  // 模拟运行状态
  const order = ['start', 'n1', 'n2', 'n3', 'n4', 'n5', 'n6', 'end']
  order.forEach((id, i) => {
    setTimeout(() => {
      const node = nodes.value.find(n => n.id === id)
      if (node) node.status = 'running'
      if (i > 0) {
        const prevEdge = edges.value.find(e => e.to === id)
        if (prevEdge) prevEdge.status = 'running'
      }
      if (i > 1) {
        const prevNode = nodes.value.find(n => n.id === order[i - 1])
        if (prevNode) prevNode.status = 'success'
        const prevPrevEdge = edges.value.find(e => e.to === order[i - 1])
        if (prevPrevEdge) prevPrevEdge.status = 'success'
      }
    }, i * 800)
  })
}

// 变量管理
const variables = ref([
  { name: '${biz_date}', type: 'String', value: '2026-09-26', desc: '业务日期' },
  { name: '${start_date}', type: 'String', value: '2026-09-01', desc: '统计开始日期' },
  { name: '${end_date}', type: 'String', value: '2026-09-30', desc: '统计结束日期' },
  { name: '${threshold}', type: 'Number', value: '1000', desc: '阈值参数' }
])

function addVariable() {
  variables.value.push({
    name: '${new_var}',
    type: 'String',
    value: '',
    desc: '新变量'
  })
  ElMessage.success('已添加变量')
}

function removeVariable(index) {
  variables.value.splice(index, 1)
  ElMessage.success('变量已删除')
}

// 调度参数
const scheduleConfig = reactive({
  cycle: 'day',
  time: '00:30',
  timeout: 120,
  retry: 3,
  retryInterval: 5
})

// 依赖配置
const dependencies = ref([
  { name: 'ODS层数据同步工作流', type: '强依赖', offset: 'T+0', status: '成功' },
  { name: '客户维度更新工作流', type: '弱依赖', offset: 'T+0', status: '成功' },
  { name: '商品维度同步工作流', type: '强依赖', offset: 'T-1', status: '成功' }
])

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.or-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
  min-height: 560px;
  background: var(--dp-bg-page);
  overflow: hidden;
}

.or-workspace {
  display: flex;
  flex: 1;
  min-height: 0;
  min-width: 0;
}

/* 左侧工作流列表 */
.or-workflow-list {
  width: 220px;
  background: #fff;
  border-right: 1px solid var(--dp-border-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.or-wf-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
  border-bottom: 1px solid var(--dp-border-light);
}

.or-wf-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.or-wf-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.or-wf-items {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.or-wf-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 6px;
  transition: all 0.15s;
  border: 1px solid transparent;
}

.or-wf-item:hover {
  background: var(--dp-primary-bg);
}

.or-wf-item.active {
  background: var(--dp-primary-light);
  border-color: var(--dp-primary);
}

.or-wf-item-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
  margin-bottom: 4px;
}

.or-wf-item-desc {
  font-size: 11px;
  color: var(--dp-text-3);
  margin-bottom: 6px;
}

.or-wf-item-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 10.5px;
  color: var(--dp-text-3);
}

.or-wf-item-tasks {
  display: flex;
  align-items: center;
  gap: 3px;
}

.or-wf-item-status {
  display: flex;
  align-items: center;
  gap: 4px;
}

.or-wf-remove {
  appearance: none;
  border: 0;
  background: transparent;
  color: var(--dp-danger);
  font-size: 11px;
  padding: 2px 0 2px 5px;
  cursor: pointer;
  opacity: 0;
}

.or-wf-item:hover .or-wf-remove,
.or-wf-item.active .or-wf-remove {
  opacity: 1;
}

.or-wf-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--dp-text-4);
}

.or-wf-dot.running {
  background: var(--dp-primary);
}

/* 主设计器 */
.or-designer {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}

/* 顶部工具栏 */
.or-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid var(--dp-border-light);
  flex-shrink: 0;
}

.or-toolbar-left,
.or-toolbar-center,
.or-toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.or-zoom-value {
  font-size: 12px;
  color: var(--dp-text-3);
  min-width: 45px;
  text-align: center;
}

/* 设计器主体 */
.or-designer-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

/* 左侧节点面板 */
.or-node-panel {
  width: 200px;
  background: #fafbfc;
  border-right: 1px solid var(--dp-border-light);
  overflow-y: auto;
  flex-shrink: 0;
  padding: 10px;
}

.or-panel-tip {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--dp-text-3);
  background: var(--dp-primary-bg);
  border: 1px dashed var(--dp-primary-light);
  border-radius: 6px;
  padding: 6px 8px;
  margin-bottom: 10px;
  line-height: 1.5;
}

.or-node-section {
  margin-bottom: 6px;
}

.or-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--dp-text-2);
  margin-bottom: 8px;
  padding: 4px 6px;
  border-radius: 6px;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}

.or-section-title:hover {
  background: #f0f2f5;
}

.or-section-name {
  flex: 1;
}

.or-section-count {
  font-size: 10px;
  font-weight: 400;
  color: var(--dp-text-4);
  background: #f2f3f5;
  border-radius: 8px;
  padding: 1px 6px;
}

.or-section-arrow {
  color: var(--dp-text-4);
  transition: transform 0.2s;
}

.or-section-arrow.open {
  transform: rotate(90deg);
}

.or-node-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0 2px 8px 2px;
}

.or-node-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid var(--dp-border-light);
  border-radius: 8px;
  cursor: grab;
  transition: all 0.15s;
}

.or-node-item:active {
  cursor: grabbing;
}

.or-node-item:hover {
  border-color: var(--dp-primary);
  box-shadow: 0 2px 8px rgba(22, 100, 255, 0.1);
  transform: translateY(-1px);
}

.or-node-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.or-node-info {
  flex: 1;
  min-width: 0;
}

.or-node-name {
  font-size: 12.5px;
  font-weight: 500;
  color: var(--dp-text-1);
}

.or-node-desc {
  font-size: 10.5px;
  color: var(--dp-text-3);
  margin-top: 2px;
}

/* 画布容器 */
.or-canvas-container {
  flex: 1;
  min-width: 0;
  min-height: 0;
  position: relative;
  overflow: auto;
  background-color: #f7f8fa;
  background-image: radial-gradient(circle, #dfe2e8 1px, transparent 1px);
  background-size: 20px 20px;
}

.or-canvas-container.or-connecting {
  cursor: crosshair;
}

/* 连线交互 */
.or-edge-hit {
  cursor: pointer;
}

.or-edge-line {
  cursor: pointer;
  transition: stroke-width 0.1s;
}

.or-edge-group:hover .or-edge-line {
  stroke-width: 3;
}

.or-edge-group.selected .or-edge-line {
  stroke: #f53f3f !important;
  stroke-width: 3;
  stroke-dasharray: 8 4;
}

.or-connecting-line {
  stroke: #1664ff;
  stroke-width: 2;
  stroke-dasharray: 6 4;
  pointer-events: none;
}

.or-design-node.connect-target {
  outline: 2px dashed #1664ff;
  outline-offset: 3px;
  border-radius: 8px;
}

.or-canvas {
  position: relative;
  min-width: 100%;
  min-height: 100%;
}

.or-svg-layer {
  position: absolute;
  top: 0;
  left: 0;
  pointer-events: none;
  z-index: 1;
}

.or-svg-layer .or-edge-hit,
.or-svg-layer .or-edge-line {
  pointer-events: stroke;
}

.or-edge-running {
  stroke-dasharray: 8 4;
  animation: or-edge-flow 1s linear infinite;
}

@keyframes or-edge-flow {
  to { stroke-dashoffset: -24; }
}

/* 设计节点 */
.or-design-node {
  position: absolute;
  width: 180px;
  background: #fff;
  border: 1.5px solid var(--dp-border);
  border-radius: 10px;
  cursor: move;
  user-select: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  z-index: 2;
  transition: box-shadow 0.15s, border-color 0.15s;
}

.or-design-node:hover {
  box-shadow: 0 4px 16px rgba(22, 100, 255, 0.12);
}

.or-design-node.selected {
  border-color: var(--dp-primary);
  box-shadow: 0 0 0 4px rgba(22, 100, 255, 0.1);
}

.or-design-node.success {
  border-color: var(--dp-success);
}

.or-design-node.running {
  border-color: var(--dp-primary);
  box-shadow: 0 0 0 4px rgba(22, 100, 255, 0.12);
}

.or-design-node.failed {
  border-color: var(--dp-danger);
}

.or-design-node-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-top: 3px solid var(--dp-primary);
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid var(--dp-border-light);
}

.or-design-node-icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.or-design-node-title {
  flex: 1;
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.or-node-status {
  flex-shrink: 0;
  font-size: 14px;
}

.or-node-status.success { color: var(--dp-success); }
.or-node-status.running { color: var(--dp-primary); }
.or-node-status.failed { color: var(--dp-danger); }

.or-design-node-body {
  padding: 8px 12px 10px;
}

.or-design-node-desc {
  font-size: 11.5px;
  color: var(--dp-text-3);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 连接锚点 */
.or-anchor {
  position: absolute;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  border: 2px solid var(--dp-primary);
  top: 50%;
  transform: translateY(-50%);
  cursor: crosshair;
  z-index: 3;
  transition: all 0.15s;
}

.or-anchor::after {
  content: '';
  position: absolute;
  inset: -6px;
}

.or-anchor:hover {
  transform: translateY(-50%) scale(1.3);
  background: var(--dp-primary);
}

.or-anchor-left { left: -7px; }
.or-anchor-right { right: -7px; }

/* 缩放控制 */
.or-zoom-controls {
  position: sticky;
  left: 100%;
  bottom: 16px;
  transform: translateX(calc(-100% - 16px));
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  background: #fff;
  padding: 8px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.or-zoom-percent {
  font-size: 11px;
  color: var(--dp-text-3);
  font-weight: 500;
}

/* 面板左边缘拖拽条 */
.or-panel-resizer {
  position: absolute;
  left: -4px;
  top: 0;
  bottom: 0;
  width: 8px;
  cursor: col-resize;
  z-index: 10;
}

.or-panel-resizer::after {
  content: '';
  position: absolute;
  left: 3px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: transparent;
  transition: background 0.15s;
}

.or-panel-resizer:hover::after,
.or-panel-resizer:active::after {
  background: var(--dp-primary);
}

/* 右侧节点属性面板 */
.or-prop-panel {
  position: relative;
  background: #fff;
  border-left: 1px solid var(--dp-border-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  min-height: 0;
}

.or-prop-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px 10px 16px;
  border-bottom: 1px solid var(--dp-border-light);
  flex-shrink: 0;
}

.or-prop-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.or-prop-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  padding: 14px 16px;
}

.or-prop-body :deep(.el-form-item__label) {
  font-size: 12px;
  color: var(--dp-text-2);
  font-weight: 500;
  line-height: 1.6;
  margin-bottom: 4px;
}

.or-prop-body :deep(.el-divider__text) {
  font-size: 12px;
  color: var(--dp-text-3);
  font-weight: 600;
}

.or-prop-footer {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid var(--dp-border-light);
}

/* 右侧工作流配置面板 */
.or-config-panel {
  position: relative;
  background: #fff;
  border-left: 1px solid var(--dp-border-light);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  min-height: 0;
}

.or-config-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px 10px 16px;
  border-bottom: 1px solid var(--dp-border-light);
  flex-shrink: 0;
}

.or-config-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.or-config-tabs {
  display: flex;
  border-bottom: 1px solid var(--dp-border-light);
  background: #fafbfc;
  flex-shrink: 0;
}

.or-config-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 8px;
  font-size: 13px;
  color: var(--dp-text-3);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.15s;
}

.or-config-tab:hover {
  color: var(--dp-primary);
}

.or-config-tab.active {
  color: var(--dp-primary);
  border-bottom-color: var(--dp-primary);
  background: #fff;
  font-weight: 500;
}

.or-config-content {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 16px;
}

.or-var-name {
  font-size: 12px;
  color: var(--dp-text-1);
  margin-bottom: 2px;
  word-break: break-all;
}

.or-tab-content {
  min-height: 100%;
}

.or-tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.or-tab-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text-1);
}

.or-param-form {
  margin: 0;
}
</style>
