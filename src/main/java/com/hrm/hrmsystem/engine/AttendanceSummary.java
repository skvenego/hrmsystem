package com.hrm.hrmsystem.engine;

/**
 * Attendance Summary DTO - Clean calculation result
 * Used across all services for consistent data
 */
public class AttendanceSummary {
    public double present = 0;
    public double absent = 0;
    public double paidLeave = 0;
    public double unpaidLeave = 0;
    public double totalDays = 0;
    
    public double getTotalWorkingDays() {
        return present + absent;
    }
    
    public double getTotalLeaveDays() {
        return paidLeave + unpaidLeave;
    }
    
    @Override
    public String toString() {
        return String.format("AttendanceSummary{present=%.1f, absent=%.1f, paidLeave=%.1f, unpaidLeave=%.1f}",
                present, absent, paidLeave, unpaidLeave);
    }
}
