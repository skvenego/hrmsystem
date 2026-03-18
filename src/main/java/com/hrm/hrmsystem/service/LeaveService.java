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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    private final LeaveRepository leaveRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveService(LeaveRepository leaveRepository, LeaveBalanceRepository leaveBalanceRepository,
                        EmployeeRepository employeeRepository) {
        this.leaveRepository = leaveRepository;
        this.leaveBalanceRepository = leaveBalanceRepository;
        this.employeeRepository = employeeRepository;
    }

    public LeaveDTO applyLeave(LeaveDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        int totalDays = (int) ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;

        Leave leave = Leave.builder()
                .employee(employee)
                .leaveType(Leave.LeaveType.valueOf(dto.getLeaveType()))
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .totalDays(totalDays)
                .reason(dto.getReason())
                .status(Leave.LeaveStatus.PENDING)
                .appliedDate(LocalDate.now())
                .build();

        leave = leaveRepository.save(leave);
        return convertToDTO(leave);
    }

    public LeaveDTO approveLeave(Long leaveId, String approvedBy) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        leave.setStatus(Leave.LeaveStatus.APPROVED);
        leave.setApprovedBy(approvedBy);

        // Deduct from leave balance
        deductLeaveBalance(leave.getEmployee().getId(), leave.getLeaveType(), leave.getTotalDays());

        leave = leaveRepository.save(leave);
        return convertToDTO(leave);
    }

    public LeaveDTO rejectLeave(Long leaveId, String rejectionReason) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        leave.setStatus(Leave.LeaveStatus.REJECTED);
        leave.setRejectionReason(rejectionReason);

        leave = leaveRepository.save(leave);
        return convertToDTO(leave);
    }

    public LeaveDTO cancelLeave(Long leaveId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getStatus() == Leave.LeaveStatus.APPROVED) {
            // Restore leave balance
            restoreLeaveBalance(leave.getEmployee().getId(), leave.getLeaveType(), leave.getTotalDays());
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
        int currentYear = LocalDate.now().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, currentYear)
                .orElseGet(() -> createDefaultLeaveBalance(employeeId, currentYear));
        return convertToBalanceDTO(balance);
    }

    public LeaveBalanceDTO initializeLeaveBalance(Long employeeId) {
        int currentYear = LocalDate.now().getYear();
        LeaveBalance balance = createDefaultLeaveBalance(employeeId, currentYear);
        return convertToBalanceDTO(balance);
    }

    private LeaveBalance createDefaultLeaveBalance(Long employeeId, int year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LeaveBalance balance = LeaveBalance.builder()
                .employee(employee)
                .sickLeave(12)
                .casualLeave(12)
                .annualLeave(15)
                .maternityLeave(90)
                .paternityLeave(15)
                .year(year)
                .build();

        return leaveBalanceRepository.save(balance);
    }

    private void deductLeaveBalance(Long employeeId, Leave.LeaveType leaveType, int days) {
        int currentYear = LocalDate.now().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, currentYear)
                .orElseGet(() -> createDefaultLeaveBalance(employeeId, currentYear));

        switch (leaveType) {
            case SICK -> balance.setSickLeave(balance.getSickLeave() - days);
            case CASUAL -> balance.setCasualLeave(balance.getCasualLeave() - days);
            case ANNUAL -> balance.setAnnualLeave(balance.getAnnualLeave() - days);
            case MATERNITY -> balance.setMaternityLeave(balance.getMaternityLeave() - days);
            case PATERNITY -> balance.setPaternityLeave(balance.getPaternityLeave() - days);
            default -> {}
        }

        leaveBalanceRepository.save(balance);
    }

    private void restoreLeaveBalance(Long employeeId, Leave.LeaveType leaveType, int days) {
        int currentYear = LocalDate.now().getYear();
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, currentYear)
                .orElseThrow(() -> new RuntimeException("Leave balance not found"));

        switch (leaveType) {
            case SICK -> balance.setSickLeave(balance.getSickLeave() + days);
            case CASUAL -> balance.setCasualLeave(balance.getCasualLeave() + days);
            case ANNUAL -> balance.setAnnualLeave(balance.getAnnualLeave() + days);
            case MATERNITY -> balance.setMaternityLeave(balance.getMaternityLeave() + days);
            case PATERNITY -> balance.setPaternityLeave(balance.getPaternityLeave() + days);
            default -> {}
        }

        leaveBalanceRepository.save(balance);
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
                .reason(leave.getReason())
                .status(leave.getStatus().name())
                .approvedBy(leave.getApprovedBy())
                .rejectionReason(leave.getRejectionReason())
                .appliedDate(leave.getAppliedDate())
                .build();
    }

    private LeaveBalanceDTO convertToBalanceDTO(LeaveBalance balance) {
        return LeaveBalanceDTO.builder()
                .id(balance.getId())
                .employeeId(balance.getEmployee().getId())
                .employeeName(balance.getEmployee().getFirstName() + " " + balance.getEmployee().getLastName())
                .sickLeave(balance.getSickLeave())
                .casualLeave(balance.getCasualLeave())
                .annualLeave(balance.getAnnualLeave())
                .maternityLeave(balance.getMaternityLeave())
                .paternityLeave(balance.getPaternityLeave())
                .year(balance.getYear())
                .build();
    }
}