package com.hf.transfer.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SupplementSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long applicationId;

    private Long supplementId;

    private String submitOperatorId;

    private String submitOperatorName;

    private List<TransferApplyDTO.MaterialDTO> materials;
}
