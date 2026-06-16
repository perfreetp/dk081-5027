package com.hf.transfer.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("hf_operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logType;

    private String bizType;

    private String bizId;

    private String bizNo;

    private String moduleName;

    private String methodName;

    private String operateDesc;

    private String operatorId;

    private String operatorName;

    private String operatorOrgCode;

    private String operatorOrgName;

    private String requestMethod;

    private String requestUrl;

    private String requestParams;

    private String responseResult;

    private Integer executeResult;

    private String errorMsg;

    private Long costTime;

    private String clientIp;

    private String clientLocation;

    private String userAgent;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
