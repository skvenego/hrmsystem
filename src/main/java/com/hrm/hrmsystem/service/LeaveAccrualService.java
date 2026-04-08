package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.*;
import com.hrm.hrmsystem.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class LeaveAccrualService {
    
    private final LeaveBalanceEnhancedRepository leaveBalanceRepo;
    private final LeaveSemesterRepository semesterRepo;
    private final ProbationPeriodRepository probationRepo;
    private final EmployeeRepository employeeRepo;
    private final LeaveTransactionRepository transactionRepo;
    
    public LeaveAccrualService(
        LeaveBalanceEnhancedRepository leaveBalanceRepo,
        LeaveSemesterRepository semesterRepo,
        ProbationPeriodRepository probationRepo,
        EmployeeRepository employeeRepo,
        LeaveTransactionRepository transactionRepo
    ) {
        this.leaveBalanceRepo = leaveBalanceRepo;
        this.semesterRepo = semesterRepo;
        this.probationRepo = probationRepo;
        this.employeeRepo = employeeRepo;
        this.transactionRepo = transactionRepo;
    }
    
    /**
     * Monthly leave accrual - runs on 1st of every month
     * Adds 1.5 leaves per month for confirmed employees
     */
    @Scheduled(cron = "0 0 0 1 * ?") // Run on 1st of every month at midnight
    @Transactional
    public void accrueMonthlyLeaves() {
        List<Employee> allEmployees = employeeRepo.findAll();
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int currentMonth = today.getMonthValue();
        
        for (Employee employee : allEmployees) {
            // Check if employee is confirmed (not in probation)
            Optional<ProbationPeriod> probationOpt = probationRepo.findByEmployeeId(employee.getId());
            
            boolean isConfirmed = probationOpt.map(p -> 
                p.getStatus() == ProbationPeriod.ProbationStatus.CONFIRMED
            ).orElse(true); // If no probation record, assume confirmed
            
            if (!isConfirmed) {
                System.out.println("⏭️ Skipping leave accrual for employee in probation: " + employee.getId());
                continue;
            }
            
            // Determine semester
            LeaveSemester.SemesterType semester = (currentMonth >= 1 && currentMonth <= 6) 
                ? LeaveSemester.SemesterType.S1 
                : LeaveSemester.SemesterType.S2;
            
            // Get or create semester record
            Optional<LeaveSemester> semesterOpt = semesterRepo.findByEmployeeIdAndYearAndSemester(
                employee.getId(), currentYear, semester
            );
            
            LeaveSemester leaveSemester = semesterOpt.orElseGet(() -> {
                LeaveSemester newSemester = new LeaveSemester(
                    employee,
                    currentYear,
                    semester,
                    semester == LeaveSemester.SemesterType.S1 ? LocalDate.of(currentYear, 1, 1) : LocalDate.of(currentYear, 7, 1),
                    semester == LeaveSemester.SemesterType.S1 ? LocalDate.of(currentYear, 6, 30) : LocalDate.of(currentYear, 12, 31)
                );
                return semesterRepo.save(newSemester);
            });
            
            // Add 1.5 leaves
            leaveSemester.setEarnedLeaves(leaveSemester.getEarnedLeaves() + 1.5);
            semesterRepo.save(leaveSemester);
            
            // Update leave balance
            Optional<LeaveBalanceEnhanced> balanceOpt = leaveBalanceRepo.findByEmployeeIdAndYear(employee.getId(), currentYear);
            
            LeaveBalanceEnhanced balance = balanceOpt.orElseGet(() -> {
                LeaveBalanceEnhanced newBalance = new LeaveBalanceEnhanced();
                newBalance.setEmployee(employee);
                newBalance.setYear(currentYear);
                return leaveBalanceRepo.save(newBalance);
            });
            
            balance.addEarnedLeave(1.5);
            leaveBalanceRepo.save(balance);
            
            // Create transaction record
            LeaveTransaction transaction = new LeaveTransaction();
            transaction.setLeaveBalance(balance);
            transaction.setTransactionType(LeaveTransaction.TransactionType.EARNED);
            transaction.setDays(1.5);
            transaction.setBalanceAfter(balance.getRemainingLeaves());
            transaction.setTransactionDate(today);
            transaction.setNotes("Monthly leave accrual - " + today.getMonth().name());
            
            transactionRepo.save(transaction);
            
            System.out.println("✅ Accrued 1.5 leaves for employee: " + employee.getId());
        }
    }
    
    /**
     * Get current leave balance for employee
     */
    public String getLeaveBalanceSummary(Long employeeId) {
        int currentYear = LocalDate.now().getYear();
        
        Optional<LeaveBalanceEnhanced> balanceOpt = leaveBalanceRepo.findByEmployeeIdAndYear(employeeId, currentYear);
        
        if (balanceOpt.isEmpty()) {
            return "No leave balance data available";
        }
        
        LeaveBalanceEnhanced balance = balanceOpt.get();
        
        StringBuilder summary = new StringBuilder();
        summary.append("Year: ").append(currentYear).append("\n");
        summary.append("Total Available: ").append(balance.getTotalAvailable()).append(" days\n");
        summary.append("Used: ").append(balance.getUsedLeaves()).append(" days\n");
        summary.append("Remaining: ").append(balance.getRemainingLeaves()).append(" days");
        
        return summary.toString();
    }
    
    /**
     * Get semester-wise leave balance
     */
    public String getSemesterWiseBalance(Long employeeId) {
        int currentYear = LocalDate.now().getYear();
        List<LeaveSemester> semesters = semesterRepo.findByEmployeeIdAndYear(employeeId, currentYear);
        
        StringBuilder summary = new StringBuilder();
        summary.append("LEAVE BALANCE ").append(currentYear).append("\n\n");
        
        for (LeaveSemester semester : semesters) {
            summary.append("Semester ").append(semester.getSemester().name()).append("\n");
            summary.append("Period: ").append(semester.getStartDate()).append(" to ").append(semester.getEndDate()).append("\n");
            summary.append("Earned: ").append(semester.getEarnedLeaves()).append(" days\n");
            summary.append("Used: ").append(semester.getUsedLeaves()).append(" days\n");
            summary.append("Remaining: ").append(semester.getRemainingLeaves()).append(" days\n");
            summary.append("Status: ").append(semester.getStatusDisplay()).append("\n\n");
        }
        
        return summary.toString();
    }
}
