/**
 * API Helper Module for Online E-Commerce Platform
 * Provides unified, reusable fetch wrapper with automatic JWT token attachment.
 */

// Base URL: relative root pointing to the current Spring Boot application host
const API_BASE_URL = '';

/**
 * Get stored JWT token from localStorage.
 */
function getStoredToken() {
    return localStorage.getItem('token');
}

/**
 * Universal API request wrapper.
 * Automatically injects Authorization header if JWT token is stored.
 *
 * @param {string} endpoint - The relative API path (e.g., '/api/auth/login')
 * @param {object} options - Fetch options (method, headers, body, etc.)
 * @returns {Promise<any>} - Parsed JSON response
 */
async function apiRequest(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;

    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        ...(options.headers || {})
    };

    const token = getStoredToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const config = {
        ...options,
        headers
    };

    try {
        const response = await fetch(url, config);

        // Attempt to parse response body as JSON
        const contentType = response.headers.get('content-type');
        let data = null;
        if (contentType && contentType.includes('application/json')) {
            data = await response.json().catch(() => null);
        } else {
            const text = await response.text().catch(() => null);
            data = text ? { message: text } : null;
        }

        if (!response.ok) {
            let errorMessage = 'An unexpected error occurred.';
            if (data) {
                if (data.message) {
                    errorMessage = data.message;
                } else if (data.error) {
                    errorMessage = data.error;
                } else if (data.details) {
                    errorMessage = Object.values(data.details).join('; ');
                }
            }

            const error = new Error(errorMessage);
            error.status = response.status;
            error.data = data;
            throw error;
        }

        return data;
    } catch (err) {
        // Network or fetch rejection
        if (!err.status && err.name === 'TypeError') {
            const networkError = new Error('Network error: Unable to connect to backend server.');
            networkError.status = 0;
            throw networkError;
        }
        throw err;
    }
}

/**
 * Send HTTP GET request.
 */
async function apiGet(endpoint) {
    return apiRequest(endpoint, { method: 'GET' });
}

/**
 * Send HTTP POST request with JSON payload.
 */
async function apiPost(endpoint, body) {
    return apiRequest(endpoint, {
        method: 'POST',
        body: JSON.stringify(body)
    });
}

/**
 * Send HTTP PUT request with JSON payload.
 */
async function apiPut(endpoint, body) {
    return apiRequest(endpoint, {
        method: 'PUT',
        body: body ? JSON.stringify(body) : undefined
    });
}

/**
 * Send HTTP PATCH request with JSON payload.
 */
async function apiPatch(endpoint, body) {
    return apiRequest(endpoint, {
        method: 'PATCH',
        body: body ? JSON.stringify(body) : undefined
    });
}

/**
 * Send HTTP DELETE request.
 */
async function apiDelete(endpoint) {
    return apiRequest(endpoint, { method: 'DELETE' });
}

