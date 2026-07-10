package com.smartelderly.api.eldercommunity.dto;

import java.util.List;

import lombok.Data;

@Data
public class MembershipsResponse {
    private List<String> joinedCommunityIds;
    private String scopeKey;
}
