package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.model.Shift;
import com.hrm.hrmsystem.service.ShiftService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@CrossOrigin(origins = "*")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @GetMapping
    public ResponseEntity<List<Shift>> getAllShifts() {
        return ResponseEntity.ok(shiftService.getAllShifts());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Shift>> getActiveShifts() {
        return ResponseEntity.ok(shiftService.getActiveShifts());
    }

    @GetMapping("/default")
    public ResponseEntity<Shift> getDefaultShift() {
        return ResponseEntity.ok(shiftService.getDefaultShift());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shift> getShiftById(@PathVariable Long id) {
        return ResponseEntity.ok(shiftService.getShiftById(id));
    }

    @PostMapping
    public ResponseEntity<Shift> createShift(@RequestBody Shift shift) {
        return ResponseEntity.ok(shiftService.createShift(shift));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Shift> updateShift(@PathVariable Long id, @RequestBody Shift shift) {
        return ResponseEntity.ok(shiftService.updateShift(id, shift));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteShift(@PathVariable Long id) {
        shiftService.deleteShift(id);
        return ResponseEntity.ok("Shift deleted successfully");
    }

    @PostMapping("/{id}/set-default")
    public ResponseEntity<Shift> setDefaultShift(@PathVariable Long id) {
        return ResponseEntity.ok(shiftService.setDefaultShift(id));
    }
}
