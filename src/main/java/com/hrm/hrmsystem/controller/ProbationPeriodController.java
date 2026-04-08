package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.model.ProbationPeriod;
import com.hrm.hrmsystem.repository.ProbationPeriodRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/probation")
@CrossOrigin(origins = "*")
public class ProbationPeriodController {

    private final ProbationPeriodRepository probationPeriodRepository;

    public ProbationPeriodController(ProbationPeriodRepository probationPeriodRepository) {
        this.probationPeriodRepository = probationPeriodRepository;
    }

    /**
     * Get probation period by employee ID
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getProbationByEmployee(@PathVariable Long employeeId) {
        Optional<ProbationPeriod> probation = probationPeriodRepository.findByEmployeeId(employeeId);
        
        if (probation.isPresent()) {
            return ResponseEntity.ok(probation.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Check if employee is on probation
     */
    @GetMapping("/employee/{employeeId}/status")
    public ResponseEntity<?> checkProbationStatus(@PathVariable Long employeeId) {
        Optional<ProbationPeriod> probation = probationPeriodRepository.findByEmployeeId(employeeId);
        
        if (probation.isPresent()) {
            ProbationPeriod period = probation.get();
            boolean isOnProbation = period.getStatus() == ProbationPeriod.ProbationStatus.PROBATION || 
                                   period.getStatus() == ProbationPeriod.ProbationStatus.EXTENDED;
            
            return ResponseEntity.ok(new ProbationStatusResponse(
                isOnProbation,
                period.getEndDate(),
                period.getPaidLeaveEligible(),
                isOnProbation ? period.getRemainingProbationDays() : 0
            ));
        } else {
            // No probation record - assume confirmed
            return ResponseEntity.ok(new ProbationStatusResponse(false, null, true, 0));
        }
    }

    // Response DTO
    public static class ProbationStatusResponse {
        private boolean isOnProbation;
        private Object endDate;
        private boolean paidLeaveEligible;
        private long remainingDays;

        public ProbationStatusResponse(boolean isOnProbation, Object endDate, 
                                      boolean paidLeaveEligible, long remainingDays) {
            this.isOnProbation = isOnProbation;
            this.endDate = endDate;
            this.paidLeaveEligible = paidLeaveEligible;
            this.remainingDays = remainingDays;
        }

        public boolean isOnProbation() { return isOnProbation; }
        public void setOnProbation(boolean isOnProbation) { this.isOnProbation = isOnProbation; }
        public Object getEndDate() { return endDate; }
        public void setEndDate(Object endDate) { this.endDate = endDate; }
        public boolean isPaidLeaveEligible() { return paidLeaveEligible; }
        public void setPaidLeaveEligible(boolean paidLeaveEligible) { this.paidLeaveEligible = paidLeaveEligible; }
        public long getRemainingDays() { return remainingDays; }
        public void setRemainingDays(long remainingDays) { this.remainingDays = remainingDays; }
    }
}
