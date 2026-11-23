package com.rainbow.springboot.thymeleafdemo.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One earning or deduction inside a saved payroll line, with how it was worked out. */
@Entity
@Table(name = "payroll_line_item")
public class PayrollLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "line_id", nullable = false)
    private PayrollLine line;

    @Column(name = "label", nullable = false, length = 60)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 12)
    private PayrollItemKind kind;

    @Column(name = "basis", length = 100)
    private String basis;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    public PayrollLineItem() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public PayrollLine getLine() { return line; }
    public void setLine(PayrollLine line) { this.line = line; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public PayrollItemKind getKind() { return kind; }
    public void setKind(PayrollItemKind kind) { this.kind = kind; }
    public String getBasis() { return basis; }
    public void setBasis(String basis) { this.basis = basis; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
