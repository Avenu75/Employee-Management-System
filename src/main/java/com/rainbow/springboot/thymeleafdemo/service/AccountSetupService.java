package com.rainbow.springboot.thymeleafdemo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.security.RolePreset;

/** Two account chores for the Super Admin: reception accounts, and linking an account to an employee. */
@Service
public class AccountSetupService {

    private final UserManagementService userManagementService;
    private final EmployeeService employeeService;
    private final AppUserRepository appUserRepository;

    public AccountSetupService(UserManagementService theUserManagementService,
                               EmployeeService theEmployeeService,
                               AppUserRepository theAppUserRepository) {
        userManagementService = theUserManagementService;
        employeeService = theEmployeeService;
        appUserRepository = theAppUserRepository;
    }

    /** Creates a login for an employee and gives it the RECEPTION role (reception gate + own profile). */
    public AppUser createReceptionUser(String username, String rawPassword, String email, int employeeId, AppUser createdBy) {
        Employee employee = employeeService.findById(employeeId);
        if (appUserRepository.findFirstByEmployee(employee).isPresent()) {
            throw new IllegalStateException(employee.getFullName() + " already has an account. "
                    + "Grant that account \"Use Reception\" under Privileges instead.");
        }
        AppUser user = userManagementService.createStaffUser(username, rawPassword, employee.getFullName(), email, employeeId);
        userManagementService.applyRolePreset(user.getId(), RolePreset.RECEPTION, createdBy);
        return user;
    }

    /**
     * Links an existing account (for example the Super Admin's own) to an employee record so
     * that it can clock in and out.
     */
    @Transactional
    public void linkToEmployee(int userId, int employeeId) {
        AppUser user = userManagementService.findById(userId);
        if (user.getEmployee() != null) {
            throw new IllegalStateException(user.getFullName() + " is already linked to an employee record.");
        }
        Employee employee = employeeService.findById(employeeId);
        if (appUserRepository.findFirstByEmployee(employee).isPresent()) {
            throw new IllegalStateException(employee.getFullName() + " is already linked to another account.");
        }
        user.setEmployee(employee);
        appUserRepository.save(user);
    }
}
