package com.hf.transfer.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("hf_supplement_record")
public class SupplementRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long applicationId;

    private String applicationNo;

    private Long taskId;

    private Integer supplementRound;

    private String requestRegion;

    private String requestOperatorId;

    private String requestOperatorName;

    private LocalDateTime requestTime;

    private String requestRemark;

    private String requiredItems;

    private LocalDateTime submitTime;

    private String submitOperatorId;

    private String submitOperatorName;

    private LocalDateTime auditTime;

    private String auditOperatorId;

    private String auditOperatorName;

    private Integer auditResult;

    private String auditRemark;

    private Integer supplementStatus;

    private LocalDateTime deadlineTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
