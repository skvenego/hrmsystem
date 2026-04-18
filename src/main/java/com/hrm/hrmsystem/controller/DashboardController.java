package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.LeaveBalanceDTO;
import com.hrm.hrmsystem.dto.PayslipDTO;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.User;
import com.hrm.hrmsystem.service.AttendanceService;
import com.hrm.hrmsystem.service.EmployeeService;
import com.hrm.hrmsystem.service.LeaveService;
import com.hrm.hrmsystem.service.PayslipService;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final UserRepository userRepository;

    public DashboardController(
            AttendanceService attendanceService,
            EmployeeService employeeService,
            LeaveService leaveService,
            PayslipService payslipService,
            EmployeeRepository employeeRepository,
            UserRepository userRepository) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
        this.leaveService = leaveService;
        this.payslipService = payslipService;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
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
                
                // Get leave balance
                try {
                    LeaveBalanceDTO balance = leaveService.getLeaveBalance(employeeId);
                    dashboardData.put("leaveBalance", balance);
                } catch (Exception e) {
                    dashboardData.put("leaveBalance", Map.of("casual", 0, "sick", 0, "earned", 0));
                }
                
                // Get attendance summary for current month
                try {
                    LocalDate today = LocalDate.now();
                    LocalDate monthStart = today.withDayOfMonth(1);
                    int presentDays = attendanceService.getAttendanceByEmployee(employeeId).stream()
                            .filter(a -> !a.getDate().isBefore(monthStart) && !a.getDate().isAfter(today))
                            .filter(a -> "PRESENT".equals(a.getStatus()) || "HALF_DAY".equals(a.getStatus()))
                            .mapToInt(a -> "HALF_DAY".equals(a.getStatus()) ? 1 : 1)
                            .sum();
                    double totalHours = attendanceService.getAttendanceByEmployee(employeeId).stream()
                            .filter(a -> !a.getDate().isBefore(monthStart) && !a.getDate().isAfter(today))
                            .mapToDouble(a -> a.getWorkingHours() != null ? a.getWorkingHours() : 0)
                            .sum();
                    
                    dashboardData.put("attendance", Map.of(
                        "presentDays", presentDays,
                        "totalHours", totalHours
                    ));
                } catch (Exception e) {
                    dashboardData.put("attendance", Map.of("presentDays", 0, "totalHours", 0));
                }
                
                // Get latest payslip
                try {
                    List<PayslipDTO> payslips = payslipService.getPayslipsByEmployee(employeeId);
                    if (!payslips.isEmpty()) {
                        // Sort by monthYear descending and get first
                        payslips.sort((a, b) -> b.getMonthYear().compareTo(a.getMonthYear()));
                        dashboardData.put("latestPayslip", payslips.get(0));
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
            LocalDate monthStart = today.withDayOfMonth(1);
            
            var attendanceList = attendanceService.getAttendanceByEmployee(employeeId);
            
            long presentDays = attendanceList.stream()
                    .filter(a -> !a.getDate().isBefore(monthStart) && !a.getDate().isAfter(today))
                    .filter(a -> "PRESENT".equals(a.getStatus()))
                    .count();
                    
            long halfDays = attendanceList.stream()
                    .filter(a -> !a.getDate().isBefore(monthStart) && !a.getDate().isAfter(today))
                    .filter(a -> "HALF_DAY".equals(a.getStatus()))
                    .count();
                    
            double totalHours = attendanceList.stream()
                    .filter(a -> !a.getDate().isBefore(monthStart) && !a.getDate().isAfter(today))
                    .mapToDouble(a -> a.getWorkingHours() != null ? a.getWorkingHours() : 0)
                    .sum();
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("presentDays", presentDays + (halfDays * 0.5));
            summary.put("totalHours", totalHours);
            summary.put("month", today.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
