package com.hrm.hrmsystem.dto;

public class NotificationPreferenceDTO {
    private Boolean emailNotifications;
    private Boolean pushNotifications;
    private Boolean smsNotifications;
    private Boolean leaveUpdates;
    private Boolean payrollUpdates;
    private Boolean attendanceUpdates;

    public NotificationPreferenceDTO() {}

    // Getters and Setters
    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    
    public Boolean getPushNotifications() { return pushNotifications; }
    public void setPushNotifications(Boolean pushNotifications) { this.pushNotifications = pushNotifications; }
    
    public Boolean getSmsNotifications() { return smsNotifications; }
    public void setSmsNotifications(Boolean smsNotifications) { this.smsNotifications = smsNotifications; }
    
    public Boolean getLeaveUpdates() { return leaveUpdates; }
    public void setLeaveUpdates(Boolean leaveUpdates) { this.leaveUpdates = leaveUpdates; }
    
    public Boolean getPayrollUpdates() { return payrollUpdates; }
    public void setPayrollUpdates(Boolean payrollUpdates) { this.payrollUpdates = payrollUpdates; }
    
    public Boolean getAttendanceUpdates() { return attendanceUpdates; }
    public void setAttendanceUpdates(Boolean attendanceUpdates) { this.attendanceUpdates = attendanceUpdates; }
}
