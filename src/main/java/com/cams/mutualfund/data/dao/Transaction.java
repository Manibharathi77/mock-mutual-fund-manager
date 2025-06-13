package com.cams.mutualfund.data.dao;

import com.cams.mutualfund.data.TransactionType;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type; // BUY, REDEEM

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cams_user_id", nullable = false)
    private CamsUser camsUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "script_id", nullable = false)
    private Script script;

    @Column(nullable = false)
    private Double navValue;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private Double units;

    private LocalDate transactionDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public CamsUser getCamsUser() {
        return camsUser;
    }

    public void setCamsUser(CamsUser camsUser) {
        this.camsUser = camsUser;
    }

    public Script getScript() {
        return script;
    }

    public void setScript(Script script) {
        this.script = script;
    }

    public Double getNavValue() {
        return navValue;
    }

    public void setNavValue(Double navValue) {
        this.navValue = navValue;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getUnits() {
        return units;
    }

    public void setUnits(Double units) {
        this.units = units;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }
}

