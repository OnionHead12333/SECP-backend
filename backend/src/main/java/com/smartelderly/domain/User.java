package com.smartelderly.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String role; // 'elder' or 'child'

    private String name;

    @Column(unique = true)
    private String phone;

    /** 与库表 users.gender ENUM 一致：male / female / unknown */
    @Column(name = "gender", length = 16)
    private String gender;

    @Column(name = "birthday")
    private LocalDate birthday;

    @PrePersist
    public void defaultGenderIfUnset() {
        if (gender == null || gender.isBlank()) {
            gender = "unknown";
        }
    }
}
