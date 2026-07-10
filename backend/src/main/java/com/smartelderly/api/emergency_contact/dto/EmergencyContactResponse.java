package com.smartelderly.api.emergency_contact.dto;

public class EmergencyContactResponse {

    private Long id;
    private Long elderId;
    private String name;
    private String phone;
    private String relation;
    private Integer priority;
    private String createdAt;
    private String updatedAt;

    public EmergencyContactResponse() {
    }

    public EmergencyContactResponse(Long id, Long elderId, String name, String phone, 
                                   String relation, Integer priority, String createdAt, String updatedAt) {
        this.id = id;
        this.elderId = elderId;
        this.name = name;
        this.phone = phone;
        this.relation = relation;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getElderId() {
        return elderId;
    }

    public void setElderId(Long elderId) {
        this.elderId = elderId;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
