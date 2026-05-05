package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.PayrollLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayrollLockRepository extends JpaRepository<PayrollLock, Long> {

    Optional<PayrollLock> findByMonthAndYear(Integer month, Integer year);

    boolean existsByMonthAndYear(Integer month, Integer year);
}
