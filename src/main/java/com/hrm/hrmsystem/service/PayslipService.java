package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.AttendanceDTO;
import com.hrm.hrmsystem.dto.PayslipDTO;
import com.hrm.hrmsystem.service.LeaveCalculationService.LeaveStatistics;
import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.model.Payroll;
import com.hrm.hrmsystem.entity.Payslip;
import com.hrm.hrmsystem.engine.AttendanceEngine;
import com.hrm.hrmsystem.engine.AttendanceSummary;
import com.hrm.hrmsystem.exception.ResourceNotFoundException;
import com.hrm.hrmsystem.exception.BadRequestException;
import com.hrm.hrmsystem.repository.PayslipRepository;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.AttendanceRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import com.hrm.hrmsystem.repository.PayrollRepository;
import jakarta.persistence.EntityManager;
import com.hrm.hrmsystem.util.PdfGeneratorUtil;
import com.hrm.hrmsystem.util.EmailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class PayslipService {

    private static final Logger log = LoggerFactory.getLogger(PayslipService.class);

    @Autowired
    private PayslipRepository payslipRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    @Lazy
    private AttendanceService attendanceService;

    @Autowired
    private LeaveRepository leaveRepository;

    @Autowired
    private UnifiedCalculationService unifiedCalculationService;

    @Autowired
    private LeaveCalculationService leaveCalculationService;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private PdfGeneratorUtil pdfGeneratorUtil;

    @Autowired
    private EmailUtil emailUtil;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private DatabaseLeaveService databaseLeaveService;

    /**
     * Generate payslip for a single employee (always regenerate with fresh data)
     * Uses REQUIRES_NEW to run in a separate transaction so it can see committed data
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PayslipDTO generatePayslip(Long employeeId, String monthYear) {
        log.info("Generating payslip for employee: {} for month: {}", employeeId, monthYear);
        
        try {
            // Fetch employee - use findById to get fresh data from database
            log.info("Step 1: Fetching employee...");
            Employee employee = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));
            log.info("Step 1: Employee found: {} {}", employee.getFirstName(), employee.getLastName());

            // Extract month and year
            log.info("Step 2: Parsing month/year...");
            String[] parts = monthYear.split("-");
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[0]);
            log.info("Step 2: Parsed month={}, year={}", month, year);

            // Check if payslip already exists - if yes, delete ALL duplicates to regenerate with fresh data
            log.info("Step 3: Checking for existing payslips...");
            java.util.List<Payslip> existingPayslips = payslipRepository.findAllByEmployeeIdAndMonthYear(employeeId, monthYear);
            log.info("Step 3: Found {} existing payslips", existingPayslips != null ? existingPayslips.size() : 0);
            
            if (existingPayslips != null && !existingPayslips.isEmpty()) {
                log.info("Deleting {} existing payslip(s) for {}-{} to regenerate with fresh data", 
                         existingPayslips.size(), employeeId, monthYear);
                // Delete all duplicate payslips
                for (int i = 0; i < existingPayslips.size(); i++) {
                    Payslip existingPayslip = existingPayslips.get(i);
                    payslipRepository.delete(existingPayslip);
                }
            }

            // Create new payslip
            log.info("Step 4: Creating new payslip...");
            Payslip payslip = new Payslip();
            payslip.setEmployee(employee);
            payslip.setMonthYear(monthYear);
            payslip.setGeneratedDate(LocalDate.now());
            payslip.setStatus(Payslip.PayslipStatus.GENERATED);
            payslip.setSalaryMonth(month);
            payslip.setSalaryYear(year);
            log.info("Step 4: Payslip object created");

            // Calculate attendance for the month (with fresh attendance data)
            log.info("Step 5: Calculating attendance...");
            calculateAttendance(payslip, month, year);
            log.info("Step 5: Attendance calculated");

            // Calculate salary with attendance & leave policy based on probation status
            log.info("Step 6: Calculating salary...");
            calculateSalary(payslip, employee, monthYear);
            log.info("Step 6: Salary calculated");

            // Save payslip
            log.info("Step 7: Saving payslip...");
            Payslip savedPayslip = payslipRepository.save(payslip);
            log.info("Payslip generated successfully with id: {}", savedPayslip.getId());

            return convertToDTO(savedPayslip);
        } catch (Exception e) {
            log.error("ERROR in generatePayslip at step: employee={}, monthYear={}", employeeId, monthYear, e);
            throw new RuntimeException("Failed to generate payslip: " + e.getMessage(), e);
        }
    }

    /**
     * Clear all payslips for a month and regenerate them with fresh attendance/leave data.
     */
    public List<PayslipDTO> regeneratePayslipsForMonth(String monthYear) {
        log.info("Regenerating all payslips for month: {}", monthYear);

        payslipRepository.deleteByMonthYear(monthYear);

        List<Employee> employees = employeeRepository.findByStatus(Employee.EmployeeStatus.ACTIVE);
        return employees.stream()
                .map(emp -> {
                    try {
                        return generatePayslip(emp.getId(), monthYear);
                    } catch (Exception e) {
                        log.warn("Could not regenerate payslip for employee {} month {}: {}", emp.getId(), monthYear, e.getMessage());
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    /**
     * Calculate attendance for the month using LeaveRepository for leave statistics (source of truth)
     * AttendanceEngine is used ONLY for present/absent calculation, NOT for leave
     */
    private void calculateAttendance(Payslip payslip, int month, int year) {
        // Clear entity manager cache to get fresh attendance data
        entityManager.clear();

        Long employeeId = payslip.getEmployee().getId();
        log.info("Calculating attendance for employee: {} for {}-{}", employeeId, year, month);

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        // ✅ DATABASE-FIRST: Use DatabaseLeaveService for leave statistics (SOURCE OF TRUTH)
        double paidLeaveDays = databaseLeaveService.getPaidLeaveDays(employeeId, year, month);
        double unpaidLeaveDays = databaseLeaveService.getUnpaidLeaveDays(employeeId, year, month);

        log.info("LeaveRepository Result for employee {} in {}-{}: paidLeave={}, unpaidLeave={}",
                 employeeId, year, month, paidLeaveDays, unpaidLeaveDays);

        // ✅ Use AttendanceEngine ONLY for attendance (present/absent), NOT for leave
        AttendanceSummary summary = unifiedCalculationService.calculateForPayroll(employeeId, year, month);

        double presentDays = summary.present;
        double absentDays = summary.absent;

        log.info("AttendanceEngine Result for employee {} in {}-{}: present={}, absent={}",
                 employeeId, year, month, presentDays, absentDays);

        // Get absent dates for record
        List<AttendanceDTO> attendanceDTOList = attendanceService.getAttendanceReport(employeeId, monthStart, monthEnd);
        java.util.List<String> absentDates = attendanceDTOList.stream()
                .filter(a -> "ABSENT".equals(a.getStatus()))
                .map(a -> a.getDate().toString())
                .collect(Collectors.toList());

        // Set the calculated values on the payslip
        payslip.setPresentDays(presentDays);
        payslip.setAbsentDays(absentDays);
        payslip.setLeaveDays(paidLeaveDays + unpaidLeaveDays);
        payslip.setPaidLeaveDays(paidLeaveDays);
        payslip.setUnpaidLeaveDays(unpaidLeaveDays);
        payslip.setHalfDays(0); // Half days are already handled in present/absent counts
        payslip.setWorkingDays(AttendanceEngine.WORKING_DAYS_PER_MONTH);

        // HRMS STANDARD: Keep metrics separate
        // presentDays = actual physical attendance
        // leaveDays = paid + unpaid leave
        // totalDays = presentDays + absentDays + leaveDays
        payslip.setTotalDays((int) (payslip.getPresentDays() + payslip.getAbsentDays() + payslip.getLeaveDays()));
        payslip.setAbsentDates(absentDates);
    }

    /**
     * Calculate salary components based on attendance and leave policy
     * - Salary calculated based on 26 working days
     * - Probation: No paid leave, all leaves/absences deducted
     * - Post-probation: 1.5 paid leave per month,
     * - Leave cycles: Jan-Jun, Jul-Dec with expiry
     */
    private void calculateSalary(Payslip payslip, Employee employee, String monthYear) {
        log.info("Calculating salary for employee: {} for month: {}", employee.getId(), monthYear);

        // Parse month and year from monthYear (format: "2026-04" or "April 2026")
        int year = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();
        try {
            if (monthYear != null && monthYear.contains("-")) {
                String[] parts = monthYear.split("-");
                year = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]);
            }
        } catch (Exception e) {
            log.warn("Could not parse monthYear: {}, using current date", monthYear);
        }

        // Check probation status
        boolean isInProbation = checkProbationStatus(employee, year, month);
        
        // Calculate leave cycle info
        int currentCycle = month <= 6 ? 1 : 2;
        log.info("Employee {} is {} probation. Leave cycle: {} for {}/{}", 
                employee.getId(), isInProbation ? "in" : "completed", currentCycle, month, year);

        // Try to get payroll data first - payslip should match payroll exactly
        Optional<Payroll> payrollOpt = payrollRepository.findFirstByEmployeeIdAndMonthAndYearOrderByIdDesc(employee.getId(), month, year);
        
        BigDecimal basicSalary = BigDecimal.ZERO, hra = BigDecimal.ZERO, da = BigDecimal.ZERO, otherAllowance = BigDecimal.ZERO, grossSalary = BigDecimal.ZERO;
        BigDecimal pf = BigDecimal.ZERO, esi = BigDecimal.ZERO, incomeTax = BigDecimal.ZERO, insurance = BigDecimal.ZERO, totalDeduction = BigDecimal.ZERO;
        BigDecimal absentLeaveDeduction = BigDecimal.ZERO;
        BigDecimal unpaidLeaveDeduction = BigDecimal.ZERO;
        String calculationMessage = "";

        // Calculate per day salary (based on 26 working days in month)
        double perDaySalary = 0.0;
        double absentDaysCount = 0.0;
        
        Payroll payroll = null;
        if (payrollOpt.isPresent()) {
            payroll = payrollOpt.get();
            log.info("Found payroll for employee {} month {}-{}: ID={}", 
                    employee.getId(), year, month, payroll.getId());
                        
            // Use payroll data directly - NO recalculation
            basicSalary = payroll.getBasicSalary() != null ? payroll.getBasicSalary() : BigDecimal.ZERO;
            hra = payroll.getHra() != null ? payroll.getHra() : BigDecimal.ZERO;
            da = payroll.getDa() != null ? payroll.getDa() : BigDecimal.ZERO;
            otherAllowance = payroll.getOtherAllowances() != null ? payroll.getOtherAllowances() : BigDecimal.ZERO;
            grossSalary = payroll.getGrossSalary() != null ? payroll.getGrossSalary() : BigDecimal.ZERO;
        
            // Calculate per day salary for debug logging
            perDaySalary = basicSalary.doubleValue() / AttendanceEngine.WORKING_DAYS_PER_MONTH;
            
            pf = payroll.getProvidentFund() != null ? payroll.getProvidentFund() : BigDecimal.ZERO;
            esi = BigDecimal.ZERO;
            incomeTax = payroll.getTax() != null ? payroll.getTax() : BigDecimal.ZERO;
            insurance = payroll.getInsurance() != null ? payroll.getInsurance() : BigDecimal.ZERO;
            
            // Total deductions from payroll ALREADY includes otherDeductions (absent/leave)
            totalDeduction = payroll.getTotalDeductions() != null ? payroll.getTotalDeductions() : BigDecimal.ZERO;
            absentLeaveDeduction = payroll.getOtherDeductions() != null ? payroll.getOtherDeductions() : BigDecimal.ZERO;
            
            calculationMessage = "Calculated from Payroll ID: " + payroll.getId();
        } else {
            // Fallback: Calculate from AttendanceEngine (single source of truth)
            log.warn("No payroll found for employee {} month {}-{}, using AttendanceEngine for calculation",
                    employee.getId(), year, month);

            // Initialize fallback values
            basicSalary = BigDecimal.ZERO;
            hra = BigDecimal.ZERO;
            da = BigDecimal.ZERO;
            otherAllowance = BigDecimal.ZERO;
            grossSalary = BigDecimal.ZERO;
            pf = BigDecimal.ZERO;
            esi = BigDecimal.ZERO;
            incomeTax = BigDecimal.ZERO;
            insurance = BigDecimal.ZERO;
            totalDeduction = BigDecimal.ZERO;
            
            // Get attendance summary from service
            AttendanceSummary attendanceSummary =
                    unifiedCalculationService.calculateForPayroll(employee.getId(), year, month);

            // Use UnifiedCalculationService for salary calculation
            AttendanceEngine.SalarySummary salarySummary =
                    unifiedCalculationService.calculateSalary(employee, attendanceSummary, isInProbation);

            // Extract ONLY attendance-related values from engine result
            absentLeaveDeduction = salarySummary.getAbsentLeaveDeduction();
            unpaidLeaveDeduction = salarySummary.getUnpaidLeaveDeduction();
            absentDaysCount = salarySummary.getAbsentDays();
            double unpaidLeaveDaysCount = salarySummary.getUnpaidLeaveDays();

            log.info("ENGINE CALCULATION - Absent={}, AbsentDeduction={}, UnpaidDeduction={}",
                    absentDaysCount, absentLeaveDeduction, unpaidLeaveDeduction);

            calculationMessage = "Calculated from AttendanceEngine (single source of truth)";
        }

        // Calculate net salary BEFORE setting to payslip
        BigDecimal netSalary;
        if (payrollOpt.isPresent()) {
            // Use payroll net salary if available
            netSalary = payroll.getNetSalary();
            if (netSalary == null) {
                netSalary = grossSalary.subtract(totalDeduction);
                log.warn("Payroll net salary was null, calculated: {}", netSalary);
            }
            payslip.setTotalDeduction(totalDeduction);
        } else {
            // No payroll - calculate net salary
            // STEP 6: FIX DOUBLE DEDUCTION - totalDeduction already includes absentLeaveDeduction
            payslip.setTotalDeduction(totalDeduction);
            netSalary = grossSalary.subtract(totalDeduction);

            // 🔥 CRITICAL: Prevent negative salary (should never happen in real payroll)
            if (netSalary.compareTo(BigDecimal.ZERO) < 0) {
                log.error("NEGATIVE SALARY DETECTED! Basic={}, PerDay={}, UnpaidLeaves={}, LeaveDeduction={}, TotalDed={}, Net={}",
                         basicSalary, perDaySalary, absentDaysCount, absentLeaveDeduction, payslip.getTotalDeduction(), netSalary);
                netSalary = BigDecimal.ZERO;
                log.warn("Setting net salary to 0 to prevent negative salary");
            }

            log.info("SALARY DEBUG - Basic={}, PerDay={}, UnpaidLeaves={}, LeaveDeduction={}, TotalDed={}, Net={}",
                    basicSalary, perDaySalary, absentDaysCount, absentLeaveDeduction, payslip.getTotalDeduction(), netSalary);
        }
        
        // Set all values to payslip
        payslip.setBasicSalary(basicSalary);
        payslip.setDa(da);
        payslip.setHra(hra);
        payslip.setOtherAllowance(otherAllowance);
        payslip.setGrossSalary(grossSalary);
        payslip.setPf(pf);
        payslip.setEsi(esi);
        payslip.setIncomeTax(incomeTax);
        payslip.setOtherDeduction(insurance);
        payslip.setAbsentLeaveDeduction(absentLeaveDeduction);
        payslip.setUnpaidLeaveDeduction(unpaidLeaveDeduction);
        payslip.setNetSalary(netSalary);
        
        log.info("DEBUG - After setting: NetSalary from payslip={}", payslip.getNetSalary());
        log.info("Payslip calculation complete: Gross={}, TotalDeduction={}, AbsentDeduction={}, Net={}",
                grossSalary, totalDeduction, absentLeaveDeduction, netSalary);
        log.info(calculationMessage);
    }

    /**
     * Check if employee is in probation period for given month/year
     */
    private boolean checkProbationStatus(Employee employee, int year, int month) {
        return com.hrm.hrmsystem.util.ProbationUtil.isInProbation(employee, LocalDate.of(year, month, 1));
    }

    private BigDecimal calculatePayrollTax(BigDecimal basicSalary) {
        BigDecimal annualSalary = basicSalary.multiply(new BigDecimal("12"));
        if (annualSalary.compareTo(new BigDecimal("1000000")) > 0) {
            return basicSalary.multiply(new BigDecimal("0.30"));
        } else if (annualSalary.compareTo(new BigDecimal("500000")) > 0) {
            return basicSalary.multiply(new BigDecimal("0.20"));
        } else if (annualSalary.compareTo(new BigDecimal("250000")) > 0) {
            return basicSalary.multiply(new BigDecimal("0.10"));
        }
        return BigDecimal.ZERO;
    }

    /**
     * Get present days for employee in a month/year
     * Uses AttendanceEngine for consistent calculation with payroll
     */
    public double getPresentDaysForEmployee(Long employeeId, Integer month, Integer year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        
        // Get opening balance from previous month
        double openingBalance = 0;
        if (month > 1) {
            LeaveBalanceService.LeaveBalanceResult prevBalance = 
                leaveBalanceService.calculate(employeeId, YearMonth.of(year, month - 1));
            openingBalance = prevBalance.remaining;
        } else if (year > 2024) {
            LeaveBalanceService.LeaveBalanceResult prevBalance = 
                leaveBalanceService.calculate(employeeId, YearMonth.of(year - 1, 12));
            openingBalance = prevBalance.remaining;
        }
        
        // Use UnifiedCalculationService - SINGLE SOURCE OF TRUTH
        AttendanceSummary summary = unifiedCalculationService.calculateForPayroll(employeeId, year, month);
        return summary.present;
    }

    /**
     * Get absent days for employee in a month/year
     * Uses AttendanceEngine for consistent calculation with payroll
     */
    public double getAbsentDaysForEmployee(Long employeeId, Integer month, Integer year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        
        // Get opening balance from previous month
        double openingBalance = 0;
        if (month > 1) {
            LeaveBalanceService.LeaveBalanceResult prevBalance = 
                leaveBalanceService.calculate(employeeId, YearMonth.of(year, month - 1));
            openingBalance = prevBalance.remaining;
        } else if (year > 2024) {
            LeaveBalanceService.LeaveBalanceResult prevBalance = 
                leaveBalanceService.calculate(employeeId, YearMonth.of(year - 1, 12));
            openingBalance = prevBalance.remaining;
        }
        
        // Use UnifiedCalculationService - SINGLE SOURCE OF TRUTH
        AttendanceSummary summary = unifiedCalculationService.calculateForPayroll(employeeId, year, month);
        return summary.absent;
    }
    
    /**
     * Get leave days for employee in a month/year
     */
    public int getLeaveDaysForEmployee(Long employeeId, Integer month, Integer year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<Leave> leaves = leaveRepository.findByEmployeeIdAndStatus(
            employeeId, Leave.LeaveStatus.APPROVED);
        
        long leaveDays = leaves.stream()
            .filter(leave -> {
                LocalDate lStart = leave.getStartDate();
                LocalDate lEnd = leave.getEndDate();
                
                // Check if leave overlaps with the month
                return !lEnd.isBefore(startDate) && !lStart.isAfter(endDate);
            })
            .mapToLong(leave -> {
                LocalDate lStart = leave.getStartDate();
                LocalDate lEnd = leave.getEndDate();
                LocalDate overlapStart = lStart.isBefore(startDate) ? startDate : lStart;
                LocalDate overlapEnd = lEnd.isAfter(endDate) ? endDate : lEnd;
                return java.time.temporal.ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
            })
            .sum();
        
        log.debug("Leave days for employee {} in {}/{}: {}", employeeId, month, year, leaveDays);
        return (int) leaveDays;
    }

    /**
     * Get half days for employee in a month/year (each counts as 0.5 for deduction)
     */
    public int getHalfDaysForEmployee(Long employeeId, Integer month, Integer year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndDateBetween(
                employeeId, startDate, endDate);

        long halfDays = attendances.stream()
                .filter(a -> a.getStatus() != null && Attendance.AttendanceStatus.HALF_DAY.equals(a.getStatus()))
                .count();

        log.debug("Half days for employee {} in {}/{}: {}", employeeId, month, year, halfDays);
        return (int) halfDays;
    }

    /**
     * Get deductible days: max(0, absent + leave + halfDay*0.5 - 1.5)
     * First 1.5 days free per month, then deduct
     */
    public double getDeductibleDays(int absentDays, int leaveDays, int halfDays) {
        double total = absentDays + leaveDays + (halfDays * 0.5);
        return Math.max(0, total - 1.5);
    }

    /**
     * Get payslip by ID
     */
    public PayslipDTO getPayslipById(Long payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + payslipId));
        return convertToDTO(payslip);
    }

    /**
     * Get payslip by employee and month/year
     */
    public PayslipDTO getPayslipByEmployeeAndMonth(Long employeeId, String monthYear) {
        // Handle duplicate payslips by getting all and returning the most recent
        java.util.List<Payslip> payslips = payslipRepository.findAllByEmployeeIdAndMonthYear(employeeId, monthYear);
        if (payslips == null || payslips.isEmpty()) {
            throw new ResourceNotFoundException("Payslip not found for employee " + employeeId + " and month " + monthYear);
        }
        // Sort by ID descending to get the most recent one
        payslips.sort((a, b) -> b.getId().compareTo(a.getId()));
        Payslip payslip = payslips.get(0);
        if (payslips.size() > 1) {
            log.warn("Found {} duplicate payslips for employee {} month {}, using most recent (ID: {})", 
                    payslips.size(), employeeId, monthYear, payslip.getId());
        }
        return convertToDTO(payslip);
    }

    /**
     * Update payslip status by employee and month/year
     */
    public void updatePayslipStatusByEmployeeAndMonth(Long employeeId, String monthYear, String status, String approvedBy, String remarks) {
        // Handle duplicate payslips by getting all and updating the most recent
        java.util.List<Payslip> payslips = payslipRepository.findAllByEmployeeIdAndMonthYear(employeeId, monthYear);
        if (payslips == null || payslips.isEmpty()) {
            log.warn("Payslip not found for employee {} and month {} to update status", employeeId, monthYear);
            return;
        }
        // Sort by ID descending to get the most recent one
        payslips.sort((a, b) -> b.getId().compareTo(a.getId()));
        Payslip payslip = payslips.get(0);
        if (payslips.size() > 1) {
            log.warn("Found {} duplicate payslips for employee {} month {}, updating most recent (ID: {})", 
                    payslips.size(), employeeId, monthYear, payslip.getId());
        }
            try {
                Payslip.PayslipStatus newStatus = Payslip.PayslipStatus.valueOf(status);
                payslip.setStatus(newStatus);
                if (approvedBy != null) payslip.setApprovedBy(approvedBy);
                if (remarks != null) payslip.setRemarks(remarks);
                if (status.equals("APPROVED") || status.equals("PAID")) {
                    payslip.setApprovedDate(LocalDate.now());
                }
                payslipRepository.save(payslip);
                log.info("Updated payslip {} status to {}", payslip.getId(), status);
            } catch (IllegalArgumentException e) {
                log.error("Invalid payslip status: {}. Valid values are: DRAFT, GENERATED, APPROVED, SENT, REJECTED", status);
                throw new RuntimeException("Invalid payslip status: " + status, e);
            }
    }

    /**
     * Get all payslips
     */
    public List<PayslipDTO> getAllPayslips() {
        log.info("Fetching all payslips");
        List<Payslip> payslips = payslipRepository.findAll();
        return payslips.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Get all payslips for an employee
     */
    public List<PayslipDTO> getPayslipsByEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        List<Payslip> payslips = payslipRepository.findByEmployeeOrderByMonthYearDesc(employee);
        return payslips.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Get payslips for a specific month
     */
    public List<PayslipDTO> getPayslipsByMonth(String monthYear) {
        // Rule: show payslip on UI only if payroll is PAID for that month/year.
        // For unpaid employees, still return employee details but all salary/deduction values as 0.
        String[] parts = monthYear.split("-");
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[0]);

        List<Employee> employees = employeeRepository.findByStatus(Employee.EmployeeStatus.ACTIVE);

        // Existing payslips for this month/year (may be missing for some employees)
        List<Payslip> existingPayslips = payslipRepository.findByMonthYear(monthYear);
        Map<Long, Payslip> payslipByEmployeeId = new HashMap<>();
        if (existingPayslips != null) {
            for (Payslip p : existingPayslips) {
                if (p != null && p.getEmployee() != null) {
                    payslipByEmployeeId.put(p.getEmployee().getId(), p);
                }
            }
        }

        List<PayslipDTO> result = new ArrayList<>();
        for (Employee employee : employees) {
            if (employee == null) continue;

            java.util.Optional<com.hrm.hrmsystem.model.Payroll> payrollOpt =
                    payrollRepository.findFirstByEmployeeIdAndMonthAndYearOrderByIdDesc(employee.getId(), month, year);

            String payrollStatus = payrollOpt.map(p -> p.getStatus().name()).orElse(null);
            boolean isPaid = payrollOpt.isPresent() && payrollOpt.get().getStatus() == com.hrm.hrmsystem.model.Payroll.PayrollStatus.PAID;

            if (isPaid) {
                // Always regenerate for PAID payroll so values are guaranteed in-sync with latest payroll formula.
                PayslipDTO dto = generatePayslip(employee.getId(), monthYear);
                dto.setPayrollStatus("PAID");
                result.add(dto);
            } else {
                // Unpaid: show employee details but zero everything.
                PayslipDTO dto = new PayslipDTO();
                dto.setId(employee.getId()); // unique key for UI lists
                dto.setEmployeeId(employee.getId());
                dto.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());
                dto.setMonthYear(monthYear);
                dto.setPayrollStatus(payrollStatus != null ? payrollStatus : "UNPAID");

                dto.setBasicSalary(BigDecimal.ZERO);
                dto.setGrossSalary(BigDecimal.ZERO);
                dto.setNetSalary(BigDecimal.ZERO);
                dto.setPf(BigDecimal.ZERO);
                dto.setEsi(BigDecimal.ZERO);
                dto.setIncomeTax(BigDecimal.ZERO);
                dto.setOtherDeduction(BigDecimal.ZERO);
                dto.setTotalDeduction(BigDecimal.ZERO);
                dto.setAbsentLeaveDeduction(BigDecimal.ZERO);

                dto.setPresentDays(0.0);
                dto.setAbsentDays(0.0);
                dto.setLeaveDays(0.0);
                dto.setHalfDays(0);
                dto.setWorkingDays(AttendanceEngine.WORKING_DAYS_PER_MONTH);
                dto.setTotalDays(30);
                dto.setAbsentDates(new ArrayList<>());

                dto.setStatus("UNPAID");
                result.add(dto);
            }
        }

        return result.stream()
                .sorted((a, b) -> {
                    String an = a.getEmployeeName() == null ? "" : a.getEmployeeName();
                    String bn = b.getEmployeeName() == null ? "" : b.getEmployeeName();
                    return an.compareToIgnoreCase(bn);
                })
                .collect(Collectors.toList());
    }

    /**
     * Approve payslip
     */
    public PayslipDTO approvePayslip(Long payslipId, String approvedBy, String remarks) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + payslipId));

        if (!payslip.getStatus().equals(Payslip.PayslipStatus.GENERATED)) {
            throw new BadRequestException("Only GENERATED payslips can be approved");
        }

        payslip.setStatus(Payslip.PayslipStatus.APPROVED);
        payslip.setApprovedBy(approvedBy);
        payslip.setApprovedDate(LocalDate.now());
        payslip.setRemarks(remarks);

        Payslip updatedPayslip = payslipRepository.save(payslip);
        log.info("Payslip {} approved by {}", payslipId, approvedBy);

        return convertToDTO(updatedPayslip);
    }

    /**
     * Reject payslip
     */
    public PayslipDTO rejectPayslip(Long payslipId, String rejectionReason) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + payslipId));

        payslip.setStatus(Payslip.PayslipStatus.REJECTED);
        payslip.setRemarks(rejectionReason);

        Payslip updatedPayslip = payslipRepository.save(payslip);
        log.info("Payslip {} rejected", payslipId);

        return convertToDTO(updatedPayslip);
    }

    /**
     * Update payslip calculations (for admin to modify before approval)
     */
    public PayslipDTO updatePayslip(Long payslipId, Map<String, Object> updates) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + payslipId));

        // Only allow updates for DRAFT or GENERATED payslips
        if (!payslip.getStatus().equals(Payslip.PayslipStatus.GENERATED) &&
            !payslip.getStatus().equals(Payslip.PayslipStatus.DRAFT)) {
            throw new BadRequestException("Cannot update payslip that is already " + payslip.getStatus());
        }

        // Update fields if provided
        if (updates.containsKey("basicSalary")) payslip.setBasicSalary(BigDecimal.valueOf(((Number) updates.get("basicSalary")).doubleValue()));
        if (updates.containsKey("hra")) payslip.setHra(BigDecimal.valueOf(((Number) updates.get("hra")).doubleValue()));
        if (updates.containsKey("da")) payslip.setDa(BigDecimal.valueOf(((Number) updates.get("da")).doubleValue()));
        if (updates.containsKey("otherAllowance")) payslip.setOtherAllowance(BigDecimal.valueOf(((Number) updates.get("otherAllowance")).doubleValue()));
        if (updates.containsKey("pf")) payslip.setPf(BigDecimal.valueOf(((Number) updates.get("pf")).doubleValue()));
        if (updates.containsKey("esi")) payslip.setEsi(BigDecimal.valueOf(((Number) updates.get("esi")).doubleValue()));
        if (updates.containsKey("incomeTax")) payslip.setIncomeTax(BigDecimal.valueOf(((Number) updates.get("incomeTax")).doubleValue()));
        if (updates.containsKey("otherDeduction")) payslip.setOtherDeduction(BigDecimal.valueOf(((Number) updates.get("otherDeduction")).doubleValue()));

        // Recalculate totals
        BigDecimal grossSalary = payslip.getBasicSalary()
                .add(payslip.getHra() != null ? payslip.getHra() : BigDecimal.ZERO)
                .add(payslip.getDa() != null ? payslip.getDa() : BigDecimal.ZERO)
                .add(payslip.getOtherAllowance() != null ? payslip.getOtherAllowance() : BigDecimal.ZERO);

        BigDecimal totalDeduction = payslip.getPf()
                .add(payslip.getEsi() != null ? payslip.getEsi() : BigDecimal.ZERO)
                .add(payslip.getIncomeTax() != null ? payslip.getIncomeTax() : BigDecimal.ZERO)
                .add(payslip.getOtherDeduction() != null ? payslip.getOtherDeduction() : BigDecimal.ZERO)
                .add(payslip.getAbsentLeaveDeduction() != null ? payslip.getAbsentLeaveDeduction() : BigDecimal.ZERO);

        BigDecimal netSalary = grossSalary.subtract(totalDeduction);

        payslip.setGrossSalary(grossSalary);
        payslip.setTotalDeduction(totalDeduction);
        payslip.setNetSalary(netSalary);

        Payslip savedPayslip = payslipRepository.save(payslip);
        log.info("Payslip {} updated by admin", payslipId);

        return convertToDTO(savedPayslip);
    }

    /**
     * Generate PDF for payslip
     */
    public String generatePayslipPdf(Long payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + payslipId));

        try {
            String pdfPath = pdfGeneratorUtil.generatePayslipPdf(payslip);
            payslip.setPdfGenerated(true);
            payslip.setPdfFilePath(pdfPath);
            payslipRepository.save(payslip);
            log.info("PDF generated for payslip {} at path: {}", payslipId, pdfPath);
            return pdfPath;
        } catch (IOException e) {
            log.error("Error generating PDF for payslip {}: {}", payslipId, e.getMessage());
            throw new RuntimeException("Error generating PDF: " + e.getMessage());
        }
    }

    /**
     * Send payslip to employee via email
     */
    public void sendPayslipToEmployee(Long payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + payslipId));

        if (!payslip.getStatus().equals(Payslip.PayslipStatus.APPROVED)) {
            throw new BadRequestException("Only APPROVED payslips can be sent");
        }

        try {
            // Generate PDF if not already generated
            if (!payslip.getPdfGenerated()) {
                generatePayslipPdf(payslipId);
            }

            // Send email with payslip
            emailUtil.sendPayslipEmail(payslip);

            payslip.setStatus(Payslip.PayslipStatus.SENT);
            payslip.setSentDate(LocalDate.now());
            payslipRepository.save(payslip);

            log.info("Payslip {} sent to employee: {}", payslipId, payslip.getEmployee().getEmail());
        } catch (Exception e) {
            log.error("Error sending payslip {}: {}", payslipId, e.getMessage());
            throw new RuntimeException("Error sending payslip: " + e.getMessage());
        }
    }

    /**
     * Bulk generate payslips for all employees in a month
     */
    public List<PayslipDTO> bulkGeneratePayslips(String monthYear) {
        log.info("Bulk generating payslips for month: {}", monthYear);

        List<Employee> employees = employeeRepository.findAll();
        List<PayslipDTO> generatedPayslips = employees.stream()
                .filter(emp -> !payslipRepository.existsByEmployeeAndMonthYear(emp, monthYear))
                .map(emp -> {
                    try {
                        return generatePayslip(emp.getId(), monthYear);
                    } catch (Exception e) {
                        log.warn("Failed to generate payslip for employee {}: {}", emp.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(p -> p != null)
                .collect(Collectors.toList());

        log.info("Bulk generation completed. Generated {} payslips", generatedPayslips.size());
        return generatedPayslips;
    }

    /**
     * Get pending approvals
     */
    public List<PayslipDTO> getPendingApprovals() {
        List<Payslip> payslips = payslipRepository.findPendingApprovals();
        return payslips.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * Delete payslip permanently from database
     */
    public void deletePayslip(Long payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + payslipId));

        // Clear absent dates to avoid FK constraint
        if (payslip.getAbsentDates() != null) {
            payslip.getAbsentDates().clear();
        }
        payslipRepository.save(payslip);
        
        payslipRepository.delete(payslip);
        log.info("Payslip {} deleted permanently", payslipId);
    }

    /**
     * Convert Payslip entity to DTO
     */
    private PayslipDTO convertToDTO(Payslip payslip) {
        PayslipDTO dto = new PayslipDTO();
        Employee employee = payslip.getEmployee();
        dto.setId(payslip.getId());
        dto.setEmployeeId(employee.getId());
        dto.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());
        dto.setDepartment(employee.getDepartment() != null ? employee.getDepartment().getName() : null);
        dto.setDesignation(employee.getDesignation());
        dto.setEmail(employee.getEmail());
        dto.setPhone(employee.getPhone());
        dto.setJoinDate(employee.getJoiningDate());
        dto.setMonthYear(payslip.getMonthYear());
        dto.setBasicSalary(payslip.getBasicSalary());
        dto.setDa(payslip.getDa());
        dto.setHra(payslip.getHra());
        dto.setOtherAllowance(payslip.getOtherAllowance());
        dto.setGrossSalary(payslip.getGrossSalary());
        dto.setPf(payslip.getPf());
        dto.setEsi(payslip.getEsi());
        dto.setIncomeTax(payslip.getIncomeTax());
        dto.setOtherDeduction(payslip.getOtherDeduction());
        dto.setTotalDeduction(payslip.getTotalDeduction());
        dto.setNetSalary(payslip.getNetSalary());
        
        log.info("DEBUG - DTO values: PF={}, Tax={}, Insurance={}, TotalDed={}, Net={}", 
                 dto.getPf(), dto.getIncomeTax(), dto.getOtherDeduction(), dto.getTotalDeduction(), dto.getNetSalary());
        dto.setPresentDays(payslip.getPresentDays());
        dto.setAbsentDays(payslip.getAbsentDays());
        dto.setLeaveDays(payslip.getLeaveDays());
        dto.setPaidLeaveDays(payslip.getPaidLeaveDays());
        dto.setUnpaidLeaveDays(payslip.getUnpaidLeaveDays());
        dto.setHalfDays(payslip.getHalfDays());
        dto.setWorkingDays(payslip.getWorkingDays());
        dto.setTotalDays(payslip.getTotalDays());
        dto.setAbsentLeaveDeduction(payslip.getAbsentLeaveDeduction());
        dto.setStatus(payslip.getStatus().toString());
        dto.setGeneratedDate(payslip.getGeneratedDate());
        dto.setApprovedDate(payslip.getApprovedDate());
        dto.setApprovedBy(payslip.getApprovedBy());
        dto.setRemarks(payslip.getRemarks());
        dto.setPdfFilePath(payslip.getPdfFilePath());
        
        // Calculate and set probation status using CURRENT date (not payslip date)
        // This ensures the status reflects the current state, not historical state
        boolean currentlyInProbation = com.hrm.hrmsystem.util.ProbationUtil.isInProbation(employee, LocalDate.now());
        dto.setInProbation(currentlyInProbation);
        dto.setProbationStatus(currentlyInProbation ? "In Progress" : "Completed");
        dto.setProbationMonths(employee.getProbationPeriodMonths() != null ? employee.getProbationPeriodMonths() : 3);

        // Calculate probation completion date
        LocalDate probationCompletionDate = null;
        if (employee.getJoiningDate() != null) {
            Integer probationMonths = employee.getProbationPeriodMonths() != null ? employee.getProbationPeriodMonths() : 3;
            probationCompletionDate = employee.getJoiningDate().plusMonths(probationMonths);
        }
        dto.setProbationCompletionDate(probationCompletionDate);

        // 🔥 SINGLE SOURCE OF TRUTH: Get leave balance from AttendanceEngine ONLY
        PayslipDTO.LeaveBalanceInfo leaveBalanceInfo = new PayslipDTO.LeaveBalanceInfo();

        try {
            // Get payslip month/year for accurate leave balance calculation
            Integer payslipMonth = payslip.getSalaryMonth();
            Integer payslipYear = payslip.getSalaryYear();
            
            // Fallback to parsing monthYear if salaryMonth/salaryYear not set
            if (payslipMonth == null || payslipYear == null) {
                String monthYear = payslip.getMonthYear();
                if (monthYear != null && monthYear.contains(" ")) {
                    String[] parts = monthYear.split(" ");
                    payslipMonth = getMonthNumber(parts[0]);
                    payslipYear = Integer.parseInt(parts[1]);
                }
            }
            
            // Default to current date if still not available
            if (payslipMonth == null || payslipYear == null) {
                payslipMonth = LocalDate.now().getMonthValue();
                payslipYear = LocalDate.now().getYear();
            }
            
            if (!currentlyInProbation) {
                // ✅ ONE CALL TO SERVICE - ALL CALCULATIONS INSIDE SERVICE
                AttendanceEngine.LeaveBalanceSummary summary = 
                        unifiedCalculationService.calculateLeaveBalance(employee.getId(), payslipYear, payslipMonth);
                
                double availableLeaves = Math.max(0, summary.earnedLeaves - summary.usedLeaves);
                
                leaveBalanceInfo.setTotalEarnedLeaves(summary.earnedLeaves);
                leaveBalanceInfo.setUsedLeaves(summary.usedLeaves);
                leaveBalanceInfo.setAvailableLeaves(availableLeaves);
                leaveBalanceInfo.setCarriedForwardLeaves(0.0);
                leaveBalanceInfo.setUnpaidLeaves(summary.unpaidLeaves);
            } else {
                leaveBalanceInfo.setTotalEarnedLeaves(0.0);
                leaveBalanceInfo.setUsedLeaves(0.0);
                leaveBalanceInfo.setAvailableLeaves(0.0);
                leaveBalanceInfo.setCarriedForwardLeaves(0.0);
                leaveBalanceInfo.setUnpaidLeaves(0.0);
            }
        } catch (Exception e) {
            log.error("Error calculating leave balance for employee {}: {}", employee.getId(), e.getMessage());
            leaveBalanceInfo.setTotalEarnedLeaves(0.0);
            leaveBalanceInfo.setUsedLeaves(0.0);
            leaveBalanceInfo.setAvailableLeaves(0.0);
            leaveBalanceInfo.setCarriedForwardLeaves(0.0);
            leaveBalanceInfo.setUnpaidLeaves(0.0);
        }
        
        dto.setLeaveBalance(leaveBalanceInfo);
        
        return dto;
    }

    /**
     * Update payslip from payroll data when payroll is edited
     */
    @Transactional
    public void updatePayslipFromPayroll(Long employeeId, String monthYear, com.hrm.hrmsystem.dto.PayrollDTO payrollDTO) {
        List<Payslip> payslips = payslipRepository.findAllByEmployeeIdAndMonthYear(employeeId, monthYear);
        if (payslips == null || payslips.isEmpty()) {
            log.warn("No payslip found for employee {} month {} to update", employeeId, monthYear);
            return;
        }
        
        // Sort by ID descending to get the most recent one
        payslips.sort((a, b) -> b.getId().compareTo(a.getId()));
        Payslip payslip = payslips.get(0);
        
        // Update payslip fields from payroll data
        payslip.setBasicSalary(payrollDTO.getBasicSalary());
        payslip.setHra(payrollDTO.getHra());
        payslip.setDa(payrollDTO.getDa());
        payslip.setOtherAllowance(payrollDTO.getOtherAllowances());
        payslip.setPf(payrollDTO.getProvidentFund());
        payslip.setIncomeTax(payrollDTO.getTax());
        // Insurance is mapped to ESI in payslip
        if (payrollDTO.getInsurance() != null) {
            payslip.setEsi(payrollDTO.getInsurance());
        }
        payslip.setOtherDeduction(payrollDTO.getOtherDeductions());
        
        // Recalculate totals
        BigDecimal grossSalary = payrollDTO.getBasicSalary()
                .add(payrollDTO.getHra())
                .add(payrollDTO.getDa())
                .add(payrollDTO.getOtherAllowances());
        payslip.setGrossSalary(grossSalary);
        
        BigDecimal totalDeductions = payrollDTO.getProvidentFund()
                .add(payrollDTO.getTax())
                .add(payrollDTO.getInsurance() != null ? payrollDTO.getInsurance() : BigDecimal.ZERO)
                .add(payrollDTO.getOtherDeductions());
        payslip.setTotalDeduction(totalDeductions);
        
        BigDecimal netSalary = grossSalary.subtract(totalDeductions);
        payslip.setNetSalary(netSalary);
        
        payslipRepository.save(payslip);
        log.info("Updated payslip {} from payroll data for employee {} month {}", 
                payslip.getId(), employeeId, monthYear);
    }

    


    /**
     * Count active months between two dates
     * Counts months where the 1st day has passed (employee was active on 1st of month)
     * Rule: 1.5 leave is credited on the 1st day of every month if employee is active
     */
    private long countActiveMonths(LocalDate startDate, LocalDate endDate) {
        long months = 0;
        LocalDate current = startDate.withDayOfMonth(1);

        while (!current.isAfter(endDate)) {
            // If we've reached or passed the 1st of this month, count it
            months++;
            current = current.plusMonths(1);
        }

        return months;
    }

    /**
     * Convert month name to month number (1-12)
     */
    private Integer getMonthNumber(String monthName) {
        if (monthName == null) return null;
        return switch (monthName.trim().toLowerCase()) {
            case "january" -> 1;
            case "february" -> 2;
            case "march" -> 3;
            case "april" -> 4;
            case "may" -> 5;
            case "june" -> 6;
            case "july" -> 7;
            case "august" -> 8;
            case "september" -> 9;
            case "october" -> 10;
            case "november" -> 11;
            case "december" -> 12;
            default -> null;
        };
    }

    /**
     * Count days between two dates EXCLUDING Sundays
     * Sundays are not counted as leave days
     */
    private long countDaysExcludingSundays(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return 0;
        }
        
        long days = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            // Only count if NOT Sunday (DayOfWeek.SUNDAY = 7)
            if (current.getDayOfWeek().getValue() != 7) {
                days++;
            }
            current = current.plusDays(1);
        }
        return days;
    }
}
