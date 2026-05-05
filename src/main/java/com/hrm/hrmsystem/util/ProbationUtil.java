package com.hrm.hrmsystem.util;

import com.hrm.hrmsystem.model.Employee;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for probation-related calculations.
 * Provides centralized logic to ensure consistency across all services.
 */
public class ProbationUtil {

    /**
     * Check if employee is in probation as of a specific reference date.
     *
     * @param employee The employee to check
     * @param referenceDate The date to check probation status against
     * @return true if employee is in probation as of the reference date
     */
    public static boolean isInProbation(Employee employee, LocalDate referenceDate) {
        if (employee.getJoiningDate() == null || employee.getProbationPeriodMonths() == null) {
            return false; // No probation defined, consider as completed
        }

        LocalDate probationEndDate = employee.getJoiningDate()
                .plusMonths(employee.getProbationPeriodMonths());

        return referenceDate.isBefore(probationEndDate) || referenceDate.isEqual(probationEndDate);
    }

    /**
     * Check if employee is in probation as of today.
     *
     * @param employee The employee to check
     * @return true if employee is currently in probation
     */
    public static boolean isInProbation(Employee employee) {
        return isInProbation(employee, LocalDate.now());
    }

    /**
     * Get probation end date for an employee.
     *
     * @param employee The employee to check
     * @return The probation end date, or null if joining date or probation period is not defined
     */
    public static LocalDate getProbationEndDate(Employee employee) {
        if (employee.getJoiningDate() == null || employee.getProbationPeriodMonths() == null) {
            return null;
        }
        return employee.getJoiningDate().plusMonths(employee.getProbationPeriodMonths());
    }

    /**
     * Get days remaining in probation as of a reference date.
     *
     * @param employee The employee to check
     * @param referenceDate The date to calculate remaining days from
     * @return Days remaining in probation, or 0 if not in probation
     */
    public static long getDaysRemainingInProbation(Employee employee, LocalDate referenceDate) {
        LocalDate probationEndDate = getProbationEndDate(employee);
        if (probationEndDate == null || !isInProbation(employee, referenceDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(referenceDate, probationEndDate);
    }

    /**
     * Get days remaining in probation as of today.
     *
     * @param employee The employee to check
     * @return Days remaining in probation, or 0 if not in probation
     */
    public static long getDaysRemainingInProbation(Employee employee) {
        return getDaysRemainingInProbation(employee, LocalDate.now());
    }

    /**
     * Probation information holder class.
     */
    public static class ProbationInfo {
        public boolean inProbation;
        public String status;
        public LocalDate startDate;
        public LocalDate endDate;
        public long daysRemaining;

        public ProbationInfo(boolean inProbation, String status, LocalDate startDate, LocalDate endDate, long daysRemaining) {
            this.inProbation = inProbation;
            this.status = status;
            this.startDate = startDate;
            this.endDate = endDate;
            this.daysRemaining = daysRemaining;
        }
    }

    /**
     * Get comprehensive probation information as of a reference date.
     *
     * @param employee The employee to check
     * @param referenceDate The date to check probation status against
     * @return ProbationInfo object containing all relevant information
     */
    public static ProbationInfo getProbationInfo(Employee employee, LocalDate referenceDate) {
        if (employee.getJoiningDate() == null || employee.getProbationPeriodMonths() == null) {
            return new ProbationInfo(false, "No probation period defined", null, null, 0);
        }

        LocalDate probationEndDate = employee.getJoiningDate().plusMonths(employee.getProbationPeriodMonths());
        boolean inProbation = isInProbation(employee, referenceDate);

        String status;
        long daysRemaining = 0;

        if (inProbation) {
            daysRemaining = ChronoUnit.DAYS.between(referenceDate, probationEndDate);
            status = "In Progress (" + daysRemaining + " days remaining)";
        } else {
            status = "Completed";
        }

        return new ProbationInfo(inProbation, status, employee.getJoiningDate(), probationEndDate, daysRemaining);
    }

    /**
     * Get comprehensive probation information as of today.
     *
     * @param employee The employee to check
     * @return ProbationInfo object containing all relevant information
     */
    public static ProbationInfo getProbationInfo(Employee employee) {
        return getProbationInfo(employee, LocalDate.now());
    }
}
