package com.rainbow.springboot.thymeleafdemo.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.security.PermissionCode;

/**
 * Turns sensitive changes into notifications.
 *
 * Who is told:
 *  - the Super Admin and every holder of CAN_EDIT_EMPLOYEE (the people who run HR);
 *  - the employee the change is about, when someone else made it;
 *  - the person who made the change is never notified about their own action.
 *
 * Profile pictures are deliberately NOT treated as sensitive and never reported.
 */
@Service
public class SensitiveChangeService {

    /** Privileges that give wide power; granting one is flagged as "Important". */
    private static final Set<String> BIG_PRIVILEGES = Set.of(
            PermissionCode.CAN_MANAGE_USERS,
            PermissionCode.CAN_MANAGE_PERMISSIONS,
            PermissionCode.CAN_RESET_PASSWORD,
            PermissionCode.CAN_DELETE_EMPLOYEE,
            PermissionCode.CAN_MANAGE_AUDIT_LOG,
            PermissionCode.CAN_VIEW_PAYROLL,
            PermissionCode.CAN_MANAGE_PAYROLL,
            PermissionCode.CAN_MANAGE_LEAVE);

    private static final String NATIONAL_ID = "National ID";

    private final NotificationService notificationService;
    private final AppUserRepository appUserRepository;

    public SensitiveChangeService(NotificationService theNotificationService,
                                  AppUserRepository theAppUserRepository) {
        notificationService = theNotificationService;
        appUserRepository = theAppUserRepository;
    }

    public static boolean isBigPrivilege(String permissionCode) {
        return permissionCode != null && BIG_PRIVILEGES.contains(permissionCode);
    }

    /** The sensitive fields of an employee as label to text. Call this BEFORE saving a change. */
    public Map<String, String> snapshot(Employee e) {
        Map<String, String> m = new LinkedHashMap<>();
        if (e == null) {
            return m;
        }
        m.put("First name", text(e.getFirstName()));
        m.put("Last name", text(e.getLastName()));
        m.put("Email", text(e.getEmail()));
        m.put("Phone", text(e.getPhoneNumber()));
        m.put(NATIONAL_ID, text(e.getNationalId()));
        m.put("Date of birth", e.getDateOfBirth() == null ? "" : e.getDateOfBirth().toString());
        m.put("Gender", e.getGender() == null ? "" : e.getGender().name());
        m.put("Job title", text(e.getJobTitle()));
        m.put("Salary", money(e.getSalary()));
        m.put("Hire date", e.getHireDate() == null ? "" : e.getHireDate().toString());
        m.put("Status", e.getStatus() == null ? "" : e.getStatus().name());
        m.put("Department", e.getDepartment() == null ? "" : text(e.getDepartment().getName()));
        return m;
    }

    @Transactional
    public void reportEmployeeChange(AppUser actor, Employee employee, Map<String, String> before) {
        List<String> changes = diff(before, snapshot(employee));
        if (changes.isEmpty()) {
            return;
        }
        String summary = String.join("; ", changes);
        notifyAboutEmployee(actor, employee,
                (self, who, subject) -> self
                        ? subject + " updated their own details: " + summary
                        : who + " updated " + subject + "'s details: " + summary,
                (who) -> who + " changed your details: " + summary);
    }

    @Transactional
    public void reportDocumentChange(AppUser actor, Employee employee, String action, List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            return;
        }
        String files = fileNames.size() + " document(s): " + String.join(", ", fileNames);
        notifyAboutEmployee(actor, employee,
                (self, who, subject) -> self
                        ? subject + " " + action + " " + files
                        : who + " " + action + " " + files + " for " + subject,
                (who) -> who + " " + action + " " + files + " on your record");
    }

    /** Tells the person whose privileges changed, and the other people who manage privileges. */
    @Transactional
    public void reportPrivilegeChange(AppUser actor, AppUser target, List<String> granted,
                                      List<String> revoked, boolean elevated) {
        if (target == null || ((granted == null || granted.isEmpty()) && (revoked == null || revoked.isEmpty()))) {
            return;
        }
        StringBuilder detail = new StringBuilder();
        if (granted != null && !granted.isEmpty()) {
            detail.append("Granted: ").append(String.join(", ", granted)).append(". ");
        }
        if (revoked != null && !revoked.isEmpty()) {
            detail.append("Revoked: ").append(String.join(", ", revoked)).append(". ");
        }
        String who = nameOf(actor);
        String body = detail.toString().trim();

        if (actor == null || actor.getId() != target.getId()) {
            notificationService.notify(target,
                    (elevated ? "Important: " : "") + "Your privileges were updated by " + who + ". " + body, "/profile");
        }
        for (AppUser other : appUserRepository.findNotifiable(PermissionCode.CAN_MANAGE_PERMISSIONS)) {
            if (other.getId() == target.getId() || (actor != null && other.getId() == actor.getId())) {
                continue;
            }
            notificationService.notify(other,
                    who + " changed the privileges of " + nameOf(target) + ". " + body, "/admin/privileges");
        }
    }

    /** Role presets (Staff / Admin) are reported the same way. */
    @Transactional
    public void reportRoleChange(AppUser actor, AppUser target, String roleName, boolean elevated) {
        if (target == null) {
            return;
        }
        String who = nameOf(actor);
        if (actor == null || actor.getId() != target.getId()) {
            notificationService.notify(target,
                    (elevated ? "Important: " : "") + "You were set as " + roleName + " by " + who
                            + ". Sign in again to see your new menu.", "/profile");
        }
        for (AppUser other : appUserRepository.findNotifiable(PermissionCode.CAN_MANAGE_PERMISSIONS)) {
            if (other.getId() == target.getId() || (actor != null && other.getId() == actor.getId())) {
                continue;
            }
            notificationService.notify(other,
                    who + " set " + nameOf(target) + " as " + roleName + ".", "/admin/privileges");
        }
    }

    private interface AdminText {
        String build(boolean self, String who, String subject);
    }

    private interface SubjectText {
        String build(String who);
    }

    private void notifyAboutEmployee(AppUser actor, Employee employee, AdminText adminText, SubjectText subjectText) {
        AppUser subjectUser = appUserRepository.findFirstByEmployee(employee).orElse(null);
        boolean self = actor != null && subjectUser != null && actor.getId() == subjectUser.getId();
        String who = nameOf(actor);

        Map<Integer, AppUser> admins = new LinkedHashMap<>();
        for (AppUser a : appUserRepository.findNotifiable(PermissionCode.CAN_EDIT_EMPLOYEE)) {
            admins.put(a.getId(), a);
        }
        if (actor != null) {
            admins.remove(actor.getId());
        }
        if (subjectUser != null) {
            admins.remove(subjectUser.getId());
        }

        notificationService.notifyUsers(admins.values(),
                adminText.build(self, who, employee.getFullName()), "/employees/profile/" + employee.getId());
        if (subjectUser != null && !self) {
            notificationService.notify(subjectUser, subjectText.build(who), "/profile");
        }
    }

    private static List<String> diff(Map<String, String> before, Map<String, String> after) {
        List<String> changes = new ArrayList<>();
        for (Map.Entry<String, String> e : after.entrySet()) {
            String old = before == null ? "" : before.getOrDefault(e.getKey(), "");
            if (Objects.equals(old, e.getValue())) {
                continue;
            }
            if (NATIONAL_ID.equals(e.getKey())) {
                changes.add(NATIONAL_ID + " changed");
            } else {
                changes.add(e.getKey() + ": " + shown(old) + " -> " + shown(e.getValue()));
            }
        }
        return changes;
    }

    private static String shown(String value) {
        return value == null || value.isBlank() ? "(empty)" : value;
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String money(BigDecimal value) {
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nameOf(AppUser user) {
        if (user == null) {
            return "Someone";
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return user.getUsername() != null ? user.getUsername() : "Someone";
    }
}
