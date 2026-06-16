package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class BatchResultItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long taskId;

    private String applicationNo;

    private Boolean success;

    private String message;

    private String failReason;
}
