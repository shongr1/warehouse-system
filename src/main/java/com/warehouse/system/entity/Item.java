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

    // --- הוספת שדה כמות (Quantity) לניהול פריטי Bulk ---
    // שנה את השורה הזו בתוך Item.java
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1; // מבטיח שתמיד תהיה לפחות יחידה אחת
    @ManyToOne(optional = false)
    @JoinColumn(name = "item_type_id", nullable = false)
    private ItemType itemType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    /* ========= מעקב וסטטוס ========= */

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

    @OneToMany(
            mappedBy = "kitItem",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<KitItemComponent> kitComponents = new ArrayList<>();

    public void addKitComponent(KitItemComponent component) {
        kitComponents.add(component);
        component.setKitItem(this);
    }

    /* ========= Getters / Setters ========= */

    public Long getId() { return id; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    // Getter & Setter לכמות (פותר את השגיאה ב-Service)
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

    public List<KitItemComponent> getKitComponents() { return kitComponents; }
    public void setKitComponents(List<KitItemComponent> kitComponents) { this.kitComponents = kitComponents; }
}