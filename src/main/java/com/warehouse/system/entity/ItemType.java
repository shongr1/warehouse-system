package com.warehouse.system.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item_types")
public class ItemType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String catalogNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean serialized;

    // ===============================
    // NEW: האם זה מארז / ערכה
    // ===============================
    @Column(name = "is_kit", nullable = false)
    private boolean kit = false;

    // ===============================
    // NEW: רשימת תתי־פריטים לערכה
    // ===============================
    @OneToMany(
            mappedBy = "kitType",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<KitComponent> components = new ArrayList<>();

    public ItemType() {}

    // ===============================
    // Getters / Setters
    // ===============================

    public Long getId() { return id; }

    public String getCatalogNumber() { return catalogNumber; }
    public void setCatalogNumber(String catalogNumber) { this.catalogNumber = catalogNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isSerialized() { return serialized; }
    public void setSerialized(boolean serialized) { this.serialized = serialized; }

    public boolean isKit() { return kit; }
    public void setKit(boolean kit) { this.kit = kit; }

    public List<KitComponent> getComponents() { return components; }
    public void setComponents(List<KitComponent> components) { this.components = components; }

    // ===============================
    // Helpers נוחים להוספה/הסרה
    // ===============================
    public void addComponent(KitComponent component) {
        components.add(component);
        component.setKitType(this);
    }

    public void removeComponent(KitComponent component) {
        components.remove(component);
        component.setKitType(null);
    }
}