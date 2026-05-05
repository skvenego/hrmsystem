package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.AttendanceDTO;
import com.hrm.hrmsystem.engine.AttendanceEngine;
import com.hrm.hrmsystem.engine.AttendanceSummary;
import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.repository.AttendanceRepository;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.repository.LeaveRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final PayslipService payslipService;
    private final LeaveRepository leaveRepository;
    private final AuditLogService auditLogService;
    private final PayrollLockService payrollLockService;
    private final AttendanceEngine attendanceEngine;

    public AttendanceService(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository, @Lazy PayslipService payslipService, LeaveRepository leaveRepository, AuditLogService auditLogService, PayrollLockService payrollLockService, AttendanceEngine attendanceEngine) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.payslipService = payslipService;
        this.leaveRepository = leaveRepository;
        this.auditLogService = auditLogService;
        this.payrollLockService = payrollLockService;
        this.attendanceEngine = attendanceEngine;
    }

    private static final LocalTime STANDARD_CHECK_IN = LocalTime.of(9, 0);
    private static final double STANDARD_WORKING_HOURS = 8.0;

    /**
     * Helper method to regenerate payslip after attendance changes
     * Frontend displays payslip data, so we must regenerate payslip when attendance changes
     */
    private void regeneratePayrollForAttendanceChange(Long employeeId, LocalDate date) {
        try {
            int month = date.getMonthValue();
            int year = date.getYear();

            // Check if payroll is locked for this month
            if (payrollLockService.isPayrollLocked(month, year)) {
                System.out.println("Payroll is locked for " + year + "-" + month + ". Skipping payslip regeneration.");
                return;
            }

            String monthYear = year + "-" + String.format("%02d", month);
            System.out.println("Regenerating payslip for employee " + employeeId + " for " + monthYear);
            payslipService.generatePayslip(employeeId, monthYear);
            System.out.println("Successfully regenerated payslip for employee " + employeeId + " for " + monthYear);
        } catch (Exception e) {
            System.err.println("Failed to regenerate payslip after attendance change: " + e.getMessage());
            e.printStackTrace();
            // Don't throw exception - attendance should still be saved even if payslip regeneration fails
        }
    }

    public AttendanceDTO checkIn(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LocalDate today = LocalDate.now();
        
        // SUNDAY CHECK: Cannot check-in on Sundays
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new RuntimeException("Sunday is a non-working day. Cannot check-in.");
        }

        List<Attendance> records = attendanceRepository.findAllByEmployeeIdAndDate(employeeId, today);

        if (!records.isEmpty() && records.get(0).getCheckInTime() != null) {
            throw new RuntimeException("Already checked in today");
        }

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .date(today)
                .checkInTime(LocalTime.now())
                .status(LocalTime.now().isAfter(STANDARD_CHECK_IN) 
                        ? Attendance.AttendanceStatus.LATE 
                        : Attendance.AttendanceStatus.PRESENT)
                .build();

        attendance = attendanceRepository.save(attendance);
        return convertToDTO(attendance);
    }

    public AttendanceDTO checkOut(Long employeeId) {
        LocalDate today = LocalDate.now();
        
        // SUNDAY CHECK: Cannot check-out on Sundays (shouldn't have checked in either)
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new RuntimeException("Sunday is a non-working day.");
        }

        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, today)
                .orElseThrow(() -> new RuntimeException("No check-in found for today"));

        if (attendance.getCheckOutTime() != null) {
            throw new RuntimeException("Already checked out today");
        }

        LocalTime checkOutTime = LocalTime.now();
        attendance.setCheckOutTime(checkOutTime);

        // Calculate working hours
        Duration duration = Duration.between(attendance.getCheckInTime(), checkOutTime);
        double workingHours = duration.toMinutes() / 60.0;
        attendance.setWorkingHours(Math.round(workingHours * 100.0) / 100.0);

        // Calculate overtime
        if (workingHours > STANDARD_WORKING_HOURS) {
            attendance.setOvertimeHours(Math.round((workingHours - STANDARD_WORKING_HOURS) * 100.0) / 100.0);
        } else {
            attendance.setOvertimeHours(0.0);
        }

        // Update status for half day
        if (workingHours < 4) {
            attendance.setStatus(Attendance.AttendanceStatus.HALF_DAY);
        }

        attendance = attendanceRepository.save(attendance);
        return convertToDTO(attendance);
    }

    public List<AttendanceDTO> getAttendanceByEmployee(Long employeeId) {
        return attendanceRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AttendanceDTO> getAttendanceByDate(LocalDate date) {
        // SUNDAY CHECK: Still show approved leaves on Sundays (leaves apply even on non-working days)
        // Get all employees who have attendance or leave records for this date
        List<Attendance> attendances = attendanceRepository.findByDate(date);
        Set<Long> employeeIds = new HashSet<>();
        
        // Add employees with attendance records
        attendances.forEach(att -> employeeIds.add(att.getEmployee().getId()));
        
        // Add employees with approved leave records for this date
        // Get all employees first, then check for leaves for each
        List<Employee> allEmployees = employeeRepository.findAll();
        Set<Long> allEmployeeIds = allEmployees.stream().map(Employee::getId).collect(Collectors.toSet());
        
        // Add all employees to the list (we'll check leaves for each)
        employeeIds.addAll(allEmployeeIds);
        
        // For each employee, calculate professional attendance using AttendanceEngine
        List<AttendanceDTO> result = new ArrayList<>();
        
        for (Long employeeId : employeeIds) {
            // Use AttendanceEngine for professional calculation
            YearMonth month = YearMonth.from(date);
            double openingBalance = 0; // For daily view, opening balance not critical
            AttendanceSummary summary = attendanceEngine.calculate(employeeId, month, openingBalance);
            
            // Create attendance record for this specific date
            AttendanceDTO dto = new AttendanceDTO();
            dto.setEmployeeId(employeeId);
            dto.setDate(date);
            
            // Check if this specific date has leave for this employee
            List<Leave> employeeLeaves = leaveRepository.findByEmployeeIdAndStatus(employeeId, Leave.LeaveStatus.APPROVED);
            Leave leaveForDate = employeeLeaves.stream()
                    .filter(l -> l.getStartDate() != null && l.getEndDate() != null)
                    .filter(l -> !date.isBefore(l.getStartDate()) && !date.isAfter(l.getEndDate()))
                    .findFirst()
                    .orElse(null);
            
            if (leaveForDate != null) {
                // This is a leave day
                if (leaveForDate.getLeaveType() == Leave.LeaveType.UNPAID) {
                    dto.setStatus("UNPAID_LEAVE");
                    dto.setWorkingHours(0.0);
                } else {
                    dto.setStatus("PAID_LEAVE");
                    dto.setWorkingHours(8.0); // Standard working hours for paid leave
                }
                dto.setRemarks(leaveForDate.getLeaveType() + " leave");
            } else {
                // Check actual attendance record
                Attendance attendance = attendances.stream()
                        .filter(att -> att.getEmployee().getId().equals(employeeId))
                        .findFirst()
                        .orElse(null);
                
                if (attendance != null && attendance.getStatus() != null) {
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
                    dto.setRemarks(attendance.getRemarks());
                } else {
                    // No attendance record and no leave - mark as absent
                    dto.setStatus("ABSENT");
                    dto.setWorkingHours(0.0);
                    dto.setRemarks("No attendance record");
                }
            }
            
            result.add(dto);
        }
        
        return result;
    }

    public List<AttendanceDTO> getAttendanceReport(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AttendanceDTO> getAllAttendanceByDateRange(LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByDateBetween(startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public AttendanceDTO markAbsent(Long employeeId, LocalDate date, String remarks) {
        // Validate: Cannot mark attendance for future dates
        if (date.isAfter(LocalDate.now())) {
            throw new RuntimeException("Cannot mark attendance for future dates");
        }
        
        // SUNDAY CHECK: Cannot mark attendance on Sundays
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new RuntimeException("Sunday is a non-working day. Attendance cannot be marked.");
        }
        
        // STEP 3: LOCK LEAVE DAYS - Check if employee is on approved leave
        boolean isOnLeave = leaveRepository.findByEmployeeId(employeeId).stream()
                .anyMatch(l -> l.getStatus() == Leave.LeaveStatus.APPROVED &&
                        !date.isBefore(l.getStartDate()) &&
                        !date.isAfter(l.getEndDate()));
        
        if (isOnLeave) {
            throw new RuntimeException("Employee is already on approved leave for this date. Cannot mark attendance.");
        }
        
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Check for existing records
        List<Attendance> existingAttendances = attendanceRepository.findAllByEmployeeIdAndDate(employeeId, date);
        
        if (existingAttendances != null && !existingAttendances.isEmpty()) {
            // Update first record and delete duplicates
            Attendance attendance = existingAttendances.get(0);
            attendance.setStatus(Attendance.AttendanceStatus.ABSENT);
            attendance.setRemarks(remarks);
            attendance = attendanceRepository.save(attendance);
            
            // Delete duplicate records
            if (existingAttendances.size() > 1) {
                for (int i = 1; i < existingAttendances.size(); i++) {
                    attendanceRepository.delete(existingAttendances.get(i));
                }
            }
            
            System.out.println("Marked ABSENT for employee " + employeeId + " on date " + date);
            return convertToDTO(attendance);
        } else {
            // Create new record
            Attendance attendance = Attendance.builder()
                    .employee(employee)
                    .date(date)
                    .status(Attendance.AttendanceStatus.ABSENT)
                    .remarks(remarks)
                    .build();

            attendance = attendanceRepository.save(attendance);
            System.out.println("Created new ABSENT record for employee " + employeeId + " on date " + date);
            return convertToDTO(attendance);
        }
    }

    public AttendanceDTO markPresent(Long employeeId, LocalDate date) {
        return markPresentWithOptions(employeeId, date, "PRESENT", 8.0);
    }

    public AttendanceDTO markHalfDay(Long employeeId, LocalDate date) {
        return markPresentWithOptions(employeeId, date, "HALF_DAY", 4.0);
    }
    
    public AttendanceDTO markPresentWithOptions(Long employeeId, LocalDate date, String status, Double workingHours) {
        // Validate: Cannot mark attendance for future dates
        if (date.isAfter(LocalDate.now())) {
            throw new RuntimeException("Cannot mark attendance for future dates");
        }
        
        // SUNDAY CHECK: Cannot mark attendance on Sundays
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new RuntimeException("Sunday is a non-working day. Attendance cannot be marked.");
        }
        
        // STEP 3: LOCK LEAVE DAYS - Check if employee is on approved leave
        boolean isOnLeave = leaveRepository.findByEmployeeId(employeeId).stream()
                .anyMatch(l -> l.getStatus() == Leave.LeaveStatus.APPROVED &&
                        !date.isBefore(l.getStartDate()) &&
                        !date.isAfter(l.getEndDate()));
        
        if (isOnLeave) {
            throw new RuntimeException("Employee is already on approved leave for this date. Cannot mark attendance.");
        }
        
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Check if already has attendance for this date
        List<Attendance> existingAttendances = attendanceRepository.findAllByEmployeeIdAndDate(employeeId, date);
        
        if (existingAttendances != null && !existingAttendances.isEmpty()) {
            // If duplicates exist, update the first one and delete others
            Attendance attendance = existingAttendances.get(0);
            try {
                attendance.setStatus(Attendance.AttendanceStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                attendance.setStatus(Attendance.AttendanceStatus.PRESENT);
            }
            attendance.setWorkingHours(workingHours != null ? workingHours : 8.0);
            attendance = attendanceRepository.save(attendance);
            
            // Delete duplicate records
            if (existingAttendances.size() > 1) {
                for (int i = 1; i < existingAttendances.size(); i++) {
                    attendanceRepository.delete(existingAttendances.get(i));
                }
            }
            
            System.out.println("Marked " + status + " for employee " + employeeId + " on date " + date + " with " + workingHours + " hours");
            return convertToDTO(attendance);
        } else {
            // Create new record with default times
            Attendance.AttendanceStatus attendanceStatus;
            try {
                attendanceStatus = Attendance.AttendanceStatus.valueOf(status);
            } catch (Exception e) {
                attendanceStatus = Attendance.AttendanceStatus.PRESENT;
            }

            double resolvedHours = workingHours != null
                    ? workingHours
                    : (attendanceStatus == Attendance.AttendanceStatus.HALF_DAY ? 4.0 : 8.5);

            Attendance attendance = Attendance.builder()
                    .employee(employee)
                    .date(date)
                    .checkInTime(java.time.LocalTime.of(10, 0)) // 10:00 AM
                    .checkOutTime(java.time.LocalTime.of(18, 30)) // 6:30 PM
                    .workingHours(resolvedHours)
                    .status(attendanceStatus)
                    .build();

            attendance = attendanceRepository.save(attendance);
            System.out.println("Created new PRESENT record for employee " + employeeId + " on date " + date);
            return convertToDTO(attendance);
        }
    }

    public AttendanceDTO updateAttendanceStatus(Long attendanceId, String status, Double workingHours) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found for ID: " + attendanceId));

        if (status != null) {
            try {
                attendance.setStatus(Attendance.AttendanceStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid status value: " + status);
            }
        }

        if (workingHours != null) {
            attendance.setWorkingHours(workingHours);
        }

        attendance = attendanceRepository.save(attendance);
        return convertToDTO(attendance);
    }

    private AttendanceDTO convertToDTO(Attendance attendance) {
        return AttendanceDTO.builder()
                .id(attendance.getId())
                .employeeId(attendance.getEmployee().getId())
                .employeeName(attendance.getEmployee().getFirstName() + " " + attendance.getEmployee().getLastName())
                .date(attendance.getDate())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .workingHours(attendance.getWorkingHours())
                .overtimeHours(attendance.getOvertimeHours())
                .status(attendance.getStatus() != null ? attendance.getStatus().name() : null)
                .remarks(attendance.getRemarks())
                .build();
    }

    /**
     * Resolve PENDING attendance status
     * Admin can mark PENDING attendance as either PRESENT or ABSENT
     * Handles half-day leave + remaining half scenarios
     */
    @Transactional
    public AttendanceDTO resolvePendingAttendance(Long attendanceId, String newStatus, String remarks) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found for ID: " + attendanceId));

        System.out.println("Resolving attendance ID " + attendanceId + " for employee " + attendance.getEmployee().getId() + " on date " + attendance.getDate() + " from " + attendance.getStatus() + " to " + newStatus);

        // Validate that current status is PENDING
        if (attendance.getStatus() != Attendance.AttendanceStatus.PENDING) {
            throw new RuntimeException("Can only resolve PENDING attendance. Current status: " + attendance.getStatus());
        }

        // Validate new status
        Attendance.AttendanceStatus resolvedStatus;
        try {
            resolvedStatus = Attendance.AttendanceStatus.valueOf(newStatus);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status value: " + newStatus + ". Must be PRESENT or ABSENT");
        }

        // Only allow PRESENT or ABSENT
        if (resolvedStatus != Attendance.AttendanceStatus.PRESENT && resolvedStatus != Attendance.AttendanceStatus.ABSENT) {
            throw new RuntimeException("Can only resolve to PRESENT or ABSENT. Provided: " + newStatus);
        }

        LocalDate attendanceDate = attendance.getDate();
        List<Leave> halfDayLeaves = leaveRepository.findByEmployeeId(attendance.getEmployee().getId()).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED)
                .filter(l -> l.getIsHalfDay() != null && l.getIsHalfDay())
                .filter(l -> l.getStartDate() != null && l.getStartDate().equals(attendanceDate))
                .collect(java.util.stream.Collectors.toList());

        boolean hasHalfDayLeave = !halfDayLeaves.isEmpty();
        String resolutionType = hasHalfDayLeave ? " (half-day leave + remaining half)" : "";

        // Log the change before updating
        String oldValue = attendance.getStatus().name();
        String newValue = newStatus;

        // Update status
        attendance.setStatus(resolvedStatus);
        if (remarks != null && !remarks.trim().isEmpty()) {
            attendance.setRemarks(remarks);
        } else {
            attendance.setRemarks("Resolved from PENDING to " + newStatus + " by admin" + resolutionType);
        }

        attendance = attendanceRepository.save(attendance);
        System.out.println("Saved attendance ID " + attendanceId + " with new status " + attendance.getStatus() + resolutionType);

        // Audit log
        auditLogService.logChange(
            "ATTENDANCE",
            attendanceId,
            "RESOLVE_PENDING",
            oldValue,
            newValue,
            attendance.getEmployee().getId(),
            remarks != null ? remarks : "Resolved from PENDING to " + newStatus + resolutionType
        );

        regeneratePayrollForAttendanceChange(attendance.getEmployee().getId(), attendance.getDate());
        return convertToDTO(attendance);
    }

    /**
     * Delete all attendance records
     */
    @Transactional
    public void deleteAllAttendance() {
        attendanceRepository.deleteAll();
    }
}