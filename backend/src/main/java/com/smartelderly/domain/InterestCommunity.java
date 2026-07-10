package com.smartelderly.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "interest_communities")
public class InterestCommunity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "short_description", nullable = false, length = 512)
    private String shortDescription;

    @Column(name = "preview_icon", nullable = false, length = 16)
    private String previewIcon;

    @Column(name = "member_hint", length = 64)
    private String memberHint;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    private Boolean isActive;
}
