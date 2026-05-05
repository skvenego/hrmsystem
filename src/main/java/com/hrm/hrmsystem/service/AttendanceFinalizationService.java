package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.AttendanceFinalization;
import com.hrm.hrmsystem.repository.AttendanceFinalizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AttendanceFinalizationService {

    @Autowired
    private AttendanceFinalizationRepository attendanceFinalizationRepository;

    /**
     * Check if attendance is finalized for a specific date range
     */
    public boolean isAttendanceFinalized(LocalDate date) {
        List<AttendanceFinalization> finalizations = attendanceFinalizationRepository.findByIsFinalizedTrue();
        for (AttendanceFinalization finalization : finalizations) {
            if (!date.isBefore(finalization.getStartDate()) && !date.isAfter(finalization.getEndDate())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finalize attendance for a date range
     */
    public AttendanceFinalization finalizeAttendance(LocalDate startDate, LocalDate endDate, String remarks) {
        // Check if any date in the range is already finalized
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (isAttendanceFinalized(current)) {
                throw new RuntimeException("Attendance for " + current + " is already finalized");
            }
            current = current.plusDays(1);
        }

        String finalizedBy = getCurrentUser();
        AttendanceFinalization finalization = new AttendanceFinalization(startDate, endDate, finalizedBy, remarks);
        return attendanceFinalizationRepository.save(finalization);
    }

    /**
     * Unfinalize attendance for a date range
     */
    public AttendanceFinalization unfinalizeAttendance(LocalDate startDate, LocalDate endDate, String remarks) {
        Optional<AttendanceFinalization> finalizationOpt = attendanceFinalizationRepository.findByStartDateAndEndDate(startDate, endDate);
        
        if (finalizationOpt.isEmpty()) {
            throw new RuntimeException("No finalization found for date range " + startDate + " to " + endDate);
        }

        AttendanceFinalization finalization = finalizationOpt.get();
        finalization.setIsFinalized(false);
        if (remarks != null && !remarks.trim().isEmpty()) {
            finalization.setRemarks(remarks);
        }
        
        return attendanceFinalizationRepository.save(finalization);
    }

    /**
     * Get attendance finalization details for a specific date range
     */
    public Optional<AttendanceFinalization> getAttendanceFinalization(LocalDate startDate, LocalDate endDate) {
        return attendanceFinalizationRepository.findByStartDateAndEndDate(startDate, endDate);
    }

    /**
     * Get all finalized attendance periods
     */
    public List<AttendanceFinalization> getAllFinalizedPeriods() {
        return attendanceFinalizationRepository.findByIsFinalizedTrue();
    }

    /**
     * Get current logged-in user
     */
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "SYSTEM";
    }
}
