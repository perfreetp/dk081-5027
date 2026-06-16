package com.hf.transfer.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BatchConfirmOutDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<BatchConfirmOutItem> items;

    private String operatorId;

    private String operatorName;

    @Data
    public static class BatchConfirmOutItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long taskId;
        private BigDecimal actualAmount;
        private String remark;
    }
}
