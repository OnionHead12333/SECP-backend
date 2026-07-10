package com.smartelderly.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "family_bindings")
public class FamilyBinding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long elderProfileId;

    @Column(nullable = false)
    private Long childUserId;

    private String relation;

    private Boolean isPrimary;

    /**
     * 与库表 {@code family_bindings.status}（字符串）及 {@link BindingStatus} 一致，避免与
     * {@link FamilyBindingRepository#findByChildUserIdAndStatus(Long, BindingStatus)} 参数类型错配（Hibernate 6）。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BindingStatus status;
}
