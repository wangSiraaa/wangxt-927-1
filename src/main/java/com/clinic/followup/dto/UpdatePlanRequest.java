package com.clinic.followup.dto;

import com.clinic.followup.enums.PlanStatus;
import com.clinic.followup.enums.RiskLevel;
import com.clinic.followup.enums.TransferStatus;
import java.time.LocalDate;

public class UpdatePlanRequest {
    private LocalDate dischargeDate;
    private String diseaseType;
    private RiskLevel riskLevel;
    private TransferStatus transferStatus;
    private PlanStatus status;
    private String attendingDoctor;
    private String assignedNurse;
    private String remarks;

    public LocalDate getDischargeDate() { return dischargeDate; }
    public void setDischargeDate(LocalDate dischargeDate) { this.dischargeDate = dischargeDate; }
    public String getDiseaseType() { return diseaseType; }
    public void setDiseaseType(String diseaseType) { this.diseaseType = diseaseType; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public TransferStatus getTransferStatus() { return transferStatus; }
    public void setTransferStatus(TransferStatus transferStatus) { this.transferStatus = transferStatus; }
    public PlanStatus getStatus() { return status; }
    public void setStatus(PlanStatus status) { this.status = status; }
    public String getAttendingDoctor() { return attendingDoctor; }
    public void setAttendingDoctor(String attendingDoctor) { this.attendingDoctor = attendingDoctor; }
    public String getAssignedNurse() { return assignedNurse; }
    public void setAssignedNurse(String assignedNurse) { this.assignedNurse = assignedNurse; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
