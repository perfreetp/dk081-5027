-- =====================================================
-- 住房公积金异地转移接续协同服务平台 数据库DDL脚本
-- 数据库版本：MySQL 8.0+
-- 创建时间：2026-06-17
-- =====================================================

CREATE DATABASE IF NOT EXISTS hf_transfer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE hf_transfer;

-- =====================================================
-- 1. 地区信息表 - 存储各公积金管理中心基本信息
-- =====================================================
DROP TABLE IF EXISTS hf_region_info;
CREATE TABLE hf_region_info (
    id              BIGINT          NOT NULL COMMENT '主键ID',
    region_code     VARCHAR(12)     NOT NULL COMMENT '地区编码(行政区划代码)',
    region_name     VARCHAR(64)     NOT NULL COMMENT '地区名称',
    center_name     VARCHAR(128)    NOT NULL COMMENT '公积金中心全称',
    center_short    VARCHAR(64)     NOT NULL COMMENT '中心简称',
    province_code   VARCHAR(6)      NOT NULL COMMENT '省编码',
    province_name   VARCHAR(32)     NOT NULL COMMENT '省名称',
    city_code       VARCHAR(12)     NOT NULL COMMENT '市编码',
    city_name       VARCHAR(32)     NOT NULL COMMENT '市名称',
    contact_person  VARCHAR(32)     COMMENT '联系人',
    contact_phone   VARCHAR(32)     COMMENT '联系电话',
    contact_email   VARCHAR(64)     COMMENT '联系邮箱',
    api_endpoint    VARCHAR(256)    COMMENT '协同接口地址',
    api_secret      VARCHAR(128)    COMMENT '接口密钥标识',
    status          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    sort_order      INT             DEFAULT 0 COMMENT '排序',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    create_by       VARCHAR(64)     COMMENT '创建人',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by       VARCHAR(64)     COMMENT '更新人',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_region_code (region_code),
    KEY idx_province (province_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公积金中心地区信息表';


-- =====================================================
-- 2. 地区受理规则表 - 不同地区的业务受理规则配置
-- =====================================================
DROP TABLE IF EXISTS hf_region_rule;
CREATE TABLE hf_region_rule (
    id                  BIGINT          NOT NULL COMMENT '主键ID',
    region_code         VARCHAR(12)     NOT NULL COMMENT '地区编码',
    rule_type           TINYINT         NOT NULL COMMENT '规则类型：1转出规则 2转入规则 3通用规则',
    rule_name           VARCHAR(128)    NOT NULL COMMENT '规则名称',
    min_contribution_months INT          COMMENT '最低缴存月数要求',
    require_id_card_verify TINYINT      NOT NULL DEFAULT 1 COMMENT '是否需要身份证核验：1是 0否',
    require_household_cert TINYINT      NOT NULL DEFAULT 0 COMMENT '是否需要户籍证明：1是 0否',
    require_work_cert   TINYINT         NOT NULL DEFAULT 0 COMMENT '是否需要工作证明：1是 0否',
    require_termination_cert TINYINT    NOT NULL DEFAULT 1 COMMENT '是否需要解除劳动关系证明：1是 0否',
    allow_partial_transfer TINYINT      NOT NULL DEFAULT 0 COMMENT '是否允许部分转移：1是 0否',
    max_transfer_amount DECIMAL(18,2)   COMMENT '单笔最高转移金额(元)',
    min_transfer_amount DECIMAL(18,2)   COMMENT '单笔最低转移金额(元)',
    processing_deadline INT             NOT NULL DEFAULT 3 COMMENT '处理时限(工作日)',
    confirmation_deadline INT           NOT NULL DEFAULT 5 COMMENT '确认时限(工作日)',
    notify_email        VARCHAR(256)    COMMENT '通知邮箱(多个逗号分隔)',
    rule_content        TEXT            COMMENT '规则补充说明',
    rule_version        INT             NOT NULL DEFAULT 1 COMMENT '规则版本',
    effective_date      DATE            COMMENT '生效日期',
    expiry_date         DATE            COMMENT '失效日期',
    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    create_by           VARCHAR(64)     COMMENT '创建人',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by           VARCHAR(64)     COMMENT '更新人',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_region_type (region_code, rule_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地区业务受理规则表';


-- =====================================================
-- 3. 转移申请表 - 核心业务主表
-- =====================================================
DROP TABLE IF EXISTS hf_transfer_application;
CREATE TABLE hf_transfer_application (
    id                      BIGINT          NOT NULL COMMENT '主键ID',
    application_no          VARCHAR(32)     NOT NULL COMMENT '申请编号(系统生成，全局唯一)',
    channel_type            TINYINT         NOT NULL COMMENT '申请渠道：1网厅 2APP 3微信小程序 4支付宝小程序 5柜台 6自助终端 7API 8银行',
    channel_order_no        VARCHAR(64)     COMMENT '渠道侧业务流水号',

    applicant_name          VARCHAR(64)     NOT NULL COMMENT '申请人姓名',
    id_card_type            TINYINT         NOT NULL DEFAULT 1 COMMENT '证件类型：1身份证 2护照 3军官证',
    id_card_no              VARCHAR(32)     NOT NULL COMMENT '证件号码(加密存储)',
    mobile_phone            VARCHAR(16)     NOT NULL COMMENT '手机号码',

    transfer_out_region     VARCHAR(12)     NOT NULL COMMENT '转出地编码',
    transfer_out_center     VARCHAR(128)    COMMENT '转出中心名称',
    transfer_out_account    VARCHAR(32)     COMMENT '转出地个人账号',

    transfer_in_region      VARCHAR(12)     NOT NULL COMMENT '转入地编码',
    transfer_in_center      VARCHAR(128)    COMMENT '转入中心名称',
    transfer_in_account     VARCHAR(32)     COMMENT '转入地个人账号',

    transfer_type           TINYINT         NOT NULL DEFAULT 1 COMMENT '转移类型：1异地转移接续 2同城转移 3跨机构转移',
    transfer_reason         TINYINT         COMMENT '转移原因：1工作调动 2户籍迁移 3离职 4退休 5其他',
    transfer_amount         DECIMAL(18,2)   COMMENT '申请转移金额(元)',
    actual_transfer_amount  DECIMAL(18,2)   COMMENT '实际转移金额(元)',

    household_type          TINYINT         COMMENT '户籍类型：1本地 2外地',
    employment_status       TINYINT         COMMENT '就业状态：1在职 2离职 3退休',

    applicant_account_name  VARCHAR(64)     COMMENT '收款账户户名',
    applicant_bank_card     VARCHAR(32)     COMMENT '收款银行卡号',
    applicant_bank_name     VARCHAR(128)    COMMENT '收款开户行',

    application_status      INT             NOT NULL COMMENT '申请状态：10待审核 20规则通过 25规则不通过 28重复申请驳回 30转出待受理 40转出已确认 45转出退回 50转入待确认 60转入已确认 70待补正材料 80已办结 90已取消 95已终止',
    current_node            VARCHAR(32)     COMMENT '当前节点编码',
    current_region          VARCHAR(12)     COMMENT '当前处理地区编码',

    submit_time             DATETIME        COMMENT '提交时间',
    audit_time              DATETIME        COMMENT '审核时间',
    transfer_out_time       DATETIME        COMMENT '转出确认时间',
    transfer_in_time        DATETIME        COMMENT '转入确认时间',
    complete_time           DATETIME        COMMENT '办结时间',
    expected_complete_time  DATETIME        COMMENT '预计办结时间',

    reject_count            INT             NOT NULL DEFAULT 0 COMMENT '退回次数',
    supplement_count        INT             NOT NULL DEFAULT 0 COMMENT '补正次数',
    urge_count              INT             NOT NULL DEFAULT 0 COMMENT '催办次数',

    is_duplicate            TINYINT         NOT NULL DEFAULT 0 COMMENT '是否重复申请：0否 1是',
    duplicate_app_no        VARCHAR(32)     COMMENT '关联的重复申请编号',
    is_conflict             TINYINT         NOT NULL DEFAULT 0 COMMENT '是否冲突申请：0否 1是',
    conflict_reason         VARCHAR(256)    COMMENT '冲突原因说明',

    remark                  VARCHAR(512)    COMMENT '备注',
    operator_id             VARCHAR(64)     COMMENT '当前处理人ID',
    operator_name           VARCHAR(64)     COMMENT '当前处理人姓名',

    deleted                 TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    create_by               VARCHAR(64)     COMMENT '创建人',
    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by               VARCHAR(64)     COMMENT '更新人',
    update_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_application_no (application_no),
    KEY idx_channel_order (channel_order_no),
    KEY idx_id_card (id_card_no),
    KEY idx_transfer_out (transfer_out_region, application_status),
    KEY idx_transfer_in (transfer_in_region, application_status),
    KEY idx_status (application_status),
    KEY idx_submit_time (submit_time),
    KEY idx_complete_time (complete_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公积金异地转移申请表';


-- =====================================================
-- 4. 申请材料表 - 申请提交的各类材料信息
-- =====================================================
DROP TABLE IF EXISTS hf_application_material;
CREATE TABLE hf_application_material (
    id                  BIGINT          NOT NULL COMMENT '主键ID',
    application_id      BIGINT          NOT NULL COMMENT '关联申请ID',
    application_no      VARCHAR(32)     NOT NULL COMMENT '申请编号',
    material_type       VARCHAR(32)     NOT NULL COMMENT '材料类型编码：ID_CARD身份证 HOUSEHOLD户籍 TERMINATION解除劳动关系 WORK_PROOF工作证明 BANK_CARD银行卡 OTHER其他',
    material_name       VARCHAR(128)    NOT NULL COMMENT '材料名称',
    file_id             VARCHAR(64)     COMMENT '文件存储ID',
    file_name           VARCHAR(256)    COMMENT '文件名',
    file_url            VARCHAR(512)    COMMENT '文件访问地址',
    file_size           BIGINT          COMMENT '文件大小(字节)',
    file_format         VARCHAR(16)     COMMENT '文件格式(jpg/pdf/doc等)',
    verify_status       TINYINT         NOT NULL DEFAULT 0 COMMENT '核验状态：0待核验 1核验通过 2核验不通过',
    verify_time         DATETIME        COMMENT '核验时间',
    verify_remark       VARCHAR(256)    COMMENT '核验备注',
    material_round      INT             NOT NULL DEFAULT 1 COMMENT '材料轮次(第几次提交)',
    is_effective        TINYINT         NOT NULL DEFAULT 1 COMMENT '是否有效：1是 0否',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    create_by           VARCHAR(64)     COMMENT '创建人',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by           VARCHAR(64)     COMMENT '更新人',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_application (application_id),
    KEY idx_application_no (application_no),
    KEY idx_material_type (material_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申请材料明细表';


-- =====================================================
-- 5. 跨中心协同任务表 - 转出/转入协同任务
-- =====================================================
DROP TABLE IF EXISTS hf_collaboration_task;
CREATE TABLE hf_collaboration_task (
    id                      BIGINT          NOT NULL COMMENT '主键ID',
    task_no                 VARCHAR(32)     NOT NULL COMMENT '协同任务编号',
    application_id          BIGINT          NOT NULL COMMENT '关联申请ID',
    application_no          VARCHAR(32)     NOT NULL COMMENT '申请编号',
    task_type               TINYINT         NOT NULL COMMENT '任务类型：1转出确认 2转入确认 3信息补正 4退件复核',
    task_direction          TINYINT         NOT NULL COMMENT '任务方向：1转出→转入 2转入→转出',
    source_region           VARCHAR(12)     NOT NULL COMMENT '发起方地区编码',
    target_region           VARCHAR(12)     NOT NULL COMMENT '接收方地区编码',

    task_status             INT             NOT NULL COMMENT '任务状态：10待处理 20处理中 30已确认 40已退回 50已超时 60已催办 70已完成 80已关闭',
    priority                TINYINT         NOT NULL DEFAULT 2 COMMENT '优先级：1高 2中 3低',

    assign_time             DATETIME        COMMENT '任务分派时间',
    deadline_time           DATETIME        COMMENT '任务截止时间',
    first_urge_time         DATETIME        COMMENT '首次催办时间',
    last_urge_time          DATETIME        COMMENT '最后催办时间',
    confirm_time            DATETIME        COMMENT '确认/处理时间',

    confirm_result          TINYINT         COMMENT '处理结果：1同意 2退回 3补正',
    confirm_remark          VARCHAR(512)    COMMENT '处理说明',
    reject_reason_code      VARCHAR(32)     COMMENT '退回原因编码',
    reject_reason_name      VARCHAR(128)    COMMENT '退回原因名称',

    supplement_items        TEXT            COMMENT '需补正材料清单(JSON格式)',

    sync_status             TINYINT         NOT NULL DEFAULT 0 COMMENT '推送状态：0待推送 1推送成功 2推送失败',
    sync_attempts           INT             NOT NULL DEFAULT 0 COMMENT '推送重试次数',
    sync_last_time          DATETIME        COMMENT '最后推送时间',
    sync_response           VARCHAR(512)    COMMENT '推送返回结果',

    remark                  VARCHAR(512)    COMMENT '备注',
    operator_id             VARCHAR(64)     COMMENT '处理人ID',
    operator_name           VARCHAR(64)     COMMENT '处理人姓名',

    deleted                 TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    create_by               VARCHAR(64)     COMMENT '创建人',
    create_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by               VARCHAR(64)     COMMENT '更新人',
    update_time             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_application (application_id),
    KEY idx_application_no (application_no),
    KEY idx_target_status (target_region, task_status),
    KEY idx_deadline (deadline_time),
    KEY idx_task_type (task_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跨中心协同任务表';


-- =====================================================
-- 6. 标准化退件原因表
-- =====================================================
DROP TABLE IF EXISTS hf_reject_reason;
CREATE TABLE hf_reject_reason (
    id                  BIGINT          NOT NULL COMMENT '主键ID',
    reason_code         VARCHAR(32)     NOT NULL COMMENT '原因编码',
    reason_category     TINYINT         NOT NULL COMMENT '原因分类：1材料类 2信息类 3规则类 4账户类 5其他',
    reason_name         VARCHAR(128)    NOT NULL COMMENT '退件原因名称',
    reason_detail       VARCHAR(512)    COMMENT '原因详细说明',
    supplement_guide    VARCHAR(512)    COMMENT '补正指引',
    applicable_scene    TINYINT         NOT NULL DEFAULT 0 COMMENT '适用环节：0全部 1规则校验 2转出审核 3转入审核',
    is_need_supplement  TINYINT         NOT NULL DEFAULT 1 COMMENT '是否可补正：1是 0否(直接终止)',
    status              TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    sort_order          INT             DEFAULT 0 COMMENT '排序',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    create_by           VARCHAR(64)     COMMENT '创建人',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by           VARCHAR(64)     COMMENT '更新人',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reason_code (reason_code),
    KEY idx_category (reason_category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准化退件原因表';


-- =====================================================
-- 7. 催办记录表 - 超时自动催办记录
-- =====================================================
DROP TABLE IF EXISTS hf_urge_record;
CREATE TABLE hf_urge_record (
    id                  BIGINT          NOT NULL COMMENT '主键ID',
    task_id             BIGINT          NOT NULL COMMENT '协同任务ID',
    task_no             VARCHAR(32)     NOT NULL COMMENT '协同任务编号',
    application_id      BIGINT          COMMENT '关联申请ID',
    application_no      VARCHAR(32)     COMMENT '申请编号',
    urge_type           TINYINT         NOT NULL COMMENT '催办类型：1系统自动 2人工发起',
    urge_level          TINYINT         NOT NULL DEFAULT 1 COMMENT '催办级别：1一般提醒 2重要提醒 3加急',
    target_region       VARCHAR(12)     COMMENT '被催办地区',
    urge_content        VARCHAR(512)    NOT NULL COMMENT '催办内容',
    notify_channel      VARCHAR(64)     COMMENT '通知渠道(多个逗号分隔)：EMAIL短信 系统消息',
    notify_status       TINYINT         NOT NULL DEFAULT 0 COMMENT '通知状态：0待发送 1发送成功 2发送失败',
    notify_response     VARCHAR(256)    COMMENT '通知返回结果',
    urge_operator_id    VARCHAR(64)     COMMENT '催办人ID(人工催办时)',
    urge_operator_name  VARCHAR(64)     COMMENT '催办人姓名',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '催办时间',
    PRIMARY KEY (id),
    KEY idx_task (task_id),
    KEY idx_application (application_id),
    KEY idx_target_region (target_region),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='催办记录表';


-- =====================================================
-- 8. 补正材料流转表 - 补正提交与审核记录
-- =====================================================
DROP TABLE IF EXISTS hf_supplement_record;
CREATE TABLE hf_supplement_record (
    id                  BIGINT          NOT NULL COMMENT '主键ID',
    application_id      BIGINT          NOT NULL COMMENT '关联申请ID',
    application_no      VARCHAR(32)     NOT NULL COMMENT '申请编号',
    task_id             BIGINT          COMMENT '协同任务ID',
    supplement_round    INT             NOT NULL COMMENT '补正轮次',
    request_region      VARCHAR(12)     NOT NULL COMMENT '要求补正的地区编码',
    request_operator_id VARCHAR(64)     COMMENT '要求补正人ID',
    request_operator_name VARCHAR(64)   COMMENT '要求补正人姓名',
    request_time        DATETIME        NOT NULL COMMENT '补正要求发出时间',
    request_remark      VARCHAR(512)    COMMENT '补正要求说明',
    required_items      TEXT            COMMENT '需补正材料清单(JSON格式)',

    submit_time         DATETIME        COMMENT '补正材料提交时间',
    submit_operator_id  VARCHAR(64)     COMMENT '提交人ID',
    submit_operator_name VARCHAR(64)    COMMENT '提交人姓名',

    audit_time          DATETIME        COMMENT '审核时间',
    audit_operator_id   VARCHAR(64)     COMMENT '审核人ID',
    audit_operator_name VARCHAR(64)     COMMENT '审核人姓名',
    audit_result        TINYINT         COMMENT '审核结果：1通过 2不通过(仍需补正)',
    audit_remark        VARCHAR(512)    COMMENT '审核备注',

    supplement_status   TINYINT         NOT NULL COMMENT '补正状态：10待提交 20已提交待审核 30审核通过 40审核不通过',
    deadline_time       DATETIME        COMMENT '补正截止时间',

    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_application (application_id),
    KEY idx_application_no (application_no),
    KEY idx_status (supplement_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补正材料流转记录表';


-- =====================================================
-- 9. 申请状态流转日志表 - 记录每一次状态变更
-- =====================================================
DROP TABLE IF EXISTS hf_application_status_log;
CREATE TABLE hf_application_status_log (
    id                  BIGINT          NOT NULL COMMENT '主键ID',
    application_id      BIGINT          NOT NULL COMMENT '关联申请ID',
    application_no      VARCHAR(32)     NOT NULL COMMENT '申请编号',
    from_status         INT             COMMENT '原状态',
    from_status_name    VARCHAR(32)     COMMENT '原状态名称',
    to_status           INT             NOT NULL COMMENT '新状态',
    to_status_name      VARCHAR(32)     NOT NULL COMMENT '新状态名称',
    operate_region      VARCHAR(12)     COMMENT '操作地区',
    operator_id         VARCHAR(64)     COMMENT '操作人ID',
    operator_name       VARCHAR(64)     COMMENT '操作人姓名',
    operate_type        VARCHAR(32)     COMMENT '操作类型编码',
    operate_desc        VARCHAR(128)    NOT NULL COMMENT '操作说明',
    operate_remark      VARCHAR(512)    COMMENT '操作备注',
    task_id             BIGINT          COMMENT '关联任务ID',
    client_ip           VARCHAR(64)     COMMENT '客户端IP',
    user_agent          VARCHAR(512)    COMMENT '用户代理',
    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_application (application_id),
    KEY idx_application_no (application_no),
    KEY idx_create_time (create_time),
    KEY idx_operator (operator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申请状态流转日志表';


-- =====================================================
-- 10. 操作审计日志表 - 全过程操作留痕
-- =====================================================
DROP TABLE IF EXISTS hf_operation_log;
CREATE TABLE hf_operation_log (
    id                  BIGINT          NOT NULL COMMENT '主键ID',
    log_type            VARCHAR(32)     NOT NULL COMMENT '日志类型：APPLICATION申请 TASK任务 RULE规则 QUERY查询 SYSTEM系统',
    biz_type            VARCHAR(64)     NOT NULL COMMENT '业务类型：CREATE创建 UPDATE修改 DELETE删除 AUDIT审核 CONFIRM确认 REJECT退回 URGE催办 SUPPLEMENT补正 QUERY查询 EXPORT导出',
    biz_id              VARCHAR(64)     COMMENT '业务主键ID',
    biz_no              VARCHAR(64)     COMMENT '业务编号',
    module_name         VARCHAR(64)     COMMENT '模块名称',
    method_name         VARCHAR(256)    COMMENT '方法名称',
    operate_desc        VARCHAR(512)    NOT NULL COMMENT '操作描述',

    operator_id         VARCHAR(64)     COMMENT '操作人ID',
    operator_name       VARCHAR(64)     COMMENT '操作人姓名',
    operator_org_code   VARCHAR(12)     COMMENT '操作人机构编码',
    operator_org_name   VARCHAR(128)    COMMENT '操作人机构名称',

    request_method      VARCHAR(16)     COMMENT 'HTTP请求方式',
    request_url         VARCHAR(512)    COMMENT '请求URL',
    request_params      TEXT            COMMENT '请求参数(JSON)',
    response_result     TEXT            COMMENT '响应结果(JSON)',
    execute_result      TINYINT         NOT NULL COMMENT '执行结果：1成功 0失败',
    error_msg           VARCHAR(1024)   COMMENT '错误信息',
    cost_time           BIGINT          COMMENT '耗时(毫秒)',

    client_ip           VARCHAR(64)     COMMENT '客户端IP',
    client_location     VARCHAR(128)    COMMENT '客户端位置',
    user_agent          VARCHAR(512)    COMMENT '用户代理',

    deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0否 1是',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_log_type (log_type),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_operator (operator_id, create_time),
    KEY idx_create_time (create_time),
    KEY idx_execute (execute_result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';


-- =====================================================
-- 11. 初始化数据
-- =====================================================

-- 初始化标准化退件原因
INSERT INTO hf_reject_reason (id, reason_code, reason_category, reason_name, reason_detail, supplement_guide, applicable_scene, is_need_supplement, status, sort_order) VALUES
(1,  'R001', 1, '身份证照片不清晰',         '提交的身份证扫描件或照片模糊、反光、不完整，无法核验身份信息。',       '请重新上传清晰完整的身份证正反面照片，确保证件号码、姓名等关键信息清晰可辨。', 0, 1, 1, 1),
(2,  'R002', 1, '缺少解除劳动关系证明',     '未提交与原单位解除劳动关系的证明材料。',                                 '请上传原单位出具的解除劳动关系证明书或离职证明文件。',                     1, 1, 1, 2),
(3,  'R003', 1, '户籍证明材料不完整',       '户籍迁移证明或户口本材料不完整，无法确认户籍信息。',                       '请上传完整的户口本户主页、本人页，或公安机关出具的户籍迁移证明。',           1, 1, 1, 3),
(4,  'R004', 1, '银行卡信息与身份不符',     '提交的收款银行卡户名与申请人姓名不一致。',                                 '请核对银行卡信息，使用申请人本人名下的银行卡。',                           3, 1, 1, 4),
(5,  'R005', 2, '身份信息核验不通过',       '申请人身份信息与公安系统数据比对不一致。',                                 '请核对身份证号码和姓名是否准确，如已变更请先办理身份信息变更。',             1, 0, 1, 5),
(6,  'R006', 2, '转出地个人账号不存在',     '在转出地中心系统中未查询到该个人公积金账号。',                             '请核实转出地公积金账号是否正确，或与转出地中心联系确认。',                   2, 1, 1, 6),
(7,  'R007', 2, '转入地未开户',             '转入地中心系统中无申请人公积金账户，需先开立账户。',                       '请先在转入地公积金中心开立个人住房公积金账户后再申请转移。',                 3, 1, 1, 7),
(8,  'R008', 3, '未达到最低缴存年限要求',   '申请人在转出地的连续缴存时长未达到规定的最低月数要求。',                   '请继续缴存至满足最低缴存期限后再提出申请。',                               1, 0, 1, 8),
(9,  'R009', 3, '账户处于封存状态未满期限', '公积金账户封存未满规定期限，暂不允许办理转移。',                           '请在账户封存期满后再申请办理异地转移。',                                   2, 0, 1, 9),
(10, 'R010', 3, '存在在途未办结业务',       '申请人存在其他正在办理中的公积金业务，需待办结后再申请。',                 '请先办结当前在途业务后再提交转移申请。',                                   0, 0, 1, 10),
(11, 'R011', 4, '转出账户存在冻结情形',     '转出地公积金账户因贷款、司法冻结等原因无法办理转移。',                     '请先处理账户冻结问题，解冻后再申请转移。',                                 2, 0, 1, 11),
(12, 'R012', 4, '存在未结清公积金贷款',     '申请人在转出地存在未结清的住房公积金贷款。',                               '请结清原公积金贷款后再申请转移。',                                         2, 0, 1, 12),
(13, 'R013', 5, '重复申请',                 '系统检测到同一申请人存在已提交且未办结的相同转移申请。',                   '请勿重复提交申请，可通过查询功能查看已有申请的办理进度。',                   0, 0, 1, 13),
(14, 'R014', 3, '违反属地化管理规定',       '根据转入地业务规则，该申请不符合属地化受理条件。',                         '请参阅转入地中心业务指南，确认符合条件后再申请。',                         3, 0, 1, 14),
(15, 'R015', 5, '其他原因',                 '其他未列明的特殊原因，详见备注说明。',                                     '请根据具体退件说明补充相关材料或联系中心客服。',                           0, 1, 1, 99);

-- 初始化部分地区示例数据
INSERT INTO hf_region_info (id, region_code, region_name, center_name, center_short, province_code, province_name, city_code, city_name, contact_person, contact_phone, status, sort_order) VALUES
(1,  '110000', '北京市', '北京住房公积金管理中心',         '北京中心', '110000', '北京市', '110000', '北京市', '张主任', '010-67235558', 1, 1),
(2,  '310000', '上海市', '上海市住房公积金管理中心',       '上海中心', '310000', '上海市', '310000', '上海市', '李主任', '021-12329',   1, 2),
(3,  '440100', '广州市', '广州住房公积金管理中心',         '广州中心', '440000', '广东省', '440100', '广州市', '王主任', '020-12345',   1, 3),
(4,  '440300', '深圳市', '深圳市住房公积金管理中心',       '深圳中心', '440000', '广东省', '440300', '深圳市', '刘主任', '0755-12329',  1, 4),
(5,  '330100', '杭州市', '杭州住房公积金管理中心',         '杭州中心', '330000', '浙江省', '330100', '杭州市', '陈主任', '0571-12329',  1, 5),
(6,  '320100', '南京市', '南京住房公积金管理中心',         '南京中心', '320000', '江苏省', '320100', '南京市', '赵主任', '025-12329',   1, 6),
(7,  '510100', '成都市', '成都住房公积金管理中心',         '成都中心', '510000', '四川省', '510100', '成都市', '孙主任', '028-12329',   1, 7),
(8,  '420100', '武汉市', '武汉住房公积金管理中心',         '武汉中心', '420000', '湖北省', '420100', '武汉市', '周主任', '027-12329',   1, 8),
(9,  '610100', '西安市', '西安住房公积金管理中心',         '西安中心', '610000', '陕西省', '610100', '西安市', '吴主任', '029-12329',   1, 9),
(10, '120000', '天津市', '天津市住房公积金管理中心',       '天津中心', '120000', '天津市', '120000', '天津市', '郑主任', '022-12329',   1, 10);

-- 初始化地区规则示例
INSERT INTO hf_region_rule (id, region_code, rule_type, rule_name, min_contribution_months, require_id_card_verify, require_household_cert, require_work_cert, require_termination_cert, processing_deadline, confirmation_deadline, status) VALUES
(1,  '110000', 1, '北京市转出受理规则',  6,  1, 0, 0, 1, 3, 5, 1),
(2,  '110000', 2, '北京市转入受理规则',  12, 1, 1, 1, 1, 3, 5, 1),
(3,  '310000', 1, '上海市转出受理规则',  6,  1, 0, 0, 1, 3, 5, 1),
(4,  '310000', 2, '上海市转入受理规则',  6,  1, 0, 1, 1, 3, 5, 1),
(5,  '440100', 1, '广州市转出受理规则',  3,  1, 0, 0, 1, 3, 5, 1),
(6,  '440100', 2, '广州市转入受理规则',  12, 1, 1, 1, 1, 3, 5, 1),
(7,  '440300', 1, '深圳市转出受理规则',  3,  1, 0, 0, 1, 3, 5, 1),
(8,  '440300', 2, '深圳市转入受理规则',  6,  1, 0, 1, 1, 3, 5, 1),
(9,  '330100', 1, '杭州市转出受理规则',  6,  1, 0, 0, 1, 3, 5, 1),
(10, '330100', 2, '杭州市转入受理规则',  12, 1, 1, 1, 1, 3, 5, 1);
