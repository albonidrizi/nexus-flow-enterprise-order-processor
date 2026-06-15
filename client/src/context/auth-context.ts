import { createContext } from 'react';
import type { Role, User } from '../types/auth';

export interface AuthContextType {
  user: User | null;
  login: (token: string, username: string, role: Role) => void;
  logout: () => void;
  isAuthenticated: boolean;
  isLoading: boolean;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
