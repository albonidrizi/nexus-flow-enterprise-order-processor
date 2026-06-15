import { useState } from 'react';
import type { FormEvent } from 'react';
import { useAuth } from '../context/useAuth';
import api from '../api/axios';
import type { AuthResponse } from '../types/auth';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';

const RegisterPage = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
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
            setError('Please enter a username');
            return;
        }
        if (trimmedUsername.length < 3) {
            setError('Username must be at least 3 characters long');
            return;
        }
        if (trimmedUsername.length > 50) {
            setError('Username must be less than 50 characters');
            return;
        }
        if (!password) {
            setError('Please enter a password');
            return;
        }
        if (password.length < 8) {
            setError('Password must be at least 8 characters long');
            return;
        }
        if (password.length > 100) {
            setError('Password must be less than 100 characters');
            return;
        }
        if (password !== confirmPassword) {
            setError('Passwords do not match');
            return;
        }

        setIsLoading(true);
        try {
            const response = await api.post<AuthResponse>('/auth/register', { 
                username: trimmedUsername, 
                password
            });
            const { token, role: userRole } = response.data;
            login(token, trimmedUsername, userRole);
            navigate('/dashboard');
        } catch (err) {
            if (axios.isAxiosError(err)) {
                const status = err.response?.status;
                const message = err.response?.data?.message;
                const validationErrors = err.response?.data?.validationErrors;
                
                if (status === 400) {
                    if (validationErrors) {
                        const firstError = Object.values(validationErrors)[0];
                        setError(String(firstError));
                    } else if (message?.includes('already exists')) {
                        setError('This username is already taken. Please choose another.');
                    } else {
                        setError(message || 'Invalid input. Please check your details.');
                    }
                } else if (status === 500) {
                    setError('Server error. Please try again later.');
                } else if (!err.response) {
                    setError('Unable to connect to server. Please check your connection.');
                } else {
                    setError(message || 'Registration failed. Please try again.');
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
                <h2>Create Account</h2>
                <p className="auth-subtitle">Create a standard user account</p>
            </div>
            <form onSubmit={handleSubmit} autoComplete="off">
                <div className="form-group">
                    <label htmlFor="username">Username</label>
                    <input 
                        id="username"
                        type="text" 
                        value={username} 
                        onChange={(e) => setUsername(e.target.value)} 
                        placeholder="Choose a username (min. 3 characters)"
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
                        placeholder="Create a password (min. 8 characters)"
                        disabled={isLoading}
                        autoComplete="off"
                    />
                </div>
                <div className="form-group">
                    <label htmlFor="confirmPassword">Confirm Password</label>
                    <input 
                        id="confirmPassword"
                        type="password" 
                        value={confirmPassword} 
                        onChange={(e) => setConfirmPassword(e.target.value)} 
                        placeholder="Confirm your password"
                        disabled={isLoading}
                        autoComplete="off"
                    />
                </div>
                {error && <div className="error-message">{error}</div>}
                <button type="submit" disabled={isLoading} className={isLoading ? 'loading' : ''}>
                    {isLoading ? 'Creating Account...' : 'Create Account'}
                </button>
            </form>
            <div className="auth-footer">
                <p>Already have an account? <Link to="/login">Sign In</Link></p>
            </div>
        </div>
    );
};

export default RegisterPage;
