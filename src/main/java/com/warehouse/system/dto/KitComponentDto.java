package com.warehouse.system.dto;

import com.warehouse.system.entity.KitComponentStatus;

public record KitComponentDto(
        String componentName,
        int actualQty,
        int expectedQty,
        String status
) {
    // בנאי נוסף שמאפשר ל-ItemDto לשלוח את ה-Enum ישירות מה-Entity
    public KitComponentDto(String componentName, int actualQty, int expectedQty, KitComponentStatus status) {
        this(
                componentName,
                actualQty,
                expectedQty,
                status != null ? status.name() : "OK"
        );
    }
}