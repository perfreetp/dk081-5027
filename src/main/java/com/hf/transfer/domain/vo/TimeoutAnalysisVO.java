package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TimeoutAnalysisVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalTimeoutCount;

    private Long autoUrgedCount;

    private Long manualUrgedCount;

    private BigDecimal avgUrgeCountPerTask;

    private List<TimeoutDistributionVO> byRegion;

    private List<TimeoutDistributionVO> byLevel;

    private List<TimeoutDistributionVO> byTaskType;

    @Data
    public static class TimeoutDistributionVO implements Serializable {
        private String dimension;
        private String dimensionName;
        private Long count;
        private BigDecimal proportion;
    }
}
