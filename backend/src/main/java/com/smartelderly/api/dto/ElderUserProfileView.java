package com.smartelderly.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElderUserProfileView {
    private String name;
    private String phone;
    private String gender;
    private String birthday;
    private Boolean claimed;
    private Integer familyCount;
}
