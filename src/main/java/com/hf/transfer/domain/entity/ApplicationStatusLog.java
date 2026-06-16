package com.hf.transfer.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("hf_application_status_log")
public class ApplicationStatusLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long applicationId;

    private String applicationNo;

    private Integer fromStatus;

    private String fromStatusName;

    private Integer toStatus;

    private String toStatusName;

    private String operateRegion;

    private String operatorId;

    private String operatorName;

    private String operateType;

    private String operateDesc;

    private String operateRemark;

    private Long taskId;

    private String clientIp;

    private String userAgent;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
