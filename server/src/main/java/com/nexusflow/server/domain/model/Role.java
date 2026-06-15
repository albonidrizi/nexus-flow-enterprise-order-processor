package com.nexusflow.server.domain.model;

public enum Role {
    ROLE_USER("USER"),
    ROLE_MANAGER("MANAGER"),
    ROLE_ADMIN("ADMIN");

    private final String securityRole;

    Role(String securityRole) {
        this.securityRole = securityRole;
    }

    public String securityRole() {
        return securityRole;
    }

    public boolean isStaff() {
        return this == ROLE_MANAGER || this == ROLE_ADMIN;
    }
}
