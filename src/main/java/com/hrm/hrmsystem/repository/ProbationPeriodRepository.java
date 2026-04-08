package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.ProbationPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProbationPeriodRepository extends JpaRepository<ProbationPeriod, Long> {
    
    /**
     * Find probation period by employee ID
     */
    Optional<ProbationPeriod> findByEmployeeId(Long employeeId);
    
    /**
     * Check if employee has a probation period record
     */
    boolean existsByEmployeeId(Long employeeId);
}
