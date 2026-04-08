// View employee details in a professional modal
window.viewEmployee = function(id) {
    console.log('Viewing employee:', id);
    
    // Fetch employee details from API
    fetchEmployeeDetails(id);
};

// Fetch employee details by ID
async function fetchEmployeeDetails(id) {
    try {
        const response = await fetch(`/api/employees/${id}`, {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });
        
        if (response.ok) {
            const employee = await response.json();
            showEmployeeModal(employee);
        } else {
            alert('Failed to fetch employee details');
        }
    } catch (error) {
        console.error('Error fetching employee details:', error);
        alert('Error fetching employee details');
    }
}

// Show professional employee details modal
function showEmployeeModal(employee) {
    // Create modal overlay
    const modalOverlay = document.createElement('div');
    modalOverlay.style.cssText = `
        position: fixed; top: 0; left: 0; width: 100%; height: 100%;
        background: rgba(0, 0, 0, 0.8);
        display: flex; justify-content: center; align-items: center;
        z-index: 10000;
    `;
    
    // Create modal content
    const modalContent = document.createElement('div');
    modalContent.style.cssText = `
        background: white; padding: 30px; border-radius: 15px;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        max-width: 700px; width: 90%; max-height: 85vh; overflow-y: auto;
        position: relative;
    `;
    
    modalContent.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; border-bottom: 3px solid #007bff; padding-bottom: 15px;">
            <h2 style="margin: 0; color: #007bff; font-size: 28px;">👁 Employee Details</h2>
            <button onclick="closeEmployeeModal()" style="background: #dc3545; color: white; border: none; padding: 10px 20px; border-radius: 8px; cursor: pointer; font-size: 16px; font-weight: bold;">✖ Close</button>
        </div>
        
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 25px;">
            <div style="background: #f8f9fa; padding: 20px; border-radius: 10px; border-left: 4px solid #007bff;">
                <h3 style="color: #007bff; margin: 0 0 15px 0; font-size: 18px;">📋 Basic Information</h3>
                <p style="margin: 8px 0;"><strong>Employee ID:</strong> EMP${String(employee.id || '').padStart(3, '0')}</p>
                <p style="margin: 8px 0;"><strong>Name:</strong> ${employee.firstName || ''} ${employee.lastName || ''}</p>
                <p style="margin: 8px 0;"><strong>Email:</strong> ${employee.email || 'N/A'}</p>
                <p style="margin: 8px 0;"><strong>Phone:</strong> ${employee.phone || 'N/A'}</p>
                <p style="margin: 8px 0;"><strong>Gender:</strong> ${employee.gender || 'N/A'}</p>
            </div>
            <div style="background: #f8f9fa; padding: 20px; border-radius: 10px; border-left: 4px solid #28a745;">
                <h3 style="color: #28a745; margin: 0 0 15px 0; font-size: 18px;">💼 Job Information</h3>
                <p style="margin: 8px 0;"><strong>Department:</strong> ${employee.departmentName || 'N/A'}</p>
                <p style="margin: 8px 0;"><strong>Designation:</strong> ${employee.designation || 'N/A'}</p>
                <p style="margin: 8px 0;"><strong>Joining Date:</strong> ${employee.joiningDate || 'N/A'}</p>
                <p style="margin: 8px 0;"><strong>Status:</strong> <span style="padding: 6px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; background: ${employee.status === 'ACTIVE' ? '#28a745' : '#ffc107'}; color: white;">${employee.status || 'ACTIVE'}</span></p>
            </div>
        </div>
        
        <div style="background: #f8f9fa; padding: 20px; border-radius: 10px; border-left: 4px solid #ffc107; margin-top: 25px;">
            <h3 style="color: #ffc107; margin: 0 0 15px 0; font-size: 18px;">💰 Financial Information</h3>
            <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 15px;">
                <p style="margin: 8px 0;"><strong>Salary:</strong> ₹${employee.salary || '0'}</p>
                <p style="margin: 8px 0;"><strong>PF:</strong> ₹${employee.pf || '0'}</p>
                <p style="margin: 8px 0;"><strong>Tax:</strong> ₹${employee.tax || '0'}</p>
            </div>
        </div>
        
        <div style="background: #f8f9fa; padding: 20px; border-radius: 10px; border-left: 4px solid #6f42c1; margin-top: 25px;">
            <h3 style="color: #6f42c1; margin: 0 0 15px 0; font-size: 18px;">📍 Address Information</h3>
            <p style="margin: 8px 0; line-height: 1.6;">${employee.address || 'No address provided'}</p>
        </div>
        
        <div style="margin-top: 30px; text-align: center; padding-top: 20px; border-top: 2px solid #e9ecef;">
            <button onclick="editEmployeeFromModal(${employee.id})" style="background: #007bff; color: white; border: none; padding: 15px 30px; border-radius: 8px; cursor: pointer; font-size: 16px; font-weight: bold; margin-right: 15px;">🔄 Edit Employee</button>
            <button onclick="closeEmployeeModal()" style="background: #6c757d; color: white; border: none; padding: 15px 30px; border-radius: 8px; cursor: pointer; font-size: 16px; font-weight: bold;">✖ Close</button>
        </div>
    `;
    
    modalOverlay.appendChild(modalContent);
    document.body.appendChild(modalOverlay);
    
    console.log('Professional employee modal displayed for:', employee);
}

// Close employee modal
window.closeEmployeeModal = function() {
    const modalOverlay = document.querySelector('div[style*="position: fixed"]');
    if (modalOverlay) {
        modalOverlay.remove();
    }
};

// Edit employee from modal
window.editEmployee = function(id) {
    console.log('Editing employee:', id);

    // ALWAYS fetch full data from backend
    fetchEmployeeById(id);
};