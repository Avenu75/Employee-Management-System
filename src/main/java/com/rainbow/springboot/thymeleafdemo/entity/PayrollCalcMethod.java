package com.rainbow.springboot.thymeleafdemo.entity;

/** How the amount of a payroll item is worked out. */
public enum PayrollCalcMethod {
    PERCENT("Percentage"),
    FIXED("Fixed amount");

    private final String label;

    PayrollCalcMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
