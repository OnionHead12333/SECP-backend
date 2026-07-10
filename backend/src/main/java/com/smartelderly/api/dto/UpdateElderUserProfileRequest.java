package com.smartelderly.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 老人端更新个人信息：对应 users 与所关联 elder_profiles 的 name/gender/birthday。
 */
@Data
public class UpdateElderUserProfileRequest {

    @NotBlank
    @Size(max = 30, message = "姓名长度不超过 30")
    private String name;

    @NotBlank
    @Pattern(regexp = "^(male|female|unknown)$", message = "性别取值无效")
    private String gender;

    /** 可选；不填可传 null。 */
    private LocalDate birthday;
}
