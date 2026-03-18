package com.hrm.hrmsystem.dto;

import java.time.LocalDate;

public class PayrollDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String department;
    private String designation;

    private Integer month;
    private Integer year;

    private Double basicSalary;
    private Double hra;
    private Double da;
    private Double ta;
    private Double otherAllowances;

    private Double providentFund;
    private Double tax;
    private Double insurance;
    private Double otherDeductions;

    private Double grossSalary;
    private Double totalDeductions;
    private Double netSalary;

    private LocalDate paymentDate;
    private String status;

    public PayrollDTO() {}

    // Getters
    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getDepartment() { return department; }
    public String getDesignation() { return designation; }
    public Integer getMonth() { return month; }
    public Integer getYear() { return year; }
    public Double getBasicSalary() { return basicSalary; }
    public Double getHra() { return hra; }
    public Double getDa() { return da; }
    public Double getTa() { return ta; }
    public Double getOtherAllowances() { return otherAllowances; }
    public Double getProvidentFund() { return providentFund; }
    public Double getTax() { return tax; }
    public Double getInsurance() { return insurance; }
    public Double getOtherDeductions() { return otherDeductions; }
    public Double getGrossSalary() { return grossSalary; }
    public Double getTotalDeductions() { return totalDeductions; }
    public Double getNetSalary() { return netSalary; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public String getStatus() { return status; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public void setDepartment(String department) { this.department = department; }
    public void setDesignation(String designation) { this.designation = designation; }
    public void setMonth(Integer month) { this.month = month; }
    public void setYear(Integer year) { this.year = year; }
    public void setBasicSalary(Double basicSalary) { this.basicSalary = basicSalary; }
    public void setHra(Double hra) { this.hra = hra; }
    public void setDa(Double da) { this.da = da; }
    public void setTa(Double ta) { this.ta = ta; }
    public void setOtherAllowances(Double otherAllowances) { this.otherAllowances = otherAllowances; }
    public void setProvidentFund(Double providentFund) { this.providentFund = providentFund; }
    public void setTax(Double tax) { this.tax = tax; }
    public void setInsurance(Double insurance) { this.insurance = insurance; }
    public void setOtherDeductions(Double otherDeductions) { this.otherDeductions = otherDeductions; }
    public void setGrossSalary(Double grossSalary) { this.grossSalary = grossSalary; }
    public void setTotalDeductions(Double totalDeductions) { this.totalDeductions = totalDeductions; }
    public void setNetSalary(Double netSalary) { this.netSalary = netSalary; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public void setStatus(String status) { this.status = status; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private String department;
        private String designation;
        private Integer month;
        private Integer year;
        private Double basicSalary;
        private Double hra;
        private Double da;
        private Double ta;
        private Double otherAllowances;
        private Double providentFund;
        private Double tax;
        private Double insurance;
        private Double otherDeductions;
        private Double grossSalary;
        private Double totalDeductions;
        private Double netSalary;
        private LocalDate paymentDate;
        private String status;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder employeeId(Long employeeId) { this.employeeId = employeeId; return this; }
        public Builder employeeName(String employeeName) { this.employeeName = employeeName; return this; }
        public Builder department(String department) { this.department = department; return this; }
        public Builder designation(String designation) { this.designation = designation; return this; }
        public Builder month(Integer month) { this.month = month; return this; }
        public Builder year(Integer year) { this.year = year; return this; }
        public Builder basicSalary(Double basicSalary) { this.basicSalary = basicSalary; return this; }
        public Builder hra(Double hra) { this.hra = hra; return this; }
        public Builder da(Double da) { this.da = da; return this; }
        public Builder ta(Double ta) { this.ta = ta; return this; }
        public Builder otherAllowances(Double otherAllowances) { this.otherAllowances = otherAllowances; return this; }
        public Builder providentFund(Double providentFund) { this.providentFund = providentFund; return this; }
        public Builder tax(Double tax) { this.tax = tax; return this; }
        public Builder insurance(Double insurance) { this.insurance = insurance; return this; }
        public Builder otherDeductions(Double otherDeductions) { this.otherDeductions = otherDeductions; return this; }
        public Builder grossSalary(Double grossSalary) { this.grossSalary = grossSalary; return this; }
        public Builder totalDeductions(Double totalDeductions) { this.totalDeductions = totalDeductions; return this; }
        public Builder netSalary(Double netSalary) { this.netSalary = netSalary; return this; }
        public Builder paymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; return this; }
        public Builder status(String status) { this.status = status; return this; }

        public PayrollDTO build() {
            PayrollDTO dto = new PayrollDTO();
            dto.id = this.id;
            dto.employeeId = this.employeeId;
            dto.employeeName = this.employeeName;
            dto.department = this.department;
            dto.designation = this.designation;
            dto.month = this.month;
            dto.year = this.year;
            dto.basicSalary = this.basicSalary;
            dto.hra = this.hra;
            dto.da = this.da;
            dto.ta = this.ta;
            dto.otherAllowances = this.otherAllowances;
            dto.providentFund = this.providentFund;
            dto.tax = this.tax;
            dto.insurance = this.insurance;
            dto.otherDeductions = this.otherDeductions;
            dto.grossSalary = this.grossSalary;
            dto.totalDeductions = this.totalDeductions;
            dto.netSalary = this.netSalary;
            dto.paymentDate = this.paymentDate;
            dto.status = this.status;
            return dto;
        }
    }
}
