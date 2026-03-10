package com.warehouse.system.dto;

import com.warehouse.system.entity.Item;
import java.util.List;

public class StockGroup {
    private Item item; // פריט מייצג
    private long count;

    // הוסף את השדות האלו:
    private boolean isKit;
    private List<KitComponentDto> components;

    public StockGroup(Item item, long count) {
        this.item = item;
        this.count = count;

        // כאן אנחנו מחברים את הלוגיקה של ה-ItemDto לתוך ה-Group
        ItemDto dto = ItemDto.fromEntity(item);
        this.isKit = dto.isKit();
        this.components = dto.components();
    }

    // Getters הכרחיים עבור Thymeleaf
    public Item getItem() { return item; }
    public long getCount() { return count; }
    public boolean getIsKit() { return isKit; } // חשוב: השם חייב להתחיל ב-get
    public List<KitComponentDto> getComponents() { return components; }
}