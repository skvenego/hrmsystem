package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.LeaveBalanceDTO;
import com.hrm.hrmsystem.engine.AttendanceEngine;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.User;
import com.hrm.hrmsystem.repository.UserRepository;
import com.hrm.hrmsystem.service.UnifiedCalculationService;
import com.hrm.hrmsystem.service.UnifiedCalculationService.LeaveStatistics;
import com.hrm.hrmsystem.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * REST Controller for centralized leave calculations
 * Single endpoint for all leave statistics - used by all pages
 */
@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
@Slf4j
public class LeaveCalculationController {

    private final UnifiedCalculationService unifiedCalculationService;
    private final AttendanceEngine attendanceEngine;
    private final UserRepository userRepository;
    private final LeaveService leaveService;

    /**
     * Get complete leave statistics for an employee
     * Used by: leave.html, payslips.html, my-leaves.html, payroll.js
     * 
     * @param employeeId Employee ID
     * @param month Month (1-12), defaults to current month
     * @param year Year, defaults to current year
     * @return LeaveStatistics with all calculated data
     */
    @GetMapping("/calculation/employee/{employeeId}")
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
            
            LeaveStatistics stats = unifiedCalculationService.calculateLeaveStatistics(employeeId, targetMonth, targetYear);
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
    @GetMapping("/calculation/employee/{employeeId}/current")
    public ResponseEntity<?> getCurrentMonthStatistics(@PathVariable Long employeeId) {
        LocalDate now = LocalDate.now();
        return getEmployeeLeaveStatistics(employeeId, now.getMonthValue(), now.getYear());
    }

    // ===== MISSING API ENDPOINTS FOR FRONTEND =====

    /**
     * Get my leave balance - fixed API
     * Used by: Dashboard and Leave History pages
     * Endpoint: /api/leaves/balance-fixed/my
     */
    @GetMapping("/balance-fixed/my")
    public ResponseEntity<?> getMyLeaveBalanceFixed() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            var userOpt = userRepository.findByUsername(email);
            if (userOpt.isEmpty() || userOpt.get().getEmployee() == null) {
                return ResponseEntity.status(404).body("Employee not found");
            }

            Employee employee = userOpt.get().getEmployee();
            
            // Delegate to LeaveService (single source of truth for DTO building)
            LeaveBalanceDTO balance = leaveService.getLeaveBalance(employee.getId());
            return ResponseEntity.ok(balance);
            
        } catch (Exception e) {
            log.error("Error in leave balance fixed API", e);
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }

    /**
     * Get my leaves
     * Used by: My Leaves page
     * Endpoint: /api/leaves/my
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyLeaves() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            var userOpt = userRepository.findByUsername(email);
            if (userOpt.isEmpty() || userOpt.get().getEmployee() == null) {
                return ResponseEntity.status(404).body("Employee not found");
            }

            Employee employee = userOpt.get().getEmployee();
            Long employeeId = employee.getId();
            
            // Get all leaves for the employee using LeaveService
            var leaves = leaveService.getLeavesByEmployee(employeeId);
            
            return ResponseEntity.ok(leaves);
            
        } catch (Exception e) {
            log.error("Error in get my leaves API", e);
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }

    /**
     * Get all leaves
     * Used by: Management Dashboard, Leave Management pages
     * Endpoint: /api/leaves
     */
    @GetMapping
    public ResponseEntity<?> getAllLeaves() {
        try {
            return ResponseEntity.ok(leaveService.getAllLeaves());
        } catch (Exception e) {
            log.error("Error fetching all leaves", e);
            return ResponseEntity.status(500).body("Internal server error: " + e.getMessage());
        }
    }

    /**
     * Get leave details by ID
     * Endpoint: /api/leaves/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getLeaveById(@PathVariable Long id) {
        try {
            var leave = leaveService.getLeaveById(id);
            return ResponseEntity.ok(leave);
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Leave not found: " + e.getMessage());
        }
    }

    /**
     * Get leave balance of a specific employee (Admin view)
     * Endpoint: /api/leaves/balance/{employeeId}
     */
    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<?> getEmployeeLeaveBalance(@PathVariable Long employeeId) {
        try {
            LeaveBalanceDTO balance = leaveService.getLeaveBalance(employeeId);
            return ResponseEntity.ok(balance);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Apply for leave
     * Used by: Apply Leave modal
     * Endpoint: /api/leaves
     */
    @PostMapping
    public ResponseEntity<?> applyLeave(@RequestBody com.hrm.hrmsystem.dto.LeaveDTO leaveDTO) {
        try {
            return ResponseEntity.ok(leaveService.applyLeave(leaveDTO));
        } catch (Exception e) {
            log.error("Error applying for leave", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Alias for applyLeave to match frontend
     * Endpoint: /api/leaves/apply
     */
    @PostMapping("/apply")
    public ResponseEntity<?> applyLeaveAlias(@RequestBody com.hrm.hrmsystem.dto.LeaveDTO leaveDTO) {
        return applyLeave(leaveDTO);
    }

    /**
     * Approve leave
     * Endpoint: /api/leaves/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveLeave(@PathVariable Long id, @RequestBody java.util.Map<String, String> request) {
        try {
            String approvedBy = request != null ? request.getOrDefault("approvedBy", "Admin") : "Admin";
            return ResponseEntity.ok(leaveService.approveLeave(id, approvedBy));
        } catch (Exception e) {
            log.error("Error approving leave", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Alias for approveLeave to match frontend (supports POST and query params)
     * Endpoint: /api/leaves/approve/{id}
     */
    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approveLeaveAlias(
            @PathVariable Long id, 
            @RequestParam(required = false) String approvedBy) {
        try {
            return ResponseEntity.ok(leaveService.approveLeave(id, approvedBy != null ? approvedBy : "Admin"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectLeave(@PathVariable Long id, @RequestBody java.util.Map<String, String> request) {
        try {
            String reason = request != null ? request.getOrDefault("reason", "Rejected by Admin") : "Rejected by Admin";
            return ResponseEntity.ok(leaveService.rejectLeave(id, reason));
        } catch (Exception e) {
            log.error("Error rejecting leave", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Alias for rejectLeave to match frontend (supports POST and query params)
     * Endpoint: /api/leaves/reject/{id}
     */
    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectLeaveAlias(
            @PathVariable Long id, 
            @RequestParam(required = false, name = "rejectionReason") String reason) {
        try {
            return ResponseEntity.ok(leaveService.rejectLeave(id, reason != null ? reason : "Rejected by Admin"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelLeave(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> request) {
        try {
            String reason = request != null ? request.getOrDefault("reason", "Cancelled") : "Cancelled";
            return ResponseEntity.ok(leaveService.cancelLeave(id, reason));
        } catch (Exception e) {
            log.error("Error cancelling leave", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Alias for cancelLeave to match frontend
     * Endpoint: /api/leaves/cancel/{id}
     */
    @PostMapping("/cancel/{id}")
    public ResponseEntity<?> cancelLeaveAlias(
            @PathVariable Long id, 
            @RequestParam(required = false) String reason) {
        try {
            return ResponseEntity.ok(leaveService.cancelLeave(id, reason != null ? reason : "Cancelled"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Modify approved leave
     * Endpoint: /api/leaves/modify/{id}
     */
    @PostMapping("/modify/{id}")
    public ResponseEntity<?> modifyLeave(
            @PathVariable Long id,
            @RequestBody com.hrm.hrmsystem.dto.LeaveDTO leaveDTO,
            @RequestParam(required = false) String reason) {
        try {
            return ResponseEntity.ok(leaveService.modifyLeave(id, leaveDTO, reason != null ? reason : "Leave modified"));
        } catch (Exception e) {
            log.error("Error modifying leave", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get my leave balance - legacy endpoint
     * Used by: Frontend pages calling old endpoint
     * Endpoint: /api/leaves/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<?> getMyLeaveBalance() {
        // Alias for fixed endpoint
        return getMyLeaveBalanceFixed();
    }
}
