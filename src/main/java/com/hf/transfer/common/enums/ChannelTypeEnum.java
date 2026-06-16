package com.hf.transfer.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChannelTypeEnum {

    ONLINE_HALL(1, "网上办事大厅"),
    MOBILE_APP(2, "手机APP"),
    WECHAT_MINIAPP(3, "微信小程序"),
    ALIPAY_MINIAPP(4, "支付宝小程序"),
    COUNTER(5, "柜台受理"),
    SELF_SERVICE_TERMINAL(6, "自助终端"),
    API_SYNC(7, "第三方系统对接"),
    BANK_OUTLET(8, "银行网点");

    private final Integer code;
    private final String name;

    public static ChannelTypeEnum getByCode(Integer code) {
        if (code == null) return null;
        for (ChannelTypeEnum e : values()) {
            if (e.code.equals(code)) return e;
        }
        return null;
    }
}
