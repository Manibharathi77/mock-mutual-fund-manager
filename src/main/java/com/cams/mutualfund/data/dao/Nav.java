package com.cams.mutualfund.data.dao;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "nav")
public class Nav {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private Double navValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "script_id", nullable = false)
    private Script script;

    public Nav() {}

    public Nav(LocalDate date, Double navValue, Script script) {
        this.date = date;
        this.navValue = navValue;
        this.script = script;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getNavValue() {
        return navValue;
    }

    public void setNavValue(Double navValue) {
        this.navValue = navValue;
    }

    public Script getScript() {
        return script;
    }

    public void setScript(Script script) {
        this.script = script;
    }
}
