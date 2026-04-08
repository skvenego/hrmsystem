package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.LeaveBalanceEnhanced;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceEnhancedRepository extends JpaRepository<LeaveBalanceEnhanced, Long> {
    
    /**
     * Find leave balance for employee in specific year
     */
    Optional<LeaveBalanceEnhanced> findByEmployeeIdAndYear(Long employeeId, Integer year);
    
    /**
     * Find all leave balances for an employee
     */
    List<LeaveBalanceEnhanced> findByEmployeeId(Long employeeId);
}
