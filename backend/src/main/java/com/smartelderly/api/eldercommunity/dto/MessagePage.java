package com.smartelderly.api.eldercommunity.dto;

import java.util.List;

import lombok.Data;

@Data
public class MessagePage {
    private List<CommunityMessage> items;
    private boolean hasMore;
    private String nextBefore;
}
