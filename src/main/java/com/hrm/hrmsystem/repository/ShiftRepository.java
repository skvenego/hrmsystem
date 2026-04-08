package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    Optional<Shift> findByName(String name);

    Optional<Shift> findByIsDefaultTrue();

    java.util.List<Shift> findByActiveTrue();
}
