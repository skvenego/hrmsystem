package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

    List<Leave> findByEmployeeId(Long employeeId);

    List<Leave> findByStatus(Leave.LeaveStatus status);

    List<Leave> findByEmployeeIdAndStatus(Long employeeId, Leave.LeaveStatus status);

    List<Leave> findByEmployeeIdAndStatusAndStartDateBetween(
            Long employeeId, Leave.LeaveStatus status, LocalDate startDate, LocalDate endDate);

    /**
     * Get all approved leaves for employee within a date range (for accurate monthly calculation)
     * Includes leaves that overlap with the range
     */
    default double sumApprovedPaidLeavesBetween(Long employeeId, LocalDate start, LocalDate end) {
        List<Leave> leaves = findByEmployeeIdAndStatus(employeeId, Leave.LeaveStatus.APPROVED);
        return leaves.stream()
            .filter(l -> l.getLeaveType() != Leave.LeaveType.UNPAID) // Only paid leaves
            .filter(l -> !l.getEndDate().isBefore(start) && !l.getStartDate().isAfter(end)) // Overlaps with range
            .mapToDouble(l -> {
                // Calculate overlap days
                LocalDate overlapStart = l.getStartDate().isBefore(start) ? start : l.getStartDate();
                LocalDate overlapEnd = l.getEndDate().isAfter(end) ? end : l.getEndDate();
                long days = ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
                // Handle half-day: total days would be 0.5 for single day half-day leave
                return l.getTotalDays() < 1.0 ? l.getTotalDays() : days;
            })
            .sum();
    }

    /**
     * Get all approved paid leaves up to a specific date (for opening balance calculation)
     */
    default double sumApprovedPaidLeavesTill(Long employeeId, LocalDate tillDate) {
        List<Leave> leaves = findByEmployeeIdAndStatus(employeeId, Leave.LeaveStatus.APPROVED);
        return leaves.stream()
            .filter(l -> l.getLeaveType() != Leave.LeaveType.UNPAID)
            .filter(l -> !l.getStartDate().isAfter(tillDate))
            .mapToDouble(l -> {
                LocalDate overlapEnd = l.getEndDate().isAfter(tillDate) ? tillDate : l.getEndDate();
                long days = ChronoUnit.DAYS.between(l.getStartDate(), overlapEnd) + 1;
                // For leaves partially after tillDate, use the overlapping portion
                if (l.getEndDate().isAfter(tillDate)) {
                    return days;
                }
                return l.getTotalDays();
            })
            .sum();
    }
}