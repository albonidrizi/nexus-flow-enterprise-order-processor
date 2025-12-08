import { useState } from 'react';
import type { FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { Role } from '../types/auth';
import type { AuthResponse } from '../types/auth';
import { useNavigate, Link } from 'react-router-dom';

const RegisterPage = () => {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [role, setRole] = useState<Role>(Role.ROLE_CUSTOMER);
    const [error, setError] = useState('');
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();
        try {
            const response = await api.post<AuthResponse>('/auth/register', { username, password, role });
            const { token, role: userRole } = response.data;
            login(token, username, userRole);
            navigate(userRole === 'ROLE_ADMIN' ? '/admin' : '/dashboard');
        } catch (err: any) {
            setError(err.response?.data?.message || 'Registration failed');
        }
    };

    return (
        <div className="auth-container">
            <h2>Register</h2>
            <form onSubmit={handleSubmit}>
                <div className="form-group">
                    <label>Username</label>
                    <input type="text" value={username} onChange={(e) => setUsername(e.target.value)} required />
                </div>
                <div className="form-group">
                    <label>Password</label>
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
                </div>
                <div className="form-group">
                    <label>Role</label>
                    <select value={role} onChange={(e) => setRole(e.target.value as Role)} style={{ width: '100%', padding: '0.5rem' }}>
                        <option value={Role.ROLE_CUSTOMER}>Customer</option>
                        <option value={Role.ROLE_ADMIN}>Admin</option>
                    </select>
                </div>
                {error && <p className="error">{error}</p>}
                <button type="submit">Register</button>
            </form>
            <p>Already have an account? <Link to="/login">Login</Link></p>
        </div>
    );
};

export default RegisterPage;
