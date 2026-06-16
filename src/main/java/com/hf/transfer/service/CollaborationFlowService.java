package com.hf.transfer.service;

import com.hf.transfer.domain.entity.CollaborationTask;

import java.util.List;

public interface CollaborationFlowService {

    CollaborationTask createTransferOutTask(Long applicationId);

    CollaborationTask createTransferInTask(Long applicationId);

    void confirmTransferOut(Long taskId, String operatorId, String operatorName,
                            String remark, java.math.BigDecimal actualAmount);

    void confirmTransferIn(Long taskId, String operatorId, String operatorName,
                           String remark);

    void rejectTask(Long taskId, String operatorId, String operatorName,
                    String rejectReasonCode, String rejectReasonName, String remark,
                    String supplementItems, boolean needSupplement);

    List<CollaborationTask> getApplicationTasks(Long applicationId);

    CollaborationTask getTaskDetail(Long taskId);

    void syncTaskToTarget(Long taskId);

    com.hf.transfer.domain.vo.BatchResultVO batchConfirmOut(com.hf.transfer.domain.dto.BatchConfirmOutDTO dto);

    com.hf.transfer.domain.vo.BatchResultVO batchReject(com.hf.transfer.domain.dto.BatchRejectDTO dto);
}
