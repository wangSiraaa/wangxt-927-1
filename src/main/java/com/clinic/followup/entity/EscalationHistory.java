package com.clinic.followup.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "escalation_history")
public class EscalationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private FollowUpPlan plan;

    @Column(nullable = false, length = 50)
    private String escalationType;

    @Column(nullable = false, length = 20)
    private String fromRole;

    @Column(nullable = false, length = 50)
    private String toDoctor;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(length = 500)
    private String doctorNote;

    private boolean resolved = false;

    private LocalDateTime resolvedAt;

    @Column(length = 500)
    private String resolution;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public FollowUpPlan getPlan() { return plan; }
    public void setPlan(FollowUpPlan plan) { this.plan = plan; }
    public String getEscalationType() { return escalationType; }
    public void setEscalationType(String escalationType) { this.escalationType = escalationType; }
    public String getFromRole() { return fromRole; }
    public void setFromRole(String fromRole) { this.fromRole = fromRole; }
    public String getToDoctor() { return toDoctor; }
    public void setToDoctor(String toDoctor) { this.toDoctor = toDoctor; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getDoctorNote() { return doctorNote; }
    public void setDoctorNote(String doctorNote) { this.doctorNote = doctorNote; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
