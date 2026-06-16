package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ProgressStepVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String stepCode;
    private String stepName;
    private String stepDescription;
    private Integer stepStatus;
    private String stepStatusName;
    private LocalDateTime stepTime;
    private String operatorRegion;
    private String operatorRegionName;
    private String remark;
}
