package com.smartelderly.api.eldercommunity.dto;

import lombok.Data;

@Data
public class MembershipSummary {
    private String communityId;
    private String communityName;
    private String scopeKey;
    private String status;
    private Long joinedAtMillis;
    private Long leftAtMillis;
    private String welcomeMessage;
}
