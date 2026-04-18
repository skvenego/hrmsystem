package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.LeaveDTO;
import com.hrm.hrmsystem.dto.LeaveBalanceDTO;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.model.LeaveBalance;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import com.hrm.hrmsystem.repository.LeaveBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    // Constants
    private static final int MAX_PAID_LEAVES_AT_ONCE = 3;
    private static final double LEAVES_PER_MONTH = 1.5;
    private static final int WORKING_DAYS_PER_MONTH = 26;

    public LeaveService(LeaveRepository leaveRepository, LeaveBalanceRepository leaveBalanceRepository,
                        EmployeeRepository employeeRepository) {
        this.leaveRepository = leaveRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public LeaveDTO applyLeave(LeaveDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Calculate total days
        double totalDays = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;

        // Handle half day
        boolean isHalfDay = dto.getIsHalfDay() != null && dto.getIsHalfDay();
        if (isHalfDay || "HALF".equals(dto.getLeaveType())) {
            totalDays = 0.5;
            isHalfDay = true;
        }

        // Check probation status
        ProbationInfo probationInfo = checkProbationStatus(employee);

        // Validate and calculate paid/unpaid days
        LeaveCalculationResult calcResult = calculateLeaveDays(employee, totalDays, isHalfDay, probationInfo);

        Leave leave = Leave.builder()
                .employee(employee)
                .leaveType(Leave.LeaveType.valueOf(dto.getLeaveType()))
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .totalDays(totalDays)
                .paidDays((double) calcResult.paidDays)
                .unpaidDays((double) calcResult.unpaidDays)
                .isHalfDay(isHalfDay)
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
        LeaveBalance balance = getOrCreateCurrentBalance(leave.getEmployee());
        double availableBalance = balance.getAvailableLeaves();

        double totalDays = leave.getTotalDays();
        double paidDays, unpaidDays;

        if (probationInfo.inProbation) {
            // During probation: all days are unpaid
            paidDays = 0;
            unpaidDays = totalDays;
        } else if (availableBalance <= 0) {
            // No balance - all unpaid
            paidDays = 0;
            unpaidDays = totalDays;
        } else {
            // Has balance - all days can be paid (max 3 at once)
            double maxPaid = Math.min(MAX_PAID_LEAVES_AT_ONCE, availableBalance);
            if (totalDays <= maxPaid) {
                paidDays = totalDays;
                unpaidDays = 0;
            } else {
                paidDays = maxPaid;
                unpaidDays = totalDays - maxPaid;
            }
        }

        // Update leave with recalculated values
        leave.setPaidDays(paidDays);
        leave.setUnpaidDays(unpaidDays);
        leave.setStatus(Leave.LeaveStatus.APPROVED);
        leave.setApprovedBy(approvedBy);

        // Save the leave first
        leave = leaveRepository.save(leave);
        leaveRepository.flush(); // Force flush to ensure data is persisted

        // Deduct from leave balance based on paid/unpaid days
        if (paidDays > 0) {
            deductLeaveBalance(leave.getEmployee().getId(), paidDays);
        }
        if (unpaidDays > 0) {
            addUnpaidLeaveBalance(leave.getEmployee().getId(), unpaidDays);
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

        if (leave.getStatus() == Leave.LeaveStatus.APPROVED) {
            // Restore leave balance
            if (leave.getPaidDays() != null && leave.getPaidDays() > 0) {
                restoreLeaveBalance(leave.getEmployee().getId(), leave.getPaidDays());
            }
            if (leave.getUnpaidDays() != null && leave.getUnpaidDays() > 0) {
                removeUnpaidLeaveBalance(leave.getEmployee().getId(), leave.getUnpaidDays());
            }
        }

        leave.setStatus(Leave.LeaveStatus.CANCELLED);
        leave = leaveRepository.save(leave);
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

        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYearAndCycle(employeeId, currentYear, currentCycle)
                .orElseGet(() -> createLeaveBalance(employee, currentYear, currentCycle));

        ProbationInfo probationInfo = checkProbationStatus(employee);

        return convertToBalanceDTO(balance, probationInfo);
    }

    public LeaveBalanceDTO initializeLeaveBalance(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        LeaveBalance balance = createLeaveBalance(employee, currentYear, currentCycle);
        ProbationInfo probationInfo = checkProbationStatus(employee);

        return convertToBalanceDTO(balance, probationInfo);
    }

    private ProbationInfo checkProbationStatus(Employee employee) {
        if (employee.getJoiningDate() == null || employee.getProbationPeriodMonths() == null) {
            return new ProbationInfo(false, "No probation period defined", null, null);
        }

        LocalDate probationEndDate = employee.getJoiningDate().plusMonths(employee.getProbationPeriodMonths());
        boolean inProbation = LocalDate.now().isBefore(probationEndDate);

        String status;
        if (inProbation) {
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), probationEndDate);
            status = "In Progress (" + daysRemaining + " days remaining)";
        } else {
            status = "Completed";
        }

        return new ProbationInfo(inProbation, status, employee.getJoiningDate(), probationEndDate);
    }

    private LeaveCalculationResult calculateLeaveDays(Employee employee, double totalDays, boolean isHalfDay, ProbationInfo probationInfo) {
        double paidDays = 0;
        double unpaidDays = 0;

        // During probation: all leaves are unpaid
        if (probationInfo.inProbation) {
            paidDays = 0;
            unpaidDays = totalDays;
            return new LeaveCalculationResult(paidDays, unpaidDays, "During probation: All leaves are unpaid");
        }

        // After probation: Apply max 3 paid leaves rule
        LeaveBalance balance = getOrCreateCurrentBalance(employee);
        double availableBalance = balance.getAvailableLeaves();

        // Max 3 paid leaves at one time rule
        int maxPaidForThisRequest = Math.min(MAX_PAID_LEAVES_AT_ONCE, (int) availableBalance);

        if (totalDays <= maxPaidForThisRequest) {
            // All days can be paid
            paidDays = totalDays;
            unpaidDays = 0;
        } else {
            // Split between paid and unpaid
            paidDays = maxPaidForThisRequest;
            unpaidDays = totalDays - maxPaidForThisRequest;
        }

        String message = unpaidDays > 0
                ? "Only " + paidDays + " days are paid (max 3 at once). " + unpaidDays + " days are unpaid."
                : "All " + paidDays + " days are paid leaves.";

        return new LeaveCalculationResult(paidDays, unpaidDays, message);
    }

    private LeaveBalance getOrCreateCurrentBalance(Employee employee) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        return leaveBalanceRepository.findByEmployeeIdAndYearAndCycle(employee.getId(), currentYear, currentCycle)
                .orElseGet(() -> createLeaveBalance(employee, currentYear, currentCycle));
    }

    private LeaveBalance createLeaveBalance(Employee employee, int year, int cycle) {
        // Calculate earned leaves based on actual months since probation completion
        int earnedLeaves = calculateEarnedLeaves(employee, year, cycle);

        // Check for carried forward from previous cycle
        int carriedForward = 0;
        if (cycle == 2) {
            // Check cycle 1 balance
            LeaveBalance prevCycle = leaveBalanceRepository
                    .findByEmployeeIdAndYearAndCycle(employee.getId(), year, 1)
                    .orElse(null);
            if (prevCycle != null) {
                double remaining = prevCycle.getAvailableLeaves();
                // Max 10 leaves can be carried forward
                carriedForward = Math.min((int) remaining, 10);
            }
        }

        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .totalEarnedLeaves(earnedLeaves)
                .usedLeaves(0.0)
                .unpaidLeaves(0.0)
                .carriedForwardLeaves(carriedForward)
                .cycle(cycle)
                .year(year)
                .build();

        return leaveBalanceRepository.save(balance);
    }

    /**
     * Calculate earned leaves based on actual months worked since probation completion
     * 1.5 leaves per month, calculated from probation end date to current date
     * Only FULL completed months are counted (partial months don't count)
     */
    private int calculateEarnedLeaves(Employee employee, int year, int cycle) {
        // If still in probation, no earned leaves available (they accrue but are frozen)
        if (employee.getJoiningDate() == null || employee.getProbationPeriodMonths() == null) {
            return 0;
        }

        LocalDate probationEndDate = employee.getJoiningDate().plusMonths(employee.getProbationPeriodMonths());
        LocalDate today = LocalDate.now();

        // If still in probation, earned leaves are 0 (frozen)
        if (today.isBefore(probationEndDate)) {
            return 0;
        }

        // Calculate months since probation completion
        LocalDate effectiveStartDate = probationEndDate;
        
        // If probation completed before current cycle started, count from cycle start
        LocalDate cycleStartDate;
        if (cycle == 1) {
            cycleStartDate = LocalDate.of(year, 1, 1);
        } else {
            cycleStartDate = LocalDate.of(year, 7, 1);
        }
        
        // Start counting from whichever is later: probation completion or cycle start
        LocalDate countingStartDate = effectiveStartDate.isAfter(cycleStartDate) ? effectiveStartDate : cycleStartDate;
        
        // Calculate months worked in this cycle
        LocalDate cycleEndDate = cycle == 1 ? LocalDate.of(year, 6, 30) : LocalDate.of(year, 12, 31);
        LocalDate countingEndDate = today.isBefore(cycleEndDate) ? today : cycleEndDate;
        
        if (countingStartDate.isAfter(countingEndDate)) {
            return 0;
        }

        // Calculate completed full months only (partial months don't count)
        long monthsWorked = ChronoUnit.MONTHS.between(countingStartDate, countingEndDate);

        // Cap at 6 months per cycle
        monthsWorked = Math.min(monthsWorked, 6);

        // Calculate earned leaves (1.5 per month)
        double earned = monthsWorked * LEAVES_PER_MONTH;
        return (int) earned;
    }

    private void deductLeaveBalance(Long employeeId, double days) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYearAndCycle(employeeId, currentYear, currentCycle)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setUsedLeaves(balance.getUsedLeaves() + days);
        leaveBalanceRepository.save(balance);
    }

    private void restoreLeaveBalance(Long employeeId, double days) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYearAndCycle(employeeId, currentYear, currentCycle)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setUsedLeaves(balance.getUsedLeaves() - days);
        leaveBalanceRepository.save(balance);
    }

    private void addUnpaidLeaveBalance(Long employeeId, double days) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYearAndCycle(employeeId, currentYear, currentCycle)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setUnpaidLeaves(balance.getUnpaidLeaves() + days);
        leaveBalanceRepository.save(balance);
    }

    private void removeUnpaidLeaveBalance(Long employeeId, double days) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYearAndCycle(employeeId, currentYear, currentCycle)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setUnpaidLeaves(balance.getUnpaidLeaves() - days);
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
        return LeaveDTO.builder()
                .id(leave.getId())
                .employeeId(leave.getEmployee().getId())
                .employeeName(leave.getEmployee().getFirstName() + " " + leave.getEmployee().getLastName())
                .leaveType(leave.getLeaveType().name())
                .startDate(leave.getStartDate())
                .endDate(leave.getEndDate())
                .totalDays(leave.getTotalDays())
                .paidDays(leave.getPaidDays())
                .unpaidDays(leave.getUnpaidDays())
                .isHalfDay(leave.getIsHalfDay())
                .reason(leave.getReason())
                .status(leave.getStatus().name())
                .appliedDate(leave.getAppliedDate())
                .approvedBy(leave.getApprovedBy())
                .rejectionReason(leave.getRejectionReason())
                .contactNumber(leave.getContactNumber())
                .build();
    }

    private LeaveBalanceDTO convertToBalanceDTO(LeaveBalance balance, ProbationInfo probationInfo) {
        // Recalculate earned leaves based on current date (monthly accrual)
        int currentEarnedLeaves = calculateEarnedLeaves(balance.getEmployee(), balance.getYear(), balance.getCycle());

        // Calculate available leaves: earned + carried forward - used
        double availableLeaves = currentEarnedLeaves + balance.getCarriedForwardLeaves() - balance.getUsedLeaves();
        if (availableLeaves < 0) availableLeaves = 0.0;

        return LeaveBalanceDTO.builder()
                .id(balance.getId())
                .employeeId(balance.getEmployee().getId())
                .employeeName(balance.getEmployee().getFirstName() + " " + balance.getEmployee().getLastName())
                .totalEarnedLeaves(currentEarnedLeaves)
                .usedLeaves(balance.getUsedLeaves())
                .unpaidLeaves(balance.getUnpaidLeaves())
                .carriedForwardLeaves(balance.getCarriedForwardLeaves())
                .availableLeaves(availableLeaves)
                .cycle(balance.getCycle())
                .year(balance.getYear())
                .inProbation(probationInfo.inProbation)
                .probationStatus(probationInfo.status)
                .joiningDate(probationInfo.startDate != null ? probationInfo.startDate.toString() : null)
                .probationEndDate(probationInfo.endDate != null ? probationInfo.endDate.toString() : null)
                .build();
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
}