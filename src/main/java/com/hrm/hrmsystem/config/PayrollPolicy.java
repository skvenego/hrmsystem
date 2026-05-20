package com.hrm.hrmsystem.config;

/**
 * ENTERPRISE PAYROLL POLICY CONFIGURATION
 * Centralized business rules - no hardcoded values in engine
 */
public class PayrollPolicy {
    
    // LEAVE POLICY
    public static final int WORKING_DAYS_PER_MONTH = 26;
    public static final double LEAVE_ACCRUAL_RATE = 1.5; // ✅ Updated to 1.5 per month as per user requirement
    public static final double ABSENT_PENALTY_MULTIPLIER = 1.0; // ✅ Updated to 1.0 as per user requirement (no double deduction)
    
    // PROBATION POLICY
    public static final int DEFAULT_PROBATION_MONTHS = 3;
    
    // PAYROLL LOCKING
    // ✅ PAYROLL LOCKING
    public static final String PAYROLL_STATUS_FINALIZED = "FINALIZED";
    public static final String PAYROLL_STATUS_DRAFT = "DRAFT";
    
    // ✅ AUDIT FIELDS
    public static final String CALCULATION_ENGINE_VERSION = "v2.0";
    
    /**
     * Get leave accrual rate (can be overridden per company policy)
     */
    public static double getLeaveAccrualRate() {
        return LEAVE_ACCRUAL_RATE;
    }
    
    /**
     * Get working days per month (Can be overridden per company policy)
     */
    public static int getWorkingDaysPerMonth() {
        return WORKING_DAYS_PER_MONTH;
    }
    
    /**
     * Get absent penalty multiplier (Can be overridden per company policy)
     */
    public static double getAbsentPenaltyMultiplier() {
        return ABSENT_PENALTY_MULTIPLIER;
    }
    
    /**
     * Get default probation months (Can be overridden per employee)
     */
    public static int getDefaultProbationMonths() {
        return DEFAULT_PROBATION_MONTHS;
    }
    
    /**
     * Check if payroll is finalized (immutable)
     */
    public static boolean isPayrollFinalized(String status) {
        return PAYROLL_STATUS_FINALIZED.equals(status);
    }
}
