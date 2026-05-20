package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.LeaveBalanceDTO;
import com.hrm.hrmsystem.dto.PayslipDTO;
import com.hrm.hrmsystem.engine.AttendanceEngine;
import com.hrm.hrmsystem.engine.AttendanceSummary;
import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.model.User;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import com.hrm.hrmsystem.repository.UserRepository;
import com.hrm.hrmsystem.service.AttendanceService;
import com.hrm.hrmsystem.service.EmployeeService;
import com.hrm.hrmsystem.service.LeaveService;
import com.hrm.hrmsystem.service.PayslipService;
import com.hrm.hrmsystem.service.UnifiedCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final LeaveService leaveService;
    private final PayslipService payslipService;
    private final EmployeeRepository employeeRepository;
    private final LeaveRepository leaveRepository;
    private final UserRepository userRepository;
    private final UnifiedCalculationService unifiedCalculationService;

    private final AttendanceEngine attendanceEngine;

    public DashboardController(
            AttendanceService attendanceService,
            EmployeeService employeeService,
            LeaveService leaveService,
            PayslipService payslipService,
            EmployeeRepository employeeRepository,
            LeaveRepository leaveRepository,
            UserRepository userRepository,
            UnifiedCalculationService unifiedCalculationService,
            AttendanceEngine attendanceEngine) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
        this.leaveService = leaveService;
        this.payslipService = payslipService;
        this.employeeRepository = employeeRepository;
        this.leaveRepository = leaveRepository;
        this.userRepository = userRepository;
        this.unifiedCalculationService = unifiedCalculationService;
        this.attendanceEngine = attendanceEngine;
    }

    /**
     * Get current user's dashboard data (for employees)
     */
    @GetMapping("/my")
    public ResponseEntity<?> getMyDashboard() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            // Get user
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
            User user = userOpt.get();
            
            // Get employee data if linked
            Map<String, Object> dashboardData = new HashMap<>();
            dashboardData.put("username", user.getUsername());
            dashboardData.put("role", user.getRole());
            dashboardData.put("email", user.getEmail());
            
            if (user.getEmployee() != null) {
                Employee employee = user.getEmployee();
                Long employeeId = employee.getId();
                
                log.info("Dashboard DEBUG - User {} linked to employee ID: {}", username, employeeId);
                
                dashboardData.put("employeeId", employeeId);
                dashboardData.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
                dashboardData.put("department", employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A");
                dashboardData.put("designation", employee.getDesignation());
                
                // Get attendance summary for current month - USE SINGLE SOURCE OF TRUTH
                try {
                    if (employeeService.isSystemUser(employee)) {
                        dashboardData.put("attendance", Map.of(
                            "presentDays", 0.0,
                            "absentDays", 0.0,
                            "totalHours", 0.0,
                            "paidUsedLeaves", 0.0,
                            "unpaidUsedLeaves", 0.0,
                            "usedLeaves", 0.0
                        ));
                        dashboardData.put("leaveBalance", LeaveBalanceDTO.builder()
                            .totalEarnedLeaves(0.0)
                            .usedLeaves(0.0)
                            .paidLeaves(0.0)
                            .unpaidLeaves(0.0)
                            .availableLeaves(0.0)
                            .remaining(0.0)
                            .build());
                    } else {
                        LocalDate today = LocalDate.now();
                        YearMonth currentMonth = YearMonth.of(today.getYear(), today.getMonthValue());

                        // ✅ Use AttendanceEngine for attendance calculation (present/absent)
                        AttendanceSummary summary = attendanceEngine.calculate(employeeId, currentMonth);
                        
                        // ✅ Use LeaveService for leave balance (Cumulative) - SINGLE SOURCE OF TRUTH
                        LeaveBalanceDTO balance = leaveService.getLeaveBalance(employeeId);
                        
                        log.info("Dashboard DEBUG - Employee {}: summary.worked={}, summary.paidLeave={}, summary.unpaidLeave={}", 
                            employeeId, summary.workedDays, summary.paidLeave, summary.unpaidLeave);
                        log.info("Dashboard DEBUG - Employee {}: Final Balance Used={}, Remaining={}, Earned={}", 
                            employeeId, balance.getUsedLeaves(), balance.getAvailableLeaves(), balance.getTotalEarnedLeaves());

                        dashboardData.put("attendance", Map.of(
                            "presentDays", summary.getPresentDays(), 
                            "absentDays", summary.absent, 
                            "totalHours", summary.getPresentDays() * 8.0
                        ));
                        
                        // ✅ Directly use the balance object - NO manual re-mapping of fields
                        dashboardData.put("leaveBalance", balance);
                    }
                } catch (Exception e) {
                    dashboardData.put("attendance", Map.of("presentDays", 0, "absentDays", 0, "paidUsedLeaves", 0, "unpaidUsedLeaves", 0, "totalHours", 0, "payableDays", 0));
                }
                
            } else {
                log.warn("Dashboard DEBUG - User {} is not linked to any employee", username);
            } // End of if (user.getEmployee() != null)
            
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load dashboard: " + e.getMessage()));
        }
    }

    /**
     * Get management dashboard stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            LocalDate today = LocalDate.now();
            
            // Employee stats
            List<Employee> nonSystemEmployees = employeeRepository.findAll().stream()
                    .filter(e -> !employeeService.isSystemUser(e))
                    .toList();
            long totalEmployees = nonSystemEmployees.size();
            List<Employee> activeEmployeesList = nonSystemEmployees.stream()
                    .filter(e -> e.getStatus() == Employee.EmployeeStatus.ACTIVE)
                    .toList();
            long activeEmployees = activeEmployeesList.size();
            
            // Leave stats for today
            List<Leave> approvedLeaves = leaveRepository.findByStatus(Leave.LeaveStatus.APPROVED);
            long onLeaveToday = approvedLeaves.stream()
                    .filter(l -> l.getEmployee() != null && !employeeService.isSystemUser(l.getEmployee()))
                    .filter(l -> !today.isBefore(l.getStartDate()) && !today.isAfter(l.getEndDate()))
                    .count();

            stats.put("employees", Map.of(
                "total", totalEmployees,
                "active", activeEmployees,
                "onLeave", onLeaveToday,
                "newThisMonth", 0
            ));
            
            // Attendance stats
            long presentToday = attendanceService.getPresentTodayCount();
            
            stats.put("attendance", Map.of(
                "presentToday", presentToday,
                "averageHours", 8.0, // Default or calculate if needed
                "onTime", presentToday, // Simplified
                "late", 0
            ));
            
            // Payroll stats
            String currentMonthStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            try {
                // Total Monthly Payroll = Sum of all active non-system employees' gross salaries
                double totalPayroll = activeEmployeesList.stream()
                        .map(Employee::getTotalGrossSalary)
                        .filter(java.util.Objects::nonNull)
                        .map(BigDecimal::doubleValue)
                        .mapToDouble(Double::doubleValue)
                        .sum();
                        
                List<PayslipDTO> rawPayslips = payslipService.getPayslipsByMonth(currentMonthStr);
                List<PayslipDTO> monthPayslips = rawPayslips.stream()
                        .filter(p -> p.getEmployeeId() != null)
                        .filter(p -> {
                            Optional<Employee> empOpt = employeeRepository.findById(p.getEmployeeId());
                            return empOpt.isPresent() && !employeeService.isSystemUser(empOpt.get());
                        })
                        .toList();
                
                // Salary Paid = Sum of approved, sent, or paid payslips (actual cost)
                double salaryPaid = monthPayslips.stream()
                        .filter(p -> List.of("APPROVED", "SENT", "PAID").contains((p.getStatus() != null ? p.getStatus() : "").toUpperCase()) || 
                                     "PAID".equals(p.getPayrollStatus()))
                        .map(PayslipDTO::getNetSalary)
                        .map(BigDecimal::doubleValue)
                        .mapToDouble(Double::doubleValue)
                        .sum();
                
                double pendingAmount = totalPayroll - salaryPaid;
                if (pendingAmount < 0) pendingAmount = 0;
                
                long pendingPayslips = monthPayslips.stream()
                        .filter(p -> "GENERATED".equals(p.getStatus()) || "DRAFT".equals(p.getStatus()))
                        .count();
                
                stats.put("payroll", Map.of(
                    "totalAmount", totalPayroll,
                    "salaryPaid", salaryPaid,
                    "pendingAmount", pendingAmount,
                    "processed", monthPayslips.size(),
                    "pending", pendingPayslips
                ));
            } catch (Exception e) {
                stats.put("payroll", Map.of("totalAmount", 0, "salaryPaid", 0, "processed", 0, "pending", 0));
            }
            
            // Leave stats
            try {
                List<com.hrm.hrmsystem.dto.LeaveDTO> rawPendingLeaves = leaveService.getPendingLeaves();
                List<com.hrm.hrmsystem.dto.LeaveDTO> pendingLeaves = rawPendingLeaves.stream()
                        .filter(l -> l.getEmployeeId() != null)
                        .filter(l -> {
                            Optional<Employee> empOpt = employeeRepository.findById(l.getEmployeeId());
                            return empOpt.isPresent() && !employeeService.isSystemUser(empOpt.get());
                        })
                        .toList();
                
                // Count approved/rejected TODAY (simplified)
                long approvedToday = approvedLeaves.stream()
                        .filter(l -> l.getEmployee() != null && !employeeService.isSystemUser(l.getEmployee()))
                        .filter(l -> today.equals(l.getAppliedDate())) // Or check an approvalDate if exists
                        .count();

                stats.put("leaves", Map.of(
                    "pendingRequests", pendingLeaves.size(),
                    "approvedToday", approvedToday,
                    "rejectedToday", 0
                ));
            } catch (Exception e) {
                stats.put("leaves", Map.of("pendingRequests", 0, "approvedToday", 0, "rejectedToday", 0));
            }
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load stats: " + e.getMessage()));
        }
    }
    
    /**
     * Get current user's latest payslip
     */
    @GetMapping("/my/payslip")
    public ResponseEntity<?> getMyLatestPayslip() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty() || userOpt.get().getEmployee() == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Employee not found"));
            }
            
            Long employeeId = userOpt.get().getEmployee().getId();
            if (employeeService.isSystemUser(userOpt.get().getEmployee())) {
                return ResponseEntity.ok(Map.of("message", "No payslips found"));
            }
            List<PayslipDTO> payslips = payslipService.getPayslipsByEmployee(employeeId);
            
            if (payslips.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "No payslips found"));
            }
            
            // Sort by monthYear descending
            payslips.sort((a, b) -> b.getMonthYear().compareTo(a.getMonthYear()));
            return ResponseEntity.ok(payslips.get(0));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get current user's attendance summary
     * Uses LeaveRepository for leave statistics (source of truth)
     */
    @GetMapping("/my/attendance/daily")
    public ResponseEntity<?> getMyDailyAttendance(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty() || userOpt.get().getEmployee() == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Employee not found"));
            }

            Long employeeId = userOpt.get().getEmployee().getId();
            if (employeeService.isSystemUser(userOpt.get().getEmployee())) {
                return ResponseEntity.ok(List.of());
            }
            LocalDate today = LocalDate.now();
            
            // Use provided month/year or default to current
            int targetMonth = month != null ? month : today.getMonthValue();
            int targetYear = year != null ? year : today.getYear();
            YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
            
            // ✅ Use AttendanceEngine for detailed daily calculation
            List<Map<String, Object>> dailyData = attendanceEngine.getDailyAttendanceDetails(employeeId, targetYear, targetMonth);
            
            return ResponseEntity.ok(dailyData);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/my/attendance")
    public ResponseEntity<?> getMyAttendanceSummary() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty() || userOpt.get().getEmployee() == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Employee not found"));
            }

            Long employeeId = userOpt.get().getEmployee().getId();
            if (employeeService.isSystemUser(userOpt.get().getEmployee())) {
                Map<String, Object> result = new HashMap<>();
                result.put("presentDays", 0.0);
                result.put("absentDays", 0.0);
                result.put("paidUsedLeaves", 0.0);
                result.put("unpaidUsedLeaves", 0.0);
                result.put("usedLeaves", 0.0);
                result.put("remainingLeaveBalance", 0.0);
                result.put("totalEarned", 0.0);
                result.put("totalHours", 0.0);
                result.put("month", LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy")));
                return ResponseEntity.ok(result);
            }
            LocalDate today = LocalDate.now();
            YearMonth yearMonth = YearMonth.of(today.getYear(), today.getMonthValue());
            LocalDate monthStart = yearMonth.atDay(1);
            LocalDate monthEnd = yearMonth.atEndOfMonth();

            // ✅ Use calculate() instead of calculatePlanned() for accurate attendance (no future dates)
            AttendanceSummary summary = attendanceEngine.calculate(employeeId, yearMonth);

            // ✅ Use LeaveService for leave balance (Cumulative) - SINGLE SOURCE OF TRUTH
            LeaveBalanceDTO balance = leaveService.getLeaveBalance(employeeId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("presentDays", summary.getPresentDays()); // Use centralized calculation from engine
            result.put("absentDays", summary.absent); // Absent = only actual absent days, not including unpaid leave
            result.put("paidUsedLeaves", balance.getPaidLeaves()); // ✅ Cumulative Paid (7.5)
            result.put("unpaidUsedLeaves", balance.getUnpaidLeaves()); // ✅ Cumulative Unpaid (0.5)
            result.put("usedLeaves", balance.getUsedLeaves()); // ✅ Total Cumulative (8.0)
            
            result.put("remainingLeaveBalance", balance.getAvailableLeaves());
            result.put("totalEarned", balance.getTotalEarnedLeaves()); // Total earned leaves from engine
            result.put("totalHours", summary.getPresentDays() * 8.0); // ✅ Added for consistency
            
            result.put("month", today.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get attendance summary for any employee (for admin/HR use)
     * Uses LeaveRepository for leave statistics (source of truth)
     * Supports month/year parameters for future month leave applications
     */
    @GetMapping("/attendance/{employeeId}")
    public ResponseEntity<?> getEmployeeAttendanceSummary(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        try {
            LocalDate today = LocalDate.now();
            int targetMonth = (month != null) ? month : today.getMonthValue();
            int targetYear = (year != null) ? year : today.getYear();

            YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
            LocalDate monthStart = yearMonth.atDay(1);
            LocalDate monthEnd = yearMonth.atEndOfMonth();

            // ✅ Use AttendanceEngine ONLY for attendance (present/absent), NOT for leave
            AttendanceSummary summary =
                    unifiedCalculationService.calculateForPayroll(employeeId, targetYear, targetMonth);

            // ✅ Use LeaveService for leave balance (Cumulative) - SINGLE SOURCE OF TRUTH
            LeaveBalanceDTO balance = leaveService.getLeaveBalance(employeeId);

            Map<String, Object> result = new HashMap<>();
            result.put("presentDays", summary.getPresentDays()); // Consistent with my-attendance
            result.put("absentDays", summary.absent); // ✅ FIXED: Use engine value only - NO calculation
            result.put("paidUsedLeaves", balance.getPaidLeaves()); // ✅ Cumulative Paid (7.5)
            result.put("unpaidUsedLeaves", balance.getUnpaidLeaves()); // ✅ Cumulative Unpaid (0.5)
            result.put("usedLeaves", balance.getUsedLeaves()); // ✅ Total Cumulative (8.0)
            result.put("totalHours", summary.getPresentDays() * 8.0); // ✅ Restored for frontend display and consistency
            result.put("remainingLeaveBalance", balance.getAvailableLeaves());
            result.put("totalEarned", balance.getTotalEarnedLeaves());
            result.put("month", yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get leave balance for specific month (for admin leave application)
     * Shows projected balance including carry forward from previous month
     * PRODUCTION FIX: Accurate calculation using repository methods
     */
    @GetMapping("/leave-balance/{employeeId}")
    public ResponseEntity<?> getEmployeeLeaveBalanceForMonth(
            @PathVariable Long employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        try {
            YearMonth targetMonth = YearMonth.of(year, month);
            
            // ✅ Use AttendanceEngine - SINGLE SOURCE OF TRUTH
            AttendanceEngine.LeaveBalanceSummary lbSummary = 
                attendanceEngine.calculateLeaveBalance(employeeId, YearMonth.of(year, month));
            
            // Return only the correct fields for UI display
            Map<String, Object> result = new HashMap<>();
            result.put("used", lbSummary.usedLeaves);
            result.put("unpaid", lbSummary.unpaidLeaves);
            result.put("totalUsed", lbSummary.getTotalUsedLeaves());
            result.put("remaining", lbSummary.getAvailableLeaves());
            result.put("totalEarned", lbSummary.earnedLeaves);
            result.put("month", targetMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            result.put("year", year);
            result.put("monthValue", month);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

}
