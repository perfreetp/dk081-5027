-- =====================================================
-- 住房公积金异地转移接续协同服务平台 数据库DDL脚本
-- 数据库版本：H2 Compatible
-- =====================================================

-- 1. 地区信息表
DROP TABLE IF EXISTS hf_region_info;
CREATE TABLE hf_region_info (
    id              BIGINT          NOT NULL,
    region_code     VARCHAR(12)     NOT NULL,
    region_name     VARCHAR(64)     NOT NULL,
    center_name     VARCHAR(128)    NOT NULL,
    center_short    VARCHAR(64)     NOT NULL,
    province_code   VARCHAR(6)      NOT NULL,
    province_name   VARCHAR(32)     NOT NULL,
    city_code       VARCHAR(12)     NOT NULL,
    city_name       VARCHAR(32)     NOT NULL,
    contact_person  VARCHAR(32),
    contact_phone   VARCHAR(32),
    contact_email   VARCHAR(64),
    api_endpoint    VARCHAR(256),
    api_secret      VARCHAR(128),
    status          SMALLINT        NOT NULL DEFAULT 1,
    sort_order      INT             DEFAULT 0,
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    create_by       VARCHAR(64),
    create_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_by       VARCHAR(64),
    update_time     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_region_info_code UNIQUE (region_code)
);

-- 2. 地区受理规则表
DROP TABLE IF EXISTS hf_region_rule;
CREATE TABLE hf_region_rule (
    id                  BIGINT          NOT NULL,
    region_code         VARCHAR(12)     NOT NULL,
    rule_type           SMALLINT        NOT NULL,
    rule_name           VARCHAR(128)    NOT NULL,
    min_contribution_months INT,
    require_id_card_verify SMALLINT     NOT NULL DEFAULT 1,
    require_household_cert SMALLINT     NOT NULL DEFAULT 0,
    require_work_cert   SMALLINT        NOT NULL DEFAULT 0,
    require_termination_cert SMALLINT   NOT NULL DEFAULT 1,
    allow_partial_transfer SMALLINT     NOT NULL DEFAULT 0,
    max_transfer_amount DECIMAL(18,2),
    min_transfer_amount DECIMAL(18,2),
    processing_deadline INT             NOT NULL DEFAULT 3,
    confirmation_deadline INT           NOT NULL DEFAULT 5,
    notify_email        VARCHAR(256),
    rule_content        TEXT,
    rule_version        INT             NOT NULL DEFAULT 1,
    effective_date      DATE,
    expiry_date         DATE,
    status              SMALLINT        NOT NULL DEFAULT 1,
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    create_by           VARCHAR(64),
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_by           VARCHAR(64),
    update_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 3. 转移申请表
DROP TABLE IF EXISTS hf_transfer_application;
CREATE TABLE hf_transfer_application (
    id                      BIGINT          NOT NULL,
    application_no          VARCHAR(32)     NOT NULL,
    channel_type            SMALLINT        NOT NULL,
    channel_order_no        VARCHAR(64),
    applicant_name          VARCHAR(64)     NOT NULL,
    id_card_type            SMALLINT        NOT NULL DEFAULT 1,
    id_card_no              VARCHAR(32)     NOT NULL,
    mobile_phone            VARCHAR(16)     NOT NULL,
    transfer_out_region     VARCHAR(12)     NOT NULL,
    transfer_out_center     VARCHAR(128),
    transfer_out_account    VARCHAR(32),
    transfer_in_region      VARCHAR(12)     NOT NULL,
    transfer_in_center      VARCHAR(128),
    transfer_in_account     VARCHAR(32),
    transfer_type           SMALLINT        NOT NULL DEFAULT 1,
    transfer_reason         SMALLINT,
    transfer_amount         DECIMAL(18,2),
    actual_transfer_amount  DECIMAL(18,2),
    household_type          SMALLINT,
    employment_status       SMALLINT,
    applicant_account_name  VARCHAR(64),
    applicant_bank_card     VARCHAR(32),
    applicant_bank_name     VARCHAR(128),
    application_status      INT             NOT NULL,
    current_node            VARCHAR(32),
    current_region          VARCHAR(12),
    submit_time             TIMESTAMP,
    audit_time              TIMESTAMP,
    transfer_out_time       TIMESTAMP,
    transfer_in_time        TIMESTAMP,
    complete_time           TIMESTAMP,
    expected_complete_time  TIMESTAMP,
    reject_count            INT             NOT NULL DEFAULT 0,
    supplement_count        INT             NOT NULL DEFAULT 0,
    urge_count              INT             NOT NULL DEFAULT 0,
    is_duplicate            SMALLINT        NOT NULL DEFAULT 0,
    duplicate_app_no        VARCHAR(32),
    is_conflict             SMALLINT        NOT NULL DEFAULT 0,
    conflict_reason         VARCHAR(256),
    remark                  VARCHAR(512),
    operator_id             VARCHAR(64),
    operator_name           VARCHAR(64),
    deleted                 SMALLINT        NOT NULL DEFAULT 0,
    create_by               VARCHAR(64),
    create_time             TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_by               VARCHAR(64),
    update_time             TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_no UNIQUE (application_no)
);

-- 4. 申请材料表
DROP TABLE IF EXISTS hf_application_material;
CREATE TABLE hf_application_material (
    id                  BIGINT          NOT NULL,
    application_id      BIGINT          NOT NULL,
    application_no      VARCHAR(32)     NOT NULL,
    material_type       VARCHAR(32)     NOT NULL,
    material_name       VARCHAR(128)    NOT NULL,
    file_id             VARCHAR(64),
    file_name           VARCHAR(256),
    file_url            VARCHAR(512),
    file_size           BIGINT,
    file_format         VARCHAR(16),
    verify_status       SMALLINT        NOT NULL DEFAULT 0,
    verify_time         TIMESTAMP,
    verify_remark       VARCHAR(256),
    material_round      INT             NOT NULL DEFAULT 1,
    is_effective        SMALLINT        NOT NULL DEFAULT 1,
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    create_by           VARCHAR(64),
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_by           VARCHAR(64),
    update_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 5. 跨中心协同任务表
DROP TABLE IF EXISTS hf_collaboration_task;
CREATE TABLE hf_collaboration_task (
    id                      BIGINT          NOT NULL,
    task_no                 VARCHAR(32)     NOT NULL,
    application_id          BIGINT          NOT NULL,
    application_no          VARCHAR(32)     NOT NULL,
    task_type               SMALLINT        NOT NULL,
    task_direction          SMALLINT        NOT NULL,
    source_region           VARCHAR(12)     NOT NULL,
    target_region           VARCHAR(12)     NOT NULL,
    task_status             INT             NOT NULL,
    priority                SMALLINT        NOT NULL DEFAULT 2,
    assign_time             TIMESTAMP,
    deadline_time           TIMESTAMP,
    first_urge_time         TIMESTAMP,
    last_urge_time          TIMESTAMP,
    confirm_time            TIMESTAMP,
    confirm_result          SMALLINT,
    confirm_remark          VARCHAR(512),
    reject_reason_code      VARCHAR(32),
    reject_reason_name      VARCHAR(128),
    supplement_items        TEXT,
    sync_status             SMALLINT        NOT NULL DEFAULT 0,
    sync_attempts           INT             NOT NULL DEFAULT 0,
    sync_last_time          TIMESTAMP,
    sync_response           VARCHAR(512),
    urge_count              INT             NOT NULL DEFAULT 0,
    remark                  VARCHAR(512),
    operator_id             VARCHAR(64),
    operator_name           VARCHAR(64),
    deleted                 SMALLINT        NOT NULL DEFAULT 0,
    create_by               VARCHAR(64),
    create_time             TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_by               VARCHAR(64),
    update_time             TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_task_no UNIQUE (task_no)
);

-- 6. 标准化退件原因表
DROP TABLE IF EXISTS hf_reject_reason;
CREATE TABLE hf_reject_reason (
    id                  BIGINT          NOT NULL,
    reason_code         VARCHAR(32)     NOT NULL,
    reason_category     SMALLINT        NOT NULL,
    reason_name         VARCHAR(128)    NOT NULL,
    reason_detail       VARCHAR(512),
    supplement_guide    VARCHAR(512),
    applicable_scene    SMALLINT        NOT NULL DEFAULT 0,
    is_need_supplement  SMALLINT        NOT NULL DEFAULT 1,
    status              SMALLINT        NOT NULL DEFAULT 1,
    sort_order          INT             DEFAULT 0,
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    create_by           VARCHAR(64),
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_by           VARCHAR(64),
    update_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_reason_code UNIQUE (reason_code)
);

-- 7. 催办记录表
DROP TABLE IF EXISTS hf_urge_record;
CREATE TABLE hf_urge_record (
    id                  BIGINT          NOT NULL,
    task_id             BIGINT          NOT NULL,
    task_no             VARCHAR(32)     NOT NULL,
    application_id      BIGINT,
    application_no      VARCHAR(32),
    urge_type           SMALLINT        NOT NULL,
    urge_level          SMALLINT        NOT NULL DEFAULT 1,
    target_region       VARCHAR(12),
    urge_content        VARCHAR(512)    NOT NULL,
    notify_channel      VARCHAR(64),
    notify_status       SMALLINT        NOT NULL DEFAULT 0,
    notify_response     VARCHAR(256),
    urge_operator_id    VARCHAR(64),
    urge_operator_name  VARCHAR(64),
    is_escalated        SMALLINT        NOT NULL DEFAULT 0,
    escalate_to_region  VARCHAR(12),
    escalate_to_center  VARCHAR(128),
    escalate_level      INT,
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 8. 补正材料流转表
DROP TABLE IF EXISTS hf_supplement_record;
CREATE TABLE hf_supplement_record (
    id                  BIGINT          NOT NULL,
    application_id      BIGINT          NOT NULL,
    application_no      VARCHAR(32)     NOT NULL,
    task_id             BIGINT,
    supplement_round    INT             NOT NULL,
    request_region      VARCHAR(12)     NOT NULL,
    request_operator_id VARCHAR(64),
    request_operator_name VARCHAR(64),
    request_time        TIMESTAMP       NOT NULL,
    request_remark      VARCHAR(512),
    required_items      TEXT,
    submit_time         TIMESTAMP,
    submit_operator_id  VARCHAR(64),
    submit_operator_name VARCHAR(64),
    audit_time          TIMESTAMP,
    audit_operator_id   VARCHAR(64),
    audit_operator_name VARCHAR(64),
    audit_result        SMALLINT,
    audit_remark        VARCHAR(512),
    supplement_status   SMALLINT        NOT NULL,
    deadline_time       TIMESTAMP,
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    update_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 9. 申请状态流转日志表
DROP TABLE IF EXISTS hf_application_status_log;
CREATE TABLE hf_application_status_log (
    id                  BIGINT          NOT NULL,
    application_id      BIGINT          NOT NULL,
    application_no      VARCHAR(32)     NOT NULL,
    from_status         INT,
    from_status_name    VARCHAR(32),
    to_status           INT             NOT NULL,
    to_status_name      VARCHAR(32)     NOT NULL,
    operate_region      VARCHAR(12),
    operator_id         VARCHAR(64),
    operator_name       VARCHAR(64),
    operate_type        VARCHAR(32),
    operate_desc        VARCHAR(128)    NOT NULL,
    operate_remark      VARCHAR(512),
    task_id             BIGINT,
    client_ip           VARCHAR(64),
    user_agent          VARCHAR(512),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 10. 操作审计日志表
DROP TABLE IF EXISTS hf_operation_log;
CREATE TABLE hf_operation_log (
    id                  BIGINT          NOT NULL,
    log_type            VARCHAR(32)     NOT NULL,
    biz_type            VARCHAR(64)     NOT NULL,
    biz_id              VARCHAR(64),
    biz_no              VARCHAR(64),
    module_name         VARCHAR(64),
    method_name         VARCHAR(256),
    operate_desc        VARCHAR(512)    NOT NULL,
    operator_id         VARCHAR(64),
    operator_name       VARCHAR(64),
    operator_org_code   VARCHAR(12),
    operator_org_name   VARCHAR(128),
    request_method      VARCHAR(16),
    request_url         VARCHAR(512),
    request_params      TEXT,
    response_result     TEXT,
    execute_result      SMALLINT        NOT NULL,
    error_msg           VARCHAR(1024),
    cost_time           BIGINT,
    client_ip           VARCHAR(64),
    client_location     VARCHAR(128),
    user_agent          VARCHAR(512),
    deleted             SMALLINT        NOT NULL DEFAULT 0,
    create_time         TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
