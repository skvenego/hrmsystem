package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.PayrollDTO;
import com.hrm.hrmsystem.dto.PayslipDTO;
import com.hrm.hrmsystem.engine.AttendanceSummary;
import com.hrm.hrmsystem.engine.AttendanceEngine;
import com.hrm.hrmsystem.model.Employee;
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
// ✅ REMOVED: YearMonth - Use AttendanceEngine for all calculations
// import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.hrm.hrmsystem.util.EmailUtil;

@Service
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private final PayrollRepository payrollRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRepository leaveRepository;
    private final PayslipService payslipService;
    private final LeaveService leaveService;
    private final UnifiedCalculationService unifiedCalculationService;
    private final AttendanceEngine attendanceEngine;
    private final EmailUtil emailUtil;

    public PayrollService(PayrollRepository payrollRepository,
                          EmployeeRepository employeeRepository,
                          LeaveRepository leaveRepository,
                          PayslipService payslipService,
                          LeaveService leaveService,
                          UnifiedCalculationService unifiedCalculationService,
                          AttendanceEngine attendanceEngine,
                          EmailUtil emailUtil) {
        this.payrollRepository = payrollRepository;
        this.employeeRepository = employeeRepository;
        this.leaveRepository = leaveRepository;
        this.payslipService = payslipService;
        this.leaveService = leaveService;
        this.unifiedCalculationService = unifiedCalculationService;
        this.attendanceEngine = attendanceEngine;
        this.emailUtil = emailUtil;
    }

    @Transactional
    public PayrollDTO generatePayroll(Long employeeId, Integer month, Integer year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (isSystemUser(employee)) {
            throw new RuntimeException("Cannot generate payroll for System Users");
        }

        Optional<Payroll> existing = payrollRepository
                .findFirstByEmployeeIdAndMonthAndYearOrderByIdDesc(employeeId, month, year);
        Payroll payroll = existing.orElseGet(Payroll::new);
        Payroll.PayrollStatus statusToUse = existing.map(Payroll::getStatus).orElse(Payroll.PayrollStatus.PENDING);
        java.time.LocalDate paymentDateToUse = existing.map(Payroll::getPaymentDate).orElse(null);

        // 🔥 SINGLE SOURCE OF TRUTH: Use engine for all calculations
        AttendanceSummary summary = unifiedCalculationService.calculateForPayroll(employeeId, year, month);
        
        double workedDays = summary.workedDays; // ✅ FIXED: Use workedDays instead of present
        double absentDays = summary.absent;
        
        // ✅ Use calculateLeaveBalance for leave data (same source as dashboard and other services)
        // ✅ FIXED: Use summary from AttendanceEngine for THIS MONTH'S values
        // leaveBalance.usedLeaves is the CUMULATIVE total for the 6-month cycle
        double paidLeaveDays = summary.paidLeave;
        double unpaidLeaveDays = summary.unpaidLeave;
        
        log.info("🔥 AttendanceEngine Result for Employee {}: {}", employeeId, summary);

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
        
        // ✅ GROSS SALARY FORMULA: Deductions always from full Gross Salary (salary field takes priority)
        // getTotalGrossSalary() returns employee.salary if set, otherwise sums components.
        // The AttendanceEngine uses this same method internally for daily-rate calculation.
        BigDecimal grossSalaryBase = employee.getTotalGrossSalary();

        // Use AttendanceEngine for ALL salary calculations (single source of truth)
        // Daily Rate = grossSalaryBase / 26 working days
        // Unpaid deduction = Daily Rate × unpaid days × 1
        // Absent deduction = Daily Rate × absent days × 2 (penalty)
        AttendanceEngine.SalarySummary salarySummary = attendanceEngine.calculateSalary(
                employee, summary, !attendanceEngine.isProbationCompleted(employee));

        log.info("💰 PayrollService - GrossBase: {}, UnpaidDeduction: {}, AbsentDeduction: {}",
                 grossSalaryBase,
                 salarySummary.getUnpaidLeaveDeduction(),
                 salarySummary.getAbsentLeaveDeduction());

        BigDecimal ta = BigDecimal.ZERO;
        BigDecimal totalAttendanceDeduction = salarySummary.getAbsentLeaveDeduction().add(salarySummary.getUnpaidLeaveDeduction());
        BigDecimal providentFund = salarySummary.getPf();
        BigDecimal tax = salarySummary.getTax();
        BigDecimal insurance = salarySummary.getInsurance();
        BigDecimal grossSalary = salarySummary.getGrossSalary();
        BigDecimal totalDeductions = salarySummary.getTotalDeductions();
        BigDecimal netSalary = salarySummary.getNetSalary();
        BigDecimal otherDeductions = totalAttendanceDeduction;

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
        payroll.setUnpaidLeaveDeduction(salarySummary.getUnpaidLeaveDeduction());
        payroll.setAbsentDeduction(salarySummary.getAbsentLeaveDeduction());
        // STORE attendance values from AttendanceEngine (single source of truth)
        payroll.setPresentDays(workedDays); // ✅ FIXED: Use workedDays instead of presentDays
        payroll.setAbsentDays(absentDays);
        payroll.setPaidLeaveDays(paidLeaveDays);
        payroll.setUnpaidLeaveDays(unpaidLeaveDays);
        payroll.setStatus(statusToUse);
        payroll.setPaymentDate(paymentDateToUse);

        payroll = payrollRepository.save(payroll);
        return convertToDTO(payroll);
    }

    @Transactional
    public List<PayrollDTO> generatePayrollForAll(Integer month, Integer year) {
        List<Employee> employees = employeeRepository.findByStatus(Employee.EmployeeStatus.ACTIVE)
            .stream()
            .filter(emp -> !isSystemUser(emp))
            .collect(Collectors.toList());

        List<PayrollDTO> generatedPayrolls = employees.stream()
                .map(emp -> generatePayroll(emp.getId(), month, year))
                .collect(Collectors.toList());

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

    @Transactional
    public PayrollDTO sendForApproval(Long payrollId, String approvedBy) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        if (payroll.getStatus() != Payroll.PayrollStatus.PENDING) {
            throw new RuntimeException("Payroll must be in PENDING status to send for approval");
        }

        payroll.setStatus(Payroll.PayrollStatus.PENDING_APPROVAL);
        payroll = payrollRepository.save(payroll);

        generatePayslipWithStatus(payroll, "DRAFT", approvedBy);

        return convertToDTO(payroll);
    }

    @Transactional
    public PayrollDTO approveByAccountant(Long payrollId, String accountantName) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + payrollId));

        log.info("Approving payroll {} - current status: {}, expected: {}", 
                 payrollId, payroll.getStatus(), Payroll.PayrollStatus.PENDING_APPROVAL);
        
        if (!Payroll.PayrollStatus.PENDING_APPROVAL.equals(payroll.getStatus())) {
            throw new RuntimeException("Payroll must be in PENDING_APPROVAL status to approve. Current status: " 
                + payroll.getStatus() + " (ID: " + payrollId + ")");
        }

        payroll.setStatus(Payroll.PayrollStatus.APPROVED);
        payroll.setAccountantApproved(true);
        payroll.setFinalApprovalDate(LocalDate.now());
        payroll = payrollRepository.save(payroll);

        try {
            generatePayslipWithStatus(payroll, "APPROVED", accountantName);
        } catch (Exception e) {
            log.warn("Could not update payslip status, but payroll was approved: {}", e.getMessage());
        }

        // Send approval notification email to employee
        if (payroll.getEmployee().getEmail() != null && !payroll.getEmployee().getEmail().isEmpty()) {
            try {
                String subject = "Your Salary Slip has been Approved - " + String.format("%02d", payroll.getMonth()) + "/" + payroll.getYear();
                String htmlBody = String.format(
                        "<html><body style='font-family: Arial, sans-serif;'>" +
                        "<h2 style='color: #4CAF50;'>Payroll Approved</h2>" +
                        "<p>Dear <strong>%s %s</strong>,</p>" +
                        "<p>Your payroll for the month of <strong>%02d/%d</strong> has been successfully approved by the accountant.</p>" +
                        "<p>Your net salary of <strong>₹%,.2f</strong> will be processed shortly.</p>" +
                        "<br>" +
                        "<p>You can view and download your detailed payslip by logging into the HRM Portal.</p>" +
                        "<br>" +
                        "<p>Best Regards,</p>" +
                        "<p><strong>HR Department</strong></p>" +
                        "<p style='color: #888; font-size: 12px;'>This is an automated email. Please do not reply.</p>" +
                        "</body></html>",
                        payroll.getEmployee().getFirstName(), payroll.getEmployee().getLastName(),
                        payroll.getMonth(), payroll.getYear(), payroll.getNetSalary()
                );
                emailUtil.sendHtmlEmail(payroll.getEmployee().getEmail(), subject, htmlBody);
                log.info("Approval email sent to employee: {}", payroll.getEmployee().getEmail());
            } catch (Exception e) {
                log.warn("Failed to send approval email to employee {}: {}", payroll.getEmployee().getEmail(), e.getMessage());
            }
        }

        log.info("Payroll {} approved successfully", payrollId);
        return convertToDTO(payroll);
    }

    @Transactional
    public PayrollDTO approveByDirector(Long payrollId, String approvedBy) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + payrollId));

        if (payroll.getStatus() == Payroll.PayrollStatus.PENDING) {
            throw new RuntimeException("Payroll must be sent for approval first");
        }

        payroll.setDirectorApproved(true);
        payroll.setFinalApprovalDate(LocalDate.now());

        if (Boolean.TRUE.equals(payroll.getAccountantApproved())) {
            payroll.setStatus(Payroll.PayrollStatus.PAID);
            payroll.setPaymentDate(LocalDate.now());
            log.info("Payroll {} fully approved by both accountant and admin — marked PAID", payrollId);
        } else {
            log.info("Payroll {} admin-approved. Awaiting accountant approval.", payrollId);
        }

        payroll = payrollRepository.save(payroll);

        try {
            generatePayslipWithStatus(payroll, payroll.getStatus() == Payroll.PayrollStatus.PAID ? "SENT" : "APPROVED", approvedBy);
        } catch (Exception e) {
            log.warn("Could not update payslip status after director approval: {}", e.getMessage());
        }

        return convertToDTO(payroll);
    }

    @Transactional
    public PayrollDTO rejectByDirector(Long payrollId, String reason) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + payrollId));

        payroll.setDirectorApproved(false);
        payroll.setStatus(Payroll.PayrollStatus.PENDING_APPROVAL);
        payroll = payrollRepository.save(payroll);

        log.info("Payroll {} rejected by admin/director. Reason: {}", payrollId, reason);
        return convertToDTO(payroll);
    }

    @Transactional
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

    @Transactional
    public PayrollDTO markAsPaid(Long payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        if (payroll.getStatus() != Payroll.PayrollStatus.APPROVED) {
            throw new RuntimeException("Payroll must be APPROVED by accountant before payment");
        }

        payroll.setStatus(Payroll.PayrollStatus.PAID);
        payroll.setDirectorApproved(true);
        payroll.setFinalApprovalDate(LocalDate.now());
        payroll.setPaymentDate(LocalDate.now());
        payroll = payrollRepository.save(payroll);

        updatePayslipStatusToPaid(payroll);

        return convertToDTO(payroll);
    }

    private void generatePayslipWithStatus(Payroll payroll, String status, String approvedBy) {
        try {
            String monthYear = payroll.getYear() + "-" + String.format("%02d", payroll.getMonth());
            
            payslipService.generatePayslip(payroll.getEmployee().getId(), monthYear);
            
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
            
            payslipService.generatePayslip(payroll.getEmployee().getId(), monthYear);
            
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

    public PayrollDTO processPayroll(Long payrollId) {
        return sendForApproval(payrollId, "HR");
    }

    @Transactional
    public PayrollDTO markAsUnpaid(Long payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found"));

        payroll.setStatus(Payroll.PayrollStatus.APPROVED);
        payroll.setPaymentDate(null);

        return convertToDTO(payroll);
    }

    public List<PayrollDTO> getPayrollByEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (employee != null && isSystemUser(employee)) {
            return new java.util.ArrayList<>();
        }
        return payrollRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PayrollDTO> getPayrollByMonth(Integer month, Integer year) {
        List<Employee> allActive = employeeRepository.findByStatus(Employee.EmployeeStatus.ACTIVE)
            .stream()
            .filter(emp -> !isSystemUser(emp))
            .collect(Collectors.toList());

        for (Employee emp : allActive) {
            try {
                leaveService.initializeLeaveBalance(emp.getId());
                generatePayroll(emp.getId(), month, year);
            } catch (Exception e) {
                log.warn("Could not regenerate payroll for employee {}: {}", emp.getId(), e.getMessage());
            }
        }

        return payrollRepository.findByMonthAndYear(month, year)
                .stream()
                .filter(p -> p.getEmployee() != null && !isSystemUser(p.getEmployee()))
                .sorted((a, b) -> a.getEmployee().getFirstName().compareToIgnoreCase(b.getEmployee().getFirstName()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PayrollDTO> getAllPayrolls() {
        return payrollRepository.findAll()
                .stream()
                .filter(p -> p.getEmployee() != null &&
                        p.getEmployee().getStatus() == Employee.EmployeeStatus.ACTIVE &&
                        !isSystemUser(p.getEmployee()))
                .sorted((p1, p2) -> {
                    int yearCompare = p2.getYear().compareTo(p1.getYear());
                    if (yearCompare != 0) return yearCompare;
                    return p2.getMonth().compareTo(p1.getMonth());
                })
                .collect(Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(
                                p -> p.getEmployee().getId(),
                                p -> p,
                                (p1, p2) -> p1,
                                java.util.LinkedHashMap::new
                        ),
                        map -> map.values().stream()
                                .map(this::convertToDTO)
                                .collect(Collectors.toList())
                ));
    }

    @Transactional(readOnly = true)
    public PayrollDTO getPayrollById(Long payrollId) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + payrollId));
        if (payroll.getEmployee() != null && isSystemUser(payroll.getEmployee())) {
            throw new RuntimeException("Access denied: Cannot retrieve payroll for System Users");
        }
        return convertToDTO(payroll);
    }

    @Transactional
    public PayrollDTO updatePayroll(Long payrollId, PayrollDTO payrollDTO) {
        Payroll payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() -> new RuntimeException("Payroll not found with id: " + payrollId));

        payroll.setBasicSalary(payrollDTO.getBasicSalary());
        payroll.setHra(payrollDTO.getHra());
        payroll.setDa(payrollDTO.getDa());
        payroll.setOtherAllowances(payrollDTO.getOtherAllowances());
        payroll.setProvidentFund(payrollDTO.getProvidentFund());
        payroll.setTax(payrollDTO.getTax());
        payroll.setInsurance(payrollDTO.getInsurance());
        payroll.setOtherDeductions(payrollDTO.getOtherDeductions());

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

        try {
            String monthYear = payroll.getYear() + "-" + String.format("%02d", payroll.getMonth());
            payslipService.updatePayslipFromPayroll(payroll.getEmployee().getId(), monthYear, payrollDTO);
        } catch (Exception e) {
            log.warn("Could not update payslip for payroll {}: {}", payrollId, e.getMessage());
        }

        return convertToDTO(payroll);
    }

    @Transactional
    public void deleteAllPayrolls() {
        log.info("Deleting all payroll records");
        payrollRepository.deleteAll();
        log.info("All payroll records deleted successfully");
    }

    private boolean isSystemUser(Employee emp) {
        if (emp.getDepartment() == null || emp.getDepartment().getName() == null) return false;
        String dept = emp.getDepartment().getName().toLowerCase();
        return dept.contains("hr") || dept.contains("director") || 
               dept.contains("leave") || dept.contains("accountant");
    }

    private PayrollDTO convertToDTO(Payroll payroll) {
        Employee employee = payroll.getEmployee();

        // Dynamically compute attendance to handle stale or legacy DB records where days were saved as 0
        AttendanceSummary attSummary = unifiedCalculationService.calculateForPayroll(employee.getId(), payroll.getYear(), payroll.getMonth());
        double presentDays = attSummary.workedDays;
        double absentDays = attSummary.absent;
        double paidLeaveDays = attSummary.paidLeave;
        double unpaidLeaveDays = attSummary.unpaidLeave;
        double leaveDays = paidLeaveDays + unpaidLeaveDays; // Total leave days
        // ✅ FIXED: Remove duplicate calculation - use engine values directly
        // leaveDays should come from AttendanceEngine if needed
        int halfDays = 0; // Half days are already accounted for in present/absent counts
        
        double absentLeaveDeduction = payroll.getOtherDeductions() != null 
                ? payroll.getOtherDeductions().doubleValue() 
                : 0.0;

        // Get leave balance
        PayrollDTO.LeaveBalanceInfo leaveBalanceInfo;
        // ✅ FIXED: Use centralized probation method from AttendanceEngine
        Boolean inProbation = !attendanceEngine.isProbationCompleted(employee);
        String probationStatus = inProbation ? "In Progress" : "Completed";
        Integer probationMonths = employee.getProbationPeriodMonths() != null ? employee.getProbationPeriodMonths() : 3;
        
        LocalDate probationCompletionDate = null;
        if (employee.getJoiningDate() != null) {
            probationCompletionDate = employee.getJoiningDate().plusMonths(probationMonths);
        }
        
        try {
            // ✅ Use AttendanceEngine ONLY - no duplicate services
            AttendanceEngine.LeaveBalanceSummary summary = attendanceEngine.calculateLeaveBalance(
                employee.getId(), payroll.getYear(), payroll.getMonth()
            );
            int currentCycle = payroll.getMonth() <= 6 ? 1 : 2;
            leaveBalanceInfo = PayrollDTO.LeaveBalanceInfo.builder()
                    .totalEarnedLeaves(summary.earnedLeaves)
                    .usedLeaves(summary.totalUsedLeaves) // ✅ FIXED: Total = Paid + Unpaid
                    .availableLeaves(summary.getAvailableLeaves())
                    .carriedForwardLeaves(0.0)
                    .unpaidLeaves(summary.unpaidLeaves) // ✅ FIXED: was 0.0
                    .cycle(currentCycle)
                    .build();
        } catch (Exception e) {
            log.error("Error fetching leave statistics for employee {}: {}", employee.getId(), e.getMessage());
            leaveBalanceInfo = PayrollDTO.LeaveBalanceInfo.builder()
                    .totalEarnedLeaves(0.0).usedLeaves(0.0).availableLeaves(0.0)
                    .carriedForwardLeaves(0.0).unpaidLeaves(0.0).cycle(payroll.getMonth() <= 6 ? 1 : 2).build();
        }

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
                .accountantApproved(payroll.getAccountantApproved())
                .directorApproved(payroll.getDirectorApproved())
                .presentDays(presentDays)
                .absentDays(absentDays)
                .leaveDays((int) leaveDays)
                .paidLeaveDays(paidLeaveDays)
                .unpaidLeaveDays(unpaidLeaveDays)
                .halfDays(halfDays)
                .absentLeaveDeduction(absentLeaveDeduction)
                .unpaidLeaveDeduction(payroll.getUnpaidLeaveDeduction())
                .absentPenaltyDeduction(payroll.getAbsentDeduction())
                .leaveBalance(leaveBalanceInfo)
                .inProbation(inProbation)
                .probationStatus(probationStatus)
                .probationMonths(probationMonths)
                .joinDate(employee.getJoiningDate())
                .probationCompletionDate(probationCompletionDate)
                .build();
    }

    public PayslipDTO generatePayslipForEmployee(Long employeeId, String monthYear) {
        return payslipService.generatePayslip(employeeId, monthYear);
    }


}
