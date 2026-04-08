// HRM System - Department Module
// Corresponds to: DepartmentController.java, DepartmentService.java, Department.java

async function loadDepartments() {
    try {
        const response = await apiCall('/api/departments');

        if (!response.ok) {
            throw new Error('Failed to fetch departments');
        }

        const departments = await response.json();

        // Populate dropdown
        const select = document.getElementById('department');
        if (select) {
            select.innerHTML = '<option value="">Select Department</option>';
            departments.forEach(dept => {
                select.innerHTML += `
                    <option value="${dept.id}">${dept.name}</option>
                `;
            });
        }

        // Render departments table
        renderDepartmentsTable(departments);

    } catch (error) {
        console.error('Error loading departments:', error);
    }
}

function renderDepartmentsTable(departments) {
    const tbody = document.getElementById('departmentsList');
    if (!tbody) return;
    
    tbody.innerHTML = departments.map(dept => `
        <tr>
            <td>${dept.id || 'N/A'}</td>
            <td>${dept.name || 'N/A'}</td>
            <td>${dept.description || '-'}</td>
            <td>${dept.headOfDepartment || '-'}</td>
            <td>${dept.createdDate ? formatDate(dept.createdDate) : '-'}</td>
            <td>${dept.employeeCount || 0}</td>
            <td>
                <button onclick="viewDepartment(${dept.id})" class="btn-view">👁️ View</button>
                <button onclick="editDepartment(${dept.id})">✏️ Edit</button>
                <button onclick="deleteDepartment(${dept.id})" class="btn-danger">🗑️ Delete</button>
            </td>
        </tr>
    `).join('');
}

function updateDepartmentDropdowns(departments) {
    const dropdowns = document.querySelectorAll('#department, #departmentFilter');
    
    dropdowns.forEach(dropdown => {
        if (!dropdown) return;
        
        const currentValue = dropdown.value;
        dropdown.innerHTML = '<option value="">Select Department</option>' +
            departments.map(dept => 
                `<option value="${dept.id}">${dept.name}</option>`
            ).join('');
        
        if (currentValue) {
            dropdown.value = currentValue;
        }
    });
}

function getDepartmentId(departmentName) {
    const departmentMap = {
        'IT': 1, 'Information Technology': 1,
        'HR': 10, 'Human Resources': 10,
        'Sales': 11, 'Sales Department': 11,
        'Design': 12, 'Design Department': 12,
        'Finance': 13, 'Finance Department': 13,
        'Marketing': 14, 'Marketing Department': 14,
        'Product': 15, 'Product Department': 15,
        'Operations': 16, 'Operations Department': 16
    };
    
    return departmentMap[departmentName] || null;
}

async function saveDepartment(event) {
    event.preventDefault();
    
    const id = document.getElementById('editDepartmentId')?.value;
    const name = document.getElementById('deptName')?.value.trim();
    const description = document.getElementById('deptDescription')?.value.trim();
    const headOfDepartment = document.getElementById('headOfDepartment')?.value.trim();
    
    if (!name) {
        alert('Department name is required');
        return;
    }
    
    const data = {
        name: name,
        description: description,
        headOfDepartment: headOfDepartment
    };
    
    try {
        const url = id ? `/api/departments/${id}` : '/api/departments';
        const method = id ? 'PUT' : 'POST';
        
        const response = await apiCall(url, {
            method: method,
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            showSuccess(`Department ${id ? 'updated' : 'created'} successfully`);
            loadDepartments();
            closeDepartmentModal();
        } else {
            const error = await response.text();
            alert('Failed to save department: ' + error);
        }
    } catch (error) {
        console.error('Save department error:', error);
        alert('Failed to save department');
    }
}

async function viewDepartment(id) {
    try {
        const response = await apiCall(`/api/departments/${id}`);
        if (response.ok) {
            const dept = await response.json();
            showDepartmentViewModal(dept);
        } else {
            alert('Failed to load department details');
        }
    } catch (error) {
        console.error('View department error:', error);
        alert('Failed to load department details');
    }
}

function showDepartmentViewModal(dept) {
    const existingModal = document.getElementById('departmentViewModal');
    if (existingModal) existingModal.remove();

    const createdDate = dept.createdDate ? new Date(dept.createdDate).toLocaleDateString('en-IN') : 'N/A';
    const employeeCount = dept.employeeCount || 0;

    const modalHTML = `
        <div id="departmentViewModal" class="modal-overlay" onclick="closeDepartmentViewModal(event)">
            <div class="modal-content department-view-modal" onclick="event.stopPropagation()">
                <div class="modal-header">
                    <h2>🏢 Department Details</h2>
                    <button class="close-btn" onclick="closeDepartmentViewModal()">&times;</button>
                </div>
                <div class="modal-body">
                    <div class="dept-profile-header">
                        <div class="dept-avatar-large">${dept.name?.charAt(0) || 'D'}</div>
                        <div class="dept-title">
                            <h3>${dept.name || 'N/A'}</h3>
                            <span class="dept-id-badge">ID: #${dept.id || 'N/A'}</span>
                            <span class="employee-count-badge">👥 ${employeeCount} Employees</span>
                        </div>
                    </div>
                    
                    <div class="info-sections">
                        <div class="info-section">
                            <h4>📋 Department Information</h4>
                            <div class="info-grid">
                                <div class="info-item"><label>Department Name:</label><span>${dept.name || 'N/A'}</span></div>
                                <div class="info-item full-width"><label>Description:</label><span>${dept.description || 'No description available'}</span></div>
                                <div class="info-item"><label>Head of Department:</label><span>${dept.headOfDepartment || 'Not assigned'}</span></div>
                                <div class="info-item"><label>Created Date:</label><span>${createdDate}</span></div>
                                <div class="info-item"><label>Total Employees:</label><span class="count-highlight">${employeeCount}</span></div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button onclick="closeDepartmentViewModal()" class="btn-secondary">Close</button>
                    <button onclick="closeDepartmentViewModal(); editDepartment(${dept.id});" class="btn-primary">✏️ Edit</button>
                </div>
            </div>
        </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHTML);
    document.getElementById('departmentViewModal').style.display = 'flex';
}

function closeDepartmentViewModal(event) {
    if (!event || event.target.id === 'departmentViewModal' || event.target.classList.contains('close-btn')) {
        const modal = document.getElementById('departmentViewModal');
        if (modal) modal.remove();
    }
}

async function editDepartment(id) {
    try {
        console.log('Fetching department with ID:', id);
        const response = await apiCall(`/api/departments/${id}`);
        console.log('Response status:', response.status);
        
        if (response.ok) {
            const dept = await response.json();
            document.getElementById('editDepartmentId').value = dept.id;
            document.getElementById('editDeptName').value = dept.name || '';
            document.getElementById('editDeptDescription').value = dept.description || '';
            document.getElementById('editHeadOfDepartment').value = dept.headOfDepartment || '';
            
            document.getElementById('editDepartmentModal').style.display = 'flex';
        } else {
            const errorText = await response.text();
            console.error('Server error response:', errorText);
            alert('Failed to load department: ' + errorText);
        }
    } catch (error) {
        console.error('Error loading department for edit:', error);
        alert('Error: ' + error.message);
    }
}

function closeEditDepartmentModal() {
    document.getElementById('editDepartmentModal').style.display = 'none';
    document.getElementById('editDepartmentForm')?.reset();
    document.getElementById('editDepartmentId').value = '';
}

function closeDepartmentModal() {
    const modal = document.getElementById('departmentModal');
    if (modal) modal.style.display = 'none';
    const form = document.getElementById('departmentForm');
    if (form) form.reset();
    const idField = document.getElementById('editDepartmentId');
    if (idField) idField.value = '';
}

async function updateDepartment(event) {
    event.preventDefault();
    
    const id = document.getElementById('editDepartmentId')?.value;
    if (!id) return;
    
    const data = {
        name: document.getElementById('editDeptName')?.value.trim(),
        description: document.getElementById('editDeptDescription')?.value.trim(),
        headOfDepartment: document.getElementById('editHeadOfDepartment')?.value.trim()
    };
    
    try {
        const response = await apiCall(`/api/departments/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            showSuccess('Department updated successfully');
            loadDepartments();
            closeEditDepartmentModal();
        } else {
            alert('Failed to update department');
        }
    } catch (error) {
        console.error('Update department error:', error);
    }
}

async function deleteDepartment(id) {
    if (!confirm('Are you sure you want to delete this department?')) return;
    
    try {
        const response = await apiCall(`/api/departments/${id}`, { method: 'DELETE' });
        
        if (response.ok) {
            showSuccess('Department deleted successfully');
            loadDepartments();
        } else {
            alert('Failed to delete department');
        }
    } catch (error) {
        console.error('Delete department error:', error);
    }
}

function searchDepartments() {
    const searchTerm = document.getElementById('departmentSearch')?.value.toLowerCase() || '';
    const rows = document.querySelectorAll('#departmentsList tr');
    
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        row.style.display = !searchTerm || text.includes(searchTerm) ? '' : 'none';
    });
}

function refreshDepartments() {
    showSuccess('Refreshing departments...');
    loadDepartments();
}

function showAddDepartmentForm() {
    hideAddDepartmentForm();
    const card = document.getElementById('addDepartmentCard');
    if (card) {
        card.style.display = 'block';
        card.scrollIntoView({ behavior: 'smooth' });
    }
}

function hideAddDepartmentForm() {
    const card = document.getElementById('addDepartmentCard');
    if (card) card.style.display = 'none';
    const form = document.getElementById('departmentForm');
    if (form) form.reset();
}

// Exports
window.loadDepartments = loadDepartments;
window.saveDepartment = saveDepartment;
window.editDepartment = editDepartment;
window.viewDepartment = viewDepartment;
window.closeDepartmentViewModal = closeDepartmentViewModal;
window.updateDepartment = updateDepartment;
window.deleteDepartment = deleteDepartment;
window.closeDepartmentModal = closeDepartmentModal;
window.closeEditDepartmentModal = closeEditDepartmentModal;
window.searchDepartments = searchDepartments;
window.refreshDepartments = refreshDepartments;
window.showAddDepartmentForm = showAddDepartmentForm;
window.hideAddDepartmentForm = hideAddDepartmentForm;
window.getDepartmentId = getDepartmentId;
