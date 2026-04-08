// HRM System - Leave Module
// Corresponds to: LeaveController.java, LeaveService.java, Leave.java, LeaveBalance.java

async function loadLeaveRequests() {
    try {
        const response = await apiCall('/api/leaves/my');
        
        if (response.ok) {
            const leaves = await response.json();
            updateLeaveHistoryTable(leaves);
        } else {
            console.log('Using mock leave data');
        }
    } catch (error) {
        console.error('Error loading leave requests:', error);
    }
}

async function loadAllLeaveRequests() {
    try {
        const response = await apiCall('/api/leaves/all');
        
        if (response.ok) {
            const leaves = await response.json();
            renderLeaveManagementTable(leaves);
        }
    } catch (error) {
        console.error('Error loading all leave requests:', error);
    }
}

async function loadLeaveBalance() {
    try {
        const response = await apiCall('/api/leaves/balance');
        
        if (response.ok) {
            const balance = await response.json();
            renderLeaveBalance(balance);
        }
    } catch (error) {
        console.error('Error loading leave balance:', error);
    }
}

function updateLeaveHistoryTable(leaves) {
    const tbody = document.getElementById('leaveHistory');
    if (!tbody) return;
    
    tbody.innerHTML = leaves.map(leave => `
        <tr>
            <td>${formatDate(leave.appliedDate)}</td>
            <td>${leave.leaveType || 'N/A'}</td>
            <td>${formatDate(leave.startDate)}</td>
            <td>${formatDate(leave.endDate)}</td>
            <td>${leave.totalDays || 0}</td>
            <td>${leave.reason || 'N/A'}</td>
            <td><span class="badge ${getStatusClass(leave.status)}">${leave.status}</span></td>
            <td>${leave.approvedBy || '-'}</td>
        </tr>
    `).join('');
}

function renderLeaveManagementTable(leaves) {
    const tbody = document.getElementById('leaveManagementList');
    if (!tbody) return;
    
    tbody.innerHTML = leaves.map(leave => `
        <tr>
            <td>${leave.employeeName || 'N/A'}</td>
            <td>${leave.leaveType || 'N/A'}</td>
            <td>${leave.reason || 'N/A'}</td>
            <td>${formatDate(leave.startDate)}</td>
            <td>${formatDate(leave.endDate)}</td>
            <td><span class="badge ${getStatusClass(leave.status)}">${leave.status}</span></td>
            <td>
                ${leave.status === 'PENDING' ? `
                    <button onclick="approveLeave(${leave.id})">Approve</button>
                    <button onclick="rejectLeave(${leave.id})">Reject</button>
                ` : '-'}
            </td>
        </tr>
    `).join('');
}

function renderLeaveBalance(balance) {
    const container = document.getElementById('leaveBalanceCards');
    if (!container) return;
    
    container.innerHTML = `
        <div class="stat-card">
            <h4>Casual Leave</h4>
            <p>${balance.casualLeave || 0} days</p>
        </div>
        <div class="stat-card">
            <h4>Sick Leave</h4>
            <p>${balance.sickLeave || 0} days</p>
        </div>
        <div class="stat-card">
            <h4>Paid Leave</h4>
            <p>${balance.paidLeave || 0} days</p>
        </div>
        <div class="stat-card">
            <h4>Earned Leave</h4>
            <p>${balance.earnedLeave || 0} days</p>
        </div>
    `;
}

async function handleLeaveSubmit(event) {
    event.preventDefault();
    
    const submitBtn = event.target.querySelector('button[type="submit"]');
    const leaveType = document.getElementById('leaveType')?.value;
    const reason = document.getElementById('reason')?.value;
    const startDate = document.getElementById('startDate')?.value;
    const endDate = document.getElementById('endDate')?.value;
    const totalDays = document.getElementById('totalDays')?.value;
    
    if (!leaveType || !reason || !startDate || !endDate) {
        alert('Please fill all required fields');
        return;
    }
    
    submitBtn.disabled = true;
    submitBtn.textContent = 'Submitting...';
    
    try {
        const response = await apiCall('/api/leaves/apply', {
            method: 'POST',
            body: JSON.stringify({
                employeeId: currentUser?.id,
                leaveType: leaveType,
                reason: reason,
                startDate: startDate,
                endDate: endDate,
                totalDays: totalDays ? parseInt(totalDays) : calculateDays(startDate, endDate)
            })
        });
        
        if (response.ok) {
            const newLeave = await response.json();
            showSuccess('Leave request submitted successfully!');
            event.target.reset();
            setTimeout(() => loadLeaveRequests(), 1000);
        } else {
            const error = await response.json();
            alert(error.message || 'Failed to submit leave request');
        }
    } catch (error) {
        console.error('Submit error:', error);
        alert('Failed to submit leave request');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = '📤 Submit Leave Request';
    }
}

function calculateLeaveDays() {
    const startDate = document.getElementById('startDate')?.value;
    const endDate = document.getElementById('endDate')?.value;
    const totalDaysInput = document.getElementById('totalDays');
    
    if (startDate && endDate && totalDaysInput) {
        const days = calculateDays(startDate, endDate);
        totalDaysInput.value = days;
    }
}

function calculateDays(start, end) {
    if (!start || !end) return 0;
    const startDate = new Date(start);
    const endDate = new Date(end);
    const diffTime = Math.abs(endDate - startDate);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
    return diffDays;
}

async function approveLeave(id) {
    try {
        const response = await apiCall(`/api/leaves/${id}/approve`, { method: 'POST' });
        
        if (response.ok) {
            showSuccess('Leave request approved');
            loadAllLeaveRequests();
        } else {
            alert('Failed to approve leave request');
        }
    } catch (error) {
        console.error('Approve leave error:', error);
    }
}

async function rejectLeave(id) {
    const reason = prompt('Enter rejection reason:');
    if (reason === null) return;
    
    try {
        const response = await apiCall(`/api/leaves/${id}/reject?rejectionReason=${encodeURIComponent(reason)}`, {
            method: 'POST'
        });
        
        if (response.ok) {
            showSuccess('Leave request rejected');
            loadAllLeaveRequests();
        } else {
            alert('Failed to reject leave request');
        }
    } catch (error) {
        console.error('Reject leave error:', error);
    }
}

function cancelLeave(id) {
    if (!confirm('Are you sure you want to cancel this leave request?')) return;
    
    apiCall(`/api/leaves/${id}/cancel`, { method: 'POST' })
        .then(response => {
            if (response.ok) {
                showSuccess('Leave request cancelled');
                loadLeaveRequests();
            }
        })
        .catch(error => console.error('Cancel leave error:', error));
}

function searchLeaves() {
    const searchTerm = document.getElementById('leaveSearch')?.value.toLowerCase() || '';
    const typeFilter = document.getElementById('leaveTypeFilter')?.value || '';
    const statusFilter = document.getElementById('leaveStatusFilter')?.value || '';
    
    const rows = document.querySelectorAll('#leaveManagementList tr, #leaveHistory tr');
    
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        const matchesSearch = !searchTerm || text.includes(searchTerm);
        const matchesType = !typeFilter || text.includes(typeFilter.toLowerCase());
        const matchesStatus = !statusFilter || text.includes(statusFilter.toLowerCase());
        
        row.style.display = matchesSearch && matchesType && matchesStatus ? '' : 'none';
    });
}

// Exports
window.loadLeaveRequests = loadLeaveRequests;
window.loadAllLeaveRequests = loadAllLeaveRequests;
window.loadLeaveBalance = loadLeaveBalance;
window.handleLeaveSubmit = handleLeaveSubmit;
window.calculateLeaveDays = calculateLeaveDays;
window.approveLeave = approveLeave;
window.rejectLeave = rejectLeave;
window.cancelLeave = cancelLeave;
window.searchLeaves = searchLeaves;
