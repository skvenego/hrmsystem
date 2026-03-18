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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final PayslipService payslipService;

    public PayrollService(PayrollRepository payrollRepository, EmployeeRepository employeeRepository, PayslipService payslipService) {
        this.payrollRepository = payrollRepository;
        this.employeeRepository = employeeRepository;
        this.payslipService = payslipService;
    }

    public PayrollDTO generatePayroll(Long employeeId, Integer month, Integer year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (payrollRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year).isPresent()) {
            throw new RuntimeException("Payroll already exists for this month");
        }

        BigDecimal basicSalary = employee.getSalary() != null ? employee.getSalary() : BigDecimal.ZERO;

        // Calculate allowances (example percentages)
        BigDecimal hra = basicSalary.multiply(new BigDecimal("0.20"));
        BigDecimal da = basicSalary.multiply(new BigDecimal("0.10"));
        BigDecimal ta = basicSalary.multiply(new BigDecimal("0.05"));
        BigDecimal otherAllowances = BigDecimal.ZERO;

        // Calculate deductions
        BigDecimal providentFund = basicSalary.multiply(new BigDecimal("0.12"));
        BigDecimal tax = calculateTax(basicSalary);
        BigDecimal insurance = new BigDecimal("500.0");
        BigDecimal otherDeductions = BigDecimal.ZERO;

        // Calculate totals
        BigDecimal grossSalary = basicSalary.add(hra).add(da).add(ta).add(otherAllowances);
        BigDecimal totalDeductions = providentFund.add(tax).add(insurance).add(otherDeductions);
        BigDecimal netSalary = grossSalary.subtract(totalDeductions);

        Payroll payroll = Payroll.builder()
                .employee(employee)
                .month(month)
                .year(year)
                .basicSalary(basicSalary.doubleValue())
                .hra(round(hra.doubleValue()))
                .da(round(da.doubleValue()))
                .ta(round(ta.doubleValue()))
                .otherAllowances(otherAllowances.doubleValue())
                .providentFund(round(providentFund.doubleValue()))
                .tax(round(tax.doubleValue()))
                .insurance(insurance.doubleValue())
                .otherDeductions(otherDeductions.doubleValue())
                .grossSalary(round(grossSalary.doubleValue()))
                .totalDeductions(round(totalDeductions.doubleValue()))
                .netSalary(round(netSalary.doubleValue()))
                .status(Payroll.PayrollStatus.PENDING)
                .build();

        payroll = payrollRepository.save(payroll);
        return convertToDTO(payroll);
    }

    public List<PayrollDTO> generatePayrollForAll(Integer month, Integer year) {
        List<Employee> employees = employeeRepository.findByStatus(Employee.EmployeeStatus.ACTIVE);

        List<PayrollDTO> generatedPayrolls = employees.stream()
                .filter(emp -> payrollRepository.findByEmployeeIdAndMonthAndYear(emp.getId(), month, year).isEmpty())
                .map(emp -> generatePayroll(emp.getId(), month, year))
                .collect(Collectors.toList());

        // Also generate payslips with attendance and leave data for all employees
        String monthYear = year + "-" + String.format("%02d", month);
        for (Employee emp : employees) {
            try {
                payslipService.generatePayslip(emp.getId(), monthYear);
            } catch (Exception e) {
                // Skip if already exists or error
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

    public List<PayrollDTO> getPayrollByMonth(Integer month, Integer year) {
        return payrollRepository.findByMonthAndYear(month, year)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public PayrollDTO getPayslip(Long employeeId, Integer month, Integer year) {
        Payroll payroll = payrollRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseThrow(() -> new RuntimeException("Payslip not found"));
        return convertToDTO(payroll);
    }

    public List<PayrollDTO> getAllPayroll() {
        return payrollRepository.findAll()
                .stream()
                .filter(payroll -> payroll.getEmployee().getStatus() == Employee.EmployeeStatus.ACTIVE)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
                .build();
    }

    /**
     * Generate payslip with attendance and leave details for a specific employee and month
     */
    public PayslipDTO generatePayslipForEmployee(Long employeeId, String monthYear) {
        // Use PayslipService to generate detailed payslip with attendance and leave data
        return payslipService.generatePayslip(employeeId, monthYear);
    }
}