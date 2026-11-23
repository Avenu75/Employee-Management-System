package com.rainbow.springboot.thymeleafdemo.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rainbow.springboot.thymeleafdemo.entity.PayrollItemKind;
import com.rainbow.springboot.thymeleafdemo.entity.PayrollRunStatus;

/**
 * Payroll for one month with totals. It is either a PREVIEW worked out live from
 * the current settings (status null) or a saved run (DRAFT or FINALISED).
 */
public class PayrollSummary {

    private final YearMonth month;
    private final int workingDays;
    private final PayrollRunStatus status;
    private final String generatedBy;
    private final LocalDateTime generatedAt;
    private final String finalisedBy;
    private final LocalDateTime finalisedAt;
    private final List<PayrollRow> rows;
    private final BigDecimal totalBase;
    private final BigDecimal totalUnpaid;
    private final BigDecimal totalEarnings;
    private final BigDecimal totalGross;
    private final BigDecimal totalDeductions;
    private final BigDecimal totalNet;
    private final List<PayrollItemTotal> itemTotals;
    private final Map<String, BigDecimal> netByDepartment;

    public PayrollSummary(YearMonth month, int workingDays, PayrollRunStatus status, String generatedBy,
                          LocalDateTime generatedAt, String finalisedBy, LocalDateTime finalisedAt,
                          List<PayrollRow> rows) {
        this.month = month;
        this.workingDays = workingDays;
        this.status = status;
        this.generatedBy = generatedBy;
        this.generatedAt = generatedAt;
        this.finalisedBy = finalisedBy;
        this.finalisedAt = finalisedAt;
        this.rows = rows;

        BigDecimal base = zero();
        BigDecimal unpaid = zero();
        BigDecimal earn = zero();
        BigDecimal gross = zero();
        BigDecimal ded = zero();
        BigDecimal net = zero();
        Map<String, BigDecimal> byDept = new LinkedHashMap<>();
        Map<String, BigDecimal> itemAmount = new LinkedHashMap<>();
        Map<String, Integer> itemCount = new LinkedHashMap<>();
        Map<String, PayrollItemKind> itemKind = new LinkedHashMap<>();
        for (PayrollRow r : rows) {
            base = base.add(r.getBase());
            unpaid = unpaid.add(r.getUnpaidDeduction());
            earn = earn.add(r.getEarnings());
            gross = gross.add(r.getGross());
            ded = ded.add(r.getDeductions());
            net = net.add(r.getNet());
            byDept.merge(r.getDepartment(), r.getNet(), BigDecimal::add);
            for (PayrollRow.Item i : r.getItems()) {
                String key = i.getKind().name() + "|" + i.getLabel();
                itemAmount.merge(key, i.getAmount(), BigDecimal::add);
                itemCount.merge(key, 1, Integer::sum);
                itemKind.put(key, i.getKind());
            }
        }
        List<PayrollItemTotal> totals = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : itemAmount.entrySet()) {
            String label = e.getKey().substring(e.getKey().indexOf('|') + 1);
            totals.add(new PayrollItemTotal(label, itemKind.get(e.getKey()), e.getValue(), itemCount.get(e.getKey())));
        }
        this.totalBase = base;
        this.totalUnpaid = unpaid;
        this.totalEarnings = earn;
        this.totalGross = gross;
        this.totalDeductions = ded;
        this.totalNet = net;
        this.itemTotals = totals;
        this.netByDepartment = byDept;
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }

    public YearMonth getMonth() { return month; }
    public int getWorkingDays() { return workingDays; }
    public PayrollRunStatus getStatus() { return status; }
    public String getGeneratedBy() { return generatedBy; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public String getFinalisedBy() { return finalisedBy; }
    public LocalDateTime getFinalisedAt() { return finalisedAt; }
    public List<PayrollRow> getRows() { return rows; }
    public BigDecimal getTotalBase() { return totalBase; }
    public BigDecimal getTotalUnpaid() { return totalUnpaid; }
    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public BigDecimal getTotalGross() { return totalGross; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public BigDecimal getTotalNet() { return totalNet; }
    public List<PayrollItemTotal> getItemTotals() { return itemTotals; }
    public Map<String, BigDecimal> getNetByDepartment() { return netByDepartment; }

    public boolean isGenerated() { return status != null; }
    public boolean isFinalised() { return status == PayrollRunStatus.FINALISED; }

    /** How many lines have deductions larger than gross pay (net below zero). */
    public int getNegativeCount() {
        int n = 0;
        for (PayrollRow r : rows) {
            if (r.isNegative()) {
                n++;
            }
        }
        return n;
    }

    /** Each department's share of total net pay, as a whole percentage, for the bar chart. */
    public Map<String, Integer> getDepartmentSharePercent() {
        Map<String, Integer> shares = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : netByDepartment.entrySet()) {
            int pct = totalNet.signum() > 0
                    ? e.getValue().multiply(BigDecimal.valueOf(100)).divide(totalNet, 0, RoundingMode.HALF_UP).intValue()
                    : 0;
            shares.put(e.getKey(), Math.max(0, pct));
        }
        return shares;
    }
}
