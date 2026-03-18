package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.dto.AttendanceDTO;
import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.repository.AttendanceRepository;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    private static final LocalTime STANDARD_CHECK_IN = LocalTime.of(9, 0);
    private static final double STANDARD_WORKING_HOURS = 8.0;

    public AttendanceDTO checkIn(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LocalDate today = LocalDate.now();

        if (attendanceRepository.findByEmployeeIdAndDate(employeeId, today).isPresent()) {
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
        return attendanceRepository.findByDate(date)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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
            return convertToDTO(attendance);
        }
    }

    public AttendanceDTO markPresent(Long employeeId, LocalDate date) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // Check if already has attendance for this date
        List<Attendance> existingAttendances = attendanceRepository.findAllByEmployeeIdAndDate(employeeId, date);
        
        if (existingAttendances != null && !existingAttendances.isEmpty()) {
            // If duplicates exist, update the first one and delete others
            Attendance attendance = existingAttendances.get(0);
            attendance.setStatus(Attendance.AttendanceStatus.PRESENT);
            attendance.setWorkingHours(8.0);
            attendance = attendanceRepository.save(attendance);
            
            // Delete duplicate records
            if (existingAttendances.size() > 1) {
                for (int i = 1; i < existingAttendances.size(); i++) {
                    attendanceRepository.delete(existingAttendances.get(i));
                }
            }
            
            return convertToDTO(attendance);
        } else {
            // Create new record with check-in at 10:00 AM and check-out at 6:30 PM
            Attendance attendance = Attendance.builder()
                    .employee(employee)
                    .date(date)
                    .checkInTime(java.time.LocalTime.of(10, 0)) // 10:00 AM
                    .checkOutTime(java.time.LocalTime.of(18, 30)) // 6:30 PM
                    .workingHours(8.5) // 8 hours 30 minutes
                    .status(Attendance.AttendanceStatus.PRESENT)
                    .build();

            attendance = attendanceRepository.save(attendance);
            return convertToDTO(attendance);
        }
    }

    public AttendanceDTO updateAttendanceStatus(Long attendanceId, String status, Double workingHours) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));

        if (status != null) {
            attendance.setStatus(Attendance.AttendanceStatus.valueOf(status));
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
}