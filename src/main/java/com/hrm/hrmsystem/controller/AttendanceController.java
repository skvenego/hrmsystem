package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.dto.AttendanceDTO;
import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.service.AttendanceService;
import com.hrm.hrmsystem.service.EmployeeService;
import com.hrm.hrmsystem.service.UnifiedCalculationService;
import com.hrm.hrmsystem.service.LeaveBalanceService;
import com.hrm.hrmsystem.engine.AttendanceSummary;
import com.hrm.hrmsystem.repository.LeaveRepository;
import com.hrm.hrmsystem.repository.AttendanceRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final LeaveBalanceService leaveBalanceService;
    private final LeaveRepository leaveRepository;
    private final AttendanceRepository attendanceRepository;
    private final UnifiedCalculationService unifiedCalculationService;

    public AttendanceController(AttendanceService attendanceService, EmployeeService employeeService, 
                              LeaveBalanceService leaveBalanceService, 
                              LeaveRepository leaveRepository, AttendanceRepository attendanceRepository,
                              UnifiedCalculationService unifiedCalculationService) {
        this.attendanceService = attendanceService;
        this.employeeService = employeeService;
        this.leaveBalanceService = leaveBalanceService;
        this.leaveRepository = leaveRepository;
        this.attendanceRepository = attendanceRepository;
        this.unifiedCalculationService = unifiedCalculationService;
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

    @GetMapping("/calculate")
    public ResponseEntity<AttendanceSummary> calculateAttendance(
            @RequestParam Long employeeId,
            @RequestParam int month,
            @RequestParam int year) {
        YearMonth targetMonth = YearMonth.of(year, month);
        
        // Calculate opening balance for accurate attendance calculation
        double openingBalance = 0;
        if (targetMonth.getMonthValue() > 1) {
            var prevBalance = leaveBalanceService.calculate(employeeId, YearMonth.of(year, targetMonth.getMonthValue() - 1));
            openingBalance = prevBalance.remaining;
        }
        
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
        if (employee == null) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
        
        // Use professional attendance calculation with leave integration (same as admin)
        LocalDate today = LocalDate.now();
        YearMonth targetMonth = YearMonth.of(year, month);
        
        // Get attendance summary using UnifiedCalculationService (same as payroll)
        AttendanceSummary summary = unifiedCalculationService.calculateForPayroll(employee.getId(), targetMonth.getYear(), targetMonth.getMonthValue());
        
        // Convert to daily attendance records for frontend display
        List<AttendanceDTO> result = new ArrayList<>();
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        // Get all approved leaves for this employee in the month
        List<Leave> approvedLeaves = leaveRepository.findByEmployeeIdAndStatus(employee.getId(), Leave.LeaveStatus.APPROVED);
        
        for (LocalDate currentDate = startDate; !currentDate.isAfter(endDate); currentDate = currentDate.plusDays(1)) {
            // Create a final copy for stream operations
            final LocalDate date = currentDate;
            
            // Handle Sundays - Always show as WEEK OFF
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                AttendanceDTO dto = new AttendanceDTO();
                dto.setEmployeeId(employee.getId());
                dto.setDate(date);
                dto.setStatus("WEEK_OFF");
                dto.setWorkingHours(0.0);
                
                // Check if leave exists for Sunday
                Leave leaveForDate = approvedLeaves.stream()
                        .filter(l -> l.getStartDate() != null && l.getEndDate() != null)
                        .filter(l -> !date.isBefore(l.getStartDate()) && !date.isAfter(l.getEndDate()))
                        .findFirst()
                        .orElse(null);
                
                if (leaveForDate != null) {
                    dto.setRemarks("Sunday (" + leaveForDate.getLeaveType() + " Leave Applied)");
                } else {
                    dto.setRemarks("Sunday");
                }
                
                result.add(dto);
                continue;
            }
            
            // Check if this date has approved leave
            Leave leaveForDate = approvedLeaves.stream()
                    .filter(l -> l.getStartDate() != null && l.getEndDate() != null)
                    .filter(l -> !date.isBefore(l.getStartDate()) && !date.isAfter(l.getEndDate()))
                    .findFirst()
                    .orElse(null);
            
            AttendanceDTO dto = new AttendanceDTO();
            dto.setEmployeeId(employee.getId());
            dto.setDate(currentDate);
            
            if (leaveForDate != null) {
                // This is a leave day
                if (leaveForDate.getLeaveType() == Leave.LeaveType.UNPAID) {
                    dto.setStatus("UNPAID_LEAVE");
                    dto.setWorkingHours(0.0);
                } else {
                    dto.setStatus("PAID_LEAVE");
                    dto.setWorkingHours(8.0);
                }
                // Professional remarks format: "Sick (personal work)"
                String reason = leaveForDate.getReason();
                if (reason != null && !reason.trim().isEmpty()) {
                    dto.setRemarks(leaveForDate.getLeaveType() + " (" + reason + ")");
                } else {
                    dto.setRemarks(leaveForDate.getLeaveType().toString());
                }
            } else {
                // Check actual attendance record
                var attendanceOpt = attendanceRepository.findByEmployeeIdAndDate(employee.getId(), date);
                if (attendanceOpt.isPresent() && attendanceOpt.get().getStatus() != null) {
                    Attendance attendance = attendanceOpt.get();
                    switch (attendance.getStatus()) {
                        case PRESENT:
                        case LATE:
                            dto.setStatus("PRESENT");
                            dto.setWorkingHours(attendance.getWorkingHours() != null ? attendance.getWorkingHours() : 8.0);
                            break;
                        case HALF_DAY:
                            dto.setStatus("HALF_DAY");
                            dto.setWorkingHours(attendance.getWorkingHours() != null ? attendance.getWorkingHours() : 4.0);
                            break;
                        case ABSENT:
                            dto.setStatus("ABSENT");
                            dto.setWorkingHours(0.0);
                            break;
                        case PENDING:
                            dto.setStatus("PENDING");
                            dto.setWorkingHours(0.0);
                            break;
                        default:
                            dto.setStatus("ABSENT");
                            dto.setWorkingHours(0.0);
                            break;
                    }
                    dto.setCheckInTime(attendance.getCheckInTime());
                    dto.setCheckOutTime(attendance.getCheckOutTime());
                } else {
                    // No attendance record and no leave - mark as ABSENT for past dates, Not Marked for future
                    if (date.isBefore(today)) {
                        dto.setStatus("ABSENT");
                        dto.setWorkingHours(0.0);
                    } else {
                        dto.setStatus("NOT_MARKED");
                        dto.setWorkingHours(0.0);
                    }
                }
            }
            
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