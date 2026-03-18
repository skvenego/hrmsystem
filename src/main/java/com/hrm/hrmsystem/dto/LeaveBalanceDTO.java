package com.hrm.hrmsystem.dto;

public class LeaveBalanceDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    private Integer sickLeave;
    private Integer casualLeave;
    private Integer annualLeave;
    private Integer maternityLeave;
    private Integer paternityLeave;
    private Integer year;

    public LeaveBalanceDTO() {}

    // Getters
    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public Integer getSickLeave() { return sickLeave; }
    public Integer getCasualLeave() { return casualLeave; }
    public Integer getAnnualLeave() { return annualLeave; }
    public Integer getMaternityLeave() { return maternityLeave; }
    public Integer getPaternityLeave() { return paternityLeave; }
    public Integer getYear() { return year; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public void setSickLeave(Integer sickLeave) { this.sickLeave = sickLeave; }
    public void setCasualLeave(Integer casualLeave) { this.casualLeave = casualLeave; }
    public void setAnnualLeave(Integer annualLeave) { this.annualLeave = annualLeave; }
    public void setMaternityLeave(Integer maternityLeave) { this.maternityLeave = maternityLeave; }
    public void setPaternityLeave(Integer paternityLeave) { this.paternityLeave = paternityLeave; }
    public void setYear(Integer year) { this.year = year; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private Integer sickLeave;
        private Integer casualLeave;
        private Integer annualLeave;
        private Integer maternityLeave;
        private Integer paternityLeave;
        private Integer year;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder employeeId(Long employeeId) { this.employeeId = employeeId; return this; }
        public Builder employeeName(String employeeName) { this.employeeName = employeeName; return this; }
        public Builder sickLeave(Integer sickLeave) { this.sickLeave = sickLeave; return this; }
        public Builder casualLeave(Integer casualLeave) { this.casualLeave = casualLeave; return this; }
        public Builder annualLeave(Integer annualLeave) { this.annualLeave = annualLeave; return this; }
        public Builder maternityLeave(Integer maternityLeave) { this.maternityLeave = maternityLeave; return this; }
        public Builder paternityLeave(Integer paternityLeave) { this.paternityLeave = paternityLeave; return this; }
        public Builder year(Integer year) { this.year = year; return this; }

        public LeaveBalanceDTO build() {
            LeaveBalanceDTO dto = new LeaveBalanceDTO();
            dto.id = this.id;
            dto.employeeId = this.employeeId;
            dto.employeeName = this.employeeName;
            dto.sickLeave = this.sickLeave;
            dto.casualLeave = this.casualLeave;
            dto.annualLeave = this.annualLeave;
            dto.maternityLeave = this.maternityLeave;
            dto.paternityLeave = this.paternityLeave;
            dto.year = this.year;
            return dto;
        }
    }
}
