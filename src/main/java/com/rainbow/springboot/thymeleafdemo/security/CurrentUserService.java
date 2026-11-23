package com.rainbow.springboot.thymeleafdemo.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;

/**
 * Small helper used by Controllers to look up the full AppUser entity for
 * whoever is currently logged in.
 *
 * Spring Security's SecurityContextHolder only knows the username (as a
 * String) plus the authorities computed by CustomUserDetailsService - it
 * does not know about our AppUser entity at all. This service bridges that
 * gap: "given the currently authenticated username, load the real AppUser
 * row" - used, for example, to know WHO is granting a permission (for the
 * UserPermission audit trail) or to decide which dashboard a freshly
 * logged-in user should land on.
 */
@Service
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository theAppUserRepository) {
        appUserRepository = theAppUserRepository;
    }

    @Transactional(readOnly = true)
    public AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        AppUser user = appUserRepository.findByUsername(username).orElse(null);
        if (user != null && user.getEmployee() != null) {
            user.getEmployee().getFullName();
            if (user.getEmployee().getDepartment() != null) {
                user.getEmployee().getDepartment().getName();
            }
            if (user.getEmployee().getStatus() != null) {
                user.getEmployee().getStatus().name();
            }
        }
        return user;
    }

    public boolean isCurrentUserSuperAdmin() {
        AppUser user = getCurrentUser();
        return user != null && user.isSuperAdmin();
    }
}
