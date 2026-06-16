package com.hf.transfer.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("hf_application_material")
public class ApplicationMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long applicationId;

    private String applicationNo;

    private String materialType;

    private String materialName;

    private String fileId;

    private String fileName;

    private String fileUrl;

    private Long fileSize;

    private String fileFormat;

    private Integer verifyStatus;

    private LocalDateTime verifyTime;

    private String verifyRemark;

    private Integer materialRound;

    private Integer isEffective;

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
