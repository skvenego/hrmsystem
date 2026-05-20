package com.hrm.hrmsystem;

import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.repository.LeaveRepository;
import com.hrm.hrmsystem.engine.AttendanceEngine;
import com.hrm.hrmsystem.service.PayrollLockService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Utility to fix incorrect leave splits for all employees
 */
@Component
public class LeaveCorrectionRunner implements CommandLineRunner {
    private final LeaveRepository leaveRepository;
    private final AttendanceEngine attendanceEngine;
    private final PayrollLockService payrollLockService;

    public LeaveCorrectionRunner(LeaveRepository leaveRepository, AttendanceEngine attendanceEngine, PayrollLockService payrollLockService) {
        this.leaveRepository = leaveRepository;
        this.attendanceEngine = attendanceEngine;
        this.payrollLockService = payrollLockService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Run only if a specific flag is passed or on every start if needed
        // For now, let's run it once to fix data.
        System.out.println("--- LEAVE CORRECTION START ---");
        
        List<Leave> approvedLeaves = leaveRepository.findAll().stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
                .toList();

        for (Leave leave : approvedLeaves) {
            int month = leave.getStartDate().getMonthValue();
            int year = leave.getStartDate().getYear();

            // Skip if payroll is locked
            if (payrollLockService.isPayrollLocked(month, year)) {
                System.out.println("Skipping Leave ID " + leave.getId() + " - Payroll locked for " + year + "-" + month);
                continue;
            }

            // Recalculate split
            AttendanceEngine.LeaveSplit split = attendanceEngine.calculateLeaveSplit(
                leave.getEmployee().getId(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getIsHalfDay() != null && leave.getIsHalfDay()
            );

            System.out.println("Recalculating Leave ID " + leave.getId() + " (" + leave.getEmployee().getFirstName() + "): " +
                "Old: P=" + leave.getFinalPaidDays() + "/U=" + leave.getFinalUnpaidDays() + 
                " -> New: P=" + split.paidDays + "/U=" + split.unpaidDays);

            // Update and freeze
            leave.setPaidDays(split.paidDays);
            leave.setUnpaidDays(split.unpaidDays);
            leave.setTotalDays(split.totalDays);
            leave.setFinalPaidDays(split.paidDays);
            leave.setFinalUnpaidDays(split.unpaidDays);
            leave.setFinalTotalDays(split.totalDays);
            
            leaveRepository.save(leave);
        }
        
        System.out.println("--- LEAVE CORRECTION END ---");
    }
}
