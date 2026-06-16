package com.hf.transfer.service;

import com.hf.transfer.common.enums.ApplicationStatusEnum;
import com.hf.transfer.domain.entity.ApplicationStatusLog;

import java.util.List;

public interface StatusLogService {

    void logStatusChange(Long applicationId, String applicationNo,
                         ApplicationStatusEnum fromStatus, ApplicationStatusEnum toStatus,
                         String operateRegion, String operatorId, String operatorName,
                         String operateType, String operateDesc, String operateRemark,
                         Long taskId, String clientIp);

    List<ApplicationStatusLog> queryByApplicationId(Long applicationId);
}
