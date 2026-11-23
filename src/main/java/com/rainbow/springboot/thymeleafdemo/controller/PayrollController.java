package com.rainbow.springboot.thymeleafdemo.controller;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rainbow.springboot.thymeleafdemo.dto.PayrollItemTotal;
import com.rainbow.springboot.thymeleafdemo.dto.PayrollRow;
import com.rainbow.springboot.thymeleafdemo.dto.PayrollSummary;
import com.rainbow.springboot.thymeleafdemo.service.PayrollService;
import com.rainbow.springboot.thymeleafdemo.service.PdfExportService;
import com.rainbow.springboot.thymeleafdemo.util.CsvUtil;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Monthly payroll: view the month, generate it, finalise it, export it.
 * Viewing needs CAN_VIEW_PAYROLL (or CAN_MANAGE_PAYROLL); generating and finalising need CAN_MANAGE_PAYROLL.
 */
@Controller
@RequestMapping("/payroll")
@PreAuthorize("hasAnyAuthority('CAN_VIEW_PAYROLL','CAN_MANAGE_PAYROLL','SUPER_ADMIN')")
public class PayrollController {

    private static final String MANAGE = "hasAnyAuthority('CAN_MANAGE_PAYROLL','SUPER_ADMIN')";

    private final PayrollService payrollService;
    private final PdfExportService pdfExportService;

    public PayrollController(PayrollService thePayrollService, PdfExportService thePdfExportService) {
        payrollService = thePayrollService;
        pdfExportService = thePdfExportService;
    }

    @GetMapping
    public String view(@RequestParam(value = "month", required = false)
                       @DateTimeFormat(pattern = "yyyy-MM") YearMonth month, Model model) {
        YearMonth selected = month != null ? month : YearMonth.now();
        PayrollSummary summary = payrollService.view(selected);
        model.addAttribute("summary", summary);
        model.addAttribute("month", selected.toString());
        model.addAttribute("currency", payrollService.getCurrency());
        if (!summary.isGenerated()) {
            model.addAttribute("readiness", payrollService.readiness(selected));
        }
        model.addAttribute("activeTab", "run");
        return "payroll/payroll";
    }

    @PostMapping("/generate")
    @PreAuthorize(MANAGE)
    public String generate(@RequestParam("month") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                           Principal principal, RedirectAttributes redirectAttributes) {
        try {
            payrollService.generate(month, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payroll for " + month + " was generated as a draft. Check it, then finalise it to lock the month.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/payroll?month=" + month;
    }

    @PostMapping("/finalise")
    @PreAuthorize(MANAGE)
    public String finalise(@RequestParam("month") @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                           Principal principal, RedirectAttributes redirectAttributes) {
        try {
            payrollService.finalise(month, principal.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Payroll for " + month + " is finalised and locked.");
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/payroll?month=" + month;
    }

    @GetMapping("/export.csv")
    public void exportCsv(@RequestParam(value = "month", required = false)
                          @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                          HttpServletResponse response) throws Exception {
        YearMonth selected = month != null ? month : YearMonth.now();
        PayrollSummary s = payrollService.view(selected);

        // one column per named earning / deduction, in the order they first appear
        Set<String> earningLabels = new LinkedHashSet<>();
        Set<String> deductionLabels = new LinkedHashSet<>();
        for (PayrollItemTotal t : s.getItemTotals()) {
            (t.isEarning() ? earningLabels : deductionLabels).add(t.getLabel());
        }

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payroll-" + selected + ".csv\"");
        try (PrintWriter w = new PrintWriter(response.getOutputStream(), false, StandardCharsets.UTF_8)) {
            w.write('﻿'); // lets Excel detect UTF-8
            java.util.List<String> head = new java.util.ArrayList<>(java.util.List.of(
                    "Employee", "Department", "Job title", "Base salary", "Unpaid days", "Unpaid deduction"));
            for (String l : earningLabels) { head.add("+ " + l); }
            head.add("Gross");
            for (String l : deductionLabels) { head.add("- " + l); }
            head.add("Total deductions");
            head.add("Net pay");
            w.println(CsvUtil.row(head.toArray(new String[0])));

            for (PayrollRow r : s.getRows()) {
                java.util.List<String> cells = new java.util.ArrayList<>(java.util.List.of(
                        r.getName(), r.getDepartment(), nz(r.getJobTitle()), r.getBase().toPlainString(),
                        String.valueOf(r.getUnpaidDays()), r.getUnpaidDeduction().toPlainString()));
                for (String l : earningLabels) { cells.add(sumOf(r, l, true)); }
                cells.add(r.getGross().toPlainString());
                for (String l : deductionLabels) { cells.add(sumOf(r, l, false)); }
                cells.add(r.getDeductions().toPlainString());
                cells.add(r.getNet().toPlainString());
                w.println(CsvUtil.row(cells.toArray(new String[0])));
            }

            java.util.List<String> tot = new java.util.ArrayList<>(java.util.List.of(
                    "TOTAL", "", "", s.getTotalBase().toPlainString(), "", s.getTotalUnpaid().toPlainString()));
            for (String l : earningLabels) { tot.add(totalOf(s, l, true)); }
            tot.add(s.getTotalGross().toPlainString());
            for (String l : deductionLabels) { tot.add(totalOf(s, l, false)); }
            tot.add(s.getTotalDeductions().toPlainString());
            tot.add(s.getTotalNet().toPlainString());
            w.println(CsvUtil.row(tot.toArray(new String[0])));
        }
    }

    @GetMapping("/export.pdf")
    public void exportPdf(@RequestParam(value = "month", required = false)
                          @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                          HttpServletResponse response) throws Exception {
        YearMonth selected = month != null ? month : YearMonth.now();
        byte[] pdf = pdfExportService.payrollPdf(payrollService.view(selected), payrollService.getCurrency());

        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payroll-" + selected + ".pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
    }

    private static String sumOf(PayrollRow r, String label, boolean earning) {
        java.math.BigDecimal sum = java.math.BigDecimal.ZERO.setScale(2);
        for (PayrollRow.Item i : r.getItems()) {
            if (i.getLabel().equals(label) && i.isEarning() == earning) {
                sum = sum.add(i.getAmount());
            }
        }
        return sum.toPlainString();
    }

    private static String totalOf(PayrollSummary s, String label, boolean earning) {
        for (PayrollItemTotal t : s.getItemTotals()) {
            if (t.getLabel().equals(label) && t.isEarning() == earning) {
                return t.getAmount().toPlainString();
            }
        }
        return "0.00";
    }

    private static String nz(String v) {
        return v == null ? "" : v;
    }
}
