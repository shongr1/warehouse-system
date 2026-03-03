package com.warehouse.system.dto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record ItemDto(
        Long id,
        String serialNumber,
        String itemTypeName,
        String warehouseName,
        String warehouseOwnerPn, // השדה שהיה חסר ל-Thymeleaf
        String status,
        LocalDateTime signatureExpiryDate,
        long daysRemaining
) {
    public static ItemDto fromEntity(com.warehouse.system.entity.Item item) {
        long days = 0;
        if (item.getSignatureExpiryDate() != null) {
            days = ChronoUnit.DAYS.between(LocalDateTime.now(), item.getSignatureExpiryDate());
        }

        // שליפת ה-PN של בעל המחסן בבטחה
        String ownerPn = "";
        if (item.getWarehouse() != null && item.getWarehouse().getOwner() != null) {
            ownerPn = item.getWarehouse().getOwner().getPersonalNumber();
        }

        return new ItemDto(
                item.getId(),
                item.getSerialNumber(),
                item.getItemType().getName(),
                item.getWarehouse().getName(),
                ownerPn, // הוספה לבנאי
                item.getStatus().name(),
                item.getSignatureExpiryDate(),
                days
        );
    }
}