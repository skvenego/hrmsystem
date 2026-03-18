package com.hrm.hrmsystem.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SalaryCalculator {

    // PF calculation: 12% of basic salary (employee contribution)
    public BigDecimal calculatePF(BigDecimal basicSalary) {
        if (basicSalary == null || basicSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return basicSalary.multiply(new BigDecimal("0.12")).setScale(2, RoundingMode.HALF_UP);
    }

    // ESI calculation: 0.75% of gross salary (for employees earning <= 21,000)
    public BigDecimal calculateESI(BigDecimal grossSalary) {
        if (grossSalary == null || grossSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // ESI applicable only if gross salary <= 21,000
        if (grossSalary.compareTo(new BigDecimal("21000")) <= 0) {
            return grossSalary.multiply(new BigDecimal("0.0075")).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    // Income Tax calculation (simplified - based on old tax regime)
    public BigDecimal calculateIncomeTax(BigDecimal grossSalary) {
        if (grossSalary == null || grossSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Annual gross salary
        BigDecimal annualSalary = grossSalary.multiply(new BigDecimal("12"));
        BigDecimal annualTax = BigDecimal.ZERO;

        // Tax slabs (Old Regime)
        if (annualSalary.compareTo(new BigDecimal("250000")) <= 0) {
            // No tax for income up to 2.5 Lakhs
            annualTax = BigDecimal.ZERO;
        } else if (annualSalary.compareTo(new BigDecimal("500000")) <= 0) {
            // 5% for income between 2.5L - 5L
            annualTax = annualSalary.subtract(new BigDecimal("250000"))
                    .multiply(new BigDecimal("0.05"));
        } else if (annualSalary.compareTo(new BigDecimal("1000000")) <= 0) {
            // 5% for 2.5L - 5L + 20% for 5L - 10L
            BigDecimal firstSlab = new BigDecimal("250000").multiply(new BigDecimal("0.05"));
            BigDecimal secondSlab = annualSalary.subtract(new BigDecimal("500000"))
                    .multiply(new BigDecimal("0.20"));
            annualTax = firstSlab.add(secondSlab);
        } else {
            // 5% for 2.5L - 5L + 20% for 5L - 10L + 30% above 10L
            BigDecimal firstSlab = new BigDecimal("250000").multiply(new BigDecimal("0.05"));
            BigDecimal secondSlab = new BigDecimal("500000").multiply(new BigDecimal("0.20"));
            BigDecimal thirdSlab = annualSalary.subtract(new BigDecimal("1000000"))
                    .multiply(new BigDecimal("0.30"));
            annualTax = firstSlab.add(secondSlab).add(thirdSlab);
        }

        // Add 4% cess
        BigDecimal cess = annualTax.multiply(new BigDecimal("0.04"));
        annualTax = annualTax.add(cess);

        // Convert to monthly tax
        return annualTax.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
    }

    // Calculate HRA (House Rent Allowance) - typically 40-50% of basic
    public BigDecimal calculateHRA(BigDecimal basicSalary) {
        if (basicSalary == null || basicSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return basicSalary.multiply(new BigDecimal("0.40")).setScale(2, RoundingMode.HALF_UP);
    }

    // Calculate DA (Dearness Allowance) - typically 17-30% of basic
    public BigDecimal calculateDA(BigDecimal basicSalary) {
        if (basicSalary == null || basicSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return basicSalary.multiply(new BigDecimal("0.17")).setScale(2, RoundingMode.HALF_UP);
    }

    // Calculate gross salary
    public BigDecimal calculateGrossSalary(BigDecimal basicSalary, BigDecimal hra, BigDecimal da, BigDecimal otherAllowances) {
        BigDecimal gross = BigDecimal.ZERO;
        if (basicSalary != null) gross = gross.add(basicSalary);
        if (hra != null) gross = gross.add(hra);
        if (da != null) gross = gross.add(da);
        if (otherAllowances != null) gross = gross.add(otherAllowances);
        return gross.setScale(2, RoundingMode.HALF_UP);
    }

    // Calculate total deductions
    public BigDecimal calculateTotalDeductions(BigDecimal pf, BigDecimal esi, BigDecimal incomeTax, BigDecimal otherDeductions) {
        BigDecimal total = BigDecimal.ZERO;
        if (pf != null) total = total.add(pf);
        if (esi != null) total = total.add(esi);
        if (incomeTax != null) total = total.add(incomeTax);
        if (otherDeductions != null) total = total.add(otherDeductions);
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // Calculate net salary
    public BigDecimal calculateNetSalary(BigDecimal grossSalary, BigDecimal totalDeductions) {
        if (grossSalary == null) grossSalary = BigDecimal.ZERO;
        if (totalDeductions == null) totalDeductions = BigDecimal.ZERO;
        return grossSalary.subtract(totalDeductions).setScale(2, RoundingMode.HALF_UP);
    }

    // Calculate salary per day
    public BigDecimal calculateSalaryPerDay(BigDecimal basicSalary, int workingDays) {
        if (basicSalary == null || basicSalary.compareTo(BigDecimal.ZERO) <= 0 || workingDays <= 0) {
            return BigDecimal.ZERO;
        }
        return basicSalary.divide(new BigDecimal(workingDays), 2, RoundingMode.HALF_UP);
    }

    // Calculate deduction for absent days
    public BigDecimal calculateAbsentDeduction(BigDecimal basicSalary, int absentDays, int workingDays) {
        BigDecimal salaryPerDay = calculateSalaryPerDay(basicSalary, workingDays);
        return salaryPerDay.multiply(new BigDecimal(absentDays)).setScale(2, RoundingMode.HALF_UP);
    }
}
