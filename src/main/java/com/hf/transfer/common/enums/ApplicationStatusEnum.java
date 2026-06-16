package com.hf.transfer.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApplicationStatusEnum {

    PENDING_REVIEW(10, "待审核", "申请已提交，等待规则校验"),
    RULE_PASSED(20, "规则通过", "校验通过，等待转出中心受理"),
    RULE_REJECTED(25, "规则不通过", "校验未通过，申请终止"),
    DUPLICATE_REJECTED(28, "重复申请驳回", "检测到重复申请，予以驳回"),
    TRANSFER_OUT_PENDING(30, "转出待受理", "已推送至转出地中心，等待确认"),
    TRANSFER_OUT_CONFIRMED(40, "转出已确认", "转出地已确认受理"),
    TRANSFER_OUT_REJECTED(45, "转出退回", "转出地中心退回申请"),
    TRANSFER_IN_PENDING(50, "转入待确认", "资金转出后，等待转入地确认"),
    TRANSFER_IN_CONFIRMED(60, "转入已确认", "转入地已确认到账"),
    SUPPLEMENT_PENDING(70, "待补正材料", "需补充材料后重新流转"),
    COMPLETED(80, "已办结", "转移接续完成，已归档"),
    CANCELLED(90, "已取消", "申请被主动取消"),
    TERMINATED(95, "已终止", "特殊原因终止流程");

    private final Integer code;
    private final String name;
    private final String description;

    public static ApplicationStatusEnum getByCode(Integer code) {
        if (code == null) return null;
        for (ApplicationStatusEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
