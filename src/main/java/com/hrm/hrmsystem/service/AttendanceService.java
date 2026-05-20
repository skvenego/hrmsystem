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

// ✅ REMOVED: DayOfWeek - Use AttendanceEngine for all working day calculations
// import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
        
        // ✅ NEW: Payroll Lock Check
        if (payrollLockService.isPayrollLocked(today.getMonthValue(), today.getYear())) {
            throw new RuntimeException("Payroll is locked for this month. Cannot check-in.");
        }
        
        // ✅ REMOVED: Manual Sunday check - Use AttendanceEngine for all working day calculations
        // SUNDAY CHECK: Cannot check-in on Sundays
        // if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
        //     throw new RuntimeException("Sunday is a non-working day. Cannot check-in.");
        // }
        // Note: Business workflow validation is OK, but calculation should use AttendanceEngine

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
        
        // ✅ NEW: Payroll Lock Check
        if (payrollLockService.isPayrollLocked(today.getMonthValue(), today.getYear())) {
            throw new RuntimeException("Payroll is locked for this month. Cannot check-out.");
        }
        
        // ✅ REMOVED: Manual Sunday check - Use AttendanceEngine for all working day calculations
        // SUNDAY CHECK: Cannot check-out on Sundays (shouldn't have checked in either)
        // if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
        //     throw new RuntimeException("Sunday is a non-working day.");
        // }
        // Note: Business workflow validation is OK, but calculation should use AttendanceEngine

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
            Employee employee = allEmployees.stream().filter(e -> e.getId().equals(employeeId)).findFirst().orElse(null);
            if (employee == null || isSystemUser(employee)) continue;

            // 1. Get balance for correct paid/unpaid resolution
            AttendanceEngine.LeaveBalanceSummary balance = attendanceEngine.calculateLeaveBalance(employeeId, YearMonth.from(date).minusMonths(1));
            
            // 2. Get records
            Attendance attendance = attendances.stream()
                    .filter(att -> att.getEmployee().getId().equals(employeeId))
                    .findFirst()
                    .orElse(null);
            
            List<Leave> employeeLeaves = leaveRepository.findByEmployeeIdAndStatus(employeeId, Leave.LeaveStatus.APPROVED);
            Leave leaveForDate = employeeLeaves.stream()
                    .filter(l -> l.getStartDate() != null && l.getEndDate() != null)
                    .filter(l -> !date.isBefore(l.getStartDate()) && !date.isAfter(l.getEndDate()))
                    .findFirst()
                    .orElse(null);

            // 3. Resolve using SINGLE SOURCE OF TRUTH
            AttendanceEngine.DayResult dayResult = attendanceEngine.resolveDay(employee, date, attendance, leaveForDate, balance.remaining);
            
            // 4. Map to DTO
            AttendanceDTO dto = new AttendanceDTO();
            dto.setEmployeeId(employeeId);
            dto.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());
            dto.setDate(date);
            if (attendance != null && attendance.getHalfType() != null) {
                dto.setHalfType(attendance.getHalfType().toString());
            }
            
            // Map DayResult status back to DTO status strings (Single Source of Truth)
            if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                dto.setStatus("WEEK_OFF");
                dto.setWorkingHours(0.0);
            } else if (dayResult.absent >= 1.0) {
                dto.setStatus("ABSENT");
                dto.setWorkingHours(0.0);
            } else if (dayResult.absent > 0) {
                dto.setStatus("PARTIAL_ABSENT");
                dto.setWorkingHours(dayResult.worked * 8.0);
            } else if (dayResult.paidLeave > 0) {
                dto.setStatus("PAID_LEAVE");
                dto.setWorkingHours(dayResult.paidLeave * 8.0);
            } else if (dayResult.unpaidLeave > 0) {
                dto.setStatus("UNPAID_LEAVE");
                dto.setWorkingHours(0.0);
            } else if (dayResult.worked >= 1.0) {
                dto.setStatus("PRESENT");
                dto.setWorkingHours(dayResult.worked * 8.0);
            } else if (dayResult.worked >= 0.5) {
                dto.setStatus("HALF_DAY");
                dto.setWorkingHours(dayResult.worked * 8.0);
            } else {
                dto.setStatus("NOT_MARKED");
                dto.setWorkingHours(0.0);
            }
            
            dto.setFullStatus(dayResult.status);
            
            // Set remarks for leaves
            if (leaveForDate != null) {
                dto.setRemarks(leaveForDate.getLeaveType().toString() + " (" + (leaveForDate.getIsHalfDay() ? "0.5" : "Full") + ")");
            }
    
            // Professional label for half-day leaves
            if (leaveForDate != null && leaveForDate.getIsHalfDay() != null && leaveForDate.getIsHalfDay()) {
                String halfLabel = leaveForDate.getHalfType() == Leave.HalfType.SECOND_HALF ? "Second Half" : "First Half";
                dto.setRemarks(halfLabel + " " + leaveForDate.getLeaveType() + " Leave");
            } else if (attendance != null && attendance.getRemarks() != null) {
                dto.setRemarks(attendance.getRemarks());
            } else if (dto.getRemarks() == null) {
                dto.setRemarks(date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY ? "Sunday" : "No record");
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
        
        // ✅ NEW: Payroll Lock Check
        if (payrollLockService.isPayrollLocked(date.getMonthValue(), date.getYear())) {
            throw new RuntimeException("Payroll is locked for this month. Cannot mark attendance.");
        }
        
        // STEP 3: LOCK LEAVE DAYS (Granular Check)
        List<Leave> approvedLeaves = leaveRepository.findByEmployeeId(employeeId).stream()
                .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED &&
                        !date.isBefore(l.getStartDate()) &&
                        !date.isAfter(l.getEndDate()))
                .collect(java.util.stream.Collectors.toList());

        for (Leave l : approvedLeaves) {
            boolean leaveIsHalf = (l.getIsHalfDay() != null && l.getIsHalfDay());
            if (!leaveIsHalf) {
                throw new RuntimeException("Employee is already on approved full-day leave for this date. Cannot mark attendance.");
            }
            // For general markAbsent, we block if ANY leave half matches a full day marking
            // But since this is a full day marking, any half leave would be a partial conflict.
            // We allow it here because markHalfDay is the preferred granular method.
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
        return markPresentWithOptions(employeeId, date, "PRESENT", 8.0, null);
    }

    public AttendanceDTO markHalfDay(Long employeeId, LocalDate date, String halfType, String status) {
        // Fetch existing attendance (handle potential duplicates by taking the first one)
        List<Attendance> existingList = attendanceRepository.findAllByEmployeeIdAndDate(employeeId, date);
        Optional<Attendance> existingOpt = existingList.isEmpty() ? Optional.empty() : Optional.of(existingList.get(0));
        
        boolean markingPresent = !"ABSENT".equalsIgnoreCase(status);
        boolean isFirstHalf = "FIRST_HALF".equals(halfType);
        Attendance.HalfType targetHalf = isFirstHalf ? Attendance.HalfType.FIRST_HALF : Attendance.HalfType.SECOND_HALF;
        Attendance.HalfType otherHalf = isFirstHalf ? Attendance.HalfType.SECOND_HALF : Attendance.HalfType.FIRST_HALF;
        
        // Determine the state of the OTHER half (Check BOTH manual attendance AND approved leave)
        boolean otherHalfCovered = true; // default to covered/present if no other status exists
        
        if (existingOpt.isPresent()) {
            Attendance att = existingOpt.get();
            if (att.getStatus() == Attendance.AttendanceStatus.ABSENT) {
                if (att.getHalfType() == null) {
                    // Both halves were absent
                    otherHalfCovered = false;
                } else {
                    // Only one half was absent. If the other half is indeed absent, then it is NOT covered/present.
                    otherHalfCovered = (att.getHalfType() == targetHalf);
                }
            }
        }
        
        // Check if other half has approved leave (which counts as covered)
        if (!otherHalfCovered) {
            String otherHalfStr = otherHalf.name();
            boolean otherHalfIsLeave = leaveRepository.findByEmployeeId(employeeId).stream()
                    .filter(l -> l.getStatus() == Leave.LeaveStatus.APPROVED &&
                            !date.isBefore(l.getStartDate()) &&
                            !date.isAfter(l.getEndDate()))
                    .anyMatch(l -> {
                        boolean leaveIsHalf = (l.getIsHalfDay() != null && l.getIsHalfDay());
                        if (!leaveIsHalf) return true; // Full day leave covers everything
                        if (l.getHalfType() == null) return false;
                        return l.getHalfType().name().equals(otherHalfStr);
                    });
            if (otherHalfIsLeave) {
                otherHalfCovered = true;
            }
        }

        // New state calculation
        String finalStatus;
        String finalHalfType = null;
        double finalHours;

        if (markingPresent) {
            if (otherHalfCovered) {
                finalStatus = "PRESENT";
                finalHalfType = null;
                finalHours = 8.0;
            } else {
                // target half is PRESENT, other half is ABSENT. Only other half is absent.
                finalStatus = "ABSENT";
                finalHalfType = otherHalf.name();
                finalHours = 4.0;
            }
        } else {
            if (otherHalfCovered) {
                // target half is ABSENT, other half is PRESENT/LEAVE. Only target half is absent.
                finalStatus = "ABSENT";
                finalHalfType = targetHalf.name();
                finalHours = 4.0;
            } else {
                // both halves are absent.
                finalStatus = "ABSENT";
                finalHalfType = null;
                finalHours = 0.0;
            }
        }

        return markPresentWithOptions(employeeId, date, finalStatus, finalHours, finalHalfType);
    }
    
    public AttendanceDTO markPresentWithOptions(Long employeeId, LocalDate date, String status, Double workingHours, String halfType) {
        // Validate: Cannot mark attendance for future dates
        if (date.isAfter(LocalDate.now())) {
            throw new RuntimeException("Cannot mark attendance for future dates");
        }
        
        // ✅ NEW: Payroll Lock Check
        if (payrollLockService.isPayrollLocked(date.getMonthValue(), date.getYear())) {
            throw new RuntimeException("Payroll is locked for this month. Cannot mark attendance.");
        }
        
        // STEP 3: LOCK LEAVE DAYS (Bypassed)
        // Conflict resolution is now handled by AttendanceEngine.resolveDay
        
        
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
            if (halfType != null) {
                try {
                    attendance.setHalfType(Attendance.HalfType.valueOf(halfType));
                } catch (Exception e) {
                    attendance.setHalfType(null);
                }
            } else {
                attendance.setHalfType(null);
            }
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
            
            if (halfType != null) {
                try {
                    attendance.setHalfType(Attendance.HalfType.valueOf(halfType));
                } catch (Exception e) {}
            }

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
                .halfType(attendance.getHalfType() != null ? attendance.getHalfType().name() : null)
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

        // ✅ NEW: Payroll Lock Check
        if (payrollLockService.isPayrollLocked(attendance.getDate().getMonthValue(), attendance.getDate().getYear())) {
            throw new RuntimeException("Payroll is locked for this month. Cannot resolve attendance.");
        }

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
     * Get count of employees present today
     */
    public long getPresentTodayCount() {
        LocalDate today = LocalDate.now();
        List<AttendanceDTO> attendances = getAttendanceByDate(today);
        return attendances.stream()
                .filter(a -> "PRESENT".equals(a.getStatus()) || 
                             "HALF_DAY".equals(a.getStatus()) || 
                             "LATE".equals(a.getStatus()))
                .count();
    }

    /**
     * Delete all attendance records
     */
    @Transactional
    public void deleteAllAttendance() {
        attendanceRepository.deleteAll();
    }

    private boolean isSystemUser(Employee emp) {
        if (emp.getDepartment() == null || emp.getDepartment().getName() == null) return false;
        String dept = emp.getDepartment().getName().toLowerCase();
        return dept.contains("hr") || dept.contains("director") || 
               dept.contains("leave") || dept.contains("accountant");
    }
}
