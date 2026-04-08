// HRM System - Employee Module
// Corresponds to: EmployeeController.java, EmployeeService.java, Employee.java

// Load employees from backend
async function loadEmployees() {
    try {
        console.log('Loading employees from /api/employees...');
        const response = await apiCall('/api/employees');
        
        if (response.ok) {
            const employees = await response.json();
            console.log('Employees loaded:', employees);
            renderEmployeesTable(employees);
        } else {
            console.error('Failed to load employees, status:', response.status);
            const errorText = await response.text();
            console.error('Error response:', errorText);
        }
    } catch (error) {
        console.error('Error loading employees:', error);
    }
}

function renderEmployeesTable(employees) {
    const tbody = document.getElementById('employeesList');
    if (!tbody) {
        console.error('employeesList element not found');
        return;
    }
    
    if (!employees || employees.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="8" style="text-align: center; padding: 2rem;">
                    <p>No employees found in database.</p>
                    <button onclick="showAddEmployeeForm()" style="margin-top: 1rem;">➕ Add First Employee</button>
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = employees.map(emp => {
        const deptName = emp.department?.name || emp.departmentName || 'N/A';
        const fullName = `${emp.firstName || ''} ${emp.lastName || ''}`.trim() || 'N/A';
        const salary = emp.salary ? `₹${emp.salary.toLocaleString('en-IN')}` : 'N/A';
        return `
        <tr>
            <td>${emp.id || 'N/A'}</td>
            <td>${fullName}</td>
            <td>${emp.email || 'N/A'}</td>
            <td>${deptName}</td>
            <td>${emp.designation || 'N/A'}</td>
            <td>${salary}</td>
            <td><span class="badge ${emp.status || 'ACTIVE'}">${emp.status || 'ACTIVE'}</span></td>
            <td>
                <button onclick="viewEmployee(${emp.id})" class="btn-view">👁️ View</button>
                <button onclick="editEmployee(${emp.id})">✏️ Edit</button>
                <button onclick="deleteEmployee(${emp.id})" class="btn-danger">🗑️ Delete</button>
            </td>
        </tr>
    `}).join('');
}

async function addEmployee(event) {
    event.preventDefault();
    
    const form = event.target;
    const submitBtn = form.querySelector('button[type="submit"]');
    const originalText = submitBtn.textContent;
    
    let employeeId = form.getAttribute('data-employee-id');
    const isEditMode = !!(employeeId || window.currentEditingEmployee);
    
    // Validate required fields
    const firstName = document.getElementById('firstName').value.trim();
    const lastName = document.getElementById('lastName').value.trim();
    const email = document.getElementById('email').value.trim();
    const departmentId = Number(document.getElementById('department').value);
    
    if (!firstName || !lastName || !email) {
        alert('Please fill in First Name, Last Name, and Email');
        return;
    }
    
    if (!departmentId || isNaN(departmentId)) {
        alert('Please select a department');
        return;
    }
    
    submitBtn.disabled = true;
    submitBtn.textContent = isEditMode ? 'Updating...' : 'Creating...';
    
    const employeeData = buildEmployeeData(isEditMode);
    console.log('Sending employee data:', employeeData);
    
    try {
        const url = isEditMode ? `/api/employees/${employeeId || window.currentEditingEmployee?.id}` : '/api/employees';
        const method = isEditMode ? 'PUT' : 'POST';
        
        const response = await apiCall(url, {
            method: method,
            body: JSON.stringify(employeeData)
        });
        
        if (response.ok) {
            const result = await response.json();
            showSuccess(`Employee ${isEditMode ? 'updated' : 'created'} successfully!`);
            loadEmployees();
            clearEmployeeForm();
        } else {
            const error = await response.text();
            console.error('Server error:', error);
            alert(`Failed to ${isEditMode ? 'update' : 'add'} employee: ${error}`);
        }
    } catch (error) {
        console.error('Employee save error:', error);
        alert('Failed to save employee');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = originalText;
    }
}

function buildEmployeeData(isEditMode) {
    const baseData = isEditMode && window.currentEditingEmployee ? window.currentEditingEmployee : {};
    
    return {
        ...baseData,
        firstName: document.getElementById('firstName').value.trim(),
        lastName: document.getElementById('lastName').value.trim(),
        email: document.getElementById('email').value.trim(),
        phone: document.getElementById('phone').value,
        gender: document.getElementById('gender').value,
        departmentId: Number(document.getElementById('department').value),
        designation: document.getElementById('designation').value,
        joiningDate: document.getElementById('joiningDate').value,
        salary: Number(document.getElementById('salary').value) || 0,
        basicSalary: Number(document.getElementById('basicSalary')?.value) || 0,
        da: Number(document.getElementById('da')?.value) || 0,
        hra: Number(document.getElementById('hra')?.value) || 0,
        otherAllowance: Number(document.getElementById('otherAllowance')?.value) || 0,
        pf: Number(document.getElementById('pf').value) || 0,
        tax: Number(document.getElementById('tax').value) || 0,
        probationPeriodMonths: Number(document.getElementById('probationPeriodMonths')?.value) || 0,
        address: document.getElementById('address').value || '',
        status: document.getElementById('status')?.value || 'ACTIVE'
    };
}

async function editEmployee(id) {
    try {
        // Load departments FIRST so dropdown has options
        await loadDepartments();
        
        const response = await apiCall(`/api/employees/${id}`);
        if (response.ok) {
            const employee = await response.json();
            window.currentEditingEmployee = employee;
            
            // Populate form - matching Employee.java fields
            document.getElementById('firstName').value = employee.firstName || '';
            document.getElementById('lastName').value = employee.lastName || '';
            document.getElementById('email').value = employee.email || '';
            document.getElementById('phone').value = employee.phone || '';
            document.getElementById('gender').value = employee.gender || '';
            document.getElementById('designation').value = employee.designation || '';
            document.getElementById('salary').value = employee.salary || '';
            document.getElementById('basicSalary').value = employee.basicSalary || '';
            document.getElementById('da').value = employee.da || '';
            document.getElementById('hra').value = employee.hra || '';
            document.getElementById('otherAllowance').value = employee.otherAllowance || '';
            document.getElementById('joiningDate').value = employee.joiningDate || '';
            document.getElementById('pf').value = employee.pf || '';
            document.getElementById('tax').value = employee.tax || '';
            document.getElementById('address').value = employee.address || '';
            document.getElementById('status').value = employee.status || 'ACTIVE';
            document.getElementById('probationPeriodMonths').value = employee.probationPeriodMonths || 0;
            
            // Set department AFTER dropdowns are loaded
            const deptField = document.getElementById('department');
            if (deptField) {
                deptField.value = employee.departmentId || employee.department?.id || '';
            }
            
            // Show the edit form WITHOUT clearing (data is already populated above)
            const card = document.getElementById('addEmployeeCard');
            if (card) {
                card.style.display = 'block';
                card.scrollIntoView({ behavior: 'smooth' });
            }
            const submitBtn = document.getElementById('addEmployeeForm')?.querySelector('button[type="submit"]');
            if (submitBtn) submitBtn.textContent = '💾 Update Employee';
        }
    } catch (error) {
        console.error('Error loading employee for edit:', error);
    }
}

async function viewEmployee(id) {
    try {
        const response = await apiCall(`/api/employees/${id}`);
        if (response.ok) {
            const emp = await response.json();
            showEmployeeViewModal(emp);
        } else {
            alert('Failed to load employee details');
        }
    } catch (error) {
        console.error('View employee error:', error);
        alert('Failed to load employee details');
    }
}

function showEmployeeViewModal(emp) {
    // Remove existing modal if any
    const existingModal = document.getElementById('employeeViewModal');
    if (existingModal) existingModal.remove();

    const deptName = emp.department?.name || emp.departmentName || 'N/A';
    const fullName = `${emp.firstName || ''} ${emp.lastName || ''}`.trim() || 'N/A';
    const joiningDate = emp.joiningDate ? new Date(emp.joiningDate).toLocaleDateString('en-IN') : 'N/A';
    const salary = emp.salary ? `₹${emp.salary.toLocaleString('en-IN')}` : 'N/A';
    const basicSalary = emp.basicSalary ? `₹${emp.basicSalary.toLocaleString('en-IN')}` : 'N/A';
    const da = emp.da ? `₹${emp.da.toLocaleString('en-IN')}` : 'N/A';
    const hra = emp.hra ? `₹${emp.hra.toLocaleString('en-IN')}` : 'N/A';
    const otherAllowance = emp.otherAllowance ? `₹${emp.otherAllowance.toLocaleString('en-IN')}` : 'N/A';
    const pf = emp.pf ? `₹${emp.pf.toLocaleString('en-IN')}` : 'N/A';
    const tax = emp.tax ? `₹${emp.tax.toLocaleString('en-IN')}` : 'N/A';

    // Calculate probation status
    let probationStatusHTML = '';
    if (emp.probationPeriodMonths && emp.probationPeriodMonths > 0 && emp.joiningDate) {
        const joiningDateObj = new Date(emp.joiningDate);
        const probationEndDate = new Date(joiningDateObj);
        probationEndDate.setMonth(probationEndDate.getMonth() + emp.probationPeriodMonths);
        
        const today = new Date();
        const isUnderProbation = today < probationEndDate;
        
        if (isUnderProbation) {
            const diffTime = probationEndDate - today;
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            const monthsRemaining = Math.floor(diffDays / 30);
            const daysRemaining = diffDays % 30;
            probationStatusHTML = `<span class="probation-badge" style="background: #ff9800;">🎯 Under Probation (${monthsRemaining}m ${daysRemaining}d remaining)</span>`;
        } else {
            probationStatusHTML = `<span class="probation-badge" style="background: #4caf50;">✅ Probation Completed</span>`;
        }
    }

    const modalHTML = `
        <div id="employeeViewModal" class="modal-overlay" onclick="closeEmployeeViewModal(event)">
            <div class="modal-content employee-view-modal" onclick="event.stopPropagation()">
                <div class="modal-header">
                    <h2>👤 Employee Details</h2>
                    <button class="close-btn" onclick="closeEmployeeViewModal()">&times;</button>
                </div>
                <div class="modal-body">
                    <div class="employee-profile-header">
                        <div class="employee-avatar-large">${emp.firstName?.charAt(0) || 'E'}${emp.lastName?.charAt(0) || ''}</div>
                        <div class="employee-title">
                            <h3>${fullName}</h3>
                            <span class="designation-badge">${emp.designation || 'N/A'}</span>
                            <span class="status-badge ${emp.status || 'ACTIVE'}">${emp.status || 'ACTIVE'}</span>
                            ${probationStatusHTML}
                        </div>
                    </div>
                    
                    <div class="info-sections">
                        <div class="info-section">
                            <h4>📋 Personal Information</h4>
                            <div class="info-grid">
                                <div class="info-item"><label>Employee ID:</label><span>#${emp.id || 'N/A'}</span></div>
                                <div class="info-item"><label>Email:</label><span>${emp.email || 'N/A'}</span></div>
                                <div class="info-item"><label>Phone:</label><span>${emp.phone || 'N/A'}</span></div>
                                <div class="info-item"><label>Gender:</label><span>${emp.gender || 'N/A'}</span></div>
                                <div class="info-item"><label>Address:</label><span>${emp.address || 'N/A'}</span></div>
                            </div>
                        </div>

                        <div class="info-section">
                            <h4>🏢 Employment Details</h4>
                            <div class="info-grid">
                                <div class="info-item"><label>Department:</label><span>${deptName}</span></div>
                                <div class="info-item"><label>Joining Date:</label><span>${joiningDate}</span></div>
                                <div class="info-item"><label>Status:</label><span>${emp.status || 'ACTIVE'}</span></div>
                                ${emp.probationPeriodMonths > 0 ? `<div class="info-item"><label>Probation Period:</label><span>${emp.probationPeriodMonths} months</span></div>` : ''}
                            </div>
                        </div>

                        <div class="info-section">
                            <h4>💰 Salary Information</h4>
                            <div class="info-grid salary-grid">
                                <div class="info-item"><label>Total Salary:</label><span class="salary-highlight">${salary}</span></div>
                                <div class="info-item"><label>Basic Salary:</label><span>${basicSalary}</span></div>
                                <div class="info-item"><label>DA:</label><span>${da}</span></div>
                                <div class="info-item"><label>HRA:</label><span>${hra}</span></div>
                                <div class="info-item"><label>Other Allowance:</label><span>${otherAllowance}</span></div>
                                <div class="info-item"><label>PF:</label><span>${pf}</span></div>
                                <div class="info-item"><label>Tax:</label><span>${tax}</span></div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button onclick="closeEmployeeViewModal()" class="btn-secondary">Close</button>
                    <button onclick="closeEmployeeViewModal(); editEmployee(${emp.id});" class="btn-primary">✏️ Edit</button>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);
    document.getElementById('employeeViewModal').style.display = 'flex';
}

function closeEmployeeViewModal(event) {
    if (!event || event.target.id === 'employeeViewModal' || event.target.classList.contains('close-btn')) {
        const modal = document.getElementById('employeeViewModal');
        if (modal) modal.remove();
    }
}

async function deleteEmployee(id) {
    if (!confirm('Are you sure you want to delete this employee?')) return;
    
    try {
        const response = await apiCall(`/api/employees/${id}`, { method: 'DELETE' });
        
        if (response.ok) {
            showSuccess('Employee deleted successfully');
            loadEmployees();
        } else {
            alert('Failed to delete employee');
        }
    } catch (error) {
        console.error('Delete employee error:', error);
        alert('Failed to delete employee');
    }
}

function clearEmployeeForm() {
    const fields = ['firstName', 'lastName', 'email', 'phone', 'gender', 'department', 'designation', 
                    'salary', 'basicSalary', 'da', 'hra', 'otherAllowance', 'joiningDate', 'pf', 'tax', 'address'];
    fields.forEach(fieldId => {
        const field = document.getElementById(fieldId);
        if (field) field.value = '';
    });
    
    const statusField = document.getElementById('status');
    if (statusField) statusField.value = 'ACTIVE';
    
    const probationMonthsField = document.getElementById('probationPeriodMonths');
    if (probationMonthsField) probationMonthsField.value = 0;
    
    const calculatePf = document.getElementById('calculatePf');
    if (calculatePf) calculatePf.checked = false;
    
    const calculateTax = document.getElementById('calculateTax');
    if (calculateTax) calculateTax.checked = false;
    
    const selectedFiles = document.getElementById('selectedFiles');
    if (selectedFiles) selectedFiles.innerHTML = '';
    
    window.currentEditingEmployee = null;
    
    const submitBtn = document.getElementById('addEmployeeForm')?.querySelector('button[type="submit"]');
    if (submitBtn) submitBtn.textContent = '➕ Add Employee';
}

function togglePfCalculation() {
    const checkbox = document.getElementById('calculatePf');
    const pfInput = document.getElementById('pf');
    const salaryInput = document.getElementById('salary');
    
    if (checkbox?.checked) {
        const salary = parseFloat(salaryInput?.value) || 0;
        const basicSalary = salary * 0.6;
        const pfAmount = basicSalary * 0.12;
        if (pfInput) {
            pfInput.value = pfAmount > 0 ? pfAmount.toFixed(2) : '0.00';
            pfInput.disabled = true;
        }
    } else if (pfInput) {
        pfInput.disabled = false;
        pfInput.value = '';
    }
}

function toggleTaxCalculation() {
    const checkbox = document.getElementById('calculateTax');
    const taxInput = document.getElementById('tax');
    const salaryInput = document.getElementById('salary');
    
    if (checkbox?.checked) {
        const salary = parseFloat(salaryInput?.value) || 0;
        const taxAmount = salary * 0.10;
        if (taxInput) {
            taxInput.value = taxAmount > 0 ? taxAmount.toFixed(2) : '0.00';
            taxInput.disabled = true;
        }
    } else if (taxInput) {
        taxInput.disabled = false;
        taxInput.value = '';
    }
}

function searchEmployees() {
    const searchTerm = document.getElementById('employeeSearch')?.value.toLowerCase() || '';
    const departmentFilter = document.getElementById('departmentFilter')?.value || '';
    const rows = document.querySelectorAll('#employeesList tr');
    
    let count = 0;
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        const matchesSearch = !searchTerm || text.includes(searchTerm);
        const matchesDepartment = !departmentFilter || text.includes(departmentFilter.toLowerCase());
        
        row.style.display = matchesSearch && matchesDepartment ? '' : 'none';
        if (row.style.display !== 'none') count++;
    });
    
    showSuccess(`Found ${count} employees`);
}

function refreshEmployees() {
    showSuccess('Refreshing employee list...');
    loadEmployees();
}

function showAddEmployeeForm() {
    clearEmployeeForm();
    loadDepartments(); // Load departments for the dropdown
    const card = document.getElementById('addEmployeeCard');
    if (card) {
        card.style.display = 'block';
        card.scrollIntoView({ behavior: 'smooth' });
    }
}

function hideAddEmployeeForm() {
    const card = document.getElementById('addEmployeeCard');
    if (card) card.style.display = 'none';
}

// Exports
window.loadEmployees = loadEmployees;
window.addEmployee = addEmployee;
window.editEmployee = editEmployee;
window.deleteEmployee = deleteEmployee;
window.viewEmployee = viewEmployee;
window.closeEmployeeViewModal = closeEmployeeViewModal;
window.clearEmployeeForm = clearEmployeeForm;
window.togglePfCalculation = togglePfCalculation;
window.toggleTaxCalculation = toggleTaxCalculation;
window.searchEmployees = searchEmployees;
window.refreshEmployees = refreshEmployees;
window.showAddEmployeeForm = showAddEmployeeForm;
window.hideAddEmployeeForm = hideAddEmployeeForm;
window.currentEditingEmployee = null;
