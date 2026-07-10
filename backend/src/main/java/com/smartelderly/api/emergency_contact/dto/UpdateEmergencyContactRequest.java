package com.smartelderly.api.emergency_contact.dto;

public class UpdateEmergencyContactRequest {

    private String name;
    private String phone;
    private String relation;
    private Integer priority;

    public UpdateEmergencyContactRequest() {
    }

    public UpdateEmergencyContactRequest(String name, String phone, String relation, Integer priority) {
        this.name = name;
        this.phone = phone;
        this.relation = relation;
        this.priority = priority;
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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
