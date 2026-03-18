package com.hrm.hrmsystem.dto;

public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String role;
    private Long employeeId;

    public RegisterRequest() {}

    public RegisterRequest(String username, String email, String password, String role, Long employeeId) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.employeeId = employeeId;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
}
