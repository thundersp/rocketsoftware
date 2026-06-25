package com.buzzmeet.security;

import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public ApplicationUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof ApplicationUser user)) {
            throw new AuthorizationDeniedException("Authenticated user context is required");
        }
        return user;
    }

    public Integer currentEmployeeId() {
        return requireCurrentUser().getEmployeeId();
    }

    public boolean hasAuthority(String authority) {
        ApplicationUser user = requireCurrentUser();
        return user.getAuthorities().stream().anyMatch(item -> authority.equals(item.getAuthority()));
    }
}