package com.hf.transfer.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BusinessErrorEnum {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),

    APPLICATION_NOT_FOUND(1001, "转移申请不存在"),
    APPLICATION_DUPLICATE(1002, "存在重复申请"),
    APPLICATION_CONFLICT(1003, "存在冲突申请"),
    APPLICATION_STATUS_ERROR(1004, "申请状态不允许当前操作"),

    RULE_NOT_FOUND(2001, "地区受理规则不存在"),
    RULE_VALIDATE_FAIL(2002, "规则校验未通过"),

    TASK_NOT_FOUND(3001, "协同任务不存在"),
    TASK_STATUS_ERROR(3002, "任务状态不允许当前操作"),
    TASK_TIMEOUT(3003, "协同任务已超时"),

    REJECT_REASON_NOT_FOUND(4001, "退件原因不存在"),
    SUPPLEMENT_MATERIAL_FAIL(4002, "补正材料提交失败"),

    REGION_NOT_FOUND(5001, "地区信息不存在"),

    SYSTEM_ERROR(9999, "系统内部错误");

    private final Integer code;
    private final String message;
}
