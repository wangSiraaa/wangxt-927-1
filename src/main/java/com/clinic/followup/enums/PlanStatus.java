package com.clinic.followup.enums;

public enum PlanStatus {
    ACTIVE("进行中"),
    ESCALATED("已升级"),
    COMPLETED("已完成"),
    CLOSED("已关闭"),
    CANCELLED("已取消");

    private final String description;

    PlanStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
