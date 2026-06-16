package com.hf.transfer.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常 | URI:{} | code:{} | message:{}",
                request.getRequestURI(), e.getCode(), e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验异常 | URI:{} | message:{}", request.getRequestURI(), message);
        return R.error(BusinessErrorEnum.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定异常 | URI:{} | message:{}", request.getRequestURI(), message);
        return R.error(BusinessErrorEnum.PARAM_ERROR.getCode(), message);
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 | URI:{}", request.getRequestURI(), e);
        return R.error(BusinessErrorEnum.SYSTEM_ERROR);
    }
}
