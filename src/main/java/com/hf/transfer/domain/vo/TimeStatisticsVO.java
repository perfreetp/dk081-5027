package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class TimeStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String timePeriod;

    private String timeLabel;

    private Long totalCount;

    private Long completedCount;

    private Long processingCount;

    private Long rejectedCount;

    private Long timeoutCount;

    private BigDecimal completionRate;

    private BigDecimal totalTransferAmount;
}
