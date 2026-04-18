package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.PayslipDTO;
import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.model.Payroll;
import com.hrm.hrmsystem.entity.Payslip;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private LeaveRepository leaveRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private PdfGeneratorUtil pdfGeneratorUtil;

    @Autowired
    private EmailUtil emailUtil;

    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private LeaveService leaveService;

    /**
     * Generate payslip for a single employee (always regenerate with fresh data)
     */
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
     * Calculate attendance for the month
     */
    private void calculateAttendance(Payslip payslip, int month, int year) {
        // Clear entity manager cache to get fresh attendance data
        entityManager.clear();
        
        log.info("Calculating attendance for employee: {} for {}-{}", 
                 payslip.getEmployee().getId(), year, month);

        List<Attendance> attendanceList = attendanceRepository.findByEmployeeAndMonthAndYear(
                payslip.getEmployee(), month, year);

        int presentDays = (int) attendanceList.stream()
                .filter(a -> Attendance.AttendanceStatus.PRESENT.equals(a.getStatus()))
                .count();

        int absentDays = (int) attendanceList.stream()
                .filter(a -> Attendance.AttendanceStatus.ABSENT.equals(a.getStatus()))
                .count();

        int halfDays = (int) attendanceList.stream()
                .filter(a -> a.getStatus() != null && Attendance.AttendanceStatus.HALF_DAY.equals(a.getStatus()))
                .count();

        // Get absent dates
        java.util.List<String> absentDates = attendanceList.stream()
                .filter(a -> Attendance.AttendanceStatus.ABSENT.equals(a.getStatus()))
                .map(a -> a.getDate().toString())
                .collect(Collectors.toList());

        // Count approved leave days for this month - separate paid and unpaid
        // Only APPROVED leaves are counted, half days = 0.5 days
        LeaveDaysSummary leaveSummary = countApprovedLeaveDaysWithPaidUnpaid(payslip.getEmployee().getId(), month, year);
        // Convert to integers for storage (half days tracked separately)
        int approvedLeaveDays = (int) Math.floor(leaveSummary.totalDays); // Full days only
        int paidLeaveDays = (int) Math.floor(leaveSummary.paidDays); // Full paid days
        int unpaidLeaveDays = (int) Math.floor(leaveSummary.unpaidDays); // Full unpaid days
        int halfDayLeaves = leaveSummary.halfDays; // Half day instances (each = 0.5 day)

        // Effective working days = Present + Approved Leave (both paid and unpaid are shown but unpaid deducted)
        int effectiveWorkingDays = presentDays + approvedLeaveDays;

        payslip.setPresentDays(effectiveWorkingDays); // Show total paid days (present + leave)
        payslip.setAbsentDays(absentDays);
        payslip.setLeaveDays(approvedLeaveDays); // Show approved leave separately
        payslip.setPaidLeaveDays(paidLeaveDays); // Track paid leave days
        payslip.setUnpaidLeaveDays(unpaidLeaveDays); // Track unpaid leave days
        payslip.setHalfDays(halfDays + halfDayLeaves); // Combine half days from attendance and leave
        payslip.setWorkingDays(26); // Standard working days
        payslip.setTotalDays(30); // Standard total days
        payslip.setAbsentDates(absentDates); // Store absent dates

        log.info("Attendance calculated - Present: {}, On Approved Leave: {} (Paid: {}, Unpaid: {}), Absent: {}, Total Paid Days: {}", 
                 presentDays, approvedLeaveDays, paidLeaveDays, unpaidLeaveDays, absentDays, effectiveWorkingDays);
    }

    /**
     * Count approved leave days in a given month - returns total including paid and unpaid
     * Only counts APPROVED leaves
     */
    private int countApprovedLeaveDays(Long employeeId, int month, int year) {
        LeaveDaysSummary summary = countApprovedLeaveDaysWithPaidUnpaid(employeeId, month, year);
        return (int) Math.floor(summary.totalDays); // Return full days only, half days tracked separately
    }
    
    /**
     * Count approved leave days with paid/unpaid breakdown
     * Only APPROVED leaves are counted
     * Half days count as 0.5 days
     */
    private LeaveDaysSummary countApprovedLeaveDaysWithPaidUnpaid(Long employeeId, int month, int year) {
        // Fetch all leaves and filter for approved ones in the given month
        List<Leave> allLeaves = leaveRepository.findByEmployeeId(employeeId);
        
        double totalLeaveDays = 0;
        double paidLeaveDays = 0;
        double unpaidLeaveDays = 0;
        int halfDays = 0;
        
        for (Leave leave : allLeaves) {
            // ONLY count APPROVED leaves - skip rejected, cancelled, pending
            if (leave.getStatus() != Leave.LeaveStatus.APPROVED) {
                continue;
            }
            
            // Calculate overlapping days with the payslip month
            LocalDate startDate = leave.getStartDate();
            LocalDate endDate = leave.getEndDate();
            
            LocalDate monthStart = LocalDate.of(year, month, 1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            
            // Find overlap
            LocalDate overlapStart = startDate.isAfter(monthStart) ? startDate : monthStart;
            LocalDate overlapEnd = endDate.isBefore(monthEnd) ? endDate : monthEnd;
            
            if (!overlapStart.isAfter(overlapEnd)) {
                // Calculate days in overlap
                double days = java.time.temporal.ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
                
                // Handle half day - count as 0.5 days
                boolean isHalfDay = leave.getIsHalfDay() != null && leave.getIsHalfDay();
                if (isHalfDay) {
                    halfDays++;
                    days = 0.5; // Half day = 0.5 days for all calculations
                }
                
                totalLeaveDays += days;
                
                // Track paid vs unpaid
                double leavePaidDays = leave.getPaidDays() != null ? leave.getPaidDays() : 0.0;
                double leaveUnpaidDays = leave.getUnpaidDays() != null ? leave.getUnpaidDays() : 0.0;
                
                // For half day, paid/unpaid should also be 0.5 or 0
                if (isHalfDay) {
                    // Half day: paidDays and unpaidDays should be 0.5 or 0
                    if (leavePaidDays > 0) {
                        paidLeaveDays += 0.5;
                    } else {
                        unpaidLeaveDays += 0.5;
                    }
                } else {
                    // Pro-rate for partial month overlap
                    if (leavePaidDays > 0 || leaveUnpaidDays > 0) {
                        double totalLeaveTotal = leavePaidDays + leaveUnpaidDays;
                        if (totalLeaveTotal > 0) {
                            // Scale paid/unpaid based on overlap proportion
                            double overlapRatio = days / (java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1);
                            paidLeaveDays += leavePaidDays * overlapRatio;
                            unpaidLeaveDays += leaveUnpaidDays * overlapRatio;
                        }
                    } else {
                        // Legacy: if no paid/unpaid set, assume all paid for backward compatibility
                        paidLeaveDays += days;
                    }
                }
            }
        }

        return new LeaveDaysSummary(totalLeaveDays, paidLeaveDays, unpaidLeaveDays, halfDays);
    }
    
    // Helper class for leave days summary
    private static class LeaveDaysSummary {
        double totalDays;
        double paidDays;
        double unpaidDays;
        int halfDays;
        
        LeaveDaysSummary(double totalDays, double paidDays, double unpaidDays, int halfDays) {
            this.totalDays = totalDays;
            this.paidDays = paidDays;
            this.unpaidDays = unpaidDays;
            this.halfDays = halfDays;
        }
    }

    /**
     * Calculate salary components based on attendance and leave policy
     * - Salary calculated based on 26 working days
     * - Probation: No paid leave, all leaves/absences deducted
     * - Post-probation: 1.5 paid leave per month, max 3 at once
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
        
        BigDecimal basicSalary, hra, da, otherAllowance, grossSalary;
        BigDecimal pf, esi, incomeTax, insurance, totalDeduction;
        BigDecimal absentLeaveDeduction = BigDecimal.ZERO;
        String calculationMessage = "";
        
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
            
            pf = payroll.getProvidentFund() != null ? payroll.getProvidentFund() : BigDecimal.ZERO;
            esi = BigDecimal.ZERO;
            incomeTax = payroll.getTax() != null ? payroll.getTax() : BigDecimal.ZERO;
            insurance = payroll.getInsurance() != null ? payroll.getInsurance() : BigDecimal.ZERO;
            
            // Total deductions from payroll ALREADY includes otherDeductions (absent/leave)
            totalDeduction = payroll.getTotalDeductions() != null ? payroll.getTotalDeductions() : BigDecimal.ZERO;
            absentLeaveDeduction = payroll.getOtherDeductions() != null ? payroll.getOtherDeductions() : BigDecimal.ZERO;
            
            calculationMessage = "Calculated from Payroll ID: " + payroll.getId();
        } else {
            // Fallback: Calculate from employee data (only if no payroll exists)
            log.warn("No payroll found for employee {} month {}-{}, calculating from employee data", 
                    employee.getId(), year, month);
            
            basicSalary = employee.getBasicSalary() != null ? employee.getBasicSalary() : BigDecimal.ZERO;
            hra = employee.getHra() != null ? employee.getHra() : BigDecimal.ZERO;
            da = employee.getDa() != null ? employee.getDa() : BigDecimal.ZERO;
            otherAllowance = employee.getOtherAllowance() != null ? employee.getOtherAllowance() : BigDecimal.ZERO;
            
            // Calculate gross salary
            grossSalary = basicSalary.add(da).add(hra).add(otherAllowance);
            
            // Calculate deductions using stored values or percentage
            pf = employee.getPf() != null ? employee.getPf() : basicSalary.multiply(new BigDecimal("0.12"));
            esi = BigDecimal.ZERO;
            incomeTax = employee.getTax() != null ? employee.getTax() : calculatePayrollTax(basicSalary);
            // Calculate insurance as percentage of basic salary
            insurance = BigDecimal.ZERO;
            if (employee.getInsurancePercentage() != null && employee.getInsurancePercentage() > 0) {
                insurance = basicSalary.multiply(BigDecimal.valueOf(employee.getInsurancePercentage()))
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            }
            
            totalDeduction = pf.add(esi).add(incomeTax).add(insurance);
            
            // Get attendance data for calculation
            int unpaidLeaveDays = payslip.getUnpaidLeaveDays() != null ? payslip.getUnpaidLeaveDays() : 0;
            int paidLeaveDays = payslip.getPaidLeaveDays() != null ? payslip.getPaidLeaveDays() : 0;
            int halfDaysCount = payslip.getHalfDays() != null ? payslip.getHalfDays() : 0;
            int absentDaysCount = payslip.getAbsentDays() != null ? payslip.getAbsentDays() : 0;
            int leaveDaysCount = payslip.getLeaveDays() != null ? payslip.getLeaveDays() : 0;
            
            log.info("Attendance data - Absent: {}, HalfDays: {}, Leaves: {}, Paid: {}, Unpaid: {}",
                    absentDaysCount, halfDaysCount, leaveDaysCount, paidLeaveDays, unpaidLeaveDays);

            // Calculate per day salary based on 26 working days (NOT 30)
            BigDecimal salaryPerDay = basicSalary.compareTo(BigDecimal.ZERO) > 0
                    ? basicSalary.divide(new BigDecimal(26), 2, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            
            log.info("Per day salary (based on 26 days): {}", salaryPerDay);
            
            // Calculate attendance deductions
            if (isInProbation) {
                double totalDeductionDays = absentDaysCount + leaveDaysCount + (halfDaysCount * 0.5);
                absentLeaveDeduction = salaryPerDay.multiply(BigDecimal.valueOf(totalDeductionDays))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                payslip.setUnpaidLeaveDays(leaveDaysCount);
                payslip.setPaidLeaveDays(0);
                calculationMessage = String.format(
                    "During probation: No paid leave. Total deduction days: %.1f (Absent: %d, Leaves: %d, Half-days: %d). Deducted: %s",
                    totalDeductionDays, absentDaysCount, leaveDaysCount, halfDaysCount, absentLeaveDeduction);
            } else {
                int totalLeavesThisMonth = leaveDaysCount;
                int maxPaidLeaves = Math.min(3, totalLeavesThisMonth);
                int extraUnpaidLeaves = totalLeavesThisMonth - maxPaidLeaves;
                double totalDeductionDays = absentDaysCount + extraUnpaidLeaves + (halfDaysCount * 0.5);
                absentLeaveDeduction = salaryPerDay.multiply(BigDecimal.valueOf(totalDeductionDays))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                payslip.setPaidLeaveDays(maxPaidLeaves);
                payslip.setUnpaidLeaveDays(extraUnpaidLeaves);
                calculationMessage = String.format(
                    "Post-probation: %d paid leaves used. Deduction for %d absent days + %d half-days = %.1f days. Deducted: %s",
                    maxPaidLeaves, absentDaysCount, halfDaysCount, totalDeductionDays, absentLeaveDeduction);
            }
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
            payslip.setTotalDeduction(totalDeduction.add(absentLeaveDeduction));
            netSalary = grossSalary.subtract(totalDeduction).subtract(absentLeaveDeduction);
        }
        
        log.info("DEBUG - Before setting: Gross={}, TotalDed={}, Net={}", grossSalary, payslip.getTotalDeduction(), netSalary);
        
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
        if (employee.getJoiningDate() == null || employee.getProbationPeriodMonths() == null) {
            return false; // No probation defined, consider as completed
        }

        LocalDate probationEndDate = employee.getJoiningDate()
                .plusMonths(employee.getProbationPeriodMonths());
        
        LocalDate payslipDate = LocalDate.of(year, month, 1);
        
        return payslipDate.isBefore(probationEndDate) || payslipDate.isEqual(probationEndDate);
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
     */
    public int getPresentDaysForEmployee(Long employeeId, Integer month, Integer year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndDateBetween(
            employeeId, startDate, endDate);
        
        long presentDays = attendances.stream()
            .filter(a -> a.getStatus() != null && Attendance.AttendanceStatus.PRESENT.equals(a.getStatus()))
            .count();
        
        log.debug("Present days for employee {} in {}/{}: {}", employeeId, month, year, presentDays);
        return (int) presentDays;
    }
    
    /**
     * Get absent days for employee in a month/year
     */
    public int getAbsentDaysForEmployee(Long employeeId, Integer month, Integer year) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndDateBetween(
            employeeId, startDate, endDate);
        
        long absentDays = attendances.stream()
            .filter(a -> a.getStatus() != null && Attendance.AttendanceStatus.ABSENT.equals(a.getStatus()))
            .count();
        
        log.debug("Absent days for employee {} in {}/{}: {}", employeeId, month, year, absentDays);
        return (int) absentDays;
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

                dto.setPresentDays(0);
                dto.setAbsentDays(0);
                dto.setLeaveDays(0);
                dto.setHalfDays(0);
                dto.setWorkingDays(26);
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
        dto.setId(payslip.getId());
        dto.setEmployeeId(payslip.getEmployee().getId());
        dto.setEmployeeName(payslip.getEmployee().getFirstName() + " " + payslip.getEmployee().getLastName());
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
        dto.setAbsentLeaveDeduction(payslip.getAbsentLeaveDeduction());
        dto.setStatus(payslip.getStatus().toString());
        dto.setGeneratedDate(payslip.getGeneratedDate());
        dto.setApprovedDate(payslip.getApprovedDate());
        dto.setApprovedBy(payslip.getApprovedBy());
        dto.setRemarks(payslip.getRemarks());
        dto.setPdfFilePath(payslip.getPdfFilePath());
        
        // Calculate and set probation status
        Employee employee = payslip.getEmployee();
        boolean isInProbation = checkProbationStatus(employee, 
            payslip.getSalaryYear() != null ? payslip.getSalaryYear() : LocalDate.now().getYear(),
            payslip.getSalaryMonth() != null ? payslip.getSalaryMonth() : LocalDate.now().getMonthValue());
        dto.setInProbation(isInProbation);
        dto.setProbationStatus(isInProbation ? "In Progress" : "Completed");
        
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
}
