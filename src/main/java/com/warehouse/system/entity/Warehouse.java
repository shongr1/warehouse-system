package com.warehouse.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "warehouses")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) // אל תשים unique אם לא חייב
    private String name;

    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id") // ❗ בלי unique
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User owner;

    public Warehouse() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public User getOwner() { return owner; }

    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }
    public void setOwner(User owner) { this.owner = owner; }
}
