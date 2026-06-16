package com.hf.transfer.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BatchRejectDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<BatchRejectItem> items;

    private String operatorId;

    private String operatorName;

    @Data
    public static class BatchRejectItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long taskId;
        private String rejectReasonCode;
        private String rejectReasonName;
        private Boolean needSupplement;
        private String supplementItems;
        private String remark;
    }
}
