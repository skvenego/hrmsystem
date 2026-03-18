package com.hrm.hrmsystem.dto;

public class AuthResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private Long employeeId;
    private String employeeName;
    private String token;
    private String message;

    public AuthResponse() {}

    public AuthResponse(Long id, String username, String email, String role, Long employeeId,
                        String employeeName, String token, String message) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.token = token;
        this.message = message;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String username;
        private String email;
        private String role;
        private Long employeeId;
        private String employeeName;
        private String token;
        private String message;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder employeeId(Long employeeId) { this.employeeId = employeeId; return this; }
        public Builder employeeName(String employeeName) { this.employeeName = employeeName; return this; }
        public Builder token(String token) { this.token = token; return this; }
        public Builder message(String message) { this.message = message; return this; }

        public AuthResponse build() {
            return new AuthResponse(id, username, email, role, employeeId, employeeName, token, message);
        }
    }
}
