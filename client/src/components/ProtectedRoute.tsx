import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/useAuth';
import { Role } from '../types/auth';

interface ProtectedRouteProps {
    allowedRoles?: Role[];
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ allowedRoles }) => {
    const { isAuthenticated, user, isLoading } = useAuth();

    if (isLoading) {
        return <div className="screen-loader">Loading...</div>;
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    if (allowedRoles && user && !allowedRoles.includes(user.role)) {
        return <Navigate to="/dashboard" replace />; // Redirect unauthorized to home/dashboard
    }

    return <Outlet />;
};

export default ProtectedRoute;
