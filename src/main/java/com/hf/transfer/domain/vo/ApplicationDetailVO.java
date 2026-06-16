package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApplicationDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String applicationNo;
    private Integer channelType;
    private String channelTypeName;
    private String channelOrderNo;

    private String applicantName;
    private Integer idCardType;
    private String idCardNo;
    private String mobilePhone;

    private String transferOutRegion;
    private String transferOutRegionName;
    private String transferOutCenter;
    private String transferOutAccount;

    private String transferInRegion;
    private String transferInRegionName;
    private String transferInCenter;
    private String transferInAccount;

    private Integer transferType;
    private String transferTypeName;
    private Integer transferReason;
    private String transferReasonName;
    private BigDecimal transferAmount;
    private BigDecimal actualTransferAmount;

    private Integer householdType;
    private Integer employmentStatus;

    private String applicantAccountName;
    private String applicantBankCard;
    private String applicantBankName;

    private Integer applicationStatus;
    private String applicationStatusName;
    private String applicationStatusDesc;
    private String currentNode;
    private String currentRegion;
    private String currentRegionName;

    private LocalDateTime submitTime;
    private LocalDateTime auditTime;
    private LocalDateTime transferOutTime;
    private LocalDateTime transferInTime;
    private LocalDateTime completeTime;
    private LocalDateTime expectedCompleteTime;

    private Integer rejectCount;
    private Integer supplementCount;
    private Integer urgeCount;

    private Integer isDuplicate;
    private String duplicateAppNo;
    private Integer isConflict;
    private String conflictReason;

    private String remark;
    private String operatorName;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private List<MaterialVO> materials;
    private List<StatusLogVO> statusLogs;
    private List<TaskSimpleVO> tasks;
}
