package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.model.SalaryAdjustment;
import com.hrm.hrmsystem.service.SalaryAdjustmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/salary-adjustments")
public class SalaryAdjustmentController {

    @Autowired
    private SalaryAdjustmentService salaryAdjustmentService;

    @PostMapping("/create")
    public ResponseEntity<SalaryAdjustment> createAdjustment(
            @RequestParam Long employeeId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam BigDecimal amount,
            @RequestParam String reason,
            @RequestParam String adjustmentType,
            @RequestParam(required = false) String remarks) {
        try {
            SalaryAdjustment adjustment = salaryAdjustmentService.createAdjustment(
                    employeeId, month, year, amount, reason, adjustmentType, remarks);
            return ResponseEntity.ok(adjustment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/employee/{employeeId}/{month}/{year}")
    public ResponseEntity<List<SalaryAdjustment>> getAdjustmentsForEmployee(
            @PathVariable Long employeeId,
            @PathVariable Integer month,
            @PathVariable Integer year) {
        List<SalaryAdjustment> adjustments = salaryAdjustmentService.getAdjustmentsForEmployee(employeeId, month, year);
        return ResponseEntity.ok(adjustments);
    }

    @GetMapping("/month/{month}/{year}")
    public ResponseEntity<List<SalaryAdjustment>> getAdjustmentsForMonth(
            @PathVariable Integer month,
            @PathVariable Integer year) {
        List<SalaryAdjustment> adjustments = salaryAdjustmentService.getAdjustmentsForMonth(month, year);
        return ResponseEntity.ok(adjustments);
    }

    @GetMapping("/unapplied")
    public ResponseEntity<List<SalaryAdjustment>> getUnappliedAdjustments() {
        List<SalaryAdjustment> adjustments = salaryAdjustmentService.getUnappliedAdjustments();
        return ResponseEntity.ok(adjustments);
    }

    @PutMapping("/{adjustmentId}/apply")
    public ResponseEntity<SalaryAdjustment> markAsApplied(@PathVariable Long adjustmentId) {
        try {
            SalaryAdjustment adjustment = salaryAdjustmentService.markAsApplied(adjustmentId);
            return ResponseEntity.ok(adjustment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{adjustmentId}")
    public ResponseEntity<Void> deleteAdjustment(@PathVariable Long adjustmentId) {
        try {
            salaryAdjustmentService.deleteAdjustment(adjustmentId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/total/{employeeId}/{month}/{year}")
    public ResponseEntity<BigDecimal> getTotalAdjustmentAmount(
            @PathVariable Long employeeId,
            @PathVariable Integer month,
            @PathVariable Integer year) {
        BigDecimal total = salaryAdjustmentService.getTotalAdjustmentAmount(employeeId, month, year);
        return ResponseEntity.ok(total);
    }
}
