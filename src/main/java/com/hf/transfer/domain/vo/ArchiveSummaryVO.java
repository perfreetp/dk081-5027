package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ArchiveSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String archiveNo;

    private LocalDateTime archiveTime;

    private BasicInfoVO basicInfo;

    private RuleValidationResultVO ruleValidationResult;

    private List<CollaborationTaskVO> collaborationTasks;

    private List<RejectRecordVO> rejectRecords;

    private List<SupplementRecordVO> supplementRecords;

    private List<UrgeRecordVO> urgeRecords;

    private List<StatusLogVO> statusLogs;

    private List<OperationLogVO> operationLogs;

    private SummaryStatisticsVO statistics;

    @Data
    public static class BasicInfoVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String applicationNo;
        private Integer channelType;
        private String channelTypeName;
        private String channelOrderNo;
        private String applicantName;
        private Integer idCardType;
        private String idCardTypeName;
        private String idCardNo;
        private String mobilePhone;
        private String transferOutRegionCode;
        private String transferOutRegionName;
        private String transferOutCenter;
        private String transferOutAccount;
        private String transferInRegionCode;
        private String transferInRegionName;
        private String transferInCenter;
        private String transferInAccount;
        private Integer transferType;
        private String transferTypeName;
        private BigDecimal transferAmount;
        private BigDecimal actualTransferAmount;
        private LocalDateTime submitTime;
        private LocalDateTime completeTime;
        private Integer finalStatus;
        private String finalStatusName;
    }

    @Data
    public static class RuleValidationResultVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Boolean passed;
        private String outRegionRuleName;
        private String inRegionRuleName;
        private Integer minContributionMonths;
        private List<String> matchedRules;
        private List<String> checkItems;
        private List<String> warnings;
        private Boolean isDuplicate;
        private Boolean isConflict;
        private String duplicateAppNo;
        private String conflictReason;
        private LocalDateTime auditTime;
        private String auditor;
    }

    @Data
    public static class CollaborationTaskVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String taskNo;
        private Integer taskType;
        private String taskTypeName;
        private Integer taskDirection;
        private String sourceRegion;
        private String sourceRegionName;
        private String targetRegion;
        private String targetRegionName;
        private Integer taskStatus;
        private String taskStatusName;
        private LocalDateTime assignTime;
        private LocalDateTime deadlineTime;
        private LocalDateTime confirmTime;
        private Integer confirmResult;
        private String confirmResultName;
        private String confirmRemark;
        private BigDecimal actualAmount;
        private Integer urgeCount;
    }

    @Data
    public static class RejectRecordVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String taskNo;
        private String rejectReasonCode;
        private String rejectReasonName;
        private String rejectReasonDetail;
        private Boolean needSupplement;
        private String supplementGuide;
        private String remark;
        private String operatorName;
        private String operateRegion;
        private LocalDateTime operateTime;
    }

    @Data
    public static class SupplementRecordVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer supplementRound;
        private String requestRegion;
        private String requestOperatorName;
        private LocalDateTime requestTime;
        private String requestRemark;
        private String requiredItems;
        private LocalDateTime submitTime;
        private String submitOperatorName;
        private LocalDateTime auditTime;
        private String auditOperatorName;
        private Integer auditResult;
        private String auditResultName;
        private String auditRemark;
        private Integer supplementStatus;
        private String supplementStatusName;
    }

    @Data
    public static class UrgeRecordVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String taskNo;
        private Integer urgeType;
        private String urgeTypeName;
        private Integer urgeLevel;
        private String urgeLevelName;
        private String urgeContent;
        private String operatorName;
        private LocalDateTime urgeTime;
        private Boolean isEscalated;
        private String escalateToRegion;
        private String escalateToCenter;
    }

    @Data
    public static class StatusLogVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer fromStatus;
        private String fromStatusName;
        private Integer toStatus;
        private String toStatusName;
        private String operateRegion;
        private String operatorName;
        private String operateType;
        private String operateDesc;
        private String remark;
        private LocalDateTime operateTime;
    }

    @Data
    public static class OperationLogVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String logType;
        private String bizType;
        private String module;
        private String operateDesc;
        private String operatorName;
        private String operatorOrgName;
        private LocalDateTime operateTime;
        private Integer executeResult;
        private Long costTime;
    }

    @Data
    public static class SummaryStatisticsVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long totalDurationMinutes;
        private Long totalDurationDays;
        private Integer transferOutDurationHours;
        private Integer transferInDurationHours;
        private Integer rejectCount;
        private Integer supplementCount;
        private Integer urgeCount;
        private Integer escalateCount;
        private String processingEfficiencyLevel;
    }
}
