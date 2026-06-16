package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class RejectStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String rejectReasonCode;

    private String rejectReasonName;

    private Integer reasonCategory;

    private String reasonCategoryName;

    private Long rejectCount;

    private BigDecimal proportion;

    private Long supplementableCount;

    private BigDecimal supplementSuccessRate;
}
