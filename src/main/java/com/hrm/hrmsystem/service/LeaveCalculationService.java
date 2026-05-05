package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.engine.AttendanceEngine;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Centralized Leave Calculation Service
 * All leave-related calculations happen here - single source of truth
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveCalculationService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceEngine attendanceEngine;

    /**
     * Complete leave statistics for an employee
     * Used by all pages: leave.html, payslips.html, my-leaves.html, payroll.js
     * Implements cycle expiry: leaves expire at end of each 6-month cycle
     */
    public LeaveStatistics calculateLeaveStatistics(Long employeeId, int month, int year) {
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Employee not found: " + employeeId));

        // 🔥 SINGLE SOURCE OF TRUTH: Use AttendanceEngine for ALL leave calculations
        AttendanceEngine.LeaveBalanceSummary summary = attendanceEngine.calculateLeaveBalance(employeeId, year, month);
        
        // Safeguard for earned leaves until engine is fully fixed
        if (summary.earnedLeaves == 0) {
            log.warn("Earned leaves not calculated in engine for employee {} ({}-{})", employeeId, month, year);
        }
        
        double totalEarnedLeaves = summary.earnedLeaves;
        double totalUsedPaidLeaves = summary.usedLeaves;
        double currentMonthPaid = summary.currentMonthUsed;
        double currentMonthUnpaid = summary.currentMonthUnpaid;
        double currentMonthTotal = summary.currentMonthUsed + summary.currentMonthUnpaid;
        
        // Use engine's available calculation
        double availableLeaves = summary.getAvailableLeaves();
        double remainingPaidLeaves = availableLeaves;

        // Build result
        LeaveStatistics stats = new LeaveStatistics();
        stats.setEmployeeId(employeeId);
        stats.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());
        stats.setMonth(month);
        stats.setYear(year);
        
        // Current month data
        stats.setCurrentMonthPaidLeaves(currentMonthPaid);
        stats.setCurrentMonthUnpaidLeaves(currentMonthUnpaid);
        stats.setCurrentMonthTotalLeaves(currentMonthTotal);
        stats.setCurrentMonthUsedLeaves(summary.currentMonthUsed);
        
        // All-time data
        stats.setTotalEarnedLeaves(totalEarnedLeaves);
        stats.setTotalUsedPaidLeaves(totalUsedPaidLeaves);
        stats.setRemainingPaidLeaves(remainingPaidLeaves);
        stats.setAvailableLeaveBalance(availableLeaves);
        
        // Additional info - tracked in engine if needed
        stats.setTotalApprovedLeaves(totalUsedPaidLeaves);

        log.info("Leave statistics for emp {} ({}-{}): earned={}, used={}, available={}, current={} paid, {} unpaid", 
                employeeId, month, year, totalEarnedLeaves, totalUsedPaidLeaves, availableLeaves, currentMonthPaid, currentMonthUnpaid);

        return stats;
    }

    /**
     * Simple DTO for leave statistics
     */
    public static class LeaveStatistics {
        private Long employeeId;
        private String employeeName;
        private int month;
        private int year;
        
        // Current month
        private double currentMonthPaidLeaves;
        private double currentMonthUnpaidLeaves;
        private double currentMonthTotalLeaves;
        private double currentMonthUsedLeaves;
        
        // All-time totals
        private double totalEarnedLeaves;
        private double totalUsedPaidLeaves;
        private double remainingPaidLeaves;
        private double availableLeaveBalance;
        
        // Total approved leaves (double to avoid data loss)
        private double totalApprovedLeaves;

        // Getters and Setters
        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
        
        public String getEmployeeName() { return employeeName; }
        public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
        
        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }
        
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }
        
        public double getCurrentMonthPaidLeaves() { return currentMonthPaidLeaves; }
        public void setCurrentMonthPaidLeaves(double currentMonthPaidLeaves) { this.currentMonthPaidLeaves = currentMonthPaidLeaves; }
        
        public double getCurrentMonthUnpaidLeaves() { return currentMonthUnpaidLeaves; }
        public void setCurrentMonthUnpaidLeaves(double currentMonthUnpaidLeaves) { this.currentMonthUnpaidLeaves = currentMonthUnpaidLeaves; }
        
        public double getCurrentMonthTotalLeaves() { return currentMonthTotalLeaves; }
        public void setCurrentMonthTotalLeaves(double currentMonthTotalLeaves) { this.currentMonthTotalLeaves = currentMonthTotalLeaves; }
        
        public double getCurrentMonthUsedLeaves() { return currentMonthUsedLeaves; }
        public void setCurrentMonthUsedLeaves(double currentMonthUsedLeaves) { this.currentMonthUsedLeaves = currentMonthUsedLeaves; }
        
        public double getTotalEarnedLeaves() { return totalEarnedLeaves; }
        public void setTotalEarnedLeaves(double totalEarnedLeaves) { this.totalEarnedLeaves = totalEarnedLeaves; }
        
        public double getTotalUsedPaidLeaves() { return totalUsedPaidLeaves; }
        public void setTotalUsedPaidLeaves(double totalUsedPaidLeaves) { this.totalUsedPaidLeaves = totalUsedPaidLeaves; }
        
        public double getRemainingPaidLeaves() { return remainingPaidLeaves; }
        public void setRemainingPaidLeaves(double remainingPaidLeaves) { this.remainingPaidLeaves = remainingPaidLeaves; }
        
        public double getAvailableLeaveBalance() { return availableLeaveBalance; }
        public void setAvailableLeaveBalance(double availableLeaveBalance) { this.availableLeaveBalance = availableLeaveBalance; }
        
        public double getTotalApprovedLeaves() { return totalApprovedLeaves; }
        public void setTotalApprovedLeaves(double totalApprovedLeaves) { this.totalApprovedLeaves = totalApprovedLeaves; }
    }
}
