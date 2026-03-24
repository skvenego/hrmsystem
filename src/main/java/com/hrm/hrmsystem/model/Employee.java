package com.hrm.hrmsystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hrm.hrmsystem.entity.Payslip;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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
        ACTIVE, INACTIVE, ON_LEAVE, TERMINATED
    }

    // Default Constructor
    public Employee() {}

    // All Args Constructor
    public Employee(Long id, String firstName, String lastName, String email, String phone,
                    Department department, String designation, LocalDate joiningDate, BigDecimal salary,
                    BigDecimal basicSalary, BigDecimal da, BigDecimal hra, BigDecimal otherAllowance,
                    EmployeeStatus status, String address, List<Leave> leaves, List<Attendance> attendances,
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
        public Builder status(EmployeeStatus status) { this.status = status; return this; }
        public Builder address(String address) { this.address = address; return this; }
        public Builder leaves(List<Leave> leaves) { this.leaves = leaves; return this; }
        public Builder attendances(List<Attendance> attendances) { this.attendances = attendances; return this; }
        public Builder payrolls(List<Payroll> payrolls) { this.payrolls = payrolls; return this; }
        public Builder leaveBalances(List<LeaveBalance> leaveBalances) { this.leaveBalances = leaveBalances; return this; }
        public Builder payslips(List<Payslip> payslips) { this.payslips = payslips; return this; }

        public Employee build() {
            return new Employee(id, firstName, lastName, email, phone, department, designation,
                    joiningDate, salary, basicSalary, da, hra, otherAllowance, status, address, 
                    leaves, attendances, payrolls, leaveBalances, payslips);
        }
    }
}
