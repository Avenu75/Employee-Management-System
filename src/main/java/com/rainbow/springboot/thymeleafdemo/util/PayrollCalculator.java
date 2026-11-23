package com.rainbow.springboot.thymeleafdemo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.rainbow.springboot.thymeleafdemo.entity.PayrollCalcMethod;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollItemKind;

/**
 * Pure payroll arithmetic for ONE employee and ONE month, kept free of any
 * Spring or database code so it can be unit tested on its own.
 *
 *   unpaid deduction = monthly salary x unpaid days / working days in the month
 *   base after unpaid = monthly salary - unpaid deduction
 *   each EARNING     = percent of "base after unpaid", or a fixed amount
 *   gross            = base after unpaid + earnings
 *   each DEDUCTION   = percent of gross, or a fixed amount
 *   net              = gross - deductions
 *
 * The components (tax, pension, dues, allowances, one-off adjustments) come from
 * the payroll settings; nothing is hard-coded here.
 */
public final class PayrollCalculator {

    private PayrollCalculator() {
    }

    /** One rule to apply: a name, earning or deduction, and a percentage or fixed amount. */
    public static final class Component {
        private final String label;
        private final PayrollItemKind kind;
        private final PayrollCalcMethod method;
        private final BigDecimal value;
        private final boolean oneOff;

        public Component(String label, PayrollItemKind kind, PayrollCalcMethod method, BigDecimal value, boolean oneOff) {
            this.label = label;
            this.kind = kind;
            this.method = method;
            this.value = value == null ? BigDecimal.ZERO : value;
            this.oneOff = oneOff;
        }

        public String getLabel() { return label; }
        public PayrollItemKind getKind() { return kind; }
        public PayrollCalcMethod getMethod() { return method; }
        public BigDecimal getValue() { return value; }
        public boolean isOneOff() { return oneOff; }
    }

    /** One worked-out line: what it is, how it was calculated and the amount. */
    public static final class Line {
        private final String label;
        private final PayrollItemKind kind;
        private final String basis;
        private final BigDecimal amount;

        Line(String label, PayrollItemKind kind, String basis, BigDecimal amount) {
            this.label = label;
            this.kind = kind;
            this.basis = basis;
            this.amount = amount;
        }

        public String getLabel() { return label; }
        public PayrollItemKind getKind() { return kind; }
        public String getBasis() { return basis; }
        public BigDecimal getAmount() { return amount; }
    }

    public static final class Result {
        private final BigDecimal base;
        private final int unpaidDays;
        private final BigDecimal unpaidDeduction;
        private final BigDecimal earnings;
        private final BigDecimal gross;
        private final BigDecimal deductions;
        private final BigDecimal net;
        private final List<Line> lines;

        Result(BigDecimal base, int unpaidDays, BigDecimal unpaidDeduction, BigDecimal earnings,
               BigDecimal gross, BigDecimal deductions, BigDecimal net, List<Line> lines) {
            this.base = base;
            this.unpaidDays = unpaidDays;
            this.unpaidDeduction = unpaidDeduction;
            this.earnings = earnings;
            this.gross = gross;
            this.deductions = deductions;
            this.net = net;
            this.lines = Collections.unmodifiableList(lines);
        }

        public BigDecimal getBase() { return base; }
        public int getUnpaidDays() { return unpaidDays; }
        public BigDecimal getUnpaidDeduction() { return unpaidDeduction; }
        public BigDecimal getEarnings() { return earnings; }
        public BigDecimal getGross() { return gross; }
        public BigDecimal getDeductions() { return deductions; }
        public BigDecimal getNet() { return net; }
        public List<Line> getLines() { return lines; }

        /** The amount of the first line with this label, or zero (handy in tests). */
        public BigDecimal amountOf(String label) {
            for (Line l : lines) {
                if (l.getLabel().equals(label)) {
                    return l.getAmount();
                }
            }
            return BigDecimal.ZERO.setScale(2);
        }
    }

    public static Result compute(BigDecimal monthlySalary, int workingDaysInMonth, int unpaidDays,
                                 List<Component> components) {
        BigDecimal base = (monthlySalary == null ? BigDecimal.ZERO : monthlySalary).setScale(2, RoundingMode.HALF_UP);

        int days = Math.max(0, Math.min(unpaidDays, workingDaysInMonth));
        BigDecimal unpaidDeduction = BigDecimal.ZERO.setScale(2);
        if (workingDaysInMonth > 0 && days > 0) {
            unpaidDeduction = base.multiply(BigDecimal.valueOf(days))
                    .divide(BigDecimal.valueOf(workingDaysInMonth), 2, RoundingMode.HALF_UP);
        }
        BigDecimal adjustedBase = base.subtract(unpaidDeduction);

        List<Component> list = components == null ? List.of() : components;
        List<Line> lines = new ArrayList<>();

        BigDecimal earnings = BigDecimal.ZERO.setScale(2);
        for (Component c : list) {
            if (c.getKind() == PayrollItemKind.EARNING) {
                Line line = work(c, adjustedBase, "base after unpaid days");
                lines.add(line);
                earnings = earnings.add(line.getAmount());
            }
        }

        BigDecimal gross = adjustedBase.add(earnings);

        BigDecimal deductions = BigDecimal.ZERO.setScale(2);
        for (Component c : list) {
            if (c.getKind() == PayrollItemKind.DEDUCTION) {
                Line line = work(c, gross, "gross pay");
                lines.add(line);
                deductions = deductions.add(line.getAmount());
            }
        }

        BigDecimal net = gross.subtract(deductions);
        return new Result(base, days, unpaidDeduction, earnings, gross, deductions, net, lines);
    }

    private static Line work(Component c, BigDecimal basisAmount, String basisName) {
        BigDecimal amount;
        String basis;
        if (c.isOneOff()) {
            amount = c.getValue().setScale(2, RoundingMode.HALF_UP);
            basis = "One-off this month";
        } else if (c.getMethod() == PayrollCalcMethod.PERCENT) {
            amount = basisAmount.multiply(c.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            basis = c.getValue().stripTrailingZeros().toPlainString() + "% of " + basisName;
        } else {
            amount = c.getValue().setScale(2, RoundingMode.HALF_UP);
            basis = "Fixed amount";
        }
        return new Line(c.getLabel(), c.getKind(), basis, amount);
    }
}
