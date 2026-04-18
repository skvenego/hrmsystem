package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.LeaveDTO;
import com.hrm.hrmsystem.dto.LeaveBalanceDTO;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.service.LeaveService;
import com.hrm.hrmsystem.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = "*")
public class LeaveController {

    private final LeaveService leaveService;
    private final EmployeeService employeeService;

    public LeaveController(LeaveService leaveService, EmployeeService employeeService) {
        this.leaveService = leaveService;
        this.employeeService = employeeService;
    }

    @PostMapping("/apply")
    public ResponseEntity<?> applyLeave(@RequestBody LeaveDTO dto) {
        try {
            // Log incoming request
            System.out.println("=== LEAVE APPLY REQUEST ===");
            System.out.println("LeaveType: " + dto.getLeaveType());
            System.out.println("StartDate: " + dto.getStartDate());
            System.out.println("EndDate: " + dto.getEndDate());
            System.out.println("TotalDays: " + dto.getTotalDays());
            System.out.println("IsHalfDay: " + dto.getIsHalfDay());
            System.out.println("Reason: " + dto.getReason());
            System.out.println("==========================");

            // Validate required fields
            if (dto.getLeaveType() == null || dto.getStartDate() == null ||
                dto.getEndDate() == null || dto.getReason() == null ||
                dto.getReason().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "All fields are required: leaveType, startDate, endDate, reason");
                System.out.println("VALIDATION FAILED: Missing required fields");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Get authenticated user's employee ID
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();
            
            // Check if user has admin role
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_HR"));
            
            // Try to get employee - admin may not have an employee record
            Employee employee = null;
            try {
                employee = employeeService.getEmployeeByEmail(email);
            } catch (RuntimeException e) {
                // Employee not found - only error if not an admin
                if (!isAdmin) {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Employee not found for authenticated user");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
                }
            }
            
            // If admin applying on behalf of employee, use the DTO's employeeId
            // Otherwise, use the authenticated employee's ID
            if (employee != null) {
                dto.setEmployeeId(employee.getId());
            } else if (dto.getEmployeeId() == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Employee ID is required for admin users");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Handle HALF leave type
            if ("HALF".equals(dto.getLeaveType()) || Boolean.TRUE.equals(dto.getIsHalfDay())) {
                dto.setTotalDays(0.5);
            }
            
            // Validate totalDays is positive
            if (dto.getTotalDays() == null || dto.getTotalDays() <= 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid total days. Must be greater than 0. Got: " + dto.getTotalDays());
                System.out.println("VALIDATION FAILED: totalDays=" + dto.getTotalDays());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            System.out.println("VALIDATION PASSED: totalDays=" + dto.getTotalDays());
            
            LeaveDTO result = leaveService.applyLeave(dto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            System.err.println("Runtime error applying leave: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            System.err.println("Error applying leave: " + e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to apply leave: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/approve/{leaveId}")
    public ResponseEntity<LeaveDTO> approveLeave(
            @PathVariable Long leaveId,
            @RequestParam String approvedBy) {
        return ResponseEntity.ok(leaveService.approveLeave(leaveId, approvedBy));
    }

    @PostMapping("/reject/{leaveId}")
    public ResponseEntity<LeaveDTO> rejectLeave(
            @PathVariable Long leaveId,
            @RequestParam String rejectionReason) {
        return ResponseEntity.ok(leaveService.rejectLeave(leaveId, rejectionReason));
    }

    @PostMapping("/cancel/{leaveId}")
    public ResponseEntity<LeaveDTO> cancelLeave(@PathVariable Long leaveId) {
        return ResponseEntity.ok(leaveService.cancelLeave(leaveId));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveDTO>> getLeavesByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.getLeavesByEmployee(employeeId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LeaveDTO>> getPendingLeaves() {
        return ResponseEntity.ok(leaveService.getPendingLeaves());
    }

    @GetMapping
    public ResponseEntity<List<LeaveDTO>> getAllLeaves() {
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveDTO> getLeaveById(@PathVariable Long id) {
        return leaveService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<LeaveBalanceDTO> getLeaveBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.getLeaveBalance(employeeId));
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getMyLeaveBalance() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        // Find employee by email and return their leave balance
        Employee employee = employeeService.getEmployeeByEmail(email);
        if (employee == null) {
            return ResponseEntity.ok(new LeaveBalanceDTO()); // Return empty balance
        }
        return ResponseEntity.ok(leaveService.getLeaveBalance(employee.getId()));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyLeaves() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        // Find employee by email and return only their leaves
        Employee employee = employeeService.getEmployeeByEmail(email);
        if (employee == null) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        return ResponseEntity.ok(leaveService.getLeavesByEmployee(employee.getId()));
    }

    @PostMapping("/balance/initialize/{employeeId}")
    public ResponseEntity<LeaveBalanceDTO> initializeLeaveBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.initializeLeaveBalance(employeeId));
    }
}
