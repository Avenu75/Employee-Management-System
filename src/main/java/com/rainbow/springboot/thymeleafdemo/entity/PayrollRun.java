package com.rainbow.springboot.thymeleafdemo.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * The saved payroll for one month. Every figure is copied into the lines when
 * the month is generated, so changing a rate later never rewrites old payroll.
 */
@Entity
@Table(name = "payroll_run")
public class PayrollRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    /** yyyy-MM, unique: there is one run per month. */
    @Column(name = "pay_month", nullable = false, unique = true, length = 7)
    private String month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private PayrollRunStatus status = PayrollRunStatus.DRAFT;

    @Column(name = "working_days", nullable = false)
    private int workingDays;

    @Column(name = "generated_by", length = 50)
    private String generatedBy;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt = LocalDateTime.now();

    @Column(name = "finalised_by", length = 50)
    private String finalisedBy;

    @Column(name = "finalised_at")
    private LocalDateTime finalisedAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("employeeName ASC")
    private List<PayrollLine> lines = new ArrayList<>();

    public PayrollRun() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
    public PayrollRunStatus getStatus() { return status; }
    public void setStatus(PayrollRunStatus status) { this.status = status; }
    public int getWorkingDays() { return workingDays; }
    public void setWorkingDays(int workingDays) { this.workingDays = workingDays; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public String getFinalisedBy() { return finalisedBy; }
    public void setFinalisedBy(String finalisedBy) { this.finalisedBy = finalisedBy; }
    public LocalDateTime getFinalisedAt() { return finalisedAt; }
    public void setFinalisedAt(LocalDateTime finalisedAt) { this.finalisedAt = finalisedAt; }
    public List<PayrollLine> getLines() { return lines; }
    public void setLines(List<PayrollLine> lines) { this.lines = lines; }

    public void addLine(PayrollLine line) {
        line.setRun(this);
        lines.add(line);
    }
}
