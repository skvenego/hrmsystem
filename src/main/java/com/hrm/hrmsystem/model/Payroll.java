package com.hrm.hrmsystem.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "payroll",
    uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "month", "year"})
)
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

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

    @Enumerated(EnumType.STRING)
    private PayrollStatus status;

    // Dual approval fields
    private Boolean accountantApproved = false;
    private Boolean directorApproved = false;
    private Boolean payslipGenerated = false;
    private LocalDate finalApprovalDate;

    public enum PayrollStatus {
        PENDING, PROCESSED, PENDING_APPROVAL, PAID
    }

    // Default Constructor
    public Payroll() {}

    // All Args Constructor
    public Payroll(Long id, Employee employee, Integer month, Integer year,
                   Double basicSalary, Double hra, Double da, Double ta, Double otherAllowances,
                   Double providentFund, Double tax, Double insurance, Double otherDeductions,
                   Double grossSalary, Double totalDeductions, Double netSalary,
                   LocalDate paymentDate, PayrollStatus status) {
        this.id = id;
        this.employee = employee;
        this.month = month;
        this.year = year;
        this.basicSalary = basicSalary;
        this.hra = hra;
        this.da = da;
        this.ta = ta;
        this.otherAllowances = otherAllowances;
        this.providentFund = providentFund;
        this.tax = tax;
        this.insurance = insurance;
        this.otherDeductions = otherDeductions;
        this.grossSalary = grossSalary;
        this.totalDeductions = totalDeductions;
        this.netSalary = netSalary;
        this.paymentDate = paymentDate;
        this.status = status;
    }

    // Getters
    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
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
    public PayrollStatus getStatus() { return status; }
    public Boolean getAccountantApproved() { return accountantApproved; }
    public Boolean getDirectorApproved() { return directorApproved; }
    public Boolean getPayslipGenerated() { return payslipGenerated; }
    public LocalDate getFinalApprovalDate() { return finalApprovalDate; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmployee(Employee employee) { this.employee = employee; }
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
    public void setStatus(PayrollStatus status) { this.status = status; }
    public void setAccountantApproved(Boolean accountantApproved) { this.accountantApproved = accountantApproved; }
    public void setDirectorApproved(Boolean directorApproved) { this.directorApproved = directorApproved; }
    public void setPayslipGenerated(Boolean payslipGenerated) { this.payslipGenerated = payslipGenerated; }
    public void setFinalApprovalDate(LocalDate finalApprovalDate) { this.finalApprovalDate = finalApprovalDate; }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Employee employee;
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
        private PayrollStatus status;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder employee(Employee employee) { this.employee = employee; return this; }
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
        public Builder status(PayrollStatus status) { this.status = status; return this; }

        public Payroll build() {
            return new Payroll(id, employee, month, year, basicSalary, hra, da, ta, otherAllowances,
                    providentFund, tax, insurance, otherDeductions, grossSalary, totalDeductions, netSalary, paymentDate, status);
        }
    }
}
