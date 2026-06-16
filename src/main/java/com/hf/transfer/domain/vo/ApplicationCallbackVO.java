package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApplicationCallbackVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String applicationNo;

    private Integer applicationStatus;

    private String applicationStatusName;

    private String applicationStatusDesc;

    private String currentNode;

    private String currentRegionCode;

    private String currentRegionName;

    private String applicantName;

    private String idCardNo;

    private String transferOutRegionName;

    private String transferInRegionName;

    private java.math.BigDecimal transferAmount;

    private java.math.BigDecimal actualTransferAmount;

    private LocalDateTime submitTime;

    private LocalDateTime expectedCompleteTime;

    private LocalDateTime completeTime;

    private OperationLogVO lastOperation;

    private NextTodoVO nextTodo;

    private List<UrgeRecordVO> urgeRecords;

    private Boolean isCompleted;

    private Boolean isTimeout;

    private Integer remainingDays;

    @Data
    public static class OperationLogVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String operateType;
        private String operateDesc;
        private String operatorName;
        private String operateRegionName;
        private LocalDateTime operateTime;
        private String remark;
    }

    @Data
    public static class NextTodoVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private String todoType;
        private String todoName;
        private String handleRegion;
        private String handleRegionName;
        private LocalDateTime deadline;
        private String requirement;
    }

    @Data
    public static class UrgeRecordVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Integer urgeType;
        private String urgeTypeName;
        private Integer urgeLevel;
        private String urgeLevelName;
        private String urgeContent;
        private String operatorName;
        private LocalDateTime urgeTime;
        private Boolean isEscalated;
        private String escalateTo;
    }
}
