package com.hf.transfer.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("hf_transfer_application")
public class TransferApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String applicationNo;

    private Integer channelType;

    private String channelOrderNo;

    private String applicantName;

    private Integer idCardType;

    private String idCardNo;

    private String mobilePhone;

    private String transferOutRegion;

    private String transferOutCenter;

    private String transferOutAccount;

    private String transferInRegion;

    private String transferInCenter;

    private String transferInAccount;

    private Integer transferType;

    private Integer transferReason;

    private BigDecimal transferAmount;

    private BigDecimal actualTransferAmount;

    private Integer householdType;

    private Integer employmentStatus;

    private String applicantAccountName;

    private String applicantBankCard;

    private String applicantBankName;

    private Integer applicationStatus;

    private String currentNode;

    private String currentRegion;

    private LocalDateTime submitTime;

    private LocalDateTime auditTime;

    private LocalDateTime transferOutTime;

    private LocalDateTime transferInTime;

    private LocalDateTime completeTime;

    private LocalDateTime expectedCompleteTime;

    private Integer rejectCount;

    private Integer supplementCount;

    private Integer urgeCount;

    private Integer isTimeout;

    private Integer isDuplicate;

    private String duplicateAppNo;

    private Integer isConflict;

    private String conflictReason;

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
