package com.clinic.followup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EscalateRequest {
    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotBlank(message = "升级类型不能为空")
    private String escalationType;

    @NotBlank(message = "来源角色不能为空")
    private String fromRole;

    @NotBlank(message = "目标医生不能为空")
    private String toDoctor;

    @NotBlank(message = "升级原因不能为空")
    private String reason;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getEscalationType() { return escalationType; }
    public void setEscalationType(String escalationType) { this.escalationType = escalationType; }
    public String getFromRole() { return fromRole; }
    public void setFromRole(String fromRole) { this.fromRole = fromRole; }
    public String getToDoctor() { return toDoctor; }
    public void setToDoctor(String toDoctor) { this.toDoctor = toDoctor; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
