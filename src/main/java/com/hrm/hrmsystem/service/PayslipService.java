package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.PayslipDTO;
import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
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
import com.hrm.hrmsystem.util.SalaryCalculator;
import com.hrm.hrmsystem.util.EmailUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
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
    private SalaryCalculator salaryCalculator;

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

        // Fetch employee - use findById to get fresh data from database
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        // Extract month and year
        String[] parts = monthYear.split("-");
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[0]);

        // Check if payslip already exists - if yes, delete ALL duplicates to regenerate with fresh data
        java.util.List<Payslip> existingPayslips = payslipRepository.findAllByEmployeeIdAndMonthYear(employeeId, monthYear);
        if (existingPayslips != null && !existingPayslips.isEmpty()) {
            log.info("Deleting {} existing payslip(s) for {}-{} to regenerate with fresh data", 
                     existingPayslips.size(), employeeId, monthYear);
            // Delete all duplicate payslips
            for (int i = 0; i < existingPayslips.size(); i++) {
                payslipRepository.delete(existingPayslips.get(i));
            }
        }

        // Create new payslip
        Payslip payslip = new Payslip();
        payslip.setEmployee(employee);
        payslip.setMonthYear(monthYear);
        payslip.setGeneratedDate(LocalDate.now());
        payslip.setStatus(Payslip.PayslipStatus.GENERATED);
        payslip.setSalaryMonth(month);
        payslip.setSalaryYear(year);

        // Calculate attendance for the month (with fresh attendance data)
        calculateAttendance(payslip, month, year);

        // Calculate salary with current employee salary details
        calculateSalary(payslip, employee);

        // Save payslip
        Payslip savedPayslip = payslipRepository.save(payslip);
        log.info("Payslip generated successfully with id: {}", savedPayslip.getId());

        return convertToDTO(savedPayslip);
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
        LeaveDaysSummary leaveSummary = countApprovedLeaveDaysWithPaidUnpaid(payslip.getEmployee().getId(), month, year);
        int approvedLeaveDays = leaveSummary.totalDays;
        int paidLeaveDays = leaveSummary.paidDays;
        int unpaidLeaveDays = leaveSummary.unpaidDays;
        int halfDayLeaves = leaveSummary.halfDays;

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
     */
    private int countApprovedLeaveDays(Long employeeId, int month, int year) {
        LeaveDaysSummary summary = countApprovedLeaveDaysWithPaidUnpaid(employeeId, month, year);
        return summary.totalDays;
    }
    
    /**
     * Count approved leave days with paid/unpaid breakdown
     */
    private LeaveDaysSummary countApprovedLeaveDaysWithPaidUnpaid(Long employeeId, int month, int year) {
        // Fetch all leaves and filter for approved ones in the given month
        List<Leave> allLeaves = leaveRepository.findByEmployeeId(employeeId);
        
        int totalLeaveDays = 0;
        int paidLeaveDays = 0;
        int unpaidLeaveDays = 0;
        int halfDays = 0;
        
        for (Leave leave : allLeaves) {
            if (leave.getStatus() != Leave.LeaveStatus.APPROVED) {
                continue; // Only count approved leaves
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
                long days = java.time.temporal.ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
                
                // Handle half day
                if (leave.getIsHalfDay() != null && leave.getIsHalfDay()) {
                    halfDays++;
                    days = 1; // Half day counts as 1 day for display, 0.5 for deduction
                }
                
                totalLeaveDays += (int) days;
                
                // Track paid vs unpaid
                int leavePaidDays = leave.getPaidDays() != null ? leave.getPaidDays() : 0;
                int leaveUnpaidDays = leave.getUnpaidDays() != null ? leave.getUnpaidDays() : 0;
                
                // Pro-rate for partial month overlap
                if (leavePaidDays > 0 || leaveUnpaidDays > 0) {
                    int totalLeaveTotal = leavePaidDays + leaveUnpaidDays;
                    if (totalLeaveTotal > 0) {
                        // Scale paid/unpaid based on overlap proportion
                        double overlapRatio = (double) days / (java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1);
                        paidLeaveDays += (int) Math.round(leavePaidDays * overlapRatio);
                        unpaidLeaveDays += (int) Math.round(leaveUnpaidDays * overlapRatio);
                    }
                } else {
                    // Legacy: if no paid/unpaid set, assume all paid for backward compatibility
                    paidLeaveDays += (int) days;
                }
            }
        }

        return new LeaveDaysSummary(totalLeaveDays, paidLeaveDays, unpaidLeaveDays, halfDays);
    }
    
    // Helper class for leave days summary
    private static class LeaveDaysSummary {
        int totalDays;
        int paidDays;
        int unpaidDays;
        int halfDays;
        
        LeaveDaysSummary(int totalDays, int paidDays, int unpaidDays, int halfDays) {
            this.totalDays = totalDays;
            this.paidDays = paidDays;
            this.unpaidDays = unpaidDays;
            this.halfDays = halfDays;
        }
    }

    /**
     * Calculate salary components
     */
    private void calculateSalary(Payslip payslip, Employee employee) {
        log.info("Calculating salary for employee: {}", employee.getId());

        // Keep Payslip math exactly aligned with PayrollService:
        // basic = employee.salary, hra=20%, da=10%, ta=5% (stored as otherAllowance), pf=12%, tax by slab, insurance=500.
        BigDecimal basicSalary = employee.getSalary() != null ? employee.getSalary() : BigDecimal.ZERO;
        BigDecimal hra = basicSalary.multiply(new BigDecimal("0.20"));
        BigDecimal da = basicSalary.multiply(new BigDecimal("0.10"));
        BigDecimal ta = basicSalary.multiply(new BigDecimal("0.05"));
        BigDecimal otherAllowance = ta;

        // Set earnings
        payslip.setBasicSalary(basicSalary);
        payslip.setDa(da);
        payslip.setHra(hra);
        payslip.setOtherAllowance(otherAllowance);

        // Calculate gross salary
        BigDecimal grossSalary = basicSalary.add(da).add(hra).add(otherAllowance);
        payslip.setGrossSalary(grossSalary);

        // Calculate deductions (same as PayrollService)
        BigDecimal pf = basicSalary.multiply(new BigDecimal("0.12"));
        BigDecimal esi = BigDecimal.ZERO; // PayrollService does not apply ESI currently
        BigDecimal incomeTax = calculatePayrollTax(basicSalary);
        BigDecimal insurance = new BigDecimal("500.0");

        payslip.setPf(pf);
        payslip.setEsi(esi);
        payslip.setIncomeTax(incomeTax);
        payslip.setOtherDeduction(insurance);

        // Calculate total deduction (before absent/leave)
        BigDecimal totalDeduction = pf.add(esi).add(incomeTax).add(insurance);
        payslip.setTotalDeduction(totalDeduction);

        // Calculate net salary before absent/leave deduction
        BigDecimal netSalary = grossSalary.subtract(totalDeduction);

        // Calculate deduction using LeaveService for unpaid leaves
        int unpaidLeaveDays = payslip.getUnpaidLeaveDays() != null ? payslip.getUnpaidLeaveDays() : 0;
        int halfDaysCount = payslip.getHalfDays() != null ? payslip.getHalfDays() : 0;
        int absentDaysCount = payslip.getAbsentDays() != null ? payslip.getAbsentDays() : 0;
        
        // Calculate leave deduction using the LeaveService logic
        // Unpaid leave days + absent days are deducted, half days count as 0.5
        double daysToDeduct = unpaidLeaveDays + absentDaysCount + (halfDaysCount * 0.5);
        
        // First 1.5 days free per month (as per existing policy)
        double deductibleDays = Math.max(0, daysToDeduct - 1.5);
        
        BigDecimal salaryPerDay = basicSalary.compareTo(BigDecimal.ZERO) > 0
                ? basicSalary.divide(new BigDecimal(26), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        
        // Calculate deduction for unpaid leaves and absences
        BigDecimal absentLeaveDeduction = salaryPerDay.multiply(BigDecimal.valueOf(deductibleDays))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        
        // Also calculate using LeaveService for consistency
        BigDecimal leaveDeduction = leaveService.calculateLeaveDeduction(employee, unpaidLeaveDays, false);
        
        // Use the higher of the two calculations
        BigDecimal finalDeduction = absentLeaveDeduction.max(leaveDeduction);
        payslip.setAbsentLeaveDeduction(finalDeduction);

        // Apply absent/leave deduction and update total deduction
        BigDecimal adjustedNetSalary = netSalary.subtract(absentLeaveDeduction);
        payslip.setNetSalary(adjustedNetSalary.compareTo(BigDecimal.ZERO) > 0 ? adjustedNetSalary : BigDecimal.ZERO);
        payslip.setTotalDeduction(totalDeduction.add(absentLeaveDeduction));

        log.info("Salary calculated - Gross: {}, Deduction: {}, AbsentLeaveDed: {}, Net: {}",
                 grossSalary, totalDeduction, absentLeaveDeduction, payslip.getNetSalary());
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
                    payrollRepository.findByEmployeeIdAndMonthAndYear(employee.getId(), month, year);

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
     * Delete payslip (only DRAFT payslips can be deleted)
     */
    public void deletePayslip(Long payslipId) {
        Payslip payslip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip not found with id: " + payslipId));

        if (!payslip.getStatus().equals(Payslip.PayslipStatus.DRAFT)) {
            throw new BadRequestException("Only DRAFT payslips can be deleted");
        }

        payslipRepository.deleteById(payslipId);
        log.info("Payslip {} deleted", payslipId);
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
        return dto;
    }
}
