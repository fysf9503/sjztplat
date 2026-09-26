-- ============================================================
-- 数据中台 MySQL 兼容 DDL（临时用于本地开发，生产用 Doris）
-- 数据库：data_platform
-- ============================================================

CREATE DATABASE IF NOT EXISTS data_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE data_platform;

-- ============================================================
-- 一、系统管理表
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id          VARCHAR(32) NOT NULL COMMENT '主键ID',
    username    VARCHAR(64) NOT NULL COMMENT '用户名',
    password    VARCHAR(128) NOT NULL COMMENT '密码',
    nickname    VARCHAR(64) COMMENT '昵称',
    email       VARCHAR(128) COMMENT '邮箱',
    phone       VARCHAR(20) COMMENT '手机号',
    avatar      VARCHAR(256) COMMENT '头像URL',
    dept_id     VARCHAR(32) COMMENT '部门ID',
    status      VARCHAR(2) DEFAULT '1' COMMENT '状态 0停用 1启用',
    remark      VARCHAR(256) COMMENT '备注',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    create_by   VARCHAR(32) COMMENT '创建人',
    update_by   VARCHAR(32) COMMENT '更新人',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_role (
    id          VARCHAR(32) NOT NULL,
    role_name   VARCHAR(64) NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(64) NOT NULL COMMENT '角色编码',
    status      VARCHAR(2) DEFAULT '1',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_menu (
    id          VARCHAR(32) NOT NULL,
    parent_id   VARCHAR(32) DEFAULT '0' COMMENT '父菜单ID',
    menu_name   VARCHAR(64) NOT NULL COMMENT '菜单名称',
    path        VARCHAR(128) COMMENT '路由路径',
    component   VARCHAR(128) COMMENT '组件路径',
    menu_type   VARCHAR(2) COMMENT '类型 M目录 C菜单 F按钮',
    permission  VARCHAR(128) COMMENT '权限标识',
    sort_order  INT DEFAULT 0 COMMENT '排序',
    status      VARCHAR(2) DEFAULT '1',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 二、数据质量表
-- ============================================================

CREATE TABLE IF NOT EXISTS quality_check_rule (
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    rule_name       VARCHAR(128) NOT NULL COMMENT '规则名称',
    rule_type       VARCHAR(32) COMMENT '规则类型',
    rule_level      VARCHAR(32) COMMENT '规则级别',
    rule_source_id  VARCHAR(32) COMMENT '数据源ID',
    rule_source_name VARCHAR(128) COMMENT '数据源名称',
    rule_sql        TEXT COMMENT '核查SQL',
    last_check_batch VARCHAR(32) COMMENT '最近核查批次号',
    status          VARCHAR(2) DEFAULT '1' COMMENT '0停用 1启用',
    remark          VARCHAR(256),
    create_time     DATETIME NOT NULL,
    update_time     DATETIME NOT NULL,
    create_by       VARCHAR(32),
    update_by       VARCHAR(32),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quality_check_report (
    id              VARCHAR(32) NOT NULL,
    check_rule_id   VARCHAR(32) NOT NULL COMMENT '规则ID',
    check_date      DATETIME NOT NULL COMMENT '核查时间',
    check_error_count INT DEFAULT 0 COMMENT '异常数',
    check_total_count INT DEFAULT 0 COMMENT '总数',
    check_result    VARCHAR(512) COMMENT '核查结果',
    check_batch     VARCHAR(32) COMMENT '批次号',
    KEY idx_rule_id (check_rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS quality_schedule_log (
    id              VARCHAR(32) NOT NULL,
    status          VARCHAR(2) COMMENT '0失败 1成功',
    execute_job_id  VARCHAR(32) COMMENT '任务ID',
    execute_rule_id VARCHAR(32) COMMENT '规则ID',
    execute_date    DATETIME COMMENT '执行时间',
    execute_result  VARCHAR(512) COMMENT '执行结果',
    execute_batch   VARCHAR(32) COMMENT '批次号',
    KEY idx_job_id (execute_job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 三、元数据管理表
-- ============================================================

CREATE TABLE IF NOT EXISTS metadata_source (
    id          VARCHAR(32) NOT NULL,
    source_name VARCHAR(128) NOT NULL COMMENT '数据源名称',
    db_type     VARCHAR(32) NOT NULL COMMENT '数据库类型',
    host        VARCHAR(128) COMMENT '主机',
    port        INT COMMENT '端口',
    db_name     VARCHAR(64) COMMENT '库名',
    username    VARCHAR(64) COMMENT '用户名',
    password    VARCHAR(128) COMMENT '密码',
    status      VARCHAR(2) DEFAULT '1',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 四、用户组与权限控制
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_user_group (
    id          VARCHAR(32) NOT NULL COMMENT '主键',
    user_id     VARCHAR(32) NOT NULL COMMENT '用户ID',
    group_id    VARCHAR(32) NOT NULL COMMENT '用户组ID',
    role_in_group VARCHAR(32) DEFAULT 'member' COMMENT '组内角色 owner/admin/member',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_group (
    id          VARCHAR(32) NOT NULL COMMENT '主键',
    group_name  VARCHAR(128) NOT NULL COMMENT '组名称',
    group_code  VARCHAR(64) NOT NULL COMMENT '组编码',
    description VARCHAR(512) COMMENT '描述',
    owner_id    VARCHAR(32) NOT NULL COMMENT '创建者ID',
    status      VARCHAR(2) DEFAULT '1' COMMENT '0停用 1启用',
    create_time DATETIME NOT NULL,
    update_time DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sys_group_resource (
    id            VARCHAR(32) NOT NULL COMMENT '主键',
    group_id      VARCHAR(32) NOT NULL COMMENT '组ID',
    resource_type VARCHAR(32) NOT NULL COMMENT '资源类型',
    resource_id   VARCHAR(32) NOT NULL COMMENT '资源ID',
    permission    VARCHAR(32) DEFAULT 'read' COMMENT '权限 read/write/admin',
    create_time   DATETIME NOT NULL,
    update_time   DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 五、动态数据抽取任务
-- ============================================================

CREATE TABLE IF NOT EXISTS data_extract_job (
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    job_name        VARCHAR(128) NOT NULL COMMENT '任务名称',
    group_id        VARCHAR(32) NOT NULL COMMENT '所属用户组',
    source_type     VARCHAR(32) NOT NULL COMMENT '源类型 catalog/jdbc',
    source_catalog  VARCHAR(64) COMMENT '源Catalog名',
    source_database VARCHAR(64) COMMENT '源库名',
    source_table    VARCHAR(128) NOT NULL COMMENT '源表名',
    target_database VARCHAR(64) NOT NULL COMMENT 'Doris目标库',
    target_table    VARCHAR(128) NOT NULL COMMENT 'Doris目标表',
    extract_type    VARCHAR(32) DEFAULT 'full' COMMENT '抽取类型 full/incremental',
    increment_field VARCHAR(64) COMMENT '增量字段',
    increment_value VARCHAR(128) COMMENT '增量水位线值',
    where_condition VARCHAR(512) COMMENT 'WHERE条件',
    field_mapping   TEXT COMMENT '字段映射JSON',
    cron_expression VARCHAR(64) COMMENT '调度Cron',
    status          VARCHAR(2) DEFAULT '1' COMMENT '0停用 1启用',
    create_by       VARCHAR(32) COMMENT '创建人',
    create_time     DATETIME NOT NULL,
    update_time     DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS data_extract_job_log (
    id              VARCHAR(32) NOT NULL,
    job_id          VARCHAR(32) NOT NULL COMMENT '任务ID',
    trigger_type    VARCHAR(16) COMMENT 'manual/schedule',
    status          VARCHAR(2) COMMENT '0失败 1成功 2运行中',
    affected_rows   BIGINT DEFAULT 0,
    error_msg       VARCHAR(1024),
    execute_batch   VARCHAR(32),
    start_time      DATETIME,
    end_time        DATETIME,
    duration_ms     BIGINT DEFAULT 0,
    KEY idx_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 六、数据血缘
-- ============================================================

CREATE TABLE IF NOT EXISTS data_lineage (
    id              VARCHAR(32) NOT NULL,
    source_catalog  VARCHAR(64) COMMENT '源Catalog',
    source_database VARCHAR(64) NOT NULL COMMENT '源库',
    source_table    VARCHAR(128) NOT NULL COMMENT '源表',
    target_database VARCHAR(64) NOT NULL COMMENT '目标库',
    target_table    VARCHAR(128) NOT NULL COMMENT '目标表',
    transform_sql   TEXT COMMENT '转换SQL',
    job_id          VARCHAR(32) COMMENT '关联任务ID',
    job_type        VARCHAR(32) COMMENT 'extract/quality/realtime',
    group_id        VARCHAR(32) COMMENT '所属用户组',
    create_time     DATETIME NOT NULL,
    KEY idx_source (source_table),
    KEY idx_target (target_table)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS data_lineage_field (
    id              VARCHAR(32) NOT NULL,
    lineage_id      VARCHAR(32) NOT NULL COMMENT '血缘记录ID',
    source_database VARCHAR(64) NOT NULL,
    source_table    VARCHAR(128) NOT NULL,
    source_field    VARCHAR(128) NOT NULL COMMENT '源字段',
    target_database VARCHAR(64) NOT NULL,
    target_table    VARCHAR(128) NOT NULL,
    target_field    VARCHAR(128) NOT NULL COMMENT '目标字段',
    transform_expr  VARCHAR(512) COMMENT '转换表达式',
    field_type      VARCHAR(32) COMMENT '字段类型 direct/derived',
    KEY idx_lineage_id (lineage_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 七、表结构管理
-- ============================================================

CREATE TABLE IF NOT EXISTS meta_table_schema (
    id              VARCHAR(32) NOT NULL,
    group_id        VARCHAR(32) COMMENT '所属用户组',
    source_catalog  VARCHAR(64) COMMENT '源Catalog',
    source_database VARCHAR(64) NOT NULL COMMENT '源库名',
    source_table    VARCHAR(128) NOT NULL COMMENT '源表名',
    target_database VARCHAR(64) NOT NULL COMMENT 'Doris目标库',
    target_table    VARCHAR(128) NOT NULL COMMENT 'Doris目标表',
    schema_json     TEXT COMMENT '表结构快照JSON',
    column_count    INT DEFAULT 0 COMMENT '字段数',
    last_sync_time  DATETIME COMMENT '上次同步时间',
    status          VARCHAR(2) DEFAULT '1' COMMENT '0停用 1启用',
    create_time     DATETIME NOT NULL,
    update_time     DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS schema_diff_log (
    id              VARCHAR(32) NOT NULL,
    table_schema_id VARCHAR(32) NOT NULL COMMENT '关联meta_table_schema',
    diff_type       VARCHAR(16) NOT NULL COMMENT 'ADD/MODIFY/DELETE',
    column_name     VARCHAR(128) NOT NULL COMMENT '变更字段',
    old_type        VARCHAR(128) COMMENT '旧类型',
    new_type        VARCHAR(128) COMMENT '新类型',
    old_comment     VARCHAR(512) COMMENT '旧注释',
    new_comment     VARCHAR(512) COMMENT '新注释',
    diff_detail     VARCHAR(512) COMMENT '变更详情',
    resolved        VARCHAR(2) DEFAULT '0' COMMENT '0未处理 1已处理 2已忽略',
    resolved_by     VARCHAR(32) COMMENT '处理人',
    resolved_time   DATETIME COMMENT '处理时间',
    create_time     DATETIME NOT NULL,
    KEY idx_schema_id (table_schema_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 八、调度配置表
-- ============================================================

CREATE TABLE IF NOT EXISTS sys_job_config (
    id               VARCHAR(32) NOT NULL,
    job_name         VARCHAR(128) NOT NULL COMMENT '任务名称',
    handler          VARCHAR(128) NOT NULL COMMENT '任务Handler',
    module           VARCHAR(64) NOT NULL COMMENT '所属模块',
    cron_expr        VARCHAR(128) NOT NULL COMMENT 'Cron表达式',
    job_param        VARCHAR(512) COMMENT '任务参数',
    description      VARCHAR(256) COMMENT '任务描述',
    ds_workflow_code BIGINT COMMENT 'DolphinScheduler 工作流 Code',
    ds_schedule_id   BIGINT COMMENT 'DolphinScheduler 调度 ID',
    status           VARCHAR(2) DEFAULT '0' COMMENT '0=停止 1=运行中',
    group_id         VARCHAR(32) COMMENT '所属用户组',
    create_time      DATETIME NOT NULL,
    update_time      DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 九、数据开发：任务定义 / 工作流 / 任务依赖
-- ============================================================

CREATE TABLE IF NOT EXISTS dev_task (
    id             VARCHAR(32) NOT NULL,
    task_name      VARCHAR(128) NOT NULL COMMENT '任务名称',
    task_type      VARCHAR(32) NOT NULL COMMENT 'FLINK/SQL/SHELL/PYTHON/CUSTOM',
    task_params    TEXT COMMENT 'DS taskParams JSON',
    description    VARCHAR(512) COMMENT '任务描述',
    source_tables  TEXT COMMENT '源表FQN JSON数组',
    target_tables  TEXT COMMENT '目标表FQN JSON数组',
    ds_task_code   BIGINT COMMENT 'DS 任务定义 Code',
    ds_task_version INT COMMENT 'DS 任务定义版本',
    status         VARCHAR(16) DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED',
    create_time    DATETIME NOT NULL,
    update_time    DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dev_workflow (
    id               VARCHAR(32) NOT NULL,
    workflow_name   VARCHAR(128) NOT NULL COMMENT '工作流名称',
    description     VARCHAR(512) COMMENT '描述',
    cron_expr       VARCHAR(128) COMMENT 'Cron调度表达式',
    ds_workflow_code BIGINT COMMENT 'DS 工作流 Code',
    ds_schedule_id  BIGINT COMMENT 'DS 调度 ID',
    status          VARCHAR(16) DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/ONLINE/OFFLINE',
    create_time     DATETIME NOT NULL,
    update_time     DATETIME NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dev_workflow_task (
    id               VARCHAR(32) NOT NULL,
    workflow_id      VARCHAR(32) NOT NULL COMMENT '工作流ID',
    task_id          VARCHAR(32) NOT NULL COMMENT '任务ID',
    ds_task_code     BIGINT COMMENT 'DS 任务Code',
    upstream_task_ids TEXT COMMENT '上游任务ID JSON数组',
    dependency_type  VARCHAR(16) DEFAULT 'AUTO' COMMENT 'AUTO/MANUAL/CONFIRMED',
    create_time      DATETIME NOT NULL,
    update_time      DATETIME,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 十、初始化数据
-- ============================================================

INSERT INTO sys_user (id, username, password, nickname, status, create_time, update_time)
VALUES ('1', 'admin', '$2a$10$placeholder', '管理员', '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE username='admin';

INSERT INTO sys_role (id, role_name, role_code, status, create_time, update_time)
VALUES ('1', '超级管理员', 'admin', '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE role_name='超级管理员';

INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, sort_order, status, create_time, update_time)
VALUES ('1', '0', '系统管理', '/system', 'M', 1, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='系统管理';
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort_order, status, create_time, update_time)
VALUES ('2', '1', '用户管理', 'users', 'system/user/index', 'C', 'system:user:list', 1, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='用户管理';
INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, sort_order, status, create_time, update_time)
VALUES ('3', '0', '数据质量', '/quality', 'M', 2, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='数据质量';
INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, sort_order, status, create_time, update_time)
VALUES ('4', '0', '实时任务', '/realtime', 'M', 3, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='实时任务';
INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, sort_order, status, create_time, update_time)
VALUES ('5', '0', '数据集成', '/integration', 'M', 4, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='数据集成';
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort_order, status, create_time, update_time)
VALUES ('6', '5', '数据湖Catalog', 'catalog', 'integration/DataLake', 'C', 'integration:catalog:list', 1, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='数据湖Catalog';
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort_order, status, create_time, update_time)
VALUES ('7', '5', '数据抽取', 'extract', 'integration/ExtractJob', 'C', 'integration:extract:list', 2, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='数据抽取';
INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, sort_order, status, create_time, update_time)
VALUES ('8', '0', '数据血缘', '/lineage', 'M', 5, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='数据血缘';
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort_order, status, create_time, update_time)
VALUES ('9', '8', '血缘图谱', 'graph', 'lineage/LineageGraph', 'C', 'lineage:graph:view', 1, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='血缘图谱';
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort_order, status, create_time, update_time)
VALUES ('10', '1', '用户组管理', 'groups', 'system/GroupList', 'C', 'system:group:list', 2, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='用户组管理';
INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, sort_order, status, create_time, update_time)
VALUES ('11', '0', '表结构管理', '/schema', 'M', 6, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='表结构管理';
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort_order, status, create_time, update_time)
VALUES ('12', '11', '表结构对比', 'compare', 'schema/SchemaCompare', 'C', 'schema:compare:view', 1, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='表结构对比';
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort_order, status, create_time, update_time)
VALUES ('13', '11', '自动建表', 'auto-ddl', 'schema/AutoDdl', 'C', 'schema:ddl:view', 2, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='自动建表';
INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, sort_order, status, create_time, update_time)
VALUES ('14', '0', '调度管理', '/schedule', 'M', 7, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='调度管理';
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort_order, status, create_time, update_time)
VALUES ('15', '14', '任务调度', 'jobs', 'schedule/JobConfig', 'C', 'schedule:job:view', 1, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='任务调度';
INSERT INTO sys_menu (id, parent_id, menu_name, path, menu_type, sort_order, status, create_time, update_time)
VALUES ('16', '0', '数据开发', '/dev', 'M', 8, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='数据开发';
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort_order, status, create_time, update_time)
VALUES ('17', '16', '任务管理', 'task', 'dev/DevTask', 'C', 'dev:task:view', 1, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='任务管理';
INSERT INTO sys_menu (id, parent_id, menu_name, path, component, menu_type, permission, sort_order, status, create_time, update_time)
VALUES ('18', '16', '工作流编排', 'workflow', 'dev/DevWorkflow', 'C', 'dev:workflow:view', 2, '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name='工作流编排';

INSERT INTO quality_check_rule (id, rule_name, rule_type, rule_level, rule_sql, status, create_time, update_time)
VALUES ('1', '订单空值检查', 'completeness', 'P1',
        'SELECT COUNT(CASE WHEN order_id IS NULL THEN 1 END), COUNT(*) FROM ods_orders',
        '1', NOW(), NOW())
ON DUPLICATE KEY UPDATE rule_name='订单空值检查';
