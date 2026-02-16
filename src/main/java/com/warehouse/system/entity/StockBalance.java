package com.warehouse.system.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "stock_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "item_type_id"})
)
public class StockBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_type_id", nullable = false)
    private ItemType itemType;

    @Column(nullable = false)
    private int quantity;

    public StockBalance() {}

    public Long getId() { return id; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
