package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.model.PayrollLock;
import com.hrm.hrmsystem.service.PayrollLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/payroll-lock")
public class PayrollLockController {

    @Autowired
    private PayrollLockService payrollLockService;

    @GetMapping("/check/{month}/{year}")
    public ResponseEntity<Boolean> checkPayrollLock(@PathVariable Integer month, @PathVariable Integer year) {
        boolean isLocked = payrollLockService.isPayrollLocked(month, year);
        return ResponseEntity.ok(isLocked);
    }

    @GetMapping("/{month}/{year}")
    public ResponseEntity<PayrollLock> getPayrollLock(@PathVariable Integer month, @PathVariable Integer year) {
        Optional<PayrollLock> lock = payrollLockService.getPayrollLock(month, year);
        return lock.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/lock/{month}/{year}")
    public ResponseEntity<PayrollLock> lockPayroll(
            @PathVariable Integer month,
            @PathVariable Integer year,
            @RequestParam(required = false) String remarks) {
        try {
            PayrollLock lock = payrollLockService.lockPayroll(month, year, remarks);
            return ResponseEntity.ok(lock);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/unlock/{month}/{year}")
    public ResponseEntity<PayrollLock> unlockPayroll(
            @PathVariable Integer month,
            @PathVariable Integer year,
            @RequestParam(required = false) String remarks) {
        try {
            PayrollLock lock = payrollLockService.unlockPayroll(month, year, remarks);
            return ResponseEntity.ok(lock);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
