package com.cams.mutualfund.data.dao;

import com.cams.mutualfund.data.Roles;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "cams_user")
public class CamsUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    /**
     * For simplicity purpose, assuming it is one to one mapping between
     * CamsUser and Role. We can explore one user to many roles if needed.
     */
    @Column(nullable = false)
    private Roles role; // "ADMIN" or "USER"

    @OneToMany(mappedBy = "camsUser", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Transaction> transactions;

    public CamsUser(String username, String password, Roles role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public CamsUser() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Roles getRole() {
        return role;
    }

    public void setRole(Roles role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


}
