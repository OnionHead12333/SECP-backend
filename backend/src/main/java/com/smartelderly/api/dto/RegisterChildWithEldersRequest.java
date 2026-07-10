package com.smartelderly.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 与移动端 {@code AuthApi.registerChildWithElders} JSON 结构一致。
 */
@Data
public class RegisterChildWithEldersRequest {

    @NotNull
    @Valid
    private ChildBlock child;

    @NotEmpty
    @Valid
    private List<ElderItem> elders;

    @Data
    public static class ChildBlock {
        @NotBlank(message = "子女姓名不能为空")
        private String name;

        private String nickname;

        @NotBlank(message = "子女手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "子女手机号格式不正确")
        private String phone;

        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class ElderItem {
        @NotBlank(message = "老人姓名不能为空")
        private String name;

        @NotBlank(message = "老人手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "老人手机号格式不正确")
        private String phone;

        @NotBlank(message = "与老人关系不能为空")
        private String relation;
    }
}
