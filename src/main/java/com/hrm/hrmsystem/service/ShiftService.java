package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.Shift;
import com.hrm.hrmsystem.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;

    public ShiftService(ShiftRepository shiftRepository) {
        this.shiftRepository = shiftRepository;
    }

    public List<Shift> getAllShifts() {
        return shiftRepository.findAll();
    }

    public List<Shift> getActiveShifts() {
        return shiftRepository.findByActiveTrue();
    }

    public Shift getShiftById(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Shift not found with id: " + id));
    }

    public Shift getDefaultShift() {
        return shiftRepository.findByIsDefaultTrue()
                .orElseGet(() -> createDefaultShift());
    }

    @Transactional
    public Shift createShift(Shift shift) {
        if (shift.getIsDefault()) {
            // Clear existing default
            shiftRepository.findByIsDefaultTrue().ifPresent(s -> {
                s.setIsDefault(false);
                shiftRepository.save(s);
            });
        }
        return shiftRepository.save(shift);
    }

    @Transactional
    public Shift updateShift(Long id, Shift shiftDetails) {
        Shift shift = getShiftById(id);
        shift.setName(shiftDetails.getName());
        shift.setStartTime(shiftDetails.getStartTime());
        shift.setEndTime(shiftDetails.getEndTime());
        shift.setWorkingHours(shiftDetails.getWorkingHours());
        shift.setDescription(shiftDetails.getDescription());
        shift.setActive(shiftDetails.getActive());

        if (shiftDetails.getIsDefault() && !shift.getIsDefault()) {
            // Clear existing default
            shiftRepository.findByIsDefaultTrue().ifPresent(s -> {
                s.setIsDefault(false);
                shiftRepository.save(s);
            });
            shift.setIsDefault(true);
        }

        return shiftRepository.save(shift);
    }

    @Transactional
    public void deleteShift(Long id) {
        Shift shift = getShiftById(id);
        if (shift.getIsDefault()) {
            throw new RuntimeException("Cannot delete default shift");
        }
        shift.setActive(false);
        shiftRepository.save(shift);
    }

    @Transactional
    public Shift setDefaultShift(Long id) {
        // Clear existing default
        shiftRepository.findByIsDefaultTrue().ifPresent(s -> {
            s.setIsDefault(false);
            shiftRepository.save(s);
        });

        Shift shift = getShiftById(id);
        shift.setIsDefault(true);
        return shiftRepository.save(shift);
    }

    // Initialize default shifts on startup
    @Transactional
    public void initializeDefaultShifts() {
        if (shiftRepository.count() == 0) {
            // Create General Shift (10:00 - 18:30) - Default
            createDefaultShift();

            // Create Morning Shift (9:00 - 17:00)
            shiftRepository.save(Shift.builder()
                    .name("Morning Shift")
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(17, 0))
                    .workingHours(8.0)
                    .description("Standard morning shift: 9:00 AM - 5:00 PM")
                    .isDefault(false)
                    .active(true)
                    .build());

            // Create Evening Shift (14:00 - 22:00)
            shiftRepository.save(Shift.builder()
                    .name("Evening Shift")
                    .startTime(LocalTime.of(14, 0))
                    .endTime(LocalTime.of(22, 0))
                    .workingHours(8.0)
                    .description("Evening shift: 2:00 PM - 10:00 PM")
                    .isDefault(false)
                    .active(true)
                    .build());

            // Create Half Day Shift (10:00 - 14:30)
            shiftRepository.save(Shift.builder()
                    .name("Half Day Shift")
                    .startTime(LocalTime.of(10, 0))
                    .endTime(LocalTime.of(14, 30))
                    .workingHours(4.5)
                    .description("Half day shift: 10:00 AM - 2:30 PM")
                    .isDefault(false)
                    .active(true)
                    .build());

            System.out.println("✅ Default shifts initialized");
        }
    }

    private Shift createDefaultShift() {
        return shiftRepository.save(Shift.builder()
                .name("General Shift")
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(18, 30))
                .workingHours(8.5)
                .description("General shift: 10:00 AM - 6:30 PM (Default)")
                .isDefault(true)
                .active(true)
                .build());
    }
}
