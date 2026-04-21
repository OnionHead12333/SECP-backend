package com.smartelderly.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "elder_profiles")
public class ElderProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String phone;

    private Long claimedUserId;

    @Column(nullable = false)
    private String status; // 'unclaimed' / 'claimed'

    private Long createdByChildId;
}
