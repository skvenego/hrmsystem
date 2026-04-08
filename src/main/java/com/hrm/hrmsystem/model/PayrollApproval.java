package com.hrm.hrmsystem.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payroll Approval Workflow - Tracks multi-level approvals
 */
@Entity
@Table(name = "payroll_approvals")
public class PayrollApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_id", nullable = false)
    private Payroll payroll;

    /** Approver role: ACCOUNTANT or DIRECTOR */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private User.Role approverRole;

    /** Actual user who approved */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    /** Approval status */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    /** Approval date and time */
    private LocalDateTime approvedAt;

    /** Rejection reason if rejected */
    @Column(length = 500)
    private String rejectionReason;

    /** Comments/notes */
    @Column(length = 500)
    private String comments;

    @PrePersist
    protected void onCreate() {
        if (this.status == ApprovalStatus.APPROVED && this.approvedAt == null) {
            this.approvedAt = LocalDateTime.now();
        }
    }

    // Constructors
    public PayrollApproval() {}

    public PayrollApproval(Payroll payroll, User.Role approverRole) {
        this.payroll = payroll;
        this.approverRole = approverRole;
        this.status = ApprovalStatus.PENDING;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Payroll getPayroll() { return payroll; }
    public void setPayroll(Payroll payroll) { this.payroll = payroll; }
    
    public User.Role getApproverRole() { return approverRole; }
    public void setApproverRole(User.Role approverRole) { this.approverRole = approverRole; }
    
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    
    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { 
        this.status = status;
        if (status == ApprovalStatus.APPROVED && this.approvedAt == null) {
            this.approvedAt = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    // Enum for approval status
    public enum ApprovalStatus {
        PENDING,    // Waiting for approval
        APPROVED,   // Approved by this role
        REJECTED    // Rejected by this role
    }

    // Helper methods
    public boolean isApproved() {
        return this.status == ApprovalStatus.APPROVED;
    }

    public boolean isPending() {
        return this.status == ApprovalStatus.PENDING;
    }

    public boolean isRejected() {
        return this.status == ApprovalStatus.REJECTED;
    }
}
