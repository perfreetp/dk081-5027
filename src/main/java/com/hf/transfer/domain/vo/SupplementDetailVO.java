package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SupplementDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long applicationId;
    private String applicationNo;
    private Long taskId;
    private Integer supplementRound;

    private String requestRegion;
    private String requestRegionName;
    private String requestOperatorName;
    private LocalDateTime requestTime;
    private String requestRemark;
    private String requiredItems;

    private LocalDateTime submitTime;
    private String submitOperatorName;

    private LocalDateTime auditTime;
    private String auditOperatorName;
    private Integer auditResult;
    private String auditResultName;
    private String auditRemark;

    private Integer supplementStatus;
    private String supplementStatusName;
    private LocalDateTime deadlineTime;

    private List<MaterialVO> materials;
}
