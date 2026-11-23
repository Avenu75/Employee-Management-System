package com.rainbow.springboot.thymeleafdemo.entity;

/** Whether a payroll item adds to an employee's pay or is taken off it. */
public enum PayrollItemKind {
    EARNING("Earning (added to pay)"),
    DEDUCTION("Deduction (taken from pay)");

    private final String label;

    PayrollItemKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
