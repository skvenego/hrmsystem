package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.Holiday;
import com.hrm.hrmsystem.repository.HolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class HolidayService {

    @Autowired
    private HolidayRepository holidayRepository;

    /**
     * Check if a specific date is a holiday
     */
    public boolean isHoliday(LocalDate date) {
        // Check for exact date match
        Optional<Holiday> holidayOpt = holidayRepository.findByDate(date);
        if (holidayOpt.isPresent() && holidayOpt.get().getIsActive()) {
            return true;
        }

        // Check for recurring holidays (same month and day)
        MonthDay monthDay = MonthDay.from(date);
        List<Holiday> recurringHolidays = holidayRepository.findByIsRecurringTrueAndIsActiveTrue();
        for (Holiday holiday : recurringHolidays) {
            if (holiday.getDate() != null) {
                MonthDay holidayMonthDay = MonthDay.from(holiday.getDate());
                if (monthDay.equals(holidayMonthDay)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Get all holidays in a date range
     */
    public List<Holiday> getHolidaysInRange(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findByDateBetween(startDate, endDate);
    }

    /**
     * Get all active holidays
     */
    public List<Holiday> getAllActiveHolidays() {
        return holidayRepository.findByIsActiveTrue();
    }

    /**
     * Create a new holiday
     */
    public Holiday createHoliday(LocalDate date, String name, String description, Boolean isRecurring) {
        Holiday holiday = new Holiday(date, name, description, isRecurring);
        return holidayRepository.save(holiday);
    }

    /**
     * Update an existing holiday
     */
    public Holiday updateHoliday(Long holidayId, LocalDate date, String name, String description, 
                                 Boolean isRecurring, Boolean isActive) {
        Optional<Holiday> holidayOpt = holidayRepository.findById(holidayId);
        if (holidayOpt.isEmpty()) {
            throw new RuntimeException("Holiday not found with ID: " + holidayId);
        }

        Holiday holiday = holidayOpt.get();
        if (date != null) holiday.setDate(date);
        if (name != null) holiday.setName(name);
        if (description != null) holiday.setDescription(description);
        if (isRecurring != null) holiday.setIsRecurring(isRecurring);
        if (isActive != null) holiday.setIsActive(isActive);

        return holidayRepository.save(holiday);
    }

    /**
     * Delete a holiday
     */
    public void deleteHoliday(Long holidayId) {
        holidayRepository.deleteById(holidayId);
    }

    /**
     * Get holiday by date
     */
    public Optional<Holiday> getHolidayByDate(LocalDate date) {
        return holidayRepository.findByDate(date);
    }
}
