package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.LeaveBalanceDTO;
import com.hrm.hrmsystem.dto.PayslipDTO;
import com.hrm.hrmsystem.engine.AttendanceSummary;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.model.User;
import com.hrm.hrmsystem.service.AttendanceService;
import com.hrm.hrmsystem.service.EmployeeService;
import com.hrm.hrmsystem.service.LeaveBalanceService;
import com.hrm.hrmsystem.service.LeaveService;
import com.hrm.hrmsystem.service.PayslipService;
import com.hrm.hrmsystem.service.UnifiedCalculationService;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import com.hrm.hrmsystem.repository.UserRepository;
import com.hrm.hrmsystem.service.DatabaseLeaveService;
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

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final LeaveService leaveService;
    private final PayslipService payslipService;
    private final EmployeeRepository employeeRepository;
    private final LeaveRepository leaveRepository;
    private final UserRepository userRepository;
    private final UnifiedCalculationService unifiedCalculationService;
    private final DatabaseLeaveService databaseLeaveService;
    private final LeaveBalanceService leaveBalanceService;

    public DashboardController(
            AttendanceService attendanceService,
            EmployeeService employeeService,
            LeaveService leaveService,
            PayslipService payslipService,
            EmployeeRepository employeeRepository,
            LeaveRepository leaveRepository,
            UserRepository userRepository,
            UnifiedCalculationService unifiedCalculationService,
            DatabaseLeaveService databaseLeaveService,
            LeaveBalanceService leaveBalanceService) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
        this.leaveService = leaveService;
        this.payslipService = payslipService;
        this.employeeRepository = employeeRepository;
        this.leaveRepository = leaveRepository;
        this.userRepository = userRepository;
        this.unifiedCalculationService = unifiedCalculationService;
        this.databaseLeaveService = databaseLeaveService;
        this.leaveBalanceService = leaveBalanceService;
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
                
                dashboardData.put("employeeId", employeeId);
                dashboardData.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
                dashboardData.put("department", employee.getDepartment() != null ? employee.getDepartment().getName() : "N/A");
                dashboardData.put("designation", employee.getDesignation());
                
                // Get leave balance - Use AttendanceEngine (single source of truth)
                try {
                    LeaveBalanceDTO balance = leaveService.getLeaveBalance(employeeId);
                    dashboardData.put("leaveBalance", balance);
                } catch (Exception e) {
                    dashboardData.put("leaveBalance", new LeaveBalanceDTO());
                }
                
                // Get attendance summary for current month - USE LEAVE REPOSITORY AS SOURCE OF TRUTH
                try {
                    LocalDate today = LocalDate.now();
                    YearMonth yearMonth = YearMonth.of(today.getYear(), today.getMonthValue());
                    LocalDate monthStart = yearMonth.atDay(1);
                    LocalDate monthEnd = yearMonth.atEndOfMonth();

                    // ✅ DATABASE-FIRST: Use DatabaseLeaveService for leave statistics (SOURCE OF TRUTH)
                    double paidLeaveDays = databaseLeaveService.getPaidLeaveDays(employeeId, today.getYear(), today.getMonthValue());
                    double unpaidLeaveDays = databaseLeaveService.getUnpaidLeaveDays(employeeId, today.getYear(), today.getMonthValue());

                    // ✅ Use AttendanceEngine ONLY for attendance (present/absent), NOT for leave
                    AttendanceSummary summary = unifiedCalculationService.calculateForUI(employeeId, today.getYear(), today.getMonthValue());

                    final double STANDARD_HOURS_PER_DAY = 8.0;
                    double effectiveDays = summary.present + paidLeaveDays;
                    double totalHours = (summary.present + paidLeaveDays) * STANDARD_HOURS_PER_DAY;

                    dashboardData.put("attendance", Map.of(
                        "presentDays", summary.present,
                        "absentDays", summary.absent,
                        "paidLeaveDays", paidLeaveDays,
                        "unpaidLeaveDays", unpaidLeaveDays,
                        "effectiveDays", effectiveDays,
                        "totalHours", totalHours
                    ));
                } catch (Exception e) {
                    dashboardData.put("attendance", Map.of("presentDays", 0, "absentDays", 0, "paidLeaveDays", 0, "unpaidLeaveDays", 0, "totalHours", 0));
                }
                
                // Get latest payslip for salary info - use AttendanceEngine for leave data
                try {
                    List<PayslipDTO> payslips = payslipService.getPayslipsByEmployee(employeeId);
                    if (!payslips.isEmpty()) {
                        payslips.sort((a, b) -> b.getMonthYear().compareTo(a.getMonthYear()));
                        PayslipDTO latestPayslip = payslips.get(0);
                        // Override with fresh calculated leave values from UnifiedCalculationService
                        LocalDate today = LocalDate.now();
                        var lbSummary = 
                            unifiedCalculationService.calculateLeaveBalance(employeeId, today.getYear(), today.getMonthValue());
                        latestPayslip.setPaidLeaveDays(lbSummary.currentMonthUsed);
                        latestPayslip.setUnpaidLeaveDays(lbSummary.currentMonthUnpaid);
                        dashboardData.put("latestPayslip", latestPayslip);
                    } else {
                        dashboardData.put("latestPayslip", null);
                    }
                } catch (Exception e) {
                    dashboardData.put("latestPayslip", null);
                }
            }
            
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
            
            // Employee stats
            long totalEmployees = employeeRepository.count();
            long activeEmployees = employeeRepository.findAll().stream()
                    .filter(e -> e.getStatus() == Employee.EmployeeStatus.ACTIVE)
                    .count();
            
            stats.put("employees", Map.of(
                "total", totalEmployees,
                "active", activeEmployees,
                "onLeave", 0,
                "newThisMonth", 0
            ));
            
            // Attendance stats
            LocalDate today = LocalDate.now();
            stats.put("attendance", Map.of(
                "presentToday", 0, // Would need today's attendance data
                "averageHours", 0,
                "onTime", 0,
                "late", 0
            ));
            
            // Payroll stats
            String currentMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            try {
                List<PayslipDTO> monthPayslips = payslipService.getPayslipsByMonth(currentMonth);
                double totalPayroll = monthPayslips.stream()
                        .map(PayslipDTO::getNetSalary)
                        .map(BigDecimal::doubleValue)
                        .mapToDouble(Double::doubleValue)
                        .sum();
                long pendingPayslips = monthPayslips.stream()
                        .filter(p -> "DRAFT".equals(p.getStatus()))
                        .count();
                
                stats.put("payroll", Map.of(
                    "totalAmount", totalPayroll,
                    "processed", monthPayslips.size(),
                    "pending", pendingPayslips
                ));
            } catch (Exception e) {
                stats.put("payroll", Map.of("totalAmount", 0, "processed", 0, "pending", 0));
            }
            
            // Leave stats
            try {
                List<?> pendingLeaves = leaveService.getPendingLeaves();
                stats.put("leaves", Map.of(
                    "pendingRequests", pendingLeaves.size(),
                    "approvedToday", 0,
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
            LocalDate today = LocalDate.now();
            YearMonth yearMonth = YearMonth.of(today.getYear(), today.getMonthValue());
            LocalDate monthStart = yearMonth.atDay(1);
            LocalDate monthEnd = yearMonth.atEndOfMonth();

            // ✅ DATABASE-FIRST: Use DatabaseLeaveService for leave statistics (SOURCE OF TRUTH)
            double paidLeaveDays = databaseLeaveService.getPaidLeaveDays(employeeId, today.getYear(), today.getMonthValue());
            double unpaidLeaveDays = databaseLeaveService.getUnpaidLeaveDays(employeeId, today.getYear(), today.getMonthValue());

            // ✅ Use AttendanceEngine ONLY for attendance (present/absent), NOT for leave
            AttendanceSummary summary = unifiedCalculationService.calculateForUI(employeeId, today.getYear(), today.getMonthValue());

            final double STANDARD_HOURS_PER_DAY = 8.0;
            double effectiveDays = summary.present + paidLeaveDays;
            double totalHours = (summary.present + paidLeaveDays) * STANDARD_HOURS_PER_DAY;

            Map<String, Object> result = new HashMap<>();
            result.put("presentDays", summary.present);
            result.put("absentDays", summary.absent);
            result.put("paidLeaveDays", paidLeaveDays);
            result.put("unpaidLeaveDays", unpaidLeaveDays);
            result.put("effectiveDays", effectiveDays);
            result.put("totalHours", totalHours);
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

            // ✅ DATABASE-FIRST: Use DatabaseLeaveService for leave statistics (SOURCE OF TRUTH)
            double paidLeaveDays = databaseLeaveService.getPaidLeaveDays(employeeId, targetYear, targetMonth);
            double unpaidLeaveDays = databaseLeaveService.getUnpaidLeaveDays(employeeId, targetYear, targetMonth);

            // ✅ Use AttendanceEngine ONLY for attendance (present/absent), NOT for leave
            AttendanceSummary summary =
                    unifiedCalculationService.calculateForPayroll(employeeId, targetYear, targetMonth);

            final double STANDARD_HOURS_PER_DAY = 8.0;
            double effectiveDays = summary.present + paidLeaveDays;
            double totalHours = (summary.present + paidLeaveDays) * STANDARD_HOURS_PER_DAY;

            Map<String, Object> result = new HashMap<>();
            result.put("presentDays", summary.present);
            result.put("absentDays", summary.absent);
            result.put("paidLeaveDays", paidLeaveDays);
            result.put("unpaidLeaveDays", unpaidLeaveDays);
            result.put("effectiveDays", effectiveDays);
            result.put("totalHours", totalHours);
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
            
            // Use UnifiedCalculationService - SINGLE SOURCE OF TRUTH
            var lbSummary = 
                unifiedCalculationService.calculateLeaveBalance(employeeId, year, month);
            
            // Return only the correct fields for UI display
            Map<String, Object> result = new HashMap<>();
            result.put("used", lbSummary.usedLeaves);
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
