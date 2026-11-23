package com.rainbow.springboot.thymeleafdemo.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** One employee's saved pay for a generated month. Names are copied so the line survives later edits. */
@Entity
@Table(name = "payroll_line")
public class PayrollLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private PayrollRun run;

    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "employee_name", nullable = false, length = 120)
    private String employeeName;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "base", nullable = false, precision = 12, scale = 2)
    private BigDecimal base;

    @Column(name = "unpaid_days", nullable = false)
    private int unpaidDays;

    @Column(name = "unpaid_deduction", nullable = false, precision = 12, scale = 2)
    private BigDecimal unpaidDeduction;

    @Column(name = "earnings", nullable = false, precision = 12, scale = 2)
    private BigDecimal earnings;

    @Column(name = "gross", nullable = false, precision = 12, scale = 2)
    private BigDecimal gross;

    @Column(name = "deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal deductions;

    @Column(name = "net", nullable = false, precision = 12, scale = 2)
    private BigDecimal net;

    @OneToMany(mappedBy = "line", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PayrollLineItem> items = new ArrayList<>();

    public PayrollLine() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public PayrollRun getRun() { return run; }
    public void setRun(PayrollRun run) { this.run = run; }
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public BigDecimal getBase() { return base; }
    public void setBase(BigDecimal base) { this.base = base; }
    public int getUnpaidDays() { return unpaidDays; }
    public void setUnpaidDays(int unpaidDays) { this.unpaidDays = unpaidDays; }
    public BigDecimal getUnpaidDeduction() { return unpaidDeduction; }
    public void setUnpaidDeduction(BigDecimal unpaidDeduction) { this.unpaidDeduction = unpaidDeduction; }
    public BigDecimal getEarnings() { return earnings; }
    public void setEarnings(BigDecimal earnings) { this.earnings = earnings; }
    public BigDecimal getGross() { return gross; }
    public void setGross(BigDecimal gross) { this.gross = gross; }
    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }
    public BigDecimal getNet() { return net; }
    public void setNet(BigDecimal net) { this.net = net; }
    public List<PayrollLineItem> getItems() { return items; }
    public void setItems(List<PayrollLineItem> items) { this.items = items; }

    public void addItem(PayrollLineItem item) {
        item.setLine(this);
        items.add(item);
    }
}
