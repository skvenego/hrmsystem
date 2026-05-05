package com.hrm.hrmsystem.engine;

import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.repository.AttendanceRepository;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import lombok.RequiredArgsConstructor;
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
        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
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
                System.out.println("DEBUG - Skipping Sunday: " + date);
                continue;
            }

            boolean handled = false;
            final LocalDate currentDate = date;

            // 🔥 MODE 1: ACTUAL (FOR SALARY) - ONLY COUNT dates <= TODAY
            if (date.isAfter(today)) {
                System.out.println("DEBUG - Skipping future date: " + date);
                continue;
            }

            System.out.println("DEBUG - Processing date: " + date);

            // STEP 3: CHECK LEAVE (Priority 1) - Leave > Attendance > Unmarked
            // MODE 1: Only count leaves for dates <= TODAY
            Leave leave = approvedLeaves.stream()
                    .filter(l -> !currentDate.isBefore(l.getStartDate()) && !currentDate.isAfter(l.getEndDate()))
                    .findFirst()
                    .orElse(null);

            if (leave != null) {
                boolean isHalfDay = Boolean.TRUE.equals(leave.getIsHalfDay());
                double leaveValue = isHalfDay ? 0.5 : 1.0;

                System.out.println("DEBUG - Found leave for date " + currentDate + ": " + leave.getLeaveType() + " (" + leave.getStartDate() + " to " + leave.getEndDate() + ")");

                if (leave.getLeaveType() == Leave.LeaveType.UNPAID) {
                    // UNPAID LEAVE
                    result.unpaidLeave += leaveValue;
                    result.absent += leaveValue;
                    System.out.println("DEBUG - Added UNPAID leave: " + leaveValue);
                } else {
                    // PAID LEAVE
                    result.paidLeave += leaveValue;
                    result.present += leaveValue;  // Paid leave counts as present
                    System.out.println("DEBUG - Added PAID leave: " + leaveValue);
                }
                handled = true;
            } else {
                System.out.println("DEBUG - No leave found for date: " + currentDate);
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
                        result.unpaidLeave += 1;
                        break;
                    case PENDING:
                        // STEP 9: DO NOT COUNT PENDING as absent
                        break;
                    case ON_LEAVE:
                        // Already handled in leave section
                        break;
                    default:
                        result.absent += 1;
                        result.unpaidLeave += 1;
                        break;
                }
                handled = true;
            }

            // STEP 5: UNMARKED (no attendance record) → ABSENT for payroll
            if (!handled) {
                result.absent += 1;
                result.unpaidLeave += 1;
            }
            
            }
        }

        return result;
    }

    /**
     * � MODE 2: PLANNED (FOR UI PREVIEW)
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

        // Fetch all approved leaves for the month (including future)
        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
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

            // MODE 2: CHECK ALL LEAVES (including future)
            for (Leave leave : approvedLeaves) {
                if (!currentDate.isBefore(leave.getStartDate()) &&
                    !currentDate.isAfter(leave.getEndDate())) {

                    double leaveValue = (leave.getIsHalfDay() != null && leave.getIsHalfDay()) ? 0.5 : 1.0;

                    if (leave.getLeaveType() == Leave.LeaveType.UNPAID) {
                        result.unpaidLeave += leaveValue;
                        result.absent += leaveValue;
                    } else {
                        // PAID LEAVE (SICK, CASUAL, etc.)
                        result.paidLeave += leaveValue;
                        result.present += leaveValue;  // Paid leave counts as present for UI
                    }
                    break; // Only count one leave per day
                }
            }
        }

        // MODE 2: PLANNED calculation complete

        return result;
    }

    /**
     * �� SINGLE SOURCE OF TRUTH: Complete cycle leave balance calculation
     * Calculates earned leaves, used leaves, and unpaid leaves for a given month
     * This is the ONLY place where leave balance calculations should happen
     * 
     * @param employeeId Employee ID
     * @param year Year (e.g., 2026)
     * @param month Month (1-12)
     * @return LeaveBalanceSummary containing earned, used, and unpaid leaves
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
            
            if (overlapsCycle || overlapsCurrentMonth) {
                // Calculate working days in overlap range (exclude Sundays only)
                boolean isHalfDay = Boolean.TRUE.equals(leave.getIsHalfDay());
                
                if (overlapsCycle) {
                    LocalDate overlapStart = leave.getStartDate().isBefore(cycleStart) ? cycleStart : leave.getStartDate();
                    LocalDate overlapEnd = leave.getEndDate().isAfter(targetMonthEnd) ? targetMonthEnd : leave.getEndDate();
                    double days = countWorkingDays(overlapStart, overlapEnd, isHalfDay);
                    
                    if (leave.getLeaveType() == Leave.LeaveType.UNPAID) {
                        cycleUnpaidLeaves += days;
                    } else {
                        cycleUsedLeaves += days;
                    }
                }
                
                if (overlapsCurrentMonth) {
                    LocalDate overlapStart = leave.getStartDate().isBefore(currentMonthStart) ? currentMonthStart : leave.getStartDate();
                    LocalDate overlapEnd = leave.getEndDate().isAfter(targetMonthEnd) ? targetMonthEnd : leave.getEndDate();
                    double days = countWorkingDays(overlapStart, overlapEnd, isHalfDay);
                    
                    if (leave.getLeaveType() == Leave.LeaveType.UNPAID) {
                        currentMonthUnpaidLeaves += days;
                    } else {
                        currentMonthUsedLeaves += days;
                    }
                }
            }
        }
        
        // ✅ Calculate earned leaves with probation check
        double earnedLeaves = calculateEarnedLeaves(employeeId, year, month, cycle);
        
        return new LeaveBalanceSummary(
                earnedLeaves,
                cycleUsedLeaves,
                cycleUnpaidLeaves,
                currentMonthUsedLeaves,
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
     */
    private double calculateEarnedLeaves(Long employeeId, int year, int month, int cycle) {
        Employee emp = employeeRepository.findById(employeeId).orElse(null);
        if (emp == null || emp.getJoiningDate() == null) {
            return 0.0;
        }
        
        LocalDate joiningDate = emp.getJoiningDate();
        int probationMonths = emp.getProbationPeriodMonths() != null ? emp.getProbationPeriodMonths() : 3;
        LocalDate probationEnd = joiningDate.plusMonths(probationMonths);
        LocalDate targetMonthEnd = YearMonth.of(year, month).atEndOfMonth();
        
        // Check if probation completed
        if (!probationEnd.isBefore(targetMonthEnd)) {
            return 0.0; // Still in probation
        }
        
        // Calculate months in current cycle since probation ended
        LocalDate cycleStart = (cycle == 1) ? LocalDate.of(year, 1, 1) : LocalDate.of(year, 7, 1);
        LocalDate effectiveStart = cycleStart.isAfter(probationEnd) ? cycleStart : probationEnd.plusDays(1);
        
        int monthsInCycle = 0;
        YearMonth startYM = YearMonth.from(effectiveStart);
        YearMonth targetYM = YearMonth.of(year, month);
        
        // Calculate months in cycle for earned leaves
        
        while (!startYM.isAfter(targetYM)) {
            monthsInCycle++;
            startYM = startYM.plusMonths(1);
        }
        
        // Cap at 9 per cycle
        return Math.min(monthsInCycle * 1.5, 9.0);
    }
    
    /**
     * 🔥 OPTIMIZED: Calculate attendance across a date range in ONE pass
     * No monthly loops - single database query for entire range
     */
    public AttendanceSummary calculateRange(Long employeeId, LocalDate start, LocalDate end) {
        AttendanceSummary result = new AttendanceSummary();
        
        // Fetch all data in ONE query for entire range
        List<Attendance> attendances = 
                attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, start, end);
        
        // Fetch all approved leaves
        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
                .toList();
        
        LocalDate today = LocalDate.now();
        
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            // Skip Sundays only (Saturday is working day)
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            
            final LocalDate currentDate = date;
            boolean handled = false;
            boolean isFuture = date.isAfter(today);
            
            // STEP 1: Check for approved leave (includes future approved leaves)
            for (Leave leave : approvedLeaves) {
                if (leave.getStartDate() == null || leave.getEndDate() == null) continue;
                
                if (!currentDate.isBefore(leave.getStartDate()) && 
                    !currentDate.isAfter(leave.getEndDate())) {
                    
                    boolean isHalfDay = Boolean.TRUE.equals(leave.getIsHalfDay());
                    double leaveValue = isHalfDay ? 0.5 : 1.0;

                    // Check UNPAID type (everything else is paid)
                    if (leave.getLeaveType() == Leave.LeaveType.UNPAID) {
                        result.unpaidLeave += leaveValue;
                        result.absent += leaveValue;
                    } else {
                        result.paidLeave += leaveValue;
                        // Don't add to present - present = actual attendance only
                    }
                    
                    handled = true;
                    break;
                }
            }
            
            // For future dates without approved leave, skip
            if (isFuture && !handled) continue;
            
            // STEP 2: Check attendance record
            if (!handled) {
                for (Attendance att : attendances) {
                    if (att.getDate() != null && att.getDate().equals(currentDate)) {
                        if (att.getStatus() != null) {
                            switch (att.getStatus()) {
                                case PRESENT:
                                case LATE:
                                    result.present += 1;
                                    System.out.println("DEBUG - Counted PRESENT/LATE. Running total: present=" + result.present);
                                    break;
                                case HALF_DAY:
                                    result.present += 0.5;
                                    result.absent += 0.5;
                                    break;
                                case ABSENT:
                                    result.absent += 1;
                                    result.unpaidLeave += 1;
                                    break;
                                case PENDING:
                                    // 🔥 DO NOT COUNT (important)
                                    System.out.println("DEBUG - PENDING status - not counting as absent");
                                    break;
                                case ON_LEAVE:
                                    // already handled in leave section
                                    break;
                                default:
                                    result.absent += 1;
                                    result.unpaidLeave += 1;
                                    break;
                            }
                        }
                        handled = true;
                        break;
                    }
                }
            }
            
            // STEP 3: UNMARKED → ABSENT for payroll (only if date is in past)
            if (!handled) {
                if (!isFuture) {
                    result.absent += 1;
                    // Don't blindly add unpaidLeave
                }
            }
        }
        
        return result;
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
        BigDecimal absentLeaveDeduction = BigDecimal.valueOf(perDaySalary).multiply(BigDecimal.valueOf(absentDaysCount))
                .setScale(2, RoundingMode.HALF_UP);

        // Total deduction
        BigDecimal totalDeduction = pf.add(esi).add(incomeTax).add(insurance).add(absentLeaveDeduction);

        // Net salary
        BigDecimal netSalary = grossSalary.subtract(totalDeduction);

        // Prevent negative salary
        if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
            netSalary = BigDecimal.ZERO;
        }

        return new SalarySummary(
                basicSalary,
                hra,
                da,
                otherAllowance,
                grossSalary,
                pf,
                esi,
                incomeTax,
                insurance,
                absentLeaveDeduction,
                totalDeduction,
                netSalary,
                perDaySalary,
                absentDaysCount
        );
    }

    /**
     * DTO for salary calculation result
     */
    public static class SalarySummary {
        public final BigDecimal basicSalary;
        public final BigDecimal hra;
        public final BigDecimal da;
        public final BigDecimal otherAllowance;
        public final BigDecimal grossSalary;
        public final BigDecimal pf;
        public final BigDecimal esi;
        public final BigDecimal incomeTax;
        public final BigDecimal insurance;
        public final BigDecimal absentLeaveDeduction;
        public final BigDecimal totalDeduction;
        public final BigDecimal netSalary;
        public final double perDaySalary;
        public final double absentDaysCount;

        public SalarySummary(BigDecimal basicSalary, BigDecimal hra, BigDecimal da, BigDecimal otherAllowance,
                            BigDecimal grossSalary, BigDecimal pf, BigDecimal esi, BigDecimal incomeTax,
                            BigDecimal insurance, BigDecimal absentLeaveDeduction, BigDecimal totalDeduction,
                            BigDecimal netSalary, double perDaySalary, double absentDaysCount) {
            this.basicSalary = basicSalary;
            this.hra = hra;
            this.da = da;
            this.otherAllowance = otherAllowance;
            this.grossSalary = grossSalary;
            this.pf = pf;
            this.esi = esi;
            this.incomeTax = incomeTax;
            this.insurance = insurance;
            this.absentLeaveDeduction = absentLeaveDeduction;
            this.totalDeduction = totalDeduction;
            this.netSalary = netSalary;
            this.perDaySalary = perDaySalary;
            this.absentDaysCount = absentDaysCount;
        }
    }
}
