package com.hrm.hrmsystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hrm.hrmsystem.entity.Payslip;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    private String designation;

    private LocalDate joiningDate;

    private BigDecimal salary;

    // Salary components for payslip calculation
    private BigDecimal basicSalary;
    private BigDecimal da;
    private BigDecimal hra;
    private BigDecimal otherAllowance;
    
    // Deductions
    private BigDecimal pf;
    private BigDecimal tax;
    @Column(name = "insurance_name")
    @JsonProperty("insuranceName")
    private String insuranceName;
    @Column(name = "insurance_percentage")
    @JsonProperty("insurancePercentage")
    private Double insurancePercentage; // Store as percentage (e.g., 0.4 for 0.4%)
    
    // Gender
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    // Probation period
    private Integer probationPeriodMonths;
    
    // Shift assignment
    @ManyToOne
    @JoinColumn(name = "shift_id")
    private Shift shift;
    
    // Audit timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;

    private String address;

    // Cascade delete relationships - when employee is deleted, related records are also deleted
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Leave> leaves = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Attendance> attendances = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Payroll> payrolls = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<LeaveBalance> leaveBalances = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Payslip> payslips = new ArrayList<>();

    public enum EmployeeStatus {
        ACTIVE, INACTIVE, ON_LEAVE, TERMINATED, PROBATION
    }
    
    public enum Gender {
        MALE, FEMALE, OTHER
    }

    // Default Constructor
    public Employee() {}

    // All Args Constructor
    public Employee(Long id, String firstName, String lastName, String email, String phone,
                    Department department, String designation, LocalDate joiningDate, BigDecimal salary,
                    BigDecimal basicSalary, BigDecimal da, BigDecimal hra, BigDecimal otherAllowance,
                    BigDecimal pf, BigDecimal tax, Gender gender, Integer probationPeriodMonths, Shift shift, EmployeeStatus status, 
                    String address, List<Leave> leaves, List<Attendance> attendances,
                    List<Payroll> payrolls, List<LeaveBalance> leaveBalances, List<Payslip> payslips) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.designation = designation;
        this.joiningDate = joiningDate;
        this.salary = salary;
        this.basicSalary = basicSalary;
        this.da = da;
        this.hra = hra;
        this.otherAllowance = otherAllowance;
        this.pf = pf;
        this.tax = tax;
        this.gender = gender;
        this.probationPeriodMonths = probationPeriodMonths;
        this.shift = shift;
        this.status = status;
        this.address = address;
        this.leaves = leaves;
        this.attendances = attendances;
        this.payrolls = payrolls;
        this.leaveBalances = leaveBalances;
        this.payslips = payslips;
    }

    // Getters
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Department getDepartment() { return department; }
    public String getDesignation() { return designation; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public BigDecimal getSalary() { return salary; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public BigDecimal getDa() { return da; }
    public BigDecimal getHra() { return hra; }
    public BigDecimal getOtherAllowance() { return otherAllowance; }
    public BigDecimal getPf() { return pf; }
    public BigDecimal getTax() { return tax; }
    public String getInsuranceName() { return insuranceName; }
    public Double getInsurancePercentage() { return insurancePercentage; }
    public Gender getGender() { return gender; }
    public Integer getProbationPeriodMonths() { return probationPeriodMonths; }
    public Shift getShift() { return shift; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public EmployeeStatus getStatus() { return status; }
    public String getAddress() { return address; }
    public List<Leave> getLeaves() { return leaves; }
    public List<Attendance> getAttendances() { return attendances; }
    public List<Payroll> getPayrolls() { return payrolls; }
    public List<LeaveBalance> getLeaveBalances() { return leaveBalances; }
    public List<Payslip> getPayslips() { return payslips; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDepartment(Department department) { this.department = department; }
    public void setDesignation(String designation) { this.designation = designation; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    public void setDa(BigDecimal da) { this.da = da; }
    public void setHra(BigDecimal hra) { this.hra = hra; }
    public void setOtherAllowance(BigDecimal otherAllowance) { this.otherAllowance = otherAllowance; }
    public void setPf(BigDecimal pf) { this.pf = pf; }
    public void setTax(BigDecimal tax) { this.tax = tax; }
    public void setInsuranceName(String insuranceName) { this.insuranceName = insuranceName; }
    public void setInsurancePercentage(Double insurancePercentage) { this.insurancePercentage = insurancePercentage; }
    public void setGender(Gender gender) { this.gender = gender; }
    public void setProbationPeriodMonths(Integer probationPeriodMonths) { this.probationPeriodMonths = probationPeriodMonths; }
    public void setShift(Shift shift) { this.shift = shift; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setStatus(EmployeeStatus status) { this.status = status; }
    public void setAddress(String address) { this.address = address; }
    public void setLeaves(List<Leave> leaves) { this.leaves = leaves; }
    public void setAttendances(List<Attendance> attendances) { this.attendances = attendances; }
    public void setPayrolls(List<Payroll> payrolls) { this.payrolls = payrolls; }
    public void setLeaveBalances(List<LeaveBalance> leaveBalances) { this.leaveBalances = leaveBalances; }
    public void setPayslips(List<Payslip> payslips) { this.payslips = payslips; }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private Department department;
        private String designation;
        private LocalDate joiningDate;
        private BigDecimal salary;
        private BigDecimal basicSalary;
        private BigDecimal da;
        private BigDecimal hra;
        private BigDecimal otherAllowance;
        private BigDecimal pf;
        private BigDecimal tax;
        private String insuranceName;
        private Double insurancePercentage;
        private Gender gender;
        private Integer probationPeriodMonths;
        private Shift shift;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private EmployeeStatus status;
        private String address;
        private List<Leave> leaves = new ArrayList<>();
        private List<Attendance> attendances = new ArrayList<>();
        private List<Payroll> payrolls = new ArrayList<>();
        private List<LeaveBalance> leaveBalances = new ArrayList<>();
        private List<Payslip> payslips = new ArrayList<>();

        public Builder id(Long id) { this.id = id; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder department(Department department) { this.department = department; return this; }
        public Builder designation(String designation) { this.designation = designation; return this; }
        public Builder joiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; return this; }
        public Builder salary(BigDecimal salary) { this.salary = salary; return this; }
        public Builder basicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; return this; }
        public Builder da(BigDecimal da) { this.da = da; return this; }
        public Builder hra(BigDecimal hra) { this.hra = hra; return this; }
        public Builder otherAllowance(BigDecimal otherAllowance) { this.otherAllowance = otherAllowance; return this; }
        public Builder pf(BigDecimal pf) { this.pf = pf; return this; }
        public Builder tax(BigDecimal tax) { this.tax = tax; return this; }
        public Builder insuranceName(String insuranceName) { this.insuranceName = insuranceName; return this; }
        public Builder insurancePercentage(Double insurancePercentage) { this.insurancePercentage = insurancePercentage; return this; }
        public Builder gender(Gender gender) { this.gender = gender; return this; }
        public Builder probationPeriodMonths(Integer probationPeriodMonths) { this.probationPeriodMonths = probationPeriodMonths; return this; }
        public Builder shift(Shift shift) { this.shift = shift; return this; }
        public Builder status(EmployeeStatus status) { this.status = status; return this; }
        public Builder address(String address) { this.address = address; return this; }
        public Builder leaves(List<Leave> leaves) { this.leaves = leaves; return this; }
        public Builder attendances(List<Attendance> attendances) { this.attendances = attendances; return this; }
        public Builder payrolls(List<Payroll> payrolls) { this.payrolls = payrolls; return this; }
        public Builder leaveBalances(List<LeaveBalance> leaveBalances) { this.leaveBalances = leaveBalances; return this; }
        public Builder payslips(List<Payslip> payslips) { this.payslips = payslips; return this; }

        public Employee build() {
            Employee employee = new Employee(id, firstName, lastName, email, phone, department, designation,
                    joiningDate, salary, basicSalary, da, hra, otherAllowance, pf, tax, gender, probationPeriodMonths, shift, status, address,
                    leaves, attendances, payrolls, leaveBalances, payslips);
            employee.setInsuranceName(insuranceName);
            employee.setInsurancePercentage(insurancePercentage);
            return employee;
        }
    }
}
