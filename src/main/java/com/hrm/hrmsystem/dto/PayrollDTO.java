package com.hrm.hrmsystem.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class PayrollDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String department;
    private String designation;

    private Integer month;
    private Integer year;

    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal da;
    private BigDecimal ta;
    private BigDecimal otherAllowances;

    private BigDecimal providentFund;
    private BigDecimal tax;
    private String insuranceName;
    private BigDecimal insurance;
    private BigDecimal otherDeductions;

    private BigDecimal grossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;

    private LocalDate paymentDate;
    private String status;
    
    // Attendance details (for display in frontend)
    private Integer presentDays;
    private Integer absentDays;
    private Integer leaveDays;
    private Integer paidLeaveDays;
    private Integer unpaidLeaveDays;
    private Integer halfDays;
    private Double absentLeaveDeduction;

}
