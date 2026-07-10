package com.smartelderly.api.eldercommunity.dto;

import lombok.Data;

@Data
public class CommunityMessage {
    private String id;
    private String communityId;
    private String role;
    private String senderDisplay;
    private String senderScopeKey;
    private String senderAvatarUrl;
    private String senderEmoji;
    private String kind;
    private String textContent;
    private String audioUrl;
    private Integer durationMs;
    private String imageUrl;
    private Long createdAtMillis;
    private String createdAt;
    private boolean mine;
}
