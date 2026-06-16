package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CenterTodoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String regionCode;

    private String regionName;

    private String centerName;

    private Integer totalCount;

    private TodoCategoryVO pendingAccept;

    private TodoCategoryVO pendingTransferOut;

    private TodoCategoryVO pendingTransferIn;

    private TodoCategoryVO pendingSupplement;

    private TodoCategoryVO approachingTimeout;

    private TodoCategoryVO alreadyTimeout;
}
