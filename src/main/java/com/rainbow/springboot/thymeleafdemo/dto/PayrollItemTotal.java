package com.rainbow.springboot.thymeleafdemo.dto;

import java.math.BigDecimal;

import com.rainbow.springboot.thymeleafdemo.entity.PayrollItemKind;

/** The total of one named earning or deduction across everybody in the month. */
public class PayrollItemTotal {

    private final String label;
    private final PayrollItemKind kind;
    private final BigDecimal amount;
    private final int employees;

    public PayrollItemTotal(String label, PayrollItemKind kind, BigDecimal amount, int employees) {
        this.label = label;
        this.kind = kind;
        this.amount = amount;
        this.employees = employees;
    }

    public String getLabel() { return label; }
    public PayrollItemKind getKind() { return kind; }
    public BigDecimal getAmount() { return amount; }
    public int getEmployees() { return employees; }
    public boolean isEarning() { return kind == PayrollItemKind.EARNING; }
}
