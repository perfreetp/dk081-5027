package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class RegionStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String regionCode;

    private String regionName;

    private String centerName;

    private Integer roleType;

    private Long totalCount;

    private Long completedCount;

    private Long processingCount;

    private Long backlogCount;

    private Long timeoutCount;

    private Long rejectedCount;

    private Long supplementCount;

    private BigDecimal completionRate;

    private BigDecimal rejectionRate;

    private BigDecimal timeoutRate;

    private BigDecimal avgProcessingDays;

    private BigDecimal totalTransferAmount;

    private Integer rank;

    private String rankLevel;
}
