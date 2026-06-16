package com.hf.transfer.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ApplicationQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long current = 1L;
    private Long size = 10L;

    private String applicationNo;

    private String channelOrderNo;

    private Integer channelType;

    private String applicantName;

    private String idCardNo;

    private String mobilePhone;

    private String transferOutRegion;

    private String transferInRegion;

    private Integer applicationStatus;

    private Integer transferType;

    private Integer transferReason;

    private LocalDateTime submitTimeStart;

    private LocalDateTime submitTimeEnd;

    private LocalDateTime completeTimeStart;

    private LocalDateTime completeTimeEnd;

    private String currentRegion;
}
