package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {
    
    @Query("SELECT d FROM EmployeeDocument d JOIN FETCH d.employee WHERE d.employee.id = :employeeId")
    List<EmployeeDocument> findByEmployeeId(@Param("employeeId") Long employeeId);

    void deleteByEmployeeId(Long employeeId);
}
