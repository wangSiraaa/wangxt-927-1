package com.clinic.followup.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PatientFollowUpVO {
    private Long planId;
    private String patientName;
    private String diseaseType;
    private String riskLevel;
    private LocalDate dischargeDate;
    private LocalDate nextFollowUpDate;
    private String nextFollowUpNote;
    private boolean needExamReport;
    private String examReportNote;
    private String attendingDoctor;
    private String assignedNurse;
    private LocalDateTime lastContactTime;
    private String lastContactResult;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getDiseaseType() { return diseaseType; }
    public void setDiseaseType(String diseaseType) { this.diseaseType = diseaseType; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public LocalDate getDischargeDate() { return dischargeDate; }
    public void setDischargeDate(LocalDate dischargeDate) { this.dischargeDate = dischargeDate; }
    public LocalDate getNextFollowUpDate() { return nextFollowUpDate; }
    public void setNextFollowUpDate(LocalDate nextFollowUpDate) { this.nextFollowUpDate = nextFollowUpDate; }
    public String getNextFollowUpNote() { return nextFollowUpNote; }
    public void setNextFollowUpNote(String nextFollowUpNote) { this.nextFollowUpNote = nextFollowUpNote; }
    public boolean isNeedExamReport() { return needExamReport; }
    public void setNeedExamReport(boolean needExamReport) { this.needExamReport = needExamReport; }
    public String getExamReportNote() { return examReportNote; }
    public void setExamReportNote(String examReportNote) { this.examReportNote = examReportNote; }
    public String getAttendingDoctor() { return attendingDoctor; }
    public void setAttendingDoctor(String attendingDoctor) { this.attendingDoctor = attendingDoctor; }
    public String getAssignedNurse() { return assignedNurse; }
    public void setAssignedNurse(String assignedNurse) { this.assignedNurse = assignedNurse; }
    public LocalDateTime getLastContactTime() { return lastContactTime; }
    public void setLastContactTime(LocalDateTime lastContactTime) { this.lastContactTime = lastContactTime; }
    public String getLastContactResult() { return lastContactResult; }
    public void setLastContactResult(String lastContactResult) { this.lastContactResult = lastContactResult; }
}
