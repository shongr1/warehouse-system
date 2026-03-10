package com.warehouse.system.dto;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public record ItemDto(
        Long id,
        String serialNumber,
        String itemTypeName,
        String warehouseName,
        String warehouseOwnerPn,
        String status,
        String notes, // השדה שהיה חסר וגרם לשגיאה ב-Log
        LocalDateTime signatureExpiryDate,
        long daysRemaining,
        boolean isKit,
        List<KitComponentDto> components
) {
    public static ItemDto fromEntity(com.warehouse.system.entity.Item item) {
        // חישוב ימים לחתימה
        long days = (item.getSignatureExpiryDate() != null)
                ? ChronoUnit.DAYS.between(LocalDateTime.now(), item.getSignatureExpiryDate())
                : 0;

        // חילוץ מספר אישי בעלים
        String ownerPn = (item.getWarehouse() != null && item.getWarehouse().getOwner() != null)
                ? item.getWarehouse().getOwner().getPersonalNumber()
                : "";

        // טיפול בקיטים - מיפוי מלא כולל כמויות וסטטוס
        boolean isKit = (item.getKitComponents() != null && !item.getKitComponents().isEmpty());
        List<KitComponentDto> componentDtos = isKit
                ? item.getKitComponents().stream()
                .map(comp -> new KitComponentDto(
                        comp.getComponentName(),
                        comp.getActualQty(),
                        comp.getExpectedQty(),
                        comp.getStatus() // שולח את ה-Enum לבנאי ה-Dto
                ))
                .toList()
                : new ArrayList<>();

        return new ItemDto(
                item.getId(),
                item.getSerialNumber(),
                item.getItemType().getName(),
                item.getWarehouse().getName(),
                ownerPn,
                item.getStatus().name(),
                item.getNotes() != null ? item.getNotes() : "", // מגן מפני Null ב-Thymeleaf
                item.getSignatureExpiryDate(),
                days,
                isKit,
                componentDtos
        );
    }
}