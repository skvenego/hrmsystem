package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.Employee;
import com.hrm.hrmsystem.model.Leave;
import com.hrm.hrmsystem.model.Payroll;
import com.hrm.hrmsystem.repository.EmployeeRepository;
import com.hrm.hrmsystem.util.EmailUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    @Autowired
    private EmailUtil emailUtil;

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * Send payroll payment notification to employee
     */
    public void sendPayrollPaymentNotification(Payroll payroll) {
        try {
            Employee employee = payroll.getEmployee();
            if (employee == null || employee.getEmail() == null) {
                return;
            }

            String toEmail = employee.getEmail();
            String subject = "Salary Payment Notification - " + getMonthYear(payroll.getMonth(), payroll.getYear());
            
            // Safe null handling for Double values
            double basicSalary = payroll.getBasicSalary() != null ? payroll.getBasicSalary() : 0.0;
            double allowances = (payroll.getHra() != null ? payroll.getHra() : 0.0) + 
                               (payroll.getDa() != null ? payroll.getDa() : 0.0) + 
                               (payroll.getTa() != null ? payroll.getTa() : 0.0);
            double deductions = payroll.getTotalDeductions() != null ? payroll.getTotalDeductions() : 0.0;
            double netSalary = payroll.getNetSalary() != null ? payroll.getNetSalary() : 0.0;
            
            String htmlBody = String.format(
                "<html><body>" +
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #4f46e5;'>💰 Salary Payment Notification</h2>" +
                "<p>Dear %s,</p>" +
                "<p>Your salary for <strong>%s %d</strong> has been processed and paid.</p>" +
                "<div style='background: #f3f4f6; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                "<h3>Payment Details:</h3>" +
                "<table style='width: 100%%; border-collapse: collapse;'>" +
                "<tr><td style='padding: 8px 0;'><strong>Basic Salary:</strong></td><td style='text-align: right;'>₹%,.2f</td></tr>" +
                "<tr><td style='padding: 8px 0;'><strong>Allowances:</strong></td><td style='text-align: right;'>₹%,.2f</td></tr>" +
                "<tr><td style='padding: 8px 0;'><strong>Deductions:</strong></td><td style='text-align: right;'>₹%,.2f</td></tr>" +
                "<tr style='border-top: 2px solid #4f46e5;'><td style='padding: 12px 0; font-weight: bold;'><strong>Net Salary:</strong></td><td style='text-align: right; color: #10b981; font-weight: bold;'>₹%,.2f</td></tr>" +
                "</table>" +
                "</div>" +
                "<p>The amount has been credited to your account.</p>" +
                "<p style='color: #6b7280; font-size: 14px;'>This is an automated notification. Please contact HR for any queries.</p>" +
                "</div>" +
                "</body></html>",
                employee.getFirstName(),
                getMonthName(payroll.getMonth()),
                payroll.getYear(),
                basicSalary,
                allowances,
                deductions,
                netSalary
            );

            emailUtil.sendHtmlEmail(toEmail, subject, htmlBody);
        } catch (Exception e) {
            System.err.println("Error sending payroll notification: " + e.getMessage());
        }
    }

    /**
     * Send leave approval/rejection notification
     */
    public void sendLeaveStatusNotification(Leave leave, boolean approved) {
        try {
            Employee employee = leave.getEmployee();
            if (employee == null || employee.getEmail() == null) {
                return;
            }

            String toEmail = employee.getEmail();
            String status = approved ? "Approved" : "Rejected";
            String statusEmoji = approved ? "✅" : "❌";
            String subject = "Leave Application " + status + " - " + leave.getLeaveType();
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            
            String htmlBody = String.format(
                "<html><body>" +
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: %s;'>%s Leave Application %s</h2>" +
                "<p>Dear %s,</p>" +
                "<p>Your leave application has been <strong>%s</strong>.</p>" +
                "<div style='background: #f3f4f6; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                "<h3>Leave Details:</h3>" +
                "<p><strong>Leave Type:</strong> %s</p>" +
                "<p><strong>From Date:</strong> %s</p>" +
                "<p><strong>To Date:</strong> %s</p>" +
                "<p><strong>Total Days:</strong> %d</p>" +
                "<p><strong>Reason:</strong> %s</p>" +
                "</div>" +
                "%s" +
                "<p style='color: #6b7280; font-size: 14px;'>This is an automated notification. Please contact HR for any queries.</p>" +
                "</div>" +
                "</body></html>",
                approved ? "#10b981" : "#ef4444",
                statusEmoji,
                status,
                employee.getFirstName(),
                status,
                leave.getLeaveType(),
                leave.getStartDate().format(formatter),
                leave.getEndDate().format(formatter),
                leave.getTotalDays(),
                leave.getReason() != null ? leave.getReason() : "N/A",
                approved ? "<p style='color: #10b981;'>Enjoy your time off!</p>" : ""
            );

            emailUtil.sendHtmlEmail(toEmail, subject, htmlBody);
        } catch (Exception e) {
            System.err.println("Error sending leave notification: " + e.getMessage());
        }
    }

    /**
     * Send new leave application notification to HR/Manager
     */
    public void sendNewLeaveApplicationNotification(Leave leave, String hrEmail) {
        try {
            if (hrEmail == null || hrEmail.isEmpty()) {
                return;
            }

            Employee employee = leave.getEmployee();
            String employeeName = employee != null ? 
                employee.getFirstName() + " " + employee.getLastName() : "Unknown";

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            
            String subject = "New Leave Application - " + employeeName;
            
            String htmlBody = String.format(
                "<html><body>" +
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #f59e0b;'>📝 New Leave Application</h2>" +
                "<p>A new leave application has been submitted.</p>" +
                "<div style='background: #f3f4f6; padding: 20px; border-radius: 8px; margin: 20px 0;'>" +
                "<h3>Employee Details:</h3>" +
                "<p><strong>Name:</strong> %s</p>" +
                "<p><strong>Email:</strong> %s</p>" +
                "<h3>Leave Details:</h3>" +
                "<p><strong>Leave Type:</strong> %s</p>" +
                "<p><strong>From Date:</strong> %s</p>" +
                "<p><strong>To Date:</strong> %s</p>" +
                "<p><strong>Total Days:</strong> %d</p>" +
                "<p><strong>Reason:</strong> %s</p>" +
                "</div>" +
                "<p>Please review and take necessary action.</p>" +
                "</div>" +
                "</body></html>",
                employeeName,
                employee != null ? employee.getEmail() : "N/A",
                leave.getLeaveType(),
                leave.getStartDate().format(formatter),
                leave.getEndDate().format(formatter),
                leave.getTotalDays(),
                leave.getReason() != null ? leave.getReason() : "N/A"
            );

            emailUtil.sendHtmlEmail(hrEmail, subject, htmlBody);
        } catch (Exception e) {
            System.err.println("Error sending new leave notification: " + e.getMessage());
        }
    }

    /**
     * Send attendance regularization notification
     */
    public void sendAttendanceRegularizationNotification(Employee employee, String date, String status) {
        try {
            if (employee.getEmail() == null) {
                return;
            }

            String toEmail = employee.getEmail();
            String subject = "Attendance Marked - " + date;
            
            String htmlBody = String.format(
                "<html><body>" +
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>" +
                "<h2 style='color: #4f46e5;'>✅ Attendance Marked</h2>" +
                "<p>Dear %s,</p>" +
                "<p>Your attendance for <strong>%s</strong> has been marked as <strong>%s</strong>.</p>" +
                "<p style='color: #6b7280; font-size: 14px;'>This is an automated notification.</p>" +
                "</div>" +
                "</body></html>",
                employee.getFirstName(),
                date,
                status
            );

            emailUtil.sendHtmlEmail(toEmail, subject, htmlBody);
        } catch (Exception e) {
            System.err.println("Error sending attendance notification: " + e.getMessage());
        }
    }

    // Helper methods
    private String getMonthName(int month) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        return months[month - 1];
    }

    private String getMonthYear(Integer month, Integer year) {
        return getMonthName(month) + " " + year;
    }
}
