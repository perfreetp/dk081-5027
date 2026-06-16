package com.hf.transfer.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UrgeEscalateLevelEnum {

    LEVEL_NORMAL(0, "普通催办", "针对普通超时任务，直接催办目标中心经办人"),
    LEVEL_CENTER(1, "升级至中心主任", "超时超过3天，升级至目标中心主任"),
    LEVEL_PROVINCE(2, "升级至省级监管", "超时超过7天，升级至省级公积金监管部门"),
    LEVEL_MINISTRY(3, "升级至部级监管", "超时超过15天，升级至住建部监管");

    private final Integer level;
    private final String name;
    private final String description;

    public static UrgeEscalateLevelEnum getByLevel(Integer level) {
        if (level == null) return LEVEL_NORMAL;
        for (UrgeEscalateLevelEnum e : values()) {
            if (e.level.equals(level)) return e;
        }
        return LEVEL_NORMAL;
    }

    public static Integer getEscalateLevelByTimeoutDays(long timeoutDays) {
        if (timeoutDays >= 15) return LEVEL_MINISTRY.getLevel();
        if (timeoutDays >= 7) return LEVEL_PROVINCE.getLevel();
        if (timeoutDays >= 3) return LEVEL_CENTER.getLevel();
        return LEVEL_NORMAL.getLevel();
    }

    public static String getNameByCode(Integer code) {
        UrgeEscalateLevelEnum e = getByLevel(code);
        return e != null ? e.getName() : null;
    }
}
