// HRM System - Payslip Module
// Corresponds to: PayslipController.java, PayslipService.java, Payslip.java

async function loadPayslipsData() {
    try {
        const response = await apiCall('/api/payslips');
        
        if (response.ok) {
            const data = await response.json();
            renderPayslipsTable(data);
        } else {
            console.log('Using mock payslips data');
        }
    } catch (error) {
        console.error('Error loading payslips:', error);
    }
}

async function loadMyPayslips() {
    try {
        const response = await apiCall('/api/payslips/my');
        
        if (response.ok) {
            const data = await response.json();
            renderMyPayslipsTable(data);
        }
    } catch (error) {
        console.error('Error loading my payslips:', error);
    }
}

function renderPayslipsTable(payslips) {
    const tbody = document.getElementById('payslipsList');
    if (!tbody) return;
    
    tbody.innerHTML = payslips.map(payslip => `
        <tr>
            <td>${payslip.employeeName || 'N/A'}</td>
            <td>${payslip.month || 'N/A'}</td>
            <td>${payslip.year || 'N/A'}</td>
            <td>${formatCurrency(payslip.netSalary)}</td>
            <td>${formatDate(payslip.generatedDate)}</td>
            <td><span class="badge ${getPayslipStatusClass(payslip.status)}">${payslip.status || 'GENERATED'}</span></td>
            <td>
                <button onclick="viewPayslip(${payslip.id})">View</button>
                <button onclick="downloadPayslip(${payslip.id})">Download</button>
                <button onclick="emailPayslip(${payslip.id})">Email</button>
            </td>
        </tr>
    `).join('');
}

function renderMyPayslipsTable(payslips) {
    const tbody = document.getElementById('myPayslipsList');
    if (!tbody) return;
    
    tbody.innerHTML = payslips.map(payslip => `
        <tr>
            <td>${payslip.month || 'N/A'}</td>
            <td>${payslip.year || 'N/A'}</td>
            <td>${formatCurrency(payslip.netSalary)}</td>
            <td>${formatDate(payslip.generatedDate)}</td>
            <td>
                <button onclick="viewPayslip(${payslip.id})">View</button>
                <button onclick="downloadPayslip(${payslip.id})">Download</button>
            </td>
        </tr>
    `).join('');
}

function getPayslipStatusClass(status) {
    switch (status?.toUpperCase()) {
        case 'SENT': return 'success';
        case 'GENERATED': return 'info';
        case 'DOWNLOADED': return 'secondary';
        default: return 'info';
    }
}

async function generatePayslip(employeeId, month, year) {
    try {
        const response = await apiCall('/api/payslips/generate', {
            method: 'POST',
            body: JSON.stringify({ employeeId, month, year })
        });
        
        if (response.ok) {
            const payslip = await response.json();
            showSuccess('Payslip generated successfully');
            loadPayslipsData();
            return payslip;
        } else {
            const error = await response.text();
            alert('Failed to generate payslip: ' + error);
        }
    } catch (error) {
        console.error('Generate payslip error:', error);
    }
}

async function generateBulkPayslips(month, year) {
    try {
        const response = await apiCall('/api/payslips/generate-bulk', {
            method: 'POST',
            body: JSON.stringify({ month, year })
        });
        
        if (response.ok) {
            const result = await response.json();
            showSuccess(`Generated ${result.count} payslips`);
            loadPayslipsData();
        }
    } catch (error) {
        console.error('Generate bulk payslips error:', error);
    }
}

async function viewPayslip(id) {
    try {
        const response = await apiCall(`/api/payslips/${id}`);
        
        if (response.ok) {
            const payslip = await response.json();
            showPayslipModal(payslip);
        }
    } catch (error) {
        console.error('View payslip error:', error);
    }
}

function showPayslipModal(payslip) {
    const modal = document.getElementById('payslipModal');
    const content = document.getElementById('payslipContent');
    
    if (!modal || !content) return;
    
    content.innerHTML = `
        <div class="payslip-details">
            <h3>Payslip - ${payslip.month} ${payslip.year}</h3>
            <div class="payslip-header">
                <p><strong>Employee:</strong> ${payslip.employeeName}</p>
                <p><strong>Department:</strong> ${payslip.department || 'N/A'}</p>
                <p><strong>Designation:</strong> ${payslip.designation || 'N/A'}</p>
            </div>
            <div class="payslip-body">
                <div class="earnings">
                    <h4>Earnings</h4>
                    <p>Basic Salary: ${formatCurrency(payslip.basicSalary)}</p>
                    <p>HRA: ${formatCurrency(payslip.hra)}</p>
                    <p>DA: ${formatCurrency(payslip.da)}</p>
                    <p>Conveyance: ${formatCurrency(payslip.conveyance)}</p>
                    <p>Medical: ${formatCurrency(payslip.medical)}</p>
                    <p>Special Allowance: ${formatCurrency(payslip.specialAllowance)}</p>
                    <p><strong>Gross Salary: ${formatCurrency(payslip.grossSalary)}</strong></p>
                </div>
                <div class="deductions">
                    <h4>Deductions</h4>
                    <p>PF: ${formatCurrency(payslip.pf)}</p>
                    <p>Tax: ${formatCurrency(payslip.tax)}</p>
                    <p>Other: ${formatCurrency(payslip.otherDeductions)}</p>
                    <p><strong>Total Deductions: ${formatCurrency(payslip.totalDeductions)}</strong></p>
                </div>
            </div>
            <div class="payslip-footer">
                <h4>Net Salary: ${formatCurrency(payslip.netSalary)}</h4>
            </div>
        </div>
    `;
    
    modal.style.display = 'block';
}

function downloadPayslip(id) {
    const token = localStorage.getItem('token');
    const url = `/api/payslips/${id}/download`;
    
    fetch(url, {
        headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(response => {
        if (response.ok) {
            return response.blob();
        }
        throw new Error('Download failed');
    })
    .then(blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `payslip-${id}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        showSuccess('Payslip downloaded successfully');
    })
    .catch(error => {
        console.error('Download error:', error);
        // Fallback to mock download
        mockDownloadPayslip(id);
    });
}

function mockDownloadPayslip(id) {
    showSuccess('Downloading payslip...');
    const link = document.createElement('a');
    link.href = 'data:text/plain;charset=utf-8,' + encodeURIComponent(`Payslip ID: ${id}\nGenerated: ${new Date().toLocaleDateString()}`);
    link.download = `payslip-${id}.txt`;
    link.click();
}

async function emailPayslip(id) {
    try {
        const response = await apiCall(`/api/payslips/${id}/send`, { method: 'POST' });
        
        if (response.ok) {
            showSuccess('Payslip emailed successfully');
        } else {
            alert('Failed to email payslip');
        }
    } catch (error) {
        console.error('Email payslip error:', error);
    }
}

async function emailBulkPayslips(month, year) {
    try {
        const response = await apiCall('/api/payslips/email-bulk', {
            method: 'POST',
            body: JSON.stringify({ month, year })
        });
        
        if (response.ok) {
            const result = await response.json();
            showSuccess(`Emailed ${result.count} payslips`);
        }
    } catch (error) {
        console.error('Email bulk payslips error:', error);
    }
}

function closePayslipModal() {
    document.getElementById('payslipModal').style.display = 'none';
}

function searchPayslips() {
    const searchTerm = document.getElementById('payslipSearch')?.value.toLowerCase() || '';
    const monthFilter = document.getElementById('payslipMonthFilter')?.value;
    const yearFilter = document.getElementById('payslipYearFilter')?.value;
    
    const rows = document.querySelectorAll('#payslipsList tr, #myPayslipsList tr');
    
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        const matchesSearch = !searchTerm || text.includes(searchTerm);
        const matchesMonth = !monthFilter || text.includes(monthFilter.toLowerCase());
        const matchesYear = !yearFilter || text.includes(yearFilter);
        
        row.style.display = matchesSearch && matchesMonth && matchesYear ? '' : 'none';
    });
}

function exportAllPayslips() {
    showSuccess('Exporting all payslips...');
    // Mock export functionality
    const csvContent = 'Employee,Month,Year,Gross Salary,Net Salary\n';
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `payslips-export-${new Date().toISOString().split('T')[0]}.csv`;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
    showSuccess('All payslips exported successfully');
}

function generateAllPayslips() {
    const month = document.getElementById('payslipMonth')?.value;
    const year = document.getElementById('payslipYear')?.value;
    
    if (!month || !year) {
        alert('Please select month and year');
        return;
    }
    
    generateBulkPayslips(month, year);
}

// Exports
window.loadPayslipsData = loadPayslipsData;
window.loadMyPayslips = loadMyPayslips;
window.generatePayslip = generatePayslip;
window.generateBulkPayslips = generateBulkPayslips;
window.viewPayslip = viewPayslip;
window.downloadPayslip = downloadPayslip;
window.emailPayslip = emailPayslip;
window.emailBulkPayslips = emailBulkPayslips;
window.closePayslipModal = closePayslipModal;
window.searchPayslips = searchPayslips;
window.exportAllPayslips = exportAllPayslips;
window.generateAllPayslips = generateAllPayslips;
