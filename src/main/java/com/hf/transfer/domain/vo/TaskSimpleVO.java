package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TaskSimpleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String taskNo;
    private Integer taskType;
    private String taskTypeName;
    private Integer taskDirection;
    private String taskDirectionName;
    private String sourceRegion;
    private String sourceRegionName;
    private String targetRegion;
    private String targetRegionName;
    private Integer taskStatus;
    private String taskStatusName;
    private LocalDateTime assignTime;
    private LocalDateTime deadlineTime;
    private LocalDateTime confirmTime;
}
