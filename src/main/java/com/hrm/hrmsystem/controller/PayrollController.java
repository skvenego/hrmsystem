package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.PayrollDTO;
import com.hrm.hrmsystem.service.PayrollService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payroll")
@CrossOrigin(origins = "*")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    // Get payrolls by month and year (query params) - used by frontend
    @GetMapping
    public ResponseEntity<List<PayrollDTO>> getPayrollByMonthYear(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        if (month != null && year != null) {
            return ResponseEntity.ok(payrollService.getPayrollByMonth(month, year));
        }
        List<PayrollDTO> payrolls = payrollService.getAllPayrolls();
        return ResponseEntity.ok(payrolls);
    }

    // Get all payrolls
    @GetMapping("/list")
    public ResponseEntity<List<PayrollDTO>> getAllPayrolls() {
        List<PayrollDTO> payrolls = payrollService.getAllPayrolls();
        return ResponseEntity.ok(payrolls);
    }

    // Generate payroll for one employee (POST with JSON body)
    @PostMapping("/generate")
    public ResponseEntity<PayrollDTO> generatePayroll(@RequestBody Map<String, Object> request) {
        Long employeeId = Long.parseLong(request.get("employeeId").toString());
        Integer month = Integer.parseInt(request.get("month").toString());
        Integer year = Integer.parseInt(request.get("year").toString());
        
        PayrollDTO payroll = payrollService.generatePayroll(employeeId, month, year);
        return ResponseEntity.ok(payroll);
    }

    // Generate payroll for all employees (POST with JSON body)
    @PostMapping("/generate-all")
    public ResponseEntity<List<PayrollDTO>> generatePayrollForAll(@RequestBody Map<String, Object> request) {
        Integer month = Integer.parseInt(request.get("month").toString());
        Integer year = Integer.parseInt(request.get("year").toString());
        
        List<PayrollDTO> payrolls = payrollService.generatePayrollForAll(month, year);
        return ResponseEntity.ok(payrolls);
    }

    // Process payroll
    @PostMapping("/process/{payrollId}")
    public ResponseEntity<PayrollDTO> processPayroll(@PathVariable Long payrollId) {
        PayrollDTO payroll = payrollService.processPayroll(payrollId);
        return ResponseEntity.ok(payroll);
    }

    // Mark as paid
    @PostMapping("/pay/{payrollId}")
    public ResponseEntity<PayrollDTO> markAsPaid(@PathVariable Long payrollId) {
        PayrollDTO payroll = payrollService.markAsPaid(payrollId);
        return ResponseEntity.ok(payroll);
    }

    // Mark as unpaid
    @PostMapping("/unpay/{payrollId}")
    public ResponseEntity<PayrollDTO> markAsUnpaid(@PathVariable Long payrollId) {
        PayrollDTO payroll = payrollService.markAsUnpaid(payrollId);
        return ResponseEntity.ok(payroll);
    }
}
