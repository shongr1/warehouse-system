package com.warehouse.system.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "item_types")
public class ItemType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String catalogNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean serialized;

    public ItemType() {}

    public Long getId() { return id; }

    public String getCatalogNumber() { return catalogNumber; }
    public void setCatalogNumber(String catalogNumber) { this.catalogNumber = catalogNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isSerialized() { return serialized; }
    public void setSerialized(boolean serialized) { this.serialized = serialized; }
}
