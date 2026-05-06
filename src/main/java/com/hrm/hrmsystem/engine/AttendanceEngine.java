package com.hrm.hrmsystem.engine;

import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.repository.AttendanceRepository;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * CORE ENGINE: Single source of truth for attendance + leave calculation
 * Follows strict priority: Leave > Attendance > Unmarked
 */
@Component
@RequiredArgsConstructor
public class AttendanceEngine {

    private final LeaveRepository leaveRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    // 🔥 SINGLE SOURCE OF TRUTH CONSTANTS
    public static final int WORKING_DAYS_PER_MONTH = 26;
    public static final double LEAVE_ACCRUAL_RATE = 1.5;

    /**
     * 🔥 MAIN CALCULATION METHOD
     * Calculates attendance summary for an employee for a specific month
     * 
     * Priority: Leave > Attendance > Unmarked
     * Sunday: Completely ignored
     */
    public AttendanceSummary calculate(Long employeeId, YearMonth month, double openingBalance) {
        AttendanceSummary result = new AttendanceSummary();

        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        // Fetch all data upfront to avoid N+1 queries
        // STEP 14: Fix leave fetching - include only leaves that overlap with the calculation month
        // IMPORTANT: Only count APPROVED leaves - REJECTED leaves are excluded from all calculations
        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED) // ONLY APPROVED LEAVES
                .filter(l -> {
                    // Include leaves that overlap with the calculation month (start to end)
                    LocalDate leaveStart = l.getStartDate();
                    LocalDate leaveEnd = l.getEndDate();
                    return leaveStart != null && leaveEnd != null && 
                           !leaveEnd.isBefore(start) && !leaveStart.isAfter(end);
                })
                .toList();
        
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, start, end);
        
        // STEP 7: Use Map for FAST attendance lookup
        Map<LocalDate, Attendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(Attendance::getDate, a -> a, (a1, a2) -> a1));
        
        // Fetch all data upfront to avoid N+1 queries

        LocalDate today = LocalDate.now();
        
        // DEBUG: Log calculation parameters
        System.out.println("DEBUG CALCULATE - Employee: " + employeeId + ", Month: " + month);
        System.out.println("DEBUG - Date range: " + start + " to " + end);
        System.out.println("DEBUG - Today: " + today);
        System.out.println("DEBUG - Found leaves: " + approvedLeaves.size());
        System.out.println("DEBUG - Found attendance records: " + attendances.size());

        // FULL MONTH CALCULATION for payroll/payslip
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            
            // STEP 1: SKIP SUNDAY COMPLETELY
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }

            // ✅ HARD RULE: salary ignores future completely
            if (date.isAfter(today)) {
                continue;
            }

            boolean handled = false;
            final LocalDate currentDate = date;

            // STEP 3: CHECK LEAVE (Priority 1) - Leave > Attendance > Unmarked
            Leave leave = approvedLeaves.stream()
                    .filter(l -> !currentDate.isBefore(l.getStartDate()) && !currentDate.isAfter(l.getEndDate()))
                    .findFirst()
                    .orElse(null);

            if (leave != null) {
                boolean isHalfDay = Boolean.TRUE.equals(leave.getIsHalfDay());
                double leaveValue = isHalfDay ? 0.5 : 1.0;

                // ✅ FIX 1: Add fallback for paidDays/unpaidDays
                // If both are NOT null → use them
                // If NULL → fallback using leaveType
                Double frozenPaidDays = leave.getPaidDays();
                Double frozenUnpaidDays = leave.getUnpaidDays();
                
                if (frozenPaidDays != null && frozenUnpaidDays != null && frozenPaidDays + frozenUnpaidDays > 0) {
                    // Use frozen database values - calculate proportion for this date
                    double totalFrozenDays = frozenPaidDays + frozenUnpaidDays;
                    double paidProportion = frozenPaidDays / totalFrozenDays;
                    double unpaidProportion = frozenUnpaidDays / totalFrozenDays;
                    
                    result.paidLeave += leaveValue * paidProportion;
                    result.unpaidLeave += leaveValue * unpaidProportion;
                    result.present += leaveValue * paidProportion;  // Paid leave counts as present
                    result.absent += leaveValue * unpaidProportion;  // Unpaid leave counts as absent
                } else {
                    // ✅ FIX 2: Do NOT skip leave if DB values are null - use fallback
                    if (Leave.LeaveType.UNPAID.equals(leave.getLeaveType())) {
                        result.unpaidLeave += leaveValue;
                        result.absent += leaveValue;  // Unpaid leave counts as absent
                    } else {
                        result.paidLeave += leaveValue;
                        result.present += leaveValue;  // Paid leave counts as present
                    }
                }
                handled = true;
            }

            // STEP 4: CHECK ATTENDANCE (Priority 2) - only if no leave
            if (!handled) {
                Attendance att = attendanceMap.get(currentDate);  // STEP 7: Fast lookup

                if (att != null && att.getStatus() != null) {
                    switch (att.getStatus()) {
                        case PRESENT:
                        case LATE:
                            result.present += 1;
                            break;
                        case HALF_DAY:
                            result.present += 0.5;
                            result.absent += 0.5;
                            break;
                        case ABSENT:
                            result.absent += 1;
                            break;
                        case PENDING:
                            // STEP 9: DO NOT COUNT PENDING as absent
                            break;
                        case ON_LEAVE:
                            // Already handled in leave section
                            break;
                        default:
                            result.absent += 1;
                            break;
                    }
                    handled = true;
                }
            }

            // STEP 5: UNMARKED (no attendance record) → ABSENT for payroll
            if (!handled) {
                result.absent += 1;
                result.unpaidLeave += 1;
            }
        }

        return result;
    }

    /**
     * �* MODE 2: PLANNED (FOR UI PREVIEW)
     * Used for:
     * - UI display
     * - Leave planning  
     * - Dashboard preview
     * 
     * Rule: COUNT future approved leaves BUT DO NOT affect salary
     */
    public AttendanceSummary calculatePlanned(Long employeeId, YearMonth month) {
        AttendanceSummary result = new AttendanceSummary();

        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        // DEBUG: Log calculation parameters
        System.out.println("DEBUG CALCULATE PLANNED - Employee: " + employeeId + ", Month: " + month);
        System.out.println("DEBUG - Date range: " + start + " to " + end);

        // Fetch all approved leaves upfront to avoid N+1 queries
        // IMPORTANT: Only count APPROVED leaves - REJECTED leaves are excluded from all calculations
        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED) // ONLY APPROVED LEAVES
                .filter(l -> {
                    // Include leaves that overlap with the calculation month
                    LocalDate leaveStart = l.getStartDate();
                    LocalDate leaveEnd = l.getEndDate();
                    return leaveStart != null && leaveEnd != null && 
                           !leaveEnd.isBefore(start) && !leaveStart.isAfter(end);
                })
                .toList();

        // DEBUG: Log found leaves
        System.out.println("DEBUG - Found approved leaves: " + approvedLeaves.size());
        for (Leave leave : approvedLeaves) {
            System.out.println("DEBUG - Leave: " + leave.getStartDate() + " to " + leave.getEndDate() + " (" + leave.getLeaveType() + ")");
        }

        // MODE 2: PLANNED calculation for UI preview

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            
            // STEP 1: SKIP SUNDAY COMPLETELY
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }

            final LocalDate currentDate = date;

            // MODE 2: CHECK ALL LEAVES (including future) - USE DATABASE VALUES ONLY
            for (Leave leave : approvedLeaves) {
                if (!currentDate.isBefore(leave.getStartDate()) &&
                    !currentDate.isAfter(leave.getEndDate())) {

                    double leaveValue = (leave.getIsHalfDay() != null && leave.getIsHalfDay()) ? 0.5 : 1.0;

                    // ✅ FIX 1: Add fallback for paidDays/unpaidDays
                    // If both are NOT null → use them
                    // If NULL → fallback using leaveType
                    Double frozenPaidDays = leave.getPaidDays();
                    Double frozenUnpaidDays = leave.getUnpaidDays();
                    
                    if (frozenPaidDays != null && frozenUnpaidDays != null && frozenPaidDays + frozenUnpaidDays > 0) {
                        // Use frozen database values - calculate proportion for this date
                        double totalFrozenDays = frozenPaidDays + frozenUnpaidDays;
                        double paidProportion = frozenPaidDays / totalFrozenDays;
                        double unpaidProportion = frozenUnpaidDays / totalFrozenDays;
                        
                        result.paidLeave += leaveValue * paidProportion;
                        result.unpaidLeave += leaveValue * unpaidProportion;
                        result.present += leaveValue * paidProportion;  // Paid leave counts as present for UI
                        result.absent += leaveValue * unpaidProportion;  // Unpaid leave counts as absent for UI
                    } else {
                        // ✅ FIX 2: Do NOT skip leave if DB values are null - use fallback
                        if (leave.getLeaveType() == Leave.LeaveType.UNPAID) {
                            result.unpaidLeave += leaveValue;
                            result.absent += leaveValue;  // Unpaid leave counts as absent for UI
                            System.out.println("FALLBACK - Unpaid leave (DB values null) for planned: " + leaveValue);
                        } else {
                            result.paidLeave += leaveValue;
                            result.present += leaveValue;  // Paid leave counts as present for UI
                            System.out.println("FALLBACK - Paid leave (DB values null) for planned: " + leaveValue);
                        }
                    }
                    break; // Only count one leave per day
                }
            }
        }

        // DEBUG: Log result
        System.out.println("DEBUG - Planned result: " + result);
        return result;
    }

    /**
     * This is the ONLY place where leave balance calculations should happen
     * 
     * @param employeeId Employee ID
     * @param year Year (e.g., 2026)
     * @param month Month (1-12)
     * @return LeaveBalanceSummary containing used and unpaid leaves only (attendance calculation)
     */
    public LeaveBalanceSummary calculateLeaveBalance(Long employeeId, int year, int month) {
        int cycle = (month <= 6) ? 1 : 2;
        LocalDate cycleStart = (cycle == 1) 
                ? LocalDate.of(year, 1, 1) 
                : LocalDate.of(year, 7, 1);
        LocalDate targetMonthEnd = YearMonth.of(year, month).atEndOfMonth();
        LocalDate currentMonthStart = YearMonth.of(year, month).atDay(1);
        
        // ✅ Calculate used leaves DIRECTLY from database (includes future approved leaves)
        // Approved leaves must be counted as "used" immediately, regardless of date
        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
                .toList();
        
        double cycleUsedLeaves = 0.0;
        double cycleUnpaidLeaves = 0.0;
        double currentMonthUsedLeaves = 0.0;
        double currentMonthUnpaidLeaves = 0.0;
        
        for (Leave leave : approvedLeaves) {
            if (leave.getStartDate() == null || leave.getEndDate() == null) continue;
            
            // Check if leave overlaps with cycle range
            boolean overlapsCycle = !leave.getStartDate().isAfter(targetMonthEnd) && 
                                    !leave.getEndDate().isBefore(cycleStart);
            // Check if leave overlaps with current month
            boolean overlapsCurrentMonth = !leave.getStartDate().isAfter(targetMonthEnd) && 
                                           !leave.getEndDate().isBefore(currentMonthStart);
            
            // ✅ FIXED: Remove double counting - separate cycle and current month logic
            if (overlapsCycle) {
                // Calculate working days in cycle overlap range (exclude Sundays only)
                LocalDate overlapStart = leave.getStartDate().isBefore(cycleStart) ? cycleStart : leave.getStartDate();
                LocalDate overlapEnd = leave.getEndDate().isAfter(targetMonthEnd) ? targetMonthEnd : leave.getEndDate();
                double days = countWorkingDays(overlapStart, overlapEnd, Boolean.TRUE.equals(leave.getIsHalfDay()));
                
                // ✅ FIX 1: Add fallback for paidDays/unpaidDays
                // If both are NOT null → use them
                // If NULL → fallback using leaveType
                Double frozenPaidDays = leave.getPaidDays();
                Double frozenUnpaidDays = leave.getUnpaidDays();
                
                if (frozenPaidDays != null && frozenUnpaidDays != null && frozenPaidDays + frozenUnpaidDays > 0) {
                    // ✅ ADVANCED FIX: Use sequential deduction (HR-style) instead of proportional distribution
                    // Apply paid days first, then unpaid days sequentially
                    double remainingPaid = frozenPaidDays;
                    double remainingUnpaid = frozenUnpaidDays;
                    double workingDaysToProcess = days;
                    
                    // Process each working day sequentially
                    for (int i = 0; i < workingDaysToProcess; i++) {
                        if (remainingPaid > 0) {
                            cycleUsedLeaves += 1.0;
                            remainingPaid--;
                        } else if (remainingUnpaid > 0) {
                            cycleUnpaidLeaves += 1.0;
                            remainingUnpaid--;
                        }
                    }
                } else {
                    // ✅ FIX 2: Do NOT skip leave if DB values are null - use fallback
                    if (leave.getLeaveType() == Leave.LeaveType.UNPAID) {
                        cycleUnpaidLeaves += days;
                    } else {
                        cycleUsedLeaves += days;
                    }
                }
            }
            
            if (overlapsCurrentMonth) {
                // Calculate working days in current month overlap range (exclude Sundays only)
                LocalDate overlapStart = leave.getStartDate().isBefore(currentMonthStart) ? currentMonthStart : leave.getStartDate();
                LocalDate overlapEnd = leave.getEndDate().isAfter(targetMonthEnd) ? targetMonthEnd : leave.getEndDate();
                double days = countWorkingDays(overlapStart, overlapEnd, Boolean.TRUE.equals(leave.getIsHalfDay()));
                
                // ✅ FIX 1: Add fallback for paidDays/unpaidDays
                // If both are NOT null → use them
                // If NULL → fallback using leaveType
                Double frozenPaidDays = leave.getPaidDays();
                Double frozenUnpaidDays = leave.getUnpaidDays();
                
                if (frozenPaidDays != null && frozenUnpaidDays != null && frozenPaidDays + frozenUnpaidDays > 0) {
                    // ✅ ADVANCED FIX: Use sequential deduction (HR-style) instead of proportional distribution
                    // Apply paid days first, then unpaid days sequentially
                    double remainingPaid = frozenPaidDays;
                    double remainingUnpaid = frozenUnpaidDays;
                    double workingDaysToProcess = days;
                    
                    // Process each working day sequentially
                    for (int i = 0; i < workingDaysToProcess; i++) {
                        if (remainingPaid > 0) {
                            currentMonthUsedLeaves += 1.0;
                            remainingPaid--;
                        } else if (remainingUnpaid > 0) {
                            currentMonthUnpaidLeaves += 1.0;
                            remainingUnpaid--;
                        }
                    }
                } else {
                    // ✅ FIX 2: Do NOT skip leave if DB values are null - use fallback
                    if (leave.getLeaveType() == Leave.LeaveType.UNPAID) {
                        currentMonthUnpaidLeaves += days;
                    } else {
                        currentMonthUsedLeaves += days;
                    }
                }
            }
        }
        
        // ✅ Calculate earned leaves with probation check
        // DEPRECATED: This logic has been moved to LeaveBalanceService
        double earnedLeaves = 0.0;
        
        // ✅ CRITICAL FIX: usedLeaves = paid + unpaid (excluding Sundays)
        double totalUsedLeaves = cycleUsedLeaves + cycleUnpaidLeaves;
        double totalCurrentMonthUsedLeaves = currentMonthUsedLeaves + currentMonthUnpaidLeaves;
        
        return new LeaveBalanceSummary(
                earnedLeaves,
                totalUsedLeaves,        // ✅ FIXED: Now includes both paid + unpaid
                cycleUnpaidLeaves,
                totalCurrentMonthUsedLeaves, // ✅ FIXED: Now includes both paid + unpaid
                currentMonthUnpaidLeaves,
                cycle
        );
    }
    
    /**
     * Count working days between two dates (exclude Sundays only, Saturday is working day)
     */
    private double countWorkingDays(LocalDate start, LocalDate end, boolean isHalfDay) {
        if (isHalfDay) return 0.5;
        double count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            count += 1.0;
        }
        return count;
    }
    
    /**
     * Calculate earned leaves considering probation period
     * ⚠️ DEPRECATED: Use LeaveBalanceService.calculateEarnedLeaves() instead
     * Your business rule (15th day logic) is implemented there
     */
    private double calculateEarnedLeaves(Long employeeId, int year, int month, int cycle) {
        throw new IllegalStateException("Use LeaveBalanceService.calculateEarnedLeaves() for leave accrual - AttendanceEngine does not implement the 15th-day probation rule");
    }
    
    /**
     * DTO for leave balance calculation result
     */
    public static class LeaveBalanceSummary {
        public final double earnedLeaves;
        public final double usedLeaves;
        public final double unpaidLeaves;
        public final double currentMonthUsed;
        public final double currentMonthUnpaid;
        public final int cycle;

        public LeaveBalanceSummary(double earnedLeaves, double usedLeaves, double unpaidLeaves,
                                   double currentMonthUsed, double currentMonthUnpaid, int cycle) {
            this.earnedLeaves = earnedLeaves;
            this.usedLeaves = usedLeaves;
            this.unpaidLeaves = unpaidLeaves;
            this.currentMonthUsed = currentMonthUsed;
            this.currentMonthUnpaid = currentMonthUnpaid;
            this.cycle = cycle;
        }

        public double getAvailableLeaves() {
            return Math.max(0, earnedLeaves - usedLeaves);
        }
    }
    
    /**
     * 🔥 SALARY CALCULATION: Calculate salary components based on attendance and employee data
     * This is the single source of truth for salary calculations
     *
     * @param employee Employee with salary components
     * @param attendanceSummary Attendance summary for the month
     * @param isInProbation Whether employee is in probation
     * @return SalarySummary with all salary components
     */
    public SalarySummary calculateSalary(Employee employee, AttendanceSummary attendanceSummary, boolean isInProbation) {
        BigDecimal basicSalary = employee.getBasicSalary() != null ? employee.getBasicSalary() : BigDecimal.ZERO;
        BigDecimal hra = employee.getHra() != null ? employee.getHra() : BigDecimal.ZERO;
        BigDecimal da = employee.getDa() != null ? employee.getDa() : BigDecimal.ZERO;
        BigDecimal otherAllowance = employee.getOtherAllowance() != null ? employee.getOtherAllowance() : BigDecimal.ZERO;

        // Calculate gross salary
        BigDecimal grossSalary = basicSalary.add(da).add(hra).add(otherAllowance);

        // Calculate per day salary (based on 26 working days)
        double perDaySalary = basicSalary.doubleValue() / 26.0;

        // Calculate deductions
        BigDecimal pf = employee.getPf() != null ? employee.getPf() : BigDecimal.ZERO;
        BigDecimal esi = BigDecimal.ZERO; // ESI not stored in Employee model
        BigDecimal incomeTax = employee.getTax() != null ? employee.getTax() : BigDecimal.ZERO;
        BigDecimal insurance = basicSalary.multiply(BigDecimal.valueOf(employee.getInsurancePercentage() != null ? employee.getInsurancePercentage() : 0))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Calculate leave deduction
        double absentDaysCount = attendanceSummary.absent;
        double unpaidLeaveDaysCount = attendanceSummary.unpaidLeave;
        BigDecimal absentLeaveDeduction = BigDecimal.valueOf(perDaySalary).multiply(BigDecimal.valueOf(absentDaysCount))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal unpaidLeaveDeduction = BigDecimal.valueOf(perDaySalary).multiply(BigDecimal.valueOf(unpaidLeaveDaysCount))
                .setScale(2, RoundingMode.HALF_UP);

        // ✅ TOTAL ATTENDANCE DEDUCTION (separate from other deductions)
        BigDecimal totalAttendanceDeduction = absentLeaveDeduction.add(unpaidLeaveDeduction);

        // Total deduction (attendance + other deductions)
        BigDecimal totalDeduction = pf.add(esi).add(incomeTax).add(insurance).add(totalAttendanceDeduction);

        // Net salary
        BigDecimal netSalary = grossSalary.subtract(totalDeduction);

        // Prevent negative salary
        if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
            netSalary = BigDecimal.ZERO;
        }

        // ✅ Use new SalarySummary constructor with only 4 parameters
        return new SalarySummary(
                absentLeaveDeduction,
                unpaidLeaveDeduction,
                absentDaysCount,
                unpaidLeaveDaysCount
        );
    }

    /**
     * DTO for salary calculation result
     */
    public static class SalarySummary {
        private BigDecimal absentLeaveDeduction;
        private BigDecimal unpaidLeaveDeduction;

        private double absentDays;
        private double unpaidLeaveDays;

        // constructor
        public SalarySummary(
                BigDecimal absentLeaveDeduction,
                BigDecimal unpaidLeaveDeduction,
                double absentDays,
                double unpaidLeaveDays
        ) {
            this.absentLeaveDeduction = absentLeaveDeduction;
            this.unpaidLeaveDeduction = unpaidLeaveDeduction;
            this.absentDays = absentDays;
            this.unpaidLeaveDays = unpaidLeaveDays;
        }

        // getters
        public BigDecimal getAbsentLeaveDeduction() {
            return absentLeaveDeduction;
        }

        public BigDecimal getUnpaidLeaveDeduction() {
            return unpaidLeaveDeduction;
        }

        public double getAbsentDays() {
            return absentDays;
        }

        public double getUnpaidLeaveDays() {
            return unpaidLeaveDays;
        }
    }
    
    /**
     * ✅ STEP 8: Centralize paid/unpaid calculation in AttendanceEngine
     * Calculate paid and unpaid days for a leave request
     */
    public static class PaidUnpaidResult {
        public final double paidDays;
        public final double unpaidDays;
        public final double totalDays;
        
        public PaidUnpaidResult(double paidDays, double unpaidDays, double totalDays) {
            this.paidDays = paidDays;
            this.unpaidDays = unpaidDays;
            this.totalDays = totalDays;
        }
    }
    

    
    /**
     * ✅ FIX 4: Calculate opening balance for a given month/year
     * This method centralizes opening balance calculation logic
     * @param employeeId Employee ID
     * @param year Year (e.g., 2026)
     * @param month Month (1-12)
     * @return Opening balance (remaining leaves from previous month)
     */
    public double calculateOpeningBalance(Long employeeId, int year, int month) {
        // Calculate previous month with proper year transition
        int prevMonth = (month == 1) ? 12 : month - 1;
        int prevYear = (month == 1) ? year - 1 : year;

        // Get leave balance summary for previous month
        LeaveBalanceSummary summary = calculateLeaveBalance(employeeId, prevYear, prevMonth);
        return summary.getAvailableLeaves();
    }
}
