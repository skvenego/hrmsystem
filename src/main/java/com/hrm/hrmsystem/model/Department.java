package com.hrm.hrmsystem.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private String code;

    private String description;

    private String headOfDepartment;

    private LocalDate createdDate;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Employee> employees;

    // Default Constructor
    public Department() {}

    // All Args Constructor
    public Department(Long id, String name, String code, String description, String headOfDepartment,
                      LocalDate createdDate, List<Employee> employees) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.headOfDepartment = headOfDepartment;
        this.createdDate = createdDate;
        this.employees = employees;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public String getHeadOfDepartment() { return headOfDepartment; }
    public LocalDate getCreatedDate() { return createdDate; }
    public List<Employee> getEmployees() { return employees; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCode(String code) { this.code = code; }
    public void setDescription(String description) { this.description = description; }
    public void setHeadOfDepartment(String headOfDepartment) { this.headOfDepartment = headOfDepartment; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
    public void setEmployees(List<Employee> employees) { this.employees = employees; }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String code;
        private String description;
        private String headOfDepartment;
        private LocalDate createdDate;
        private List<Employee> employees;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder headOfDepartment(String headOfDepartment) { this.headOfDepartment = headOfDepartment; return this; }
        public Builder createdDate(LocalDate createdDate) { this.createdDate = createdDate; return this; }
        public Builder employees(List<Employee> employees) { this.employees = employees; return this; }

        public Department build() {
            return new Department(id, name, code, description, headOfDepartment, createdDate, employees);
        }
    }
}
