package com.warehouse.system.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "serial_number", nullable = true)
    private String serialNumber;

    @Column(name = "internal_catalog_id", nullable = true)
    private String internalCatalogId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_type_id", nullable = false)
    private ItemType itemType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @ManyToOne
    @JoinColumn(name = "signed_by_user_id")
    private User signedBy;

    @Column(name = "signature_date")
    private LocalDateTime signatureDate;

    @Column(name = "signature_expiry_date")
    private LocalDateTime signatureExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ItemStatus status = ItemStatus.IN_STOCK;

    @Column(name = "location")
    private String location;

    @Column(name = "notes")
    private String notes;

    // ==========================================
    // תיקון פה: שינוי ל-components ועדכון ה-mappedBy
    // ==========================================
    @OneToMany(
            mappedBy = "item", // חייב להתאים לשם השדה בתוך KitComponent
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<KitComponent> components = new ArrayList<>();

    public void addComponent(KitComponent component) {
        components.add(component);
        component.setItem(this);
    }

    /* ========= Getters / Setters ========= */

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getInternalCatalogId() { return internalCatalogId; }
    public void setInternalCatalogId(String internalCatalogId) { this.internalCatalogId = internalCatalogId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public User getSignedBy() { return signedBy; }
    public void setSignedBy(User signedBy) { this.signedBy = signedBy; }

    public LocalDateTime getSignatureDate() { return signatureDate; }
    public void setSignatureDate(LocalDateTime signatureDate) { this.signatureDate = signatureDate; }

    public LocalDateTime getSignatureExpiryDate() { return signatureExpiryDate; }
    public void setSignatureExpiryDate(LocalDateTime signatureExpiryDate) { this.signatureExpiryDate = signatureExpiryDate; }

    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<KitComponent> getComponents() { return components; }
    public void setComponents(List<KitComponent> components) { this.components = components; }
    // בתוך Item.java

    // גשר לפתרון השגיאה - מחזיר את רשימת הרכיבים בשם שה-Service מחפש
    public List<KitComponent> getKitComponents() {
        return this.components;
    }

    // אופציונלי: אם יש לך קוד שמנסה גם להגדיר את הרשימה בשם הזה
    public void setKitComponents(List<KitComponent> kitComponents) {
        this.components = kitComponents;
    }


}