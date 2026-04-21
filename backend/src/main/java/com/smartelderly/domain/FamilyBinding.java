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

    @Column(nullable = false)
    private String status; // 'pending' / 'active' / 'rejected' / 'removed'
}
