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

    public Long getId() { return id; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }
}
