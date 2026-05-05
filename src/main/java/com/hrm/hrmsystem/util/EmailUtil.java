package com.hrm.hrmsystem.util;

import com.hrm.hrmsystem.entity.Payslip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Component
public class EmailUtil {

    private static final Logger log = LoggerFactory.getLogger(EmailUtil.class);

    private final JavaMailSender mailSender;

    public EmailUtil(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send a simple text email
     */
    public void sendSimpleEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            log.info("Simple email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send HTML email
     */
    public void sendHtmlEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
            log.info("HTML email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error sending HTML email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send email with attachment
     */
    public void sendEmailWithAttachment(String toEmail, String subject, String body, byte[] attachment, String filename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.addAttachment(filename, () -> new java.io.ByteArrayInputStream(attachment));
            
            mailSender.send(message);
            log.info("Email with attachment sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Error sending email with attachment to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Send leave application notification to HR
     */
    public void sendLeaveNotification(String employeeName, String leaveType, String fromDate, String toDate, String hrEmail) {
        String subject = "New Leave Application from " + employeeName;
        String htmlBody = String.format(
                "<html><body>" +
                "<h2>Leave Application Notification</h2>" +
                "<p><strong>Employee Name:</strong> %s</p>" +
                "<p><strong>Leave Type:</strong> %s</p>" +
                "<p><strong>From Date:</strong> %s</p>" +
                "<p><strong>To Date:</strong> %s</p>" +
                "<p>Please review and approve/reject the leave application.</p>" +
                "</body></html>",
                employeeName, leaveType, fromDate, toDate
        );
        
        sendHtmlEmail(hrEmail, subject, htmlBody);
    }

    /**
     * Send payslip to employee
     */
    public void sendPayslip(String employeeEmail, String employeeName, String month, byte[] payslipPdf) {
        String subject = "Your Payslip for " + month;
        String htmlBody = String.format(
                "<html><body>" +
                "<h2>Your Payslip</h2>" +
                "<p>Dear %s,</p>" +
                "<p>Please find attached your payslip for %s.</p>" +
                "<p>Thank you for your work!</p>" +
                "</body></html>",
                employeeName, month
        );
        
        sendEmailWithAttachment(employeeEmail, subject, htmlBody, payslipPdf, "Payslip_" + month + ".pdf");
    }

    /**
     * Send payslip email from Payslip entity (with file attachment)
     */
    public void sendPayslipEmail(Payslip payslip) {
        try {
            String employeeEmail = payslip.getEmployee().getEmail();
            String employeeName = payslip.getEmployee().getFirstName() + " " + payslip.getEmployee().getLastName();
            String subject = "Your Salary Slip - " + formatMonthYear(payslip.getMonthYear());
            
            String htmlBody = String.format(
                    "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<h2 style='color: #333;'>Salary Slip</h2>" +
                    "<p>Dear <strong>%s</strong>,</p>" +
                    "<p>Please find attached your salary slip for <strong>%s</strong>.</p>" +
                    "<br>" +
                    "<table style='border-collapse: collapse; width: 100%%;'>" +
                    "<tr style='background-color: #f0f0f0;'>" +
                    "<td style='border: 1px solid #ddd; padding: 8px;'><strong>Gross Salary</strong></td>" +
                    "<td style='border: 1px solid #ddd; padding: 8px; text-align: right;'>₹ %,.2f</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td style='border: 1px solid #ddd; padding: 8px;'><strong>Deductions</strong></td>" +
                    "<td style='border: 1px solid #ddd; padding: 8px; text-align: right;'>₹ %,.2f</td>" +
                    "</tr>" +
                    "<tr style='background-color: #e8f4f8;'>" +
                    "<td style='border: 1px solid #ddd; padding: 8px;'><strong>Net Salary</strong></td>" +
                    "<td style='border: 1px solid #ddd; padding: 8px; text-align: right;'><strong>₹ %,.2f</strong></td>" +
                    "</tr>" +
                    "</table>" +
                    "<br>" +
                    "<p style='color: #666;'><em>This is an automated email. Please do not reply.</em></p>" +
                    "</body></html>",
                    employeeName,
                    formatMonthYear(payslip.getMonthYear()),
                    payslip.getGrossSalary(),
                    payslip.getTotalDeduction(),
                    payslip.getNetSalary()
            );

            // Send email with PDF attachment
            if (payslip.getPdfFilePath() != null) {
                File pdfFile = new File(payslip.getPdfFilePath());
                if (pdfFile.exists()) {
                    try {
                        byte[] fileContent = Files.readAllBytes(pdfFile.toPath());
                        sendEmailWithAttachment(employeeEmail, subject, htmlBody, fileContent, pdfFile.getName());
                    } catch (IOException e) {
                        log.warn("Could not read PDF file for payslip {}: {}", payslip.getId(), e.getMessage());
                        sendHtmlEmail(employeeEmail, subject, htmlBody);
                    }
                } else {
                    log.warn("PDF file not found for payslip {}", payslip.getId());
                    sendHtmlEmail(employeeEmail, subject, htmlBody);
                }
            } else {
                sendHtmlEmail(employeeEmail, subject, htmlBody);
            }
            
            log.info("Payslip email sent to: {} for month: {}", employeeEmail, payslip.getMonthYear());
        } catch (Exception e) {
            log.error("Error sending payslip email: {}", e.getMessage());
        }
    }

    /**
     * Format month year from YYYY-MM to Month Year
     */
    private String formatMonthYear(String monthYear) {
        String[] parts = monthYear.split("-");
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[0]);
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return monthNames[month - 1] + " " + year;
    }

    /**
     * Send approval/rejection notification
     */
    public void sendLeaveStatusNotification(String employeeEmail, String employeeName, String status, String reason) {
        String subject = "Leave Application " + status.toUpperCase();
        String htmlBody = String.format(
                "<html><body>" +
                "<h2>Leave Application Status Update</h2>" +
                "<p>Dear %s,</p>" +
                "<p>Your leave application has been <strong>%s</strong>.</p>" +
                "<p><strong>Reason:</strong> %s</p>" +
                "</body></html>",
                employeeName, status, reason
        );
        
        sendHtmlEmail(employeeEmail, subject, htmlBody);
    }

    /**
     * Send leave cancellation notification to employee
     */
    public void sendLeaveCancellationNotification(String employeeEmail, String employeeName, String leaveType, String fromDate, String toDate, String cancellationReason) {
        String subject = "Leave Application Cancelled";
        String reasonText = cancellationReason != null && !cancellationReason.trim().isEmpty() 
                ? "<p><strong>Cancellation Reason:</strong> " + cancellationReason + "</p>"
                : "";
        String htmlBody = String.format(
                "<html><body style='font-family: Arial, sans-serif;'>" +
                "<h2 style='color: #dc2626;'>Leave Application Cancelled</h2>" +
                "<p>Dear <strong>%s</strong>,</p>" +
                "<p>Your leave application has been <strong>cancelled</strong>.</p>" +
                "<p><strong>Leave Type:</strong> %s</p>" +
                "<p><strong>From Date:</strong> %s</p>" +
                "<p><strong>To Date:</strong> %s</p>" +
                "%s" +
                "<p style='color: #666;'><em>If you have any questions, please contact HR.</em></p>" +
                "</body></html>",
                employeeName, leaveType, fromDate, toDate, reasonText
        );
        
        sendHtmlEmail(employeeEmail, subject, htmlBody);
    }

    /**
     * Send attendance reminder
     */
    public void sendAttendanceReminder(String employeeEmail, String employeeName) {
        String subject = "Attendance Reminder";
        String htmlBody = String.format(
                "<html><body>" +
                "<h2>Attendance Reminder</h2>" +
                "<p>Dear %s,</p>" +
                "<p>Please remember to check out for today.</p>" +
                "</body></html>",
                employeeName
        );
        
        sendHtmlEmail(employeeEmail, subject, htmlBody);
    }
}
