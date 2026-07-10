package com.smartelderly.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.smartelderly.util.TimeUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "families")
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_name", length = 100)
    private String familyName;

    @Column(name = "home_address", length = 255)
    private String homeAddress;

    @Column(name = "home_latitude", precision = 10, scale = 7)
    private BigDecimal homeLatitude;

    @Column(name = "home_longitude", precision = 10, scale = 7)
    private BigDecimal homeLongitude;

    @Column(name = "created_by_child_id")
    private Long createdByChildId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = TimeUtils.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = TimeUtils.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(String homeAddress) {
        this.homeAddress = homeAddress;
    }

    public BigDecimal getHomeLatitude() {
        return homeLatitude;
    }

    public void setHomeLatitude(BigDecimal homeLatitude) {
        this.homeLatitude = homeLatitude;
    }

    public BigDecimal getHomeLongitude() {
        return homeLongitude;
    }

    public void setHomeLongitude(BigDecimal homeLongitude) {
        this.homeLongitude = homeLongitude;
    }

    public Long getCreatedByChildId() {
        return createdByChildId;
    }

    public void setCreatedByChildId(Long createdByChildId) {
        this.createdByChildId = createdByChildId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
