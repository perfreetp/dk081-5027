package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TypeStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer transferType;

    private String transferTypeName;

    private Long totalCount;

    private Long completedCount;

    private Long backlogCount;

    private Long timeoutCount;

    private BigDecimal completionRate;

    private BigDecimal avgProcessingDays;

    private BigDecimal totalTransferAmount;

    private BigDecimal proportion;
}
