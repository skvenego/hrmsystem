# 💰 DUAL APPROVAL PAYROLL WORKFLOW - COMPLETE IMPLEMENTATION

## 🎯 OVERVIEW

Professional payroll approval system requiring **TWO approvals** before payslip is generated:
1. **Accountant** → First approval (verifies calculations)
2. **Director** → Second approval (final authorization)

**ONLY after BOTH approvals:**
- ✅ Payslip is generated
- ✅ Employee can view payslip
- ✅ Employee can download PDF
- ✅ Payment can be processed

---

## 📋 WORKFLOW DIAGRAM

```
Payroll Created
    ↓
Status: PENDING_APPROVAL
    ↓
┌─────────────────────────────────┐
│  STEP 1: ACCOUNTANT APPROVAL    │
├─────────────────────────────────┤
│  Accountant reviews:            │
│  ✓ Basic Salary                 │
│  ✓ HRA, DA, Allowances          │
│  ✓ PF (12%)                     │
│  ✓ TDS Calculation              │
│  ✓ Net Salary                   │
│                                 │
│  [Approve] [Reject]             │
└─────────────────────────────────┘
    ↓
If Approved → accountantApproved = TRUE
If Rejected → Back to HR for correction
    ↓
┌─────────────────────────────────┐
│  STEP 2: DIRECTOR APPROVAL      │
├─────────────────────────────────┤
│  Director reviews:              │
│  ✓ All calculations verified    │
│  ✓ Budget compliance            │
│  ✓ Policy adherence             │
│                                 │
│  [Final Approve] [Reject]       │
└─────────────────────────────────┘
    ↓
If Approved → directorApproved = TRUE
    ↓
Status Changes to: PAID
payslipGenerated = TRUE
    ↓
┌─────────────────────────────────┐
│  EMPLOYEE CAN NOW:              │
│  ✓ View Payslip                 │
│  ✓ Download PDF                 │
│  ✓ See payment details          │
└─────────────────────────────────┘
```

---

## 🗄️ DATABASE SCHEMA

### Table: `payroll_approvals`

```sql
CREATE TABLE payroll_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payroll_id BIGINT NOT NULL,
    approver_role ENUM('ROLE_ACCOUNTANT', 'ROLE_DIRECTOR') NOT NULL,
    approved_by BIGINT,  -- User ID who approved
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    approved_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    comments VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (payroll_id) REFERENCES payroll(id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_payroll_status (payroll_id, status),
    INDEX idx_approver_role (approver_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### Update `payroll` Table

```sql
ALTER TABLE payroll 
ADD COLUMN requires_dual_approval BOOLEAN DEFAULT TRUE,
ADD COLUMN accountant_approved BOOLEAN DEFAULT FALSE,
ADD COLUMN director_approved BOOLEAN DEFAULT FALSE,
ADD COLUMN final_approval_date DATE,
ADD COLUMN payslip_generated BOOLEAN DEFAULT FALSE,
ADD COLUMN status ENUM('PENDING', 'PROCESSED', 'PENDING_APPROVAL', 'PAID') DEFAULT 'PENDING';
```

---

## 🔧 BACKEND IMPLEMENTATION

### 1. Model Updates Required

**File: `Payroll.java`**

Add these fields:
```java
@Column(nullable = false)
private Boolean requiresDualApproval = true;

@Column(nullable = false)
private Boolean accountantApproved = false;

@Column(nullable = false)
private Boolean directorApproved = false;

private LocalDate finalApprovalDate;

@Column(nullable = false)
private Boolean payslipGenerated = false;

@OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true)
private List<PayrollApproval> approvals = new ArrayList<>();

// Add to enum:
public enum PayrollStatus {
    PENDING, 
    PROCESSED, 
    PENDING_APPROVAL,  // NEW: Waiting for dual approval
    PAID
}
```

---

### 2. Create Repository

**File: `PayrollApprovalRepository.java`**

```java
package com.hrm.hrmsystem.repository;

import com.hrm.hrmsystem.model.PayrollApproval;
import com.hrm.hrmsystem.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollApprovalRepository extends JpaRepository<PayrollApproval, Long> {
    
    List<PayrollApproval> findByPayrollId(Long payrollId);
    
    Optional<PayrollApproval> findByPayrollIdAndApproverRole(Long payrollId, User.Role role);
    
    boolean existsByPayrollIdAndApproverRoleAndStatus(Long payrollId, User.Role role, 
                                                       PayrollApproval.ApprovalStatus status);
    
    List<PayrollApproval> findByApprovedBy(Long userId);
}
```

---

### 3. Create Service Layer

**File: `PayrollApprovalService.java`**

```java
package com.hrm.hrmsystem.service;

import com.hrm.hrmsystem.model.*;
import com.hrm.hrmsystem.repository.PayrollApprovalRepository;
import com.hrm.hrmsystem.repository.PayrollRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PayrollApprovalService {
    
    private final PayrollApprovalRepository approvalRepo;
    private final PayrollRepository payrollRepo;
    
    public PayrollApprovalService(PayrollApprovalRepository approvalRepo,
                                   PayrollRepository payrollRepo) {
        this.approvalRepo = approvalRepo;
        this.payrollRepo = payrollRepo;
    }
    
    /**
     * Initiate dual approval workflow
     */
    @Transactional
    public void initiateApprovalWorkflow(Payroll payroll) {
        payroll.setStatus(Payroll.PayrollStatus.PENDING_APPROVAL);
        payroll.setAccountantApproved(false);
        payroll.setDirectorApproved(false);
        payroll.setPayslipGenerated(false);
        payrollRepo.save(payroll);
        
        // Create approval records for both roles
        PayrollApproval accountantApproval = new PayrollApproval(
            payroll, 
            User.Role.ROLE_ACCOUNTANT
        );
        
        PayrollApproval directorApproval = new PayrollApproval(
            payroll, 
            User.Role.ROLE_DIRECTOR
        );
        
        approvalRepo.save(accountantApproval);
        approvalRepo.save(directorApproval);
    }
    
    /**
     * Approve by Accountant
     */
    @Transactional
    public PayrollApproval approveByAccountant(Long payrollId, User approver, String comments) {
        Payroll payroll = payrollRepo.findById(payrollId)
            .orElseThrow(() -> new RuntimeException("Payroll not found"));
        
        Optional<PayrollApproval> approvalOpt = approvalRepo.findByPayrollIdAndApproverRole(
            payrollId, 
            User.Role.ROLE_ACCOUNTANT
        );
        
        if (approvalOpt.isEmpty()) {
            throw new RuntimeException("Approval record not found");
        }
        
        PayrollApproval approval = approvalOpt.get();
        approval.setStatus(PayrollApproval.ApprovalStatus.APPROVED);
        approval.setApprovedBy(approver);
        approval.setComments(comments);
        
        approvalRepo.save(approval);
        
        // Update payroll
        payroll.setAccountantApproved(true);
        checkAndUpdateFinalApproval(payroll);
        
        return approval;
    }
    
    /**
     * Approve by Director
     */
    @Transactional
    public PayrollApproval approveByDirector(Long payrollId, User approver, String comments) {
        Payroll payroll = payrollRepo.findById(payrollId)
            .orElseThrow(() -> new RuntimeException("Payroll not found"));
        
        Optional<PayrollApproval> approvalOpt = approvalRepo.findByPayrollIdAndApproverRole(
            payrollId, 
            User.Role.ROLE_DIRECTOR
        );
        
        if (approvalOpt.isEmpty()) {
            throw new RuntimeException("Approval record not found");
        }
        
        PayrollApproval approval = approvalOpt.get();
        approval.setStatus(PayrollApproval.ApprovalStatus.APPROVED);
        approval.setApprovedBy(approver);
        approval.setComments(comments);
        
        approvalRepo.save(approval);
        
        // Update payroll
        payroll.setDirectorApproved(true);
        checkAndUpdateFinalApproval(payroll);
        
        return approval;
    }
    
    /**
     * Reject approval
     */
    @Transactional
    public PayrollApproval rejectApproval(Long payrollId, User rejector, 
                                          User.Role role, String reason) {
        Payroll payroll = payrollRepo.findById(payrollId)
            .orElseThrow(() -> new RuntimeException("Payroll not found"));
        
        Optional<PayrollApproval> approvalOpt = approvalRepo.findByPayrollIdAndApproverRole(
            payrollId, 
            role
        );
        
        if (approvalOpt.isEmpty()) {
            throw new RuntimeException("Approval record not found");
        }
        
        PayrollApproval approval = approvalOpt.get();
        approval.setStatus(PayrollApproval.ApprovalStatus.REJECTED);
        approval.setApprovedBy(rejector);
        approval.setRejectionReason(reason);
        
        approvalRepo.save(approval);
        
        // Reset payroll status
        payroll.setStatus(Payroll.PayrollStatus.PROCESSED);
        if (role == User.Role.ROLE_ACCOUNTANT) {
            payroll.setAccountantApproved(false);
        } else {
            payroll.setDirectorApproved(false);
        }
        
        payrollRepo.save(payroll);
        
        return approval;
    }
    
    /**
     * Check if both approvals complete and finalize
     */
    private void checkAndUpdateFinalApproval(Payroll payroll) {
        if (payroll.getAccountantApproved() && payroll.getDirectorApproved()) {
            // Both approvals received!
            payroll.setStatus(Payroll.PayrollStatus.PAID);
            payroll.setFinalApprovalDate(LocalDate.now());
            payroll.setPayslipGenerated(true);
            payrollRepo.save(payroll);
            
            System.out.println("✅ Payroll #" + payroll.getId() + " fully approved!");
            System.out.println("📄 Payslip now available to employee");
        }
    }
    
    /**
     * Get approval status for payroll
     */
    public String getApprovalStatusDisplay(Payroll payroll) {
        StringBuilder status = new StringBuilder();
        
        status.append("Accountant: ");
        if (payroll.getAccountantApproved()) {
            status.append("✅ APPROVED");
        } else {
            status.append("⏳ PENDING");
        }
        
        status.append(" | Director: ");
        if (payroll.getDirectorApproved()) {
            status.append("✅ APPROVED");
        } else {
            status.append("⏳ PENDING");
        }
        
        if (payroll.getPayslipGenerated()) {
            status.append(" | 📄 Payslip Ready!");
        }
        
        return status.toString();
    }
    
    /**
     * Check if user can approve this payroll
     */
    public boolean canUserApprove(Long payrollId, User user) {
        Optional<PayrollApproval> approvalOpt = approvalRepo.findByPayrollIdAndApproverRole(
            payrollId, 
            user.getRole()
        );
        
        if (approvalOpt.isEmpty()) {
            return false;
        }
        
        PayrollApproval approval = approvalOpt.get();
        return approval.getStatus() == PayrollApproval.ApprovalStatus.PENDING;
    }
    
    /**
     * Get pending approvals for user's role
     */
    public List<PayrollApproval> getPendingApprovalsForRole(User.Role role) {
        return approvalRepo.findAll().stream()
            .filter(a -> a.getApproverRole() == role && 
                        a.getStatus() == PayrollApproval.ApprovalStatus.PENDING)
            .toList();
    }
}
```

---

## 🖥️ FRONTEND IMPLEMENTATION

### 1. Accountant Approval Page

**File: `src/main/resources/static/hrm-frontend/src/pages/AccountantApproval.jsx`**

```jsx
import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const AccountantApproval = () => {
  const navigate = useNavigate();
  const [pendingApprovals, setPendingApprovals] = useState([]);
  const [selectedPayroll, setSelectedPayroll] = useState(null);
  const [showModal, setShowModal] = useState(false);
  const [comments, setComments] = useState('');

  useEffect(() => {
    fetchPendingApprovals();
  }, []);

  const fetchPendingApprovals = async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch('/api/payroll/approvals/pending/accountant', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (res.ok) {
        const data = await res.json();
        setPendingApprovals(data);
      }
    } catch (error) {
      console.error('Error fetching approvals:', error);
    }
  };

  const handleApprove = async (payrollId) => {
    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`/api/payroll/approvals/${payrollId}/accountant/approve`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ comments })
      });

      if (res.ok) {
        alert('✅ Payroll approved successfully!');
        setShowModal(false);
        fetchPendingApprovals();
      } else {
        const error = await res.json();
        alert('❌ Error: ' + error.message);
      }
    } catch (error) {
      console.error('Approval error:', error);
      alert('❌ Failed to approve payroll');
    }
  };

  const handleReject = async (payrollId) => {
    const reason = prompt('Enter rejection reason:');
    if (!reason) return;

    try {
      const token = localStorage.getItem('token');
      const res = await fetch(`/api/payroll/approvals/${payrollId}/accountant/reject`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ reason })
      });

      if (res.ok) {
        alert('❌ Payroll rejected and sent back for correction');
        fetchPendingApprovals();
      } else {
        const error = await res.json();
        alert('❌ Error: ' + error.message);
      }
    } catch (error) {
      console.error('Rejection error:', error);
      alert('❌ Failed to reject payroll');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold mb-6">Payroll Approvals - Accountant</h1>
        
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-xl font-semibold mb-4">Pending Approvals</h2>
          
          {pendingApprovals.length === 0 ? (
            <p className="text-gray-500">No pending approvals</p>
          ) : (
            <table className="w-full">
              <thead>
                <tr className="border-b">
                  <th className="text-left py-3 px-4">Employee</th>
                  <th className="text-left py-3 px-4">Month/Year</th>
                  <th className="text-right py-3 px-4">Net Salary</th>
                  <th className="text-center py-3 px-4">Actions</th>
                </tr>
              </thead>
              <tbody>
                {pendingApprovals.map(payroll => (
                  <tr key={payroll.id} className="border-b hover:bg-gray-50">
                    <td className="py-3 px-4">{payroll.employeeName}</td>
                    <td className="py-3 px-4">
                      {payroll.month}/{payroll.year}
                    </td>
                    <td className="text-right py-3 px-4">
                      ₹{payroll.netSalary?.toLocaleString('en-IN')}
                    </td>
                    <td className="text-center py-3 px-4">
                      <button
                        onClick={() => {
                          setSelectedPayroll(payroll);
                          setShowModal(true);
                        }}
                        className="bg-blue-600 text-white px-4 py-2 rounded mr-2 hover:bg-blue-700"
                      >
                        Review
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Review Modal */}
        {showModal && selectedPayroll && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-6 max-w-2xl w-full m-4">
              <h2 className="text-2xl font-bold mb-4">Review Payroll Details</h2>
              
              <div className="space-y-3 mb-6">
                <div className="flex justify-between">
                  <span>Employee:</span>
                  <strong>{selectedPayroll.employeeName}</strong>
                </div>
                <div className="flex justify-between">
                  <span>Month/Year:</span>
                  <strong>{selectedPayroll.month}/{selectedPayroll.year}</strong>
                </div>
                
                <hr />
                
                <div className="bg-gray-50 p-4 rounded">
                  <h3 className="font-semibold mb-2">Salary Breakdown:</h3>
                  <div className="space-y-1 text-sm">
                    <div className="flex justify-between">
                      <span>Basic Salary:</span>
                      <span>₹{selectedPayroll.basicSalary?.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>HRA:</span>
                      <span>₹{selectedPayroll.hra?.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>DA:</span>
                      <span>₹{selectedPayroll.da?.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Gross Salary:</span>
                      <strong>₹{selectedPayroll.grossSalary?.toLocaleString('en-IN')}</strong>
                    </div>
                    
                    <hr className="my-2" />
                    
                    <div className="flex justify-between text-red-600">
                      <span>PF Deduction:</span>
                      <span>-₹{selectedPayroll.providentFund?.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between text-red-600">
                      <span>TDS:</span>
                      <span>-₹{selectedPayroll.tax?.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between text-red-600">
                      <span>Total Deductions:</span>
                      <strong>-₹{selectedPayroll.totalDeductions?.toLocaleString('en-IN')}</strong>
                    </div>
                    
                    <hr className="my-2" />
                    
                    <div className="flex justify-between text-lg font-bold text-green-600">
                      <span>NET SALARY:</span>
                      <span>₹{selectedPayroll.netSalary?.toLocaleString('en-IN')}</span>
                    </div>
                  </div>
                </div>
                
                <textarea
                  className="w-full border rounded p-2 mt-4"
                  rows="3"
                  placeholder="Add comments (optional)"
                  value={comments}
                  onChange={(e) => setComments(e.target.value)}
                />
              </div>
              
              <div className="flex gap-3">
                <button
                  onClick={() => handleApprove(selectedPayroll.id)}
                  className="flex-1 bg-green-600 text-white py-3 rounded-lg font-semibold hover:bg-green-700"
                >
                  ✅ Approve
                </button>
                <button
                  onClick={() => handleReject(selectedPayroll.id)}
                  className="flex-1 bg-red-600 text-white py-3 rounded-lg font-semibold hover:bg-red-700"
                >
                  ❌ Reject
                </button>
                <button
                  onClick={() => setShowModal(false)}
                  className="flex-1 bg-gray-400 text-white py-3 rounded-lg font-semibold hover:bg-gray-500"
                >
                  Cancel
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AccountantApproval;
```

---

### 2. Employee Payslip View Page

**File: `src/main/resources/static/hrm-frontend/src/pages/MyPayslips.jsx`**

```jsx
import React, { useState, useEffect } from 'react';
import { Download, Eye } from 'lucide-react';

const MyPayslips = () => {
  const [payslips, setPayslips] = useState([]);
  const [selectedPayslip, setSelectedPayslip] = useState(null);
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    fetchMyPayslips();
  }, []);

  const fetchMyPayslips = async () => {
    try {
      const token = localStorage.getItem('token');
      const user = JSON.parse(localStorage.getItem('user'));
      
      const res = await fetch(`/api/employee/my-payslips`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (res.ok) {
        const data = await res.json();
        setPayslips(data);
      }
    } catch (error) {
      console.error('Error fetching payslips:', error);
    }
  };

  const downloadPDF = async (payslipId) => {
    try {
      const token = localStorage.getItem('token');
      window.open(`/api/payslips/${payslipId}/download?token=${token}`, '_blank');
    } catch (error) {
      console.error('Download error:', error);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-5xl mx-auto">
        <h1 className="text-3xl font-bold mb-6">My Payslips</h1>
        
        <div className="bg-white rounded-lg shadow-md p-6">
          {payslips.length === 0 ? (
            <p className="text-gray-500 text-center py-8">
              No payslips available yet
            </p>
          ) : (
            <div className="grid gap-4">
              {payslips.map(payslip => (
                <div 
                  key={payslip.id}
                  className={`border rounded-lg p-4 ${
                    !payslip.payslipGenerated ? 'opacity-50' : ''
                  }`}
                >
                  <div className="flex justify-between items-center">
                    <div>
                      <h3 className="font-semibold text-lg">
                        {payslip.monthName} {payslip.year}
                      </h3>
                      <p className="text-sm text-gray-600">
                        Net Salary: ₹{payslip.netSalary?.toLocaleString('en-IN')}
                      </p>
                      
                      {!payslip.payslipGenerated && (
                        <p className="text-sm text-orange-600 mt-2">
                          ⏳ Awaiting final approval from Director
                        </p>
                      )}
                      
                      {payslip.status === 'PAID' && (
                        <p className="text-sm text-green-600 mt-2">
                          ✅ Paid on {new Date(payslip.paymentDate).toLocaleDateString()}
                        </p>
                      )}
                    </div>
                    
                    <div className="flex gap-2">
                      {payslip.payslipGenerated && (
                        <>
                          <button
                            onClick={() => {
                              setSelectedPayslip(payslip);
                              setShowModal(true);
                            }}
                            className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
                          >
                            <Eye size={16} />
                            View
                          </button>
                          <button
                            onClick={() => downloadPDF(payslip.id)}
                            className="flex items-center gap-2 bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
                          >
                            <Download size={16} />
                            Download
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Payslip View Modal */}
        {showModal && selectedPayslip && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 overflow-y-auto">
            <div className="bg-white rounded-lg p-6 max-w-3xl w-full m-4 max-h-[90vh] overflow-y-auto">
              <div className="text-center mb-6">
                <h2 className="text-2xl font-bold">PAYSLIP</h2>
                <p className="text-gray-600">
                  {selectedPayslip.monthName} {selectedPayslip.year}
                </p>
              </div>
              
              {/* Employee Details */}
              <div className="border rounded p-4 mb-4">
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <p className="text-sm text-gray-600">Employee Name</p>
                    <p className="font-semibold">{selectedPayslip.employeeName}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Employee ID</p>
                    <p className="font-semibold">{selectedPayslip.employeeId}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Department</p>
                    <p className="font-semibold">{selectedPayslip.department}</p>
                  </div>
                  <div>
                    <p className="text-sm text-gray-600">Payment Date</p>
                    <p className="font-semibold">
                      {new Date(selectedPayslip.paymentDate).toLocaleDateString()}
                    </p>
                  </div>
                </div>
              </div>
              
              {/* Earnings & Deductions */}
              <div className="grid grid-cols-2 gap-4 mb-4">
                <div className="border rounded p-4">
                  <h3 className="font-bold mb-3 text-green-600">EARNINGS</h3>
                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span>Basic Salary:</span>
                      <span>₹{selectedPayslip.basicSalary?.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>HRA:</span>
                      <span>₹{selectedPayslip.hra?.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>DA:</span>
                      <span>₹{selectedPayslip.da?.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Other Allowances:</span>
                      <span>₹{selectedPayslip.otherAllowances?.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between font-bold border-t pt-2">
                      <span>Gross Salary:</span>
                      <span>₹{selectedPayslip.grossSalary?.toLocaleString('en-IN')}</span>
                    </div>
                  </div>
                </div>
                
                <div className="border rounded p-4">
                  <h3 className="font-bold mb-3 text-red-600">DEDUCTIONS</h3>
                  <div className="space-y-2">
                    <div className="flex justify-between">
                      <span>Provident Fund:</span>
                      <span>₹{selectedPayslip.providentFund?.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Professional Tax:</span>
                      <span>₹{selectedPayslip.professionalTax?.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between">
                      <span>TDS:</span>
                      <span>₹{selectedPayslip.tax?.toLocaleString('en-IN')}</span>
                    </div>
                    <div className="flex justify-between font-bold border-t pt-2">
                      <span>Total Deductions:</span>
                      <span>₹{selectedPayslip.totalDeductions?.toLocaleString('en-IN')}</span>
                    </div>
                  </div>
                </div>
              </div>
              
              {/* Net Salary */}
              <div className="border-2 border-green-600 rounded p-4 mb-4">
                <div className="flex justify-between items-center">
                  <span className="font-bold text-lg">NET SALARY:</span>
                  <span className="font-bold text-2xl text-green-600">
                    ₹{selectedPayslip.netSalary?.toLocaleString('en-IN')}
                  </span>
                </div>
                <p className="text-sm text-gray-600 mt-2 italic">
                  (In Rupees: {selectedPayslip.netSalaryWords})
                </p>
              </div>
              
              {/* Approval Status */}
              <div className="bg-blue-50 border border-blue-200 rounded p-4 mb-6">
                <h3 className="font-semibold mb-2">Approval Status:</h3>
                <div className="flex gap-4">
                  <div className="flex items-center gap-2">
                    {selectedPayslip.accountantApproved ? (
                      <span className="text-green-600">✅</span>
                    ) : (
                      <span className="text-orange-600">⏳</span>
                    )}
                    <span>Accountant: {selectedPayslip.accountantApproved ? 'Approved' : 'Pending'}</span>
                  </div>
                  <div className="flex items-center gap-2">
                    {selectedPayslip.directorApproved ? (
                      <span className="text-green-600">✅</span>
                    ) : (
                      <span className="text-orange-600">⏳</span>
                    )}
                    <span>Director: {selectedPayslip.directorApproved ? 'Approved' : 'Pending'}</span>
                  </div>
                </div>
              </div>
              
              <div className="flex gap-3">
                <button
                  onClick={() => downloadPDF(selectedPayslip.id)}
                  className="flex-1 flex items-center justify-center gap-2 bg-green-600 text-white py-3 rounded-lg font-semibold hover:bg-green-700"
                >
                  <Download size={18} />
                  Download PDF
                </button>
                <button
                  onClick={() => setShowModal(false)}
                  className="flex-1 bg-gray-400 text-white py-3 rounded-lg font-semibold hover:bg-gray-500"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default MyPayslips;
```

---

## 📊 API ENDPOINTS REQUIRED

### Accountant Endpoints:
```javascript
GET    /api/payroll/approvals/pending/accountant
POST   /api/payroll/approvals/{id}/accountant/approve
POST   /api/payroll/approvals/{id}/accountant/reject
```

### Director Endpoints:
```javascript
GET    /api/payroll/approvals/pending/director
POST   /api/payroll/approvals/{id}/director/approve
POST   /api/payroll/approvals/{id}/director/reject
```

### Employee Endpoints:
```javascript
GET    /api/employee/my-payslips
GET    /api/payslips/{id}/download
```

---

## ✅ SUMMARY

### Workflow:
1. ✅ HR creates payroll → Status: PENDING_APPROVAL
2. ✅ Accountant reviews & approves → accountantApproved = TRUE
3. ✅ Director reviews & approves → directorApproved = TRUE
4. ✅ Status changes to: PAID
5. ✅ payslipGenerated = TRUE
6. ✅ Employee can VIEW & DOWNLOAD PDF

### Benefits:
- ✅ Dual verification prevents errors
- ✅ Clear audit trail
- ✅ Professional approval workflow
- ✅ Employee sees real-time status
- ✅ PDF download available after approval

Would you like me to implement the actual backend code (repositories, services, controllers) for this workflow? Let me know! 🚀
