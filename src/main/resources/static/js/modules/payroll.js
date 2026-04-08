// HRM System - Payroll Module
// Corresponds to: PayrollController.java, PayrollService.java, Payroll.java, PayrollApprovalController.java, PayrollApprovalService.java

async function loadPayrollData() {
    try {
        const response = await apiCall('/api/payroll');
        
        if (response.ok) {
            const data = await response.json();
            renderPayrollTable(data);
        } else {
            console.log('Using mock payroll data');
        }
    } catch (error) {
        console.error('Error loading payroll:', error);
    }
}

async function loadMyPayroll() {
    try {
        const response = await apiCall('/api/payroll/my');
        
        if (response.ok) {
            const data = await response.json();
            renderMyPayrollTable(data);
        }
    } catch (error) {
        console.error('Error loading my payroll:', error);
    }
}

function renderPayrollTable(payrollList) {
    const tbody = document.getElementById('payrollList');
    if (!tbody) return;
    
    tbody.innerHTML = payrollList.map(pay => {
        const monthYear = `${pay.month || ''}/${pay.year || ''}`;
        const approvalStatus = getApprovalStatus(pay);
        
        return `
        <tr>
            <td>${pay.employeeName || 'N/A'}</td>
            <td>${monthYear}</td>
            <td>${formatCurrency(pay.grossSalary)}</td>
            <td>${formatCurrency(pay.totalDeductions)}</td>
            <td>${formatCurrency(pay.netSalary)}</td>
            <td><span class="badge ${getPayrollStatusClass(pay.status)}">${pay.status || 'PENDING'}</span></td>
            <td>${approvalStatus}</td>
            <td>
                <button onclick="viewPayroll(${pay.id})">👁️ View</button>
                ${pay.status === 'PENDING' ? `<button onclick="submitForApproval(${pay.id})">📤 Submit</button>` : ''}
            </td>
        </tr>
    `}).join('');
}

function getApprovalStatus(pay) {
    if (!pay.requiresDualApproval) return 'N/A';
    
    if (pay.accountantApproved && pay.directorApproved) {
        return '✅ Approved';
    } else if (pay.accountantApproved) {
        return '⏳ Director Pending';
    } else if (pay.directorApproved) {
        return '⏳ Accountant Pending';
    } else {
        return '⏳ Pending Both';
    }
}

function renderMyPayrollTable(payrollList) {
    const tbody = document.getElementById('myPayrollList');
    if (!tbody) return;
    
    tbody.innerHTML = payrollList.map(pay => `
        <tr>
            <td>${formatDate(pay.payPeriodStart)} - ${formatDate(pay.payPeriodEnd)}</td>
            <td>${formatCurrency(pay.basicSalary)}</td>
            <td>${formatCurrency(pay.deductions)}</td>
            <td>${formatCurrency(pay.netSalary)}</td>
            <td><span class="badge ${getPayrollStatusClass(pay.status)}">${pay.status || 'DRAFT'}</span></td>
            <td>
                <button onclick="viewPayroll(${pay.id})">View</button>
            </td>
        </tr>
    `).join('');
}

function getPayrollStatusClass(status) {
    switch (status?.toUpperCase()) {
        case 'PAID': return 'success';
        case 'PROCESSED': return 'info';
        case 'PENDING_APPROVAL': return 'warning';
        case 'PENDING': return 'secondary';
        default: return 'secondary';
    }
}

async function calculatePayroll(employeeId, month, year) {
    try {
        const response = await apiCall(`/api/payroll/calculate?employeeId=${employeeId}&month=${month}&year=${year}`);
        
        if (response.ok) {
            const payroll = await response.json();
            populatePayrollForm(payroll);
        }
    } catch (error) {
        console.error('Calculate payroll error:', error);
    }
}

function populatePayrollForm(payroll) {
    document.getElementById('payrollBasicSalary').value = payroll.basicSalary || '';
    document.getElementById('payrollHra').value = payroll.hra || '';
    document.getElementById('payrollDa').value = payroll.da || '';
    document.getElementById('payrollConveyance').value = payroll.conveyance || '';
    document.getElementById('payrollMedical').value = payroll.medical || '';
    document.getElementById('payrollSpecial').value = payroll.specialAllowance || '';
    document.getElementById('payrollPf').value = payroll.pf || '';
    document.getElementById('payrollTax').value = payroll.tax || '';
    document.getElementById('payrollOtherDeductions').value = payroll.otherDeductions || '';
    document.getElementById('payrollNetSalary').value = payroll.netSalary || '';
}

async function generatePayroll(event) {
    event.preventDefault();
    
    const submitBtn = event.target.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = 'Generating...';
    
    const employeeId = document.getElementById('payrollEmployee')?.value;
    const monthYear = document.getElementById('payrollMonthYear')?.value;
    
    if (!employeeId || !monthYear) {
        alert('Please select employee and month/year');
        submitBtn.disabled = false;
        submitBtn.textContent = '🔄 Generate Payroll';
        return;
    }
    
    const [year, month] = monthYear.split('-');
    
    const data = {
        employeeId: employeeId,
        month: parseInt(month),
        year: parseInt(year),
        basicSalary: Number(document.getElementById('payrollBasicSalary')?.value) || 0,
        hra: Number(document.getElementById('payrollHra')?.value) || 0,
        da: Number(document.getElementById('payrollDa')?.value) || 0,
        ta: Number(document.getElementById('payrollTa')?.value) || 0,
        otherAllowances: Number(document.getElementById('payrollOtherAllowances')?.value) || 0,
        providentFund: Number(document.getElementById('payrollPf')?.value) || 0,
        tax: Number(document.getElementById('payrollTax')?.value) || 0,
        insurance: Number(document.getElementById('payrollInsurance')?.value) || 0,
        otherDeductions: Number(document.getElementById('payrollOtherDeductions')?.value) || 0
    };
    
    // Calculate gross and net
    data.grossSalary = data.basicSalary + data.hra + data.da + data.ta + data.otherAllowances;
    data.totalDeductions = data.providentFund + data.tax + data.insurance + data.otherDeductions;
    data.netSalary = data.grossSalary - data.totalDeductions;
    
    try {
        const response = await apiCall('/api/payroll', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            showSuccess('Payroll generated successfully');
            loadPayrollData();
            hideGeneratePayrollForm();
            event.target.reset();
        } else {
            const error = await response.text();
            alert('Failed to generate payroll: ' + error);
        }
    } catch (error) {
        console.error('Generate payroll error:', error);
        alert('Failed to generate payroll');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = '🔄 Generate Payroll';
    }
}

function showGeneratePayrollForm() {
    const card = document.getElementById('generatePayrollCard');
    if (card) {
        card.style.display = 'block';
        card.scrollIntoView({ behavior: 'smooth' });
    }
}

function hideGeneratePayrollForm() {
    const card = document.getElementById('generatePayrollCard');
    if (card) card.style.display = 'none';
}

async function submitForApproval(id) {
    try {
        const response = await apiCall(`/api/payroll/${id}/submit`, { method: 'POST' });
        
        if (response.ok) {
            showSuccess('Payroll submitted for approval');
            loadPayrollData();
        } else {
            alert('Failed to submit payroll');
        }
    } catch (error) {
        console.error('Submit for approval error:', error);
    }
}

async function approvePayroll(id) {
    try {
        const response = await apiCall(`/api/payroll-approvals/${id}/approve`, { method: 'POST' });
        
        if (response.ok) {
            showSuccess('Payroll approved');
            loadPendingApprovals();
        }
    } catch (error) {
        console.error('Approve payroll error:', error);
    }
}

async function rejectPayroll(id) {
    const reason = prompt('Enter rejection reason:');
    if (reason === null) return;
    
    try {
        const response = await apiCall(`/api/payroll-approvals/${id}/reject`, {
            method: 'POST',
            body: JSON.stringify({ reason })
        });
        
        if (response.ok) {
            showSuccess('Payroll rejected');
            loadPendingApprovals();
        }
    } catch (error) {
        console.error('Reject payroll error:', error);
    }
}

async function loadPendingApprovals() {
    try {
        const response = await apiCall('/api/payroll-approvals/pending');
        
        if (response.ok) {
            const data = await response.json();
            renderPendingApprovalsTable(data);
        }
    } catch (error) {
        console.error('Error loading pending approvals:', error);
    }
}

function renderPendingApprovalsTable(approvals) {
    const tbody = document.getElementById('pendingApprovalsList');
    if (!tbody) return;
    
    tbody.innerHTML = approvals.map(app => `
        <tr>
            <td>${app.employeeName || 'N/A'}</td>
            <td>${formatDate(app.payPeriodStart)} - ${formatDate(app.payPeriodEnd)}</td>
            <td>${formatCurrency(app.netSalary)}</td>
            <td>${app.submittedBy || 'N/A'}</td>
            <td>${formatDate(app.submittedAt)}</td>
            <td>
                <button onclick="approvePayroll(${app.id})">Approve</button>
                <button onclick="rejectPayroll(${app.id})">Reject</button>
            </td>
        </tr>
    `).join('');
}

async function viewPayroll(id) {
    try {
        const response = await apiCall(`/api/payroll/${id}`);
        
        if (response.ok) {
            const payroll = await response.json();
            showPayrollDetailsModal(payroll);
        }
    } catch (error) {
        console.error('View payroll error:', error);
    }
}

function showPayrollDetailsModal(payroll) {
    // Implementation for showing payroll details in a modal
    console.log('Payroll details:', payroll);
}

function openPayrollModal() {
    document.getElementById('payrollModal').style.display = 'block';
}

function closePayrollModal() {
    document.getElementById('payrollModal').style.display = 'none';
    document.getElementById('payrollForm')?.reset();
}

function searchPayroll() {
    const searchTerm = document.getElementById('payrollSearch')?.value.toLowerCase() || '';
    const monthFilter = document.getElementById('payrollMonthFilter')?.value;
    const statusFilter = document.getElementById('payrollStatusFilter')?.value;
    
    const rows = document.querySelectorAll('#payrollList tr');
    
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        const matchesSearch = !searchTerm || text.includes(searchTerm);
        const matchesMonth = !monthFilter || text.includes(monthFilter);
        const matchesStatus = !statusFilter || text.includes(statusFilter.toLowerCase());
        
        row.style.display = matchesSearch && matchesMonth && matchesStatus ? '' : 'none';
    });
}

// Exports
window.loadPayrollData = loadPayrollData;
window.loadMyPayroll = loadMyPayroll;
window.calculatePayroll = calculatePayroll;
window.generatePayroll = generatePayroll;
window.showGeneratePayrollForm = showGeneratePayrollForm;
window.hideGeneratePayrollForm = hideGeneratePayrollForm;
window.submitForApproval = submitForApproval;
window.approvePayroll = approvePayroll;
window.rejectPayroll = rejectPayroll;
window.loadPendingApprovals = loadPendingApprovals;
window.viewPayroll = viewPayroll;
window.openPayrollModal = openPayrollModal;
window.closePayrollModal = closePayrollModal;
window.searchPayroll = searchPayroll;
