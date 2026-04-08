// HRM System - Settings Module
// Corresponds to: NotificationPreference.java, User settings

async function handleSettingsSubmit(event) {
    event.preventDefault();
    
    const submitBtn = event.target.querySelector('button[type="submit"]');
    const currentPassword = document.getElementById('currentPassword')?.value;
    const newPassword = document.getElementById('newPassword')?.value;
    const confirmPassword = document.getElementById('confirmPassword')?.value;
    
    if (!currentPassword || !newPassword || !confirmPassword) {
        alert('Please fill all password fields');
        return;
    }
    
    if (newPassword !== confirmPassword) {
        alert('New passwords do not match');
        return;
    }
    
    if (newPassword.length < 6) {
        alert('Password must be at least 6 characters');
        return;
    }
    
    submitBtn.disabled = true;
    submitBtn.textContent = 'Changing...';
    
    try {
        const response = await apiCall('/api/auth/change-password', {
            method: 'POST',
            body: JSON.stringify({
                currentPassword,
                newPassword
            })
        });
        
        if (response.ok) {
            event.target.reset();
            showSuccess('Password changed successfully!');
        } else {
            const error = await response.text();
            alert('Failed to change password: ' + error);
        }
    } catch (error) {
        console.error('Change password error:', error);
        alert('Failed to change password');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Change Password';
    }
}

async function loadNotificationPreferences() {
    try {
        const response = await apiCall('/api/notifications/preferences');
        
        if (response.ok) {
            const prefs = await response.json();
            populateNotificationSettings(prefs);
        }
    } catch (error) {
        console.error('Error loading notification preferences:', error);
    }
}

function populateNotificationSettings(prefs) {
    document.getElementById('emailNotifications').checked = prefs.emailNotifications || false;
    document.getElementById('smsNotifications').checked = prefs.smsNotifications || false;
    document.getElementById('pushNotifications').checked = prefs.pushNotifications || false;
    document.getElementById('leaveUpdates').checked = prefs.leaveUpdates || false;
    document.getElementById('payrollUpdates').checked = prefs.payrollUpdates || false;
    document.getElementById('attendanceUpdates').checked = prefs.attendanceUpdates || false;
}

async function saveNotificationPreferences(event) {
    event.preventDefault();
    
    const prefs = {
        emailNotifications: document.getElementById('emailNotifications')?.checked || false,
        smsNotifications: document.getElementById('smsNotifications')?.checked || false,
        pushNotifications: document.getElementById('pushNotifications')?.checked || false,
        leaveUpdates: document.getElementById('leaveUpdates')?.checked || false,
        payrollUpdates: document.getElementById('payrollUpdates')?.checked || false,
        attendanceUpdates: document.getElementById('attendanceUpdates')?.checked || false
    };
    
    try {
        const response = await apiCall('/api/notifications/preferences', {
            method: 'PUT',
            body: JSON.stringify(prefs)
        });
        
        if (response.ok) {
            showSuccess('Notification preferences saved');
        } else {
            alert('Failed to save preferences');
        }
    } catch (error) {
        console.error('Save notification preferences error:', error);
    }
}

async function updateTheme(theme) {
    document.body.setAttribute('data-theme', theme);
    localStorage.setItem('theme', theme);
    
    try {
        await apiCall('/api/users/theme', {
            method: 'PUT',
            body: JSON.stringify({ theme })
        });
    } catch (error) {
        console.error('Update theme error:', error);
    }
}

function loadTheme() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.body.setAttribute('data-theme', savedTheme);
    
    const themeSelect = document.getElementById('themeSelect');
    if (themeSelect) themeSelect.value = savedTheme;
}

async function enable2FA() {
    try {
        const response = await apiCall('/api/auth/2fa/enable', { method: 'POST' });
        
        if (response.ok) {
            const data = await response.json();
            show2FASetupModal(data.qrCode, data.secret);
        }
    } catch (error) {
        console.error('Enable 2FA error:', error);
    }
}

function show2FASetupModal(qrCode, secret) {
    // Implementation for showing 2FA setup with QR code
    console.log('2FA Setup:', { qrCode, secret });
}

async function verify2FA(code) {
    try {
        const response = await apiCall('/api/auth/2fa/verify', {
            method: 'POST',
            body: JSON.stringify({ code })
        });
        
        return response.ok;
    } catch (error) {
        console.error('Verify 2FA error:', error);
        return false;
    }
}

async function disable2FA() {
    if (!confirm('Are you sure you want to disable 2FA?')) return;
    
    try {
        const response = await apiCall('/api/auth/2fa/disable', { method: 'POST' });
        
        if (response.ok) {
            showSuccess('2FA disabled successfully');
        }
    } catch (error) {
        console.error('Disable 2FA error:', error);
    }
}

// Initialize theme on load
document.addEventListener('DOMContentLoaded', loadTheme);

// Exports
window.handleSettingsSubmit = handleSettingsSubmit;
window.loadNotificationPreferences = loadNotificationPreferences;
window.saveNotificationPreferences = saveNotificationPreferences;
window.updateTheme = updateTheme;
window.loadTheme = loadTheme;
window.enable2FA = enable2FA;
window.verify2FA = verify2FA;
window.disable2FA = disable2FA;
