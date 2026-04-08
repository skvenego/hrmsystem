package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.LeaveSemester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveSemesterRepository extends JpaRepository<LeaveSemester, Long> {
    
    /**
     * Find all semesters for an employee in a specific year
     */
    List<LeaveSemester> findByEmployeeIdAndYear(Long employeeId, Integer year);
    
    /**
     * Find specific semester for employee
     */
    Optional<LeaveSemester> findByEmployeeIdAndYearAndSemester(
        Long employeeId, 
        Integer year, 
        LeaveSemester.SemesterType semester
    );
    
    /**
     * Find current active semester for employee
     */
    Optional<LeaveSemester> findByEmployeeIdAndIsActiveTrue(Long employeeId);
}
