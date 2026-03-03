package com.warehouse.system.dto;

import com.warehouse.system.entity.Item;

public class StockGroup {
    private Item item; // פריט מייצג
    private long count; // כמה יחידות יש ממנו

    public StockGroup(Item item, long count) {
        this.item = item;
        this.count = count;
    }
    // Getters
    public Item getItem() { return item; }
    public long getCount() { return count; }
}
