package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.service.LeaveCalculationService;
import com.hrm.hrmsystem.service.LeaveCalculationService.LeaveStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for centralized leave calculations
 * Single endpoint for all leave statistics - used by all pages
 */
@RestController
@RequestMapping("/api/leaves/calculation")
@RequiredArgsConstructor
@Slf4j
public class LeaveCalculationController {

    private final LeaveCalculationService leaveCalculationService;

    /**
     * Get complete leave statistics for an employee
     * Used by: leave.html, payslips.html, my-leaves.html, payroll.js
     * 
     * @param employeeId Employee ID
     * @param month Month (1-12), defaults to current month
     * @param year Year, defaults to current year
     * @return LeaveStatistics with all calculated data
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getEmployeeLeaveStatistics(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        try {
            // Default to current month/year if not provided
            LocalDate now = LocalDate.now();
            int targetMonth = month != null ? month : now.getMonthValue();
            int targetYear = year != null ? year : now.getYear();
            
            // Validate month and year
            if (targetMonth < 1 || targetMonth > 12) {
                return ResponseEntity.badRequest().body("Invalid month: must be between 1 and 12");
            }
            if (targetYear < 2000 || targetYear > 2100) {
                return ResponseEntity.badRequest().body("Invalid year: must be between 2000 and 2100");
            }
            
            log.info("Fetching leave statistics for employee {}: month={}, year={}", employeeId, targetMonth, targetYear);
            
            LeaveStatistics stats = leaveCalculationService.calculateLeaveStatistics(employeeId, targetMonth, targetYear);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            log.error("Error fetching leave statistics for employee {}: {}", employeeId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error fetching leave statistics for employee {}: {}", employeeId, e.getMessage());
            return ResponseEntity.internalServerError().body("Internal server error: " + e.getMessage());
        }
    }

    /**
     * Get current month leave statistics for an employee
     * Shortcut endpoint for current month
     */
    @GetMapping("/employee/{employeeId}/current")
    public ResponseEntity<?> getCurrentMonthStatistics(@PathVariable Long employeeId) {
        LocalDate now = LocalDate.now();
        return getEmployeeLeaveStatistics(employeeId, now.getMonthValue(), now.getYear());
    }
}
