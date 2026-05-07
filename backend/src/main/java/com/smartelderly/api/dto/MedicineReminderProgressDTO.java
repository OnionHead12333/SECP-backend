package com.smartelderly.api.dto;

public class MedicineReminderProgressDTO {
    public int plannedCount;
    public int confirmedCount;
    public int missedCount;
    public int pendingCount;
    public double completionPercent;
    public int activeReminderId;
    public String medicineName;
    public String doseDesc;
    public String lastConfirmedAt;
    public String nextReminderAt;
}