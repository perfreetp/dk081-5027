package com.hf.transfer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hf.transfer.common.PageResult;
import com.hf.transfer.domain.entity.OperationLog;
import com.hf.transfer.mapper.OperationLogMapper;
import com.hf.transfer.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public void saveLog(OperationLog operationLog) {
        operationLogMapper.insert(operationLog);
    }

    @Override
    @Async("logExecutor")
    public void saveLogAsync(String logType, String bizType, String bizId, String bizNo,
                             String moduleName, String operateDesc,
                             String operatorId, String operatorName, String operatorOrgCode, String operatorOrgName,
                             String requestMethod, String requestUrl, String requestParams,
                             String responseResult, Integer executeResult, String errorMsg,
                             Long costTime, String clientIp, String userAgent) {
        try {
            OperationLog logEntity = new OperationLog();
            logEntity.setLogType(logType);
            logEntity.setBizType(bizType);
            logEntity.setBizId(bizId);
            logEntity.setBizNo(bizNo);
            logEntity.setModuleName(moduleName);
            logEntity.setOperateDesc(operateDesc);
            logEntity.setOperatorId(operatorId);
            logEntity.setOperatorName(operatorName);
            logEntity.setOperatorOrgCode(operatorOrgCode);
            logEntity.setOperatorOrgName(operatorOrgName);
            logEntity.setRequestMethod(requestMethod);
            logEntity.setRequestUrl(requestUrl);
            if (requestParams != null && requestParams.length() > 4000) {
                requestParams = requestParams.substring(0, 4000) + "...(truncated)";
            }
            logEntity.setRequestParams(requestParams);
            if (responseResult != null && responseResult.length() > 4000) {
                responseResult = responseResult.substring(0, 4000) + "...(truncated)";
            }
            logEntity.setResponseResult(responseResult);
            logEntity.setExecuteResult(executeResult);
            logEntity.setErrorMsg(errorMsg);
            logEntity.setCostTime(costTime);
            logEntity.setClientIp(clientIp);
            logEntity.setUserAgent(userAgent);
            operationLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.error("[操作日志] 异步保存失败", e);
        }
    }

    @Override
    public PageResult<OperationLog> queryLogPage(Long current, Long size,
                                                 String logType, String bizType,
                                                 String operatorId, String bizNo,
                                                 LocalDateTime startTime, LocalDateTime endTime,
                                                 Integer executeResult) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (logType != null) wrapper.eq(OperationLog::getLogType, logType);
        if (bizType != null) wrapper.eq(OperationLog::getBizType, bizType);
        if (operatorId != null) wrapper.eq(OperationLog::getOperatorId, operatorId);
        if (bizNo != null) wrapper.like(OperationLog::getBizNo, bizNo);
        if (startTime != null) wrapper.ge(OperationLog::getCreateTime, startTime);
        if (endTime != null) wrapper.le(OperationLog::getCreateTime, endTime);
        if (executeResult != null) wrapper.eq(OperationLog::getExecuteResult, executeResult);
        wrapper.orderByDesc(OperationLog::getCreateTime);

        IPage<OperationLog> page = operationLogMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(page);
    }
}
