package com.rainbow.springboot.thymeleafdemo.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.security.CurrentUserService;
import com.rainbow.springboot.thymeleafdemo.service.AccountSetupService;
import com.rainbow.springboot.thymeleafdemo.service.EmployeeService;

/** Super Admin forms: create a reception account, and link an account to an employee record. */
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class AccountSetupController {

    private final AccountSetupService accountSetupService;
    private final EmployeeService employeeService;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;

    public AccountSetupController(AccountSetupService theAccountSetupService,
                                  EmployeeService theEmployeeService,
                                  AppUserRepository theAppUserRepository,
                                  CurrentUserService theCurrentUserService) {
        accountSetupService = theAccountSetupService;
        employeeService = theEmployeeService;
        appUserRepository = theAppUserRepository;
        currentUserService = theCurrentUserService;
    }

    @GetMapping("/new-reception")
    public String receptionForm(Model model) {
        model.addAttribute("employees", employeeService.findAll());
        return "admin/create-reception-user";
    }

    @PostMapping("/new-reception")
    public String createReception(@RequestParam("employeeId") int employeeId,
                                  @RequestParam("username") String username,
                                  @RequestParam("password") String password,
                                  @RequestParam(name = "email", required = false) String email,
                                  Model model, RedirectAttributes redirectAttributes) {
        try {
            if (username.isBlank() || password.length() < 8) {
                throw new IllegalStateException("Enter a username and a password of at least 8 characters.");
            }
            accountSetupService.createReceptionUser(username.trim(), password, email, employeeId,
                    currentUserService.getCurrentUser());
        } catch (RuntimeException ex) {
            model.addAttribute("employees", employeeService.findAll());
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/create-reception-user";
        }
        redirectAttributes.addFlashAttribute("successMessage",
                "Reception account \"" + username.trim() + "\" was created. It can open the reception page after signing in.");
        return "redirect:/admin/users";
    }

    @GetMapping("/link")
    public String linkForm(Model model) {
        fillLinkForm(model);
        return "admin/link-employee";
    }

    @PostMapping("/link")
    public String link(@RequestParam("userId") int userId,
                       @RequestParam("employeeId") int employeeId,
                       Model model, RedirectAttributes redirectAttributes) {
        try {
            accountSetupService.linkToEmployee(userId, employeeId);
        } catch (RuntimeException ex) {
            fillLinkForm(model);
            model.addAttribute("errorMessage", ex.getMessage());
            return "admin/link-employee";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Account linked. It can now clock in and out under My attendance.");
        return "redirect:/admin/users";
    }

    private void fillLinkForm(Model model) {
        List<AppUser> unlinked = appUserRepository.findAllByOrderByFullNameAsc().stream()
                .filter(u -> u.getEmployee() == null)
                .collect(Collectors.toList());
        model.addAttribute("accounts", unlinked);
        model.addAttribute("employees", employeeService.findAll());
    }
}
