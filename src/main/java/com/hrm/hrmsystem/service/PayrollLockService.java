package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.PayrollLock;
import com.hrm.hrmsystem.repository.PayrollLockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class PayrollLockService {

    @Autowired
    private PayrollLockRepository payrollLockRepository;

    /**
     * Check if payroll is locked for a specific month/year
     */
    public boolean isPayrollLocked(Integer month, Integer year) {
        Optional<PayrollLock> lock = payrollLockRepository.findByMonthAndYear(month, year);
        return lock.isPresent() && lock.get().getIsLocked();
    }

    /**
     * Lock payroll for a specific month/year
     */
    public PayrollLock lockPayroll(Integer month, Integer year, String remarks) {
        if (isPayrollLocked(month, year)) {
            throw new RuntimeException("Payroll for " + year + "-" + month + " is already locked");
        }

        String lockedBy = getCurrentUser();
        PayrollLock lock = new PayrollLock(month, year, lockedBy, remarks);
        return payrollLockRepository.save(lock);
    }

    /**
     * Unlock payroll for a specific month/year
     */
    public PayrollLock unlockPayroll(Integer month, Integer year, String remarks) {
        Optional<PayrollLock> lockOpt = payrollLockRepository.findByMonthAndYear(month, year);
        
        if (lockOpt.isEmpty()) {
            throw new RuntimeException("No lock found for " + year + "-" + month);
        }

        PayrollLock lock = lockOpt.get();
        lock.setIsLocked(false);
        if (remarks != null && !remarks.trim().isEmpty()) {
            lock.setRemarks(remarks);
        }
        
        return payrollLockRepository.save(lock);
    }

    /**
     * Get payroll lock details for a specific month/year
     */
    public Optional<PayrollLock> getPayrollLock(Integer month, Integer year) {
        return payrollLockRepository.findByMonthAndYear(month, year);
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
