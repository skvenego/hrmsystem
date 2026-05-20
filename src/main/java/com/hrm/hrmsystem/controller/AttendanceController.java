package com.hrm.hrmsystem.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.hrm.hrmsystem.dto.AttendanceDTO;
import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.service.AttendanceService;
import com.hrm.hrmsystem.service.EmployeeService;
import com.hrm.hrmsystem.service.UnifiedCalculationService;
import com.hrm.hrmsystem.engine.AttendanceEngine;
import com.hrm.hrmsystem.engine.AttendanceEngine.LeaveDayResult;
import com.hrm.hrmsystem.engine.AttendanceSummary;
import com.hrm.hrmsystem.repository.LeaveRepository;
import com.hrm.hrmsystem.repository.AttendanceRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {
    
    private final AttendanceService attendanceService;
    private final AttendanceEngine attendanceEngine;
    private final EmployeeService employeeService;

    private final LeaveRepository leaveRepository;
    private final AttendanceRepository attendanceRepository;
    private final UnifiedCalculationService unifiedCalculationService;
    
    public AttendanceController(AttendanceService attendanceService, EmployeeService employeeService, 
                               LeaveRepository leaveRepository, AttendanceRepository attendanceRepository,
                               UnifiedCalculationService unifiedCalculationService, AttendanceEngine attendanceEngine) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
        this.leaveRepository = leaveRepository;
        this.attendanceRepository = attendanceRepository;
        this.unifiedCalculationService = unifiedCalculationService;
        this.attendanceEngine = attendanceEngine;
    }

    @PostMapping("/check-in/{employeeId}")
    public ResponseEntity<AttendanceDTO> checkIn(@PathVariable Long employeeId) {
        return ResponseEntity.ok(attendanceService.checkIn(employeeId));
    }

    @PostMapping("/check-out/{employeeId}")
    public ResponseEntity<AttendanceDTO> checkOut(@PathVariable Long employeeId) {
        return ResponseEntity.ok(attendanceService.checkOut(employeeId));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AttendanceDTO>> getAttendanceByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByEmployee(employeeId));
    }

    @GetMapping("/employee/{employeeId}/monthly")
    public ResponseEntity<List<AttendanceDTO>> getMonthlyAttendance(
            @PathVariable Long employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return ResponseEntity.ok(attendanceService.getAttendanceReport(employeeId, startDate, endDate));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<AttendanceDTO>> getAttendanceByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.getAttendanceByDate(date));
    }

    @GetMapping("/report/{employeeId}")
    public ResponseEntity<List<AttendanceDTO>> getAttendanceReport(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(attendanceService.getAttendanceReport(employeeId, startDate, endDate));
    }

    @GetMapping("/report")
    public ResponseEntity<List<AttendanceDTO>> getAllAttendanceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(attendanceService.getAllAttendanceByDateRange(startDate, endDate));
    }

    @PostMapping("/mark-absent/{employeeId}")
    public ResponseEntity<?> markAbsent(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String remarks) {
        try {
            return ResponseEntity.ok(attendanceService.markAbsent(employeeId, date, remarks));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/mark-present/{employeeId}")
    public ResponseEntity<?> markPresent(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double workingHours) {
        try {
            if (status != null && workingHours != null) {
                return ResponseEntity.ok(attendanceService.markPresentWithOptions(employeeId, date, status, workingHours, null));
            } else {
                return ResponseEntity.ok(attendanceService.markPresent(employeeId, date));
            }
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/mark-half-day/{employeeId}")
    public ResponseEntity<?> markHalfDay(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String halfType,
            @RequestParam(required = false, defaultValue = "HALF_DAY") String status) {
        try {
            return ResponseEntity.ok(attendanceService.markHalfDay(employeeId, date, halfType, status));
        } catch (RuntimeException e) {
            System.err.println("Error marking half-day: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{attendanceId}/mark-present")
    public ResponseEntity<AttendanceDTO> updateToPresent(
            @PathVariable Long attendanceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double workingHours) {
        return ResponseEntity.ok(attendanceService.updateAttendanceStatus(attendanceId, status, workingHours));
    }

    /**
     * Admin endpoint to resolve PENDING attendance status
     * Marks PENDING attendance as either PRESENT or ABSENT
     */
    @PutMapping("/{attendanceId}/resolve-pending")
    public ResponseEntity<AttendanceDTO> resolvePendingAttendance(
            @PathVariable Long attendanceId,
            @RequestParam String newStatus, // "PRESENT" or "ABSENT"
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(attendanceService.resolvePendingAttendance(attendanceId, newStatus, remarks));
    }

    @GetMapping("/calculate")
    public ResponseEntity<AttendanceSummary> calculateAttendance(
            @RequestParam Long employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        YearMonth targetMonth = YearMonth.of(year, month);
        
        // ✅ REMOVED: Manual opening balance calculation - Use AttendanceEngine for all calculations
        // Calculate opening balance for accurate attendance calculation
        // double openingBalance = 0;
        // if (targetMonth.getMonthValue() > 1) {
        //     var prevBalance = leaveBalanceService.calculate(employeeId, YearMonth.of(year, targetMonth.getMonthValue() - 1));
        //     openingBalance = prevBalance.remaining;
        // }
        double openingBalance = 0; // Placeholder - will be calculated by AttendanceEngine
        
        // Get attendance summary using UnifiedCalculationService
        AttendanceSummary summary = unifiedCalculationService.calculateForPayroll(employeeId, targetMonth.getYear(), targetMonth.getMonthValue());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyAttendance(
            @RequestParam int month,
            @RequestParam int year) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Employee employee = employeeService.getEmployeeByEmail(email);
        if (employee == null || employeeService.isSystemUser(employee)) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        
        // Use professional attendance calculation with leave integration (same as admin)
        LocalDate today = LocalDate.now();
        YearMonth targetMonth = YearMonth.of(year, month);
        
        // Get attendance summary using UnifiedCalculationService (same as payroll)
        AttendanceSummary attendanceSummary = unifiedCalculationService.calculateForPayroll(employee.getId(), targetMonth.getYear(), targetMonth.getMonthValue());
        
        // ✅ FIXED: Use the engine's daily details for 100% consistency with payroll and dashboard
        List<AttendanceDTO> result = new ArrayList<>();
        List<java.util.Map<String, Object>> dailyDetails = attendanceEngine.getDailyAttendanceDetails(employee.getId(), year, month);
        
        for (java.util.Map<String, Object> day : dailyDetails) {
            AttendanceDTO dto = new AttendanceDTO();
            dto.setEmployeeId(employee.getId());
            dto.setDate(LocalDate.parse(day.get("date").toString()));
            if (day.get("status") != null) dto.setStatus(day.get("status").toString());
            if (day.get("halfType") != null) dto.setHalfType(day.get("halfType").toString());
            dto.setWorkingHours(day.get("workingHours") != null ? (Double) day.get("workingHours") : 0.0);
            
            if (day.get("checkInTime") != null) dto.setCheckInTime((LocalTime) day.get("checkInTime"));
            if (day.get("checkOutTime") != null) dto.setCheckOutTime((LocalTime) day.get("checkOutTime"));
            if (day.get("remarks") != null) dto.setRemarks(day.get("remarks").toString());
            
            result.add(dto);
        }
        
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<String> clearAllAttendance() {
        try {
            attendanceService.deleteAllAttendance();
            return ResponseEntity.ok("All attendance records deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting attendance records: " + e.getMessage());
        }
    }
}
