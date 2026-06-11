package com.clinic.followup.entity;

import com.clinic.followup.enums.PlanStatus;
import com.clinic.followup.enums.RiskLevel;
import com.clinic.followup.enums.TransferStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "follow_up_plan")
public class FollowUpPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String patientName;

    @Column(nullable = false, length = 20)
    private String patientIdCard;

    @Column(length = 20)
    private String patientPhone;

    @Column(nullable = false)
    private LocalDate dischargeDate;

    @Column(nullable = false, length = 100)
    private String diseaseType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus transferStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanStatus status = PlanStatus.ACTIVE;

    @Column(length = 50)
    private String attendingDoctor;

    @Column(length = 50)
    private String assignedNurse;

    private Integer consecutiveMissed = 0;

    @Column(length = 500)
    private String remarks;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FollowUpRecord> records = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FollowUpNode> nodes = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EscalationHistory> escalationHistories = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public PlanStatus getStatus() { return status; }
    public void setStatus(PlanStatus status) { this.status = status; }
    public String getAttendingDoctor() { return attendingDoctor; }
    public void setAttendingDoctor(String attendingDoctor) { this.attendingDoctor = attendingDoctor; }
    public String getAssignedNurse() { return assignedNurse; }
    public void setAssignedNurse(String assignedNurse) { this.assignedNurse = assignedNurse; }
    public Integer getConsecutiveMissed() { return consecutiveMissed; }
    public void setConsecutiveMissed(Integer consecutiveMissed) { this.consecutiveMissed = consecutiveMissed; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<FollowUpRecord> getRecords() { return records; }
    public void setRecords(List<FollowUpRecord> records) { this.records = records; }
    public List<FollowUpNode> getNodes() { return nodes; }
    public void setNodes(List<FollowUpNode> nodes) { this.nodes = nodes; }
    public List<EscalationHistory> getEscalationHistories() { return escalationHistories; }
    public void setEscalationHistories(List<EscalationHistory> escalationHistories) { this.escalationHistories = escalationHistories; }
}
