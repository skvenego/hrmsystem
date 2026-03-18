package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, Long> {

    List<Leave> findByEmployeeId(Long employeeId);

    List<Leave> findByStatus(Leave.LeaveStatus status);

    List<Leave> findByEmployeeIdAndStatus(Long employeeId, Leave.LeaveStatus status);
}