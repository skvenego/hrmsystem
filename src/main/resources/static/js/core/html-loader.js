// HRM System - HTML Component Loader
// Dynamically loads HTML modules into the page

const HTMLModules = {
    loaded: new Set(),
    
    async load(moduleName, targetId) {
        const target = document.getElementById(targetId);
        if (!target) return;
        
        // Skip if already loaded
        if (this.loaded.has(moduleName)) return;
        
        try {
            const response = await fetch(`html/modules/${moduleName}.html`);
            if (response.ok) {
                const html = await response.text();
                target.innerHTML = html;
                this.loaded.add(moduleName);
                
                // Re-attach event listeners for forms
                this.reattachEventListeners(target);
            }
        } catch (error) {
            console.error(`Failed to load HTML module: ${moduleName}`, error);
        }
    },
    
    reattachEventListeners(container) {
        // Re-attach form submit listeners
        const forms = container.querySelectorAll('form');
        forms.forEach(form => {
            const id = form.id;
            if (id === 'loginForm') form.addEventListener('submit', handleLogin);
            else if (id === 'leaveForm') form.addEventListener('submit', handleLeaveSubmit);
            else if (id === 'profileForm') form.addEventListener('submit', handleProfileSubmit);
            else if (id === 'settingsForm') form.addEventListener('submit', handleSettingsSubmit);
            else if (id === 'addEmployeeForm') form.addEventListener('submit', addEmployee);
        });
    },
    
    // Load all modules on init
    async loadAll() {
        const modules = [
            { name: 'dashboard', target: 'dashboardSection' },
            { name: 'leave', target: 'leaveSection' },
            { name: 'payslips', target: 'payslipsSection' },
            { name: 'profile', target: 'profileSection' },
            { name: 'documents', target: 'documentsSection' },
            { name: 'settings', target: 'settingsSection' },
            { name: 'employees', target: 'employeesSection' },
            { name: 'departments', target: 'departmentsSection' },
            { name: 'attendance', target: 'attendanceSection' },
            { name: 'leaveManagement', target: 'leaveManagementSection' },
            { name: 'payroll', target: 'payrollSection' },
            { name: 'adminPayslips', target: 'adminPayslipsSection' },
            { name: 'shift', target: 'shiftSection' }
        ];
        
        for (const mod of modules) {
            await this.load(mod.name, mod.target);
        }
    }
};

window.HTMLModules = HTMLModules;
