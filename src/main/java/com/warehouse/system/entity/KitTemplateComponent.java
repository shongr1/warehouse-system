package com.warehouse.system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "kit_template_components")
public class KitTemplateComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_type_id", nullable = false)
    @JsonIgnore // מונע לולאה אינסופית בזמן יצירת JSON ומאפשר לטבלה ב-UI להתמלא
    private ItemType kitType;

    @Column(name = "component_name", nullable = false)
    private String componentName;

    @Column(name = "sub_catalog_number")
    private String subCatalogNumber;

    @Column(name = "quantity", nullable = false)
    private int quantity; // הכמות שצריכה להיות בערכה כזו כברירת מחדל

    public KitTemplateComponent() {}

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ItemType getKitType() { return kitType; }
    public void setKitType(ItemType kitType) { this.kitType = kitType; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public String getSubCatalogNumber() { return subCatalogNumber; }
    public void setSubCatalogNumber(String subCatalogNumber) { this.subCatalogNumber = subCatalogNumber; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}