package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.PayrollDTO;
import com.hrm.hrmsystem.dto.PayslipDTO;
import com.hrm.hrmsystem.engine.AttendanceSummary;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Payroll;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import com.hrm.hrmsystem.repository.PayrollRepository;
import com.hrm.hrmsystem.repository.LeaveRepositoryCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private final LeaveCalculationService leaveCalculationService;
    private final LeaveService leaveService;
    private final LeaveBalanceService leaveBalanceService;
    private final UnifiedCalculationService unifiedCalculationService;
    private final LeaveRepositoryCustom leaveRepositoryCustom;

    public PayrollService(PayrollRepository payrollRepository,
                          EmployeeRepository employeeRepository,
                          LeaveRepository leaveRepository,
                          PayslipService payslipService,
                          LeaveCalculationService leaveCalculationService,
                          LeaveService leaveService,
                          LeaveBalanceService leaveBalanceService,
                          UnifiedCalculationService unifiedCalculationService,
                          LeaveRepositoryCustom leaveRepositoryCustom) {
        this.payrollRepository = payrollRepository;
        this.employeeRepository = employeeRepository;
        this.leaveRepository = leaveRepository;
        this.payslipService = payslipService;
        this.leaveCalculationService = leaveCalculationService;
        this.leaveService = leaveService;
        this.leaveBalanceService = leaveBalanceService;
        this.unifiedCalculationService = unifiedCalculationService;
        this.leaveRepositoryCustom = leaveRepositoryCustom;
    }

    @Transactional
    public PayrollDTO generatePayroll(Long employeeId, Integer month, Integer year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Optional<Payroll> existing = payrollRepository
                .findFirstByEmployeeIdAndMonthAndYearOrderByIdDesc(employeeId, month, year);
        Payroll payroll = existing.orElseGet(Payroll::new);
        Payroll.PayrollStatus statusToUse = existing.map(Payroll::getStatus).orElse(Payroll.PayrollStatus.PENDING);
        java.time.LocalDate paymentDateToUse = existing.map(Payroll::getPaymentDate).orElse(null);

        // 🔥 DATABASE-FIRST: Use direct database SUM for leave (single source of truth)
        // Present/Absent from AttendanceEngine, Leave from Database
        AttendanceSummary summary = unifiedCalculationService.calculateForPayroll(employeeId, year, month);
        
        double presentDays = summary.present;
        double absentDays = summary.absent;
        
        // ✅ DATABASE TRUTH: Direct SUM from leaves table - CORRECT MONTH FILTERING
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        double paidLeaveDays = leaveRepositoryCustom.getMonthlyPaidLeaveDays(employeeId, monthStart, monthEnd);
        double unpaidLeaveDays = leaveRepositoryCustom.getMonthlyUnpaidLeaveDays(employeeId, monthStart, monthEnd);
        
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
        
        BigDecimal totalSalary = employee.getSalary() != null
                ? employee.getSalary()
                : basicSalary.add(hra).add(da).add(otherAllowance);

        BigDecimal perDayBasic = basicSalary.compareTo(BigDecimal.ZERO) > 0
                ? basicSalary.divide(new BigDecimal("26"), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 🔥 CRITICAL FIX: absentDays already includes unpaidLeaveDays from AttendanceEngine
        // So we only deduct once for total absent days (which includes unpaid leave)
        BigDecimal totalAttendanceDeduction = perDayBasic.multiply(BigDecimal.valueOf(absentDays))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        log.info("Salary Calculation - Basic: {}, HRA: {}, DA: {}, Other: {}, Total: {}",
                 basicSalary, hra, da, otherAllowance, totalSalary);
        log.info("Attendance Deduction - Total Absent Days (includes unpaid leave): {}, Amount: {}",
                 absentDays, totalAttendanceDeduction);

        BigDecimal ta = BigDecimal.ZERO;

        BigDecimal providentFund = employee.getPf() != null
                ? employee.getPf()
                : totalSalary.multiply(new BigDecimal("0.12")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal tax = employee.getTax() != null
                ? employee.getTax()
                : calculateTax(totalSalary);
        BigDecimal insurance = BigDecimal.ZERO;
        if (employee.getInsurancePercentage() != null && employee.getInsurancePercentage() > 0) {
            insurance = basicSalary.multiply(BigDecimal.valueOf(employee.getInsurancePercentage()))
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }
        BigDecimal otherDeductions = totalAttendanceDeduction;

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
        // STORE attendance values from AttendanceEngine (single source of truth)
        payroll.setPresentDays(presentDays);
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
        List<Employee> employees = employeeRepository.findByStatus(Employee.EmployeeStatus.ACTIVE);

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

        log.info("Payroll {} approved successfully", payrollId);
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
        return payrollRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<PayrollDTO> getPayrollByMonth(Integer month, Integer year) {
        List<Employee> allActive = employeeRepository.findByStatus(Employee.EmployeeStatus.ACTIVE);

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
                .sorted((a, b) -> a.getEmployee().getFirstName().compareToIgnoreCase(b.getEmployee().getFirstName()))
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<PayrollDTO> getAllPayrolls() {
        return payrollRepository.findAll()
                .stream()
                .filter(p -> p.getEmployee() != null &&
                        p.getEmployee().getStatus() == Employee.EmployeeStatus.ACTIVE)
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

    private boolean checkProbationStatus(Employee employee, int year, int month) {
        if (employee.getJoiningDate() == null || employee.getProbationPeriodMonths() == null) {
            return false;
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

        // USE STORED attendance values from Payroll entity (set during generation)
        // These come from AttendanceEngine - single source of truth
        double presentDays = payroll.getPresentDays() != null ? payroll.getPresentDays() : 0.0;
        double absentDays = payroll.getAbsentDays() != null ? payroll.getAbsentDays() : 0.0;
        double paidLeaveDays = payroll.getPaidLeaveDays() != null ? payroll.getPaidLeaveDays() : 0.0;
        double unpaidLeaveDays = payroll.getUnpaidLeaveDays() != null ? payroll.getUnpaidLeaveDays() : 0.0;
        int leaveDays = (int) (paidLeaveDays + unpaidLeaveDays);
        int halfDays = 0; // Half days are already accounted for in present/absent counts
        
        double absentLeaveDeduction = payroll.getOtherDeductions() != null 
                ? payroll.getOtherDeductions().doubleValue() 
                : 0.0;

        // Get leave balance
        PayrollDTO.LeaveBalanceInfo leaveBalanceInfo;
        Boolean inProbation = checkProbationStatus(employee, payroll.getYear(), payroll.getMonth());
        String probationStatus = inProbation ? "In Progress" : "Completed";
        Integer probationMonths = employee.getProbationPeriodMonths() != null ? employee.getProbationPeriodMonths() : 3;
        
        LocalDate probationCompletionDate = null;
        if (employee.getJoiningDate() != null) {
            probationCompletionDate = employee.getJoiningDate().plusMonths(probationMonths);
        }
        
        try {
            var leaveStats = leaveCalculationService.calculateLeaveStatistics(employee.getId(), payroll.getMonth(), payroll.getYear());
            int currentCycle = payroll.getMonth() <= 6 ? 1 : 2;
            leaveBalanceInfo = PayrollDTO.LeaveBalanceInfo.builder()
                    .totalEarnedLeaves(leaveStats.getTotalEarnedLeaves())
                    .usedLeaves(leaveStats.getTotalUsedPaidLeaves())
                    .availableLeaves(leaveStats.getAvailableLeaveBalance())
                    .carriedForwardLeaves(0.0)
                    .unpaidLeaves(0.0)
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
                .presentDays(presentDays)
                .absentDays(absentDays)
                .leaveDays(leaveDays)
                .paidLeaveDays(paidLeaveDays)
                .unpaidLeaveDays(unpaidLeaveDays)
                .halfDays(halfDays)
                .absentLeaveDeduction(absentLeaveDeduction)
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
