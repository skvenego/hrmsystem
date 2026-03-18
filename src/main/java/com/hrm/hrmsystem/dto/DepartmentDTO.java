package com.hrm.hrmsystem.dto;

import java.time.LocalDate;

public class DepartmentDTO {

    private Long id;
    private String name;
    private String description;
    private String headOfDepartment;
    private LocalDate createdDate;
    private Integer employeeCount;

    public DepartmentDTO() {}

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getHeadOfDepartment() { return headOfDepartment; }
    public LocalDate getCreatedDate() { return createdDate; }
    public Integer getEmployeeCount() { return employeeCount; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setHeadOfDepartment(String headOfDepartment) { this.headOfDepartment = headOfDepartment; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
    public void setEmployeeCount(Integer employeeCount) { this.employeeCount = employeeCount; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String description;
        private String headOfDepartment;
        private LocalDate createdDate;
        private Integer employeeCount;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder headOfDepartment(String headOfDepartment) { this.headOfDepartment = headOfDepartment; return this; }
        public Builder createdDate(LocalDate createdDate) { this.createdDate = createdDate; return this; }
        public Builder employeeCount(Integer employeeCount) { this.employeeCount = employeeCount; return this; }

        public DepartmentDTO build() {
            DepartmentDTO dto = new DepartmentDTO();
            dto.id = this.id;
            dto.name = this.name;
            dto.description = this.description;
            dto.headOfDepartment = this.headOfDepartment;
            dto.createdDate = this.createdDate;
            dto.employeeCount = this.employeeCount;
            return dto;
        }
    }
}
