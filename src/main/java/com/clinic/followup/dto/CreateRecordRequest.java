package com.clinic.followup.dto;

import com.clinic.followup.enums.CallResult;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateRecordRequest {
    @NotNull(message = "计划ID不能为空")
    private Long planId;

    private Long nodeId;

    @NotNull(message = "通话结果不能为空")
    private CallResult callResult;

    private String noAnswerReason;

    private String conversationContent;

    private LocalDate nextReminderDate;

    private String nextReminderNote;

    private boolean needExamReport;

    private String examReportNote;

    private String operatorName;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Long getNodeId() { return nodeId; }
    public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
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
}
