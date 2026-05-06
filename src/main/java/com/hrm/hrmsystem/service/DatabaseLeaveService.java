package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.repository.LeaveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * DATABASE-FIRST LEAVE SERVICE
 * Source of truth for all leave statistics
 * Always queries database directly, no calculations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DatabaseLeaveService {

    private final LeaveRepository leaveRepository;

    /**
     * Get paid leave days for employee in a month
     * Direct database query, no calculations
     */
    public double getPaidLeaveDays(Long employeeId, int year, int month) {
        LocalDate monthStart = YearMonth.of(year, month).atDay(1);
        LocalDate monthEnd = YearMonth.of(year, month).atEndOfMonth();

        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
                .filter(l -> {
                    LocalDate leaveStart = l.getStartDate();
                    LocalDate leaveEnd = l.getEndDate();
                    return leaveStart != null && leaveEnd != null &&
                           !leaveEnd.isBefore(monthStart) && !leaveStart.isAfter(monthEnd);
                })
                .toList();

        double totalPaid = 0.0;
        for (Leave leave : approvedLeaves) {
            if (leave.getPaidDays() != null) {
                totalPaid += leave.getPaidDays();
            }
        }

        log.debug("Database: Paid leave days for employee {} in {}/{} = {}", employeeId, month, year, totalPaid);
        return totalPaid;
    }

    /**
     * Get unpaid leave days for employee in a month
     * Direct database query, no calculations
     */
    public double getUnpaidLeaveDays(Long employeeId, int year, int month) {
        LocalDate monthStart = YearMonth.of(year, month).atDay(1);
        LocalDate monthEnd = YearMonth.of(year, month).atEndOfMonth();

        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
                .filter(l -> {
                    LocalDate leaveStart = l.getStartDate();
                    LocalDate leaveEnd = l.getEndDate();
                    return leaveStart != null && leaveEnd != null &&
                           !leaveEnd.isBefore(monthStart) && !leaveStart.isAfter(monthEnd);
                })
                .toList();

        double totalUnpaid = 0.0;
        for (Leave leave : approvedLeaves) {
            if (leave.getUnpaidDays() != null) {
                totalUnpaid += leave.getUnpaidDays();
            }
        }

        log.debug("Database: Unpaid leave days for employee {} in {}/{} = {}", employeeId, month, year, totalUnpaid);
        return totalUnpaid;
    }

    /**
     * Get total used leave days (paid + unpaid) for employee in a month
     * Direct database query, no calculations
     */
    public double getUsedLeaveDays(Long employeeId, int year, int month) {
        double paid = getPaidLeaveDays(employeeId, year, month);
        double unpaid = getUnpaidLeaveDays(employeeId, year, month);
        double total = paid + unpaid;

        log.debug("Database: Total used leave days for employee {} in {}/{} = {} (paid: {}, unpaid: {})", 
                 employeeId, month, year, total, paid, unpaid);
        return total;
    }

    /**
     * Get all approved leaves for employee in a month
     * Direct database query, no calculations
     */
    public List<Leave> getApprovedLeaves(Long employeeId, int year, int month) {
        LocalDate monthStart = YearMonth.of(year, month).atDay(1);
        LocalDate monthEnd = YearMonth.of(year, month).atEndOfMonth();

        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
                .filter(l -> {
                    LocalDate leaveStart = l.getStartDate();
                    LocalDate leaveEnd = l.getEndDate();
                    return leaveStart != null && leaveEnd != null &&
                           !leaveEnd.isBefore(monthStart) && !leaveStart.isAfter(monthEnd);
                })
                .toList();

        log.debug("Database: Found {} approved leaves for employee {} in {}/{}", 
                 approvedLeaves.size(), employeeId, month, year);
        return approvedLeaves;
    }
}
