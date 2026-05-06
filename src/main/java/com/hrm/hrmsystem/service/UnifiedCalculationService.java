package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.engine.AttendanceEngine;
import com.hrm.hrmsystem.engine.AttendanceSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

/**
 * 🔥 UNIFIED CALCULATION SERVICE
 * Single service layer for ALL calculations
 * Enforces: Controller → Service → Engine flow
 * Eliminates duplicate calculations across services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnifiedCalculationService {

    private final AttendanceEngine attendanceEngine;

    /**
     * Calculate attendance summary for payroll (salary calculation)
     * Ignores future dates - for salary/payslip calculations
     */
    @Transactional(readOnly = true)
    public AttendanceSummary calculateForPayroll(Long employeeId, int year, int month) {
        // ✅ Add validation
        if (employeeId == null || year <= 0 || month <= 0 || month > 12) {
            throw new IllegalArgumentException("Invalid input: employeeId cannot be null, year must be positive, month must be 1-12");
        }
        
        YearMonth yearMonth = YearMonth.of(year, month);
        
        // ✅ Service controls the flow - get opening balance first
        double openingBalance = calculateOpeningBalance(employeeId, year, month);
        
        log.debug("Calculating payroll attendance for employee {} for {}/{} with opening balance {}", employeeId, month, year, openingBalance);
        
        // Use engine's calculate() method (ignores future dates)
        return attendanceEngine.calculate(employeeId, yearMonth, openingBalance);
    }

    /**
     * Calculate attendance summary for UI (dashboard, planning)
     * Includes future approved leaves - for UI preview
     */
    @Transactional(readOnly = true)
    public AttendanceSummary calculateForUI(Long employeeId, int year, int month) {
        // ✅ Add validation
        if (employeeId == null || year <= 0 || month <= 0 || month > 12) {
            throw new IllegalArgumentException("Invalid input: employeeId cannot be null, year must be positive, month must be 1-12");
        }
        
        YearMonth yearMonth = YearMonth.of(year, month);
        
        log.debug("Calculating UI attendance for employee {} for {}/{}", employeeId, month, year);
        
        // Use engine's calculatePlanned() method (includes future dates)
        return attendanceEngine.calculatePlanned(employeeId, yearMonth);
    }

    /**
     * Calculate leave balance for an employee
     * Used by all leave-related operations
     */
    @Transactional(readOnly = true)
    public AttendanceEngine.LeaveBalanceSummary calculateLeaveBalance(Long employeeId, int year, int month) {
        // ✅ Add validation
        if (employeeId == null || year <= 0 || month <= 0 || month > 12) {
            throw new IllegalArgumentException("Invalid input: employeeId cannot be null, year must be positive, month must be 1-12");
        }
        
        log.debug("Calculating leave balance for employee {} for {}/{}", employeeId, month, year);
        return attendanceEngine.calculateLeaveBalance(employeeId, year, month);
    }

    /**
     * Calculate opening balance for a month
     * Used by payroll and payslip calculations
     */
    @Transactional(readOnly = true)
    public double calculateOpeningBalance(Long employeeId, int year, int month) {
        // ✅ Add validation
        if (employeeId == null || year <= 0 || month <= 0 || month > 12) {
            throw new IllegalArgumentException("Invalid input: employeeId cannot be null, year must be positive, month must be 1-12");
        }
        
        log.debug("Calculating opening balance for employee {} for {}/{}", employeeId, month, year);
        return attendanceEngine.calculateOpeningBalance(employeeId, year, month);
    }

    /**
     * Calculate salary components based on attendance
     * Used by payroll and payslip services
     */
    public AttendanceEngine.SalarySummary calculateSalary(
            com.hrm.hrmsystem.model.Employee employee, 
            AttendanceSummary attendanceSummary, 
            boolean isInProbation) {
        // ✅ Add validation
        if (employee == null || attendanceSummary == null) {
            throw new IllegalArgumentException("Invalid input: employee and attendanceSummary cannot be null");
        }
        
        log.debug("Calculating salary for employee {} with probation status: {}", employee.getId(), isInProbation);
        return attendanceEngine.calculateSalary(employee, attendanceSummary, isInProbation);
    }


}
