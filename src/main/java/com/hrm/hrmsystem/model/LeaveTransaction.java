package com.hrm.hrmsystem.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Leave Transaction - Tracks each leave application with dates
 */
@Entity
@Table(name = "leave_transactions")
public class LeaveTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_balance_id", nullable = false)
    private LeaveBalanceEnhanced leaveBalance;

    /** Reference to the actual leave application */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_id", unique = true)
    private Leave leave;

    /** Transaction type: EARNED, USED, CANCELLED, CARRIED_FORWARD, LAPSED */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    /** Number of days in this transaction */
    @Column(nullable = false)
    private Double days = 0.0;

    /** Balance after this transaction */
    @Column(nullable = false)
    private Double balanceAfter = 0.0;

    /** Date when transaction occurred */
    @Column(nullable = false)
    private LocalDate transactionDate = LocalDate.now();

    /** Notes/Reason */
    @Column(length = 500)
    private String notes;

    // Constructors
    public LeaveTransaction() {}

    public LeaveTransaction(LeaveBalanceEnhanced balance, TransactionType type, Double days, String notes) {
        this.leaveBalance = balance;
        this.transactionType = type;
        this.days = days;
        this.notes = notes;
        this.transactionDate = LocalDate.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LeaveBalanceEnhanced getLeaveBalance() { return leaveBalance; }
    public void setLeaveBalance(LeaveBalanceEnhanced leaveBalance) { this.leaveBalance = leaveBalance; }
    
    public Leave getLeave() { return leave; }
    public void setLeave(Leave leave) { this.leave = leave; }
    
    public TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(TransactionType transactionType) { this.transactionType = transactionType; }
    
    public Double getDays() { return days; }
    public void setDays(Double days) { this.days = days; }
    
    public Double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Double balanceAfter) { this.balanceAfter = balanceAfter; }
    
    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // Enum for transaction types
    public enum TransactionType {
        EARNED,           // Monthly accrual (1.5 leaves/month)
        USED,             // Leave taken
        CANCELLED,        // Leave cancelled/restored
        CARRIED_FORWARD,  // From previous year
        LAPSED            // Expired leaves
    }
}
