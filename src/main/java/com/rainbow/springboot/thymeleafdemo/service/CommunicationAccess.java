package com.rainbow.springboot.thymeleafdemo.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.dao.UserPermissionRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.UserPermission;
import com.rainbow.springboot.thymeleafdemo.security.PermissionCode;

/** Answers "who is a manager?" for announcements and messages. */
@Component
public class CommunicationAccess {

    private final UserPermissionRepository userPermissionRepository;
    private final AppUserRepository appUserRepository;

    public CommunicationAccess(UserPermissionRepository theUserPermissionRepository,
                               AppUserRepository theAppUserRepository) {
        userPermissionRepository = theUserPermissionRepository;
        appUserRepository = theAppUserRepository;
    }

    /** Super admins and holders of CAN_MANAGE_ANNOUNCEMENTS: the "employer" side of every conversation. */
    @Transactional(readOnly = true)
    public boolean isManager(AppUser user) {
        if (user == null) {
            return false;
        }
        if (user.isSuperAdmin()) {
            return true;
        }
        for (UserPermission up : userPermissionRepository.findByUser(user)) {
            if (PermissionCode.CAN_MANAGE_ANNOUNCEMENTS.equals(up.getPermission().getCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether this user may read the Administration announcements. Reception accounts may not,
     * unless they were granted CAN_VIEW_ADMIN_ANNOUNCEMENTS; everyone else (managers, staff) may.
     */
    @Transactional(readOnly = true)
    public boolean canViewAdministration(AppUser user) {
        if (user == null) {
            return false;
        }
        if (user.isSuperAdmin()) {
            return true;
        }
        boolean reception = false;
        for (UserPermission up : userPermissionRepository.findByUser(user)) {
            String code = up.getPermission().getCode();
            if (PermissionCode.CAN_MANAGE_ANNOUNCEMENTS.equals(code) || PermissionCode.CAN_VIEW_ADMIN_ANNOUNCEMENTS.equals(code)) {
                return true;
            }
            if (PermissionCode.CAN_USE_RECEPTION.equals(code)) {
                reception = true;
            }
        }
        return !reception;
    }

    @Transactional(readOnly = true)
    public List<AppUser> managers() {
        return appUserRepository.findNotifiable(PermissionCode.CAN_MANAGE_ANNOUNCEMENTS);
    }
}
