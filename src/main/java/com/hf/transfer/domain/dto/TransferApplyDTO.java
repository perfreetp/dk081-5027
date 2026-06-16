package com.hf.transfer.domain.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
public class TransferApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "申请渠道不能为空")
    private Integer channelType;

    private String channelOrderNo;

    @NotBlank(message = "申请人姓名不能为空")
    private String applicantName;

    private Integer idCardType = 1;

    @NotBlank(message = "证件号码不能为空")
    private String idCardNo;

    @NotBlank(message = "手机号码不能为空")
    private String mobilePhone;

    @NotBlank(message = "转出地编码不能为空")
    private String transferOutRegion;

    private String transferOutCenter;

    private String transferOutAccount;

    @NotBlank(message = "转入地编码不能为空")
    private String transferInRegion;

    private String transferInCenter;

    private String transferInAccount;

    @NotNull(message = "转移类型不能为空")
    private Integer transferType;

    private Integer transferReason;

    private BigDecimal transferAmount;

    private Integer householdType;

    private Integer employmentStatus;

    private String applicantAccountName;

    private String applicantBankCard;

    private String applicantBankName;

    private String remark;

    private List<MaterialDTO> materials;

    @Data
    public static class MaterialDTO implements Serializable {
        private String materialType;
        private String materialName;
        private String fileId;
        private String fileName;
        private String fileUrl;
        private Long fileSize;
        private String fileFormat;
    }
}
