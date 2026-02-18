package com.warehouse.system.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"item_type_id", "serial_number"})
)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="serial_number", nullable = false)
    private String serialNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_type_id", nullable = false)
    private ItemType itemType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    /* ========= NEW FIELDS (tracking) ========= */

    // מי מחזיק/הוקצה אליו בפועל (בעלות אצל אדם) – אופציונלי
    @ManyToOne
    @JoinColumn(name = "owner_user_id")
    private User owner;

    // מי חתום/אחראי כרגע – אופציונלי
    @ManyToOne
    @JoinColumn(name = "signed_by_user_id")
    private User signedBy;

    // מצב הפריט
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ItemStatus status = ItemStatus.IN_STOCK;

    // מיקום בתוך המחסן (מדף/חדר/ארון) – אופציונלי
    @Column(name = "location")
    private String location;

    /* ========= getters/setters ========= */

    public Long getId() { return id; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public User getSignedBy() { return signedBy; }
    public void setSignedBy(User signedBy) { this.signedBy = signedBy; }

    public ItemStatus getStatus() { return status; }
    public void setStatus(ItemStatus status) { this.status = status; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
