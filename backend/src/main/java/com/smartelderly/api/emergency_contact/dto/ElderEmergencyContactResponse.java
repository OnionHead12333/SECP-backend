package com.smartelderly.api.emergency_contact.dto;

public class ElderEmergencyContactResponse {

    private Long contactId;
    private String name;
    private String phone;
    private String relation;
    private Integer priority;
    private Boolean isPrimary;

    public ElderEmergencyContactResponse() {
    }

    public ElderEmergencyContactResponse(Long contactId, String name, String phone, 
                                        String relation, Integer priority, Boolean isPrimary) {
        this.contactId = contactId;
        this.name = name;
        this.phone = phone;
        this.relation = relation;
        this.priority = priority;
        this.isPrimary = isPrimary;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
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

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}
