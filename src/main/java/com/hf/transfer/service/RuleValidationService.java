package com.hf.transfer.service;

import com.hf.transfer.domain.dto.TransferApplyDTO;
import com.hf.transfer.domain.entity.RegionRule;
import com.hf.transfer.domain.entity.TransferApplication;
import com.hf.transfer.domain.vo.ApplicationDetailVO;

import java.util.List;

public interface RuleValidationService {

    RegionRule matchRegionRule(String regionCode, Integer ruleType);

    List<RegionRule> matchRulesForApplication(TransferApplyDTO dto);

    ValidationResult validateApplication(TransferApplyDTO dto);

    DuplicateCheckResult checkDuplicate(TransferApplyDTO dto);

    ConflictCheckResult checkConflict(TransferApplyDTO dto);

    RuleValidateVO validateFull(TransferApplication application);

    @lombok.Data
    class ValidationResult {
        private boolean passed;
        private List<String> errorMessages;
        private String rejectReasonCode;
        private String rejectReasonName;
        private RegionRule transferOutRule;
        private RegionRule transferInRule;
    }

    @lombok.Data
    class DuplicateCheckResult {
        private boolean duplicate;
        private List<TransferApplication> duplicateApplications;
        private String description;
    }

    @lombok.Data
    class ConflictCheckResult {
        private boolean conflict;
        private List<String> conflictReasons;
        private String description;
    }

    @lombok.Data
    class RuleValidateVO {
        private boolean passed;
        private List<String> passedItems;
        private List<String> failedItems;
        private String suggestion;
        private String rejectReasonCode;
        private String rejectReasonName;
    }
}
