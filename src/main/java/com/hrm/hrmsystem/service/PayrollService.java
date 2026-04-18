package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.PayrollDTO;
import com.hrm.hrmsystem.dto.PayslipDTO;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.model.Payroll;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
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
    private final LeaveRepository leaveRepository;
    private final PayslipService payslipService;

    public PayrollService(PayrollRepository payrollRepository,
                          EmployeeRepository employeeRepository,
                          LeaveRepository leaveRepository,
                          PayslipService payslipService) {
        this.payrollRepository = payrollRepository;
        this.employeeRepository = employeeRepository;
        this.leaveRepository = leaveRepository;
        this.payslipService = payslipService;
    }

    /**
     * Generate payroll for single employee with attendance-based calculation
     */
    @Transactional
    public PayrollDTO generatePayroll(Long employeeId, Integer month, Integer year) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // ✅ Prevent duplicate - use findFirst to handle existing duplicates
        Optional<Payroll> existing = payrollRepository
                .findFirstByEmployeeIdAndMonthAndYearOrderByIdDesc(employeeId, month, year);
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

        // Get ACTUAL approved leave data with paid/unpaid days from leave records (single source of truth)
        double actualPaidLeaveDays = 0;
        double actualUnpaidLeaveDays = 0;
        
        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());
        
        List<Leave> approvedLeaves = leaveRepository.findByEmployeeIdAndStatusAndStartDateBetween(
                employeeId, Leave.LeaveStatus.APPROVED, startOfMonth, endOfMonth);
        
        for (Leave leave : approvedLeaves) {
            actualPaidLeaveDays += leave.getPaidDays();
            actualUnpaidLeaveDays += leave.getUnpaidDays();
        }
        
        // If no approved leaves found, use attendance-based calculation as fallback
        double paidLeaveDays = actualPaidLeaveDays > 0 ? actualPaidLeaveDays : 
                              (checkProbationStatus(employee, year, month) ? 0 : Math.min(3, leaveDays));
        double unpaidLeaveDays = actualUnpaidLeaveDays > 0 ? actualUnpaidLeaveDays :
                                leaveDays - (checkProbationStatus(employee, year, month) ? 0 : Math.min(3, leaveDays));

        log.info("Attendance for Employee {}: Present={}, Absent={}, Leave={}, HalfDays={}",
                 employeeId, presentDays, absentDays, leaveDays, halfDays);
        log.info("Leave Data from DB: ApprovedLeaves={}, PaidDays={}, UnpaidDays={}",
                 approvedLeaves.size(), paidLeaveDays, unpaidLeaveDays);

        // Use employee's stored salary components from employee details
        BigDecimal basicSalary = employee.getBasicSalary() != null
                ? employee.getBasicSalary()
                : BigDecimal.ZERO;
        BigDecimal hra = employee.getHra() != null
                ? employee.getHra()
                : BigDecimal.ZERO;
        BigDecimal da = employee.getDa() != null
                ? employee.getDa()
                : BigDecimal.ZERO;
        BigDecimal otherAllowance = employee.getOtherAllowance() != null
                ? employee.getOtherAllowance()
                : BigDecimal.ZERO;
        
        // Total salary for per-day calculation
        BigDecimal totalSalary = employee.getSalary() != null
                ? employee.getSalary()
                : basicSalary.add(hra).add(da).add(otherAllowance);

        // Per-day salary for deduction (26 working days standard) - based on BASIC salary only
        BigDecimal perDayBasic = basicSalary.compareTo(BigDecimal.ZERO) > 0
                ? basicSalary.divide(new BigDecimal("26"), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Calculate absent-only deduction (employee didn't request leave, just absent)
        BigDecimal absentOnlyDeduction = perDayBasic.multiply(BigDecimal.valueOf(absentDays))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        // Calculate leave deduction for unpaid leaves
        BigDecimal leaveDeduction = perDayBasic.multiply(BigDecimal.valueOf(unpaidLeaveDays))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        
        BigDecimal totalAttendanceDeduction = absentOnlyDeduction.add(leaveDeduction);

        log.info("Salary Calculation - Basic: {}, HRA: {}, DA: {}, Other: {}, Total: {}",
                 basicSalary, hra, da, otherAllowance, totalSalary);
        log.info("Attendance Deduction - Absent: {}, Unpaid Leave: {}, Total: {}",
                 absentOnlyDeduction, leaveDeduction, totalAttendanceDeduction);

        // Earnings from employee stored values
        BigDecimal ta = BigDecimal.ZERO; // Can be calculated or stored separately

        // Deductions from employee stored values
        BigDecimal providentFund = employee.getPf() != null
                ? employee.getPf()
                : totalSalary.multiply(new BigDecimal("0.12")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal tax = employee.getTax() != null
                ? employee.getTax()
                : calculateTax(totalSalary);
        // Calculate insurance as percentage of BASIC salary (e.g., 0.5%)
        BigDecimal insurance = BigDecimal.ZERO;
        if (employee.getInsurancePercentage() != null && employee.getInsurancePercentage() > 0) {
            insurance = basicSalary.multiply(BigDecimal.valueOf(employee.getInsurancePercentage()))
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }
        BigDecimal otherDeductions = totalAttendanceDeduction;

        // Totals
        BigDecimal grossSalary = basicSalary.add(hra).add(da).add(ta).add(otherAllowance);
        BigDecimal totalDeductions = providentFund.add(tax).add(insurance).add(otherDeductions);
        BigDecimal netSalary = grossSalary.subtract(totalDeductions);

        payroll.setEmployee(employee);
        payroll.setMonth(month);
        payroll.setYear(year);
        payroll.setBasicSalary(basicSalary);
        payroll.setHra(hra.setScale(2, java.math.RoundingMode.HALF_UP));
        payroll.setDa(da.setScale(2, java.math.RoundingMode.HALF_UP));
        payroll.setTa(ta.setScale(2, java.math.RoundingMode.HALF_UP));
        payroll.setOtherAllowances(otherAllowance);
        payroll.setProvidentFund(providentFund.setScale(2, java.math.RoundingMode.HALF_UP));
        payroll.setTax(tax.setScale(2, java.math.RoundingMode.HALF_UP));
        payroll.setInsurance(insurance.setScale(2, java.math.RoundingMode.HALF_UP));
        payroll.setOtherDeductions(otherDeductions);
        payroll.setGrossSalary(grossSalary.setScale(2, java.math.RoundingMode.HALF_UP));
        payroll.setTotalDeductions(totalDeductions.setScale(2, java.math.RoundingMode.HALF_UP));
        payroll.setNetSalary(netSalary.setScale(2, java.math.RoundingMode.HALF_UP));
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

    /**
     * Send payroll for accountant approval
     */
    public PayrollDTO sendForApproval(Long payrollId, String approvedBy) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        if (payroll.getStatus() != Payroll.PayrollStatus.PENDING) {
            throw new RuntimeException("Payroll must be in PENDING status to send for approval");
        }

        payroll.setStatus(Payroll.PayrollStatus.PENDING_APPROVAL);
        payroll = payrollRepository.save(payroll);

        // Generate payslip with pending status
        generatePayslipWithStatus(payroll, "PENDING_APPROVAL", approvedBy);

        return convertToDTO(payroll);
    }

    /**
     * Approve payroll by accountant
     */
    public PayrollDTO approveByAccountant(Long payrollId, String accountantName) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + payrollId));

        log.info("Approving payroll {} - current status: {}, expected: {}", 
                 payrollId, payroll.getStatus(), Payroll.PayrollStatus.PENDING_APPROVAL);
        
        // Use equals() instead of != for enum comparison
        if (!Payroll.PayrollStatus.PENDING_APPROVAL.equals(payroll.getStatus())) {
            throw new RuntimeException("Payroll must be in PENDING_APPROVAL status to approve. Current status: " 
                + payroll.getStatus() + " (ID: " + payrollId + ")");
        }

        payroll.setStatus(Payroll.PayrollStatus.APPROVED);
        payroll.setAccountantApproved(true);
        payroll.setFinalApprovalDate(LocalDate.now());
        payroll = payrollRepository.save(payroll);

        // Update payslip with approval info
        try {
            generatePayslipWithStatus(payroll, "APPROVED", accountantName);
        } catch (Exception e) {
            log.warn("Could not update payslip status, but payroll was approved: {}", e.getMessage());
            // Don't fail the approval if payslip update fails
        }

        log.info("Payroll {} approved successfully", payrollId);
        return convertToDTO(payroll);
    }

    /**
     * Reject payroll by accountant
     */
    public PayrollDTO rejectByAccountant(Long payrollId, String reason) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        if (payroll.getStatus() != Payroll.PayrollStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Payroll must be in PENDING_APPROVAL status to reject");
        }

        payroll.setStatus(Payroll.PayrollStatus.PENDING);
        payroll.setAccountantApproved(false);
        payroll = payrollRepository.save(payroll);

        return convertToDTO(payroll);
    }

    /**
     * Mark payroll as paid (after accountant approval)
     */
    public PayrollDTO markAsPaid(Long payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        if (payroll.getStatus() != Payroll.PayrollStatus.APPROVED) {
            throw new RuntimeException("Payroll must be APPROVED by accountant before payment");
        }

        payroll.setStatus(Payroll.PayrollStatus.PAID);
        payroll.setPaymentDate(LocalDate.now());
        payroll = payrollRepository.save(payroll);

        // Update payslip status
        updatePayslipStatusToPaid(payroll);

        return convertToDTO(payroll);
    }

    private void generatePayslipWithStatus(Payroll payroll, String status, String approvedBy) {
        try {
            String monthYear = payroll.getYear() + "-" + String.format("%02d", payroll.getMonth());
            
            // First generate/regenerate payslip to ensure it exists
            payslipService.generatePayslip(payroll.getEmployee().getId(), monthYear);
            
            // Now update the payslip status in the database
            payslipService.updatePayslipStatusByEmployeeAndMonth(
                payroll.getEmployee().getId(), 
                monthYear, 
                status, 
                approvedBy, 
                "Approved by HR and Accountants"
            );
            
            log.info("Updated payslip status to {} for employee {} month {}", status, payroll.getEmployee().getId(), monthYear);
        } catch (Exception e) {
            log.warn("Could not update payslip status: {}", e.getMessage());
        }
    }

    private void updatePayslipStatusToPaid(Payroll payroll) {
        try {
            String monthYear = payroll.getYear() + "-" + String.format("%02d", payroll.getMonth());
            
            // First generate/regenerate payslip to ensure it exists
            payslipService.generatePayslip(payroll.getEmployee().getId(), monthYear);
            
            // Now update the payslip status in the database
            payslipService.updatePayslipStatusByEmployeeAndMonth(
                payroll.getEmployee().getId(), 
                monthYear, 
                "SENT", 
                "HR", 
                "Payment processed. Approved by HR and Accountants"
            );
            
            log.info("Updated payslip status to SENT for employee {} month {}", payroll.getEmployee().getId(), monthYear);
        } catch (Exception e) {
            log.warn("Could not update payslip to paid status: {}", e.getMessage());
        }
    }

    /**
     * Process payroll - sends for approval (legacy method for backward compatibility)
     */
    public PayrollDTO processPayroll(Long payrollId) {
        return sendForApproval(payrollId, "HR");
    }

    /**
     * Mark as unpaid - revert payment status
     */
    public PayrollDTO markAsUnpaid(Long payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        payroll.setStatus(Payroll.PayrollStatus.APPROVED);
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
        // Use findFirst to handle duplicate payroll records - returns most recent
        Payroll payroll = payrollRepository
                .findFirstByEmployeeIdAndMonthAndYearOrderByIdDesc(employeeId, month, year)
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

    /**
     * Check if employee is in probation period for given month/year
     */
    private boolean checkProbationStatus(Employee employee, int year, int month) {
        if (employee.getJoiningDate() == null || employee.getProbationPeriodMonths() == null) {
            return false; // No probation defined, consider as completed
        }

        LocalDate probationEndDate = employee.getJoiningDate()
                .plusMonths(employee.getProbationPeriodMonths());
        
        LocalDate payslipDate = LocalDate.of(year, month, 1);
        
        return payslipDate.isBefore(probationEndDate) || payslipDate.isEqual(probationEndDate);
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

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private PayrollDTO convertToDTO(Payroll payroll) {
        Employee employee = payroll.getEmployee();

        // Get attendance data for display
        int presentDays = payslipService.getPresentDaysForEmployee(employee.getId(), payroll.getMonth(), payroll.getYear());
        int absentDays = payslipService.getAbsentDaysForEmployee(employee.getId(), payroll.getMonth(), payroll.getYear());
        int leaveDays = payslipService.getLeaveDaysForEmployee(employee.getId(), payroll.getMonth(), payroll.getYear());
        int halfDays = payslipService.getHalfDaysForEmployee(employee.getId(), payroll.getMonth(), payroll.getYear());
        
        // Calculate paid vs unpaid leave days
        int paidLeaveDays = 0;
        int unpaidLeaveDays = 0;
        boolean isInProbation = checkProbationStatus(employee, payroll.getYear(), payroll.getMonth());
        
        if (isInProbation) {
            paidLeaveDays = 0;
            unpaidLeaveDays = leaveDays;
        } else {
            paidLeaveDays = Math.min(3, leaveDays);
            unpaidLeaveDays = leaveDays - paidLeaveDays;
        }
        
        double absentLeaveDeduction = payroll.getOtherDeductions() != null 
                ? payroll.getOtherDeductions().doubleValue() 
                : 0.0;

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
                .insuranceName(employee.getInsuranceName())
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
                .paidLeaveDays(paidLeaveDays)
                .unpaidLeaveDays(unpaidLeaveDays)
                .halfDays(halfDays)
                .absentLeaveDeduction(absentLeaveDeduction)
                .build();
    }

    public PayslipDTO generatePayslipForEmployee(Long employeeId, String monthYear) {
        return payslipService.generatePayslip(employeeId, monthYear);
    }

    /**
     * Get payroll by ID
     */
    @Transactional(readOnly = true)
    public PayrollDTO getPayrollById(Long payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + payrollId));
        return convertToDTO(payroll);
    }

    /**
     * Update payroll and associated payslip
     */
    @Transactional
    public PayrollDTO updatePayroll(Long payrollId, PayrollDTO payrollDTO) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + payrollId));

        // Update payroll fields
        payroll.setBasicSalary(payrollDTO.getBasicSalary());
        payroll.setHra(payrollDTO.getHra());
        payroll.setDa(payrollDTO.getDa());
        payroll.setOtherAllowances(payrollDTO.getOtherAllowances());
        payroll.setProvidentFund(payrollDTO.getProvidentFund());
        payroll.setTax(payrollDTO.getTax());
        payroll.setInsurance(payrollDTO.getInsurance());
        payroll.setOtherDeductions(payrollDTO.getOtherDeductions());

        // Recalculate totals
        BigDecimal grossSalary = payrollDTO.getBasicSalary()
                .add(payrollDTO.getHra())
                .add(payrollDTO.getDa())
                .add(payrollDTO.getOtherAllowances());
        payroll.setGrossSalary(grossSalary);

        BigDecimal totalDeductions = payrollDTO.getProvidentFund()
                .add(payrollDTO.getTax())
                .add(payrollDTO.getInsurance())
                .add(payrollDTO.getOtherDeductions());
        payroll.setTotalDeductions(totalDeductions);

        BigDecimal netSalary = grossSalary.subtract(totalDeductions);
        payroll.setNetSalary(netSalary);

        payroll = payrollRepository.save(payroll);

        // Update associated payslip
        try {
            String monthYear = payroll.getYear() + "-" + String.format("%02d", payroll.getMonth());
            payslipService.updatePayslipFromPayroll(payroll.getEmployee().getId(), monthYear, payrollDTO);
        } catch (Exception e) {
            log.warn("Could not update payslip for payroll {}: {}", payrollId, e.getMessage());
        }

        return convertToDTO(payroll);
    }
}