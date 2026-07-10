package com.smartelderly.api.eldercommunity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinCommunityRequest {

    @NotBlank(message = "communityId不能为空")
    private String communityId;
}
