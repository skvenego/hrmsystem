package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.LeaveTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveTransactionRepository extends JpaRepository<LeaveTransaction, Long> {
    
    /**
     * Find all transactions for a leave balance, ordered by date
     */
    List<LeaveTransaction> findByLeaveBalanceIdOrderByTransactionDateDesc(Long leaveBalanceId);
    
    /**
     * Find transactions by leave ID
     */
    List<LeaveTransaction> findByLeaveId(Long leaveId);
}
