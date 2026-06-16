package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class KpiTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String date;

    private Long applyCount;

    private Long completedCount;

    private BigDecimal completionRate;
}
