import { useState } from 'react';
import type { ReactNode } from 'react';
import { Role } from '../types/auth';
import type { User } from '../types/auth';
import { AuthContext } from './auth-context';

const roleValues = Object.values(Role);

const readStoredUser = (): User | null => {
  const token = localStorage.getItem('token');
  const username = localStorage.getItem('username');
  const role = localStorage.getItem('role');

  if (token && username && roleValues.includes(role as Role)) {
    return { username, role: role as Role };
  }

  return null;
};

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<User | null>(() => readStoredUser());

  const login = (token: string, username: string, role: Role) => {
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);
    localStorage.setItem('role', role);
    setUser({ username, role });
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user, isLoading: false }}>
      {children}
    </AuthContext.Provider>
  );
};
