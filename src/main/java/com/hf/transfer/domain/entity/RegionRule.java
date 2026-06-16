package com.hf.transfer.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("hf_region_rule")
public class RegionRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String regionCode;

    private Integer ruleType;

    private String ruleName;

    private Integer minContributionMonths;

    private Integer requireIdCardVerify;

    private Integer requireHouseholdCert;

    private Integer requireWorkCert;

    private Integer requireTerminationCert;

    private Integer allowPartialTransfer;

    private BigDecimal maxTransferAmount;

    private BigDecimal minTransferAmount;

    private Integer processingDeadline;

    private Integer confirmationDeadline;

    private String notifyEmail;

    private String ruleContent;

    private Integer ruleVersion;

    private LocalDateTime effectiveDate;

    private LocalDateTime expiryDate;

    private Integer status;

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
