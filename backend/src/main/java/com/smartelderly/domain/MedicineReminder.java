package com.smartelderly.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "medicine_reminders")
public class MedicineReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int id;
    @Column(name = "elder_profile_id")
    public int elderId;
    @Column(name = "medicine_name")
    public String medicineName;
    public String dosage;
    @Column(name = "remind_time")
    public LocalDateTime remindTime;
    public boolean enabled;
    // 可能的其他字段
}