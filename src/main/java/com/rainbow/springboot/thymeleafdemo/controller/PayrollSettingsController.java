package com.rainbow.springboot.thymeleafdemo.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.entity.Employee;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollCalcMethod;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollItem;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollItemKind;
import com.rainbow.springboot.thymeleafdemo.service.PayrollService;

import jakarta.validation.Valid;

/**
 * Where payroll is set up BEFORE it is generated: the recurring rules (tax, pension,
 * dues, allowances) and the one-off adjustments for a month. Needs CAN_MANAGE_PAYROLL.
 */
@Controller
@RequestMapping("/payroll")
@PreAuthorize("hasAnyAuthority('CAN_MANAGE_PAYROLL','SUPER_ADMIN')")
public class PayrollSettingsController {

    private final PayrollService payrollService;

    public PayrollSettingsController(PayrollService thePayrollService) {
        payrollService = thePayrollService;
    }

    // ------------------------------------------------------------ recurring rules

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("items", payrollService.allItems());
        model.addAttribute("month", YearMonth.now().toString());
        model.addAttribute("currency", payrollService.getCurrency());
        model.addAttribute("activeTab", "rules");
        return "payroll/settings";
    }

    @GetMapping("/settings/new")
    public String newItem(Model model) {
        return itemForm(model, new PayrollItem(), Set.of());
    }

    @GetMapping("/settings/edit")
    public String editItem(@RequestParam("itemId") int id, Model model) {
        PayrollItem item = payrollService.findItem(id);
        Set<Integer> selected = item.getEmployees().stream().map(Employee::getId).collect(Collectors.toSet());
        return itemForm(model, item, selected);
    }

    @PostMapping("/settings/save")
    public String saveItem(@Valid @ModelAttribute("item") PayrollItem item, BindingResult bindingResult,
                           @RequestParam(value = "employeeIds", required = false) List<Integer> employeeIds,
                           Principal principal, Model model, RedirectAttributes redirectAttributes) {
        if (item.getCalcMethod() == PayrollCalcMethod.PERCENT && item.getAmountValue() != null
                && item.getAmountValue().compareTo(new BigDecimal("100")) > 0) {
            bindingResult.rejectValue("amountValue", "percent.max", "A percentage cannot be more than 100.");
        }
        if (!item.isAllEmployees() && (employeeIds == null || employeeIds.isEmpty())) {
            bindingResult.rejectValue("allEmployees", "employees.required",
                    "Choose at least one employee, or apply it to all employees.");
        }
        if (bindingResult.hasErrors()) {
            Set<Integer> selected = employeeIds == null ? Set.of() : Set.copyOf(employeeIds);
            return itemForm(model, item, selected);
        }
        payrollService.saveItem(item, employeeIds, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "\"" + item.getName() + "\" was saved.");
        return "redirect:/payroll/settings";
    }

    @PostMapping("/settings/toggle")
    public String toggleItem(@RequestParam("itemId") int id, Principal principal, RedirectAttributes redirectAttributes) {
        payrollService.toggleItem(id, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage", "The payroll item was updated.");
        return "redirect:/payroll/settings";
    }

    @PostMapping("/settings/delete")
    public String deleteItem(@RequestParam("itemId") int id, Principal principal, RedirectAttributes redirectAttributes) {
        payrollService.deleteItem(id, principal.getName());
        redirectAttributes.addFlashAttribute("successMessage",
                "The payroll item was deleted. Payroll months already generated keep their saved figures.");
        return "redirect:/payroll/settings";
    }

    private String itemForm(Model model, PayrollItem item, Set<Integer> selected) {
        model.addAttribute("item", item);
        model.addAttribute("selectedIds", selected);
        model.addAttribute("month", YearMonth.now().toString());
        model.addAttribute("employees", payrollService.payableEmployees(YearMonth.now()));
        model.addAttribute("kinds", PayrollItemKind.values());
        model.addAttribute("methods", PayrollCalcMethod.values());
        model.addAttribute("activeTab", "rules");
        return "payroll/item-form";
    }

    // ---------------------------------------------------------------- adjustments

    @GetMapping("/adjustments")
    public String adjustments(@RequestParam(value = "month", required = false)
                              @DateTimeFormat(pattern = "yyyy-MM") YearMonth month, Model model) {
        YearMonth selected = month != null ? month : YearMonth.now();
        model.addAttribute("month", selected.toString());
        model.addAttribute("adjustments", payrollService.adjustments(selected));
        model.addAttribute("employees", payrollService.payableEmployees(selected));
        model.addAttribute("kinds", PayrollItemKind.values());
        model.addAttribute("locked", payrollService.isFinalised(selected));
        model.addAttribute("currency", payrollService.getCurrency());
        model.addAttribute("activeTab", "adjustments");
        return "payroll/adjustments";
    }

    @PostMapping("/adjustments/add")
    public String addAdjustment(@RequestParam("month") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                                @RequestParam("employeeId") int employeeId,
                                @RequestParam("label") String label,
                                @RequestParam("kind") PayrollItemKind kind,
                                @RequestParam("amount") BigDecimal amount,
                                @RequestParam(value = "note", required = false) String note,
                                Principal principal, RedirectAttributes redirectAttributes) {
        if (label == null || label.isBlank() || label.length() > 60) {
            redirectAttributes.addFlashAttribute("errorMessage", "Enter a name of up to 60 characters.");
        } else if (amount == null || amount.signum() <= 0 || amount.compareTo(new BigDecimal("99999999.99")) > 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Enter an amount greater than zero.");
        } else if (note != null && note.length() > 200) {
            redirectAttributes.addFlashAttribute("errorMessage", "The note must be at most 200 characters.");
        } else {
            try {
                payrollService.addAdjustment(month, employeeId, label, kind, amount, note, principal.getName());
                redirectAttributes.addFlashAttribute("successMessage", "The adjustment was added to " + month + ".");
            } catch (IllegalStateException | IllegalArgumentException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            }
        }
        return "redirect:/payroll/adjustments?month=" + month;
    }

    @PostMapping("/adjustments/delete")
    public String deleteAdjustment(@RequestParam("month") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                                   @RequestParam("adjustmentId") int id,
                                   Principal principal, RedirectAttributes redirectAttributes) {
        try {
            payrollService.deleteAdjustment(id, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage", "The adjustment was removed.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/payroll/adjustments?month=" + month;
    }
}
