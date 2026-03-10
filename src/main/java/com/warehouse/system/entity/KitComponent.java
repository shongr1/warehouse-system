package com.warehouse.system.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "kit_components")
public class KitComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // הקישור לפריט הספציפי (ה-Kit עצמו) במחסן
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "component_name", nullable = false)
    private String componentName;

    @Column(name = "sub_catalog_number")
    private String subCatalogNumber;

    @Column(name = "expected_quantity", nullable = false)
    private int expectedQuantity; // הכמות שצריכה להיות לפי התקן

    @Column(name = "actual_quantity", nullable = false)
    private int actualQuantity;   // הכמות שיש בערכה הזו כרגע בפועל

    @Column(name = "status")
    private String status;        // AVAILABLE, MISSING, DAMAGED

    // --- Constructors ---
    public KitComponent() {}

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }

    public String getSubCatalogNumber() { return subCatalogNumber; }
    public void setSubCatalogNumber(String subCatalogNumber) { this.subCatalogNumber = subCatalogNumber; }

    public int getExpectedQuantity() { return expectedQuantity; }
    public void setExpectedQuantity(int expectedQuantity) { this.expectedQuantity = expectedQuantity; }

    public int getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(int actualQuantity) { this.actualQuantity = actualQuantity; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // ============================================================
    // "גשר" לפתרון שגיאות ה-Controller (התאמה לשם הישן)
    // ============================================================
    public int getQuantity() {
        return expectedQuantity;
    }

    public void setQuantity(int quantity) {
        this.expectedQuantity = quantity;
    }
    // בתוך KitComponent.java

    public ItemType getKitType() {
        if (this.item != null) {
            return this.item.getItemType();
        }
        return null;
    }
    // בתוך KitComponent.java

    public void setKitType(ItemType kitType) {
        // אנחנו לא שומרים את ה-kitType ישירות ברכיב,
        // אלא מוודאים שהרכיב מקושר ל-Item ששייך ל-ItemType הזה.
        if (this.item != null) {
            this.item.setItemType(kitType);
        }

    }
    // בתוך KitComponent.java

    // גשר עבור getActualQty -> מפנה ל-actualQuantity
    public int getActualQty() {
        return this.actualQuantity;
    }

    public void setActualQty(int actualQty) {
        this.actualQuantity = actualQty;
    }

    // גשר עבור getExpectedQty -> מפנה ל-expectedQuantity
    public int getExpectedQty() {
        return this.expectedQuantity;
    }

    public void setExpectedQty(int expectedQty) {
        this.expectedQuantity = expectedQty;
    }
}