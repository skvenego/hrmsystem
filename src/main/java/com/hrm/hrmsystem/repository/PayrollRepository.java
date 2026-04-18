package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    List<Payroll> findByEmployeeId(Long employeeId);

    Optional<Payroll> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    // Use this when there might be duplicates - returns the most recent one
    Optional<Payroll> findFirstByEmployeeIdAndMonthAndYearOrderByIdDesc(Long employeeId, Integer month, Integer year);

    List<Payroll> findByMonthAndYear(Integer month, Integer year);

    List<Payroll> findByStatus(Payroll.PayrollStatus status);
}