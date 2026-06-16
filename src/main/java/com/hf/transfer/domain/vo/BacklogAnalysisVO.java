package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BacklogAnalysisVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalBacklogCount;

    private Long outRegionBacklog;

    private Long inRegionBacklog;

    private Long supplementBacklog;

    private Long pendingReviewBacklog;

    private List<BacklogItemVO> backlogItems;

    @Data
    public static class BacklogItemVO implements Serializable {
        private String statusName;
        private Long count;
        private Double proportion;
    }
}
