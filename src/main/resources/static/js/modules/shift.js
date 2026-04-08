// HRM System - Shift Management Module
// Corresponds to: ShiftController.java, ShiftService.java, Shift.java

// Load all shifts
async function loadShifts() {
    try {
        const response = await apiCall('/api/shifts');

        if (response.ok) {
            const shifts = await response.json();
            renderShiftTable(shifts);
        } else {
            console.error('Failed to load shifts');
            renderShiftTable([]);
        }
    } catch (error) {
        console.error('Error loading shifts:', error);
        renderShiftTable([]);
    }
}

// Render shift table
function renderShiftTable(shifts) {
    const tbody = document.getElementById('shiftList');
    if (!tbody) return;

    if (!shifts || shifts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align: center;">No shifts found</td></tr>';
        return;
    }

    tbody.innerHTML = shifts.map(shift => `
        <tr>
            <td>${shift.id}</td>
            <td>${shift.name}</td>
            <td>${formatTime(shift.startTime)}</td>
            <td>${formatTime(shift.endTime)}</td>
            <td>${shift.workingHours}</td>
            <td>${shift.isDefault ? '✅ Yes' : 'No'}</td>
            <td>${shift.active ? '🟢 Active' : '🔴 Inactive'}</td>
            <td>${shift.description || '-'}</td>
            <td>
                <button onclick="openEditShiftModal(${shift.id})">✏️ Edit</button>
                ${!shift.isDefault ? `<button onclick="deleteShift(${shift.id})" class="btn-danger">🗑️ Delete</button>` : ''}
                ${!shift.isDefault ? `<button onclick="setDefaultShift(${shift.id})">⭐ Set Default</button>` : ''}
            </td>
        </tr>
    `).join('');
}

// Open modal for adding new shift
function openShiftModal() {
    document.getElementById('shiftModalTitle').textContent = '➕ Add Shift';
    document.getElementById('shiftForm').reset();
    document.getElementById('shiftId').value = '';

    // Set default times
    document.getElementById('shiftStartTime').value = '10:00';
    document.getElementById('shiftEndTime').value = '18:30';
    document.getElementById('shiftWorkingHours').value = '8.5';

    document.getElementById('shiftModal').style.display = 'block';
}

// Open modal for editing shift
async function openEditShiftModal(id) {
    try {
        const response = await apiCall(`/api/shifts/${id}`);
        if (response.ok) {
            const shift = await response.json();

            document.getElementById('shiftModalTitle').textContent = '✏️ Edit Shift';
            document.getElementById('shiftId').value = shift.id;
            document.getElementById('shiftName').value = shift.name;
            document.getElementById('shiftStartTime').value = shift.startTime;
            document.getElementById('shiftEndTime').value = shift.endTime;
            document.getElementById('shiftWorkingHours').value = shift.workingHours;
            document.getElementById('shiftDescription').value = shift.description || '';
            document.getElementById('shiftIsDefault').checked = shift.isDefault;
            document.getElementById('shiftActive').checked = shift.active;

            document.getElementById('shiftModal').style.display = 'block';
        } else {
            showError('Failed to load shift details');
        }
    } catch (error) {
        console.error('Error loading shift:', error);
        showError('Error loading shift details');
    }
}

// Close shift modal
function closeShiftModal() {
    document.getElementById('shiftModal').style.display = 'none';
}

// Save shift (create or update)
async function saveShift(event) {
    event.preventDefault();

    const id = document.getElementById('shiftId').value;
    const shiftData = {
        name: document.getElementById('shiftName').value,
        startTime: document.getElementById('shiftStartTime').value,
        endTime: document.getElementById('shiftEndTime').value,
        workingHours: parseFloat(document.getElementById('shiftWorkingHours').value),
        description: document.getElementById('shiftDescription').value,
        isDefault: document.getElementById('shiftIsDefault').checked,
        active: document.getElementById('shiftActive').checked
    };

    try {
        const url = id ? `/api/shifts/${id}` : '/api/shifts';
        const method = id ? 'PUT' : 'POST';

        const response = await apiCall(url, {
            method: method,
            body: JSON.stringify(shiftData)
        });

        if (response.ok) {
            showSuccess(id ? 'Shift updated successfully' : 'Shift created successfully');
            closeShiftModal();
            loadShifts();
        } else {
            const error = await response.text();
            showError('Failed to save shift: ' + error);
        }
    } catch (error) {
        console.error('Error saving shift:', error);
        showError('Error saving shift');
    }
}

// Delete shift
async function deleteShift(id) {
    if (!confirm('Are you sure you want to delete this shift?')) return;

    try {
        const response = await apiCall(`/api/shifts/${id}`, { method: 'DELETE' });

        if (response.ok) {
            showSuccess('Shift deleted successfully');
            loadShifts();
        } else {
            const error = await response.text();
            showError('Failed to delete shift: ' + error);
        }
    } catch (error) {
        console.error('Error deleting shift:', error);
        showError('Error deleting shift');
    }
}

// Set shift as default
async function setDefaultShift(id) {
    if (!confirm('Set this shift as default?')) return;

    try {
        const response = await apiCall(`/api/shifts/${id}/set-default`, { method: 'POST' });

        if (response.ok) {
            showSuccess('Default shift updated successfully');
            loadShifts();
        } else {
            const error = await response.text();
            showError('Failed to set default shift: ' + error);
        }
    } catch (error) {
        console.error('Error setting default shift:', error);
        showError('Error setting default shift');
    }
}
