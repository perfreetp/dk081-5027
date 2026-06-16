package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BatchResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private List<BatchResultItemVO> results;
}
