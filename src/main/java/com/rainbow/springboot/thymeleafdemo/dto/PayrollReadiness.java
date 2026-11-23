package com.rainbow.springboot.thymeleafdemo.dto;

import java.util.List;

import com.rainbow.springboot.thymeleafdemo.entity.PayrollItem;

/** What will go into a month's payroll, shown before the person presses Generate. */
public class PayrollReadiness {

    private final List<PayrollItem> activeItems;
    private final int adjustmentCount;
    private final int employeeCount;
    private final List<String> noSalaryNames;

    public PayrollReadiness(List<PayrollItem> activeItems, int adjustmentCount, int employeeCount,
                            List<String> noSalaryNames) {
        this.activeItems = activeItems;
        this.adjustmentCount = adjustmentCount;
        this.employeeCount = employeeCount;
        this.noSalaryNames = noSalaryNames;
    }

    public List<PayrollItem> getActiveItems() { return activeItems; }
    public int getAdjustmentCount() { return adjustmentCount; }
    public int getEmployeeCount() { return employeeCount; }
    public List<String> getNoSalaryNames() { return noSalaryNames; }

    /** Payroll cannot be generated until at least one rule exists and there is someone to pay. */
    public boolean isReady() {
        return !activeItems.isEmpty() && employeeCount > 0;
    }
}
