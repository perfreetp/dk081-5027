package com.hf.transfer.service;

import com.hf.transfer.common.PageResult;
import com.hf.transfer.domain.entity.OperationLog;

import java.time.LocalDateTime;

public interface OperationLogService {

    void saveLog(OperationLog operationLog);

    void saveLogAsync(String logType, String bizType, String bizId, String bizNo,
                      String moduleName, String operateDesc,
                      String operatorId, String operatorName, String operatorOrgCode, String operatorOrgName,
                      String requestMethod, String requestUrl, String requestParams,
                      String responseResult, Integer executeResult, String errorMsg,
                      Long costTime, String clientIp, String userAgent);

    PageResult<OperationLog> queryLogPage(Long current, Long size,
                                          String logType, String bizType,
                                          String operatorId, String bizNo,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          Integer executeResult);
}
