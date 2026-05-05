package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.AttendanceDTO;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.service.AttendanceService;
import com.hrm.hrmsystem.service.EmployeeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;

    public AttendanceController(AttendanceService attendanceService, EmployeeService employeeService) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
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
                return ResponseEntity.ok(attendanceService.markPresentWithOptions(employeeId, date, status, workingHours));
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            return ResponseEntity.ok(attendanceService.markHalfDay(employeeId, date));
        } catch (RuntimeException e) {
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

    @GetMapping("/my")
    public ResponseEntity<?> getMyAttendance(
            @RequestParam int month,
            @RequestParam int year) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        Employee employee = employeeService.getEmployeeByEmail(email);
        if (employee == null) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        // Calculate start and end dates from month/year
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return ResponseEntity.ok(attendanceService.getAttendanceReport(employee.getId(), startDate, endDate));
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