package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class StatusLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer fromStatus;
    private String fromStatusName;
    private Integer toStatus;
    private String toStatusName;
    private String operateRegion;
    private String operateRegionName;
    private String operatorName;
    private String operateType;
    private String operateDesc;
    private String operateRemark;
    private LocalDateTime createTime;
}
