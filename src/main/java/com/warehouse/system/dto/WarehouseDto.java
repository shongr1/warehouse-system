package com.warehouse.system.dto;

public record WarehouseDto(
        Long id,
        String name,
        String location,
        OwnerDto owner
) {
    public record OwnerDto(
            Long id,
            String fullName,
            String personalNumber,
            String role
    ) {}
}
