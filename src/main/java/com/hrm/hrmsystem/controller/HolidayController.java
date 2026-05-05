package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.model.Holiday;
import com.hrm.hrmsystem.service.HolidayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    @Autowired
    private HolidayService holidayService;

    @GetMapping("/check/{date}")
    public ResponseEntity<Boolean> checkHoliday(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        boolean isHoliday = holidayService.isHoliday(date);
        return ResponseEntity.ok(isHoliday);
    }

    @GetMapping("/range/{startDate}/{endDate}")
    public ResponseEntity<List<Holiday>> getHolidaysInRange(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<Holiday> holidays = holidayService.getHolidaysInRange(startDate, endDate);
        return ResponseEntity.ok(holidays);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Holiday>> getAllActiveHolidays() {
        List<Holiday> holidays = holidayService.getAllActiveHolidays();
        return ResponseEntity.ok(holidays);
    }

    @GetMapping("/{date}")
    public ResponseEntity<Holiday> getHolidayByDate(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Optional<Holiday> holiday = holidayService.getHolidayByDate(date);
        return holiday.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public ResponseEntity<Holiday> createHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "false") Boolean isRecurring) {
        Holiday holiday = holidayService.createHoliday(date, name, description, isRecurring);
        return ResponseEntity.ok(holiday);
    }

    @PutMapping("/{holidayId}")
    public ResponseEntity<Holiday> updateHoliday(
            @PathVariable Long holidayId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean isRecurring,
            @RequestParam(required = false) Boolean isActive) {
        try {
            Holiday holiday = holidayService.updateHoliday(holidayId, date, name, description, isRecurring, isActive);
            return ResponseEntity.ok(holiday);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{holidayId}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long holidayId) {
        holidayService.deleteHoliday(holidayId);
        return ResponseEntity.ok().build();
    }
}
