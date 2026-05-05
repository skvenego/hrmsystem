package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.AttendanceFinalization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceFinalizationRepository extends JpaRepository<AttendanceFinalization, Long> {

    Optional<AttendanceFinalization> findByStartDateAndEndDate(LocalDate startDate, LocalDate endDate);

    List<AttendanceFinalization> findByFinalizationDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByStartDateAndEndDate(LocalDate startDate, LocalDate endDate);

    List<AttendanceFinalization> findByIsFinalizedTrue();
}
