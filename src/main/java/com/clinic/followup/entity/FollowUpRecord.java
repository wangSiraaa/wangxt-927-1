package com.clinic.followup.entity;

import com.clinic.followup.enums.CallResult;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "follow_up_record")
public class FollowUpRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private FollowUpPlan plan;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    private FollowUpNode node;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CallResult callResult;

    @Column(length = 200)
    private String noAnswerReason;

    @Column(length = 1000)
    private String conversationContent;

    private LocalDate nextReminderDate;

    @Column(length = 500)
    private String nextReminderNote;

    private boolean needExamReport = false;

    @Column(length = 200)
    private String examReportNote;

    @Column(length = 50)
    private String operatorName;

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
    public FollowUpNode getNode() { return node; }
    public void setNode(FollowUpNode node) { this.node = node; }
    public CallResult getCallResult() { return callResult; }
    public void setCallResult(CallResult callResult) { this.callResult = callResult; }
    public String getNoAnswerReason() { return noAnswerReason; }
    public void setNoAnswerReason(String noAnswerReason) { this.noAnswerReason = noAnswerReason; }
    public String getConversationContent() { return conversationContent; }
    public void setConversationContent(String conversationContent) { this.conversationContent = conversationContent; }
    public LocalDate getNextReminderDate() { return nextReminderDate; }
    public void setNextReminderDate(LocalDate nextReminderDate) { this.nextReminderDate = nextReminderDate; }
    public String getNextReminderNote() { return nextReminderNote; }
    public void setNextReminderNote(String nextReminderNote) { this.nextReminderNote = nextReminderNote; }
    public boolean isNeedExamReport() { return needExamReport; }
    public void setNeedExamReport(boolean needExamReport) { this.needExamReport = needExamReport; }
    public String getExamReportNote() { return examReportNote; }
    public void setExamReportNote(String examReportNote) { this.examReportNote = examReportNote; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
