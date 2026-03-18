package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.AttendanceDTO;
import com.hrm.hrmsystem.service.AttendanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(attendanceService.markPresent(employeeId, date));
    }

    @PutMapping("/{attendanceId}/mark-present")
    public ResponseEntity<AttendanceDTO> updateToPresent(
            @PathVariable Long attendanceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double workingHours) {
        return ResponseEntity.ok(attendanceService.updateAttendanceStatus(attendanceId, status, workingHours));
    }
}