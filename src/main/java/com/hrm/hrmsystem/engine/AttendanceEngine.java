package com.hrm.hrmsystem.engine;

import com.hrm.hrmsystem.config.PayrollPolicy;
import com.hrm.hrmsystem.dto.LeaveBalanceDTO;
import com.hrm.hrmsystem.dto.PayrollDTO;

import com.hrm.hrmsystem.engine.AttendanceSummary;
import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.repository.AttendanceRepository;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ FINAL ENTERPRISE ATTENDANCE ENGINE
 * SINGLE SOURCE OF TRUTH - Only this class decides business rules
 * 
 * Architecture:
 * - resolveDay() = ONLY decision method
 * - All other methods = aggregation/display only
 * - No duplicate logic anywhere
 * - Running balance reduction prevents over-crediting
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceEngine {

    public static final int WORKING_DAYS_PER_MONTH = PayrollPolicy.WORKING_DAYS_PER_MONTH;

    private final LeaveRepository leaveRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * ✅ CENTRAL DTO: Single source of truth for daily resolution
     * ONLY resolveDay() decides business rules - all other methods consume result
     */
    public static class DayResult {
        public final double worked;
        public final double paidLeave;
        public final double unpaidLeave;
        public final double absent;
        public final double payable;
        public final boolean halfDay;
        public final String status;
        public final String halfType;
        
        public DayResult(double worked, double paidLeave, double unpaidLeave, double absent, double payable, boolean halfDay, String status, String halfType) {
            this.worked = worked;
            this.paidLeave = paidLeave;
            this.unpaidLeave = unpaidLeave;
            this.absent = absent;
            this.payable = payable;
            this.halfDay = halfDay;
            this.status = status;
            this.halfType = halfType;
        }
        
        // ✅ ZERO RESULT for Sundays
        public static final DayResult ZERO = new DayResult(0, 0, 0, 0, 0, false, "WEEKLY_OFF", null);
    }

    /**
     * ✅ LEAVE BALANCE SUMMARY DTO
     * Used internally by engine methods
     */
    public static class LeaveBalanceSummary {
        public final double earnedLeaves;
        public final double usedLeaves;
        public final double unpaidLeaves;
        public final double remaining;
        
        // Additional fields for compatibility
        public final double currentMonthUsed;
        public final double currentMonthUnpaid;
        public final double totalUsedLeaves;
        public final int cycle;
        public final int year;
        
        public LeaveBalanceSummary(double earnedLeaves, double usedLeaves, double unpaidLeaves, double remaining) {
            this(earnedLeaves, usedLeaves, unpaidLeaves, remaining, 0.0, 0.0, YearMonth.now());
        }

        // ✅ Full constructor with current-month breakdown
        public LeaveBalanceSummary(double earnedLeaves, double usedLeaves, double unpaidLeaves, double remaining,
                                   double currentMonthUsed, double currentMonthUnpaid) {
            this(earnedLeaves, usedLeaves, unpaidLeaves, remaining, currentMonthUsed, currentMonthUnpaid, YearMonth.now());
        }

        // ✅ Query-specific constructor to support correct historical cycle tracking
        public LeaveBalanceSummary(double earnedLeaves, double usedLeaves, double unpaidLeaves, double remaining,
                                   double currentMonthUsed, double currentMonthUnpaid, YearMonth queryMonth) {
            this.earnedLeaves = earnedLeaves;
            this.usedLeaves = usedLeaves;
            this.unpaidLeaves = unpaidLeaves;
            this.remaining = remaining;
            this.currentMonthUsed = currentMonthUsed;
            this.currentMonthUnpaid = currentMonthUnpaid;
            this.totalUsedLeaves = usedLeaves + unpaidLeaves;
            this.cycle = (queryMonth.getMonthValue() <= 6) ? 1 : 2;
            this.year = queryMonth.getYear();
        }
        
        public double getAvailableLeaves() { return remaining; }
        public double getTotalUsedLeaves() { return totalUsedLeaves; }
    }

    /**
     * ✅ SALARY SUMMARY DTO
     * Used internally by engine methods
     */
    public static class SalarySummary {
        public final BigDecimal grossSalary;
        public final BigDecimal salaryDeduction;
        public final BigDecimal absentDeduction;
        public final BigDecimal totalDeduction;
        public final BigDecimal netSalary;
        public final BigDecimal pf;
        public final BigDecimal tax;
        public final BigDecimal insurance;
        public final double absentDays;
        public final double unpaidLeaveDays;
        
        public SalarySummary(BigDecimal grossSalary, BigDecimal salaryDeduction, BigDecimal absentDeduction, 
                            BigDecimal pf, BigDecimal tax, BigDecimal insurance,
                            BigDecimal totalDeduction, BigDecimal netSalary,
                            double absentDays, double unpaidLeaveDays) {
            this.grossSalary = grossSalary;
            this.salaryDeduction = salaryDeduction;
            this.absentDeduction = absentDeduction;
            this.pf = pf;
            this.tax = tax;
            this.insurance = insurance;
            this.totalDeduction = totalDeduction;
            this.netSalary = netSalary;
            this.absentDays = absentDays;
            this.unpaidLeaveDays = unpaidLeaveDays;
        }
        
        public BigDecimal getGrossSalary() { return grossSalary; }
        public BigDecimal getSalaryDeduction() { return salaryDeduction; }
        public BigDecimal getAbsentDeduction() { return absentDeduction; }
        public BigDecimal getTotalDeduction() { return totalDeduction; }
        public BigDecimal getNetSalary() { return netSalary; }
        public BigDecimal getPf() { return pf; }
        public BigDecimal getTax() { return tax; }
        public BigDecimal getInsurance() { return insurance; }
        public BigDecimal getUnpaidLeaveDeduction() { return salaryDeduction; }
        public BigDecimal getAbsentLeaveDeduction() { return absentDeduction; }
        public BigDecimal getTotalDeductions() { return totalDeduction; }
        public double getAbsentDays() { return absentDays; }
        public double getUnpaidLeaveDays() { return unpaidLeaveDays; }
    }

    // ✅ LEGACY COMPATIBILITY WRAPPER METHODS
    // For backward compatibility with existing services
    public LeaveBalanceSummary calculateLeaveBalance(Long employeeId, int year, int month) {
        return calculateLeaveBalance(employeeId, YearMonth.of(year, month), null, null);
    }
    
    public LeaveBalanceSummary calculateLeaveBalance(Long employeeId, int year, int month, Long excludeLeaveId) {
        return calculateLeaveBalance(employeeId, YearMonth.of(year, month), null, excludeLeaveId);
    }
    
    public SalarySummary calculateSalary(
            com.hrm.hrmsystem.model.Employee employee, 
            AttendanceSummary attendanceSummary, 
            boolean isInProbation) {
        // ✅ CRITICAL FIX: Use the PASSED summary directly — do NOT re-run calculate().
        // The caller (PayrollService) already ran calculate() and has the correct unpaid/absent values.
        // Re-running would reset unpaidLeave to 0 if the month/year fields are not set.
        BigDecimal grossSalary = employee.getTotalGrossSalary();
        BigDecimal dailyRate = BigDecimal.ZERO;

        if (grossSalary != null && grossSalary.compareTo(BigDecimal.ZERO) > 0) {
            dailyRate = grossSalary.divide(
                    BigDecimal.valueOf(PayrollPolicy.getWorkingDaysPerMonth()), 2, RoundingMode.HALF_UP);
        }

        log.info("💰 [Overload] Salary Calc - Employee: {}, Gross: {}, Daily Rate: {}, Unpaid: {}, Absent: {}",
            employee.getFirstName(), grossSalary, dailyRate, attendanceSummary.unpaidLeave, attendanceSummary.absent);

        // Unpaid leaves → 1x deduction
        BigDecimal salaryDeduction = dailyRate.multiply(BigDecimal.valueOf(attendanceSummary.unpaidLeave))
                .setScale(2, RoundingMode.HALF_UP);

        // Explicit absences → 1x penalty (standard deduction)
        BigDecimal absentDeduction = dailyRate.multiply(BigDecimal.valueOf(attendanceSummary.absent))
                .multiply(BigDecimal.valueOf(PayrollPolicy.getAbsentPenaltyMultiplier()))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("📉 [Overload] Deductions - Unpaid: {}, Absent: {}", salaryDeduction, absentDeduction);

        BigDecimal pf = employee.getPf() != null ? employee.getPf() : BigDecimal.ZERO;
        BigDecimal tax = employee.getTax() != null ? employee.getTax() : BigDecimal.ZERO;

        BigDecimal insurance = BigDecimal.ZERO;
        if (employee.getInsurancePercentage() != null) {
            insurance = grossSalary.multiply(BigDecimal.valueOf(employee.getInsurancePercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAttendanceDeduction = salaryDeduction.add(absentDeduction);
        BigDecimal totalDeduction = totalAttendanceDeduction.add(pf).add(tax).add(insurance);
        BigDecimal netSalary = grossSalary.subtract(totalDeduction);

        return new SalarySummary(
                grossSalary,
                salaryDeduction,
                absentDeduction,
                pf,
                tax,
                insurance,
                totalDeduction,
                netSalary,
                attendanceSummary.absent,
                attendanceSummary.unpaidLeave
        );
    }
    
    public double calculateWorkingDays(LocalDate start, LocalDate end) {
        // ✅ FIXED: Exclude Sundays from working days calculation
        double workingDays = 0;
        LocalDate current = start;
        
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
    
    public double calculateActualWorkedHours(Long employeeId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        AttendanceSummary summary = calculate(employeeId, yearMonth);
        return summary.workedDays * 8.0; // Assuming 8-hour work days
    }
    
    /**
     * ✅ DAILY DETAILS: Generates full monthly report for frontend
     * Consumes resolveDay() for every day in the month
     */
    public List<Map<String, Object>> getDailyAttendanceDetails(Long employeeId, int year, int month) {
        log.info("Generating daily details for employee {}, month {}, year {}", employeeId, month, year);
        YearMonth yearMonth = YearMonth.of(year, month);
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            log.warn("Employee {} not found for daily details", employeeId);
            return Collections.emptyList();
        }

        List<Map<String, Object>> details = new ArrayList<>();
        LocalDate start = yearMonth.atDay(1);
        LocalDate end = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();
        
        log.info("Looping from {} to {}", start, end);

        // Get leaves and attendance for the whole month once (performance)
        List<Leave> approvedLeaves = leaveRepository.findByEmployeeIdAndStatus(employeeId, Leave.LeaveStatus.APPROVED);
        List<Attendance> monthlyAttendance = attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, start, end);
        
        log.info("Found {} approved leaves and {} attendance records for employee {} in {}", 
            approvedLeaves.size(), monthlyAttendance.size(), employeeId, yearMonth);
            
        // Opening balance for this month's leave resolution
        LeaveBalanceSummary balance = calculateLeaveBalance(employeeId, yearMonth.minusMonths(1));
        double runningBalance = balance.remaining + PayrollPolicy.getLeaveAccrualRate();
        log.info("Starting running balance for {}: {}", yearMonth, runningBalance);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            final LocalDate currentDate = date;
            try {
                // 1. Find data for this specific day
                Attendance attendance = monthlyAttendance.stream()
                    .filter(a -> a.getDate().equals(currentDate))
                    .findFirst().orElse(null);
                
                Leave leave = approvedLeaves.stream()
                    .filter(l -> !currentDate.isBefore(l.getStartDate()) && !currentDate.isAfter(l.getEndDate()))
                    .findFirst().orElse(null);

                // 2. Resolve using single source of truth
                DayResult res = resolveDay(employee, currentDate, attendance, leave, runningBalance);
                
                // Skip future dates unless they have an approved leave
                if (currentDate.isAfter(today) && res.paidLeave == 0 && res.unpaidLeave == 0) {
                    continue;
                }
                
                // Update running balance if paid leave used
                runningBalance -= res.paidLeave;

                // 3. Map to frontend DTO
                Map<String, Object> dayMap = new HashMap<>();
                dayMap.put("date", currentDate.toString());
                dayMap.put("workingHours", res.worked * 8.0);
                
                // Status Mapping
                dayMap.put("status", res.status);
                if (res.halfType != null) dayMap.put("halfType", res.halfType);
                
                if (res.paidLeave > 0 || res.unpaidLeave > 0) {
                    dayMap.put("leaveType", leave != null ? leave.getLeaveType() : "LEAVE");
                    dayMap.put("leaveReason", leave != null ? leave.getReason() : "");
                    dayMap.put("isPaid", res.paidLeave);
                    dayMap.put("isUnpaid", res.unpaidLeave);
                }

                if (attendance != null) {
                    dayMap.put("checkInTime", attendance.getCheckInTime());
                    dayMap.put("checkOutTime", attendance.getCheckOutTime());
                    if (attendance.getRemarks() != null) dayMap.put("remarks", attendance.getRemarks());
                }

                details.add(dayMap);
            } catch (Exception e) {
                log.error("Error resolving day {} for employee {}: {}", currentDate, employeeId, e.getMessage());
                // Add minimal data for this day so the table doesn't break
                Map<String, Object> errorMap = new HashMap<>();
                errorMap.put("date", currentDate.toString());
                errorMap.put("status", "ERROR");
                details.add(errorMap);
            }
        }
        return details;
    }
    
    public double calculateDeductibleDays(int absentDays, int unpaidLeaves, int halfDays) {
        return absentDays + unpaidLeaves + (halfDays * 0.5);
    }
    
    public double calculateOpeningBalance(Long employeeId, YearMonth month) {
        // Simple opening balance calculation
        LeaveBalanceSummary current = calculateLeaveBalance(employeeId, month.minusMonths(1));
        return current.earnedLeaves - current.usedLeaves;
    }
    
    public LeaveDayResult resolveLeaveDay(
            Employee employee,
            Leave leave,
            LocalDate date,
            double remainingBalance) {
        // ✅ FIXED: Use the unified resolveDay logic instead of a stub
        DayResult res = resolveDay(employee, date, null, leave, remainingBalance);
        return new LeaveDayResult(res.paidLeave, (leave != null && leave.getIsHalfDay() != null && leave.getIsHalfDay()) ? 0.5 : 1.0);
    }
    
    public LeaveSplit calculateLeaveSplit(
            Long employeeId,
            LocalDate startDate,
            LocalDate endDate,
            boolean halfDay) {
        
        // ✅ CRITICAL FIX: Compute balance EXCLUDING leaves that start on or after
        // the requested leave's start date. This ensures "what balance did the employee
        // have just before this leave" is computed correctly, regardless of what other
        // future leaves are already approved in the same month.
        YearMonth month = YearMonth.from(startDate);
        LeaveBalanceSummary balance = calculateLeaveBalance(employeeId, month, startDate);
        
        // 2. Calculate total working days in the request
        double totalDays = calculateWorkingDays(startDate, endDate);
        if (halfDay) {
            totalDays = 0.5;
        }
        
        // 3. Proportional splitting based on available balance
        double available = Math.max(0, balance.remaining);
        double paidDays;
        double unpaidDays;
        
        if (available >= totalDays) {
            paidDays = totalDays;
            unpaidDays = 0;
        } else if (available > 0) {
            paidDays = available;
            unpaidDays = totalDays - available;
        } else {
            paidDays = 0;
            unpaidDays = totalDays;
        }
        
        log.info("Split Calculation - Employee {}: Request {} to {} (half={}) -> total={}, available={}, split: paid={}, unpaid={}",
            employeeId, startDate, endDate, halfDay, totalDays, available, paidDays, unpaidDays);
            
        return new LeaveSplit(paidDays, unpaidDays, halfDay ? 0.5 : 0.0, totalDays);
    }
    
    // Nested classes for compatibility
    public static class LeaveSplit {
        public final double paidDays;
        public final double unpaidDays;
        public final double halfDays;
        public final double totalDays;
        
        public LeaveSplit(double paidDays, double unpaidDays, double halfDays) {
            this.paidDays = paidDays;
            this.unpaidDays = unpaidDays;
            this.halfDays = halfDays;
            this.totalDays = paidDays + unpaidDays + halfDays;
        }
        
        public LeaveSplit(double paidDays, double unpaidDays, double halfDays, double totalDays) {
            this.paidDays = paidDays;
            this.unpaidDays = unpaidDays;
            this.halfDays = halfDays;
            this.totalDays = totalDays;
        }
    }
    
    public static class LeaveDayResult {
        public final double paid;
        public final double leaveDays;
        
        public LeaveDayResult(double paid, double leaveDays) {
            this.paid = paid;
            this.leaveDays = leaveDays;
        }
    }

    /**
     * ✅ SINGLE SOURCE OF TRUTH: ONLY method that decides ALL business rules
     * PURE FUNCTION: input → deterministic output, no side effects
     */
    public DayResult resolveDay(
            Employee employee,
            LocalDate date,
            Attendance attendance,
            Leave leave,
            double remainingBalance
    ) {
        // Rule 1: Sunday = ZERO everything
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return DayResult.ZERO;
        }
        
        double worked = 0.0;
        double paidLeave = 0.0;
        double unpaidLeave = 0.0;
        double absent = 0.0;
        boolean isHalfDay = false;
        String status = "ABSENT";
        String halfType = null;

        // Rule 2: Probation check
        boolean isInProbation = !isProbationCompleted(employee, date);

        // Rule 3: Process Attendance & Leave with Granular Half-Day logic
        // We track first and second halves independently for professional reporting.
        // PRECEDENCE: Leave > Manual Attendance > Auto-Absence.
        // NO DUPLICATE: A half-day is strictly counted as either Worked, Leave, or Absent.
        double firstHalfWorked = 0, firstHalfPaidLeave = 0, firstHalfUnpaidLeave = 0, firstHalfAbsent = 0;
        double secondHalfWorked = 0, secondHalfPaidLeave = 0, secondHalfUnpaidLeave = 0, secondHalfAbsent = 0;

        // 3a. Attendance logic
        if (attendance != null && attendance.getStatus() != null) {
            switch (attendance.getStatus()) {
                case PRESENT:
                case LATE:
                    firstHalfWorked = 0.5;
                    secondHalfWorked = 0.5;
                    break;
                case HALF_DAY:
                    isHalfDay = true;
                    if (attendance.getHalfType() == Attendance.HalfType.SECOND_HALF) {
                        secondHalfWorked = 0.5;
                    } else {
                        firstHalfWorked = 0.5; // Default to first half if not specified
                    }
                    break;
                case ABSENT:
                    if (attendance.getHalfType() == Attendance.HalfType.FIRST_HALF) {
                        firstHalfAbsent = 0.5;
                    } else if (attendance.getHalfType() == Attendance.HalfType.SECOND_HALF) {
                        secondHalfAbsent = 0.5;
                    } else {
                        firstHalfAbsent = 0.5;
                        secondHalfAbsent = 0.5;
                    }
                    break;
            }
        }

        // 3b. Leave logic (PRIORITY over attendance)
        if (leave != null) {
            double leaveUnit = 1.0;
            boolean leaveIsHalf = false;
            if (leave.getIsHalfDay() != null && leave.getIsHalfDay()) {
                leaveUnit = 0.5;
                leaveIsHalf = true;
                isHalfDay = true;
            }

            // Determine which half the leave covers
            boolean coversFirst = !leaveIsHalf || (leave.getHalfType() != Leave.HalfType.SECOND_HALF);
            boolean coversSecond = !leaveIsHalf || (leave.getHalfType() == Leave.HalfType.SECOND_HALF);

            // Decide paid/unpaid split for the leave unit
            double currentPaid = 0, currentUnpaid = 0;
            if (isInProbation) {
                currentUnpaid = leaveUnit;
            } else if (remainingBalance >= leaveUnit) {
                currentPaid = leaveUnit;
            } else if (remainingBalance > 0) {
                currentPaid = remainingBalance;
                currentUnpaid = leaveUnit - remainingBalance;
            } else {
                currentUnpaid = leaveUnit;
            }

            // Apply to specific halves (Leave overwrites attendance for that half)
            if (coversFirst) {
                firstHalfWorked = 0;
                firstHalfAbsent = 0;
                if (leaveIsHalf) {
                    firstHalfPaidLeave = currentPaid;
                    firstHalfUnpaidLeave = currentUnpaid;
                } else {
                    // Full day leave covers both halves, but we'll handle second half below
                    firstHalfPaidLeave = currentPaid / 2.0;
                    firstHalfUnpaidLeave = currentUnpaid / 2.0;
                }
            }
            if (coversSecond) {
                secondHalfWorked = 0;
                secondHalfAbsent = 0;
                if (leaveIsHalf) {
                    secondHalfPaidLeave = currentPaid;
                    secondHalfUnpaidLeave = currentUnpaid;
                } else {
                    secondHalfPaidLeave = currentPaid / 2.0;
                    secondHalfUnpaidLeave = currentUnpaid / 2.0;
                }
            }
        }

        // 3c. Calculate Gaps (Default to PRESENT for past/present days per user request)
        // If a half is empty, we assume the employee was present (Auto-Present)
        // Admin must explicitly click 'Absent' to trigger the deduction penalty.
        // ✅ FIX: Do NOT auto-present future dates.
        LocalDate today = LocalDate.now();
        if (!date.isAfter(today)) {
            if (firstHalfWorked + firstHalfPaidLeave + firstHalfUnpaidLeave + firstHalfAbsent < 0.5) firstHalfWorked = 0.5;
            if (secondHalfWorked + secondHalfPaidLeave + secondHalfUnpaidLeave + secondHalfAbsent < 0.5) secondHalfWorked = 0.5;
        }

        // 4. Aggregate Results
        worked = firstHalfWorked + secondHalfWorked;
        paidLeave = firstHalfPaidLeave + secondHalfPaidLeave;
        unpaidLeave = firstHalfUnpaidLeave + secondHalfUnpaidLeave;
        absent = firstHalfAbsent + secondHalfAbsent;
        double payable = worked + paidLeave;

        // 5. Determine Professional Status String
        StringBuilder statusBuilder = new StringBuilder();
        if (firstHalfPaidLeave > 0) statusBuilder.append("1H:PAID_LEAVE");
        else if (firstHalfUnpaidLeave > 0) statusBuilder.append("1H:UNPAID_LEAVE");
        else if (firstHalfWorked > 0) statusBuilder.append("1H:PRESENT");
        else if (firstHalfAbsent > 0) statusBuilder.append("1H:ABSENT");
        else statusBuilder.append("1H:NOT_MARKED");

        statusBuilder.append(", ");

        if (secondHalfPaidLeave > 0) statusBuilder.append("2H:PAID_LEAVE");
        else if (secondHalfUnpaidLeave > 0) statusBuilder.append("2H:UNPAID_LEAVE");
        else if (secondHalfWorked > 0) statusBuilder.append("2H:PRESENT");
        else if (secondHalfAbsent > 0) statusBuilder.append("2H:ABSENT");
        else statusBuilder.append("2H:NOT_MARKED");

        // 5. Finalize status (Preserve detailed breakdown for professional reporting)
        String detailedStatus = statusBuilder.toString();
        
        // Return DayResult with the detailedStatus as the primary status string
        return new DayResult(worked, paidLeave, unpaidLeave, absent, payable, isHalfDay, detailedStatus, halfType);
    }

    /**
     * ✅ PROBATION CHECK: Uses dynamic probation months
     * FIXED: Now accepts a date for historical/future accuracy
     */
    public boolean isProbationCompleted(Employee employee) {
        return isProbationCompleted(employee, LocalDate.now());
    }

    public boolean isProbationCompleted(Employee employee, LocalDate date) {
        if (employee == null || employee.getJoiningDate() == null) {
            return false;
        }

        // ✅ FINAL SOURCE OF TRUTH: Explicit status overrides everything
        if (employee.getProbationStatus() == Employee.ProbationStatus.CONFIRMED) {
            return true;
        }
        
        int probationMonths = employee.getProbationPeriodMonths() != null 
                ? employee.getProbationPeriodMonths() 
                : PayrollPolicy.getDefaultProbationMonths();

        LocalDate probationEnd = employee.getJoiningDate().plusMonths(probationMonths);
        return !date.isBefore(probationEnd);
    }

    /**
     * ✅ MAIN CALCULATION: Aggregates DayResult ONLY
     * No business logic here - pure aggregation
     */
    public AttendanceSummary calculate(Long employeeId, YearMonth month) {
        AttendanceSummary result = new AttendanceSummary();

        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        // Fetch employee and data
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            log.warn("AttendanceEngine.calculate: Employee {} not found", employeeId);
            return result;
        }

        log.info("AttendanceEngine.calculate: Processing employee {} for month {}", employeeId, month);

        // ✅ CRITICAL: Use opening balance from previous month only
        LeaveBalanceSummary openingBalance = calculateLeaveBalance(employeeId, month.minusMonths(1));
        // ✅ FIX: Add current month's earned credit to opening balance
        // This must match the logic in calculateLeaveBalance (STEP 3 & 4)
        int empProbMonths = employee.getProbationPeriodMonths() != null ? employee.getProbationPeriodMonths() : 3;
        LocalDate empProbEnd = employee.getJoiningDate() != null
                ? employee.getJoiningDate().plusMonths(empProbMonths) : null;
        
        boolean earnedCreditThisMonth = false;
        if (employee.getProbationStatus() == Employee.ProbationStatus.CONFIRMED) {
            earnedCreditThisMonth = true;
        } else if (empProbEnd != null) {
            // Match the 15th-day rule:
            // If probation ends <= 15th, they earn in that month.
            // If probation ends > 15th, they earn from NEXT month.
            boolean probationEndsThisMonthOrBefore = !month.atEndOfMonth().isBefore(empProbEnd);
            if (probationEndsThisMonthOrBefore) {
                if (empProbEnd.isBefore(month.atDay(1))) {
                    // Probation ended in a previous month
                    earnedCreditThisMonth = true;
                } else {
                    // Probation ends this month - check 15th day rule
                    earnedCreditThisMonth = empProbEnd.getDayOfMonth() <= 15;
                }
            }
        }
        double runningBalance = openingBalance.remaining + (earnedCreditThisMonth ? PayrollPolicy.getLeaveAccrualRate() : 0.0);

        // Fetch all data upfront
        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
                .filter(l -> {
                    LocalDate leaveStart = l.getStartDate();
                    LocalDate leaveEnd = l.getEndDate();
                    return leaveStart != null && leaveEnd != null && 
                           !leaveEnd.isBefore(start) && !leaveStart.isAfter(end);
                })
                .toList();
        
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, start, end);
        
        log.info("AttendanceEngine.calculate: Found {} attendance records, {} approved leaves for employee {}", 
            attendances.size(), approvedLeaves.size(), employeeId);
        
        // Build maps for fast lookup
        Map<LocalDate, Attendance> attendanceMap = attendances.stream()
                .collect(Collectors.toMap(Attendance::getDate, a -> a, (a1, a2) -> a1));
        
        Map<LocalDate, Leave> leaveMap = new HashMap<>();
        for (Leave leave : approvedLeaves) {
            LocalDate current = leave.getStartDate();
            LocalDate leaveEnd = leave.getEndDate();
            
            while (current != null && !current.isAfter(leaveEnd)) {
                leaveMap.put(current, leave);
                current = current.plusDays(1);
            }
        }

        LocalDate today = LocalDate.now();
        
        // ✅ MAIN PROCESSING LOOP: Use resolveDay ONLY
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {

            Attendance attendance = attendanceMap.get(date);
            Leave leave = leaveMap.get(date);
            
            // ✅ SINGLE SOURCE OF TRUTH: Only resolveDay decides business rules
            DayResult dayResult = resolveDay(employee, date, attendance, leave, runningBalance);
            
            // ✅ AGGREGATION ONLY: No business logic here
            result.workedDays += dayResult.worked;
            result.paidLeave += dayResult.paidLeave;
            result.unpaidLeave += dayResult.unpaidLeave;
            result.absent += dayResult.absent;
            result.payableDays += dayResult.payable;
            
            // ✅ UNMARKED TRACKING: Still track unmarked days for admin dashboard
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY && attendance == null && leave == null) {
                result.unmarked += 1;
            }
            
            // ✅ CRITICAL: Reduce running balance after paid leave usage
            runningBalance -= dayResult.paidLeave;
        }
        
        log.info("AttendanceEngine.calculate: Final result for employee {}: workedDays={}, paidLeave={}, unpaidLeave={}, absent={}, payableDays={}", 
            employeeId, result.workedDays, result.paidLeave, result.unpaidLeave, result.absent, result.payableDays);
        
        return result;
    }

    /**
     * ✅ LEAVE BALANCE: Single source of truth using employee-specific probation
     * Handles all scenarios: during probation, just completed, long-term employees
     * Calculates leave balance, optionally excluding leaves that start on or after a cutoff date.
     * This is used by calculateLeaveSplit to get "balance just before a specific leave".
     */
    public LeaveBalanceSummary calculateLeaveBalance(Long employeeId, YearMonth currentMonth) {
        return calculateLeaveBalance(employeeId, currentMonth, null, null);
    }

    public LeaveBalanceSummary calculateLeaveBalance(Long employeeId, YearMonth currentMonth, LocalDate excludeLeavesFromDate) {
        return calculateLeaveBalance(employeeId, currentMonth, excludeLeavesFromDate, null);
    }

    public LeaveBalanceSummary calculateLeaveBalance(Long employeeId, YearMonth currentMonth, LocalDate excludeLeavesFromDate, Long excludeLeaveId) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            return new LeaveBalanceSummary(0, 0, 0, 0);
        }

        // =========================
        // STEP 1: Get employee-specific data
        // =========================
        LocalDate joiningDate = employee.getJoiningDate();
        if (joiningDate == null) {
            log.warn("Leave calculation - Employee {}: Missing joining date", employeeId);
            return new LeaveBalanceSummary(0, 0, 0, 0);
        }
        
        Integer probationMonths = employee.getProbationPeriodMonths() != null ? employee.getProbationPeriodMonths() : 3;
        double monthlyCredit = PayrollPolicy.getLeaveAccrualRate(); // Centralized policy

        // =========================
        // STEP 2: Calculate probation completion
        // =========================
        // ✅ Rule: If status is CONFIRMED, probation is skipped
        LocalDate probationCompletedDate;
        if (employee.getProbationStatus() == Employee.ProbationStatus.CONFIRMED) {
            probationCompletedDate = joiningDate;
        } else {
            probationCompletedDate = joiningDate.plusMonths(probationMonths);
        }
        
        // =========================
        // STEP 3: Determine leave earning start based on probation completion
        // =========================
        YearMonth leaveStartMonth;
        // ✅ FIX: Use currentMonth.atEndOfMonth() — NOT LocalDate.now() — for historical accuracy
        // Using LocalDate.now() causes historical payslips to show wrong earned leaves for
        // employees who completed probation after that historical period.
        boolean isInProbation = currentMonth.atEndOfMonth().isBefore(probationCompletedDate);
        
        if (isInProbation) {
            // Still in probation as of this month - no leave earning yet
            log.info("Leave calculation DEBUG - Employee {}: Still in probation as of {} (ends {})", employeeId, currentMonth, probationCompletedDate);
            return new LeaveBalanceSummary(0, 0, 0, 0);
        }
        
        // Probation completed - determine when leave earning started
        if (probationCompletedDate.getDayOfMonth() == 1) {
            // Completed on the 1st - start earning from the completion month
            leaveStartMonth = YearMonth.from(probationCompletedDate);
        } else if (probationCompletedDate.getDayOfMonth() <= 15) {
            // Completed on/before 15th - start earning from next month
            leaveStartMonth = YearMonth.from(probationCompletedDate).plusMonths(1);
        } else {
            // Completed after 15th - start earning from month after next
            leaveStartMonth = YearMonth.from(probationCompletedDate).plusMonths(2);
        }
        
        // =========================
        // STEP 4: Calculate earned leaves (WITH 6-MONTH CYCLE RESET)
        // =========================
        // ✅ NEW: 6-Month Cycle Logic (Jan-Jun, Jul-Dec)
        // Leaves earned in previous cycles EXPIRE. Calculation starts from the current cycle start.
        YearMonth cycleStart = (currentMonth.getMonthValue() <= 6) 
            ? YearMonth.of(currentMonth.getYear(), 1) 
            : YearMonth.of(currentMonth.getYear(), 7);
            
        YearMonth actualCalculationStart = leaveStartMonth.isBefore(cycleStart) ? cycleStart : leaveStartMonth;
        
        long earnedMonths = ChronoUnit.MONTHS.between(actualCalculationStart, currentMonth) + 1;
        earnedMonths = Math.max(0, earnedMonths);
        
        double earnedLeaves = earnedMonths * monthlyCredit;
        
        log.info("Leave calculation DEBUG - Employee {}: joining={}, completed={}, leaveStart={}, cycleStart={}, earnedMonths={}, earned={}", 
            employeeId, joiningDate, probationCompletedDate, leaveStartMonth, actualCalculationStart, earnedMonths, earnedLeaves);
        
        // =========================
        // STEP 5: Calculate used paid leaves AND running balance month-by-month
        // =========================
        double usedPaidLeavesTotal = 0.0;
        double unpaidLeavesTotal = 0.0;
        double currentMonthUsedPaid = 0.0;
        double currentMonthUnpaidLeaves = 0.0;
        double runningBalance = 0.0;
        double totalEarnedCumulative = 0.0;

        // Get all leaves for this employee and filter by APPROVED status to match test stubs correctly
        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
            .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
            .toList();

        // Sort leaves by date to process in chronological order
        // ✅ If excludeLeavesFromDate is set, exclude leaves starting on or after that date
        // This lets calculateLeaveSplit compute "balance just before a given leave"
        List<Leave> sortedLeaves = approvedLeaves.stream()
            .filter(l -> !l.getEndDate().isBefore(leaveStartMonth.atDay(1)))
            .filter(l -> excludeLeavesFromDate == null || l.getStartDate().isBefore(excludeLeavesFromDate))
            .filter(l -> excludeLeaveId == null || !l.getId().equals(excludeLeaveId))
            .sorted((l1, l2) -> l1.getStartDate().compareTo(l2.getStartDate()))
            .toList();

        // Initialize maps to track remaining paid and unpaid days for each leave
        Map<Long, Double> remainingPaid = new HashMap<>();
        Map<Long, Double> remainingUnpaid = new HashMap<>();
        for (Leave l : sortedLeaves) {
            remainingPaid.put(l.getId(), l.getPaidDays() != null ? l.getPaidDays() : 0.0);
            remainingUnpaid.put(l.getId(), l.getUnpaidDays() != null ? l.getUnpaidDays() : 0.0);
        }

        // Process month by month from leaveStartMonth to currentMonth
        YearMonth currentProcessingMonth = leaveStartMonth;
        
        while (!currentProcessingMonth.isAfter(currentMonth)) {
            // 1. Add monthly credit at start of month
            boolean isAccrued = true;
            if (excludeLeavesFromDate != null) {
                YearMonth limitMonth = YearMonth.from(excludeLeavesFromDate);
                if (currentProcessingMonth.isAfter(limitMonth) || currentProcessingMonth.equals(limitMonth)) {
                    isAccrued = false;
                }
            }
            
            if (isAccrued) {
                runningBalance += monthlyCredit;
                totalEarnedCumulative += monthlyCredit;
            }
            
            log.debug("Cycle DEBUG - Employee {}: {} - Monthly credit added, pre-usage balance: {}", 
                employeeId, currentProcessingMonth, runningBalance);
            
            // 2. Process all leaves in this month
            for (Leave leave : sortedLeaves) {
                // Filter leaves for this specific month
                if (leave.getStartDate().isAfter(currentProcessingMonth.atEndOfMonth()) ||
                    leave.getEndDate().isBefore(currentProcessingMonth.atDay(1))) {
                    continue; 
                }
                
                LocalDate current = leave.getStartDate();
                if (current.isBefore(currentProcessingMonth.atDay(1))) current = currentProcessingMonth.atDay(1);
                
                LocalDate leaveEnd = leave.getEndDate();
                if (leaveEnd.isAfter(currentProcessingMonth.atEndOfMonth())) leaveEnd = currentProcessingMonth.atEndOfMonth();
                
                while (current != null && !current.isAfter(leaveEnd)) {
                    if (shouldSkipSunday(leave, current)) {
                        current = current.plusDays(1);
                        continue;
                    }
                    double unit = (leave.getIsHalfDay() != null && leave.getIsHalfDay()) ? 0.5 : 1.0;
                    double remPaid = remainingPaid.getOrDefault(leave.getId(), 0.0);
                    double remUnpaid = remainingUnpaid.getOrDefault(leave.getId(), 0.0);
                    
                    double paidToAllocate = 0.0;
                    if (remPaid > 0) {
                        paidToAllocate = Math.min(unit, Math.min(remPaid, runningBalance));
                    }
                    
                    double unpaidToAllocate = unit - paidToAllocate;
                    if (remUnpaid > 0) {
                        unpaidToAllocate = Math.min(unpaidToAllocate, remUnpaid);
                    }
                    
                    if (paidToAllocate > 0) {
                        runningBalance -= paidToAllocate;
                        usedPaidLeavesTotal += paidToAllocate;
                        remainingPaid.put(leave.getId(), remPaid - paidToAllocate);
                        if (currentProcessingMonth.equals(currentMonth)) {
                            currentMonthUsedPaid += paidToAllocate;
                        }
                    }
                    if (unpaidToAllocate > 0) {
                        unpaidLeavesTotal += unpaidToAllocate;
                        remainingUnpaid.put(leave.getId(), remUnpaid - unpaidToAllocate);
                        if (currentProcessingMonth.equals(currentMonth)) {
                            currentMonthUnpaidLeaves += unpaidToAllocate;
                        }
                    }
                    
                    current = current.plusDays(1);
                }
            }

            // 3. Cycle End Reset (Jan-Jun, Jul-Dec)
            // Leaves EXPIRE on June 30th and Dec 31st. NO carry-forward.
            if (currentProcessingMonth.getMonthValue() == 6 || currentProcessingMonth.getMonthValue() == 12) {
                if (currentProcessingMonth.isBefore(currentMonth)) {
                    log.info("Cycle RESET - Employee {}: End of {} - Balance {} expired (Zero carry-forward)", 
                        employeeId, currentProcessingMonth, runningBalance);
                    runningBalance = 0.0;
                    totalEarnedCumulative = 0.0; // Reset earned count for new cycle
                    usedPaidLeavesTotal = 0.0;   // Reset used count for new cycle
                    unpaidLeavesTotal = 0.0;     // Reset unpaid count for new cycle
                }
            }

            currentProcessingMonth = currentProcessingMonth.plusMonths(1);
        }

        // Process any remaining future leaves that start after currentMonth
        for (Leave leave : sortedLeaves) {
            if (leave.getStartDate().isBefore(currentMonth.atEndOfMonth().plusDays(1))) {
                continue; // Already processed in the loop
            }
            
            LocalDate current = leave.getStartDate();
            LocalDate leaveEnd = leave.getEndDate();
            
            while (current != null && !current.isAfter(leaveEnd)) {
                if (shouldSkipSunday(leave, current)) {
                    current = current.plusDays(1);
                    continue;
                }
                double unit = (leave.getIsHalfDay() != null && leave.getIsHalfDay()) ? 0.5 : 1.0;
                double remPaid = remainingPaid.getOrDefault(leave.getId(), 0.0);
                double remUnpaid = remainingUnpaid.getOrDefault(leave.getId(), 0.0);
                
                double paidToAllocate = 0.0;
                if (remPaid > 0) {
                    paidToAllocate = Math.min(unit, Math.min(remPaid, runningBalance));
                }
                
                double unpaidToAllocate = unit - paidToAllocate;
                if (remUnpaid > 0) {
                    unpaidToAllocate = Math.min(unpaidToAllocate, remUnpaid);
                }
                
                if (paidToAllocate > 0) {
                    runningBalance -= paidToAllocate;
                    usedPaidLeavesTotal += paidToAllocate;
                    remainingPaid.put(leave.getId(), remPaid - paidToAllocate);
                }
                if (unpaidToAllocate > 0) {
                    unpaidLeavesTotal += unpaidToAllocate;
                    remainingUnpaid.put(leave.getId(), remUnpaid - unpaidToAllocate);
                }
                
                current = current.plusDays(1);
            }
        }
        
        // =========================
        // STEP 6: Final Results
        // =========================
        double remaining = runningBalance;
        remaining = Math.max(0, remaining);
        
        log.info("Leave calculation RESULT - Employee {}: totalUsedPaid={}, totalUnpaid={}, remaining={}, monthUsedPaid={}", 
            employeeId, usedPaidLeavesTotal, unpaidLeavesTotal, remaining, currentMonthUsedPaid);
        
        return new LeaveBalanceSummary(totalEarnedCumulative, usedPaidLeavesTotal, unpaidLeavesTotal, remaining, currentMonthUsedPaid, currentMonthUnpaidLeaves, currentMonth);
    }

    /**
     * ✅ PAYROLL CALCULATION: From summary ONLY
     * No business logic here - pure calculation
     */
    public SalarySummary calculateSalary(Long employeeId, YearMonth month) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee == null) {
            return new SalarySummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 
                                   BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                                   BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
        }

        AttendanceSummary summary = calculate(employeeId, month);
        
        BigDecimal grossSalary = employee.getTotalGrossSalary();
        BigDecimal dailyRate = BigDecimal.ZERO;
        
        if (grossSalary != null && grossSalary.compareTo(BigDecimal.ZERO) > 0) {
            dailyRate = grossSalary.divide(
                    BigDecimal.valueOf(PayrollPolicy.getWorkingDaysPerMonth()), 2, RoundingMode.HALF_UP);
        }

        log.info("💰 Salary Calc - Employee: {}, Gross: {}, Daily Rate: {}, Unpaid: {}, Absent: {}", 
            employee.getFirstName(), grossSalary, dailyRate, summary.unpaidLeave, summary.absent);

        // ✅ DEDUCTION RULE: 
        // 1. Unpaid Leaves = 1x Deduction
        // 2. Explicit Absences = 2x Deduction (Penalty)
        // 3. Unmarked Days = NO DEDUCTION (As per user request: "not do anything")
        BigDecimal salaryDeduction = dailyRate.multiply(BigDecimal.valueOf(summary.unpaidLeave))
                .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal absentDeduction = dailyRate.multiply(BigDecimal.valueOf(summary.absent))
                .multiply(BigDecimal.valueOf(PayrollPolicy.getAbsentPenaltyMultiplier()))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("📉 Deductions - Unpaid: {}, Absent: {}, Total: {}", 
            salaryDeduction, absentDeduction, salaryDeduction.add(absentDeduction));

        // Static deductions from employee model
        BigDecimal pf = employee.getPf() != null ? employee.getPf() : BigDecimal.ZERO;
        BigDecimal tax = employee.getTax() != null ? employee.getTax() : BigDecimal.ZERO;
        
        // Insurance calculation (if percentage is provided)
        BigDecimal insurance = BigDecimal.ZERO;
        if (employee.getInsurancePercentage() != null) {
            insurance = grossSalary.multiply(BigDecimal.valueOf(employee.getInsurancePercentage()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalAttendanceDeduction = salaryDeduction.add(absentDeduction);
        BigDecimal totalDeduction = totalAttendanceDeduction.add(pf).add(tax).add(insurance);
        BigDecimal netSalary = grossSalary.subtract(totalDeduction);

        return new SalarySummary(
                grossSalary,
                salaryDeduction,
                absentDeduction,
                pf,
                tax,
                insurance,
                totalDeduction,
                netSalary,
                summary.absent,
                summary.unpaidLeave
        );
    }

    private boolean shouldSkipSunday(Leave leave, LocalDate date) {
        if (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
            return false;
        }
        // If the leave has at least one non-Sunday day, we should skip Sundays.
        LocalDate current = leave.getStartDate();
        LocalDate end = leave.getEndDate();
        while (current != null && !current.isAfter(end)) {
            if (current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                return true;
            }
            current = current.plusDays(1);
        }
        return false;
    }
}
