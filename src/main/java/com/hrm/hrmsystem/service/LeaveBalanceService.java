package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import com.hrm.hrmsystem.engine.AttendanceEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * LEAVE BALANCE ENGINE - Dynamic probation calculation
 * Clean, single source of truth for leave balance
 */
@Service
@RequiredArgsConstructor
public class LeaveBalanceService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRepository leaveRepository;
    private final AttendanceEngine attendanceEngine;

    /**
     * Leave Balance Result DTO
     */
    public static class LeaveBalanceResult {
        public double earned = 0;
        public double used = 0;
        public double remaining = 0;
        public int cycle = 1;
        public int year = 2026;
        
        @Override
        public String toString() {
            return String.format("LeaveBalance{earned=%.1f, used=%.1f, remaining=%.1f}",
                    earned, used, remaining);
        }
    }

    /**
     * 🔥 MAIN CALCULATION METHOD
     * Calculates leave balance with dynamic probation handling
     */
    public LeaveBalanceResult calculate(Long employeeId, YearMonth month) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        LeaveBalanceResult res = new LeaveBalanceResult();
        res.year = month.getYear();
        res.cycle = month.getMonthValue() <= 6 ? 1 : 2;

        LocalDate joinDate = emp.getJoiningDate();
        if (joinDate == null) {
            return res; // No join date = no leave balance
        }

        Integer probationMonths = emp.getProbationPeriodMonths();
        int probation = (probationMonths != null) ? probationMonths : 3;

        LocalDate probationEnd = joinDate.plusMonths(probation);
        LocalDate endOfMonth = month.atEndOfMonth();

        // 🔥 PROFESSIONAL HRM RULE:
        // If probation ends ON or BEFORE 15th → 1.5 credit on 1st of NEXT month
        // If probation ends AFTER 15th → 1.5 credit on 1st of month AFTER NEXT
        
        // Check if still in probation
        if (!endOfMonth.isAfter(probationEnd)) {
            return res; // Still in probation, no leave earned
        }

        // Calculate effective start month for leave accrual
        YearMonth effectiveStartMonth;
        int probationDay = probationEnd.getDayOfMonth();
        
        if (probationDay <= 15) {
            // Probation ended on/before 15th → start from NEXT month
            effectiveStartMonth = YearMonth.from(probationEnd).plusMonths(1);
        } else {
            // Probation ended after 15th → start from month AFTER next
            effectiveStartMonth = YearMonth.from(probationEnd).plusMonths(2);
        }

        // Cap at cycle start
        int cycleStartMonth = res.cycle == 1 ? 1 : 7;
        int cycleYear = res.year;
        YearMonth cycleStart = YearMonth.of(cycleYear, cycleStartMonth);
        
        // Start from effective month or cycle start, whichever is later
        YearMonth startMonth = effectiveStartMonth.isAfter(cycleStart) ? effectiveStartMonth : cycleStart;

        // Count months up to current month
        int months = 0;
        YearMonth iter = startMonth;
        while (!iter.isAfter(month)) {
            months++;
            iter = iter.plusMonths(1);
        }

        res.earned = months * 1.5;
        
        // STEP 13: Use ONLY AttendanceEngine for leave calculation (remove legacy methods)
        AttendanceEngine.LeaveBalanceSummary balanceSummary = attendanceEngine.calculateLeaveBalance(employeeId, month.getYear(), month.getMonthValue());
        res.used = balanceSummary.usedLeaves;
        res.remaining = Math.max(0, res.earned - res.used);

        return res;
    }

    /**
     * Calculate total earned leaves up to a specific date (for opening balance calculation)
     * STEP 13: Use ONLY AttendanceEngine for all calculations
     */
    public double calculateTotalEarned(Long employeeId, LocalDate tillDate) {
        YearMonth month = YearMonth.from(tillDate);
        AttendanceEngine.LeaveBalanceSummary balanceSummary = attendanceEngine.calculateLeaveBalance(employeeId, month.getYear(), month.getMonthValue());
        return balanceSummary.earnedLeaves;
    }

    // STEP 13: All legacy calculation methods removed - using ONLY AttendanceEngine
}
