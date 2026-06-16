package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ApplicationListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String applicationNo;
    private Integer channelType;
    private String channelTypeName;

    private String applicantName;
    private String idCardNoMasked;
    private String mobilePhoneMasked;

    private String transferOutRegion;
    private String transferOutRegionName;
    private String transferInRegion;
    private String transferInRegionName;

    private Integer transferType;
    private String transferTypeName;
    private BigDecimal transferAmount;

    private Integer applicationStatus;
    private String applicationStatusName;
    private String applicationStatusDesc;

    private LocalDateTime submitTime;
    private LocalDateTime expectedCompleteTime;
    private LocalDateTime completeTime;

    private Integer urgeCount;
    private Integer supplementCount;
}
