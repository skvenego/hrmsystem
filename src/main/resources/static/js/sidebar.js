// Sidebar navigation component with RBAC

// Employee Sidebar (Limited access)
const employeeSidebar = `
    <div class="sidebar-header"><h2>HRM System</h2></div>
    <nav class="sidebar-nav">
        <a href="dashboard.html" class="nav-item" id="nav-dashboard">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7"></rect>
                <rect x="14" y="3" width="7" height="7"></rect>
                <rect x="14" y="14" width="7" height="7"></rect>
                <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
            Dashboard
        </a>
        <a href="leave-request.html" class="nav-item" id="nav-leave-request">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" x2="12" y1="5" y2="19"></line>
                <line x1="5" x2="19" y1="12" y2="12"></line>
            </svg>
            Apply Leave
        </a>
        <a href="my-leaves.html" class="nav-item" id="nav-my-leaves">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
            </svg>
            My Leaves
        </a>
        <a href="my-payslips.html" class="nav-item" id="nav-my-payslips">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" x2="8" y1="13" y2="13"></line>
            </svg>
            My Payslips
        </a>
        <a href="my-attendance.html" class="nav-item" id="nav-my-attendance">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="16" x2="16" y1="2" y2="6"></line>
                <line x1="8" x2="8" y1="2" y2="6"></line>
                <line x1="3" x2="21" y1="10" y2="10"></line>
            </svg>
            My Attendance
        </a>
        <a href="profile.html" class="nav-item" id="nav-profile">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
            </svg>
            My Profile
        </a>
    </nav>
    <div class="sidebar-footer">
        <div style="padding: 0 1rem 1rem; color: #9ca3af; font-size: 0.75rem; text-align: center;">
            Role: <span id="user-role-display">Employee</span>
        </div>
        <button onclick="auth.logout()" class="logout-btn">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" x2="9" y1="12" y2="12"></line>
            </svg>
            Logout
        </button>
    </div>
`;

// Management Sidebar (Full access)
const managementSidebar = `
    <div class="sidebar-header"><h2>HRM System</h2></div>
    <nav class="sidebar-nav">
        <a href="dashboard.html" class="nav-item" id="nav-dashboard">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7"></rect>
                <rect x="14" y="3" width="7" height="7"></rect>
                <rect x="14" y="14" width="7" height="7"></rect>
                <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
            Dashboard
        </a>
        <a href="employees.html" class="nav-item" id="nav-employees">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
            </svg>
            Employees
        </a>
        <a href="departments.html" class="nav-item" id="nav-departments">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 21h18"></path>
                <path d="M5 21V7l8-4 8 4v14"></path>
                <path d="M9 21v-6h6v6"></path>
            </svg>
            Departments
        </a>
        <a href="attendance.html" class="nav-item" id="nav-attendance">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
                <line x1="16" x2="16" y1="2" y2="6"></line>
                <line x1="8" x2="8" y1="2" y2="6"></line>
                <line x1="3" x2="21" y1="10" y2="10"></line>
            </svg>
            Attendance
        </a>
        <a href="leave.html" class="nav-item" id="nav-leave">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" x2="8" y1="13" y2="13"></line>
                <line x1="16" x2="8" y1="17" y2="17"></line>
            </svg>
            Leave Management
        </a>
        <a href="payroll.html" class="nav-item" id="nav-payroll">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" x2="12" y1="2" y2="22"></line>
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
            </svg>
            Payroll
        </a>
        <a href="payslips.html" class="nav-item" id="nav-payslips">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" x2="8" y1="13" y2="13"></line>
                <line x1="16" x2="8" y1="17" y2="17"></line>
            </svg>
            Payslips
        </a>
        <a href="profile.html" class="nav-item" id="nav-profile">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
            </svg>
            Profile
        </a>
        <a href="settings.html" class="nav-item" id="nav-settings">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="3"></circle>
                <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
            </svg>
            Settings
        </a>
    </nav>
    <div class="sidebar-footer">
        <div style="padding: 0 1rem 1rem; color: #9ca3af; font-size: 0.75rem; text-align: center;">
            Role: <span id="user-role-display">Admin</span>
        </div>
        <button onclick="auth.logout()" class="logout-btn">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" x2="9" y1="12" y2="12"></line>
            </svg>
            Logout
        </button>
    </div>
`;

// Accountant Sidebar
const accountantSidebar = `
    <div class="sidebar-header"><h2>HRM System</h2></div>
    <nav class="sidebar-nav">
        <a href="dashboard.html" class="nav-item" id="nav-dashboard">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7"></rect>
                <rect x="14" y="3" width="7" height="7"></rect>
                <rect x="14" y="14" width="7" height="7"></rect>
                <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
            Dashboard
        </a>
        <a href="payroll.html" class="nav-item" id="nav-payroll">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" x2="12" y1="2" y2="22"></line>
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
            </svg>
            Payroll
        </a>
        <a href="accountant-approvals.html" class="nav-item" id="nav-approvals">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <path d="m9 15 2 2 4-4"></path>
            </svg>
            My Approvals
        </a>
        <a href="payslips.html" class="nav-item" id="nav-payslips">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
                <line x1="16" x2="8" y1="13" y2="13"></line>
            </svg>
            Payslips
        </a>
        <a href="profile.html" class="nav-item" id="nav-profile">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
            </svg>
            Profile
        </a>
    </nav>
    <div class="sidebar-footer">
        <div style="padding: 0 1rem 1rem; color: #9ca3af; font-size: 0.75rem; text-align: center;">
            Role: <span id="user-role-display">Accountant</span>
        </div>
        <button onclick="auth.logout()" class="logout-btn">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" x2="9" y1="12" y2="12"></line>
            </svg>
            Logout
        </button>
    </div>
`;

// Director Sidebar
const directorSidebar = `
    <div class="sidebar-header"><h2>HRM System</h2></div>
    <nav class="sidebar-nav">
        <a href="dashboard.html" class="nav-item" id="nav-dashboard">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="7" height="7"></rect>
                <rect x="14" y="3" width="7" height="7"></rect>
                <rect x="14" y="14" width="7" height="7"></rect>
                <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
            Dashboard
        </a>
        <a href="employees.html" class="nav-item" id="nav-employees">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
            </svg>
            Employees
        </a>
        <a href="payroll.html" class="nav-item" id="nav-payroll">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" x2="12" y1="2" y2="22"></line>
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"></path>
            </svg>
            Payroll
        </a>
        <a href="director-approvals.html" class="nav-item" id="nav-approvals">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"></path>
            </svg>
            Final Approvals
        </a>
        <a href="payslips.html" class="nav-item" id="nav-payslips">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                <polyline points="14 2 14 8 20 8"></polyline>
            </svg>
            Payslips
        </a>
        <a href="profile.html" class="nav-item" id="nav-profile">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
            </svg>
            Profile
        </a>
    </nav>
    <div class="sidebar-footer">
        <div style="padding: 0 1rem 1rem; color: #9ca3af; font-size: 0.75rem; text-align: center;">
            Role: <span id="user-role-display">Director</span>
        </div>
        <button onclick="auth.logout()" class="logout-btn">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
                <polyline points="16 17 21 12 16 7"></polyline>
                <line x1="21" x2="9" y1="12" y2="12"></line>
            </svg>
            Logout
        </button>
    </div>
`;

// Render sidebar based on role
function renderSidebar() {
    const sidebar = document.querySelector('.sidebar');
    if (!sidebar) return;
    
    const role = auth.getUserRole();
    console.log('Current user role:', role);
    console.log('Is management:', auth.isManagement());
    
    if (role === 'ROLE_ACCOUNTANT') {
        sidebar.innerHTML = accountantSidebar;
    } else if (role === 'ROLE_DIRECTOR') {
        sidebar.innerHTML = directorSidebar;
    } else if (auth.isManagement()) {
        sidebar.innerHTML = managementSidebar;
    } else {
        sidebar.innerHTML = employeeSidebar;
    }
    
    // Update role display
    const roleDisplay = document.getElementById('user-role-display');
    if (roleDisplay) {
        roleDisplay.textContent = auth.getRoleDisplayName();
    }
    
    // Highlight current page
    const currentPage = window.location.pathname.split('/').pop().replace('.html', '');
    const navItem = document.getElementById(`nav-${currentPage}`);
    if (navItem) {
        navItem.classList.add('active');
    }
}

// Check page access
function checkPageAccess() {
    const role = auth.getUserRole();
    // Extract filename from URL (handles both /html/page.html and /page.html)
    const pathParts = window.location.pathname.split('/');
    const currentPage = pathParts[pathParts.length - 1] || pathParts[pathParts.length - 2] || 'dashboard.html';
    
    // Pages restricted to management only
    const managementOnlyPages = [
        'employees.html', 'departments.html', 'attendance.html', 
        'leave.html', 'payroll.html', 'payslips.html', 'settings.html'
    ];
    
    // Pages restricted to specific roles
    const accountantPages = ['accountant-approvals.html'];
    const directorPages = ['director-approvals.html'];
    
    // Employee-only pages (management can also access some)
    const employeePages = ['leave-request.html', 'my-leaves.html', 'my-payslips.html', 'my-attendance.html'];
    
    // Check if management page and user is employee
    if (managementOnlyPages.includes(currentPage) && role === 'ROLE_EMPLOYEE') {
        auth.showNotification('Access denied: Insufficient permissions', 'error');
        window.location.href = '/html/dashboard.html';
        return false;
    }
    
    // Check accountant pages
    if (accountantPages.includes(currentPage) && !auth.isAccountant()) {
        auth.showNotification('Access denied: Accountant role required', 'error');
        window.location.href = '/html/dashboard.html';
        return false;
    }
    
    // Check director pages
    if (directorPages.includes(currentPage) && !auth.isDirector()) {
        auth.showNotification('Access denied: Director role required', 'error');
        window.location.href = '/html/dashboard.html';
        return false;
    }
    
    return true;
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    if (auth.isAuthenticated()) {
        renderSidebar();
        checkPageAccess();
    }
});

window.sidebar = {
    renderSidebar,
    checkPageAccess
};
