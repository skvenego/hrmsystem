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
    private SalaryCalculator salaryCalculator;

    @Autowired
    private PdfGeneratorUtil pdfGeneratorUtil;

    @Autowired
    private EmailUtil emailUtil;

    @Autowired
    private EntityManager entityManager;

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

        // Get absent dates
        java.util.List<String> absentDates = attendanceList.stream()
                .filter(a -> Attendance.AttendanceStatus.ABSENT.equals(a.getStatus()))
                .map(a -> a.getDate().toString())
                .collect(Collectors.toList());

        // Count approved leave days for this month
        int approvedLeaveDays = countApprovedLeaveDays(payslip.getEmployee().getId(), month, year);

        // Effective working days = Present + Approved Leave (both are paid)
        int effectiveWorkingDays = presentDays + approvedLeaveDays;

        payslip.setPresentDays(effectiveWorkingDays); // Show total paid days (present + leave)
        payslip.setAbsentDays(absentDays);
        payslip.setLeaveDays(approvedLeaveDays); // Show approved leave separately
        payslip.setWorkingDays(26); // Standard working days
        payslip.setTotalDays(30); // Standard total days
        payslip.setAbsentDates(absentDates); // Store absent dates

        log.info("Attendance calculated - Present: {}, On Approved Leave: {}, Absent: {}, Total Paid Days: {}", 
                 presentDays, approvedLeaveDays, absentDays, effectiveWorkingDays);
    }

    /**
     * Count approved leave days in a given month
     */
    private int countApprovedLeaveDays(Long employeeId, int month, int year) {
        // Fetch all leaves and filter for approved ones in the given month
        List<Leave> allLeaves = leaveRepository.findByEmployeeId(employeeId);
        
        int totalLeaveDays = 0;
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
                totalLeaveDays += (int) days;
            }
        }

        return totalLeaveDays;
    }

    /**
     * Calculate salary components
     */
    private void calculateSalary(Payslip payslip, Employee employee) {
        log.info("Calculating salary for employee: {}", employee.getId());

        // Get employee salary details
        BigDecimal basicSalary = employee.getBasicSalary() != null ? employee.getBasicSalary() : BigDecimal.ZERO;
        BigDecimal da = employee.getDa() != null ? employee.getDa() : BigDecimal.ZERO;
        BigDecimal hra = employee.getHra() != null ? employee.getHra() : BigDecimal.ZERO;
        BigDecimal otherAllowance = employee.getOtherAllowance() != null ? employee.getOtherAllowance() : BigDecimal.ZERO;

        // Set earnings
        payslip.setBasicSalary(basicSalary);
        payslip.setDa(da);
        payslip.setHra(hra);
        payslip.setOtherAllowance(otherAllowance);

        // Calculate gross salary
        BigDecimal grossSalary = basicSalary.add(da).add(hra).add(otherAllowance);
        payslip.setGrossSalary(grossSalary);

        // Calculate deductions
        BigDecimal pf = salaryCalculator.calculatePF(basicSalary);
        BigDecimal esi = salaryCalculator.calculateESI(grossSalary);
        BigDecimal incomeTax = salaryCalculator.calculateIncomeTax(grossSalary);

        payslip.setPf(pf);
        payslip.setEsi(esi);
        payslip.setIncomeTax(incomeTax);
        payslip.setOtherDeduction(BigDecimal.ZERO);

        // Calculate total deduction
        BigDecimal totalDeduction = pf.add(esi).add(incomeTax);
        payslip.setTotalDeduction(totalDeduction);

        // Calculate net salary
        BigDecimal netSalary = grossSalary.subtract(totalDeduction);
        payslip.setNetSalary(netSalary);

        // Adjust for absent days (salary cut for absent days)
        BigDecimal salaryPerDay = basicSalary.divide(new BigDecimal(26), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal absentDeduction = salaryPerDay.multiply(new BigDecimal(payslip.getAbsentDays()));

        // Apply absent day deduction
        BigDecimal adjustedNetSalary = netSalary.subtract(absentDeduction);
        payslip.setNetSalary(adjustedNetSalary.compareTo(BigDecimal.ZERO) > 0 ? adjustedNetSalary : BigDecimal.ZERO);

        log.info("Salary calculated - Gross: {}, Deduction: {}, Net: {}", 
                 grossSalary, totalDeduction, payslip.getNetSalary());
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
        List<Payslip> payslips = payslipRepository.findByMonthYear(monthYear);
        return payslips.stream().map(this::convertToDTO).collect(Collectors.toList());
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
        dto.setStatus(payslip.getStatus().toString());
        dto.setGeneratedDate(payslip.getGeneratedDate());
        dto.setApprovedDate(payslip.getApprovedDate());
        dto.setApprovedBy(payslip.getApprovedBy());
        dto.setRemarks(payslip.getRemarks());
        return dto;
    }
}
