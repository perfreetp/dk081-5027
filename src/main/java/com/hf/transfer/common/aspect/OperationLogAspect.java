package com.hf.transfer.common.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hf.transfer.common.annotation.OpLog;
import com.hf.transfer.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OpLog opLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Integer executeResult = 1;
        String errorMsg = null;

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getName() + "." + method.getName();

        String requestParams = "";
        if (opLog.saveParams()) {
            try {
                Object[] args = joinPoint.getArgs();
                Parameter[] parameters = method.getParameters();
                java.util.Map<String, Object> paramMap = new java.util.LinkedHashMap<>();
                for (int i = 0; i < parameters.length; i++) {
                    String name = parameters[i].getName();
                    Object value = args[i];
                    if (value != null && isSerializable(value)) {
                        paramMap.put(name, value);
                    }
                }
                requestParams = JSONUtil.toJsonStr(paramMap);
            } catch (Exception e) {
                log.warn("[操作日志] 参数序列化失败", e);
            }
        }

        String clientIp = "";
        String requestUrl = "";
        String requestMethod = "";
        String userAgent = "";
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                clientIp = getClientIp(request);
                requestUrl = request.getRequestURI();
                requestMethod = request.getMethod();
                userAgent = request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.warn("[操作日志] 获取请求信息失败", e);
        }

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            executeResult = 0;
            errorMsg = t.getMessage();
            if (errorMsg != null && errorMsg.length() > 1000) {
                errorMsg = errorMsg.substring(0, 1000);
            }
            throw t;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            String responseResult = "";
            if (opLog.saveResult() && result != null) {
                try {
                    responseResult = JSONUtil.toJsonStr(result);
                } catch (Exception e) {
                    log.warn("[操作日志] 结果序列化失败", e);
                }
            }

            String bizId = "";
            String bizNo = "";
            try {
                Object[] args = joinPoint.getArgs();
                for (Object arg : args) {
                    if (arg == null) continue;
                    java.lang.reflect.Field idField = findField(arg.getClass(), "id");
                    if (idField != null) {
                        idField.setAccessible(true);
                        Object v = idField.get(arg);
                        if (v != null) bizId = v.toString();
                    }
                    java.lang.reflect.Field noField = findField(arg.getClass(), "applicationNo");
                    if (noField == null) noField = findField(arg.getClass(), "taskNo");
                    if (noField != null) {
                        noField.setAccessible(true);
                        Object v = noField.get(arg);
                        if (v != null) bizNo = v.toString();
                    }
                }
                if (StrUtil.isBlank(bizId) && result != null) {
                    java.lang.reflect.Field idField = findField(result.getClass(), "data");
                    if (idField != null) {
                        idField.setAccessible(true);
                        Object data = idField.get(result);
                        if (data instanceof Long) bizId = data.toString();
                        else if (data instanceof String) {
                            String s = (String) data;
                            if (StrUtil.isNumeric(s)) bizId = s;
                            else bizNo = s;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("[操作日志] 提取业务ID失败", e);
            }

            try {
                operationLogService.saveLogAsync(
                        opLog.logType(),
                        opLog.bizType(),
                        bizId,
                        bizNo,
                        opLog.module(),
                        opLog.desc(),
                        null,
                        null,
                        null,
                        null,
                        requestMethod,
                        requestUrl,
                        requestParams,
                        responseResult,
                        executeResult,
                        errorMsg,
                        costTime,
                        clientIp,
                        userAgent
                );
            } catch (Exception e) {
                log.error("[操作日志] 保存失败", e);
            }
        }
    }

    private boolean isSerializable(Object obj) {
        if (obj instanceof jakarta.servlet.http.HttpServletRequest) return false;
        if (obj instanceof jakarta.servlet.http.HttpServletResponse) return false;
        if (obj instanceof org.springframework.web.multipart.MultipartFile) return false;
        return true;
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null && clazz != Object.class) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
