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
import java.time.temporal.ChronoUnit;
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
     * Uses LeaveRepository as SOURCE OF TRUTH for used leaves
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
        if (probationEnd.isAfter(endOfMonth)) {
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

        // ✅ Calculate earned leaves with 15th-day probation rule
        // Check if still in probation
        if (probationEnd.isAfter(month.atEndOfMonth())) {
            return res; // Still in probation, no leave earned
        }
        
        // Calculate effective start month for leave accrual
        LocalDate effectiveStart = probationEnd.withDayOfMonth(1).plusMonths(1); // Start earning from 1st of NEXT month after probation ends
        if (probationDay > 15) {
            effectiveStart = probationEnd.withDayOfMonth(1).plusMonths(2); // For >15th, start from 1st of month AFTER next
        }
        
        // Calculate months worked since effective start
        // Use month.atDay(1) to get first day of target month
        long monthsWorked = ChronoUnit.MONTHS.between(
            effectiveStart.withDayOfMonth(1),
            month.atDay(1)
        );
        
        // Include current month in calculation
        monthsWorked++;
        
        // Cap at 9 per cycle
        res.earned = Math.min(monthsWorked * 1.5, 9.0);

        // ✅ FIX: Use LeaveRepository DIRECTLY for used leaves (SOURCE OF TRUTH)
        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
                .filter(l -> {
                    LocalDate leaveStart = l.getStartDate();
                    LocalDate leaveEnd = l.getEndDate();
                    return leaveStart != null && leaveEnd != null &&
                           !leaveEnd.isBefore(monthStart) && !leaveStart.isAfter(monthEnd);
                })
                .toList();

        double paidLeave = 0.0;
        double unpaidLeave = 0.0;
        for (Leave leave : approvedLeaves) {
            if (leave.getPaidDays() != null) {
                paidLeave += leave.getPaidDays();
            }
            if (leave.getUnpaidDays() != null) {
                unpaidLeave += leave.getUnpaidDays();
            }
        }

        res.used = paidLeave + unpaidLeave;  // ✅ Used = Paid + Unpaid
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

    /**
     * Get complete leave summary for employee
     * ✅ Uses LeaveBalanceService's 15th-day probation rule
     * ✅ Calculates earned, used, remaining leaves correctly
     */
    public LeaveBalanceResult getLeaveSummary(Long empId, YearMonth month) {
        return calculate(empId, month);
    }
    
    // STEP 13: All legacy calculation methods removed - using ONLY AttendanceEngine
}
