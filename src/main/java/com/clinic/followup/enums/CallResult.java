package com.clinic.followup.enums;

public enum CallResult {
    CONNECTED("已接通"),
    NO_ANSWER("未接通"),
    BUSY("忙线"),
    WRONG_NUMBER("号码错误"),
    REFUSED("拒绝随访");

    private final String description;

    CallResult(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
