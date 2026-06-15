import { useState } from 'react';
import type { FormEvent } from 'react';
import { useAuth } from '../context/useAuth';
import api from '../api/axios';
import type { AuthResponse } from '../types/auth';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { Role } from '../types/auth';

const LoginPage = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setError('');

        // Client-side validation
        const trimmedUsername = username.trim();
        if (!trimmedUsername) {
            setError('Please enter your username');
            return;
        }
        if (!password) {
            setError('Please enter your password');
            return;
        }

        setIsLoading(true);
        try {
            const response = await api.post<AuthResponse>('/auth/login', { 
                username: trimmedUsername, 
                password 
            });
            const { token, role } = response.data;
            login(token, trimmedUsername, role);
            navigate(role === Role.ROLE_ADMIN || role === Role.ROLE_MANAGER ? '/admin' : '/dashboard');
        } catch (err) {
            if (axios.isAxiosError(err)) {
                const status = err.response?.status;
                const message = err.response?.data?.message;
                
                if (status === 401) {
                    setError('Invalid username or password. Please try again.');
                } else if (status === 400) {
                    setError(message || 'Invalid request. Please check your input.');
                } else if (status === 500) {
                    setError('Server error. Please try again later.');
                } else if (!err.response) {
                    setError('Unable to connect to server. Please check your connection.');
                } else {
                    setError(message || 'Login failed. Please try again.');
                }
            } else {
                setError('An unexpected error occurred. Please try again.');
            }
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-header">
                <h2>NexusFlow</h2>
                <p className="auth-subtitle">Enterprise order processing console</p>
            </div>
            <form onSubmit={handleSubmit} autoComplete="off">
                <div className="form-group">
                    <label htmlFor="username">Username</label>
                    <input
                        id="username"
                        type="text"
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        placeholder="Enter your username"
                        disabled={isLoading}
                        autoComplete="off"
                        autoFocus
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="password">Password</label>
                    <input
                        id="password"
                        type="password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        placeholder="Enter your password"
                        disabled={isLoading}
                        autoComplete="off"
                    />
                </div>
                {error && <div className="error-message">{error}</div>}
                <button type="submit" disabled={isLoading} className={isLoading ? 'loading' : ''}>
                    {isLoading ? 'Signing in...' : 'Sign In'}
                </button>
            </form>
            <div className="auth-footer">
                <p>Don't have an account? <Link to="/register">Create Account</Link></p>
                <div className="demo-credentials">
                    <p><strong>Demo:</strong> user/user12345, manager/manager123, admin/admin123</p>
                </div>
            </div>
        </div>
    );
};

export default LoginPage;
