package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.SalaryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryAdjustmentRepository extends JpaRepository<SalaryAdjustment, Long> {

    List<SalaryAdjustment> findByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    List<SalaryAdjustment> findByEmployeeId(Long employeeId);

    List<SalaryAdjustment> findByMonthAndYear(Integer month, Integer year);

    List<SalaryAdjustment> findByIsAppliedFalse();

    Optional<SalaryAdjustment> findByIdAndEmployeeId(Long id, Long employeeId);
}
