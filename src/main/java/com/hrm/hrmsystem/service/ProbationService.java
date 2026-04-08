package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.*;
import com.hrm.hrmsystem.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProbationService {
    
    private final ProbationPeriodRepository probationRepo;
    private final EmployeeRepository employeeRepo;
    
    public ProbationService(ProbationPeriodRepository probationRepo, EmployeeRepository employeeRepo) {
        this.probationRepo = probationRepo;
        this.employeeRepo = employeeRepo;
    }
    
    /**
     * Create probation period for employee
     */
    @Transactional
    public ProbationPeriod createProbation(Long employeeId, LocalDate startDate, Integer durationMonths) {
        Employee employee = employeeRepo.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        // Check if already exists
        if (probationRepo.existsByEmployeeId(employeeId)) {
            throw new RuntimeException("Probation period already exists for this employee");
        }
        
        ProbationPeriod probation = new ProbationPeriod();
        probation.setEmployee(employee);
        probation.setStartDate(startDate);
        probation.setDurationMonths(durationMonths);
        probation.setStatus(ProbationPeriod.ProbationStatus.PROBATION);
        probation.setPaidLeaveEligible(false);
        probation.setPfPercentage(12.0);
        probation.setAnnualTax(0.0);
        probation.setMonthlyTds(0.0);
        
        return probationRepo.save(probation);
    }
    
    /**
     * Confirm employee (complete probation)
     */
    @Transactional
    public ProbationPeriod confirmProbation(Long employeeId) {
        ProbationPeriod probation = probationRepo.findByEmployeeId(employeeId)
            .orElseThrow(() -> new RuntimeException("Probation period not found"));
        
        probation.setStatus(ProbationPeriod.ProbationStatus.CONFIRMED);
        probation.setPaidLeaveEligible(true);
        probation.setEndDate(LocalDate.now());
        
        return probationRepo.save(probation);
    }
    
    /**
     * Extend probation period
     */
    @Transactional
    public ProbationPeriod extendProbation(Long employeeId, Integer additionalMonths) {
        ProbationPeriod probation = probationRepo.findByEmployeeId(employeeId)
            .orElseThrow(() -> new RuntimeException("Probation period not found"));
        
        probation.setDurationMonths(probation.getDurationMonths() + additionalMonths);
        probation.setStatus(ProbationPeriod.ProbationStatus.EXTENDED);
        
        return probationRepo.save(probation);
    }
    
    /**
     * Check if employee is in probation
     */
    public boolean isInProbation(Long employeeId) {
        return probationRepo.findByEmployeeId(employeeId)
            .map(p -> p.getStatus() == ProbationPeriod.ProbationStatus.PROBATION)
            .orElse(false);
    }
    
    /**
     * Get probation details for employee
     */
    public ProbationPeriod getProbationDetails(Long employeeId) {
        return probationRepo.findByEmployeeId(employeeId)
            .orElse(null);
    }
    
    /**
     * Auto-check and update probation status
     */
    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    @Transactional
    public void autoCheckProbationStatus() {
        List<ProbationPeriod> allProbations = probationRepo.findAll();
        LocalDate today = LocalDate.now();
        
        for (ProbationPeriod probation : allProbations) {
            if (probation.getStatus() == ProbationPeriod.ProbationStatus.PROBATION) {
                if (today.isAfter(probation.getEndDate()) || today.isEqual(probation.getEndDate())) {
                    // Auto-confirm
                    probation.setStatus(ProbationPeriod.ProbationStatus.CONFIRMED);
                    probation.setPaidLeaveEligible(true);
                    System.out.println("✅ Auto-confirmed probation for employee: " + probation.getEmployee().getId());
                }
            }
        }
        
        probationRepo.saveAll(allProbations);
    }
}
