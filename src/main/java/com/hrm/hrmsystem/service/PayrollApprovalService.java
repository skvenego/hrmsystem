package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.*;
import com.hrm.hrmsystem.repository.PayrollApprovalRepository;
import com.hrm.hrmsystem.repository.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PayrollApprovalService {
    
    private final PayrollApprovalRepository approvalRepo;
    private final PayrollRepository payrollRepo;
    private final NotificationService notificationService;
    
    public PayrollApprovalService(
        PayrollApprovalRepository approvalRepo,
        PayrollRepository payrollRepo,
        NotificationService notificationService
    ) {
        this.approvalRepo = approvalRepo;
        this.payrollRepo = payrollRepo;
        this.notificationService = notificationService;
    }
    
    /**
     * Initiate dual approval workflow for a payroll
     */
    @Transactional
    public void initiateApprovalWorkflow(Payroll payroll) {
        payroll.setStatus(Payroll.PayrollStatus.PENDING_APPROVAL);
        payroll.setAccountantApproved(false);
        payroll.setDirectorApproved(false);
        payroll.setPayslipGenerated(false);
        payrollRepo.save(payroll);
        
        // Create approval records for both roles
        PayrollApproval accountantApproval = new PayrollApproval(
            payroll, 
            User.Role.ROLE_ACCOUNTANT
        );
        
        PayrollApproval adminApproval = new PayrollApproval(
            payroll, 
            User.Role.ROLE_ADMIN
        );
        
        approvalRepo.save(accountantApproval);
        approvalRepo.save(adminApproval);
        
        System.out.println("✅ Payroll #" + payroll.getId() + " sent for dual approval");
    }
    
    /**
     * Approve payroll by Accountant
     */
    @Transactional
    public PayrollApproval approveByAccountant(
        Long payrollId, 
        User approver, 
        String comments
    ) {
        Payroll payroll = payrollRepo.findById(payrollId)
            .orElseThrow(() -> new RuntimeException("Payroll not found"));
        
        Optional<PayrollApproval> approvalOpt = approvalRepo.findByPayrollIdAndApproverRole(
            payrollId, 
            User.Role.ROLE_ACCOUNTANT
        );
        
        if (approvalOpt.isEmpty()) {
            throw new RuntimeException("Approval record not found");
        }
        
        PayrollApproval approval = approvalOpt.get();
        approval.setStatus(PayrollApproval.ApprovalStatus.APPROVED);
        approval.setApprovedBy(approver);
        approval.setComments(comments != null ? comments : "");
        
        approvalRepo.save(approval);
        
        // Update payroll
        payroll.setAccountantApproved(true);
        checkAndUpdateFinalApproval(payroll);
        
        System.out.println("✅ Accountant approved Payroll #" + payrollId);
        
        if (!payroll.getDirectorApproved()) {
            try {
                String title = "Payslip Pending Admin Approval";
                String msg = "Accountant has approved the payslip for " + payroll.getEmployee().getFirstName() + " (" + payroll.getMonth() + "/" + payroll.getYear() + "). Pending your final approval.";
                notificationService.sendInAppNotificationToRole(User.Role.ROLE_ADMIN, title, msg, "PAYSLIP_APPROVAL", "/html/admin-approvals.html");
            } catch (Exception e) {
                System.err.println("❌ Failed to send in-app notification to admin: " + e.getMessage());
            }
        }
        
        return approval;
    }
    
    /**
     * Approve payroll by Admin
     */
    @Transactional
    public PayrollApproval approveByAdmin(
        Long payrollId, 
        User approver, 
        String comments
    ) {
        Payroll payroll = payrollRepo.findById(payrollId)
            .orElseThrow(() -> new RuntimeException("Payroll not found"));
        
        Optional<PayrollApproval> approvalOpt = approvalRepo.findByPayrollIdAndApproverRole(
            payrollId, 
            User.Role.ROLE_ADMIN
        );
        
        if (approvalOpt.isEmpty()) {
            throw new RuntimeException("Approval record not found");
        }
        
        PayrollApproval approval = approvalOpt.get();
        approval.setStatus(PayrollApproval.ApprovalStatus.APPROVED);
        approval.setApprovedBy(approver);
        approval.setComments(comments != null ? comments : "");
        
        approvalRepo.save(approval);
        
        // Update payroll
        payroll.setDirectorApproved(true); // Using this field for Admin approval
        checkAndUpdateFinalApproval(payroll);
        
        System.out.println("✅ Admin approved Payroll #" + payrollId);
        
        if (!payroll.getAccountantApproved()) {
            try {
                String title = "Payslip Pending Accountant Approval";
                String msg = "HR has approved the payslip for " + payroll.getEmployee().getFirstName() + " (" + payroll.getMonth() + "/" + payroll.getYear() + "). Pending your final approval.";
                notificationService.sendInAppNotificationToRole(User.Role.ROLE_ACCOUNTANT, title, msg, "PAYSLIP_APPROVAL", "/html/accountant-approvals.html");
            } catch (Exception e) {
                System.err.println("❌ Failed to send in-app notification to accountant: " + e.getMessage());
            }
        }
        
        return approval;
    }
    
    /**
     * Reject payroll approval
     */
    @Transactional
    public PayrollApproval rejectApproval(
        Long payrollId, 
        User rejector, 
        User.Role role, 
        String reason
    ) {
        Payroll payroll = payrollRepo.findById(payrollId)
            .orElseThrow(() -> new RuntimeException("Payroll not found"));
        
        Optional<PayrollApproval> approvalOpt = approvalRepo.findByPayrollIdAndApproverRole(
            payrollId, 
            role
        );
        
        if (approvalOpt.isEmpty()) {
            throw new RuntimeException("Approval record not found");
        }
        
        PayrollApproval approval = approvalOpt.get();
        approval.setStatus(PayrollApproval.ApprovalStatus.REJECTED);
        approval.setApprovedBy(rejector);
        approval.setRejectionReason(reason != null ? reason : "No reason provided");
        
        approvalRepo.save(approval);
        
        // Reset payroll status back to processed for correction
        payroll.setStatus(Payroll.PayrollStatus.PROCESSED);
        if (role == User.Role.ROLE_ACCOUNTANT) {
            payroll.setAccountantApproved(false);
        } else {
            payroll.setDirectorApproved(false);
        }
        
        payrollRepo.save(payroll);
        
        System.out.println("❌ Payroll #" + payrollId + " rejected by " + role);
        
        return approval;
    }
    
    /**
     * Check if both approvals are complete and finalize
     */
    private void checkAndUpdateFinalApproval(Payroll payroll) {
        if (payroll.getAccountantApproved() && payroll.getDirectorApproved()) {
            // Both approvals received!
            payroll.setStatus(Payroll.PayrollStatus.PAID);
            payroll.setFinalApprovalDate(LocalDate.now());
            payroll.setPayslipGenerated(true);
            payrollRepo.save(payroll);
            
            System.out.println("✅✅ Payroll #" + payroll.getId() + " FULLY APPROVED!");
            System.out.println("📄 Payslip now available to employee");

            // Send notification to employee
            try {
                notificationService.sendPayrollPaymentNotification(payroll);
                
                String title = "Payslip Fully Approved";
                String msg = "Your payslip for " + payroll.getMonth() + "/" + payroll.getYear() + " has been fully approved and processed.";
                notificationService.sendInAppNotificationToEmployee(payroll.getEmployee(), title, msg, "INFO", "/html/my-payslips.html");
            } catch (Exception e) {
                System.err.println("❌ Failed to send payroll notification: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get approval status display string
     */
    public String getApprovalStatusDisplay(Payroll payroll) {
        StringBuilder status = new StringBuilder();
        
        status.append("Accountant: ");
        if (payroll.getAccountantApproved()) {
            status.append("✅ APPROVED");
        } else {
            status.append("⏳ PENDING");
        }
        
        status.append(" | Admin: ");
        if (payroll.getDirectorApproved()) {
            status.append("✅ APPROVED");
        } else {
            status.append("⏳ PENDING");
        }
        
        if (payroll.getPayslipGenerated()) {
            status.append(" | 📄 Payslip Ready!");
        }
        
        return status.toString();
    }
    
    /**
     * Check if user can approve this payroll based on their role
     */
    public boolean canUserApprove(Long payrollId, User user) {
        Optional<PayrollApproval> approvalOpt = approvalRepo.findByPayrollIdAndApproverRole(
            payrollId, 
            user.getRole()
        );
        
        if (approvalOpt.isEmpty()) {
            return false;
        }
        
        PayrollApproval approval = approvalOpt.get();
        return approval.getStatus() == PayrollApproval.ApprovalStatus.PENDING;
    }
    
    /**
     * Get pending approvals for a specific role
     * Uses EntityGraph to eagerly fetch payroll and employee data
     */
    @Transactional(readOnly = true)
    public List<PayrollApproval> getPendingApprovalsForRole(User.Role role) {
        return approvalRepo.findPendingApprovalsWithDetails(
            role, 
            PayrollApproval.ApprovalStatus.PENDING
        );
    }
    
    /**
     * Get all approvals for a payroll
     */
    public List<PayrollApproval> getApprovalsForPayroll(Long payrollId) {
        return approvalRepo.findByPayrollId(payrollId);
    }
}
