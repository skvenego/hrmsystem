package com.hrm.hrmsystem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

/**
 * CUSTOM LEAVE REPOSITORY - DATABASE-FIRST CALCULATIONS
 * Single source of truth for all leave statistics
 * NO logic, NO calculations, ONLY database aggregation
 */
@Repository
public interface LeaveRepositoryCustom extends JpaRepository<com.hrm.hrmsystem.model.Leave, Long> {

    /**
     * Get total paid leave days for employee (all time)
     * DATABASE TRUTH - NO FILTERING, NO LOGIC
     */
    @Query("SELECT COALESCE(SUM(l.paidDays), 0.0) " +
            "FROM com.hrm.hrmsystem.model.Leave l " +
            "WHERE l.employee.id = :employeeId " +
            "AND l.status = 'APPROVED'")
    Double getTotalPaidLeaveDays(@Param("employeeId") Long employeeId);

    /**
     * Get total unpaid leave days for employee (all time)
     * DATABASE TRUTH - NO FILTERING, NO LOGIC
     */
    @Query("SELECT COALESCE(SUM(l.unpaidDays), 0.0) " +
            "FROM com.hrm.hrmsystem.model.Leave l " +
            "WHERE l.employee.id = :employeeId " +
            "AND l.status = 'APPROVED'")
    Double getTotalUnpaidLeaveDays(@Param("employeeId") Long employeeId);

    /**
     * Get paid leave days for employee in specific month
     * DATABASE TRUTH - CORRECT MONTH FILTERING
     */
    @Query("SELECT COALESCE(SUM(l.paidDays), 0.0) " +
            "FROM com.hrm.hrmsystem.model.Leave l " +
            "WHERE l.employee.id = :employeeId " +
            "AND l.status = 'APPROVED' " +
            "AND (l.startDate <= :monthEnd AND l.endDate >= :monthStart)")
    Double getMonthlyPaidLeaveDays(@Param("employeeId") Long employeeId,
                                 @Param("monthStart") LocalDate monthStart,
                                 @Param("monthEnd") LocalDate monthEnd);

    /**
     * Get unpaid leave days for employee in specific month
     * DATABASE TRUTH - CORRECT MONTH FILTERING
     */
    @Query("SELECT COALESCE(SUM(l.unpaidDays), 0.0) " +
            "FROM com.hrm.hrmsystem.model.Leave l " +
            "WHERE l.employee.id = :employeeId " +
            "AND l.status = 'APPROVED' " +
            "AND (l.startDate <= :monthEnd AND l.endDate >= :monthStart)")
    Double getMonthlyUnpaidLeaveDays(@Param("employeeId") Long employeeId,
                                   @Param("monthStart") LocalDate monthStart,
                                   @Param("monthEnd") LocalDate monthEnd);
}
