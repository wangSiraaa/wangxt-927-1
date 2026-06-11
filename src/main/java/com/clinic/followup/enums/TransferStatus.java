package com.clinic.followup.enums;

public enum TransferStatus {
    NOT_TRANSFERRED("未转院"),
    TRANSFERRED("已转院");

    private final String description;

    TransferStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
