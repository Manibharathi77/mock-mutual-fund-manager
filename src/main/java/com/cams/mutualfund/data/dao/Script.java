package com.cams.mutualfund.data.dao;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "script")
public class Script {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String fundCode;

    @Column(nullable = false)
    private String name;

    private String category;

    private String amc;

    private boolean active = true;

    @OneToMany(mappedBy = "script",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Nav> navs;

    public Script() {}

    public Script(String fundCode, String name, String category, String amc) {
        this.fundCode = fundCode;
        this.name = name;
        this.category = category;
        this.amc = amc;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFundCode() {
        return fundCode;
    }

    public void setFundCode(String fundCode) {
        this.fundCode = fundCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAmc() {
        return amc;
    }

    public void setAmc(String amc) {
        this.amc = amc;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Nav> getNavs() {
        return navs;
    }

    public void setNavs(List<Nav> navs) {
        this.navs = navs;
    }
}
