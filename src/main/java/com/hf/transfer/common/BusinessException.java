package com.hf.transfer.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer code;
    private final String message;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(BusinessErrorEnum errorEnum) {
        super(errorEnum.getMessage());
        this.code = errorEnum.getCode();
        this.message = errorEnum.getMessage();
    }

    public BusinessException(BusinessErrorEnum errorEnum, String detailMessage) {
        super(errorEnum.getMessage() + " - " + detailMessage);
        this.code = errorEnum.getCode();
        this.message = errorEnum.getMessage() + " - " + detailMessage;
    }
}
