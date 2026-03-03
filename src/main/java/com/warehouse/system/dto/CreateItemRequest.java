package com.warehouse.system.dto;

/**
 * אובייקט פשוט להעברת נתונים מה-Frontend ליצירת פריט חדש.
 */
public record CreateItemRequest(
        String serialNumber,
        Long itemTypeId,
        Long warehouseId,
        Long categoryId, // הוספנו את השדה החדש שביקשת
        String location
) {}