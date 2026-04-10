package com.hrm.hrmsystem.dto;

public class LeaveBalanceDTO {

    private Long id;
    private Long employeeId;
    private String employeeName;
    
    // Simplified leave balance
    private Integer totalEarnedLeaves;
    private Integer usedLeaves;
    private Integer unpaidLeaves;
    private Integer carriedForwardLeaves;
    private Integer availableLeaves;
    
    // Cycle info
    private Integer cycle;
    private Integer year;
    
    // Probation info
    private Boolean inProbation;
    private String probationStatus;

    public LeaveBalanceDTO() {}

    // Getters
    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public Integer getTotalEarnedLeaves() { return totalEarnedLeaves; }
    public Integer getUsedLeaves() { return usedLeaves; }
    public Integer getUnpaidLeaves() { return unpaidLeaves; }
    public Integer getCarriedForwardLeaves() { return carriedForwardLeaves; }
    public Integer getAvailableLeaves() { return availableLeaves; }
    public Integer getCycle() { return cycle; }
    public Integer getYear() { return year; }
    public Boolean getInProbation() { return inProbation; }
    public String getProbationStatus() { return probationStatus; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public void setTotalEarnedLeaves(Integer totalEarnedLeaves) { this.totalEarnedLeaves = totalEarnedLeaves; }
    public void setUsedLeaves(Integer usedLeaves) { this.usedLeaves = usedLeaves; }
    public void setUnpaidLeaves(Integer unpaidLeaves) { this.unpaidLeaves = unpaidLeaves; }
    public void setCarriedForwardLeaves(Integer carriedForwardLeaves) { this.carriedForwardLeaves = carriedForwardLeaves; }
    public void setAvailableLeaves(Integer availableLeaves) { this.availableLeaves = availableLeaves; }
    public void setCycle(Integer cycle) { this.cycle = cycle; }
    public void setYear(Integer year) { this.year = year; }
    public void setInProbation(Boolean inProbation) { this.inProbation = inProbation; }
    public void setProbationStatus(String probationStatus) { this.probationStatus = probationStatus; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long employeeId;
        private String employeeName;
        private Integer totalEarnedLeaves;
        private Integer usedLeaves;
        private Integer unpaidLeaves;
        private Integer carriedForwardLeaves;
        private Integer availableLeaves;
        private Integer cycle;
        private Integer year;
        private Boolean inProbation;
        private String probationStatus;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder employeeId(Long employeeId) { this.employeeId = employeeId; return this; }
        public Builder employeeName(String employeeName) { this.employeeName = employeeName; return this; }
        public Builder totalEarnedLeaves(Integer totalEarnedLeaves) { this.totalEarnedLeaves = totalEarnedLeaves; return this; }
        public Builder usedLeaves(Integer usedLeaves) { this.usedLeaves = usedLeaves; return this; }
        public Builder unpaidLeaves(Integer unpaidLeaves) { this.unpaidLeaves = unpaidLeaves; return this; }
        public Builder carriedForwardLeaves(Integer carriedForwardLeaves) { this.carriedForwardLeaves = carriedForwardLeaves; return this; }
        public Builder availableLeaves(Integer availableLeaves) { this.availableLeaves = availableLeaves; return this; }
        public Builder cycle(Integer cycle) { this.cycle = cycle; return this; }
        public Builder year(Integer year) { this.year = year; return this; }
        public Builder inProbation(Boolean inProbation) { this.inProbation = inProbation; return this; }
        public Builder probationStatus(String probationStatus) { this.probationStatus = probationStatus; return this; }

        public LeaveBalanceDTO build() {
            LeaveBalanceDTO dto = new LeaveBalanceDTO();
            dto.id = this.id;
            dto.employeeId = this.employeeId;
            dto.employeeName = this.employeeName;
            dto.totalEarnedLeaves = this.totalEarnedLeaves;
            dto.usedLeaves = this.usedLeaves;
            dto.unpaidLeaves = this.unpaidLeaves;
            dto.carriedForwardLeaves = this.carriedForwardLeaves;
            dto.availableLeaves = this.availableLeaves;
            dto.cycle = this.cycle;
            dto.year = this.year;
            dto.inProbation = this.inProbation;
            dto.probationStatus = this.probationStatus;
            return dto;
        }
    }
}
