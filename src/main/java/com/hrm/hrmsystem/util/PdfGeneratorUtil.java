package com.hrm.hrmsystem.util;

import com.hrm.hrmsystem.entity.Payslip;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class PdfGeneratorUtil {

    private static final String PDF_DIRECTORY = "payslips/";
    private static final String COMPANY_NAME = "ENEGO SERVICE PRIVATE LIMITED";
    private static final String COMPANY_ADDRESS = "Official Registered Address";

    /**
     * Generate PDF for payslip
     */
    public String generatePayslipPdf(Payslip payslip) throws IOException {
        try {
            // Create directory if not exists
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(PDF_DIRECTORY));

            // Generate unique filename
            String fileName = generateFileName(payslip);
            String filePath = PDF_DIRECTORY + fileName;

            // Create PDF document
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            document.setMargins(20, 20, 20, 20);

            // Add header
            addHeader(document, payslip);

            // Add employee information
            addEmployeeInfo(document, payslip);

            // Add salary details table
            addSalaryDetailsTable(document, payslip);

            // Add footer
            addFooter(document);

            document.close();

            return filePath;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Generate unique filename for PDF
     */
    private String generateFileName(Payslip payslip) {
        String empId = payslip.getEmployee().getId().toString();
        String monthYear = payslip.getMonthYear();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("Payslip_%s_%s_%s.pdf", empId, monthYear, uuid);
    }

    /**
     * Add header section to PDF
     */
    private void addHeader(Document document, Payslip payslip) throws IOException {
        // Company header
        Paragraph companyName = new Paragraph(COMPANY_NAME)
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);

        Paragraph companyAddress = new Paragraph(COMPANY_ADDRESS)
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER);

        Paragraph payslipTitle = new Paragraph("SALARY SLIP")
                .setFontSize(16)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);

        Paragraph monthYear = new Paragraph("For Month: " + formatMonthYear(payslip.getMonthYear()))
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);

        document.add(companyName);
        document.add(companyAddress);
        document.add(payslipTitle);
        document.add(monthYear);
    }

    /**
     * Add employee information
     */
    private void addEmployeeInfo(Document document, Payslip payslip) throws IOException {
        PdfFont boldFont = PdfFontFactory.createFont();
        
        Table infoTable = new Table(2);
        infoTable.setWidth(500);

        // Employee details
        addTableCell(infoTable, "Employee ID:", payslip.getEmployee().getId().toString(), boldFont);
        addTableCell(infoTable, "Employee Name:", payslip.getEmployee().getFirstName() + " " + payslip.getEmployee().getLastName(), boldFont);
        addTableCell(infoTable, "Email:", payslip.getEmployee().getEmail(), boldFont);
        addTableCell(infoTable, "Department:", payslip.getEmployee().getDepartment().getName(), boldFont);
        addTableCell(infoTable, "Position:", payslip.getEmployee().getDesignation(), boldFont);
        addTableCell(infoTable, "Date of Joining:", payslip.getEmployee().getJoiningDate().toString(), boldFont);
        addTableCell(infoTable, "Present Days:", payslip.getPresentDays().toString(), boldFont);
        addTableCell(infoTable, "Absent Days:", payslip.getAbsentDays().toString(), boldFont);

        document.add(infoTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Add salary details table
     */
    private void addSalaryDetailsTable(Document document, Payslip payslip) throws IOException {
        // Earnings Section
        Table earningsTable = new Table(2);
        earningsTable.setWidth(500);
        earningsTable.setMarginBottom(10);

        // Header
        Cell headerCell1 = new Cell().add(new Paragraph("EARNINGS").setBold());
        headerCell1.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        Cell headerCell2 = new Cell().add(new Paragraph("AMOUNT").setBold());
        headerCell2.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        earningsTable.addCell(headerCell1);
        earningsTable.addCell(headerCell2);

        // Add earnings rows
        addEarningsRow(earningsTable, "Basic Salary", payslip.getBasicSalary());
        addEarningsRow(earningsTable, "Dearness Allowance (DA)", payslip.getDa());
        addEarningsRow(earningsTable, "House Rent Allowance (HRA)", payslip.getHra());
        addEarningsRow(earningsTable, "Other Allowance", payslip.getOtherAllowance());

        // Gross Salary
        Cell grossCell = new Cell().add(new Paragraph("GROSS SALARY").setBold());
        grossCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        earningsTable.addCell(grossCell);
        earningsTable.addCell(new Cell().add(new Paragraph(formatAmount(payslip.getGrossSalary()))));

        document.add(earningsTable);

        // Deductions Section
        Table deductionsTable = new Table(2);
        deductionsTable.setWidth(500);
        deductionsTable.setMarginTop(10);
        deductionsTable.setMarginBottom(10);

        // Header
        Cell deductHeaderCell1 = new Cell().add(new Paragraph("DEDUCTIONS").setBold());
        deductHeaderCell1.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        Cell deductHeaderCell2 = new Cell().add(new Paragraph("AMOUNT").setBold());
        deductHeaderCell2.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        deductionsTable.addCell(deductHeaderCell1);
        deductionsTable.addCell(deductHeaderCell2);

        // Add deductions rows
        addDeductionRow(deductionsTable, "Provident Fund (PF)", payslip.getPf());
        addDeductionRow(deductionsTable, "Employee State Insurance (ESI)", payslip.getEsi());
        addDeductionRow(deductionsTable, "Income Tax", payslip.getIncomeTax());
        
        // Calculate insurance and general other deductions
        BigDecimal ins = payslip.getInsurance() != null ? payslip.getInsurance() : BigDecimal.ZERO;
        BigDecimal other = payslip.getOtherDeduction() != null ? payslip.getOtherDeduction() : BigDecimal.ZERO;
        BigDecimal generalOther = other.subtract(ins);
        if (generalOther.compareTo(BigDecimal.ZERO) < 0) {
            generalOther = BigDecimal.ZERO;
        }
        
        if (ins.compareTo(BigDecimal.ZERO) > 0) {
            addDeductionRow(deductionsTable, "Insurance", ins);
        }
        if (generalOther.compareTo(BigDecimal.ZERO) > 0) {
            addDeductionRow(deductionsTable, "Other Deductions", generalOther);
        }
        
        // Calculate attendance/leave deductions
        BigDecimal unpaid = payslip.getUnpaidLeaveDeduction() != null ? payslip.getUnpaidLeaveDeduction() : BigDecimal.ZERO;
        BigDecimal totalAttendance = payslip.getAbsentLeaveDeduction() != null ? payslip.getAbsentLeaveDeduction() : BigDecimal.ZERO;
        BigDecimal absentPenalty = totalAttendance.subtract(unpaid);
        if (absentPenalty.compareTo(BigDecimal.ZERO) < 0) {
            absentPenalty = BigDecimal.ZERO;
        }
        
        if (unpaid.compareTo(BigDecimal.ZERO) > 0) {
            addDeductionRow(deductionsTable, "Unpaid Leave Deduction (" + (payslip.getUnpaidLeaveDays() != null ? payslip.getUnpaidLeaveDays() : 0) + " days)", unpaid);
        }
        if (absentPenalty.compareTo(BigDecimal.ZERO) > 0) {
            addDeductionRow(deductionsTable, "Absent Penalty Deduction (" + (payslip.getAbsentDays() != null ? payslip.getAbsentDays() : 0) + " days)", absentPenalty);
        }

        // Total Deduction
        Cell totalDeductCell = new Cell().add(new Paragraph("TOTAL DEDUCTION").setBold());
        totalDeductCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        deductionsTable.addCell(totalDeductCell);
        deductionsTable.addCell(new Cell().add(new Paragraph(formatAmount(payslip.getTotalDeduction()))));

        document.add(deductionsTable);

        // Net Salary Section
        Table netSalaryTable = new Table(2);
        netSalaryTable.setWidth(500);
        netSalaryTable.setMarginTop(20);

        Cell netCell = new Cell().add(new Paragraph("NET SALARY").setBold());
        netCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        netSalaryTable.addCell(netCell);

        Cell netAmountCell = new Cell().add(new Paragraph(formatAmount(payslip.getNetSalary())).setBold());
        netAmountCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        netSalaryTable.addCell(netAmountCell);

        document.add(netSalaryTable);

        // Add approval section
        if (payslip.getApprovedBy() != null) {
            document.add(new Paragraph("\n"));
            Table approvalTable = new Table(2);
            addTableCell(approvalTable, "Approved By:", payslip.getApprovedBy(), null);
            addTableCell(approvalTable, "Approved Date:", payslip.getApprovedDate().toString(), null);
            addTableCell(approvalTable, "Status:", payslip.getStatus().toString(), null);
            addTableCell(approvalTable, "Remarks:", payslip.getRemarks() != null ? payslip.getRemarks() : "N/A", null);
            document.add(approvalTable);
        }
    }

    /**
     * Add footer to PDF
     */
    private void addFooter(Document document) {
        document.add(new Paragraph("\n"));
        Paragraph footer = new Paragraph("This is a system-generated payslip. No signature required.")
                .setFontSize(9)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20);
        document.add(footer);

        Paragraph timestamp = new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(timestamp);
    }

    /**
     * Helper method to add earning row
     */
    private void addEarningsRow(Table table, String label, BigDecimal amount) {
        table.addCell(new Cell().add(new Paragraph(label)));
        table.addCell(new Cell().add(new Paragraph(formatAmount(amount))));
    }

    /**
     * Helper method to add deduction row
     */
    private void addDeductionRow(Table table, String label, BigDecimal amount) {
        table.addCell(new Cell().add(new Paragraph(label)));
        table.addCell(new Cell().add(new Paragraph(formatAmount(amount))));
    }

    /**
     * Helper method to add table cell
     */
    private void addTableCell(Table table, String label, String value, PdfFont font) {
        Cell labelCell = new Cell().add(new Paragraph(label).setBold());
        Cell valueCell = new Cell().add(new Paragraph(value));
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /**
     * Format amount as currency
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return String.format("₹ %,.2f", amount);
    }

    /**
     * Format month year
     */
    private String formatMonthYear(String monthYear) {
        // Convert "2024-01" to "January 2024"
        String[] parts = monthYear.split("-");
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[0]);
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return monthNames[month - 1] + " " + year;
    }
}
