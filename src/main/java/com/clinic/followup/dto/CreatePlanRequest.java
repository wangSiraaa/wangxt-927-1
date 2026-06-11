package com.clinic.followup.dto;

import com.clinic.followup.enums.RiskLevel;
import com.clinic.followup.enums.TransferStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreatePlanRequest {
    @NotBlank(message = "患者姓名不能为空")
    private String patientName;

    @NotBlank(message = "患者身份证号不能为空")
    private String patientIdCard;

    private String patientPhone;

    @NotNull(message = "出院日期不能为空")
    private LocalDate dischargeDate;

    @NotBlank(message = "病种不能为空")
    private String diseaseType;

    @NotNull(message = "风险等级不能为空")
    private RiskLevel riskLevel;

    @NotNull(message = "转院状态不能为空")
    private TransferStatus transferStatus;

    private String attendingDoctor;

    private String assignedNurse;

    private String remarks;

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getPatientIdCard() { return patientIdCard; }
    public void setPatientIdCard(String patientIdCard) { this.patientIdCard = patientIdCard; }
    public String getPatientPhone() { return patientPhone; }
    public void setPatientPhone(String patientPhone) { this.patientPhone = patientPhone; }
    public LocalDate getDischargeDate() { return dischargeDate; }
    public void setDischargeDate(LocalDate dischargeDate) { this.dischargeDate = dischargeDate; }
    public String getDiseaseType() { return diseaseType; }
    public void setDiseaseType(String diseaseType) { this.diseaseType = diseaseType; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public TransferStatus getTransferStatus() { return transferStatus; }
    public void setTransferStatus(TransferStatus transferStatus) { this.transferStatus = transferStatus; }
    public String getAttendingDoctor() { return attendingDoctor; }
    public void setAttendingDoctor(String attendingDoctor) { this.attendingDoctor = attendingDoctor; }
    public String getAssignedNurse() { return assignedNurse; }
    public void setAssignedNurse(String assignedNurse) { this.assignedNurse = assignedNurse; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
