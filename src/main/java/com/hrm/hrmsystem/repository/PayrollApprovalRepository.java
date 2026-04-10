package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.PayrollApproval;
import com.hrm.hrmsystem.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollApprovalRepository extends JpaRepository<PayrollApproval, Long> {
    
    /**
     * Find all approvals for a specific payroll
     */
    List<PayrollApproval> findByPayrollId(Long payrollId);
    
    /**
     * Find approval by payroll ID and approver role
     */
    Optional<PayrollApproval> findByPayrollIdAndApproverRole(Long payrollId, User.Role role);
    
    /**
     * Check if an approval exists with specific status
     */
    boolean existsByPayrollIdAndApproverRoleAndStatus(
        Long payrollId, 
        User.Role role, 
        PayrollApproval.ApprovalStatus status
    );
    
    /**
     * Find all approvals by a specific user
     */
    List<PayrollApproval> findByApprovedById(Long approvedById);
    
    /**
     * Find pending approvals for a specific role
     */
    List<PayrollApproval> findByApproverRoleAndStatus(
        User.Role role, 
        PayrollApproval.ApprovalStatus status
    );
    
    /**
     * Find pending approvals with eager fetching of payroll and employee
     * Uses EntityGraph to avoid LazyInitializationException
     */
    @EntityGraph(attributePaths = {"payroll", "payroll.employee"})
    @Query("SELECT pa FROM PayrollApproval pa WHERE pa.approverRole = :role AND pa.status = :status")
    List<PayrollApproval> findPendingApprovalsWithDetails(
        @Param("role") User.Role role, 
        @Param("status") PayrollApproval.ApprovalStatus status
    );
}
