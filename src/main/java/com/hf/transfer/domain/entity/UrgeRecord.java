package com.hf.transfer.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("hf_urge_record")
public class UrgeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long taskId;

    private String taskNo;

    private Long applicationId;

    private String applicationNo;

    private Integer urgeType;

    private Integer urgeLevel;

    private String targetRegion;

    private String urgeContent;

    private String notifyChannel;

    private Integer notifyStatus;

    private String notifyResponse;

    private String urgeOperatorId;

    private String urgeOperatorName;

    private Boolean isEscalated;

    private String escalateToRegion;

    private String escalateToCenter;

    private Integer escalateLevel;

    private String escalateTo;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
