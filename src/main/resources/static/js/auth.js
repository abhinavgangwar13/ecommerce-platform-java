/**
 * Authentication Module for Online E-Commerce Platform
 * Handles user login, registration, JWT persistence, session status, and role-based redirects.
 */

const Auth = {
    /**
     * Retrieve stored JWT token.
     */
    getToken() {
        return localStorage.getItem('token');
    },

    /**
     * Retrieve stored user metadata (id, name, email, role).
     */
    getUser() {
        const userStr = localStorage.getItem('user');
        if (!userStr) return null;
        try {
            return JSON.parse(userStr);
        } catch (e) {
            console.error('Failed to parse stored user:', e);
            return null;
        }
    },

    /**
     * Check if a valid session exists.
     */
    isLoggedIn() {
        return !!this.getToken();
    },

    /**
     * Store authentication token and user information.
     * Never stores passwords.
     */
    storeAuth(authResponse) {
        if (!authResponse || !authResponse.token) return;

        localStorage.setItem('token', authResponse.token);

        const user = {
            id: authResponse.id,
            name: authResponse.name,
            email: authResponse.email,
            role: authResponse.role
        };
        localStorage.setItem('user', JSON.stringify(user));
    },

    /**
     * Clear all stored authentication tokens and session data.
     */
    clearAuth() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    },

    /**
     * Log in with credentials and persist session.
     */
    async login(email, password) {
        const payload = {
            email: email.trim(),
            password: password
        };

        const response = await apiPost('/api/auth/login', payload);
        this.storeAuth(response);
        return response;
    },

    /**
     * Register a new user account.
     * Allowed roles from frontend: BUYER or SELLER.
     */
    async register(name, email, password, role) {
        const payload = {
            name: name.trim(),
            email: email.trim().toLowerCase(),
            password: password,
            role: role
        };

        const response = await apiPost('/api/auth/register', payload);
        return response;
    },

    /**
     * Log out current user and redirect to home page.
     */
    logout(redirectUrl = 'index.html') {
        this.clearAuth();
        window.location.href = redirectUrl;
    },

    /**
     * Get target dashboard URL based on user role.
     */
    getDashboardUrl(role) {
        switch (role) {
            case 'ADMIN':
                return 'admin-dashboard.html';
            case 'SELLER':
                return 'seller-dashboard.html';
            case 'BUYER':
                return 'buyer-dashboard.html';
            default:
                return 'index.html';
        }
    },

    /**
     * Redirect authenticated user to their role-specific dashboard.
     */
    redirectByRole(role) {
        const target = this.getDashboardUrl(role);
        window.location.href = target;
    },

    /**
     * Helper to render dynamic navbar auth links across pages.
     */
    initNavbar() {
        const guestNav = document.getElementById('guest-nav');
        const userNav = document.getElementById('user-nav');
        const userNameSpan = document.getElementById('nav-user-name');
        const userRoleBadge = document.getElementById('nav-user-role');
        const dashboardLink = document.getElementById('nav-dashboard-link');

        if (this.isLoggedIn()) {
            const user = this.getUser();
            if (guestNav) guestNav.classList.add('d-none');
            if (userNav) userNav.classList.remove('d-none');

            if (user) {
                if (userNameSpan) userNameSpan.textContent = user.name || user.email;
                if (userRoleBadge) {
                    userRoleBadge.textContent = user.role;
                    userRoleBadge.className = 'badge ms-1 ' +
                        (user.role === 'ADMIN' ? 'bg-danger' :
                         user.role === 'SELLER' ? 'bg-warning text-dark' : 'bg-primary');
                }
                if (dashboardLink) {
                    dashboardLink.href = this.getDashboardUrl(user.role);
                }
            }
        } else {
            if (guestNav) guestNav.classList.remove('d-none');
            if (userNav) userNav.classList.add('d-none');
        }
    }
};

// Auto-initialize navbar if present on page load
document.addEventListener('DOMContentLoaded', () => {
    Auth.initNavbar();
});
