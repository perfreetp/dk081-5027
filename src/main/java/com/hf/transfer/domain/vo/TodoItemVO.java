package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TodoItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long applicationId;

    private String applicationNo;

    private String applicantName;

    private String idCardNo;

    private String transferOutRegionName;

    private String transferInRegionName;

    private BigDecimal transferAmount;

    private Integer applicationStatus;

    private String applicationStatusName;

    private LocalDateTime submitTime;

    private LocalDateTime deadlineTime;

    private Long remainingHours;

    private String currentNode;

    private String currentOperator;
}
