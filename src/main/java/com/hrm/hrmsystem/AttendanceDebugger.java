package com.hrm.hrmsystem;

import com.hrm.hrmsystem.model.Attendance;
import com.hrm.hrmsystem.repository.AttendanceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class AttendanceDebugger implements CommandLineRunner {
    private final AttendanceRepository attendanceRepository;

    public AttendanceDebugger(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    @Override
    public void run(String... args) {
        LocalDate today = LocalDate.now();
        List<Attendance> todayRecords = attendanceRepository.findByDate(today);
        System.out.println("DEBUG: Attendance records for " + today + ": " + todayRecords.size());
        for (Attendance a : todayRecords) {
            System.out.println("DEBUG: Employee " + a.getEmployee().getId() + " status: " + a.getStatus());
        }
    }
}
