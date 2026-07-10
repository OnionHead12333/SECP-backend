package com.smartelderly.api.eldercommunity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendTextMessageRequest {

    @NotBlank(message = "kind不能为空")
    private String kind;

    @NotBlank(message = "textContent不能为空")
    @Size(max = 2000, message = "textContent不能超过2000字")
    private String textContent;
}
