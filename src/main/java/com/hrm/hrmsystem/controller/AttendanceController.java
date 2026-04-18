package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.AttendanceDTO;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.service.AttendanceService;
import com.hrm.hrmsystem.service.EmployeeService;
import org.springframework.format.annotation.DateTimeFormat;
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
    public ResponseEntity<AttendanceDTO> markAbsent(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(attendanceService.markAbsent(employeeId, date, remarks));
    }

    @PostMapping("/mark-present/{employeeId}")
    public ResponseEntity<AttendanceDTO> markPresent(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double workingHours) {
        if (status != null && workingHours != null) {
            return ResponseEntity.ok(attendanceService.markPresentWithOptions(employeeId, date, status, workingHours));
        } else {
            return ResponseEntity.ok(attendanceService.markPresent(employeeId, date));
        }
    }

    @PostMapping("/mark-half-day/{employeeId}")
    public ResponseEntity<AttendanceDTO> markHalfDay(
            @PathVariable Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.markHalfDay(employeeId, date));
    }

    @PutMapping("/{attendanceId}/mark-present")
    public ResponseEntity<AttendanceDTO> updateToPresent(
            @PathVariable Long attendanceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double workingHours) {
        return ResponseEntity.ok(attendanceService.updateAttendanceStatus(attendanceId, status, workingHours));
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
}