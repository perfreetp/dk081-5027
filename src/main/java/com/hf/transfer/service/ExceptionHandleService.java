package com.hf.transfer.service;

import com.hf.transfer.common.PageResult;
import com.hf.transfer.domain.entity.RejectReason;
import com.hf.transfer.domain.entity.UrgeRecord;
import com.hf.transfer.domain.dto.SupplementSubmitDTO;
import com.hf.transfer.domain.vo.SupplementDetailVO;

import java.util.List;

public interface ExceptionHandleService {

    void processTimeoutTasks();

    UrgeRecord urgeTaskManually(Long taskId, Integer urgeLevel, String content,
                                String operatorId, String operatorName);

    List<RejectReason> listRejectReasons(Integer category, Integer scene);

    SupplementDetailVO getSupplementDetail(Long supplementId);

    SupplementDetailVO getSupplementByApplication(Long applicationId);

    void requestSupplement(Long applicationId, Long taskId, String requestRegion,
                           String requestOperatorId, String requestOperatorName,
                           String requestRemark, String requiredItems, Integer deadlineDays);

    Long submitSupplementMaterials(Long applicationId, Long supplementId,
                                   String submitOperatorId, String submitOperatorName,
                                   List<com.hf.transfer.domain.dto.TransferApplyDTO.MaterialDTO> materials);

    void auditSupplement(Long supplementId, Integer auditResult, String auditRemark,
                         String auditOperatorId, String auditOperatorName);

    PageResult<UrgeRecord> queryUrgeRecords(Long current, Long size,
                                            Long taskId, String applicationNo,
                                            String targetRegion, Integer urgeType,
                                            java.time.LocalDateTime startTime,
                                            java.time.LocalDateTime endTime);

    java.util.List<UrgeRecord> getUrgeRecordsByApplication(Long applicationId);
}
