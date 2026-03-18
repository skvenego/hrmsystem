package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.PayrollDTO;
import com.hrm.hrmsystem.dto.PayslipDTO;
import com.hrm.hrmsystem.service.PayrollService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@CrossOrigin(origins = "*")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @PostMapping("/generate/{employeeId}")
    public ResponseEntity<PayrollDTO> generatePayroll(
            @PathVariable Long employeeId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        return ResponseEntity.ok(payrollService.generatePayroll(employeeId, month, year));
    }

    @PostMapping("/generate-all")
    public ResponseEntity<String> generatePayrollForAll(
            @RequestParam Integer month,
            @RequestParam Integer year) {
        try {
            int generatedCount = payrollService.generatePayrollForAll(month, year).size();
            return ResponseEntity.ok(generatedCount + " payroll records generated successfully!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error generating payroll: " + e.getMessage());
        }
    }

    @PostMapping("/process/{payrollId}")
    public ResponseEntity<PayrollDTO> processPayroll(@PathVariable Long payrollId) {
        return ResponseEntity.ok(payrollService.processPayroll(payrollId));
    }

    @PostMapping("/pay/{payrollId}")
    public ResponseEntity<PayrollDTO> markAsPaid(@PathVariable Long payrollId) {
        return ResponseEntity.ok(payrollService.markAsPaid(payrollId));
    }

    @PostMapping("/unpay/{payrollId}")
    public ResponseEntity<PayrollDTO> markAsUnpaid(@PathVariable Long payrollId) {
        return ResponseEntity.ok(payrollService.markAsUnpaid(payrollId));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PayrollDTO>> getPayrollByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payrollService.getPayrollByEmployee(employeeId));
    }

    @GetMapping("/month")
    public ResponseEntity<List<PayrollDTO>> getPayrollByMonth(
            @RequestParam Integer month,
            @RequestParam Integer year) {
        return ResponseEntity.ok(payrollService.getPayrollByMonth(month, year));
    }

    @GetMapping("/payslip/{employeeId}")
    public ResponseEntity<PayslipDTO> getPayslip(
            @PathVariable Long employeeId,
            @RequestParam Integer month,
            @RequestParam Integer year) {
        
        // Generate and return payslip with all details
        String monthYear = year + "-" + String.format("%02d", month);
        PayslipDTO payslip = payrollService.generatePayslipForEmployee(employeeId, monthYear);
        return ResponseEntity.ok(payslip);
    }

    @GetMapping
    public ResponseEntity<List<PayrollDTO>> getAllPayroll() {
        return ResponseEntity.ok(payrollService.getAllPayroll());
    }
}