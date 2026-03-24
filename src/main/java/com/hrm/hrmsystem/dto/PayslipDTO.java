package com.hrm.hrmsystem.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PayslipDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private String monthYear;
    
    // Earnings
    private BigDecimal basicSalary;
    private BigDecimal da;
    private BigDecimal hra;
    private BigDecimal otherAllowance;
    private BigDecimal grossSalary;
    
    // Deductions
    private BigDecimal pf;
    private BigDecimal esi;
    private BigDecimal incomeTax;
    private BigDecimal otherDeduction;
    private BigDecimal totalDeduction;
    private BigDecimal absentLeaveDeduction;

    // Net Salary
    private BigDecimal netSalary;
    
    // Attendance
    private Integer presentDays;
    private Integer absentDays;
    private Integer leaveDays;
    private Integer halfDays;
    private Integer workingDays;
    private Integer totalDays;
    private java.util.List<String> absentDates; // List of absent dates
    
    // Status
    private String status;
    // Payroll status for the same month/year (used to show payslip only when payroll is PAID)
    private String payrollStatus;
    private LocalDate generatedDate;
    private LocalDate approvedDate;
    private String approvedBy;
    private String remarks;
    private String pdfFilePath;

    public PayslipDTO() {}

    // Getters
    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getMonthYear() { return monthYear; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public BigDecimal getDa() { return da; }
    public BigDecimal getHra() { return hra; }
    public BigDecimal getOtherAllowance() { return otherAllowance; }
    public BigDecimal getGrossSalary() { return grossSalary; }
    public BigDecimal getPf() { return pf; }
    public BigDecimal getEsi() { return esi; }
    public BigDecimal getIncomeTax() { return incomeTax; }
    public BigDecimal getOtherDeduction() { return otherDeduction; }
    public BigDecimal getTotalDeduction() { return totalDeduction; }
    public BigDecimal getAbsentLeaveDeduction() { return absentLeaveDeduction; }
    public BigDecimal getNetSalary() { return netSalary; }
    public Integer getPresentDays() { return presentDays; }
    public Integer getAbsentDays() { return absentDays; }
    public Integer getLeaveDays() { return leaveDays; }
    public Integer getHalfDays() { return halfDays; }
    public Integer getWorkingDays() { return workingDays; }
    public Integer getTotalDays() { return totalDays; }
    public java.util.List<String> getAbsentDates() { return absentDates; }
    public String getStatus() { return status; }
    public String getPayrollStatus() { return payrollStatus; }
    public LocalDate getGeneratedDate() { return generatedDate; }
    public LocalDate getApprovedDate() { return approvedDate; }
    public String getApprovedBy() { return approvedBy; }
    public String getRemarks() { return remarks; }
    public String getPdfFilePath() { return pdfFilePath; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public void setMonthYear(String monthYear) { this.monthYear = monthYear; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    public void setDa(BigDecimal da) { this.da = da; }
    public void setHra(BigDecimal hra) { this.hra = hra; }
    public void setOtherAllowance(BigDecimal otherAllowance) { this.otherAllowance = otherAllowance; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }
    public void setPf(BigDecimal pf) { this.pf = pf; }
    public void setEsi(BigDecimal esi) { this.esi = esi; }
    public void setIncomeTax(BigDecimal incomeTax) { this.incomeTax = incomeTax; }
    public void setOtherDeduction(BigDecimal otherDeduction) { this.otherDeduction = otherDeduction; }
    public void setTotalDeduction(BigDecimal totalDeduction) { this.totalDeduction = totalDeduction; }
    public void setAbsentLeaveDeduction(BigDecimal absentLeaveDeduction) { this.absentLeaveDeduction = absentLeaveDeduction; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
    public void setPresentDays(Integer presentDays) { this.presentDays = presentDays; }
    public void setAbsentDays(Integer absentDays) { this.absentDays = absentDays; }
    public void setLeaveDays(Integer leaveDays) { this.leaveDays = leaveDays; }
    public void setHalfDays(Integer halfDays) { this.halfDays = halfDays; }
    public void setWorkingDays(Integer workingDays) { this.workingDays = workingDays; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }
    public void setAbsentDates(java.util.List<String> absentDates) { this.absentDates = absentDates; }
    public void setStatus(String status) { this.status = status; }
    public void setPayrollStatus(String payrollStatus) { this.payrollStatus = payrollStatus; }
    public void setGeneratedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; }
    public void setApprovedDate(LocalDate approvedDate) { this.approvedDate = approvedDate; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setPdfFilePath(String pdfFilePath) { this.pdfFilePath = pdfFilePath; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private String monthYear;
        private BigDecimal basicSalary;
        private BigDecimal da;
        private BigDecimal hra;
        private BigDecimal otherAllowance;
        private BigDecimal grossSalary;
        private BigDecimal pf;
        private BigDecimal esi;
        private BigDecimal incomeTax;
        private BigDecimal otherDeduction;
        private BigDecimal totalDeduction;
        private BigDecimal netSalary;
        private Integer presentDays;
        private Integer absentDays;
        private Integer leaveDays;
        private Integer workingDays;
        private Integer totalDays;
        private java.util.List<String> absentDates;
        private String status;
        private String payrollStatus;
        private LocalDate generatedDate;
        private LocalDate approvedDate;
        private String approvedBy;
        private String remarks;
        private String pdfFilePath;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder employeeId(Long employeeId) { this.employeeId = employeeId; return this; }
        public Builder employeeName(String employeeName) { this.employeeName = employeeName; return this; }
        public Builder monthYear(String monthYear) { this.monthYear = monthYear; return this; }
        public Builder basicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; return this; }
        public Builder da(BigDecimal da) { this.da = da; return this; }
        public Builder hra(BigDecimal hra) { this.hra = hra; return this; }
        public Builder otherAllowance(BigDecimal otherAllowance) { this.otherAllowance = otherAllowance; return this; }
        public Builder grossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; return this; }
        public Builder pf(BigDecimal pf) { this.pf = pf; return this; }
        public Builder esi(BigDecimal esi) { this.esi = esi; return this; }
        public Builder incomeTax(BigDecimal incomeTax) { this.incomeTax = incomeTax; return this; }
        public Builder otherDeduction(BigDecimal otherDeduction) { this.otherDeduction = otherDeduction; return this; }
        public Builder totalDeduction(BigDecimal totalDeduction) { this.totalDeduction = totalDeduction; return this; }
        public Builder netSalary(BigDecimal netSalary) { this.netSalary = netSalary; return this; }
        public Builder presentDays(Integer presentDays) { this.presentDays = presentDays; return this; }
        public Builder absentDays(Integer absentDays) { this.absentDays = absentDays; return this; }
        public Builder leaveDays(Integer leaveDays) { this.leaveDays = leaveDays; return this; }
        public Builder workingDays(Integer workingDays) { this.workingDays = workingDays; return this; }
        public Builder totalDays(Integer totalDays) { this.totalDays = totalDays; return this; }
        public Builder absentDates(java.util.List<String> absentDates) { this.absentDates = absentDates; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder payrollStatus(String payrollStatus) { this.payrollStatus = payrollStatus; return this; }
        public Builder generatedDate(LocalDate generatedDate) { this.generatedDate = generatedDate; return this; }
        public Builder approvedDate(LocalDate approvedDate) { this.approvedDate = approvedDate; return this; }
        public Builder approvedBy(String approvedBy) { this.approvedBy = approvedBy; return this; }
        public Builder remarks(String remarks) { this.remarks = remarks; return this; }
        public Builder pdfFilePath(String pdfFilePath) { this.pdfFilePath = pdfFilePath; return this; }

        public PayslipDTO build() {
            PayslipDTO dto = new PayslipDTO();
            dto.id = this.id;
            dto.employeeId = this.employeeId;
            dto.employeeName = this.employeeName;
            dto.monthYear = this.monthYear;
            dto.basicSalary = this.basicSalary;
            dto.da = this.da;
            dto.hra = this.hra;
            dto.otherAllowance = this.otherAllowance;
            dto.grossSalary = this.grossSalary;
            dto.pf = this.pf;
            dto.esi = this.esi;
            dto.incomeTax = this.incomeTax;
            dto.otherDeduction = this.otherDeduction;
            dto.totalDeduction = this.totalDeduction;
            dto.netSalary = this.netSalary;
            dto.presentDays = this.presentDays;
            dto.absentDays = this.absentDays;
            dto.leaveDays = this.leaveDays;
            dto.workingDays = this.workingDays;
            dto.totalDays = this.totalDays;
            dto.absentDates = this.absentDates;
            dto.status = this.status;
            dto.payrollStatus = this.payrollStatus;
            dto.generatedDate = this.generatedDate;
            dto.approvedDate = this.approvedDate;
            dto.approvedBy = this.approvedBy;
            dto.remarks = this.remarks;
            dto.pdfFilePath = this.pdfFilePath;
            return dto;
        }
    }
}
