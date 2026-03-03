package com.warehouse.system.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "kit_components",
        uniqueConstraints = @UniqueConstraint(columnNames = {"kit_item_type_id", "sub_catalog_number"})
)
public class KitComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "kit_item_type_id", nullable = false)
    private ItemType kitType;

    @Column(name = "component_name", nullable = false)
    private String componentName;

    @Column(name = "sub_catalog_number", nullable = false)
    private String subCatalogNumber;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public Long getId() { return id; }

    public ItemType getKitType() { return kitType; }
    public void setKitType(ItemType kitType) { this.kitType = kitType; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public String getSubCatalogNumber() { return subCatalogNumber; }
    public void setSubCatalogNumber(String subCatalogNumber) { this.subCatalogNumber = subCatalogNumber; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}