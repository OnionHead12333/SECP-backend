package com.smartelderly.api.eldercommunity.dto;

import lombok.Data;

@Data
public class InterestCommunityBrief {
    private String id;
    private String name;
    private String shortDescription;
    private String previewIcon;
    private String memberHint;
    private boolean joined;
}
