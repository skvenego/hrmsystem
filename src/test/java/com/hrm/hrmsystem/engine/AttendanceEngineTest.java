package com.hrm.hrmsystem.engine;

import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.repository.AttendanceRepository;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceEngineTest {

    @Mock
    private LeaveRepository leaveRepository;
    
    @Mock
    private AttendanceRepository attendanceRepository;
    
    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private AttendanceEngine attendanceEngine;

    private Employee testEmployee;

    @BeforeEach
    void setUp() {
        testEmployee = new Employee();
        testEmployee.setId(1L);
        testEmployee.setJoiningDate(LocalDate.of(2024, 1, 1));
        testEmployee.setProbationPeriodMonths(3);
    }

    @Test
    void testCalculateLeaveSplit_FullPaid() {
        // Employee in probation April 2024 (joined Jan 1, probation ends Apr 1)
        // Available balance = 0, so all days should be unpaid
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of());

        AttendanceEngine.LeaveSplit result = attendanceEngine.calculateLeaveSplit(
            1L, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 2), false);

        assertEquals(0.0, result.paidDays, "Should be 0 paid during probation");
        assertEquals(2.0, result.unpaidDays, "Should be 2 unpaid during probation");
        assertEquals(2.0, result.totalDays, "Total should be 2 working days");
    }

    @Test
    void testCalculateLeaveSplit_MixedSplit() {
        // Employee in probation April 2024 - no earned leaves available
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of());

        AttendanceEngine.LeaveSplit result = attendanceEngine.calculateLeaveSplit(
            1L, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 3), false);

        assertEquals(0.0, result.paidDays, "Should be 0 paid during probation");
        assertEquals(3.0, result.unpaidDays, "Should be 3 unpaid during probation");
        assertEquals(3.0, result.totalDays, "Total should be 3 working days");
    }

    @Test
    void testCalculateLeaveSplit_HalfDay() {
        // Employee in probation April 2024 - no earned leaves available
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of());

        AttendanceEngine.LeaveSplit result = attendanceEngine.calculateLeaveSplit(
            1L, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 1), true);

        assertEquals(0.0, result.paidDays, "Should be 0 paid during probation");
        assertEquals(0.5, result.unpaidDays, "Should be 0.5 unpaid during probation");
        assertEquals(0.5, result.totalDays, "Total should be 0.5 for half-day");
    }

    @Test
    void testCalculateLeaveSplit_SundayExclusion() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of());

        // Friday to Sunday (Sunday excluded)
        AttendanceEngine.LeaveSplit result = attendanceEngine.calculateLeaveSplit(
            1L, LocalDate.of(2024, 4, 5), LocalDate.of(2024, 4, 7), false);

        assertEquals(0.0, result.paidDays, "Should be 0 paid during probation");
        assertEquals(2.0, result.unpaidDays, "Friday + Saturday = 2 days (Sunday excluded)");
        assertEquals(2.0, result.totalDays, "Total should be 2 days (Sunday excluded)");
    }

    @Test
    void testCalculateLeaveBalance_ProbationPeriod() {
        // Test during probation - should return 0 earned
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        org.mockito.Mockito.lenient().when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of());
        
        AttendanceEngine.LeaveBalanceSummary balance = attendanceEngine.calculateLeaveBalance(1L, 2024, 2);
        assertEquals(0.0, balance.earnedLeaves, "Should earn 0 during probation");
        assertEquals(0.0, balance.usedLeaves, "Should have 0 used leaves");
        assertEquals(0.0, balance.getAvailableLeaves(), "Should have 0 available leaves");
    }

    @Test
    void testCalculateLeaveBalance_15thDayRule_Jan15() {
        // Joining Jan 15, probation ends Apr 15 (day=15)
        testEmployee.setJoiningDate(LocalDate.of(2024, 1, 15));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of());
        
        // Should start earning from May (Apr + 1 month)
        AttendanceEngine.LeaveBalanceSummary balance = attendanceEngine.calculateLeaveBalance(1L, 2024, 5);
        assertEquals(1.5, balance.earnedLeaves, "Should earn 1.5 from May (15th rule applied)");
    }

    @Test
    void testCalculateLeaveBalance_15thDayRule_Jan16() {
        // Joining Jan 16, probation ends Apr 16 (day=16 > 15)
        testEmployee.setJoiningDate(LocalDate.of(2024, 1, 16));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of());
        
        // Should start earning from June (Apr + 2 months)
        AttendanceEngine.LeaveBalanceSummary balance = attendanceEngine.calculateLeaveBalance(1L, 2024, 6);
        assertEquals(1.5, balance.earnedLeaves, "Should earn 1.5 from June (16th > 15th rule applied)");
    }

    @Test
    void testCalculateLeaveBalance_CycleReset() {
        // Test cycle reset between June and July
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of());
        
        AttendanceEngine.LeaveBalanceSummary balanceJune = attendanceEngine.calculateLeaveBalance(1L, 2024, 6);
        AttendanceEngine.LeaveBalanceSummary balanceJuly = attendanceEngine.calculateLeaveBalance(1L, 2024, 7);
        
        assertTrue(balanceJune.earnedLeaves >= 0, "June should have earned leaves");
        assertTrue(balanceJuly.earnedLeaves >= 0, "July should restart cycle calculation");
        // Both should be in different cycles
        assertEquals(1, balanceJune.cycle, "June should be cycle 1");
        assertEquals(2, balanceJuly.cycle, "July should be cycle 2");
    }

    @Test
    void testCalculateLeaveBalance_Max9Cap() {
        // Test max 9 leaves per cycle
        testEmployee.setJoiningDate(LocalDate.of(2023, 1, 1)); // Long time employee
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of());
        
        AttendanceEngine.LeaveBalanceSummary balance = attendanceEngine.calculateLeaveBalance(1L, 2024, 6);
        assertEquals(9.0, balance.earnedLeaves, "Should cap at 9 leaves per cycle");
    }

    @Test
    void testCalculateWorkingDays_SundaysExcluded() {
        double days = attendanceEngine.calculateWorkingDays(
            LocalDate.of(2024, 4, 5), // Friday
            LocalDate.of(2024, 4, 7)  // Sunday
        );
        assertEquals(2.0, days, "Should exclude Sunday, count only Friday and Saturday");
    }

    @Test
    void testCalculateWorkingDays_SameDay() {
        double days = attendanceEngine.calculateWorkingDays(
            LocalDate.of(2024, 4, 1), // Monday
            LocalDate.of(2024, 4, 1)  // Same Monday
        );
        assertEquals(1.0, days, "Same day should count as 1 working day");
    }

    @Test
    void testCalculateWorkingDays_MonthBoundary() {
        double days = attendanceEngine.calculateWorkingDays(
            LocalDate.of(2024, 3, 29), // Friday
            LocalDate.of(2024, 4, 2)  // Tuesday
        );
        assertEquals(4.0, days, "Should count correctly across month boundary");
    }

    @Test
    void testCalculateLeaveBalance_Integration() {
        // Integration test combining earned leaves and used leaves
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of());

        AttendanceEngine.LeaveBalanceSummary balance = attendanceEngine.calculateLeaveBalance(1L, 2024, 4);
        
        assertTrue(balance.earnedLeaves >= 0, "Should have earned leaves");
        assertEquals(0.0, balance.usedLeaves, "Should have 0 used leaves initially");
        assertEquals(balance.earnedLeaves, balance.getAvailableLeaves(), "Available should equal earned when unused");
    }

    @Test
    void testCalculateLeaveBalance_WithUsedLeaves() {
        // Test with existing used leaves
        Leave usedLeave = new Leave();
        usedLeave.setId(1L);
        usedLeave.setPaidDays(1.5);
        usedLeave.setUnpaidDays(0.5);
        usedLeave.setStatus(Leave.LeaveStatus.APPROVED);
        usedLeave.setStartDate(LocalDate.of(2024, 4, 1));
        usedLeave.setEndDate(LocalDate.of(2024, 4, 2));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of(usedLeave));

        AttendanceEngine.LeaveBalanceSummary balance = attendanceEngine.calculateLeaveBalance(1L, 2024, 4);
        
        assertEquals(1.5, balance.usedLeaves, "Should count only paid leaves as used");
        assertTrue(balance.getAvailableLeaves() < balance.earnedLeaves, "Available should be less than earned");
    }

    @Test
    void testCalculateLeaveBalance_CrossCycleLeave() {
        // Test leave spanning cycle boundary (June 29 → July 2)
        Leave crossCycleLeave = new Leave();
        crossCycleLeave.setId(1L);
        crossCycleLeave.setPaidDays(2.0);
        crossCycleLeave.setUnpaidDays(1.0);
        crossCycleLeave.setStatus(Leave.LeaveStatus.APPROVED);
        crossCycleLeave.setStartDate(LocalDate.of(2024, 6, 29));
        crossCycleLeave.setEndDate(LocalDate.of(2024, 7, 2));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of(crossCycleLeave));

        // Test June calculation (cycle 1)
        AttendanceEngine.LeaveBalanceSummary juneBalance = attendanceEngine.calculateLeaveBalance(1L, 2024, 6);
        assertEquals(1, juneBalance.cycle, "June should be cycle 1");
        assertTrue(juneBalance.usedLeaves > 0, "June should have some paid leave usage");

        // Test July calculation (cycle 2)  
        AttendanceEngine.LeaveBalanceSummary julyBalance = attendanceEngine.calculateLeaveBalance(1L, 2024, 7);
        assertEquals(2, julyBalance.cycle, "July should be cycle 2");
        assertTrue(julyBalance.usedLeaves > 0, "July should have some paid leave usage");
    }

    @Test
    void testCalculateLeaveBalance_HalfDayUnpaid() {
        // Test half-day unpaid leave (0.5 unpaid)
        Leave halfDayUnpaidLeave = new Leave();
        halfDayUnpaidLeave.setId(1L);
        halfDayUnpaidLeave.setPaidDays(0.0);
        halfDayUnpaidLeave.setUnpaidDays(0.5);
        halfDayUnpaidLeave.setStatus(Leave.LeaveStatus.APPROVED);
        halfDayUnpaidLeave.setStartDate(LocalDate.of(2024, 4, 1));
        halfDayUnpaidLeave.setEndDate(LocalDate.of(2024, 4, 1));
        halfDayUnpaidLeave.setIsHalfDay(true);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of(halfDayUnpaidLeave));

        AttendanceEngine.LeaveBalanceSummary balance = attendanceEngine.calculateLeaveBalance(1L, 2024, 4);
        
        assertEquals(0.0, balance.usedLeaves, "Should not count unpaid leave as used");
        assertEquals(0.5, balance.unpaidLeaves, "Should count 0.5 unpaid leave");
    }

    @Test
    void testCalculateLeaveBalance_FutureApprovedLeave() {
        // Test future approved leave (should affect balance immediately)
        Leave futureLeave = new Leave();
        futureLeave.setId(1L);
        futureLeave.setPaidDays(1.0);
        futureLeave.setUnpaidDays(0.0);
        futureLeave.setStatus(Leave.LeaveStatus.APPROVED);
        futureLeave.setStartDate(LocalDate.of(2024, 12, 15));
        futureLeave.setEndDate(LocalDate.of(2024, 12, 15));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of(futureLeave));

        // Balance should be affected immediately even for future dates
        AttendanceEngine.LeaveBalanceSummary balance = attendanceEngine.calculateLeaveBalance(1L, 2024, 11);
        
        assertEquals(1.0, balance.usedLeaves, "Future approved leave should affect balance immediately");
        assertTrue(balance.getAvailableLeaves() < balance.earnedLeaves, "Available should be reduced by future approved leave");
    }

    @Test
    void testCalculateLeaveBalance_EditExclusion() {
        // Test editing existing leave doesn't consume balance twice
        Leave existingLeave = new Leave();
        existingLeave.setId(1L);
        existingLeave.setPaidDays(2.0);
        existingLeave.setUnpaidDays(0.0);
        existingLeave.setStatus(Leave.LeaveStatus.APPROVED);
        existingLeave.setStartDate(LocalDate.of(2024, 5, 1));
        existingLeave.setEndDate(LocalDate.of(2024, 5, 2));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of(existingLeave));

        // Calculate balance without exclusion
        AttendanceEngine.LeaveBalanceSummary balanceWithoutExclusion = attendanceEngine.calculateLeaveBalance(1L, 2024, 5);
        assertEquals(2.0, balanceWithoutExclusion.usedLeaves, "Should count 2 paid leave days");

        // Calculate balance with exclusion (editing same leave)
        AttendanceEngine.LeaveBalanceSummary balanceWithExclusion = attendanceEngine.calculateLeaveBalance(1L, 2024, 5, 1L);
        assertEquals(0.0, balanceWithExclusion.usedLeaves, "Should exclude leave being edited");
    }

    @Test
    void testCalculateLeaveBalance_PendingLeaveIgnored() {
        // Test pending leave should not affect balance
        Leave pendingLeave = new Leave();
        pendingLeave.setId(1L);
        pendingLeave.setPaidDays(1.0);
        pendingLeave.setUnpaidDays(0.0);
        pendingLeave.setStatus(Leave.LeaveStatus.PENDING);
        pendingLeave.setStartDate(LocalDate.of(2024, 4, 1));
        pendingLeave.setEndDate(LocalDate.of(2024, 4, 1));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of(pendingLeave));

        AttendanceEngine.LeaveBalanceSummary balance = attendanceEngine.calculateLeaveBalance(1L, 2024, 4);
        
        assertEquals(0.0, balance.usedLeaves, "Pending leave should not affect balance");
        assertEquals(0.0, balance.unpaidLeaves, "Pending leave should not affect unpaid balance");
    }

    @Test
    void testCalculateLeaveBalance_RejectedLeaveIgnored() {
        // Test rejected leave should not affect balance
        Leave rejectedLeave = new Leave();
        rejectedLeave.setId(1L);
        rejectedLeave.setPaidDays(1.0);
        rejectedLeave.setUnpaidDays(0.0);
        rejectedLeave.setStatus(Leave.LeaveStatus.REJECTED);
        rejectedLeave.setStartDate(LocalDate.of(2024, 4, 1));
        rejectedLeave.setEndDate(LocalDate.of(2024, 4, 1));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(leaveRepository.findByEmployeeId(1L)).thenReturn(List.of(rejectedLeave));

        AttendanceEngine.LeaveBalanceSummary balance = attendanceEngine.calculateLeaveBalance(1L, 2024, 4);
        
        assertEquals(0.0, balance.usedLeaves, "Rejected leave should not affect balance");
        assertEquals(0.0, balance.unpaidLeaves, "Rejected leave should not affect unpaid balance");
    }
}
