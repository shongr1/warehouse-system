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

    @Column(name = "is_kit", nullable = false)
    private boolean kit = false;

    // --- השדה החדש: בעלים של סוג המוצר ---
    @ManyToOne
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @OneToMany(
            mappedBy = "kitType",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<KitTemplateComponent> kitTemplateComponents = new ArrayList<>();

    public ItemType() {}

    // --- Getters / Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCatalogNumber() { return catalogNumber; }
    public void setCatalogNumber(String catalogNumber) { this.catalogNumber = catalogNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isSerialized() { return serialized; }
    public void setSerialized(boolean serialized) { this.serialized = serialized; }

    public boolean isKit() { return kit; }
    public void setKit(boolean kit) { this.kit = kit; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public List<KitTemplateComponent> getKitTemplateComponents() {
        return kitTemplateComponents;
    }

    public void setKitTemplateComponents(List<KitTemplateComponent> kitTemplateComponents) {
        this.kitTemplateComponents = kitTemplateComponents;
    }

    // --- Helpers (ניהול הקשר הדו-כיווני) ---

    public void addKitTemplateComponent(KitTemplateComponent component) {
        if (component != null) {
            kitTemplateComponents.add(component);
            component.setKitType(this);
        }
    }

    public void removeKitTemplateComponent(KitTemplateComponent component) {
        if (component != null) {
            kitTemplateComponents.remove(component);
            component.setKitType(null);
        }
    }

    public void addTemplateComponent(KitTemplateComponent component) {
        this.addKitTemplateComponent(component);
    }

    public List<KitTemplateComponent> getTemplateComponents() {
        return this.kitTemplateComponents;
    }
}