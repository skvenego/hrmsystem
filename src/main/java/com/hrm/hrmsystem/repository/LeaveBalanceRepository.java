package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByEmployeeIdAndYear(Long employeeId, Integer year);
    
    Optional<LeaveBalance> findByEmployeeIdAndYearAndCycle(Long employeeId, Integer year, Integer cycle);
    
    List<LeaveBalance> findByEmployeeId(Long employeeId);
}