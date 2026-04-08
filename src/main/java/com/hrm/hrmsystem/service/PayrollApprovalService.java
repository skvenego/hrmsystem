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
    
    public PayrollApprovalService(
        PayrollApprovalRepository approvalRepo,
        PayrollRepository payrollRepo
    ) {
        this.approvalRepo = approvalRepo;
        this.payrollRepo = payrollRepo;
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
        
        PayrollApproval directorApproval = new PayrollApproval(
            payroll, 
            User.Role.ROLE_DIRECTOR
        );
        
        approvalRepo.save(accountantApproval);
        approvalRepo.save(directorApproval);
        
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
        
        return approval;
    }
    
    /**
     * Approve payroll by Director
     */
    @Transactional
    public PayrollApproval approveByDirector(
        Long payrollId, 
        User approver, 
        String comments
    ) {
        Payroll payroll = payrollRepo.findById(payrollId)
            .orElseThrow(() -> new RuntimeException("Payroll not found"));
        
        Optional<PayrollApproval> approvalOpt = approvalRepo.findByPayrollIdAndApproverRole(
            payrollId, 
            User.Role.ROLE_DIRECTOR
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
        payroll.setDirectorApproved(true);
        checkAndUpdateFinalApproval(payroll);
        
        System.out.println("✅ Director approved Payroll #" + payrollId);
        
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
        
        status.append(" | Director: ");
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
     */
    public List<PayrollApproval> getPendingApprovalsForRole(User.Role role) {
        return approvalRepo.findByApproverRoleAndStatus(
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
