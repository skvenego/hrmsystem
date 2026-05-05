package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.model.AttendanceFinalization;
import com.hrm.hrmsystem.service.AttendanceFinalizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/attendance-finalization")
public class AttendanceFinalizationController {

    @Autowired
    private AttendanceFinalizationService attendanceFinalizationService;

    @GetMapping("/check/{date}")
    public ResponseEntity<Boolean> checkAttendanceFinalization(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean isFinalized = attendanceFinalizationService.isAttendanceFinalized(date);
        return ResponseEntity.ok(isFinalized);
    }

    @GetMapping("/{startDate}/{endDate}")
    public ResponseEntity<AttendanceFinalization> getAttendanceFinalization(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Optional<AttendanceFinalization> finalization = attendanceFinalizationService.getAttendanceFinalization(startDate, endDate);
        return finalization.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/all")
    public ResponseEntity<List<AttendanceFinalization>> getAllFinalizedPeriods() {
        List<AttendanceFinalization> finalizations = attendanceFinalizationService.getAllFinalizedPeriods();
        return ResponseEntity.ok(finalizations);
    }

    @PostMapping("/finalize")
    public ResponseEntity<AttendanceFinalization> finalizeAttendance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String remarks) {
        try {
            AttendanceFinalization finalization = attendanceFinalizationService.finalizeAttendance(startDate, endDate, remarks);
            return ResponseEntity.ok(finalization);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/unfinalize")
    public ResponseEntity<AttendanceFinalization> unfinalizeAttendance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String remarks) {
        try {
            AttendanceFinalization finalization = attendanceFinalizationService.unfinalizeAttendance(startDate, endDate, remarks);
            return ResponseEntity.ok(finalization);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
