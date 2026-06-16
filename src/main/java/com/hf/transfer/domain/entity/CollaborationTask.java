package com.hf.transfer.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("hf_collaboration_task")
public class CollaborationTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskNo;

    private Long applicationId;

    private String applicationNo;

    private Integer taskType;

    private Integer taskDirection;

    private String sourceRegion;

    private String targetRegion;

    private Integer taskStatus;

    private Integer priority;

    private LocalDateTime assignTime;

    private LocalDateTime deadlineTime;

    private LocalDateTime firstUrgeTime;

    private LocalDateTime lastUrgeTime;

    private LocalDateTime confirmTime;

    private Integer confirmResult;

    private String confirmRemark;

    private String rejectReasonCode;

    private String rejectReasonName;

    private String supplementItems;

    private Integer syncStatus;

    private Integer syncAttempts;

    private LocalDateTime syncLastTime;

    private String syncResponse;

    private Integer urgeCount;

    private String remark;

    private String operatorId;

    private String operatorName;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
