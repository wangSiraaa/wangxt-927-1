package com.clinic.followup.enums;

public enum RiskLevel {
    HIGH("高风险"),
    MEDIUM("中风险"),
    LOW("低风险");

    private final String description;

    RiskLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
