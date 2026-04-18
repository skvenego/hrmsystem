// Sidebar navigation component with RBAC

// Employee Sidebar (Limited access)
const employeeSidebar = `
  <div class="sidebar-header">
    <div class="brand">
      <div class="brand-icon">H</div>
      <div>
        <div class="brand-text">HRM System</div>
        <div class="brand-tagline">Employee Portal</div>
      </div>
    </div>
  </div>

  <div class="user-profile">
    <div class="user-avatar" id="user-avatar">U</div>
    <div class="user-info-sidebar">
      <div class="user-name" id="sidebar-user-name">Loading...</div>
      <div class="user-role">
        <span class="role-badge" id="sidebar-user-role">EMPLOYEE</span>
      </div>
    </div>
  </div>

  <div class="nav-section">
    <div class="nav-title">Main Menu</div>
    <ul class="nav-list">
      <li class="nav-item">
        <a href="dashboard.html" id="nav-dashboard">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/>
          </svg>
          Dashboard
        </a>
      </li>
      <li class="nav-item">
        <a href="leave-request.html" id="nav-leave-request">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          Apply Leave
        </a>
      </li>
      <li class="nav-item">
        <a href="my-leaves.html" id="nav-my-leaves">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
          </svg>
          My Leaves
        </a>
      </li>
      <li class="nav-item">
        <a href="my-payslips.html" id="nav-my-payslips">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          My Payslips
        </a>
      </li>
      <li class="nav-item">
        <a href="my-attendance.html" id="nav-my-attendance">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          My Attendance
        </a>
      </li>
      <li class="nav-item">
        <a href="profile.html" id="nav-profile">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
          </svg>
          My Profile
        </a>
      </li>
    </ul>

    <div class="nav-title" style="margin-top: 1.5rem;">Preferences</div>
    <ul class="nav-list">
      <li class="nav-item">
        <a href="settings.html" id="nav-settings">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
          </svg>
          Settings
        </a>
      </li>
    </ul>
  </div>

  <div class="sidebar-footer">
    <button class="logout-btn" onclick="auth.logout()">
      <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
      </svg>
      Logout
    </button>
  </div>
`;

// Management Sidebar (Full access)
const managementSidebar = `
  <div class="sidebar-header">
    <div class="brand">
      <div class="brand-icon">H</div>
      <div>
        <div class="brand-text">HRM System</div>
        <div class="brand-tagline">Admin Portal</div>
      </div>
    </div>
  </div>

  <div class="user-profile">
    <div class="user-avatar" id="user-avatar">U</div>
    <div class="user-info-sidebar">
      <div class="user-name" id="sidebar-user-name">Loading...</div>
      <div class="user-role">
        <span class="role-badge" id="sidebar-user-role">ADMIN</span>
      </div>
    </div>
  </div>

  <div class="nav-section">
    <div class="nav-title">Main Menu</div>
    <ul class="nav-list">
      <li class="nav-item">
        <a href="dashboard.html" id="nav-dashboard">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/>
          </svg>
          Dashboard
        </a>
      </li>
      <li class="nav-item">
        <a href="employees.html" id="nav-employees">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>
          </svg>
          Employees
        </a>
      </li>
      <li class="nav-item">
        <a href="departments.html" id="nav-departments">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 21h18M5 21V7l8-4 8 4v14M9 21v-6h6v6"/>
          </svg>
          Departments
        </a>
      </li>
      <li class="nav-item">
        <a href="attendance.html" id="nav-attendance">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/>
            <line x1="16" y1="2" x2="16" y2="6" stroke="currentColor" stroke-width="2"/>
            <line x1="8" y1="2" x2="8" y2="6" stroke="currentColor" stroke-width="2"/>
            <line x1="3" y1="10" x2="21" y2="10" stroke="currentColor" stroke-width="2"/>
          </svg>
          Attendance
        </a>
      </li>
      <li class="nav-item">
        <a href="leave.html" id="nav-leave">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
            <polyline points="14 2 14 8 20 8" stroke="currentColor" stroke-width="2"/>
            <line x1="16" y1="13" x2="8" y2="13" stroke="currentColor" stroke-width="2"/>
            <line x1="16" y1="17" x2="8" y2="17" stroke="currentColor" stroke-width="2"/>
          </svg>
          Leave Management
        </a>
      </li>
      <li class="nav-item">
        <a href="payroll.html" id="nav-payroll">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <line x1="12" y1="2" x2="12" y2="22" stroke="currentColor" stroke-width="2"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
          </svg>
          Payroll
        </a>
      </li>
      <li class="nav-item">
        <a href="payslips.html" id="nav-payslips">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
            <polyline points="14 2 14 8 20 8" stroke="currentColor" stroke-width="2"/>
            <line x1="16" y1="13" x2="8" y2="13" stroke="currentColor" stroke-width="2"/>
            <line x1="16" y1="17" x2="8" y2="17" stroke="currentColor" stroke-width="2"/>
          </svg>
          Payslips
        </a>
      </li>
      <li class="nav-item">
        <a href="profile.html" id="nav-profile">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
            <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
          </svg>
          My Profile
        </a>
      </li>
    </ul>

    <div class="nav-title" style="margin-top: 1.5rem;">System</div>
    <ul class="nav-list">
      <li class="nav-item">
        <a href="settings.html" id="nav-settings">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
          </svg>
          Settings
        </a>
      </li>
    </ul>
  </div>

  <div class="sidebar-footer">
    <button class="logout-btn" onclick="auth.logout()">
      <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
      </svg>
      Logout
    </button>
  </div>
`;

// Accountant Sidebar
const accountantSidebar = `
  <div class="sidebar-header">
    <div class="brand">
      <div class="brand-icon">H</div>
      <div>
        <div class="brand-text">HRM System</div>
        <div class="brand-tagline">Accountant Portal</div>
      </div>
    </div>
  </div>

  <div class="user-profile">
    <div class="user-avatar" id="user-avatar">U</div>
    <div class="user-info-sidebar">
      <div class="user-name" id="sidebar-user-name">Loading...</div>
      <div class="user-role">
        <span class="role-badge" id="sidebar-user-role">ACCOUNTANT</span>
      </div>
    </div>
  </div>

  <div class="nav-section">
    <div class="nav-title">Main Menu</div>
    <ul class="nav-list">
      <li class="nav-item">
        <a href="dashboard.html" id="nav-dashboard">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/>
          </svg>
          Dashboard
        </a>
      </li>
      <li class="nav-item">
        <a href="payroll.html" id="nav-payroll">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <line x1="12" y1="2" x2="12" y2="22" stroke="currentColor" stroke-width="2"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
          </svg>
          Payroll
        </a>
      </li>
      <li class="nav-item">
        <a href="accountant-approvals.html" id="nav-approvals">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
            <polyline points="14 2 14 8 20 8" stroke="currentColor" stroke-width="2"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 15l2 2 4-4"/>
          </svg>
          My Approvals
        </a>
      </li>
      <li class="nav-item">
        <a href="payslips.html" id="nav-payslips">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
            <polyline points="14 2 14 8 20 8" stroke="currentColor" stroke-width="2"/>
            <line x1="16" y1="13" x2="8" y2="13" stroke="currentColor" stroke-width="2"/>
          </svg>
          Payslips
        </a>
      </li>
      <li class="nav-item">
        <a href="profile.html" id="nav-profile">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
            <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
          </svg>
          My Profile
        </a>
      </li>
    </ul>
  </div>

  <div class="sidebar-footer">
    <button class="logout-btn" onclick="auth.logout()">
      <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
      </svg>
      Logout
    </button>
  </div>
`;

// Director Sidebar
const directorSidebar = `
  <div class="sidebar-header">
    <div class="brand">
      <div class="brand-icon">H</div>
      <div>
        <div class="brand-text">HRM System</div>
        <div class="brand-tagline">Director Portal</div>
      </div>
    </div>
  </div>

  <div class="user-profile">
    <div class="user-avatar" id="user-avatar">U</div>
    <div class="user-info-sidebar">
      <div class="user-name" id="sidebar-user-name">Loading...</div>
      <div class="user-role">
        <span class="role-badge" id="sidebar-user-role">DIRECTOR</span>
      </div>
    </div>
  </div>

  <div class="nav-section">
    <div class="nav-title">Main Menu</div>
    <ul class="nav-list">
      <li class="nav-item">
        <a href="dashboard.html" id="nav-dashboard">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/>
          </svg>
          Dashboard
        </a>
      </li>
      <li class="nav-item">
        <a href="employees.html" id="nav-employees">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
            <circle cx="9" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
          </svg>
          Employees
        </a>
      </li>
      <li class="nav-item">
        <a href="payroll.html" id="nav-payroll">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <line x1="12" y1="2" x2="12" y2="22" stroke="currentColor" stroke-width="2"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/>
          </svg>
          Payroll
        </a>
      </li>
      <li class="nav-item">
        <a href="director-approvals.html" id="nav-approvals">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
          </svg>
          Final Approvals
        </a>
      </li>
      <li class="nav-item">
        <a href="payslips.html" id="nav-payslips">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
            <polyline points="14 2 14 8 20 8" stroke="currentColor" stroke-width="2"/>
          </svg>
          Payslips
        </a>
      </li>
      <li class="nav-item">
        <a href="profile.html" id="nav-profile">
          <svg class="nav-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
            <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
          </svg>
          My Profile
        </a>
      </li>
    </ul>
  </div>

  <div class="sidebar-footer">
    <button class="logout-btn" onclick="auth.logout()">
      <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
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
    
    // Show sidebar based on exact role
    if (role === 'ROLE_ACCOUNTANT') {
        sidebar.innerHTML = accountantSidebar;
    } else if (role === 'ROLE_DIRECTOR') {
        sidebar.innerHTML = directorSidebar;
    } else if (role === 'ROLE_ADMIN' || role === 'ROLE_HR') {
        sidebar.innerHTML = managementSidebar;
    } else {
        sidebar.innerHTML = employeeSidebar;
    }
    
    // Update user info in sidebar
    const user = auth.getCurrentUser();
    if (user) {
        const sidebarUserName = document.getElementById('sidebar-user-name');
        const userAvatar = document.getElementById('user-avatar');
        const sidebarUserRole = document.getElementById('sidebar-user-role');
        
        if (sidebarUserName) {
            sidebarUserName.textContent = user.name || user.username || 'User';
        }
        if (userAvatar) {
            userAvatar.textContent = (user.name || user.username || 'U').charAt(0).toUpperCase();
        }
        if (sidebarUserRole) {
            sidebarUserRole.textContent = auth.getRoleDisplayName() || 'EMPLOYEE';
        }
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
    
    // Pages restricted by role
    const hrAdminPages = ['employees.html', 'departments.html', 'leave.html'];
    const hrAdminManagerPages = ['attendance.html']; // Admin, HR, Manager
    const payrollPages = ['payroll.html', 'payslips.html']; // Admin, HR, Accountant
    const adminOnlyPages = [];  // Settings page is accessible by all authenticated users
    
    // Pages restricted to specific roles
    const accountantPages = ['accountant-approvals.html'];
    const directorPages = ['director-approvals.html'];
    
    // Employee-only pages (management can also access some)
    const employeePages = ['leave-request.html', 'my-leaves.html', 'my-payslips.html', 'my-attendance.html'];
    
    // Check HR/Admin pages
    if (hrAdminPages.includes(currentPage) && !['ROLE_ADMIN', 'ROLE_HR'].includes(role)) {
        auth.showNotification('Access denied: HR/Admin role required', 'error');
        window.location.href = '/html/dashboard.html';
        return false;
    }
    
    // Check Attendance pages (Admin, HR, Manager)
    if (hrAdminManagerPages.includes(currentPage) && !['ROLE_ADMIN', 'ROLE_HR', 'ROLE_MANAGER'].includes(role)) {
        auth.showNotification('Access denied: Manager role required', 'error');
        window.location.href = '/html/dashboard.html';
        return false;
    }
    
    // Check Payroll pages (Admin, HR, Accountant)
    if (payrollPages.includes(currentPage) && !['ROLE_ADMIN', 'ROLE_HR', 'ROLE_ACCOUNTANT'].includes(role)) {
        auth.showNotification('Access denied: Payroll access required', 'error');
        window.location.href = '/html/dashboard.html';
        return false;
    }
    
    // Check Admin-only pages
    if (adminOnlyPages.includes(currentPage) && role !== 'ROLE_ADMIN') {
        auth.showNotification('Access denied: Admin only', 'error');
        window.location.href = '/html/dashboard.html';
        return false;
    }
    
    // Check accountant pages
    if (accountantPages.includes(currentPage) && role !== 'ROLE_ACCOUNTANT') {
        auth.showNotification('Access denied: Accountant role required', 'error');
        window.location.href = '/html/dashboard.html';
        return false;
    }
    
    // Check director pages
    if (directorPages.includes(currentPage) && role !== 'ROLE_DIRECTOR') {
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
