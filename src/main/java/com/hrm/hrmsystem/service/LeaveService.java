package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.LeaveDTO;
import com.hrm.hrmsystem.dto.LeaveBalanceDTO;
import com.hrm.hrmsystem.engine.AttendanceEngine;
import com.hrm.hrmsystem.engine.AttendanceSummary;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.model.LeaveBalance;
import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import com.hrm.hrmsystem.repository.LeaveBalanceRepository;
import com.hrm.hrmsystem.repository.AttendanceRepository;
import com.hrm.hrmsystem.util.EmailUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class LeaveService {

    private static final Logger logger = LoggerFactory.getLogger(LeaveService.class);

    private final LeaveRepository leaveRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveCalculationService leaveCalculationService;
    private final LeaveBalanceService leaveBalanceService;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceEngine attendanceEngine;
    private final AuditLogService auditLogService;
    private final PayrollLockService payrollLockService;
    private final EmailUtil emailUtil;

    // Constants
    private static final int WORKING_DAYS_PER_MONTH = 26;

    public LeaveService(LeaveRepository leaveRepository, LeaveBalanceRepository leaveBalanceRepository,
                        EmployeeRepository employeeRepository, LeaveCalculationService leaveCalculationService,
                        LeaveBalanceService leaveBalanceService, AttendanceRepository attendanceRepository,
                        AttendanceEngine attendanceEngine,
                        AuditLogService auditLogService, PayrollLockService payrollLockService, EmailUtil emailUtil) {
        this.leaveRepository = leaveRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
        this.leaveCalculationService = leaveCalculationService;
        this.leaveBalanceService = leaveBalanceService;
        this.attendanceRepository = attendanceRepository;
        this.attendanceEngine = attendanceEngine;
        this.auditLogService = auditLogService;
        this.payrollLockService = payrollLockService;
        this.emailUtil = emailUtil;
    }

    @Transactional
    public LeaveDTO applyLeave(LeaveDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // CRITICAL FIX: Validate dates are not in the past
        LocalDate today = LocalDate.now();
        if (dto.getStartDate().isBefore(today)) {
            throw new RuntimeException("Cannot apply leave for past dates. Start date " + 
                dto.getStartDate() + " is before today " + today);
        }

        // CRITICAL FIX: Detect target month for leave application
        YearMonth leaveMonth = YearMonth.from(dto.getStartDate());
        logger.info("Applying leave for employee {} in month: {}", dto.getEmployeeId(), leaveMonth);

        // Calculate total days EXCLUDING Sundays
        double totalDays = countDaysExcludingSundays(dto.getStartDate(), dto.getEndDate());

        // Handle half day
        boolean isHalfDay = dto.getIsHalfDay() != null && dto.getIsHalfDay();
        if (isHalfDay || "HALF".equals(dto.getLeaveType())) {
            totalDays = 0.5;
            isHalfDay = true;
        }

        // VALIDATION RULE: Check if both halves are already marked as leave for same date
        if (isHalfDay && dto.getHalfType() != null) {
            LocalDate leaveDate = dto.getStartDate();
            List<Leave> existingLeaves = leaveRepository.findByEmployeeId(dto.getEmployeeId()).stream()
                    .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED || l.getStatus() == Leave.LeaveStatus.PENDING)
                    .filter(l -> l.getStartDate() != null && l.getStartDate().equals(leaveDate))
                    .filter(l -> l.getIsHalfDay() != null && l.getIsHalfDay())
                    .collect(Collectors.toList());

            for (Leave existingLeave : existingLeaves) {
                if (existingLeave.getHalfType() != null) {
                    // If existing leave is for the other half, reject or convert to full day
                    Leave.HalfType existingHalfType = existingLeave.getHalfType();
                    Leave.HalfType newHalfType = Leave.HalfType.valueOf(dto.getHalfType());

                    if (existingHalfType != newHalfType) {
                        // Different halves - this would result in both halves being leave
                        throw new RuntimeException(
                            "Cannot apply half-day leave for " + newHalfType + " - " + existingHalfType + 
                            " is already marked as leave on " + leaveDate + ". " +
                            "Please apply for a full-day leave instead."
                        );
                    }
                }
            }
        }

        // STEP 4: PREVENT OVERLAPPING LEAVES - Check for existing approved/pending leaves
        List<Leave> existingLeaves = leaveRepository.findByEmployeeId(dto.getEmployeeId()).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED || l.getStatus() == Leave.LeaveStatus.PENDING)
                .filter(l -> l.getStartDate() != null && l.getEndDate() != null)
                .collect(Collectors.toList());

        for (Leave existingLeave : existingLeaves) {
            // Check if new leave dates overlap with existing leave
            boolean overlap = !dto.getStartDate().isAfter(existingLeave.getEndDate()) &&
                            !dto.getEndDate().isBefore(existingLeave.getStartDate());
            
            if (overlap) {
                throw new RuntimeException(
                    "You are already on leave from " + existingLeave.getStartDate() + 
                    " to " + existingLeave.getEndDate() + 
                    " (Status: " + existingLeave.getStatus() + "). " +
                    "Cannot apply overlapping leave for " + dto.getStartDate() + 
                    " to " + dto.getEndDate() + "."
                );
            }
        }

        // Parse half type if provided
        Leave.HalfType halfType = null;
        if (isHalfDay && dto.getHalfType() != null) {
            try {
                halfType = Leave.HalfType.valueOf(dto.getHalfType());
            } catch (IllegalArgumentException e) {
                // Invalid half type, default to null
                logger.warn("Invalid half type provided: {}", dto.getHalfType());
            }
        }

        // Check probation status
        ProbationInfo probationInfo = checkProbationStatus(employee);

        // CRITICAL FIX: Validate using TOTAL remaining till end date (not monthly)
        if (!probationInfo.inProbation && !dto.getLeaveType().equals("UNPAID")) {
            LocalDate endDate = dto.getEndDate();
            
            // Calculate total earned till end date
            double totalEarned = leaveBalanceService.calculateTotalEarned(employee.getId(), endDate);
            
            // Calculate total used till end date (including current application)
            double totalUsed = leaveRepository.sumApprovedPaidLeavesTill(employee.getId(), endDate);
            
            // Total remaining balance
            double totalRemaining = Math.max(0, totalEarned - totalUsed);
            
            // STRICT VALIDATION: Cannot apply more paid leave than total available balance
            // This validates against the total balance as of the leave end date
            double requestedPaidDays = dto.getLeaveType().equals("CASUAL") || dto.getLeaveType().equals("SICK") 
                ? (isHalfDay ? 0.5 : totalDays) 
                : 0;
                
            if (requestedPaidDays > totalRemaining) {
                throw new RuntimeException(
                    String.format("Insufficient paid leave balance for %s %d. " +
                        "Total Available: %.1f days, Requested: %.1f days. " +
                        "Please reduce leave days or mark as unpaid.",
                        leaveMonth.getMonth(), leaveMonth.getYear(),
                        totalRemaining, requestedPaidDays)
                );
            }
        }

        // Validate and calculate paid/unpaid days
        // Pass leave start date to calculate projected balance as of leave month (for future accrual consideration)
        LeaveCalculationResult calcResult = calculateLeaveDays(employee, totalDays, isHalfDay, probationInfo, dto.getStartDate());

        Leave leave = Leave.builder()
                .employee(employee)
                .leaveType(Leave.LeaveType.valueOf(dto.getLeaveType()))
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .totalDays(totalDays)
                .paidDays((double) calcResult.paidDays)
                .unpaidDays((double) calcResult.unpaidDays)
                .isHalfDay(isHalfDay)
                .halfType(halfType)
                .reason(dto.getReason())
                .contactNumber(dto.getContactNumber())
                .status(Leave.LeaveStatus.PENDING)
                .appliedDate(LocalDate.now())
                .build();

        leave = leaveRepository.save(leave);

        LeaveDTO result = convertToDTO(leave);
        result.setPaidDays((double) calcResult.paidDays);
        result.setUnpaidDays((double) calcResult.unpaidDays);

        return result;
    }

    /**
     * 🔒 VALIDATION: Prevent modification of approved leaves
     * This is the CORE protection for the freeze mechanism
     */
    private void validateLeaveNotApproved(Leave leave) {
        if (leave.getStatus() == Leave.LeaveStatus.APPROVED) {
            throw new RuntimeException("Cannot modify approved leave. Leave is frozen and cannot be changed.");
        }
    }

    @Transactional
    public LeaveDTO approveLeave(Long leaveId, String approvedBy) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        // Validate state
        if (leave.getStatus() != Leave.LeaveStatus.PENDING) {
            throw new RuntimeException("Leave is not in pending state");
        }

        // Recalculate paid/unpaid days to ensure correctness (especially for half-days)
        ProbationInfo probationInfo = checkProbationStatus(leave.getEmployee());
        
        // CRITICAL FIX: Recalculate total days excluding Sundays (don't use stored value)
        double totalDays = countDaysExcludingSundays(leave.getStartDate(), leave.getEndDate());
        // Handle half-day
        if (leave.getIsHalfDay() != null && leave.getIsHalfDay()) {
            totalDays = 0.5;
        }
        double paidDays = 0;
        double unpaidDays = 0;

        if (probationInfo.inProbation) {
            // During probation: all days are unpaid
            paidDays = 0;
            unpaidDays = totalDays;
        } else {
            // After probation: Calculate paid/unpaid per month for multi-month leaves
            // Example: Apr 29 - May 2 = Apr portion (2 days) + May portion (2 days)
            LocalDate leaveStart = leave.getStartDate();
            LocalDate leaveEnd = leave.getEndDate();
            
            // Process each month portion separately
            LocalDate currentMonthStart = leaveStart.withDayOfMonth(1);
            
            // Track remaining balance for the entire leave period
            double remainingBalance = 0;
            
            while (!currentMonthStart.isAfter(leaveEnd)) {
                LocalDate currentMonthEnd = currentMonthStart.withDayOfMonth(currentMonthStart.lengthOfMonth());
                
                // Calculate overlap with this month
                LocalDate overlapStart = leaveStart.isAfter(currentMonthStart) ? leaveStart : currentMonthStart;
                LocalDate overlapEnd = leaveEnd.isBefore(currentMonthEnd) ? leaveEnd : currentMonthEnd;
                
                if (!overlapStart.isAfter(overlapEnd)) {
                    // CRITICAL FIX: Count days excluding Sundays for this month portion
                    long daysInMonth = countDaysExcludingSundays(overlapStart, overlapEnd);
                    
                    // Get projected balance as of this month
                    double monthBalance = calculateProjectedBalance(leave.getEmployee(), overlapStart);
                    
                    // CRITICAL FIX: Calculate available balance for this month
                    // Available = This month's balance - Any remaining balance from previous months
                    double availableForThisMonth = monthBalance + remainingBalance;
                    
                    // Calculate paid/unpaid for this month portion
                    double monthPaid = Math.min(daysInMonth, Math.max(0, availableForThisMonth));
                    double monthUnpaid = daysInMonth - monthPaid;
                    
                    // Update remaining balance for next month
                    remainingBalance = availableForThisMonth - monthPaid;
                    
                    paidDays += monthPaid;
                    unpaidDays += monthUnpaid;
                    
                    logger.info("Leave month portion {} to {}: {} days, monthBalance={}, remaining={}, available={}, paid={}, unpaid={}",
                            overlapStart, overlapEnd, daysInMonth, monthBalance, remainingBalance + monthPaid, availableForThisMonth, monthPaid, monthUnpaid);
                }
                
                // Move to next month
                currentMonthStart = currentMonthStart.plusMonths(1);
            }
        }

        // Update leave with recalculated values
        leave.setPaidDays(paidDays);
        leave.setUnpaidDays(unpaidDays);
        leave.setStatus(Leave.LeaveStatus.APPROVED);
        leave.setApprovedBy(approvedBy);
        
        // 🔒 FREEZE DATA AT APPROVAL TIME - Prevent recalculation
        leave.setFinalPaidDays(paidDays);
        leave.setFinalUnpaidDays(unpaidDays);
        leave.setFinalTotalDays(totalDays);
        leave.setFrozenAt(LocalDate.now());

        // Save the leave first
        leave = leaveRepository.save(leave);
        leaveRepository.flush(); // Force flush to ensure data is persisted

        // Deduct from leave balance ONLY for paid days
        // Unpaid leaves do NOT affect leave balance, only salary
        if (paidDays > 0) {
            deductLeaveBalance(leave.getEmployee().getId(), paidDays);
        }
        // Unpaid leaves are NOT tracked in leave balance - they only affect salary

        // Initialize attendance for half-day leave
        if (leave.getIsHalfDay() != null && leave.getIsHalfDay() && leave.getHalfType() != null) {
            initializeHalfDayAttendance(leave);
        }

        // Reload from database to get fresh data
        leave = leaveRepository.findById(leave.getId())
                .orElseThrow(() -> new RuntimeException("Leave not found after save"));
        return convertToDTO(leave);
    }

    @Transactional
    public LeaveDTO rejectLeave(Long leaveId, String rejectionReason) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        leave.setStatus(Leave.LeaveStatus.REJECTED);
        leave.setRejectionReason(rejectionReason);

        leave = leaveRepository.save(leave);
        return convertToDTO(leave);
    }

    @Transactional
    public LeaveDTO cancelLeave(Long leaveId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        // 🔒 VALIDATION: Prevent cancellation of approved leaves
        validateLeaveNotApproved(leave);

        leave.setStatus(Leave.LeaveStatus.CANCELLED);
        leave = leaveRepository.save(leave);
        
        // Note: Payroll recalculation should be handled separately by the calling service/controller
        // to avoid circular dependency between LeaveService and PayrollService
        
        return convertToDTO(leave);
    }

    public List<LeaveDTO> getLeavesByEmployee(Long employeeId) {
        return leaveRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveDTO> getPendingLeaves() {
        return leaveRepository.findByStatus(Leave.LeaveStatus.PENDING)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveDTO> getAllLeaves() {
        return leaveRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<LeaveDTO> findById(Long id) {
        return leaveRepository.findById(id)
                .map(this::convertToDTO);
    }

    public LeaveBalanceDTO getLeaveBalance(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        // 🔥 SINGLE SOURCE OF TRUTH: Use AttendanceEngine for all leave calculations
        AttendanceEngine.LeaveBalanceSummary summary =
                attendanceEngine.calculateLeaveBalance(employeeId, currentYear, currentMonth);

        logger.info("DEBUG ENGINE VALUES - Employee {}: earned={}, used={}, unpaid={}, currentMonthUsed={}, currentMonthUnpaid={}",
                employeeId, summary.earnedLeaves, summary.usedLeaves, summary.unpaidLeaves, summary.currentMonthUsed, summary.currentMonthUnpaid);

        // Use earned leaves from engine (single source of truth)
        double totalEarnedLeaves = summary.earnedLeaves;

        logger.info("DEBUG SERVICE VALUES - Employee {}: totalEarnedLeaves={}", employeeId, totalEarnedLeaves);

        // Get or create balance record
        List<LeaveBalance> balanceList = leaveBalanceRepository.findAllByEmployeeIdAndYearAndCycle(employeeId, currentYear, currentCycle);
        LeaveBalance balance = balanceList.stream().findFirst()
                .orElseGet(() -> createLeaveBalance(employee, currentYear, currentCycle));

        ProbationInfo probationInfo = checkProbationStatus(employee);

        // Calculate cycle expiry warning
        LocalDate cycleEndDate = currentCycle == 1 ? LocalDate.of(currentYear, 6, 30) : LocalDate.of(currentYear, 12, 31);
        long daysUntilCycleEnd = ChronoUnit.DAYS.between(today, cycleEndDate);

        boolean cycleExpiryWarning = false;
        String cycleExpiryMessage = null;

        // Show warning if within 30 days of cycle end and has unused leaves
        if (daysUntilCycleEnd <= 30 && daysUntilCycleEnd >= 0 && (totalEarnedLeaves - summary.usedLeaves) > 0) {
            cycleExpiryWarning = true;
            cycleExpiryMessage = "Cycle ends in " + daysUntilCycleEnd + " days. Unused leaves will expire.";
        }

        // DEBUG: Log values before building DTO
        System.out.println("DEBUG LEAVE BALANCE - Employee: " + employeeId);
        System.out.println("DEBUG - Total Earned: " + totalEarnedLeaves);
        System.out.println("DEBUG - Used Leaves: " + summary.usedLeaves);
        System.out.println("DEBUG - Remaining: " + Math.max(0, totalEarnedLeaves - summary.usedLeaves));
        
        // DEBUG: Log what we're setting in DTO
        System.out.println("DEBUG SETTING - openingBalance: " + totalEarnedLeaves);
        System.out.println("DEBUG SETTING - earnedThisMonth: " + summary.usedLeaves);
        System.out.println("DEBUG SETTING - usedThisMonth: 0.0");
        System.out.println("DEBUG SETTING - remaining: " + Math.max(0, totalEarnedLeaves - summary.usedLeaves));

        LeaveBalanceDTO dto = LeaveBalanceDTO.builder()
                .id(balance.getId())
                .employeeId(employeeId)
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .totalEarnedLeaves(totalEarnedLeaves)
                .usedLeaves(summary.usedLeaves)
                .unpaidLeaves(summary.unpaidLeaves)
                .carriedForwardLeaves(0) // No carry forward allowed
                .availableLeaves(Math.max(0, totalEarnedLeaves - summary.usedLeaves))
                // UI fields - Map calculated values to frontend fields
                .openingBalance(totalEarnedLeaves) // Total Earned: calculated → shows in "Total Earned" field
                .earnedThisMonth(summary.usedLeaves) // Used: calculated → shows in "Used" field  
                .usedThisMonth(0.0) // Not used → shows in old "Used This Month" field
                .remaining(Math.max(0, totalEarnedLeaves - summary.usedLeaves)) // Remaining: calculated → shows in "Remaining" field
                .cycle(currentCycle)
                .year(currentYear)
                .inProbation(probationInfo.inProbation)
                .probationStatus(probationInfo.inProbation ? "In Progress" : "Completed")
                .joiningDate(probationInfo.startDate != null ? probationInfo.startDate.toString() : null)
                .probationEndDate(probationInfo.endDate != null ? probationInfo.endDate.toString() : null)
                .cycleExpiryWarning(cycleExpiryWarning)
                .cycleExpiryMessage(cycleExpiryMessage)
                .daysUntilCycleEnd((int) daysUntilCycleEnd)
                .workingDaysForNextLeave(null) // Not applicable for monthly accrual
                .nextLeaveProgress(null) // Not applicable for monthly accrual
                .build();

        return dto;
    }

    /**
     * Initialize or get leave balance - delegates to getLeaveBalance to avoid duplicate code
     */
    public LeaveBalanceDTO initializeLeaveBalance(Long employeeId) {
        // CRITICAL FIX: Remove duplicate code - simply delegate to getLeaveBalance
        // Both methods do the same calculation
        return getLeaveBalance(employeeId);
    }

    /**
     * Calculate projected balance as of a specific leave month.
     * This considers future accrual when validating leaves applied in advance.
     * 
     * Example: In April, user applies for May leave:
     * - April earned: 6.0 (4 months × 1.5)
     * - May earned: 7.5 (5 months × 1.5) ← includes May accrual
     * - Used leaves: Only count leaves ending on or before May 31
     * - Available: 7.5 - used
     */
    /**
     * 🔥 SINGLE SOURCE OF TRUTH: Use AttendanceEngine for projected balance
     */
    public double calculateProjectedBalance(Employee employee, LocalDate leaveStartDate) {
        if (employee.getJoiningDate() == null) {
            return 0.0;
        }
        
        int leaveYear = leaveStartDate.getYear();
        int leaveMonth = leaveStartDate.getMonthValue();
        
        // 🔥 ONE CALL to engine - all calculations inside
        AttendanceEngine.LeaveBalanceSummary summary = 
                attendanceEngine.calculateLeaveBalance(employee.getId(), leaveYear, leaveMonth);
        
        // Available = earned - used (from engine)
        double available = Math.max(0, summary.earnedLeaves - summary.usedLeaves);
        
        logger.info("Projected balance for emp {} ({}-{}): earned={}, used={}, available={}",
                employee.getId(), leaveMonth, leaveYear, summary.earnedLeaves, summary.usedLeaves, available);
        
        return available;
    }

    private LeaveBalance getOrCreateCurrentBalance(Employee employee) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        List<LeaveBalance> balanceList = leaveBalanceRepository.findAllByEmployeeIdAndYearAndCycle(employee.getId(), currentYear, currentCycle);
        return balanceList.stream().findFirst()
                .orElseGet(() -> createLeaveBalance(employee, currentYear, currentCycle));
    }

    private LeaveBalance createLeaveBalance(Employee employee, int year, int cycle) {
        // Calculate earned leaves based on working days since probation completion
        // No carry forward allowed per new rules
        int earnedLeaves = (int) attendanceEngine.calculateLeaveBalance(employee.getId(), year, LocalDate.now().getMonthValue()).earnedLeaves;

        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .totalEarnedLeaves(earnedLeaves)
                .usedLeaves(0.0)
                .carriedForwardLeaves(0) // No carry forward allowed
                .carryForwardLimit(0) // No carry forward allowed
                .carriedForwardFromPrevious(0) // No carry forward allowed
                .expiredLeaves(0) // No carry forward, so no expiry
                .year(year)
                .cycle(cycle)
                .build();
        return leaveBalanceRepository.save(balance);
    }
    
    private void deductLeaveBalance(Long employeeId, double days) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        var stats = leaveCalculationService.calculateLeaveStatistics(employeeId, currentMonth, currentYear);

        List<LeaveBalance> balanceList = leaveBalanceRepository.findAllByEmployeeIdAndYearAndCycle(employeeId, currentYear, currentCycle);
        LeaveBalance balance = balanceList.stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setUsedLeaves(balance.getUsedLeaves() + days);
        leaveBalanceRepository.save(balance);
    }

    private void restoreLeaveBalance(Long employeeId, double days) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        var stats = leaveCalculationService.calculateLeaveStatistics(employeeId, currentMonth, currentYear);

        List<LeaveBalance> balanceList = leaveBalanceRepository.findAllByEmployeeIdAndYearAndCycle(employeeId, currentYear, currentCycle);
        LeaveBalance balance = balanceList.stream().findFirst()
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setUsedLeaves(balance.getUsedLeaves() - days);
        leaveBalanceRepository.save(balance);
    }

    // Payroll calculation method
    public BigDecimal calculateLeaveDeduction(Employee employee, double unpaidDays, boolean isHalfDay) {
        if (employee.getSalary() == null || employee.getSalary().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // Per day salary = Monthly Salary / Working Days (26)
        BigDecimal perDaySalary = employee.getSalary().divide(BigDecimal.valueOf(WORKING_DAYS_PER_MONTH), 2, RoundingMode.HALF_UP);

        // For half day, deduct half of per day salary
        BigDecimal deductionMultiplier = isHalfDay ? new BigDecimal("0.5") : BigDecimal.ONE;
        
        return perDaySalary.multiply(BigDecimal.valueOf(unpaidDays)).multiply(deductionMultiplier);
    }

    private LeaveDTO convertToDTO(Leave leave) {
        // CRITICAL FIX: Recalculate all values fresh (don't use stored values which may be wrong)
        double recalculatedTotalDays = countDaysExcludingSundays(leave.getStartDate(), leave.getEndDate());
        if (leave.getIsHalfDay() != null && leave.getIsHalfDay()) {
            recalculatedTotalDays = 0.5;
        }
        
        // For approved leaves, recalculate paid/unpaid based on monthly balance
        double recalculatedPaidDays = leave.getPaidDays() != null ? leave.getPaidDays() : 0;
        double recalculatedUnpaidDays = leave.getUnpaidDays() != null ? leave.getUnpaidDays() : 0;
        
        // If total days changed, adjust paid/unpaid proportionally
        if (leave.getTotalDays() != null && leave.getTotalDays() > 0) {
            double ratio = recalculatedTotalDays / leave.getTotalDays();
            recalculatedPaidDays = recalculatedPaidDays * ratio;
            recalculatedUnpaidDays = recalculatedUnpaidDays * ratio;
        }
        
        return LeaveDTO.builder()
                .id(leave.getId())
                .employeeId(leave.getEmployee().getId())
                .employeeName(leave.getEmployee().getFirstName() + " " + leave.getEmployee().getLastName())
                .leaveType(leave.getLeaveType().name())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .totalDays(recalculatedTotalDays)
                .paidDays(recalculatedPaidDays)
                .unpaidDays(recalculatedUnpaidDays)
                .isHalfDay(leave.getIsHalfDay())
                .halfType(leave.getHalfType() != null ? leave.getHalfType().name() : null)
                .reason(leave.getReason())
                .status(leave.getStatus().name())
                .appliedDate(leave.getAppliedDate())
                .approvedBy(leave.getApprovedBy())
                .rejectionReason(leave.getRejectionReason())
                .build();
    }

    /**
     * Initialize attendance for half-day leave
     * STRICT RULE: When half-day leave is approved, the entire day is marked as PENDING
     * Admin must resolve whether the employee worked the remaining half
     * DO NOT auto-mark as HALF_DAY or LEAVE - always PENDING for admin resolution
     */
    private void initializeHalfDayAttendance(Leave leave) {
        LocalDate leaveDate = leave.getStartDate();
        if (leaveDate == null) {
            logger.warn("Cannot initialize attendance for half-day leave: no start date");
            return;
        }

        // Check if attendance already exists for this date
        List<Attendance> existingAttendance = attendanceRepository.findAllByEmployeeIdAndDate(
                leave.getEmployee().getId(), leaveDate);

        if (!existingAttendance.isEmpty()) {
            logger.info("Attendance already exists for employee {} on {}, skipping initialization",
                    leave.getEmployee().getId(), leaveDate);
            return;
        }

        // STRICT RULE: Always mark as PENDING for half-day leave
        // Admin must resolve: was the remaining half worked (PRESENT) or not (ABSENT)?
        Attendance attendance = Attendance.builder()
                .employee(leave.getEmployee())
                .date(leaveDate)
                .status(Attendance.AttendanceStatus.PENDING)
                .remarks("Half-day leave approved - " + leave.getHalfType().name() + ". Admin must resolve remaining half.")
                .build();

        attendanceRepository.save(attendance);
    }

    /**
 * Cancel a leave request (for approved leaves only)
 * 🔒 UPDATED: Prevent cancellation of approved leaves - freeze mechanism
 */
@Transactional
public LeaveDTO cancelLeave(Long leaveId, String reason) {
    Leave leave = leaveRepository.findById(leaveId)
            .orElseThrow(() -> new RuntimeException("Leave not found"));

    // 🔒 VALIDATION: Prevent cancellation of approved leaves
    // Once approved, leave is frozen and cannot be cancelled
    validateLeaveNotApproved(leave);

    // Log old status for audit
    String oldStatus = leave.getStatus().name();

    // Change status to CANCELLED and save cancellation reason
        leave.setStatus(Leave.LeaveStatus.CANCELLED);
        leave.setCancellationReason(reason);
        leave = leaveRepository.save(leave);

        // Remove leave effect from attendance - mark as PENDING for admin resolution
        LocalDate currentDate = leave.getStartDate();
        while (!currentDate.isAfter(leave.getEndDate())) {
            List<Attendance> attendances = attendanceRepository.findAllByEmployeeIdAndDate(
                    leave.getEmployee().getId(), currentDate);

            if (!attendances.isEmpty()) {
                Attendance attendance = attendances.get(0);
                // If attendance was marked as ON_LEAVE due to this leave, change to PENDING
                if (attendance.getStatus() == Attendance.AttendanceStatus.ON_LEAVE ||
                    attendance.getStatus() == Attendance.AttendanceStatus.PENDING) {
                    attendance.setStatus(Attendance.AttendanceStatus.PENDING);
                    attendance.setRemarks("Leave cancelled - pending admin resolution");
                    attendanceRepository.save(attendance);
                    logger.info("Marked attendance as PENDING for cancelled leave: employee {}, date {}",
                            leave.getEmployee().getId(), currentDate);
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        // Audit log
        auditLogService.logChange(
            "LEAVE",
            leaveId,
            "CANCEL",
            oldStatus,
            "CANCELLED",
            leave.getEmployee().getId(),
            reason != null ? reason : "Leave cancelled by admin"
        );

        logger.info("Cancelled leave ID {} for employee {}. Attendance marked as PENDING.",
                leaveId, leave.getEmployee().getId());

        // Send email notification to employee with cancellation reason
        try {
            String employeeEmail = leave.getEmployee().getEmail();
            String employeeName = leave.getEmployee().getFirstName() + " " + leave.getEmployee().getLastName();
            if (employeeEmail != null && !employeeEmail.trim().isEmpty()) {
                emailUtil.sendLeaveCancellationNotification(
                    employeeEmail,
                    employeeName,
                    leave.getLeaveType().name(),
                    leave.getStartDate().toString(),
                    leave.getEndDate().toString(),
                    reason
                );
                logger.info("Sent cancellation email to employee: {}", employeeEmail);
            } else {
                logger.warn("Employee email not found, skipping cancellation notification");
            }
        } catch (Exception e) {
            logger.error("Failed to send cancellation email: {}", e.getMessage());
            // Don't fail the cancellation if email fails
        }
        
        // Note: Payroll recalculation should be handled separately by the calling service/controller
        // to avoid circular dependency between LeaveService and PayrollService

        return convertToDTO(leave);
    }

    /**
     * Modify an approved leave
     * Updates leave details and recalculates attendance
     */
    @Transactional
    public LeaveDTO modifyLeave(Long leaveId, LeaveDTO updatedLeaveDTO, String reason) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        // Only allow modifying APPROVED leaves
        if (leave.getStatus() != Leave.LeaveStatus.APPROVED) {
            throw new RuntimeException("Can only modify APPROVED leaves. Current status: " + leave.getStatus());
        }

        // Check payroll lock for the leave dates
        int month = leave.getStartDate().getMonthValue();
        int year = leave.getStartDate().getYear();
        if (payrollLockService.isPayrollLocked(month, year)) {
            throw new RuntimeException("Cannot modify leave: Payroll is locked for " + year + "-" + month + ". Please use salary adjustment system instead.");
        }

        // Log old values for audit
        String oldStatus = leave.getStatus().name();
        String oldDates = leave.getStartDate() + " to " + leave.getEndDate();
        String oldHalfDay = leave.getIsHalfDay() ? (leave.getHalfType() != null ? leave.getHalfType().name() : "YES") : "NO";

        // Store old values for attendance recalculation
        LocalDate oldStartDate = leave.getStartDate();
        LocalDate oldEndDate = leave.getEndDate();
        boolean oldIsHalfDay = leave.getIsHalfDay();
        Leave.HalfType oldHalfType = leave.getHalfType();

        // Update leave details
        if (updatedLeaveDTO.getStartDate() != null) {
            leave.setStartDate(updatedLeaveDTO.getStartDate());
        }
        if (updatedLeaveDTO.getEndDate() != null) {
            leave.setEndDate(updatedLeaveDTO.getEndDate());
        }
        if (updatedLeaveDTO.getIsHalfDay() != null) {
            leave.setIsHalfDay(updatedLeaveDTO.getIsHalfDay());
        }
        if (updatedLeaveDTO.getHalfType() != null) {
            try {
                leave.setHalfType(Leave.HalfType.valueOf(updatedLeaveDTO.getHalfType()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid half type provided: {}", updatedLeaveDTO.getHalfType());
            }
        }
        if (updatedLeaveDTO.getReason() != null) {
            leave.setReason(updatedLeaveDTO.getReason());
        }

        // Recalculate total days
        double totalDays = ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
        if (leave.getIsHalfDay()) {
            totalDays = 0.5;
        }
        leave.setTotalDays(totalDays);

        // Change status to MODIFIED
        leave.setStatus(Leave.LeaveStatus.MODIFIED);
        leave = leaveRepository.save(leave);

        // Recalculate attendance: remove old leave effect, apply new leave effect
        // First, remove old leave effect from attendance
        LocalDate currentDate = oldStartDate;
        while (!currentDate.isAfter(oldEndDate)) {
            List<Attendance> attendances = attendanceRepository.findAllByEmployeeIdAndDate(
                    leave.getEmployee().getId(), currentDate);

            if (!attendances.isEmpty()) {
                Attendance attendance = attendances.get(0);
                // If attendance was marked as ON_LEAVE or PENDING due to this leave, change to PENDING
                if (attendance.getStatus() == Attendance.AttendanceStatus.ON_LEAVE ||
                    attendance.getStatus() == Attendance.AttendanceStatus.PENDING) {
                    attendance.setStatus(Attendance.AttendanceStatus.PENDING);
                    attendance.setRemarks("Leave modified - pending recalculation");
                    attendanceRepository.save(attendance);
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        // Then, apply new leave effect to attendance
        currentDate = leave.getStartDate();
        while (!currentDate.isAfter(leave.getEndDate())) {
            List<Attendance> attendances = attendanceRepository.findAllByEmployeeIdAndDate(
                    leave.getEmployee().getId(), currentDate);

            if (attendances.isEmpty()) {
                // Create new attendance record
                Attendance attendance = Attendance.builder()
                        .employee(leave.getEmployee())
                        .date(currentDate)
                        .status(Attendance.AttendanceStatus.PENDING)
                        .remarks("Modified leave - pending admin resolution")
                        .build();
                attendanceRepository.save(attendance);
            } else {
                // Update existing attendance
                Attendance attendance = attendances.get(0);
                attendance.setStatus(Attendance.AttendanceStatus.PENDING);
                attendance.setRemarks("Modified leave - pending admin resolution");
                attendanceRepository.save(attendance);
            }
            currentDate = currentDate.plusDays(1);
        }

        // Audit log
        String newDates = leave.getStartDate() + " to " + leave.getEndDate();
        String newHalfDay = leave.getIsHalfDay() ? (leave.getHalfType() != null ? leave.getHalfType().name() : "YES") : "NO";

        auditLogService.logChange(
            "LEAVE",
            leaveId,
            "MODIFY",
            oldDates + " | " + oldHalfDay,
            newDates + " | " + newHalfDay,
            leave.getEmployee().getId(),
            reason != null ? reason : "Leave modified by admin"
        );

        logger.info("Modified leave ID {} for employee {}. Attendance recalculated.",
                leaveId, leave.getEmployee().getId());

        return convertToDTO(leave);
    }

    /**
     * Delete all leave records
     */
    @Transactional
    public void deleteAllLeaves() {
        leaveRepository.deleteAll();
        leaveBalanceRepository.deleteAll(); // Also clear leave balances to reset used leaves
    }

    /**
     * Check probation status for an employee
     */
    private ProbationInfo checkProbationStatus(Employee employee) {
        if (employee.getJoiningDate() == null) {
            return new ProbationInfo(false, "Unknown", null, null);
        }
        
        int probationMonths = employee.getProbationPeriodMonths() != null ? 
                employee.getProbationPeriodMonths() : 3;
        LocalDate probationEndDate = employee.getJoiningDate().plusMonths(probationMonths);
        LocalDate today = LocalDate.now();
        
        boolean inProbation = !today.isAfter(probationEndDate);
        String status = inProbation ? "In Progress" : "Completed";
        
        return new ProbationInfo(inProbation, status, employee.getJoiningDate(), probationEndDate);
    }

    /**
     * 🔥 SINGLE SOURCE OF TRUTH: Calculate leave days using AttendanceEngine
     */
    private LeaveCalculationResult calculateLeaveDays(Employee employee, double totalDays, 
                                                      boolean isHalfDay, ProbationInfo probationInfo, 
                                                      LocalDate leaveStartDate) {
        // During probation: all leaves are unpaid
        if (probationInfo.inProbation) {
            double unpaidDays = isHalfDay ? 0.5 : totalDays;
            return new LeaveCalculationResult(0, unpaidDays, 
                    "In probation: " + unpaidDays + " day(s) unpaid");
        }
        
        // After probation: Use AttendanceEngine for balance
        int leaveYear = leaveStartDate.getYear();
        int leaveMonth = leaveStartDate.getMonthValue();
        
        // 🔥 ONE CALL to engine
        AttendanceEngine.LeaveBalanceSummary summary = 
                attendanceEngine.calculateLeaveBalance(employee.getId(), leaveYear, leaveMonth);
        
        double available = Math.max(0, summary.earnedLeaves - summary.usedLeaves);
        
        // Calculate paid/unpaid
        double paidDays, unpaidDays;
        String message;
        
        if (isHalfDay) {
            // Half days: 0.5 paid if any balance available
            paidDays = available >= 0.5 ? 0.5 : 0;
            unpaidDays = 0.5 - paidDays;
            message = "Half day: " + paidDays + " paid, " + unpaidDays + " unpaid";
        } else {
            // Full days: min of total vs available
            paidDays = Math.min(totalDays, available);
            unpaidDays = totalDays - paidDays;
            message = unpaidDays > 0
                    ? paidDays + " day(s) paid, " + unpaidDays + " day(s) unpaid"
                    : "All " + paidDays + " days are paid leaves";
        }
        
        logger.info("Leave days for emp {} ({}-{}): earned={}, used={}, available={}, total={}, paid={}, unpaid={}",
                employee.getId(), leaveMonth, leaveYear, 
                summary.earnedLeaves, summary.usedLeaves, available,
                totalDays, paidDays, unpaidDays);
        
        return new LeaveCalculationResult(paidDays, unpaidDays, message);
    }

    // Inner classes
    private static class ProbationInfo {
        boolean inProbation;
        String status;
        LocalDate startDate;
        LocalDate endDate;

        ProbationInfo(boolean inProbation, String status, LocalDate startDate, LocalDate endDate) {
            this.inProbation = inProbation;
            this.status = status;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }

    private static class LeaveCalculationResult {
        double paidDays;
        double unpaidDays;
        String message;

        LeaveCalculationResult(double paidDays, double unpaidDays, String message) {
            this.paidDays = paidDays;
            this.unpaidDays = unpaidDays;
            this.message = message;
        }
    }

    /**
     * Count days between two dates EXCLUDING Sundays
     * Sundays are not counted as leave days
     */
    private long countDaysExcludingSundays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return 0;
        }
        
        long days = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            // Only count if NOT Sunday (DayOfWeek.SUNDAY = 7)
            if (current.getDayOfWeek().getValue() != 7) {
                days++;
            }
            current = current.plusDays(1);
        }
        return days;
    }
}




