package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class StatisticsOverviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String periodLabel;

    private Long totalApplyCount;

    private Long completedCount;

    private Long processingCount;

    private Long backlogCount;

    private Long timeoutCount;

    private Long rejectedCount;

    private Long supplementCount;

    private Long duplicateCount;

    private BigDecimal completionRate;

    private BigDecimal rejectionRate;

    private BigDecimal timeoutRate;

    private BigDecimal supplementRate;

    private BigDecimal avgProcessingDays;

    private BigDecimal totalTransferAmount;

    private BigDecimal avgTransferAmount;

    private List<KpiTrendVO> trends;
}
