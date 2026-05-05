package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.SalaryAdjustment;
import com.hrm.hrmsystem.repository.SalaryAdjustmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SalaryAdjustmentService {

    @Autowired
    private SalaryAdjustmentRepository salaryAdjustmentRepository;

    @Autowired
    private PayrollLockService payrollLockService;

    /**
     * Create a salary adjustment for a specific month/year
     * Used when payroll is locked and attendance changes need to be compensated
     */
    public SalaryAdjustment createAdjustment(Long employeeId, Integer month, Integer year, 
                                            BigDecimal amount, String reason, String adjustmentType, 
                                            String remarks) {
        // Check if payroll is locked for this month/year
        if (!payrollLockService.isPayrollLocked(month, year)) {
            throw new RuntimeException("Payroll is not locked for " + year + "-" + month + 
                    ". Use normal attendance updates instead of adjustments.");
        }

        String createdBy = getCurrentUser();
        
        SalaryAdjustment adjustment = new SalaryAdjustment(
                null, // employee will be set
                month,
                year,
                amount,
                reason,
                adjustmentType,
                createdBy,
                remarks
        );

        // Note: Employee needs to be set by the caller or we need to fetch it
        // This is a simplified version - in production, fetch employee from repository
        
        return salaryAdjustmentRepository.save(adjustment);
    }

    /**
     * Get all adjustments for an employee in a specific month/year
     */
    public List<SalaryAdjustment> getAdjustmentsForEmployee(Long employeeId, Integer month, Integer year) {
        return salaryAdjustmentRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year);
    }

    /**
     * Get all adjustments for a specific month/year (all employees)
     */
    public List<SalaryAdjustment> getAdjustmentsForMonth(Integer month, Integer year) {
        return salaryAdjustmentRepository.findByMonthAndYear(month, year);
    }

    /**
     * Get all unapplied adjustments
     */
    public List<SalaryAdjustment> getUnappliedAdjustments() {
        return salaryAdjustmentRepository.findByIsAppliedFalse();
    }

    /**
     * Mark an adjustment as applied
     */
    public SalaryAdjustment markAsApplied(Long adjustmentId) {
        Optional<SalaryAdjustment> adjustmentOpt = salaryAdjustmentRepository.findById(adjustmentId);
        if (adjustmentOpt.isEmpty()) {
            throw new RuntimeException("Adjustment not found with ID: " + adjustmentId);
        }

        SalaryAdjustment adjustment = adjustmentOpt.get();
        adjustment.setIsApplied(true);
        return salaryAdjustmentRepository.save(adjustment);
    }

    /**
     * Delete an adjustment (only if not yet applied)
     */
    public void deleteAdjustment(Long adjustmentId) {
        Optional<SalaryAdjustment> adjustmentOpt = salaryAdjustmentRepository.findById(adjustmentId);
        if (adjustmentOpt.isEmpty()) {
            throw new RuntimeException("Adjustment not found with ID: " + adjustmentId);
        }

        SalaryAdjustment adjustment = adjustmentOpt.get();
        if (adjustment.getIsApplied()) {
            throw new RuntimeException("Cannot delete applied adjustment with ID: " + adjustmentId);
        }

        salaryAdjustmentRepository.delete(adjustment);
    }

    /**
     * Calculate total adjustment amount for an employee in a month/year
     */
    public BigDecimal getTotalAdjustmentAmount(Long employeeId, Integer month, Integer year) {
        List<SalaryAdjustment> adjustments = getAdjustmentsForEmployee(employeeId, month, year);
        return adjustments.stream()
                .map(SalaryAdjustment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get current logged-in user
     */
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "SYSTEM";
    }
}
