package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.LeaveDTO;
import com.hrm.hrmsystem.dto.LeaveBalanceDTO;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.service.LeaveService;
import com.hrm.hrmsystem.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
            LeaveDTO result = leaveService.applyLeave(dto);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
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

    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<LeaveBalanceDTO> getLeaveBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.getLeaveBalance(employeeId));
    }

    @GetMapping("/balance")
    public ResponseEntity<LeaveBalanceDTO> getMyLeaveBalance() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        // Find employee by email and return their leave balance
        Employee employee = employeeService.getEmployeeByEmail(email);
        return ResponseEntity.ok(leaveService.getLeaveBalance(employee.getId()));
    }

    @PostMapping("/balance/initialize/{employeeId}")
    public ResponseEntity<LeaveBalanceDTO> initializeLeaveBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.initializeLeaveBalance(employeeId));
    }
}
