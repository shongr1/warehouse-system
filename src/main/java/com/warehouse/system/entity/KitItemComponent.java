package com.warehouse.system.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "kit_item_components",
        uniqueConstraints = @UniqueConstraint(columnNames = {"kit_item_id", "kit_component_id"})
)
public class KitItemComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "kit_item_id", nullable = false)
    private Item kitItem;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "kit_component_id", nullable = false)
    private KitComponent kitComponent;

    // --- NEW: שדות לתיעוד השם והמק"ט ברגע היצירה ---
    @Column(name = "component_name")
    private String componentName;

    @Column(name = "sub_catalog_number")
    private String subCatalogNumber;
    // ------------------------------------------

    @Column(name = "expected_qty", nullable = false)
    private int expectedQty;

    @Column(name = "actual_qty", nullable = false)
    private int actualQty = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private KitComponentStatus status = KitComponentStatus.MISSING;

    @Column(columnDefinition = "text")
    private String notes;

    /* Getters & Setters */

    public Long getId() { return id; }

    public Item getKitItem() { return kitItem; }
    public void setKitItem(Item kitItem) { this.kitItem = kitItem; }

    public KitComponent getKitComponent() { return kitComponent; }
    public void setKitComponent(KitComponent kitComponent) { this.kitComponent = kitComponent; }

    // Getters/Setters החדשים
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public String getSubCatalogNumber() { return subCatalogNumber; }
    public void setSubCatalogNumber(String subCatalogNumber) { this.subCatalogNumber = subCatalogNumber; }

    public int getExpectedQty() { return expectedQty; }
    public void setExpectedQty(int expectedQty) { this.expectedQty = expectedQty; }

    public int getActualQty() { return actualQty; }
    public void setActualQty(int actualQty) { this.actualQty = actualQty; }

    public KitComponentStatus getStatus() { return status; }
    public void setStatus(KitComponentStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}