package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.PayrollDTO;
import com.hrm.hrmsystem.dto.PayslipDTO;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Payroll;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.PayrollRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final PayslipService payslipService;

    public PayrollService(PayrollRepository payrollRepository,
                          EmployeeRepository employeeRepository,
                          PayslipService payslipService) {
        this.payrollRepository = payrollRepository;
        this.employeeRepository = employeeRepository;
        this.payslipService = payslipService;
    }

    /**
     * Generate payroll for single employee with attendance-based calculation
     */
    @Transactional
    public PayrollDTO generatePayroll(Long employeeId, Integer month, Integer year) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // ✅ Prevent duplicate
        Optional<Payroll> existing = payrollRepository
                .findByEmployeeIdAndMonthAndYear(employeeId, month, year);
        // Always recalculate payroll for the month/year because attendance/leave can change.
        // If payroll exists, we update it; otherwise we create it.
        Payroll payroll = existing.orElseGet(Payroll::new);
        Payroll.PayrollStatus statusToUse = existing.map(Payroll::getStatus).orElse(Payroll.PayrollStatus.PENDING);
        java.time.LocalDate paymentDateToUse = existing.map(Payroll::getPaymentDate).orElse(null);

        // Get attendance data for this month/year
        int presentDays = payslipService.getPresentDaysForEmployee(employeeId, month, year);
        int absentDays = payslipService.getAbsentDaysForEmployee(employeeId, month, year);
        int leaveDays = payslipService.getLeaveDaysForEmployee(employeeId, month, year);
        int halfDays = payslipService.getHalfDaysForEmployee(employeeId, month, year);

        log.info("Attendance for Employee {}: Present={}, Absent={}, Leave={}, HalfDays={}",
                 employeeId, presentDays, absentDays, leaveDays, halfDays);

        BigDecimal basicSalary = employee.getSalary() != null
                ? employee.getSalary()
                : BigDecimal.ZERO;

        // Per-day salary for deduction (26 working days standard)
        BigDecimal perDayBasic = basicSalary.compareTo(BigDecimal.ZERO) > 0
                ? basicSalary.divide(new BigDecimal("26"), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        // Deductible days: max(0, absent + leave + halfDay*0.5 - 1.5) - first 1.5 days free
        double deductibleDays = payslipService.getDeductibleDays(absentDays, leaveDays, halfDays);
        BigDecimal absentLeaveDeduction = perDayBasic.multiply(BigDecimal.valueOf(deductibleDays))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        log.info("Salary Calculation - Basic: {}, Per Day: {}, DeductibleDays: {}, AbsentLeaveDed: {}",
                 basicSalary, perDayBasic, deductibleDays, absentLeaveDeduction);

        // Earnings (based on full basic salary; deduction applied separately)
        BigDecimal hra = basicSalary.multiply(new BigDecimal("0.20"));
        BigDecimal da = basicSalary.multiply(new BigDecimal("0.10"));
        BigDecimal ta = basicSalary.multiply(new BigDecimal("0.05"));
        BigDecimal otherAllowances = BigDecimal.ZERO;

        // Deductions
        BigDecimal providentFund = basicSalary.multiply(new BigDecimal("0.12"));
        BigDecimal tax = calculateTax(basicSalary);
        BigDecimal insurance = new BigDecimal("500.0");
        BigDecimal otherDeductions = absentLeaveDeduction;

        // Totals
        BigDecimal grossSalary = basicSalary.add(hra).add(da).add(ta).add(otherAllowances);
        BigDecimal totalDeductions = providentFund.add(tax).add(insurance).add(otherDeductions);
        BigDecimal netSalary = grossSalary.subtract(totalDeductions);

        payroll.setEmployee(employee);
        payroll.setMonth(month);
        payroll.setYear(year);
        payroll.setBasicSalary(basicSalary.doubleValue());
        payroll.setHra(round(hra.doubleValue()));
        payroll.setDa(round(da.doubleValue()));
        payroll.setTa(round(ta.doubleValue()));
        payroll.setOtherAllowances(otherAllowances.doubleValue());
        payroll.setProvidentFund(round(providentFund.doubleValue()));
        payroll.setTax(round(tax.doubleValue()));
        payroll.setInsurance(insurance.doubleValue());
        payroll.setOtherDeductions(absentLeaveDeduction.doubleValue());
        payroll.setGrossSalary(round(grossSalary.doubleValue()));
        payroll.setTotalDeductions(round(totalDeductions.doubleValue()));
        payroll.setNetSalary(round(netSalary.doubleValue()));
        payroll.setStatus(statusToUse);
        payroll.setPaymentDate(paymentDateToUse);

        payroll = payrollRepository.save(payroll);
        return convertToDTO(payroll);
    }

    /**
     * Generate payroll for all employees (NO duplicates)
     */
    public List<PayrollDTO> generatePayrollForAll(Integer month, Integer year) {

        List<Employee> employees =
                employeeRepository.findByStatus(Employee.EmployeeStatus.ACTIVE);

        // Always generate/update so attendance/leave edits are reflected.
        List<PayrollDTO> generatedPayrolls = employees.stream()
                .map(emp -> generatePayroll(emp.getId(), month, year))
                .collect(Collectors.toList());

        // Generate payslips
        String monthYear = year + "-" + String.format("%02d", month);

        for (Employee emp : employees) {
            try {
                payslipService.generatePayslip(emp.getId(), monthYear);
            } catch (Exception e) {
                log.warn("Could not generate payslip for employee {}: {}", emp.getId(), e.getMessage());
            }
        }

        return generatedPayrolls;
    }

    public PayrollDTO processPayroll(Long payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        payroll.setStatus(Payroll.PayrollStatus.PROCESSED);
        payroll = payrollRepository.save(payroll);

        return convertToDTO(payroll);
    }

    public PayrollDTO markAsPaid(Long payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        payroll.setStatus(Payroll.PayrollStatus.PAID);
        payroll.setPaymentDate(LocalDate.now());

        payroll = payrollRepository.save(payroll);
        return convertToDTO(payroll);
    }

    public PayrollDTO markAsUnpaid(Long payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        payroll.setStatus(Payroll.PayrollStatus.PROCESSED);
        payroll.setPaymentDate(null);

        payroll = payrollRepository.save(payroll);
        return convertToDTO(payroll);
    }

    public List<PayrollDTO> getPayrollByEmployee(Long employeeId) {
        return payrollRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get payroll for month - returns all active employees.
     * Auto-generates payroll for employees who don't have records yet.
     */
    public List<PayrollDTO> getPayrollByMonth(Integer month, Integer year) {
        List<Employee> allActive = employeeRepository.findByStatus(Employee.EmployeeStatus.ACTIVE);

        // Always recalculate/update payroll for each active employee for this period.
        // Attendance and leave can be changed after initial generation.
        for (Employee emp : allActive) {
            try {
                generatePayroll(emp.getId(), month, year);
            } catch (Exception e) {
                log.warn("Could not regenerate payroll for employee {}: {}", emp.getId(), e.getMessage());
            }
        }

        return payrollRepository.findByMonthAndYear(month, year)
                .stream()
                .sorted((a, b) -> a.getEmployee().getFirstName().compareToIgnoreCase(b.getEmployee().getFirstName()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PayrollDTO> getAllPayrollData() {
        return payrollRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PayrollDTO getPayslip(Long employeeId, Integer month, Integer year) {
        Payroll payroll = payrollRepository
                .findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseThrow(() -> new RuntimeException("Payslip not found"));

        return convertToDTO(payroll);
    }

public List<PayrollDTO> getAllPayroll() {
    return payrollRepository.findAll()
            .stream()
            .filter(p -> p.getEmployee() != null &&
                        p.getEmployee().getStatus() == Employee.EmployeeStatus.ACTIVE)
            .sorted((p1, p2) -> {
                // Sort by year (descending), then month (descending)
                int yearCompare = p2.getYear().compareTo(p1.getYear());
                if (yearCompare != 0) return yearCompare;
                return p2.getMonth().compareTo(p1.getMonth());
            })
            .collect(Collectors.collectingAndThen(
                java.util.stream.Collectors.toMap(
                    p -> p.getEmployee().getId(),
                    p -> p,
                    (p1, p2) -> p1, // Keep first occurrence
                    java.util.LinkedHashMap::new
                ),
                map -> map.values().stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList())
            ));
}

// Simple method to get all payrolls (for controller)
public List<PayrollDTO> getAllPayrolls() {
    return getAllPayroll();
}

    private BigDecimal calculateTax(BigDecimal basicSalary) {
        BigDecimal annualSalary = basicSalary.multiply(new BigDecimal("12"));
        BigDecimal tax = BigDecimal.ZERO;

        if (annualSalary.compareTo(new BigDecimal("1000000")) > 0) {
            tax = basicSalary.multiply(new BigDecimal("0.30"));
        } else if (annualSalary.compareTo(new BigDecimal("500000")) > 0) {
            tax = basicSalary.multiply(new BigDecimal("0.20"));
        } else if (annualSalary.compareTo(new BigDecimal("250000")) > 0) {
            tax = basicSalary.multiply(new BigDecimal("0.10"));
        }

        return tax;
    }

    private Double round(Double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private PayrollDTO convertToDTO(Payroll payroll) {
        Employee employee = payroll.getEmployee();

        // Get attendance data for display
        int presentDays = payslipService.getPresentDaysForEmployee(employee.getId(), payroll.getMonth(), payroll.getYear());
        int absentDays = payslipService.getAbsentDaysForEmployee(employee.getId(), payroll.getMonth(), payroll.getYear());
        int leaveDays = payslipService.getLeaveDaysForEmployee(employee.getId(), payroll.getMonth(), payroll.getYear());
        int halfDays = payslipService.getHalfDaysForEmployee(employee.getId(), payroll.getMonth(), payroll.getYear());
        double deductibleDays = payslipService.getDeductibleDays(absentDays, leaveDays, halfDays);
        BigDecimal basicForCalc = employee.getSalary() != null ? employee.getSalary() : BigDecimal.ZERO;
        double absentLeaveDeduction = basicForCalc.compareTo(BigDecimal.ZERO) > 0
                ? basicForCalc.divide(new BigDecimal("26"), 2, java.math.RoundingMode.HALF_UP).doubleValue() * deductibleDays
                : 0;

        return PayrollDTO.builder()
                .id(payroll.getId())
                .employeeId(employee.getId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .department(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .designation(employee.getDesignation())
                .month(payroll.getMonth())
                .year(payroll.getYear())
                .basicSalary(payroll.getBasicSalary())
                .hra(payroll.getHra())
                .da(payroll.getDa())
                .ta(payroll.getTa())
                .otherAllowances(payroll.getOtherAllowances())
                .providentFund(payroll.getProvidentFund())
                .tax(payroll.getTax())
                .insurance(payroll.getInsurance())
                .otherDeductions(payroll.getOtherDeductions())
                .grossSalary(payroll.getGrossSalary())
                .totalDeductions(payroll.getTotalDeductions())
                .netSalary(payroll.getNetSalary())
                .paymentDate(payroll.getPaymentDate())
                .status(payroll.getStatus().name())
                .presentDays(presentDays)
                .absentDays(absentDays)
                .leaveDays(leaveDays)
                .halfDays(halfDays)
                .absentLeaveDeduction(absentLeaveDeduction)
                .build();
    }

    public PayslipDTO generatePayslipForEmployee(Long employeeId, String monthYear) {
        return payslipService.generatePayslip(employeeId, monthYear);
    }
}