package com.zyntral.modules.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Maps the {@code roles} reference table (USER, ADMIN). */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    private Short id;

    @Column(nullable = false, unique = true)
    private String code;

    protected Role() {}

    public Short getId() { return id; }
    public String getCode() { return code; }
}
