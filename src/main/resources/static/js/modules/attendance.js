// HRM System - Attendance Module
// Corresponds to: AttendanceController.java, AttendanceService.java, Attendance.java

// Store current shifts and selected shift
let currentShifts = [];
let selectedShift = null;

// Format time from HH:MM:SS or HH:MM:SS.milliseconds to HH:MM
function formatTime(timeString) {
    if (!timeString || timeString === '-') return '-';
    // Handle time strings like "10:00:00.000000" or "14:38:06.945000"
    const timePart = timeString.split('.')[0]; // Remove milliseconds
    const parts = timePart.split(':');
    if (parts.length >= 2) {
        return `${parts[0]}:${parts[1]}`;
    }
    return timeString;
}

// Initialize attendance page with today's date
function initAttendancePage() {
    const datePicker = document.getElementById('attendanceDate');
    if (datePicker) {
        // Set today's date in YYYY-MM-DD format
        const today = new Date().toISOString().split('T')[0];
        datePicker.value = today;
    }
    // Load shifts first, then attendance data
    loadShiftsForAttendance();
    loadAttendanceData();
}

async function loadAttendanceData() {
    try {
        const dateFilter = document.getElementById('attendanceDate')?.value;
        let url = '/api/attendance';
        
        // If date is selected, fetch attendance for that date
        if (dateFilter) {
            url = `/api/attendance/date/${dateFilter}`;
        }
        
        const response = await apiCall(url);
        
        if (response.ok) {
            const data = await response.json();
            renderAttendanceTable(data);
        } else {
            console.log('Using mock attendance data');
            renderAttendanceTable([]);
        }
    } catch (error) {
        console.error('Error loading attendance:', error);
        renderAttendanceTable([]);
    }
}

async function loadMyAttendance() {
    try {
        const response = await apiCall('/api/attendance/my');
        
        if (response.ok) {
            const data = await response.json();
            renderMyAttendanceTable(data);
        }
    } catch (error) {
        console.error('Error loading my attendance:', error);
    }
}

function renderAttendanceTable(attendanceList) {
    const tbody = document.getElementById('attendanceList');
    if (!tbody) return;
    
    tbody.innerHTML = attendanceList.map(att => `
        <tr>
            <td>${att.employeeId || 'N/A'}</td>
            <td>${formatTime(att.checkInTime)}</td>
            <td>${formatTime(att.checkOutTime)}</td>
            <td>${formatDate(att.date)}</td>
            <td>${att.overtimeHours || '-'}</td>
            <td>${att.remarks || '-'}</td>
            <td><span class="badge ${getAttendanceStatusClass(att.status)}">${att.status || 'PRESENT'}</span></td>
            <td>${att.workingHours || '-'}</td>
            <td>${att.employeeName || 'N/A'}</td>
            <td>
                <button onclick="openEditAttendanceModal(${att.id || att.employeeId})">✏️ Edit</button>
            </td>
        </tr>
    `).join('');
}

function renderMyAttendanceTable(attendanceList) {
    const tbody = document.getElementById('myAttendanceList');
    if (!tbody) return;
    
    tbody.innerHTML = attendanceList.map(att => `
        <tr>
            <td>${formatDate(att.date)}</td>
            <td>${att.checkIn || 'N/A'}</td>
            <td>${att.checkOut || 'N/A'}</td>
            <td><span class="badge ${getAttendanceStatusClass(att.status)}">${att.status || 'PRESENT'}</span></td>
            <td>${att.workHours || 'N/A'}</td>
        </tr>
    `).join('');
}

function getAttendanceStatusClass(status) {
    switch (status?.toUpperCase()) {
        case 'PRESENT': return 'success';
        case 'ABSENT': return 'danger';
        case 'HALF_DAY': return 'warning';
        case 'LATE': return 'info';
        case 'ON_LEAVE': return 'secondary';
        default: return 'success';
    }
}

async function markAttendance(event) {
    event.preventDefault();
    
    const employeeId = document.getElementById('attendanceEmployeeId')?.value;
    const date = document.getElementById('attendanceDate')?.value;
    const status = document.getElementById('attendanceStatus')?.value;
    const checkIn = document.getElementById('checkInTime')?.value;
    const checkOut = document.getElementById('checkOutTime')?.value;
    
    const data = {
        employeeId: employeeId,
        date: date,
        status: status,
        checkIn: checkIn,
        checkOut: checkOut
    };
    
    try {
        const response = await apiCall('/api/attendance', {
            method: 'POST',
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            showSuccess('Attendance marked successfully');
            loadAttendanceData();
            event.target.reset();
        } else {
            const error = await response.text();
            alert('Failed to mark attendance: ' + error);
        }
    } catch (error) {
        console.error('Mark attendance error:', error);
        alert('Failed to mark attendance');
    }
}

async function checkIn() {
    try {
        const response = await apiCall('/api/attendance/checkin', { method: 'POST' });
        
        if (response.ok) {
            showSuccess('Checked in successfully!');
            loadMyAttendance();
        } else {
            alert('Failed to check in');
        }
    } catch (error) {
        console.error('Check in error:', error);
        alert('Failed to check in');
    }
}

async function checkOut() {
    try {
        const response = await apiCall('/api/attendance/checkout', { method: 'POST' });
        
        if (response.ok) {
            showSuccess('Checked out successfully!');
            loadMyAttendance();
        } else {
            alert('Failed to check out');
        }
    } catch (error) {
        console.error('Check out error:', error);
        alert('Failed to check out');
    }
}

async function openEditAttendanceModal(id) {
    try {
        const response = await apiCall(`/api/attendance/${id}`);
        if (response.ok) {
            const att = await response.json();
            document.getElementById('editAttendanceId').value = att.id;
            document.getElementById('editCheckInTime').value = att.checkInTime || '';
            document.getElementById('editCheckOutTime').value = att.checkOutTime || '';
            document.getElementById('editAttendanceStatus').value = att.status || 'PRESENT';
            document.getElementById('editWorkingHours').value = att.workingHours || '';
            document.getElementById('editRemarks').value = att.remarks || '';
            
            document.getElementById('editAttendanceModal').style.display = 'flex';
        }
    } catch (error) {
        console.error('Error loading attendance for edit:', error);
    }
}

function closeEditAttendanceModal() {
    document.getElementById('editAttendanceModal').style.display = 'none';
}

async function updateAttendance(event) {
    event.preventDefault();
    
    const id = document.getElementById('editAttendanceId')?.value;
    if (!id) return;
    
    const data = {
        checkInTime: document.getElementById('editCheckInTime')?.value,
        checkOutTime: document.getElementById('editCheckOutTime')?.value,
        status: document.getElementById('editAttendanceStatus')?.value,
        workingHours: parseFloat(document.getElementById('editWorkingHours')?.value) || 0,
        remarks: document.getElementById('editRemarks')?.value
    };
    
    try {
        const response = await apiCall(`/api/attendance/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
        
        if (response.ok) {
            showSuccess('Attendance updated successfully');
            loadAttendanceData();
            closeEditAttendanceModal();
        } else {
            alert('Failed to update attendance');
        }
    } catch (error) {
        console.error('Update attendance error:', error);
    }
}

async function editAttendance(id) {
    openEditAttendanceModal(id);
}

async function deleteAttendance(id) {
    if (!confirm('Are you sure you want to delete this attendance record?')) return;
    
    try {
        const response = await apiCall(`/api/attendance/${id}`, { method: 'DELETE' });
        
        if (response.ok) {
            showSuccess('Attendance record deleted');
            loadAttendanceData();
        }
    } catch (error) {
        console.error('Delete attendance error:', error);
    }
}

function searchAttendance() {
    const searchTerm = document.getElementById('attendanceSearch')?.value.toLowerCase() || '';
    const dateFilter = document.getElementById('attendanceDateFilter')?.value;
    const statusFilter = document.getElementById('attendanceStatusFilter')?.value;
    
    const rows = document.querySelectorAll('#attendanceList tr');
    
    rows.forEach(row => {
        const text = row.textContent.toLowerCase();
        const matchesSearch = !searchTerm || text.includes(searchTerm);
        const matchesDate = !dateFilter || text.includes(dateFilter);
        const matchesStatus = !statusFilter || text.includes(statusFilter.toLowerCase());
        
        row.style.display = matchesSearch && matchesDate && matchesStatus ? '' : 'none';
    });
}

async function markAllPresent() {
    if (!confirm('Mark all employees as present for today?')) return;
    
    try {
        const response = await apiCall('/api/attendance/mark-all-present', { method: 'POST' });
        
        if (response.ok) {
            showSuccess('All employees marked as present');
            loadAttendanceData();
        } else {
            alert('Failed to mark attendance');
        }
    } catch (error) {
        console.error('Mark all present error:', error);
        alert('Failed to mark attendance');
    }
}

function exportAttendance() {
    const rows = document.querySelectorAll('#attendanceList tr');
    let csv = 'Employee ID,Name,Date,Check In,Check Out,Working Hours,Overtime,Status,Remarks\n';
    
    rows.forEach(row => {
        if (row.style.display !== 'none') {
            const cells = row.querySelectorAll('td');
            if (cells.length >= 9) {
                const rowData = Array.from(cells).slice(0, 9).map(cell => {
                    let text = cell.textContent.trim();
                    if (text.includes(',')) text = `"${text}"`;
                    return text;
                }).join(',');
                csv += rowData + '\n';
            }
        }
    });
    
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `attendance_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
    
    showSuccess('Attendance exported successfully');
}

// Exports
window.initAttendancePage = initAttendancePage;
window.loadAttendanceData = loadAttendanceData;
window.loadMyAttendance = loadMyAttendance;
window.markAttendance = markAttendance;
window.checkIn = checkIn;
window.checkOut = checkOut;
window.openEditAttendanceModal = openEditAttendanceModal;
window.closeEditAttendanceModal = closeEditAttendanceModal;
window.updateAttendance = updateAttendance;
window.editAttendance = editAttendance;
window.deleteAttendance = deleteAttendance;
window.searchAttendance = searchAttendance;
window.markAllPresent = markAllPresent;
window.exportAttendance = exportAttendance;

// Shift-related functions
window.loadShiftsForAttendance = loadShiftsForAttendance;
window.onShiftChange = onShiftChange;
window.openQuickAddShiftModal = openQuickAddShiftModal;
window.closeQuickAddShiftModal = closeQuickAddShiftModal;
window.saveQuickShift = saveQuickShift;

// Load shifts for the attendance page dropdown
async function loadShiftsForAttendance() {
    try {
        const response = await apiCall('/api/shifts/active');
        if (response.ok) {
            currentShifts = await response.json();
            const shiftSelect = document.getElementById('shiftSelect');
            if (shiftSelect) {
                // Keep the default option
                shiftSelect.innerHTML = '<option value="">⏰ Select Shift</option>';
                currentShifts.forEach(shift => {
                    const option = document.createElement('option');
                    option.value = shift.id;
                    option.textContent = `${shift.name} (${formatTime(shift.startTime)} - ${formatTime(shift.endTime)})`;
                    if (shift.isDefault) {
                        option.textContent += ' ⭐';
                        option.selected = true;
                        selectedShift = shift;
                    }
                    shiftSelect.appendChild(option);
                });
            }
        }
    } catch (error) {
        console.error('Error loading shifts:', error);
    }
}

// Handle shift selection change
function onShiftChange() {
    const shiftSelect = document.getElementById('shiftSelect');
    const shiftId = shiftSelect?.value;
    if (shiftId) {
        selectedShift = currentShifts.find(s => s.id == shiftId);
        console.log('Selected shift:', selectedShift);
        // Recalculate attendance based on selected shift
        recalculateAttendanceBasedOnShift();
    } else {
        selectedShift = null;
    }
}

// Recalculate attendance status based on selected shift times
function recalculateAttendanceBasedOnShift() {
    if (!selectedShift) return;
    
    const rows = document.querySelectorAll('#attendanceList tr');
    rows.forEach(row => {
        const checkInCell = row.cells[1]; // Check In column
        const statusCell = row.cells[6]; // Status column
        const checkInTime = checkInCell?.textContent?.trim();
        
        if (checkInTime && checkInTime !== '-' && checkInTime !== 'N/A') {
            const shiftStartTime = selectedShift.startTime; // HH:mm format
            const status = calculateStatusBasedOnShift(checkInTime, shiftStartTime);
            
            // Update status badge
            const badge = statusCell?.querySelector('.badge');
            if (badge) {
                badge.className = `badge ${getAttendanceStatusClass(status)}`;
                badge.textContent = status;
            }
        }
    });
}

// Calculate attendance status based on shift start time
function calculateStatusBasedOnShift(checkInTime, shiftStartTime) {
    // Parse times (checkInTime might be "10:00", shiftStartTime might be "10:00")
    const checkIn = parseTimeToMinutes(checkInTime);
    const shiftStart = parseTimeToMinutes(shiftStartTime);
    
    if (checkIn === null || shiftStart === null) {
        return 'PRESENT';
    }
    
    // If check-in is more than 15 minutes after shift start, mark as LATE
    if (checkIn > shiftStart + 15) {
        return 'LATE';
    } else if (checkIn > shiftStart) {
        return 'LATE';
    }
    return 'PRESENT';
}

// Parse time string (HH:mm) to minutes since midnight
function parseTimeToMinutes(timeStr) {
    if (!timeStr || timeStr === '-') return null;
    const parts = timeStr.split(':');
    if (parts.length >= 2) {
        const hours = parseInt(parts[0], 10);
        const minutes = parseInt(parts[1], 10);
        return hours * 60 + minutes;
    }
    return null;
}

// Open quick add shift modal
function openQuickAddShiftModal() {
    document.getElementById('quickAddShiftModal').style.display = 'flex';
    // Set default times
    document.getElementById('quickShiftStartTime').value = '10:00';
    document.getElementById('quickShiftEndTime').value = '18:30';
    document.getElementById('quickShiftWorkingHours').value = '8.5';
}

// Close quick add shift modal
function closeQuickAddShiftModal() {
    document.getElementById('quickAddShiftModal').style.display = 'none';
    document.getElementById('quickAddShiftForm').reset();
}

// Save quick shift from attendance page
async function saveQuickShift(event) {
    event.preventDefault();
    
    const shiftData = {
        name: document.getElementById('quickShiftName').value,
        startTime: document.getElementById('quickShiftStartTime').value,
        endTime: document.getElementById('quickShiftEndTime').value,
        workingHours: parseFloat(document.getElementById('quickShiftWorkingHours').value),
        description: document.getElementById('quickShiftDescription').value,
        isDefault: false,
        active: true
    };
    
    try {
        const response = await apiCall('/api/shifts', {
            method: 'POST',
            body: JSON.stringify(shiftData)
        });
        
        if (response.ok) {
            const newShift = await response.json();
            showSuccess('Shift created successfully!');
            closeQuickAddShiftModal();
            // Reload shifts and select the new one
            await loadShiftsForAttendance();
            // Select the newly created shift
            const shiftSelect = document.getElementById('shiftSelect');
            if (shiftSelect) {
                shiftSelect.value = newShift.id;
                selectedShift = newShift;
                recalculateAttendanceBasedOnShift();
            }
        } else {
            const error = await response.text();
            showError('Failed to create shift: ' + error);
        }
    } catch (error) {
        console.error('Error creating shift:', error);
        showError('Error creating shift');
    }
}
