package com.hrm.hrmsystem;

import com.hrm.hrmsystem.engine.AttendanceEngine;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.YearMonth;
import java.util.List;

@SpringBootTest
public class DebugDataTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private AttendanceEngine attendanceEngine;

    @Test
    public void printAnubhavData() {
        System.out.println("=== DEBUG DATA FOR ANUBHAV ===");
        List<Employee> allEmployees = employeeRepository.findAll();
        Employee anubhav = null;
        for (Employee e : allEmployees) {
            if (e.getFirstName().toLowerCase().contains("anubhav") || 
                e.getLastName().toLowerCase().contains("anubhav")) {
                anubhav = e;
                break;
            }
        }

        if (anubhav == null) {
            System.out.println("ANUBHAV NOT FOUND");
            return;
        }

        System.out.println("Found: " + anubhav.getFirstName() + " " + anubhav.getLastName() + ", ID: " + anubhav.getId());
        System.out.println("Joining Date: " + anubhav.getJoiningDate());

        List<Leave> leaves = leaveRepository.findByEmployeeId(anubhav.getId());
        System.out.println("\nAll Leaves:");
        for (Leave l : leaves) {
            System.out.println("Leave ID: " + l.getId() + ", Status: " + l.getStatus() + 
                               ", Dates: " + l.getStartDate() + " to " + l.getEndDate() + 
                               ", TotalDays: " + l.getTotalDays() + 
                               ", PaidDays: " + l.getPaidDays() + 
                               ", UnpaidDays: " + l.getUnpaidDays());
        }

        // Test cycle 1 (Jan-June)
        System.out.println("\n=== CYCLE 1 (May 2026) ===");
        YearMonth ym = YearMonth.of(2026, 5);
        AttendanceEngine.LeaveBalanceSummary summary = attendanceEngine.calculateLeaveBalance(anubhav.getId(), ym);
        System.out.println("Earned Leaves: " + summary.earnedLeaves);
        System.out.println("used (Paid): " + summary.usedLeaves);
        System.out.println("Unpaid Leaves: " + summary.unpaidLeaves);
        System.out.println("Remaining Balance: " + summary.remaining);
        
        System.out.println("\n=== CYCLE 1 (June 2026) ===");
        YearMonth ym2 = YearMonth.of(2026, 6);
        AttendanceEngine.LeaveBalanceSummary summary2 = attendanceEngine.calculateLeaveBalance(anubhav.getId(), ym2);
        System.out.println("Earned Leaves: " + summary2.earnedLeaves);
        System.out.println("used (Paid): " + summary2.usedLeaves);
        System.out.println("Unpaid Leaves: " + summary2.unpaidLeaves);
        System.out.println("Remaining Balance: " + summary2.remaining);

        System.out.println("=== END DEBUG DATA ===");
    }
}
