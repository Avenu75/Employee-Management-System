package com.rainbow.springboot.thymeleafdemo.dto;

import java.math.BigDecimal;
import java.util.List;

import com.rainbow.springboot.thymeleafdemo.entity.PayrollItemKind;

/** One employee's line in the monthly payroll, with every earning and deduction that made it up. */
public class PayrollRow {

    /** One earning or deduction on the line. */
    public static class Item {
        private final String label;
        private final PayrollItemKind kind;
        private final String basis;
        private final BigDecimal amount;

        public Item(String label, PayrollItemKind kind, String basis, BigDecimal amount) {
            this.label = label;
            this.kind = kind;
            this.basis = basis;
            this.amount = amount;
        }

        public String getLabel() { return label; }
        public PayrollItemKind getKind() { return kind; }
        public String getBasis() { return basis; }
        public BigDecimal getAmount() { return amount; }
        public boolean isEarning() { return kind == PayrollItemKind.EARNING; }
    }

    private final Integer employeeId;
    private final String name;
    private final String jobTitle;
    private final String department;
    private final BigDecimal base;
    private final int unpaidDays;
    private final BigDecimal unpaidDeduction;
    private final BigDecimal earnings;
    private final BigDecimal gross;
    private final BigDecimal deductions;
    private final BigDecimal net;
    private final List<Item> items;

    public PayrollRow(Integer employeeId, String name, String jobTitle, String department, BigDecimal base,
                      int unpaidDays, BigDecimal unpaidDeduction, BigDecimal earnings, BigDecimal gross,
                      BigDecimal deductions, BigDecimal net, List<Item> items) {
        this.employeeId = employeeId;
        this.name = name;
        this.jobTitle = jobTitle;
        this.department = department;
        this.base = base;
        this.unpaidDays = unpaidDays;
        this.unpaidDeduction = unpaidDeduction;
        this.earnings = earnings;
        this.gross = gross;
        this.deductions = deductions;
        this.net = net;
        this.items = items;
    }

    public Integer getEmployeeId() { return employeeId; }
    public String getName() { return name; }
    public String getJobTitle() { return jobTitle; }
    public String getDepartment() { return department; }
    public BigDecimal getBase() { return base; }
    public int getUnpaidDays() { return unpaidDays; }
    public BigDecimal getUnpaidDeduction() { return unpaidDeduction; }
    public BigDecimal getEarnings() { return earnings; }
    public BigDecimal getGross() { return gross; }
    public BigDecimal getDeductions() { return deductions; }
    public BigDecimal getNet() { return net; }
    public List<Item> getItems() { return items; }

    public boolean isNegative() {
        return net.signum() < 0;
    }
}
