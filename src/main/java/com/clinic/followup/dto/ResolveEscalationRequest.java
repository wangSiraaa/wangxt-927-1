package com.clinic.followup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ResolveEscalationRequest {
    @NotNull(message = "升级记录ID不能为空")
    private Long escalationId;

    private String doctorNote;

    @NotBlank(message = "处理结果不能为空")
    private String resolution;

    private boolean resumeFollowUp = true;

    public Long getEscalationId() { return escalationId; }
    public void setEscalationId(Long escalationId) { this.escalationId = escalationId; }
    public String getDoctorNote() { return doctorNote; }
    public void setDoctorNote(String doctorNote) { this.doctorNote = doctorNote; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public boolean isResumeFollowUp() { return resumeFollowUp; }
    public void setResumeFollowUp(boolean resumeFollowUp) { this.resumeFollowUp = resumeFollowUp; }
}
