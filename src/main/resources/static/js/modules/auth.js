// HRM System - Auth Module
// Corresponds to: AuthController.java, AuthService.java, User.java

async function handleLogin(event) {
    event.preventDefault();
    
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;
    const errorDiv = document.getElementById('error');
    const submitBtn = event.target.querySelector('button[type="submit"]');
    
    if (!username || !password) {
        showError('Please enter username and password');
        return;
    }
    
    submitBtn.disabled = true;
    submitBtn.textContent = 'Logging in...';
    errorDiv.style.display = 'none';
    
    try {
        await simulateLogin(username, password);
    } catch (error) {
        showError('Login failed. Please try again.');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Login';
    }
}

async function simulateLogin(username, password) {
    await new Promise(resolve => setTimeout(resolve, 1000));
    
    const testUsers = {
        'sachin': { password: 'password123', role: 'EMPLOYEE', name: 'Sachin Employee' },
        'hr': { password: 'password123', role: 'HR', name: 'HR Manager' },
        'admin': { password: 'password123', role: 'ADMIN', name: 'Administrator' },
        'director': { password: 'password123', role: 'DIRECTOR', name: 'Director' },
        'accountant': { password: 'password123', role: 'ACCOUNTANT', name: 'Accountant' }
    };
    
    const user = testUsers[username.toLowerCase()];
    
    if (!user || user.password !== password) {
        throw new Error('Invalid credentials');
    }
    
    currentUser = {
        id: 1,
        username: username,
        name: user.name,
        role: user.role,
        email: `${username}@company.com`,
        phone: '9876543210'
    };
    
    localStorage.setItem('token', 'mock-token-' + Date.now());
    localStorage.setItem('user', JSON.stringify(currentUser));
    
    showSuccess('Login successful! Redirecting...');
    setTimeout(() => {
        showDashboard();
        updateUserInfo();
    }, 1000);
}

async function loginWithAPI(username, password) {
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        
        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('token', data.token);
            localStorage.setItem('user', JSON.stringify(data.user));
            currentUser = data.user;
            showDashboard();
            updateUserInfo();
        } else {
            const error = await response.json();
            showError(error.message || 'Invalid credentials');
        }
    } catch (error) {
        console.error('Login API error:', error);
        showError('Login service unavailable');
    }
}

async function registerUser(userData) {
    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(userData)
        });
        
        return await response.json();
    } catch (error) {
        console.error('Registration error:', error);
        throw error;
    }
}

async function changePassword(currentPassword, newPassword) {
    try {
        const response = await apiCall('/api/auth/change-password', {
            method: 'POST',
            body: JSON.stringify({ currentPassword, newPassword })
        });
        
        return response.ok;
    } catch (error) {
        console.error('Change password error:', error);
        return false;
    }
}

async function forgotPassword(email) {
    try {
        const response = await fetch('/api/auth/forgot-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email })
        });
        
        return response.ok;
    } catch (error) {
        console.error('Forgot password error:', error);
        return false;
    }
}

// Exports
window.handleLogin = handleLogin;
window.loginWithAPI = loginWithAPI;
window.registerUser = registerUser;
window.changePassword = changePassword;
window.forgotPassword = forgotPassword;
