package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.model.*;
import com.hrm.hrmsystem.service.PayrollApprovalService;
import com.hrm.hrmsystem.service.PayrollService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payroll/approvals")
@CrossOrigin(origins = "*")
public class PayrollApprovalController {

    private final PayrollApprovalService approvalService;
    private final PayrollService payrollService;

    public PayrollApprovalController(
        PayrollApprovalService approvalService,
        PayrollService payrollService
    ) {
        this.approvalService = approvalService;
        this.payrollService = payrollService;
    }

    /**
     * Get pending approvals for Accountant
     */
    @GetMapping("/pending/accountant")
    public ResponseEntity<?> getPendingApprovalsForAccountant() {
        try {
            List<PayrollApproval> pendingApprovals = approvalService.getPendingApprovalsForRole(
                User.Role.ROLE_ACCOUNTANT
            );

            List<Map<String, Object>> response = pendingApprovals.stream().map(approval -> {
                Payroll payroll = approval.getPayroll();
                Employee employee = payroll.getEmployee();
                
                Map<String, Object> map = new HashMap<>();
                map.put("id", payroll.getId());
                map.put("approvalId", approval.getId());
                map.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
                map.put("employeeId", employee.getId());
                map.put("month", payroll.getMonth());
                map.put("year", payroll.getYear());
                map.put("netSalary", payroll.getNetSalary());
                map.put("grossSalary", payroll.getGrossSalary());
                map.put("basicSalary", payroll.getBasicSalary());
                map.put("hra", payroll.getHra());
                map.put("da", payroll.getDa());
                map.put("providentFund", payroll.getProvidentFund());
                map.put("tax", payroll.getTax());
                map.put("totalDeductions", payroll.getTotalDeductions());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get pending approvals for Director
     */
    @GetMapping("/pending/director")
    public ResponseEntity<?> getPendingApprovalsForDirector() {
        try {
            List<PayrollApproval> pendingApprovals = approvalService.getPendingApprovalsForRole(
                User.Role.ROLE_DIRECTOR
            );

            List<Map<String, Object>> response = pendingApprovals.stream().map(approval -> {
                Payroll payroll = approval.getPayroll();
                Employee employee = payroll.getEmployee();
                
                Map<String, Object> map = new HashMap<>();
                map.put("id", payroll.getId());
                map.put("approvalId", approval.getId());
                map.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
                map.put("employeeId", employee.getId());
                map.put("month", payroll.getMonth());
                map.put("year", payroll.getYear());
                map.put("netSalary", payroll.getNetSalary());
                map.put("accountantApproved", payroll.getAccountantApproved());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Approve by Accountant
     */
    @PostMapping("/{id}/accountant/approve")
    public ResponseEntity<?> approveByAccountant(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> requestBody
    ) {
        try {
            // In real implementation, get actual user from authentication
            // For now, using a dummy user or first accountant
            User currentUser = getCurrentUser(); // You need to implement this
            
            if (currentUser.getRole() != User.Role.ROLE_ACCOUNTANT) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Only Accountant can approve"));
            }

            String comments = requestBody != null ? requestBody.get("comments") : "";
            
            PayrollApproval approval = approvalService.approveByAccountant(id, currentUser, comments);
            
            return ResponseEntity.ok(Map.of(
                "message", "Payroll approved successfully",
                "status", "APPROVED"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Approve by Director
     */
    @PostMapping("/{id}/director/approve")
    public ResponseEntity<?> approveByDirector(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> requestBody
    ) {
        try {
            User currentUser = getCurrentUser(); // You need to implement this
            
            if (currentUser.getRole() != User.Role.ROLE_DIRECTOR) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Only Director can approve"));
            }

            String comments = requestBody != null ? requestBody.get("comments") : "";
            
            PayrollApproval approval = approvalService.approveByDirector(id, currentUser, comments);
            
            return ResponseEntity.ok(Map.of(
                "message", "Payroll approved successfully",
                "status", "FINAL_APPROVED"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reject by Accountant
     */
    @PostMapping("/{id}/accountant/reject")
    public ResponseEntity<?> rejectByAccountant(
        @PathVariable Long id,
        @RequestBody Map<String, String> requestBody
    ) {
        try {
            User currentUser = getCurrentUser();
            
            if (currentUser.getRole() != User.Role.ROLE_ACCOUNTANT) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Only Accountant can reject"));
            }

            String reason = requestBody.get("reason");
            
            PayrollApproval approval = approvalService.rejectApproval(
                id, currentUser, User.Role.ROLE_ACCOUNTANT, reason
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Payroll rejected and sent back for correction",
                "status", "REJECTED"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reject by Director
     */
    @PostMapping("/{id}/director/reject")
    public ResponseEntity<?> rejectByDirector(
        @PathVariable Long id,
        @RequestBody Map<String, String> requestBody
    ) {
        try {
            User currentUser = getCurrentUser();
            
            if (currentUser.getRole() != User.Role.ROLE_DIRECTOR) {
                return ResponseEntity.status(403)
                    .body(Map.of("error", "Only Director can reject"));
            }

            String reason = requestBody.get("reason");
            
            PayrollApproval approval = approvalService.rejectApproval(
                id, currentUser, User.Role.ROLE_DIRECTOR, reason
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Payroll rejected",
                "status", "REJECTED"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get approval status for a specific payroll
     */
    @GetMapping("/{payrollId}/status")
    public ResponseEntity<?> getApprovalStatus(@PathVariable Long payrollId) {
        try {
            // This would need access to payroll repository
            // For now, returning a placeholder
            return ResponseEntity.ok(Map.of(
                "payrollId", payrollId,
                "status", "PENDING",
                "message", "Approval status endpoint"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Helper method to get current authenticated user
     * TODO: Implement proper authentication
     */
    private User getCurrentUser() {
        // This is a placeholder - in production, use @AuthenticationPrincipal
        // or SecurityContextHolder to get actual logged-in user
        
        // For testing purposes, return a dummy user
        User user = new User();
        user.setId(1L);
        user.setUsername("test");
        user.setEmail("test@example.com");
        user.setRole(User.Role.ROLE_ACCOUNTANT); // Change based on testing needs
        
        return user;
    }
}
