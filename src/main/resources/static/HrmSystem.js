// HRM System - Main Application
// Similar to your HrmsystemApplication.java

class HrmSystem {
    constructor() {
        this.version = APP_CONFIG.VERSION;
        this.isInitialized = false;
        this.modules = {};
        this.pages = {};
        this.init();
    }

    async init() {
        try {
            console.log(`HRM System v${this.version} - Initializing...`);
            
            // Wait for DOM to be ready
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', () => this.onDOMReady());
            } else {
                this.onDOMReady();
            }
        } catch (error) {
            console.error('Failed to initialize HRM System:', error);
            this.showCriticalError('Failed to initialize application');
        }
    }

    onDOMReady() {
        console.log('DOM ready - Starting application initialization...');
        
        try {
            // Initialize services
            this.initializeServices();
            
            // Initialize pages
            this.initializePages();
            
            // Setup global event listeners
            this.setupGlobalEventListeners();
            
            // Setup error handling
            this.setupErrorHandling();
            
            // Check authentication status
            this.checkAuthStatus();
            
            // Show application ready
            this.onApplicationReady();
            
            this.isInitialized = true;
            console.log('HRM System initialized successfully');
            
        } catch (error) {
            console.error('Application initialization failed:', error);
            this.showCriticalError('Application initialization failed');
        }
    }

    initializeServices() {
        // Initialize services in dependency order
        this.modules = {
            authService: new AuthService(),
            apiService: new ApiService()
        };

        // Make services globally available
        window.authService = this.modules.authService;
        window.apiService = this.modules.apiService;

        // Verify critical services
        const criticalServices = ['authService'];
        const missingServices = criticalServices.filter(name => !this.modules[name]);
        
        if (missingServices.length > 0) {
            throw new Error(`Missing critical services: ${missingServices.join(', ')}`);
        }
    }

    initializePages() {
        // Initialize pages
        this.pages = {
            login: new LoginPage(),
            dashboard: new DashboardPage()
        };

        // Make pages globally available
        window.loginPage = this.pages.login;
        window.dashboardPage = this.pages.dashboard;
        window.dashboardManager = this.pages.dashboard; // For backward compatibility

        // Verify critical pages
        const criticalPages = ['login', 'dashboard'];
        const missingPages = criticalPages.filter(name => !this.pages[name]);
        
        if (missingPages.length > 0) {
            throw new Error(`Missing critical pages: ${missingPages.join(', ')}`);
        }
    }

    setupGlobalEventListeners() {
        // Handle network connectivity
        window.addEventListener('online', () => {
            this.showNetworkStatus('online');
            console.log('Network connection restored');
        });

        window.addEventListener('offline', () => {
            this.showNetworkStatus('offline');
            console.log('Network connection lost');
        });

        // Handle browser back/forward
        window.addEventListener('popstate', (event) => {
            this.handleNavigation(event);
        });

        // Handle visibility change (tab switching)
        document.addEventListener('visibilitychange', () => {
            this.handleVisibilityChange();
        });

        // Handle window resize
        window.addEventListener('resize', this.debounce(() => {
            this.handleResize();
        }, 250));

        // Handle keyboard shortcuts
        document.addEventListener('keydown', (event) => {
            this.handleKeyboardShortcuts(event);
        });

        // Handle form submissions globally
        document.addEventListener('submit', (event) => {
            this.handleFormSubmit(event);
        }, true);

        // Handle click events globally
        document.addEventListener('click', (event) => {
            this.handleGlobalClick(event);
        }, true);
    }

    setupErrorHandling() {
        // Global error handler
        window.addEventListener('error', (event) => {
            console.error('Global error:', event.error);
            this.logError('Global Error', {
                message: event.error.message,
                stack: event.error.stack,
                filename: event.filename,
                lineno: event.lineno,
                colno: event.colno
            });
        });

        // Unhandled promise rejection handler
        window.addEventListener('unhandledrejection', (event) => {
            console.error('Unhandled promise rejection:', event.reason);
            this.logError('Unhandled Promise Rejection', {
                reason: event.reason
            });
            event.preventDefault();
        });
    }

    checkAuthStatus() {
        if (this.modules.authService && this.modules.authService.isAuthenticated) {
            this.showDashboard();
            this.pages.dashboard.updateUserInfo();
        } else {
            this.showLogin();
        }
    }

    onApplicationReady() {
        // Hide loading spinner if present
        const loadingElements = document.querySelectorAll('.loading-spinner, .app-loading');
        loadingElements.forEach(element => {
            element.style.display = 'none';
        });

        // Show welcome message for first-time users
        if (!this.hasVisitedBefore()) {
            this.showWelcomeMessage();
            this.markAsVisited();
        }

        // Check for browser compatibility
        this.checkBrowserCompatibility();

        // Setup periodic tasks
        this.setupPeriodicTasks();
    }

    // Navigation handling
    handleNavigation(event) {
        // Handle browser navigation
        const hash = window.location.hash.substring(1);
        if (hash && this.pages.dashboard) {
            this.pages.dashboard.showPage(hash);
        }
    }

    handleVisibilityChange() {
        if (document.hidden) {
            // Page is hidden (user switched tabs)
            console.log('Page hidden - pausing background tasks');
            this.pauseBackgroundTasks();
        } else {
            // Page is visible again
            console.log('Page visible - resuming background tasks');
            this.resumeBackgroundTasks();
            
            // Refresh data if needed
            if (this.pages.dashboard) {
                this.pages.dashboard.loadPageData(this.pages.dashboard.currentPage);
            }
        }
    }

    handleResize() {
        // Handle responsive layout changes
        const isMobile = window.innerWidth < 768;
        document.body.classList.toggle('mobile-view', isMobile);
        
        // Adjust sidebar for mobile
        const sidebar = document.querySelector('.sidebar');
        if (sidebar) {
            sidebar.classList.toggle('mobile-collapsed', isMobile);
        }
    }

    handleKeyboardShortcuts(event) {
        // Only handle shortcuts when not in input fields
        if (event.target.tagName === 'INPUT' || event.target.tagName === 'TEXTAREA') {
            return;
        }

        const key = event.key.toLowerCase();
        const ctrl = event.ctrlKey || event.metaKey;
        const shift = event.shiftKey;

        // Ctrl+K: Quick search (if implemented)
        if (ctrl && key === 'k') {
            event.preventDefault();
            this.openQuickSearch();
        }

        // Ctrl+/: Show keyboard shortcuts
        if (ctrl && key === '/') {
            event.preventDefault();
            this.showKeyboardShortcuts();
        }

        // Escape: Close modals or go back
        if (key === 'escape') {
            this.handleEscapeKey();
        }

        // Ctrl+L: Logout
        if (ctrl && key === 'l') {
            event.preventDefault();
            if (this.modules.authService && this.modules.authService.isAuthenticated) {
                this.modules.authService.logout();
            }
        }
    }

    handleFormSubmit(event) {
        // Add global form validation or processing if needed
        const form = event.target;
        
        // Add loading state to forms
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn && !form.classList.contains('no-global-loading')) {
            submitBtn.classList.add('loading');
            setTimeout(() => {
                submitBtn.classList.remove('loading');
            }, 2000); // Reset after 2 seconds as fallback
        }
    }

    handleGlobalClick(event) {
        // Handle global click events
        const target = event.target;

        // Close dropdowns when clicking outside
        if (!target.closest('.dropdown')) {
            document.querySelectorAll('.dropdown.active').forEach(dropdown => {
                dropdown.classList.remove('active');
            });
        }

        // Handle modal backdrop clicks
        if (target.classList.contains('modal')) {
            const modal = target.closest('.modal');
            if (modal) {
                modal.classList.remove('active');
            }
        }
    }

    handleEscapeKey() {
        // Close active modals
        document.querySelectorAll('.modal.active').forEach(modal => {
            modal.classList.remove('active');
        });

        // Close active dropdowns
        document.querySelectorAll('.dropdown.active').forEach(dropdown => {
            dropdown.classList.remove('active');
        });

        // Exit fullscreen if active
        if (document.fullscreenElement) {
            document.exitFullscreen();
        }
    }

    // Page management
    showLogin() {
        if (this.pages.login) {
            this.pages.login.show();
        }
        if (this.pages.dashboard) {
            this.pages.dashboard.hide();
        }
    }

    showDashboard() {
        if (this.pages.login) {
            this.pages.login.hide();
        }
        if (this.pages.dashboard) {
            this.pages.dashboard.show();
        }
    }

    // Network status
    showNetworkStatus(status) {
        const statusElement = document.getElementById('networkStatus');
        if (!statusElement) {
            this.createNetworkStatusIndicator();
        }

        const indicator = document.getElementById('networkStatus');
        if (indicator) {
            indicator.className = `network-status ${status}`;
            indicator.textContent = status === 'online' ? '🟢 Online' : '🔴 Offline';
            
            // Auto-hide after 3 seconds
            setTimeout(() => {
                indicator.style.display = 'none';
            }, 3000);
        }
    }

    createNetworkStatusIndicator() {
        const indicator = document.createElement('div');
        indicator.id = 'networkStatus';
        indicator.className = 'network-status';
        indicator.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 8px 16px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: 600;
            z-index: 10000;
            transition: all 0.3s ease;
        `;
        document.body.appendChild(indicator);
    }

    // Background tasks
    setupPeriodicTasks() {
        // Refresh notifications every 5 minutes
        setInterval(() => {
            if (this.pages.dashboard && !document.hidden) {
                if (this.pages.dashboard.components.notifications) {
                    this.pages.dashboard.components.notifications.refresh();
                }
            }
        }, 5 * 60 * 1000);

        // Check session validity every 10 minutes
        setInterval(() => {
            if (this.modules.authService && this.modules.authService.isAuthenticated) {
                this.validateSession();
            }
        }, 10 * 60 * 1000);
    }

    pauseBackgroundTasks() {
        // Pause any background tasks when page is hidden
        console.log('Background tasks paused');
    }

    resumeBackgroundTasks() {
        // Resume background tasks when page is visible again
        console.log('Background tasks resumed');
    }

    async validateSession() {
        try {
            if (this.modules.authService) {
                const isValid = await this.modules.authService.validateSession();
                if (!isValid) {
                    console.log('Session invalid, logging out...');
                    this.modules.authService.logout();
                }
            }
        } catch (error) {
            console.error('Session validation failed:', error);
        }
    }

    // User experience
    showWelcomeMessage() {
        if (this.modules.authService && this.modules.authService.isAuthenticated) {
            const userName = this.modules.authService.getDisplayName();
            this.modules.authService.showAlert(`Welcome back, ${userName}! 👋`, 'success');
        }
    }

    hasVisitedBefore() {
        return StorageHelpers.get('hrm_visited_before', false);
    }

    markAsVisited() {
        StorageHelpers.set('hrm_visited_before', true);
    }

    checkBrowserCompatibility() {
        const requiredFeatures = [
            'fetch',
            'localStorage',
            'sessionStorage',
            'Promise'
        ];

        const missingFeatures = requiredFeatures.filter(feature => !(feature in window));

        if (missingFeatures.length > 0) {
            console.warn('Browser missing required features:', missingFeatures);
            this.showBrowserCompatibilityWarning(missingFeatures);
        }
    }

    showBrowserCompatibilityWarning(missingFeatures) {
        const message = `Your browser may not support some features: ${missingFeatures.join(', ')}. Please update your browser for the best experience.`;
        
        if (this.modules.authService) {
            this.modules.authService.showAlert(message, 'warning');
        } else {
            alert(message);
        }
    }

    // Utility methods
    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    throttle(func, limit) {
        let inThrottle;
        return function(...args) {
            if (!inThrottle) {
                func.apply(this, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }

    // Error handling
    showCriticalError(message) {
        const errorDiv = document.createElement('div');
        errorDiv.innerHTML = `
            <div style="
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(0, 0, 0, 0.8);
                display: flex;
                align-items: center;
                justify-content: center;
                z-index: 99999;
                color: white;
                text-align: center;
                padding: 20px;
            ">
                <div>
                    <h2>⚠️ Critical Error</h2>
                    <p>${message}</p>
                    <button onclick="location.reload()" style="
                        margin-top: 20px;
                        padding: 10px 20px;
                        background: #3b82f6;
                        color: white;
                        border: none;
                        border-radius: 5px;
                        cursor: pointer;
                    ">Reload Page</button>
                </div>
            </div>
        `;
        document.body.appendChild(errorDiv);
    }

    logError(type, details) {
        // Log errors to console and could send to error tracking service
        console.error(`HRM Error [${type}]:`, details);
        
        // Store error locally for debugging
        const errors = StorageHelpers.get('hrm_errors', []);
        errors.push({
            type,
            details,
            timestamp: new Date().toISOString(),
            userAgent: navigator.userAgent,
            url: window.location.href
        });
        
        // Keep only last 50 errors
        if (errors.length > 50) {
            errors.splice(0, errors.length - 50);
        }
        
        StorageHelpers.set('hrm_errors', errors);
    }

    // Feature methods (placeholders for future implementation)
    openQuickSearch() {
        console.log('Quick search feature not yet implemented');
    }

    showKeyboardShortcuts() {
        const shortcuts = [
            'Ctrl+K: Quick Search',
            'Ctrl+/: Show Shortcuts',
            'Ctrl+L: Logout',
            'Escape: Close Modals'
        ];
        
        const message = `Keyboard Shortcuts:\n${shortcuts.join('\n')}`;
        alert(message);
    }

    // Public API
    getVersion() {
        return this.version;
    }

    isReady() {
        return this.isInitialized;
    }

    getModule(name) {
        return this.modules[name];
    }

    getPage(name) {
        return this.pages[name];
    }
}

// Initialize the application
window.hrmApp = new HrmSystem();

// Export for use in other modules
window.HrmSystem = HrmSystem;
