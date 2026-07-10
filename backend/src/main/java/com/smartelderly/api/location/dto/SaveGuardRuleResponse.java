package com.smartelderly.api.location.dto;

public class SaveGuardRuleResponse {

    private Long elderId;

    public SaveGuardRuleResponse() {
    }

    public SaveGuardRuleResponse(Long elderId) {
        this.elderId = elderId;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
    }
}
