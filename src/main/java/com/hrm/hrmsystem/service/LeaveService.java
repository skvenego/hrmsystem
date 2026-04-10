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
        int totalDays = (int) ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;

        // Handle half day
        boolean isHalfDay = dto.getIsHalfDay() != null && dto.getIsHalfDay();
        if (isHalfDay) {
            totalDays = 1;
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
                .paidDays(calcResult.paidDays)
                .unpaidDays(calcResult.unpaidDays)
                .isHalfDay(isHalfDay)
                .reason(dto.getReason())
                .status(Leave.LeaveStatus.PENDING)
                .appliedDate(LocalDate.now())
                .build();

        leave = leaveRepository.save(leave);

        LeaveDTO result = convertToDTO(leave);
        result.setPaidDays(calcResult.paidDays);
        result.setUnpaidDays(calcResult.unpaidDays);

        return result;
    }

    @Transactional
    public LeaveDTO approveLeave(Long leaveId, String approvedBy) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() != Leave.LeaveStatus.PENDING) {
            throw new RuntimeException("Leave is not in pending status");
        }

        leave.setStatus(Leave.LeaveStatus.APPROVED);
        leave.setApprovedBy(approvedBy);

        // Deduct from leave balance (only paid days)
        if (leave.getPaidDays() != null && leave.getPaidDays() > 0) {
            deductLeaveBalance(leave.getEmployee().getId(), leave.getPaidDays());
        }

        // Track unpaid days separately
        if (leave.getUnpaidDays() != null && leave.getUnpaidDays() > 0) {
            addUnpaidLeaveBalance(leave.getEmployee().getId(), leave.getUnpaidDays());
        }

        leave = leaveRepository.save(leave);
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

    private LeaveCalculationResult calculateLeaveDays(Employee employee, int totalDays, boolean isHalfDay, ProbationInfo probationInfo) {
        int paidDays = 0;
        int unpaidDays = 0;

        // During probation: all leaves are unpaid
        if (probationInfo.inProbation) {
            paidDays = 0;
            unpaidDays = totalDays;
            return new LeaveCalculationResult(paidDays, unpaidDays, "During probation: All leaves are unpaid");
        }

        // After probation: Apply max 3 paid leaves rule
        LeaveBalance balance = getOrCreateCurrentBalance(employee);
        int availableBalance = balance.getAvailableLeaves();

        // Max 3 paid leaves at one time rule
        int maxPaidForThisRequest = Math.min(MAX_PAID_LEAVES_AT_ONCE, availableBalance);

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
        // Calculate earned leaves based on months in cycle
        int monthsInCycle = 6; // Each cycle is 6 months
        int earnedLeaves = (int) (monthsInCycle * LEAVES_PER_MONTH); // 1.5 per month = 9 per cycle

        // Check for carried forward from previous cycle
        int carriedForward = 0;
        if (cycle == 2) {
            // Check cycle 1 balance
            LeaveBalance prevCycle = leaveBalanceRepository
                    .findByEmployeeIdAndYearAndCycle(employee.getId(), year, 1)
                    .orElse(null);
            if (prevCycle != null) {
                int remaining = prevCycle.getAvailableLeaves();
                // Max 10 leaves can be carried forward
                carriedForward = Math.min(remaining, 10);
            }
        }

        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .totalEarnedLeaves(earnedLeaves)
                .usedLeaves(0)
                .unpaidLeaves(0)
                .carriedForwardLeaves(carriedForward)
                .cycle(cycle)
                .year(year)
                .build();

        return leaveBalanceRepository.save(balance);
    }

    private void deductLeaveBalance(Long employeeId, int days) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYearAndCycle(employeeId, currentYear, currentCycle)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setUsedLeaves(balance.getUsedLeaves() + days);
        leaveBalanceRepository.save(balance);
    }

    private void restoreLeaveBalance(Long employeeId, int days) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYearAndCycle(employeeId, currentYear, currentCycle)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setUsedLeaves(balance.getUsedLeaves() - days);
        leaveBalanceRepository.save(balance);
    }

    private void addUnpaidLeaveBalance(Long employeeId, int days) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYearAndCycle(employeeId, currentYear, currentCycle)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setUnpaidLeaves(balance.getUnpaidLeaves() + days);
        leaveBalanceRepository.save(balance);
    }

    private void removeUnpaidLeaveBalance(Long employeeId, int days) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        int currentCycle = currentMonth <= 6 ? 1 : 2;

        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYearAndCycle(employeeId, currentYear, currentCycle)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        balance.setUnpaidLeaves(balance.getUnpaidLeaves() - days);
        leaveBalanceRepository.save(balance);
    }

    // Payroll calculation method
    public BigDecimal calculateLeaveDeduction(Employee employee, int unpaidDays, boolean isHalfDay) {
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
                .approvedBy(leave.getApprovedBy())
                .rejectionReason(leave.getRejectionReason())
                .appliedDate(leave.getAppliedDate())
                .build();
    }

    private LeaveBalanceDTO convertToBalanceDTO(LeaveBalance balance, ProbationInfo probationInfo) {
        return LeaveBalanceDTO.builder()
                .id(balance.getId())
                .employeeId(balance.getEmployee().getId())
                .employeeName(balance.getEmployee().getFirstName() + " " + balance.getEmployee().getLastName())
                .totalEarnedLeaves(balance.getTotalEarnedLeaves())
                .usedLeaves(balance.getUsedLeaves())
                .unpaidLeaves(balance.getUnpaidLeaves())
                .carriedForwardLeaves(balance.getCarriedForwardLeaves())
                .availableLeaves(balance.getAvailableLeaves())
                .cycle(balance.getCycle())
                .year(balance.getYear())
                .inProbation(probationInfo.inProbation)
                .probationStatus(probationInfo.status)
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
        int paidDays;
        int unpaidDays;
        String message;

        LeaveCalculationResult(int paidDays, int unpaidDays, String message) {
            this.paidDays = paidDays;
            this.unpaidDays = unpaidDays;
            this.message = message;
        }
    }
}