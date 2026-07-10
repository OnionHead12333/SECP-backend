package com.smartelderly.api.emergency_contact.dto;

public class ElderAddEmergencyContactRequest {

    private String elderPhone;
    private String name;
    private String phone;
    private String relation;
    private Boolean isPrimary;

    public ElderAddEmergencyContactRequest() {
    }

    public ElderAddEmergencyContactRequest(String elderPhone, String name, String phone, 
                                          String relation, Boolean isPrimary) {
        this.elderPhone = elderPhone;
        this.name = name;
        this.phone = phone;
        this.relation = relation;
        this.isPrimary = isPrimary;
    }

    public String getElderPhone() {
        return elderPhone;
    }

    public void setElderPhone(String elderPhone) {
        this.elderPhone = elderPhone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}
