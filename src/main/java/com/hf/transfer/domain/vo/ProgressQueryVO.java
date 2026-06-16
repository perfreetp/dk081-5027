package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProgressQueryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String applicationNo;
    private Integer applicationStatus;
    private String applicationStatusName;
    private String applicationStatusDesc;
    private String currentNode;
    private String currentRegion;
    private String currentRegionName;

    private String applicantName;
    private String transferOutRegionName;
    private String transferInRegionName;
    private BigDecimal transferAmount;

    private LocalDateTime submitTime;
    private LocalDateTime expectedCompleteTime;
    private LocalDateTime completeTime;

    private Integer rejectCount;
    private Integer supplementCount;
    private Integer urgeCount;

    private List<ProgressStepVO> steps;
}
