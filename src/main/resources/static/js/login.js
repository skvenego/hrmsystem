// Login functionality
const loginForm = document.getElementById('login-form');
const errorMessage = document.getElementById('error-message');
const submitBtn = document.getElementById('submit-btn');
const btnText = document.getElementById('btn-text');
const btnLoading = document.getElementById('btn-loading');
const togglePassword = document.getElementById('toggle-password');
const passwordInput = document.getElementById('password');
const eyeIcon = document.getElementById('eye-icon');
const eyeOffIcon = document.getElementById('eye-off-icon');

// Toggle password visibility
togglePassword.addEventListener('click', () => {
    const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
    passwordInput.setAttribute('type', type);
    
    if (type === 'text') {
        eyeIcon.style.display = 'none';
        eyeOffIcon.style.display = 'block';
    } else {
        eyeIcon.style.display = 'block';
        eyeOffIcon.style.display = 'none';
    }
});

// Handle form submission
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    
    // Clear previous errors
    hideError();
    
    // Show loading state
    setLoading(true);
    
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ username, password })
        });
        
        let data;
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
            data = await response.json();
        } else {
            const text = await response.text();
            data = { message: text || 'Login failed' };
        }
        
        if (!response.ok) {
            throw new Error(data.message || data.error || 'Login failed');
        }
        
        // Store auth data
        setAuth(data.token, {
            id: data.id,
            username: data.username,
            email: data.email,
            role: data.role,
            employeeId: data.employeeId,
            employeeName: data.employeeName
        });
        
        // Show success and redirect
        alert(data.message || 'Login successful!');
        window.location.href = '/html/dashboard.html';
        
    } catch (err) {
        showError(err.message || 'Invalid username or password. Please try again.');
    } finally {
        setLoading(false);
    }
});

// Show error message
function showError(message) {
    errorMessage.textContent = message;
    errorMessage.style.display = 'block';
}

// Hide error message
function hideError() {
    errorMessage.style.display = 'none';
    errorMessage.textContent = '';
}

// Set loading state
function setLoading(loading) {
    submitBtn.disabled = loading;
    if (loading) {
        btnText.style.display = 'none';
        btnLoading.style.display = 'flex';
    } else {
        btnText.style.display = 'block';
        btnLoading.style.display = 'none';
    }
}

// Store authentication data
function setAuth(token, userData) {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify(userData));
}

// Get authentication data
function getAuth() {
    const token = localStorage.getItem('token');
    const user = localStorage.getItem('user');
    return {
        token,
        user: user ? JSON.parse(user) : null
    };
}

// Check if user is authenticated
function isAuthenticated() {
    const { token } = getAuth();
    return !!token;
}

// Google login placeholder
function googleLogin() {
    alert('Google login coming soon!');
}

// Auto-fill credentials for test accounts
function fillCredentials(username, password) {
    document.getElementById('username').value = username;
    document.getElementById('password').value = password;
    hideError();
}

// Check if already logged in and redirect
if (isAuthenticated()) {
    window.location.href = '/html/dashboard.html';
}

// Export functions for use in other scripts
window.auth = {
    setAuth,
    getAuth,
    isAuthenticated
};
