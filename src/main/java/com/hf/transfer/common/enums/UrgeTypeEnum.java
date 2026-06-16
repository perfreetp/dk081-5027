package com.hf.transfer.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UrgeTypeEnum {

    AUTO_TIMEOUT(1, "系统自动催办", "定时任务自动扫描超时任务"),
    MANUAL_NORMAL(2, "人工普通催办", "业务人员手动发起"),
    MANUAL_IMPORTANT(3, "人工重要催办", "业务人员手动紧急催办"),
    ESCALATE_CENTER(11, "升级至中心主任", "超时3天升级"),
    ESCALATE_PROVINCE(12, "升级至省级监管", "超时7天升级"),
    ESCALATE_MINISTRY(13, "升级至部级监管", "超时15天升级");

    private final Integer code;
    private final String name;
    private final String description;

    public static UrgeTypeEnum getByCode(Integer code) {
        if (code == null) return null;
        for (UrgeTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }

    public static boolean isEscalateType(Integer code) {
        if (code == null) return false;
        return code >= 10;
    }

    public static String getNameByCode(Integer code) {
        UrgeTypeEnum e = getByCode(code);
        return e != null ? e.getName() : null;
    }
}
