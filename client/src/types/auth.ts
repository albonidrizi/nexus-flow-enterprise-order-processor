export const Role = {
    ROLE_CUSTOMER: 'ROLE_CUSTOMER',
    ROLE_ADMIN: 'ROLE_ADMIN'
} as const;

export type Role = (typeof Role)[keyof typeof Role];

export interface User {
    username: string;
    role: Role;
}

export interface AuthResponse {
    token: string;
    username: string;
    role: Role;
}
