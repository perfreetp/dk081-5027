package com.hf.transfer.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskStatusEnum {

    PENDING(10, "待处理"),
    PROCESSING(20, "处理中"),
    CONFIRMED(30, "已确认"),
    REJECTED(40, "已退回"),
    TIMEOUT(50, "已超时"),
    URGED(60, "已催办"),
    COMPLETED(70, "已完成"),
    CLOSED(80, "已关闭");

    private final Integer code;
    private final String name;

    public static TaskStatusEnum getByCode(Integer code) {
        if (code == null) return null;
        for (TaskStatusEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
