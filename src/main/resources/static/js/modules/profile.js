// HRM System - Profile Module
// Corresponds to: User.java, Employee.java profile endpoints

async function loadProfileData() {
    if (!currentUser) return;
    
    try {
        // Try to fetch full profile from API - User.java endpoint
        const response = await apiCall(`/api/users/${currentUser.id}`);
        
        if (response.ok) {
            const profile = await response.json();
            populateProfileForm(profile);
        } else {
            // Fallback to stored user data
            populateProfileFormFromStorage();
        }
    } catch (error) {
        console.error('Error loading profile:', error);
        populateProfileFormFromStorage();
    }
}

function populateProfileForm(profile) {
    // User.java fields: username, email, phone, address, role
    const usernameField = document.getElementById('profileUsername');
    if (usernameField) usernameField.value = profile.username || currentUser?.username || '';
    
    const emailField = document.getElementById('profileEmail');
    if (emailField) emailField.value = profile.email || '';
    
    const phoneField = document.getElementById('profilePhone');
    if (phoneField) phoneField.value = profile.phone || '';
    
    const addressField = document.getElementById('profileAddress');
    if (addressField) addressField.value = profile.address || '';
    
    const roleField = document.getElementById('profileRole');
    if (roleField) roleField.value = profile.role || currentUser?.role || '';
}

function populateProfileFormFromStorage() {
    if (!currentUser) return;
    
    const usernameField = document.getElementById('profileUsername');
    if (usernameField) usernameField.value = currentUser.username || '';
    
    const emailField = document.getElementById('profileEmail');
    if (emailField) emailField.value = currentUser.email || '';
    
    const phoneField = document.getElementById('profilePhone');
    if (phoneField) phoneField.value = currentUser.phone || '';
    
    const roleField = document.getElementById('profileRole');
    if (roleField) roleField.value = currentUser.role || '';
}

async function handleProfileSubmit(event) {
    event.preventDefault();
    
    const submitBtn = event.target.querySelector('button[type="submit"]');
    submitBtn.disabled = true;
    submitBtn.textContent = '💾 Updating...';
    
    try {
        // User.java fields: username, email, phone, address
        const profileData = {
            email: document.getElementById('profileEmail')?.value.trim(),
            phone: document.getElementById('profilePhone')?.value,
            address: document.getElementById('profileAddress')?.value
        };
        
        const response = await apiCall(`/api/users/${currentUser?.id}`, {
            method: 'PUT',
            body: JSON.stringify(profileData)
        });
        
        if (response.ok) {
            // Update local user data
            currentUser = {
                ...currentUser,
                email: profileData.email,
                phone: profileData.phone,
                address: profileData.address
            };
            localStorage.setItem('user', JSON.stringify(currentUser));
            
            showSuccess('Profile updated successfully!');
            updateUserInfo();
        } else {
            const error = await response.text();
            alert('Failed to update profile: ' + error);
        }
    } catch (error) {
        console.error('Profile update error:', error);
        alert('Failed to update profile');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = '💾 Update Profile';
    }
}

async function changePassword(event) {
    event.preventDefault();
    
    const currentPassword = document.getElementById('currentPassword')?.value;
    const newPassword = document.getElementById('newPassword')?.value;
    const confirmPassword = document.getElementById('confirmPassword')?.value;
    
    if (!currentPassword || !newPassword || !confirmPassword) {
        alert('Please fill all password fields');
        return;
    }
    
    if (newPassword !== confirmPassword) {
        alert('New password and confirm password do not match');
        return;
    }
    
    try {
        const response = await apiCall('/api/users/change-password', {
            method: 'POST',
            body: JSON.stringify({
                currentPassword: currentPassword,
                newPassword: newPassword
            })
        });
        
        if (response.ok) {
            showSuccess('Password changed successfully!');
            document.getElementById('passwordForm')?.reset();
        } else {
            const error = await response.text();
            alert('Failed to change password: ' + error);
        }
    } catch (error) {
        console.error('Change password error:', error);
        alert('Failed to change password');
    }
}

async function uploadProfilePicture(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    const formData = new FormData();
    formData.append('file', file);
    
    try {
        const response = await fetch('/api/profile/picture', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: formData
        });
        
        if (response.ok) {
            showSuccess('Profile picture updated');
            // Refresh profile to show new picture
            loadProfileData();
        } else {
            alert('Failed to upload profile picture');
        }
    } catch (error) {
        console.error('Upload profile picture error:', error);
    }
}

async function updateProfilePicture(file) {
    const formData = new FormData();
    formData.append('profilePicture', file);
    
    try {
        const response = await fetch('/api/employees/profile-picture', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            },
            body: formData
        });
        
        if (response.ok) {
            showSuccess('Profile picture updated successfully');
        } else {
            alert('Failed to update profile picture');
        }
    } catch (error) {
        console.error('Update profile picture error:', error);
    }
}

// Exports
window.loadProfileData = loadProfileData;
window.populateProfileForm = populateProfileForm;
window.populateProfileFormFromStorage = populateProfileFormFromStorage;
window.handleProfileSubmit = handleProfileSubmit;
window.changePassword = changePassword;
window.uploadProfilePicture = uploadProfilePicture;
window.updateProfilePicture = updateProfilePicture;
