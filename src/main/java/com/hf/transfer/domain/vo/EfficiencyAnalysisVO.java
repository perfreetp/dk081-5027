package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class EfficiencyAnalysisVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String regionCode;

    private String regionName;

    private BigDecimal avgTotalDays;

    private BigDecimal avgRuleAuditDays;

    private BigDecimal avgTransferOutDays;

    private BigDecimal avgTransferInDays;

    private BigDecimal avgSupplementDays;

    private BigDecimal sLAComplianceRate;

    private Long within3DaysCount;

    private Long within7DaysCount;

    private Long within15DaysCount;

    private Long over15DaysCount;
}
