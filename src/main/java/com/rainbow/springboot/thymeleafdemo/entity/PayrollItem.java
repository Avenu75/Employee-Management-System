package com.rainbow.springboot.thymeleafdemo.entity;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * One payroll rule set up BEFORE payroll is generated: income tax, pension,
 * a union due, a transport allowance and so on. When a month is generated,
 * every active item that applies to an employee is worked out for them.
 *
 * A percentage item is taken from the base pay after unpaid days (earnings)
 * or from gross pay (deductions); a fixed item is a flat amount.
 */
@Entity
@Table(name = "payroll_item")
public class PayrollItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @NotBlank(message = "Name is required")
    @Size(max = 60, message = "Name must be at most 60 characters")
    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @NotNull(message = "Choose earning or deduction")
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 12)
    private PayrollItemKind kind = PayrollItemKind.DEDUCTION;

    @NotNull(message = "Choose how it is calculated")
    @Enumerated(EnumType.STRING)
    @Column(name = "calc_method", nullable = false, length = 10)
    private PayrollCalcMethod calcMethod = PayrollCalcMethod.PERCENT;

    /** A percentage such as 10 (meaning 10%), or a money amount, depending on calcMethod. */
    @NotNull(message = "Enter a value")
    @DecimalMin(value = "0.00", message = "The value cannot be negative")
    @DecimalMax(value = "99999999.99", message = "The value is too large")
    @Column(name = "item_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountValue;

    @Column(name = "all_employees", nullable = false)
    private boolean allEmployees = true;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Size(max = 200, message = "Note must be at most 200 characters")
    @Column(name = "note", length = 200)
    private String note;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "payroll_item_employee",
            joinColumns = @JoinColumn(name = "item_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id"))
    private Set<Employee> employees = new HashSet<>();

    public PayrollItem() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PayrollItemKind getKind() { return kind; }
    public void setKind(PayrollItemKind kind) { this.kind = kind; }
    public PayrollCalcMethod getCalcMethod() { return calcMethod; }
    public void setCalcMethod(PayrollCalcMethod calcMethod) { this.calcMethod = calcMethod; }
    public BigDecimal getAmountValue() { return amountValue; }
    public void setAmountValue(BigDecimal amountValue) { this.amountValue = amountValue; }
    public boolean isAllEmployees() { return allEmployees; }
    public void setAllEmployees(boolean allEmployees) { this.allEmployees = allEmployees; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Set<Employee> getEmployees() { return employees; }
    public void setEmployees(Set<Employee> employees) { this.employees = employees; }

    /** True when this item is worked out for the given employee id. */
    public boolean appliesTo(int employeeId) {
        if (allEmployees) {
            return true;
        }
        for (Employee e : employees) {
            if (e.getId() == employeeId) {
                return true;
            }
        }
        return false;
    }

    /** Short description for the settings table, e.g. "10% of gross" or "Fixed 50.00". */
    public String getDescribe() {
        if (amountValue == null) {
            return "";
        }
        if (calcMethod == PayrollCalcMethod.PERCENT) {
            return amountValue.stripTrailingZeros().toPlainString() + "%";
        }
        return amountValue.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
