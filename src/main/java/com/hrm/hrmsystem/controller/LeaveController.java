package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.LeaveDTO;
import com.hrm.hrmsystem.dto.LeaveBalanceDTO;
import com.hrm.hrmsystem.service.LeaveService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(origins = "*")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping("/apply")
    public ResponseEntity<LeaveDTO> applyLeave(@RequestBody LeaveDTO dto) {
        return ResponseEntity.ok(leaveService.applyLeave(dto));
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

    @PostMapping("/balance/initialize/{employeeId}")
    public ResponseEntity<LeaveBalanceDTO> initializeLeaveBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveService.initializeLeaveBalance(employeeId));
    }
}
