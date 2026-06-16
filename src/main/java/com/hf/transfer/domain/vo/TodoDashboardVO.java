package com.hf.transfer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class TodoDashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer totalPending;

    private Integer pendingAccept;

    private Integer pendingTransferOut;

    private Integer pendingTransferIn;

    private Integer pendingSupplement;

    private Integer pendingUrge;

    private Integer approachingTimeout;

    private Integer alreadyTimeout;

    private List<CenterTodoVO> centerTodos;
}
